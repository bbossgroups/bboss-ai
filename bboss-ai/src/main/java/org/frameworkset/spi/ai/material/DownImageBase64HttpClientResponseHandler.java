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

import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

/**
 * @author biaoping.yin
 * @Date 2026/1/20
 */
public class DownImageBase64HttpClientResponseHandler implements HttpClientResponseHandler<String> {
    @Override
    public String handleResponse(ClassicHttpResponse response) throws HttpException, IOException {
        if (response.getCode() == 200) {
            try (InputStream inputStream = response.getEntity().getContent();
                 ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {

                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    byteArrayOutputStream.write(buffer, 0, bytesRead);
                }

                byte[] imageBytes = byteArrayOutputStream.toByteArray();
                String base64Image = Base64.getEncoder().encodeToString(imageBytes);

                return "data:image/jpeg;base64," + base64Image;
            }
        } else {
            throw new IOException("Download failed: " + response.getCode());
        }
    }
}
