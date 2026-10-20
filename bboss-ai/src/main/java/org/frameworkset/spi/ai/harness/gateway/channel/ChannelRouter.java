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

import org.frameworkset.spi.ai.harness.gateway.MsgContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Resolves the target {@code agentId} and stable {@link MsgContext} for an {@link InboundMessage}
 * by evaluating {@link ChannelBinding} rules in deterministic priority order.
 *
 * <h2>Binding evaluation tiers (highest to lowest priority)</h2>
 *
 * <ol>
 *   <li><b>explicit</b> — {@link InboundMessage#getPreferredAgentId()} ()} short-circuit when the caller
 *       has already nominated a specific agent. Bindings are still consulted to determine
 *       {@code sessionScope} and outbound addressing.
 *   <li><b>peer</b> — exact {@link Peer#key()} match (e.g. {@code "direct:u_42"})
 *   <li><b>peer.parent</b> — exact match on the thread-parent peer's key
 *   <li><b>guild + roles</b> — guild id matches AND sender holds at least one of the binding's
 *       roles
 *   <li><b>guild</b> — guild id matches (no role constraint)
 *   <li><b>team</b> — team id matches
 *   <li><b>account</b> — account id matches
 *   <li><b>channel</b> — channel id matches
 *   <li><b>default</b> — {@link ChannelConfig#getDefaultAgentId()} ()} or {@code globalDefaultAgentId}
 * </ol>
 *
 * <p>Within each tier the first binding in {@link ChannelConfig#getBindings()} ()} list order that
 * matches wins.
 *
 * <h2>Session key construction ({@link MsgContext})</h2>
 *
 * After resolving {@code agentId}, the router builds a {@link MsgContext} whose {@link
 * MsgContext#canonicalKey()} produces a stable session key:
 *
 * <ul>
 *   <li>DM + {@link DmScope#MAIN} — {@code channel} field only; all DMs share one session
 *   <li>DM + other scopes — room = peerId (optionally group = accountId)
 *   <li>Thread — room = parentPeer id, threadId = peer id
 *   <li>Non-DM channel/group — room = peer id, group = guild
 * </ul>
 *
 * The resolved {@code agentId} is always included in {link MsgContext#extra()} under key {@code
 * "agentId"} so that the gateway can pick the correct agent from its registry.
 */
public final class ChannelRouter {
	
	private static final List<String> TIERS;
	
	static {
		List<String> tiers = new ArrayList<String>(7);
		tiers.add("peer");
		tiers.add("parentPeer");
		tiers.add("guildRoles");
		tiers.add("guild");
		tiers.add("team");
		tiers.add("account");
		tiers.add("channel");
		TIERS = Collections.unmodifiableList(tiers);
	}
	
	private final String globalDefaultAgentId;
	
	/**
	 * @param globalDefaultAgentId fallback agent id when no binding and no channel-level default
	 *     match; typically the id of the agent registered via
	 *     {link Gateway#bindMainAgent}
	 */
	public ChannelRouter(String globalDefaultAgentId) {
		this.globalDefaultAgentId = globalDefaultAgentId != null ? globalDefaultAgentId : "main";
	}
	
	/**
	 * Evaluates bindings in priority order and returns a {@link RouteResult} ready for gateway
	 * execution.
	 *
	 * @param config channel-level routing config (bindings + dmScope + channel default agent)
	 * @param msg normalized inbound message
	 */
	public RouteResult resolveRoute(ChannelConfig config, InboundMessage msg) {
		Objects.requireNonNull(config, "config");
		Objects.requireNonNull(msg, "msg");
		
		String agentId;
		String matchedBy;
		String explicit = msg.getPreferredAgentId();
		ChannelBinding matchedBinding = findMatchedBinding(config.getBindings(), msg);
		
		if (explicit != null && !explicit.trim().isEmpty()) {
			agentId = explicit;
			matchedBy = "explicit";
		} else if (matchedBinding != null) {
			agentId = matchedBinding.getAgentId();
			matchedBy = detectMatchedTier(config.getBindings(), msg);
		} else if (config.getDefaultAgentId() != null) {
			agentId = config.getDefaultAgentId();
			matchedBy = "channel-default";
		} else {
			agentId = globalDefaultAgentId;
			matchedBy = "global-default";
		}
		
		DmScope effectiveScope =
				matchedBinding != null && matchedBinding.getSessionScope() != null
						? matchedBinding.getSessionScope()
						: config.getDmScope();
		
		String userId =
				msg.getSenderId() != null
						? msg.getSenderId()
						: (msg.getPeer().getKind().isDirect() ? msg.getPeer().getId() : null);
		
		MsgContext context = buildContext(msg, effectiveScope, agentId, userId);
		OutboundAddress outbound = buildOutboundAddress(msg);
		return new RouteResult(agentId, context, matchedBy, outbound);
	}
	
	// -----------------------------------------------------------------
	//  Binding evaluation - 7 tiers
	// -----------------------------------------------------------------
	
	private ChannelBinding findMatchedBinding(List<ChannelBinding> bindings, InboundMessage msg) {
		for (String tier : TIERS) {
			ChannelBinding b = findFirstBinding(bindings, msg, tier);
			if (b != null) {
				return b;
			}
		}
		return null;
	}
	
	private String detectMatchedTier(List<ChannelBinding> bindings, InboundMessage msg) {
		for (String tier : TIERS) {
			if (findFirstBinding(bindings, msg, tier) != null) {
				return "guildRoles".equals(tier) ? "guild+roles" : tier;
			}
		}
		return "none";
	}
	
	private ChannelBinding findFirstBinding(
			List<ChannelBinding> bindings, InboundMessage msg, String tier) {
		for (ChannelBinding b : bindings) {
			if (matches(b, msg, tier)) {
				return b;
			}
		}
		return null;
	}
	
	private boolean matches(ChannelBinding b, InboundMessage msg, String tier) {
		switch (tier) {
			case "peer":
				return b.getPeer() != null && b.getPeer().equals(msg.getPeer().key());
			case "parentPeer":
				return b.getParentPeer() != null
						&& msg.getParentPeer() != null
						&& b.getParentPeer().equals(msg.getParentPeer().key());
			case "guildRoles":
				return b.getGuild() != null
						&& !b.getRoles().isEmpty()
						&& b.getGuild().equals(msg.getGuild())
						&& hasAnyRoleMatch(msg.getRoles(), b.getRoles());
			case "guild":
				return b.getGuild() != null
						&& b.getRoles().isEmpty()
						&& b.getGuild().equals(msg.getGuild());
			case "team":
				return b.getTeam() != null && b.getTeam().equals(msg.getTeam());
			case "account":
				return b.getAccount() != null && b.getAccount().equals(msg.getAccountId());
			case "channel":
				return b.getChannel() != null && b.getChannel().equals(msg.getChannelId());
			default:
				return false;
		}
	}
	
	private static boolean hasAnyRoleMatch(
			java.util.Collection<String> messageRoles,
			java.util.Set<String> bindingRoles) {
		if (messageRoles == null || messageRoles.isEmpty()) {
			return false;
		}
		for (String role : messageRoles) {
			if (bindingRoles.contains(role)) {
				return true;
			}
		}
		return false;
	}
	
	// -----------------------------------------------------------------
	//  MsgContext construction from routing result + dmScope
	// -----------------------------------------------------------------
	
	private MsgContext buildContext(
			InboundMessage msg, DmScope dmScope, String agentId, String userId) {
		Map<String, String> extra = new HashMap<String, String>(1);
		extra.put("agentId", agentId);
		extra = Collections.unmodifiableMap(extra);
		
		String channel = msg.getChannelId();
		Peer peer = msg.getPeer();
		
		MsgContext ctx;
		if (peer.getKind().isThread()) {
			String parentRoom = msg.getParentPeer() != null ? msg.getParentPeer().getId() : null;
			ctx = new MsgContext(channel, msg.getGuild(), parentRoom, peer.getId(), null, extra);
		} else if (peer.getKind().isDirect()) {
			ctx = buildDmContext(channel, peer.getId(), msg.getAccountId(), dmScope, extra);
		} else {
			ctx = new MsgContext(channel, msg.getGuild(), peer.getId(), null, null, extra);
		}
		
		return userId != null ? ctx.withUserId(userId) : ctx;
	}
	
	private MsgContext buildDmContext(
			String channel,
			String peerId,
			String accountId,
			DmScope dmScope,
			Map<String, String> extra) {
		switch (dmScope) {
			case MAIN:
				return new MsgContext(channel, null, null, null, null, extra);
			case PER_PEER:
				return new MsgContext(channel, null, peerId, null, null, extra);
			case PER_CHANNEL_PEER:
				return new MsgContext(channel, null, peerId, null, null, extra);
			case PER_ACCOUNT_CHANNEL_PEER:
				return new MsgContext(channel, accountId, peerId, null, null, extra);
			default:
				return new MsgContext(channel, null, null, null, null, extra);
		}
	}
	
	// -----------------------------------------------------------------
	//  OutboundAddress construction
	// -----------------------------------------------------------------
	
	private OutboundAddress buildOutboundAddress(InboundMessage msg) {
		String to = msg.getChannelId() + ":" + msg.getPeer().getKind().name() + ":" + msg.getPeer().getId();
		String threadId = msg.getPeer().getKind().isThread() ? msg.getPeer().getId() : null;
		return new OutboundAddress(msg.getChannelId(), msg.getAccountId(), to, threadId);
	}
}