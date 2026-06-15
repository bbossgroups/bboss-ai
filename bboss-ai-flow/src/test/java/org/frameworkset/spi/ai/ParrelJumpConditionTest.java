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
import org.frameworkset.spi.ai.flow.*;
import org.frameworkset.spi.ai.model.ChatAgentMessage;
import org.frameworkset.spi.ai.model.LastSessionMessage;
import org.frameworkset.spi.ai.store.StoreContext;
import org.frameworkset.spi.remote.http.HttpRequestProxy;
import org.frameworkset.tran.jobflow.context.JobFlowNodeExecuteContext;
import org.frameworkset.tran.jobflow.context.NodeTriggerContext;
import org.frameworkset.tran.jobflow.script.TriggerScriptAPI;
import org.frameworkset.util.concurrent.IntegerCount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author biaoping.yin
 * @Date 2026/4/13
 */
public class ParrelJumpConditionTest {
    private static Logger logger = LoggerFactory.getLogger(ParrelJumpConditionTest.class);
    public static void main(String[] args) throws InterruptedException, IOException {


        HttpRequestProxy.startHttpPools("application-stream.properties");


        HttpRequestProxy.startHttpPools("mcpserver.properties");


        multiagentIntroduceProvinces("qwenvlplus","介绍省份智能体","qwen3.6-plus",null);

    }
    public static void initDB(){


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
                .setModel(model).setThinking(false)
                .setMaas(maas).setPrompt(prompt);
        StoreContext storeContext = new StoreContext();
        storeContext.setAgentMessageTypeConvertor(new CustomAgentMessageTypeConvertor());
        //定义工作流智能体，设置会话存储机制为DB，设置DB数据源、当前会id以及用户id
        // 设置短期会话窗口
        AIPlanAgent planAgent = new AIPlanAgent(storeContext .setSessionId(sessionId).setUserId("user123").setSessionSize(100)                 
                .setStoreType(StoreContext.STORE_TYPE_DB).setRequestId("request123")
                .setDataSource("visualops"))
                .setAgentMessage(chatAgentMessage)
                .setAgentName("工作流智能体").setAgentId("workflowAgent")
                 ;
        AIBaseNodeAgent introduceProvinces = new AINodeAgent("用200字介绍中国有多少个省份和直辖市").setAgentName("介绍中国省份和直辖市").setAgentId("introduceProvinces");
        planAgent.addAgent(introduceProvinces);
        planAgent.addAgent(new AIFlowNode() {
            /**
             * 由子类继承和实现
             *
             * @param jobFlowNodeExecuteContext
             * @return
             */
            @Override
            public Object call(JobFlowNodeExecuteContext jobFlowNodeExecuteContext) {
                //生成一个10以内的随机整数，如果随机数是偶数则触发节点
                int randomInt = (int) (Math.random() * 10);
                logger.info("randomInt:{}", randomInt);
                jobFlowNodeExecuteContext.addJobFlowContextData("randomInt", randomInt);
                return null;
            }
        });
        //1.定义一个后序条件跳转节点
        AIFlowNode aiFlowNode = new AIFlowNode() {
            @Override
            public Object call(JobFlowNodeExecuteContext jobFlowNodeExecuteContext)   {
                logger.info("call 自定义节点customNode（满足条件时，会直接跳转到这个节点执行，绕过aiParrelAgent和循环跳转节点introduceProvinces，直接执行aiFlowNode后续流程节点）。");
                jobFlowNodeExecuteContext.addJobFlowContextData("customNode", "customNodeData");
                return null;
            }
        };
        //2.添加后序条件跳转节点
        planAgent.addConditionFlowNode(true,aiFlowNode, new TriggerScriptAPI() {
            @Override
            public boolean needTrigger(NodeTriggerContext nodeTriggerContext) throws Exception {
                int randomInt = (int) nodeTriggerContext.getFlowContextData("randomInt");
                
                if (randomInt % 2 == 0) {
                    return true;
                }
                return false;
            }
        });
        
        AIFlowNode aiFlowNode1 = new AIFlowNode() {
            @Override
            public Object call(JobFlowNodeExecuteContext jobFlowNodeExecuteContext)   {
                logger.info("call 自定义节点customNode1。");
                jobFlowNodeExecuteContext.addJobFlowContextData("customNode", "customNodeData1");
                return null;
            }
        };
        planAgent.addConditionFlowNode(aiFlowNode1, new TriggerScriptAPI() {
            @Override
            public boolean needTrigger(NodeTriggerContext nodeTriggerContext) throws Exception {
                //生成一个10以内的随机整数，如果随机数是奇数则触发节点
                int randomInt = (int) nodeTriggerContext.getFlowContextData("randomInt");
                if (randomInt % 2 != 0) {
                    return true;
                }
                return false;
            }
        });

        AIFlowNode aiFlowNode2 = new AIFlowNode() {
            @Override
            public Object call(JobFlowNodeExecuteContext jobFlowNodeExecuteContext)   {
                logger.info("call 自定义节点customNode2。");
                jobFlowNodeExecuteContext.addJobFlowContextData("customNode", "customNodeData2");
                return null;
            }
        };
        planAgent.addConditionFlowNode(aiFlowNode2, true);
        //构建并行子智能体
        AIParrelAgent aiParrelAgent = new AIParrelAgent(planAgent).setAgentId("aiParrelAgent").setAgentName("并行智能体");
        aiParrelAgent.addAgent(new AINodeAgent("用50字介绍湖南，并且和介绍中国省份和直辖市内容合并输出").setAgentId("jieshaohunan").setAgentName("用50字介绍湖南"));
        aiParrelAgent.addAgent(new UserNodeAgent("用50字介绍湖北").setAgentId("jieshaohubei").setAgentName("用50字介绍湖北"));
        aiParrelAgent.addAgent(new UserNodeAgent("用50字介绍江西").setAgentId("jieshaojiangxi").setAgentName("用50字介绍江西"));
        aiParrelAgent.addAgent(new UserNodeAgent("将下面的文字翻译为英文（不要回答问题）：用50字介绍江西").setAgentId("translate").setAgentName("将文字翻译为英文"));
        planAgent.addAgent(aiParrelAgent);
        IntegerCount integerCount = new IntegerCount();
        planAgent.addConditionFlowNode(true,introduceProvinces, new TriggerScriptAPI() {
            @Override
            public boolean needTrigger(NodeTriggerContext nodeTriggerContext) throws Exception {
                int i = integerCount.increament();
                if(i == 1) {
                    return true;
                }
                else{
                    return false;
                }
            }
        });
        //3.添加后序条件跳转节点到主流程中，前面的条件分支节点会直接跳转的本节点
        planAgent.addAgent(aiFlowNode);
        planAgent.addAgent(new AIFlowNode() {
            @Override
            public Object call(JobFlowNodeExecuteContext jobFlowNodeExecuteContext)   {
                logger.info("call 自定义节点3。");
                jobFlowNodeExecuteContext.addJobFlowContextData("customNode", "customNodeData3");
                return null;
            }
        });

        planAgent.addAgent(new AIFlowNode() {
            @Override
            public Object call(JobFlowNodeExecuteContext jobFlowNodeExecuteContext)   {
                logger.info("call 自定义节点4。customNode:{}", jobFlowNodeExecuteContext.getJobFlowContextData("customNode"));
                return null;
            }
        });
        
        
        //开始对话，执行对话流程，并返回会话结果   
        LastSessionMessage lastSessionMessage = planAgent.chat();
        
        //输出会话结果        
        if(lastSessionMessage != null) {
            String data = lastSessionMessage.getData();
            logger.info("serverEvent:{}", data);
        }


    }

}
