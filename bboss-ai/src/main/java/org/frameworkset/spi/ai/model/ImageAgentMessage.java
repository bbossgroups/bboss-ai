package org.frameworkset.spi.ai.model;
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

import org.frameworkset.spi.ai.material.StoreFilePathFunction;

import java.util.ArrayList;
import java.util.List;

/**
 * 图片生成报文
 * @author biaoping.yin
 * @Date 2026/1/4
 */
public class ImageAgentMessage extends StoreAgentMessage<ImageAgentMessage>{
   
    private String storeImageType ;
    private List<String> imageUrls;

    public String getGenImageCompletionsUrl() {
        return genImageCompletionsUrl;
    }

    public ImageAgentMessage setGenImageCompletionsUrl(String genImageCompletionsUrl) {
        this.genImageCompletionsUrl = genImageCompletionsUrl;
        return this;
    }

    /**
     * maas平台图片生成接口地址
     */
    private String genImageCompletionsUrl;
    public ImageAgentMessage setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
        return this;
    }

    public ImageAgentMessage addImageUrl(String imageUrl) {
        if(imageUrls == null){
            imageUrls = new ArrayList<>();
        }
        imageUrls.add(imageUrl);
        return this;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }
     

    public String getStoreImageType() {
        return storeImageType;
    }

    public ImageAgentMessage setStoreImageType(String storeImageType) {
        this.storeImageType = storeImageType;
        return this;
    }

 
}
