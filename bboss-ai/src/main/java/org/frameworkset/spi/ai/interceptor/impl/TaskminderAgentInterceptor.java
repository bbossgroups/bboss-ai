package org.frameworkset.spi.ai.interceptor.impl;
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
import org.frameworkset.spi.ai.interceptor.AgentInterceptor;
import org.frameworkset.spi.ai.model.FunctionTool;
import org.frameworkset.spi.ai.model.FunctionToolDefine;

/**
 *
 * @author biaoping.yin
 * @Date 2026/8/25
 */
public class TaskminderAgentInterceptor implements AgentInterceptor {
	 
	
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
	public void onReasoning(
			AIAgent agent,
			ChatContext ctx
//			ReasoningInput input,
//			Function<ReasoningInput, Flux<AgentEvent>> next
	) {
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
	public String onSystemPrompt(AIAgent agent, ChatContext  ctx, String currentPrompt) {
		return currentPrompt;
	}
	
}
