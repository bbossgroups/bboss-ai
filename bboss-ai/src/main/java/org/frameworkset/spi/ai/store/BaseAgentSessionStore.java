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

import org.frameworkset.spi.ai.model.ServerEvent;
import org.frameworkset.spi.ai.util.BaseStreamDataBuilder;
import org.frameworkset.spi.ai.util.MessageBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author biaoping.yin
 * @Date 2026/4/2
 */
public abstract class BaseAgentSessionStore<T extends BaseAgentSessionStore> implements AgentSessionStore<T>{
    /**
     * 用户会话id
     */

    private String sessionId;
    /**
     * 用户id，可选
     */
    private String userId;
    /**
     * 会话对应的agentId
     */
    private String agentId;
    protected StoreContext storeContext;
    protected AgentSessionStore parentAgentSessionStore;
    /** 短期记忆：使用静态变量存储会话记忆（实际项目中建议使用缓存或数据库）*/
    protected List<Map<String, Object>> sessionMemory;
    public BaseAgentSessionStore(List<Map<String, Object>> sessionMemory){
        this.sessionMemory = sessionMemory;

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

    public BaseAgentSessionStore(){
        this.sessionMemory = new ArrayList<>();

    }

    public AgentSessionStore getParentAgentSessionStore() {
        return parentAgentSessionStore;
    }

    public BaseAgentSessionStore(StoreContext storeContext){
        this.storeContext = storeContext;
        this.sessionId = storeContext.getSessionId();   
        this.userId = storeContext.getUserId();
        this.agentId = storeContext.getAgentId();
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


    @Override
    public AgentSessionStore getSubTaskSessionMemory(String agentId) {
        if(subTaskSessionMemorys == null){
            return null;
        }
        return subTaskSessionMemorys.get(agentId);
    }


    @Override
    public void appendSessionMessageFromParent(Map<String, Object> message){
        _addSessionMessage(message,true);
    }

    private void _addSessionMessage(Map<String, Object> message,boolean appendSessionMessageFromParent){
        if(sessionMemory == null){
            return ;
        }
        sessionMemory.add(message);
        if(sessionSize > 0 && sessionMemory.size() > sessionSize){
            sessionMemory.remove(0);
        }
        if(!appendSessionMessageFromParent && parentAgentSessionStore != null){
            parentAgentSessionStore.addSessionMessage(message,agentId);
        }
    }
    @Override
    public void addSessionMessage(Map<String, Object> message){
        _addSessionMessage(message,false);
         
    }
    @Override
    public void addSessionMessage(Map<String, Object> message,String agentId){
        _addSessionMessage(message,false);
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
    public Map<String,Object> getLastMessage(String prompt,String agentId){
        if(sessionMemory == null || sessionMemory.size() == 0){
            return null;
        }
        return sessionMemory.get(sessionMemory.size() - 1);
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
}
