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

import org.frameworkset.spi.ai.model.LastSessionMessage;
import org.frameworkset.spi.ai.model.ServerEvent;
import org.frameworkset.spi.ai.util.BaseStreamDataBuilder;

import java.util.List;
import java.util.Map;

/**
 * @author biaoping.yin
 * @Date 2026/1/4
 */
public interface AgentSessionStore<T extends AgentSessionStore> {
    void init();
    String genSubAgentId();
    void addSubTaskSessionMemory(String agentId,AgentSessionStore subTaskSession);
    String getAgentId();
    void saveLastSessionMessage(LastSessionMessage lastSessionMessage,String refAgentId);
    LastSessionMessage getLastSubAgentSessionMessage(String prompt, String agentId);
    AgentSessionStore getSubTaskSessionMemory(String agentId) ;

    /**
     * 根据prompt和agentId加载记忆消息，如果未加载记忆消息，则进行加载
     * @param prompt
     * @param agentId
     * @return
     */
    boolean loadSessionMemory(String prompt,String agentId);
    
    String getParantAgentId();

    T setSessionSize(int sessionSize) ;
    T setAgentId(String agentId) ;
    int getSessionSize() ;
    String getSessionId(); 

    void addSessionMessage(Map<String, Object> message);

    LastSessionMessage addAgentResultSessionMessage(String message);

    LastSessionMessage addAgentResultSessionMessage(Map<String, Object> message,String agentId,String parentAgentId);
    void appendSessionMessageFromParent(Map<String, Object> message);
    void addSessionMessage( Map<String, Object> systemMessage,String prompt,String agentId,String parentAgentId);
   
    Map<String, Object> addAssistantSessionMessage(String message);

    Map<String, Object> addAssistantSessionMessage(ServerEvent serverEvent);
    Map<String, Object> addAssistantSessionMessage(BaseStreamDataBuilder baseStreamDataBuilder);


    List<Map<String, Object>> getSessionMemory();

    List<Map<String, Object>>  getAgentSessionMessage(LastSessionMessage lastSubAgentSessionMessage,String agentId,int agentSessionSize);

    LastSessionMessage persistentSessionMessage(Map<String, Object> message, 
                                                String agentId,String parentAgentId,String marks,String metadata,String agentResultMessage);
    AgentSessionStore getMainAgentSessionStore() ;

    T setMainAgentSessionStore(AgentSessionStore mainAgentSessionStore) ;

    void setParantAgentLastSessionMessage(LastSessionMessage lastSubAgentSessionMessage);

    void setSubAgentLastSessionMessage(LastSessionMessage lastSubAgentSessionMessage);
}
