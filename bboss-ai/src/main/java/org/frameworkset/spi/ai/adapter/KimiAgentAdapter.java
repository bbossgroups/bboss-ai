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

import org.frameworkset.spi.ai.model.ChatAgentMessage;
import org.frameworkset.spi.ai.model.ImageVLAgentMessage;
import org.frameworkset.spi.ai.model.VideoVLAgentMessage;

/**
 * Kimi模型智能体适配器
 * @author biaoping.yin
 * @Date 2026/1/4
 */
public class KimiAgentAdapter extends QwenAgentAdapter{
    @Override
    public String getImageVLCompletionsUrl(ImageVLAgentMessage imageVLAgentMessage) {
        return "/v1/chat/completions";
    }
    @Override
    public String getChatCompletionsUrl(ChatAgentMessage chatAgentMessage) {
        return "/v1/chat/completions";
    }


    public String getVideoVLCompletionsUrl(VideoVLAgentMessage videoVLAgentMessage) {
        return "/v1/chat/completions";
    }
}
