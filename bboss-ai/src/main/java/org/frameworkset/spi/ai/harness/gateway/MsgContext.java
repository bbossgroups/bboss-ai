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
package org.frameworkset.spi.ai.harness.gateway;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Routing context for inbound turns (direct API, channel adapter, group/room/thread). Used by the
 * gateway to map stable conversation keys to session ids.
 *
 * <p>The {@link #userId} field carries the message sender's identity for multi-tenant namespace
 * isolation in {@link org.frameworkset.spi.ai.AIAgent}. It is derived from {@link
 * org.frameworkset.spi.ai.harness.gateway.channel.InboundMessage#getSenderId()} and does <em>not</em>
 * participate in {@link #canonicalKey()} computation — the same user's conversations always map to
 * the same session key regardless of how userId is set.
 */
public class MsgContext {
	
	private String channel;
	private String group;
	private String room;
	private String threadId;
	private String threadTs;
	private Map<String, String> extra = Collections.emptyMap();
	private String userId;
	
	/** No-arg constructor required for JavaBean conventions. */
	public MsgContext() {
	}
	
	/**
	 * Full constructor.
	 *
	 * @param channel logical channel name (e.g. slack, discord, web)
	 * @param group optional group / team / workspace id
	 * @param room optional room / channel id
	 * @param threadId optional thread / topic id
	 * @param threadTs optional provider-specific thread timestamp or message anchor
	 * @param extra additional key/value pairs for adapters
	 * @param userId optional authenticated user identity for HarnessAgent namespace isolation
	 */
	public MsgContext(
			String channel,
			String group,
			String room,
			String threadId,
			String threadTs,
			Map<String, String> extra,
			String userId) {
		this.channel = channel;
		this.group = group;
		this.room = room;
		this.threadId = threadId;
		this.threadTs = threadTs;
		setExtra(extra);
		this.userId = userId;
	}
	
	/**
	 * Convenience constructor without {@code userId} (backwards-compatible for callsites that do
	 * not carry user identity).
	 */
	public MsgContext(
			String channel,
			String group,
			String room,
			String threadId,
			String threadTs,
			Map<String, String> extra) {
		this(channel, group, room, threadId, threadTs, extra, null);
	}
	
	/** Default single-conversation context (no channel metadata, no userId). */
	public static MsgContext defaultContext() {
		return new MsgContext(
				"default", null, null, null, null, Collections.<String, String>emptyMap(), null);
	}
	
	/** Returns a copy of this context with the given {@code userId} set. */
	public MsgContext withUserId(String userId) {
		return new MsgContext(channel, group, room, threadId, threadTs, extra, userId);
	}
	
	/**
	 * Stable key for session routing: same logical conversation maps to the same gateway session
	 * id.
	 */
	public String canonicalKey() {
		StringBuilder sb = new StringBuilder(64);
		sb.append(channel != null ? channel : "default");
		if (group != null && !group.trim().isEmpty()) {
			sb.append("|g:").append(group.trim());
		}
		if (room != null && !room.trim().isEmpty()) {
			sb.append("|r:").append(room.trim());
		}
		if (threadId != null && !threadId.trim().isEmpty()) {
			sb.append("|t:").append(threadId.trim());
		}
		if (threadTs != null && !threadTs.trim().isEmpty()) {
			sb.append("|ts:").append(threadTs.trim());
		}
		if (!extra.isEmpty()) {
			java.util.List<String> keys = new java.util.ArrayList<String>(extra.keySet());
			Collections.sort(keys);
			for (String key : keys) {
				sb.append("|x:").append(key).append('=').append(extra.get(key));
			}
		}
		return sb.toString();
	}
	
	public String getChannel() {
		return channel;
	}
	
	public void setChannel(String channel) {
		this.channel = channel;
	}
	
	public String getGroup() {
		return group;
	}
	
	public void setGroup(String group) {
		this.group = group;
	}
	
	public String getRoom() {
		return room;
	}
	
	public void setRoom(String room) {
		this.room = room;
	}
	
	public String getThreadId() {
		return threadId;
	}
	
	public void setThreadId(String threadId) {
		this.threadId = threadId;
	}
	
	public String getThreadTs() {
		return threadTs;
	}
	
	public void setThreadTs(String threadTs) {
		this.threadTs = threadTs;
	}
	
	public Map<String, String> getExtra() {
		return extra;
	}
	
	public void setExtra(Map<String, String> extra) {
		if (extra != null) {
			this.extra =
					Collections.unmodifiableMap(new LinkedHashMap<String, String>(extra));
		} else {
			this.extra = Collections.emptyMap();
		}
	}
	
	public String getUserId() {
		return userId;
	}
	
	public void setUserId(String userId) {
		this.userId = userId;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof MsgContext)) {
			return false;
		}
		MsgContext that = (MsgContext) o;
		return Objects.equals(channel, that.channel)
				&& Objects.equals(group, that.group)
				&& Objects.equals(room, that.room)
				&& Objects.equals(threadId, that.threadId)
				&& Objects.equals(threadTs, that.threadTs)
				&& Objects.equals(extra, that.extra)
				&& Objects.equals(userId, that.userId);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(channel, group, room, threadId, threadTs, extra, userId);
	}
	
	@Override
	public String toString() {
		return "MsgContext{"
				+ "channel='"
				+ channel
				+ '\''
				+ ", group='"
				+ group
				+ '\''
				+ ", room='"
				+ room
				+ '\''
				+ ", threadId='"
				+ threadId
				+ '\''
				+ ", threadTs='"
				+ threadTs
				+ '\''
				+ ", extra="
				+ extra
				+ ", userId='"
				+ userId
				+ '\''
				+ '}';
	}
}