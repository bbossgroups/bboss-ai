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
import org.frameworkset.spi.ai.material.GenMaterialFileDownload;
import org.frameworkset.spi.ai.model.*;
import org.frameworkset.spi.ai.util.*;
import org.frameworkset.spi.ai.material.GenFileDownload;
import org.frameworkset.spi.reactor.SSEHeaderSetFunction;
import org.frameworkset.spi.remote.http.ClientConfiguration;

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

     
    protected AgentAdapter initAgentAdapter(){
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
    protected abstract Map buildGenImageRequestMap(ImageAgentMessage imageAgentMessage);

    protected void buildTools(AgentMessage agentMessage,Map<String, Object> requestMap){
        agentMessage.init();
        if(agentMessage.getTools() != null){
            Object tools = agentMessage.getTools();
            if(tools instanceof List){
                requestMap.put("tools", tools);
            }
            else if(tools instanceof String){
                requestMap.put("tools", SimpleStringUtil.json2ListObject((String)tools,Map.class));
            }
        }
    }
    protected void filterParameters(AgentMessage agentMessage,Map<String, Object> requestMap, Map<String, Object> parameters) {
        if(SimpleStringUtil.isEmpty( parameters)){
            if( agentMessage.getStream() != null){
                requestMap.put("stream", agentMessage.getStream());
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
            if(!parameters.containsKey("stream") && agentMessage.getStream() != null){
                requestMap.put("stream", agentMessage.getStream());
            }

            if(!parameters.containsKey("temperature") && agentMessage.getTemperature() != null){
                requestMap.put("temperature", agentMessage.getTemperature());
            }
            if(!parameters.containsKey("max_tokens") && agentMessage.getMaxTokens() != null){
                requestMap.put("max_tokens", agentMessage.getMaxTokens());
            }
            
        }
        buildTools(  agentMessage, requestMap);
    }
    protected Object handleImageParserMessages(List<Map<String, Object>> messages){
        return messages;
    }
    

    public Map buildVideoVLRequestMap(VideoVLAgentMessage videoVLAgentMessage) {


        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("model",videoVLAgentMessage.getModel());
        List<String > videoUrls = videoVLAgentMessage.getVideoUrls();

        Map<String, Object> userMessage = null;
        Map<String, Object> systemMessage = null;
        if(videoUrls != null && videoUrls.size() > 0) {
            userMessage = buildInputVideosMessage(videoVLAgentMessage.getPrompt(), videoUrls.toArray(new String[]{}));
        }
        else{
            userMessage = buildInputVideosMessage(videoVLAgentMessage.getPrompt(), (String[])null);
        }
        // 构建消息历史列表，包含之前的会话记忆

        List<Map<String, Object>> sessionMemory = videoVLAgentMessage.getSessionMemory();
        List<Map<String, Object>> messages = null;
        if(sessionMemory != null){
            if(sessionMemory.size() == 0){
                if(videoVLAgentMessage.getSystemPrompt() != null){
                    systemMessage = MessageBuilder.buildSystemMessage(videoVLAgentMessage.getSystemPrompt());
                    videoVLAgentMessage.addSessionMessage(systemMessage);
                }
            }
            videoVLAgentMessage.addSessionMessage(userMessage);
            messages = new ArrayList<>(sessionMemory);
        }
        else{
            messages = new ArrayList<>();
            if(videoVLAgentMessage.getSystemPrompt() != null) {
                systemMessage = MessageBuilder.buildSystemMessage(videoVLAgentMessage.getSystemPrompt());
                messages.add(systemMessage);
            }
            messages.add(userMessage);
        }


        requestMap.put("messages", handleImageParserMessages(messages));
        Map parameters = videoVLAgentMessage.getParameters();

        filterParameters(videoVLAgentMessage,requestMap,parameters);

        return requestMap;
    }
    
    public Map buildImageVLRequestMap(ImageVLAgentMessage imageAgentMessage) {

        
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("model",imageAgentMessage.getModel());
        List<String > imageUrls = imageAgentMessage.getImageUrls();

        Map<String, Object> userMessage = null;
        Map<String, Object> systemMessage = null;
        if(imageUrls != null && imageUrls.size() > 0) {
            userMessage = buildInputImagesMessage(imageAgentMessage.getPrompt(), imageUrls.toArray(new String[]{}));
        }
        else{
            userMessage = buildInputImagesMessage(imageAgentMessage.getPrompt(), (String[])null);
        }
        // 构建消息历史列表，包含之前的会话记忆

        List<Map<String, Object>> sessionMemory = imageAgentMessage.getSessionMemory();
        List<Map<String, Object>> messages = null;
        if(sessionMemory != null){
            if(sessionMemory.size() == 0){
                if(imageAgentMessage.getSystemPrompt() != null){
                    systemMessage = MessageBuilder.buildSystemMessage(imageAgentMessage.getSystemPrompt());
                    imageAgentMessage.addSessionMessage(systemMessage);
                }
            }
            imageAgentMessage.addSessionMessage(userMessage);
            messages = new ArrayList<>(sessionMemory);
        }
        else{
            messages = new ArrayList<>();
            if(imageAgentMessage.getSystemPrompt() != null) {
                systemMessage = MessageBuilder.buildSystemMessage(imageAgentMessage.getSystemPrompt());
                messages.add(systemMessage);
            }
            messages.add(userMessage);
        }
         

        requestMap.put("messages", handleImageParserMessages(messages));
        Map parameters = imageAgentMessage.getParameters();

        filterParameters(imageAgentMessage,requestMap,parameters);

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
        return AIResponseUtil.parseQianwenAudioGenStreamContentFromData(data);
    }

    /**
     * 语音识别：data:{"output":{"choices":[{"message":{"annotations":[{"type":"audio_info","language":"zh","emotion":"neutral"}],"content":[{"text":"欢迎与"}],"role":"assistant"},"finish_reason":"null"}]},"usage":{"output_tokens_details":{"text_tokens":6},"input_tokens_details":{"text_tokens":16},"seconds":1},"request_id":"e84128d5-4bae-4e7e-91ab-6fb33504d2e3"}
     * LLM和图像识别：data: {"id":"ccf32be6-ad2f-4658-963a-fc3c22346e6b","object":"chat.completion.chunk","created":1761725211,"model":"deepseek-reasoner","system_fingerprint":"fp_ffc7281d48_prod0820_fp8_kvcache","choices":[{"index":0,"delta":{"content":null,"reasoning_content":"在"},"logprobs":null,"finish_reason":null}]}
     * @param data
     * @return
     */
    public StreamData parseStreamContentFromData(BaseStreamDataBuilder streamDataBuilder, String data){
        return AIResponseUtil.parseStreamContentFromData(streamDataBuilder,data);
    }

    /**
     * 语音识别：data:{"output":{"choices":[{"message":{"annotations":[{"type":"audio_info","language":"zh","emotion":"neutral"}],"content":[{"text":"欢迎与"}],"role":"assistant"},"finish_reason":"null"}]},"usage":{"output_tokens_details":{"text_tokens":6},"input_tokens_details":{"text_tokens":16},"seconds":1},"request_id":"e84128d5-4bae-4e7e-91ab-6fb33504d2e3"}
     * LLM和图像识别：data: {"id":"ccf32be6-ad2f-4658-963a-fc3c22346e6b","object":"chat.completion.chunk","created":1761725211,"model":"deepseek-reasoner","system_fingerprint":"fp_ffc7281d48_prod0820_fp8_kvcache","choices":[{"index":0,"delta":{"content":null,"reasoning_content":"在"},"logprobs":null,"finish_reason":null}]}
     * @param data
     * @return
     */
    public StreamData parseImageParserStreamContentFromData(BaseStreamDataBuilder streamDataBuilder,String data){
        return AIResponseUtil.parseStreamContentFromData(streamDataBuilder,data);
    }

    public StreamData parseVideoParserStreamContentFromData(BaseStreamDataBuilder streamDataBuilder,String data){
        return AIResponseUtil.parseStreamContentFromData(streamDataBuilder,data);
    }

    /**
     * 语音识别数据解析
     * @param data
     * @return
     */
    public StreamData parseAudioStreamContentFromData(StreamDataBuilder streamDataBuilder,String data){
        return AIResponseUtil.parseAudioStreamContentFromData(  streamDataBuilder,data);
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

    protected Map<String, Object> buildInputToolMessage(ToolAgentMessage toolAgentMessage) {
        FunctionTool tool = toolAgentMessage.getFunctionTool();
        String toolId = tool.getId();
        String functionName = tool.getFunctionName();
        FunctionCall functionCall = toolAgentMessage.getFunctionCall(functionName);
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
            else{
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
    public Map buildOpenAIRequestMapWithTool(ToolAgentMessage toolAgentMessage){
        Map<String, Object> userMessage = buildInputToolMessage(  toolAgentMessage);
       
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("model", toolAgentMessage.getModel());

        List<Map<String, Object>> messages = null;
        List<Map<String, Object>> sessionMemory = toolAgentMessage.getSessionMemory();
        if(sessionMemory != null){
            // 构建消息历史列表，包含之前的会话记忆           

            
            // 添加当前用户消息
            toolAgentMessage.addSessionMessage(userMessage);
            messages = new ArrayList<>(sessionMemory);


        }
        else{
            messages = new ArrayList<>();
            
            messages.add(userMessage);
        }



        requestMap.put("messages", messages);
        Map parameters = toolAgentMessage.getParameters();
        if(SimpleStringUtil.isNotEmpty( parameters)){

            requestMap.putAll(parameters);
            if(!parameters.containsKey("stream") && toolAgentMessage.getStream() != null){
                requestMap.put("stream", toolAgentMessage.getStream());
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
            if( toolAgentMessage.getStream() != null){
                requestMap.put("stream", toolAgentMessage.getStream());
            }

            if( toolAgentMessage.getTemperature() != null){
                requestMap.put("temperature", toolAgentMessage  .getTemperature());
            }
            if( toolAgentMessage.getMaxTokens() != null){
                requestMap.put("max_tokens", toolAgentMessage.getMaxTokens());
            }
        }
//        buildTools(toolAgentMessage, requestMap);
        return requestMap;
    }
    /**
     * 构建智能问答请求参数
     * @param chatAgentMessage
     * @return
     */
    public Map buildOpenAIRequestMap(ChatAgentMessage chatAgentMessage){
        String message = chatAgentMessage.getPrompt();
        Map<String, Object> userMessage = MessageBuilder.buildUserMessage( message);
        Map<String,Object> systemMessage = null;
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("model", chatAgentMessage.getModel());

        List<Map<String, Object>> messages = null;
        List<Map<String, Object>> sessionMemory = chatAgentMessage.getSessionMemory();
        if(sessionMemory != null){
            // 构建消息历史列表，包含之前的会话记忆           

            if(sessionMemory.size() == 0){
                if(chatAgentMessage.getSystemPrompt() != null){
                    systemMessage = MessageBuilder.buildSystemMessage(chatAgentMessage.getSystemPrompt());
                    chatAgentMessage.addSessionMessage(systemMessage);
                }
            }
            // 添加当前用户消息
            chatAgentMessage.addSessionMessage(userMessage);
            messages = new ArrayList<>(sessionMemory);
            
            
        }
        else{
            messages = new ArrayList<>();
            if(chatAgentMessage.getSystemPrompt() != null){
                if(systemMessage == null){
                    systemMessage = MessageBuilder.buildSystemMessage(chatAgentMessage.getSystemPrompt());
                }
                messages.add(systemMessage);
            }
            messages.add(userMessage);
        }
        
        

        requestMap.put("messages", messages);
        Map parameters = chatAgentMessage.getParameters();
        if(SimpleStringUtil.isNotEmpty( parameters)){

            requestMap.putAll(parameters);
            if(!parameters.containsKey("stream") && chatAgentMessage.getStream() != null){
                requestMap.put("stream", chatAgentMessage.getStream());
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
            if( chatAgentMessage.getStream() != null){
                requestMap.put("stream", chatAgentMessage.getStream());
            }
            
            if( chatAgentMessage.getTemperature() != null){
                requestMap.put("temperature", chatAgentMessage.getTemperature());
            }
            if( chatAgentMessage.getMaxTokens() != null){
                requestMap.put("max_tokens", chatAgentMessage.getMaxTokens());
            }
        }

        buildTools(chatAgentMessage, requestMap);
        return requestMap;
    }
    public abstract ImageEvent buildGenImageResponse(ClientConfiguration config, ImageAgentMessage imageAgentMessage,Map imageData);
   
  
    public Object buildGenImageRequestParameter(ClientConfiguration clientConfiguration, Object imageAgentMessage){
        if(imageAgentMessage instanceof ImageAgentMessage){
            ImageAgentMessage temp = (ImageAgentMessage)imageAgentMessage;
            imageAgentMessage = buildGenImageRequestMap(temp);
            temp.setGenImageCompletionsUrl(this.getGenImageCompletionsUrl(temp));
            if(temp.getGenFileStoreDir() == null)
                temp.setGenFileStoreDir(clientConfiguration.getExtendConfig("genFileStoreDir"));
            if(temp.getEndpoint() == null)
                temp.setEndpoint(clientConfiguration.getExtendConfig("endpoint"));
            if(temp.getStoreImageType() == null)
                temp.setStoreImageType(clientConfiguration.getExtendConfig("storeImageType"));

            if(temp.getGenFileStoreDir() != null)
                temp.setGenFileStoreDir(temp.getGenFileStoreDir().trim());
            if(temp.getEndpoint() != null)
                temp.setEndpoint(temp.getEndpoint().trim());
            if(temp.getStoreImageType() != null)
                temp.setStoreImageType(temp.getStoreImageType().trim());
        }
        return imageAgentMessage;
        
    }
    
 
    public SSEHeaderSetFunction getAudioGenSSEHeaderSetFunction(){
        return SSEHeaderSetFunction.DEFAULT_SSEHEADERSETFUNCTION;
    }

    public ChatObject buildOpenAIRequestParameter(ClientConfiguration clientConfiguration,Object agentMessage){
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
        return _agentMessage.buildChatObject(clientConfiguration,this);
         
 
    }
    protected abstract Map<String, Object> buildGenAudioRequestMap(AudioAgentMessage audioAgentMessage);
  
    public Map<String, Object> _buildGenAudioRequestMap(AudioAgentMessage audioAgentMessage,ClientConfiguration clientConfiguration){

        if(audioAgentMessage.getGenFileStoreDir() == null)
            audioAgentMessage.setGenFileStoreDir(clientConfiguration.getExtendConfig("genFileStoreDir"));
        if(audioAgentMessage.getEndpoint() == null)
            audioAgentMessage.setEndpoint(clientConfiguration.getExtendConfig("endpoint"));
        if(audioAgentMessage.getStoreAudioType() == null){
            audioAgentMessage.setStoreAudioType(clientConfiguration.getExtendConfig("storeAudioType"));
        }
        Map params = buildGenAudioRequestMap(audioAgentMessage);
        audioAgentMessage.setGenAudioCompletionsUrl(getGenAudioCompletionsUrl(audioAgentMessage));
        if(audioAgentMessage.getStream() != null){
            params.put("stream", audioAgentMessage.getStream());
        }
        return params;
    }

    public Map<String, Object> _buildGetVideoResultRquestMap(VideoStoreAgentMessage videoStoreAgentMessage,ClientConfiguration clientConfiguration){

        if(videoStoreAgentMessage.getGenFileStoreDir() == null)
            videoStoreAgentMessage.setGenFileStoreDir(clientConfiguration.getExtendConfig("genFileStoreDir"));
        if(videoStoreAgentMessage.getEndpoint() == null)
            videoStoreAgentMessage.setEndpoint(clientConfiguration.getExtendConfig("endpoint"));
        if(videoStoreAgentMessage.getStoreVideoType() == null){
            videoStoreAgentMessage.setStoreVideoType(clientConfiguration.getExtendConfig("storeVideoType"));
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
    public Object buildGenAudioRequestParameter(ClientConfiguration clientConfiguration, Object audioAgentMessage) {
        if(audioAgentMessage instanceof AudioAgentMessage){
            AudioAgentMessage temp = (AudioAgentMessage)audioAgentMessage;
            audioAgentMessage = this._buildGenAudioRequestMap(temp,clientConfiguration);
             
           
        }
        
        return audioAgentMessage;
    }

    public abstract AudioEvent buildGenAudioResponse(ClientConfiguration config, AudioAgentMessage message, Map data);

    public Map buildAudioSTTRequestMap(AudioSTTAgentMessage audioSTTAgentMessage) {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("model", audioSTTAgentMessage.getModel());

        // 构建消息历史列表，包含之前的会话记忆
        List<Map<String, Object>> messages = audioSTTAgentMessage.getSessionMemory() !=  null?
                new ArrayList<>(audioSTTAgentMessage.getSessionMemory()):new ArrayList<>();
        Object audio = audioSTTAgentMessage.getAudio();
        // 添加当前用户消息
        Map<String, Object> userMessage = null;
        if(audio != null) {
            userMessage = MessageBuilder.buildAudioSystemMessage(audioSTTAgentMessage.getPrompt());
        }
        else{
            userMessage = MessageBuilder.buildAudioUserMessage(audioSTTAgentMessage.getPrompt());
        }
        messages.add(userMessage);
        audioSTTAgentMessage.addSessionMessage(userMessage);
       
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
        if(audioSTTAgentMessage.getStream() != null){
            requestMap.put("stream", audioSTTAgentMessage.getStream());
        }
        if(audioSTTAgentMessage.getResultFormat() != null)
            requestMap.put("result_format", audioSTTAgentMessage.getResultFormat());
        return requestMap;
    }
    protected abstract Object buildGenVideoRequestMap(VideoAgentMessage videoAgentMessage,ClientConfiguration clientConfiguration);
  
    public Object buildVideoRequestParameter(ClientConfiguration clientConfiguration, VideoAgentMessage videoAgentMessage) {
        videoAgentMessage.setSubmitVideoTaskUrl(getSubmitVideoTaskUrl(  videoAgentMessage));
        return this.buildGenVideoRequestMap(videoAgentMessage,clientConfiguration);
    }

    public abstract VideoTask buildVideoResponseTask(ClientConfiguration clientConfiguration, VideoAgentMessage videoAgentMessage,Map taskInfo);

    public VideoGenResult buildVideoGenResult(ClientConfiguration clientConfiguration,VideoStoreAgentMessage videoStoreAgentMessage,Map taskInfo) {
        return null;
    }

  
}
