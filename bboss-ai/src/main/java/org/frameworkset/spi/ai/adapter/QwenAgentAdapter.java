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
import org.frameworkset.spi.ai.model.*;
import org.frameworkset.spi.ai.util.AIResponseUtil;
import org.frameworkset.spi.ai.util.MessageBuilder;
import org.frameworkset.spi.reactor.SSEHeaderSetFunction;
import org.frameworkset.spi.remote.http.ClientConfiguration;

import java.util.*;

/**
 * 阿里百炼通义系列模型智能体适配器
 * @author biaoping.yin
 * @Date 2026/1/4
 */
public class QwenAgentAdapter extends AgentAdapter{
    public String getVideoVLCompletionsUrl(VideoVLAgentMessage videoVLAgentMessage) {
        return "/v1/chat/completions";
    }
    @Override
    public String getImageVLCompletionsUrl(ImageVLAgentMessage imageVLAgentMessage) {
        return "/compatible-mode/v1/chat/completions";
    }

    @Override
    public String getGenImageCompletionsUrl(ImageAgentMessage imageAgentMessage) {
        return "/api/v1/services/aigc/multimodal-generation/generation";
    }
    @Override
    public String getVideoTaskResultUrl(VideoStoreAgentMessage videoStoreAgentMessage){
        return "/api/v1/tasks/"+videoStoreAgentMessage.getTaskId();
    }
    public AudioEvent buildGenAudioResponse(ClientConfiguration config, AudioAgentMessage message, Map data){
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
            audioEvent.setAudioUrl(genFileDownload.downloadAudio(config, message, null, audioUrl));
        }
        return audioEvent;
    }
    public VideoGenResult buildVideoGenResult(ClientConfiguration clientConfiguration,VideoStoreAgentMessage videoStoreAgentMessage,Map taskInfo) {
        VideoGenResult result = new VideoGenResult();
        Map output = (Map)taskInfo.get("output");
        if(output != null) {
            result.setTaskId((String) output.get("task_id"));
            result.setTaskStatus((String) output.get("task_status"));
            result.setVideoGenUrl((String) output.get("video_url"));
            if(result.getVideoGenUrl() != null && result.getVideoGenUrl().length() > 0) {
                result.setVideoUrl(genFileDownload.downloadVideo(clientConfiguration, videoStoreAgentMessage, null, result.getVideoGenUrl()));
            }
            result.setSubmitTime((String) output.get("submit_time"));
            result.setScheduledTime((String) output.get("scheduled_time"));
            result.setEndTime((String) output.get("end_time"));
            result.setOrigPrompt((String) output.get("orig_prompt"));
//            "submit_time": "2025-09-29 14:18:52.331",
//                    "scheduled_time": "2025-09-29 14:18:59.290",
//                    "end_time": "2025-09-29 14:23:39.407",
//                    "orig_prompt": "一幅史诗级可爱的场景。一只小巧可爱的卡通小猫将军，身穿细节精致的金色盔甲，头戴一个稍大的头盔，勇敢地站在悬崖上。他骑着一匹虽小但英勇的战马，说：”青海长云暗雪山，孤城遥望玉门关。黄沙百战穿金甲，不破楼兰终不还。“。悬崖下方，一支由老鼠组成的、数量庞大、无穷无尽的军队正带着临时制作的武器向前冲锋。这是一个戏剧性的、大规模的战斗场景，灵感来自中国古代的战争史诗。远处的雪山上空，天空乌云密布。整体氛围是“可爱”与“霸气”的搞笑和史诗般的融合。",

       
            result.setCode((String) output.get("code"));
            result.setMessage((String) output.get("message"));
        }
        result.setRequestId((String) taskInfo.get("request_id"));
//        result.put("taskId",output.get("task_id"));
//        result.put("taskStatus",output.get("task_status"));
//        result.put("videoUrl",output.get("video_url"));
//        result.put("requestId",taskInfo.get("request_id"));
        return result;
    }

    @Override
    public String getChatCompletionsUrl(ChatAgentMessage chatAgentMessage) {
        return "/compatible-mode/v1/chat/completions";
    }

    public VideoTask buildVideoResponseTask(ClientConfiguration clientConfiguration, VideoAgentMessage videoAgentMessage,Map taskInfo ){
        Map output = (Map)taskInfo.get("output");
        VideoTask result = new VideoTask();
        if(output != null) {
            result.setTaskId((String) output.get("task_id"));
            result.setTaskStatus((String) output.get("task_status"));
            
        }
        else {
            result.setCode((String) taskInfo.get("code"));
            result.setMessage((String) taskInfo.get("message"));
        }
        result.setRequestId((String) taskInfo.get("request_id"));
        return result;
    }
    public String getSubmitVideoTaskUrl(VideoAgentMessage videoAgentMessage){
        if(videoAgentMessage.getFirstFrameUrl() != null) {
            return "/api/v1/services/aigc/image2video/video-synthesis";
        }
        else {
            return "/api/v1/services/aigc/video-generation/video-synthesis";
        }
    }
    @Override
    protected Object buildGenVideoRequestMap(VideoAgentMessage videoAgentMessage, ClientConfiguration clientConfiguration) {
        
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("model",videoAgentMessage.getModel());




        Map<String,Object> inputVoice = new LinkedHashMap();
        inputVoice.put("prompt",videoAgentMessage.getPrompt());
        if(videoAgentMessage.getAudioUrl() != null){
            inputVoice.put("audio_url",videoAgentMessage.getAudioUrl());
        }
        if(videoAgentMessage.getImgUrl() != null){
            inputVoice.put("img_url",videoAgentMessage.getImgUrl());
        }
        if(videoAgentMessage.getFirstFrameUrl() != null){
            inputVoice.put("first_frame_url",videoAgentMessage.getFirstFrameUrl());
        }
        
        if(videoAgentMessage.getLastFrameUrl() != null){
            inputVoice.put("last_frame_url",videoAgentMessage.getLastFrameUrl());
        }
        if(videoAgentMessage.getTemplate() != null){
            inputVoice.put("template",videoAgentMessage.getTemplate());
        }
        if (videoAgentMessage.getNegativePrompt() != null){
            inputVoice.put("negative_prompt",videoAgentMessage.getNegativePrompt());
        }
//        inputVoice.put("audio_url","https://help-static-aliyun-doc.aliyuncs.com/file-manage-files/zh-CN/20250923/hbiayh/%E4%BB%8E%E5%86%9B%E8%A1%8C.mp3");
        if(videoAgentMessage.getLanguageType() != null) {
            inputVoice.put("language_type", videoAgentMessage.getLanguageType());
        }

        requestMap.put("input",inputVoice);

        /**
         * "parameters": {
         *         "size": "832*480",
         *         "prompt_extend": true,
         *         "duration": 10,
         *         "audio": true
         *     }
         */
        Map<String,Object> parameters = videoAgentMessage.getParameters();
//        parameters.put("size","832*480");
//        parameters.put("prompt_extend",true);
//        parameters.put("duration",10);
//        parameters.put("audio",true);
        if(parameters != null) {

            requestMap.put("parameters", parameters);
        }
        if(!videoAgentMessage.containsHeader("X-DashScope-Async")){
            videoAgentMessage.addHeader("X-DashScope-Async", "enable");
        }
        return requestMap;
    }


    protected Map<String, Object> buildGetVideoResultRquestMap(VideoStoreAgentMessage videoStoreAgentMessage){

        String requestUrl = getVideoTaskResultUrl(videoStoreAgentMessage);
        videoStoreAgentMessage.setVideoTaskResultUrl(requestUrl);
        return null;
    }

    public SSEHeaderSetFunction getAudioGenSSEHeaderSetFunction(){
        return new SSEHeaderSetFunction() {
            @Override
            public void setSSEHeaders(Map headers) {

                headers.put("X-DashScope-SSE", "enable");
            }
        };
    }

    /**
     * 处理音频识别流数据
     * {"output":{"audio":{"data":"xxxx",
     *   "expires_at":1769158890,
     *   "id":"audio_66356352-8808-49bd-9c9c-d0283a3e2eb1"},
     *   "finish_reason":"null"},
     *   "usage":{"characters":53},
     *   "request_id":"66356352-8808-49bd-9c9c-d0283a3e2eb1"}
     * @param data
     * @return
     */
    @Override
    public StreamData parseAudioGenStreamContentFromData(String data){
        return AIResponseUtil.parseQianwenAudioGenStreamContentFromData(data);
    }
    @Override
    public String getGenAudioCompletionsUrl(AudioAgentMessage audioAgentMessage){
        return "/api/v1/services/aigc/multimodal-generation/generation";
    }

    /**
     * maas平台音频识别服务地址
     * @param audioSTTAgentMessage
     * @return
     */
    @Override

    public String getAudioSTTCompletionsUrl(AudioSTTAgentMessage audioSTTAgentMessage){
        return "/api/v1/services/aigc/multimodal-generation/generation";
    }
    @Override
    protected Map<String, Object> buildGenAudioRequestMap(AudioAgentMessage audioAgentMessage) {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("model", audioAgentMessage.getModel());




        Map<String,Object> inputVoice = new LinkedHashMap();
        inputVoice.put("text",audioAgentMessage.getPrompt());
        if(audioAgentMessage.getParameters() != null){
            inputVoice.putAll(audioAgentMessage.getParameters());
        }
        else{
            inputVoice.put("voice","Cherry");
            inputVoice.put("language_type","Chinese");
        }
        

        requestMap.put("input",inputVoice);
        return requestMap;
    }

    @Override
    protected Map buildGenImageRequestMap(ImageAgentMessage imageAgentMessage) {

        Map<String, Object> requestMap = new HashMap<>();

        requestMap.put("model", imageAgentMessage.getModel());


        Map<String,Object> input = new LinkedHashMap<>();


        // 构建消息历史列表，包含之前的会话记忆

        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> userMessage = MessageBuilder.buildGenImageMessage(imageAgentMessage);
        messages.add(userMessage);
        input.put("messages", messages);
        requestMap.put("input", input);

        // enable_thinking 参数开启思考过程，thinking_budget 参数设置最大推理过程 Token 数
//        Map parameters = new LinkedHashMap();
//        parameters.put("negative_prompt","");
//        parameters.put("prompt_extend",true);
//        parameters.put("watermark",false);
//        parameters.put("size","1328*1328");
        Map parameters = imageAgentMessage.getParameters();
        if(SimpleStringUtil.isEmpty( parameters)){
            // enable_thinking 参数开启思考过程，thinking_budget 参数设置最大推理过程 Token 数
//            parameters = new LinkedHashMap();
//            parameters.put("negative_prompt","");
//            parameters.put("prompt_extend",true);
//            parameters.put("watermark",false);
//            parameters.put("size","1328*1328");
        }
        else{
            requestMap.put("parameters", parameters);
        }
        
       
        
        return requestMap;
    }
    public ImageEvent buildGenImageResponse(ClientConfiguration config,ImageAgentMessage imageAgentMessage, Map imageData){
        Map output = (Map)imageData.get("output");
        List choices = (List)output.get("choices");
        if(choices == null || choices.size() == 0)
            return null;
        Map choice = (Map)choices.get(0);
        Map messageData = (Map)choice.get("message");

        String finishReason = (String)choice.get("finish_reason");
        List imageContentData = (List)messageData.get("content");
        ImageEvent imageEvent = null;
        if(imageContentData != null ){
            int size = imageContentData.size();
            if(size > 0) {
                imageEvent = new ImageEvent();
                if(size == 1) {
                    Map image = (Map) imageContentData.get(0);
                    String imageUrl = (String) image.get("image");

                    imageEvent.setGenImageUrl(imageUrl);
                    imageEvent.setImageUrl(genFileDownload.downloadImage(config,imageAgentMessage,null,imageUrl));
                }
                else{
                    for(int i = 0; i < size; i++){
                        Map image = (Map) imageContentData.get(i);
                        String imageUrl = (String) image.get("image");
                        imageEvent.addGenImageUrl(imageUrl);
                        imageEvent.addImageUrl(genFileDownload.downloadImage(config,  imageAgentMessage,null,imageUrl));
                    }
                }
                imageEvent.setFinishReason(finishReason);
            }
        }
        return imageEvent;
    }


}
