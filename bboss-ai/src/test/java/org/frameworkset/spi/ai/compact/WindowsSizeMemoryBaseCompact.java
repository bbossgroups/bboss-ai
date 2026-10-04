package org.frameworkset.spi.ai.compact;
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

import org.frameworkset.spi.ai.AIAgent;
import org.frameworkset.spi.ai.StreamTest;
import org.frameworkset.spi.ai.mcp.feishu.FeishuMcpRegist;
import org.frameworkset.spi.ai.mcp.tools.MCPToolsRegist;
import org.frameworkset.spi.ai.model.ChatAgentMessage;
import org.frameworkset.spi.ai.model.ServerEvent;
import org.frameworkset.spi.ai.store.StoreContext;
import org.frameworkset.spi.remote.http.HttpRequestProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;

/**
 *
 * @author biaoping.yin
 * @Date 2026/9/10
 */
public class WindowsSizeMemoryBaseCompact {
	private static Logger logger = LoggerFactory.getLogger(StreamTest.class);

	public static void main(String[] args) throws InterruptedException, IOException {
		//加载配置文件，启动负载均衡器,应用中只需要执行一次
//        AgentAdapterFactory.registerAgentAdapter("custom",CustomAgentAdapter.class);
		HttpRequestProxy.startHttpPools("application-stream.properties");
		
		
		HttpRequestProxy.startHttpPools("mcpserver.properties");
		

 
		streamChatWithMcpTools("custom1","shuqi","qwen3.7-plus","推荐一部穿越小说");
 
	}
	
	
	public static void streamChatWithMcpTools(String maas, String mcpServer,
											  String model, String prompt) throws InterruptedException {
		ChatAgentMessage chatAgentMessage = new ChatAgentMessage()
				
				.setSystemPrompt("You are a helpful assistant. Keep answers under two sentences.")
				.setStoreContext(new StoreContext().setSessionMemory(new ArrayList<>())
						.setSessionSize(3).setTriggerSessionSize(6)
						.setCompactModelInfo(maas, model)
						.setUserId("1234567890")
						.setSessionId("1234567890").setRequestId("1234567890"))
//                .setModel("deepseek-chat")
				.setModel(model)
				.setStream( false) ;
		chatAgentMessage.setThinking(false);
	
		MCPToolsRegist mcpToolsRegist = null;
		//feishumcp
		if(!mcpServer.equals("feishumcp")){
			mcpToolsRegist = new MCPToolsRegist(mcpServer);
		}
		else{
			
			mcpToolsRegist = new FeishuMcpRegist("feishumcp");
		}
		AIAgent agent = new AIAgent().setAgentId("压缩案例");
		agent.setToolsRegist(mcpToolsRegist);
		System.out.println("Question: " + prompt);
		ServerEvent serverEvent = agent.chat(maas,prompt,chatAgentMessage);
		
		System.out.println(serverEvent.getData()); 
		// 等待异步操作完成，否则流式异步方法执行后会因为主线程的退出而退出，看不到后续响应的报文
		String[] questions = {
				"The capital of France is Paris. Remember this fact.",
				"The population of Tokyo is about 14 million. Remember this.",
				"What is the tallest building in the world?",
				"Who wrote the novel '1984'?",
				"What programming language was created by James Gosling?",
		};
		agent.setToolsRegist(mcpToolsRegist);
		for (String question : questions) {
			System.out.println("Question: " + question);
			serverEvent = agent.chat(maas,question, chatAgentMessage);
			
			System.out.println(serverEvent.getData());
		}
		
		
	}
	
}
