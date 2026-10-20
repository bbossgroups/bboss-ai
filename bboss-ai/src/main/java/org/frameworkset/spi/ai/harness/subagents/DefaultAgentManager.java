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
package org.frameworkset.spi.ai.harness.subagents;


import org.frameworkset.spi.ai.AIAgent;
import org.frameworkset.spi.ai.filesystem.WorkspaceManager;
import org.frameworkset.spi.ai.model.ChatAgentMessage;
import org.frameworkset.spi.ai.model.ChatObject;
import org.frameworkset.spi.ai.model.ServerEvent;
import org.frameworkset.spi.ai.model.StreamOptions;
import reactor.core.publisher.Flux;

import java.util.*;

/**
 * Pure agent factory and invoker — knows how to create agents from registered factories and invoke
 * them with a prompt.
 *
 * <p>This is the <em>agent-internal</em> layer. It has <strong>no</strong> session registry, no lane
 * management, no run tracking. The
 * agent-internal {@link AgentSpawnTool} uses this directly for
 * lightweight subagent invocation.
 */
public final class DefaultAgentManager {

    private volatile Map<String, SubagentFactory> agentFactories;
    private volatile Map<String, SubagentDeclaration> declarations;
    private final WorkspaceManager workspaceManager;

    /**
     * Builds a manager from subagent entries (factories plus optional {@link SubagentDeclaration}
     * metadata for remote configuration).
     */
    public DefaultAgentManager(List<SubagentEntry> entries, WorkspaceManager workspaceManager) {
        Map<String, SubagentFactory> factories = new HashMap<>();
        Map<String, SubagentDeclaration> decls = new HashMap<>();
        for (SubagentEntry e : entries) {
            factories.put(e.getName(), e.getFactory());
            if (e.getDeclaration() != null) {
                decls.put(e.getName(), e.getDeclaration());
            }
        }
		this.agentFactories = Collections.unmodifiableMap(new HashMap<>(factories));
		this.declarations = Collections.unmodifiableMap(new HashMap<>(decls));
        this.workspaceManager = workspaceManager;
    }

    /** Replaces the current set of entries with a new snapshot. */
    public void refreshEntries(List<SubagentEntry> entries) {
        Map<String, SubagentFactory> factories = new HashMap<>();
        Map<String, SubagentDeclaration> decls = new HashMap<>();
        for (SubagentEntry e : entries) {
            factories.put(e.getName(), e.getFactory());
            if (e.getDeclaration() != null) {
                decls.put(e.getName(), e.getDeclaration	());
            }
        }
		this.agentFactories = Collections.unmodifiableMap(new HashMap<>(factories));
		this.declarations = Collections.unmodifiableMap(new HashMap<>(decls));
    }

    /**
     * Atomic alias of {@link #refreshEntries(List)} used by
     * {@link io.agentscope.harness.agent.middleware.DynamicSubagentsMiddleware} to swap the registered
     * subagent set on every reasoning step. The two volatile reference assignments below ensure
     * any concurrent reader observes either the previous snapshot or the new one fully — never a
     * partial state.
     */
    public void replaceAgents(List<SubagentEntry> entries) {
        refreshEntries(entries);
    }

    /**
     * Race-safe lookup-and-create. Returns {@link Optional#empty()} when no factory is registered
     * for {@code agentId} at the moment of the volatile read, when the registered declaration is
     * {@link SubagentDeclaration.Mode#PRIMARY}-only (cannot be spawned as a subagent), otherwise
     * returns a freshly created agent. Preferred over the two-step {@link #hasAgent(String)} +
     * {@link #createAgent(String, RuntimeContext)} pair when the registry may be replaced
     * concurrently (e.g. dynamic reload between calls).
     *
     * <p>{@code parentRc} is forwarded to {@link SubagentFactory#create(RuntimeContext)} so child
     * agents can bucket their persisted state by parent identity. Pass
     * {@link RuntimeContext#empty()} when no parent context is available.
     */
    public Optional<AIAgent> createAgentIfPresent(String agentId, ChatObject parentRc) {
        if (agentId == null) {
            return Optional.empty();
        }
        SubagentFactory factory = agentFactories.get(agentId);
        if (factory == null) {
            return Optional.empty();
        }
        SubagentDeclaration decl = declarations.get(agentId);
        if (decl != null && decl.getMode() == SubagentDeclaration.Mode.PRIMARY) {
            return Optional.empty();
        }
        return Optional.of(factory.create(parentRc != null ? parentRc : ChatObject.empty()));
    }

    /**
     * Returns {@code true} when {@code agentId} is registered with a {@code PRIMARY}-only
     * declaration. Lets {link AgentSpawnTool} produce a more
     * helpful error message ("PRIMARY-only, cannot be spawned") instead of the generic
     * "Unknown agent_id" when {@link #createAgentIfPresent} returns empty.
     */
    public boolean isPrimaryOnly(String agentId) {
        if (agentId == null) return false;
        SubagentDeclaration decl = declarations.get(agentId);
        return decl != null && decl.getMode() == SubagentDeclaration.Mode.PRIMARY;
    }

    /** Whether a factory is registered for the given agent id. */
    public boolean hasAgent(String agentId) {
        return agentId != null && agentFactories.containsKey(agentId);
    }

    /** Immutable view of registered subagent factories keyed by {@code agent_id}. */
    public Map<String, SubagentFactory> getAgentFactories() {
        return agentFactories;
    }

    /** Optional declaration metadata for the given {@code agent_id} (e.g. remote URL). */
    public Optional<SubagentDeclaration> getDeclaration(String agentId) {
        return Optional.ofNullable(declarations.get(agentId));
    }

    /**
     * Creates a new agent instance from the registered factory.
     *
     * @throws IllegalArgumentException if no factory is registered for the given id
     */
    public AIAgent createAgent(String agentId, ChatObject parentRc) {
        SubagentFactory factory = agentFactories.get(agentId);
        if (factory == null) {
            throw new IllegalArgumentException("Unknown agent_id: " + agentId);
        }
        return factory.create(parentRc != null ? parentRc : ChatObject.empty());
    }

    /**
     * Invokes an agent with a user prompt. Handles both plain {@link Agent} and {@link
     * HarnessAgent} (injects {@link RuntimeContext} for the latter).
     *
     * <p>For {@link HarnessAgent} children, {@code userId} is propagated so that isolation-key
     * resolution (e.g. {@code USER}-scoped sandbox slots) works correctly. A fresh {@code
     * sessionId} is always assigned independently of the parent session.
     *
     * @param agent the agent to invoke
     * @param sessionId a new, child-specific session id
     * @param userId the parent's user-id (may be {@code null})
     * @param prompt the user message to send
     */
    public ServerEvent invokeAgent(AIAgent agent, String sessionId, String userId, String prompt) {
        return invokeAgent(agent, sessionId, userId, prompt, null);
    }

    public ServerEvent invokeAgent(
			AIAgent agent, String sessionId, String userId, String prompt, ChatObject parentRc) {
//		ChatObject ctx =
//                parentRc != null
//                        ? ChatObject.builder(parentRc)
//                                .sessionId(sessionId)
//                                .userId(userId)
//                                .build()
//                        : ChatObject.builder().sessionId(sessionId).userId(userId).build();
//        if (agent instanceof ReActAgent react) {
//            return agent.chat(prompt, ctx);
//        }
//        if (agent instanceof HarnessAgent harness) {
//            return harness.call(userMessage(prompt), ctx);
//        }
//        return agent.call(List.of(userMessage(prompt)));
		return agent.chat(prompt, (ChatAgentMessage) parentRc.getAgentMessage());
    }

    /**
     * Invokes an agent and returns its execution as a tagged {@link Flux} of {@link Event}s.
     *
     * <p>Every event in the returned flux carries an {@link EventSource} built from {@code source}
     * combined with the child's {@code agentId}/{@code sessionId}. This allows parent consumers
     * to identify which subagent emitted each event without out-of-band metadata.
     *
     * <p>The {@code parentSource} argument should be the {@link EventSource} already stored in the
     * parent's Reactor Context (if any). When the parent itself is a subagent, its path is used as
     * the prefix so the full call-hierarchy path is preserved across multiple nesting levels.
     *
     * @param agent the agent to invoke
     * @param sessionId a new, child-specific session id
     * @param userId the parent's user-id (may be {@code null})
     * @param prompt the user message to send
     * @param source the {@link EventSource} that will be stamped onto every emitted event
     * @param options stream configuration passed to the child agent
     * @return {@link Flux} of tagged events; never null
     */
    public Flux<ServerEvent> invokeAgentStream(
            AIAgent agent,
            String sessionId,
            String userId,
            String prompt,
			ServerEvent source,
            StreamOptions options) {
        return invokeAgentStream(agent, sessionId, userId, prompt, source, options, null);
    }

    public Flux<ServerEvent> invokeAgentStream(
			AIAgent agent,
            String sessionId,
            String userId,
            String prompt,
			ServerEvent source,
            StreamOptions options,
            ChatObject parentRc) {
        Flux<ServerEvent> childFlux;
        StreamOptions effective = options != null ? options : StreamOptions.defaults();
//        RuntimeContext ctx =
//                parentRc != null
//                        ? RuntimeContext.builder(parentRc)
//                                .sessionId(sessionId)
//                                .userId(userId)
//                                .build()
//                        : RuntimeContext.builder().sessionId(sessionId).userId(userId).build();
//        if (agent instanceof ReActAgent react) {
//            childFlux = react.stream(List.of(userMessage(prompt)), effective, ctx);
//        } else if (agent instanceof HarnessAgent harness) {
//            childFlux = harness.stream(List.of(userMessage(prompt)), effective, ctx);
//        } else {
//            childFlux = agent.stream(List.of(userMessage(prompt)), effective);
//        }
		childFlux = agent.streamChat(prompt, (ChatAgentMessage) parentRc.getAgentMessage());
//        return childFlux.map(event -> event.withSource(source));
		return childFlux;
    }

    public WorkspaceManager getWorkspaceManager() {
        return workspaceManager;
    }

//    private static Msg userMessage(String prompt) {
//        return Msg.builder().role(MsgRole.USER).textContent(prompt).build();
//    }
}
