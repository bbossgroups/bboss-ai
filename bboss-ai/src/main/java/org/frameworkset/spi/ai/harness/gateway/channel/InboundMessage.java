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
package org.frameworkset.spi.ai.harness.gateway.channel;


import org.frameworkset.spi.ai.model.ChatObject;
import org.frameworkset.spi.ai.model.LinkedMessageMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Normalized inbound message produced by a {@link Channel} adapter before routing. Carries all
 * identity and context metadata needed by {@link ChannelRouter} to resolve an agent and build a
 * stable session key.
 *
 * <p>Two identity fields are deliberately separate:
 * <ul>
 *   <li>{@link #peer} — the <em>conversation</em> identifier used to construct the session key
 *       (DM user id, group chat id, channel id)
 *   <li>{@link #senderId} — the <em>message sender's</em> identity, used as {@code userId} for
 *       HarnessAgent namespace isolation. In DM scenarios these are equal; in group/channel
 *       scenarios {@code peer.id} is the group and {@code senderId} is the individual author.
 * </ul>
 *
 * <p>{@link #runtimeContext} is <em>not</em> used for routing and does not participate in session
 * key computation. It is forwarded to the Gateway as the caller merge base for the agent turn.
 */
public class InboundMessage {
	
	private String channelId;
	private String accountId;
	private Peer peer;
	private String senderId;
	private Peer parentPeer;
	private String guild;
	private String team;
	private Set<String> roles = Collections.emptySet();
	private List<LinkedMessageMap<String, Object>> messages = Collections.emptyList();
	private String preferredAgentId;
	private ChatObject runtimeContext;
	
	/** No-arg constructor required for JavaBean conventions. */
	public InboundMessage() {
	}
	
	/**
	 * Full constructor.
	 *
	 * @param channelId identifier of the originating channel (e.g. {@code "chatui"}, {@code
	 *     "slack"})
	 * @param accountId optional multi-account identifier (e.g. Slack app installation id); null for
	 *     single-account channels
	 * @param peer the conversation peer (DM partner, channel, group, or thread) — used for session
	 *     key
	 * @param senderId optional sender identity for user-level isolation; if null, {@link #peer}'s
	 *     id is used as fallback in DM contexts
	 * @param parentPeer optional parent peer for thread-nested messages; used for {@code
	 *     binding.peer.parent} inheritance
	 * @param guild optional guild / server / workspace id (Discord guild, Slack workspace)
	 * @param team optional team id (Slack team, MS Teams team)
	 * @param roles optional set of role ids the sender holds within the guild; used for {@code
	 *     binding.guild+roles} tier evaluation
	 * @param messages the actual message content to forward to the agent
	 * @param preferredAgentId optional explicit agent override evaluated by {@link ChannelRouter}
	 *     as a tier-0 short-circuit before any binding tier. Set this when the caller already knows
	 *     which agent should handle the request (e.g. a path-mapped Web UI where the user clicked
	 *     an agent card); the router still builds session and outbound context normally so
	 *     bindings continue to control {@code sessionScope} and outbound addressing. {@code null}
	 *     for normal binding-driven routing.
	 * @param runtimeContext optional caller-supplied {@link ChatObject} merged into the agent
	 *     turn; {@code null} when none. Not part of routing / session identity.
	 */
	public InboundMessage(
			String channelId,
			String accountId,
			Peer peer,
			String senderId,
			Peer parentPeer,
			String guild,
			String team,
			Set<String> roles,
			List<LinkedMessageMap<String, Object>> messages,
			String preferredAgentId,
			ChatObject runtimeContext) {
		setChannelId(channelId);
		this.accountId = accountId;
		setPeer(peer);
		this.senderId = senderId;
		this.parentPeer = parentPeer;
		this.guild = guild;
		this.team = team;
		setRoles(roles);
		setMessages(messages);
		this.preferredAgentId = preferredAgentId;
		this.runtimeContext = runtimeContext;
	}
	
	/**
	 * Single-turn DM with no guild/team/role metadata (typical for direct chat UI use).
	 *
	 * <p>In DM context {@code senderId} equals {@code peerId} — the conversation partner is also
	 * the message author.
	 */
	public static InboundMessage dm(String channelId, String peerId, List<LinkedMessageMap<String, Object>> messages) {
		return new InboundMessage(
				channelId,
				null,
				Peer.direct(peerId),
				peerId,
				null,
				null,
				null,
				Collections.<String>emptySet(),
				copyList(messages),
				null,
				null);
	}
	
	/**
	 * Single-turn DM with an explicit {@link #preferredAgentId} override — the router will use the
	 * supplied {@code agentId} as a tier-0 short-circuit instead of evaluating bindings.
	 */
	public static InboundMessage dmFor(
			String channelId, String peerId, String agentId, List<LinkedMessageMap<String, Object>> messages) {
		Objects.requireNonNull(agentId, "agentId");
		return new InboundMessage(
				channelId,
				null,
				Peer.direct(peerId),
				peerId,
				null,
				null,
				null,
				Collections.<String>emptySet(),
				copyList(messages),
				agentId,
				null);
	}
	
	/**
	 * Channel / group message with optional guild context. The {@code senderId} identifies the
	 * individual author separately from the {@code roomId} conversation peer.
	 */
	public static InboundMessage channel(
			String channelId, String roomId, String senderId, String guild, List<LinkedMessageMap<String, Object>> messages) {
		return new InboundMessage(
				channelId,
				null,
				Peer.channel(roomId),
				senderId,
				null,
				guild,
				null,
				Collections.<String>emptySet(),
				copyList(messages),
				null,
				null);
	}
	
	/**
	 * Channel message with optional guild context (no explicit senderId — sender identity unknown,
	 * falls back to room peer for isolation).
	 */
	public static InboundMessage channel(
			String channelId, String roomId, String guild, List<LinkedMessageMap<String, Object>> messages) {
		return new InboundMessage(
				channelId,
				null,
				Peer.channel(roomId),
				null,
				null,
				guild,
				null,
				Collections.<String>emptySet(),
				copyList(messages),
				null,
				null);
	}
	
	/** Whether this message originates from a direct / DM peer. */
	public boolean isDm() {
		return peer.getKind().isDirect();
	}
	
	/** Whether this message is a thread-nested reply. */
	public boolean isThread() {
		return peer.getKind().isThread();
	}
	
	/** Returns a copy carrying the given {@link ChatObject}. */
	public InboundMessage withRuntimeContext(ChatObject runtimeContext) {
		return new InboundMessage(
				channelId,
				accountId,
				peer,
				senderId,
				parentPeer,
				guild,
				team,
				roles,
				messages,
				preferredAgentId,
				runtimeContext);
	}
	
	/** Returns a builder for constructing messages with all optional fields. */
	public static Builder builder(String channelId, Peer peer, List<LinkedMessageMap<String, Object>> messages) {
		return new Builder(channelId, peer, messages);
	}
	
	private static List<LinkedMessageMap<String, Object>> copyList(List<LinkedMessageMap<String, Object>> messages) {
		return Collections.unmodifiableList(new ArrayList<LinkedMessageMap<String, Object>>(messages));
	}
	
	public String getChannelId() {
		return channelId;
	}
	
	public void setChannelId(String channelId) {
		this.channelId = Objects.requireNonNull(channelId, "channelId");
	}
	
	public String getAccountId() {
		return accountId;
	}
	
	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}
	
	public Peer getPeer() {
		return peer;
	}
	
	public void setPeer(Peer peer) {
		this.peer = Objects.requireNonNull(peer, "peer");
	}
	
	public String getSenderId() {
		return senderId;
	}
	
	public void setSenderId(String senderId) {
		this.senderId = senderId;
	}
	
	public Peer getParentPeer() {
		return parentPeer;
	}
	
	public void setParentPeer(Peer parentPeer) {
		this.parentPeer = parentPeer;
	}
	
	public String getGuild() {
		return guild;
	}
	
	public void setGuild(String guild) {
		this.guild = guild;
	}
	
	public String getTeam() {
		return team;
	}
	
	public void setTeam(String team) {
		this.team = team;
	}
	
	public Set<String> getRoles() {
		return roles;
	}
	
	public void setRoles(Set<String> roles) {
		if (roles != null) {
			this.roles = Collections.unmodifiableSet(new LinkedHashSet<String>(roles));
		} else {
			this.roles = Collections.emptySet();
		}
	}
	
	public List<LinkedMessageMap<String, Object>> getMessages() {
		return messages;
	}
	
	public void setMessages(List<LinkedMessageMap<String, Object>> messages) {
		this.messages =
				messages != null
						? Collections.unmodifiableList(new ArrayList<LinkedMessageMap<String, Object>>(messages))
						: Collections.<LinkedMessageMap<String, Object>>emptyList();
	}
	
	public String getPreferredAgentId() {
		return preferredAgentId;
	}
	
	public void setPreferredAgentId(String preferredAgentId) {
		this.preferredAgentId = preferredAgentId;
	}
	
	public ChatObject getRuntimeContext() {
		return runtimeContext;
	}
	
	public void setRuntimeContext(ChatObject runtimeContext) {
		this.runtimeContext = runtimeContext;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof InboundMessage)) {
			return false;
		}
		InboundMessage that = (InboundMessage) o;
		return Objects.equals(channelId, that.channelId)
				&& Objects.equals(accountId, that.accountId)
				&& Objects.equals(peer, that.peer)
				&& Objects.equals(senderId, that.senderId)
				&& Objects.equals(parentPeer, that.parentPeer)
				&& Objects.equals(guild, that.guild)
				&& Objects.equals(team, that.team)
				&& Objects.equals(roles, that.roles)
				&& Objects.equals(messages, that.messages)
				&& Objects.equals(preferredAgentId, that.preferredAgentId)
				&& Objects.equals(runtimeContext, that.runtimeContext);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(
				channelId,
				accountId,
				peer,
				senderId,
				parentPeer,
				guild,
				team,
				roles,
				messages,
				preferredAgentId,
				runtimeContext);
	}
	
	@Override
	public String toString() {
		return "InboundMessage{"
				+ "channelId='"
				+ channelId
				+ '\''
				+ ", accountId='"
				+ accountId
				+ '\''
				+ ", peer="
				+ peer
				+ ", senderId='"
				+ senderId
				+ '\''
				+ ", parentPeer="
				+ parentPeer
				+ ", guild='"
				+ guild
				+ '\''
				+ ", team='"
				+ team
				+ '\''
				+ ", roles="
				+ roles
				+ ", messages="
				+ messages
				+ ", preferredAgentId='"
				+ preferredAgentId
				+ '\''
				+ ", runtimeContext="
				+ runtimeContext
				+ '}';
	}
	
	public static final class Builder {
		private final String channelId;
		private final Peer peer;
		private final List<LinkedMessageMap<String, Object>> messages;
		private String accountId;
		private String senderId;
		private Peer parentPeer;
		private String guild;
		private String team;
		private Set<String> roles = Collections.emptySet();
		private String preferredAgentId;
		private ChatObject runtimeContext;
		
		private Builder(String channelId, Peer peer, List<LinkedMessageMap<String, Object>> messages) {
			this.channelId = channelId;
			this.peer = peer;
			this.messages = messages;
		}
		
		public Builder accountId(String accountId) {
			this.accountId = accountId;
			return this;
		}
		
		public Builder senderId(String senderId) {
			this.senderId = senderId;
			return this;
		}
		
		public Builder parentPeer(Peer parentPeer) {
			this.parentPeer = parentPeer;
			return this;
		}
		
		public Builder guild(String guild) {
			this.guild = guild;
			return this;
		}
		
		public Builder team(String team) {
			this.team = team;
			return this;
		}
		
		public Builder roles(Set<String> roles) {
			this.roles = roles != null ? roles : Collections.<String>emptySet();
			return this;
		}
		
		public Builder preferredAgentId(String preferredAgentId) {
			this.preferredAgentId = preferredAgentId;
			return this;
		}
		
		public Builder runtimeContext(ChatObject runtimeContext) {
			this.runtimeContext = runtimeContext;
			return this;
		}
		
		public InboundMessage build() {
			return new InboundMessage(
					channelId,
					accountId,
					peer,
					senderId,
					parentPeer,
					guild,
					team,
					roles,
					messages,
					preferredAgentId,
					runtimeContext);
		}
	}
}