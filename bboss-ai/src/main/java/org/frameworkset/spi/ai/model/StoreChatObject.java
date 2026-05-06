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

/**
 * @author biaoping.yin
 * @Date 2026/5/6
 */
public class StoreChatObject {
    protected Object message;
    private String submitVideoTaskUrl;
    /**
     * 图片生成存储目录
     */
    protected String genFileStoreDir;
    protected String endpoint;

    private String storeImageType ;
    /**
     * 存储文件相对路径，包含名称
     */
    private String storeFilePath;
    protected StoreFilePathFunction storeFilePathFunction;
    /**
     * 存储音频文件类型:
     * file 下载文件
     * url 不下载文件
     */
    private String storeVideoType;
    /**
     * 存储音频文件类型:
     * file 下载文件
     * url 不下载文件
     */
    private String storeAudioType;


    public String getStoreAudioType() {
        return storeAudioType;
    }

    public StoreChatObject setStoreAudioType(String storeAudioType) {
        this.storeAudioType = storeAudioType;
        return this;
    }
    public String getStoreVideoType() {
        return storeVideoType;
    }

    public void setStoreVideoType(String storeVideoType) {
        this.storeVideoType = storeVideoType;
    }
    public void setMessage(Object message) {
        this.message = message;
    }

    public Object getMessage() {
        return message;
    }

    public  StoreChatObject setStoreFilePath(String storeFilePath) {
        this.storeFilePath = storeFilePath;
        return  this;
    }

    public String getStoreFilePath() {
        return storeFilePath;
    }

    public String getGenFileStoreDir() {
        return genFileStoreDir;
    }

    public StoreChatObject setGenFileStoreDir(String genFileStoreDir) {
        this.genFileStoreDir = genFileStoreDir;
        return  this;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public StoreChatObject setEndpoint(String endpoint) {
        this.endpoint = endpoint;
        return  this;
    }



    public StoreFilePathFunction getStoreFilePathFunction() {
        return storeFilePathFunction;
    }

    public StoreChatObject setStoreFilePathFunction(StoreFilePathFunction storeFilePathFunction) {
        this.storeFilePathFunction = storeFilePathFunction;
        return  this;
    }

    public String getStoreImageType() {
        return storeImageType;
    }

    public void setStoreImageType(String storeImageType) {
        this.storeImageType = storeImageType;
    }

    public String getSubmitVideoTaskUrl() {
        return submitVideoTaskUrl;
    }

    public void setSubmitVideoTaskUrl(String submitVideoTaskUrl) {
        this.submitVideoTaskUrl = submitVideoTaskUrl;
    }
}
