package org.frameworkset.spi.ai.callback;
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

import org.frameworkset.spi.ai.model.ServerEvent;
import reactor.core.publisher.FluxSink;

import java.util.Map;

/**
 * 智能体chat或者streamchat时，会创建一个ChatContext对象，用于保存会话级别的信息
 * @author biaoping.yin
 * @Date 2026/5/12
 */
public class ChatContext {
    /**
     * 大模型检索匹配工具阶段、识别和提取工具参数
     */
    public static final int TOOL_CALL_STAGE_SEARCH_TOOL = 1;

    /**
     * 大模型执行工具阶段
     */
    public static final int TOOL_CALL_STAGE_EXECUTE_TOOL = 2;


    /**
     * 处理工具调用响应阶段
     */
    public static final int TOOL_CALL_STAGE_HANDLE_TOOL_RESPONSE = 3;
    private ChatStreamCallback chatStreamCallback;
    /**
     * 会话是否使用工具调用
     */
    private boolean chatWithToolcall;



    /**
     * 智能体会话级别控制是否开启流式返回
     */
    private Boolean streamable;

    /**
     * 智能体会话级别控制是否开启思考过程返回
     */
    private Boolean thinking;
    private Map<String,Object> contextData;


    /**
     * 如果会话使用工具调用chatWithToolcall为true，则用toolCallStage标记工具调用阶段，
     * 默认为TOOL_CALL_STAGE_SEARCH_TOOL阶段
     */
    private int toolCallStage = TOOL_CALL_STAGE_SEARCH_TOOL;
    private Object lock = new Object();
    private FluxSink<ServerEvent> agentSink;
    /**
     * 保存计算后的提示词，避免重复计算
     */
    private String evaledPrompt;

    public void setChatStreamCallback(ChatStreamCallback chatStreamCallback) {
        this.chatStreamCallback = chatStreamCallback;
    }

    public ChatStreamCallback getChatStreamCallback() {
        return chatStreamCallback;
    }
    
    public String evalPrompt(String prompt){
        if(evaledPrompt != null){
            return evaledPrompt;
        }
        if(chatStreamCallback != null) {
            evaledPrompt = chatStreamCallback.evalPrompt(prompt);
        }
        else {
            evaledPrompt = prompt;
        }
        return evaledPrompt;
    }

    public boolean isChatWithToolcall() {
        return chatWithToolcall;
    }

    public void setChatWithToolcall(boolean chatWithToolcall) {
        this.chatWithToolcall = chatWithToolcall;
    }
    public int getToolCallStage() {
        return toolCallStage;
    }

    public void setToolCallStage(int toolCallStage) {
        this.toolCallStage = toolCallStage;
    }

    public FluxSink<ServerEvent> getAgentSink() {
        return agentSink;
    }

    public void setAgentSink(FluxSink<ServerEvent> agentSink) {
        if(this.agentSink != null){
            return;
        }
        synchronized (lock) {
            if(this.agentSink != null){
                return;
            }
            this.agentSink = agentSink;
        }
    }

    public Boolean getStreamable() {
        return streamable;
    }

    public void setStreamable(Boolean streamable) {
        this.streamable = streamable;
    }

    public Boolean getThinking() {
        return thinking;
    }

    public void setThinking(Boolean thinking) {
        this.thinking = thinking;
    }
    
    public ChatContext addContextData(String key,Object value){
        if(contextData == null){
            contextData = new java.util.LinkedHashMap<>();
        }
        contextData.put(key,value);
        return this;
    }
    public Object getContextData(String key){
        if(contextData == null){
            return null;
        }
        return contextData.get(key);
    }

    public ChatContext setContextData(Map<String, Object> contextData) {
        this.contextData = contextData;
        return this;
    }
}
