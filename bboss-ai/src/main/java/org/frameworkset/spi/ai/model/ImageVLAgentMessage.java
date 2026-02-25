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
import org.frameworkset.spi.ai.util.BaseStreamDataBuilder;
import org.frameworkset.spi.ai.util.StreamDataBuilder;
import org.frameworkset.spi.reactor.SSEHeaderSetFunction;
import org.frameworkset.spi.remote.http.ClientConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 图片识别报文体
 * @author biaoping.yin
 * @Date 2026/1/4
 */
public class ImageVLAgentMessage extends SessionAgentMessage<ImageVLAgentMessage>{
    private List<String> imageUrls;
    private String imageVLCompletionsUrl;
    

    @Override
    public ChatObject buildChatObject(ClientConfiguration clientConfiguration, AgentAdapter agentAdapter) {
        ChatObject chatObject = new ChatObject();
        SSEHeaderSetFunction sseHeaderSetFunction = null;
        Map parameters = null;
        Boolean stream = false;
        String aiChatRequestType = null;
        Object agentMessage = null;
        StreamDataBuilder streamDataBuilder = null;

        parameters = agentAdapter.buildImageVLRequestMap(this);
        setImageVLCompletionsUrl(agentAdapter.getImageVLCompletionsUrl(this));
        stream = (Boolean)parameters.get("stream");
        aiChatRequestType = agentAdapter.getAIImageParserRequestType();
        agentMessage = parameters;
        streamDataBuilder = new BaseStreamDataBuilder() {
            @Override
            public StreamData build(AgentAdapter agentAdapter, String line) {
                return agentAdapter.parseImageParserStreamContentFromData(this,line);
            }


            @Override
            public boolean isDone(AgentAdapter agentAdapter,String data) {
                return agentAdapter.isImageParserDone(data);
            }

            @Override
            public String getDoneData(AgentAdapter agentAdapter) {
                return agentAdapter.getImageParserDoneData();
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
        chatObject.setAgentMessage(this);
        chatObject.setStream(stream);
        chatObject.setCompletionsUrl(this.getImageVLCompletionsUrl());
        chatObject.setAiChatRequestType(aiChatRequestType);
        chatObject.setStreamDataBuilder(streamDataBuilder);
        return chatObject;
    }

    public String getImageVLCompletionsUrl() {
        return imageVLCompletionsUrl;
    }

    public ImageVLAgentMessage setImageVLCompletionsUrl(String imageVLCompletionsUrl) {
        this.imageVLCompletionsUrl = imageVLCompletionsUrl;
        return this;
    }

    public ImageVLAgentMessage setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
        return this;
    }

    public ImageVLAgentMessage addImageUrl(String imageUrl) {
        if(imageUrls == null){
            imageUrls = new ArrayList<>();
        }
        imageUrls.add(imageUrl);
        return this;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }
}
