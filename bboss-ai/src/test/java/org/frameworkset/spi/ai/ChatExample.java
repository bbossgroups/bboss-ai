package org.frameworkset.spi.ai;

import com.frameworkset.util.SimpleStringUtil;
import org.frameworkset.spi.ai.model.ChatAgentMessage;
import org.frameworkset.spi.ai.model.ServerEvent;
import org.frameworkset.spi.ai.store.StoreContext;
import org.frameworkset.spi.remote.http.HttpRequestProxy;
import reactor.core.publisher.Flux;

import java.util.concurrent.CountDownLatch;

public class ChatExample {
	
    public static void main(String[] args) {
        // 初始化配置
        HttpRequestProxy.startHttpPools("application-stream.properties");

        // 创建消息对象
        ChatAgentMessage chatAgentMessage = new ChatAgentMessage();
        chatAgentMessage.setModel("deepseek-v4-pro");  // 模型名称
        chatAgentMessage.setPrompt("介绍一下 Java Reactor 编程模式");
        chatAgentMessage.setSystemPrompt("你是一个编程大师");
        chatAgentMessage.setTemperature(0.7);         // 温度参数
        chatAgentMessage.setMaxTokens(8192L);          // 最大输出 Token
		// 内存存储方式（默认）
		StoreContext memoryContext = new StoreContext()
				.setSessionSize(50)
				.setStoreType(StoreContext.STORE_TYPE_MEMORY);
		chatAgentMessage.setStoreContext(memoryContext);
		
        // 创建 AIAgent 并调用
        AIAgent aiAgent = new AIAgent();
        ServerEvent response = aiAgent.chat("deepseek", chatAgentMessage);

        // 输出结果
        System.out.println(response.getData());
    }
	public static void callDeepseekSimple() throws InterruptedException {
		//定义问题变量
		String message = "介绍一下bboss jobflow";
		//设置模型调用参数，
		ChatAgentMessage chatAgentMessage = new ChatAgentMessage();
		chatAgentMessage.setModel("deepseek-v4-pro");
		chatAgentMessage.setMaas("deepseek");
		chatAgentMessage.setThinking(true);
//        chatAgentMessage.setModel("qwen3.7-plus");
//        chatAgentMessage.setMaas("qwenvlplus");
		chatAgentMessage.setPrompt(message);
		
		chatAgentMessage.setStream( true).setTemperature(0.7).addParameter("max_tokens", 2048);
		
		CountDownLatch countDownLatch = new CountDownLatch(1);
		//通过bboss httpproxy响应式异步交互接口，请求Deepseek模型服务，提交问题
		AIAgent aiAgent = new AIAgent();
		Flux<ServerEvent> flux = aiAgent.streamChat(chatAgentMessage);
		flux.doOnSubscribe(subscription -> System.out.println("开始订阅流..."))
				.doOnNext(chunk -> System.out.print(chunk.getData())) //打印流式调用返回的问题答案片段
				.doOnComplete(() -> {countDownLatch.countDown();System.out.println();System.out.println("\n=== 流完成 ===");})
				.doOnError(error ->{countDownLatch.countDown(); System.out.println("错误: " + SimpleStringUtil.exceptionToString(error));})
				.subscribe();
		
		// 等待异步操作完成，否则流式异步方法执行后会因为主线程的退出而退出，看不到后续响应的报文
		countDownLatch.await();
	}
}