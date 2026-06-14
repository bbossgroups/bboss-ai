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
import org.frameworkset.spi.ai.mcp.feishu.FeishuMcpRegist;
import org.frameworkset.spi.ai.mcp.tools.MCPToolsRegist;
import org.frameworkset.spi.ai.model.ChatAgentMessage;
import org.frameworkset.spi.ai.model.LastSessionMessage;
import org.frameworkset.spi.ai.store.StoreContext;
import org.frameworkset.spi.ai.tools.ToolsRegist;
import org.frameworkset.spi.remote.http.HttpRequestProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author biaoping.yin
 * @Date 2026/4/13
 */
public class RoutingTest {
    private static Logger logger = LoggerFactory.getLogger(StreamTest.class);
    public static void main(String[] args) throws InterruptedException, IOException {


        HttpRequestProxy.startHttpPools("application-stream.properties");


        HttpRequestProxy.startHttpPools("mcpserver.properties");

//        multiagent("qwenvlplus","qwen3.6-plus");
//        multiagent("zhipu","glm-5.1");
//        multiagentWeathor("zhipu","glm-5.1",null);
//        multiagentWeathor("zhipu","glm-5.1","6021bcca95fe4393bd4726f7b667a75a");
//        multiuserAgentWeathor("zhipu","glm-5.1","3021bcca95fe4393bd4726f7b667a75a");
//          multiagentWeathor("zhipu","查询长沙市天气，根据天气情况给出穿衣建议、出行建议","glm-5.1",null);
//        multiagentWeathor("zhipu","创建一篇关于中国首都介绍的飞书文档","glm-5.1",null);

//        multiagentWeathor("qwenvlplus","创建一篇关于中国首都介绍的飞书文档","qwen3.6-plus",null);


        multiagentWeathor("kimi","创建一篇关于中国首都介绍的飞书文档","kimi-k2.6",null);
//        multiagentWeathor("qwenvlplus","介绍一下solon","qwen3.6-plus",null);

    }
    public static void initDB(){
 

        SQLUtil.startPool("visualops",//数据源名称
                "com.mysql.cj.jdbc.Driver",//oracle驱动
                "jdbc:mysql://192.168.137.1:3306/bboss?useUnicode=true&characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true",//mysql链接串
                "root","123456",//数据库账号和口令
                "select 1 " //数据库连接校验sql
        );
    }

    public static void multiagentWeathor(String maas, String prompt,String model,String sessionId) throws InterruptedException {
        initDB();
        //定义会话实体：设置模型、maas平台，用户问题
        ChatAgentMessage chatAgentMessage = new ChatAgentMessage()                            
                .setModel(model)
                .setMaas(maas).setPrompt(prompt);

        //定义工作流智能体，设置会话存储机制为DB，设置DB数据源、当前会id以及用户id
        // 设置短期会话窗口
        AIPlanAgent aiPlanAgent = new AIPlanAgent(new StoreContext()
                .setSessionId(sessionId).setUserId("user123").setSessionSize(100)                 
                .setStoreType(StoreContext.STORE_TYPE_DB)
                .setDataSource("visualops"))
                .setAgentMessage(chatAgentMessage)
                .setAgentName("工作流智能体").setAgentId("workflowAgent")
                 ;
        //构建路由规则智能体
        aiPlanAgent.addAgent(new AIRouteAgent()
                .setAgentId("Router").setAgentName("路由规则智能体")
                .setSystemPrompt("你是一个路由智能体。你的目标是将用户查询路由到正确的后续任务，注意你不需要回答用户的问题。")                
                .addRoutingChoice("weatherAgent","查询城市天气，并给出穿衣出行建议")
                .addRoutingChoice("docAgent","操作飞书文档")
        ); 

        //构建天气查询和出现建议智能体：当用户问题匹配上时执行
        aiPlanAgent.addRouteChoiceAgent(new UserNodeAgent(new MCPToolsRegist("visualops"))
                .setAgentId("weatherAgent").setAgentName("天气查询智能体"));
        
        ToolsRegist feishuMcp = new FeishuMcpRegist("feishumcp");

        //构建飞书文档操作智能体：当用户问题匹配上时执行
        aiPlanAgent.addRouteChoiceAgent(new UserNodeAgent(feishuMcp).setAgentId("docAgent").setAgentName("飞书文档智能体"));

        //构建默认智能体：当用户问题匹配不上时执行,将直接回答问题
        aiPlanAgent.addDefaultRouteChoiceAgent(new AINodeAgent().setAgentId("defaultAgent").setAgentName("默认智能体"));
        
        //构建裁判智能体：判断是否回答了问题
        aiPlanAgent.addAgent(new AIJudgeAgent("评估结果是否回答了问题:\n#[input.query,scope=node]\n# 问题答案：\n#[answer,scope=node],回答请回复：是，否则回复：否").setAgentId("judgeAgent").setAgentName("评估智能体"));
        
        //构建最终飞书报告创建智能体：添加将问题答案创建为飞书文档的智能体
        aiPlanAgent.addAgent(new AINodeAgent("将结果创建为飞书文档", feishuMcp).setAgentId("createDocAgent").setAgentName("飞书文档创建智能体"),
                nodeTriggerContext -> {
            String judgeResult = (String) nodeTriggerContext.getFlowContextData("judgeAgent.judgeResult");
            if("是".equals(judgeResult)){
                return true;
            }
            return false;
        });
        
        //开始对话，执行对话流程，并返回会话结果
        LastSessionMessage lastSessionMessage = aiPlanAgent.chat();
        
        //输出会话结果        
        if(lastSessionMessage != null) {
            logger.info("serverEvent:{}", lastSessionMessage.getData());
        }


    }

}
