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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.FluxSink;


import static java.lang.Thread.sleep;

/**
 * @author biaoping.yin
 * @Date 2026/3/3
 */
public class McpSSESink {
    private static Logger logger = LoggerFactory.getLogger(McpSSESink.class);
    private FluxSink<String> sink;
    private String sessionId;
    private Thread ping ;


    private boolean notificationsInitialized;
    public McpSSESink(FluxSink<String> sink,String sessionId) {
        this.sink = sink;
        this.sessionId = sessionId;
    }
    public void send(String data) {
        sink.next(data);
    }
    public void complete() {
        try {
            sink.complete();
        } catch (Exception e) {
        }
        if(ping != null) {
            ping.interrupt();
            try {
                ping.join();
            } catch (InterruptedException e) {
            }
        }
    }

    public void destory() {
        logger.info("destory McpSSESink sessionId:{}",sessionId);
        if(ping != null) {
            ping.interrupt();
            try {
                ping.join();
            } catch (InterruptedException e) {
            }
        }
    }

    public String getSessionId() {
        return sessionId;
    }
    
    public void sendSSEEndpoint(String endpoint){
        sink.next("event:endpoint\n");
        sink.next("data:"+endpoint+"?sessionId="+sessionId+"\n");
    }
    public void sendSSEMessage(String message){
        sink.next("event:message\n");
        sink.next("data:"+message+"\n");
    }
    public boolean isNotificationsInitialized() {
        return notificationsInitialized;
    }

    public void setNotificationsInitialized(boolean notificationsInitialized) {
        this.notificationsInitialized = notificationsInitialized;
    }
    
    public void ping(){
        if(ping == null){
            ping = new Thread(()->{
                while(true){
                    try {
                        sleep(60000l);
                    } catch (InterruptedException e) {
                        break;
                    }
                    sink.next("ping:"+System.currentTimeMillis()+"\n");
                }
            });
            ping.setDaemon(true);
            ping.start();
            try {
                ping.join();
            } catch (InterruptedException e) {
                
            }
                    
        }
        
       
    }

}
