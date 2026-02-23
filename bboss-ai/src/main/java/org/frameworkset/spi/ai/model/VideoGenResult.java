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

/**
 * @author biaoping.yin
 * @Date 2026/1/31
 */
public class VideoGenResult extends VideoTask{
    /**
     * 视频播放地址
     */
    private String videoUrl;
    /**
     * 视频原始地址
     */
    private String videoGenUrl;
    private String submitTime;
    private String scheduledTime;
    private String endTime;
    private String origPrompt;

    public String getSubmitTime() {
        return submitTime;
    }

    public void setSubmitTime(String submitTime) {
        this.submitTime = submitTime;
    }

    public String getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(String scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getOrigPrompt() {
        return origPrompt;
    }

    public void setOrigPrompt(String origPrompt) {
        this.origPrompt = origPrompt;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getVideoGenUrl() {
        return videoGenUrl;
    }

    public void setVideoGenUrl(String videoGenUrl) {
        this.videoGenUrl = videoGenUrl;
    }
}
