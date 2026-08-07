package org.frameworkset.spi.ai.hitl;
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

import org.frameworkset.spi.ai.audit.Auditor;
import org.frameworkset.spi.ai.model.ChatObject;
import org.frameworkset.spi.ai.tool.AgentTraceHolder;
import org.frameworkset.spi.ai.tool.ToolCallContext;
import org.frameworkset.spi.ai.tools.BaseAuditorTool;
import org.frameworkset.spi.ai.tools.HitlAssistant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;

/**
 *
 * @author biaoping.yin
 * @Date 2026/8/5
 */
public class BaseHitlTaskTool<T extends BaseHitlTaskTool> extends BaseAuditorTool<T> implements HitlTaskToolInf<T>{
	private static final Logger logger = LoggerFactory.getLogger(BaseHitlTaskTool.class);
	
	protected HitlAssistant hitlAssistant;
	protected long hitlTaskTimeout;
	/**
	 * 任务超时处理方式，默认为rejected
	 * continue：继续执行
	 * rejected：拒绝执行
	 */
	private String timeoutAction = TIMEOUT_ACTION_REJECTED;
	public BaseHitlTaskTool(Auditor auditor) {
		super(auditor);
	}
	public BaseHitlTaskTool( ) {
		super( );
	}
	public String getTimeoutAction() {
		return timeoutAction;
	}
	
	public T setTimeoutAction(String timeoutAction) {
		this.timeoutAction = timeoutAction;
		return (T)this;
	}
	public HitlAssistant getHitlAssistant() {
		return hitlAssistant;
	}
	
	public T setHitlAssistant(HitlAssistant hitlAssistant) {
		this.hitlAssistant = hitlAssistant;
		return (T)this;
	}
	
	protected Map<String,Object> innerHitlTaskTool(String hitlTaskReason, ToolCallContext toolCallContext){
		// 参数 null/空白校验：防止空任务传递给人工
		if (hitlTaskReason == null || hitlTaskReason.equals("")) {
			if(logger.isWarnEnabled()) {
				logger.warn("hitlTaskTool called with null or empty hitlTaskReason, rejecting request");
			}
			return Collections.singletonMap("error", "hitlTaskReason must not be null or empty");
		}
		Map<String,Object> result = audit( "hitlTaskTool", hitlTaskReason);
		if(result != null)
			return result;
		
		
		
		// chatObject null 检查：防御非对话上下文调用
		ChatObject chatObject = AgentTraceHolder.getChatObject();
		
		if (chatObject == null) {
			if(logger.isWarnEnabled()) {
				logger.warn("hitlTaskTool: chatObject is null, cannot create HITL task outside agent context");
			}
			return Collections.singletonMap("error", "HITL task requires agent context (chatObject is null)");
		}
		
		try {
			HitlTaskHelper helper = HitlTaskHelper.getHitlTaskHelper();
			
			Map<String, Object> hitlTaskResult = helper.createHitlCallTask(this,hitlTaskReason, chatObject,toolCallContext);
			
			// 返回结果 null 保护
			if (hitlTaskResult == null) {
				if(logger.isDebugEnabled()) {
					logger.debug("hitlTaskTool: createHitlCallTask returned null for reason: {}",
							hitlTaskReason.length() > 500 ? hitlTaskReason.substring(0, 500) + "..." : hitlTaskReason);
				}
				return Collections.singletonMap("message", "HITL task completed with null result, please ignore and continue.");
			}
			
			
			return hitlTaskResult;
		} catch (Exception e) {
			if(logger.isErrorEnabled()) {
				logger.error("hitlTaskTool: failed to create HITL task for reason: {}", hitlTaskReason.length() > 500 ? hitlTaskReason.substring(0, 500) + "..." : hitlTaskReason, e);
			}
			String errorMessage = e.getCause() == null?e.getMessage():e.getCause().getMessage();
			if(errorMessage == null || errorMessage.equals("")){
				errorMessage = "Exception: failed to create HITL task";
			}
			return Collections.singletonMap("error",  errorMessage + ", ignore operation and continue.");
		}
	}
	
	@Override
	public T setHitlTaskTimeout(long hitlTaskTimeout) {
		this.hitlTaskTimeout = hitlTaskTimeout;
		return (T)this;
	}
	
	@Override
	public long getHitlTaskTimeout() {
		return hitlTaskTimeout;
	}
}
