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
package org.frameworkset.spi.ai.harness.gateway.channel.chatui;

 
import org.frameworkset.spi.ai.model.LinkedMessageMap;
import org.frameworkset.spi.ai.util.MessageBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Request object for {@link ChatUiChannel}. Carries the peer identity (optional) and the messages
 * to send to the agent.
 *
 * <p>The {@code peerId} is used by the channel router to build a stable session key:
 *
 * <ul>
 *   <li>If {@code peerId} is null and the channel's DmScope is MAIN, all requests share one
 *       session — convenient for single-user or testing scenarios.
 *   <li>If {@code peerId} is provided, each distinct peer gets its own session (when using
 *       PER_PEER or similar scopes).
 * </ul>
 */
public final class ChatUiRequest {
	
	private final String peerId;
	private final String agentId;
	private final String subagentId;
	private final List<LinkedMessageMap<String, Object>> messages;
	
	public ChatUiRequest(String peerId, String agentId, String subagentId, List<LinkedMessageMap<String, Object>> messages) {
		Objects.requireNonNull(messages, "messages");
		if (messages.isEmpty()) {
			throw new IllegalArgumentException("messages must not be empty");
		}
		this.peerId = peerId;
		this.agentId = agentId;
		this.subagentId = subagentId;
		this.messages = Collections.unmodifiableList(new ArrayList<LinkedMessageMap<String, Object>>(messages));
	}
	
	/** Single user-text message in single-session mode (no peer). */
	public static ChatUiRequest of(String text) {
		Objects.requireNonNull(text, "text");
		return new ChatUiRequest(null, null, null, Collections.singletonList(userMsg(text)));
	}
	
	/** Single user-text message associated with a specific peer id. */
	public static ChatUiRequest withPeer(String peerId, String text) {
		Objects.requireNonNull(peerId, "peerId");
		Objects.requireNonNull(text, "text");
		return new ChatUiRequest(peerId, null, null, Collections.singletonList(userMsg(text)));
	}
	
	/** Single user-text message targeted at a specific agent. */
	public static ChatUiRequest forAgent(String peerId, String agentId, String text) {
		Objects.requireNonNull(agentId, "agentId");
		Objects.requireNonNull(text, "text");
		return new ChatUiRequest(peerId, agentId, null, Collections.singletonList(userMsg(text)));
	}
	
	/** Single user-text message routed directly to an exposed subagent. */
	public static ChatUiRequest toSubagent(String subagentId, String text) {
		Objects.requireNonNull(subagentId, "subagentId");
		Objects.requireNonNull(text, "text");
		return new ChatUiRequest(null, null, subagentId, Collections.singletonList(userMsg(text)));
	}
	
	/** Multi-message request without a peer (single-session mode). */
	public static ChatUiRequest of(List<LinkedMessageMap<String, Object>> messages) {
		return new ChatUiRequest(null, null, null, messages);
	}
	
	/** Multi-message request for a specific peer. */
	public static ChatUiRequest withPeer(String peerId, List<LinkedMessageMap<String, Object>> messages) {
		return new ChatUiRequest(peerId, null, null, messages);
	}
	
	private static LinkedMessageMap<String, Object> userMsg(String text) {
		return MessageBuilder.buildUserMessage(text);
	}
	
	public String getPeerId() {
		return peerId;
	}
	
	public String getAgentId() {
		return agentId;
	}
	
	public String getSubagentId() {
		return subagentId;
	}
	
	public List<LinkedMessageMap<String, Object>> getMessages() {
		return messages;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ChatUiRequest)) {
			return false;
		}
		ChatUiRequest that = (ChatUiRequest) o;
		return Objects.equals(peerId, that.peerId)
				&& Objects.equals(agentId, that.agentId)
				&& Objects.equals(subagentId, that.subagentId)
				&& Objects.equals(messages, that.messages);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(peerId, agentId, subagentId, messages);
	}
	
	@Override
	public String toString() {
		return "ChatUiRequest{"
				+ "peerId='" + peerId + '\''
				+ ", agentId='" + agentId + '\''
				+ ", subagentId='" + subagentId + '\''
				+ ", messages=" + messages
				+ '}';
	}
}