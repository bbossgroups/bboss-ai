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
package org.frameworkset.spi.ai.compaction;

 

import org.frameworkset.spi.ai.model.ModelInfo;

import java.util.HashSet;
import java.util.Set;

/**
 * Configuration for conversation compaction (summarization).
 *
 * <ul>
 *   <li><b>trigger</b> — when to run compaction (by token count or message count)</li>
 *   <li><b>keep</b> — how many recent messages to preserve verbatim after compaction</li>
 * </ul>
 *
 * <p>Defaults (dynamic mode):
 * <ul>
 *   <li>Trigger dynamically when estimated token count reaches {@code model.contextWindow - reserved(20k)}.
 *       Falls back to {@value #FALLBACK_TRIGGER_TOKENS} tokens when the model does not report its context
 *       window, or at 50 messages (whichever comes first)</li>
 *   <li>Keep tail: dynamically computed as {@code min(8k, max(2k, usable * 0.25))} tokens.
 *       Falls back to 20 messages when the model does not report its context window</li>
 *   <li>Prune: enabled by default — aggregates old tool result outputs and trims them when the
 *       prunable total exceeds 20k tokens (protects the most recent 40k tokens)</li>
 *   <li>Summarization is enabled; memory flush and offload are both enabled before summary</li>
 * </ul>
 *
 * <h2>Memory prompt landscape</h2>
 *
 * The harness has three LLM-driven memory operations, each with its own prompt; they live
 * in two complementary config classes:
 *
 * <ul>
 *   <li><b>Compaction summary</b> — distills the conversation prefix into one summary
 *       message before reasoning. Prompt: {@link #getSummaryPrompt()} on <em>this</em>
 *       class.</li>
 *   <li><b>Flush</b> — extracts long-term memories into today's daily ledger. Prompt:
 *       {@code MemoryConfig.flushPrompt()}.</li>
 *   <li><b>Consolidation</b> — periodically merges daily ledgers into {@code MEMORY.md}.
 *       Prompt: {@code MemoryConfig.consolidationPrompt()}.</li>
 * </ul>
 *
 * Configure the latter two via {@code HarnessAgent.builder().memory(MemoryConfig...)}.
 *
 */
public class CompactionConfig {
	
	/**
	 * Compaction policy: window size, according to message window size for trimming.
	 */
	public static final int COMPACTION_POLICY_WINDOWSIZE = 0;
	
	/**
	 * Compaction policy: summary, according to message window size for trimming and appending summary to the front.
	 */
	public static final int COMPACTION_POLICY_SUMMARY = 1;

    /**
     * Fallback trigger threshold (in tokens) when the model does not report its context window
     * and {@code triggerTokens} is set to dynamic mode (0).
     */
    public static final int FALLBACK_TRIGGER_TOKENS = 160_000;

    /** Default summary prompt with structured format. */
    public static final String DEFAULT_SUMMARY_PROMPT = "#[prompts/compaction/DEFAULT_SUMMARY_PROMPT.md,type=resource]";
//            """
//            <role>
//            Context Extraction Assistant
//            </role>
//
//            <primary_objective>
//            Your sole objective in this task is to extract the highest quality/most relevant \
//            context from the conversation history below.
//            </primary_objective>
//
//            <objective_information>
//            You're nearing the total number of input tokens you can accept, so you must extract \
//            the highest quality/most relevant pieces of information from your conversation history.
//            This context will then overwrite the conversation history presented below. Because of \
//            this, ensure the context you extract is only the most important information to \
//            continue working toward your overall goal.
//            </objective_information>
//
//            <instructions>
//            The conversation history below will be replaced with the context you extract in this \
//            step. You want to ensure that you don't repeat any actions you've already completed, \
//            so the context you extract from the conversation history should be focused on the \
//            most important information to your overall goal.
//
//            Structure your summary using these sections (populate each or write "None"):
//
//            ## SESSION INTENT
//            What is the user's primary goal or request?
//
//            ## SUMMARY
//            The most important context, decisions, reasoning, and rejected options.
//
//            ## ARTIFACTS
//            Files or resources created, modified, or accessed (with specific paths and changes).
//
//            ## NEXT STEPS
//            Specific tasks remaining to achieve the session intent.
//            </instructions>
//
//            Carefully read through the entire conversation history below and extract the most \
//            important context. Respond ONLY with the extracted context.
//
//            <messages>
//            {messages}
//            </messages>\
//            """;
	
	private int triggerMessages = 50;
	private int triggerTokens = 0;
	private int reserved = 20_000;
	private int keepMessages = 20;
	private int keepTokens = -1;
	private int keepTokensMin = 2_000;
	private int keepTokensMax = 8_000;
	private double keepTokensRatio = 0.25;
	private String summaryPrompt = DEFAULT_SUMMARY_PROMPT;
	private boolean flushBeforeCompact = true;
	private boolean offloadBeforeCompact = true;
	private TruncateArgsConfig truncateArgsConfig = null;
	private PruneConfig pruneConfig = new PruneConfig();
	private ModelInfo model = null;

     

    /** Message count above which compaction is triggered (0 = disabled). */
    public int getTriggerMessages() {
        return triggerMessages;
    }

    /**
     * Estimated token count above which compaction is triggered.
     * {@code 0} = dynamic mode (compute from model's context window minus {@link #getReserved()}).
     */
    public int getTriggerTokens() {
        return triggerTokens;
    }

    /**
     * Token buffer reserved for the compaction process itself (summary prompt + output).
     * Only used in dynamic mode ({@code triggerTokens == 0}).
     */
    public int getReserved() {
        return reserved;
    }

    /**
     * Number of recent <em>conversation</em> messages (non-SYSTEM) to preserve verbatim.
     * Used when {@link #getKeepTokens()} is 0 (static keep mode).
     */
    public int getKeepMessages() {
        return keepMessages;
    }

    /**
     * Token budget for the preserved tail.
     * <ul>
     *   <li>{@code > 0}: static budget — scan from end until this budget is exhausted</li>
     *   <li>{@code 0}: use {@link #getKeepMessages()} instead (message-count mode)</li>
     *   <li>{@code -1}: dynamic — compute as
     *       {@code min(keepTokensMax, max(keepTokensMin, usable * keepTokensRatio))}</li>
     * </ul>
     */
    public int getKeepTokens() {
        return keepTokens;
    }

    public int getKeepTokensMin() {
        return keepTokensMin;
    }

    public int getKeepTokensMax() {
        return keepTokensMax;
    }

    public double getKeepTokensRatio() {
        return keepTokensRatio;
    }

    /**
     * Prompt template used for the summarization LLM call. Must contain {@code {messages}}.
     */
    public String getSummaryPrompt() {
        return summaryPrompt;
    }

    /** Whether to flush long-term memories from the prefix before compaction. */
    public boolean isFlushBeforeCompact() {
        return flushBeforeCompact;
    }

    /** Whether to offload raw messages to the session JSONL before compaction. */
    public boolean isOffloadBeforeCompact() {
        return offloadBeforeCompact;
    }

    /**
     * Configuration for the lightweight pre-summarization argument truncation pass.
     * When {@code null}, argument truncation is disabled.
     */
    public TruncateArgsConfig getTruncateArgsConfig() {
        return truncateArgsConfig;
    }

    /**
     * Configuration for aggregate tool-result pruning. When {@code null}, pruning is disabled.
     */
    public PruneConfig getPruneConfig() {
        return pruneConfig;
    }

    /**
     * Optional model override for compaction (summarization). {@code null} means use
     * the agent's primary model.
     */
    public ModelInfo getModel() {
        return model;
    }

	
    /**
     * Creates a resolved copy with effective trigger and keep values computed from a model's
     * context window. Used by {@code CompactionMiddleware} to resolve dynamic defaults.
     */
    public CompactionConfig withEffective(int effectiveTriggerTokens, int effectiveKeepTokens) {
       
        this.triggerTokens = effectiveTriggerTokens;
       
        this.keepTokens = effectiveKeepTokens;
		return this;
        
    }

   

        /** Trigger compaction when conversation has at least this many messages (0 = disabled). */
        public CompactionConfig setTriggerMessages(int triggerMessages) {
            this.triggerMessages = triggerMessages;
            return this;
        }

        /**
         * Trigger compaction when estimated token count exceeds this value.
         * {@code 0} (default) = dynamic mode: compute from model's context window minus
         * {link #reserved(int)}.
         */
        public CompactionConfig setTriggerTokens(int triggerTokens) {
            this.triggerTokens = triggerTokens;
            return this;
        }

        /**
         * Token buffer reserved for the compaction process (default 20 000).
         * Only used in dynamic mode ({@code triggerTokens == 0}).
         */
        public CompactionConfig setReserved(int reserved) {
            this.reserved = reserved;
            return this;
        }

        /** Number of recent messages to keep verbatim after compaction. */
        public CompactionConfig setKeepMessages(int keepMessages) {
            this.keepMessages = keepMessages;
            return this;
        }

        /**
         * Token budget for the preserved tail.
         * {@code -1} (default) = dynamic mode; {@code 0} = use keepMessages; {@code > 0} = static.
         */
        public CompactionConfig setKeepTokens(int keepTokens) {
            this.keepTokens = keepTokens;
            return this;
        }

        public CompactionConfig setKeepTokensMin(int keepTokensMin) {
            this.keepTokensMin = keepTokensMin;
            return this;
        }

        public CompactionConfig setKeepTokensMax(int keepTokensMax) {
            this.keepTokensMax = keepTokensMax;
            return this;
        }

        public CompactionConfig setKeepTokensRatio(double keepTokensRatio) {
            this.keepTokensRatio = keepTokensRatio;
            return this;
        }

        /** Custom summary prompt. Must contain {@code {messages}} placeholder. */
        public CompactionConfig setSummaryPrompt(String summaryPrompt) {
            this.summaryPrompt = summaryPrompt;
            return this;
        }

        /** Whether to flush long-term memories before compaction (default true). */
        public CompactionConfig setFlushBeforeCompact(boolean flushBeforeCompact) {
            this.flushBeforeCompact = flushBeforeCompact;
            return this;
        }

        /** Whether to offload raw messages to session JSONL before compaction (default true). */
        public CompactionConfig setOffloadBeforeCompact(boolean offloadBeforeCompact) {
            this.offloadBeforeCompact = offloadBeforeCompact;
            return this;
        }

        /**
         * Enables lightweight pre-summarization argument truncation.
         * Pass {@code null} to disable.
         */
        public CompactionConfig setTruncateArgs(TruncateArgsConfig config) {
            this.truncateArgsConfig = config;
            return this;
        }

        /**
         * Configures aggregate tool-result pruning. Defaults to {link PruneConfig#defaults()}.
         * Pass {@code null} to disable.
         */
        public CompactionConfig setPruneConfig(PruneConfig config) {
            this.pruneConfig = config;
            return this;
        }

        /**
         * Sets a dedicated model for compaction (summarization), allowing a
         * lighter/cheaper model than the agent's primary reasoning model.
         */
        public CompactionConfig setModel(ModelInfo model) {
            this.model = model;
            return this;
        }
 
 
   

    // -------------------------------------------------------------------------
    //  TruncateArgsConfig
    // -------------------------------------------------------------------------

   

    
}
