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

import org.frameworkset.spi.ai.AIAgent;
import org.frameworkset.spi.ai.model.*;
import org.frameworkset.spi.ai.util.AIResponseUtil;
import org.frameworkset.spi.ai.util.MessageBuilder;
import org.frameworkset.spi.ai.util.StreamDataBuilder;
import org.frameworkset.spi.remote.http.ClientConfiguration;

import java.io.File;
import java.util.*;

/**
 * Zhipu模型智能体适配器
 * @author biaoping.yin
 * @Date 2026/1/4
 */
public class ZhipuAgentAdapter extends DoubaoAgentAdapter{
    @Override
    public String getImageVLCompletionsUrl(ImageVLAgentMessage imageVLAgentMessage) {
        return "/api/paas/v4/chat/completions";
    }
    @Override
    public String getChatCompletionsUrl(ChatAgentMessage chatAgentMessage) {
        return "/api/paas/v4/chat/completions";
    }
    @Override
    public String getGenImageCompletionsUrl(ImageAgentMessage imageAgentMessage) {
        return "/api/paas/v4/images/generations";
    }

    @Override
    public String getSubmitVideoTaskUrl(VideoAgentMessage videoAgentMessage) {
        return "/api/paas/v4/videos/generations";
    }

    @Override
    public String getVideoTaskResultUrl(VideoStoreAgentMessage videoStoreAgentMessage) {
        return "https://open.bigmodel.cn/api/paas/v4/async-result/"+videoStoreAgentMessage.getTaskId();
    }

    @Override
    public Boolean getDefaultThinking() {
        return false;
    }

    @Override
    /**
     * https://docs.bigmodel.cn/cn/guide/models/sound-and-video/glm-tts#%E5%8D%95%E9%9F%B3%E8%89%B2%E8%B6%85%E6%8B%9F%E4%BA%BAtts
     * 
     * curl -X POST "https://open.bigmodel.cn/api/paas/v4/audio/speech" \
     *     -H "Authorization: Bearer API Key" \
     *     -H "Content-Type: application/json" \
     *     -d '{
     *           "model": "glm-tts",
     *           "input": "你好呀,欢迎来到智谱开放平台",
     *           "voice": "female",
     *           "response_format": "pcm",
     *           "encode_format": "base64",
     *           "stream": true,
     *           "speed": 1.0,
     *           "volume": 1.0
     *     }' \
     */
    protected Map<String, Object> buildGenAudioRequestMap(AudioAgentMessage audioAgentMessage,AIAgent aiAgent) {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("model", audioAgentMessage.getModel());
        requestMap.put("input", getPrompt(  audioAgentMessage,   aiAgent));
    
        if(audioAgentMessage.getParameters() != null && audioAgentMessage.getParameters().size() > 0){
            requestMap.putAll(audioAgentMessage.getParameters());
        }
        return requestMap;
    }

    /**
     * maas平台音频识别服务地址
     * @param audioSTTAgentMessage
     * @return
     */
    @Override
    public String getAudioSTTCompletionsUrl(AudioSTTAgentMessage audioSTTAgentMessage){
        return "/api/paas/v4/audio/transcriptions";
    }
    @Override
    public String getGenAudioCompletionsUrl(AudioAgentMessage audioAgentMessage){
        return "/api/paas/v4/audio/speech";
    }

    /**
     * 处理音频识别流数据
     * {"id":"2026012618501535d155fd2f884b93","created":1769424615,"model":"glm-tts",
     * "choices":[{"index":0,"delta":{"role":"assistant","content":"","return_sample_rate":24000,"return_format":"pcm"}}]}
     * @param data
     * @return
     */
    @Override
    public StreamData parseAudioGenStreamContentFromData(String data){
        return AIResponseUtil.parseZhipuAudioGenStreamContentFromData(data);
    }

    public AudioEvent buildGenAudioResponse(ClientConfiguration config, AudioAgentMessage message,StoreChatObject storeChatObject, Map data){
        Map output = (Map)data.get("output");
        Map audio = (Map)output.get("audio");
        String finishReason = (String)output.get("finish_reason");

        if(audio == null && finishReason == null)
            return null;
        AudioEvent audioEvent = new AudioEvent();
        audioEvent.setFinishReason(finishReason);
        String audioUrl = (String)audio.get("url");
        String auditData = (String)audio.get("data");
        Object expiresAt_ = audio.get("expires_at");
        if(expiresAt_ != null) {
            if (expiresAt_ instanceof Long) {
                audioEvent.setExpiresAt((Long) expiresAt_);
            } else {
                audioEvent.setExpiresAt((Integer) expiresAt_);
            }
        }
        audioEvent.setAudioBase64(auditData);

        if(audioUrl != null) {
            audioEvent.setGenAudioUrl(audioUrl);
            audioEvent.setAudioUrl(genFileDownload.downloadAudio(config, message,  storeChatObject, null, audioUrl));
        }
        return audioEvent;
    }
    /**
     * 获取音频识别模型智能问答请求参数类型
     * @return
     */
    public String getAIAudioParsertRequestType(){
        return AIConstants.AI_CHAT_REQUEST_POST_FORM;

    }

    /**
     * 解析语音识别流数据
     * @param data
     * @return
     */
    public StreamData parseAudioStreamContentFromData(StreamDataBuilder streamDataBuilder, String data){
        return AIResponseUtil.parseZhipuAudioStreamContentFromData(  streamDataBuilder,data);
    }
    @Override
    public Map buildAudioSTTRequestMap(AudioSTTAgentMessage audioSTTAgentMessage, AIAgent aiAgent) {

        String agentId = aiAgent.getAgentId();
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("model", audioSTTAgentMessage.getModel());
        requestMap.put("prompt", getPrompt(  audioSTTAgentMessage,   aiAgent));
        
        Object audio = audioSTTAgentMessage.getAudio();
        // 添加当前用户消息
        Map<String, Object> userMessage = null;
        if(audio != null) {
            userMessage = MessageBuilder.buildAudioSystemMessage(getPrompt(  audioSTTAgentMessage,   aiAgent));
        }
        else{
            userMessage = MessageBuilder.buildAudioUserMessage(getPrompt(  audioSTTAgentMessage,   aiAgent));
        }
        
        audioSTTAgentMessage.addSessionMessage(userMessage,aiAgent);

        if(audio != null) {
            if(audio instanceof File){
                Map<String,File> files = new LinkedHashMap<>();
                files.put("file",(File)audio);
                audioSTTAgentMessage.setFiles( files);
            }
            else if (audio instanceof byte[]) {
                requestMap.put("file_base64","data:" + audioSTTAgentMessage.getContentType() + ";base64," +
                        Base64.getEncoder().encodeToString((byte[]) audio));
            } else if (audio instanceof String) {
                requestMap.put("file_base64",audio);
            }
            else{
                throw new AIRuntimeException("audio must be File or byte[] or String");
            }
        }
        
        Map parameters = audioSTTAgentMessage.getParameters();
        if(parameters != null) {
            requestMap.putAll( parameters);
        }
        if(audioSTTAgentMessage.getStream() != null){
            requestMap.put("stream", audioSTTAgentMessage.getStream());
        }
         
        return requestMap;
    }

    @Override
    protected Object buildGenVideoRequestMap(VideoAgentMessage videoAgentMessage, ClientConfiguration clientConfiguration,AIAgent aiAgent) {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("model",videoAgentMessage.getModel());

        MessageBuilder.buildZhipuGenVideoMessage(requestMap,videoAgentMessage,aiAgent);

        Map<String,Object> parameters = videoAgentMessage.getParameters();
        if(parameters != null){
            requestMap.putAll(parameters);
        }


        return requestMap;
    }

    @Override
    public VideoTask buildVideoResponseTask(ClientConfiguration clientConfiguration, VideoAgentMessage videoAgentMessage, Map taskInfo) {
        VideoTask result = new VideoTask();
        if(taskInfo != null) {
            result.setTaskId((String) taskInfo.get("id"));
            result.setTaskStatus((String)taskInfo.get("task_status"));

        }
        else {
            Map error = (Map) taskInfo.get("error");
            if(error != null) {
                result.setCode((String) error.get("code"));
                result.setMessage((String) error.get("message"));
            }
        }

        return result;
    }

    /**
     * {
     *   "id": "<string>",
     *   "request_id": "<string>",
     *   "created": 123,
     *   "model": "<string>",
     *   "choices": [
     *     {
     *       "index": 123,
     *       "message": {
     *         "role": "assistant",
     *         "content": "<string>",
     *         "reasoning_content": "<string>",
     *         "audio": {
     *           "id": "<string>",
     *           "data": "<string>",
     *           "expires_at": "<string>"
     *         },
     *         "tool_calls": [
     *           {
     *             "function": {
     *               "name": "<string>",
     *               "arguments": "<string>"
     *             },
     *             "mcp": {
     *               "id": "<string>",
     *               "type": "mcp_list_tools",
     *               "server_label": "<string>",
     *               "error": "<string>",
     *               "tools": [
     *                 {
     *                   "name": "<string>",
     *                   "description": "<string>",
     *                   "annotations": {},
     *                   "input_schema": {
     *                     "type": "object",
     *                     "properties": {},
     *                     "required": [
     *                       "<string>"
     *                     ],
     *                     "additionalProperties": true
     *                   }
     *                 }
     *               ],
     *               "arguments": "<string>",
     *               "name": "<string>",
     *               "output": {}
     *             },
     *             "id": "<string>",
     *             "type": "<string>"
     *           }
     *         ]
     *       },
     *       "finish_reason": "<string>"
     *     }
     *   ],
     *   "usage": {
     *     "prompt_tokens": 123,
     *     "completion_tokens": 123,
     *     "prompt_tokens_details": {
     *       "cached_tokens": 123
     *     },
     *     "total_tokens": 123
     *   },
     *   "video_result": [
     *     {
     *       "url": "<string>",
     *       "cover_image_url": "<string>"
     *     }
     *   ],
     *   "web_search": [
     *     {
     *       "icon": "<string>",
     *       "title": "<string>",
     *       "link": "<string>",
     *       "media": "<string>",
     *       "publish_date": "<string>",
     *       "content": "<string>",
     *       "refer": "<string>"
     *     }
     *   ],
     *   "content_filter": [
     *     {
     *       "role": "<string>",
     *       "level": 123
     *     }
     *   ]
     * }
     *
     * @param clientConfiguration
     * @param videoStoreAgentMessage
     * @param taskInfo
     * @return
     */
    @Override
    public VideoGenResult buildVideoGenResult(ClientConfiguration clientConfiguration, VideoStoreAgentMessage videoStoreAgentMessage,StoreChatObject storeChatObject, Map taskInfo) {
        VideoGenResult result = new VideoGenResult();

        if(taskInfo != null) {

            result.setTaskId((String) taskInfo.get("id"));
            result.setTaskStatus((String) taskInfo.get("task_status"));
            List<Map> video_result = (List<Map>) taskInfo.get("video_result");
           
            if(video_result != null && video_result.size() > 0){
                Map content = video_result.get(0);
                String videoGenUrl = (String) content.get("url");
                result.setVideoGenUrl(videoGenUrl);

                String coverImageGenUrl = (String) content.get("cover_image_url");
                result.setCoverImageGenUrl(coverImageGenUrl);
                
                if(videoGenUrl != null && videoGenUrl.length() > 0) {
                    result.setVideoUrl(genFileDownload.downloadVideo(clientConfiguration, videoStoreAgentMessage,storeChatObject, null, videoGenUrl));
                }
                
                if(coverImageGenUrl != null && coverImageGenUrl.length() > 0) {
                    result.setCoverImageUrl(genFileDownload.downloadVideoImage(clientConfiguration, videoStoreAgentMessage,storeChatObject,  coverImageGenUrl));
                }
            }

            Map error = (Map) taskInfo.get("error");

            if(error != null) {
                result.setCode((String) error.get("code"));
                result.setMessage((String) error.get("message"));
            }
        }


        return result;
    }
}
