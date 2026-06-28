package org.frameworkset.spi.ai.tool;
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

import org.frameworkset.spi.ai.callback.ChatContext;
import org.frameworkset.spi.ai.model.ChatObject;
import org.frameworkset.spi.ai.model.ServerEvent;
import org.frameworkset.spi.ai.model.TokenMetrics;
import org.frameworkset.spi.ai.model.TraceMessage;

/**
 * @author biaoping.yin
 * @Date 2026/6/28
 */
public class AgentTraceHolder {
    private static final ThreadLocal<ChatObject> chatObjectThreadLocal = new ThreadLocal<>();
    private static boolean toolTrace = true;
    
    public static boolean isToolTrace() {
        return toolTrace;
    }
    public static void setToolTrace(boolean toolTrace) {
        AgentTraceHolder.toolTrace = toolTrace;
    }
    public static void setChatObject(ChatObject chatObject){
        chatObjectThreadLocal.set(chatObject);
    }
    public static ChatObject getChatObject(){
        return chatObjectThreadLocal.get();
    }
    public static void removeChatObject(){
        chatObjectThreadLocal.remove();
    }

    /**
     * 记录智能体跟踪日志消息
     * @param traceMessage
     */
    public static void trace(TraceMessage traceMessage){         
        ChatObject chatObject = getChatObject();
        if(chatObject != null){
            chatObject.getAgent().recordTraceMessage(traceMessage);
        }
    }

    /**
     * 记录智能体跟踪日志消息
     * @param traceMessage
     * @param tokenMetrics
     */
    public static void trace(TraceMessage traceMessage, TokenMetrics tokenMetrics){         
        ChatObject chatObject = getChatObject();
        if(chatObject != null){
            chatObject.getAgent().recordTraceMessage(traceMessage, tokenMetrics);
        }
    }

    /**
     * 向终端推送事件消息
     * @param serverEvent
     */
    public static void emitterServerEvent(ServerEvent serverEvent){
        ChatObject chatObject = getChatObject();
        if(chatObject != null){
            ChatContext chatContext = chatObject.getChatContext();
            chatContext.getAgentSink().next(serverEvent);
        }
    }

}
