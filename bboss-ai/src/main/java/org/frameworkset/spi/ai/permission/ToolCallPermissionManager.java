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

import org.frameworkset.spi.ai.context.ChatContext;
import org.frameworkset.spi.ai.model.FunctionTool;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 外部权限管理接口
 * @author biaoping.yin
 * @Date 2026/9/30
 */
public interface ToolCallPermissionManager {
	default PermissionDecision checkPermissions(FunctionTool functionTool, Map<String, Object> toolInput, ChatContext chatContext){
		return PermissionDecision.passthrough(functionTool.getFunctionName());
	}
	
	default boolean matchRule(String ruleContent, Map<String, Object> input) {
		return ruleContent == null;
	}
	
	/**
	 * Default suggestion: a single tool-name-level {@link PermissionBehavior#ALLOW} rule sourced
	 * from {@code "suggested"}. Subclasses with finer-grained context (file paths, command
	 * prefixes) override this to produce more specific patterns.
	 */
	default List<PermissionRule> generateSuggestions(FunctionTool functionTool, Map<String, Object> toolInput) {
		List<PermissionRule> suggestions = new ArrayList<>();
		suggestions.add(new PermissionRule(functionTool.getFunctionName(), null, PermissionBehavior.ALLOW, "suggested"));
		return suggestions;
	}
}
