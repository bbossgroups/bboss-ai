/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.frameworkset.spi.ai.memory;

 

import org.frameworkset.spi.ai.memory.coordination.LocalPeriodicGate;
import org.frameworkset.spi.ai.memory.coordination.PeriodicGate;
import org.frameworkset.spi.ai.model.ModelInfo;

import java.time.Duration;
import java.util.Objects;

/**
 * Unified configuration for the long-term memory pipeline (flush + consolidation +
 * maintenance). Pair with {@link org.frameworkset.spi.ai.compaction.CompactionConfig} for the in-context summarization pipeline.
 *
 * <p>The harness has three LLM-driven memory operations, each with its own prompt and
 * triggering rules:
 *
 * <ol>
 *   <li><b>Flush</b> — extracts long-term memories from a conversation window into today's
 *       daily ledger ({@code memory/YYYY-MM-DD.md}). Prompt: {@link #flushPrompt()},
 *       defaults to {@link MemoryFlushManager#DEFAULT_FLUSH_PROMPT}. Trigger:
 *       {@link #flushTrigger()}.</li>
 *   <li><b>Consolidation</b> — periodically merges daily ledgers into the curated
 *       {@code MEMORY.md}. Prompt: {@link #consolidationPrompt()}, defaults to
 *       {@link MemoryConsolidator#DEFAULT_CONSOLIDATION_PROMPT}. Run cadence:
 *       {@link #consolidationMinGap()}.</li>
 *   <li><b>Compaction summary</b> — distills the conversation prefix into one summary
 *       message before reasoning. Prompt lives on {@link org.frameworkset.spi.ai.compaction.CompactionConfig#getSummaryPrompt()};
 *       configure via {@code .compaction(CompactionConfig...)} rather than here.</li>
 * </ol>
 *
 * <p>Flush and consolidation use independent throttle windows. A throttled policy allows the
 * first eligible call immediately; its minimum gap applies only between subsequent runs and is
 * not an initial delay. {@link FlushTrigger#never()} disables only the per-call flush.
 *
 * <p>All fields have sensible defaults; new MemoryConfig() returns a config equivalent
 * to the harness's historical behavior so adopting this class is a no-op upgrade.
 */
public final class MemoryConfig {

    /** Default {@code consolidationMaxTokens}. */
    public static final int DEFAULT_CONSOLIDATION_MAX_TOKENS = 4_000;

    /** Default {@code consolidationMinGap} — matches {@code MemoryMaintenanceMiddleware}. */
    public static final Duration DEFAULT_CONSOLIDATION_MIN_GAP = Duration.ofMinutes(30);

    /** Default retention before a daily ledger is archived. */
    public static final int DEFAULT_DAILY_FILE_RETENTION_DAYS = 90;

    /** Default retention before a session JSONL log is pruned. */
    public static final int DEFAULT_SESSION_RETENTION_DAYS = 180;
	
	public PeriodicGate getPeriodicGate() {
		return periodicGate;
	}
	
	public MemoryConfig setPeriodicGate(PeriodicGate periodicGate) {
		this.periodicGate = periodicGate;
		return this;
	}
	
	
	/** Strategy for the per-call flush hook. See {@link FlushTrigger}. */
    public enum FlushMode {
        /** Flush after every agent call. */
        ALWAYS,
        /** Disable per-call flush entirely (offload still runs). */
        NEVER,
        /** Flush at most once per {@link FlushTrigger#minGap()}. */
        THROTTLED
    }

    /**
     * Trigger policy for {link MemoryFlushMiddleware}.
     *
     * <p>A throttled trigger allows the first eligible call immediately. Its minimum gap is
     * measured between subsequent per-call flushes, independently of consolidation maintenance.
     * {@link #never()} disables only this per-call flush path.
     *
     * <p>{@code throttled(Duration.ZERO)} normalises to {@link #always()} so callers do not
     * need a special branch for the degenerate case.
     */
    public static final class FlushTrigger {

        private static final FlushTrigger ALWAYS_INSTANCE =
                new FlushTrigger(FlushMode.ALWAYS, Duration.ZERO);
        private static final FlushTrigger NEVER_INSTANCE =
                new FlushTrigger(FlushMode.NEVER, Duration.ZERO);

        private final FlushMode mode;
        private final Duration minGap;

        private FlushTrigger(FlushMode mode, Duration minGap) {
            this.mode = mode;
            this.minGap = minGap;
        }

        public static FlushTrigger always() {
            return ALWAYS_INSTANCE;
        }

        public static FlushTrigger never() {
            return NEVER_INSTANCE;
        }

        public static FlushTrigger throttled(Duration minGap) {
            if (minGap == null) {
                throw new IllegalArgumentException("minGap must not be null");
            }
            if (minGap.isNegative()) {
                throw new IllegalArgumentException("minGap must not be negative");
            }
            if (minGap.isZero()) {
                return ALWAYS_INSTANCE;
            }
            return new FlushTrigger(FlushMode.THROTTLED, minGap);
        }

        public FlushMode mode() {
            return mode;
        }

        public Duration minGap() {
            return minGap;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof FlushTrigger )) return false;
			FlushTrigger other = (FlushTrigger) o;
            return mode == other.mode && Objects.equals(minGap, other.minGap);
        }

        @Override
        public int hashCode() {
            return Objects.hash(mode, minGap);
        }

        @Override
        public String toString() {
            return mode == FlushMode.THROTTLED
                    ? "FlushTrigger{THROTTLED, minGap=" + minGap + "}"
                    : "FlushTrigger{" + mode + "}";
        }
    }

//    private final ModelInfo model;
//    private final String flushPrompt;
//    private final String consolidationPrompt;
//    private final int consolidationMaxTokens;
//    private final Duration consolidationMinGap;
//    private final int dailyFileRetentionDays;
//    private final int sessionRetentionDays;
//    private final FlushTrigger flushTrigger;
    private  PeriodicGate periodicGate = new LocalPeriodicGate();	
	private ModelInfo model = null;
	private String flushPrompt = null;
	private String consolidationPrompt = null;
	private int consolidationMaxTokens = DEFAULT_CONSOLIDATION_MAX_TOKENS;
	private Duration consolidationMinGap = DEFAULT_CONSOLIDATION_MIN_GAP;
	private int dailyFileRetentionDays = DEFAULT_DAILY_FILE_RETENTION_DAYS;
	private int sessionRetentionDays = DEFAULT_SESSION_RETENTION_DAYS;
	private FlushTrigger flushTrigger = FlushTrigger.always();

    public MemoryConfig( ) {
        
    }

    /**
     * Optional model override for memory operations (flush + consolidation).
     * {@code null} means use the agent's primary model.
     */
    public ModelInfo model() {
        return model;
    }

    /**
     * Override for the flush prompt. {@code null} means use
     * {@link MemoryFlushManager#DEFAULT_FLUSH_PROMPT}.
     */
    public String flushPrompt() {
        return flushPrompt;
    }

    /**
     * Override for the consolidation prompt. {@code null} means use
     * {@link MemoryConsolidator#DEFAULT_CONSOLIDATION_PROMPT}.
     *
     * <p>Custom prompts must contain exactly two {@code %d} placeholders (max-tokens and
     * max-chars, in that order). The Builder enforces this at construction time.
     */
    public String consolidationPrompt() {
        return consolidationPrompt;
    }

    public int consolidationMaxTokens() {
        return consolidationMaxTokens;
    }

    /**
     * Minimum gap between consolidation/maintenance runs. The first eligible call runs
     * immediately; this duration is not an initial delay and is independent of
     * {@link #flushTrigger()}.
     */
    public Duration consolidationMinGap() {
        return consolidationMinGap;
    }

    public int dailyFileRetentionDays() {
        return dailyFileRetentionDays;
    }

    public int sessionRetentionDays() {
        return sessionRetentionDays;
    }

    public FlushTrigger flushTrigger() {
        return flushTrigger;
    }
 

	/**
	 * Sets a dedicated model for memory operations (flush + consolidation),
	 * allowing a lighter/cheaper model than the agent's primary reasoning model.
	 * When not set, the agent's primary model is used.
	 */
	public MemoryConfig setModel(ModelInfo model) {
		this.model = model;
		return this;
	}


	/**
	 * Overrides the prompt used by {@link MemoryFlushManager#flushMemories}.
	 * {@code null} restores the default. The text is passed verbatim as a SYSTEM
	 * message — it does not need any placeholders.
	 */
	public MemoryConfig setFlushPrompt(String flushPrompt) {
		this.flushPrompt = flushPrompt;
		return this;
	}

	/**
	 * Overrides the prompt used by {@link MemoryConsolidator#consolidate}. The prompt
	 * is rendered via {@link String#format} with two {@code int} arguments
	 * (max-tokens, max-chars), so a custom prompt MUST contain exactly two {@code %d}
	 * placeholders. {@code null} restores the default.
	 *
	 * @throws IllegalArgumentException if the prompt does not contain exactly two
	 *     {@code %d} placeholders
	 */
	public MemoryConfig setConsolidationPrompt(String consolidationPrompt) {
		if (consolidationPrompt != null) {
			if(consolidationPrompt.indexOf("#[maxMemoryTokens]") < 0 || consolidationPrompt.indexOf("#[maxChars]") < 0)
			{
				throw new IllegalArgumentException(
						"consolidationPrompt must contain exactly two placeholders "
								+ "(#[maxMemoryTokens], #[maxChars]):"+consolidationPrompt);
			}
		}
		this.consolidationPrompt = consolidationPrompt;
		return this;
	}

	/** Token budget passed to the consolidation prompt. Must be positive. */
	public MemoryConfig setConsolidationMaxTokens(int consolidationMaxTokens) {
		if (consolidationMaxTokens <= 0) {
			throw new IllegalArgumentException(
					"consolidationMaxTokens must be positive, got " + consolidationMaxTokens);
		}
		this.consolidationMaxTokens = consolidationMaxTokens;
		return this;
	}

	/**
	 * Minimum gap between consolidation/maintenance runs. The first eligible call runs
	 * immediately; this duration is not an initial delay and is independent of
	 * {@link MemoryConfig#flushTrigger()}. Must not be null.
	 */
	public MemoryConfig setConsolidationMinGap(Duration consolidationMinGap) {
		if (consolidationMinGap == null) {
			throw new IllegalArgumentException("consolidationMinGap must not be null");
		}
		if (consolidationMinGap.isNegative()) {
			throw new IllegalArgumentException("consolidationMinGap must not be negative");
		}
		this.consolidationMinGap = consolidationMinGap;
		return this;
	}

	/** Days before a daily ledger is moved to {@code memory/archive/}. */
	public MemoryConfig dailyFileRetentionDays(int dailyFileRetentionDays) {
		if (dailyFileRetentionDays <= 0) {
			throw new IllegalArgumentException(
					"dailyFileRetentionDays must be positive, got " + dailyFileRetentionDays);
		}
		this.dailyFileRetentionDays = dailyFileRetentionDays;
		return this;
	}

	/** Days before a session JSONL log is pruned. */
	public MemoryConfig sessionRetentionDays(int sessionRetentionDays) {
		if (sessionRetentionDays <= 0) {
			throw new IllegalArgumentException(
					"sessionRetentionDays must be positive, got " + sessionRetentionDays);
		}
		this.sessionRetentionDays = sessionRetentionDays;
		return this;
	}

	/** Trigger policy for the per-call flush hook. Must not be null. */
	public MemoryConfig setFlushTrigger(FlushTrigger flushTrigger) {
		if (flushTrigger == null) {
			throw new IllegalArgumentException("flushTrigger must not be null");
		}
		this.flushTrigger = flushTrigger;
		return this;
	}



	private int countOccurrences(String haystack, String needle) {
		int count = 0;
		int idx = 0;
		while ((idx = haystack.indexOf(needle, idx)) != -1) {
			count++;
			idx += needle.length();
		}
		return count;
	}
    
}
