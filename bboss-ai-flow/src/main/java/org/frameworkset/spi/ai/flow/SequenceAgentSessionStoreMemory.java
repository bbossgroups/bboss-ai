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

import org.frameworkset.spi.ai.AIAgent;
import org.frameworkset.spi.ai.model.LastSessionMessage;
import org.frameworkset.spi.ai.model.LinkedMessageMap;
import org.frameworkset.spi.ai.store.AgentSessionStore;
import org.frameworkset.spi.ai.store.AgentSessionStoreMemory;
import org.frameworkset.spi.ai.store.StoreContext;

import java.util.List;
import java.util.Map;


/**
 * @author biaoping.yin
 * @Date 2026/5/3
 */
public class SequenceAgentSessionStoreMemory extends AgentSessionStoreMemory<SequenceAgentSessionStoreMemory> {
    public SequenceAgentSessionStoreMemory(List sessionMemory) {
        super(sessionMemory);
    }

    public SequenceAgentSessionStoreMemory(List sessionMemory, int sessionSize) {
        super(sessionMemory, sessionSize);
    }

    public SequenceAgentSessionStoreMemory(int sessionSize) {
        super(sessionSize);
    }

    public SequenceAgentSessionStoreMemory(AgentSessionStore parentAgentSessionStore, int sessionSize) {
        super(parentAgentSessionStore, sessionSize);
    }

    public SequenceAgentSessionStoreMemory() {
    }

    public SequenceAgentSessionStoreMemory(String sessionId, String userId, String agentId) {
        super(sessionId, userId, agentId);
    }

    public SequenceAgentSessionStoreMemory(StoreContext storeContext, AIAgent agent) {
        super(storeContext,   agent);
    }

    private Object lock = new Object();

    @Override
    public SequenceAgentSessionStoreMemory setAIAgent(AIAgent aiAgent) {
        this.aiAgent = aiAgent;
        return  this;
    }
    /**
     * 串行节点需要获取上级智能体的最后一个消息作为串行分支节点的头部节点输入消息
     * @return
     */
    @Override
    public LastSessionMessage getLastSubAgentSessionMessage(){
 
        if(this.aiAgent.isSequenceHeaderNode()) {
            if (this.parentAgentSessionStore != null) {
                return this.parentAgentSessionStore.getLastSubAgentSessionMessage();
            } else {
                return this.mainAgentSessionStore.getLastSubAgentSessionMessage();
            }
        }
        else{
            return super.getLastSubAgentSessionMessage();
        }
    }


    /**
     * 添加子智能体结果消息
     * @param persistentMessage
     * @param agentId
     * @param parentAgentId
     */
    @Override
    public LastSessionMessage addAgentResultSessionMessage( LinkedMessageMap<String, Object> persistentMessage,
                                                           String agentId, String parentAgentId){

        LastSessionMessage lastSessionMessage = null;
//        if(this.mainAgentSessionStore != null) {//需要通过主智能体持久化消息
////            loadSessionMemory(message,  agentId);
//            //msgId,createTime,sessionId,seqNo,message,role
//            lastSessionMessage  = mainAgentSessionStore.persistentSessionMessage(persistentMessage, agentId,parentAgentId,null,null,MESSAGE_TYPE_AGENTRESULTMESSAGE);
//
//        }
//        else if(this.persistentSessionMemory){//主智能体直接持久化消息
////            loadSessionMemory(message,  agentId);
//            lastSessionMessage  = persistentSessionMessage(persistentMessage, agentId,parentAgentId,null,null,MESSAGE_TYPE_AGENTRESULTMESSAGE);
//
//
//        }
        //msgId,createTime,sessionId,seqNo,message,role


        //不要将串行智能体节点中的子智能体结果消息添加到会话内存中
//        appendSessionMessage(message);

        return lastSessionMessage;

    }
}
