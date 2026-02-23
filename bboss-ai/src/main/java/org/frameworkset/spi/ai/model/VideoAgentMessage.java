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
 * 语音生成模型消息
 * @author biaoping.yin
 * @Date 2026/1/4
 */
public class VideoAgentMessage extends AgentMessage<VideoAgentMessage> {
    
    private String languageType;


    private String audioUrl;
    
    private String submitVideoTaskUrl;

    private String template ;

    private String imgUrl;
    private String firstFrameUrl;
    private String lastFrameUrl;

    public String getFirstFrameUrl() {
        return firstFrameUrl;
    }

    public void setFirstFrameUrl(String firstFrameUrl) {
        this.firstFrameUrl = firstFrameUrl;
    }

    public String getLastFrameUrl() {
        return lastFrameUrl;
    }

    public void setLastFrameUrl(String lastFrameUrl) {
        this.lastFrameUrl = lastFrameUrl;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    /**
     * 生成视频的图片地址
     * @param imgUrl
     */
    public VideoAgentMessage setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
        return this;
    }
    public String getLanguageType() {
        return languageType;
    }

    public VideoAgentMessage setLanguageType(String languageType) {
        this.languageType = languageType;
        return this;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public VideoAgentMessage setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
        return this;
    }

    public String getSubmitVideoTaskUrl() {
        return submitVideoTaskUrl;
    }

    public VideoAgentMessage setSubmitVideoTaskUrl(String submitVideoTaskUrl) {
        this.submitVideoTaskUrl = submitVideoTaskUrl;
        return this;
    }

    public String getTemplate() {
        return template;
    }

    public VideoAgentMessage setTemplate(String template) {
        this.template = template;
        return this;
    }
}
