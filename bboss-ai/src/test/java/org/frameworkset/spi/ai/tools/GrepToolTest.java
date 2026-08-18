package org.frameworkset.spi.ai.tools;
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
import org.frameworkset.spi.ai.model.ChatAgentMessage;
import org.frameworkset.spi.ai.model.ServerEvent;
import org.frameworkset.spi.remote.http.HttpRequestProxy;
import reactor.core.publisher.Flux;

import java.util.concurrent.CountDownLatch;

/**
 *
 * @author biaoping.yin
 * @Date 2026/8/17
 */
public class GrepToolTest {
	static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GrepToolTest.class);
	public static void main(String[] args) {
		HttpRequestProxy.startHttpPools("application-stream.properties");
		//设置模型调用参数，
		ChatAgentMessage chatAgentMessage = new ChatAgentMessage();
//		chatAgentMessage.setModel("MiniMax-M2.7").setMaas("minimax").setRetry(3);
//        chatAgentMessage.setModel("qwen3.7-plus").setMaas("qwenvlplus").setRetry(3);
		chatAgentMessage.setModel("deepseek-v4-pro");
		chatAgentMessage.setMaas("deepseek");
		String question = "检索包含关键字多轮会话的文件";
		chatAgentMessage.setPrompt(question).setSystemPrompt("你是一个文件检索专家，可以根据用户要求从文件中检索包含用户要求关键字的文件内容");
		
		chatAgentMessage.setStream( true).setThinking(false);//.addParameter("max_tokens", 2048);
		
		CountDownLatch countDownLatch = new CountDownLatch(1);
		String message = "根据用户问题：#[input.query]，调用文件检索工具grep，检索包含用户问题的文件内容。如果用户问题中没有指定文件目录，则将目录设置为空";
		AIAgent aiAgent = new AIAgent(message);
		aiAgent.registBeanTool(new GrepFunctionTool(60).addBaseDirectory("C:\\workspace\\bbossgroups\\bboss-elasticsearch\\docs"));
		
		//通过bboss httpproxy响应式异步交互接口，请求Deepseek模型服务，提交问题
		Flux<ServerEvent> flux = aiAgent.streamChat(chatAgentMessage);
		
		flux.doOnSubscribe(subscription -> logger.info("开始订阅流..."))
				.doOnNext(chunk -> {
					
					if(chunk.getData() != null) {
						System.out.print(chunk.getData());
					}
//					
				}) //打印流式调用返回的问题答案片段
				.doOnComplete(() -> {countDownLatch.countDown();System.out.println();logger.info("\n=== 流完成 ===");})
				.doOnError(error ->{countDownLatch.countDown(); logger.error("错误: " + error.getMessage(),error);})
				.subscribe();
		try {
			countDownLatch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
