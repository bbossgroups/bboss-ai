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

import org.frameworkset.spi.ai.model.FunctionTool;

/**
 *
 * @author biaoping.yin
 * @Date 2026/9/19
 */
public class PermissionVerdict {
	private FunctionTool functionTool;
	private PermissionBehavior behavior;
	private	 PermissionDecision permissionDecision;
	

	
	public PermissionVerdict(FunctionTool functionTool, PermissionBehavior permissionBehavior,PermissionDecision permissionDecision ) {
		this.functionTool = functionTool;
		this.behavior = permissionBehavior;
		this.permissionDecision = permissionDecision;	
	}
	public PermissionVerdict(){
		
	}
	
	
	public FunctionTool getFunctionTool() {
		return functionTool;
	}
	
	public void setFunctionTool(FunctionTool functionTool) {
		this.functionTool = functionTool;
	}
	
	public PermissionBehavior getBehavior() {
		return behavior;
	}
	
	public void setBehavior(PermissionBehavior behavior) {
		this.behavior = behavior;
	}
	
	public PermissionDecision getPermissionDecision() {
		return permissionDecision;
	}
	
	public void setPermissionDecision(PermissionDecision permissionDecision) {
		this.permissionDecision = permissionDecision;
	}
 
	
}
