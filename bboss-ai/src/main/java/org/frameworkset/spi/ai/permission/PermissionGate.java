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

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author biaoping.yin
 * @Date 2026/9/22
 */
public class PermissionGate {
	private List<PermissionVerdict> pendingAsk;
	private Map<String,PermissionVerdict> autoDeniedIds;
	
	public PermissionGate(List<PermissionVerdict> pendingAsk, Map<String,PermissionVerdict> autoDeniedIds) {
		this.pendingAsk = pendingAsk;
		this.autoDeniedIds = autoDeniedIds;
	}

	
	public List<PermissionVerdict> getPendingAsk() {
		return pendingAsk;
	}
 
	public Map<String,PermissionVerdict> getAutoDeniedIds() {
		return autoDeniedIds;
	}
}
