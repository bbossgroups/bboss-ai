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
import org.frameworkset.spi.ai.material.GenFileDownload;
import org.frameworkset.spi.ai.util.BaseStreamDataBuilder;
import org.frameworkset.spi.ai.util.StreamDataBuilder;
import org.frameworkset.spi.reactor.SSEHeaderSetFunction;
import org.frameworkset.spi.remote.http.ClientConfiguration;

import java.util.Map;

/**
 * 语音生成模型消息
 * @author biaoping.yin
 * @Date 2026/1/4
 */
public class AudioAgentMessage extends StoreAgentMessage<AudioAgentMessage> {
    /**
     * 存储音频文件类型:
     * file 下载文件
     * url 不下载文件
     */
    private String storeAudioType;
    /**
     * maas平台音频生成服务地址
     */
    private String genAudioCompletionsUrl;
    
    public String getStoreAudioType() {
        return storeAudioType;
    }

    public AudioAgentMessage setStoreAudioType(String storeAudioType) {
        this.storeAudioType = storeAudioType;
        return this;
    }

    public AudioAgentMessage setGenAudioCompletionsUrl(String genAudioCompletionsUrl) {
        this.genAudioCompletionsUrl = genAudioCompletionsUrl;
        return this;
    }

    public String getGenAudioCompletionsUrl() {
        return genAudioCompletionsUrl;
    }

    @Override
    public ChatObject buildChatObject(ClientConfiguration clientConfiguration, AgentAdapter agentAdapter) {
        ChatObject chatObject = new ChatObject();
        SSEHeaderSetFunction sseHeaderSetFunction = null;
        Map parameters = null;
        Boolean stream = false;
        String aiChatRequestType = null;
        StreamDataBuilder streamDataBuilder = null;
        Object agentMessage = null;       
        parameters = agentAdapter._buildGenAudioRequestMap(this,clientConfiguration);
        stream = (Boolean)parameters.get("stream");
        aiChatRequestType = agentAdapter.getAIChatRequestType();
        agentMessage = parameters;
        sseHeaderSetFunction = agentAdapter.getAudioGenSSEHeaderSetFunction();
        streamDataBuilder = new BaseStreamDataBuilder() {
            @Override
            public StreamData build(AgentAdapter agentAdapter, String line) {
                return agentAdapter.parseAudioGenStreamContentFromData(line);
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
                String url = serverEvent.getGenUrl();
                if(url != null) {
                    GenFileDownload genFileDownload = agentAdapter.getGenFileDownload();
                    serverEvent.setUrl(genFileDownload.downloadAudio(clientConfiguration, AudioAgentMessage.this, null, url));
                }
            }
            @Override
            public ChatObject getChatObject() {
                return chatObject;
            }
        };
         


        if(stream == null){
            stream = false;
        }
        chatObject.setCompletionsUrl(this.getGenAudioCompletionsUrl());
        chatObject.setSseHeaderSetFunction(sseHeaderSetFunction);
        chatObject.setMessage(agentMessage);
        chatObject.setAgentMessage(this);
        chatObject.setStream(stream);
        chatObject.setAiChatRequestType(aiChatRequestType);
        chatObject.setStreamDataBuilder(streamDataBuilder);
        return chatObject;
    }
}
