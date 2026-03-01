package org.frameworkset.spi.ai;
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
import org.frameworkset.spi.ai.mcp.MCPClient;
import org.frameworkset.spi.ai.mcp.model.McpListToolResponse;
import org.frameworkset.spi.ai.util.AIAgentUtil;
import org.frameworkset.spi.remote.http.HttpMethodName;
import org.frameworkset.spi.remote.http.HttpRequestProxy;
import org.slf4j.Logger;
import reactor.core.publisher.Flux;

import java.util.concurrent.CountDownLatch;

/**
 * @author biaoping.yin
 * @Date 2026/2/23
 */
public class McpClientTest {
    private static Logger logger = org.slf4j.LoggerFactory.getLogger(McpClientTest.class);
    static void init() throws InterruptedException {
        
//        String initJson = """
//                {
//                  "jsonrpc": "2.0",
//                  "id": 2,
//                  "method": "initialize",
//                  "params": {
//                    "protocolVersion": "2025-06-18",
//                    "capabilities": {
//                      "elicitation": {}
//                    },
//                    "clientInfo": {
//                      "name": "example-client",
//                      "version": "1.0.0"
//                    }
//                  }
//                }
//                """;
//        CountDownLatch countDownLatch = new CountDownLatch(1);
////        Flux<String> flux = AIAgentUtil.stream("aliyun","/api/v1/mcps/amap-maps/message?sessionId=d6e6dbdd-b27c-44b3-b5c9-971ca44bfbc9",initJson);
//        Flux<String> flux = AIAgentUtil.stream("aliyun","/api/v1/mcps/amap-maps/sse",null, HttpMethodName.HTTP_GET);
//        flux.doOnSubscribe(subscription -> logger.info("开始订阅流..."))
//                .doOnNext(chunk -> System.out.println(chunk)) //打印流式调用返回的问题答案片段
//                .doOnComplete(() -> {
//                    logger.info("\n=== 流完成 ===");
//                    countDownLatch.countDown();
//                })
//                .doOnError(error -> {
//                    logger.error("错误: " + error.getMessage(),error);
//                    countDownLatch.countDown();
//                })
//                .subscribe();
//        // 等待异步操作完成，否则流式异步方法执行后会因为主线程的退出而退出，看不到后续响应的报文
//        countDownLatch.await();
//
//        String listToolJson  = """
//                {
//                   "jsonrpc": "2.0",
//                   "id": 1,
//                   "method": "tools/list"
//                 }
//                """;
//        reponse = HttpRequestProxy.sendJsonBody("aliyun",listToolJson,"/api/v1/mcps/amap-maps/sse");
//        logger.info(reponse);
    }

    static void initGaotie() throws InterruptedException {
//        String initJson = """
//                {
//                  "jsonrpc": "2.0",
//                  "id": 2,
//                  "method": "initialize",
//                  "params": {
//                    "protocolVersion": "2025-06-18",
//                    "capabilities": {
//                      "elicitation": {}
//                    },
//                    "clientInfo": {
//                      "name": "example-client",
//                      "version": "1.0.0"
//                    }
//                  }
//                }
//                """;
//        Flux<String> flux = AIAgentUtil.stream("gaotie","/sse",initJson,HttpMethodName.HTTP_GET);
//        flux.doOnSubscribe(subscription -> logger.info("开始订阅流..."))
//                .doOnNext(chunk -> System.out.print(chunk)) //打印流式调用返回的问题答案片段
//                .doOnComplete(() -> logger.info("\n=== 流完成 ==="))
//                .doOnError(error -> logger.error("错误: " + error.getMessage(),error))
//                .subscribe();
//
//        // 等待异步操作完成，否则流式异步方法执行后会因为主线程的退出而退出，看不到后续响应的报文
//        Thread.sleep(100000000);
////
////        String listToolJson  = """
////                {
////                   "jsonrpc": "2.0",
////                   "id": 1,
////                   "method": "tools/list"
////                 }
////                """;
////        reponse = HttpRequestProxy.sendJsonBody("aliyun",listToolJson,"/api/v1/mcps/amap-maps/sse");
////        logger.info(reponse);
    }
    public static void main(String[] args) throws InterruptedException {
//        initGaotie();
//        init();
        HttpRequestProxy.startHttpPools("mcpserver.properties");
        MCPClient mcpClient = new MCPClient("gaotie");
        mcpClient.init();

        McpListToolResponse tools = mcpClient.listTools();
		
		
        logger.info("tools:{}", JsonUtil.object2json(tools));
		
    }

}
