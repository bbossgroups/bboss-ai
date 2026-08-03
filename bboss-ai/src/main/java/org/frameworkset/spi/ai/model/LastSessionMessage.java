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

import java.util.Map;

/**
 * @author biaoping.yin
 * @Date 2026/4/10
 */
public class LastSessionMessage {
    private Map<String,Object> lastSessionMessage;
	private String groupId;
	private String parentGroupId;
    /**会话id*/
    private String sessionId;
    /**消息id*/
    private String msgId;
    /**
     * 前端用户请求id，每次请求生成一个
     */
    protected String requestId; 
    /**消息对应智能体id*/
    private String msgAgentId;
    private String msgParentAgentId;    
    private long elapsed;
    
    /**
     * 是不是最新的消息，如果是，则需要记录引用关系到数据库
     * 当智能体第一次初始化加载历史记录时，需要从引用关系表中获取对应的消息
     * true 最新消息
     * false 不是最新消息
     */
    private boolean freshMessage;



    private TokenMetrics tokenMetrics;

    /**
     * 智能体节点类型：标准化智能体节点（standard）、串行容器智能体节点（sequence）、并行容器智能体节点（parallel）
     */
    private String agentNodeType;
    /**
     * 创建消息的子agentid，节点类型是串行容器智能体节点（sequence）、并行容器智能体节点（parallel）有值
     */
    private String subAgentIdBy;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getData(){
        if(lastSessionMessage != null){
            return (String)lastSessionMessage.get("content");
        }
        return null;
    }

    public Map<String, Object> getLastSessionMessage() {
        
        return lastSessionMessage;
    }

    public void setLastSessionMessage(Map<String, Object> lastSessionMessage) {
        this.lastSessionMessage = lastSessionMessage;
    }

    public boolean isFreshMessage() {
        return freshMessage;
    }

    public void setFreshMessage(boolean freshMessage) {
        this.freshMessage = freshMessage;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public String getMsgAgentId() {
        return msgAgentId;
    }

    public void setMsgAgentId(String msgAgentId) {
        this.msgAgentId = msgAgentId;
    }

    public String getMsgParentAgentId() {
        return msgParentAgentId;
    }

    public void setMsgParentAgentId(String msgParentAgentId) {
        this.msgParentAgentId = msgParentAgentId;
    }
    public TokenMetrics getTokenMetrics() {
        return tokenMetrics;
    }

    public void setTokenMetrics(TokenMetrics tokenMetrics) {
        this.tokenMetrics = tokenMetrics;
    }

    public long getElapsed() {
        return elapsed;
    }

    public void setElapsed(long elapsed) {
        this.elapsed = elapsed;
    }

    public String getAgentNodeType() {
        return agentNodeType;
    }

    public void setAgentNodeType(String agentNodeType) {
        this.agentNodeType = agentNodeType;
    }

    public String getSubAgentIdBy() {
        return subAgentIdBy;
    }

    public void setSubAgentIdBy(String subAgentIdBy) {
        this.subAgentIdBy = subAgentIdBy;
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
