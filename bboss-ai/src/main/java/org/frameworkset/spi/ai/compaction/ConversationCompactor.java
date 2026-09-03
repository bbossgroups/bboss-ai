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
import org.frameworkset.spi.ai.callback.ChatContext;
import org.frameworkset.spi.ai.model.ChatAgentMessage;
import org.frameworkset.spi.ai.model.LinkedMessageMap;
import org.frameworkset.spi.ai.model.ModelInfo;
import org.frameworkset.spi.ai.model.ServerEvent;
import org.frameworkset.spi.ai.util.MessageBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
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
    private final MemoryManager flushManager;

    public ConversationCompactor(ModelInfo model, MemoryManager flushManager) {
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
     * @param agentId              agent identifier used for the memory offload path
     * @param sessionId            session identifier used for the memory offload path
     * @return {@code Optional.empty()} when no compaction was needed; otherwise the replacement
     *         message list consisting of {@code [summaryUserMsg] + preservedTail}
     */
    public List<LinkedMessageMap<String, Object>> compactIfNeeded(
			ChatContext chatContext, AIAgent agent,
            List<LinkedMessageMap<String, Object>> conversationMessages,
			CompactionConfig config,
			String agentId,
			String sessionId) {

        if (conversationMessages == null || conversationMessages.isEmpty()) {
            return Collections.emptyList();
        }

        // Step 1a: Lightweight arg truncation (non-LLM).
        // Step 1b: Aggregate tool-result pruning (non-LLM).
        List<LinkedMessageMap<String, Object>> messages =
                pruneToolResults(
                        truncateArgs(conversationMessages, config.getTruncateArgsConfig()),
                        config.getPruneConfig());

        int totalTokens = TokenCounterUtil.calculateToken(messages);
        if (!shouldCompact(messages, totalTokens, config)) {
            return Collections.emptyList();
        }

        int cutoff = determineCutoffIndex(messages, totalTokens, config);
        if (cutoff <= 0) {
            log.debug("Compaction triggered but safe cutoff is 0 — skipping");
            return Collections.emptyList();
        }

        // Keep prior summaries in the summarization input so each compaction builds on the
        // previous one, but exclude them from memory flushing to avoid duplicate extraction.
        List<LinkedMessageMap<String, Object>> summaryInput = new ArrayList<>(messages.subList(0, cutoff));
        List<LinkedMessageMap<String, Object>> flushInput = filterSummaryMessages(summaryInput);
        List<LinkedMessageMap<String, Object>> tail = new ArrayList<>(messages.subList(cutoff, messages.size()));

        log.info(
                "Compaction triggered: total={} msgs / {} tokens, cutoff={}, keeping={} msgs",
                messages.size(),
                totalTokens,
                cutoff,
                tail.size());

        // Step 2: Flush long-term memories only from newly compacted raw messages (best-effort).
		if(config.isFlushBeforeCompact()) {
				flushManager
					.flushMemories(chatContext, agent, flushInput);
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
		String summarize = summarizePrefix(summaryInput, config);
		
		List<LinkedMessageMap<String, Object>> compacted = new ArrayList<>();
		LinkedMessageMap<String, Object> summaryMsg = buildSummaryMessage(summarize, null);
		compacted.add(summaryMsg);
		compacted.addAll(tail);
		log.info(
				"Compaction complete: {} msgs → 1"
						+ " summary + {} tail = {}"
						+ " total",
				messages.size(),
				tail.size(),
				compacted.size());
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
            log.debug(
                    "Compaction trigger: message count {} >= {}",
                    messages.size(),
                    config.getTriggerMessages());
            return true;
        }
        if (config.getTriggerTokens() > 0 && totalTokens >= config.getTriggerTokens()) {
            log.debug(
                    "Compaction trigger: token count {} >= {}",
                    totalTokens,
                    config.getTriggerTokens());
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
    private static int determineCutoffIndex(
            List<LinkedMessageMap<String, Object>> messages, int totalTokens, CompactionConfig config) {
        int rawCutoff;
        if (config.getKeepTokens() > 0) {
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
    private static int findSafeCutoffPoint(List<LinkedMessageMap<String, Object>> messages, int cutoffIndex) {
        if (cutoffIndex <= 0 || cutoffIndex >= messages.size()) {
            return cutoffIndex;
        }

        LinkedMessageMap<String, Object> atCutoff = messages.get(cutoffIndex);
        String role = (String) atCutoff.get("role");
        if (!MessageBuilder.ROLE_TOOL.equals(role)) {
            return cutoffIndex;
        }

        // Collect tool-call IDs from consecutive TOOL messages at/after the cutoff
        List<String> toolCallIds = new ArrayList<>();
        int idx = cutoffIndex;
        while (idx < messages.size()  ) {
			LinkedMessageMap<String, Object> tmp = messages.get(idx);
			role = (String) tmp.get("role");
			if(!MessageBuilder.ROLE_TOOL.equals(role)){
				break;
			}
			String toolcallid = (String) tmp.get("tool_call_id");
			toolCallIds.add(toolcallid);
//            for (ContentBlock block : messages.get(idx).getContent()) {
//                if (block instanceof ToolResultBlock tr && tr.getId() != null) {
//                    toolCallIds.add(tr.getId());
//                }
//            }
            idx++;
        }

        if (toolCallIds.isEmpty()) {
            // No IDs found — advance past all TOOL messages to avoid orphaned results
            return idx;
        }

        // Search backward for the ASSISTANT message that issued those tool calls
        for (int i = cutoffIndex - 1; i >= 0; i--) {
			LinkedMessageMap<String, Object> msg = messages.get(i);
			role = (String) msg.get("role");
            if (MessageBuilder.ROLE_ASSISTANT.equals(role)) {
				List<Map<String,Object>> toolCalls = (List<Map<String,Object>>) msg.get("tool_calls");
				if(toolCalls != null && !toolCalls.isEmpty()){
					for(Map toolCall : toolCalls){
						String toolCallId = (String) toolCall.get("id");
						if(toolCallIds.contains(toolCallId)){
							return i;
						}
					}
				}
//                for (ContentBlock block : msg.getContent()) {
//                    if (block instanceof ToolUseBlock tu && toolCallIds.contains(tu.getId())) {
//                        // Move the cutoff to just before this ASSISTANT message
//                        return i;
//                    }
//                }
            }
        }

        // Fallback: advance past all TOOL messages
        return idx;
    }

    // -------------------------------------------------------------------------
    // Summarization
    // -------------------------------------------------------------------------

    private String summarizePrefix(List<LinkedMessageMap<String, Object>> prefix, CompactionConfig config) {
        if (prefix.isEmpty()) {
            return "No previous conversation history.";
        }

        String formatted = formatMessagesForSummary(prefix);
        String prompt = config.getSummaryPrompt().replace("{messages}", formatted);

//        List<LinkedMessageMap<String, Object>> summarizationInput = new ArrayList<>();
//		summarizationInput.add( MessageBuilder.buildUserMessage(prompt) );
		AIAgent agent = new AIAgent(prompt);
		ChatAgentMessage chatAgentMessage = new ChatAgentMessage();
		chatAgentMessage.setModel(model.getModel());
		chatAgentMessage.setMaas(model.getMaas());
		try {
			ServerEvent serverEvent = agent.chat(chatAgentMessage);			
			String summary = serverEvent.getData();
			if(summary != null && !summary.isEmpty()){
				return summary;
			}
			else{
				return "(Summary unavailable)";
			}
		} catch (Exception e) {
			log.warn("Summarization LLM call failed: {}", e.getMessage());
			return "(Summarization failed: " + e.getMessage() + ")";
		}
//
//        return model.stream(summarizationInput, null, null)
//                .reduce(
//                        new StringBuilder(),
//                        (sb, resp) -> {
//                            if (resp.getContent() != null) {
//                                for (ContentBlock block : resp.getContent()) {
//                                    if (block instanceof TextBlock tb && tb.getText() != null) {
//                                        sb.append(tb.getText());
//                                    }
//                                }
//                            }
//                            return sb;
//                        })
//                .map(StringBuilder::toString)
//                .map(String::strip)
//                .filter(s -> !s.isBlank())
//                .defaultIfEmpty("(Summary unavailable)")
//                .onErrorResume(
//                        e -> {
//                            if (containsInterruptedException(e)) {
//                                return Mono.error(e);
//                            }
//                            log.warn("Summarization LLM call failed: {}", e.getMessage());
//                            return Mono.just("(Summarization failed: " + e.getMessage() + ")");
//                        });
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
     * Formats a list of messages as a human-readable text block for the summarization LLM.
     *
     * <p>Renders TEXT blocks verbatim; TOOL_USE and TOOL_RESULT blocks as concise inline
     * representations so the summarizer understands what actions were taken.
     */
    static String formatMessagesForSummary(List<LinkedMessageMap<String, Object>> messages) {
		StringBuilder sb = new StringBuilder();
		for(LinkedMessageMap<String, Object> msg : messages){
			String role = (String) msg.get("role");
			if(role != null && !role.equals(MessageBuilder.ROLE_SYSTEM)){
				String s = renderMessageForSummary(msg,role);
				if(!s.isEmpty()) {
					if(sb.length() > 0){
						sb.append("\n\n");
					}
					sb.append(s);
				}
			}
		}
		return sb.toString();
//        return messages.stream()
//                .filter(m ->{
//					String role = (String)m.get("role");
//					return role != null && !role.equals(MessageBuilder.ROLE_SYSTEM);
////					m.getRole() != null && m.getRole() != MsgRole.SYSTEM;
//				} )
//                .map(ConversationCompactor::renderMessageForSummary)
//                .filter(s -> !s.isEmpty())
//                .collect(Collectors.joining("\n\n"));
    }

    private static String renderMessageForSummary(LinkedMessageMap<String, Object> msg,String role) {
        String roleLabel = null;
		if(role.equals(MessageBuilder.ROLE_USER)){
			roleLabel = "Human";
		}
		else if(role.equals(MessageBuilder.ROLE_ASSISTANT)){
			roleLabel = "AI";
		}
		else if(role.equals(MessageBuilder.ROLE_TOOL)){
			roleLabel = "Tool";
		}
		else {
			roleLabel = role;
		}
		
//                switch (msg.getRole()) {
//                    case USER -> "Human";
//                    case ASSISTANT -> "AI";
//                    case TOOL -> "Tool";
//                    default -> msg.getRole().name();
//                };

        StringBuilder sb = new StringBuilder(roleLabel).append(": ");
		String content = (String) msg.get("content");
		boolean first = true;
		if(content != null) {
			first = false;
			sb.append(content);
		}
		
		List<Map<String,Object>> toolCalls = (List<Map<String,Object>>) msg.get("tool_calls");
		if(toolCalls != null && !toolCalls.isEmpty()){
			// Append tool calls if any
			for(Map toolCall : toolCalls){
				if (!first) sb.append(" ");
				Map<String, Object> function = (Map<String, Object>) toolCall.get("function");
				String name = (String) function.get("name");
				sb.append("[tool_call: ").append(name).append("]");
				first = false;
//				sb.append(renderToolUse(toolCall));
			}
		}
		else if(role.equals(MessageBuilder.ROLE_TOOL)){
			sb.append("[tool_result: ")
					.append(msg.getName() != null ? msg.getName() : "?")
					.append("] ");
			if (!content.isEmpty()) {
				sb.append(content.length() > 500 ? content.substring(0, 500) + "..." : content);
			}
		}
		else{
			sb.append(content.trim());
		}
		
//			
//        for (ContentBlock block : msg.getContent()) {
//            if (!first) sb.append(" ");
//            first = false;
//            if (block instanceof TextBlock tb && tb.getText() != null && !tb.getText().isBlank()) {
//                sb.append(tb.getText().strip());
//            } else if (block instanceof ToolUseBlock tu) {
//                sb.append("[tool_call: ").append(tu.getName()).append("]");
//            } else if (block instanceof ToolResultBlock tr) {
//                String text = extractToolResultText(tr);
//                sb.append("[tool_result: ")
//                        .append(tr.getName() != null ? tr.getName() : "?")
//                        .append("] ");
//                if (!text.isBlank()) {
//                    sb.append(text.length() > 500 ? text.substring(0, 500) + "..." : text);
//                }
//            }
//        }
        return sb.toString().trim();
    }

//    private static String extractToolResultText(ToolResultBlock tr) {
//        if (tr.getOutput() == null) return "";
//        return tr.getOutput().stream()
//                .filter(b -> b instanceof TextBlock)
//                .map(b -> ((TextBlock) b).getText())
//                .filter(t -> t != null && !t.isBlank())
//                .collect(Collectors.joining(" "));
//    }

    // -------------------------------------------------------------------------
    // Summary message construction
    // -------------------------------------------------------------------------

    /**
     * Builds a USER message carrying the summary.
     *
     * <p>When {@code filePath} is non-null, the message includes a reference to where the full
     * conversation history was offloaded.
     * When null, falls back to the simple "summary to date" format.
     *
     * <p>The message name is set to {@link #SUMMARY_MSG_NAME} so hooks can identify generated
     * summaries, and the stable content-based ID keeps repeated session offloads idempotent.
     */
    private static LinkedMessageMap<String, Object> buildSummaryMessage(String summary, String filePath) {
        String content;
        if (filePath != null) {
            content =
                    "You are in the middle of a conversation that has been summarized.\n\n"
                            + "The full conversation history has been saved to "
                            + filePath
                            + " should you need to refer back to it for details.\n\n"
                            + "A condensed summary follows:\n\n"
                            + "<summary>\n"
                            + summary
                            + "\n</summary>";
        } else {
            content = "Here is a summary of the conversation to date:\n\n" + summary;
        }
		LinkedMessageMap<String, Object> linkedMessageMap = new LinkedMessageMap<>();
		linkedMessageMap.setId(buildSummaryMessageId(content));
		linkedMessageMap.put("role", MessageBuilder.ROLE_USER);
		linkedMessageMap.setName( SUMMARY_MSG_NAME);
		linkedMessageMap.put("content", content);
		return linkedMessageMap;
//        return Msg.builder()
//                .id(buildSummaryMessageId(content))
//                .role(MsgRole.USER)
//                .name(SUMMARY_MSG_NAME)
//                .content(TextBlock.builder().text(content).build())
//                .build();
    }

    private static String buildSummaryMessageId(String content) {
        UUID stableId = UUID.nameUUIDFromBytes(content.getBytes(StandardCharsets.UTF_8));
        return SUMMARY_MSG_NAME + ":" + stableId;
    }

    // -------------------------------------------------------------------------
    // Summary message filtering (chained summarization support)
    // -------------------------------------------------------------------------

    /**
     * Removes previously injected summary messages from a list.
     *
     * <p>Prior summaries remain part of the next summarization input, but memory flushing must
     * only process the newly compacted raw messages to avoid duplicate extraction.
     */
    static List<LinkedMessageMap<String, Object>> filterSummaryMessages(List<LinkedMessageMap<String, Object>> messages) {
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
     * <p>Non-LLM operation. Returns the original list if no pruning occurred.
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
        List<int[]> toPrune = new ArrayList<>();
        for (int i = messages.size() - 1; i >= 0; i--) {
			LinkedMessageMap<String, Object> msg = messages.get(i);
			String role = (String) msg.get("role");
            if (!role.equals(MessageBuilder.ROLE_TOOL)) {
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
				toPrune.add(new int[] {i, 0});
			}
            
        }

        if (prunableTokens < pruneConfig.getMinimumTokens() || toPrune.isEmpty()) {
            return messages;
        }

        List<LinkedMessageMap<String, Object>> result = new ArrayList<>(messages);
        for (int[] pos : toPrune) {
            int msgIdx = pos[0];
            int blockIdx = pos[1];
			LinkedMessageMap<String, Object> msg = result.get(msgIdx);			
			String newContent = (String) msg.get("content");
			
			String text  = newContent;
			String preview = buildPrunePreview(text, maxChars);
			
			LinkedMessageMap<String, Object> pruned = new LinkedMessageMap<>();
			pruned.setName(msg.getName());
			pruned.putAll(msg);
			pruned.put("content", preview);
			pruned.setTimestamp(msg.getTimestamp());
			result.set(msgIdx, pruned);
//            ToolResultBlock tr = (ToolResultBlock) newBlocks.get(blockIdx);
//            String text = extractToolResultText(tr);
//            String preview = buildPrunePreview(text, maxChars);
//            ToolResultBlock pruned =
//                    ToolResultBlock.builder()
//                            .id(tr.getId())
//                            .name(tr.getName())
//                            .output(List.of(TextBlock.builder().text(preview).build()))
//                            .build();
//            newBlocks.set(blockIdx, pruned);
//            result.set(
//                    msgIdx,
//                    Msg.builder()
//                            .id(msg.getId())
//                            .name(msg.getName())
//                            .role(msg.getRole())
//                            .content(newBlocks)
//                            .metadata(msg.getMetadata())
//                            .timestamp(msg.getTimestamp())
//                            .build());
        }

        log.info("Pruned {} tool results (~{} tokens freed)", toPrune.size(), prunableTokens);
        return result;
    }

    private static String buildPrunePreview(String text, int maxChars) {
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
     * Returns a copy of the message with large {@code ToolUseBlock} argument values shortened.
     * If no argument exceeds the limit, the original message reference is returned unchanged.
     */
    private static LinkedMessageMap<String, Object> truncateToolUseArgs(LinkedMessageMap<String, Object> msg, TruncateArgsConfig cfg) {
		String content = (String) msg.get("content");
		if(content == null || content.isEmpty()){
			return msg;
		}
//        List<ContentBlock> blocks = msg.getContent();
//        if (blocks == null || blocks.isEmpty()) {
//            return msg;
//        }
		
		List<Map<String,Object>> toolCalls = (List<Map<String,Object>>) msg.get("tool_calls");
		if(toolCalls == null || toolCalls.isEmpty()){
			return msg;
		}
        boolean anyModified = false;
		List<Map<String,Object>> newToolCalls = new ArrayList<>(toolCalls.size());
		for(Map<String,Object> toolCall : toolCalls){
			Map<String,Object> newToolCall = truncateToolUseBlock(toolCall, cfg);
		 	if(newToolCall != null){
				anyModified = true;
			}
		}
		return msg;
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
		String name = (String) function.get("name");
		
		String  input = (String) function.get("arguments");
		if (input == null || input.isEmpty()) {
			return null;
		}
		Map<String,Object> arguments = JsonUtil.json2Object(input,Map.class);
      

        boolean anyModified = false;
        Map<String, Object> newInput = new HashMap<>(arguments);
        for (Map.Entry<String, Object> entry : arguments.entrySet()) {
			Object value = entry.getValue();
			if(value instanceof String){
				String s =  (String) value;
				if(s.length() > cfg.getMaxArgLength()){
					newInput.put(entry.getKey(), s.substring(0, 20) + cfg.getTruncationText());
					anyModified = true;
				}
			}
//            if (entry.getValue() instanceof String s && s.length() > cfg.getMaxArgLength()) {
//                newInput.put(entry.getKey(), s.substring(0, 20) + cfg.getTruncationText());
//                anyModified = true;
//            }
        }

        if (!anyModified) {
            return null;
        }
		input = JsonUtil.object2json(newInput);
		function.put("arguments", input);
        return toolCall;
    }
}
