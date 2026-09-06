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

 
 
import org.frameworkset.spi.ai.AIAgent;
import org.frameworkset.spi.ai.context.AgentRuntimeContext;
import org.frameworkset.spi.ai.context.ChatContext;
import org.frameworkset.spi.ai.interceptor.AgentInterceptor;
import org.frameworkset.spi.ai.model.LinkedMessageMap;
import org.frameworkset.spi.ai.model.ModelInfo;
import org.frameworkset.spi.ai.util.MessageBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Middleware that performs conversation compaction before each LLM reasoning call.
 *
 * <p>Fires on {@link #onReasoning}. When the compaction threshold is exceeded:
 * <ol>
 *   <li>Long-term memories are flushed from the prefix via {link MemoryFlushManager}.</li>
 *   <li>The full conversation is offloaded to the session JSONL.</li>
 *   <li>The prefix is distilled into a structured summary via one LLM call.</li>
 *   <li>The agent's working {link AgentState#contextMutable() context} is replaced with
 *       {@code [summaryMsg] + preservedTail}.</li>
 *   <li>The downstream {link ReasoningInput} is rebuilt with
 *       {@code [systemMsg] + [summaryMsg] + preservedTail}.</li>
 * </ol>
 *
 * <p>When {link CompactionConfig#getTriggerTokens()} is 0 (dynamic mode, the default), the
 * effective trigger threshold is computed as {@code model.getContextWindowSize() - reserved}.
 * If the model does not report its context window, falls back to
 * {link CompactionConfig#FALLBACK_TRIGGER_TOKENS}.
 */
public class CompactionInterceptor implements AgentInterceptor {

    private static final Logger log = LoggerFactory.getLogger(CompactionInterceptor.class);
	private ModelInfo modelInfo;
    private   CompactionConfig config;

    public CompactionInterceptor(
            ModelInfo modelInfo, CompactionConfig config) {
        this.modelInfo = modelInfo;
        this.config = config;
    }

    @Override
    public void onReasoning(
            AIAgent agent,
			ChatContext chatContext
          ) {
         
        final AgentRuntimeContext rc = chatContext != null ? chatContext.getAgentRuntimeContext() : new AgentRuntimeContext();
		
		List<LinkedMessageMap<String,Object>> messages = chatContext.getMessages();
		LinkedMessageMap<String,Object> systemMsg = null;
		List<LinkedMessageMap<String,Object>> conversation;
		if (messages != null
				&& !messages.isEmpty()
				&& messages.get(0).get("role") == MessageBuilder.ROLE_SYSTEM) {
			systemMsg = messages.get(0);
			conversation = new ArrayList<>(messages.subList(1, messages.size()));
		} else {
			conversation = messages != null ? new ArrayList<>(messages) : Collections.emptyList();
		}

		String agentId = agent.getAgentId();
		String sessionId = agent.getSessionId();
		String userId = agent.getUserId();

		CompactionConfig effectiveConfig = resolveEffectiveConfig();
		
		MemoryManager flushManager =
				new MemoryManager(modelInfo);
		ConversationCompactor compactor =
				new ConversationCompactor(modelInfo, flushManager);
		final LinkedMessageMap<String,Object> sys = systemMsg;

		// Only compaction may degrade; downstream reasoning errors must propagate.
		List<LinkedMessageMap<String, Object>> compacted = compactor
				.compactIfNeeded(chatContext,agent, conversation, effectiveConfig, agentId, sessionId);
		List<LinkedMessageMap<String, Object>> newMessages = new ArrayList<>();
		if (sys != null) {
			newMessages.add(sys);
		}
		newMessages.addAll(compacted);
		chatContext.setMessages(newMessages);
//		compactor
//				.compactIfNeeded(chatContext,agent, conversation, effectiveConfig, agentId, sessionId)
//				.onErrorResume(
//						error -> {
//							if (containsInterruptedException(error)) {
//								return Mono.error(error);
//							}
//							log.warn(
//									"Compaction failed, continuing without compaction:"
//											+ " {}",
//									error.getMessage());
//							return Mono.just(Optional.empty());
//						})
//				.flatMapMany(
//						optResult -> {
//							if (optResult.isEmpty()) {
//								return next.apply(input);
//							}
//							List<Msg> compacted = optResult.get();
//							applyToContext(
//									RuntimeContext.resolveAgentState(rc, reActAgent),
//									compacted);
//							log.debug(
//									"Compacted to {} messages before reasoning",
//									compacted.size());
//							List<Msg> newMessages = new ArrayList<>();
//							if (sys != null) {
//								newMessages.add(sys);
//							}
//							newMessages.addAll(compacted);
//							return next.apply(
//									new ReasoningInput(
//											newMessages,
//											input.tools(),
//											input.options()));
//						});
                 
    }

    private static boolean containsInterruptedException(Throwable error) {
        IdentityHashMap<Throwable, Boolean> visited = new IdentityHashMap<>();
        Throwable current = error;
        while (current != null && visited.put(current, Boolean.TRUE) == null) {
            if (current instanceof InterruptedException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    /**
     * Resolves dynamic defaults in the config using the model's context window.
     */
    private CompactionConfig resolveEffectiveConfig() {
        int configTrigger = config.getTriggerTokens();
        int configKeep = config.getKeepTokens();

        boolean needsDynamic = (configTrigger == 0) || (configKeep == -1);
        if (!needsDynamic) {
            return config;
        }

        int contextWindow = modelInfo.getContextWindowSize();

        int effectiveTrigger;
        if (configTrigger == 0) {
            if (contextWindow > 0) {
                effectiveTrigger = contextWindow - config.getReserved();
                if (effectiveTrigger <= 0) {
                    // reserved exceeds the model's context window; a negative or zero trigger
                    // would fire compaction on every call. Clamp to half the context window so
                    // compaction still activates at a sensible point without thrashing.
                    effectiveTrigger = Math.max(1, contextWindow / 2);
                    log.warn(
                            "Dynamic compaction trigger clamped: contextWindow={} <= reserved={}"
                                    + "; using proportional trigger={}. Consider reducing"
                                    + " reserved() for this model.",
                            contextWindow,
                            config.getReserved(),
                            effectiveTrigger);
                } else {
                    log.debug(
                            "Dynamic compaction trigger: contextWindow={} - reserved={} = {}",
                            contextWindow,
                            config.getReserved(),
                            effectiveTrigger);
                }
            } else {
                effectiveTrigger = CompactionConfig.FALLBACK_TRIGGER_TOKENS;
                log.debug(
                        "Model does not report context window, using fallback trigger: {}",
                        effectiveTrigger);
            }
        } else {
            effectiveTrigger = configTrigger;
        }

        int effectiveKeep;
        if (configKeep == -1) {
            if (contextWindow > 0) {
                int usable = contextWindow - config.getReserved();
                effectiveKeep =
                        Math.min(
                                config.getKeepTokensMax(),
                                Math.max(
                                        config.getKeepTokensMin(),
                                        (int) (usable * config.getKeepTokensRatio())));
                log.debug("Dynamic keep tokens: {}", effectiveKeep);
            } else {
                effectiveKeep = 0;
            }
        } else {
            effectiveKeep = configKeep;
        }

        return config.withEffective(effectiveTrigger, effectiveKeep);
    }

//    private static void applyToContext(AgentState state, List<Msg> compacted) {
//        if (state == null) {
//            log.warn("Cannot apply compacted messages: AgentState is null");
//            return;
//        }
//        try {
//            List<Msg> ctx = state.contextMutable();
//            ctx.clear();
//            ctx.addAll(compacted);
//            log.debug("Applied compacted messages to state ({} messages)", compacted.size());
//        } catch (Exception e) {
//            log.warn("Failed to apply compacted messages to state: {}", e.getMessage());
//        }
//    }
}
