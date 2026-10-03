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

/**
 * A single permission rule pinned to a tool name.
 *
 * <p>{@code ruleContent} is interpreted by the owning tool's {@code matchRule} method. When it is
 * {@code null}, the rule applies to every invocation of the named tool (tool-name-level rule).
 *
 * toolName the tool this rule targets (never {@code null})
 * ruleContent optional tool-specific matcher pattern (nullable)
 * behavior the resulting behavior when the rule matches (never {@code null})
 * source where the rule originated (e.g. {@code "userSettings"}, {@code "suggested"})
 */
public class PermissionRule {
	/**
	 * toolName the tool this rule targets (never {@code null})
	 */
	private String toolName;
	/**
	 * ruleContent optional tool-specific matcher pattern (nullable)
	 */
	private String ruleContent;
	/**
	 * behavior the resulting behavior when the rule matches (never {@code null})
	 */
	private PermissionBehavior behavior;
	/**
	 * source where the rule originated (e.g. {@code "userSettings"}, {@code "suggested"})
	 */
	private String source;
	
	public PermissionRule(){
		
	}
	public PermissionRule(String toolName, String ruleContent, PermissionBehavior permissionBehavior, String source) {
		this.toolName = toolName;
		this.ruleContent = ruleContent;
		this.behavior = permissionBehavior;
		this.source = source;
	}
	public PermissionRule(  String ruleContent, PermissionBehavior permissionBehavior, String source) {
		this.ruleContent = ruleContent;
		this.behavior = permissionBehavior;
		this.source = source;
	}
	public PermissionRule(  String ruleContent,   String source) {
		this.ruleContent = ruleContent;
		this.source = source;
	}
	public PermissionRule(    String source) {
		this.source = source;
	}
	public String getToolName() {
		return toolName;
	}
	
	public void setToolName(String toolName) {
		this.toolName = toolName;
	}
	
	public String getRuleContent() {
		return ruleContent;
	}
	
	public void setRuleContent(String ruleContent) {
		this.ruleContent = ruleContent;
	}
	
	public PermissionBehavior getBehavior() {
		return behavior;
	}
	
	public void setBehavior(PermissionBehavior behavior) {
		this.behavior = behavior;
	}
	
	public String getSource() {
		return source;
	}
	
	public void setSource(String source) {
		this.source = source;
	}
}
