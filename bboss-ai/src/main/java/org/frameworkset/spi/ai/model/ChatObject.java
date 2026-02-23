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
import org.frameworkset.spi.ai.util.StreamDataBuilder;
import org.frameworkset.spi.reactor.SSEHeaderSetFunction;
import org.frameworkset.spi.remote.http.ClientConfiguration;

import java.io.File;
import java.util.Map;

/**
 * @author biaoping.yin
 * @Date 2026/1/4
 */
public class ChatObject {
    private boolean isStream;
    private Object message;
    private String aiChatRequestType = AIConstants.AI_CHAT_REQUEST_BODY_JSON;
    private SSEHeaderSetFunction sseHeaderSetFunction;
    private StreamDataBuilder streamDataBuilder;
    private Map<String, File> files;


    private String completionsUrl;
    public String getCompletionsUrl() {
        return completionsUrl;
    }

    public void setCompletionsUrl(String completionsUrl) {
        this.completionsUrl = completionsUrl;
    }

    public String getDoneData(AgentAdapter agentAdapter) {
        return streamDataBuilder.getDoneData(agentAdapter);
    }
 

    public void setSseHeaderSetFunction(SSEHeaderSetFunction sseHeaderSetFunction) {
        if(sseHeaderSetFunction != null) {
            this.sseHeaderSetFunction = sseHeaderSetFunction;
        }
        else{
            this.sseHeaderSetFunction = SSEHeaderSetFunction.DEFAULT_SSEHEADERSETFUNCTION;
        }
    }

    public SSEHeaderSetFunction getSseHeaderSetFunction() {
        return sseHeaderSetFunction;
    }

    /**
     * 获取智能问答请求参数类型
     * @return
     */
    public String getAIChatRequestType(){
        return aiChatRequestType;

    }

    public void setAiChatRequestType(String aiChatRequestType) {
        this.aiChatRequestType = aiChatRequestType;
    }

    public boolean isStream() {
        return isStream;
    }

    public void setStream(boolean stream) {
        isStream = stream;
    }

    public Object getMessage() {
        return message;
    }

    public void setMessage(Object message) {
        this.message = message;
    }

    public void setStreamDataBuilder(StreamDataBuilder streamDataBuilder) {
        this.streamDataBuilder = streamDataBuilder;
    }

    public StreamDataBuilder getStreamDataBuilder() {
        return streamDataBuilder;
    }

    public Map<String, File> getFiles() {
        return files;
    }

    public void setFiles(Map<String, File> files) {
        this.files = files;
    }
}
