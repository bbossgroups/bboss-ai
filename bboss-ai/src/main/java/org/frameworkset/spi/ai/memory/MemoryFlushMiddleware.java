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
import org.frameworkset.spi.ai.model.LinkedMessageMap;
import org.frameworkset.spi.ai.model.ModelInfo;
import org.frameworkset.util.concurrent.ThreadPoolFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * Middleware that triggers memory flush and message offload at the end of each agent call.
 *
 * <p>Runs in {link onAgent}'s {@code doOnComplete} so long-term memories are extracted and
 * persisted after every call, even when conversation compaction was not triggered during that
 * call. The flush is <em>fire-and-forget</em>: the agent stream completes immediately while the
 * extraction runs on a background scheduler. When {link CompactionManager} is active, it
 * handles flush/offload for the messages it summarizes; this middleware covers the remaining
 * tail of messages that were kept verbatim.
 *
 * <p>Flush is gated by a {@link MemoryConfig.FlushTrigger}:
 * <ul>
 *   <li>{@link MemoryConfig.FlushMode#ALWAYS} (default) — flush after every call.</li>
 *   <li>{@link MemoryConfig.FlushMode#NEVER} — never flush via this middleware. The CompactionMiddleware
 *       and overflow-recovery paths still run their own flush when they fire.</li>
 *   <li>{@link MemoryConfig.FlushMode#THROTTLED} — flush at most once per
 *       {@link MemoryConfig.FlushTrigger#minGap()}.</li>
 * </ul>
 *
 * <p>Session transcript append is <b>not</b> handled here — see {link TranscriptMiddleware},
 * which runs independently of memory flush so history stays complete even when flush is
 * disabled.
 *
 * <p>Concurrent flushes for the same isolation key are serialised: at most one flush runs per
 * key at a time, and pending flushes of the same conversation coalesce into a single queued
 * task, because a queued task reads the conversation state only when it executes and the
 * newest task therefore subsumes the ones it replaces. Flushes of distinct conversations
 * under the same key are queued separately.
 *
 * <p>The class is thread-safe: instances hold only configuration, and the per-key queues live
 * in a static map and are synchronised internally.
 *
 * <p>The throttle window is tracked per <em>isolation key</em>, which matches the memory data
 * isolation in use:
 * <ul>
 *   <li>{link IsolationScope#USER} (default) — one window per {@code userId}.</li>
 *   <li>{link IsolationScope#SESSION} — one window per {@code sessionId}.</li>
 *   <li>{link IsolationScope#AGENT} / {link IsolationScope#GLOBAL} — one shared window for
 *       the whole agent instance (prevents concurrent flush races on shared memory files).</li>
 * </ul>
 */
public class MemoryFlushMiddleware  {

    private static final Logger log = LoggerFactory.getLogger(MemoryFlushMiddleware.class);

    private   ModelInfo model;
    private   String flushPrompt;
    private   MemoryConfig.FlushTrigger flushTrigger;
//    private final IsolationScope isolationScope;
    private   PeriodicGate periodicGate;
 

    public MemoryFlushMiddleware(ModelInfo model) {
        this(
                
                model,
                MemoryFlushManager.DEFAULT_FLUSH_PROMPT,
                MemoryConfig.FlushTrigger.always(),
                 
                new LocalPeriodicGate());
    }
 

    public MemoryFlushMiddleware(
			ModelInfo model,
            String flushPrompt,
            MemoryConfig.FlushTrigger flushTrigger
             ) {
        this(
                model,
                flushPrompt,
                flushTrigger,
                new LocalPeriodicGate());
    }

    public MemoryFlushMiddleware(
			ModelInfo model,
            String flushPrompt,
            MemoryConfig.FlushTrigger flushTrigger,
            PeriodicGate periodicGate) {
        this.model = model;
        this.flushPrompt =
                flushPrompt != null ? flushPrompt : MemoryFlushManager.DEFAULT_FLUSH_PROMPT;
        this.flushTrigger =
                flushTrigger != null ? flushTrigger : MemoryConfig.FlushTrigger.always();
        this.periodicGate = periodicGate != null ? periodicGate : new LocalPeriodicGate();
    }

 

    public void doOnComplete(
            AIAgent agent,
            ChatObject chatObject           ) {
		// in-flight槽位由调用方(AIAgentUtil.doOnComplete)在提交线程统一登记，
		// 这里只负责调度任务，避免"已提交但尚未开始执行"的空窗期导致静默等待提前返回。
		scheduleFlush(agent, chatObject);
    }

	class FlushRunable{
		ChatObject chatObject;
		AIAgent agent;
		String key;
		public FlushRunable(String key,AIAgent agent,ChatObject chatObject){
			this.agent = agent;
			this.chatObject = chatObject;
			this.key = key;	
		}
		public void run(boolean asyn){
			runFlush(key, agent, chatObject, asyn);
		}
	}
    private void scheduleFlush( AIAgent agent,
								ChatObject chatObject) {
        try {
			MemoryBackgroundTasks.begin();
			String key = compositeTimerKey(agent);
			// Coalescing key of the task: the conversation whose state the task reads when it
			// executes. The agent reference distinguishes agent instances serving the same user
			// and session ids, whose flushes must not displace each other.
			ConversationKey conversationKey =
					new ConversationKey(
							agent, blankToEmpty(agent.getUserId()), blankToEmpty(agent.getSessionId()));
			FlushRunable task = new FlushRunable	(key, agent, chatObject);
			FlushRunable[] starter = new FlushRunable[1];
			FlushRunable[] replaced = new FlushRunable[1];
			FLUSH_QUEUES.compute(
					key,
					(k, queue) -> {
						FlushQueue q = queue == null ? new FlushQueue() : queue;
						if (q.running) {
							// A queued task reads the conversation state only when it executes, and
							// calls of one conversation are serialised on the agent's
							// (userId, sessionId) serialization key, so the newest task's state
							// includes everything the replaced tasks would have read.
							replaced[0] = q.pending.put(conversationKey, task);
						} else {
							q.running = true;
							starter[0] = task;
						}
						return q;
					});
			if(replaced[0] != null){
				MemoryBackgroundTasks.end();
			}
			if (starter[0] != null) {
				starter[0].run(true);
			}
		}
		catch (Exception e) {
			MemoryBackgroundTasks.end();
			log.warn("Memory flush failed: {}", e.getMessage(),e);
		}
		catch (Throwable e) {
			MemoryBackgroundTasks.end();
			log.warn("Memory flush failed: {}", e.getMessage(),e);
		}
	}

    private void runFlush(String key, AIAgent agent, ChatObject chatObject,boolean asyn) {
		if(asyn) {
			ExecutorService MEMORY_FLUSH_TASK_EXECUTOR = MemoryUtil.getMemoryFlushTaskExecutor();
			int maxRetry = 3;
			int retry = 0;
			do {
				try {
					MEMORY_FLUSH_TASK_EXECUTOR.submit(() -> {
						try {
							doFlush(agent, chatObject);
						} catch (Exception e) {
							log.warn("Memory flush failed: {}", e.getMessage(), e);
						} finally {
							MemoryBackgroundTasks.end();
							drainFlushQueue(key);
						}
					});
					break;
				} catch (Exception e) {
					retry++;
					log.warn("Memory flush failed: {}", e.getMessage(), e);
					if(retry >= maxRetry){
						try {
							doFlush(agent, chatObject);
						} catch (Exception ex) {
							log.warn("Memory flush failed: {}", ex.getMessage(), ex);
						} finally {
							MemoryBackgroundTasks.end();
							drainFlushQueue(key);
						}
						
						break;
					}
					 
					
					 
				}
			} while (true);
		}
		else{
			try {
				doFlush(agent, chatObject);
			} catch (Exception e) {
				log.warn("Memory flush failed: {}", e.getMessage(), e);
			} finally {
				MemoryBackgroundTasks.end();
//				drainFlushQueue(key,false);
			}
		}
	}

    private void drainFlushQueue(String key) {
			do{
				FlushRunable[] next = new FlushRunable[1];
				FLUSH_QUEUES.compute(
						key,
						(k, queue) -> {
							if (queue == null) {
								return null;
							}
							Iterator<FlushRunable> it = queue.pending.values().iterator();
							if (it.hasNext()) {
								next[0] = it.next();
								it.remove();
							}
							if (next[0] == null) {
								return null; // idle: evict so the map holds only active keys
							}
							return queue; // the dequeued task continues as the running flush
						});
				if (next[0] != null) {
					next[0].run(false);
				}
				else{
					break;
				}
			}while(true);
		 
    }

    /**
     * Per-isolation-key pending flush tasks, keeping at most one flush in flight per memory
     * namespace. Fields are only mutated inside {@link ConcurrentHashMap#compute} (which holds
     * the per-key bin lock), so they need no additional synchronisation; entries are removed
     * as soon as a key goes idle.
     */
    private static final class FlushQueue {
        final LinkedHashMap<ConversationKey, FlushRunable> pending = new LinkedHashMap<>();
        boolean running;
    }

    private static final ConcurrentHashMap<String, FlushQueue> FLUSH_QUEUES =
            new ConcurrentHashMap<>();

    private void doFlush(AIAgent agent, ChatObject chatObject) {
//        AgentState state = RuntimeContext.resolveAgentState(rc, agent);
//        if (state == null) {
//            return Mono.empty();
//        }
        // Read at execution time rather than scheduling time: a task that waited in the queue
        // sees everything appended in the meantime, so coalescing queued tasks never loses
        // messages.
		List<LinkedMessageMap<String, Object>> messages = agent.getAgentSessionStore().getSessionMemory();
		if(messages == null || messages.isEmpty()){
			return ;
		}
//        List<Msg> messages = new ArrayList<>(state.getContext());
		messages = new ArrayList<>(messages);
//        if (messages.isEmpty()) {
//            return Mono.empty();
//        }
        if (!shouldFlushNow(agent)) {
            log.debug("Memory flush skipped (trigger={})", flushTrigger);
            return ;
        }

		 
        ModelInfo model = this.model;
        if(model == null){
            model = chatObject.getChatContext().getModelInfo();
        }
        MemoryFlushManager flushManager =
                new MemoryFlushManager(model, flushPrompt);
        flushManager
                .flushMemories(agent, messages);
        log.debug("Memory flush completed");
//					.doOnSuccess(v -> log.debug("Memory flush completed"))
//					.onErrorResume(
//							e -> {
//								log.warn("Memory flush failed: {}", e.getMessage());
//								return Mono.empty();
//							});
		 
    }

    /**
     * Returns whether this call should trigger a flush, applying the configured trigger policy.
     * For {@link MemoryConfig.FlushMode#THROTTLED}, uses an {link AtomicReference#compareAndSet}
     * race to ensure at most one caller within {@code minGap} wins the slot.
     *
     * <p>The throttle window is keyed by the isolation dimension that matches the memory data
     * namespace (see {link #timerKeyFor(RuntimeContext)}).
     *
     * <p>Package-private for unit testing of the trigger gate without standing up a full
     * {@code Agent}.
     */
    boolean shouldFlushNow(AIAgent agent) {
        switch (flushTrigger.mode()) {
            case ALWAYS:
                return true;
            case NEVER:
                return false;
            case THROTTLED:
                return periodicGate.tryClaim(compositeTimerKey(agent), flushTrigger.minGap());
            default:
                return true;
        }
    }

    /**
     * Builds a composite key from {link IsolationScope} name and the per-call identity returned
     * by {link #timerKeyFor(RuntimeContext)}. The operation prefix keeps flush independent
     * of maintenance when both use the same {@link PeriodicGate}. The scope prefix keeps
     * different isolation dimensions from sharing a slot.
     */
    private String compositeTimerKey(AIAgent agent) {
        return "memory-flush:user:" + timerKeyFor(agent);
    }

    /** The conversation a queued flush reads when it executes: agent instance, user, session. */
    public static class ConversationKey {
		private AIAgent agent;
		private String userId;
		private String sessionId;		
		public ConversationKey() {
		}
		public ConversationKey(AIAgent agent, String userId, String sessionId) {
			this.agent = agent;
			this.userId = userId;
			this.sessionId = sessionId;
		}
		public String toString() {
			return "ConversationKey [agent=" + agent + ", userId=" + userId + ", sessionId=" + sessionId + "]";
		}

		/**
		 * 合并/去重的关键：agent 按实例身份(引用)比较，userId/sessionId 按值比较。
		 * 若缺少 equals/hashCode，每次都会生成新的键，FLUSH_QUEUES 的 pending 无法覆盖旧任务，
		 * 导致同一会话的多次 flush 无法合并、任务持续堆积。
		 */
		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof ConversationKey)) {
				return false;
			}
			ConversationKey that = (ConversationKey) o;
			return agent == that.agent
					&& Objects.equals(userId, that.userId)
					&& Objects.equals(sessionId, that.sessionId);
		}

		@Override
		public int hashCode() {
			int result = System.identityHashCode(agent);
			result = 31 * result + (userId != null ? userId.hashCode() : 0);
			result = 31 * result + (sessionId != null ? sessionId.hashCode() : 0);
			return result;	
			// return Objects.hash(agent, userId, sessionId);
		}
		
		public void setAgent(AIAgent agent) {
			this.agent = agent;
		}
		
		public AIAgent getAgent() {
			return agent;
		}
		public void setUserId(String userId) {
			this.userId = userId;
		}
		public String getUserId() {
			return userId;
		}
		public void setSessionId(String sessionId) {
			this.sessionId = sessionId;
		}
		public String getSessionId() {
			return sessionId;
		}	
	}

    /**
     * Normalises {@code null} and blank identifiers to {@code ""} so absent ids share one slot.
     * Package-private: shared with {link MemoryMaintenanceMiddleware#timerKeyFor}.
     */
    static String blankToEmpty(String s) {
        return (s != null && !s.isEmpty()) ? s : "";
    }

    /**
     * Derives the per-call identity portion of the composite timer key from the configured
     * {link IsolationScope} and the {link RuntimeContext}, mirroring the memory data
     * namespace:
     * <ul>
     *   <li>{link IsolationScope#USER} — {@code userId} (empty string for anonymous)</li>
     *   <li>{link IsolationScope#SESSION} — {@code sessionId} (empty string when absent)</li>
     *   <li>{link IsolationScope#AGENT} / {link IsolationScope#GLOBAL} — constant {@code ""}
     *       so all callers share one throttle slot, serialising flushes on shared memory files</li>
     * </ul>
     */
    String timerKeyFor(AIAgent agent) {
		return blankToEmpty(agent.getUserId())	;
//        return switch (isolationScope) {
//            case USER -> blankToEmpty(rc != null ? rc.getUserId() : null);
//            case SESSION -> blankToEmpty(rc != null ? rc.getSessionId() : null);
//            case AGENT, GLOBAL -> "";
//        };
    }
}
