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

import com.frameworkset.util.SimpleStringUtil;
import org.frameworkset.spi.ai.flow.AIContainerAgent;
import org.frameworkset.spi.ai.flow.AIFlowNode;
import org.frameworkset.spi.ai.flow.AIPlanAgent;
import org.frameworkset.spi.ai.flow.UserNodeAgent;
import org.frameworkset.spi.ai.mcp.feishu.FeishuMcpRegist;
import org.frameworkset.spi.ai.model.ChatAgentMessage;
import org.frameworkset.spi.ai.model.ServerEvent;
import org.frameworkset.spi.ai.store.StoreContext;
import org.frameworkset.spi.ai.tools.ToolsRegist;
import org.frameworkset.tran.jobflow.NodeTrigger;
import org.frameworkset.tran.jobflow.context.JobFlowNodeExecuteContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.concurrent.CountDownLatch;

import static org.frameworkset.spi.ai.StreamTest.initDB;

/**
 * @author biaoping.yin
 * @Date 2026/5/16
 */
public class CondtionTest {
    private static Logger logger = LoggerFactory.getLogger(StreamTest.class);
    public static void multiagentWeathor(String maas, String prompt,String model,String sessionId) throws InterruptedException {
        initDB();
        //定义会话实体：设置模型、maas平台，用户问题
        ChatAgentMessage chatAgentMessage = new ChatAgentMessage()
                .setModel(model)
                .setMaas(maas).setPrompt(prompt).setStream(true);

        //定义工作流智能体，设置会话存储机制为DB，设置DB数据源、当前会id以及用户id
        // 设置短期会话窗口
        AIPlanAgent planAgent = new AIPlanAgent(new StoreContext()
                .setSessionId(sessionId).setUserId("user123")
                .setRequestId(SimpleStringUtil.getUUID32())
                .setSessionSize(100)
                .setStoreType(StoreContext.STORE_TYPE_DB)
                .setDataSource("visualops"))
                .setAgentMessage(chatAgentMessage)
                .setAgentName("工作流智能体").setAgentId("workflowAgent");
        planAgent.addAgent(new AIFlowNode() {


            /**
             * 由子类继承和实现
             *
             * @param jobFlowNodeExecuteContext
             * @return
             */
            @Override
            public Object call(JobFlowNodeExecuteContext jobFlowNodeExecuteContext) {
                jobFlowNodeExecuteContext.addJobFlowContextData("routeChoice","docAgent");//模拟设置后续节点id
                return null;
            }
        });
        ToolsRegist feishuMcp = new FeishuMcpRegist("feishumcp");
        planAgent.addConditionFlowNode(new UserNodeAgent(feishuMcp).setAgentId("docAgent").setAgentName("飞书文档智能体"),
                nodeTriggerContext -> {
                    String agentId = (String) nodeTriggerContext.getFlowContextData("routeChoice",true);
                    
                    if(agentId != null && agentId.equals("docAgent")){
                        return true;
                    }
                    return false;
                });
        planAgent.addConditionFlowNode(new UserNodeAgent(feishuMcp).setAgentId("logAgent").setAgentName("日志分析智能体"),
                nodeTriggerContext -> {
                    String agentId = (String) nodeTriggerContext.getFlowContextData("routeChoice",true);
                    
                    if(agentId != null && agentId.equals("logAgent")){
                        return true;
                    }
                    return false;
                });

        planAgent.addConditionFlowNode(new UserNodeAgent( ).setAgentId("defaultAgent").setAgentName("默认智能体直接回答问题"),true);

        planAgent.addAnotherConditionJobFlowNodeAgent(false,new UserNodeAgent(feishuMcp).setAgentId("weatherAgent").setAgentName("天气分析智能体"), nodeTriggerContext -> {
                            String agentId = (String) nodeTriggerContext.getFlowContextData("routeChoice",true);
        
                            if(agentId != null && agentId.equals("weatherAgent")){
                                return true;
                            }
                            return false;
                        });
//开始对话，执行对话流程，并返回会话结果
        Flux<ServerEvent> flux = planAgent.chatStream();
 
        CountDownLatch countDownLatch = new CountDownLatch(1);
        flux
                .doOnSubscribe(subscription -> logger.info("开始订阅流..."))
//                .limitRate(5) //背压：限制请求速率
//                .buffer(3) //缓冲：每3个元素缓冲一次
//                .doOnNext(bufferedEvents -> {
                .doOnNext(event -> {
                    // 处理模型响应并更新会话记忆
//                    for(ServerEvent event : bufferedEvents) 
                    {
                        //答案前后都可以添加链接和标题，实现相关知识资料链接
                        if(event.isFirst() ){
                            if(!event.isToolCallResponse()) {
                                event.addExtendData("url", "https://www.bbossgroups.com");
                                event.addExtendData("title", "bboss官网");
                            }
                        }

                        if(event.isDone()){
                            event.addExtendData("url", "https://www.bbossgroups.com");
                            event.addExtendData("title", "bboss官网");
                        }
                        if(event.getData() != null)
                            System.out.print(event.getData());




                        if(event.isToolCallsType()) {
                            System.out.println();
                            System.out.println("开始执行工具：");
                        }


                        if(event.isDone() || event.finished()){
                            System.out.println();
                        }
                        
                    }
                }).doOnComplete(() -> {
                    logger.info("\n=== 流完成 ===");
                    countDownLatch.countDown();
                })
                .doOnError(error -> {
                    logger.error("错误: " + error.getMessage(),error);
                    countDownLatch.countDown();
                })
                .subscribe();
        // 等待异步操作完成，否则流式异步方法执行后会因为主线程的退出而退出，看不到后续响应的报文
        countDownLatch.await();
    }

}
