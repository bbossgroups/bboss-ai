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

import org.frameworkset.spi.ai.util.MessageBuilder;

import java.util.List;
import java.util.Map;

/**
 * @author biaoping.yin
 * @Date 2026/1/4
 */
public class SessionAgentMessage<T extends SessionAgentMessage> extends AgentMessage<T> {
    /** 使用静态变量存储会话记忆（实际项目中建议使用缓存或数据库）*/
    private List<Map<String, Object>> sessionMemory;

    /**
     * 会话窗口大小，默认20
     */
    private int sessionSize = 20;

    public T setSessionMemory(List<Map<String, Object>> sessionMemory) {
        this.sessionMemory = sessionMemory;
        return (T)this;
    }

    public List<Map<String, Object>> getSessionMemory() {
        return sessionMemory;
    }


    public T setSessionSize(int sessionSize) {
        this.sessionSize = sessionSize;
        return (T)this;
    }

    public int getSessionSize() {
        return sessionSize;
    }
    public T addSessionMessage(Map<String, Object> message){        
        if(sessionMemory == null){
            return (T)this;
        }
        sessionMemory.add(message);
        if(sessionMemory.size() > sessionSize){
            sessionMemory.remove(0);
        }
        return (T)this;
    }
    public T addAssistantSessionMessage(String message){
        if(sessionMemory == null){
            return (T)this;
        }
        Map<String, Object> assistantMessage = MessageBuilder.buildAssistantMessage(message);
        return addSessionMessage(assistantMessage);
    }

    public T addAssistantSessionMessage(ServerEvent serverEvent){
        if(sessionMemory == null){
            return (T)this;
        }
        Map<String, Object> assistantMessage = MessageBuilder.buildAssistantMessage(serverEvent);

        return addSessionMessage(assistantMessage);
    }

    public T addAssistantSessionMessage(StreamData streamData){
        if(sessionMemory == null){
            return (T)this;
        }
        Map<String, Object> assistantMessage = MessageBuilder.buildAssistantMessage(streamData);

        return addSessionMessage(assistantMessage);
    }



    @Deprecated
    /**
     * 添加会话消息
     * @param message
     * @return
     * @deprecated 请使用addAssistantSessionMessage方法
     */
    public T addSessionMessage(String message){
         
        return addAssistantSessionMessage(  message);
    }
}
