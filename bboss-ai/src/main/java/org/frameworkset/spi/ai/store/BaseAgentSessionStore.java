package org.frameworkset.spi.ai.store;
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
import org.frameworkset.spi.ai.util.BaseStreamDataBuilder;
import org.frameworkset.spi.ai.util.MessageBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author biaoping.yin
 * @Date 2026/4/2
 */
public abstract class BaseAgentSessionStore implements AgentSessionStore{
    /** 短期记忆：使用静态变量存储会话记忆（实际项目中建议使用缓存或数据库）*/
    protected List<Map<String, Object>> sessionMemory;
    public BaseAgentSessionStore(List<Map<String, Object>> sessionMemory){
        this.sessionMemory = sessionMemory;

    }

    public BaseAgentSessionStore(List<Map<String, Object>> sessionMemory,int sessionSize){
        this.sessionMemory = sessionMemory;
        this.sessionSize = sessionSize;

    }
    public BaseAgentSessionStore( int sessionSize){
        this.sessionMemory = new ArrayList<>();
        this.sessionSize = sessionSize;

    }
    public BaseAgentSessionStore(){
        this.sessionMemory = new ArrayList<>();

    }
    /**
     * 子任务会话记忆
     */
    private Map<String,AgentSessionStore> subTaskSessionMemorys;


    public List<Map<String, Object>> getSessionMemory() {
        return sessionMemory;
    }
    @Override
    public void addSubTaskSessionMemory(String agentId,AgentSessionStore subTaskSessionMemory) {
        if(subTaskSessionMemorys == null){
            subTaskSessionMemorys = new LinkedHashMap<>();
        }
        this.subTaskSessionMemorys.put(agentId, subTaskSessionMemory);
    }


    @Override
    public AgentSessionStore getSubTaskSessionMemory(String agentId) {
        if(subTaskSessionMemorys == null){
            return null;
        }
        return subTaskSessionMemorys.get(agentId);
    }

    @Override
    public void addSessionMessage(Map<String, Object> message){
        if(sessionMemory == null){
            return ;
        }
        sessionMemory.add(message);
        if(sessionMemory.size() > sessionSize){
            sessionMemory.remove(0);
        }
    }
    @Override
    public Map<String, Object> addAssistantSessionMessage(String message){
        if(sessionMemory == null){
            return null;
        }
        Map<String, Object> assistantMessage = MessageBuilder.buildAssistantMessage(message);
        addSessionMessage(assistantMessage);
        return assistantMessage;
    }
    @Override
    public Map<String, Object> addAssistantSessionMessage(ServerEvent serverEvent){
        if(sessionMemory == null){
            return null;
        }
        Map<String, Object> assistantMessage = MessageBuilder.buildAssistantMessage(serverEvent);

        addSessionMessage(assistantMessage);
        return assistantMessage;
    }
    @Override
    public Map<String, Object> addAssistantSessionMessage(BaseStreamDataBuilder baseStreamDataBuilder){
        if(sessionMemory == null){
            return null;
        }

        Map<String, Object> assistantMessage = MessageBuilder.buildAssistantMessage(  baseStreamDataBuilder);

        addSessionMessage(assistantMessage);
        return assistantMessage;
    }
    public Map<String,Object> getLastMessage(){
        if(sessionMemory == null || sessionMemory.size() == 0){
            return null;
        }
        return sessionMemory.get(sessionMemory.size() - 1);
    }
    /**
     * 会话窗口大小，默认20
     */
    protected int sessionSize = 50;
    @Override
    public void setSessionSize(int sessionSize) {
        this.sessionSize = sessionSize;
    }
    @Override
    public int getSessionSize() {
        return sessionSize;
    }
}
