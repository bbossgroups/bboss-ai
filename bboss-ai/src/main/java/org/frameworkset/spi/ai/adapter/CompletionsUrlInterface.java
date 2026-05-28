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
 * @author biaoping.yin
 * @Date 2026/2/1
 */
public interface CompletionsUrlInterface {
    String getGenAudioCompletionsUrl(ClientConfiguration clientConfiguration,AudioAgentMessage audioAgentMessage);
    String getAudioSTTCompletionsUrl(ClientConfiguration clientConfiguration,AudioSTTAgentMessage audioSTTAgentMessage);
    String getImageVLCompletionsUrl(ClientConfiguration clientConfiguration,ImageVLAgentMessage imageVLAgentMessage);  
    String getVideoVLCompletionsUrl(ClientConfiguration clientConfiguration,VideoVLAgentMessage videoVLAgentMessage);
    String getGenImageCompletionsUrl(ClientConfiguration clientConfiguration,ImageAgentMessage imageAgentMessage);
    String getSubmitVideoTaskUrl(ClientConfiguration clientConfiguration,VideoAgentMessage videoAgentMessage);
    String getChatCompletionsUrl(ClientConfiguration clientConfiguration,ChatAgentMessage chatAgentMessage) ;
    String getRerankUrl(ClientConfiguration clientConfiguration,AgentMessage chatAgentMessage) ;
    String getEmbeddingUrl(ClientConfiguration clientConfiguration,AgentMessage chatAgentMessage) ;
    String getVideoTaskResultUrl(ClientConfiguration clientConfiguration,VideoStoreAgentMessage videoStoreAgentMessage);

}
