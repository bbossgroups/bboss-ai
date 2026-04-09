package org.frameworkset.spi.ai;
/**
 * Copyright 2025 bboss
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
import com.frameworkset.util.SimpleStringUtil;
import org.frameworkset.spi.ai.mcp.feishu.FeishuMcpRegist;
import org.frameworkset.spi.ai.mcp.tools.MCPToolsRegist;
import org.frameworkset.spi.ai.model.ChatAgentMessage;
import org.frameworkset.spi.ai.model.ServerEvent;
import org.frameworkset.spi.ai.store.AgentSessionStoreMemory;
import org.frameworkset.spi.ai.store.StoreContext;
import org.frameworkset.spi.remote.http.HttpRequestProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author biaoping.yin
 * @Date 2025/10/11
 */
public class StreamTest {
    private static Logger logger = LoggerFactory.getLogger(StreamTest.class);
    public static void main(String[] args) throws InterruptedException, IOException {
       
       
        HttpRequestProxy.startHttpPools("application-stream.properties");
        
		
		HttpRequestProxy.startHttpPools("mcpserver.properties");

//        multiagent("qwenvlplus","qwen3.6-plus");
//        multiagent("zhipu","glm-5");
//        multiagentWeathor("zhipu","glm-5",null);
        multiagentWeathor("zhipu","glm-5","1021bcca95fe4393bd4726f7b667a75a");
        
        
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
    public static void multiagent(String maas, String model) throws InterruptedException {
        initDB();
        ChatAgentMessage chatAgentMessage = new ChatAgentMessage()
                .setSystemPrompt("你是一个日志分析和文档创建助手")
                .setStoreContext(new StoreContext()
                        .setSessionMemory(new ArrayList<>()).setSessionSize(10)
                        .setStoreType(StoreContext.STORE_TYPE_DB)
                        .setDataSource("visualops").setSessionId(SimpleStringUtil.getUUID32()).setUserId("admin")
                )
                .setModel(model)
                .setStream( false)
                .setMaxTokens(65536L);
        chatAgentMessage.setThinking(false);
        chatAgentMessage.setMaas(maas);


        AIAgent logAgent = new AIAgent("分析可视化运营平台VISUALOPS中admin用户日志",new MCPToolsRegist("visualops"),50).setAgentId("logAgent");

        AIAgent docAgent = new AIAgent("将分析的日志结果创建为飞书文档",
                                    new FeishuMcpRegist("feishumcp")
                                    .setAppId("cli_a9d43b87aff89cd1")
                                    .setAppSecret("gIhy0EbVfgQGlpNBN8r10gtqMKMnYCJs")
                                    .setTools("search-user,get-user,fetch-file,search-doc,create-doc,fetch-doc,update-doc,list-docs,get-comments,add-comments")
                                    ,50).setAgentId("docAgent");
        ServerEvent serverEvent = logAgent.chat(chatAgentMessage);
        logger.info(serverEvent.getData());
        ServerEvent serverEvent1 = docAgent.chat(chatAgentMessage);
        logger.info("serverEvent:{}", serverEvent1.getData());



    }

    public static void multiagentWeathor(String maas, String model,String sessionId) throws InterruptedException {
        initDB();
        ChatAgentMessage chatAgentMessage = new ChatAgentMessage()
                .setSystemPrompt("你是一个天气分析和文档创建助手")
                .setStoreContext(new StoreContext()
                        .setSessionMemory(new ArrayList<>()).setSessionSize(10)
                        .setStoreType(StoreContext.STORE_TYPE_DB)
//                        .setStoreType(StoreContext.STORE_TYPE_MEMORY)
                        .setDataSource("visualops")//.setSessionId(sessionId == null?SimpleStringUtil.getUUID32():sessionId).setUserId("admin")
                )
                .setModel(model)
                .setStream( false)
                .setMaxTokens(65536L);
        chatAgentMessage.setThinking(false);
        chatAgentMessage.setMaas(maas);


        AIAgent logAgent = new AIAgent("查询长沙市天气，并给出穿衣出行建议",new MCPToolsRegist("visualops"),50)
                .setAgentId("logAgent");

        AIAgent docAgent = new AIAgent("将长沙市天气查询结果和出穿衣出行建议创建为飞书文档",
                new FeishuMcpRegist("feishumcp")
                        .setAppId("cli_a9d43b87aff89cd1")
                        .setAppSecret("gIhy0EbVfgQGlpNBN8r10gtqMKMnYCJs")
                        .setTools("search-user,get-user,fetch-file,search-doc,create-doc,fetch-doc,update-doc,list-docs,get-comments,add-comments")
                ,50).setAgentId("docAgent");
        ServerEvent serverEvent = logAgent.chat(chatAgentMessage);
        logger.info(serverEvent.getData());
        ServerEvent serverEvent1 = docAgent.chat(chatAgentMessage);
        logger.info("serverEvent:{}", serverEvent1.getData());



    }


}
