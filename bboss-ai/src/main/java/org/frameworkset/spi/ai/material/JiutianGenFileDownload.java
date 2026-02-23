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

import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.frameworkset.spi.ai.model.AudioAgentMessage;
import org.frameworkset.spi.ai.model.ImageAgentMessage;
import org.frameworkset.spi.ai.model.VideoStoreAgentMessage;
import org.frameworkset.spi.ai.util.AIResponseUtil;
import org.frameworkset.spi.remote.http.ClientConfiguration;
import org.frameworkset.spi.remote.http.HttpRequestProxy;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author biaoping.yin
 * @Date 2026/1/20
 */
public class JiutianGenFileDownload implements GenFileDownload {

//    @Override
//    public String downloadImage(ClientConfiguration config, String imageUrl) {
//        String downUrl = "/largemodel/moma/api/v1/fs/getFile";
//        Map<String,Object> params = new LinkedHashMap<>();
//        params.put("key",imageUrl);
//        StringBuilder url = new StringBuilder();
//        HttpRequestProxy.httpGet(config,downUrl,new HttpClientResponseHandler<Void>(){
//            /**
//             * Processes an {@link ClassicHttpResponse} and returns some value
//             * corresponding to that response.
//             *
//             * @param response The response to process
//             * @return A value determined by the response
//             * @throws IOException   in case of a problem or the connection was aborted
//             * @throws HttpException in case of an HTTP protocol violation.
//             */
//            @Override
//            public Void handleResponse(ClassicHttpResponse response) throws HttpException, IOException {
//                //将响应作为字节流写入一个文件中
//                
//                return null;
//            }
//
//        },params);
//    }
    
    @Override
    public String downloadImage(ClientConfiguration config, ImageAgentMessage imageAgentMessage, String downUrl, String imageUrl) {
    //    String downUrl = "/largemodel/moma/api/v1/fs/getFile";
        Map<String,Object> params = new LinkedHashMap<>();
        params.put("key", imageUrl);
        HttpClientResponseHandler<String> httpClientResponseHandler = AIResponseUtil.buildDownImageHttpClientResponseHandler(config,imageAgentMessage,imageUrl);
        
        return HttpRequestProxy.httpGet(config, downUrl, httpClientResponseHandler, params);
    }

    @Override
    public String downloadAudio(ClientConfiguration config, AudioAgentMessage audioAgentMessage, String downUrl, String audioUrl) {
        return null;
    }

    @Override
    public String downloadVideo(ClientConfiguration config, VideoStoreAgentMessage videoStoreAgentMessage, String downUrl, String videoUrl) {
        return null;
    }

}
