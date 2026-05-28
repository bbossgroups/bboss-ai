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
import org.frameworkset.spi.remote.http.ClientConfiguration;

/**
 * 阿里百炼通义系列模型智能体适配器
 * @author biaoping.yin
 * @Date 2026/1/4
 */
public class XinferenceAgentAdapter extends QwenAgentAdapter{
    @Override
    public String getChatCompletionsUrl(ClientConfiguration clientConfiguration, ChatAgentMessage chatAgentMessage) {
        return "/v1/chat/completions";
    }

    @Override
    public String getEmbeddingUrl(ClientConfiguration clientConfiguration,AgentMessage agentMessage) {
        return "/v1/embeddings";
    }
    @Override
    public String getRerankUrl(ClientConfiguration clientConfiguration,AgentMessage agentMessage) {
        return "/v1/rerank";
    }
    @Override
    public String getSubmitVideoTaskUrl(ClientConfiguration clientConfiguration,VideoAgentMessage videoAgentMessage){
        if(videoAgentMessage.getFirstFrameUrl() != null) {
            return "/api/v1/services/aigc/image2video/video-synthesis";
        }
        else {
            return "/api/v1/services/aigc/video-generation/video-synthesis";
        }
    }
    @Override
    public String getGenAudioCompletionsUrl(ClientConfiguration clientConfiguration,AudioAgentMessage audioAgentMessage){
        return "/api/v1/services/aigc/multimodal-generation/generation";
    }

    /**
     * maas平台音频识别服务地址
     * @param audioSTTAgentMessage
     * @return
     */
    @Override

    public String getAudioSTTCompletionsUrl(ClientConfiguration clientConfiguration,AudioSTTAgentMessage audioSTTAgentMessage){
        return "/api/v1/services/aigc/multimodal-generation/generation";
    }
    @Override
    public String getVideoVLCompletionsUrl(ClientConfiguration clientConfiguration,VideoVLAgentMessage videoVLAgentMessage) {
        return "/v1/chat/completions";
    }
    @Override
    public String getImageVLCompletionsUrl(ClientConfiguration clientConfiguration,ImageVLAgentMessage imageVLAgentMessage) {
        return "/compatible-mode/v1/chat/completions";
    }

    @Override
    public String getGenImageCompletionsUrl(ClientConfiguration clientConfiguration,ImageAgentMessage imageAgentMessage) {
        return "/api/v1/services/aigc/multimodal-generation/generation";
    }
    @Override
    public String getVideoTaskResultUrl(ClientConfiguration clientConfiguration,VideoStoreAgentMessage videoStoreAgentMessage){
        return "/api/v1/tasks/"+videoStoreAgentMessage.getTaskId();
    }
   
}
