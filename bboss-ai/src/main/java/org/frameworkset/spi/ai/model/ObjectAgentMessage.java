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

import org.frameworkset.spi.ai.adapter.AgentAdapter;
import org.frameworkset.spi.ai.util.StreamDataBuilder;
import org.frameworkset.spi.reactor.SSEHeaderSetFunction;
import org.frameworkset.spi.remote.http.ClientConfiguration;

/**
 * @author biaoping.yin
 * @Date 2026/1/27
 */
public class ObjectAgentMessage extends AgentMessage<ObjectAgentMessage>{
    private Object agentMessage;
    public ObjectAgentMessage(Object agentMessage){
        this.agentMessage = agentMessage;
    }
    @Override
    public ChatObject buildChatObject(ClientConfiguration clientConfiguration, AgentAdapter agentAdapter) {
        ChatObject chatObject = new ChatObject();
        SSEHeaderSetFunction sseHeaderSetFunction = null;
        Boolean stream = false;
        String aiChatRequestType = null;
        StreamDataBuilder streamDataBuilder = null;   
        chatObject.setSseHeaderSetFunction(sseHeaderSetFunction);
        chatObject.setMessage(agentMessage);
        chatObject.setStream(stream);
        chatObject.setAiChatRequestType(aiChatRequestType);
        chatObject.setStreamDataBuilder(streamDataBuilder);
        return chatObject;
    }
}
