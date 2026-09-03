package org.frameworkset.spi.ai.interceptor;
/**
 * Copyright 2026 bboss
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.frameworkset.spi.ai.AIAgent;
import org.frameworkset.spi.ai.callback.ChatContext;
import org.frameworkset.spi.ai.model.FunctionTool;
import org.frameworkset.spi.ai.model.FunctionToolDefine;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 *
 * @author biaoping.yin
 * @Date 2026/8/25
 */
public interface AgentInterceptor {
	/**
	 * Returns this middleware's execution order.
	 *
	 * <p>A larger number means a higher priority and places the middleware closer to the outside
	 * of the onion chain: for example, {@code 2} runs before {@code 1}, and {@code 0} runs after
	 * {@code 1}. Middlewares with the same order retain their builder registration order. The
	 * default value is {@code 1}.
	 *
	 * @return the execution order; higher values execute first before delegating to {@code next}
	 */
	default int order() {
		return 1;
	}
	
//	/**
//	 * Intercept the entire agent invocation.
//	 *
//	 * @param agent the agent instance
//	 * @param ctx   per-call runtime context (session, user, attributes)
//	 * @param input agent input (messages)
//	 * @param next  calls the next middleware or the core agent logic
//	 * @return event stream from the agent invocation
//	 */
//	default Flux<AgentEvent> onAgent(
//			Agent agent,
//			RuntimeContext ctx,
//			AgentInput input,
//			Function<AgentInput, Flux<AgentEvent>> next) {
//		return next.apply(input);
//	}
	
	/**
	 * Intercept the reasoning phase (LLM call + streaming output parsing).
	 *
	 * @param agent the agent instance
	 * @param ctx   per-call runtime context (session, user, attributes)
//	 * @param input reasoning input (messages, tools, options)
//	 * @param next  calls the next middleware or the core reasoning logic
	 * @return event stream from reasoning
	 */
	default void onReasoning(
			AIAgent agent,
			ChatContext ctx
//			ReasoningInput input,
//			Function<ReasoningInput, Flux<AgentEvent>> next
	) {
//		return next.apply(input);
	}
	
	/**
	 * Intercept the tool-call execution phase.
	 *
	 * @param agent the agent instance
	 * @param ctx   per-call runtime context (session, user, attributes)
	 * @param input acting input (the tool calls)
//	 * @param next  calls the next middleware or the core acting logic
	 * @return event stream from acting
	 */
	default void onActing(
			AIAgent agent,
			ChatContext ctx,
			FunctionTool input,
			FunctionToolDefine functionToolDefine) {
		 
	}
	
	/**
	 * Intercept the raw model API call.
	 *
	 * <p>Transformations to { io.agentscope.core.event.TextBlockDeltaEvent} instances in the
	 * returned stream are reflected in the final response message. This allows middleware to
	 * normalize model text before consumers, including native structured-output parsing, use it.
	 *
	 * @param agent the agent instance
	 * @param ctx   per-call runtime context (session, user, attributes)
//	 * @param input model-call input (messages, tools, options, model)
//	 * @param next  calls the next middleware or the actual model invocation
	 * @return event stream from the model call
	 */
	default void onModelCall(
			AIAgent agent,
			ChatContext ctx) {
//		return next.apply(input);
	}
	
	/**
	 * Transform the system prompt string (pipeline pattern).
	 *
	 * <p>Multiple middlewares are applied sequentially; each receives the
	 * output of the previous one.
	 *
	 * @param agent         the agent instance
	 * @param ctx           per-call runtime context (session, user, attributes)
	 * @param currentPrompt the current system prompt
	 * @return the (possibly transformed) system prompt
	 */
	default String onSystemPrompt(AIAgent agent, ChatContext  ctx, String currentPrompt) {
		return currentPrompt;
	}
	
}
