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
public class VideoVLAgentMessage extends SessionAgentMessage<VideoVLAgentMessage>{
    private List<String> videoUrls;
    private String videoVLCompletionsUrl;
    

    @Override
    public ChatObject buildChatObject(ClientConfiguration clientConfiguration, AgentAdapter agentAdapter) {
        ChatObject chatObject = new ChatObject();
        SSEHeaderSetFunction sseHeaderSetFunction = null;
        Map parameters = null;
        Boolean stream = false;
        String aiChatRequestType = null;
        Object agentMessage = null;
        StreamDataBuilder streamDataBuilder = null;

        parameters = agentAdapter.buildVideoVLRequestMap(this);
        setVideoVLCompletionsUrl(agentAdapter.getVideoVLCompletionsUrl(this));
        stream = (Boolean)parameters.get("stream");
        aiChatRequestType = agentAdapter.getAIVideoParserRequestType();
        agentMessage = parameters;
        streamDataBuilder = new BaseStreamDataBuilder() {
            @Override
            public StreamData build(AgentAdapter agentAdapter, String line) {
                return agentAdapter.parseVideoParserStreamContentFromData(this,line);
            }


            @Override
            public boolean isDone(AgentAdapter agentAdapter,String data) {
                return agentAdapter.isVideoParserDone(data);
            }

            @Override
            public String getDoneData(AgentAdapter agentAdapter) {
                return agentAdapter.getVideoParserDoneData();
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
        chatObject.setStream(stream);
        chatObject.setAgentMessage(this);
        chatObject.setCompletionsUrl(this.getVideoVLCompletionsUrl());
        chatObject.setAiChatRequestType(aiChatRequestType);
        chatObject.setStreamDataBuilder(streamDataBuilder);
        return chatObject;
    }

 

    public VideoVLAgentMessage setVideoUrls(List<String> videoUrls) {
        this.videoUrls = videoUrls;
        return this;
    }

    public VideoVLAgentMessage addVideoUrl(String videoUrl) {
        if(videoUrls == null){
            videoUrls = new ArrayList<>();
        }
        videoUrls.add(videoUrl);
        return this;
    }

    public List<String> getVideoUrls() {
        return videoUrls;
    }

    public String getVideoVLCompletionsUrl() {
        return videoVLCompletionsUrl;
    }

    public VideoVLAgentMessage setVideoVLCompletionsUrl(String videoVLCompletionsUrl) {
        this.videoVLCompletionsUrl = videoVLCompletionsUrl;
        return this;
    }
}
