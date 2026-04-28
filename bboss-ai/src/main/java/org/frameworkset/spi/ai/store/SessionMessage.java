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

import com.frameworkset.orm.annotation.Column;

import java.util.Date;
import java.util.Map;

/**
 * @author biaoping.yin
 * @Date 2026/4/3
 */
public class SessionMessage {
    /**
     * 智能体输出消息
     */
    public static final String MESSAGE_TYPE_AGENTRESULTMESSAGE = "1";
    /**
     * 智能体输入消息
     */
    public static final String MESSAGE_TYPE_USER_MESSAGE = "2";

    /**
     * 智能体过程消息
     */
    public static final String MESSAGE_TYPE_MIDDLE_MESSAGE = "0";
    
    private String msgId;
    /**
     * 前端用户请求id，每次请求生成一个唯一id
     */
    private String requestId;
    private Date createTime;
    private String sessionId;
    private String agentId;


    private String marks;
    private String metadata;

    private String parentAgentId;
    private String agentResultMessage = MESSAGE_TYPE_MIDDLE_MESSAGE;
    private int seqNo;
    
    @Column(type = "clob",editor = "org.frameworkset.spi.ai.store.db.SessionMessageEditor")
    private Map<String, Object> message;
    private String role;

    public Map<String, Object> getMessage() {
        return message;
    }

    public void setMessage(Map<String, Object> message) {
        this.message = message;
    }

    public String getParentAgentId() {
        return parentAgentId;
    }

    public void setParentAgentId(String parentAgentId) {
        this.parentAgentId = parentAgentId;
    }

    public String getAgentResultMessage() {
        return agentResultMessage;
    }

    public void setAgentResultMessage(String agentResultMessage) {
        this.agentResultMessage = agentResultMessage;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public int getSeqNo() {
        return seqNo;
    }

    public void setSeqNo(int seqNo) {
        this.seqNo = seqNo;
    }

    public String getAgentId() {
        return agentId;
    }
    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }


    public String getMarks() {
        return marks;
    }

    public void setMarks(String marks) {
        this.marks = marks;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}
