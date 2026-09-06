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
import org.frameworkset.spi.ai.context.ChatContext;
import org.frameworkset.spi.ai.model.*;
import org.frameworkset.spi.remote.http.ClientConfiguration;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Minimax系列模型智能体适配器
 * @author biaoping.yin
 * @Date 2026/1/4
 */
public class MiniMaxAgentAdapter extends DoubaoAgentAdapter{
    @Override
    public String getSubmitVideoTaskUrl(ClientConfiguration clientConfiguration, VideoAgentMessage videoAgentMessage) {
        return "/v1/video_generation";
    }

    @Override
    public String getVideoTaskResultUrl(ClientConfiguration clientConfiguration,VideoStoreAgentMessage videoStoreAgentMessage) {
        return "/v1/query/video_generation";
    }

    
    @Override
    public String getImageVLCompletionsUrl(ClientConfiguration clientConfiguration,ImageVLAgentMessage imageVLAgentMessage) {
//        throw new UnsupportedOperationException("getImageVLCompletionsUrl");
        return "/v1/text/chatcompletion_v2";
    }
    @Override
    public String getChatCompletionsUrl(ClientConfiguration clientConfiguration,ChatAgentMessage chatAgentMessage) {
        return "/v1/text/chatcompletion_v2";
    }
    @Override
    public String getGenImageCompletionsUrl(ClientConfiguration clientConfiguration,ImageAgentMessage imageAgentMessage) {
        return "/v1/image_generation";
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
    protected Map<String, Object> buildGenAudioRequestMap(AudioAgentMessage audioAgentMessage, AIAgent aiAgent, ChatContext chatCallback) {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("model", audioAgentMessage.getModel());
        String prompt = getPrompt(  audioAgentMessage,   aiAgent);
        if(chatCallback != null){
            prompt = chatCallback.evalPrompt(prompt);
        }
        requestMap.put("input", prompt);
    
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
    public String getAudioSTTCompletionsUrl(ClientConfiguration clientConfiguration,AudioSTTAgentMessage audioSTTAgentMessage){
        throw new UnsupportedOperationException("getAudioSTTCompletionsUrl");
    }
    @Override
    public String getGenAudioCompletionsUrl(ClientConfiguration clientConfiguration,AudioAgentMessage audioAgentMessage){
        return "/v1/t2a_v2";
    }

    @Override
    public StreamData buildErrorStreamData(Map map, TokenMetrics tokenMetrics) {
        Map baseRsp = (Map) map.get("base_resp");
        String code =String.valueOf(baseRsp.get("status_code"));
        String message = (String) baseRsp.get("status_msg");

        if(code != null) {
            return new StreamData(ServerEvent.CONTENT, message, code).setStreamTokenMetrics(tokenMetrics);
        }
        return null;
    }
	
	
	@Override
	protected void buildThinking(ChatAgentMessage chatAgentMessage, ChatObject chatObject, Map<String, Object> requestMap) {
		Boolean thinking = chatAgentMessage.getThinking();
		ChatContext chatContext = chatObject.getChatContext();
		if(chatContext != null && chatContext.getThinking() != null){
			thinking = chatContext.getThinking();
			
		}
		
		//invalid params, invalid thinking.type: "enabled" (allowed: adaptive, disabled)
		if(thinking != null){
			if( thinking == false) {
				Map data = new LinkedHashMap();
				data.put("type", "disabled");
				requestMap.put("thinking", data);
				chatObject.setThinking(false);
			}
			else{
				Map data = new LinkedHashMap();
				data.put("type", "adaptive");
				requestMap.put("thinking", data);
				chatObject.setThinking(true);
			}
		}
	}
}
