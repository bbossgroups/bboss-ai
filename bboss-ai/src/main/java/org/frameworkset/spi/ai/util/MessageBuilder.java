package org.frameworkset.spi.ai.util;
/**
 * Copyright 2025 bboss
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

import org.frameworkset.spi.ai.model.ImageAgentMessage;
import org.frameworkset.spi.ai.model.ServerEvent;
import org.frameworkset.spi.ai.model.StreamData;
import org.frameworkset.spi.ai.model.VideoAgentMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author biaoping.yin
 * @Date 2025/11/2
 */
public class MessageBuilder {
    private static final Logger logger = LoggerFactory.getLogger(MessageBuilder.class);
    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";
    public static final String ROLE_SYSTEM = "system";
    public static final String ROLE_TOOL = "tool";
    
    public static final String TYPE_TEXT = "text";
    public static final String TYPE_IMAGE = "image_url";
    public static final String TYPE_VIDEO = "video_url";
    public static Map<String,Object> buildSystemMessage(String message){


        return buildMessage(ROLE_SYSTEM,  message);
    }
    public static Map<String,Object> buildAudioSystemMessage(String message){
        // 添加当前用户消息
 
        return buildAudioUserMessage(  message,  ROLE_SYSTEM);
    }

    public static Map<String,Object> buildAudioUserMessage(String message){
        // 添加当前用户消息
 
        return buildAudioUserMessage(  message,  ROLE_USER);
    }

    public static Map<String,Object> buildAudioUserMessage(String message,String role){
        // 添加当前用户消息
        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", role);
        List<Map> contents = new ArrayList<>();
        Map contentData = new LinkedHashMap();
        contentData.put("text", message);
        contents.add(contentData);
        userMessage.put("content", contents);
        return userMessage;
    }

    public static Map<String,Object> buildAudioMessage(String audioUrl){
        LinkedHashMap<String, Object> userMessage = new LinkedHashMap<>();
        userMessage.put("role", ROLE_USER);
        List contents = new ArrayList<>();
        Map<String,Object> contentData = new LinkedHashMap<>();
        contentData.put("audio", audioUrl);

        contents.add(contentData);
        userMessage.put("content", contents);
        return userMessage;
    }
    public static void buildGenVideoMessage(Map<String, Object> requestMap, VideoAgentMessage videoAgentMessage){
        

        List contents = new ArrayList<>();
        Map contentData = new LinkedHashMap();
        contentData.put("text", videoAgentMessage.getPrompt());
        contentData.put("type", "text");
        contents.add(contentData);

        if(videoAgentMessage.getImgUrl() != null){
            contentData = new LinkedHashMap();
            Map imageData = new LinkedHashMap();
            imageData.put("url", videoAgentMessage.getImgUrl());
            contentData.put("image_url", imageData);
            contentData.put("type", "image_url");
            contents.add(contentData);
            
        }
        if(videoAgentMessage.getFirstFrameUrl() != null){
            contentData = new LinkedHashMap();
            Map firstFrameData = new LinkedHashMap();
            firstFrameData.put("url", videoAgentMessage.getFirstFrameUrl());
            contentData.put("image_url", firstFrameData);
            contentData.put("type", "image_url");
            contentData.put("role", "first_frame");
            contents.add(contentData);
            
        }

        if(videoAgentMessage.getLastFrameUrl() != null){
            contentData = new LinkedHashMap();
            Map lastFrameData = new LinkedHashMap();
            lastFrameData.put("url", videoAgentMessage.getLastFrameUrl());
            contentData.put("image_url", lastFrameData);
            contentData.put("type", "image_url");
            contentData.put("role", "last_frame");
            contents.add(contentData);
        }
        requestMap.put("content", contents);
    }
    public static Map<String,Object> buildGenImageMessage(String message){
        Map<String, Object> userMessage = new LinkedHashMap<>();
        userMessage.put("role",ROLE_USER);

        List contents = new ArrayList<>();
        Map contentData = new LinkedHashMap();
        contentData.put("text", message);

        contents.add(contentData);
        userMessage.put("content", contents);
        return userMessage;
    }

    public static Map<String,Object> buildGenImageMessage(ImageAgentMessage imageAgentMessage){
        Map<String, Object> userMessage = new LinkedHashMap<>();
        userMessage.put("role",ROLE_USER);

        List contents = new ArrayList<>();
        Map contentData = new LinkedHashMap();
        contentData.put("text", imageAgentMessage.getPrompt());
        
        

        contents.add(contentData);
        List<String> imageUrls = imageAgentMessage.getImageUrls();
        if(imageUrls != null && imageUrls.size() > 0) {
            for(String imageUrl:imageUrls){
                contentData = new LinkedHashMap();
                contentData.put("image", imageUrl);
                contents.add(contentData);
            }
            
        }
        userMessage.put("content", contents);
        return userMessage;
    }
    /**
     * 构建图片识别消息，传入识别提示词和图片url清单，构建模型请求参数报文
     * @param message 提示
     * @param videoUrls 图片url
     * @return
     */
    public static Map<String,Object> buildInputVideosMessage( String message,String... videoUrls){

        List contents = new ArrayList<>();
        Map contentData = null;

        if(videoUrls != null && videoUrls.length > 0) {
            for (String videoUrl : videoUrls) {
                contentData = new LinkedHashMap();
                contentData.put("type", TYPE_VIDEO);
                String _videoUrl = videoUrl;
                contentData.put("video_url", new HashMap<String, String>() {{

                    put("url", _videoUrl);
                }});
                contents.add(contentData);
            }
        }

        contentData = new LinkedHashMap();
        contentData.put("type", TYPE_TEXT);
        contentData.put("text", message);;
        contents.add(contentData);
        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", ROLE_USER);
        userMessage.put("content", contents);
        return userMessage;
    }

    /**
     * 构建图片识别消息，传入识别提示词和图片url清单，构建模型请求参数报文
     * @param message 提示
     * @param imageUrls 图片url
     * @return
     */
    public static Map<String,Object> buildInputImagesMessage( String message,String... imageUrls){

        List contents = new ArrayList<>();
        Map contentData = null;
       
        if(imageUrls != null && imageUrls.length > 0) {
            for (String imageUrl : imageUrls) {
                contentData = new LinkedHashMap();
                contentData.put("type", TYPE_IMAGE);
                String _imageUrl = imageUrl;
                contentData.put("image_url", new HashMap<String, String>() {{

                    put("url", _imageUrl);
                }});
                contents.add(contentData);
            }
        }

        contentData = new LinkedHashMap();
        contentData.put("type", TYPE_TEXT);
        contentData.put("text", message);
        contents.add(contentData);
        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", ROLE_USER);
        userMessage.put("content", contents);
        return userMessage;
    }

    /**
     * 构建图片识别消息，传入识别提示词和图片url清单，构建模型请求参数报文
     * @param message 提示
     * @param imageUrls 图片url
     * @return
     */
    public static Map<String,Object> buildJiuTianInputImagesMessage( String message,String... imageUrls){

        List contents = new ArrayList<>();
        Map contentData = new LinkedHashMap();
        contentData.put("type", TYPE_TEXT);
        contentData.put("text", message);;
        contents.add(contentData);
        if(imageUrls != null && imageUrls.length > 0) {
            for (String imageUrl : imageUrls) {
                contentData = new LinkedHashMap();
                contentData.put("type", TYPE_IMAGE);
                String _imageUrl = imageUrl;
                contentData.put("image_url", _imageUrl);
                contents.add(contentData);
            }
        }

        
        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", ROLE_USER);
        userMessage.put("content", contents);
        return userMessage;
    }

    public static Map<String,Object> buildAudioMessage(AudioDataBuilder audioDataBuilder){
        LinkedHashMap<String, Object> userMessage = new LinkedHashMap<>();
        userMessage.put("role", ROLE_USER);
        List contents = new ArrayList<>();
        Map<String,Object> contentData = new LinkedHashMap<>();
        contentData.put("audio", audioDataBuilder.buildAudioBase64Data());

        contents.add(contentData);
        userMessage.put("content", contents);
        return userMessage;
    }
    public static Map<String,Object> buildUserMessage(String message){


        return buildMessage(ROLE_USER,  message);
    }

    public static Map<String,Object> buildAssistantMessage(String message){


        return buildMessage(ROLE_ASSISTANT,  message);
    }

    public static Map<String,Object> buildAssistantMessage(ServerEvent serverEvent){


        return buildMessage(ROLE_ASSISTANT,    serverEvent);
    }

    public static Map<String,Object> buildAssistantMessage(BaseStreamDataBuilder baseStreamDataBuilder){


        return buildMessage(ROLE_ASSISTANT,      baseStreamDataBuilder);
    }

    

    public static Map<String,Object> buildMessage(String role,String message){

        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", role);
        userMessage.put("content", message);

        return userMessage;
    }

    public static Map<String,Object> buildMessage(String role,ServerEvent serverEvent){

        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", role);
        if(serverEvent.getData() != null) {
            userMessage.put("content", serverEvent.getData());
        }
        if(serverEvent.getReasoningContent() != null){
            userMessage.put("reasoning_content", serverEvent.getReasoningContent());
        }
        if(serverEvent.getToolCalls() != null)
            userMessage.put("tool_calls",serverEvent.getToolCalls());
        return userMessage;
    }

    public static Map<String,Object> buildMessage(String role,BaseStreamDataBuilder baseStreamDataBuilder){
        StreamData streamData = baseStreamDataBuilder.getToolCallsStreamData();
        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", role);
        if(streamData.getContent() != null) {
            userMessage.put("content", streamData.getContent());
        }
        else if(baseStreamDataBuilder.getToolCallContentStreamData() != null){
            userMessage.put("content", baseStreamDataBuilder.getToolCallContentStreamData());
        }
        if(streamData.getReasoningContent() != null){
            userMessage.put("reasoning_content", streamData.getReasoningContent());
        }
        else if(baseStreamDataBuilder.getToolCallThinkingStreamData() != null){
            userMessage.put("reasoning_content", baseStreamDataBuilder.getToolCallThinkingStreamData());
        }
        if(streamData.getToolCalls() != null)
            userMessage.put("tool_calls",streamData.getToolCalls());
        return userMessage;
    }

    

    

    public static Map<String,Object> buildToolMessage(String message,String toolId){

        Map<String, Object> toolMessage = new HashMap<>();
        toolMessage.put("role", ROLE_TOOL);
        toolMessage.put("content", message);
        toolMessage.put("tool_call_id", toolId);
 

        return toolMessage;
    }




}
