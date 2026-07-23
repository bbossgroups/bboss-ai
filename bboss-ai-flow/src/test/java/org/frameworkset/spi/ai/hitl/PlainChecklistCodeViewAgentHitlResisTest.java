package org.frameworkset.spi.ai.hitl;
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
import org.frameworkset.nosql.redis.RedisConfig;
import org.frameworkset.nosql.redis.RedisDB;
import org.frameworkset.nosql.redis.RedisFactory;
import org.frameworkset.spi.ai.flow.AINodeAgent;
import org.frameworkset.spi.ai.flow.AIPlanAgent;
import org.frameworkset.spi.ai.hitl.cluster.RedisHitlTaskCallListener;
import org.frameworkset.spi.ai.model.ChatAgentMessage;
import org.frameworkset.spi.ai.model.ServerEvent;
import org.frameworkset.spi.ai.skill.SkillsToolRegist;
import org.frameworkset.spi.ai.store.AgentSessionService;
import org.frameworkset.spi.ai.store.StoreContext;
import org.frameworkset.spi.ai.store.db.AgentSessionServiceImpl;
import org.frameworkset.spi.ai.tools.FileFunctionTool;
import org.frameworkset.spi.ai.tools.HitlTaskcallTool;
import org.frameworkset.spi.remote.http.HttpRequestProxy;
import reactor.core.publisher.Flux;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * 智能体集群部署场景：基于redis实现人工介入任务和智能体之间的通讯机制，适合于智能体集群部署场景
 * @author biaoping.yin
 * @Date 2026/7/15
 */
public class PlainChecklistCodeViewAgentHitlResisTest {
	private static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PlainChecklistCodeViewAgentHitlResisTest.class);
	
	public static void main(String[] args) {
		try {
			HttpRequestProxy.startHttpPools("application-stream.properties");
//            String message = "当前OS为windows，生成一段shell脚本，首先查找占用端口808的进程，如果存在对应进程，则关闭进程，输出端口进程信息和关闭核对结果";
//            message = "请依次执行以下命令：\n1.获取OS版本信息\n2.获取CPU信息\n3.打印OS和CPU信息\n4.查找端口808的进程\n5.如果存在对应进程，则关闭进程\n6.输出端口进程信息和关闭核对结果";
			initDB();
			initRedis();
			AgentSessionService agentSessionService = new AgentSessionServiceImpl();
			agentSessionService.setDatasource("visualops");
			
			HitlTaskHelper.getHitlTaskHelper()
					.setAgentSessionService(agentSessionService)
					.setRedisChannel("test",RedisHitlTaskCallListener.DEFAULT_CHANNEL)					 
					.init();
			callMinimaxSimple();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public static void initRedis() {
		//构建名称为test的redis数据源，可以通过RedisFactory.builRedisDB构建其他的数据源
		//不同的数据源设置不同的name，如果对应的name已经被其他redis集群使用，则忽略创建
		RedisConfig redisConfig = new RedisConfig();
		redisConfig.setName("test")
				.setAuth("ecs123456")
				//集群节点可以通过逗号分隔，也可以通过\n符分隔
//          .setServers("101.13.4.15:6359\n101.13.4.15:6369\n101.13.4.15:6379\n101.13.4.15:6389")
				.setServers("101.13.6.7:6381,101.13.6.7:6382,101.13.6.7:6383,101.13.6.7:6384,101.13.6.7:6385,101.13.6.7:6386")
				
				.setMaxRedirections(5)
				.setMode(RedisDB.mode_cluster)
				.setConnectionTimeout(10000)
				.setSocketTimeout(10000)
				.setPoolMaxWaitMillis(2000)
				.setPoolMaxTotal(50)
				.setPoolTimeoutRetry(3)
				.setPoolTimeoutRetryInterval(500l)
				.setMaxIdle(-1)
				.setMinIdle(-1)
				.setTestOnBorrow(true)
				.setTestOnReturn(false)
				.setTestWhileIdle(false);
		RedisFactory.builRedisDB(redisConfig);
	}
	
	public static void initDB() {
		
		SQLUtil.startPool("visualops",//数据源名称
				"com.mysql.cj.jdbc.Driver",//mysql驱动
				"jdbc:mysql://192.168.137.1:3306/bboss?useUnicode=true&characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true",//mysql链接串
				"root", "123456",//数据库账号和口令
				"select 1 " //数据库连接校验sql
		);
	}
	
	public static void callMinimaxSimple() throws InterruptedException {
		//MiniMax-M2.7
		//定义问题变量
		
		//设置模型调用参数，
		ChatAgentMessage chatAgentMessage = new ChatAgentMessage();
//		chatAgentMessage.setModel("MiniMax-M2.7").setMaas("minimax").setRetry(3);
//        chatAgentMessage.setModel("qwen3.7-plus").setMaas("qwenvlplus").setRetry(3);
//        chatAgentMessage.setModel("qwen3.7-plus").setMaas("qwentokenplan").setRetry(3);
		
		chatAgentMessage.setMaas("deepseek").setModel("deepseek-v4-pro");
		chatAgentMessage.setRetry(3);
		String message = "请评审Java文件中的代码并修复问题,java文件路径：C:\\data\\ai\\code\\HitlTaskcallTool.java";
		chatAgentMessage.setPrompt(message).setSystemPrompt("你是一个 Java 代码审查助手。 长期规则： - 如果用户提交 Java 代码并要求审查，先调用 Skill 工具加载 code-review-skill。 - 加载技能书后，再按照技能书里的审查顺序审查java代码。 - 优先指出 bug、安全风险、边界条件、异常处理和缺失测试。 - 如果信息不足，要说明缺少哪些上下文，不要编造项目背景。 - 不要输出与代码审查无关的泛泛建议。 输出要求： - 用中文回答。 - 使用 Markdown。 - 先给总体结论，再列主要问题，最后给测试建议和下一步。");
		
		chatAgentMessage.setStream(true).setThinking(false).setTemperature(0.7);//.addParameter("max_tokens", 2048);
		chatAgentMessage.setStoreContext(new StoreContext()
				.setUserId("user123").setSessionSize(100).setRequestId("request123")
				.setStoreType(StoreContext.STORE_TYPE_DB)
				.setDataSource("visualops"));
		
		CountDownLatch countDownLatch = new CountDownLatch(1);
//		
		AIPlanAgent aiPlanAgent = new AIPlanAgent(new StoreContext()
				.setUserId("user123").setSessionSize(100).setRequestId("request123")
				.setStoreType(StoreContext.STORE_TYPE_DB)
				.setDataSource("visualops"))
				.setAgentMessage(chatAgentMessage)
				.setAgentName("工作流智能体").setAgentId("workflowAgent")
				;
		AINodeAgent agent = new AINodeAgent();
		agent.setEnableLoopToolCall(true);//启用智能体多次调用工具机制
		agent.setMaxLoopToolCalls(80);
		agent.registTools(new SkillsToolRegist()
						.addClasspathSkills("skills"))
				.registBeanTool(new HitlTaskcallTool());//注册人工介入任务调用工具，用于人工介入任务的调用
		//注册文件操作工具，用于读取文件
		agent.registBeanTool(new FileFunctionTool("C:\\data\\ai\\code")
				.addBaseDirectory("C:\\workspace\\bbossgroups\\bboss-ai\\bboss-ai-flow\\out\\test\\resources\\skills\\code-review-skill\\"));
		
		aiPlanAgent.addAgent(agent);
		//通过bboss httpproxy响应式异步交互接口，请求Deepseek模型服务，提交问题
		Flux<ServerEvent> flux = aiPlanAgent.chatStream();
		
		flux.doOnSubscribe(subscription -> logger.info("开始订阅流..."))
				.doOnNext(chunk -> {
					
					
					if (chunk.isHitl()) {
						if (chunk.getData() != null) {
							System.out.print(chunk.getData());
							
						}
						String hitlTaskId = chunk.getHitlTaskId();
						if (hitlTaskId != null) {
							//模拟人工任务处理:通过，并通知智能体继续处理
							Map<String, Object> hitlTaskData = new LinkedHashMap<>();
							hitlTaskData.put("confirm", "确认修改文件");
							hitlTaskData.put("otherData", "用户补充意见：各个问题都符合要求,可以整改");
							HitlTaskHelper.handleHitlCallTask(hitlTaskData, null, hitlTaskId);
//							//模拟人工任务处理:拒绝，并通知智能体拒绝处理
//							hitlTaskData = new LinkedHashMap<>();
//							hitlTaskData.put("confirm", "不要修改文件");
//							hitlTaskData.put("otherData", "用户补充意见：还有其他问题需要注意，比如变量命名、代码格式等，请继续检查和修复");
//							HitlTaskHelper.refuseHitlCallTask(hitlTaskData, null, hitlTaskId);
							
						}
					}
					else if(chunk.isStepType()){
						System.out.println();
					}
					else if (chunk.getData() != null) {
						System.out.print(chunk.getData());
						
					}

//                    if(chunk.isToolCallsType()){
//                        System.out.println();
//                    }
//                    if(chunk.isDone()){
//                        System.out.println();
//                    }
//					
				}) //打印流式调用返回的问题答案片段
				.doOnComplete(() -> {
					countDownLatch.countDown();
					System.out.println();
					logger.info("\n=== 流完成 ===");
				})
				.doOnError(error -> {
					countDownLatch.countDown();
					logger.error("错误: " + error.getMessage(), error);
				})
				.subscribe();
		
		// 等待异步操作完成，否则流式异步方法执行后会因为主线程的退出而退出，看不到后续响应的报文
		countDownLatch.await();
	}
}
