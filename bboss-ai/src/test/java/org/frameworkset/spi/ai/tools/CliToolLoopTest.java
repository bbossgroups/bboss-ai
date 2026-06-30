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
 * @author biaoping.yin
 * @Date 2026/6/24
 */
public class CliToolLoopTest {
	private static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CliToolLoopTest.class);
	
	public static void main(String[] args) {
		try {
			HttpRequestProxy.startHttpPools("application-stream.properties");
            String message = "当前OS为windows，生成一段shell脚本，首先查找占用端口808的进程，如果存在对应进程，则关闭进程，输出端口进程信息和关闭核对结果";
            message = "获取OS版本信息，然后获取CPU信息，最后打印OS和CPU信息";
			callMinimaxSimple(message);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	public static void callMinimaxSimple(String message) throws InterruptedException {
		//MiniMax-M2.7
		//定义问题变量
		
		//设置模型调用参数，
		ChatAgentMessage chatAgentMessage = new ChatAgentMessage();
//		chatAgentMessage.setModel("MiniMax-M2.7").setMaas("minimax").setRetry(3);
//        chatAgentMessage.setModel("qwen3.7-plus").setMaas("qwenvlplus").setRetry(3);
        chatAgentMessage.setModel("deepseek-v4-pro");
        chatAgentMessage.setMaas("deepseek");
		chatAgentMessage.setPrompt(message).setSystemPrompt("你是一个专家，可以根据用户要求生成符合要求的、完整的、可执行shell脚本，脚本中可以包含完成用户要求的多条指令代码，并将生成的脚本交由工具执行，输出执行结果。注意事项：脚本将通过java Process调用cmd或者sh来执行，确保脚本在目标操作系统上运行。");
		
		chatAgentMessage.setStream( true).setThinking(false).setTemperature(0.7);//.addParameter("max_tokens", 2048);
		
		CountDownLatch countDownLatch = new CountDownLatch(1);
		AIAgent aiAgent = new AIAgent();
        aiAgent.setEnableLoopToolCall(true);
        aiAgent.registBeanTool(new GetOSFunctionTool(60));
        aiAgent.registBeanTool(new CLIShellFunctionTool(60));
		 
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
		
		// 等待异步操作完成，否则流式异步方法执行后会因为主线程的退出而退出，看不到后续响应的报文
		countDownLatch.await();
       
	}
    
    
    
	
}
