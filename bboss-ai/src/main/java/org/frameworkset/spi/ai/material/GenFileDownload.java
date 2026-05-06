package org.frameworkset.spi.ai.material;
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

import org.frameworkset.spi.ai.model.AudioAgentMessage;
import org.frameworkset.spi.ai.model.ImageAgentMessage;
import org.frameworkset.spi.ai.model.StoreChatObject;
import org.frameworkset.spi.ai.model.VideoStoreAgentMessage;
import org.frameworkset.spi.remote.http.ClientConfiguration;

/**
 * @author biaoping.yin
 * @Date 2026/1/20
 */
public interface GenFileDownload {

    String downloadImage(ClientConfiguration config, ImageAgentMessage imageAgentMessage, StoreChatObject storeChatObject, String downUrl, String imageUrl);
    String downloadVideoImage(ClientConfiguration config, VideoStoreAgentMessage videoStoreAgentMessage,StoreChatObject storeChatObject,  String imageUrl);
    String downloadAudio(ClientConfiguration config, AudioAgentMessage audioAgentMessage,StoreChatObject storeChatObject, String downUrl, String audioUrl);
    String downloadVideo(ClientConfiguration config, VideoStoreAgentMessage videoStoreAgentMessage,StoreChatObject storeChatObject, String downUrl, String videoUrl);
}
