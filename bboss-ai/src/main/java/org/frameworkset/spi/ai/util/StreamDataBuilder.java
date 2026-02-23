package org.frameworkset.spi.ai.util;
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

import com.frameworkset.util.SimpleStringUtil;
import org.frameworkset.spi.ai.adapter.AgentAdapter;
import org.frameworkset.spi.ai.model.ChatObject;
import org.frameworkset.spi.ai.model.FunctionTool;
import org.frameworkset.spi.ai.model.ServerEvent;
import org.frameworkset.spi.ai.model.StreamData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author biaoping.yin
 * @Date 2026/1/12
 */
public interface StreamDataBuilder {
    StreamData build(AgentAdapter agentAdapter , String line);
    boolean isDone(AgentAdapter agentAdapter,String data);
    String getDoneData(AgentAdapter agentAdapter);
    void handleServerEvent(AgentAdapter agentAdapter,ServerEvent serverEvent);
    ChatObject getChatObject();

    default boolean isToolCall(String finishReason){
        if(finishReason != null && finishReason.equals("tool_calls")){
            return true;
        }
        return false;
    }
    
    default StreamData functionTools(Map message,String finishReason){
        
        if(message != null) {
            //tool_calls -> {ArrayList@5174}  size = 1
            List<Map> tool_calls  = (List)message.get("tool_calls");
            if(tool_calls != null && tool_calls.size() > 0) {
                List<FunctionTool> functionTools = new ArrayList<>();
                for (Map tool_call : tool_calls) {
                    FunctionTool functionTool = new FunctionTool();
                    functionTool.setId((String)tool_call.get("id"));
                    functionTool.setIndex((Integer)tool_call.get("index"));
                    functionTool.setType((String)tool_call.get("type"));
                    Map function = (Map)tool_call.get("function");
                    String arguments = (String)function.get("arguments");
                    if(arguments != null) {
                        functionTool.setArguments(SimpleStringUtil.json2Object(arguments,Map.class));
                    }
                    functionTool.setFunctionName((String)function.get("name"));
                    functionTools.add(functionTool);
                }
                
                return new StreamData(functionTools,tool_calls,finishReason);
                
            }
           

        }
        return null;
    }
}
