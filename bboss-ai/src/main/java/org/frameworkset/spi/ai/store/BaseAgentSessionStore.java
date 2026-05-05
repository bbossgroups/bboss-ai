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

import org.frameworkset.spi.ai.AIAgent;
import org.frameworkset.spi.ai.model.LastSessionMessage;
import org.frameworkset.spi.ai.model.ServerEvent;
import org.frameworkset.spi.ai.util.BaseStreamDataBuilder;
import org.frameworkset.spi.ai.util.MessageBuilder;
import org.frameworkset.util.concurrent.IntegerCount;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.frameworkset.spi.ai.store.SessionMessage.MESSAGE_TYPE_AGENTRESULTMESSAGE;
import static org.frameworkset.spi.ai.store.SessionMessage.MESSAGE_TYPE_MIDDLE_MESSAGE;

/**
 * @author biaoping.yin
 * @Date 2026/4/2
 */
public abstract class BaseAgentSessionStore<T extends BaseAgentSessionStore> implements AgentSessionStore<T>{
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
    /**
     * 会话对应的agentId
     */
    protected String agentId;
    protected StoreContext storeContext;
    protected AgentSessionStore parentAgentSessionStore;

    protected AIAgent aiAgent;

 


    protected AgentSessionStore mainAgentSessionStore;
    /** 短期记忆：使用静态变量存储会话记忆（实际项目中建议使用缓存或数据库）*/
    protected List<Map<String, Object>> sessionMemory;
    public BaseAgentSessionStore(List<Map<String, Object>> sessionMemory){
        this.sessionMemory = sessionMemory;

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
        if(agentId == null){
            this.agentId = "agentId-0";
        }
        this.sessionMemory = storeContext.getSessionMemory();
        this.sessionSize = storeContext.getSessionSize();
        if(sessionMemory == null){
            this.sessionMemory = new ArrayList<>();
        }

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
    public void appendSessionMessageFromParent(Map<String, Object> message){
        appendSessionMessage( message);
        
    }
    protected void appendSessionMessage(Map<String, Object> message){
        if(sessionMemory == null){
            return ;
        }
        sessionMemory.add(message);
        if(sessionSize > 0 && sessionMemory.size() > sessionSize){
            sessionMemory.remove(0);
        }
    }



    @Override
    public void addSessionMessage(Map<String, Object> message){

        if(sessionMemory == null){
            return ;
        }
        appendSessionMessage(message);
        if(mainAgentSessionStore != null){
            mainAgentSessionStore.persistentSessionMessage(message, agentId,this.getParantAgentId(),null,null,MESSAGE_TYPE_MIDDLE_MESSAGE);
        }
        else if(this.persistentSessionMemory){
            persistentSessionMessage(message, agentId,this.getParantAgentId(),null,null,MESSAGE_TYPE_MIDDLE_MESSAGE);
        }
         
    }

    @Override
    public LastSessionMessage addAgentResultSessionMessage(String message){
        Map<String, Object> assistantMessage = MessageBuilder.buildAssistantMessage(message);
        LastSessionMessage lastSubAgentSessionMessage = null;
        if(sessionMemory == null){
            lastSubAgentSessionMessage = new LastSessionMessage();
            lastSubAgentSessionMessage.setLastSessionMessage(assistantMessage);
            lastSubAgentSessionMessage.setRequestId(this.getRequestId());
            return lastSubAgentSessionMessage;
        }
        appendSessionMessage(assistantMessage);

        if(parentAgentSessionStore != null){
            if(aiAgent != null ){
                if(!aiAgent.isDisablePush2ParentLastSubMessage()) {
//                if(!aiAgent.isDisableGloableStore()) {
                    lastSubAgentSessionMessage = parentAgentSessionStore.addAgentResultSessionMessage(assistantMessage, agentId, this.getParantAgentId());
                }
                else{
                    parentAgentSessionStore.persistentSessionMessage(assistantMessage, agentId, this.getParantAgentId(),null,null,MESSAGE_TYPE_AGENTRESULTMESSAGE);
                }
            }
            else{
                lastSubAgentSessionMessage = parentAgentSessionStore.addAgentResultSessionMessage(assistantMessage, agentId, this.getParantAgentId());
            }
            

        }
        else{
            lastSubAgentSessionMessage = new LastSessionMessage();
            lastSubAgentSessionMessage.setLastSessionMessage(assistantMessage);
            lastSubAgentSessionMessage.setRequestId(this.getRequestId());
        }



        return lastSubAgentSessionMessage;
    }
    /**
     * 添加子智能体结果消息
     * @param message
     * @param agentId
     * @param parentAgentId
     */
    @Override
    public LastSessionMessage addAgentResultSessionMessage(Map<String, Object> message,String agentId,String parentAgentId){

        LastSessionMessage lastSessionMessage = null;
        if(this.mainAgentSessionStore != null) {//需要通过主智能体持久化消息
//            loadSessionMemory(message,  agentId);
            //msgId,createTime,sessionId,seqNo,message,role
            lastSessionMessage  = mainAgentSessionStore.persistentSessionMessage(message, agentId,parentAgentId,null,null,MESSAGE_TYPE_AGENTRESULTMESSAGE);
            
        }
        else if(this.persistentSessionMemory){//主智能体直接持久化消息
//            loadSessionMemory(message,  agentId);
            lastSessionMessage  = persistentSessionMessage(message, agentId,parentAgentId,null,null,MESSAGE_TYPE_AGENTRESULTMESSAGE);
            

        }
        //msgId,createTime,sessionId,seqNo,message,role


        appendSessionMessage(message);
        
        return lastSessionMessage;

    }

    @Override
    public void addSessionMessage( Map<String, Object> systemMessage,String prompt,String agentId,String parentAgentId){

        if(this.mainAgentSessionStore != null) {//需要通过主智能体持久化消息
            //msgId,createTime,sessionId,seqNo,message,role
            mainAgentSessionStore.persistentSessionMessage(systemMessage, agentId,parentAgentId,null,null,MESSAGE_TYPE_MIDDLE_MESSAGE);
        }
        else if(this.persistentSessionMemory){//主智能体直接持久化消息
            persistentSessionMessage(systemMessage, agentId,parentAgentId,null,null,MESSAGE_TYPE_MIDDLE_MESSAGE);//1 代表子智能体输出结果 0 代表子智能体中间消息，3 代表用户输入消息
        }
//            SQLExecutor.insertWithDBName(dataSource, agentSessionStoreDBConfig.getInsertSessionMessageSQL(),
//                    SimpleStringUtil.getUUID32(),new Date(),this.getSessionId(),agentId, integerCount.increament(), JsonUtil.object2json(systemMessage),
//                    systemMessage.get("role"));

        appendSessionMessage(systemMessage);
    }
 
    @Override
    public Map<String, Object> addAssistantSessionMessage(String message){
        if(sessionMemory == null){
            return null;
        }
        Map<String, Object> assistantMessage = MessageBuilder.buildAssistantMessage(message);
        addSessionMessage(assistantMessage);
        return assistantMessage;
    }
    @Override
    public Map<String, Object> addAssistantSessionMessage(ServerEvent serverEvent){
        if(sessionMemory == null){
            return null;
        }
        Map<String, Object> assistantMessage = MessageBuilder.buildAssistantMessage(serverEvent);

        addSessionMessage(assistantMessage);
        return assistantMessage;
    }
    @Override
    public Map<String, Object> addAssistantSessionMessage(BaseStreamDataBuilder baseStreamDataBuilder){
        if(sessionMemory == null){
            return null;
        }

        Map<String, Object> assistantMessage = MessageBuilder.buildAssistantMessage(  baseStreamDataBuilder);

        addSessionMessage(assistantMessage);
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
        if(lastSubAgentSessionMessage != null){
            return lastSubAgentSessionMessage;
        }
        else{
            return null;
        }
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

    public String getRequestId() {
        return requestId;
    }

    public T setRequestId(String requestId) {
        this.requestId = requestId;
        return (T) this;
    }
    
}
