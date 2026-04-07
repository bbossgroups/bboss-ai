package org.frameworkset.spi.ai.model;
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

import org.frameworkset.spi.ai.store.*;
import org.frameworkset.spi.ai.util.BaseStreamDataBuilder;

import java.util.List;
import java.util.Map;

/**
 * @author biaoping.yin
 * @Date 2026/1/4
 */
public abstract class SessionAgentMessage<T extends SessionAgentMessage> extends AgentMessage<T> {
    
    
    /** 使用静态变量存储会话记忆（实际项目中建议使用缓存或数据库）*/
    private AgentSessionStore sessionStore;
    private AgentSessionStore mainSessionStore;
    private StoreContext storeContext;
    private AgentSessionStoreBuilder agentSessionStoreBuilder = new DefaultAgentSessionStoreBuilder();

    public T setStoreContext(StoreContext storeContext) {
        this.storeContext = storeContext;
        return (T)this;
    }

    public T setSessionMemory(List<Map<String,Object>> session) {

        if(sessionStore == null) {
            storeContext = new StoreContext();
            storeContext.setSessionMemory(session);
            sessionStore = this.agentSessionStoreBuilder.build(storeContext);
            mainSessionStore = sessionStore;
        }
        else if(sessionStore instanceof AgentSessionStoreMemory){
            AgentSessionStoreMemory agentSessionStoreMemory = (AgentSessionStoreMemory)sessionStore;
            if(agentSessionStoreMemory.getSessionMemory() != null){
                throw new AIRuntimeException("Session memory already exists");
            }
            agentSessionStoreMemory.setSessionMemory(session);
            mainSessionStore = sessionStore;
            
        }
        return (T)this;
    }
    
    
    public T setSessionMemory(List<Map<String,Object>> session,int sessionSize) {
        if(sessionStore == null) {
            storeContext = new StoreContext();
            storeContext.setSessionSize(sessionSize);
            storeContext.setSessionMemory(session);
            sessionStore = this.agentSessionStoreBuilder.build(storeContext);
            mainSessionStore = sessionStore;
        }
        sessionStore.setSessionSize(sessionSize);

        return (T)this;
    }

    public T setSessionSize(int sessionSize) {
        
        if(sessionStore == null) {
            if(storeContext == null){
                storeContext = new StoreContext();
                storeContext.setSessionSize(sessionSize);
            }
            sessionStore = this.agentSessionStoreBuilder.build(storeContext);
            mainSessionStore = sessionStore;
        }
        else {
            if(storeContext == null){
                storeContext = new StoreContext();
                storeContext.setSessionSize(sessionSize);
            }
            sessionStore.setSessionSize(sessionSize);
            mainSessionStore.setSessionSize(sessionSize);
        }

        return (T)this;
    }
    public List<Map<String,Object>> getSessionMemory() {
        initSessionStore();
        if(sessionStore == null){
            return null;
        }
         
        return sessionStore.getSessionMemory();
    }

    public T setSessionStore(AgentSessionStore sessionStore) {
        this.sessionStore = sessionStore;
        
        return (T)this;
    }

    public AgentSessionStore getMainSessionStore() {
        initSessionStore();
        return mainSessionStore;
    }

    public AgentSessionStore getSessionStore() {
        initSessionStore();
        return sessionStore;
    }
    
    public T addSubTaskSessionStore(String agentId,AgentSessionStore subTaskSessionStore) {
        initSessionStore();
        sessionStore.addSubTaskSessionMemory(agentId, subTaskSessionStore);
        return (T)this;
    }
    
    

    public AgentSessionStore getSubTaskSessionMemory(String agentId) {
        initSessionStore();
        if(sessionStore == null){
            return null;
        }
        return sessionStore.getSubTaskSessionMemory(agentId);
    }

    private void initSessionStore(){
        if(sessionStore == null && storeContext != null){
            sessionStore = this.agentSessionStoreBuilder.build(storeContext);
            mainSessionStore = sessionStore;
        }
    }

    public int getSessionSize() {
        initSessionStore();
        if (sessionStore != null)
            return sessionStore.getSessionSize();
        return 0;
    
    }

    public T addSessionMessage(Map<String, Object> systemMessage,String prompt){
        initSessionStore();
        if(sessionStore == null){
            return (T)this;
        }
        sessionStore.addSessionMessage(systemMessage,  prompt,  sessionStore.getAgentId());
        
        return (T)this;
    }
    
    public T addSessionMessage(Map<String, Object> message){   
        initSessionStore();
        if(sessionStore == null){
            return (T)this;
        }
        
        sessionStore.addSessionMessage(message);         
        return (T)this;
    }
    public Map<String,Object> addAssistantSessionMessage(String message){
        initSessionStore();
        if(sessionStore == null){
            return null;
        }
        return sessionStore.addAssistantSessionMessage(message);
    }

    public Map<String,Object> addAssistantSessionMessage(ServerEvent serverEvent){
        initSessionStore();
        if(sessionStore == null){
            return null;
        }
        return sessionStore.addAssistantSessionMessage(serverEvent);
    }

    public Map<String,Object> addAssistantSessionMessage(BaseStreamDataBuilder baseStreamDataBuilder){
        initSessionStore();
        if(sessionStore == null){
            return null;
        }
        return sessionStore.addAssistantSessionMessage(baseStreamDataBuilder);
    }



    @Deprecated
    /**
     * 添加会话消息
     * @param message
     * @return
     * @deprecated 请使用addAssistantSessionMessage方法
     */
    public Map<String,Object> addSessionMessage(String message){
         
        return addAssistantSessionMessage(  message);
    }
}
