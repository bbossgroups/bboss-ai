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
import org.frameworkset.spi.ai.context.ChatContext;
import org.frameworkset.spi.ai.material.JiutianGenFileDownload;
import org.frameworkset.spi.ai.model.*;
import org.frameworkset.spi.ai.util.AIResponseUtil;
import org.frameworkset.spi.ai.util.BaseStreamDataBuilder;
import org.frameworkset.spi.ai.util.MessageBuilder;
import org.frameworkset.spi.remote.http.ClientConfiguration;
import org.slf4j.Logger;

import java.util.*;

/**
 * Jiutian模型智能体适配器
 * @author biaoping.yin
 * @Date 2026/1/4
 */
public class JiutianAgentAdapter extends QwenAgentAdapter{
    private Logger logger = org.slf4j.LoggerFactory.getLogger(JiutianAgentAdapter.class);
    private static String downImageUrl = "/largemodel/moma/api/v1/fs/getFile";
	public String getReasoningContent( Map delta ){
		String reasoning_content = (String) delta.get("reasoning");
		if(reasoning_content == null){
			reasoning_content = (String) delta.get("reasoning_content");
		}
		return reasoning_content;
	}
	@Override
    public String getChatCompletionsUrl(ClientConfiguration clientConfiguration,ChatAgentMessage chatAgentMessage) {
        return "/largemodel/moma/api/v3/chat/completions";
    }
    @Override
    public String getImageVLCompletionsUrl(ClientConfiguration clientConfiguration,ImageVLAgentMessage imageVLAgentMessage) {
        return "/largemodel/moma/api/v3/image/text";
    }

    @Override
    public String getGenImageCompletionsUrl(ClientConfiguration clientConfiguration,ImageAgentMessage imageAgentMessage) {
        return "/largemodel/moma/api/v3/images/generations";
    }

    @Override
    protected AgentAdapter initAgentAdapter(){
        genFileDownload = new JiutianGenFileDownload();
        return this;
    }
    
    @Override
    public Map buildGenImageRequestMap(ImageAgentMessage imageAgentMessage,AIAgent aiAgent) {

        Map<String, Object> requestMap = new HashMap<>();

        requestMap.put("model", imageAgentMessage.getModel());
        requestMap.put("prompt", getPrompt(  imageAgentMessage,   aiAgent));
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
     * @param config
     * @param imageData
     * @return
     */
    public ImageEvent buildGenImageResponse(ClientConfiguration config,ImageAgentMessage imageAgentMessage,StoreChatObject storeChatObject, Map imageData){
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
                imageEvent.setImageUrl(genFileDownload.downloadImage(config,  imageAgentMessage,storeChatObject,downImageUrl,imageUrl));

            } else {
                for (int i = 0; i < size; i++) {
                    Map image = (Map) imageContentData.get(i);
                    String imageUrl = (String) image.get("url");
                    imageEvent.addImageUrl(imageUrl);
                    imageEvent.addImageUrl(genFileDownload.downloadImage(config,  imageAgentMessage,storeChatObject,downImageUrl,imageUrl));
                }
            }
            imageEvent.setFinishReason(finishReason);
        }
         
        return imageEvent;
    }
	
	@Override
	protected void buildThinking(ChatAgentMessage chatAgentMessage,ChatObject chatObject,Map<String, Object> requestMap){
//        Map parameters = chatAgentMessage.getParameters();
		ChatContext chatContext = chatObject.getChatContext();
		ClientConfiguration clientConfiguration = chatContext.getClientConfiguration();
		if(clientConfiguration != null) {
			String standard_reasoning = clientConfiguration.getExtendConfig("standard_reasoning");
			if (standard_reasoning != null && standard_reasoning.equals("true")) {
				super.buildThinking(chatAgentMessage, chatObject, requestMap);
				return;
			}
		}
		Boolean thinking = chatAgentMessage.getThinking();
		String effort = chatAgentMessage.getEffort();
		
		if(chatContext != null ){
			if(chatContext.getThinking() != null)
				thinking = chatContext.getThinking();
			if(chatContext.getEffort() != null){
				effort = chatContext.getEffort();
			}
			
		}
		if(thinking != null){
			Map reasoning = new LinkedHashMap();
			reasoning.put("enabled", thinking);
			if(effort != null) {
				reasoning.put("effort", effort);
			}
			requestMap.put("reasoning", reasoning);
			chatObject.setThinking(thinking);			 
		}
		
	}
	
	

    @Override
    public Map buildImageVLRequestMap(ImageVLAgentMessage imageAgentMessage, AIAgent aiAgent, ChatContext chatContext) {
		List<LinkedMessageMap<String, Object>> sessionMemory = aiAgent.getSessionMemory(true);
        Map<String, Object> requestMap = new HashMap<>();
        
        requestMap.put("model", imageAgentMessage.getModel());
       
        List<String> imageUrls = imageAgentMessage.getImageUrls();
        String prompt = getPrompt(  imageAgentMessage,   aiAgent);
        if(chatContext != null)
        {
            prompt = chatContext.evalPrompt(prompt);
        }
        if(sessionMemory == null || (sessionMemory != null && sessionMemory.size() == 0)) {
            

            if (imageUrls != null && imageUrls.size() > 0) {
                requestMap.put("image", imageUrls.get(0));
            }
            requestMap.put("prompt",  prompt);
        }
// 构建消息历史列表，包含之前的会话记忆

     
        if(sessionMemory != null) {
			
			LinkedMessageMap<String, Object> userMessage = null;
			LinkedMessageMap<String, Object> systemMessage = null;
            if (imageUrls != null && imageUrls.size() > 0) {
                userMessage = buildInputImagesMessage( prompt, imageUrls.toArray(new String[]{}));
            } else {
                userMessage = buildInputImagesMessage( prompt, (String[]) null);
            }

            List<LinkedMessageMap<String, Object>> messages = null;
            if (sessionMemory != null) {
                if (sessionMemory.size() == 0) {
                    String systemPrompt = getSystemPrompt(imageAgentMessage,aiAgent);
                    if(systemPrompt != null){
                        if(chatContext != null)
                        {
                            systemPrompt = chatContext.evalSystemPrompt(systemPrompt);
                        }
                        systemMessage = MessageBuilder.buildSystemMessage(systemPrompt);
                        imageAgentMessage.addSessionMessage(systemMessage,aiAgent);
                    }
                }
                imageAgentMessage.addSessionMessage(userMessage,aiAgent);
                messages = new ArrayList<>(sessionMemory);
            } else {
                messages = new ArrayList<>();
                String systemPrompt = getSystemPrompt(imageAgentMessage,aiAgent);
                if(systemPrompt != null){
                    if(chatContext != null)
                    {
                        systemPrompt = chatContext.evalSystemPrompt(systemPrompt);
                    }
                    systemMessage = MessageBuilder.buildSystemMessage(systemPrompt);
                    messages.add(systemMessage);
                }
                messages.add(userMessage);
            }


            requestMap.put("messages", handleImageParserMessages(messages));
            Map parameters = imageAgentMessage.getParameters();

            filterParameters(chatContext,imageAgentMessage,aiAgent, requestMap, parameters);
        }

        return requestMap;
    }
    @Override
    protected LinkedMessageMap<String, Object> buildInputImagesMessage(String message,String... imageUrls) {
        return MessageBuilder.buildJiuTianInputImagesMessage(message,imageUrls);
    }
    @Override
    protected Object handleImageParserMessages(List<LinkedMessageMap<String, Object>> messages){
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
    public StreamData parseImageParserStreamContentFromData(BaseStreamDataBuilder streamDataBuilder, Map data){
        return AIResponseUtil.parseJiutianImageParserStreamContentFromData(streamDataBuilder,data);
    }
    
}
