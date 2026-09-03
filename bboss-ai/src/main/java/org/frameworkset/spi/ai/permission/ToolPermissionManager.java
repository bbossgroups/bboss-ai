package org.frameworkset.spi.ai.permission;
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

import org.frameworkset.spi.ai.callback.ChatContext;
import org.frameworkset.spi.ai.model.FunctionTool;
import org.frameworkset.spi.ai.plan.PlanTools;

/**
 *
 * @author biaoping.yin
 * @Date 2026/8/24
 */
public class ToolPermissionManager {
	private static final String[] planModeAlwaysAllowedTools = new String[]{
			PlanTools.PLAN_ENTER,
			PlanTools.PLAN_WRITE,
			PlanTools.PLAN_EXIT,
			"todo_write",
			"agent_spawn",
			"agent_send",
			"agent_list",
			"task_output",
			"task_list"
	};
	/**
	 * 判断权限
	 * @param chatContext
	 * @param functionTool
	 * @return
	 */
	public boolean isAllow(ChatContext chatContext, FunctionTool functionTool){
		return true;
	}
	
	public PermissionType getPermissionType(ChatContext chatContext, FunctionTool functionTool){
		return PermissionType.ALLOW;
	}
	
	
}
