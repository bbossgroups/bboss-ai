package org.frameworkset.spi.ai.context;
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

import org.frameworkset.spi.ai.compaction.CompactionConfig;
import org.frameworkset.spi.ai.state.PlanModeContextState;
import org.frameworkset.spi.ai.state.TaskContextState;

/**
 *
 * @author biaoping.yin
 * @Date 2026/8/25
 */
public class AgentRuntimeContext {
	/**
	 * 是否输出SSE数据到日志文件
	 */
	private boolean debugSSEData;
	
	private PlanModeContextState planModeContextState;
	private TaskContextState taskContextState;
	/**
	 * 是否启用计划模式
	 */
	private boolean enablePlanMode;
	

	private CompactionConfig compactionConfig;
	
	/**
	 * 是否启用任务列表
	 */
	private boolean taskListEnabled ;
	
	public PlanModeContextState getPlanModeContextState() {
		return planModeContextState;
	}
	
	public AgentRuntimeContext setPlanModeContextState(PlanModeContextState planModeContextState) {
		this.planModeContextState = planModeContextState;
		return this;
	}
	
	public TaskContextState getTaskContextState() {
		return taskContextState;
	}
	
	public AgentRuntimeContext setTaskContextState(TaskContextState taskContextState) {
		this.taskContextState = taskContextState;
		return this;
	}
	
	public boolean isEnablePlanMode() {
		return enablePlanMode;
	}
	
	public AgentRuntimeContext setEnablePlanMode(boolean enablePlanMode) {
		this.enablePlanMode = enablePlanMode;
		return this;
	}
	
	public boolean isTaskListEnabled() {
		return taskListEnabled;
	}
	
	public AgentRuntimeContext setTaskListEnabled(boolean taskListEnabled) {
		this.taskListEnabled = taskListEnabled;
		return this;
	}
	
	public boolean isDebugSSEData() {
		return debugSSEData;
	}
	
	public AgentRuntimeContext setDebugSSEData(boolean debugSSEData) {
		this.debugSSEData = debugSSEData;
		return this;
	}
	
	
	public CompactionConfig getCompactionConfig() {
		return compactionConfig;
	}
	
	public AgentRuntimeContext setCompactionConfig(CompactionConfig compactionConfig) {
		this.compactionConfig = compactionConfig;
		return this;
	}
	
}
