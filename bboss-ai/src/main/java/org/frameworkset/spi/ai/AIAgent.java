package org.frameworkset.spi.ai;
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
import com.frameworkset.util.SimpleStringUtil;
import org.frameworkset.spi.ai.model.*;
import org.frameworkset.spi.ai.store.AgentSessionStore;
import org.frameworkset.spi.ai.store.AgentSessionStoreMemory;
import org.frameworkset.spi.ai.tools.ToolsRegist;
import org.frameworkset.spi.ai.util.AIAgentUtil;
import org.slf4j.Logger;
import reactor.core.publisher.Flux;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能体工具包
 * @author biaoping.yin
 * @Date 2026/1/4
 */
public class AIAgent {
    private static Logger logger = org.slf4j.LoggerFactory.getLogger(AIAgent.class);
    private String prompt;
    private String systemPrompt;
    private String type;
    private int sessionSize;

    /**
     * 工具清单，标准工具规范格式
     */
    private List<FunctionToolDefine> tools;

    @JsonIgnore
    private Map<String,FunctionCall> toolCalls;

    @JsonIgnore
    private ToolsRegist toolsRegist;
    
    private AgentSessionStore agentSessionStore;

    private AgentSessionStore parentSessionStore;
    private String agentId;
    public AIAgent(){
        this(null);
    }

    public String getPrompt() {
        return prompt;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public AIAgent setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
        return this;
    }

    public AIAgent setParentSessionStore(AgentSessionStore parentSessionStore) {
        this.parentSessionStore = parentSessionStore;
        return this;
    }

    public AgentSessionStore getParentSessionStore() {
        return parentSessionStore;
    }

    public AgentSessionStore getAgentSessionStore() {
        return agentSessionStore;
    }

    public AIAgent(String prompt, String type, ToolsRegist toolsRegist, Integer sessionSize){
        this.prompt = prompt;
        this.type = type;
        this.toolsRegist = toolsRegist;
//        this.agentId = SimpleStringUtil.getUUID32();
        if(sessionSize != null ){
            this.sessionSize = sessionSize;
        }
//            agentSessionStore = new AgentSessionStoreMemory(sessionSize);
    }

    public AIAgent setAgentId(String agentId) {
        this.agentId = agentId;
        return this;
    }

    public AIAgent(String prompt, String type, ToolsRegist toolsRegist){
        this(  prompt, type, toolsRegist,null);
    }

    public AIAgent(String prompt,ToolsRegist toolsRegist){
        this(  prompt, null, toolsRegist,null);
    }

    public AIAgent(String prompt,ToolsRegist toolsRegist,int sessionSize){
        this(  prompt, null, toolsRegist,sessionSize);
    }

    public AIAgent(String prompt,String type){
        this(  prompt, type, null,null);
    }

    public AIAgent(String prompt){
        this(  prompt, null, null,null);
    }

    public AIAgent(String prompt,int sessionSize){
        this(  prompt, null, null,sessionSize);
    }
    
    private void reactMessage(AgentMessage agentMessage){
//        if(prompt != null)
//            agentMessage.setPrompt(prompt);
//        if(toolsRegist != null)
//            agentMessage.setToolsRegist(toolsRegist);
         
    
        if(agentMessage instanceof SessionAgentMessage) {
            SessionAgentMessage sessionAgentMessage = (SessionAgentMessage)agentMessage;
            AgentSessionStore mainSessionStore = sessionAgentMessage.getMainSessionStore();
            if(mainSessionStore != null) {
                if(agentId == null){
                    if(parentSessionStore != null){
                        agentId = parentSessionStore.genSubAgentId();
                    }
                    else{
                        agentId = mainSessionStore.genSubAgentId();
                    }
                }
                mainSessionStore.loadSessionMemory(prompt == null ? agentMessage.getPrompt() : prompt, agentId);
                if(agentSessionStore == null){
                    if(parentSessionStore != null)
                        agentSessionStore = new AgentSessionStoreMemory(parentSessionStore,sessionSize);
                    else
                        agentSessionStore = new AgentSessionStoreMemory(mainSessionStore,sessionSize);
                    agentSessionStore.setAgentId(agentId);
                    agentSessionStore.setMainAgentSessionStore(mainSessionStore);
                }
                List<Map<String,Object>> sessionMemory = agentSessionStore.getSessionMemory();
                boolean empty = sessionMemory.isEmpty();
//                sessionAgentMessage.setSessionStore(agentSessionStore);
                mainSessionStore.addSubTaskSessionMemory(agentId, agentSessionStore);
                //需要将父智能体中产生的最新的消息作为当前智能体的执行上下文
                LastSessionMessage lastSubAgentSessionMessage = null;
                if(parentSessionStore != null) {
                    lastSubAgentSessionMessage = parentSessionStore.getLastSubAgentSessionMessage(prompt == null ? agentMessage.getPrompt() : prompt, agentId);
                }
                else{
                    lastSubAgentSessionMessage = mainSessionStore.getLastSubAgentSessionMessage(prompt == null ? agentMessage.getPrompt() : prompt, agentId);
                }
                
                if(empty) {
                    //加载历史消息
                    List<Map<String, Object>> sessionMessages = mainSessionStore.getAgentSessionMessage(lastSubAgentSessionMessage, agentId, sessionSize);
                    if (sessionMessages != null && sessionMessages.size() > 0) {
                        for (Map<String, Object> sessionMessage : sessionMessages) {
                            agentSessionStore.appendSessionMessageFromParent(sessionMessage);
                        }


                    }
                }
                else if(lastSubAgentSessionMessage != null){//不为空，直接append主智能体中的最后一条消息
                    agentSessionStore.appendSessionMessageFromParent(lastSubAgentSessionMessage.getLastSessionMessage());
                    mainSessionStore.saveLastSessionMessage(lastSubAgentSessionMessage, agentId);
                    //记录消息引用关系
                }
                
                 
            }
        }
        
    }
    /**
     * 实现图片生成功能
     * @param maasName
     * @param imageAgentMessage
     * @return
     */
    public ImageEvent genImage(String maasName, ImageAgentMessage imageAgentMessage){
        reactMessage(  imageAgentMessage);
        return AIAgentUtil.multimodalImageGeneration(maasName, imageAgentMessage,this);
    }
    public ImageEvent genImage( ImageAgentMessage imageAgentMessage){
        reactMessage(  imageAgentMessage);
        return AIAgentUtil.multimodalImageGeneration(imageAgentMessage.getMaas(), imageAgentMessage,this);
    }

    /**
     * 提交视频生成任务
     * @param maasName
     * @param videoAgentMessage
     * @return
     */
    public VideoTask submitVideoTask(String maasName,VideoAgentMessage videoAgentMessage){
        reactMessage(  videoAgentMessage);
        return AIAgentUtil.submitVideoTask(maasName,videoAgentMessage,this);
    }
    public VideoTask submitVideoTask(VideoAgentMessage videoAgentMessage){
        reactMessage(  videoAgentMessage);
        return AIAgentUtil.submitVideoTask(videoAgentMessage.getMaas(),videoAgentMessage,this);
    }

    public VideoGenResult getVideoTaskResult(String maasName, VideoStoreAgentMessage videoStoreAgentMessage){
        return AIAgentUtil.getVideoTaskResult(maasName,videoStoreAgentMessage);
    }

    public VideoGenResult getVideoTaskResult( VideoStoreAgentMessage videoStoreAgentMessage){
        return AIAgentUtil.getVideoTaskResult(videoStoreAgentMessage.getMaas(),videoStoreAgentMessage);
    }
    /**
     * 调用音频合成模型，生成音频
     * @param audioAgentMessage
     * @return
     */
    public AudioEvent genAudio(  AudioAgentMessage audioAgentMessage){
        reactMessage(  audioAgentMessage);
        return AIAgentUtil.multimodalAudioGeneration( audioAgentMessage.getMaas(),audioAgentMessage,this);
    }

    /**
     * 调用音频合成模型，生成音频
     * @param audioAgentMessage
     * @return
     */
    public Flux<ServerEvent> streamAudioGen(AudioAgentMessage audioAgentMessage){
        reactMessage(  audioAgentMessage);
        return AIAgentUtil.streamAudioGenerationEvent(audioAgentMessage.getMaas(),audioAgentMessage,this);
    }

    /**
     * 调用音频合成模型，生成音频
     * @param maasName
     * @param audioAgentMessage
     * @return
     */
    public Flux<ServerEvent> streamAudioGen(String maasName,  AudioAgentMessage audioAgentMessage){
        reactMessage(  audioAgentMessage);
        return AIAgentUtil.streamAudioGenerationEvent(maasName,audioAgentMessage,this);
    }
    /**
     * 调用音频合成模型，生成音频
     * @param maasName
     * @param audioAgentMessage
     * @return
     */
    public AudioEvent genAudio(String maasName,   AudioAgentMessage audioAgentMessage){
        reactMessage(  audioAgentMessage);
        return AIAgentUtil.multimodalAudioGeneration(maasName, audioAgentMessage,this);
    }

    /**
     * 实现流式音频识别处理
     * @param maasName
     * @param audioSTTAgentMessage
     * @return
     */
    public Flux<ServerEvent> streamAudioParser(String maasName,  AudioSTTAgentMessage audioSTTAgentMessage){
        reactMessage(  audioSTTAgentMessage);
//        audioSTTAgentMessage.init();

        return AIAgentUtil.streamChatCompletionEvent(maasName, audioSTTAgentMessage,this);
    }
    /**
     * 实现流式图片识别处理
     * @param maasName
     * @param imageVLAgentMessage
     * @return
     */
    public Flux<ServerEvent> streamImageParser(String maasName,   ImageVLAgentMessage imageVLAgentMessage){
        reactMessage(  imageVLAgentMessage);
//        imageVLAgentMessage.init();
   
        return AIAgentUtil.streamChatCompletionEvent(maasName, imageVLAgentMessage,this);
    }
    /**
     * 实现流式图片识别处理
     * @param videoVLAgentMessage
     * @return
     */
    public Flux<ServerEvent> streamVideoParser(   VideoVLAgentMessage videoVLAgentMessage){
        reactMessage(  videoVLAgentMessage);
        return AIAgentUtil.streamChatCompletionEvent(videoVLAgentMessage.getMaas(), videoVLAgentMessage,this);
    }
    
    /**
     * 实现流式视频识别处理
     * @param maasName
     * @param videoVLAgentMessage
     * @return
     */
    public Flux<ServerEvent> streamVideoParser(String maasName,   VideoVLAgentMessage videoVLAgentMessage){
        reactMessage(  videoVLAgentMessage);

//        videoVLAgentMessage.init();

        return AIAgentUtil.streamChatCompletionEvent(maasName, videoVLAgentMessage,this);
    }
    /**
     * 实现流式图片识别处理
     * @param imageVLAgentMessage
     * @return
     */
    public Flux<ServerEvent> streamImageParser(   ImageVLAgentMessage imageVLAgentMessage){
        reactMessage(  imageVLAgentMessage);
        return AIAgentUtil.streamChatCompletionEvent(imageVLAgentMessage.getMaas(), imageVLAgentMessage,this);
    }
    /**
     * 实现流式智能问答功能,在指定的数据源上执行
     */
    public Flux<ServerEvent> streamChat(String maasName,   ChatAgentMessage chatAgentMessage){
    
        return streamChat(  maasName,     chatAgentMessage,true);
    }

    /**
     * 实现流式智能问答功能,在指定的数据源上执行
     */
    public Flux<ServerEvent> streamChat(String maasName,   ChatAgentMessage chatAgentMessage,boolean toolStream){
        reactMessage(  chatAgentMessage);
//        chatAgentMessage.init();

        return AIAgentUtil.streamChatCompletionEvent(maasName, chatAgentMessage,this);
    }

    /**
     * 实现流式智能问答功能,在指定的数据源上执行
     */
    public Flux<ServerEvent> streamChat( ChatAgentMessage chatAgentMessage){
        reactMessage(  chatAgentMessage);
        return AIAgentUtil.streamChatCompletionEvent(chatAgentMessage.getMaas(), chatAgentMessage,this);
    }

    /**
     * 实现同步智能问答,在指定的数据源上执行
     * @deprecated 请使用chat方法
     */
    @Deprecated
    public ServerEvent chatCompletionEvent(String maasName,  ChatAgentMessage chatAgentMessage){
        reactMessage(  chatAgentMessage);
        return AIAgentUtil.chatCompletionEvent(maasName,chatAgentMessage,this);
    }

    /**
     * 实现同步智能问答,在指定的数据源上执行
     */
    public ServerEvent chat(String maasName,  ChatAgentMessage chatAgentMessage){
        reactMessage(  chatAgentMessage);
        ServerEvent serverEvent = AIAgentUtil.chatCompletionEvent(maasName,chatAgentMessage,this);
        if(serverEvent != null && serverEvent.getData() != null){
//            Map<String,Object> message = chatAgentMessage.addAssistantSessionMessage(serverEvent.getData());
            if(this.agentSessionStore != null) {
                LastSessionMessage lastSubAgentSessionMessage = this.agentSessionStore.addAgentResultSessionMessage(serverEvent.getData());
                this.agentSessionStore.setParantAgentLastSessionMessage(lastSubAgentSessionMessage);
            }
            
            
        }
        return serverEvent;
//        return AIAgentUtil.chatCompletionEvent(maasName,chatAgentMessage);
    }

    /**
     * 实现同步智能问答,在指定的数据源上执行
     * @deprecated 请使用chat方法
     */
    @Deprecated
    public ServerEvent chatCompletionEvent(  ChatAgentMessage chatAgentMessage){
        reactMessage(  chatAgentMessage);
        return chat(chatAgentMessage.getMaas(),chatAgentMessage);
    }

    /**
     * 实现同步智能问答,在指定的数据源上执行
     */
    public ServerEvent chat(  ChatAgentMessage chatAgentMessage){
        return chat(chatAgentMessage.getMaas(),    chatAgentMessage);
       
    }

    /**
     * 实现同步图片识别处理
     * @param imageVLAgentMessage
     * @return
     */
    public ServerEvent imageParser(  ImageVLAgentMessage imageVLAgentMessage){
        reactMessage( imageVLAgentMessage);
        return AIAgentUtil.imageParser(imageVLAgentMessage.getMaas(), imageVLAgentMessage,this);
    }

    /**
     * 实现同步图片识别处理
     * @param maasName
     * @param imageVLAgentMessage
     * @return
     */
    public ServerEvent imageParser(String maasName,  ImageVLAgentMessage imageVLAgentMessage){
        reactMessage( imageVLAgentMessage);
        return AIAgentUtil.imageParser(maasName, imageVLAgentMessage,this);
    }
    /**
     * 实现同步音频识别处理
     * @param maasName
     * @param audioSTTAgentMessage
     * @return
     */
    public ServerEvent audioParser(String maasName, AudioSTTAgentMessage audioSTTAgentMessage){
        reactMessage( audioSTTAgentMessage);
        return AIAgentUtil.audioParser(maasName,audioSTTAgentMessage,this);
    }
    /**
     * 实现同步音频识别处理
     * @param videoVLAgentMessage
     * @return
     */
    public ServerEvent videoParser( VideoVLAgentMessage videoVLAgentMessage){
        reactMessage( videoVLAgentMessage);
        return AIAgentUtil.videoParser(videoVLAgentMessage.getMaas(),videoVLAgentMessage,this);
    }
    /**
     * 实现同步音频识别处理
     * @param maasName
     * @param videoVLAgentMessage
     * @return
     */
    public ServerEvent videoParser(String maasName, VideoVLAgentMessage videoVLAgentMessage){
        reactMessage( videoVLAgentMessage);
        return AIAgentUtil.videoParser(maasName,videoVLAgentMessage,this);
    }


    public String getAgentId() {
        return this.agentId != null ? this.agentId : this.agentSessionStore.getAgentId();
    }

    public String getParentAgentId() {
        
        return this.agentSessionStore.getParantAgentId();
    }

    public List<Map<String, Object>> getSessionMemory() {
        
        return this.agentSessionStore.getSessionMemory();
    }


    public List<FunctionToolDefine> getTools() {
        return tools;
    }

    public AIAgent setTools(List<FunctionToolDefine> tools) {
        reset();
        this.tools = tools;
        return this;
    }

    public AIAgent registTools(List<FunctionToolDefine> tools) {
        reset();
        if(this.tools == null){
            this.tools = new ArrayList<>();
        }
        this.tools.addAll( tools);
        return this;
    }
    private boolean toolInited = false;
    private Object initLock = new Object();
    public void init(){
        if(toolInited)
            return;
        synchronized (initLock) {
            if(toolInited ){
                return;
            }
            if (this.toolsRegist != null) {
                toolsRegist.init();
                List<FunctionToolDefine> functionToolDefines = this.toolsRegist.registTools();
                if (functionToolDefines != null && functionToolDefines.size() > 0) {
                    FunctionCall functionCall = null;
                    for (FunctionToolDefine functionToolDefine : functionToolDefines) {
                        functionCall = functionToolDefine.getFunctionCall();
                        if (functionCall == null) {
                            functionCall = toolsRegist.getFunctionCall(functionToolDefine.getFunction().getName());
                            if (functionCall != null) {
                                functionToolDefine.setFunctionCall(functionCall);
                            }
                        }
                    }
                    this.registTools(functionToolDefines);
                }
            }

            if (this.toolCalls != null && toolCalls.size() > 0) {
                for (Map.Entry<String, FunctionCall> entry : toolCalls.entrySet()) {
                    FunctionCall functionCall = entry.getValue();
                    String toolName = entry.getKey();
                    if (tools != null && tools.size() > 0) {
                        for (FunctionToolDefine functionToolDefine : tools) {
                            if (functionToolDefine.getFunction().getName().equals(toolName)) {
                                functionToolDefine.setFunctionCall(functionCall);
                                break;
                            }
                        }
                    }

                }
            }
            if (tools != null && tools.size() > 0) {

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
            toolInited = true;
        }
    }

//    public boolean isToolThinkingMessage(){
//        init();
//        return this.tools != null && tools.size() > 0 ;
//    }

    public FunctionCall getFunctionCall(String toolName){
        init();
        if(toolCalls == null)
            return null;
        return toolCalls.get(toolName);
    }

    public AIAgent setToolsRegist(ToolsRegist toolsRegist) {
        reset();
        this.toolsRegist = toolsRegist;
        return this;
    }
    public void destroy(){
        reset();

    }
    private void reset(){
        if(toolInited) {
            synchronized (initLock) {
                if (!toolInited){
                    return;
                }
                if (this.tools != null) {
                    tools.clear();
                }
                if (toolsRegist != null) {
                    toolsRegist.destroy();
                    toolsRegist = null;
                }
                if (this.toolCalls != null) {
                    toolCalls.clear();
                }
                toolInited = false;
            }
        }
    }
    public AIAgent registTool(FunctionToolDefine functionToolDefine) {
        reset();
        if (this.tools == null) {
            tools = new ArrayList<>();
        }
        tools.add(functionToolDefine);

        return this;
    }

    public AIAgent registToolCalls(Map<String,FunctionCall> toolCalls) {
        reset();
        if(this.toolCalls == null){
            toolCalls = new LinkedHashMap<>();
        }
        this.toolCalls.putAll(toolCalls);
        return this;
    }
    public AIAgent registToolCall(String toolName,FunctionCall functionCall) {
        reset();
        if(this.toolCalls == null){
            toolCalls = new LinkedHashMap<>();
        }
        this.toolCalls.put(toolName, functionCall);
        return this;
    }

}
