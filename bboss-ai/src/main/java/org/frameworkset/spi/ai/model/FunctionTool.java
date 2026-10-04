package org.frameworkset.spi.ai.model;
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
import org.frameworkset.spi.ai.model.tool.ToolCallState;
import org.frameworkset.spi.ai.permission.PermissionBehavior;
import org.frameworkset.spi.ai.permission.PermissionDecision;
import org.frameworkset.spi.ai.permission.PermissionRule;
import org.frameworkset.spi.ai.tool.ToolBase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author biaoping.yin
 * @Date 2026/2/9
 */
public class FunctionTool {
    private int index;
    private String id;
    private String type;
    private String functionName;
	private FunctionToolDefine functionToolDefine;
    private Map arguments;
	private Object objectArguments;
	private boolean readOnly;
	
	private ToolCallState toolCallState;
	private ToolBase toolBase;
	
	
	private Class inputType ;
    
    private List<Map> toolcalls;

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFunctionName() {
        return functionName;
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }
	
	public FunctionTool addArgument(String name,Object value){
		if(arguments == null)
			arguments = new java.util.LinkedHashMap<>();
		arguments.put(name, value);
		return this;
	}

    public Map getArguments() {
        return arguments;
    }

    public void setArguments(Map arguments) {
        this.arguments = arguments;
    }

    public List<Map> getToolcalls() {
        return toolcalls;
    }

    public void setToolcalls(List<Map> toolcalls) {
        this.toolcalls = toolcalls;
    }
	
	
	public Class getInputType() {
		return inputType;
	}
	
	public void setInputType(Class inputType) {
		this.inputType = inputType;
	}
	
	public Object getObjectArguments() {
		return objectArguments;
	}
	
	public void setObjectArguments(Object objectArguments) {
		this.objectArguments = objectArguments;
	}
	
	public ToolCallState getToolCallState() {
		return toolCallState;
	}
	
	public void setToolCallState(ToolCallState toolCallState) {
		this.toolCallState = toolCallState;
	}
	
	public ToolBase getToolBase() {
		return toolBase;
	}
	
	public void setToolBase(ToolBase toolBase) {
		this.toolBase = toolBase;
	}
	
	public boolean matchRule(String ruleContent, Map<String, Object> input) {
		if(toolBase != null) {
			return toolBase.matchRule(ruleContent, input);
		}
		return ruleContent == null;
	}
	
	/**
	 * Default suggestion: a single tool-name-level {@link PermissionBehavior#ALLOW} rule sourced
	 * from {@code "suggested"}. Subclasses with finer-grained context (file paths, command
	 * prefixes) override this to produce more specific patterns.
	 */
	public List<PermissionRule> generateSuggestions(Map<String, Object> toolInput) {
		if(toolBase != null) {
			return toolBase.generateSuggestions(this, toolInput);
		}
		else{
			List<PermissionRule> suggestions = new ArrayList<>();
			suggestions.add(new PermissionRule(getFunctionName(), null, PermissionBehavior.ALLOW, "suggested"));
			return suggestions;
		}
	}
	
	public boolean isReadOnly() {
		return functionToolDefine != null && functionToolDefine.isReadOnly();
	}
	
	 
	
	public FunctionToolDefine getFunctionToolDefine() {
		return functionToolDefine;
	}
	
	public void setFunctionToolDefine(FunctionToolDefine functionToolDefine) {
		this.functionToolDefine = functionToolDefine;
	}
	
	public PermissionDecision checkPermissions( Map<String, Object> toolInput, ChatContext chatContext) {
		if(toolBase != null) {
			return toolBase.checkPermissions(this, toolInput, chatContext);
		}
		return PermissionDecision.passthrough(getFunctionName());
	}
}
