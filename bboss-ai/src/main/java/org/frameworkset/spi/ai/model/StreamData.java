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


import java.util.List;
import java.util.Map;

/**
 * @author biaoping.yin
 * @Date 2025/10/29
 */
public class StreamData {

    private int type = ServerEvent.CONTENT;
//    private String data;
    /**
     * 解析后工具调用数据：工具调用列表
     */
    private List<FunctionTool> functionTools;



    /**
     * 原始工具调用数据：工具调用列表
     */
    private List<Map> toolCalls;

    private boolean buildToolCallsFinished;
    private  String finishReason;
    private boolean done;
    private String url;
    /**
     * 工具返回数据：角色
     */
    private String role;
    /**
     * 工具返回数据：内容
     */
    private String content;



    private String reasoningContent;
    private Map toolCallsChunk;
    private List<StreamData> toolCallsStreamDatas;
    public StreamData(){
    }
    public StreamData(int type, String data, String url,String finishReason){
    	this.type = type;
    	this.content = data;
        this.finishReason = finishReason;
 
        this.url = url;
    }
    public StreamData(int type, String data, String finishReason){
        this.type = type;
        this.content = data;
        this.finishReason = finishReason;
     
    }

    public StreamData(List<FunctionTool> functionTools, List<Map> toolCalls, String finishReason){
        this.type = ServerEvent.TOOL_CALLS;
        this.functionTools = functionTools;
        this.finishReason = finishReason;
 
        this.toolCalls = toolCalls;
        this.buildToolCallsFinished = true;
    }

    public StreamData(Map toolCallsChunk , String finishReason){
        this.type = ServerEvent.TOOL_CALLS;
        this.finishReason = finishReason;
        this.toolCallsChunk = toolCallsChunk; 
    }

    public StreamData(int type, String data, String url, String finishReason,boolean done){
        this.type = type;
        this.content = data;
        this.finishReason = finishReason;
    
        this.done = done;
        this.url = url;
    }
    public StreamData(int type, String data, String finishReason,boolean done){
        this.type = type;
        this.content = data;
        this.finishReason = finishReason;
      
        this.done = done;
    }

    public StreamData setRole(String role) {
        this.role = role;
        return this;
    }
    public boolean isToolCalls(){
        return type == ServerEvent.TOOL_CALLS;
    }
    public boolean isBuildToolCallsFinished() {
        return buildToolCallsFinished;
    }

    public StreamData setContent(String content) {
        this.content = content;
        return this;
    }

    public String getContent() {
        return content;
    }

    public String getRole() {
        return role;
    }

    public boolean isDone() {
        return done;
    }

    public int getType() {
		return type;
	}
    
    public StreamData setType(int type) {
		this.type = type;
        return this;
	}
    public String getReasoningContent() {
        return reasoningContent;
    }

    public StreamData setReasoningContent(String reasoningContent) {
        this.reasoningContent = reasoningContent;
        return this;
    }
    public boolean isEmpty(){
        return content == null || content.length() == 0;
    }
    
    public String getFinishReason() {
		return finishReason;
	}

    public StreamData setFinishReason(String finishReason) {
        this.finishReason = finishReason;
        return this;
    }
    
    public String getUrl() {
		return url;
	}

    public void setFunctionTools(List<FunctionTool> functionTools) {
        this.functionTools = functionTools;
    }

    public List<FunctionTool> getFunctionTools() {
        return functionTools;
    }

    public List<Map> getToolCalls() {
        return toolCalls;
    }

    public StreamData setToolCalls(List<Map> toolCalls) {
        this.toolCalls = toolCalls;
        return this;
    }

    public List<StreamData> getToolCallsStreamDatas() {
        return toolCallsStreamDatas;
    }

    public void appendToolCallsStreamData(StreamData streamData) {
        if(toolCallsStreamDatas == null){
            toolCallsStreamDatas = new java.util.ArrayList<>();
        }
        toolCallsStreamDatas.add(streamData);
    }

    public Map getToolCallsChunk() {
        return toolCallsChunk;
    }

    public void setToolCallsChunk(Map toolCallsChunk) {
        this.toolCallsChunk = toolCallsChunk;
    }

    public boolean isReasoning() {
        return type == ServerEvent.REASONING_CONTENT;
    }

    public boolean isContent() {
        return type == ServerEvent.CONTENT;
    }
}
