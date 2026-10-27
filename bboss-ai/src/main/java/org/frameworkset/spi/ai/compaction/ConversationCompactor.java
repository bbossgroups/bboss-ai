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

import com.frameworkset.util.JsonUtil;
import org.frameworkset.spi.ai.AIAgent;
import org.frameworkset.spi.ai.context.ChatContext;
import org.frameworkset.spi.ai.memory.MemoryFlushManager;
import org.frameworkset.spi.ai.model.LinkedMessageMap;
import org.frameworkset.spi.ai.model.ModelInfo;
import org.frameworkset.spi.ai.util.MessageBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <h2>Algorithm</h2>
 * <ol>
 *   <li><b>Check trigger</b> — token count or message count exceeds threshold</li>
 *   <li><b>Determine cutoff</b> — find the earliest index that keeps the tail within the
 *       "keep" budget; never split an ASSISTANT tool-call from its TOOL result(s)</li>
 *   <li><b>Memory flush</b> (optional) — extract long-term memories from the prefix via
 *       {link MemoryFlushManager#flushMemories}</li>
 *   <li><b>Message offload</b> (optional) — persist the full conversation to the session
 *       JSONL via {link MemoryFlushManager#offloadMessages}</li>
 *   <li><b>Summarize</b> — one LLM call to distill the prefix into a structured summary</li>
 *   <li><b>Rebuild</b> — return {@code [summaryUserMsg] + preservedTail}</li>
 * </ol>
 *
 * <p>The caller is responsible for updating both the agent's working memory and the LLM-facing
 * message list (see {link CompactionMiddleware}).
 */
public class ConversationCompactor {

    private static final Logger log = LoggerFactory.getLogger(ConversationCompactor.class);

    /** Marker stored in message name to identify injected summary messages. */
    public static final String SUMMARY_MSG_NAME = "__compaction_summary__";
    private final ModelInfo model;
    private final MemoryFlushManager flushManager;

    public ConversationCompactor(ModelInfo model, MemoryFlushManager flushManager) {
        this.model = model;
        this.flushManager = flushManager;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Runs compaction on the supplied conversation messages if a trigger condition is met.
     *
     * <p>Only <em>conversation</em> messages (non-SYSTEM) should be passed. The caller must
     * separate system messages before invoking this method and re-prepend them after.
     *
     * @param conversationMessages non-SYSTEM messages (USER / ASSISTANT / TOOL)
     * @param config               compaction configuration
     * @return {@code Optional.empty()} when no compaction was needed; otherwise the replacement
     *         message list consisting of {@code [summaryUserMsg] + preservedTail}
     */
    public List<LinkedMessageMap<String, Object>> compactIfNeeded(ChatContext chatContext,
																  AIAgent agent,
																  List<LinkedMessageMap<String, Object>> conversationMessages,
																  CompactionConfig config
																 ) {

        if (conversationMessages == null || conversationMessages.isEmpty()) {
            return Collections.emptyList();
        }

        // Step 1a: Lightweight arg truncation (non-LLM).
        // Step 1b: Aggregate tool-result pruning (non-LLM).
        List<LinkedMessageMap<String, Object>> messages = null;
		try {
			messages = pruneToolResults(
					truncateArgs(conversationMessages, config.getTruncateArgsConfig()),
					config.getPruneConfig());
		}
		catch (Exception e) {
			log.warn("Lightweight trim failed, using original messages:", e);
			messages = conversationMessages;
		}
        boolean trimmed = (messages != conversationMessages);  
        int totalTokens = TokenCounterUtil.calculateToken(messages);
        if (!shouldCompact(messages, totalTokens, config)) {
                  
            return trimmed ? messages : Collections.emptyList();
                    
        }

        int cutoff = determineCutoffIndex(messages, totalTokens, config);
        if (cutoff <= 0) {
			if (log.isDebugEnabled()) {
				log.debug("Compaction triggered but safe cutoff is 0 — skipping");
			}
            return trimmed ? messages : Collections.emptyList();   // 同上
        }

        // Keep prior summaries in the summarization input so each compaction builds on the
        // previous one, but exclude them from memory flushing to avoid duplicate extraction.
        List<LinkedMessageMap<String, Object>> summaryInput = new ArrayList<>(messages.subList(0, cutoff));
        
        List<LinkedMessageMap<String, Object>> tail = new ArrayList<>(messages.subList(cutoff, messages.size()));

		if(log.isInfoEnabled()) {
			log.info(
					"Compaction triggered: total={} msgs / {} tokens, cutoff={}, keeping={} msgs",
					messages.size(),
					totalTokens,
					cutoff,
					tail.size());
		}

        // Step 2: Flush long-term memories only from newly compacted raw messages (best-effort).
		if(config.isFlushBeforeCompact()) {
			List<LinkedMessageMap<String, Object>> flushInput = filterSummaryMessages(summaryInput);
			flushManager
				.flushMemoriesAsync(  agent, flushInput);
		}
//        Mono<Void> flushStep =
//                config.isFlushBeforeCompact()
//                        ? flushManager
//                                .flushMemories(chatContext, agent,flushInput)
//                                .doOnSuccess(v -> log.debug("Memory flush before compaction done"))
//                                .onErrorResume(
//                                        e -> {
//                                            if (containsInterruptedException(e)) {
//                                                return Mono.error(e);
//                                            }
//                                            log.warn(
//                                                    "Memory flush before compaction failed: {}",
//                                                    e.getMessage());
//                                            return Mono.empty();
//                                        })
//                        : Mono.empty();

        // Step 3: Offload raw messages to JSONL and capture the file path.
        // If offload fails, we continue with null — the summary message falls back to the
        // simple format without a file reference.
//        Mono<String> offloadStep;
//        if (config.isOffloadBeforeCompact()) {
//            offloadStep =
//                    Mono.fromCallable(
//                                    () -> {
//                                        flushManager.offloadMessages(
//                                                chatContext, messages, agentId, sessionId);
//                                        return flushManager.resolveOffloadPath(
//                                                chatContext, agentId, sessionId);
//                                    })
//                            .doOnSuccess(
//                                    path ->
//                                            log.debug(
//                                                    "Message offload before compaction done,"
//                                                            + " path={}",
//                                                    path))
//                            .onErrorResume(
//                                    e -> {
//                                        if (containsInterruptedException(e)) {
//                                            return Mono.error(e);
//                                        }
//                                        log.warn(
//                                                "Message offload before compaction failed: {}",
//                                                e.getMessage());
//                                        return Mono.just("");
//                                    });
//        } else {
//            offloadStep = Mono.just("");
//        }

        // Step 4: LLM summarization of prior summaries plus the newly compacted prefix.
		String summarize = SummeryUtils.summarizePrefix(summaryInput, config,chatContext);
		
		List<LinkedMessageMap<String, Object>> compacted = new ArrayList<>();
		// Native logging already preserves all pre-compaction messages. Reference the logical
		// history API; never write another full transcript or expose private journal paths.

		LinkedMessageMap<String, Object> summaryMsg = SummeryUtils.buildSummaryMessage(chatContext,agent,summarize,summaryInput, null,tail.get(0));
		agent.saveSummeryMessage(summaryMsg);//持久化摘要消息
		compacted.add(summaryMsg);
		compacted.addAll(tail);
		if(log.isInfoEnabled()) {
			log.info(
					"Compaction complete: {} msgs → 1"
							+ " summary + {} tail = {}"
							+ " total",
					messages.size(),
					tail.size(),
					compacted.size());
		}
		return compacted;
//		
//        return flushStep
//                .then(offloadStep)
//                .flatMap(
//                        offloadPath ->
//                                summarizePrefix(summaryInput, config)
//                                        .map(
//                                                summary -> {
//                                                    String filePath =
//                                                            offloadPath.isBlank()
//                                                                    ? null
//                                                                    : offloadPath;
//                                                    Msg summaryMsg =
//                                                            buildSummaryMessage(summary, filePath);
//                                                    List<Msg> compacted = new ArrayList<>();
//                                                    compacted.add(summaryMsg);
//                                                    compacted.addAll(tail);
//                                                    log.info(
//                                                            "Compaction complete: {} msgs → 1"
//                                                                    + " summary + {} tail = {}"
//                                                                    + " total",
//                                                            messages.size(),
//                                                            tail.size(),
//                                                            compacted.size());
//                                                    return Optional.of(compacted);
//                                                }));
    }

    // -------------------------------------------------------------------------
    // Trigger logic
    // -------------------------------------------------------------------------

    private static boolean shouldCompact(
            List<LinkedMessageMap<String, Object>> messages, int totalTokens, CompactionConfig config) {
		if (config.getTriggerMessages() > 0 && messages.size() >= config.getTriggerMessages()) {
			if(log.isDebugEnabled()) {
				log.debug(
						"Compaction trigger: message count {} >= {}",
						messages.size(),
						config.getTriggerMessages());
			}
			return true;
		}
		if (config.getTriggerTokens() > 0 && totalTokens >= config.getTriggerTokens()) {
			if(log.isDebugEnabled()) {
				log.debug(
						"Compaction trigger: token count {} >= {}",
						totalTokens,
						config.getTriggerTokens());
			}
			return true;
		}
		return false;
    }

    // -------------------------------------------------------------------------
    // Cutoff / partition logic
    // -------------------------------------------------------------------------

    /**
     * Determines the cutoff index separating the prefix-to-summarize from the tail-to-keep.
     *
     * <p>The cutoff is adjusted so that ASSISTANT/TOOL pairs are never split.
     */
    public static int determineCutoffIndex(
            List<LinkedMessageMap<String, Object>> messages, int totalTokens, CompactionConfig config) {
        int rawCutoff;
        if (config.getKeepTokens() > 0 && config.getCompactionPolicy() == CompactionConfig.COMPACTION_POLICY_TOKENS) {
            rawCutoff = findTokenBasedCutoff(messages, totalTokens, config.getKeepTokens());
        } else {
            rawCutoff = findMessageBasedCutoff(messages, config.getKeepMessages());
        }
        return findSafeCutoffPoint(messages, rawCutoff);
    }

    /** Returns the earliest index such that {@code messages[index:]} fits within the token budget. */
    private static int findTokenBasedCutoff(List<LinkedMessageMap<String, Object>> messages, int totalTokens, int keepTokens) {
        if (totalTokens <= keepTokens) {
            return 0;
        }
        // Binary search for the earliest index where the suffix token count <= keepTokens
        int left = 0;
        int right = messages.size();
        int candidate = messages.size();
        int maxIter = Integer.SIZE - Integer.numberOfLeadingZeros(messages.size()) + 1;
        for (int i = 0; i < maxIter && left < right; i++) {
            int mid = (left + right) / 2;
            if (TokenCounterUtil.calculateToken(messages.subList(mid, messages.size()))
                    <= keepTokens) {
                candidate = mid;
                right = mid;
            } else {
                left = mid + 1;
            }
        }
        // Clamp so at least 1 message is always kept
        return Math.min(candidate, messages.size() - 1);
    }

    /** Returns the cutoff that keeps the last {@code keepMessages} messages verbatim. */
    private static int findMessageBasedCutoff(List<LinkedMessageMap<String, Object>> messages, int keepMessages) {
        if (messages.size() <= keepMessages) {
            return 0;
        }
        return messages.size() - keepMessages;
    }

    /**
     * Adjusts the cutoff to avoid splitting ASSISTANT tool-call/TOOL-result pairs.
     *
     * <p>If the message at {@code cutoffIndex} has role TOOL, we search backward for the
     * ASSISTANT message whose tool-use blocks correspond to those tool results and move the
     * cutoff to include that ASSISTANT message in the prefix (i.e., cut before it).
     *
     */
    public static int findSafeCutoffPoint(List<LinkedMessageMap<String, Object>> compactedMessages, int cutoffIndex) {
        if (cutoffIndex <= 0 || cutoffIndex >= compactedMessages.size()) {
            return cutoffIndex;
        }
		
		//确保工具调用结果和对应的入参都在keep messages窗口内
		Map<String, Object> toolCallIds = new LinkedHashMap<>();
		for(int j = compactedMessages.size() - 1; j >= cutoffIndex; j --){
			LinkedMessageMap<String, Object> message = compactedMessages.get(j);
			String role = (String) message.get("role");
			if(role.equals(MessageBuilder.ROLE_TOOL)) { //处理工具调用结果
				String id = (String) message.get("tool_call_id");
				
				toolCallIds.put(id, 1);
			}
			else{
				//获取工具入参
				List<Map<String, Object>> toolCalls = (List<Map<String, Object>>) message.get("tool_calls");
				if(toolCalls != null && !toolCalls.isEmpty()){ //工具入参
					for(Map toolCall : toolCalls){
						String toolCallId = (String) toolCall.get("id");
						if(toolCallIds.containsKey(toolCallId)){ //在keep messages窗口内找到工具入参消息，直接清理掉
							toolCallIds.remove(toolCallId);
						}
						
					}
					
				}
			}
		}
		if(!toolCallIds.isEmpty()){ //意味着工具入参消息在窗口之外，需要将这些工具入参消息添加到messages中
			int k = 0;
			for(k = cutoffIndex -1 ; k >= 0 ; k --){
				LinkedMessageMap<String, Object> message = compactedMessages.get(k);
				String role = (String) message.get("role");
				if(role.equals(MessageBuilder.ROLE_TOOL)) { //在回溯过程中，又碰到了工具调用结果，还需继续进行回溯
					String id = (String) message.get("tool_call_id");
					toolCallIds.put(id, 1);
				}
				else {
					//获取工具入参
					List<Map<String, Object>> toolCalls = (List<Map<String, Object>>) message.get("tool_calls");
					if (toolCalls != null && !toolCalls.isEmpty()) { //工具入参
						for (Map toolCall : toolCalls) {
							String toolCallId = (String) toolCall.get("id");
							if (toolCallIds.containsKey(toolCallId)) { //在keep messages窗口内找到工具入参消息，直接清理掉
								toolCallIds.remove(toolCallId);
							}
						}
					}
				}
				if (toolCallIds.isEmpty()) {
					break;
				}
			}
			cutoffIndex = k < 0 ? 0 : k;
		}

        // Fallback: advance past all TOOL messages
        return cutoffIndex;
    }
 

    // -------------------------------------------------------------------------
    // Summary message construction
    // -------------------------------------------------------------------------

      

    // -------------------------------------------------------------------------
    // Summary message filtering (chained summarization support)
    // -------------------------------------------------------------------------

    /**
     * Removes previously injected summary messages from a list.
     *
     * <p>Prior summaries remain part of the next summarization input, but memory flushing must
     * only process the newly compacted raw messages to avoid duplicate extraction.
     */
    public static List<LinkedMessageMap<String, Object>> filterSummaryMessages(List<LinkedMessageMap<String, Object>> messages) {
        return messages.stream()
                .filter(message -> !SUMMARY_MSG_NAME.equals(message.getName()))
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Argument truncation (pre-summarization, non-LLM)
    // -------------------------------------------------------------------------

    /**
     * Aggregate tool-result pruning: walks backward through TOOL messages, protects the most
     * recent {@code protectTokens} worth of tool output, then replaces older oversized tool
     * results with a head+tail preview when the total prunable amount exceeds
     * {@code minimumTokens}.
     *
     * <p>Non-LLM operation. Pruned messages are replaced with copies so that shared message
     * instances (e.g. memory-store session memory reused across agents) are never mutated.
     * Returns the original list when nothing was pruned.
     */
    List<LinkedMessageMap<String, Object>> pruneToolResults(List<LinkedMessageMap<String, Object>> messages, PruneConfig pruneConfig) {
        if (pruneConfig == null || messages == null || messages.isEmpty()) {
            return messages;
        }

        int protectBudget = pruneConfig.getProtectTokens();
        int maxChars = pruneConfig.getMaxOutputChars();
        Set<String> excluded = pruneConfig.getExcludedTools();

        int protectedTokens = 0;
        int prunableTokens = 0;
        List<Integer> toPrune = new ArrayList<>();
        for (int i = messages.size() - 1; i >= 0; i--) {
			LinkedMessageMap<String, Object> msg = messages.get(i);
			String role = (String) msg.get("role");
            if (!role.equals(MessageBuilder.ROLE_TOOL)) {
                continue;
            }
			if(msg.isResultTrimmed()){
				continue;
			}
			String content = (String) msg.get("content");
            if (content == null) {
                continue;
            }
			 
            
			if (msg.getName() != null && excluded.contains(msg.getName())) {
				continue;
			}
			 
			int tokens = TokenCounterUtil.estimateMessageTokens(msg);
			if (protectedTokens < protectBudget) {
				protectedTokens += tokens;
				continue;
			}
			if (content.length() > maxChars) {
				prunableTokens += tokens;
				toPrune.add(i);
			}
            
        }

        if (prunableTokens < pruneConfig.getMinimumTokens() || toPrune.isEmpty()) {
            return messages;
        }

        // Replace pruned results with copies so the original message instances are untouched.
        List<LinkedMessageMap<String, Object>> result = null;
        int prunedCount = 0;
        for (int msgIdx : toPrune) {
//            int msgIdx = pos[0];
            LinkedMessageMap<String, Object> msg = messages.get(msgIdx);
            String text = (String) msg.get("content");
            String preview = buildPrunePreview(text, maxChars);
            if (preview == text) {
                continue;
            }
            if (result == null) {
                result = new ArrayList<>(messages);
            }
            LinkedMessageMap<String, Object> pruned = msg.copyMessage();            
            pruned.put("content", preview);
			pruned.setResultTrimmed(true);
            result.set(msgIdx, pruned);
            prunedCount++;
        }

        if (result == null) {
            return messages;
        }
        log.info("Pruned {} tool results (~{} tokens freed)", prunedCount, prunableTokens);
        return result;
    }

    public static String buildPrunePreview(String text, int maxChars) {
        int half = maxChars / 2;
        String head = text.substring(0, Math.min(half, text.length()));
        String tail =
                text.length() > half ? text.substring(Math.max(text.length() - half, half)) : "";
        int removed = text.length() - head.length() - tail.length();
        if (removed <= 0) {
            return text;
        }
        return head + "\n\n...(" + removed + " chars pruned)...\n\n" + tail;
    }
	

    /**
     * Truncates large {@code ToolUseBlock} argument values in old messages.
     *
     * <p>This is a lightweight, non-LLM pass that fires at a separate (lower) threshold
     * than full summarization. Only messages before the keep window are modified; recent
     * messages are left intact.
     *
     * <p>When {@code truncateConfig} is {@code null}, the original list is returned unchanged.
     */
    List<LinkedMessageMap<String, Object>> truncateArgs(List<LinkedMessageMap<String, Object>> messages, TruncateArgsConfig truncateConfig) {
        if (truncateConfig == null || messages == null || messages.isEmpty()) {
            return messages;
        }

        int totalTokens = TokenCounterUtil.calculateToken(messages);
        if (!shouldTruncateArgs(messages, totalTokens, truncateConfig)) {
            return messages;
        }

        int cutoff = determineTruncateCutoff(messages, truncateConfig);
        if (cutoff >= messages.size()) {
            return messages; // Nothing in the truncation window
        }

        boolean anyModified = false;
        List<LinkedMessageMap<String, Object>> result = new ArrayList<>(messages.size());
        for (int i = 0; i < messages.size(); i++) {
            LinkedMessageMap<String, Object> msg = messages.get(i);
            String role = (String) msg.get("role");
            if (i < cutoff && role.equals(MessageBuilder.ROLE_ASSISTANT)) {
				if(msg.isInputTrimmed()){
					result.add(msg);
					continue;
				}
                LinkedMessageMap<String, Object> truncated = truncateToolUseArgs(msg, truncateConfig);
                result.add(truncated);
                if (truncated != msg) {
                    anyModified = true;
                }
            } else {
                result.add(msg);
            }
        }

        if (anyModified) {
            log.debug("Arg truncation applied to messages before index {}", cutoff);
        }
        return anyModified ? result : messages;
    }

    private static boolean shouldTruncateArgs(
            List<LinkedMessageMap<String, Object>> messages, int totalTokens, TruncateArgsConfig cfg) {
        if (cfg.getTriggerMessages() > 0 && messages.size() >= cfg.getTriggerMessages()) {
            return true;
        }
        return cfg.getTriggerTokens() > 0 && totalTokens >= cfg.getTriggerTokens();
    }

		private static int determineTruncateCutoff(List<LinkedMessageMap<String, Object>> messages, TruncateArgsConfig cfg) {
        if (cfg.getKeepTokens() > 0) {
            // Token-budget-based keep window: scan from the end
            int tokensKept = 0;
            for (int i = messages.size() - 1; i >= 0; i--) {
                int msgTokens = TokenCounterUtil.estimateMessageTokens(messages.get(i));
                if (tokensKept + msgTokens > cfg.getKeepTokens()) {
                    return i + 1;
                }
                tokensKept += msgTokens;
            }
            return 0;
        }
        // Message-count keep window
        int keep = cfg.getKeepMessages();
        return Math.max(0, messages.size() - keep);
    }

    /**
     * 工具调用入参处理
     * 处理工具调用入参，如果入参超过限制，则进行截断
     * If no argument exceeds the limit, the original message reference is returned unchanged.
     */
    private static LinkedMessageMap<String, Object> truncateToolUseArgs(LinkedMessageMap<String, Object> msg, TruncateArgsConfig cfg) {
		List<Map<String,Object>> toolCalls = (List<Map<String,Object>>) msg.get("tool_calls");
		if(toolCalls == null || toolCalls.isEmpty()){
			return msg;
		}
        boolean anyModified = false;
		List<Map<String,Object>> newToolCalls = new ArrayList<>(toolCalls.size());
		for(Map<String,Object> toolCall : toolCalls){
			Map<String,Object> newToolCall = truncateToolUseBlock(toolCall, cfg);
			if(newToolCall != toolCall){
				anyModified = true;
			}
			newToolCalls.add(newToolCall);
		}
		
		
		if(!anyModified){
			return msg;
		}
		LinkedMessageMap<String, Object> processed = msg.copyMessage();
		processed.setInputTrimmed(true);
		processed.put("tool_calls", newToolCalls);
		return processed;
//        List<ContentBlock> newBlocks = new ArrayList<>(blocks.size());
//        for (ContentBlock block : blocks) {
//            if (block instanceof ToolUseBlock tu) {
//                ToolUseBlock truncated = truncateToolUseBlock(tu, cfg);
//                newBlocks.add(truncated);
//                if (truncated != tu) {
//                    anyModified = true;
//                }
//            } else {
//                newBlocks.add(block);
//            }
//        }
//
//        if (!anyModified) {
//            return msg;
//        }
//        return Msg.builder().role(msg.getRole()).name(msg.getName()).content(newBlocks).build();
    }

    /**
     * Returns a copy of the {@code ToolUseBlock} with large string arg values truncated,
     * or the original if no truncation was needed.
     */
    private static Map<String,Object> truncateToolUseBlock(Map<String,Object> toolCall, TruncateArgsConfig cfg) {
		Map<String, Object> function = (Map<String, Object>) toolCall.get("function");
		if (function == null) {
			return toolCall;
		}
		String input = (String) function.get("arguments");
		// 整串都没超过单值上限，内部叶子必然不超，无需解析
		if (input == null || input.isEmpty() || input.length() <= cfg.getMaxArgLength()) {
			return toolCall;
		}
		Map<String,Object> arguments;
		try {
			arguments = JsonUtil.json2Object(input, Map.class);
		} catch (Exception e) {
			return toolCall;
		}
		if (arguments == null || arguments.isEmpty()) {
			return toolCall;
		}
      

        boolean anyModified = false;
        Map<String, Object> newInput = new HashMap<>(arguments);
        for (Map.Entry<String, Object> entry : arguments.entrySet()) {
			Object value = entry.getValue();
			if(value instanceof String){
				String s =  (String) value;
				if(s.length() > cfg.getMaxArgLength()){
					newInput.put(entry.getKey(), s.substring(0, Math.min(20, s.length())) + cfg.getTruncationText());
					anyModified = true;
				}
			}
//            if (entry.getValue() instanceof String s && s.length() > cfg.getMaxArgLength()) {
//                newInput.put(entry.getKey(), s.substring(0, 20) + cfg.getTruncationText());
//                anyModified = true;
//            }
        }

        if (!anyModified) {
            return toolCall;
        }
		Map<String, Object> newFunction = new HashMap<>(function);
		newFunction.put("arguments", JsonUtil.object2json(newInput));
		Map<String, Object> newToolCall = new HashMap<>(toolCall);
		newToolCall.put("function", newFunction);
        return newToolCall;
    }
}
