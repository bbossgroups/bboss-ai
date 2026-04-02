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

import org.frameworkset.spi.ai.mcp.feishu.FeishuMcpRegist;
import org.frameworkset.spi.ai.mcp.tools.MCPToolsRegist;
import org.frameworkset.spi.ai.model.ChatAgentMessage;
import org.frameworkset.spi.ai.model.ServerEvent;
import org.frameworkset.spi.ai.store.AgentSessionStoreMemory;
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

        multiagent("qwenvlplus","qwen3.6-plus");
    }


    public static void multiagent(String maas, String model) throws InterruptedException {
        ChatAgentMessage chatAgentMessage = new ChatAgentMessage()
                .setSystemPrompt("你是一个日志分析和文档创建助手")
                .setSessionStore(new AgentSessionStoreMemory())
                .setModel(model)
                .setStream( false)
                .setMaxTokens(65536L);
        chatAgentMessage.setThinking(false);
        chatAgentMessage.setMaas(maas);


        AIAgent logAgent = new AIAgent("分析可视化运营平台VISUALOPS中admin用户日志",new MCPToolsRegist("visualops"),50);

        AIAgent docAgent = new AIAgent("将分析的日志结果创建为飞书文档",
                                    new FeishuMcpRegist("feishumcp","cli_a9d43b87aff89cd","gIhy0EbVfgQGlpNBN8r10gtqMKMnYCJs",
                                    "search-user,get-user,fetch-file,search-doc,create-doc,fetch-doc,update-doc,list-docs,get-comments,add-comments")
                                    ,50);
        ServerEvent serverEvent = logAgent.chat(chatAgentMessage);
        logger.info(serverEvent.getData());
        ServerEvent serverEvent1 = docAgent.chat(chatAgentMessage);
        logger.info("serverEvent:{}", serverEvent1.getData());



    }


}
