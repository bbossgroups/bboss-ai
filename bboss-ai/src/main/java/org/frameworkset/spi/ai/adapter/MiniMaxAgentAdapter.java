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

import org.frameworkset.spi.ai.model.*;
import org.frameworkset.spi.ai.util.AIResponseUtil;
import org.frameworkset.spi.remote.http.ClientConfiguration;

import java.util.HashMap;
import java.util.Map;

/**
 * Minimax系列模型智能体适配器
 * @author biaoping.yin
 * @Date 2026/1/4
 */
public class MiniMaxAgentAdapter extends DoubaoAgentAdapter{
    @Override
    public String getSubmitVideoTaskUrl(VideoAgentMessage videoAgentMessage) {
        return "/v1/video_generation";
    }

    @Override
    public String getVideoTaskResultUrl(VideoStoreAgentMessage videoStoreAgentMessage) {
        return "/v1/query/video_generation";
    }

    
    @Override
    public String getImageVLCompletionsUrl(ImageVLAgentMessage imageVLAgentMessage) {
        throw new UnsupportedOperationException("getImageVLCompletionsUrl");
    }
    @Override
    public String getChatCompletionsUrl(ChatAgentMessage chatAgentMessage) {
        return "/v1/text/chatcompletion_v2";
    }
    @Override
    public String getGenImageCompletionsUrl(ImageAgentMessage imageAgentMessage) {
        return "/v1/image_generation";
    }
    @Override
    public Boolean getDefaultThinking() {
        return true;
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
    protected Map<String, Object> buildGenAudioRequestMap(AudioAgentMessage audioAgentMessage) {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("model", audioAgentMessage.getModel());
        requestMap.put("input", audioAgentMessage.getPrompt());
    
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
        throw new UnsupportedOperationException("getAudioSTTCompletionsUrl");
    }
    @Override
    public String getGenAudioCompletionsUrl(AudioAgentMessage audioAgentMessage){
        return "/v1/t2a_v2";
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
    
}
