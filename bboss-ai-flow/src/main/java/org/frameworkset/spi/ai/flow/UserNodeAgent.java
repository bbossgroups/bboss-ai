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
 * 用户智能体：不会介绍上游消息
 * @author biaoping.yin
 * @Date 2026/4/14
 */
public class UserNodeAgent 
        extends AIBaseNodeAgent<UserNodeAgent> {     

    public UserNodeAgent(String prompt, String type, ToolsRegist toolsRegist, Integer sessionSize) {
        super(prompt, type, toolsRegist, sessionSize);
    }

    public UserNodeAgent(String prompt, String type, ToolsRegist toolsRegist) {
        super(prompt, type, toolsRegist);
    }

    public UserNodeAgent(String prompt, ToolsRegist toolsRegist) {
        super(prompt, toolsRegist);
    }

    public UserNodeAgent(String prompt, ToolsRegist toolsRegist, int sessionSize) {
        super(prompt, toolsRegist, sessionSize);
    }

    public UserNodeAgent(String prompt, String type) {
        super(prompt, type);
    }
    public UserNodeAgent(String prompt, int sessionSize) {
        super(prompt, sessionSize);
    }
    public UserNodeAgent(String prompt) {
        super(prompt);
    }

  
    public UserNodeAgent(  ) {
        super(  );
    }

    public UserNodeAgent(ToolsRegist mcpToolsRegist) {
        super( mcpToolsRegist);
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
