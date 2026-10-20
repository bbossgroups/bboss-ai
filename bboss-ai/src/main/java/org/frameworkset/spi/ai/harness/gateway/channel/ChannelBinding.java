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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * One routing rule that maps a set of inbound-message conditions to a target {@code agentId}.
 *
 * <p>A binding is evaluated by {@link ChannelRouter} at the tier corresponding to the most
 * specific non-null condition field it carries (priority order):
 *
 * <ol>
 *   <li>{@link #peer} — exact peer-key match ({@code "direct:u_123"} or {@code "channel:c_help"})
 *   <li>{@link #parentPeer} — parent-peer key match (thread-parent inheritance)
 *   <li>{@link #guild} + {@link #roles} — guild membership AND at least one role matches
 *   <li>{@link #guild} alone — guild membership (no role constraint)
 *   <li>{@link #team} — team id match
 *   <li>{@link #account} — account id match
 *   <li>{@link #channel} — channel id match
 * </ol>
 *
 * <p>Within a tier, the first binding in {@link ChannelConfig#getBindings()} list order that matches
 * wins. Only one condition tier is active per binding: the most specific non-null field determines
 * the tier.
 *
 * <p>The optional {@link #sessionScope} field provides a per-binding session override: when non-null
 * it takes precedence over the channel-level {@link ChannelConfig#getDmScope()} for DM session key
 * construction.
 */
public class ChannelBinding {
	
	private String agentId;
	private String peer;
	private String parentPeer;
	private String guild;
	private Set<String> roles = Collections.emptySet();
	private String team;
	private String account;
	private String channel;
	private DmScope sessionScope;
	
	/** No-arg constructor required for JavaBean conventions. */
	public ChannelBinding() {
	}
	
	/**
	 * Full constructor.
	 *
	 * @param agentId the target agent id when this binding matches (required)
	 * @param peer exact peer key to match (e.g. {@code "direct:u_42"}); highest priority
	 * @param parentPeer parent-peer key for thread-parent inheritance (e.g. {@code "channel:c_ops"})
	 * @param guild guild/server/workspace id to match
	 * @param roles role ids to match within the guild (evaluated alongside {@code guild}); if
	 *     non-empty the binding is a guild+roles tier binding; if empty it is a guild-only tier
	 *     binding
	 * @param team team id to match
	 * @param account account id to match
	 * @param channel channel id to match (lower priority; rarely needed inside channel-specific
	 *     config)
	 * @param sessionScope optional per-binding DM session scope override; null means inherit from
	 *     {@link ChannelConfig#getDmScope()}
	 */
	public ChannelBinding(
			String agentId,
			String peer,
			String parentPeer,
			String guild,
			Set<String> roles,
			String team,
			String account,
			String channel,
			DmScope sessionScope) {
		setAgentId(agentId);
		this.peer = peer;
		this.parentPeer = parentPeer;
		this.guild = guild;
		setRoles(roles);
		this.team = team;
		this.account = account;
		this.channel = channel;
		this.sessionScope = sessionScope;
	}
	
	/** Matches an exact peer (e.g. a specific DM user or channel id). */
	public static ChannelBinding forPeer(String peer, String agentId) {
		return new ChannelBinding(
				agentId, peer, null, null, Collections.<String>emptySet(), null, null, null, null);
	}
	
	/** Matches messages that are threads under a specific parent peer. */
	public static ChannelBinding forParentPeer(String parentPeer, String agentId) {
		return new ChannelBinding(
				agentId, null, parentPeer, null, Collections.<String>emptySet(), null, null, null, null);
	}
	
	/** Matches any message from a specific guild (server/workspace). */
	public static ChannelBinding forGuild(String guild, String agentId) {
		return new ChannelBinding(
				agentId, null, null, guild, Collections.<String>emptySet(), null, null, null, null);
	}
	
	/** Matches messages from a guild where the sender holds at least one of the given roles. */
	public static ChannelBinding forGuildRoles(String guild, Set<String> roles, String agentId) {
		return new ChannelBinding(agentId, null, null, guild, roles, null, null, null, null);
	}
	
	/** Matches a specific team id. */
	public static ChannelBinding forTeam(String team, String agentId) {
		return new ChannelBinding(
				agentId, null, null, null, Collections.<String>emptySet(), team, null, null, null);
	}
	
	/** Matches a specific account id. */
	public static ChannelBinding forAccount(String account, String agentId) {
		return new ChannelBinding(
				agentId, null, null, null, Collections.<String>emptySet(), null, account, null, null);
	}
	
	/** Matches a specific channel id. */
	public static ChannelBinding forChannel(String channel, String agentId) {
		return new ChannelBinding(
				agentId, null, null, null, Collections.<String>emptySet(), null, null, channel, null);
	}
	
	/** Returns a copy of this binding with a per-binding session scope override. */
	public ChannelBinding withSessionScope(DmScope scope) {
		return new ChannelBinding(
				agentId, peer, parentPeer, guild, roles, team, account, channel, scope);
	}
	
	public String getAgentId() {
		return agentId;
	}
	
	public void setAgentId(String agentId) {
		this.agentId = Objects.requireNonNull(agentId, "agentId");
	}
	
	public String getPeer() {
		return peer;
	}
	
	public void setPeer(String peer) {
		this.peer = peer;
	}
	
	public String getParentPeer() {
		return parentPeer;
	}
	
	public void setParentPeer(String parentPeer) {
		this.parentPeer = parentPeer;
	}
	
	public String getGuild() {
		return guild;
	}
	
	public void setGuild(String guild) {
		this.guild = guild;
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
	
	public String getTeam() {
		return team;
	}
	
	public void setTeam(String team) {
		this.team = team;
	}
	
	public String getAccount() {
		return account;
	}
	
	public void setAccount(String account) {
		this.account = account;
	}
	
	public String getChannel() {
		return channel;
	}
	
	public void setChannel(String channel) {
		this.channel = channel;
	}
	
	public DmScope getSessionScope() {
		return sessionScope;
	}
	
	public void setSessionScope(DmScope sessionScope) {
		this.sessionScope = sessionScope;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ChannelBinding)) {
			return false;
		}
		ChannelBinding that = (ChannelBinding) o;
		return Objects.equals(agentId, that.agentId)
				&& Objects.equals(peer, that.peer)
				&& Objects.equals(parentPeer, that.parentPeer)
				&& Objects.equals(guild, that.guild)
				&& Objects.equals(roles, that.roles)
				&& Objects.equals(team, that.team)
				&& Objects.equals(account, that.account)
				&& Objects.equals(channel, that.channel)
				&& sessionScope == that.sessionScope;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(
				agentId, peer, parentPeer, guild, roles, team, account, channel, sessionScope);
	}
	
	@Override
	public String toString() {
		return "ChannelBinding{"
				+ "agentId='"
				+ agentId
				+ '\''
				+ ", peer='"
				+ peer
				+ '\''
				+ ", parentPeer='"
				+ parentPeer
				+ '\''
				+ ", guild='"
				+ guild
				+ '\''
				+ ", roles="
				+ roles
				+ ", team='"
				+ team
				+ '\''
				+ ", account='"
				+ account
				+ '\''
				+ ", channel='"
				+ channel
				+ '\''
				+ ", sessionScope="
				+ sessionScope
				+ '}';
	}
}