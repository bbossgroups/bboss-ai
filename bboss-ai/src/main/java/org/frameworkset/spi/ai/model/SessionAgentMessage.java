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

import org.frameworkset.spi.ai.AIAgent;
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
//    private AgentSessionStore sessionStore;
    private AgentSessionStore mainSessionStore;
    private StoreContext storeContext;
    private AgentSessionStoreBuilder agentSessionStoreBuilder = new DefaultAgentSessionStoreBuilder();

    public T setStoreContext(StoreContext storeContext) {
        this.storeContext = storeContext;
        return (T)this;
    }

    public T setSessionMemory(List<Map<String,Object>> session) {

        if(mainSessionStore == null) {
            storeContext = new StoreContext();
            storeContext.setSessionMemory(session);
            mainSessionStore = this.agentSessionStoreBuilder.build(storeContext);
//            mainSessionStore = sessionStore;
        }
        else if(mainSessionStore instanceof AgentSessionStoreMemory){
            AgentSessionStoreMemory agentSessionStoreMemory = (AgentSessionStoreMemory)mainSessionStore;
            if(agentSessionStoreMemory.getSessionMemory() != null){
                throw new AIRuntimeException("Session memory already exists");
            }
            agentSessionStoreMemory.setSessionMemory(session);
//            mainSessionStore = sessionStore;
            
        }
        return (T)this;
    }
    
    
    public T setSessionMemory(List<Map<String,Object>> session,int sessionSize) {
        if(mainSessionStore == null) {
            storeContext = new StoreContext();
            storeContext.setSessionSize(sessionSize);
            storeContext.setSessionMemory(session);
            mainSessionStore = this.agentSessionStoreBuilder.build(storeContext);
//            mainSessionStore = sessionStore;
        }
        else if(mainSessionStore instanceof AgentSessionStoreMemory){
            AgentSessionStoreMemory agentSessionStoreMemory = (AgentSessionStoreMemory)mainSessionStore;
            if(agentSessionStoreMemory.getSessionMemory() != null){
                throw new AIRuntimeException("Session memory already exists");
            }
            agentSessionStoreMemory.setSessionMemory(session);
            agentSessionStoreMemory.setSessionSize(sessionSize);
//            mainSessionStore = sessionStore;

        }

        return (T)this;
    }

    public T setSessionSize(int sessionSize) {
        
        if(mainSessionStore == null) {
            if(storeContext == null){
                storeContext = new StoreContext();
            }
            storeContext.setSessionSize(sessionSize);
            mainSessionStore = this.agentSessionStoreBuilder.build(storeContext);
//            mainSessionStore = sessionStore;
        }
        else {
            if(storeContext == null){
                storeContext = new StoreContext();
                
            }
            storeContext.setSessionSize(sessionSize);
//            sessionStore.setSessionSize(sessionSize);
            mainSessionStore.setSessionSize(sessionSize);
        }

        return (T)this;
    }
//    public List<Map<String,Object>> getSessionMemory() {
//        initSessionStore();
//        if(mainSessionStore == null){
//            return null;
//        }
//         
//        return mainSessionStore.getSessionMemory();
//    }

//    public T setSessionStore(AgentSessionStore sessionStore) {
//        this.mainSessionStore = sessionStore;
//        
//        return (T)this;
//    }

    public AgentSessionStore getMainSessionStore() {
        initSessionStore();
        return mainSessionStore;
    }

  
 
    
     
    private void initSessionStore(){
        if(mainSessionStore == null && storeContext != null){
            mainSessionStore = this.agentSessionStoreBuilder.build(storeContext);
//            mainSessionStore = sessionStore;
        }
    }
    public T setMainSessionStore(AgentSessionStore mainSessionStore){
        this.mainSessionStore = mainSessionStore;
        return (T)this;
    }

    public int getSessionSize() {
        initSessionStore();
        if (mainSessionStore != null)
            return mainSessionStore.getSessionSize();
        return 0;
    
    }

    private AgentSessionStore getAgentSessionStore(String agentId){
        AgentSessionStore agentSessionStore = null;
        if(agentId == null){
            agentSessionStore = mainSessionStore;
        }
        else {
            agentSessionStore = mainSessionStore.getSubTaskSessionMemory(agentId);
        }
        return agentSessionStore;
    }
    public T addSessionMessage(Map<String, Object> systemMessage,String prompt, AIAgent aiAgent){
        initSessionStore();
        if(mainSessionStore == null){
            return (T)this;
        }
        String agentId = aiAgent != null ?aiAgent.getAgentId():null;
        AgentSessionStore agentSessionStore = getAgentSessionStore(  agentId);
        if(agentSessionStore == null){
            agentSessionStore = aiAgent.getAgentSessionStore();
        }
        if(agentSessionStore == null){
            return (T)this;
        }
        agentSessionStore.addSessionMessage(systemMessage,  prompt,  agentSessionStore.getAgentId(), agentSessionStore.getParantAgentId());
        
        return (T)this;
    }
    
    public T addSessionMessage(Map<String, Object> message, AIAgent aiAgent){   
        initSessionStore();
        if(mainSessionStore == null){
            return (T)this;
        }
        String agentId = aiAgent != null ?aiAgent.getAgentId():null;
        AgentSessionStore agentSessionStore = getAgentSessionStore(  agentId);
        if(agentSessionStore == null){
            return (T)this;
        }
        agentSessionStore.addSessionMessage(message);         
        return (T)this;
    }
    
    public LastSessionMessage addAgentResultSessionMessage(String message,AIAgent aiAgent){
        initSessionStore();
        if(mainSessionStore == null){
            return null;
        }
        String agentId = aiAgent != null ?aiAgent.getAgentId():null;
        AgentSessionStore agentSessionStore = getAgentSessionStore(  agentId);
        if(agentSessionStore == null){
            return null;
        }
        LastSessionMessage lastSessionMessage = agentSessionStore.addAgentResultSessionMessage(message);
        if(!aiAgent.isDisableGloableStore()) {
            agentSessionStore.setParentAgentLastSessionMessage(lastSessionMessage);
        }
        return lastSessionMessage;
    }
    public Map<String,Object> addAssistantSessionMessage(String message,AIAgent aiAgent){
        initSessionStore();
        if(mainSessionStore == null){
            return null;
        }
        String agentId = aiAgent != null ?aiAgent.getAgentId():null;
        AgentSessionStore agentSessionStore = getAgentSessionStore(  agentId);
        if(agentSessionStore == null){
            return null;
        }
        return agentSessionStore.addAssistantSessionMessage(message);
    }

    public Map<String,Object> addAssistantSessionMessage(ServerEvent serverEvent, AIAgent aiAgent){
        initSessionStore();
        if(mainSessionStore == null){
            return null;
        }
        String agentId = aiAgent != null ?aiAgent.getAgentId():null;
        AgentSessionStore agentSessionStore = getAgentSessionStore(  agentId);
        if(agentSessionStore == null){
            return null;
        }
        return agentSessionStore.addAssistantSessionMessage(serverEvent);
    }

    public Map<String,Object> addAssistantSessionMessage(BaseStreamDataBuilder baseStreamDataBuilder, AIAgent aiAgent){
        initSessionStore();
        if(mainSessionStore == null){
            return null;
        }
        String agentId = aiAgent != null ?aiAgent.getAgentId():null;
        AgentSessionStore agentSessionStore = getAgentSessionStore(  agentId);
        if(agentSessionStore == null){
            return null;
        }
        return agentSessionStore.addAssistantSessionMessage(baseStreamDataBuilder);
    }


 
}
