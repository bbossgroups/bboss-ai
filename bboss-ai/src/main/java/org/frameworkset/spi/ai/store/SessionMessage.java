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
import org.frameworkset.spi.ai.model.TokenMetrics;

import java.util.Date;
import java.util.Map;

/**
 * @author biaoping.yin
 * @Date 2026/4/3
 */
public class SessionMessage {
    /**
     * 智能体用户输入消息:包括用户输入的原始问题、用户上传文件、用户图片描述等
     */
    public static final String MESSAGE_TYPE_USERINPUTMESSAGE = "8";
    /**
     * 智能体输出消息:ASSISTANT中的一种
     */
    public static final String MESSAGE_TYPE_AGENTRESULTMESSAGE = "1";
    /**
     * 智能体用户输入消息：提交给大模型或者其他多模态模型
     */
    public static final String MESSAGE_TYPE_USER_MESSAGE = "2";

    /**
     * 智能体辅助消息
     */
    public static final String MESSAGE_TYPE_ASSISTANT_MESSAGE = "0";


    /**
     * 智能体系统消息
     */
    public static final String MESSAGE_TYPE_SYSTEM_MESSAGE = "3";



    /**
     * 智能体跟踪消息
     */
    public static final String MESSAGE_TYPE_TRACE_MESSAGE = "5";

    /**
     * 智能体RAG知识消息
     */
    public static final String MESSAGE_TYPE_RAG_MESSAGE = "6";

    /**
     * 智能体拒答消息
     */
    public static final String MESSAGE_TYPE_REFUSE_MESSAGE = "7";


    /**
     * 智能体用户输入消息:包括用户输入的原始问题、用户上传文件、用户图片描述等
     */
    public static final String MESSAGE_TYPE_USERINPUTMESSAGE_NAME = "userinput";
    /**
     * 智能体输出消息:ASSISTANT中的一种
     */
    public static final String MESSAGE_TYPE_AGENTRESULTMESSAGE_NAME = "agentresult";
    /**
     * 智能体用户输入消息
     */
    public static final String MESSAGE_TYPE_USER_MESSAGE_NAME = "user";

    /**
     * 智能体辅助消息
     */
    public static final String MESSAGE_TYPE_ASSISTANT_MESSAGE_NAME = "assistant";


    /**
     * 智能体系统消息
     */
    public static final String MESSAGE_TYPE_SYSTEM_MESSAGE_NAME = "system";



    /**
     * 智能体跟踪消息
     */
    public static final String MESSAGE_TYPE_TRACE_MESSAGE_NAME = "trace";

    /**
     * 智能体RAG知识消息
     */
    public static final String MESSAGE_TYPE_RAG_MESSAGE_NAME = "rag";

    /**
     * 智能体拒答消息
     */
    public static final String MESSAGE_TYPE_REFUSE_MESSAGE_NAME = "refuse";
    
    private String msgId;



    private long elapsed;
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
    private String messageType = MESSAGE_TYPE_ASSISTANT_MESSAGE;
    private int seqNo;



    private String traceId;
    /**
     * 记录用户输入问题
     */
    private String inputQuery;
    
    @Column(type = "clob",editor = "org.frameworkset.spi.ai.store.db.SessionMessageEditor")
    private Map<String, Object> message;
    @Column(type = "clob",editor = "org.frameworkset.spi.ai.store.db.TokenMetricsEditor")
    private TokenMetrics tokenMetrics;
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

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
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

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
}
