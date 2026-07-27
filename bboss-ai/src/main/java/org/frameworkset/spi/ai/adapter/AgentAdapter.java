package org.frameworkset.spi.ai.adapter;
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

import com.frameworkset.util.FileUtil;
import com.frameworkset.util.JsonUtil;
import com.frameworkset.util.SimpleStringUtil;
import org.frameworkset.spi.ai.AIAgent;
import org.frameworkset.spi.ai.callback.ChatContext;
import org.frameworkset.spi.ai.material.GenFileDownload;
import org.frameworkset.spi.ai.material.GenMaterialFileDownload;
import org.frameworkset.spi.ai.mcp.model.MCPToolCallResponse;
import org.frameworkset.spi.ai.model.*;
import org.frameworkset.spi.ai.tool.AgentTraceHolder;
import org.frameworkset.spi.ai.util.*;
import org.frameworkset.spi.reactor.SSEHeaderSetFunction;
import org.frameworkset.spi.remote.http.ClientConfiguration;
import org.frameworkset.spi.remote.http.HttpRequestProxy;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * 智能体适配器：针对不同厂家的模型平台服务进行适配，包括请求参数转换、结果转换等
 * @author biaoping.yin
 * @Date 2026/1/4
 */
public abstract class AgentAdapter implements CompletionsUrlInterface{
    private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AgentAdapter.class);
    protected GenFileDownload genFileDownload;
    private boolean inited;

    public String getReasoningContent( Map delta ){
		String reasoning_content = (String) delta.get("reasoning_content");
		return reasoning_content;
	} 
    protected AgentAdapter initAgentAdapter(){
        if(inited)
            return this;
        inited = true;
        genFileDownload = new GenMaterialFileDownload();
        return this;
    }

    public GenFileDownload getGenFileDownload() {
        return genFileDownload;
    }

    /**
     * 构建生成图片请求参数
     * @param imageAgentMessage
     * @return
     */
    protected abstract Map buildGenImageRequestMap(ImageAgentMessage imageAgentMessage,AIAgent aiAgent);

    
 
    protected void buildTools(ChatContext chatContext,AgentMessage agentMessage,AIAgent agent,Map<String, Object> requestMap){
        agent.init();
        List<FunctionToolDefine> tools = agent.getToolsByToolSearch(chatContext,agentMessage);
        if(tools != null && tools.size() > 0){
            chatContext.setAgentTools(tools);
//            Object tools = aiAgent.getTools();
            requestMap.put("tools",   tools);       
            if(agent.getEnableLoopToolCall() != null && agent.getEnableLoopToolCall())
                requestMap.put("tool_choice", "auto");            
            chatContext.setChatWithToolcall(true);
        }
    }

    public   float[] embedding(ClientConfiguration config,EmbeddingMessage embeddingMessage,AIAgent agent,Map<String,Object> params) {
        EmbeddingResponse result = HttpRequestProxy.sendJsonBody(embeddingMessage.getMaas(), params, getEmbeddingUrl(config,embeddingMessage), EmbeddingResponse.class);
        if(result != null){
            return result.embedding();
        }
        return null;
    }
    protected void filterParameters(ChatContext chatContext,AgentMessage agentMessage,AIAgent aiAgent,Map<String, Object> requestMap, Map<String, Object> parameters) {
        Boolean stream = chatContext.getStreamable();
		if(stream == null){
			stream = agentMessage.getStream();
		}
		if(SimpleStringUtil.isEmpty( parameters)){
            if( stream != null){
                requestMap.put("stream", stream);
            }

            if( agentMessage.getTemperature() != null){
                requestMap.put("temperature", agentMessage.getTemperature());
            }
            if(agentMessage.getMaxTokens() != null)
                requestMap.put("max_tokens", agentMessage.getMaxTokens());
            
        }
        else {
            requestMap.putAll( parameters);
            //设置默认参数
            if(!parameters.containsKey("stream") && stream != null){
                requestMap.put("stream", stream);
            }

            if(!parameters.containsKey("temperature") && agentMessage.getTemperature() != null){
                requestMap.put("temperature", agentMessage.getTemperature());
            }
            if(!parameters.containsKey("max_tokens") && agentMessage.getMaxTokens() != null){
                requestMap.put("max_tokens", agentMessage.getMaxTokens());
            }
            
        }
		//"stream_options": {"include_usage": true}
		Boolean includeUsage = agentMessage.getIncludeUsage();
		if(includeUsage != null){
			Map streamOptions = new HashMap();
			streamOptions.put("include_usage", includeUsage);
			requestMap.put("stream_options", streamOptions);
			
		}
        buildTools(chatContext,agentMessage,  aiAgent, requestMap);
    }
    protected Object handleImageParserMessages(List<Map<String, Object>> messages){
        return messages;
    }

    protected String getSystemPrompt(AgentMessage agentMessage, AIAgent aiAgent){
        return aiAgent.evalSystemPrompt(  agentMessage);
    }
    protected String getPrompt(AgentMessage agentMessage, AIAgent aiAgent){
        return aiAgent.evalPrompt(agentMessage);
    }
    public Map buildVideoVLRequestMap(VideoVLAgentMessage videoVLAgentMessage, AIAgent aiAgent,ChatContext chatContext) {
		// 构建消息历史列表，包含之前的会话记忆
		
		List<Map<String, Object>> sessionMemory = aiAgent.getSessionMemory(true);
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("model",videoVLAgentMessage.getModel());
        List<String > videoUrls = videoVLAgentMessage.getVideoUrls();

        Map<String, Object> userMessage = null;
        Map<String, Object> systemMessage = null;
        String prompt = getPrompt(  videoVLAgentMessage,   aiAgent);
        if(chatContext != null){
            prompt = chatContext.evalPrompt(prompt);
            
        }
        if(videoUrls != null && videoUrls.size() > 0) {
            userMessage = buildInputVideosMessage(prompt, videoUrls.toArray(new String[]{}));
        }
        else{
            userMessage = buildInputVideosMessage(prompt, (String[])null);
        }
      
        List<Map<String, Object>> messages = null;
        if(sessionMemory != null){
            if(sessionMemory.size() == 0){
                String systemPrompt = getSystemPrompt(videoVLAgentMessage,aiAgent);
                if(systemPrompt != null){
                    if(chatContext != null){
                        systemPrompt = chatContext.evalSystemPrompt(systemPrompt);

                    }
                    systemMessage = MessageBuilder.buildSystemMessage(systemPrompt);
                    videoVLAgentMessage.addSessionMessage(systemMessage,aiAgent);
                }
            }
            videoVLAgentMessage.addSessionMessage(userMessage,aiAgent);
            messages = new ArrayList<>(sessionMemory);
        }
        else{
            messages = new ArrayList<>();
            String systemPrompt = getSystemPrompt(videoVLAgentMessage,aiAgent);
            if(systemPrompt != null){
                if(chatContext != null){
                    systemPrompt = chatContext.evalSystemPrompt(systemPrompt);

                }
                systemMessage = MessageBuilder.buildSystemMessage(systemPrompt);
                messages.add(systemMessage);
            }
            messages.add(userMessage);
        }


        requestMap.put("messages", handleImageParserMessages(messages));
        Map parameters = videoVLAgentMessage.getParameters();

        filterParameters(chatContext,videoVLAgentMessage,aiAgent,requestMap,parameters);

        return requestMap;
    }
    
    public Map buildImageVLRequestMap(ImageVLAgentMessage imageAgentMessage, AIAgent aiAgent, ChatContext chatContext) {
		
		// 构建消息历史列表，包含之前的会话记忆
		
		List<Map<String, Object>> sessionMemory = aiAgent.getSessionMemory(true);
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("model",imageAgentMessage.getModel());
        List<String > imageUrls = imageAgentMessage.getImageUrls();

        Map<String, Object> userMessage = null;
        Map<String, Object> systemMessage = null;
        String prompt = getPrompt(  imageAgentMessage,   aiAgent);
        if(chatContext != null){
            prompt = chatContext.evalPrompt(prompt);
        }
        if(imageUrls != null && imageUrls.size() > 0) {           
            userMessage = buildInputImagesMessage(prompt, imageUrls.toArray(new String[]{}));
        }
        else{
            userMessage = buildInputImagesMessage(prompt, (String[])null);
        }
 
        List<Map<String, Object>> messages = null;
        if(sessionMemory != null){
            if(sessionMemory.size() == 0){
                String systemPrompt = getSystemPrompt(imageAgentMessage,aiAgent);
                if(systemPrompt != null){
                    if(chatContext != null){
                        systemPrompt = chatContext.evalSystemPrompt(systemPrompt);
                    }
                    systemMessage = MessageBuilder.buildSystemMessage(systemPrompt);
                    imageAgentMessage.addSessionMessage(systemMessage,aiAgent);
                }
            }
            imageAgentMessage.addSessionMessage(userMessage,aiAgent);
            messages = new ArrayList<>(sessionMemory);
        }
        else{
            messages = new ArrayList<>();
            String systemPrompt = getSystemPrompt(imageAgentMessage,aiAgent);
            if(systemPrompt != null){
                if(chatContext != null){
                    systemPrompt = chatContext.evalSystemPrompt(systemPrompt);
                }
                systemMessage = MessageBuilder.buildSystemMessage(systemPrompt);
                messages.add(systemMessage);
            }
            messages.add(userMessage);
        }
         

        requestMap.put("messages", handleImageParserMessages(messages));
        Map parameters = imageAgentMessage.getParameters();

        filterParameters(chatContext,imageAgentMessage,aiAgent,requestMap,parameters);

        return requestMap;
    }
    public boolean isDone(String data){
        return "[DONE]".equals(data);

    }
    
    public String getDoneData(){
        return "data:[DONE]";
    }
    public boolean isVideoParserDone(String data){
        return isDone(  data);

    }

    public String getVideoParserDoneData(){
        return getDoneData();
    }

    public boolean isImageParserDone(String data){
        return isDone(  data);

    }

    public String getImageParserDoneData(){
        return getDoneData();
    }
    /**
     * 处理音频识别流数据
     * {"output":{"audio":{"data":"xxxx",
     *   "expires_at":1769158890,
     *   "id":"audio_66356352-8808-49bd-9c9c-d0283a3e2eb1"},
     *   "finish_reason":"null"},
     *   "usage":{"characters":53},
     *   "request_id":"66356352-8808-49bd-9c9c-d0283a3e2eb1"}
     * @param data
     * @return
     */
    public StreamData parseAudioGenStreamContentFromData(String data){
        return AIResponseUtil.parseQianwenAudioGenStreamContentFromData(this,data);
    }

    /**
     * 语音识别：data:{"output":{"choices":[{"message":{"annotations":[{"type":"audio_info","language":"zh","emotion":"neutral"}],"content":[{"text":"欢迎与"}],"role":"assistant"},"finish_reason":"null"}]},"usage":{"output_tokens_details":{"text_tokens":6},"input_tokens_details":{"text_tokens":16},"seconds":1},"request_id":"e84128d5-4bae-4e7e-91ab-6fb33504d2e3"}
     * LLM和图像识别：data: {"id":"ccf32be6-ad2f-4658-963a-fc3c22346e6b","object":"chat.completion.chunk","created":1761725211,"model":"deepseek-reasoner","system_fingerprint":"fp_ffc7281d48_prod0820_fp8_kvcache","choices":[{"index":0,"delta":{"content":null,"reasoning_content":"在"},"logprobs":null,"finish_reason":null}]}
     * @param data
     * @return
     */
    public StreamData parseStreamContentFromData(BaseStreamDataBuilder streamDataBuilder, String data){
        return AIResponseUtil.parseStreamContentFromData(this,streamDataBuilder,data);
    }

    /**
     * 语音识别：data:{"output":{"choices":[{"message":{"annotations":[{"type":"audio_info","language":"zh","emotion":"neutral"}],"content":[{"text":"欢迎与"}],"role":"assistant"},"finish_reason":"null"}]},"usage":{"output_tokens_details":{"text_tokens":6},"input_tokens_details":{"text_tokens":16},"seconds":1},"request_id":"e84128d5-4bae-4e7e-91ab-6fb33504d2e3"}
     * LLM和图像识别：data: {"id":"ccf32be6-ad2f-4658-963a-fc3c22346e6b","object":"chat.completion.chunk","created":1761725211,"model":"deepseek-reasoner","system_fingerprint":"fp_ffc7281d48_prod0820_fp8_kvcache","choices":[{"index":0,"delta":{"content":null,"reasoning_content":"在"},"logprobs":null,"finish_reason":null}]}
     * @param data
     * @return
     */
    public StreamData parseImageParserStreamContentFromData(BaseStreamDataBuilder streamDataBuilder,String data){
        return AIResponseUtil.parseStreamContentFromData(this,streamDataBuilder,data);
    }

    public StreamData parseVideoParserStreamContentFromData(BaseStreamDataBuilder streamDataBuilder,String data){
        return AIResponseUtil.parseStreamContentFromData(this,streamDataBuilder,data);
    }

    /**
     * 语音识别数据解析
     * @param data
     * @return
     */
    public StreamData parseAudioStreamContentFromData(StreamDataBuilder streamDataBuilder,String data){
        return AIResponseUtil.parseAudioStreamContentFromData( this, streamDataBuilder,data);
    }

    
    
    /**
     * 获取图片识别模型智能问答请求参数类型
     * @return
     */
    public String getAIImageParserRequestType(){
        return AIConstants.AI_CHAT_REQUEST_BODY_JSON;
        
    }

    /**
     * 获取图片识别模型智能问答请求参数类型
     * @return
     */
    public String getAIVideoParserRequestType(){
        return AIConstants.AI_CHAT_REQUEST_BODY_JSON;

    }

    /**
     * 获取音频识别模型智能问答请求参数类型
     * @return
     */
    public String getAIAudioParsertRequestType(){
        return AIConstants.AI_CHAT_REQUEST_BODY_JSON;

    }

    /**
     * 获取智能问答请求参数类型
     * @return
     */
    public String getAIChatRequestType(){
        return AIConstants.AI_CHAT_REQUEST_BODY_JSON;

    }
    protected Map<String, Object> buildInputVideosMessage(String message,String... videoUrls) {
        return MessageBuilder.buildInputVideosMessage(message,videoUrls);
    }
    
    protected Map<String, Object> buildInputImagesMessage(String message,String... imageUrls) {
        return MessageBuilder.buildInputImagesMessage(message,imageUrls);
    }

    protected List<Map<String, Object>> buildInputToolMessages(ToolAgentMessage toolAgentMessage,AIAgent aiAgent,ChatObject chatObject) {
        List<FunctionTool> tools = toolAgentMessage.getFunctionTools();
        List<Map<String, Object>> toolMessages = new ArrayList<>(tools.size());
        try {
            AgentTraceHolder.setChatObject(chatObject);
            for (FunctionTool tool : tools) {
                String toolId = tool.getId();
                String functionName = tool.getFunctionName();
                FunctionCall functionCall = aiAgent.getFunctionCall(functionName);
                try {
                    if (functionCall == null) {
                        throw new FunctionCallException("FunctionCall of " + functionName + " is null.");
                    }
                    Object result = functionCall.call(tool);
                    if (result == null) {
                        throw new FunctionCallException("FunctionCall of " + functionName + " return null:" + JsonUtil.object2json(tool));
                    }
                    Map<String, Object> toolMessage = null;
                    if (result instanceof String)
                        toolMessage = MessageBuilder.buildToolMessage((String) result, toolId);
                    else if (result instanceof MCPToolCallResponse) {
                        result = ((MCPToolCallResponse) result).getResult();
                        toolMessage = MessageBuilder.buildToolMessage(JsonUtil.object2json(result), toolId);
                    } else {
                        toolMessage = MessageBuilder.buildToolMessage(JsonUtil.object2json(result), toolId);
                    }
                    toolMessages.add(toolMessage);
//                return toolMessage;

                } catch (Exception e) {
                    throw new FunctionCallException("Call tool function[" + functionName + "] failed:", e);
                }
            }
        }
        finally {
            AgentTraceHolder.removeChatObject();
        }
        return toolMessages;
    }
    protected Map<String, Object> buildInputToolMessage(ToolAgentMessage toolAgentMessage,AIAgent aiAgent) {
        FunctionTool tool = toolAgentMessage.getFunctionTool();
        String toolId = tool.getId();
        String functionName = tool.getFunctionName();
        FunctionCall functionCall = aiAgent.getFunctionCall(functionName);
        try {
            if(functionCall == null){
                throw new FunctionCallException("FunctionCall of "+ functionName +" is null.");
            }
            Object result = functionCall.call(tool);
            if(result == null){
                throw new FunctionCallException("FunctionCall of "+ functionName +" return null:"+JsonUtil.object2json(tool));
            }
            Map<String,Object> toolMessage = null;
            if(result instanceof String)
                toolMessage = MessageBuilder.buildToolMessage((String)result,toolId);
            else if (result instanceof MCPToolCallResponse){
                result = ((MCPToolCallResponse)result).getResult();
                toolMessage = MessageBuilder.buildToolMessage(JsonUtil.object2json(result),toolId);
            }
			else {
				toolMessage = MessageBuilder.buildToolMessage(JsonUtil.object2json(result),toolId);
			}
            return toolMessage;
        } catch (Exception e) {
            throw new FunctionCallException("Call tool function["+ functionName +"] failed:",e);
        }
    }
    /**
     * 构建智能问答请求参数
     * @param toolAgentMessage
     * @return
     */
    public Map buildOpenAIRequestMapWithTool(ToolAgentMessage toolAgentMessage, AIAgent agent,ChatObject chatObject,ChatContext chatContext){
//        Map<String, Object> userMessage = buildInputToolMessage(  toolAgentMessage,aiAgent);
		List<Map<String, Object>> sessionMemory = agent.getSessionMemory(true);
        List<Map<String, Object>> userMessages = buildInputToolMessages(  toolAgentMessage,agent,chatObject);
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("model", toolAgentMessage.getModel());

        List<Map<String, Object>> messages = null;
   
        if(sessionMemory != null){
            // 构建消息历史列表，包含之前的会话记忆           
            for(Map<String, Object> userMessage : userMessages) {

                // 添加当前用户消息
                toolAgentMessage.addSessionMessage(userMessage, agent);
            }
            messages = new ArrayList<>(sessionMemory);


        }
        else{
            messages = new ArrayList<>();
            for(Map<String, Object> userMessage : userMessages) {
                messages.add(userMessage);
                // 添加当前用户消息
//                toolAgentMessage.addSessionMessage(userMessage, aiAgent);
            }
            
        }
        requestMap.put("messages", messages);
		Boolean stream = chatContext.getStreamable();
		if(stream == null){
			stream = toolAgentMessage.getStream();
		}
        Map parameters = toolAgentMessage.getParameters();
        if(SimpleStringUtil.isNotEmpty( parameters)){

            requestMap.putAll(parameters);
            if(!parameters.containsKey("stream") && stream != null){
                requestMap.put("stream", stream);
            }
            if(!parameters.containsKey("temperature") && toolAgentMessage.getTemperature() != null){
                requestMap.put("temperature", toolAgentMessage.getTemperature());
            }

            if(!parameters.containsKey("max_tokens") && toolAgentMessage.getMaxTokens() != null){
                requestMap.put("max_tokens", toolAgentMessage.getMaxTokens());
            }
        }
        else {
            //设置默认参数
            if( stream != null){
                requestMap.put("stream", stream);
            }

            if( toolAgentMessage.getTemperature() != null){
                requestMap.put("temperature", toolAgentMessage  .getTemperature());
            }
            if( toolAgentMessage.getMaxTokens() != null){
                requestMap.put("max_tokens", toolAgentMessage.getMaxTokens());
            }
        }
		//"stream_options": {"include_usage": true}
		Boolean includeUsage = toolAgentMessage.getIncludeUsage();
		if(includeUsage != null){
			Map streamOptions = new HashMap();
			streamOptions.put("include_usage", includeUsage);
			requestMap.put("stream_options", streamOptions);
			
		}
        buildThinking(  toolAgentMessage, chatObject, requestMap);
        if(agent.getEnableLoopToolCall() != null && agent.getEnableLoopToolCall()) {
            int maxLoopToolCalls = agent.getMaxLoopToolCalls();
            boolean buildTools = true;
            if(maxLoopToolCalls > 0){
                int loopToolCalls = chatContext.increamentLoopToolCalls();
                //判断工具调用轮次是否超过最大值，如果超过最大值，将不再往上下文中添加工具调用信息
                if(loopToolCalls > maxLoopToolCalls){                    
                    buildTools = false;
                    logger.info("Loop tool calls exceeds max loop tool calls {} and stop loop tool call.",  maxLoopToolCalls);
                }
            }
            if(buildTools) {
                buildTools(chatContext, toolAgentMessage, agent, requestMap);
            }
        }
        return requestMap;
    }
    
    protected void buildThinking(ChatAgentMessage chatAgentMessage,ChatObject chatObject,Map<String, Object> requestMap){
//        Map parameters = chatAgentMessage.getParameters();
        Boolean thinking = chatAgentMessage.getThinking();
        ChatContext chatContext = chatObject.getChatContext();
        if(chatContext != null && chatContext.getThinking() != null){
            thinking = chatContext.getThinking();
            
        }		
		if(thinking != null){
			if( thinking == false) {
				Map data = new LinkedHashMap();
				data.put("type", "disabled");
				requestMap.put("thinking", data);
				chatObject.setThinking(false);
			}
			else{
				Map data = new LinkedHashMap();
				data.put("type", "enabled");
				requestMap.put("thinking", data);
				chatObject.setThinking(true);
			}
		}
        
//        if(thinking != null){
//            if(parameters != null) {
//                if (!parameters.containsKey("thinking")) {
//                    Map data =  new LinkedHashMap();
//                    data.put("type", thinking?"enabled":"disabled");
//                    requestMap.put("thinking", data);
//                }
//            }
//            else{
////                chatAgentMessage.addMapParameter("thinking", "type", "enabled");//kimi-k2.5禁用思维模式,启用：enabled
//                Map data =  new LinkedHashMap();
//                data.put("type", thinking?"enabled":"disabled");
//                requestMap.put("thinking", data);
//            }
//            chatObject.setThinking(thinking);
//        }   
//        else{
//            if (parameters !=null && parameters.containsKey("thinking")) {
//                Map data =  (Map) parameters.get("thinking");
//                String type = (String)data.get("type");
//                if("enabled".equals(type)){
//                    chatObject.setThinking(true);
//                }
//                else{
//                    chatObject.setThinking(false);
//                }
//              
//            }
//        }
        
        
        
    }
    /**
     * 构建智能问答请求参数
     * @param chatAgentMessage
     * @return
     */
    public Map buildOpenAIRequestMap(ChatAgentMessage chatAgentMessage, AIAgent aiAgent,ChatObject chatObject, ChatContext chatContext) {
		
		List<Map<String, Object>> sessionMemory = aiAgent.getSessionMemory(true);
        String agentId = aiAgent.getAgentId();
        String message = getPrompt(  chatAgentMessage,   aiAgent);
        if(SimpleStringUtil.isEmpty(message)){
            throw new AIRuntimeException("Prompt message is empty.");
        }
		
        if(chatContext != null){
            message = chatContext.evalPrompt(message);
        }
        Map<String, Object> userMessage = MessageBuilder.buildUserMessage( message);
        Map<String,Object> systemMessage = null;
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("model", chatAgentMessage.getModel());

        List<Map<String, Object>> messages = null;
        if(sessionMemory != null){
            // 构建消息历史列表，包含之前的会话记忆           

            if(sessionMemory.size() == 0){
                String systemPrompt = getSystemPrompt(chatAgentMessage,aiAgent);
                if(systemPrompt != null){
                    if(chatContext != null){
                        systemPrompt = chatContext.evalSystemPrompt(systemPrompt);
                    }
                    systemMessage = MessageBuilder.buildSystemMessage(systemPrompt);
                    chatAgentMessage.addSessionMessage(systemMessage,message,aiAgent);
                }
            }
            // 添加当前用户消息
            chatAgentMessage.addSessionMessage(userMessage,agentId,aiAgent);
            messages = new ArrayList<>(sessionMemory);
            
            
        }
        else{
            messages = new ArrayList<>();
            String systemPrompt = getSystemPrompt(chatAgentMessage,aiAgent);
            if(systemPrompt != null){
                if(chatContext != null){
                    systemPrompt = chatContext.evalSystemPrompt(systemPrompt);
                }
                systemMessage = MessageBuilder.buildSystemMessage(systemPrompt);                 
                messages.add(systemMessage);
            }
            messages.add(userMessage);
        }
        
        

        requestMap.put("messages", messages);
        Map parameters = chatAgentMessage.getParameters();
		Boolean stream = chatContext.getStreamable();
		if(stream == null){
			stream = chatAgentMessage.getStream();
		}
        if(SimpleStringUtil.isNotEmpty( parameters)){

            requestMap.putAll(parameters);
            if(!parameters.containsKey("stream") && stream != null){
                requestMap.put("stream", stream);
            }
            if(!parameters.containsKey("temperature") && chatAgentMessage.getTemperature() != null){
                requestMap.put("temperature", chatAgentMessage.getTemperature());
            }

            if(!parameters.containsKey("max_tokens") && chatAgentMessage.getMaxTokens() != null){
                requestMap.put("max_tokens", chatAgentMessage.getMaxTokens());
            }
        }
        else {
            //设置默认参数
            if( stream != null){
                requestMap.put("stream", stream);
            }
            
            if( chatAgentMessage.getTemperature() != null){
                requestMap.put("temperature", chatAgentMessage.getTemperature());
            }
            if( chatAgentMessage.getMaxTokens() != null){
                requestMap.put("max_tokens", chatAgentMessage.getMaxTokens());
            }
        }
		//"stream_options": {"include_usage": true}
		Boolean includeUsage = chatAgentMessage.getIncludeUsage();
		if(includeUsage != null){
			Map streamOptions = new HashMap();
			streamOptions.put("include_usage", includeUsage);
			requestMap.put("stream_options", streamOptions);
			
		}
        buildThinking(  chatAgentMessage,chatObject, requestMap);
        buildTools(chatContext,chatAgentMessage,aiAgent, requestMap);
        return requestMap;
    }
    public abstract ImageEvent buildGenImageResponse(ClientConfiguration config, ImageAgentMessage imageAgentMessage,StoreChatObject storeChatObject,Map imageData);
   
  
    public StoreChatObject buildGenImageRequestParameter(ClientConfiguration clientConfiguration, Object imageAgentMessage,AIAgent aiAgent){
        StoreChatObject storeChatObject = new StoreChatObject();
        if(imageAgentMessage instanceof ImageAgentMessage){
            ImageAgentMessage temp = (ImageAgentMessage)imageAgentMessage;
            imageAgentMessage = buildGenImageRequestMap(temp,aiAgent);
//            temp.setGenImageCompletionsUrl(this.getGenImageCompletionsUrl(temp));
            storeChatObject.setGenFileStoreDir(clientConfiguration.getExtendConfig("genFileStoreDir"));
            storeChatObject.setEndpoint(clientConfiguration.getExtendConfig("endpoint"));
            storeChatObject.setStoreImageType(clientConfiguration.getExtendConfig("storeImageType"));

            if(storeChatObject.getGenFileStoreDir() != null)
                storeChatObject.setGenFileStoreDir(storeChatObject.getGenFileStoreDir().trim());
            if(storeChatObject.getEndpoint() != null)
                storeChatObject.setEndpoint(storeChatObject.getEndpoint().trim());
            if(storeChatObject.getStoreImageType() != null)
                storeChatObject.setStoreImageType(storeChatObject.getStoreImageType().trim());
        }
        storeChatObject.setMessage(imageAgentMessage);
        return storeChatObject;
        
    }
    
 
    public SSEHeaderSetFunction getAudioGenSSEHeaderSetFunction(){
        return SSEHeaderSetFunction.DEFAULT_SSEHEADERSETFUNCTION;
    }
    public Map<String,Object> buildEmbeddingMessage(ClientConfiguration config,EmbeddingMessage embeddingMessage,AIAgent aiAgent){
        Map params = new HashMap();
        params.put("input", embeddingMessage.getInput());//设置将要向量化的数据
        params.put("model", embeddingMessage.getModel());
        if(embeddingMessage.getParameters() != null && embeddingMessage.getParameters().size() > 0){
            params.putAll(embeddingMessage.getParameters());
        }
        return params;
    }
    public ChatObject buildOpenAIRequestParameter(ClientConfiguration clientConfiguration,Object agentMessage, AIAgent aiAgent,ChatContext chatCallback){
        AgentMessage _agentMessage = null;
        if(agentMessage instanceof AgentMessage){
            _agentMessage =  ((AgentMessage)agentMessage);
        }          
        else if (agentMessage instanceof Map){
            _agentMessage = new MapAgentMessage((Map)agentMessage);
        }
        else{
            _agentMessage = new ObjectAgentMessage(agentMessage);
        }
        ChatObject chatObject = _agentMessage.buildChatObject(clientConfiguration,this,   aiAgent,   chatCallback);
        return chatObject;
         
 
    }
    protected abstract Map<String, Object> buildGenAudioRequestMap(AudioAgentMessage audioAgentMessage,AIAgent aiAgent, ChatContext chatContext);
  
    public Map<String, Object> _buildGenAudioRequestMap(AudioAgentMessage audioAgentMessage,StoreChatObject storeChatObject,
                                                        ClientConfiguration clientConfiguration,AIAgent aiAgent, ChatContext chatContext){

        if(storeChatObject.getGenFileStoreDir() == null)
            storeChatObject.setGenFileStoreDir(clientConfiguration.getExtendConfig("genFileStoreDir"));
        if(storeChatObject.getEndpoint() == null)
            storeChatObject.setEndpoint(clientConfiguration.getExtendConfig("endpoint"));
        if(storeChatObject.getStoreAudioType() == null){
            storeChatObject.setStoreAudioType(clientConfiguration.getExtendConfig("storeAudioType"));
        }
		Boolean stream = chatContext.getStreamable();
		if(stream == null){
			stream = audioAgentMessage.getStream();
		}
		
        Map params = buildGenAudioRequestMap(audioAgentMessage,aiAgent,   chatContext);
//        audioAgentMessage.setGenAudioCompletionsUrl(getGenAudioCompletionsUrl(audioAgentMessage));
        if(stream != null){
            params.put("stream", stream);
			
        }
		//"stream_options": {"include_usage": true}
		Boolean includeUsage = audioAgentMessage.getIncludeUsage();
		if(includeUsage != null){
			Map streamOptions = new HashMap();
			streamOptions.put("include_usage", includeUsage);
			params.put("stream_options", streamOptions);
			
		}
        return params;
    }

    public Map<String, Object> _buildGetVideoResultRquestMap(VideoStoreAgentMessage videoStoreAgentMessage,StoreChatObject storeChatObject,ClientConfiguration clientConfiguration){

        if(storeChatObject.getGenFileStoreDir() == null)
            storeChatObject.setGenFileStoreDir(clientConfiguration.getExtendConfig("genFileStoreDir"));
        if(storeChatObject.getEndpoint() == null)
            storeChatObject.setEndpoint(clientConfiguration.getExtendConfig("endpoint"));
        if(storeChatObject.getStoreVideoType() == null){
            storeChatObject.setStoreVideoType(clientConfiguration.getExtendConfig("storeVideoType"));
        }
       
        return buildGetVideoResultRquestMap(  videoStoreAgentMessage);
    }

    protected abstract Map<String, Object> buildGetVideoResultRquestMap(VideoStoreAgentMessage videoStoreAgentMessage);

    /**
     * 构建音频生成请求参数
     * @param clientConfiguration
     * @param audioAgentMessage
     * @return
     */
    public StoreChatObject buildGenAudioRequestParameter(ClientConfiguration clientConfiguration, Object audioAgentMessage,AIAgent aiAgent, ChatContext chatCallback) {
        StoreChatObject storeChatObject = new StoreChatObject();
        if(audioAgentMessage instanceof AudioAgentMessage){
            AudioAgentMessage temp = (AudioAgentMessage)audioAgentMessage;
            audioAgentMessage = this._buildGenAudioRequestMap(temp,storeChatObject,clientConfiguration,aiAgent,   chatCallback);
             
           
        }
        storeChatObject.setMessage(audioAgentMessage);
        return storeChatObject;
    }

    public abstract AudioEvent buildGenAudioResponse(ClientConfiguration config, AudioAgentMessage message,StoreChatObject storeChatObject, Map data);

    public Map buildAudioSTTRequestMap(AudioSTTAgentMessage audioSTTAgentMessage, AIAgent aiAgent,ChatContext chatContext) {

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("model", audioSTTAgentMessage.getModel());
		List<Map<String, Object>> sessionMemory = aiAgent.getSessionMemory(true);
        // 构建消息历史列表，包含之前的会话记忆
        List<Map<String, Object>> messages = sessionMemory !=  null?
                new ArrayList<>(sessionMemory):new ArrayList<>();
        Object audio = audioSTTAgentMessage.getAudio();
        // 添加当前用户消息
        Map<String, Object> userMessage = null;
        String prompt = getPrompt(  audioSTTAgentMessage,   aiAgent);
        if(chatContext != null){
            prompt = chatContext.evalPrompt(prompt);
        }
        if(audio != null) {
            userMessage = MessageBuilder.buildAudioSystemMessage(prompt);
        }
        else{
            userMessage = MessageBuilder.buildAudioUserMessage(prompt);
        }
        messages.add(userMessage);
        audioSTTAgentMessage.addSessionMessage(userMessage,aiAgent);
       
        if(audio != null) {
            AudioDataBuilder audioDataBuilder = audioSTTAgentMessage.getAudioDataBuilder();
            if (audioDataBuilder == null) {
                audioDataBuilder = () -> {
                    String base64Audio = null;


                    if (audio instanceof File) {

                        try {
                            byte[] audioBytes = FileUtil.getBytes((File) audio);
                            String contentType = audioSTTAgentMessage.getContentType();
                            if (contentType == null) {
                                contentType = "audio/wav";
                            }
                            base64Audio = "data:" + contentType + ";base64," +
                                    Base64.getEncoder().encodeToString(audioBytes);
                        } catch (IOException e) {
                            throw new AIRuntimeException(e);
                        }

                    } else if (audio instanceof byte[]) {
                        base64Audio = "data:" + audioSTTAgentMessage.getContentType() + ";base64," +
                                Base64.getEncoder().encodeToString((byte[]) audio);
                    } else if (audio instanceof String) {
                        base64Audio = (String) audio;
                    }
                    return base64Audio;

                };
            }

            //直接设置音频url地址
//        MessageBuilder.buildAudioMessage("https://dashscope.oss-cn-beijing.aliyuncs.com/audios/welcome.mp3");
            //将音频文件转换为base64编码
            userMessage = MessageBuilder.buildAudioMessage(audioDataBuilder);

            messages.add(userMessage);
        }
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("messages", messages);
        requestMap.put("input", input);
        Map parameters = audioSTTAgentMessage.getParameters();
        if(parameters != null) {
            requestMap.put("parameters", parameters);
        }
		Boolean stream = chatContext.getStreamable();
		if(stream == null){
			stream = audioSTTAgentMessage.getStream();
		}
        if(stream!= null){
            requestMap.put("stream", stream);
        }
		//"stream_options": {"include_usage": true}
		Boolean includeUsage = audioSTTAgentMessage.getIncludeUsage();
		if(includeUsage != null){
			Map streamOptions = new HashMap();
			streamOptions.put("include_usage", includeUsage);
			requestMap.put("stream_options", streamOptions);
			
		}
        if(audioSTTAgentMessage.getResultFormat() != null)
            requestMap.put("result_format", audioSTTAgentMessage.getResultFormat());
        return requestMap;
    }
    protected abstract Object buildGenVideoRequestMap(VideoAgentMessage videoAgentMessage,ClientConfiguration clientConfiguration,AIAgent aiAgent);
  
    public StoreChatObject buildVideoRequestParameter(ClientConfiguration clientConfiguration, VideoAgentMessage videoAgentMessage,AIAgent aiAgent) {
        StoreChatObject storeChatObject = new StoreChatObject();
        storeChatObject.setSubmitVideoTaskUrl(getSubmitVideoTaskUrl(  clientConfiguration,  videoAgentMessage));
        storeChatObject.setMessage(this.buildGenVideoRequestMap(videoAgentMessage,clientConfiguration,aiAgent));
        return storeChatObject;
    }

    public abstract VideoTask buildVideoResponseTask(ClientConfiguration clientConfiguration, VideoAgentMessage videoAgentMessage,Map taskInfo);

    public VideoGenResult buildVideoGenResult(ClientConfiguration clientConfiguration,VideoStoreAgentMessage videoStoreAgentMessage,StoreChatObject storeChatObject,Map taskInfo) {
        return null;
    }


 
    public Boolean getCustomThinking(Map parameters) {
        Map thinking = (Map)parameters.get("thinking");
        if(thinking != null){
            String type = (String)thinking.get("type");
            if(type != null ){
                if(type.equals("enabled")) {
                    return true;
                }
                else{
                    return false;
                }
            }
        }
        return null;
    }


    public Map<String, Object> buildRerankMessage(ClientConfiguration config, RerankMessage rerankMessage, AIAgent agent) {
        Map rerankParams = new LinkedHashMap();
        rerankParams.put("model", rerankMessage.getModel());  // 使用项目规范的 rerank 模型
        rerankParams.put("documents", rerankMessage.convertDocuments());
        rerankParams.put("query", rerankMessage.getQuery());
        rerankParams.put("return_documents", rerankMessage.isReturnDocuments());  // 如需返回原始文本可开启
        if(rerankMessage.getParameters() != null && rerankMessage.getParameters().size() > 0)
            rerankParams.put("parameters", rerankMessage.getParameters());
        return rerankParams;
    }

    public List<RerankedDocument> rerank(ClientConfiguration clientConfiguration,RerankMessage rerankMessage, AIAgent agent, Map<String, Object> params) {
        Map response = HttpRequestProxy.sendJsonBody(rerankMessage.getMaas(), params, this.getRerankUrl( clientConfiguration, rerankMessage), Map.class);
        if(logger.isDebugEnabled()) {
            logger.debug("Rerank 响应: {}", JsonUtil.object2json(response));
        }
        List<RerankedDocument> rerankedDocuments = null;
       
        // 解析 Rerank 结果
        if (response != null && response.containsKey("results")) {
            List<Map<String, Object>> rerankResults = (List<Map<String, Object>>) response.get("results");
            if(logger.isDebugEnabled()) {
                logger.debug("========== Rerank 排序结果 ==========");
            }
            List<RerankDocument> rerankDatas = rerankMessage.getRerankDocuments();
            RerankedDocument rerankedDocument = null;
            RerankDocument rerankDocument = null;
            rerankedDocuments = new ArrayList<>();
            for (int i = 0; i < rerankResults.size(); i++) {
                rerankedDocument = new RerankedDocument();
                Map<String, Object> result = rerankResults.get(i);
                int index = (Integer) result.get("index");
                rerankedDocument.setIndex(index);
                double relevanceScore = (Double) result.get("relevance_score");
                rerankedDocument.setRelevanceScore(relevanceScore);
                rerankDocument = rerankDatas.get(index);
                rerankedDocument.setDocument(rerankDocument.getDocument());
                rerankedDocument.setMetadata(rerankDocument.getMetadata());
                rerankedDocument.setVectorScore(rerankDocument.getVectorScore());
                rerankedDocument.setBm25Score(rerankDocument.getBm25Score());
                if(logger.isDebugEnabled()) {
                    logger.debug("[{}] RrfScore: {}, relevance_score: {}, content: {}", i, rerankedDocument.getVectorScore(), relevanceScore,
                            rerankedDocument.getDocument());
                }
                rerankedDocuments.add(rerankedDocument);
                
            }
        }
        return rerankedDocuments;
    }

    public StreamData buildErrorStreamData(Map map, TokenMetrics tokenMetrics) {
        String code =  (String)map.get("code");
        String message = (String) map.get("message");

        if(code != null) {
            return new StreamData(ServerEvent.CONTENT, message, code).setStreamTokenMetrics(tokenMetrics);
        }
        return null;
    }
}
