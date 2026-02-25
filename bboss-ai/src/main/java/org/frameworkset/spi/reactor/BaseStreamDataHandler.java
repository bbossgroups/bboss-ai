package org.frameworkset.spi.reactor;
/**
 * Copyright 2025 bboss
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

import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.frameworkset.spi.ai.adapter.AgentAdapter;
import org.frameworkset.spi.ai.model.ChatAgentMessage;
import org.frameworkset.spi.ai.model.ChatObject;
import org.frameworkset.spi.ai.model.ServerEvent;
import org.frameworkset.spi.ai.model.ToolAgentMessage;
import org.frameworkset.spi.ai.util.AIAgentUtil;
import org.frameworkset.spi.ai.util.BaseStreamDataBuilder;
import org.frameworkset.spi.ai.util.StreamDataBuilder;
import org.frameworkset.spi.remote.http.ClientConfiguration;
import reactor.core.publisher.FluxSink;

import java.util.Map;

/**
 * @author biaoping.yin
 * @Date 2025/10/19
 */
public abstract class BaseStreamDataHandler<T> implements StreamDataHandler<T> {
    protected HttpUriRequestBase httpUriRequestBase;
    protected AgentAdapter agentAdapter;
    protected boolean stream;
   
 
    protected ChatObject chatObject;
    public boolean isStream() {
        return stream;
    }

    public void setStream(boolean stream) {
        this.stream = stream;
    }

    public void setHttpUriRequestBase(HttpUriRequestBase httpUriRequestBase) {
        this.httpUriRequestBase = httpUriRequestBase;
    }
    public HttpUriRequestBase getHttpUriRequestBase() {
        return httpUriRequestBase;
    }
     
    public void streamChatCompletionEvent(ClientConfiguration clientConfiguration, ChatObject chatObject, BaseStreamDataBuilder baseStreamDataBuilder,FluxSink<T> sink) {
        ChatAgentMessage _chatMessage = (ChatAgentMessage) chatObject.getAgentMessage();
        _chatMessage.addAssistantSessionMessage(baseStreamDataBuilder.getToolCallsStreamData());
        ToolAgentMessage toolAgentMessage = new ToolAgentMessage(_chatMessage, baseStreamDataBuilder.getFunctionTools());
        AIAgentUtil.streamChatCompletionEvent(clientConfiguration, toolAgentMessage,sink);
    }


//    public abstract void streamChatCompletionEvent(ClientConfiguration clientConfiguration, ChatObject chatObject, BaseStreamDataBuilder baseStreamDataBuilder, FluxSink<T> sink);
//    
     

    public void setAgentAdapter(AgentAdapter agentAdapter) {
        this.agentAdapter = agentAdapter;
    }

    @Override
    public AgentAdapter getAgentAdapter() {
        return agentAdapter;
    }

    @Override
    public ChatObject getChatObject() {
        return chatObject;
    }

    public void setChatObject(ChatObject chatObject) {
        this.chatObject = chatObject;
        
    }

    public StreamDataBuilder getStreamDataBuilder() {
        return this.chatObject.getStreamDataBuilder();
    }

    public String getDoneData() {
        return chatObject.getDoneData(this.agentAdapter);
    }
}
