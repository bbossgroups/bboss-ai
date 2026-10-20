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
import org.frameworkset.spi.ai.store.AgentStore;
import org.frameworkset.spi.ai.store.AgentStoreMemory;
import org.frameworkset.spi.ai.store.StoreContext;

import java.util.ArrayList;
import java.util.List;


/**
 * @author biaoping.yin
 * @Date 2026/5/3
 */
public class ParrelAgentStoreMemory extends AgentStoreMemory<ParrelAgentStoreMemory> {
    public ParrelAgentStoreMemory(List sessionMemory) {
        super(sessionMemory);
    }

 

 

    public ParrelAgentStoreMemory(AgentStore parentAgentStore) {
        super(parentAgentStore);
    }

    public ParrelAgentStoreMemory() {
    }

    public ParrelAgentStoreMemory(String sessionId, String userId, String agentId) {
        super(sessionId, userId, agentId);
    }

    public ParrelAgentStoreMemory(StoreContext storeContext, AIAgent agent) {
        super(storeContext,agent);
    }

    private Object lock = new Object();
    @Override
    public void setSubAgentLastSessionMessage(LastSessionMessage lastSubAgentSessionMessage) {
        synchronized (lock) {
            if (lastSubAgentSessionMessages == null) {
                lastSubAgentSessionMessages = new ArrayList<>();
            }
            lastSubAgentSessionMessages.add(lastSubAgentSessionMessage);
            this.lastSubAgentSessionMessage = lastSubAgentSessionMessage;
        }
    }

    /**
     * 并行节点需要获取上级智能体的最后一个消息作为并行分支节点的输入消息
     * @return
     */
    @Override
    public LastSessionMessage getLastSubAgentSessionMessage(){
//        if(lastSubAgentSessionMessage != null){
//            return lastSubAgentSessionMessage;
//        }
//        else{
//            return null;
//        }
        if(this.parentAgentStore != null){
            return this.parentAgentStore.getLastSubAgentSessionMessage();
        }
        else{
            return this.mainAgentStore.getLastSubAgentSessionMessage();
        }
    }


    /**
     * 添加子智能体结果消息
     * @param message
     * @param agentId
     * @param parentAgentId
     */
    @Override
    public LastSessionMessage addAgentResultSessionMessage(LinkedMessageMap<String, Object> message, String agentId, String parentAgentId,
														   AIAgent aiAgent ){

        LastSessionMessage lastSessionMessage = null;
//        if(this.mainAgentSessionStore != null) {//需要通过主智能体持久化消息
////            loadSessionMemory(message,  agentId);
//            //msgId,createTime,sessionId,seqNo,message,role
//            lastSessionMessage  = mainAgentSessionStore.persistentSessionMessage(message, agentId,parentAgentId,null,null,MESSAGE_TYPE_AGENTRESULTMESSAGE);
//
//        }
//        else if(this.persistentSessionMemory){//主智能体直接持久化消息
////            loadSessionMemory(message,  agentId);
//            lastSessionMessage  = persistentSessionMessage(message, agentId,parentAgentId,null,null,MESSAGE_TYPE_AGENTRESULTMESSAGE);
//
//
//        }
        //msgId,createTime,sessionId,seqNo,message,role


//        appendSessionMessage(message);

        return lastSessionMessage;

    }
}
