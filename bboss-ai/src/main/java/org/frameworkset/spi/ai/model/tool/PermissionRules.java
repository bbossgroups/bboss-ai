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

import java.util.List;
import java.util.Map;

/**
 *
 * @author biaoping.yin
 * @Date 2026/9/28
 */
public class PermissionRules {
	public static final String PERMISSION_RULES_KEY = "permissionRules";
	/**
	 * 允许调用工具的权限规则
	 */
	
	private Map<String, List<PermissionRule>> allowRules;
	/**
	 * 拒绝调用工具的权限规则
	 */
	
	private   Map<String, List<PermissionRule>> denyRules;
	/**
	 * 需要用户确认的权限规则
	 */
	
	private   Map<String, List<PermissionRule>> askRules;
	public Map<String, List<PermissionRule>> getAllowRules() {
		return allowRules;
	}
	public void setAllowRules(Map<String, List<PermissionRule>> allowRules) {
		this.allowRules = allowRules;
	}
	
	public Map<String, List<PermissionRule>> getDenyRules() {
		return denyRules;
	}
	public void setDenyRules(Map<String, List<PermissionRule>> denyRules) {
		this.denyRules = denyRules;
	}
	public Map<String, List<PermissionRule>> getAskRules() {
		return askRules;
	}
	public void setAskRules(Map<String, List<PermissionRule>> askRules) {
		this.askRules = askRules;
	}
}
