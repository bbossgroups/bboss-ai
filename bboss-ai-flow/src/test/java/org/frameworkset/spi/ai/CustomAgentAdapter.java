package org.frameworkset.spi.ai;
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

import org.frameworkset.spi.ai.adapter.QwenAgentAdapter;
import org.frameworkset.spi.ai.model.*;

/**
 * @author biaoping.yin
 * @Date 2026/3/26
 */
public class CustomAgentAdapter extends QwenAgentAdapter {
    @Override
    public String getChatCompletionsUrl(ChatAgentMessage chatAgentMessage) {
        return "/compatible-mode/v1/chat/completions";
    }
    @Override
    public String getSubmitVideoTaskUrl(VideoAgentMessage videoAgentMessage){
        if(videoAgentMessage.getFirstFrameUrl() != null) {
            return "/api/v1/services/aigc/image2video/video-synthesis";
        }
        else {
            return "/api/v1/services/aigc/video-generation/video-synthesis";
        }
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
}
