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

import org.frameworkset.spi.ai.model.*;
import org.frameworkset.spi.ai.util.AIResponseUtil;
import org.frameworkset.spi.remote.http.ClientConfiguration;
import org.frameworkset.spi.remote.http.HttpRequestProxy;

/**
 * @author biaoping.yin
 * @Date 2026/1/20
 */
public class GenMaterialFileDownload implements GenFileDownload {


    
    @Override
    public String downloadImage(ClientConfiguration config, ImageAgentMessage imageAgentMessage, StoreChatObject storeChatObject, String downUrl, String imageUrl) {
    //    String downUrl = "/largemodel/moma/api/v1/fs/getFile";
        if(storeChatObject.getStoreImageType() == null || storeChatObject.getStoreImageType().equals(AIConstants.STORETYPE_URL)){
            return imageUrl;
        }
        return HttpRequestProxy.httpGet(config, imageUrl, AIResponseUtil.buildDownImageHttpClientResponseHandler(config,imageAgentMessage,storeChatObject,imageUrl));
    }


    @Override
    public String downloadVideoImage(ClientConfiguration config, VideoStoreAgentMessage videoStoreAgentMessage, StoreChatObject storeChatObject, String imageUrl){
        if(storeChatObject.getStoreVideoType() == null || storeChatObject.getStoreVideoType().equals(AIConstants.STORETYPE_URL)){
            return imageUrl;
        }
        return HttpRequestProxy.httpGet(config, imageUrl, AIResponseUtil.buildDownVideoImageHttpClientResponseHandler(config,videoStoreAgentMessage,storeChatObject,imageUrl));
    }

    @Override
    public String downloadAudio(ClientConfiguration config, AudioAgentMessage audioAgentMessage,StoreChatObject storeChatObject, String downUrl, String audioUrl) {
        if(storeChatObject.getStoreAudioType() == null || storeChatObject.getStoreAudioType().equals(AIConstants.STORETYPE_URL)){
            return audioUrl;
        }
        return HttpRequestProxy.httpGet(config, audioUrl, AIResponseUtil.buildDownAudioHttpClientResponseHandler(config,audioAgentMessage,storeChatObject,audioUrl));
    }

    @Override
    public String downloadVideo(ClientConfiguration config, VideoStoreAgentMessage videoStoreAgentMessage,StoreChatObject storeChatObject, String downUrl, String videoUrl) {
        if(storeChatObject.getStoreVideoType() == null || storeChatObject.getStoreVideoType().equals(AIConstants.STORETYPE_URL)){
            return videoUrl;
        }
        return HttpRequestProxy.httpGet(config, videoUrl, AIResponseUtil.buildDownVideoHttpClientResponseHandler(config,videoStoreAgentMessage,storeChatObject,videoUrl));
    }

}
