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

import org.frameworkset.spi.ai.adapter.AgentAdapter;
import org.frameworkset.spi.ai.util.AudioDataBuilder;
import org.frameworkset.spi.ai.util.StreamDataBuilder;
import org.frameworkset.spi.reactor.SSEHeaderSetFunction;
import org.frameworkset.spi.remote.http.ClientConfiguration;

import java.io.File;
import java.util.Map;

/**
 * 语音识别消息模型
 * @author biaoping.yin
 * @Date 2026/1/4
 */
public class AudioSTTAgentMessage<T> extends SessionAgentMessage<AudioSTTAgentMessage> {
    /**
     * 音频数据,支持:url,base64编码数据,MultipartFile,File 对象
     */
    private T audio;

    private String resultFormat;
    private AudioDataBuilder audioDataBuilder;
    private String contentType;

    private Map<String, File> files;

    public String getAudioSTTCompletionsUrl() {
        return audioSTTCompletionsUrl;
    }

    public AudioSTTAgentMessage setAudioSTTCompletionsUrl(String audioSTTCompletionsUrl) {
        this.audioSTTCompletionsUrl = audioSTTCompletionsUrl;
        return this;
    }

    private String audioSTTCompletionsUrl;

    /**
     * 音频数据,支持:url,base64编码数据,MultipartFile,File 对象
     * @param audio
     * @return
     */
    public AudioSTTAgentMessage setAudio(T audio) {
        this.audio = audio;
        return this;
    }

    public T getAudio() {
        return audio;
    }

    public void setFiles(Map<String, File> files) {
        this.files = files;
    }

    public Map<String, File> getFiles() {
        return files;
    }

    /**
     * 构建流式风格接口ChatObject对象
     *
     * @param clientConfiguration
     * @param agentAdapter
     * @return
     */
    @Override
    public ChatObject buildChatObject(ClientConfiguration clientConfiguration, AgentAdapter agentAdapter) {
        ChatObject chatObject = new ChatObject();
        SSEHeaderSetFunction sseHeaderSetFunction = null;
        Map parameters = null;
        Boolean stream = false;
        String aiChatRequestType = null;
        Object agentMessage = null;
        StreamDataBuilder streamDataBuilder = null;

        
        parameters = agentAdapter.buildAudioSTTRequestMap(this);
        this.audioSTTCompletionsUrl = agentAdapter.getAudioSTTCompletionsUrl(this);
        stream = (Boolean)parameters.get("stream");
        aiChatRequestType = agentAdapter.getAIAudioParsertRequestType();
        agentMessage = parameters;
        streamDataBuilder = new StreamDataBuilder() {
            @Override
            public StreamData build(AgentAdapter agentAdapter, String line) {
                return agentAdapter.parseAudioStreamContentFromData(this,line);
            }


            @Override
            public boolean isDone(AgentAdapter agentAdapter,String data) {
                return agentAdapter.isDone(data);
            }

            @Override
            public String getDoneData(AgentAdapter agentAdapter) {
                return agentAdapter.getDoneData();
            }

            @Override
            public void handleServerEvent(AgentAdapter agentAdapter,ServerEvent serverEvent){

            }
            @Override
            public ChatObject getChatObject() {
                return chatObject;
            }
        };
        if(stream == null){
            stream = false;
        }
        chatObject.setSseHeaderSetFunction(sseHeaderSetFunction);
        chatObject.setMessage(agentMessage);
        chatObject.setCompletionsUrl(this.getAudioSTTCompletionsUrl());
        chatObject.setStream(stream);
        chatObject.setFiles( files);
        chatObject.setAiChatRequestType(aiChatRequestType);
        chatObject.setStreamDataBuilder(streamDataBuilder);
        return chatObject;
    }

    public AudioDataBuilder getAudioDataBuilder() {
        return audioDataBuilder;
    }

    public AudioSTTAgentMessage setAudioDataBuilder(AudioDataBuilder audioDataBuilder) {
        this.audioDataBuilder = audioDataBuilder;
        return this;
    }
    
    public String getResultFormat() {
		return resultFormat;
	}

    public AudioSTTAgentMessage setResultFormat(String resultFormat) {
        this.resultFormat = resultFormat;
        return this;
    }

    public String getContentType() {
        return contentType;
    }

    public AudioSTTAgentMessage setContentType(String contentType) {
        this.contentType = contentType;
        return this;
    }
}
