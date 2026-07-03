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
import org.frameworkset.spi.ai.model.*;
import org.frameworkset.spi.ai.util.BaseStreamDataBuilder;

import java.util.List;
import java.util.Map;

/**
 * @author biaoping.yin
 * @Date 2026/1/4
 */
public interface AgentSessionStore<T extends AgentSessionStore> {
    void init();
    void addSubTaskSessionMemory(String agentId,AgentSessionStore subTaskSession);
    StoreContext getStoreContext();
    String getAgentId();
    String genSubAgentId(AgentIdAssign agentIdAssign);
    void saveLastSessionMessage(LastSessionMessage lastSessionMessage,String refAgentId);
    LastSessionMessage getLastSubAgentSessionMessage();
    List<LastSessionMessage> getLastSubAgentSessionMessages();
    AgentSessionStore getSubTaskSessionMemory(String agentId) ;
    
    /**
     * 根据prompt和agentId加载记忆消息，如果未加载记忆消息，则进行加载
     * @param prompt
     * @param agentId
     * @return
     */
    boolean loadSessionMemory(String prompt,String agentId);

    /**
     * 根据prompt和agentId加载记忆消息，如果未加载记忆消息，则进行加载
     * 如果会话不存在 则创建会话
     * 如果是新建的会话，则返回true，否则返回false
     * @param prompt
     * @param domain 
     * @param agentId
     * @return
     */
    boolean loadSessionMemory(String prompt,String domain,String agentId);
    
    String getParantAgentId();
    AIAgent getAiAgent();
    T setAIAgent(AIAgent aiAgent);
    T setSessionSize(int sessionSize) ;
    T setAgentId(String agentId) ;
    int getSessionSize() ;
    String getSessionId(); 

    void addSessionMessage(PersistentMessage message);

    LastSessionMessage addAgentResultSessionMessage(AgentResultSessionMessageContext agentResultSessionMessageContext,String persistentMessage);
    LastSessionMessage addAgentResultSessionMessage(ServerEvent serverEvent);

    LastSessionMessage addAgentResultSessionMessage(Map<String, Object> message,String agentId,String parentAgentId);
    void appendSessionMessageFromParent(Map<String,Object> message);
    void addSessionMessage( Map<String,Object> systemMessage,String prompt,String agentId,String parentAgentId,String agentNodeType);
   
//    Map<String, Object> addAssistantSessionMessage(String message);

    Map<String, Object> addAssistantSessionMessage(ServerEvent serverEvent);
    Map<String, Object> addAssistantSessionMessage(BaseStreamDataBuilder baseStreamDataBuilder);


    List<Map<String, Object>> getSessionMemory();

    List<Map<String, Object>>  getAgentSessionMessage(LastSessionMessage lastSubAgentSessionMessage,String agentId,int agentSessionSize);

    void recordTraceMessage(TraceMessage traceMessage);
    void recordTraceMessage(TraceMessage traceMessage,TokenMetrics tokenMetrics);
    
    LastSessionMessage persistentSessionMessage(PersistentMessage persistentMessage,//Map<String, Object> message,
                                                String agentId, String parentAgentId,String agentNodeType,String subAgentIdBy, String marks, String metadata, String messageType);
            
            //, TokenMetrics tokenMetrics);
    AgentSessionStore getMainAgentSessionStore() ;

    T setMainAgentSessionStore(AgentSessionStore mainAgentSessionStore) ;

    void setParentAgentLastSessionMessage(LastSessionMessage lastSubAgentSessionMessage);

    void setSubAgentLastSessionMessage(LastSessionMessage lastSubAgentSessionMessage);

    void cleanLastSessionMessages();

    void removeSession(String sessionId);

    String genSubAgentName(String agentId);
}
