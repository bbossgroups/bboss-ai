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

import org.frameworkset.spi.ai.permission.PermissionRule;

import java.util.Map;

/**
 * 
 * @author biaoping.yin
 * @Date 2026/9/24
 */
public class ToolCallAskResult {
	private String toolId;
	private String toolName;
	private boolean approved	 ;
	private String hitlConfirm;
	/**
	 * 用户选择无需重复判断的权限规则：allwaysAllow,allwaysDeny
	 */
	private PermissionRule choosedAlwaysPermissionRule;
	private Map<String,Object>	 updateInput;
	
	public String getToolId() {
		return toolId;
	}
	
	public void setToolId(String toolId) {
		this.toolId = toolId;
	}
	
	public String getToolName() {
		return toolName;
	}
	
	public void setToolName(String toolName) {
		this.toolName = toolName;
	}
	public boolean isApproved() {
		return approved;
	}	
	public void setApproved(boolean approved) {
		this.approved = approved;
	}
	
	public String getHitlConfirm() {
		return hitlConfirm;
	}
	
	public void setHitlConfirm(String hitlConfirm) {
		this.hitlConfirm = hitlConfirm;
	}
	
	public Map<String, Object> getUpdateInput() {
		return updateInput;
	}
	
	public void setUpdateInput(Map<String, Object> updateInput) {
		this.updateInput = updateInput;
	}
	
	public PermissionRule getChoosedAlwaysPermissionRule() {
		return choosedAlwaysPermissionRule;
	}
	
	public void setChoosedAlwaysPermissionRule(PermissionRule choosedAlwaysPermissionRule) {
		this.choosedAlwaysPermissionRule = choosedAlwaysPermissionRule;
	}
}
