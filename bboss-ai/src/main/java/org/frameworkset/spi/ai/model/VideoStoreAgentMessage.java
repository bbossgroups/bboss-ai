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
public  class VideoStoreAgentMessage extends StoreAgentMessage<VideoStoreAgentMessage>{
    private String taskId;
    private String videoTaskResultUrl;

    /**
     * 存储音频文件类型:
     * file 下载文件
     * url 不下载文件
     */
    private String storeVideoType;

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getStoreVideoType() {
        return storeVideoType;
    }

    public void setStoreVideoType(String storeVideoType) {
        this.storeVideoType = storeVideoType;
    }

    public String getVideoTaskResultUrl() {
        return videoTaskResultUrl;
    }

    public void setVideoTaskResultUrl(String videoTaskResultUrl) {
        this.videoTaskResultUrl = videoTaskResultUrl;
    }
}
