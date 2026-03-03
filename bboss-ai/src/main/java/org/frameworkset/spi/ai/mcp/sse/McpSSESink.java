package org.frameworkset.spi.ai.mcp.sse;
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

import reactor.core.publisher.FluxSink;

/**
 * @author biaoping.yin
 * @Date 2026/3/3
 */
public class McpSSESink {
    private FluxSink<String> sink;
    private String sessionId;


    private boolean notificationsInitialized;
    public McpSSESink(FluxSink<String> sink,String sessionId) {
        this.sink = sink;
        this.sessionId = sessionId;
    }
    public void send(String data) {
        sink.next(data);
    }
    public void complete() {
        sink.complete();
    }

    public String getSessionId() {
        return sessionId;
    }
    
    public void sendSSEEndpoint(String endpoint){
        sink.next("event:endpoint");
        sink.next("data:"+endpoint+"?sessionId="+sessionId);
    }
    public void sendSSEMessage(String message){
        sink.next("event:message");
        sink.next("data:"+message);
    }
    public boolean isNotificationsInitialized() {
        return notificationsInitialized;
    }

    public void setNotificationsInitialized(boolean notificationsInitialized) {
        this.notificationsInitialized = notificationsInitialized;
    }

}
