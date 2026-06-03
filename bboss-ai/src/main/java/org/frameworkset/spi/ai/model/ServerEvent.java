package org.frameworkset.spi.ai.model;
/**
 * Copyright 2025 bboss
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
import com.fasterxml.jackson.annotation.JsonInclude;
import org.frameworkset.spi.ai.AIAgent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 封装流式调用数据报文：
 * type 字段表示数据报文类型，0表示数据报文，1表示错误报文
 * data 字段包含数据内容，当type为0时，data字段包含数据内容，当type为1时，data字段包含错误信息
 * @author biaoping.yin
 * @Date 2025/10/19
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServerEvent extends MultimodalGeneration implements AIEvent{
    /**
     * 数据消息
     */
    public static final int TYPE_DATA = 0;
    /**
     * 异常消息
     */
    public static final int TYPE_ERROR = 1;

    /**
     * trace信息，traceId
     */
    public static final int TYPE_TRACE = 2;
    /**
     * 拒绝消息：
     */
    public static final int TYPE_REFUSAL = 3;
    
    /**
     * 知识库资料消息：
     */
    public static final int TYPE_RAG_KNOWLEDGE = 5;

    /**
     * 步骤消息：
     */
    public static final int TYPE_STEP = 6;

    /**
     * 数据类型，0表示答案内容，1表示思维链内容, 2 表示工具调用，3 表示mcp服务调用，5 表示监控对象，默认值为0
     */
    public static final int CONTENT = 0;
    public static final int REASONING_CONTENT = 1;
    public static final int TOOL_CALLS = 2;

    public static final int TOKEN_METRICS = 5;
    public static final int MCP_TOOL_CALLS = 3;
    private Double confidence;
    /**
     * 数据内容
     * 当type为0时 数据消息，当type1时表示异常消息,当type为2时表示trace信息，traceId 当type为3时表示拒答消息，当type为5时表示知识库资料消息 
     */
    private String data;

    /**
     * 加工后的url地址
     */
    private String url; 
    /**
     * 获取模型生成的url地址
     */
    private String genUrl;
    /**
     * 扩展数据
     */
    private Map<String,Object> extendDatas ;
    
    private List<FunctionTool> functionTools;


    /**
     * 原始工具调用数据：工具调用列表
     */
    private List<Map> toolCalls;

    /**
     * 工具返回数据：角色
     */
    private String role;
    /**
     * 工具返回数据：内容
     */
    private String content;

    /**
     * 工具返回数据：推理内容
     */

    private String reasoningContent;

    /**
     * 数据报文类型:0 数据消息，1表示异常消息,2 trace信息，traceId 3 拒答消息 5 知识库资料消息 6 步骤消息
     * 默认值为0     
     */
    private int type = TYPE_DATA;

    /**
     * 数据内容类型，0表示答案内容，1表示思维链内容, 2 表示工具调用，3 表示mcp服务调用，5 表示监控对象，默认值为0     *  
     */
    private int contentType = CONTENT;

    /**
     * 标识是否是执行工具调用，分析工具调用结果数据后，返回的结果内容
     * true 是，false 不是
     */
    private boolean toolCallResponse;
    
    /**
     * 标记数据获取是否完成
     */
    private boolean done;
    private List ragKnowledge;


    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    private String traceId;

    private TokenMetrics tokenMetrics;

    /**
     * 步骤消息：智能体id
     */
    private String agentId;
    /**
     * 步骤消息：智能体名称
     */
    private String agentName;

    /**
     * 步骤消息：父智能体id
     */
    private String parentAgentId;
    /**
     * 步骤消息：父智能体名称
     */
    private String parentAgentName;
    
    


    /**
     * 是否是第一个数据报文
     */
    private boolean first;
    @JsonIgnore
    private AIAgent agent;
    
    private String fullStreamData;
    public String getFullStreamData() {
        return fullStreamData;
    }
    public void setFullStreamData(String fullStreamData) {
        this.fullStreamData = fullStreamData;
    }

    /**
     * 数据报文类型:0 数据消息，1表示异常消息,2 trace信息，traceId 3 拒答消息 5 知识库资料消息 6 步骤消息
     * 默认值为0     
     * @return 
     */
    public int getType() {
        return type;
    }
    
    public AIAgent getAgent() {
        return agent;
    }

    public void setAgent(AIAgent agent) {
        this.agent = agent;
    }

    /**
     * 数据报文类型:0 数据消息，1表示异常消息,2 trace信息，traceId 3 拒答消息 5 知识库资料消息 6 步骤消息
     * 默认值为0     
     */
    public void setType(int type) {
        this.type = type;
    }
    /**
     * 数据内容
     * 当type为0时 数据消息，当type1时表示异常消息,当type为2时表示trace信息，traceId 当type为3时表示拒答消息，当type为5时表示知识库资料消息 
     */
    public String getData() {
        return data;
    }
    /**
     * 数据内容
     * 当type为0时 数据消息，当type1时表示异常消息,当type为2时表示trace信息，traceId 当type为3时表示拒答消息，当type为5时表示知识库资料消息 
     */
    public void setData(String data) {
        this.data = data;
    }

    /**
     * 设置扩展数据
     * @param extendDatas
     */
    public void setExtendDatas(Map<String, Object> extendDatas) {
        this.extendDatas = extendDatas;
    }
    
    /**
     * 添加扩展数据
     * @param name
     * @param value
     * @return
     */
    public ServerEvent addExtendData(String name,Object value){
        if(extendDatas == null){
            extendDatas = new LinkedHashMap<>();            
        }
        extendDatas.put(name,value);
        return this;
    }

    /**
     * 添加扩展数据
     * @param extendDatas
     * @return
     */
    public ServerEvent addExtendDatas(Map<String, Object> extendDatas){
        if(extendDatas == null){
            extendDatas = new LinkedHashMap<>();
        }
        extendDatas.putAll(extendDatas);
        return this;
    }

    /**
     * 获取扩展数据
     * @return
     */
    public Map<String, Object> getExtendDatas() {
        return extendDatas;
    }
    
    public boolean isDone() {
		return done;
	}

    public void setDone(boolean done) {
        this.done = done;
    }
    /**
     * 是否是第一个数据报文
     * @return
     */
    public boolean isFirst() {
        return first;
    }
    /**
     * 设置是否是第一个数据报文
     * @param first
     */
    public void setFirst(boolean first) {
        this.first = first;
    }


    /**
     * 数据内容类型，0表示答案内容，1表示思维链内容, 2 表示工具调用，3 表示mcp服务调用，5 表示监控对象，默认值为0     *  
     */
    public int getContentType() {
        return contentType;
    }

    /**
     * 数据内容类型，0表示答案内容，1表示思维链内容, 2 表示工具调用，3 表示mcp服务调用，5 表示监控对象，默认值为0     *  
     */
    public void setContentType(int contentType) {
        this.contentType = contentType;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getGenUrl() {
        return genUrl;
    }

    public void setGenUrl(String genUrl) {
        this.genUrl = genUrl;
    }
    public List<FunctionTool> getFunctionTools() {
        return functionTools;
    }
    public void setFunctionTools(List<FunctionTool> functionTools) {
        this.functionTools = functionTools;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public String getRole() {
        return role;
    }

    public List<Map> getToolCalls() {
        return toolCalls;
    }

    public void setToolCalls(List<Map> toolCalls) {
        this.toolCalls = toolCalls;
    }

    public String getReasoningContent() {
        return reasoningContent;
    }

    public void setReasoningContent(String reasoningContent) {
        this.reasoningContent = reasoningContent;
    }
    
    public boolean finished(){
        return finishReason != null && finishReason.toLowerCase().equals("stop");
    }

    public boolean isToolCallsType() {
        return contentType == TOOL_CALLS;
    }
    
    public boolean isToolCallResponse(){
        return toolCallResponse;
    }

    public void setToolCallResponse(boolean toolCallResponse) {
        this.toolCallResponse = toolCallResponse;
    }
    public TokenMetrics getTokenMetrics() {
        return tokenMetrics;
    }

    public void setTokenMetrics(TokenMetrics tokenMetrics) {
        this.tokenMetrics = tokenMetrics;
    }
 
    public List getRagKnowledge() {
        return ragKnowledge;
    }
    public void setRagKnowledge(List ragKnowledge) {
        this.ragKnowledge = ragKnowledge;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public String getParentAgentId() {
        return parentAgentId;
    }

    public void setParentAgentId(String parentAgentId) {
        this.parentAgentId = parentAgentId;
    }

    public String getParentAgentName() {
        return parentAgentName;
    }

    public void setParentAgentName(String parentAgentName) {
        this.parentAgentName = parentAgentName;
    }
}
