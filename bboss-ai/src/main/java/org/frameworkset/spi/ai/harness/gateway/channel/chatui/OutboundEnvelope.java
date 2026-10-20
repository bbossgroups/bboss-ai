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

 
import org.frameworkset.spi.ai.harness.gateway.channel.OutboundAddress;
import org.frameworkset.spi.ai.model.LinkedMessageMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A buffered proactive outbound message delivered to {@link ChatUiChannel} by the gateway. Callers
 * retrieve these via {@link ChatUiChannel#pollOutbound()}.
 */
public final class OutboundEnvelope {
	
	private final OutboundAddress address;
	private final List<LinkedMessageMap<String, Object>> messages;
	private final long timestampMs;
	
	public OutboundEnvelope(OutboundAddress address, List<LinkedMessageMap<String, Object>> messages, long timestampMs) {
		this.address = address;
		this.messages = messages == null
				? Collections.<LinkedMessageMap<String, Object>>emptyList()
				: Collections.unmodifiableList(new ArrayList<LinkedMessageMap<String, Object>>(messages));
		this.timestampMs = timestampMs;
	}
	
	public OutboundEnvelope(OutboundAddress address, List<LinkedMessageMap<String, Object>> messages) {
		this(address, messages, System.currentTimeMillis());
	}
	
	public OutboundAddress getAddress() {
		return address;
	}
	
	public List<LinkedMessageMap<String, Object>> getMessages() {
		return messages;
	}
	
	public long getTimestampMs() {
		return timestampMs;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof OutboundEnvelope)) {
			return false;
		}
		OutboundEnvelope that = (OutboundEnvelope) o;
		return timestampMs == that.timestampMs
				&& Objects.equals(address, that.address)
				&& Objects.equals(messages, that.messages);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(address, messages, timestampMs);
	}
	
	@Override
	public String toString() {
		return "OutboundEnvelope{"
				+ "address=" + address
				+ ", messages=" + messages
				+ ", timestampMs=" + timestampMs
				+ '}';
	}
}