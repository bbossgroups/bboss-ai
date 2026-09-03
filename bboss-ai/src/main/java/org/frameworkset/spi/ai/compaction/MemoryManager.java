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

import com.frameworkset.util.SimpleStringUtil;
//import io.agentscope.harness.agent.memory.compaction.ConversationCompactor;
//import io.agentscope.harness.agent.memory.session.SessionTranscriptWriter;
import org.frameworkset.spi.ai.AIAgent;
import org.frameworkset.spi.ai.callback.ChatContext;
import org.frameworkset.spi.ai.model.*;
import org.frameworkset.spi.ai.util.MessageBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Manages memory flush operations: extracting long-term memories from a conversation
 * window and appending them to today's daily memory ledger.
 *
 * <p><b>Two-layer memory model</b> (this class owns only the first layer):
 * <ul>
 *   <li>{@code memory/YYYY-MM-DD.md} — append-only daily ledger. Each compaction's flush
 *       appends a timestamped section here. Written ONLY by this class.</li>
 *   <li>{@code MEMORY.md} — globally curated, deduplicated, size-bounded long-term memory.
 *       Written ONLY by {link MemoryConsolidator} on a periodic schedule. Treated as
 *       read-only context here.</li>
 * </ul>
 */
public class MemoryManager {

    private static final Logger log = LoggerFactory.getLogger(MemoryManager.class);
 

    /**
     * Default prompt for the memory extraction step. Exposed publicly so callers can extend
     * (e.g. append project-specific guidelines) when constructing
     * {link io.agentscope.harness.agent.memory.MemoryConfig}.
     */
    public static final String DEFAULT_FLUSH_PROMPT = "#[prompts/compaction/DEFAULT_FLUSH_PROMPT.md,type=resource]";
//            """
//            You are a memory extraction assistant. Analyze the conversation below and extract \
//            important facts, decisions, preferences, and contextual information that should be \
//            remembered for future conversations.
//
//            Output ONLY the extracted memories as a markdown bullet list. Each item should be \
//            a concise, self-contained fact. Include dates, names, and specifics when available.
//
//            If there is nothing worth remembering, respond with exactly: NO_REPLY
//
//            Guidelines:
//            - Extract user preferences, personal information, project decisions
//            - Capture important technical decisions and their rationale
//            - Note any commitments, deadlines, or action items
//            - Record relationship context (who works on what, team structure)
//            - Ignore routine greetings, tool invocations, and ephemeral status updates
//
//            IMPORTANT — write target and append-only rules:
//            - You are writing to TODAY'S daily memory ledger (memory/YYYY-MM-DD.md), NOT to \
//            MEMORY.md. The daily ledger is append-only — your output will be appended after the \
//            entries already shown below.
//            - MEMORY.md is the curated long-term memory and is shown ONLY as read-only context. \
//            Do NOT restate facts already covered by MEMORY.md or by today's earlier entries; a \
//            separate consolidation step periodically merges new daily entries into MEMORY.md.
//            - Keep each bullet point independent and self-contained so entries can be searched \
//            individually.\
//            """;

    private final ModelInfo model;
    private final String flushPrompt;

    public MemoryManager(ModelInfo	 model) {
        this(model, DEFAULT_FLUSH_PROMPT);
    }

    /**
     * @param flushPrompt SYSTEM prompt for the extraction LLM call. {@code null} falls back to
     *     {@link #DEFAULT_FLUSH_PROMPT}.
     */
    public MemoryManager(ModelInfo model, String flushPrompt) {
        this.model = model;
        this.flushPrompt = flushPrompt != null ? flushPrompt : DEFAULT_FLUSH_PROMPT;
    }

    /**
     * Extracts long-term memories from messages using the model and writes them to disk.
     *
     * <p>Provides existing MEMORY.md and today's daily file content to the extraction LLM
     * so it can effectively deduplicate and avoid re-extracting known facts.
     */
    public void flushMemories(ChatContext rc, AIAgent agent, List<LinkedMessageMap<String, Object>> messages) {
        String conversationText = serializeMessages(messages);
        if (conversationText.isEmpty()) {
            return ;
        }

		Memory longTermMemory = readExistingLongTermMemoryContent(rc, agent);
        String existingMemory = longTermMemory != null ? longTermMemory.getContent() : ""	;
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
//        String dailyRelPath = WorkspaceConstants.MEMORY_DIR + "/" + today + ".md";
		Memory dailyMemory = readExistingDayMemoryContent(rc,agent, today);
        String existingDaily = dailyMemory != null ? dailyMemory.getContent() : ""	;	

        StringBuilder userPrompt = new StringBuilder();
        if (!existingMemory.isEmpty()) {
            userPrompt
                    .append("MEMORY.md (read-only curated long-term memory — do NOT restate):\n")
                    .append(existingMemory)
                    .append("\n\n");
        }
        if (!existingDaily.isEmpty()) {
            userPrompt
                    .append("Today's daily ledger so far (your output will be appended after):\n")
                    .append(existingDaily)
                    .append("\n\n");
        }
        userPrompt
                .append(
                        "Extract NEW memories from this conversation window (skip anything"
                                + " already covered above):\n\n")
                .append(conversationText);

        List<Map<String, Object>> flushInput = new ArrayList<>();
        flushInput.add(MessageBuilder.buildMessage(MessageBuilder.ROLE_SYSTEM, flushPrompt));
//                Msg.builder()
//                        .role(MsgRole.SYSTEM)
//                        .content(TextBlock.builder().text(flushPrompt).build())
//                        .build());
        flushInput.add(MessageBuilder.buildMessage(MessageBuilder.ROLE_USER, userPrompt.toString()));
//                Msg.builder()
//                        .role(MsgRole.USER)
//                        .content(TextBlock.builder().text(userPrompt.toString()).build())
//                        .build());
		AIAgent aiAgent = new AIAgent(userPrompt.toString()).setSystemPrompt(flushPrompt);
		ChatAgentMessage chatAgentMessage = new ChatAgentMessage();
		chatAgentMessage.setMaas(model.getMaas());
		chatAgentMessage.setModel(model.getModel());
		ServerEvent serverEvent = aiAgent.chat(chatAgentMessage);
		String extracted = serverEvent.getData();
		if (extracted.isEmpty() || extracted.trim().equals("NO_REPLY")) {
			if(log.isDebugEnabled()) {
				log.debug("No memories to flush");
			}
			return ;
		}
		writeMemoryFiles(rc, dailyMemory,  extracted,agent);
//        return model.stream(flushInput, null, null)
//                .reduce(
//                        new StringBuilder(),
//                        (sb, chatResponse) -> {
//                            List<ContentBlock> blocks = chatResponse.getContent();
//                            if (blocks != null) {
//                                for (ContentBlock block : blocks) {
//                                    if (block instanceof TextBlock tb) {
//                                        String t = tb.getText();
//                                        if (t != null) {
//                                            sb.append(t);
//                                        }
//                                    }
//                                }
//                            }
//                            return sb;
//                        })
//                .flatMap(
//                        sb -> {
//                            String extracted = sb.toString();
//                            if (extracted.isBlank() || extracted.strip().equals("NO_REPLY")) {
//                                log.debug("No memories to flush");
//                                return Mono.empty();
//                            }
//                            writeMemoryFiles(rc, extracted);
//                            return Mono.empty();
//                        });
    }

//    /**
//     * Returns the string path of the session JSONL file where messages for the given agent and
//     * session are offloaded. Used by the compaction layer to embed the archive location in the
//     * summary message so the agent can retrieve full history if needed.
//     *
//     * @deprecated Prefer {link SessionTranscriptWriter#resolveContextPath}.
//     */
//    @Deprecated
//    public String resolveOffloadPath(RuntimeContext rc, String agentId, String sessionId) {
//        return new SessionTranscriptWriter(workspaceManager)
//                .resolveContextPath(rc, agentId, sessionId);
//    }
//
//    /**
//     * Offloads raw messages to the JSONL session tree.
//     *
//     * @deprecated Prefer {@link SessionTranscriptWriter#appendMessages}. Kept as a thin
//     *     delegate so compaction / overflow-recovery call sites keep compiling during the
//     *     migration; new code should use {@link SessionTranscriptWriter} directly.
//     */
//    @Deprecated
//    public void offloadMessages(
//            RuntimeContext rc, List<Msg> messages, String agentId, String sessionId) {
//        new SessionTranscriptWriter(workspaceManager)
//                .appendMessages(rc, messages, agentId, sessionId);
//        log.debug(
//                "Offloaded {} messages for agent={}, session={}",
//                messages.size(),
//                agentId,
//                sessionId);
//    }

    /**
     * Appends the extracted entries to today's daily memory ledger.
     *
     * <p>MEMORY.md is intentionally <b>NOT</b> touched here — it is owned by
     * {link MemoryConsolidator}, which periodically merges the daily ledgers into a
     * curated, size-bounded MEMORY.md.
     */
    private void writeMemoryFiles(ChatContext rc, Memory	 content,String extracted,AIAgent agent) {
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

        String dailyEntry =
                String.format(
                        "\n## Memory Flush — %s\n%s\n",
                        java.time.Instant.now().toString(), extracted);

//        String dailyRelPath = WorkspaceConstants.MEMORY_DIR + "/" + today + ".md";
		String oldContent = content.getContent();
		StringBuilder newContent = new StringBuilder();
		newContent.append(oldContent).append(dailyEntry);
		content.setContent(newContent.toString());
		agent.getMainSessionStore().getAgentMemoryStore().writeDailyMemory(rc, content);	
//        workspaceManager.appendUtf8WorkspaceRelative(rc, dailyRelPath, dailyEntry);
    }

    private Memory readExistingLongTermMemoryContent(ChatContext rc,AIAgent agent) {
        try {
			Memory content = agent.getMainSessionStore().getAgentMemoryStore().readExistingLongTermMemoryContent(  rc,  agent);
            return content;
        } catch (Exception e) {
            log.debug("Could not read long-term memory for agent {},{}: {}", agent.getAgentId(), agent.getAgentName(),e.getMessage());
            return null;
        }
    }
	
	private Memory readExistingDayMemoryContent(ChatContext rc,AIAgent agent,String day) {
		try {
			Memory content = agent.getMainSessionStore().getAgentMemoryStore().readExistingDayMemoryContent(rc, agent, day	);
			return content != null ? content : null;
		} catch (Exception e) {
			log.debug("Could not read daily memory for agent {},{},day {}: {}", agent.getAgentId(), agent.getAgentName(), day, e.getMessage());
			return null;
		}
	}

    private static final String SESSION_CONTEXT_TAG = "<session_context>";

    /**
     * Serializes all messages into a textual representation for the memory extraction model.
     * Includes USER, ASSISTANT, and TOOL messages. Assistant tool-call blocks and tool-result
     * blocks are rendered as concise text so the model can extract memories from tool interactions.
     * Internal context messages are skipped because they are generated from existing context,
     * not new user/assistant/tool facts.
     */
    private String serializeMessages(List<LinkedMessageMap<String, Object>> messages) {
		StringBuilder sb = new StringBuilder();
		for (LinkedMessageMap<String, Object> message : messages) {
			String role = (String) message.get("role");
			if(role != null && !role.equals(MessageBuilder.ROLE_SYSTEM) && !isInternalContextMessage(message, role)){
				String s = renderMessage(message,role);
				if(SimpleStringUtil.isNotEmpty(s)) {
					if(sb.length() > 0)
						sb.append("\n");
					sb.append(s);
				}
			}
        }
		return sb.toString();
//        return messages.stream()
//                .filter(m -> m.getRole() != null && m.getRole() != MsgRole.SYSTEM)
//                .filter(m -> !isInternalContextMessage(m))
//                .map(this::renderMessage)
//                .filter(s -> s != null && !s.isBlank())
//                .collect(Collectors.joining("\n"));
    }

    private static boolean isInternalContextMessage(LinkedMessageMap<String, Object> msg,String role) {
        return isSessionContextMessage(msg,role) || isCompactionSummaryMessage(msg);
    }

    private static boolean isCompactionSummaryMessage(LinkedMessageMap<String, Object> msg) {
        return msg != null && ConversationCompactor.SUMMARY_MSG_NAME.equals(msg.getName());
    }

    private static boolean isSessionContextMessage(LinkedMessageMap<String, Object> message,String role) {
        if (role != null && !role.equals(MessageBuilder.ROLE_USER)) {
            return false;
        }
        String text = (String) message.get("content");
        return text != null && text.contains(SESSION_CONTEXT_TAG);
    }

    private String renderMessage(LinkedMessageMap msg,String role) {
        String body = renderContentBlocks(msg,  role);
        if (body == null) {
            return null;
        }
        return "[" + role + "]: " + body;
    }

    /**
     * Renders all content blocks of a message into a single text string.
     * Returns null if no renderable content is found.
     */
    private String renderContentBlocks(LinkedMessageMap<String, Object> msg,String role) {
		List<Map<String,Object>> toolCalls = (List<Map<String,Object>>) msg.get("tool_calls");
		String content = (String) msg.get("content");
        if (content == null || content.isEmpty()) {
            return null;
        }

		StringBuilder sb = new StringBuilder();
		sb.append(content);
		// Append tool calls if any
		if(toolCalls != null && !toolCalls.isEmpty()){
			for(Map toolCall : toolCalls){
				sb.append("\n");
				sb.append(renderToolUse(toolCall));
			}
		}
		else if(role != null && role.equals(MessageBuilder.ROLE_TOOL)){
			sb.append("\n");
			sb.append(renderToolResult(msg));
		}
		return sb.toString();
//        List<String> parts = new ArrayList<>();
//        for (ContentBlock block : blocks) {
//            if (block instanceof TextBlock tb) {
//                String text = tb.getText();
//                if (text != null && !text.isBlank()) {
//                    parts.add(text);
//                }
//            } else if (block instanceof ToolUseBlock tu) {
//                parts.add(renderToolUse(tu));
//            } else if (block instanceof ToolResultBlock tr) {
//                parts.add(renderToolResult(tr));
//            }
//        }
//
//        if (parts.isEmpty()) {
//            return null;
//        }
//        return String.join("\n", parts);
    }
	
	/**
	 * 处理大模型识别的工具方法id和输入参数信息
	 * @param tu
	 * @return
	 */
    private static String renderToolUse(Map<String,Object> tu) {
        StringBuilder sb = new StringBuilder();
		Map<String, Object> function = (Map<String, Object>) tu.get("function");
		String name = (String) function.get("name");
        sb.append("[tool_call: ").append(name);
		String  input = (String) function.get("arguments");
        if (input != null && !input.isEmpty()) {
            try {
                String inputJson = input;
                if (inputJson.length() > 500) {
                    inputJson = inputJson.substring(0, 500) + "...";
                }
                sb.append("(").append(inputJson).append(")");
            } catch (Exception e) {
                sb.append("(...)");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private static String renderToolResult(LinkedMessageMap<String, Object> msg) {
        StringBuilder sb = new StringBuilder();
		String name = msg.getName();
        sb.append("[tool_result");
        if (name != null) {
            sb.append(": ").append(name);
        }
        sb.append("] ");
		String content = (String) msg.get("content");
        if (content != null) {            
			if (content.length() > 1000) {
				sb.append(content, 0, 1000).append("...(truncated)");
			} else {
				sb.append(content);
			}               
            
        }
        return sb.toString();
    }
}
