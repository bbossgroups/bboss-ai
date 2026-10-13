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


import org.frameworkset.spi.ai.AIAgent;
import org.frameworkset.spi.ai.memory.coordination.LocalPeriodicGate;
import org.frameworkset.spi.ai.memory.coordination.PeriodicGate;
import org.frameworkset.spi.ai.model.ChatObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

/**
 * Middleware that performs periodic memory maintenance after each agent call.
 *
 * <p>Fires on the agent invocation completion (via {@code onAgent doOnComplete}, after
 * {@link MemoryFlushMiddleware}) and is throttled by a configurable minimum gap so it
 * does not run on every single call. The maintenance is <em>fire-and-forget</em>: the agent
 * stream completes immediately while the maintenance runs on a background scheduler.
 *
 * <p>Maintenance steps executed in order:
 * <ol>
 *   <li>Expire daily memory files older than {@code dailyFileRetentionDays} by moving
 *       them to {@code memory/archive/}.</li>
 *   <li>Run LLM-based consolidation ({@link MemoryConsolidator#consolidate}) if a
 *       consolidator is configured.</li>
 *   <li>Prune session log files older than {@code sessionRetentionDays}.</li>
 * </ol>
 *
 * <p>The throttle window is tracked per <em>isolation key</em>, which matches the memory data
 * isolation in use:
 * <ul>
 *   <li>{link IsolationScope#USER} (default) — one window per {@code userId}.</li>
 *   <li>{link IsolationScope#SESSION} — one window per {@code sessionId}.</li>
 *   <li>{link IsolationScope#AGENT} / {link IsolationScope#GLOBAL} — one shared window for
 *       the whole agent instance (prevents concurrent maintenance races on shared memory files).</li>
 * </ul>
 */
public class MemoryMaintenance  {

    private static final Logger log = LoggerFactory.getLogger(MemoryMaintenance.class);

    /** Default minimum gap between two maintenance runs. */
    public static final Duration DEFAULT_MIN_GAP = Duration.ofMinutes(30);

    private  MemoryConsolidator consolidator;
    private  Duration minGap;
    private  PeriodicGate periodicGate;

 

    public MemoryMaintenance(
            MemoryConsolidator consolidator,
            Duration minGap) {
        this(
                consolidator,
                minGap,
                new LocalPeriodicGate());
    }

    public MemoryMaintenance(
            MemoryConsolidator consolidator,
            Duration minGap,
            PeriodicGate periodicGate) {
        this.consolidator = consolidator;
        this.minGap = minGap != null ? minGap : DEFAULT_MIN_GAP;
        this.periodicGate = periodicGate != null ? periodicGate : new LocalPeriodicGate();
    }

    public MemoryMaintenance(
             MemoryConsolidator consolidator) {
        this(  consolidator,   DEFAULT_MIN_GAP);
    }


    public void doOnComplete(
            AIAgent agent,
            ChatObject chatObject
            ) {
       
        // The maintenance body — including the gate claim, which is remote I/O under a
        // store-backed gate — runs on the background scheduler. Only the in-flight counter is
        // updated synchronously, so a quiescence check can never observe an empty in-flight
        // set before the task is counted.
       try {
		   MemoryBackgroundTasks.begin();
		   doMaintenance(agent, chatObject);
		  
	   }
	   catch (Exception e){
		   log.warn(
				   "Memory maintenance failed: {}",
				   e.getMessage());
	   }
	   finally {
		   MemoryBackgroundTasks.end();
	   }
//				.subscribeOn(Schedulers.boundedElastic())
//				.doFinally(signal -> MemoryBackgroundTasks.end())
//				.subscribe(
//						null,
//						e ->
//								log.warn(
//										"Memory maintenance failed: {}",
//										e.getMessage()));
                    
    }

    private void doMaintenance( AIAgent agent,
									  ChatObject chatObject) {
//        if (!periodicGate.tryClaim(compositeTimerKey(agent,chatObject), minGap)) {
//            // Throttled out; the in-flight slot acquired at dispatch is released when this
//            // Mono completes.
//            return ;
//        }
        runMaintenance(agent,chatObject);
    }

    /**
     * Builds a composite key from {link IsolationScope} name and the per-call identity returned
     * by {@link #timerKeyFor(AIAgent, ChatObject)}. The operation prefix keeps maintenance independent
     * of flush when both use the same {@link PeriodicGate}. The scope prefix keeps different
     * isolation dimensions from sharing a slot.
     */
    private String compositeTimerKey( AIAgent agent,
									  ChatObject chatObject) {
        return "memory-maintenance:user:" + timerKeyFor(agent,chatObject	);
    }
	static String blankToEmpty(String s) {
		return (s != null && !s.isEmpty()) ? s : "";
	}

    /**
     * Derives the per-call identity portion of the composite timer key from the configured
     * {link IsolationScope} and the {link RuntimeContext}, mirroring the memory data
     * namespace. See {link MemoryFlushMiddleware#timerKeyFor(AIAgent, ChatObject	)} for the
     * identical logic.
     */
    String timerKeyFor( AIAgent agent,
						ChatObject chatObject) {
		return blankToEmpty(agent.getUserId() );
//        return switch (isolationScope) {
//            case USER -> MemoryFlushMiddleware.blankToEmpty(rc != null ? rc.getUserId() : null);
//            case SESSION ->
//                    MemoryFlushMiddleware.blankToEmpty(rc != null ? rc.getSessionId() : null);
//            case AGENT, GLOBAL -> "";
//        };
    }

    private void runMaintenance(AIAgent agent,
								ChatObject chatObject) {
        log.debug("Running memory maintenance...");
        consolidateMemory(  agent,
				  chatObject);
        log.debug("Memory maintenance completed");
    }
 
    private void consolidateMemory(AIAgent agent,
								   ChatObject chatObject) {
        if (consolidator == null) {
            return;
        }
        try {
            consolidator.consolidate(  agent,
					  chatObject) ;
        } catch (Exception e) {
            log.warn("Memory consolidation failed: {}", e.getMessage(),e);
        }
    }
 
}
