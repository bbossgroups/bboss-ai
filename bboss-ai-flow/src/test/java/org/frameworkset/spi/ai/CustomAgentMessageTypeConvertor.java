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

import org.frameworkset.spi.ai.store.AgentMessageTypeConvertor;

/**
 * @author biaoping.yin
 * @Date 2026/6/15
 */
public class CustomAgentMessageTypeConvertor extends AgentMessageTypeConvertor {
    /**
     * 将角色转换为消息类型messageType，对应agent_session_message表中的messageType字段    
     *
     * @param role
     * @return
     */
    @Override
    public String convertMessageType(String role) {
        if("custom".equals(role)){
            return "101";
        }
        return super.convertMessageType(role);
    }
}
