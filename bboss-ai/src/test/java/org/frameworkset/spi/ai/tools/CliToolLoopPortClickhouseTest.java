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

import com.frameworkset.common.poolman.util.DBConf;
import com.frameworkset.common.poolman.util.SQLManager;
import org.frameworkset.spi.ai.AIAgent;
import org.frameworkset.spi.ai.model.ChatAgentMessage;
import org.frameworkset.spi.ai.model.ServerEvent;
import org.frameworkset.spi.ai.store.StoreContext;
import org.frameworkset.spi.remote.http.HttpRequestProxy;
import reactor.core.publisher.Flux;

import java.util.concurrent.CountDownLatch;

/**
 * @author biaoping.yin
 * @Date 2026/6/24
 */
public class CliToolLoopPortClickhouseTest {
	private static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CliToolLoopPortClickhouseTest.class);
	
	public static void main(String[] args) {
		try {
			HttpRequestProxy.startHttpPools("application-stream.properties");
//            String message = "当前OS为windows，生成一段shell脚本，首先查找占用端口808的进程，如果存在对应进程，则关闭进程，输出端口进程信息和关闭核对结果";
//            message = "请依次执行以下命令：\n1.获取OS版本信息\n2.获取CPU信息\n3.打印OS和CPU信息\n4.查找端口808的进程\n5.如果存在对应进程，则关闭进程\n6.输出端口进程信息和关闭核对结果";
            initDB();
			callMinimaxSimple( );
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
    public static void initDB(){
 
		
		DBConf tempConf = new DBConf();
		tempConf.setPoolname("visualops");
		tempConf.setDriver("com.clickhouse.jdbc.ClickHouseDriver");
		tempConf.setJdbcurl("jdbc:clickhouse:http://100.13.6.4:28123,100.13.6.7:28123,100.13.4.6:28123/visualops?b.enableBalance=true&b.balance=roundbin");
		tempConf.setUsername("default");
		tempConf.setPassword("123456");
		tempConf.setValidationQuery("select 1 ");
		//tempConf.setTxIsolationLevel("READ_COMMITTED");
		tempConf.setJndiName("jndi-visualops" );
		tempConf.setInitialConnections(10);
		tempConf.setMinimumSize(10);
		tempConf.setMaximumSize(20);
		tempConf.setUsepool(true);
	 
		tempConf.setShowsql(false);
		SQLManager. startPool(tempConf);
    }
	public static void callMinimaxSimple( ) throws InterruptedException {
		//MiniMax-M2.7
		//定义问题变量
		
		//设置模型调用参数，
		ChatAgentMessage chatAgentMessage = new ChatAgentMessage();
//		chatAgentMessage.setModel("MiniMax-M2.7").setMaas("minimax").setRetry(3);
//        chatAgentMessage.setModel("qwen3.7-plus").setMaas("qwenvlplus").setRetry(3);
//        chatAgentMessage.setModel("qwen3.7-plus").setMaas("qwentokenplan").setRetry(3);
        
        chatAgentMessage.setMaas("deepseek").setModel("deepseek-v4-pro");
        chatAgentMessage.setRetry(3);
        
		String question = "查找和管理端口808";
		chatAgentMessage.setPrompt(question,true).setSystemPrompt("你是一个专家，可以根据用户要求获取系统信息，生成符合要求的、完整的、可执行的shell脚本" +
                "，并将生成的脚本交由工具执行，输出执行结果。注意事项：通过Java Process调用cmd或者sh来执行脚本，确保脚本在目标操作系统上能够正常运行。");
		
		chatAgentMessage.setStream( true).setThinking(false).setTemperature(0.7);//.addParameter("max_tokens", 2048);
        chatAgentMessage.setStoreContext(new StoreContext()
                .setUserId("user123").setSessionSize(100).setRequestId("request123")
                .setStoreType(StoreContext.STORE_TYPE_DB)
                .setDataSource("visualops").setClickhouseCluster("vops_3shards_1replicas"));
		
		CountDownLatch countDownLatch = new CountDownLatch(1);
		String message = "#[loopprompt.txt,type=resource]";
		AIAgent agent = new AIAgent(message);
        agent.setEnableLoopToolCall(true);//启用智能体多次调用工具机制
        agent.setMaxLoopToolCalls(80);
        //注册获取当前操作系统OS信息工具：框架内置工具
        agent.registBeanTool(new GetOSFunctionTool(60));
        //注册脚本执行工具，会根据获取到的OS信息，生成对应的OS环境命令行脚本进行执行：框架内置工具
        agent.registBeanTool(new CLIShellFunctionTool(60));
		agent.registBeanTool(new FileFunctionTool("C:\\data\\ai\\aigenfiles\\tools\\"))
				.setKeywordToolSearcher("获取OS、OS版本、OS架构以及CPU信息","将内容写入到指定文件","执行shell脚本","获取服务器时间");
		 
		//通过bboss httpproxy响应式异步交互接口，请求Deepseek模型服务，提交问题
		Flux<ServerEvent> flux = agent.streamChat(chatAgentMessage);
		
		flux.doOnSubscribe(subscription -> logger.info("开始订阅流..."))
				.doOnNext(chunk -> {
					
					if(chunk.getData() != null) {
						System.out.print(chunk.getData());
                        
					}
                    if(chunk.isStepType()){
                        System.out.println();
                    }
//                    if(chunk.isDone()){
//                        System.out.println();
//                    }
//					
				}) //打印流式调用返回的问题答案片段
				.doOnComplete(() -> {countDownLatch.countDown();System.out.println();logger.info("\n=== 流完成 ===");})
				.doOnError(error ->{countDownLatch.countDown(); logger.error("错误: " + error.getMessage(),error);})
				.subscribe();
		
		// 等待异步操作完成，否则流式异步方法执行后会因为主线程的退出而退出，看不到后续响应的报文
		countDownLatch.await();
       
	}
    
    
    
	
}
