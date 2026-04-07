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

import java.util.List;
import java.util.Map;

/**
 * @author biaoping.yin
 * @Date 2026/1/4
 */
public class AgentSessionStoreMemory<T extends AgentSessionStoreMemory> extends BaseAgentSessionStore<T>{

    
    public AgentSessionStoreMemory(List<Map<String, Object>> sessionMemory) {
        super(sessionMemory);
    }

    public AgentSessionStoreMemory(List<Map<String, Object>> sessionMemory, int sessionSize) {
        super(sessionMemory, sessionSize);
    }

    public AgentSessionStoreMemory(int sessionSize) {
        super(sessionSize);
    }

    public AgentSessionStoreMemory(AgentSessionStore parentAgentSessionStore,int sessionSize) {
        super(parentAgentSessionStore,sessionSize);
    }

    

    public AgentSessionStoreMemory() {
    }

    public AgentSessionStoreMemory(String sessionId, String userId, String agentId) {
        super(sessionId, userId, agentId );
    }

    public AgentSessionStoreMemory(StoreContext storeContext) {
        super(storeContext);
    }

    @Override
    public void addSessionMessage(Map<String, Object> systemMessage,String prompt,String agentId) {
        super.addSessionMessage(systemMessage);
    }

    @Override
    public List<Map<String, Object>> getAgentSessionMessage(Map<String, Object> lastMessage, String agentId, int agentSessionSize) {
        return null;
    }
}
