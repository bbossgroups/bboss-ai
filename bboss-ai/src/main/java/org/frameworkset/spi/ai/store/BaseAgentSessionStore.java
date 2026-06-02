package org.frameworkset.spi.ai.store;
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

import com.frameworkset.util.JsonUtil;
import org.frameworkset.spi.ai.AIAgent;
import org.frameworkset.spi.ai.model.*;
import org.frameworkset.spi.ai.util.BaseStreamDataBuilder;
import org.frameworkset.spi.ai.util.MessageBuilder;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.frameworkset.spi.ai.store.SessionMessage.*;

/**
 * @author biaoping.yin
 * @Date 2026/4/2
 */
public abstract class BaseAgentSessionStore<T extends BaseAgentSessionStore> implements AgentSessionStore<T>{
    private static Logger log = org.slf4j.LoggerFactory.getLogger(BaseAgentSessionStore.class); 
    /**
     * 在内存中持久化用户消息
     */
    protected boolean persistentSessionMemory;
     
    /**
     * 用户会话id
     */

    protected String sessionId;



    /**
     * 前端用户请求id，每次请求生成一个
     */
    protected String requestId;
    /**
     * 用户id，可选
     */
    private String userId;

    private String traceId;
    /**
     * 会话对应的agentId
     */
    protected String agentId;
    protected StoreContext storeContext;
    protected AgentSessionStore parentAgentSessionStore;

    protected AIAgent aiAgent;


    protected AgentIdAssign agentIdAssign = new AgentIdAssign();
    public String genSubAgentId(){

        return "agentId-"+agentIdAssign.getAgentId();
    }

    protected AgentSessionStore mainAgentSessionStore;
    /** 短期记忆：使用静态变量存储会话记忆（实际项目中建议使用缓存或数据库）*/
    protected List<Map<String, Object>> sessionMemory;
    public BaseAgentSessionStore(List<Map<String, Object>> sessionMemory){
        this.sessionMemory = sessionMemory;

    }
    @Override
    public StoreContext getStoreContext(){
        if(this.storeContext != null){
            return storeContext;
        }
        if(this.mainAgentSessionStore != null && this.mainAgentSessionStore != this){
            return mainAgentSessionStore.getStoreContext();
        }
        return null;
    }
    @Override
    public T setAIAgent(AIAgent aiAgent) {
        this.aiAgent = aiAgent;
        return (T)this;
    }

    @Override
    public AIAgent getAiAgent() {
        return aiAgent;
    }

 

    public T setSessionMemory(List<Map<String, Object>> sessionMemory) {
        this.sessionMemory = sessionMemory;
        return (T) this;
    }

    public BaseAgentSessionStore(List<Map<String, Object>> sessionMemory, int sessionSize){
        this.sessionMemory = sessionMemory;
        this.sessionSize = sessionSize;

    }
    public BaseAgentSessionStore( int sessionSize){
        this.sessionMemory = new ArrayList<>();
        this.sessionSize = sessionSize;

    }
    public BaseAgentSessionStore( AgentSessionStore parentAgentSessionStore,int sessionSize){
        this.parentAgentSessionStore = parentAgentSessionStore;
        this.sessionMemory = new ArrayList<>();
        this.sessionSize = sessionSize;

    }
    public void addSelfSessionMessage(Map<String, Object> message){
        
    }
    public BaseAgentSessionStore(){
        this.sessionMemory = new ArrayList<>();

    }

    public AgentSessionStore getParentAgentSessionStore() {
        return parentAgentSessionStore;
    }

    public BaseAgentSessionStore(StoreContext storeContext){
        this.persistentSessionMemory = true;
        this.storeContext = storeContext;
        this.sessionId = storeContext.getSessionId();   
        this.userId = storeContext.getUserId();
        this.agentId = storeContext.getAgentId();
        this.requestId = storeContext.getRequestId();
        this.traceId = storeContext.getTraceId();
        if(agentId == null){
            this.agentId = "agentId-0";
        }
        this.sessionMemory = storeContext.getSessionMemory();
        this.sessionSize = storeContext.getSessionSize();
        if(sessionMemory == null){
            this.sessionMemory = new ArrayList<>();
        }

    }

    public String getTraceId() {
        return traceId;
    }

    /**
     * 子任务会话记忆
     */
    private Map<String,AgentSessionStore> subTaskSessionMemorys;

    public BaseAgentSessionStore(String sessionId, String userId, String agentId ) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.agentId = agentId;
    }


    public List<Map<String, Object>> getSessionMemory() {
        return sessionMemory;
    }
    @Override
    public void addSubTaskSessionMemory(String agentId,AgentSessionStore subTaskSessionMemory) {
        if(subTaskSessionMemorys == null){
            subTaskSessionMemorys = new LinkedHashMap<>();
        }
        this.subTaskSessionMemorys.put(agentId, subTaskSessionMemory);
    }


    public String getParantAgentId(){
        if(parentAgentSessionStore != null)
            return parentAgentSessionStore.getAgentId();
        else if(mainAgentSessionStore != null){
            return mainAgentSessionStore.getAgentId();
        }
        return null;
    }

    @Override
    public AgentSessionStore getSubTaskSessionMemory(String agentId) {
        if(subTaskSessionMemorys == null){
            return null;
        }
        return subTaskSessionMemorys.get(agentId);
    }

 
   
 
    @Override
    public void appendSessionMessageFromParent(Map<String,Object> message){
        appendSessionMessage( message);
        
    }
    protected void appendSessionMessage(Map<String,Object> persistentMessage){
        if(sessionMemory == null){
            return ;
        }
//        if(agentId != null && agentId.equals("parrelHotelAgent")){
//            log.info("appendSessionMessage ");
//        }
        sessionMemory.add(persistentMessage);
        if(sessionSize > 0 && sessionMemory.size() > sessionSize){
            sessionMemory.remove(0);
        }
    }
    @Override
    public void recordTraceMessage(TraceMessage traceMessage) {
        PersistentMessage persistentMessage = new PersistentMessage();
        persistentMessage.setMessage(traceMessage.getMessage());
        TokenMetrics tokenMetrics = new TokenMetrics();
        tokenMetrics.setStartTime(traceMessage.getStartTime());
        tokenMetrics.setEndTime(traceMessage.getEndTime());
        persistentMessage.setTokenMetrics(tokenMetrics);
        String metadata = null;
        if(traceMessage.getMetaData() != null){
            metadata = JsonUtil.object2json(traceMessage.getMetaData());
        }
        this.persistentSessionMessage(persistentMessage,this.getAgentId(), this.getParantAgentId(), (String)null, metadata, MESSAGE_TYPE_TRACE_MESSAGE);
    }

    /**
     * 0 代表子智能体辅助消息， 1 代表子智能体输出结果 2 代表用户输入消息 3 智能体系统消息 5 智能体跟踪消息
     * @param role
     * @return
     */

    private String messageType(String role){
        if("system".equals(role)){
            return MESSAGE_TYPE_SYSTEM_MESSAGE;
        }
        else if("user".equals(role)){
            return MESSAGE_TYPE_USER_MESSAGE;
        }
        else if("assistant".equals(role)){
            return MESSAGE_TYPE_ASSISTANT_MESSAGE;
        }
 
        return MESSAGE_TYPE_ASSISTANT_MESSAGE;
    }
    @Override
    public void addSessionMessage(PersistentMessage persistentMessage){

        if(sessionMemory == null){
            return ;
        }
        Map<String,Object> message = persistentMessage.getMessage();
        appendSessionMessage(message);
       
        String role = (String) message.get("role");
        String messageType = messageType(role);
        if(mainAgentSessionStore != null){
            mainAgentSessionStore.persistentSessionMessage(persistentMessage, agentId,this.getParantAgentId(),null,null, messageType);
        }
        else if(this.persistentSessionMemory){
            persistentSessionMessage(persistentMessage, agentId,this.getParantAgentId(),null,null, messageType);
        }
         
    }

    @Override
    public LastSessionMessage addAgentResultSessionMessage(ServerEvent serverEvent){
        Map<String, Object> assistantMessage = MessageBuilder.buildAssistantMessage(serverEvent.getData());
        
        return addAgentResultSessionMessage(assistantMessage, serverEvent.getTokenMetrics());
    }

     
    private LastSessionMessage addAgentResultSessionMessage(Map<String, Object> assistantMessage, TokenMetrics tokenMetrics){
//        Map<String, Object> assistantMessage = MessageBuilder.buildAssistantMessage(serverEvent.getData());
        LastSessionMessage lastSubAgentSessionMessage = null;
       
        if(sessionMemory == null){
            TokenMetrics tokenMetrics_ = tokenMetrics;
            long elapsed = 0l;

            if(tokenMetrics_ != null){
                if(tokenMetrics_.getStartTime() != null && tokenMetrics_.getEndTime() != null){
                    elapsed = tokenMetrics_.getEndTime() - tokenMetrics_.getStartTime();
                }
            }
            lastSubAgentSessionMessage = new LastSessionMessage();
            lastSubAgentSessionMessage.setLastSessionMessage(assistantMessage);
            lastSubAgentSessionMessage.setRequestId(this.getRequestId());
            
            lastSubAgentSessionMessage.setTokenMetrics(tokenMetrics);
            lastSubAgentSessionMessage.setElapsed(elapsed);
            return lastSubAgentSessionMessage;
        }
//        message.setMessage(assistantMessage);
        appendSessionMessage(assistantMessage);

        if(parentAgentSessionStore != null){
            if(aiAgent != null ){
                if(!aiAgent.isDisablePush2ParentLastSubMessage()) {
//                if(!aiAgent.isDisableGloableStore()) {
//                    lastSubAgentSessionMessage = parentAgentSessionStore.addAgentResultSessionMessage(assistantMessage, agentId, this.getParantAgentId());
                    parentAgentSessionStore.addAgentResultSessionMessage(assistantMessage, agentId, this.getParantAgentId());
                }
                else{

                }
            }
            else{
//                lastSubAgentSessionMessage = parentAgentSessionStore.addAgentResultSessionMessage(assistantMessage, agentId, this.getParantAgentId());
                parentAgentSessionStore.addAgentResultSessionMessage(assistantMessage, agentId, this.getParantAgentId());
            }


        }
//        else{
//            lastSubAgentSessionMessage = new LastSessionMessage();
//            lastSubAgentSessionMessage.setLastSessionMessage(assistantMessage);
//            lastSubAgentSessionMessage.setRequestId(this.getRequestId());
//        }

        if(mainAgentSessionStore != null){
            PersistentMessage persistentMessage = new PersistentMessage();
            persistentMessage.setMessage(assistantMessage);
            persistentMessage.setTokenMetrics(tokenMetrics);
            lastSubAgentSessionMessage = mainAgentSessionStore.persistentSessionMessage(persistentMessage, agentId, this.getParantAgentId(),null,null,MESSAGE_TYPE_AGENTRESULTMESSAGE);
        }
        else{
            TokenMetrics tokenMetrics_ = tokenMetrics;
            long elapsed = 0l;

            if(tokenMetrics_ != null){
                if(tokenMetrics_.getStartTime() != null && tokenMetrics_.getEndTime() != null){
                    elapsed = tokenMetrics_.getEndTime() - tokenMetrics_.getStartTime();
                }
            }
            lastSubAgentSessionMessage = new LastSessionMessage();
            lastSubAgentSessionMessage.setLastSessionMessage(assistantMessage);
            lastSubAgentSessionMessage.setTokenMetrics(tokenMetrics);
            lastSubAgentSessionMessage.setElapsed(elapsed);
            lastSubAgentSessionMessage.setRequestId(this.getRequestId());
        }


       
        return lastSubAgentSessionMessage;
    }
    @Override
    public LastSessionMessage addAgentResultSessionMessage(TokenMetrics tokenMetrics,String message){
        Map<String, Object> assistantMessage = MessageBuilder.buildAssistantMessage(message);
         
        return   addAgentResultSessionMessage(assistantMessage, tokenMetrics);
    }
    /**
     * 添加子智能体结果消息
     * @param persistentMessage
     * @param agentId
     * @param parentAgentId
     */
    @Override
    public LastSessionMessage addAgentResultSessionMessage(Map<String, Object> persistentMessage//Map<String, Object> message
                                                            ,String agentId,String parentAgentId){

        LastSessionMessage lastSessionMessage = null;
//        if(this.mainAgentSessionStore != null) {//需要通过主智能体持久化消息
////            loadSessionMemory(message,  agentId);
//            //msgId,createTime,sessionId,seqNo,message,role
//            lastSessionMessage  = mainAgentSessionStore.persistentSessionMessage(persistentMessage, agentId,parentAgentId,null,null,MESSAGE_TYPE_AGENTRESULTMESSAGE);
//            
//        }
//        else if(this.persistentSessionMemory){//主智能体直接持久化消息
////            loadSessionMemory(message,  agentId);
//            lastSessionMessage  = persistentSessionMessage(persistentMessage, agentId,parentAgentId,null,null,MESSAGE_TYPE_AGENTRESULTMESSAGE);
//            
//
//        }
        //msgId,createTime,sessionId,seqNo,message,role


        appendSessionMessage(persistentMessage);
        
        return lastSessionMessage;

    }

    @Override
    public void addSessionMessage( Map<String, Object> message//Map<String, Object> systemMessage
                                    ,String prompt,String agentId,String parentAgentId){
        String role = (String) message.get("role");
        String messageType = messageType(role);
        if(this.mainAgentSessionStore != null) {//需要通过主智能体持久化消息
            //msgId,createTime,sessionId,seqNo,message,role
            PersistentMessage persistentMessage = new PersistentMessage();
            persistentMessage.setMessage(message);
            mainAgentSessionStore.persistentSessionMessage(persistentMessage, agentId,parentAgentId,null,null, messageType);
        }
        else if(this.persistentSessionMemory){//主智能体直接持久化消息
            PersistentMessage persistentMessage = new PersistentMessage();
            persistentMessage.setMessage(message);
            persistentSessionMessage(persistentMessage, agentId,parentAgentId,null,null, messageType);//0 代表子智能体辅助消息， 1 代表子智能体输出结果 2 代表用户输入消息 3 智能体系统消息 5 智能体跟踪消息

            
        }
//            SQLExecutor.insertWithDBName(dataSource, agentSessionStoreDBConfig.getInsertSessionMessageSQL(),
//                    SimpleStringUtil.getUUID32(),new Date(),this.getSessionId(),agentId, integerCount.increament(), JsonUtil.object2json(systemMessage),
//                    systemMessage.get("role"));

        appendSessionMessage(message);
    }
 
//    @Override
//    public Map<String, Object> addAssistantSessionMessage(String message){
//        if(sessionMemory == null){
//            return null;
//        }
//        Map<String, Object> assistantMessage = MessageBuilder.buildAssistantMessage(message);
//        PersistentMessage persistentMessage = new PersistentMessage();
//        persistentMessage.setMessage(assistantMessage);
//        addSessionMessage(assistantMessage);
//        return assistantMessage;
//    }
    @Override
    public Map<String, Object> addAssistantSessionMessage(ServerEvent serverEvent){
        if(sessionMemory == null){
            return null;
        }
        Map<String, Object> assistantMessage = MessageBuilder.buildAssistantMessage(serverEvent);
        PersistentMessage persistentMessage = new PersistentMessage();
        persistentMessage.setMessage(assistantMessage);
        persistentMessage.setTokenMetrics(serverEvent.getTokenMetrics());
        addSessionMessage(persistentMessage);
        return assistantMessage;
    }
    @Override
    public Map<String, Object> addAssistantSessionMessage(BaseStreamDataBuilder baseStreamDataBuilder){
        if(sessionMemory == null){
            return null;
        }

        Map<String, Object> assistantMessage = MessageBuilder.buildAssistantMessage(  baseStreamDataBuilder);

        StreamData streamData = baseStreamDataBuilder.getToolCallsStreamData();
        PersistentMessage persistentMessage = new PersistentMessage();
        persistentMessage.setMessage(assistantMessage);
        persistentMessage.setTokenMetrics(streamData.getStreamTokenMetrics());
        persistentMessage.setTotalTokenMetrics(streamData.getTotalTokenMetrics());
        addSessionMessage(persistentMessage);
        return assistantMessage;
    }

//    /**
//     * 主agent初始化记忆消息，如果未加载记忆消息，则进行加载
//     * @param userMessage
//     * @return
//     */
//    protected abstract boolean loadSessionMemory(Map<String, Object> userMessage,String agentId);
//    /**
//     * 根据prompt和agentId加载记忆消息，如果未加载记忆消息，则进行加载
//     * @param prompt
//     * @param agentId
//     * @return
//     */
//    public abstract boolean loadSessionMemory(String prompt,String agentId);

    protected LastSessionMessage lastSubAgentSessionMessage;

    /**
     * 并行节点：所有并行分支执行完毕后的结果集合
     */
    protected List<LastSessionMessage> lastSubAgentSessionMessages;

    public List<LastSessionMessage> getLastSubAgentSessionMessages() {
        return lastSubAgentSessionMessages;
    }

    @Override
    public LastSessionMessage getLastSubAgentSessionMessage(){
//        this.loadSessionMemory(prompt,agentId);
        return lastSubAgentSessionMessage;
         
        /**
        if(this.loadSessionMemory(prompt,agentId))//如果是从历史数据中加载，则无需返回最近消息，否则需返回最新消息给子智能体
            return null;
        if(sessionMemory == null || sessionMemory.size() == 0){
            return null;
        }
        LastSessionMessage lastSessionMessage = new LastSessionMessage();
        lastSessionMessage.setLastSessionMessage(sessionMemory.get(sessionMemory.size() - 1));
        lastSessionMessage.setFreshMessage(true);
        return lastSessionMessage;
         */
    }
    /**
     * 会话窗口大小，默认20
     */
    protected int sessionSize = 50;
    @Override
    public T setSessionSize(int sessionSize) {
        this.sessionSize = sessionSize;
        return (T) this;
    }
    @Override
    public int getSessionSize() {
        return sessionSize;
    }

    
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAgentId() {
        return agentId;
    }

    public T setAgentId(String agentId) {
        this.agentId = agentId;

        return (T) this;
    }

    public AgentSessionStore getMainAgentSessionStore() {
        return mainAgentSessionStore;
    }

    public T setMainAgentSessionStore(AgentSessionStore mainAgentSessionStore) {
        this.mainAgentSessionStore = mainAgentSessionStore;
        return (T) this;
    }

    public void setParentAgentLastSessionMessage(LastSessionMessage lastSubAgentSessionMessage){
        if(this.parentAgentSessionStore != null){
            parentAgentSessionStore.setSubAgentLastSessionMessage(lastSubAgentSessionMessage);
        }
        else {
            this.lastSubAgentSessionMessage = lastSubAgentSessionMessage;
        }
    }
    
    public void setSubAgentLastSessionMessage(LastSessionMessage lastSubAgentSessionMessage){
        this.lastSubAgentSessionMessage = lastSubAgentSessionMessage;
        //todo 如果当前子智能体所属的父智能体是父智能体对应的上级智能体的的最后一个子智能体，那么需要级联设置
    }

    public void cleanLastSessionMessages(){
        if(this.lastSubAgentSessionMessages != null){
            lastSubAgentSessionMessages.clear();
        }
    }




    public String getRequestId() {
        return requestId;
    }

    public T setRequestId(String requestId) {
        this.requestId = requestId;
        return (T) this;
    }
    
}
