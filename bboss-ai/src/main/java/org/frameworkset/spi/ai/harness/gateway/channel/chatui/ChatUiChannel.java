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

import org.frameworkset.spi.ai.harness.gateway.Gateway;
import org.frameworkset.spi.ai.harness.gateway.MsgContext;
import org.frameworkset.spi.ai.harness.gateway.channel.*;
import org.frameworkset.spi.ai.model.LinkedMessageMap;
import org.frameworkset.spi.ai.model.ServerEvent;
import org.frameworkset.spi.ai.util.MessageBuilder;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Default {@link Channel} implementation for direct Chat UI interactions: no external transport,
 * no webhook, no websocket — the caller submits a {@link ChatUiRequest} programmatically and
 * receives the agent reply reactively.
 *
 * <p>Suitable for:
 * <ul>
 *   <li>Embedded web chat UIs that send HTTP requests to the harness
 *   <li>CLI tools and integration tests that need a typed API to the agent
 *   <li>Single-agent single-session scenarios where the default {@link DmScope#MAIN} collapses all
 *       conversations into one session
 * </ul>
 *
 * <h2>Obtaining an instance</h2>
 *
 * The recommended way is through {@link
 * GatewayBootstrap#chatUiChannel()}:
 *
 * <pre>{@code
 * GatewayBootstrap gw = GatewayBootstrap.builder().model(model)
 *     .agent("main", b -> b.name("assistant").sysPrompt("..."))
 *     .build();
 * ChatUiChannel chat = gw.chatUiChannel();
 * chat.send("Hello!").block();
 * }</pre>
 */
public final class ChatUiChannel implements Channel {
	
	public static final String CHANNEL_ID = "chatui";
	
	private volatile Gateway gateway;
	private volatile ChannelConfig config;
	private final ChannelRouter router;
	private final ConcurrentLinkedQueue<OutboundEnvelope> outboundQueue =
			new ConcurrentLinkedQueue<>();
	
	private ChatUiChannel(Gateway gateway, ChannelConfig config) {
		this.gateway = gateway;
		this.config = Objects.requireNonNull(config, "config");
		this.router = new ChannelRouter(null);
	}
	
	// -----------------------------------------------------------------
	//  Factories
	// -----------------------------------------------------------------
	
	/** Creates a Chat UI channel with the default {@link DmScope#MAIN} config. */
	public static ChatUiChannel create() {
		return new ChatUiChannel(null, ChannelConfig.of(CHANNEL_ID));
	}
	
	/** Creates a Chat UI channel with an explicit config. */
	public static ChatUiChannel create(ChannelConfig config) {
		return new ChatUiChannel(null, config);
	}
	
	/** Creates a per-peer Chat UI channel. Each distinct peerId gets its own session. */
	public static ChatUiChannel perPeer() {
		ChannelConfig cfg = ChannelConfig.builder(CHANNEL_ID).dmScope(DmScope.PER_PEER).build();
		return new ChatUiChannel(null, cfg);
	}
	
	/** Creates a Chat UI channel with a pre-wired gateway. */
	public static ChatUiChannel create(Gateway gateway) {
		return new ChatUiChannel(
				Objects.requireNonNull(gateway, "gateway"), ChannelConfig.of(CHANNEL_ID));
	}
	
	/** Creates a Chat UI channel with a pre-wired gateway and explicit config. */
	public static ChatUiChannel create(Gateway gateway, ChannelConfig config) {
		return new ChatUiChannel(Objects.requireNonNull(gateway, "gateway"), config);
	}
	
	// -----------------------------------------------------------------
	//  Channel lifecycle
	// -----------------------------------------------------------------
	
	@Override
	public void init(Gateway gateway) {
		if (this.gateway == null) {
			this.gateway = Objects.requireNonNull(gateway, "gateway");
		}
	}
	
	@Override
	public void start() {}
	
	@Override
	public void stop() {}
	
	@Override
	public String channelId() {
		return CHANNEL_ID;
	}
	
	@Override
	public ChannelConfig config() {
		return config;
	}
	
	@Override
	public boolean applyRoutingConfig(ChannelConfig newConfig) {
		Objects.requireNonNull(newConfig, "newConfig");
		if (!CHANNEL_ID.equals(newConfig.getChannelId())) {
			return false;
		}
		this.config = newConfig;
		return true;
	}
	
	@Override
	public LinkedMessageMap<String, Object> dispatch(InboundMessage message) {
		Objects.requireNonNull(message, "message");
		RouteResult route = router.resolveRoute(config, message);
		return resolveGateway()
				.run(
						route.getContext(),
						message.getMessages(),
						route.getOutboundAddress(),
						message.getRuntimeContext(),
						message);
	}
	
	/**
	 * Returns the {@link RouteResult} this channel would produce for {@code message} without
	 * dispatching it. Useful for pre-computing the session key before sending.
	 */
	public RouteResult previewRoute(InboundMessage message) {
		Objects.requireNonNull(message, "message");
		return router.resolveRoute(config, message);
	}
	
	@Override
	public void deliver(OutboundAddress address, List<LinkedMessageMap<String, Object>> messages) {
		if (messages != null && !messages.isEmpty()) {
			outboundQueue.add(new OutboundEnvelope(address, messages));
		}
	}
	
	/**
	 * Drains and returns all buffered proactive outbound messages. Returns an empty list if no
	 * messages are pending.
	 */
	public List<OutboundEnvelope> pollOutbound() {
		List<OutboundEnvelope> result = new ArrayList<>();
		OutboundEnvelope e;
		while ((e = outboundQueue.poll()) != null) {
			result.add(e);
		}
		return result;
	}
	
	/** Returns the number of buffered proactive outbound messages. */
	public int outboundQueueSize() {
		return outboundQueue.size();
	}
	
	// -----------------------------------------------------------------
	//  Convenience send APIs
	// -----------------------------------------------------------------
	
	/** Sends a plain-text message in single-session mode (no peer id). */
	public LinkedMessageMap<String, Object> send(String text) {
		return send(ChatUiRequest.of(Objects.requireNonNull(text, "text")));
	}
	
	/**
	 * Sends a pre-built {@link LinkedMessageMap<String, Object>} (text or multimodal) in single-session mode.
	 *
	 * <pre>{@code
	 * Msg multimodal = Msg.builder()
	 *         .role(MsgRole.USER)
	 *         .content(
	 *                 TextBlock.builder().text("Describe this").build(),
	 *                 ImageBlock.builder()
	 *                         .source(URLSource.builder().url(imageUrl).build())
	 *                         .build())
	 *         .build();
	 * chat.send(multimodal).block();
	 * }</pre>
	 */
	public LinkedMessageMap<String, Object> send(LinkedMessageMap<String, Object> message) {
		Objects.requireNonNull(message, "message");
		return send(ChatUiRequest.of(Collections.singletonList(message)));
	}
	
	/**
	 * Sends one or more pre-built {@link LinkedMessageMap<String, Object>}s in single-session mode. Use this for multimodal
	 * content or multi-part user turns.
	 */
	public LinkedMessageMap<String, Object> send(List<LinkedMessageMap<String, Object>> messages) {
		return send(ChatUiRequest.of(requireMessages(messages)));
	}
	
	/** Sends a plain-text message from a specific peer. */
	public LinkedMessageMap<String, Object> send(String peerId, String text) {
		Objects.requireNonNull(peerId, "peerId");
		Objects.requireNonNull(text, "text");
		return send(ChatUiRequest.withPeer(peerId, text));
	}
	
	/** Sends a pre-built {@link LinkedMessageMap<String, Object>} from a specific peer. */
	public LinkedMessageMap<String, Object> send(String peerId, LinkedMessageMap<String, Object> message) {
		Objects.requireNonNull(peerId, "peerId");
		Objects.requireNonNull(message, "message");
		return send(ChatUiRequest.withPeer(peerId, Collections.singletonList(message)));
	}
	
	/** Sends one or more pre-built {@link LinkedMessageMap<String, Object>}s from a specific peer. */
	public LinkedMessageMap<String, Object> send(String peerId, List<LinkedMessageMap<String, Object>> messages) {
		Objects.requireNonNull(peerId, "peerId");
		return send(ChatUiRequest.withPeer(peerId, requireMessages(messages)));
	}
	
	/**
	 * Sends a plain-text message with explicit routing identity. The {@link SendOptions} determines
	 * the user identity and session key directly — no {@link DmScope} configuration required.
	 *
	 * @see SendOptions#userId(String)
	 * @see SendOptions#of(String, String)
	 */
	public LinkedMessageMap<String, Object> send(SendOptions options, String text) {
		Objects.requireNonNull(options, "options");
		Objects.requireNonNull(text, "text");
		LinkedMessageMap<String, Object> msg = MessageBuilder.buildUserMessage(text).setName(options.getUserId());
		return dispatchWithOptions(options, Collections.singletonList(msg));
	}
	
	/**
	 * Sends a pre-built {@link LinkedMessageMap<String, Object>} with explicit routing identity. Prefer this overload for
	 * multimodal content (images, audio, …) while still using {@link SendOptions} for session
	 * routing.
	 */
	public LinkedMessageMap<String, Object> send(SendOptions options, LinkedMessageMap<String, Object> message) {
		Objects.requireNonNull(options, "options");
		Objects.requireNonNull(message, "message");
		return dispatchWithOptions(options, Collections.singletonList(message));
	}
	
	/**
	 * Sends one or more pre-built {@link LinkedMessageMap<String, Object>}s with explicit routing identity. Useful for
	 * multimodal or multi-part user turns.
	 */
	public LinkedMessageMap<String, Object> send(SendOptions options, List<LinkedMessageMap<String, Object>> messages) {
		Objects.requireNonNull(options, "options");
		return dispatchWithOptions(options, requireMessages(messages));
	}
	
	/** Sends a message directly to an exposed subagent, bypassing normal routing. */
	public LinkedMessageMap<String, Object> sendToSubagent(String subagentId, String text) {
		Objects.requireNonNull(subagentId, "subagentId");
		Objects.requireNonNull(text, "text");
		LinkedMessageMap<String, Object> msg = MessageBuilder.buildUserMessage(text);
		return resolveGateway().runSubagent(subagentId, Collections.singletonList(msg));
	}
	
	/** Sends a pre-built {@link LinkedMessageMap<String, Object>} directly to an exposed subagent. */
	public LinkedMessageMap<String, Object> sendToSubagent(String subagentId, LinkedMessageMap<String, Object> message) {
		Objects.requireNonNull(subagentId, "subagentId");
		Objects.requireNonNull(message, "message");
		return resolveGateway().runSubagent(subagentId, Collections.singletonList(message));
	}
	
	/** Sends one or more pre-built {@link LinkedMessageMap<String, Object>}s directly to an exposed subagent. */
	public LinkedMessageMap<String, Object> sendToSubagent(String subagentId, List<LinkedMessageMap<String, Object>> messages) {
		Objects.requireNonNull(subagentId, "subagentId");
		return resolveGateway().runSubagent(subagentId, requireMessages(messages));
	}
	
	/** Sends a structured {@link ChatUiRequest} and returns the agent reply reactively. */
	public LinkedMessageMap<String, Object> send(ChatUiRequest request) {
		Objects.requireNonNull(request, "request");
		if (request.getSubagentId() != null) {
			return resolveGateway().runSubagent(request.getSubagentId(), request.getMessages());
		}
		return dispatch(buildInbound(request));
	}
	
	// -----------------------------------------------------------------
	//  Streaming send APIs
	// -----------------------------------------------------------------
	
	@Override
	public Flux<ServerEvent> dispatchStream(InboundMessage message) {
		Objects.requireNonNull(message, "message");
		RouteResult route = router.resolveRoute(config, message);
		return resolveGateway()
				.runStream(
						route.getContext(),
						message.getMessages(),
						route.getOutboundAddress(),
						message.getRuntimeContext(),
						message);
	}
	
	/** Streaming variant of {@link #send(String)}. Returns fine-grained {@link ServerEvent}s. */
	public Flux<ServerEvent> sendStream(String text) {
		return sendStream(ChatUiRequest.of(Objects.requireNonNull(text, "text")));
	}
	
	/** Streaming variant of {link #send(Msg)}. */
	public Flux<ServerEvent> sendStream(LinkedMessageMap<String, Object> message) {
		Objects.requireNonNull(message, "message");
		return sendStream(ChatUiRequest.of(Collections.singletonList(message)));
	}
	
	/** Streaming variant of {@link #send(List)}. */
	public Flux<ServerEvent> sendStream(List<LinkedMessageMap<String, Object>> messages) {
		return sendStream(ChatUiRequest.of(requireMessages(messages)));
	}
	
	/** Streaming variant of {@link #send(String, String)}. */
	public Flux<ServerEvent> sendStream(String peerId, String text) {
		Objects.requireNonNull(peerId, "peerId");
		Objects.requireNonNull(text, "text");
		return sendStream(ChatUiRequest.withPeer(peerId, text));
	}
	
	/** Streaming variant of {@link #send(String, LinkedMessageMap<String, Object>)}. */
	public Flux<ServerEvent> sendStream(String peerId, LinkedMessageMap<String, Object> message) {
		Objects.requireNonNull(peerId, "peerId");
		Objects.requireNonNull(message, "message");
		return sendStream(ChatUiRequest.withPeer(peerId, Collections.singletonList(message)));
	}
	
	/** Streaming variant of {@link #send(String, List)}. */
	public Flux<ServerEvent> sendStream(String peerId, List<LinkedMessageMap<String, Object>> messages) {
		Objects.requireNonNull(peerId, "peerId");
		return sendStream(ChatUiRequest.withPeer(peerId, requireMessages(messages)));
	}
	
	/** Streaming variant of {@link #send(SendOptions, String)}. */
	public Flux<ServerEvent> sendStream(SendOptions options, String text) {
		Objects.requireNonNull(options, "options");
		Objects.requireNonNull(text, "text");
		LinkedMessageMap<String, Object> msg = MessageBuilder.buildUserMessage(text).setName(options.getUserId());
		return dispatchStreamWithOptions(options, Collections.singletonList(msg));
	}
	
	/** Streaming variant of {@link #send(SendOptions, LinkedMessageMap<String, Object>)}. */
	public Flux<ServerEvent> sendStream(SendOptions options, LinkedMessageMap<String, Object> message) {
		Objects.requireNonNull(options, "options");
		Objects.requireNonNull(message, "message");
		return dispatchStreamWithOptions(options, Collections.singletonList(message));
	}
	
	/** Streaming variant of {@link #send(SendOptions, List)}. */
	public Flux<ServerEvent> sendStream(SendOptions options, List<LinkedMessageMap<String, Object>> messages) {
		Objects.requireNonNull(options, "options");
		return dispatchStreamWithOptions(options, requireMessages(messages));
	}
	
	/** Streaming variant of {@link #sendToSubagent(String, String)}. */
	public Flux<ServerEvent> sendToSubagentStream(String subagentId, String text) {
		Objects.requireNonNull(subagentId, "subagentId");
		Objects.requireNonNull(text, "text");
		LinkedMessageMap<String, Object> msg = MessageBuilder.buildUserMessage(text);
		return resolveGateway().runSubagentStream(subagentId, Collections.singletonList(msg));
	}
	
	/** Streaming variant of {@link #sendToSubagent(String, LinkedMessageMap<String, Object>)}. */
	public Flux<ServerEvent> sendToSubagentStream(String subagentId, LinkedMessageMap<String, Object> message) {
		Objects.requireNonNull(subagentId, "subagentId");
		Objects.requireNonNull(message, "message");
		return resolveGateway().runSubagentStream(subagentId, Collections.singletonList(message));
	}
	
	/** Streaming variant of {@link #sendToSubagent(String, List)}. */
	public Flux<ServerEvent> sendToSubagentStream(String subagentId, List<LinkedMessageMap<String, Object>> messages) {
		Objects.requireNonNull(subagentId, "subagentId");
		return resolveGateway().runSubagentStream(subagentId, requireMessages(messages));
	}
	
	/** Streaming variant of {@link #send(ChatUiRequest)}. */
	public Flux<ServerEvent> sendStream(ChatUiRequest request) {
		Objects.requireNonNull(request, "request");
		if (request.getSubagentId() != null) {
			return resolveGateway().runSubagentStream(request.getSubagentId(), request.getMessages());
		}
		return dispatchStream(buildInbound(request));
	}
	
	// -----------------------------------------------------------------
	//  Internal
	// -----------------------------------------------------------------
	
	private static List<LinkedMessageMap<String, Object>> requireMessages(List<LinkedMessageMap<String, Object>> messages) {
		Objects.requireNonNull(messages, "messages");
		if (messages.isEmpty()) {
			throw new IllegalArgumentException("messages must not be empty");
		}
		return messages;
	}
	
	private LinkedMessageMap<String, Object> dispatchWithOptions(SendOptions options, List<LinkedMessageMap<String, Object>> messages) {
		MsgContext ctx = buildContextFromOptions(options);
		OutboundAddress outbound = buildOutboundFromOptions(options);
		return resolveGateway().run(ctx, messages, outbound, options.getRuntimeContext());
	}
	
	private Flux<ServerEvent> dispatchStreamWithOptions(SendOptions options, List<LinkedMessageMap<String, Object>> messages) {
		MsgContext ctx = buildContextFromOptions(options);
		OutboundAddress outbound = buildOutboundFromOptions(options);
		return resolveGateway().runStream(ctx, messages, outbound, options.getRuntimeContext());
	}
	
	private MsgContext buildContextFromOptions(SendOptions options) {
		String room = options.effectiveSessionKey();
		String agentId = options.getAgentId();
		if (agentId == null) {
			agentId = config.getDefaultAgentId();
		}
		Map<String, String> extra;
		if (agentId != null) {
			extra = new HashMap<>();
			extra.put("agentId", agentId);
		} else {
			extra = Collections.emptyMap();
		}
		return new MsgContext(CHANNEL_ID, null, room, null, null, extra)
				.withUserId(options.getUserId());
	}
	
	private OutboundAddress buildOutboundFromOptions(SendOptions options) {
		return OutboundAddress.direct(CHANNEL_ID, CHANNEL_ID + ":" + options.effectiveSessionKey());
	}
	
	private Gateway resolveGateway() {
		Gateway g = gateway;
		if (g == null) {
			throw new IllegalStateException(
					"ChatUiChannel has no gateway. Use GatewayBootstrap.chatUiChannel(),"
							+ " or construct with ChatUiChannel.create(gateway).");
		}
		return g;
	}
	
	private InboundMessage buildInbound(ChatUiRequest request) {
		String peerId = request.getPeerId();
		String agentId = request.getAgentId();
		if (peerId != null && !peerId.trim().isEmpty()) {
			if (agentId != null) {
				return InboundMessage.dmFor(CHANNEL_ID, peerId.trim(), agentId, request.getMessages());
			}
			return InboundMessage.dm(CHANNEL_ID, peerId.trim(), request.getMessages());
		}
		if (agentId != null) {
			return InboundMessage.dmFor(CHANNEL_ID, "__anonymous__", agentId, request.getMessages());
		}
		return InboundMessage.dm(CHANNEL_ID, "__anonymous__", request.getMessages());
	}
}