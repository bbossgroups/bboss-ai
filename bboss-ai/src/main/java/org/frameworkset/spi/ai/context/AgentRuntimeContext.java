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

import org.apache.commons.collections.MapUtils;
import org.frameworkset.spi.ai.compaction.CompactionConfig;
import org.frameworkset.spi.ai.filesystem.AbstractFilesystem;
import org.frameworkset.spi.ai.filesystem.WorkspaceManager;
import org.frameworkset.spi.ai.interceptor.AgentInterceptor;
import org.frameworkset.spi.ai.permission.PermissionBehavior;
import org.frameworkset.spi.ai.permission.PermissionMode;
import org.frameworkset.spi.ai.permission.PermissionRule;
import org.frameworkset.spi.ai.permission.ToolCallPermissionManager;
import org.frameworkset.spi.ai.state.PlanModeContextState;
import org.frameworkset.spi.ai.state.TaskContextState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 智能体运行上下文，静态配置
 * @author biaoping.yin
 * @Date 2026/8/25
 */
public class AgentRuntimeContext<T extends AgentRuntimeContext> {

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
 
	
	private boolean disableMemoryTools ;
	private boolean disableMemoryHooks ;
	
	private boolean disableFilesystemTools = false;
	

	
	/** 允许操作的基目录，为空则不限制 */
	private List<String> baseDirectories;
	private boolean disableShellTool = false;
	private boolean disableWebTools = false;
	private String httpWebSearchToolProxy = "http_websearch_tool";
	private String httpWebfetchToolProxy = "http_webfetch_tool";
	/**
	 * 智能体工作空间目录
	 */
	private String workspace;
	
	/**
	 * 是否启用任务列表
	 */
	private boolean taskListEnabled ;
	
	public PlanModeContextState getPlanModeContextState() {
		return planModeContextState;
	}
	
	public T setPlanModeContextState(PlanModeContextState planModeContextState) {
		this.planModeContextState = planModeContextState;
		return (T)this;
	}
	
	public TaskContextState getTaskContextState() {
		return taskContextState;
	}
	
	public T setTaskContextState(TaskContextState taskContextState) {
		this.taskContextState = taskContextState;
		return (T)this;
	}
	
	public boolean isEnablePlanMode() {
		return enablePlanMode;
	}
	
	public T setEnablePlanMode(boolean enablePlanMode) {
		this.enablePlanMode = enablePlanMode;
		return (T)this;
	}
	
	public boolean isTaskListEnabled() {
		return taskListEnabled;
	}
	
	public T setTaskListEnabled(boolean taskListEnabled) {
		this.taskListEnabled = taskListEnabled;
		return (T)this;
	}
	
	public boolean isDebugSSEData() {
		return debugSSEData;
	}
	
	public T setDebugSSEData(boolean debugSSEData) {
		this.debugSSEData = debugSSEData;
		return (T)this;
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
	
	public T setMode(PermissionMode mode) {
		this.mode = mode;
		return (T)this;
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
	
	public T setPermissionHitlTaskTimeout(long permissionHitlTaskTimeout) {
		this.permissionHitlTaskTimeout = permissionHitlTaskTimeout;
		return (T)this;
	}
	
	
	public T addAllowRule(String toolName, PermissionRule permissionRule) {
		if(permissionRule.getToolName() == null){
			permissionRule.setToolName(toolName);
		}
		if(permissionRule.getBehavior() == null)
			permissionRule.setBehavior(PermissionBehavior.ALLOW);
		if(allowRules == null){
			allowRules = new ConcurrentHashMap<>();	
		}
		allowRules.computeIfAbsent(toolName, k -> new ArrayList<>()).add(permissionRule);
		return (T)this;
	}
	
	/**
	 * source where the rule originated (e.g. {@code "userSettings"}, {@code "suggested"})	 
	 * ruleContent optional tool-specific matcher pattern (nullable)
	 */
	public T addAllowRule(String toolName, String ruleContent,String source) {
		PermissionRule permissionRule = new PermissionRule();
		permissionRule.setSource(source);
		permissionRule.setRuleContent(ruleContent);
		permissionRule.setBehavior(PermissionBehavior.ALLOW);
		
		return addAllowRule(  toolName, permissionRule);
	}
	
	/**
	 * source where the rule originated (e.g. {@code "userSettings"}, {@code "suggested"})
	 */
	
	public T addAllowRule(String toolName, String source) {
		
		return addAllowRule(  toolName,  (String) null,  source) ;
	}
	
	public T addDenyRule(String toolName, PermissionRule permissionRule) {
		if(permissionRule.getToolName() == null){
			permissionRule.setToolName(toolName);
		}
		if(permissionRule.getBehavior() == null)
			permissionRule.setBehavior(PermissionBehavior.DENY);
		if(denyRules == null){
			denyRules = new ConcurrentHashMap<>();
		}
		denyRules.computeIfAbsent(toolName, k -> new ArrayList<>()).add(permissionRule);
		return (T)this;
	}
	
	/**
	 * source where the rule originated (e.g. {@code "userSettings"}, {@code "suggested"})	 
	 * ruleContent optional tool-specific matcher pattern (nullable)
	 */ 
	public T addDenyRule(String toolName, String ruleContent,String source) {
		PermissionRule permissionRule = new PermissionRule();
		permissionRule.setSource(source);
		permissionRule.setRuleContent(ruleContent);
		permissionRule.setBehavior(PermissionBehavior.DENY);	 
		 
		return addDenyRule(  toolName,   permissionRule);
	}
	
	/**
	 * source where the rule originated (e.g. {@code "userSettings"}, {@code "suggested"})
	 */
	
	public T addDenyRule(String toolName, String source) {
		 
		return addDenyRule(  toolName,  (String) null,  source) ;
	}
	public T addAskRule(String toolName, PermissionRule permissionRule) {
		if(permissionRule.getToolName() == null){
			permissionRule.setToolName(toolName);
		}
		if(permissionRule.getBehavior() == null)
			permissionRule.setBehavior(PermissionBehavior.ASK);
		if(askRules == null){
			askRules = new ConcurrentHashMap<>();
		}
		askRules.computeIfAbsent(toolName, k -> new ArrayList<>()).add(permissionRule);
		return (T)this;
	}
	/**
	 * source where the rule originated (e.g. {@code "userSettings"}, {@code "suggested"})	 
	 * ruleContent optional tool-specific matcher pattern (nullable)
	 */
	public T addAskRule(String toolName, String ruleContent,String source) {
		PermissionRule permissionRule = new PermissionRule();
		permissionRule.setSource(source);
		permissionRule.setRuleContent(ruleContent);
		permissionRule.setBehavior(PermissionBehavior.ASK);
		 
		return addAskRule(  toolName, permissionRule);
	}
	
	/**
	 * source where the rule originated (e.g. {@code "userSettings"}, {@code "suggested"})
	 */
	
	public T addAskRule(String toolName, String source) {
		
		return addAskRule(  toolName,  (String) null,  source) ;
	}
	public PermissionMode getMode() {
		return mode;
	}
	
	public ToolCallPermissionManager getToolCallPermissionManager() {
		return toolCallPermissionManager;
	}
	
	public T setToolCallPermissionManager(ToolCallPermissionManager toolCallPermissionManager) {
		this.toolCallPermissionManager = toolCallPermissionManager;
		return (T)this;
	}
	
	
	public boolean isDisableMemoryTools() {
		return disableMemoryTools;
	}
	
	public T setDisableMemoryTools(boolean disableMemoryTools) {
		this.disableMemoryTools = disableMemoryTools;
		return (T)this;
	}
	
	public boolean isDisableMemoryHooks() {
		return disableMemoryHooks;
	}
	
	public T setDisableMemoryHooks(boolean disableMemoryHooks) {
		this.disableMemoryHooks = disableMemoryHooks;
		return (T)this;
	}
	
	public String getWorkspace() {
		return workspace;
	}
	
	public T setWorkspace(String workspace) {
		this.workspace = workspace;
		return (T)this;
	}
	protected boolean inited;
	protected Object initLock = new Object();
	public void init(WorkspaceManager workspaceManager) {
		if(inited)
			return;
		synchronized (initLock) {
			if (inited)
				return;
			inited = true;
		};
	}
	
	
	public boolean isDisableFilesystemTools() {
		return disableFilesystemTools;
	}
	
	public T setDisableFilesystemTools(boolean disableFilesystemTools) {
		this.disableFilesystemTools = disableFilesystemTools;
		return (T)this;
	}
	
	public boolean isDisableShellTool() {
		return disableShellTool;
	}
	
	public T setDisableShellTool(boolean disableShellTool) {
		this.disableShellTool = disableShellTool;
		return (T)this;
	}
	
	public boolean isDisableWebTools() {
		return disableWebTools;
	}
	
	public T setDisableWebTools(boolean disableWebTools) {
		this.disableWebTools = disableWebTools;
		return (T)this;
	}
	public List<String> getBaseDirectories() {
		return baseDirectories;
	}
	
	public T setBaseDirectories(List<String> baseDirectories) {
		this.baseDirectories = baseDirectories;
		return (T)this;
	}
	
	public String getHttpWebSearchToolProxy() {
		return httpWebSearchToolProxy;
	}
	
	public T setHttpWebSearchToolProxy(String httpWebSearchToolProxy) {
		this.httpWebSearchToolProxy = httpWebSearchToolProxy;
		return (T)this;
	}
	
	public String getHttpWebfetchToolProxy() {
		return httpWebfetchToolProxy;
	}
	
	public T setHttpWebfetchToolProxy(String httpWebfetchToolProxy) {
		this.httpWebfetchToolProxy = httpWebfetchToolProxy;
		return (T)this;
	}
}
