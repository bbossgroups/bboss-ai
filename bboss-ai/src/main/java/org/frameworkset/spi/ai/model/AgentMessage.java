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

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.frameworkset.spi.ai.adapter.AgentAdapter;
import org.frameworkset.spi.ai.tools.ToolsRegist;
import org.frameworkset.spi.remote.http.ClientConfiguration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author biaoping.yin
 * @Date 2026/1/4
 */
public class AgentMessage<T extends AgentMessage> {

    /**
     * 提示词工程
     */
    private String prompt;

    /**
     * 工具清单，标准工具规范格式
     */
    private List<FunctionToolDefine> tools;

    @JsonIgnore
    private Map<String,FunctionCall> toolCalls;

    @JsonIgnore
    private ToolsRegist toolsRegist;
    /**
     * 默认角色提示词工程
     */
    private String systemPrompt;

    public String getNegativePrompt() {
        return negativePrompt;
    }

    public T setNegativePrompt(String negativePrompt) {
        this.negativePrompt = negativePrompt;
        return (T)this;
    }
    
    

    /**
     * 反向提示词工程
     */
    private String negativePrompt ;
    private String model ;
    private Map parameters;



    private Integer maxTokens; 
    private Boolean stream;
    private Double temperature;
    private Map headers = null;
//        header.put("X-DashScope-Async","enable");
    /**
     * 消息级别模型类型，优先级高于模型服务级别配置，取值参考：
     * public class AIConstants {
     *     public static final String AI_MODEL_TYPE_QWEN = "qwen";
     *     public static final String AI_MODEL_TYPE_DOUBAO = "doubao";
     *     public static final String AI_MODEL_TYPE_DEEPSEEK = "deepseek";
     *     public static final String AI_MODEL_TYPE_KIMI = "kimi";
     *     public static final String AI_MODEL_TYPE_NONE = "none";
     *
     * }
     */
//    private String modelType;

    public List<FunctionToolDefine> getTools() {
        return tools;
    }

    public T setTools(List<FunctionToolDefine> tools) {
        this.tools = tools;
        return (T)this;
    }

    public T registTools(List<FunctionToolDefine> tools) {
        if(this.tools == null){
            this.tools = new ArrayList<>();
        }
        this.tools.addAll( tools);
        return (T)this;
    }


    public T registTool(FunctionToolDefine functionToolDefine) {
        if(this.tools == null){
            tools = new ArrayList<>();
        }
        tools.add(functionToolDefine);
        return (T)this;
    }
    
    public T registToolCalls(Map<String,FunctionCall> toolCalls) {
        if(this.toolCalls == null){
            toolCalls = new LinkedHashMap<>();
        }
        this.toolCalls.putAll(toolCalls);
        return (T)this;
    }
    public T registToolCall(String toolName,FunctionCall functionCall) {
        if(this.toolCalls == null){
            toolCalls = new LinkedHashMap<>();
        }
        this.toolCalls.put(toolName, functionCall);
        return (T)this;
    }
    
    

    public String getPrompt() {
        return prompt;
    }
    public String getSystemPrompt() {
        return systemPrompt;
    }

    public T setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
        return (T)this;
    }

    public T addHeader(String key,String value){
        if(headers == null){
            headers = new LinkedHashMap<>();
        }
        headers.put(key, value);
        return (T)this;
    }

    public Map getHeaders() {
        return headers;
    }
    
    public boolean containsHeader(String key){
        if(headers == null)
            return false;
        return headers.containsKey(key);
    }

    /**
     * 构建流式风格接口ChatObject对象
     * @param clientConfiguration
     * @param agentAdapter
     * @return
     */
    public ChatObject buildChatObject(ClientConfiguration clientConfiguration, AgentAdapter agentAdapter){
        return null;
    }
    public T setPrompt(String prompt) {
        this.prompt = prompt;
        return (T)this;
    }

    public String getModel() {
        return model;
    }

    public T setModel(String model) {
        this.model = model;
        return (T)this;
    }

    public Map getParameters() {
        return parameters;
    }

    public T setParameters(Map parameters) {
        this.parameters = parameters;
        return (T)this;
    }

    public T addParameter(String key,Object value){
        if(parameters == null){
            parameters = new LinkedHashMap<>();
        }
        parameters.put(key, value);
        return (T)this;
    }

    /**
     * 往值类型为Map的参数中添加key和value对
     * @param mapKey
     * @param key
     * @param value
     * @return
     */
    public T addMapParameter(String mapKey,String key,Object value){
        Map data = null;
        if(parameters == null){
            parameters = new LinkedHashMap<>();
            data = new LinkedHashMap();
            parameters.put(mapKey,data);
        }
        else{
            data = (Map)parameters.get(mapKey);
            if(data == null) {
                data = new LinkedHashMap();
                parameters.put(mapKey, data);
            }
        }

        data.put(key, value);
        return (T)this;
    }
//    
//    public String getModelType() {
//		return modelType;
//	}
//    
//    public T setModelType(String modelType) {
//		this.modelType = modelType;
//		return (T)this;
//	}

    public Boolean getStream() {
        return stream;
    }

    public T setStream(Boolean stream) {
        this.stream = stream;
        return (T)this;
    }
    
    public Double getTemperature() {
		return temperature;
	}

    public T setTemperature(Double temperature) {
        this.temperature = temperature;
        return (T)this;
    }
    public Integer getMaxTokens() {
        return maxTokens;
    }

    public T setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
        return (T)this;
    }
    private boolean init = false;
    public void init(){
        if(init)
            return;
        init = true;
        
        if(this.toolsRegist != null ){
            List<FunctionToolDefine> functionToolDefines = this.toolsRegist.registTools();
            if(functionToolDefines != null && functionToolDefines.size() > 0){
                FunctionCall functionCall = null;
                for(FunctionToolDefine functionToolDefine:functionToolDefines){
                    functionCall = functionToolDefine.getFunctionCall();
                    if(functionCall == null){
                        functionCall = toolsRegist.getFunctionCall(functionToolDefine.getFunction().getName());
                        if(functionCall != null){
                            functionToolDefine.setFunctionCall(functionCall);
                        }
                    }
                }
                this.registTools(functionToolDefines);
            }
        }

        if(this.toolCalls != null && toolCalls.size() > 0){
            for(Map.Entry<String,FunctionCall> entry:toolCalls.entrySet()){
                FunctionCall functionCall = entry.getValue();
                String toolName  = entry.getKey();
                if(tools !=  null && tools.size() > 0) {
                    for (FunctionToolDefine functionToolDefine : tools) {
                        if (functionToolDefine.getFunction().getName().equals(toolName)) {
                            functionToolDefine.setFunctionCall(functionCall);
                            break;
                        }
                    }
                }

            }
        }
        if(tools !=  null && tools.size() > 0) {

            for (FunctionToolDefine functionToolDefine : tools) {
                FunctionCall functionCall = functionToolDefine.getFunctionCall();
                if (functionCall != null) {
                    String toolName = functionToolDefine.getFunction().getName();
                    if (toolCalls == null) {
                        toolCalls = new LinkedHashMap<>();
                    }
                    if (!toolCalls.containsKey(toolName))
                        toolCalls.put(toolName, functionCall);
                }

            }
        }
    }
    
    public FunctionCall getFunctionCall(String toolName){
        if(toolCalls == null)
            return null;
        return toolCalls.get(toolName);
    }

    public T setToolsRegist(ToolsRegist toolsRegist) {
        this.toolsRegist = toolsRegist;
        return (T)this;
    }
}
