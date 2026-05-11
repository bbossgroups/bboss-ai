package org.frameworkset.spi.ai.util;
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

import com.frameworkset.util.SimpleStringUtil;
import org.frameworkset.spi.ai.adapter.AgentAdapter;
import org.frameworkset.spi.ai.model.FunctionTool;
import org.frameworkset.spi.ai.model.StreamData;
import org.frameworkset.spi.ai.model.TokenMetrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author biaoping.yin
 * @Date 2026/2/24
 */
public abstract class BaseStreamDataBuilder implements StreamDataBuilder{
    private TokenMetrics tokenMetrics;
    /**
     * stream模式下工具识别对象
     */
    private StreamData toolCallsStreamData;
    
    private boolean toolResolved;
    /**
     * stream模式下思考工具识别过程对象
     */
    private StringBuilder toolCallThinkingStreamData;

    /**
     * stream模式下工具识别过程对象
     */
    private StringBuilder toolCallContentStreamData;
    /**
     * stream模式下完整返回消息对象
     */
    private StringBuilder fullStreamData;

    /**
     * stream模式下完整返回思考推理消息对象
     */
    private StringBuilder fullReasoningStreamData;

    public StreamData getToolCallsStreamData() {
        return toolCallsStreamData;
    }
    
    public void appendFullStreamData(StreamData streamData){
        if(fullStreamData == null)
            fullStreamData = new StringBuilder();
        if(streamData.getContent() != null) {
            fullStreamData.append(streamData.getContent());
        }
    }

    public void appendFullReasoningStreamData(StreamData streamData){
        if(fullReasoningStreamData == null)
            fullReasoningStreamData = new StringBuilder();
        if(streamData.getReasoningContent() != null)
            fullReasoningStreamData.append(streamData.getReasoningContent());
    }
    
    public String getFullStreamData() {
        if(fullStreamData != null) {
            return fullStreamData.toString();
        }
        return null;
    }
    public String getFullReasoningStreamData() {
        if(fullReasoningStreamData != null) {
            return fullReasoningStreamData.toString();
        }
        return null;
    }

    public TokenMetrics getTokenMetrics() {
        return tokenMetrics;
    }
    public TokenMetrics buildTokenMetrics(Map usage){
        TokenMetrics tokenMetrics = new TokenMetrics();
        tokenMetrics.setUsage(usage);
        tokenMetrics.setTotalTokens((Integer)usage.get("total_tokens"));
        tokenMetrics.setPromptTokens((Integer)usage.get("prompt_tokens"));
        tokenMetrics.setCompletionTokens((Integer)usage.get("completion_tokens"));
        Map completion_tokens_details = (Map)usage.get("completion_tokens_details");
        
        tokenMetrics.setReasoningTokens((Integer)completion_tokens_details.get("reasoning_tokens"));
        
        Map prompt_tokens_details = (Map)usage.get("prompt_tokens_details");
        tokenMetrics.setCachedTokens((Integer)prompt_tokens_details.get("cached_tokens"));
        return tokenMetrics;
    }

    public StreamData buildWrapped(AgentAdapter agentAdapter , String line){
        StreamData streamData = build(agentAdapter,line);
        if(streamData.isContent()){
            appendFullStreamData(streamData);
        }
        else if(streamData.isReasoning()){
            appendFullReasoningStreamData(streamData);
        }
        computeTokens(streamData);
        return streamData;
    }
    
    private void computeTokens(StreamData streamData){
        TokenMetrics streamTokenMetrics = streamData.getStreamTokenMetrics();
        if(streamTokenMetrics != null) {
            if(tokenMetrics == null)
                tokenMetrics = new TokenMetrics();
            tokenMetrics.increaseTotalTokens(streamTokenMetrics.getTotalTokens());
            tokenMetrics.increasePromptTokens(streamTokenMetrics.getPromptTokens());
            tokenMetrics.increaseCompletionTokens(streamTokenMetrics.getCompletionTokens());
            tokenMetrics.increaseReasoningTokens(streamTokenMetrics.getReasoningTokens());
            tokenMetrics.increaseCachedTokens(streamTokenMetrics.getCachedTokens());
        }
        streamData.setTotalTokenMetrics(tokenMetrics);
    }
    public void appendToolCallThinkingStreamData(StreamData streamData){
       String thinkContent = null;
        String content = null;
        if(streamData.isReasoning()){
            
            if(streamData.getContent() != null){
                thinkContent = streamData.getContent();
                 
            }
        }
        else if(streamData.getReasoningContent() != null){
            thinkContent = streamData.getReasoningContent() ;
        }
        else if(streamData.isContent()   && streamData.getContent() != null){
            content = streamData.getContent() ;
        }
        if(thinkContent != null){
            if(toolCallThinkingStreamData == null)
                toolCallThinkingStreamData = new StringBuilder();
            toolCallThinkingStreamData.append(thinkContent);
        }
        if(content != null){
            if(toolCallContentStreamData == null)
                toolCallContentStreamData = new StringBuilder();
            toolCallContentStreamData.append(content);
        }
       
    }
    public String getToolCallThinkingStreamData() {
        if(toolCallThinkingStreamData != null) {
            return toolCallThinkingStreamData.toString();
        }
        return null;
    }
    
    public String getToolCallContentStreamData() {
        if(toolCallContentStreamData != null) {
            return toolCallContentStreamData.toString();
        }
        return null;
    }
 
    public void appendToolCallsStreamData(StreamData streamData){
        if(toolCallsStreamData == null)
            toolCallsStreamData = streamData;
        else
            toolCallsStreamData.appendToolCallsStreamData(streamData);
    }

    public boolean isToolCall(String finishReason){
        if(finishReason != null && finishReason.equals("tool_calls")){
            return true;
        }
        return false;
    }
    
    public boolean isToolResolved() {
        return toolResolved;
    }

    public void setToolResolved(boolean toolResolved) {
        this.toolResolved = toolResolved;
    }

    public StreamData functionTools(List<Map> tool_calls, String finishReason){

        if(tool_calls != null) {
            //tool_calls -> {ArrayList@5174}  size = 1
//            List<Map> tool_calls  = (List)message.get("tool_calls");
            if(tool_calls != null && tool_calls.size() > 0) {
                List<FunctionTool> functionTools = new ArrayList<>();
                for (Map tool_call : tool_calls) {
                    FunctionTool functionTool = new FunctionTool();
                    functionTool.setId((String)tool_call.get("id"));
                    functionTool.setIndex((Integer)tool_call.get("index"));
                    functionTool.setType((String)tool_call.get("type"));
                    Map function = (Map)tool_call.get("function");
                    String arguments = (String)function.get("arguments");
                    if(arguments != null) {
                        functionTool.setArguments(SimpleStringUtil.json2Object(arguments,Map.class));
                    }
                    functionTool.setFunctionName((String)function.get("name"));
                    functionTools.add(functionTool);
                }

                return new StreamData(functionTools,tool_calls,finishReason);

            }
            else{
                return new StreamData(null,null,finishReason);
            }


        }
        return null;
    }

    public FunctionTool functionTool(StringBuilder argumentsBuilder,Map tool_call ){        
            //tool_calls -> {ArrayList@5174}  size = 1
//            List<Map> tool_calls  = (List)message.get("tool_calls");

            FunctionTool functionTool = new FunctionTool();
            functionTool.setId((String)tool_call.get("id"));
            functionTool.setIndex((Integer)tool_call.get("index"));
            functionTool.setType((String)tool_call.get("type"));
            Map function = (Map)tool_call.get("function");
            String arguments = (String)function.get("arguments");
            argumentsBuilder.append( arguments);
            functionTool.setFunctionName((String)function.get("name"));                  

            return functionTool;        
      
    }

    public void appendArguments(StringBuilder argumentsBuilder, Map tool_call ){
        //tool_calls -> {ArrayList@5174}  size = 1
//            List<Map> tool_calls  = (List)message.get("tool_calls");

       
        Map function = (Map)tool_call.get("function");
        String arguments = (String)function.get("arguments");
        argumentsBuilder.append( arguments);

       
    }
    public StreamData functionToolsChunk(List<Map> tool_calls, String finishReason){

        if(tool_calls != null && tool_calls.size() > 0) {
            //tool_calls -> {ArrayList@5174}  size = 1
//            List<Map> tool_calls  = (List)message.get("tool_calls");
            

            return new StreamData(  tool_calls.get(0),finishReason);

        }
        else{
            return new StreamData(null,null,finishReason);
        }


        
    }

    public List<FunctionTool> getFunctionTools() {
        if(toolCallsStreamData != null){
            return toolCallsStreamData.getFunctionTools();
        }
        else{
            return null;
        }
    }
}
