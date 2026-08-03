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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author biaoping.yin
 * @Date 2026/5/23
 */
public class TraceMessage {
    private Map<String, Object> message;
    private Long startTime;
    private Long endTime;
    private String sessionId;
    /**
     * 创建或者消息所属的agentid,如果节点类型是串行容器智能体节点（sequence）、并行容器智能体节点（parallel），对应创建消息的agentid为subAgentIdBy对应的值
     */
    private String agentId;
    private String parentAgentId;
	
	
	private String groupId;
	private String parentGroupId;
    /**
     * 智能体节点类型：标准化智能体节点（standard）、串行容器智能体节点（sequence）、并行容器智能体节点（parallel）
     */
    private String agentNodeType;
    /**
     * 创建消息的子agentid，节点类型是串行容器智能体节点（sequence）、并行容器智能体节点（parallel）有值
     */
    private String subAgentIdBy;
    private String requestId;
    private String userId;
    private String traceId;
    private Map<String, Object> metaData;

    public Map<String, Object> getMessage() {
        return message;
    }

    public TraceMessage setMessage(Map<String, Object> message) {
        this.message = message;
        return this;
    }

    public Long getStartTime() {
        return startTime;
    }

    public TraceMessage setStartTime(Long startTime) {
        this.startTime = startTime;
        return this;
    }

    public Long getEndTime() {
        return endTime;
    }

    public TraceMessage setEndTime(Long endTime) {
        this.endTime = endTime;
        return this;
    }

    public String getSessionId() {
        return sessionId;
    }

    public TraceMessage setSessionId(String sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    public String getAgentId() {
        return agentId;
    }

    /**
     * 创建或者消息所属的agentid,如果节点类型是串行容器智能体节点（sequence）、并行容器智能体节点（parallel），对应创建消息的agentid为subAgentIdBy对应的值
     * @param agentId
     * @return
     */
    public TraceMessage setAgentId(String agentId) {
        this.agentId = agentId;
        return this;
    }

    public String getParentAgentId() {
        return parentAgentId;
    }

    public TraceMessage setParentAgentId(String parentAgentId) {
        this.parentAgentId = parentAgentId;
        return this;
    }

    public String getRequestId() {
        return requestId;
    }

    public TraceMessage setRequestId(String requestId) {
        this.requestId = requestId;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public TraceMessage setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public String getTraceId() {
        return traceId;
    }

    public TraceMessage setTraceId(String traceId) {
        this.traceId = traceId;
        return this;
    }

    public Map<String, Object> getMetaData() {
        return metaData;
    }

    public TraceMessage setMetaData(Map<String, Object> metaData) {
        this.metaData = metaData;
        return this;
    }
    public TraceMessage put(String key, Object value){
        if(message == null){
            message = new LinkedHashMap<>();
        }
        message.put(key,value);
        return this;
    }

    public String getSubAgentIdBy() {
        return subAgentIdBy;
    }

    /**
     * 创建消息的子agentid，节点类型是串行容器智能体节点（sequence）、并行容器智能体节点（parallel）有值
     * @param subAgentIdBy
     * @return
     */
    public TraceMessage setSubAgentIdBy(String subAgentIdBy) {
        this.subAgentIdBy = subAgentIdBy;
        return this;
    }

    public String getAgentNodeType() {
        return agentNodeType;
    }

    /**
     * 智能体节点类型：标准化智能体节点（standard）、串行容器智能体节点（sequence）、并行容器智能体节点（parallel）
     * @param agentNodeType
     * @return
     */
    public TraceMessage setAgentNodeType(String agentNodeType) {
        this.agentNodeType = agentNodeType;
        return this;
    }
	
	public String getGroupId() {
		return groupId;
	}
	
	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}
	
	public String getParentGroupId() {
		return parentGroupId;
	}
	
	public void setParentGroupId(String parentGroupId) {
		this.parentGroupId = parentGroupId;
	}
}
