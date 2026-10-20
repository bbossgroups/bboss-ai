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
import org.frameworkset.spi.ai.model.ChatObject;

import java.util.Objects;

/**
 * Inputs available to a {@link ChannelRuntimeContextResolver} when resolving caller context for a
 * Gateway turn.
 *
 * <p>All fields are optional and may be {@code null}; the class intentionally performs no non-null
 * validation in order to preserve the record's original semantics.
 */
public class ChannelRuntimeContextRequest {
	
	private String channelId;
	private InboundMessage inboundMessage;
	private MsgContext msgContext;
	private OutboundAddress outboundAddress;
	private ChatObject callerContext;
	
	/** No-arg constructor required for JavaBean conventions. */
	public ChannelRuntimeContextRequest() {
	}
	
	/**
	 * Full constructor.
	 *
	 * @param channelId originating channel id (e.g. {@code "chatui"}, {@code "dingtalk"}); may be
	 *     null
	 * @param inboundMessage the normalized inbound envelope when the turn came through {@link
	 *     Channel#dispatch}; {@code null} for programmatic {@code SendOptions} paths that skip an
	 *     inbound envelope
	 * @param msgContext resolved routing context for this turn
	 * @param outboundAddress delivery target for replies; may be null
	 * @param callerContext context carried on {link SendOptions} or {@link
	 *     InboundMessage}; may be null
	 */
	public ChannelRuntimeContextRequest(
			String channelId,
			InboundMessage inboundMessage,
			MsgContext msgContext,
			OutboundAddress outboundAddress,
			ChatObject callerContext) {
		this.channelId = channelId;
		this.inboundMessage = inboundMessage;
		this.msgContext = msgContext;
		this.outboundAddress = outboundAddress;
		this.callerContext = callerContext;
	}
	
	public String getChannelId() {
		return channelId;
	}
	
	public void setChannelId(String channelId) {
		this.channelId = channelId;
	}
	
	public InboundMessage getInboundMessage() {
		return inboundMessage;
	}
	
	public void setInboundMessage(InboundMessage inboundMessage) {
		this.inboundMessage = inboundMessage;
	}
	
	public MsgContext getMsgContext() {
		return msgContext;
	}
	
	public void setMsgContext(MsgContext msgContext) {
		this.msgContext = msgContext;
	}
	
	public OutboundAddress getOutboundAddress() {
		return outboundAddress;
	}
	
	public void setOutboundAddress(OutboundAddress outboundAddress) {
		this.outboundAddress = outboundAddress;
	}
	
	public ChatObject getCallerContext() {
		return callerContext;
	}
	
	public void setCallerContext(ChatObject callerContext) {
		this.callerContext = callerContext;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ChannelRuntimeContextRequest)) {
			return false;
		}
		ChannelRuntimeContextRequest that = (ChannelRuntimeContextRequest) o;
		return Objects.equals(channelId, that.channelId)
				&& Objects.equals(inboundMessage, that.inboundMessage)
				&& Objects.equals(msgContext, that.msgContext)
				&& Objects.equals(outboundAddress, that.outboundAddress)
				&& Objects.equals(callerContext, that.callerContext);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(
				channelId, inboundMessage, msgContext, outboundAddress, callerContext);
	}
	
	@Override
	public String toString() {
		return "ChannelRuntimeContextRequest{"
				+ "channelId='"
				+ channelId
				+ '\''
				+ ", inboundMessage="
				+ inboundMessage
				+ ", msgContext="
				+ msgContext
				+ ", outboundAddress="
				+ outboundAddress
				+ ", callerContext="
				+ callerContext
				+ '}';
	}
}