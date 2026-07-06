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
import org.frameworkset.spi.ai.AIAgent;
import org.frameworkset.spi.ai.adapter.AgentAdapter;
import org.frameworkset.spi.ai.model.*;
import org.frameworkset.spi.ai.store.SessionMessage;
import org.frameworkset.util.concurrent.BooleanWrapperInf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.FluxSink;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author biaoping.yin
 * @Date 2026/2/24
 */
public abstract class BaseStreamDataBuilder implements StreamDataBuilder{
    private static Logger logger = LoggerFactory.getLogger(BaseStreamDataBuilder.class);
    private Long startTime;
    private Long endTime;
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
        
        if(streamData.getContent() != null) {
            if(fullStreamData == null)
                fullStreamData = new StringBuilder();
            fullStreamData.append(streamData.getContent());
        }
    }

    public void appendFullReasoningStreamData(StreamData streamData){
        
        if(streamData.getContent() != null) {
            if (fullReasoningStreamData == null) {
                fullReasoningStreamData = new StringBuilder();
            }
            fullReasoningStreamData.append(streamData.getContent());
        }
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
        Integer total_tokens = (Integer)usage.get("total_tokens");
        if(total_tokens != null) {
            tokenMetrics.setTotalTokens(total_tokens);
        }
        Integer prompt_tokens = (Integer)usage.get("prompt_tokens");
        if(prompt_tokens != null) {
            tokenMetrics.setPromptTokens(prompt_tokens);
        }
        Integer completion_tokens = (Integer)usage.get("completion_tokens");
        if(completion_tokens != null) {
            tokenMetrics.setCompletionTokens(completion_tokens);
        }
		Integer prompt_cache_hit_tokens = (Integer)usage.get("prompt_cache_hit_tokens");
		if(prompt_cache_hit_tokens != null) {
            tokenMetrics.setPromptCacheHitTokens(prompt_cache_hit_tokens);
        }
		
		Integer prompt_cache_miss_tokens = (Integer)usage.get("prompt_cache_miss_tokens");
		if(prompt_cache_miss_tokens != null) {
            tokenMetrics.setPromptCacheMissTokens(prompt_cache_miss_tokens);
        }
        
        Map completion_tokens_details = (Map)usage.get("completion_tokens_details");
        if(completion_tokens_details != null) {
            Integer reasoning_tokens = (Integer) completion_tokens_details.get("reasoning_tokens");
            if(reasoning_tokens != null)
                tokenMetrics.setCompletionReasoningTokens(reasoning_tokens);
            Integer text_tokens = (Integer) completion_tokens_details.get("text_tokens");
            if(text_tokens != null)
                tokenMetrics.setCompletionTextTokens(text_tokens);
        }
        
        Map prompt_tokens_details = (Map)usage.get("prompt_tokens_details");
        if(prompt_tokens_details != null) {
            Integer cached_tokens = (Integer) prompt_tokens_details.get("cached_tokens");
            if(cached_tokens != null)
                tokenMetrics.setPromptCachedTokens(cached_tokens);

            Integer text_tokens = (Integer) prompt_tokens_details.get("text_tokens");
            if(text_tokens != null)
                tokenMetrics.setPromptTextTokens(text_tokens);
        }
        return tokenMetrics;
    }

    public StreamData buildWrapped(AgentAdapter agentAdapter , String line){
        StreamData streamData = build(agentAdapter,line);
        if(streamData == null){
            return null;
        }
        if(streamData.isContent()){
            appendFullStreamData(streamData);
        }
        else if(streamData.isReasoning()){
            appendFullReasoningStreamData(streamData);
        }
        computeTokens(streamData);
        streamData.setMaas(this.getMaas());
        return streamData;
    }

    public String addChatWithToolCallSessionMessage(TokenMetrics tokenMetrics, FluxSink<ServerEvent> sink,
                                                    BooleanWrapperInf firstEventTag, List<FunctionTool> functionTools){
        String data = null;
        StringBuilder newData = new StringBuilder();
        if(fullReasoningStreamData != null){
            tokenMetrics.setReasoningData(fullReasoningStreamData.toString());
//            newData.append("<reasoning>").append(fullReasoningStreamData.toString()).append("</reasoning>\r\n");
        }
        if(this.fullStreamData != null && this.fullStreamData.length() > 0){
            newData. append(fullStreamData);

        }
        AIAgent agent = this.getChatObject().getAgent();
        if(this.fullStreamData != null && this.fullStreamData.length() > 0) {
            data = newData.toString(); 
        }         
        else{
            logger.warn("No content generated by agent {},set data to empty string", this.getChatObject().getAgent().getAgentName());
            if(functionTools != null && functionTools.size() > 0) {
                StringBuilder builder = new StringBuilder();
                int i = 0;
                for (FunctionTool functionTool : functionTools) {
                    if (i > 0) {
                        builder.append(",");
                    }
                    builder.append(functionTool.getFunctionName());
                    i++;
                }
                data = "匹配到工具：" + builder.toString() + "，准备执行工具。";
                ServerEvent serverEvent = new ServerEvent();

                 
                ChatObject chatObject = getChatObject();
                serverEvent.setAgent(chatObject.getAgent());
                if (firstEventTag.get()) {
                    firstEventTag.set(false);
                    serverEvent.setFirst(true);
                }
                
                serverEvent.setData(data);
                serverEvent.setType(ServerEvent.TYPE_AGENT);             
             
                sink.next(serverEvent);
            }
            else{
                
                data = "";
            }
        }
        TraceMessage traceMessage = new TraceMessage();
        Map<String, Object> assistantMessage = MessageBuilder.buildMessage(SessionMessage.MESSAGE_TYPE_TOOLSEARCH_MESSAGE_NAME,data );
        traceMessage.setMessage(assistantMessage);
        agent.recordTraceMessage(  traceMessage,tokenMetrics);
        return data;
    }
    public String addAgentResultSessionMessage(TokenMetrics tokenMetrics){
        String data = null;
        StringBuilder newData = new StringBuilder();
        if(fullReasoningStreamData != null){
            tokenMetrics.setReasoningData(fullReasoningStreamData.toString());
//            newData.append("<reasoning>").append(fullReasoningStreamData.toString()).append("</reasoning>\r\n");
        }
        if(this.fullStreamData != null){
            newData. append(fullStreamData);
           
        }
        if(newData.length() > 0) {
            data = newData.toString();
//            agentMessage.addAgentResultSessionMessage(  tokenMetrics,data, this.getChatObject().getAiAgent());
            
        }
        else{
            data = "没有返回内容！";
            logger.warn("No content generated by agent {},set data to empty string", this.getChatObject().getAgent().getAgentName());
        }
        AgentResultSessionMessageContext agentResultSessionMessageContext = new AgentResultSessionMessageContext();
        agentResultSessionMessageContext.setTokenMetrics(tokenMetrics);
        this.getChatObject().getAgent().addAgentResultSessionMessage(agentResultSessionMessageContext, data);
        return data;
    }
    private void computeTokens(StreamData streamData){
        TokenMetrics streamTokenMetrics = streamData.getStreamTokenMetrics();
        if(streamTokenMetrics != null) {
            if(tokenMetrics == null) {
                tokenMetrics = new TokenMetrics();
                tokenMetrics.setModel(streamTokenMetrics.getModel());
                tokenMetrics.setStartTime(this.getStartTime());
                tokenMetrics.setMaas(streamTokenMetrics.getMaas());
            }
            tokenMetrics.increaseTotalTokens(streamTokenMetrics.getTotalTokens());
            tokenMetrics.increasePromptTokens(streamTokenMetrics.getPromptTokens());
            tokenMetrics.increaseCompletionTokens(streamTokenMetrics.getCompletionTokens());
            tokenMetrics.increaseCompletionReasoningTokens(streamTokenMetrics.getCompletionReasoningTokens());
            tokenMetrics.increaseCompletionTextTokens(streamTokenMetrics.getCompletionTextTokens());
            tokenMetrics.increasePromptCachedTokens(streamTokenMetrics.getPromptCachedTokens());
            tokenMetrics.increasePromptTextTokens(streamTokenMetrics.getPromptTextTokens());
			tokenMetrics.increasePromptCacheHitTokens(streamTokenMetrics.getPromptCacheHitTokens());
			tokenMetrics.increasePromptCacheMissTokens(streamTokenMetrics.getPromptCacheMissTokens());
            tokenMetrics.setEndTime(streamTokenMetrics.getEndTime());
        }
        streamData.setTotalTokenMetrics(tokenMetrics);
    }

    public void setTokenMetrics(TokenMetrics tokenMetrics) {
        this.tokenMetrics = tokenMetrics;
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

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }
}
