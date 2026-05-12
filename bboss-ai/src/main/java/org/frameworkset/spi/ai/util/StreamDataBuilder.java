package org.frameworkset.spi.ai.util;
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
import org.frameworkset.spi.ai.model.ChatObject;
import org.frameworkset.spi.ai.model.ServerEvent;
import org.frameworkset.spi.ai.model.StreamData;
import org.frameworkset.spi.ai.model.TokenMetrics;

/**
 * @author biaoping.yin
 * @Date 2026/1/12
 */
public interface StreamDataBuilder {
    StreamData build(AgentAdapter agentAdapter , String line);
    default StreamData buildWrapped(AgentAdapter agentAdapter , String line){
        return build(agentAdapter,line);
    }

    boolean isDone(AgentAdapter agentAdapter,String data);
    String getDoneData(AgentAdapter agentAdapter);
    void handleServerEvent(AgentAdapter agentAdapter,ServerEvent serverEvent);
    ChatObject getChatObject();



    String addAgentResultSessionMessage();
    TokenMetrics getTokenMetrics();
    void setTokenMetrics(TokenMetrics tokenMetrics);
}
