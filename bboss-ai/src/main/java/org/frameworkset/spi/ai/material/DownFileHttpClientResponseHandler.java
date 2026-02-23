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

import com.frameworkset.util.SimpleStringUtil;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.frameworkset.spi.ai.model.StoreAgentMessage;
import org.frameworkset.spi.remote.http.BaseURLResponseHandler;
import org.frameworkset.spi.remote.http.ClientConfiguration;
import org.frameworkset.spi.remote.http.ResponseUtil;

import java.io.*;

/**
 * @author biaoping.yin
 * @Date 2026/1/20
 */
public class DownFileHttpClientResponseHandler extends BaseURLResponseHandler<String> {
    private ClientConfiguration clientConfiguration;
    private String url;
    private StoreAgentMessage storeAgentMessage;
    public DownFileHttpClientResponseHandler(ClientConfiguration clientConfiguration, StoreAgentMessage storeAgentMessage, String url) {
        this.clientConfiguration = clientConfiguration;
        this.url = url;
        this.storeAgentMessage = storeAgentMessage;

    }

    @Override
    public String handleResponse(ClassicHttpResponse response) throws HttpException, IOException {
        int status = response.getCode();
        if (status == 200) {
            String storeFilePath = null;
            if(storeAgentMessage.getStoreFilePathFunction() != null){
                storeFilePath = storeAgentMessage.getStoreFilePathFunction().getStoreFilePath(url);
            }
            else{
                storeFilePath = storeAgentMessage.getStoreFilePath();
            }
            String targetPath = generateFilePath(url,storeFilePath); // 根据URL生成目标文件路径
            File file = new File(targetPath);
            if(!file.getParentFile().exists())
                file.getParentFile().mkdirs();
            try (InputStream inputStream = response.getEntity().getContent();
                 FileOutputStream outputStream = new FileOutputStream(targetPath)) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
            }
            if(storeAgentMessage.getEndpoint() == null) {
                return targetPath; // 返回下载文件路径
            }
            else{
                return SimpleStringUtil.getRealPath(storeAgentMessage.getEndpoint(),storeFilePath);
            }
        } else {
            throw ResponseUtil.buildException(  url,  response,  status);
        }
    }
   
    private String generateFilePath(String url,String storeFilePath) {
        // 根据imageUrl生成本地文件路径
        String fileName = SimpleStringUtil.getRealPath(storeAgentMessage.getGenFileStoreDir(),storeFilePath);
        return fileName;
    }


    
}
