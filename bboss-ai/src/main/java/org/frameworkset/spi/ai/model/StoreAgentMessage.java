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
 * 图片生成报文
 * @author biaoping.yin
 * @Date 2026/1/4
 */
public abstract class StoreAgentMessage<T extends StoreAgentMessage> extends AgentMessage<T>{
    /**
     * 图片生成存储目录
     */
    protected String genFileStoreDir;
    protected String endpoint;
    /**
     * 存储文件相对路径，包含名称
     */
    private String storeFilePath;
    protected StoreFilePathFunction storeFilePathFunction;
   

    public  T setStoreFilePath(String storeFilePath) {
        this.storeFilePath = storeFilePath;
        return (T)this;
    }

    public String getStoreFilePath() {
        return storeFilePath;
    }

    public String getGenFileStoreDir() {
        return genFileStoreDir;
    }

    public T setGenFileStoreDir(String genFileStoreDir) {
        this.genFileStoreDir = genFileStoreDir;
        return (T)this;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public T setEndpoint(String endpoint) {
        this.endpoint = endpoint;
        return (T)this;
    }

 

    public StoreFilePathFunction getStoreFilePathFunction() {
        return storeFilePathFunction;
    }

    public T setStoreFilePathFunction(StoreFilePathFunction storeFilePathFunction) {
        this.storeFilePathFunction = storeFilePathFunction;
        return (T) this;
    }
}
