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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.frameworkset.spi.ai.compaction.CompactionConfig;
import org.frameworkset.spi.ai.permission.ToolCallPermissionManager;
import org.frameworkset.spi.ai.permission.PermissionMode;
import org.frameworkset.spi.ai.permission.PermissionRule;
import org.frameworkset.spi.ai.state.PlanModeContextState;
import org.frameworkset.spi.ai.state.TaskContextState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
	

	
	private ToolCallPermissionManager toolCallPermissionManager;
	
	private PlanModeContextState planModeContextState;
	private TaskContextState taskContextState;
	private PermissionMode mode = PermissionMode.BYPASS;
	private  Map<String, List<PermissionRule>> allowRules;
	private  Map<String, List<PermissionRule>> denyRules;
	private  Map<String, List<PermissionRule>> askRules;
	

	/**
	 * 权限确认超时时间
	 */
	private long permissionHitlTaskTimeout = 60L * 1000L;
 
	/**
	 * 是否启用计划模式
	 */
	private boolean enablePlanMode;
	
	/**
	 * 是否启用计划模式
	 */
	private boolean enableMemorySearch;
	

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
	
	public boolean isEnableMemorySearch() {
		return enableMemorySearch;
	}
	public AgentRuntimeContext setEnableMemorySearch(boolean enableMemorySearch) {
		this.enableMemorySearch = enableMemorySearch;
		return this;
	}
	 
	
	public Map<String, List<PermissionRule>> getAllowRules() {
		return allowRules;
	}
	
 
	public Map<String, List<PermissionRule>> getDenyRules() {
		return denyRules;
	}
	
 
	
	public Map<String, List<PermissionRule>> getAskRules() {
		return askRules;
	}
	
	public AgentRuntimeContext setMode(PermissionMode mode) {
		this.mode = mode;
		return this;
	}
	
	public boolean isTrivial() {
		return mode == PermissionMode.DEFAULT
//				&& workingDirectories.isEmpty()
				&& MapUtils.isEmpty(allowRules)
				&& MapUtils.isEmpty(denyRules)
				&& MapUtils.isEmpty(askRules);
	}
	public long getPermissionHitlTaskTimeout() {
		return permissionHitlTaskTimeout;
	}
	
	public AgentRuntimeContext setPermissionHitlTaskTimeout(long permissionHitlTaskTimeout) {
		this.permissionHitlTaskTimeout = permissionHitlTaskTimeout;
		return this;
	}
	
	
	public AgentRuntimeContext addAllowRule(String toolName, PermissionRule permissionRule) {
		if(permissionRule.getToolName() == null){
			permissionRule.setToolName(toolName);
		}
		if(allowRules == null){
			allowRules = new ConcurrentHashMap<>();	
		}
		allowRules.computeIfAbsent(toolName, k -> new ArrayList<>()).add(permissionRule);
		return this;
	}
	
	public AgentRuntimeContext addDenyRule(String toolName, PermissionRule permissionRule) {
		if(permissionRule.getToolName() == null){
			permissionRule.setToolName(toolName);
		}
		if(denyRules == null){
			denyRules = new ConcurrentHashMap<>();
		}
		denyRules.computeIfAbsent(toolName, k -> new ArrayList<>()).add(permissionRule);
		return this;
	}
	public AgentRuntimeContext addAskRule(String toolName, PermissionRule permissionRule) {
		if(permissionRule.getToolName() == null){
			permissionRule.setToolName(toolName);
		}
		if(askRules == null){
			askRules = new ConcurrentHashMap<>();
		}
		askRules.computeIfAbsent(toolName, k -> new ArrayList<>()).add(permissionRule);
		return this;
	}
	
	public PermissionMode getMode() {
		return mode;
	}
	
	public ToolCallPermissionManager getToolCallPermissionManager() {
		return toolCallPermissionManager;
	}
	
	public AgentRuntimeContext setToolCallPermissionManager(ToolCallPermissionManager toolCallPermissionManager) {
		this.toolCallPermissionManager = toolCallPermissionManager;
		return this;
	}
}
