package org.frameworkset.spi.ai.model.tool;
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

import com.frameworkset.orm.annotation.Column;
import org.frameworkset.spi.ai.permission.PermissionRule;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 存储用户调用工具的权限规则
 * @author biaoping.yin
 * @Date 2026/9/28
 */
public class AgentToolCallRules {
	private LocalDateTime createTime;
	
	private LocalDateTime updateTime;

	/**
	 * 存储用户调用工具的权限规则id
	 */
	private String agentToolCallRulesId;
	/**
	 * 用户id
	 */
	private String userId;
	/**
	 * 调用工具的会话id
	 */
	private String sessionId;
	/**
	 * 工具名称
	 */
	private String toolName;
	/**
	 * 调用工具的智能体id
	 */
	private String agentId;
	/**
	 * 允许调用工具的权限规则
	 */
	@Column(type = "clob",editor = "org.frameworkset.spi.ai.store.db.AgentToolCallRulesEditor")	
	private   PermissionRules permissionRules;
	 
	
	public String getUserId() {
		return userId;
	}
	
	public void setUserId(String userId) {
		this.userId = userId;
	}
	
	public String getSessionId() {
		return sessionId;
	}
	
	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}
	
	public String getToolName() {
		return toolName;
	}
	
	public void setToolName(String toolName) {
		this.toolName = toolName;
	}
	 
	
	public String getAgentId() {
		return agentId;
	}
	
	public void setAgentId(String agentId) {
		this.agentId = agentId;
	}
	public String getAgentToolCallRulesId() {
		return agentToolCallRulesId;
	}
	
	public void setAgentToolCallRulesId(String agentToolCallRulesId) {
		this.agentToolCallRulesId = agentToolCallRulesId;
	}
	
	public LocalDateTime getCreateTime() {
		return createTime;
	}
	public void setCreateTime(LocalDateTime createTime) {
		this.createTime = createTime;
	}
	
	public PermissionRules getPermissionRules() {
		return permissionRules;
	}
	public void setPermissionRules(PermissionRules permissionRules) {
		this.permissionRules = permissionRules;
	}
	
	public LocalDateTime getUpdateTime() {
		return updateTime;
	}
	
	public void setUpdateTime(LocalDateTime updateTime) {
		this.updateTime = updateTime;
	}
}
