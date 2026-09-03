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

import org.frameworkset.spi.ai.AIAgent;
import org.frameworkset.spi.ai.model.*;
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
    public static LinkedMessageMap<String,Object> buildSystemMessage(String message){


        return buildMessage(ROLE_SYSTEM,  message);
    }
    public static LinkedMessageMap<String,Object> buildAudioSystemMessage(String message){
        // 添加当前用户消息
 
        return buildAudioUserMessage(  message,  ROLE_SYSTEM);
    }

    public static LinkedMessageMap<String,Object> buildAudioUserMessage(String message){
        // 添加当前用户消息
 
        return buildAudioUserMessage(  message,  ROLE_USER);
    }

    public static LinkedMessageMap<String,Object> buildAudioUserMessage(String message,String role){
        // 添加当前用户消息
		LinkedMessageMap<String, Object> userMessage = new LinkedMessageMap<>();
        userMessage.put("role", role);
        List<Map> contents = new ArrayList<>();
        Map contentData = new LinkedHashMap();
        contentData.put("text", message);
        contents.add(contentData);
        userMessage.put("content", contents);
        return userMessage;
    }

    public static LinkedMessageMap<String,Object> buildAudioMessage(String audioUrl){
		LinkedMessageMap<String, Object> userMessage = new LinkedMessageMap<>();
        userMessage.put("role", ROLE_USER);
        List contents = new ArrayList<>();
        Map<String,Object> contentData = new LinkedHashMap<>();
        contentData.put("audio", audioUrl);

        contents.add(contentData);
        userMessage.put("content", contents);
        return userMessage;
    }

    public static  String getSystemPrompt(AgentMessage agentMessage, AIAgent aiAgent){
        return aiAgent.getSystemPrompt() != null ? aiAgent.getSystemPrompt():agentMessage.getSystemPrompt();
    }
    public static  String getPrompt(AgentMessage agentMessage, AIAgent aiAgent){
        return aiAgent.getPrompt() != null ? aiAgent.getPrompt():agentMessage.getPrompt();
    }
    public static void buildZhipuGenVideoMessage(Map<String, Object> requestMap, VideoAgentMessage videoAgentMessage, AIAgent aiAgent){
        requestMap.put("prompt", getPrompt(  videoAgentMessage,   aiAgent));

        List contents = new ArrayList<>();
        Map contentData = null;

        if(videoAgentMessage.getImgUrl() != null){
            requestMap.put("image_url",videoAgentMessage.getImgUrl());
           

        }
        
        if(videoAgentMessage.getFirstFrameUrl() != null && videoAgentMessage.getLastFrameUrl() != null){
            requestMap.put("image_url",new String[]{videoAgentMessage.getFirstFrameUrl(),videoAgentMessage.getLastFrameUrl()});

        }       
    }

    /**
     * curl https://ark.cn-beijing.volces.com/api/v3/contents/generations/tasks \
     *   -H "Content-Type: application/json" \
     *   -H "Authorization: Bearer $ARK_API_KEY" \
     *   -d '{
     *     "model": "doubao-seedance-2-0-260128",
     *     "content": [
     *          {
     *             "type": "text",
     *             "text": "全程使用视频1的第一视角构图，全程使用音频1作为背景音乐。第一人称视角果茶宣传广告，seedance牌「苹苹安安」苹果果茶限定款；首帧为图片1，你的手摘下一颗带晨露的阿克苏红苹果，轻脆的苹果碰撞声；2-4 秒：快速切镜，你的手将苹果块投入雪克杯，加入冰块与茶底，用力摇晃，冰块碰撞声与摇晃声卡点轻快鼓点，背景音：「鲜切现摇」；4-6 秒：第一人称成品特写，分层果茶倒入透明杯，你的手轻挤奶盖在顶部铺展，在杯身贴上粉红包标，镜头拉近看奶盖与果茶的分层纹理；6-8 秒：第一人称手持举杯，你将图片2中的果茶举到镜头前（模拟递到观众面前的视角），杯身标签清晰可见，背景音「来一口鲜爽」，尾帧定格为图片2。背景声音统一为女生音色。"
     *         },
     *         {
     *             "type": "image_url",
     *             "image_url": {
     *                 "url": "https://ark-project.tos-cn-beijing.volces.com/doc_image/r2v_tea_pic1.jpg"
     *             },
     *             "role": "reference_image"
     *         },
     *         {
     *             "type": "image_url",
     *             "image_url": {
     *                 "url": "https://ark-project.tos-cn-beijing.volces.com/doc_image/r2v_tea_pic2.jpg"
     *             },
     *             "role": "reference_image"
     *         },
     *         {
     *           "type": "video_url",
     *           "video_url": {
     *               "url": "https://ark-project.tos-cn-beijing.volces.com/doc_video/r2v_tea_video1.mp4"
     *           },
     *           "role": "reference_video"
     *         },
     *         {
     *           "type": "audio_url",
     *           "audio_url": {
     *               "url": "https://ark-project.tos-cn-beijing.volces.com/doc_audio/r2v_tea_audio1.mp3"
     *           },
     *           "role": "reference_audio"
     *         }
     *     ],
     *     "generate_audio":true,
     *     "ratio": "16:9",
     *     "duration": 11,
     *     "watermark": false
     * }'
     * @param requestMap
     * @param videoAgentMessage
     * @param aiAgent
     */
    public static void buildDoubaoGenVideoMessage(Map<String, Object> requestMap, VideoAgentMessage videoAgentMessage,AIAgent aiAgent){


        List contents = new ArrayList<>();
        Map contentData = new LinkedHashMap();
        contentData.put("text", getPrompt(  videoAgentMessage,   aiAgent));
        contentData.put("type", "text");
        contents.add(contentData);

        if(videoAgentMessage.getImgUrl() != null){
            contentData = new LinkedHashMap();
            Map imageData = new LinkedHashMap();
            imageData.put("url", videoAgentMessage.getImgUrl());
            contentData.put("image_url", imageData);
            contentData.put("type", "image_url");
            contentData.put("role", "reference_image");
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

        if(videoAgentMessage.getVideoUrl() != null){
            contentData = new LinkedHashMap();
            Map videoData = new LinkedHashMap();
            videoData.put("url", videoAgentMessage.getVideoUrl());
            contentData.put("video_url", videoData);
            contentData.put("type", "video_url");
            contentData.put("role", "reference_video");
            contents.add(contentData);
        }

        if(videoAgentMessage.getAudioUrl() != null){
            contentData = new LinkedHashMap();
            Map audioData = new LinkedHashMap();
            audioData.put("url", videoAgentMessage.getAudioUrl());
            contentData.put("audio_url", audioData);
            contentData.put("type", "audio_url");
            contentData.put("role", "reference_audio");
            contents.add(contentData);
        }
        requestMap.put("content", contents);
    }
    public static void buildGenVideoMessage(Map<String, Object> requestMap, VideoAgentMessage videoAgentMessage,AIAgent aiAgent){
        

        List contents = new ArrayList<>();
        Map contentData = new LinkedHashMap();
        contentData.put("text", getPrompt(  videoAgentMessage,   aiAgent));
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
		LinkedMessageMap<String, Object> userMessage = new LinkedMessageMap<>();
        userMessage.put("role",ROLE_USER);

        List contents = new ArrayList<>();
        Map contentData = new LinkedHashMap();
        contentData.put("text", message);

        contents.add(contentData);
        userMessage.put("content", contents);
        return userMessage;
    }

    public static LinkedMessageMap<String,Object> buildGenImageMessage(ImageAgentMessage imageAgentMessage,AIAgent aiAgent){
		LinkedMessageMap<String, Object> userMessage = new LinkedMessageMap<>();
        userMessage.put("role",ROLE_USER);

        List contents = new ArrayList<>();
        Map contentData = new LinkedHashMap();
        contentData.put("text", getPrompt(  imageAgentMessage,   aiAgent));
        
        

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
    public static LinkedMessageMap<String,Object> buildInputVideosMessage( String message,String... videoUrls){

        List contents = new ArrayList<>();
        Map contentData = null;
        contentData = new LinkedHashMap();
        contentData.put("type", TYPE_TEXT);
        contentData.put("text", message);;
        contents.add(contentData);
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

  
		LinkedMessageMap<String, Object> userMessage = new LinkedMessageMap<>();
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
    public static LinkedMessageMap<String,Object> buildInputImagesMessage( String message,String... imageUrls){

        List contents = new ArrayList<>();
        Map contentData = null;
        contentData = new LinkedHashMap();
        contentData.put("type", TYPE_TEXT);
        contentData.put("text", message);
        contents.add(contentData);
        
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

        
		LinkedMessageMap<String, Object> userMessage = new LinkedMessageMap<>();
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
    public static LinkedMessageMap<String,Object> buildJiuTianInputImagesMessage( String message,String... imageUrls){

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
		
		
		LinkedMessageMap<String, Object> userMessage = new LinkedMessageMap<>();
        userMessage.put("role", ROLE_USER);
        userMessage.put("content", contents);
        return userMessage;
    }

    public static LinkedMessageMap<String,Object> buildAudioMessage(AudioDataBuilder audioDataBuilder){
		LinkedMessageMap<String, Object> userMessage = new LinkedMessageMap<>();
        userMessage.put("role", ROLE_USER);
        List contents = new ArrayList<>();
        Map<String,Object> contentData = new LinkedHashMap<>();
        contentData.put("audio", audioDataBuilder.buildAudioBase64Data());

        contents.add(contentData);
        userMessage.put("content", contents);
        return userMessage;
    }
    public static LinkedMessageMap<String,Object> buildUserMessage(String message){


        return buildMessage(ROLE_USER,  message);
    }

    public static LinkedMessageMap<String,Object> buildAssistantMessage(String message){


        return buildMessage(ROLE_ASSISTANT,  message);
    }

    public static LinkedMessageMap<String,Object> buildAssistantMessage(ServerEvent serverEvent){


        return buildMessage(ROLE_ASSISTANT,    serverEvent);
    }

    public static LinkedMessageMap<String,Object> buildAssistantMessage(BaseStreamDataBuilder baseStreamDataBuilder){


        return buildMessage(ROLE_ASSISTANT,      baseStreamDataBuilder);
    }

    

    public static LinkedMessageMap<String,Object> buildMessage(String role,String message){
		
		LinkedMessageMap<String, Object> userMessage = new LinkedMessageMap<>();
        userMessage.put("role", role);
        userMessage.put("content", message);

        return userMessage;
    }

    public static LinkedMessageMap<String,Object> buildMessage(String role,ServerEvent serverEvent){
		
		LinkedMessageMap<String, Object> userMessage = new LinkedMessageMap<>();
        userMessage.put("role", role);
        if(serverEvent.getData() != null) {
            userMessage.put("content", serverEvent.getData());
        }
//        if(serverEvent.getReasoningContent() != null){
//            userMessage.put("reasoning_content", serverEvent.getReasoningContent());
//        }
        if(serverEvent.getToolCalls() != null)
            userMessage.put("tool_calls",serverEvent.getToolCalls());
        return userMessage;
    }

    public static LinkedMessageMap<String,Object> buildMessage(String role,BaseStreamDataBuilder baseStreamDataBuilder){
        StreamData streamData = baseStreamDataBuilder.getToolCallsStreamData();
		LinkedMessageMap<String, Object> userMessage = new LinkedMessageMap<>();
        userMessage.put("role", role);
        if(streamData.getContent() != null) {
            userMessage.put("content", streamData.getContent());
        }
        else if(baseStreamDataBuilder.getToolCallContentStreamData() != null){
            userMessage.put("content", baseStreamDataBuilder.getToolCallContentStreamData());
        }
//        if(streamData.getReasoningContent() != null){
//            userMessage.put("reasoning_content", streamData.getReasoningContent());
//        }
//        else if(baseStreamDataBuilder.getToolCallThinkingStreamData() != null){
//            userMessage.put("reasoning_content", baseStreamDataBuilder.getToolCallThinkingStreamData());
//        }
        if(streamData.getToolCalls() != null) {
			
			userMessage.put("tool_calls", streamData.getToolCalls());
		}
        return userMessage;
    }

    

    

    /**
     * 构建工具调用结果消息，传入工具返回消息和工具调用id，构建模型请求参数报文
     * @param message
     * @param toolId
     * @return
     */
    public static LinkedMessageMap<String,Object> buildToolMessage(String message,String toolId,FunctionTool tool){
		
		LinkedMessageMap<String, Object> toolMessage = new LinkedMessageMap<>();
		toolMessage.setName(tool.getFunctionName());
        toolMessage.put("role", ROLE_TOOL);
        toolMessage.put("content", message);
        toolMessage.put("tool_call_id", toolId);
 

        return toolMessage;
    }




}
