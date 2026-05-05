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

import com.frameworkset.common.poolman.util.SQLUtil;
import org.frameworkset.spi.ai.flow.AINodeAgent;
import org.frameworkset.spi.ai.flow.AIParrelAgent;
import org.frameworkset.spi.ai.flow.AIPlanAgent;
import org.frameworkset.spi.ai.flow.UserNodeAgent;
import org.frameworkset.spi.ai.model.ChatAgentMessage;
import org.frameworkset.spi.ai.model.LastSessionMessage;
import org.frameworkset.spi.ai.model.ServerEvent;
import org.frameworkset.spi.ai.store.StoreContext;
import org.frameworkset.spi.remote.http.HttpRequestProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * @author biaoping.yin
 * @Date 2026/4/13
 */
public class ParrelStreamTest {
    private static Logger logger = LoggerFactory.getLogger(ParrelStreamTest.class);
    public static void main(String[] args) throws InterruptedException, IOException {


        HttpRequestProxy.startHttpPools("application-stream.properties");


        HttpRequestProxy.startHttpPools("mcpserver.properties");


        multiagentIntroduceProvinces("kimi","介绍省份智能体","kimi-k2.6",null);

    }
    public static void initDB(){
//        SQLUtil.startPool("visualops",//数据源名称
//                "com.mysql.cj.jdbc.Driver",//oracle驱动
//                "jdbc:mysql://10.13.6.127:3306/visualops?useUnicode=true&characterEncoding=utf-8&useSSL=false",//mysql链接串
//                "root","passwd",//数据库账号和口令
//                "select 1 " //数据库连接校验sql
//        );

        SQLUtil.startPool("visualops",//数据源名称
                "com.mysql.cj.jdbc.Driver",//oracle驱动
                "jdbc:mysql://192.168.137.1:3306/bboss?useUnicode=true&characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true",//mysql链接串
                "root","123456",//数据库账号和口令
                "select 1 " //数据库连接校验sql
        );
    }

    public static void multiagentIntroduceProvinces(String maas, String prompt, String model, String sessionId) throws InterruptedException {
        initDB();
        //定义会话实体：设置模型、maas平台，用户问题
        ChatAgentMessage chatAgentMessage = new ChatAgentMessage()                            
                .setModel(model).setThinking(false).setStream(true)
                .setMaas(maas).setPrompt(prompt);

        //定义工作流智能体，设置会话存储机制为DB，设置DB数据源、当前会id以及用户id
        // 设置短期会话窗口
        AIPlanAgent aiPlanAgent = new AIPlanAgent(new StoreContext()
                .setSessionId(sessionId).setUserId("user123").setSessionSize(100)                 
                .setStoreType(StoreContext.STORE_TYPE_DB).setRequestId("request123")
                .setDataSource("visualops"))
                .setAgentMessage(chatAgentMessage)
                .setAgentName("工作流智能体").setAgentId("workflowAgent")
                 ;

        aiPlanAgent.addAgent(new AINodeAgent("用200字介绍中国有多少个省份和直辖市").setAgentName("介绍中国省份和直辖市").setAgentId("introduceProvinces"));
        //构建并行智能体
        AIParrelAgent aiParrelAgent = new AIParrelAgent(aiPlanAgent).setAgentId("aiParrelAgent").setAgentName("共享任务节点");
        aiParrelAgent.addAgent(new AINodeAgent("用50字介绍湖南").setAgentId("jieshaohunan").setAgentName("用50字介绍湖南"));
        aiParrelAgent.addAgent(new UserNodeAgent("用50字介绍湖北").setAgentId("jieshaohubei").setAgentName("用50字介绍湖北"));
        aiParrelAgent.addAgent(new UserNodeAgent("用50字介绍江西").setAgentId("jieshaojiangxi").setAgentName("用50字介绍江西"));   
        aiPlanAgent.addParrelAgent(aiParrelAgent);

        //开始对话，执行对话流程，并返回会话结果
        Flux<ServerEvent> flux = aiPlanAgent.chatStream();
// 用于累积完整的回答
        StringBuilder completeAnswer = new StringBuilder();
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
                        if(!event.isDone() ) {
                            // 累积回答内容
                            if(event.getData() != null) {
                                completeAnswer.append(event.getData());
                            }
                        } else  {

                            if( completeAnswer.length() > 0) {
                                // 当收到完成信号且有累积内容时，将完整回答添加到会话记忆
                                chatAgentMessage.addAgentResultSessionMessage(completeAnswer.toString(),event.getAgent());
                                completeAnswer.setLength(0);


                            }
                            //完整的事件消息框架内部已经添加到会话记忆，无需再次添加到会话记忆
//                            else if(event.getData() != null){
//                                chatAgentMessage.addAgentResultSessionMessage(event.getData(),event.getAgent());
//                            }

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
