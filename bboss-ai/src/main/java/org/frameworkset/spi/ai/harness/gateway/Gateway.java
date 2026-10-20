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

 
import org.frameworkset.spi.ai.AIAgent;
import org.frameworkset.spi.ai.harness.gateway.channel.InboundMessage;
import org.frameworkset.spi.ai.harness.gateway.channel.OutboundAddress;
import org.frameworkset.spi.ai.model.ChatObject;
import org.frameworkset.spi.ai.model.LinkedMessageMap;
import org.frameworkset.spi.ai.model.ServerEvent;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.List;

/**
 * Gateway-style entrypoint: route inbound turns by {@link MsgContext}, and bind the primary {@link
 * AIAgent} for announce delivery.
 *
 * <p>Implementations should serialize turns per logical session (fair wait) so injected announce
 * runs and user/channel {@link #run} calls do not race the same {@code AIAgent} turn.
 *
 * <p>Callers may supply a {@link ChatObject} that is merged into the agent turn. Gateway-owned
 * identity fields ({@code sessionId}, {@code userId}, {@code outboundAddress}, …) always win on
 * conflict.
 */
public interface Gateway {

    /**
     * Binds the primary harness agent after construction. The agent is registered under its
     * {@link AIAgent#getAgentId()} for routing by {@link #run}.
     */
    void bindMainAgent(AIAgent agent);

    /**
     * Registers a named harness agent for routing. {@link #run} will resolve the target agent by
     * the {@code agentId} supplied in {@link MsgContext#getExtra()} ()} (key {@code "agentId"}), falling
     * back to the agent registered via {@link #bindMainAgent}.
     */
    default void registerAgent(String agentId, AIAgent agent) {}

    /** Inbound turn (direct API or channel adapter). */
    LinkedMessageMap<String, Object> run(MsgContext context, List<LinkedMessageMap<String, Object>> messages);

    /**
     * Inbound turn with outbound address. The {@code outboundAddress} is recorded as the session's
     * "last route" so that proactive outbound messages (e.g. subagent announces) can be delivered
     * back to the originating channel/peer.
     *
     * @param context routing context for session key resolution
     * @param messages the inbound messages
     * @param outboundAddress the delivery target for proactive replies; may be null
     */
    default LinkedMessageMap<String, Object> run(MsgContext context, List<LinkedMessageMap<String, Object>> messages, OutboundAddress outboundAddress) {
        return run(context, messages, outboundAddress, null);
    }

    /**
     * Inbound turn with outbound address and an optional caller {@link org.frameworkset.spi.ai.model.ChatObject}.
     *
     * <p>The caller context is merged into the turn; gateway identity fields take precedence. The
     * default implementation ignores {@code callerContext} and delegates to {@link
     * #run(MsgContext, List)}.
     *
     * @param context routing context for session key resolution
     * @param messages the inbound messages
     * @param outboundAddress the delivery target for proactive replies; may be null
     * @param callerContext optional application context to merge; may be null
     */
    default LinkedMessageMap<String, Object> run(
            MsgContext context,
            List<LinkedMessageMap<String, Object>> messages,
            OutboundAddress outboundAddress,
            ChatObject callerContext) {
        return run(context, messages, outboundAddress, callerContext, null);
    }

    /**
     * Inbound turn with full Channel metadata for {link
     * ChannelRuntimeContextResolver}.
     *
     * @param inboundMessage optional inbound envelope passed to the resolver; may be null
     */
    default LinkedMessageMap<String, Object> run(
            MsgContext context,
            List<LinkedMessageMap<String, Object>> messages,
            OutboundAddress outboundAddress,
			ChatObject callerContext,
            InboundMessage inboundMessage) {
        return run(context, messages);
    }
	
	/** Single-message convenience. */
	default LinkedMessageMap<String, Object> run(
			MsgContext context, LinkedMessageMap<String, Object> message) {
		return run(context, Collections.singletonList(message));
	}

    /** Streaming variant of {@link #run(MsgContext, List)}. Returns fine-grained events. */
    default Flux<ServerEvent> runStream(MsgContext context, List<LinkedMessageMap<String, Object>> messages) {
        return runStream(context, messages, null, null);
    }

    /** Streaming variant of {@link #run(MsgContext, List, OutboundAddress)}. */
    default Flux<ServerEvent> runStream(
            MsgContext context, List<LinkedMessageMap<String, Object>> messages, OutboundAddress outboundAddress) {
        return runStream(context, messages, outboundAddress, null);
    }

    /**
     * Streaming variant of {@link #run(MsgContext, List, OutboundAddress, ChatObject)}.
     *
     * <p>The default signals unsupported streaming.
     */
    default Flux<ServerEvent> runStream(
            MsgContext context,
            List<LinkedMessageMap<String, Object>> messages,
            OutboundAddress outboundAddress,
            ChatObject callerContext) {
        return runStream(context, messages, outboundAddress, callerContext, null);
    }

    /**
     * Streaming variant of {@link #run(MsgContext, List, OutboundAddress, ChatObject,
     * InboundMessage)}.
     */
    default Flux<ServerEvent> runStream(
            MsgContext context,
            List<LinkedMessageMap<String, Object>> messages,
            OutboundAddress outboundAddress,
            ChatObject callerContext,
            InboundMessage inboundMessage) {
        return Flux.error(new UnsupportedOperationException("Streaming not supported"));
    }

    /**
     * Routes messages directly to an exposed subagent session identified by {@code subagentId},
     * bypassing normal binding-based routing. Returns the subagent's reply.
     *
     * @param subagentId the handle returned by
     *     {link SubagentGatewayBridge.this.ExposeResult#getId()}
     * @param messages the messages to send to the subagent
     */
    default LinkedMessageMap<String, Object> runSubagent(String subagentId, List<LinkedMessageMap<String, Object>> messages) {
        throw  new UnsupportedOperationException("Subagent routing not supported");
    }

    /** Streaming variant of {@link #runSubagent(String, List)}. */
    default Flux<ServerEvent> runSubagentStream(String subagentId, List<LinkedMessageMap<String, Object>> messages) {
        return Flux.error(new UnsupportedOperationException("Subagent streaming not supported"));
    }
}
