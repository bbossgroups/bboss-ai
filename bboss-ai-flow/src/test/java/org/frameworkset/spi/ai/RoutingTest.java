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
import org.frameworkset.spi.ai.flow.AIPlanAgent;
import org.frameworkset.spi.ai.flow.AIRouteAgent;
import org.frameworkset.spi.ai.flow.UserRouteChoiceAgent;
import org.frameworkset.spi.ai.mcp.feishu.FeishuMcpRegist;
import org.frameworkset.spi.ai.mcp.tools.MCPToolsRegist;
import org.frameworkset.spi.ai.model.ChatAgentMessage;
import org.frameworkset.spi.ai.model.LastSessionMessage;
import org.frameworkset.spi.ai.store.StoreContext;
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
        multiagentWeathor("zhipu","创建一篇关于中国首都介绍的飞书文档","glm-5.1",null);

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

    public static void multiagentWeathor(String maas, String prompt,String model,String sessionId) throws InterruptedException {
        initDB();
        ChatAgentMessage chatAgentMessage = new ChatAgentMessage()                            
                .setModel(model)
                .setMaas(maas).setPrompt(prompt);

        AIPlanAgent aiPlanAgent = new AIPlanAgent(new StoreContext()
                .setSessionId(sessionId).setSessionSize(100)                 
                .setStoreType(StoreContext.STORE_TYPE_DB)
                .setDataSource("visualops"))
                .setAgentMessage(chatAgentMessage)
                 ;

        aiPlanAgent.addAIRouteAgent(new AIRouteAgent()
                .setAgentId("Router")
                .setSystemPrompt("你是一个路由智能体。你的目标是将用户查询路由到正确的后续任务，注意你不需要回答用户的问题。")                
                .addRoutingChoice("weatherAgent","查询城市天气，并给出穿衣出行建议")
                .addRoutingChoice("docAgent","操作飞书文档")
        );



        UserRouteChoiceAgent logAgent = new UserRouteChoiceAgent(new MCPToolsRegist("visualops"))
                .setAgentId("weatherAgent");

        aiPlanAgent.addRouteChoiceAgent(logAgent);


        UserRouteChoiceAgent docAgent = new UserRouteChoiceAgent(
                new FeishuMcpRegist("feishumcp")
                        .setAppId("cli_a9d43b87aff89cd1")
                        .setAppSecret("gIhy0EbVfgQGlpNBN8r10gtqMKMnYCJs")
                        .setTools("search-user,get-user,fetch-file,search-doc,create-doc,fetch-doc,update-doc,list-docs,get-comments,add-comments")
                ).setAgentId("docAgent");

        aiPlanAgent.addRouteChoiceAgent(docAgent);
        LastSessionMessage lastSessionMessage = aiPlanAgent.chat();
        
        logger.info("serverEvent:{}", lastSessionMessage.getData());


    }

}
