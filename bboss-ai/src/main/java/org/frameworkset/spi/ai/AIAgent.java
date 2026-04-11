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

import com.frameworkset.util.SimpleStringUtil;
import org.frameworkset.spi.ai.model.*;
import org.frameworkset.spi.ai.store.AgentSessionStore;
import org.frameworkset.spi.ai.store.AgentSessionStoreMemory;
import org.frameworkset.spi.ai.tools.ToolsRegist;
import org.frameworkset.spi.ai.util.AIAgentUtil;
import org.slf4j.Logger;
import reactor.core.publisher.Flux;


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
    private String type;
    private int sessionSize;
    private ToolsRegist toolsRegist;
    private AgentSessionStore agentSessionStore;

    private AgentSessionStore parentSessionStore;
    private String agentId;
    public AIAgent(){
        this(null);
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
        this.agentId = SimpleStringUtil.getUUID32();
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
        if(prompt != null)
            agentMessage.setPrompt(prompt);
        if(toolsRegist != null)
            agentMessage.setToolsRegist(toolsRegist);
         
    
        if(agentMessage instanceof SessionAgentMessage) {
            SessionAgentMessage sessionAgentMessage = (SessionAgentMessage)agentMessage;
            AgentSessionStore mainSessionStore = sessionAgentMessage.getMainSessionStore();
            if(mainSessionStore != null) {
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
                sessionAgentMessage.setSessionStore(agentSessionStore);
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
        return AIAgentUtil.multimodalImageGeneration(maasName, imageAgentMessage);
    }
    public ImageEvent genImage( ImageAgentMessage imageAgentMessage){
        reactMessage(  imageAgentMessage);
        return AIAgentUtil.multimodalImageGeneration(imageAgentMessage.getMaas(), imageAgentMessage);
    }

    /**
     * 提交视频生成任务
     * @param maasName
     * @param videoAgentMessage
     * @return
     */
    public VideoTask submitVideoTask(String maasName,VideoAgentMessage videoAgentMessage){
        reactMessage(  videoAgentMessage);
        return AIAgentUtil.submitVideoTask(maasName,videoAgentMessage);
    }
    public VideoTask submitVideoTask(VideoAgentMessage videoAgentMessage){
        reactMessage(  videoAgentMessage);
        return AIAgentUtil.submitVideoTask(videoAgentMessage.getMaas(),videoAgentMessage);
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
        return AIAgentUtil.multimodalAudioGeneration( audioAgentMessage.getMaas(),audioAgentMessage);
    }

    /**
     * 调用音频合成模型，生成音频
     * @param audioAgentMessage
     * @return
     */
    public Flux<ServerEvent> streamAudioGen(AudioAgentMessage audioAgentMessage){
        reactMessage(  audioAgentMessage);
        return AIAgentUtil.streamAudioGenerationEvent(audioAgentMessage.getMaas(),audioAgentMessage);
    }

    /**
     * 调用音频合成模型，生成音频
     * @param maasName
     * @param audioAgentMessage
     * @return
     */
    public Flux<ServerEvent> streamAudioGen(String maasName,  AudioAgentMessage audioAgentMessage){
        reactMessage(  audioAgentMessage);
        return AIAgentUtil.streamAudioGenerationEvent(maasName,audioAgentMessage);
    }
    /**
     * 调用音频合成模型，生成音频
     * @param maasName
     * @param audioAgentMessage
     * @return
     */
    public AudioEvent genAudio(String maasName,   AudioAgentMessage audioAgentMessage){
        reactMessage(  audioAgentMessage);
        return AIAgentUtil.multimodalAudioGeneration(maasName, audioAgentMessage);
    }

    /**
     * 实现流式音频识别处理
     * @param maasName
     * @param audioSTTAgentMessage
     * @return
     */
    public Flux<ServerEvent> streamAudioParser(String maasName,  AudioSTTAgentMessage audioSTTAgentMessage){
        reactMessage(  audioSTTAgentMessage);
        audioSTTAgentMessage.init();

        return AIAgentUtil.streamChatCompletionEvent(maasName, audioSTTAgentMessage);
    }
    /**
     * 实现流式图片识别处理
     * @param maasName
     * @param imageVLAgentMessage
     * @return
     */
    public Flux<ServerEvent> streamImageParser(String maasName,   ImageVLAgentMessage imageVLAgentMessage){
        reactMessage(  imageVLAgentMessage);
        imageVLAgentMessage.init();
   
        return AIAgentUtil.streamChatCompletionEvent(maasName, imageVLAgentMessage);
    }
    /**
     * 实现流式图片识别处理
     * @param videoVLAgentMessage
     * @return
     */
    public Flux<ServerEvent> streamVideoParser(   VideoVLAgentMessage videoVLAgentMessage){
        reactMessage(  videoVLAgentMessage);
        return AIAgentUtil.streamChatCompletionEvent(videoVLAgentMessage.getMaas(), videoVLAgentMessage);
    }
    
    /**
     * 实现流式视频识别处理
     * @param maasName
     * @param videoVLAgentMessage
     * @return
     */
    public Flux<ServerEvent> streamVideoParser(String maasName,   VideoVLAgentMessage videoVLAgentMessage){
        reactMessage(  videoVLAgentMessage);

        videoVLAgentMessage.init();

        return AIAgentUtil.streamChatCompletionEvent(maasName, videoVLAgentMessage);
    }
    /**
     * 实现流式图片识别处理
     * @param imageVLAgentMessage
     * @return
     */
    public Flux<ServerEvent> streamImageParser(   ImageVLAgentMessage imageVLAgentMessage){
        reactMessage(  imageVLAgentMessage);
        return AIAgentUtil.streamChatCompletionEvent(imageVLAgentMessage.getMaas(), imageVLAgentMessage);
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
        chatAgentMessage.init();

        return AIAgentUtil.streamChatCompletionEvent(maasName, chatAgentMessage);
    }

    /**
     * 实现流式智能问答功能,在指定的数据源上执行
     */
    public Flux<ServerEvent> streamChat( ChatAgentMessage chatAgentMessage){
        reactMessage(  chatAgentMessage);
        return AIAgentUtil.streamChatCompletionEvent(chatAgentMessage.getMaas(), chatAgentMessage);
    }

    /**
     * 实现同步智能问答,在指定的数据源上执行
     * @deprecated 请使用chat方法
     */
    @Deprecated
    public ServerEvent chatCompletionEvent(String maasName,  ChatAgentMessage chatAgentMessage){
        reactMessage(  chatAgentMessage);
        return AIAgentUtil.chatCompletionEvent(maasName,chatAgentMessage);
    }

    /**
     * 实现同步智能问答,在指定的数据源上执行
     */
    public ServerEvent chat(String maasName,  ChatAgentMessage chatAgentMessage){
        reactMessage(  chatAgentMessage);
        ServerEvent serverEvent = AIAgentUtil.chatCompletionEvent(maasName,chatAgentMessage);
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
        return AIAgentUtil.imageParser(imageVLAgentMessage.getMaas(), imageVLAgentMessage);
    }

    /**
     * 实现同步图片识别处理
     * @param maasName
     * @param imageVLAgentMessage
     * @return
     */
    public ServerEvent imageParser(String maasName,  ImageVLAgentMessage imageVLAgentMessage){
        reactMessage( imageVLAgentMessage);
        return AIAgentUtil.imageParser(maasName, imageVLAgentMessage);
    }
    /**
     * 实现同步音频识别处理
     * @param maasName
     * @param audioSTTAgentMessage
     * @return
     */
    public ServerEvent audioParser(String maasName, AudioSTTAgentMessage audioSTTAgentMessage){
        reactMessage( audioSTTAgentMessage);
        return AIAgentUtil.audioParser(maasName,audioSTTAgentMessage);
    }
    /**
     * 实现同步音频识别处理
     * @param videoVLAgentMessage
     * @return
     */
    public ServerEvent videoParser( VideoVLAgentMessage videoVLAgentMessage){
        reactMessage( videoVLAgentMessage);
        return AIAgentUtil.videoParser(videoVLAgentMessage.getMaas(),videoVLAgentMessage);
    }
    /**
     * 实现同步音频识别处理
     * @param maasName
     * @param videoVLAgentMessage
     * @return
     */
    public ServerEvent videoParser(String maasName, VideoVLAgentMessage videoVLAgentMessage){
        reactMessage( videoVLAgentMessage);
        return AIAgentUtil.videoParser(maasName,videoVLAgentMessage);
    }
    



}
