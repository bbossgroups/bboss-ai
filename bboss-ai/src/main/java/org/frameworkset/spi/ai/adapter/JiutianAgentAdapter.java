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
import org.frameworkset.spi.ai.material.JiutianGenFileDownload;
import org.frameworkset.spi.ai.model.*;
import org.frameworkset.spi.ai.util.AIResponseUtil;
import org.frameworkset.spi.ai.util.BaseStreamDataBuilder;
import org.frameworkset.spi.ai.util.MessageBuilder;
import org.frameworkset.spi.ai.util.StreamDataBuilder;
import org.frameworkset.spi.remote.http.ClientConfiguration;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Kimi模型智能体适配器
 * @author biaoping.yin
 * @Date 2026/1/4
 */
public class JiutianAgentAdapter extends QwenAgentAdapter{
    private Logger logger = org.slf4j.LoggerFactory.getLogger(JiutianAgentAdapter.class);
    private static String downImageUrl = "/largemodel/moma/api/v1/fs/getFile";
    @Override
    public String getChatCompletionsUrl(ChatAgentMessage chatAgentMessage) {
        return "/largemodel/moma/api/v3/chat/completions";
    }
    @Override
    public String getImageVLCompletionsUrl(ImageVLAgentMessage imageVLAgentMessage) {
        return "/largemodel/moma/api/v3/image/text";
    }

    @Override
    public String getGenImageCompletionsUrl(ImageAgentMessage imageAgentMessage) {
        return "/largemodel/moma/api/v3/images/generations";
    }

    @Override
    protected AgentAdapter initAgentAdapter(){
        genFileDownload = new JiutianGenFileDownload();
        return this;
    }
    
    @Override
    public Map buildGenImageRequestMap(ImageAgentMessage imageAgentMessage) {

        Map<String, Object> requestMap = new HashMap<>();

        requestMap.put("model", imageAgentMessage.getModel());
        requestMap.put("prompt", imageAgentMessage.getPrompt());
        List<String> imageUrls = imageAgentMessage.getImageUrls();
        if(imageUrls != null && imageUrls.size() > 0){
            requestMap.put("filePath", imageUrls.get(0));
        }

        Map parameters = imageAgentMessage.getParameters();
        if(SimpleStringUtil.isEmpty( parameters)){
            //默认参数
//            requestMap.put("sequential_image_generation", "disabled");
//            requestMap.put("response_format", "url");
//            requestMap.put("size", "2k");
//            requestMap.put("watermark", true);
        }
        else{
            requestMap.putAll(parameters);
        }


//        requestMap.put("sequential_image_generation", "disabled");
//        requestMap.put("response_format", "url");
//        requestMap.put("size", "2k");
//        requestMap.put("watermark", true);
        return requestMap;
    }
    
     

    /**
     * https://jiutian.10086.cn/portal/common-helpcenter#/document/1157?platformCode=DMX_TYZX
     * @param config
     * @param imageData
     * @return
     */
    public ImageEvent buildGenImageResponse(ClientConfiguration config,ImageAgentMessage imageAgentMessage, Map imageData){
        ImageEvent imageEvent = null;
        List choices = (List)imageData.get("choices");
        if(choices == null || choices.size() == 0) {
            String response = (String) imageData.get("response");
            imageEvent = new ImageEvent();
            imageEvent.setResponse(response);
            imageEvent.setContentEvent((String)imageData.get("contentEvent"));
            return imageEvent;
        }
        
        Map choice = (Map)choices.get(0);

        String finishReason = (String)choice.get("finish_reason");
        List imageContentData = (List)choice.get("data");
        int size = imageContentData.size();
        
        if(size > 0) {
            imageEvent = new ImageEvent();
            if (imageContentData.size() == 1) {
                Map image = (Map) imageContentData.get(0);
                String imageUrl = (String) image.get("url");
                imageEvent.setGenImageUrl(imageUrl);
                imageEvent.setImageUrl(genFileDownload.downloadImage(config,  imageAgentMessage,downImageUrl,imageUrl));

            } else {
                for (int i = 0; i < size; i++) {
                    Map image = (Map) imageContentData.get(i);
                    String imageUrl = (String) image.get("url");
                    imageEvent.addImageUrl(imageUrl);
                    imageEvent.addImageUrl(genFileDownload.downloadImage(config,  imageAgentMessage,downImageUrl,imageUrl));
                }
            }
            imageEvent.setFinishReason(finishReason);
        }
         
        return imageEvent;
    }

    @Override
    public Map buildImageVLRequestMap(ImageVLAgentMessage imageAgentMessage) {
        Map<String, Object> requestMap = new HashMap<>();
        
        requestMap.put("model", imageAgentMessage.getModel());
        List<Map<String, Object>> sessionMemory = imageAgentMessage.getSessionMemory();
        List<String> imageUrls = imageAgentMessage.getImageUrls();
        if(sessionMemory == null || (sessionMemory != null && sessionMemory.size() == 0)) {
            

            if (imageUrls != null && imageUrls.size() > 0) {
                requestMap.put("image", imageUrls.get(0));
            }
            requestMap.put("prompt", imageAgentMessage.getPrompt());
        }
// 构建消息历史列表，包含之前的会话记忆

     
        if(sessionMemory != null) {
            
            Map<String, Object> userMessage = null;
            Map<String, Object> systemMessage = null;
            if (imageUrls != null && imageUrls.size() > 0) {
                userMessage = buildInputImagesMessage(imageAgentMessage.getPrompt(), imageUrls.toArray(new String[]{}));
            } else {
                userMessage = buildInputImagesMessage(imageAgentMessage.getPrompt(), (String[]) null);
            }

            List<Map<String, Object>> messages = null;
            if (sessionMemory != null) {
                if (sessionMemory.size() == 0) {
                    if (imageAgentMessage.getSystemPrompt() != null) {
                        systemMessage = MessageBuilder.buildSystemMessage(imageAgentMessage.getSystemPrompt());
                        imageAgentMessage.addSessionMessage(systemMessage);
                    }
                }
                imageAgentMessage.addSessionMessage(userMessage);
                messages = new ArrayList<>(sessionMemory);
            } else {
                messages = new ArrayList<>();
                if (imageAgentMessage.getSystemPrompt() != null) {
                    systemMessage = MessageBuilder.buildSystemMessage(imageAgentMessage.getSystemPrompt());
                    messages.add(systemMessage);
                }
                messages.add(userMessage);
            }


            requestMap.put("messages", handleImageParserMessages(messages));
            Map parameters = imageAgentMessage.getParameters();

            filterParameters(imageAgentMessage, requestMap, parameters);
        }

        return requestMap;
    }
    @Override
    protected Map<String, Object> buildInputImagesMessage(String message,String... imageUrls) {
        return MessageBuilder.buildJiuTianInputImagesMessage(message,imageUrls);
    }
    @Override
    protected Object handleImageParserMessages(List<Map<String, Object>> messages){
//        return SimpleStringUtil.object2json(messages);
        return messages;
    }
 
//    protected Map buildImageVLRequestMap(ImageVLAgentMessage imageAgentMessage) {
//
//        Map<String, Object> requestMap = new HashMap<>();
//        requestMap.put("model",imageAgentMessage.getModel());
//
////        "image": image_path,
////                "prompt": "描述下这张图片",
////        requestMap.put("prompt",imageAgentMessage.getMessage());
////        requestMap.put("image",imageAgentMessage.getImageUrls().get(0));
//
//        // 构建消息历史列表，包含之前的会话记忆
//
//        List<Map<String, Object>> sessionMemory = imageAgentMessage.getSessionMemory();
//        List<Map<String, Object>> messages = null;
//        if(sessionMemory != null && sessionMemory.size() > 0){
//            messages = new ArrayList<>(sessionMemory);
//        }
//        else{
//            messages = new ArrayList<>();
//        }
//
//        Map<String, Object> userMessage = buildInputImagesMessage(imageAgentMessage.getMessage(),imageAgentMessage.getImageUrls().toArray(new String[]{}));
//        messages.add(userMessage);
//
//        String data = SimpleStringUtil.object2json(messages);
//        
//        requestMap.put("messages", data);
//        Map parameters = imageAgentMessage.getParameters();
//
//        filterParameters(imageAgentMessage,requestMap,parameters);
//
//        return requestMap;
//    }
    public String getAIImageParserRequestType(){
//        return AIConstants.AI_CHAT_REQUEST_POST_FORM;
        return AIConstants.AI_CHAT_REQUEST_BODY_JSON;

    }
//    protected void filterParameters(AgentMessage agentMessage, Map<String, Object> requestMap, Map<String, Object> parameters) {
//        if(SimpleStringUtil.isEmpty( parameters)){
//            if( agentMessage.getStream() != null){
//                requestMap.put("stream", agentMessage.getStream());
//            }
//            
//
//            if( agentMessage.getTemperature() != null){
//                requestMap.put("temperature", agentMessage.getTemperature());
//            }
//
//            // enable_thinking 参数开启思考过程，thinking_budget 参数设置最大推理过程 Token 数
//
//        }
//        else {
//            //设置默认参数
//            if(!parameters.containsKey("stream") && agentMessage.getStream() != null){
//                requestMap.put("stream", agentMessage.getStream());
//            }
//            if(!parameters.containsKey("temperature") && agentMessage.getTemperature() != null){
//                requestMap.put("temperature", agentMessage.getTemperature());
//            }
//            requestMap.putAll( parameters);
//        }
//    }


    @Override
    public boolean isImageParserDone(String data){
        return "[EOS]".equals(data);

    }
    @Override
    public String getImageParserDoneData(){
        return "data:[EOS]";
    }

 

    @Override
    public StreamData parseImageParserStreamContentFromData(BaseStreamDataBuilder streamDataBuilder, String data){
        return AIResponseUtil.parseJiutianImageParserStreamContentFromData(streamDataBuilder,data);
    }
    
}
