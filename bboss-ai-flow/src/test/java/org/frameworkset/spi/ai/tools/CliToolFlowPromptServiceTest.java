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

import com.frameworkset.common.poolman.util.SQLUtil;
import org.frameworkset.spi.ai.flow.AIJudgeAgent;
import org.frameworkset.spi.ai.flow.AINodeAgent;
import org.frameworkset.spi.ai.flow.AIPlanAgent;
import org.frameworkset.spi.ai.model.ChatAgentMessage;
import org.frameworkset.spi.ai.model.ServerEvent;
import org.frameworkset.spi.ai.prompt.AgentResouceService;
import org.frameworkset.spi.ai.prompt.PromptResourceCache;
import org.frameworkset.spi.ai.prompt.PromptVariable;
import org.frameworkset.spi.ai.store.StoreContext;
import org.frameworkset.spi.ai.util.ClasspathResourceReader;
import org.frameworkset.spi.remote.http.HttpRequestProxy;
import org.frameworkset.util.concurrent.IntegerCount;
import reactor.core.publisher.Flux;

import java.util.concurrent.CountDownLatch;

/**
 * @author biaoping.yin
 * @Date 2026/6/24
 */
public class CliToolFlowPromptServiceTest {
	private static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CliToolFlowPromptServiceTest.class);
	
	public static void main(String[] args) {
		try {
			HttpRequestProxy.startHttpPools("application-stream.properties");
            initDB();
			PromptResourceCache.getInstance().setAgentResouceService(new AgentResouceService() {
				@Override
				public String getResourceContent(PromptVariable variable) throws Exception {
					String resource = variable.getVariableName();
					String charset = variable.getCharset();
					String content = ClasspathResourceReader.readClasspathResource(resource, charset);
					return content;
				}
			});
			callMinimaxSimple();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
    public static void initDB(){


        SQLUtil.startPool("visualops",//数据源名称
                "com.mysql.cj.jdbc.Driver",//oracle驱动
                "jdbc:mysql://192.168.137.1:3306/bboss?useUnicode=true&characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true",//mysql链接串
                "root","123456",//数据库账号和口令
                "select 1 " //数据库连接校验sql
        );
    }

    public static void callMinimaxSimple() throws InterruptedException {
		//MiniMax-M2.7
		//定义问题变量
//		String message = "当前OS为windows，生成一段符合windows语法的shell脚本，先查找占用端口808的进程，如果存在对应进程，则关闭进程，如果不存在相关进程，则无需处理,脚本正常执行完毕的情况下，立即终止并返回执行结果。\n# 结果输出要求：直接返回脚本及脚本执行结果";
		//设置模型调用参数，
		ChatAgentMessage chatAgentMessage = new ChatAgentMessage();
//		chatAgentMessage.setModel("MiniMax-M2.7").setMaas("minimax").setRetry(3);
        chatAgentMessage.setModel("qwen3.7-plus").setMaas("qwenvlplus").setRetry(3);
        //采用qwen3.7-plus模型时，需要阻止模型反复调用工具
//        String message = "当前OS为windows，帮忙查找占用端口808的进程，如果存在对应进程，则关闭进程，如果不存在相关进程，则无需处理。\n# 工具调用要求：只执行一次工具，执行后只分析结果，不要再返回工具调用信息和工具参数\n# 结果输出要求：直接返回脚本及脚本执行结果";
       
//        String message = "#[prompt.txt,type=resource]";
//        String message = "#[http://localhost:85/prompt.txt,type=url,charset=UTF-8]";
//        String message = "#[C:\\workspace\\bbossgroups\\bboss-ai\\bboss-ai-flow\\src\\test\\resources\\prompt.txt,type=file,charset=UTF-8]";
//        chatAgentMessage.setModel("deepseek-v4-pro").setMaas("deepseek").setRetry(3);
		String question = "查找占用端口808的进程";
		chatAgentMessage.setPrompt(question,true).setSystemPrompt("你是一个命令执行专家，可以根据用户要求生成符合要求的、完整的、可执行shell脚本，" +
                "脚本必须符合用户要求的指令代码，将指令脚本交由工具执行，并输出执行结果。" +
                "注意事项：脚本将通过java Process调用cmd或者sh来执行，确保脚本在目标操作系统上运行。");
		
		chatAgentMessage.setStream( true).setThinking(false).setTemperature(0.7);//.addParameter("max_tokens", 2048);
		
		CountDownLatch countDownLatch = new CountDownLatch(1);

        IntegerCount integerCount = new IntegerCount();
        // 定义工作流智能体，设置会话存储机制为DB
        AIPlanAgent planAgent = new AIPlanAgent(new StoreContext()
                .setUserId("user123").setSessionSize(100)
                .setStoreType(StoreContext.STORE_TYPE_DB)
                .setDataSource("visualops"))
                .setAgentMessage(chatAgentMessage)
                .setAgentName("命令执行工作流").setAgentId("commandExecutionWorkflowAgent");
		String message = "#[prompt.txt,type=service]";
		AINodeAgent scan2ndClosePortProcessAgent = new AINodeAgent(message)
				.setAgentId("scan2ndClosePortProcessAgent").setAgentName("扫描并关闭端口进程");
		scan2ndClosePortProcessAgent.registBeanTool(new CLIShellFunctionTool(60));
        planAgent.addAgent(scan2ndClosePortProcessAgent);
        
        planAgent.addAgent(new AIJudgeAgent("评估问题答案是否成功处理了用户提出的问题,成功处理或者如果没有查到对应进程则输出：是；脚本执行报错或者没有正确处理则输出：否\n#用户问题:\n#[input.query1,scope=node,defalut=无]\n# 问题答案：\n#[answer,scope=node]")
				.setAgentId("judgeAgent").setAgentName("评估智能体"));
        //构建最终飞书报告创建智能体：添加将问题答案创建为飞书文档的智能体
        planAgent.addConditionFlowNode(scan2ndClosePortProcessAgent,
                nodeTriggerContext -> {
                    String judgeResult = (String) nodeTriggerContext.getFlowContextData("judgeAgent.judgeResult");
                    if("否".equals(judgeResult)){
                        int i = integerCount.increament();
                        if(i >= 5) //最多执行5次
                            return false;
                        return true;
                    }
                    else{
                        logger.info("judgeAgent.judgeResult：{}",judgeResult);
                    }
                    return false;
                });
        //通过bboss httpproxy响应式异步交互接口，请求Deepseek模型服务，提交问题
		Flux<ServerEvent> flux = planAgent.chatStream();
		
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
