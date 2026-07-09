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
 * @Date 2026/4/7
 */
public class StoreContext {
    public static final String STORE_TYPE_DB = "db";
    public static final String STORE_TYPE_MEMORY = "memory";
	private String clickhouseCluster;
    private AgentSessionStore mainSessionStore;
    private AgentMessageTypeConvertor agentMessageTypeConvertor;
    /**
     * 会话id
     */
    private String sessionId;

    /**
     * 是否重置session
     */
    private boolean resetSession;
    



    /**
     * 请求id
     */
    private String requestId;
    private String userId;
    private String agentId;
    private String dataSource;
    
    private String domain;
    


    private String traceId;
    /**
     * 会话基本信息存储表名称
     */
    private String sessionTableName = "agent_session";

    /**
     * 会话消息记录存储表名称
     */
    private String sessionMessageTableName = "agent_session_message";
    private List<Map<String,Object>> sessionMemory;
    private int sessionSize = 20;
    public int getSessionSize() {
        return sessionSize;
    }
    public StoreContext setSessionSize(int sessionSize) {
        this.sessionSize = sessionSize;
        return this;
    }
    private String storeType = STORE_TYPE_MEMORY;

    public String getSessionId() {
        return sessionId;
    }

    public StoreContext setSessionId(String sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public StoreContext setAgentMessageTypeConvertor(AgentMessageTypeConvertor agentMessageTypeConvertor) {
        this.agentMessageTypeConvertor = agentMessageTypeConvertor;
        return this;
    }

    public AgentMessageTypeConvertor getAgentMessageTypeConvertor() {
        return agentMessageTypeConvertor;
    }

    public StoreContext setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public String getAgentId() {
        return agentId;
    }

    public StoreContext setAgentId(String agentId) {
        this.agentId = agentId;
        return this;
    }

    public String getDataSource() {
        return dataSource;
    }

    public StoreContext setDataSource(String dataSource) {
        this.dataSource = dataSource;
        return this;
    }
    
    public List<Map<String,Object>> getSessionMemory() {
        return sessionMemory;
    }
    public StoreContext setSessionMemory(List<Map<String,Object>> sessionMemory) {
        this.sessionMemory = sessionMemory;
        return this;
    }
    public String getStoreType() {
        return storeType;
    }
    public StoreContext setStoreType(String storeType) {
        this.storeType = storeType;
        return this;
    }
    public String getSessionTableName() {
        return sessionTableName;
    }
    public StoreContext setSessionTableName(String sessionTableName) {
        this.sessionTableName = sessionTableName;
        return this;
    }
    public String getSessionMessageTableName() {
        return sessionMessageTableName;
    }
    public StoreContext setSessionMessageTableName(String sessionMessageTableName) {
        this.sessionMessageTableName = sessionMessageTableName;
        return this;
    }
    public String getRequestId() {
        return requestId;
    }

    public StoreContext setRequestId(String requestId) {
        this.requestId = requestId;
        return this;
    }
    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
    public boolean isResetSession() {
        return resetSession;
    }
    public StoreContext setResetSession(boolean resetSession) {
        this.resetSession = resetSession;
        return this;
    }

    public AgentSessionStore getMainSessionStore() {
        return mainSessionStore;
    }

    public void setMainSessionStore(AgentSessionStore mainSessionStore) {
        this.mainSessionStore = mainSessionStore;
    }


    public String getDomain() {
        return domain;
    }

    public StoreContext setDomain(String domain) {
        this.domain = domain;
        return this;
    }
	
	public String getClickhouseCluster() {
		return clickhouseCluster;
	}
	
	public StoreContext setClickhouseCluster(String clickhouseCluster) {
		this.clickhouseCluster = clickhouseCluster;
		return this;
	}
}
