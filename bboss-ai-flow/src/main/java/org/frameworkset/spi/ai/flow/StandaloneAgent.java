package org.frameworkset.spi.ai.flow;
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

import org.frameworkset.spi.ai.model.AgentMessage;
import org.frameworkset.spi.ai.model.LastSessionMessage;
import org.frameworkset.spi.ai.store.AgentSessionStore;
import org.frameworkset.spi.ai.tools.ToolsRegist;

/**
 * 独立智能体：不会接受上游消息,也不会向下游推送消息
 * @author biaoping.yin
 * @Date 2026/4/14
 */
public class StandaloneAgent 
        extends AIBaseNodeAgent<StandaloneAgent> {     

    public StandaloneAgent(String prompt,   ToolsRegist toolsRegist, Integer sessionSize) {
        super(prompt,   toolsRegist, sessionSize);
        this.disablePush2ParentLastSubMessage = true;
        this.disableReferenceParentLastSubMessage = true;
    }
 

    public StandaloneAgent(String prompt, ToolsRegist toolsRegist) {
        super(prompt, toolsRegist);
        this.disablePush2ParentLastSubMessage = true;
        this.disableReferenceParentLastSubMessage = true;
    }

    public StandaloneAgent(String prompt, ToolsRegist toolsRegist, int sessionSize) {
        super(prompt, toolsRegist, sessionSize);
        this.disablePush2ParentLastSubMessage = true;
        this.disableReferenceParentLastSubMessage = true;
    }

 
    public StandaloneAgent(String prompt, int sessionSize) {
        super(prompt, sessionSize);
        this.disablePush2ParentLastSubMessage = true;
        this.disableReferenceParentLastSubMessage = true;
    }
    public StandaloneAgent(String prompt) {
        super(prompt);
        this.disablePush2ParentLastSubMessage = true;
        this.disableReferenceParentLastSubMessage = true;
    }

  
    public StandaloneAgent(  ) {
        super(  );
        this.disablePush2ParentLastSubMessage = true;
        this.disableReferenceParentLastSubMessage = true;
    }

    public StandaloneAgent(ToolsRegist mcpToolsRegist) {
        super( mcpToolsRegist);
        this.disablePush2ParentLastSubMessage = true;
        this.disableReferenceParentLastSubMessage = true;
    }

    /**
     * 用户agent,无需追加父智能体中产生的最新的消息作为当前会话的初始消息
     * @param mainSessionStore
     * @param agentMessage
     * @return
     */
    protected LastSessionMessage getLastSubAgentSessionMessage(AgentSessionStore mainSessionStore, AgentMessage agentMessage){
        return null;
    }
 
}
