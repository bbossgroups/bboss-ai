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
 * Minimax系列模型智能体适配器
 * @author biaoping.yin
 * @Date 2026/1/4
 */
public class TencentHYAgentAdapter extends DoubaoAgentAdapter{
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
        throw new UnsupportedOperationException("getAudioSTTCompletionsUrl");
    }
 
 
  
    
}
