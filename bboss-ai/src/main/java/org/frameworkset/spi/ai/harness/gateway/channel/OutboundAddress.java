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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Delivery target for outbound (proactive) messages. Constructed by {@link ChannelRouter} during
 * inbound routing and stored by the gateway as the session's "last route" so that proactive replies
 * (e.g. subagent completion announces) can be delivered back to the correct channel and peer.
 */
public class OutboundAddress {
	
	private String channelId;
	private String accountId;
	private String to;
	private String threadId;
	
	/** No-arg constructor required for JavaBean conventions. */
	public OutboundAddress() {
	}
	
	/**
	 * Full constructor.
	 *
	 * @param channelId the channel adapter to deliver through (e.g. {@code "chatui"}, {@code
	 *     "slack"})
	 * @param accountId optional multi-account identifier (nullable for single-account channels)
	 * @param to delivery address in {@code "channelId:peerKind:peerId"} format (e.g. {@code
	 *     "dingtalk:GROUP:cidXXX"})
	 * @param threadId optional thread context for threaded replies (nullable)
	 */
	public OutboundAddress(String channelId, String accountId, String to, String threadId) {
		setChannelId(channelId);
		this.accountId = accountId;
		setTo(to);
		this.threadId = threadId;
	}
	
	/** Creates an address for a direct (non-threaded) message. */
	public static OutboundAddress direct(String channelId, String to) {
		return new OutboundAddress(channelId, null, to, null);
	}
	
	/** Creates an address with account context. */
	public static OutboundAddress withAccount(String channelId, String accountId, String to) {
		return new OutboundAddress(channelId, accountId, to, null);
	}
	
	/** Serializes this address to a flat map for {@code BaseStore} persistence. */
	public Map<String, Object> toMap() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("channelId", channelId);
		if (accountId != null) {
			m.put("accountId", accountId);
		}
		m.put("to", to);
		if (threadId != null) {
			m.put("threadId", threadId);
		}
		return m;
	}
	
	/** Reconstructs an address from a persisted map, tolerating missing optional fields. */
	public static OutboundAddress fromMap(Map<String, Object> m) {
		if (m == null) {
			return null;
		}
		Object channelId = m.get("channelId");
		Object to = m.get("to");
		if (channelId == null || to == null) {
			return null;
		}
		Object accountId = m.get("accountId");
		Object threadId = m.get("threadId");
		return new OutboundAddress(
				String.valueOf(channelId),
				accountId != null ? String.valueOf(accountId) : null,
				String.valueOf(to),
				threadId != null ? String.valueOf(threadId) : null);
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
	
	public String getTo() {
		return to;
	}
	
	public void setTo(String to) {
		this.to = Objects.requireNonNull(to, "to");
	}
	
	public String getThreadId() {
		return threadId;
	}
	
	public void setThreadId(String threadId) {
		this.threadId = threadId;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof OutboundAddress)) {
			return false;
		}
		OutboundAddress that = (OutboundAddress) o;
		return Objects.equals(channelId, that.channelId)
				&& Objects.equals(accountId, that.accountId)
				&& Objects.equals(to, that.to)
				&& Objects.equals(threadId, that.threadId);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(channelId, accountId, to, threadId);
	}
	
	@Override
	public String toString() {
		return "OutboundAddress{"
				+ "channelId='"
				+ channelId
				+ '\''
				+ ", accountId='"
				+ accountId
				+ '\''
				+ ", to='"
				+ to
				+ '\''
				+ ", threadId='"
				+ threadId
				+ '\''
				+ '}';
	}
}