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

import org.frameworkset.spi.ai.mcp.model.*;
import org.frameworkset.spi.ai.mcp.sse.SSEMcpCallHelper;
import org.frameworkset.spi.ai.util.AIAgentUtil;
import org.frameworkset.spi.remote.http.ClientConfiguration;
import org.frameworkset.spi.remote.http.HttpRequestProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.util.concurrent.CountDownLatch;

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
    private int warnMultsRejects = 1000;
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
    private boolean sseStreamCompleted;
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
        connect(false);
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
    private Object reconnectedLock = new Object();

    /**
     * 某些情况下，MCP服务器可能会断开连接，需要重新连接
     */
    private void reconnected(){
        if(!sseStreamCompleted){
            return;
        }
        synchronized (reconnectedLock){
            if(!sseStreamCompleted){
                return;
            }
            if(fluxDisposable != null) {
                this.fluxDisposable.dispose();
            }
            this.messagePath = null;
            this.fluxDisposable = null;
            this.start.interrupt();
            try {
                this.start.join();
            } catch (InterruptedException e) {
                
            }
            this.sseMcpCallHelper.clearCalls();
            connect(true);
            sseStreamCompleted = false;
        }
    }
    private void setsseStreamCompleted(){
        synchronized (reconnectedLock){
            sseStreamCompleted = true;
        }
		start.interrupt();
    }
    private void connect(boolean reconnected){
        countDownLatch = new CountDownLatch(1);
        start = new Thread(new Runnable() {
            @Override
            public void run() {
                Flux<String> flux = AIAgentUtil.mcpSSE(mcpServer,ssePath);
               
                Disposable disposable = flux.doOnSubscribe(subscription -> logger.info("{} 开始订阅mcp by {}",mcpServer,reconnected?"reconnected":"connect"))
                        .doOnNext(chunk -> {
                            handlSSEEvent(chunk,reconnected);

                        }) //打印流式调用返回的问题答案片段
                        .doOnComplete(() -> {

                            setsseStreamCompleted();
                            logger.info("{} SSE订阅流完成", mcpServer);

                        })
                        .doOnError(error -> {
                            setsseStreamCompleted();
                            logger.error(mcpServer+" SSE订阅流异常",error);
                            

                        })
                        .subscribe();
                try {
                    MCPClient.this.fluxDisposable = disposable;
                     
                    synchronized (this) {
                        wait();
                    }
                    logger.info("{} SSE订阅流关闭啦。", mcpServer);    
                     
                } catch (InterruptedException e) {
                    logger.info("{} SSE订阅流关闭啦。", mcpServer);
                }
                
            }
        });
        start.start();
        try {
            countDownLatch.await();
            initialization();
            notificationsInitialized();
        } catch (InterruptedException e) {
            logger.error("MCPClient "+mcpServer+" initialization interrupted", e);
        }

    }
     
    /**
     * event:endpoint
     * data:/api/v1/mcps/amap-maps/message?sessionId=2e60ceea-5419-4935-9eb4-d8766be8677a
     * @param event
     */
    private void handlSSEEvent(String event, boolean reconnected){
//        if(event.startsWith("endpoint:")){
//            logger.info(event);
//        }
//        else 
        if(event.startsWith("data:")){
            if(messagePath == null){
                messagePath = event.substring(5).trim();
                String session = messagePath.substring(messagePath.indexOf("?")+1);
                sessionId = session.substring(session.indexOf("=")+1);
                if(!reconnected) {
                    sseMcpCallHelper.init(this);
                    logger.info("Mcp server {} connected:{},sessionId:{}", mcpServer, messagePath, sessionId);
                }
                else{
                    logger.info("Mcp server {} reconnected:{},sessionId:{}", mcpServer, messagePath, sessionId);
                }
               
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
        reconnected();
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
//        this.sseStreamCompleted = true;
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
             logger.debug("{} notificationsInitialized:{}",mcpServer, data);
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
