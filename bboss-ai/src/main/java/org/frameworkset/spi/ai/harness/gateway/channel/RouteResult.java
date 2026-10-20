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

import java.util.Objects;

/**
 * Output of {@link ChannelRouter#resolveRoute}: the resolved agent id, the {@link MsgContext} to
 * pass to {link Gateway#run}, a diagnostic {@code matchedBy}
 * label indicating which binding tier (or fallback) produced the result, and the {@link
 * OutboundAddress} for delivering replies back to the originating channel/peer.
 */
public class RouteResult {
	
	private String agentId;
	private MsgContext context;
	private String matchedBy;
	private OutboundAddress outboundAddress;
	
	/** No-arg constructor required for JavaBean conventions. */
	public RouteResult() {
	}
	
	/**
	 * Full constructor.
	 *
	 * @param agentId the resolved target agent id
	 * @param context the routing context (channel, session scope keys, agentId extra) ready for
	 *     gateway execution
	 * @param matchedBy human-readable label of the binding tier that matched (e.g. {@code "peer"},
	 *     {@code "guild+roles"}, {@code "default"}); useful for debugging and logging
	 * @param outboundAddress delivery target for proactive replies (e.g. subagent announces);
	 *     derived from the inbound message's channel and peer metadata
	 */
	public RouteResult(
			String agentId, MsgContext context, String matchedBy, OutboundAddress outboundAddress) {
		this.agentId = agentId;
		this.context = context;
		this.matchedBy = matchedBy;
		this.outboundAddress = outboundAddress;
	}
	
	public String getAgentId() {
		return agentId;
	}
	
	public void setAgentId(String agentId) {
		this.agentId = agentId;
	}
	
	public MsgContext getContext() {
		return context;
	}
	
	public void setContext(MsgContext context) {
		this.context = context;
	}
	
	public String getMatchedBy() {
		return matchedBy;
	}
	
	public void setMatchedBy(String matchedBy) {
		this.matchedBy = matchedBy;
	}
	
	public OutboundAddress getOutboundAddress() {
		return outboundAddress;
	}
	
	public void setOutboundAddress(OutboundAddress outboundAddress) {
		this.outboundAddress = outboundAddress;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof RouteResult)) {
			return false;
		}
		RouteResult that = (RouteResult) o;
		return Objects.equals(agentId, that.agentId)
				&& Objects.equals(context, that.context)
				&& Objects.equals(matchedBy, that.matchedBy)
				&& Objects.equals(outboundAddress, that.outboundAddress);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(agentId, context, matchedBy, outboundAddress);
	}
	
	@Override
	public String toString() {
		return "RouteResult{"
				+ "agentId='"
				+ agentId
				+ '\''
				+ ", context="
				+ context
				+ ", matchedBy='"
				+ matchedBy
				+ '\''
				+ ", outboundAddress="
				+ outboundAddress
				+ '}';
	}
}