package org.frameworkset.spi.ai.compaction;
/**
 * Copyright 2026 bboss
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.frameworkset.spi.ai.AIAgent;
import org.frameworkset.spi.ai.context.ChatContext;
import org.frameworkset.spi.ai.model.ChatAgentMessage;
import org.frameworkset.spi.ai.model.LinkedMessageMap;
import org.frameworkset.spi.ai.model.ModelInfo;
import org.frameworkset.spi.ai.model.ServerEvent;
import org.frameworkset.spi.ai.store.SessionMessage;
import org.frameworkset.spi.ai.util.MessageBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author biaoping.yin
 * @Date 2026/9/10
 */
public class SummeryUtils {
	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SummeryUtils.class);
	/**
	 * Formats a list of messages as a human-readable text block for the summarization LLM.
	 *
	 * <p>Renders TEXT blocks verbatim; TOOL_USE and TOOL_RESULT blocks as concise inline
	 * representations so the summarizer understands what actions were taken.
	 */
	public static String formatMessagesForSummary(List<LinkedMessageMap<String, Object>> messages) {
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
	
	/**
	 * Formats a list of messages as a human-readable text block for the summarization LLM.
	 *
	 * <p>Renders TEXT blocks verbatim; TOOL_USE and TOOL_RESULT blocks as concise inline
	 * representations so the summarizer understands what actions were taken.
	 */
	public static String formatSessionMessagesForSummary(List<SessionMessage> messages) {
		StringBuilder sb = new StringBuilder();
		for(SessionMessage msg : messages){
			String role =  msg.getRole();
			if(role != null && !role.equals(MessageBuilder.ROLE_SYSTEM)){
				String s = renderMessageForSummary(msg.getMessage(),role);
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
//		if(!role.equals(MessageBuilder.ROLE_TOOL) && content != null) {
//			sb.append(content);
//		}
		
		List<Map<String,Object>> toolCalls = (List<Map<String,Object>>) msg.get("tool_calls");
		if(toolCalls != null && !toolCalls.isEmpty()){
			// Append tool calls if any
			for(Map toolCall : toolCalls){
				if (sb.length() > 0 ) sb.append("\n\n");
				Map<String, Object> function = (Map<String, Object>) toolCall.get("function");
				String name = (String) function.get("name");
				sb.append("[tool_call: ").append(name).append("]");
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
		else if (content != null) {			
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
	 * <p>The message name is set to {@link ConversationCompactor#SUMMARY_MSG_NAME} so hooks can identify generated
	 * summaries, and the stable content-based ID keeps repeated session offloads idempotent.
	 */
	public static LinkedMessageMap<String, Object> buildSummaryMessage(ChatContext chatContext,
			AIAgent agent,String summary,
																	   List<LinkedMessageMap<String, Object>> summeryMessages,
																	   String filePath,
																	   LinkedMessageMap<String, Object> nextMessage) {
		String content;
		LinkedMessageMap<String, Object> linkedMessageMap = new LinkedMessageMap<>();
		Map<String, Object> meta = new LinkedHashMap<>();
		List<String> summaryIds = new ArrayList<>(summeryMessages.size());
		for(LinkedMessageMap<String, Object> summeryMessage : summeryMessages) {
			summaryIds.add(summeryMessage.getId());
		}
		meta.put("summaryIds", summaryIds);
		linkedMessageMap.setMeta(meta);
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
			if(chatContext.enableMemorySearch()) {
//			content = "Here is a summary of the conversation to date:\n\n" + summary;
				content =
						"You are in the middle of a conversation that has been summarized.\n\n"
								+ "The full summary history messageIds in the conversation is \n\n"
								+ "<summaryMessageIds>\n"
								+ String.join(",", summaryIds)
								+ "\n</summaryMessageIds> \n\n"
								+ " should you need to refer back to it for details.\n\n"
								+ "A condensed summary follows:\n\n"
								+ "<summary>\n"
								+ summary
								+ "\n</summary>";
			}
			else{
				content = "Here is a summary of the conversation to date:\n\n" + summary;
			}
		}
		
//		linkedMessageMap.setId(buildSummaryMessageId(content));
		linkedMessageMap.setMessageType(SessionMessage.MESSAGE_TYPE_SUMMARY_MESSAGE);
		linkedMessageMap.put("role", MessageBuilder.ROLE_USER);
		linkedMessageMap.setName( ConversationCompactor.SUMMARY_MSG_NAME);
		linkedMessageMap.put("content", content);
		linkedMessageMap.setNextMsgId(nextMessage.getId());
		
		/**
		 * 直接和下一个消息进行关联，编号一致
		 */
		linkedMessageMap.setSeqNo(agent.getNextSeqNo());
		return linkedMessageMap;
//        return Msg.builder()
//                .id(buildSummaryMessageId(content))
//                .role(MsgRole.USER)
//                .name(SUMMARY_MSG_NAME)
//                .content(TextBlock.builder().text(content).build())
//                .build();
	}
	
//	private static String buildSummaryMessageId(String content) {
//		UUID stableId = UUID.nameUUIDFromBytes(content.getBytes(StandardCharsets.UTF_8));
//		return ConversationCompactor.SUMMARY_MSG_NAME + ":" + stableId;
//	}
	public static String summarizePrefix(List<LinkedMessageMap<String, Object>> prefix, CompactionConfig config, ChatContext chatContext) {
		if (prefix.isEmpty()) {
			return "No previous conversation history.";
		}
		
		String formatted = formatMessagesForSummary(prefix);
//        String prompt = config.getSummaryPrompt().replace("{messages}", formatted);

//        List<LinkedMessageMap<String, Object>> summarizationInput = new ArrayList<>();
//		summarizationInput.add( MessageBuilder.buildUserMessage(prompt) );
		ModelInfo model = config.getCompactModel();
		if(model == null)
			model = chatContext.getModelInfo();
		AIAgent agent = new AIAgent(config.getSummaryPrompt()).setAgentId("__summary__");
		ChatAgentMessage chatAgentMessage = new ChatAgentMessage();
		chatAgentMessage.setModel(model.getModel());
		chatAgentMessage.setMaas(model.getMaas());
		agent.addParam("messages",formatted);
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
}
