package org.frameworkset.spi.ai.tools;
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

import com.frameworkset.util.SimpleStringUtil;
import org.frameworkset.spi.ai.hitl.HitlTaskHelper;
import org.frameworkset.spi.ai.model.ChatObject;
import org.frameworkset.spi.ai.model.annotation.Tool;
import org.frameworkset.spi.ai.model.annotation.ToolParam;
import org.frameworkset.spi.ai.tool.AgentTraceHolder;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.Map;

/**
 * 内置人工介入工具，用户可参考实现，可根据实际场景自定义实现
 * @author biaoping.yin
 * @Date 2026/7/16
 */
public class HitlTaskcallTool {
	private static Logger logger = org.slf4j.LoggerFactory.getLogger(HitlTaskcallTool.class);
	@Tool(name = "hitlTaskTool", description = "人工介入工具(HITL - Human-in-the-Loop)：当AI无法独立完成任务、遇到关键决策点、需要人工审批或验证时调用；" +
									"适用于：1.复杂问题需要人类专业判断 2.敏感操作需要人工确认 3.任务执行结果不符合预期需要人工介入调整 4.超出AI权限范围的操作；" +
									"上下文内容要求：精简聚焦，包含三要素——已执行步骤、卡住原因、建议关注要点，让人类在3秒内快速理解并做出决策。")
	public Map<String,Object> hitlTaskTool(@ToolParam(name = "hitlTaskReason",required = true,
														description = "人工介入原因，需包含：1.任务背景与已执行步骤 2.当前卡住的具体原因（技术障碍/权限限制/信息缺失等）3.建议人类关注的关键点或待决策事项 4.期望人类提供的具体帮助；格式清晰，精简聚焦，便于人类快速理解。") 
											   String hitlTaskReason){
//		ChatObject chatObject = AgentTraceHolder.getChatObject();
//		Map<String,Object> hitlTaskResult = HitlTaskHelper.getHitlTaskHelper().createHitlCallTask(hitlTaskReason, chatObject);
//		return hitlTaskResult;		
		// 【修复问题1】参数 null/空白校验：防止空任务传递给人工
		if (hitlTaskReason == null || hitlTaskReason.trim().isEmpty()) {
			logger.warn("hitlTaskTool called with null or empty hitlTaskReason, rejecting request");
			return Collections.singletonMap("error", "hitlTaskReason must not be null or empty");
		}
		
		// 【修复问题2】chatObject null 检查：防御非对话上下文调用
		ChatObject chatObject = AgentTraceHolder.getChatObject();
 
		
		// 【修复问题3】HitlTaskHelper null 检查 + 【修复问题4】异常捕获
		try {
			HitlTaskHelper helper = HitlTaskHelper.getHitlTaskHelper(); 
			
			Map<String, Object> hitlTaskResult = helper.createHitlCallTask(hitlTaskReason, chatObject);
			
			// 【修复问题5】返回结果 null 保护
			if (hitlTaskResult == null) {
				logger.warn("hitlTaskTool: createHitlCallTask returned null for reason: " + hitlTaskReason);
				return Collections.emptyMap();
			}
			
			return hitlTaskResult;
		} catch (Exception e) {
			logger.error( "hitlTaskTool: failed to create HITL task for reason: " + hitlTaskReason, e);
			return Collections.singletonMap("error", "Failed to create HITL task: " + SimpleStringUtil.exceptionToString(e));
		}
	}
}
