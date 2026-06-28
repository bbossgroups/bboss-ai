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

import static org.frameworkset.spi.ai.store.SessionMessage.*;
import static org.frameworkset.spi.ai.store.SessionMessage.MESSAGE_TYPE_ASSISTANT_MESSAGE;

/**
 * 将角色转换为消息类型messageType，对应agent_session_message表中的messageType字段
 * 0 代表子智能体辅助消息， 1 代表子智能体输出结果 2 代表用户输入消息 3 智能体系统消息 5 智能体跟踪消息
 * 如果用户需要扩展自己的消息角色和编码，可以继承AgentMessageTypeConvertor类并重写convertMessageType方法，0-100为系统内置编码，如果用户自定义编码请从101开始
 * @author biaoping.yin
 * @Date 2026/6/15
 */
public class AgentMessageTypeConvertor {
    /**
     * 将角色转换为消息类型messageType，对应agent_session_message表中的messageType字段
     * 0 代表子智能体辅助消息， 1 代表子智能体输出结果 2 代表用户输入消息 3 智能体系统消息 5 智能体跟踪消息
     * @param role
     * @return
     */
    public String convertMessageType(String role){       
        if("system".equals(role)){
            return MESSAGE_TYPE_SYSTEM_MESSAGE;
        }
        else if("user".equals(role)){
            return MESSAGE_TYPE_USER_MESSAGE;
        }
        else if("assistant".equals(role)){
            return MESSAGE_TYPE_ASSISTANT_MESSAGE;
        }
        else if("trace".equals(role)){
            return MESSAGE_TYPE_TRACE_MESSAGE;
        }

        else if("rag".equals(role)){
            return MESSAGE_TYPE_RAG_MESSAGE;
        }

        else if("refuse".equals(role)){
            return MESSAGE_TYPE_REFUSE_MESSAGE;
        }
        else if(SessionMessage.MESSAGE_TYPE_USER_INPUTMESSAGE_NAME.equals(role)){
            return MESSAGE_TYPE_USER_INPUTMESSAGE;
        }
        else if(MESSAGE_TYPE_LLM_INPUTMESSAGE_NAME.equals(role)){
            return MESSAGE_TYPE_LLM_INPUTMESSAGE;
        }

        else if(MESSAGE_TYPE_LLM_OUTPUTMESSAGE_NAME.equals(role)){
            return MESSAGE_TYPE_LLM_OUTPUTMESSAGE;
        }
        else if(MESSAGE_TYPE_EMBEDDING_INPUTMESSAGE_NAME.equals(role)){
            return MESSAGE_TYPE_EMBEDDING_INPUTMESSAGE;
        }


        else if(MESSAGE_TYPE_EMBEDDING_OUTPUTMESSAGE_NAME.equals(role)){
            return MESSAGE_TYPE_EMBEDDING_OUTPUTMESSAGE;
        }
        else if(MESSAGE_TYPE_RERANK_INPUTMESSAGE_NAME.equals(role)){
            return MESSAGE_TYPE_RERANK_INPUTMESSAGE;
        }
        else if(MESSAGE_TYPE_RERANK_OUTPUTMESSAGE_NAME.equals(role)){
            return MESSAGE_TYPE_RERANK_OUTPUTMESSAGE;
        }
        else if(MESSAGE_TYPE_AGENT_RESULTMESSAGE_NAME.equals(role)) {
            return MESSAGE_TYPE_AGENT_RESULTMESSAGE;
        }
        else if(MESSAGE_TYPE_TOOLSEARCH_MESSAGE_NAME.equals(role)) {
            return MESSAGE_TYPE_TOOLSEARCH_MESSAGE;
        }
        else if(MESSAGE_TYPE_MCPCALL_MESSAGE_NAME.equals(role)) {
            return MESSAGE_TYPE_MCPCALL_MESSAGE;
        }
        else if(MESSAGE_TYPE_TOOLCALL_MESSAGE_NAME.equals(role)) {
            return MESSAGE_TYPE_TOOLCALL_MESSAGE;
        }
        



        return MESSAGE_TYPE_ASSISTANT_MESSAGE;
    }
}
