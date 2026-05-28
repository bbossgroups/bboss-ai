package org.frameworkset.spi.ai.adapter;
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
import org.frameworkset.spi.ai.AIAgent;
import org.frameworkset.spi.ai.model.*;
import org.frameworkset.spi.ai.util.MessageBuilder;
import org.frameworkset.spi.remote.http.ClientConfiguration;

import java.util.*;

/**
 * @author biaoping.yin
 * @Date 2026/1/8
 */
public class OpenaiAgentAdapter extends QwenAgentAdapter{
    @Override
    public String getSubmitVideoTaskUrl(ClientConfiguration clientConfiguration, VideoAgentMessage videoAgentMessage) {
        return "/v1/chat/completions";
    }

    @Override
    public String getVideoTaskResultUrl(ClientConfiguration clientConfiguration,VideoStoreAgentMessage videoStoreAgentMessage) {
        return "/v1/chat/completions";
    }


    @Override
    public String getImageVLCompletionsUrl(ClientConfiguration clientConfiguration,ImageVLAgentMessage imageVLAgentMessage) {
//        throw new UnsupportedOperationException("getImageVLCompletionsUrl");
        return "/v1/chat/completions";
    }
    @Override
    public String getChatCompletionsUrl(ClientConfiguration clientConfiguration,ChatAgentMessage chatAgentMessage) {
        return "/v1/chat/completions";
    }
    @Override
    public String getGenImageCompletionsUrl(ClientConfiguration clientConfiguration,ImageAgentMessage imageAgentMessage) {
        return "/v1/chat/completions";
    }

    @Override
    protected void buildThinking(ChatAgentMessage chatAgentMessage,Map<String, Object> requestMap){
//        Map parameters = chatAgentMessage.getParameters();
//        Boolean thinking = chatAgentMessage.getThinking();
//        if(thinking != null){
//            if(parameters != null) {
//                if (!parameters.containsKey("thinking")) {
//                    Map data =  new LinkedHashMap();
//                    data.put("type", thinking?"enabled":"disabled");
//                    requestMap.put("thinking", data);
//                }
//            }
//            else{
////                chatAgentMessage.addMapParameter("thinking", "type", "enabled");//kimi-k2.5禁用思维模式,启用：enabled
//                Map data =  new LinkedHashMap();
//                data.put("type", thinking?"enabled":"disabled");
//                requestMap.put("thinking", data);
//            }
//        }
//        else{
//            chatAgentMessage.setThinking(this.getDefaultThinking());
//        }



    }
//    /**
//     * 构建智能问答请求参数
//     * @param chatAgentMessage
//     * @return
//     */
//    public Map buildOpenAIRequestMap1(ChatAgentMessage chatAgentMessage, AIAgent aiAgent) {
////        super.buildOpenAIRequestMap(chatAgentMessage, aiAgent);
//        String agentId = aiAgent.getAgentId();
//        String message = getPrompt(  chatAgentMessage,   aiAgent);
//        Map<String, Object> userMessage = MessageBuilder.buildUserMessage( message);
//        Map<String,Object> systemMessage = null;
//        Map<String, Object> requestMap = new HashMap<>();
//        requestMap.put("model", chatAgentMessage.getModel());
//
//        List<Map<String, Object>> messages = null;
//        List<Map<String, Object>> sessionMemory = aiAgent.getSessionMemory();
//        if(sessionMemory != null){
//            // 构建消息历史列表，包含之前的会话记忆           
//
//            if(sessionMemory.size() == 0){
//                String systemPrompt = getSystemPrompt(chatAgentMessage,aiAgent);
//                if(systemPrompt != null){
//                    systemMessage = MessageBuilder.buildSystemMessage(systemPrompt);
//                    chatAgentMessage.addSessionMessage(systemMessage,message,aiAgent);
//                }
//            }
//            // 添加当前用户消息
//            chatAgentMessage.addSessionMessage(userMessage,agentId,aiAgent);
//            messages = new ArrayList<>(sessionMemory);
//
//
//        }
//        else{
//            messages = new ArrayList<>();
//            String systemPrompt = getSystemPrompt(chatAgentMessage,aiAgent);
//            if(systemPrompt != null){
//                systemMessage = MessageBuilder.buildSystemMessage(systemPrompt);
//                messages.add(systemMessage);
//            }
//            messages.add(userMessage);
//        }
//
//
//
//        requestMap.put("messages", messages);
//        Map parameters = chatAgentMessage.getParameters();
//        if(SimpleStringUtil.isNotEmpty( parameters)){
//
//            requestMap.putAll(parameters);
//            if(!parameters.containsKey("stream") && chatAgentMessage.getStream() != null){
//                requestMap.put("stream", chatAgentMessage.getStream());
//            }
//            if(!parameters.containsKey("temperature") && chatAgentMessage.getTemperature() != null){
//                requestMap.put("temperature", chatAgentMessage.getTemperature());
//            }
//
//            if(parameters.containsKey("max_tokens")){
//                requestMap.remove("max_tokens");
//            }
//
////            if(!parameters.containsKey("max_tokens") && chatAgentMessage.getMaxTokens() != null){
////                requestMap.put("max_tokens", chatAgentMessage.getMaxTokens());
////            }
//        }
//        else {
//            //设置默认参数
//            if( chatAgentMessage.getStream() != null){
//                requestMap.put("stream", chatAgentMessage.getStream());
//            }
//
//            if( chatAgentMessage.getTemperature() != null){
//                requestMap.put("temperature", chatAgentMessage.getTemperature());
//            }
////            if( chatAgentMessage.getMaxTokens() != null){
////                requestMap.put("max_tokens", chatAgentMessage.getMaxTokens());
////            }
//        }
//        buildThinking(  chatAgentMessage, requestMap);
////        requestMap.put("group","codex");
//        buildTools(aiAgent, requestMap);
//        return requestMap;
//    }
//
////    @Override
//    protected Object convertTools1(Object tools){
//        if(tools instanceof List){
//            List<FunctionToolDefine> functionToolDefines = (List<FunctionToolDefine>)tools;
//            List<Function> functions = new ArrayList<>();
//            for(FunctionToolDefine functionToolDefine : functionToolDefines){
//                Function function = functionToolDefine.getFunction();
//                function.setType(functionToolDefine.getType());
//                functions.add(function);
//            }
//            return functions;
//        }
//        else if(tools instanceof String){
//            return SimpleStringUtil.json2ListObject((String)tools,Map.class);
//        }
//        return tools;
//    }
}
