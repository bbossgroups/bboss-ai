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

import com.frameworkset.util.JsonUtil;
import org.frameworkset.spi.ai.adapter.AgentAdapter;
import org.frameworkset.spi.ai.util.MessageBuilder;
import org.frameworkset.spi.ai.util.StreamDataBuilder;
import org.frameworkset.spi.reactor.SSEHeaderSetFunction;
import org.frameworkset.spi.remote.http.ClientConfiguration;

import java.util.List;
import java.util.Map;

/**
 * @author biaoping.yin
 * @Date 2026/2/21
 */
public class ToolAgentMessage extends ChatAgentMessage{
    private ChatAgentMessage chatAgentMessage;
    private List<FunctionTool> functionTools;
    public ToolAgentMessage(ChatAgentMessage chatAgentMessage,List<FunctionTool> functionTools) {
        this.chatAgentMessage = chatAgentMessage;
        this.functionTools = functionTools;
    }

    public List<FunctionTool> getFunctionTools() {
        return functionTools;
    }

    public FunctionTool getFunctionTool() {
        return functionTools.get(0);
    }

    public ChatAgentMessage getChatAgentMessage() {
        return chatAgentMessage;
    }

    @Override
    public FunctionCall getFunctionCall(String toolName) {
        return chatAgentMessage.getFunctionCall(toolName);
    }

    @Override
    protected Map buildOpenAIRequestMap(AgentAdapter agentAdapter){
        Map parameters = agentAdapter.buildOpenAIRequestMapWithTool(this);
        return parameters;
    }
    

 

    @Override
    public String getModel() {
        return chatAgentMessage.getModel();
    }

    @Override
    public Map getParameters() {
        return chatAgentMessage.getParameters();
    }

    @Override
    public Boolean getStream() {
        return chatAgentMessage.getStream();
    }

    @Override
    public Double getTemperature() {
        return chatAgentMessage.getTemperature();
    }

    @Override
    public Integer getMaxTokens() {
        return chatAgentMessage.getMaxTokens();
    }

    @Override
    public List<Map<String, Object>> getSessionMemory() {
        return chatAgentMessage.getSessionMemory();
    }

    @Override
    public int getSessionSize() {
        return chatAgentMessage.getSessionSize();
    }
    @Override
    public String getChatCompletionsUrl() {
        return chatAgentMessage.getChatCompletionsUrl();
    }

    @Override
    public ChatAgentMessage addSessionMessage(Map<String, Object> message) {
          chatAgentMessage.addSessionMessage(message);
          return this;
    }

    /**
     * 消息级别模型类型，优先级高于模型服务级别配置，取值参考：
     * public class AIConstants {
     * public static final String AI_MODEL_TYPE_QWEN = "qwen";
     * public static final String AI_MODEL_TYPE_DOUBAO = "doubao";
     * public static final String AI_MODEL_TYPE_DEEPSEEK = "deepseek";
     * public static final String AI_MODEL_TYPE_KIMI = "kimi";
     * public static final String AI_MODEL_TYPE_NONE = "none";
     * <p>
     * }
     */
    @Override
    public List<FunctionToolDefine> getTools() {
        return chatAgentMessage.getTools();
    }

    @Override
    public void init() {
//        super.init();
    }
}
