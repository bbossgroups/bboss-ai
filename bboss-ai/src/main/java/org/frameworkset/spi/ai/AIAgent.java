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
import org.apache.commons.collections.CollectionUtils;
import org.frameworkset.spi.ai.callback.AgentOutput;
import org.frameworkset.spi.ai.callback.ChatContext;
import org.frameworkset.spi.ai.material.StoreFilePathFunction;
import org.frameworkset.spi.ai.model.*;
import org.frameworkset.spi.ai.store.*;
import org.frameworkset.spi.ai.tool.BeanToolHandle;
import org.frameworkset.spi.ai.tools.ToolsRegist;
import org.frameworkset.spi.ai.util.AIAgentUtil;
import org.frameworkset.spi.reactor.DisposeEventHandler;
import org.slf4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 多模态智能体对象
 * @author biaoping.yin
 * @Date 2026/1/4
 */
public class AIAgent<T extends AIAgent> {
    private static Logger logger = org.slf4j.LoggerFactory.getLogger(AIAgent.class);
    protected String prompt;
    protected String systemPrompt;
    private String type;
    protected int sessionSize;


    /**
     * 输出变量名
     */
    protected String outputVaribleName;

    /**
     * 输出变量名
     */
    protected int outputVaribleScope = AIFlowConst.AIFLOW_VAR_SCOPE_FLOW;
    
    protected AgentOutput agentOutput;

    protected AgentSessionStore mainSessionStore;
    protected StoreContext storeContext;

    public AIAgent(StoreContext storeContext) {
        this.storeContext = storeContext;

    }
    private Object mainSessionStoreLock = new Object();
    protected void initSessionStore(){
        if(mainSessionStore != null)
            return;
        synchronized (mainSessionStoreLock) {
            if(mainSessionStore != null)
                return;
            if (mainSessionStore == null && storeContext != null) {
                AgentSessionStoreBuilder agentSessionStoreBuilder = new DefaultAgentSessionStoreBuilder();
                mainSessionStore = agentSessionStoreBuilder.build(storeContext);
                mainSessionStore.setAIAgent(this);
                if (agentMessage != null && agentMessage instanceof SessionAgentMessage) {
                    ((SessionAgentMessage) agentMessage).setMainSessionStore(mainSessionStore);
                }
                if(storeContext.isResetSession() && storeContext.getSessionId() != null){
                    mainSessionStore.removeSession(storeContext.getSessionId());
                }
            }
        }
    }
    /**
     * true 智能体会话记录不会被记录到父智能体的会议记忆，同时也不会加载父智能体的会话记忆
     * false 会记录
     * 已经废弃
     */
    protected boolean disableGloableStore;


    /**
     * true 智能体会话记录不会被记录到父智能体的会议记忆
     * false 会记录
     *
     */
    protected boolean disablePush2ParentLastSubMessage;

    /**
     * true 控制智能体不会加载父智能体的会话记忆
     * false 会加载
     *
     */
    protected boolean disableReferenceParentLastSubMessage;

    /**
     * 工具清单，标准工具规范格式
     */
    protected List<FunctionToolDefine> tools;



    protected boolean disableStream;
    protected boolean sequenceHeaderNode;

    @JsonIgnore
    protected Map<String,FunctionCall> toolCalls;

    @JsonIgnore
    protected ToolsRegist toolsRegist;
    @JsonIgnore
    protected AgentSessionStore agentSessionStore;
    @JsonIgnore
    protected AgentSessionStore parentSessionStore;
    protected String agentId;

    public T setParentAgent(AIAgent parentAgent) {
        this.parentAgent = parentAgent;
        if(this.agentId == null){
            agentId = parentAgent.genSubAgentId();
        }
        return (T)this;
    }

    public boolean isSequenceHeaderNode() {
        return sequenceHeaderNode;
    }

    public T setSequenceHeaderNode(boolean sequenceHeaderNode) {
        this.sequenceHeaderNode = sequenceHeaderNode;
        return (T)this;
    }

    protected AIAgent parentAgent;


    protected String agentName;
    @JsonIgnore
    protected AgentMessage agentMessage;
    public AIAgent(){
        this((String)null);
    }
    public float[] embedding(EmbeddingMessage embeddingMessage){
        return AIAgentUtil.embedding(embeddingMessage,this);
    }
    public List<RerankedDocument> rerank(RerankMessage rerankMessage){
        return AIAgentUtil.rerank(rerankMessage,this);
    }
    public FluxSink<ServerEvent> getAgentFluxSink(){
        return null;
    }
    
    public DisposeEventHandler getDisposeEventHandler(){
        return null;
    }

    public Flux<ServerEvent> getFlux(){
        return null;
    }
    
    public AIAgent getParentAgent(){
        if(parentAgent != null){
            return parentAgent;
        }
        if(this.parentSessionStore != null){
            return parentSessionStore.getAiAgent();
        }
        return null;
    }

    public LastSessionMessage getLastSessionMessage(){
        return agentSessionStore != null?agentSessionStore.getLastSubAgentSessionMessage():null;
    }

    public void cleanLastSessionMessages(){
        if(this.agentSessionStore != null){
            agentSessionStore.cleanLastSessionMessages();
        }
    }
    public List<LastSessionMessage> getLastSessionMessages(){
        return agentSessionStore != null?agentSessionStore.getLastSubAgentSessionMessages():null;
    }

    public T setAgentMessage(AgentMessage agentMessage) {
        this.agentMessage = agentMessage;
        return (T)this;
    }

    public AgentMessage getAgentMessage() {
        return agentMessage;
    }

    public boolean isDisableGloableStore() {
        return disableGloableStore;
    }

    public T setPrompt(String prompt) {
        this.prompt = prompt;
        return (T)this;
    }

    protected AgentIdAssign agentIdAssign = new AgentIdAssign();
    public String genSubAgentId(){
        return this.agentId + "-"+agentIdAssign.getAgentId();
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

    public T setParentSessionStore(AgentSessionStore parentSessionStore) {
        this.parentSessionStore = parentSessionStore;
        return (T)this;
    }

    public AgentSessionStore getParentSessionStore() {
        return parentSessionStore;
    }

    public AgentSessionStore getAgentSessionStore() {
        return agentSessionStore;
    }

    public AgentSessionStore getMainSessionStore() {
        if(mainSessionStore != null)
            return mainSessionStore;
        if(parentAgent != null){
            return parentAgent.getMainSessionStore();
        }
        if(storeContext != null){
            initSessionStore();
        }
        return mainSessionStore;
    }

    public AIAgent(String prompt, String type, ToolsRegist toolsRegist, Integer sessionSize){
        this.prompt = prompt;
        this.type = type;
        this.toolsRegist = toolsRegist;
//        this.agentId = SimpleStringUtil.getUUID32();
        if(sessionSize != null ){
            this.sessionSize = sessionSize;
        }
//        this.agentId = SimpleStringUtil.getUUID32();
        this.agentName = this.getClass().getName();
//            agentSessionStore = new AgentSessionStoreMemory(sessionSize);
    }

    public T setAgentId(String agentId) {
        this.agentId = agentId;
        return (T)this;
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

    public AIAgent(ToolsRegist toolsRegist){
        this(  null, null, toolsRegist,null);
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
    
    protected LastSessionMessage getLastSubAgentSessionMessage(AgentSessionStore mainSessionStore,AgentMessage agentMessage){
        //UserAgent无需追加父智能体中产生的最新的消息作
        LastSessionMessage lastSubAgentSessionMessage = null;
        if(parentSessionStore != null) {
            lastSubAgentSessionMessage = parentSessionStore.getLastSubAgentSessionMessage( );
        }
        else{
            lastSubAgentSessionMessage = mainSessionStore.getLastSubAgentSessionMessage( );
        }
        return lastSubAgentSessionMessage;
    }
    
    protected void loadHistoryMessages(AgentSessionStore mainSessionStore,AgentMessage agentMessage){
        if(agentSessionStore == null){
            return;
        }
        List<Map<String,Object>> sessionMemory = agentSessionStore.getSessionMemory();
        boolean empty = sessionMemory.isEmpty();
//                sessionAgentMessage.setSessionStore(agentSessionStore);
        mainSessionStore.addSubTaskSessionMemory(agentId, agentSessionStore);
        if(!isDisableReferenceParentLastSubMessage()) {
//        if(!isDisableGloableStore()) {
            //UserAgent无需追加父智能体中产生的最新的消息作
            LastSessionMessage lastSubAgentSessionMessage = getLastSubAgentSessionMessage(mainSessionStore, agentMessage);


            if (empty) {
                //加载历史消息
                List<Map<String, Object>> sessionMessages = mainSessionStore.getAgentSessionMessage(lastSubAgentSessionMessage, agentId, sessionSize);
                if (sessionMessages != null && sessionMessages.size() > 0) {
                    for (Map<String, Object> sessionMessage : sessionMessages) {
                        agentSessionStore.appendSessionMessageFromParent(sessionMessage);
                    }


                }
            } else if (lastSubAgentSessionMessage != null) {//不为空，直接append主智能体中的最后一条消息
                agentSessionStore.appendSessionMessageFromParent(lastSubAgentSessionMessage.getLastSessionMessage());
                mainSessionStore.saveLastSessionMessage(lastSubAgentSessionMessage, agentId);
                //记录消息引用关系
            }
        }
    }
    public void reactMessage(AgentMessage agentMessage){

        AgentSessionStore mainSessionStore = this.getMainSessionStore();
        SessionAgentMessage sessionAgentMessage = null;
        if( agentMessage instanceof SessionAgentMessage) {
            sessionAgentMessage = (SessionAgentMessage)agentMessage;
            if(mainSessionStore == null) {
                mainSessionStore = sessionAgentMessage.getMainSessionStore();
                this.mainSessionStore = mainSessionStore;
            }
            
        }
        
        if(agentId == null){
            if(parentAgent != null){
                agentId = parentAgent.genSubAgentId();
            }
            else{
                agentId = mainSessionStore != null?mainSessionStore.genSubAgentId(): SimpleStringUtil.getUUID32();
            }
        }
        if(mainSessionStore != null) {
             

            String title  = this.evalPrompt(agentMessage);
            AIAgent parentAgent = this.getParentAgent();
            if(parentAgent != null){
                String tmp = parentAgent.getFirstSubAgentPrompt();
                if(tmp != null){
                    title = tmp;
                }
            }

            mainSessionStore.loadSessionMemory(title, agentId);
            if(parentSessionStore == null && this.parentAgent != null){
                this.parentSessionStore = parentAgent.getAgentSessionStore();
            }
            if(agentSessionStore == null){
                if(parentSessionStore != null)
                    agentSessionStore = buildAgentSessionStore(parentSessionStore,sessionSize);
                else
                    agentSessionStore = buildAgentSessionStore(mainSessionStore,sessionSize);
                agentSessionStore.setAgentId(agentId);
                agentSessionStore.setAIAgent(this);
                agentSessionStore.setMainAgentSessionStore(mainSessionStore);
            }
            loadHistoryMessages(  mainSessionStore,  agentMessage);


        }
        
    }
    
    protected AgentSessionStore buildAgentSessionStore(AgentSessionStore parentSessionStore,int sessionSize){
        return new AgentSessionStoreMemory(parentSessionStore,sessionSize);
    }

    /**
     * 实现图片生成功能
     * @param maasName
     * @param imageAgentMessage
     * @return
     */
    public ImageEvent genImage(String maasName, ImageAgentMessage imageAgentMessage ){
        return genImage(  maasName,   imageAgentMessage, (StoreFilePathFunction)null);
    }
    /**
     * 实现图片生成功能
     * @param maasName
     * @param imageAgentMessage
     * @return
     */
    public ImageEvent genImage(String maasName, ImageAgentMessage imageAgentMessage, StoreFilePathFunction storeFilePathFunction){
        reactMessage(  imageAgentMessage);
        return AIAgentUtil.multimodalImageGeneration(maasName, imageAgentMessage,   storeFilePathFunction,this);
    }
    public ImageEvent genImage( ImageAgentMessage imageAgentMessage){
        return genImage(imageAgentMessage.getMaas(), imageAgentMessage );
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

    public VideoGenResult getVideoTaskResult(String maasName, VideoStoreAgentMessage videoStoreAgentMessage ){
        return getVideoTaskResult(  maasName,  videoStoreAgentMessage, (StoreFilePathFunction)null);
    }
    public VideoGenResult getVideoTaskResult(String maasName, VideoStoreAgentMessage videoStoreAgentMessage, StoreFilePathFunction storeFilePathFunction){
        return    AIAgentUtil.getVideoTaskResult(videoStoreAgentMessage.getMaas(),videoStoreAgentMessage,storeFilePathFunction);
    }
    

    public VideoGenResult getVideoTaskResult( VideoStoreAgentMessage videoStoreAgentMessage){
        return AIAgentUtil.getVideoTaskResult(videoStoreAgentMessage.getMaas(),videoStoreAgentMessage,(StoreFilePathFunction)null);
    }
    /**
     * 调用音频合成模型，生成音频
     * @param audioAgentMessage
     * @return
     */
    public AudioEvent genAudio(  AudioAgentMessage audioAgentMessage){
        reactMessage(  audioAgentMessage);
        return AIAgentUtil.multimodalAudioGeneration( audioAgentMessage.getMaas(),audioAgentMessage,(StoreFilePathFunction)null,this);
    }
    public AudioEvent genAudio(  AudioAgentMessage audioAgentMessage, StoreFilePathFunction storeFilePathFunction){
        return genAudio(  audioAgentMessage.getMaas(),  audioAgentMessage,   storeFilePathFunction);
    }
    /**
     * 调用音频合成模型，生成音频
     * @param audioAgentMessage
     * @return
     */
    public Flux<ServerEvent> streamAudioGen(AudioAgentMessage audioAgentMessage){
        reactMessage(  audioAgentMessage);
        return AIAgentUtil.streamAudioGenerationEvent(audioAgentMessage.getMaas(),audioAgentMessage,(StoreFilePathFunction)null,this);
    }

    /**
     * 调用音频合成模型，生成音频
     * @param maasName
     * @param audioAgentMessage
     * @return
     */
    public Flux<ServerEvent> streamAudioGen(String maasName,  AudioAgentMessage audioAgentMessage, StoreFilePathFunction storeFilePathFunction){
        reactMessage(  audioAgentMessage);
        return AIAgentUtil.streamAudioGenerationEvent(maasName,audioAgentMessage,storeFilePathFunction,this);
    }
    public Flux<ServerEvent> streamAudioGen(String maasName,  AudioAgentMessage audioAgentMessage ){
        return streamAudioGen(maasName, audioAgentMessage, (StoreFilePathFunction)null);
    }
    /**
     * 调用音频合成模型，生成音频
     * @param maasName
     * @param audioAgentMessage
     * @return
     */
    public AudioEvent genAudio(String maasName,   AudioAgentMessage audioAgentMessage){
        return genAudio(  maasName,     audioAgentMessage, (StoreFilePathFunction)null);
    }
    /**
     * 调用音频合成模型，生成音频
     * @param maasName
     * @param audioAgentMessage
     * @return
     */
    public AudioEvent genAudio(String maasName,   AudioAgentMessage audioAgentMessage, StoreFilePathFunction storeFilePathFunction){
        reactMessage(  audioAgentMessage);
        return AIAgentUtil.multimodalAudioGeneration(maasName, audioAgentMessage,storeFilePathFunction,this);
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
//    /**
//     * 实现流式智能问答功能,在指定的数据源上执行
//     */
//    public Flux<ServerEvent> streamChat(String maasName,   ChatAgentMessage chatAgentMessage){
//    
//        return streamChat(  maasName,     chatAgentMessage,true);
//    }

    /**
     * 实现流式智能问答功能,在指定的数据源上执行
     */
    public Flux<ServerEvent> streamChat(String maasName,   ChatAgentMessage chatAgentMessage ){
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
     * 实现流式智能问答功能,在指定的数据源上执行
     */
    public Flux<ServerEvent> streamChat(String maasName,   ChatAgentMessage chatAgentMessage, ChatContext chatStreamCallback ){
        reactMessage(  chatAgentMessage);
//        chatAgentMessage.init();

        return AIAgentUtil.streamChatCompletionEvent(maasName, chatAgentMessage,this,chatStreamCallback);
    }

    /**
     * 实现流式智能问答功能,在指定的数据源上执行
     */
    public Flux<ServerEvent> streamChat(ChatAgentMessage chatAgentMessage, ChatContext chatStreamCallback){
        return streamChat(chatAgentMessage.getMaas(),   chatAgentMessage, chatStreamCallback );
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


    public ServerEvent chat(String maasName,  ChatAgentMessage chatAgentMessage ){
        return chat(  maasName,   chatAgentMessage,(ChatContext)null);
    }
    /**
     * 实现同步智能问答,在指定的数据源上执行
     */
    public ServerEvent chat(String maasName,  ChatAgentMessage chatAgentMessage,ChatContext chatCallback){
        reactMessage(  chatAgentMessage);
        ServerEvent serverEvent = AIAgentUtil.chatCompletionEvent(maasName,chatAgentMessage,this,chatCallback);
        if(serverEvent != null && serverEvent.getData() != null){
//            Map<String,Object> message = chatAgentMessage.addAssistantSessionMessage(serverEvent.getData());
//            addAgentResultSessionMessage(serverEvent.getData());
            addAgentResultSessionMessage(serverEvent);
            
            if(chatCallback != null && chatCallback.getChatStreamCallback() != null){
                chatCallback.getChatStreamCallback().streamDone(serverEvent);
            }
        }
        return serverEvent;
//        return AIAgentUtil.chatCompletionEvent(maasName,chatAgentMessage);
    }
    
    public LastSessionMessage addAgentResultSessionMessage(TokenMetrics tokenMetrics,String message){
        LastSessionMessage lastSubAgentSessionMessage = null;
        if(this.agentSessionStore != null ) {

            lastSubAgentSessionMessage = this.agentSessionStore.addAgentResultSessionMessage(  tokenMetrics,message);
//            if( !isDisableGloableStore()) {
            if( !isDisablePush2ParentLastSubMessage()) {
                this.agentSessionStore.setParentAgentLastSessionMessage(lastSubAgentSessionMessage);
            }
        }
        return  lastSubAgentSessionMessage;
    }

    public LastSessionMessage addAgentResultSessionMessage(ServerEvent serverEvent){
        LastSessionMessage lastSubAgentSessionMessage = null;
        if(this.agentSessionStore != null ) {

            lastSubAgentSessionMessage = this.agentSessionStore.addAgentResultSessionMessage(serverEvent);
//            if( !isDisableGloableStore()) {
            if( !isDisablePush2ParentLastSubMessage()) {
                this.agentSessionStore.setParentAgentLastSessionMessage(lastSubAgentSessionMessage);
            }
        }
        return  lastSubAgentSessionMessage;
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
     * 实现同步智能问答,在指定的数据源上执行
     */
    public ServerEvent chat(ChatAgentMessage chatAgentMessage, ChatContext chatCallback){
        return chat(chatAgentMessage.getMaas(),    chatAgentMessage, chatCallback);

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
        return this.agentId != null ? this.agentId : agentSessionStore != null?this.agentSessionStore.getAgentId():null;
    }

    public String getParentAgentId() {
        if(parentAgent != null){
            return parentAgent.getAgentId();
        }
        if(agentSessionStore != null) {
            return this.agentSessionStore.getParantAgentId();
        }
        return null;
    }

    public List<Map<String, Object>> getSessionMemory() {
        if(this.agentSessionStore != null) {
            return this.agentSessionStore.getSessionMemory();
        }
        return null;
    }


    public List<FunctionToolDefine> getTools() {
        return tools;
    }

    public T setTools(List<FunctionToolDefine> tools) {
        reset();
        this.tools = tools;
        return (T)this;
    }

    public T registTools(List<FunctionToolDefine> tools) {
        reset();
        if(this.tools == null){
            this.tools = new ArrayList<>();
        }
        this.tools.addAll( tools);
        return (T)this;
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

    public T setToolsRegist(ToolsRegist toolsRegist) {
        reset();
        this.toolsRegist = toolsRegist;
        return (T)this;
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
    public T registBeanTool(Object beanTool) {
        List<FunctionToolDefine> functionToolDefines = BeanToolHandle.parserTools(beanTool);
        if(CollectionUtils.isNotEmpty(functionToolDefines)){
            reset();
            if (this.tools == null) {
                tools = new ArrayList<>();
            }
            tools.addAll(functionToolDefines);
        }
        

        return (T) this;
    }
    public T registTool(FunctionToolDefine functionToolDefine) {
        reset();
        if (this.tools == null) {
            tools = new ArrayList<>();
        }
        tools.add(functionToolDefine);

        return (T) this;
    }

    public T registToolCalls(Map<String,FunctionCall> toolCalls) {
        reset();
        if(this.toolCalls == null){
            toolCalls = new LinkedHashMap<>();
        }
        this.toolCalls.putAll(toolCalls);
        return (T)this;
    }
    public T registToolCall(String toolName,FunctionCall functionCall) {
        reset();
        if(this.toolCalls == null){
            toolCalls = new LinkedHashMap<>();
        }
        this.toolCalls.put(toolName, functionCall);
        return (T)this;
    }

    public String getAgentName() {
        return agentName;
    }

    public T setAgentName(String agentName) {
        this.agentName = agentName;
        return (T)this;
    }


    public  String evalSystemPrompt(AgentMessage agentMessage){
        String systemPrompt = this.getSystemPrompt();
        if(systemPrompt == null){           
            systemPrompt = agentMessage.getSystemPrompt();            
        }
        if(systemPrompt != null ){
            if(this.getParentAgent() != null) {
                this.getParentAgent().setFirstSubAgentSystemPrompt(systemPrompt);
            }
            else{
                this.setFirstSubAgentSystemPrompt(systemPrompt);
            }
        }
        return systemPrompt;
    }
    public  String evalPrompt(AgentMessage agentMessage){
        String prompt = this.getPrompt();
        if(prompt == null){           
            
            prompt = agentMessage.getPrompt();
            
        }
        if(prompt != null ){
            if(this.getParentAgent() != null) {
                this.getParentAgent().setFirstSubAgentPrompt(prompt);
            }
            else{
                this.setFirstSubAgentPrompt(prompt);
            }
        }
        return prompt;
    }

    private String firstSubAgentPrompt;
    
    public String getFirstSubAgentPrompt(){
        return this.firstSubAgentPrompt;
    }

    public T setFirstSubAgentPrompt(String firstSubAgentPrompt) {
        if(this.firstSubAgentPrompt == null && firstSubAgentPrompt != null) {
            this.firstSubAgentPrompt = firstSubAgentPrompt;
        }
        return (T)this;
    }

    private String firstSubAgentSystemPrompt;
    public T setFirstSubAgentSystemPrompt(String systemPrompt) {
        if(firstSubAgentSystemPrompt == null){
            this.firstSubAgentSystemPrompt = systemPrompt;
        }
        return (T)this;
    }
    public String getFirstSubAgentSystemPrompt(){
        return this.firstSubAgentSystemPrompt;
    }
    public boolean isDisableStream() {
        if(disableStream) {
            return disableStream;
        }
        else if(this.parentAgent != null && this.parentAgent.isDisableStream()){
            return true;
            
        }
        return false;
    }

    public void setDisableStream(boolean disableStream) {
        this.disableStream = disableStream;
    }

    public boolean isDisablePush2ParentLastSubMessage() {
        return disablePush2ParentLastSubMessage;
    }

    public void setDisablePush2ParentLastSubMessage(boolean disablePush2ParentLastSubMessage) {
        this.disablePush2ParentLastSubMessage = disablePush2ParentLastSubMessage;
    }

    public boolean isDisableReferenceParentLastSubMessage() {
        return disableReferenceParentLastSubMessage;
    }

    public void setDisableReferenceParentLastSubMessage(boolean disableReferenceParentLastSubMessage) {
        this.disableReferenceParentLastSubMessage = disableReferenceParentLastSubMessage;
    }

    public String getOutputVaribleName() {
        return outputVaribleName;
    }
    
    public T setOutputVaribleName(String outputVaribleName, int outputVaribleScope) {
        this.outputVaribleName = outputVaribleName;
        this.outputVaribleScope = outputVaribleScope;
        return (T)this;
    }

    public int getOutputVaribleScope() {
        return outputVaribleScope;
    }

    /**
     * 变量范围：流程
     * @return
     */
    public boolean isFlowOutputVaribleScope(){
        return outputVaribleScope == AIFlowConst.AIFLOW_VAR_SCOPE_FLOW;
    }

    /**
     * 变量范围：容器级别
     * @return
     */
    public boolean isContainerOutputVaribleScope(){
        return outputVaribleScope == AIFlowConst.AIFLOW_VAR_SCOPE_CONTAINER;
    }

    /**
     * 变量范围：节点级别
     * @return
     */
    public boolean isNodeOutputVaribleScope(){
        return outputVaribleScope == AIFlowConst.AIFLOW_VAR_SCOPE_NODE;
    }

    public T setAgentOutput(AgentOutput agentOutput) {
        this.agentOutput = agentOutput;
        return (T)this;
    }

    public AgentOutput getAgentOutput() {
        return agentOutput;
    }
}
