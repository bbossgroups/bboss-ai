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

import com.frameworkset.util.SimpleStringUtil;
import org.frameworkset.spi.reactor.ReactorCallException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Schedulers;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author biaoping.yin
 * @Date 2026/3/2
 */
public class MCPSSEServer {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MCPSSEServer.class);
//    private Map<String, McpSSESink> mcpSSESinkMap = new ConcurrentHashMap<>();

    private Map<String, Map<String, McpSSESink>> apiSessions = new ConcurrentHashMap<>();
    private Object lock = new Object();
    public McpSSESink getMcpSSESink(String apiKey, String sessionId) {
        Map<String, McpSSESink> mcpSSESinkMap = apiSessions.get(apiKey);
        if(mcpSSESinkMap == null) {
            return null;
        }
        return mcpSSESinkMap.get(sessionId);
    }
    public void destory() {
        Iterator<Map.Entry<String, Map<String, McpSSESink>>> iterator = apiSessions.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Map<String, McpSSESink>> entry = iterator.next();
            Map<String, McpSSESink> mcpSSESinkMap = entry.getValue();
            Iterator<Map.Entry<String, McpSSESink>> mcpSSESinkIterator = mcpSSESinkMap.entrySet().iterator();
            while (mcpSSESinkIterator.hasNext()) {
                Map.Entry<String, McpSSESink> mcpSSESinkEntry = mcpSSESinkIterator.next();
                McpSSESink mcpSSESink = mcpSSESinkEntry.getValue();
                mcpSSESink.complete();
                
            }
        }
        apiSessions.clear();
        
       
    }
    public Flux<String> sse(String apiKey,String sseEndpoint) {
        Map<String, McpSSESink> _mcpSSESinkMap = apiSessions.get(apiKey);
        if(_mcpSSESinkMap == null) {
            synchronized (lock) {
                _mcpSSESinkMap = apiSessions.get(apiKey);
                if(_mcpSSESinkMap == null) {
                    _mcpSSESinkMap = new ConcurrentHashMap<>();
                    apiSessions.put(apiKey, _mcpSSESinkMap);
                }
            }
        }
        Map<String, McpSSESink> mcpSSESinkMap = _mcpSSESinkMap;
        return Flux.<String>create(sink -> {
                    McpSSESink  mcpSSESink = null;
                    try {
                       
                        //sessionId为key
                         
                        String sessionId = SimpleStringUtil.getUUID32();
                        mcpSSESink = new McpSSESink(sink,sessionId);
                        McpSSESink  mcpSSESink_ = mcpSSESink;
                        sink.onDispose(() -> {
                            mcpSSESink_.destory();
                        });
                        mcpSSESinkMap.put(sessionId, mcpSSESink);
                       
                        mcpSSESink.sendSSEEndpoint(sseEndpoint);
                       
                        mcpSSESink.ping();
//							sink.next("event:endpoint");
//							sink.next("data:/mcp/message.api?sessionId="+sessionId);

                    } catch (ReactorCallException e) {
//                        logger.error("流式请求失败：poolName["+poolName +"],url["+url +"],data:" + data);
//                        sink.error(e);
                    } catch (Exception e) {
//                        sink.error(new ReactorCallException("流式请求失败：poolName["+poolName +"],url["+url +"],", e));
                    }
                    catch (Throwable e) {
//                        sink.error(new ReactorCallException("流式请求失败：poolName["+poolName +"],url["+url +"],", e));
                    }
                    finally {
                        if(mcpSSESink != null) {
                            mcpSSESink.complete();
                        }
                        else{
                            sink.complete();
                        }
                    }
                }, FluxSink.OverflowStrategy.BUFFER)
                .subscribeOn(Schedulers.boundedElastic()) // 在弹性线程池中执行阻塞IO
//                .timeout(Duration.ofSeconds(60)) // 设置超时
                .onErrorResume(throwable -> {
//                    String error = SimpleStringUtil.exceptionToString(throwable);
//                    System.err.println("流式处理错误: " + throwable.getMessage());
//                    String error = SimpleStringUtil.exceptionToString(throwable);
                    if(logger.isDebugEnabled()) {
                        logger.debug(throwable.getMessage(), throwable);
                    }
                    // 修改此处，将错误信息作为Flux输出
                    return Flux.empty();
                });

        
    }
}
