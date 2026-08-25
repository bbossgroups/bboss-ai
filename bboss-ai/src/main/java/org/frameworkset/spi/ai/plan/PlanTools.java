package org.frameworkset.spi.ai.plan;
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

import org.frameworkset.spi.ai.model.ChatObject;
import org.frameworkset.spi.ai.model.annotation.Tool;
import org.frameworkset.spi.ai.model.annotation.ToolParam;
import org.frameworkset.spi.ai.tool.AgentTraceHolder;
import org.frameworkset.spi.ai.permission.ToolPermissionManager;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author biaoping.yin
 * @Date 2026/8/24
 */
public class PlanTools extends ToolPermissionManager {
	private String planPath;
	@Tool(name = "plan_enter",description = "Enter PLAN mode: a read-only phase for investigating the"
			+ " codebase and designing an approach before making any"
			+ " changes. While in plan mode you cannot edit files or"
			+ " run mutating commands; use plan_write to record your"
			+ " plan and plan_exit when you are ready to execute. Use"
			+ " this for non-trivial, multi-step, or ambiguous tasks.")
	public Map planEnter(){
		ChatObject chatObject = AgentTraceHolder.getChatObject();
		String text =  "Entered PLAN mode (read-only). Investigate freely, then call"
				+ " plan_write to record your plan at \""
				+ planPath
				+ "\". When the plan is ready, call plan_exit to request"
				+ " approval and begin executing.";
		Map result = new LinkedHashMap();
		result.put("text", text);
		return result;
	}
	
	@Tool(name = "plan_write",description = "Create or overwrite the current plan as a markdown document."
			+ " Pass the COMPLETE plan content; it replaces the file."
			+ " Use clear sections (goal, steps, risks, verification).")
	public Map planWrite(@ToolParam(name = "content", description = "The full markdown plan content.", 
							required = true) String content){
		ChatObject chatObject = AgentTraceHolder.getChatObject();
		String text =  "Entered PLAN mode (read-only). Investigate freely, then call"
				+ " plan_write to record your plan at \""
				+ planPath
				+ "\". When the plan is ready, call plan_exit to request"
				+ " approval and begin executing.";
		
		Map result = new LinkedHashMap();
		result.put("text", text);
		return result;
	}
	
	@Tool(name = "plan_exit",description = "Finish planning and request permission to start executing the"
			+ " plan. This pauses for the user to approve your plan. On"
			+ " approval you return to BUILD mode and may modify files;"
			+ " on rejection you stay in PLAN mode and should revise.")
	public Map planExit(@ToolParam(name = "summary", description = "Optional short summary of the plan for"
			+ " the user to approve.",
			required = true) String summary){
		ChatObject chatObject = AgentTraceHolder.getChatObject();
		String text =   "Plan approved. You are now in BUILD mode and may modify files and run"
				+ " commands. Start executing the plan now. Seed your task list"
				+ " with todo_write (one item per plan step), keep exactly one task"
				+ " in_progress, and update it as you go.";
		
		Map result = new LinkedHashMap();
		result.put("text", text);
		return result;
	}
	
}
