package org.frameworkset.spi.ai;
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
 * 智能体:用户智能体，
 * @author biaoping.yin
 * @Date 2026/1/4
 */
public class UserAgent<T extends UserAgent> extends AIAgent<T> {

    public UserAgent() {
    }

    public UserAgent(String prompt, String type, ToolsRegist toolsRegist, Integer sessionSize) {
        super(prompt, type, toolsRegist, sessionSize);
    }

    public UserAgent(String prompt, String type, ToolsRegist toolsRegist) {
        super(prompt, type, toolsRegist);
    }

    public UserAgent(String prompt, ToolsRegist toolsRegist) {
        super(prompt, toolsRegist);
    }

    public UserAgent(String prompt, ToolsRegist toolsRegist, int sessionSize) {
        super(prompt, toolsRegist, sessionSize);
    }

    public UserAgent(String prompt, String type) {
        super(prompt, type);
    }

    public UserAgent(String prompt) {
        super(prompt);
    }

    public UserAgent(String prompt, int sessionSize) {
        super(prompt, sessionSize);
    }

    /**
     * 用户agent,无需追加父智能体中产生的最新的消息作为当前会话的初始消息
     * @param mainSessionStore
     * @param agentMessage
     * @return
     */
    protected LastSessionMessage getLastSubAgentSessionMessage(AgentSessionStore mainSessionStore,AgentMessage agentMessage){
       return null;
    }
   
}
