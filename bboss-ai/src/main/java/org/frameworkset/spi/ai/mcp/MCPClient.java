package org.frameworkset.spi.ai.mcp;
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

import com.frameworkset.util.JsonUtil;
import com.frameworkset.util.ValueObjectUtil;
import org.frameworkset.bulk.CommonBulkRetryHandler;
import org.frameworkset.spi.ai.mcp.model.*;
import org.frameworkset.spi.ai.util.AIAgentUtil;
import org.frameworkset.spi.remote.http.ClientConfiguration;
import org.frameworkset.spi.remote.http.HttpMethodName;
import org.frameworkset.spi.remote.http.HttpRequestProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import org.frameworkset.spi.ai.mcp.sse.SSEMcpCallHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static java.lang.Thread.sleep;

/**
 * @author biaoping.yin
 * @Date 2026/2/26
 */
public class MCPClient {
    private Thread start;
    private String mcpServer;
    private String ssePath;
    private String messagePath;

    private long blockedWaitTimeout;
    private int warnMultsRejects;
    private String bulkProcessorName = "SSEProcessor";
    private String bulkRejectMessage = "Reject sse";

 
    private int workThreads = 300;
    private int workThreadQueue = 100;
    
    
    /**
     * Mcp-Session-Id
     */
    private String sessionId;
    private Disposable fluxDisposable;
    private CountDownLatch countDownLatch = null;
    private final static Logger logger = LoggerFactory.getLogger(MCPClient.class);
	private SSEMcpCallHelper sseMcpCallHelper;
    public MCPClient(String mcpServer){
        this.mcpServer = mcpServer;
		sseMcpCallHelper = new SSEMcpCallHelper();
    }
    public String getMcpServer() {
        return mcpServer;
    }
    public MCPClient setMcpServer(String mcpServer) {
        this.mcpServer = mcpServer;
        return this;
    }
	
	
    public void init(){
        ClientConfiguration clientConfiguration = ClientConfiguration.getClientConfiguration(this.mcpServer);
        ssePath = clientConfiguration.getExtendConfig("sseendpoint");
        connect();
    }
    public void destory(){
        if(this.fluxDisposable != null){
            try {
                this.fluxDisposable.dispose();
            } catch (Exception e) {
                logger.error("关闭订阅流异常",e);
            }
        }
        start.interrupt();
        try {
            start.join();
        } catch (InterruptedException e) {
             
        }
        if(sseMcpCallHelper != null ) {
            sseMcpCallHelper.destory();
        }
    }
    private void connect(){
        countDownLatch = new CountDownLatch(1);
        start = new Thread(new Runnable() {
            @Override
            public void run() {
                Flux<String> flux = AIAgentUtil.mcpSSE(mcpServer,ssePath);
               
                Disposable disposable = flux.doOnSubscribe(subscription -> logger.info("开始订阅mcp..."))
                        .doOnNext(chunk -> {
                            handlSSEEvent(chunk);

                        }) //打印流式调用返回的问题答案片段
                        .doOnComplete(() -> {
                            logger.info("SSE订阅流完成");

                        })
                        .doOnError(error -> {
                            logger.error("SSE订阅流异常",error);

                        })
                        .subscribe();
                try {
                    
                     
                    synchronized (this) {
                        wait();
                    }
                        
                     
                } catch (InterruptedException e) {

                }
                MCPClient.this.fluxDisposable = disposable;
            }
        });
        start.start();
        try {
            countDownLatch.await();
            initialization();
            notificationsInitialized();
        } catch (InterruptedException e) {
            logger.error("MCPClient initialization interrupted", e);
        }

    }
     
    /**
     * event:endpoint
     * data:/api/v1/mcps/amap-maps/message?sessionId=2e60ceea-5419-4935-9eb4-d8766be8677a
     * @param event
     */
    private void handlSSEEvent(String event){
//        if(event.startsWith("endpoint:")){
//            logger.info(event);
//        }
//        else 
        if(event.startsWith("data:")){
            if(messagePath == null){
                messagePath = event.substring(5).trim();
                String session = messagePath.substring(messagePath.indexOf("?")+1);
                sessionId = session.substring(session.indexOf("=")+1);
                logger.info("Mcp server {} connected:{},sessionId:{}",mcpServer,messagePath,sessionId);
                sseMcpCallHelper.init(this);
                countDownLatch.countDown();
            }
			else{
				String data = event.substring(5).trim();
				sseMcpCallHelper.handleMcpSSEMessage(data);				 
			}
        }
        else{
            if(logger.isDebugEnabled()) {
                logger.debug(event);
            }
        }
    }

    public String getMessagePath() {
        return messagePath;
    }
	
    private RequestId requestId = new RequestId();
	
    public McpListToolResponse listTools(){
//        String listTools = """
//                {
//                  "jsonrpc": "2.0",
//                  "id": 1,
//                  "method": "tools/list"
//                }
//                """;
		McpListToolRequest mcpToolRequest = new McpListToolRequest();
		mcpToolRequest.setId(this.requestId.nextReqNo());
		McpListToolResponse mcpListToolResponse = this.sseMcpCallHelper.listTools(this, mcpToolRequest);
		return mcpListToolResponse;
//        String listTools = "";
//        Map<String,String> headers = new LinkedHashMap<>();
//        headers.put("Mcp-Session-Id", sessionId);
       
//		StringBuilder builder = new StringBuilder();
//		List<String> results = new ArrayList<>();
//		// 使用 collectList() 收集所有数据，然后处理
//		AIAgentUtil.stream(mcpServer, messagePath, listTools, HttpMethodName.HTTP_POST, new DataCollector() {
//			@Override
//			public void collector(String data) {
//				results.add(data);
//			}
//		});
//		
//		
//		
//		// 处理收集到的结果
//		if (results != null) {
//			for (String chunk : results) {
//				handlListToolEvent(chunk, builder);
//			}
//		}
//		logger.info("listTools:{}", builder.toString());
//		return builder.toString();
    }

    public String notificationsInitialized()
    {
//        String notificationsInitialized = """
//                {
//                  "jsonrpc": "2.0",
//                  "method": "notifications/initialized"
//                }
//                """;
//        String notificationsInitialized = "";
//        Map<String,String> headers = new LinkedHashMap<>();
//        headers.put("Mcp-Session-Id", sessionId);
		McpToolRequest notificationsInitialized = new McpToolRequest();
		notificationsInitialized.setMethod("notifications/initialized");
        String data = HttpRequestProxy.sendJsonBody(mcpServer,notificationsInitialized,messagePath,String.class);
         if(logger.isDebugEnabled()) {
             logger.debug("notificationsInitialized:{}", data);
         }
		return data;
    }
    public McpInitializedToolResponse initialization(){
//        String initJson = """
//              {"jsonrpc":"2.0","id":0,"method":"initialize",
//              "params":{"protocolVersion":"2025-06-18",
//              "capabilities":{"elicitation":{}},
//              "clientInfo":{"name":"mcp-client","version":"1.0.0"}}}
//        """;
		McpInitializedToolRequest mcpInitializedToolRequest = new McpInitializedToolRequest();
		mcpInitializedToolRequest.setId(requestId.nextReqNo());
		mcpInitializedToolRequest.clientInfo("mcp-client","1.0.0");		
		mcpInitializedToolRequest.protocolVersion("2025-06-18");
		mcpInitializedToolRequest.setJsonrpc("2.0");
		return this.sseMcpCallHelper.initializationCall(this,mcpInitializedToolRequest);
		
////        String initJson = "";
////        Map<String,String> headers = new LinkedHashMap<>();
////        headers.put("Mcp-Session-Id", sessionId);
//        String data = HttpRequestProxy.sendJsonBody(mcpServer,mcpInitializedToolRequest, messagePath,String.class);
//        if(true)
//            return data;
//         
//		logger.info("initialization:{}", builder.toString());
//        return builder.toString();

    }

    public long getBlockedWaitTimeout() {
        return blockedWaitTimeout;
    }

    public void setBlockedWaitTimeout(long blockedWaitTimeout) {
        this.blockedWaitTimeout = blockedWaitTimeout;
    }

    public int getWarnMultsRejects() {
        return warnMultsRejects;
    }

    public void setWarnMultsRejects(int warnMultsRejects) {
        this.warnMultsRejects = warnMultsRejects;
    }

    public String getBulkProcessorName() {
        return bulkProcessorName;
    }

    public void setBulkProcessorName(String bulkProcessorName) {
        this.bulkProcessorName = bulkProcessorName;
    }

    public String getBulkRejectMessage() {
        return bulkRejectMessage;
    }

    public void setBulkRejectMessage(String bulkRejectMessage) {
        this.bulkRejectMessage = bulkRejectMessage;
    }

    public int getWorkThreads() {
        return workThreads;
    }

    public void setWorkThreads(int workThreads) {
        this.workThreads = workThreads;
    }

    public int getWorkThreadQueue() {
        return workThreadQueue;
    }

    public void setWorkThreadQueue(int workThreadQueue) {
        this.workThreadQueue = workThreadQueue;
    }
}
