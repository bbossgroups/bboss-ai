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
import org.frameworkset.spi.ai.callback.AgentOutput;
import org.frameworkset.spi.ai.flow.AINodeAgent;
import org.frameworkset.spi.ai.flow.AIParrelAgent;
import org.frameworkset.spi.ai.flow.AIPlanAgent;
import org.frameworkset.spi.ai.flow.AIRouteAgent;
import org.frameworkset.spi.ai.model.ChatAgentMessage;
import org.frameworkset.spi.ai.model.LastSessionMessage;
import org.frameworkset.spi.ai.model.ServerEvent;
import org.frameworkset.spi.ai.store.StoreContext;
import org.frameworkset.spi.ai.tool.BeanToolsRegist;
import org.frameworkset.spi.remote.http.HttpRequestProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * 酒店和飞机预定智能体工作流 - 非流式版本
 * 工作流说明：
 * 1. 路由智能体判断用户意图（酒店/机票/都要）
 * 2. 根据路由结果执行对应的查询智能体（支持并行查询酒店和机票）
 * 3. 汇总智能体整合结果并给出最终预定建议
 *
 * @author biaoping.yin
 * @Date 2026/5/20
 */
public class BookingTest {
    private static Logger logger = LoggerFactory.getLogger(BookingTest.class);

    public static void main(String[] args) throws InterruptedException, IOException {
        // 初始化HTTP连接池
        HttpRequestProxy.startHttpPools("application-stream.properties");
        HttpRequestProxy.startHttpPools("mcpserver.properties");

        // 场景1：只查询酒店
//        bookingWorkflow("kimi", "帮我预定北京市中心5月25日到5月28日的五星级酒店", "kimi-k2.6", null);

        // 场景2：只查询机票
//        bookingWorkflow("kimi", "帮我预定5月25日上海到北京的机票，要上午的航班", "kimi-k2.6", null);

        // 场景3：酒店和机票都要（路由到并行查询）
//        bookingWorkflow("kimi", "我5月25日到5月28日要去北京出差，帮我预定酒店和机票", "kimi-k2.6", null);

        bookingWorkflow("qwenvlplus", "我5月25日到5月28日要去北京出差，帮我预定酒店和机票", "qwen3.6-plus", null);
    }

    public static void initDB(){
        SQLUtil.startPool("visualops",//数据源名称
                "com.mysql.cj.jdbc.Driver",//mysql驱动
                "jdbc:mysql://192.168.137.1:3306/bboss?useUnicode=true&characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true",//mysql链接串
                "root","123456",//数据库账号和口令
                "select 1 " //数据库连接校验sql
        );
    }

    public static void bookingWorkflow(String maas, String prompt, String model, String sessionId) throws InterruptedException {
        initDB();
        // 定义会话实体：设置模型、maas平台，用户问题
        ChatAgentMessage chatAgentMessage = new ChatAgentMessage()
                .setModel(model)
                .setMaas(maas).setPrompt(prompt);
        AgentOutput agentOutput = new AgentOutput() {
            @Override
            public void output(ServerEvent message) {
                if(message.getData() != null) {
                    System.out.println(message.getData());
                }
                else {

                    System.out.println(message.getFullStreamData());
                }
            }
        };
        // 定义工作流智能体，设置会话存储机制为DB
        AIPlanAgent planAgent = new AIPlanAgent(new StoreContext()
                .setSessionId(sessionId).setUserId("user123").setSessionSize(100)
                .setStoreType(StoreContext.STORE_TYPE_DB)
                .setDataSource("visualops"))
                .setAgentMessage(chatAgentMessage)
                .setAgentName("预定工作流智能体").setAgentId("bookingWorkflowAgent");

        // ==================== 阶段1：路由智能体 ====================
        // 路由智能体判断用户意图：酒店、机票、都要
        planAgent.addAgent(new AIRouteAgent()
                .setAgentId("bookingRouter").setAgentName("预定路由智能体")
                .setSystemPrompt("你是一个行程预定路由智能体。请分析用户的问题，判断用户需要预定什么，注意你不需要直接回答用户的问题，只需要做路由判断。")
                .addRoutingChoice("hotelAgent", "用户只需要预定酒店")
                .addRoutingChoice("flightAgent", "用户只需要预定机票")
                .addRoutingChoice("bothAgent", "用户需要同时预定酒店和机票")
        );
        ToolsRegist toolsRegist = new BeanToolsRegist(new PreOrderTool());
        // ==================== 阶段2：分支查询智能体 ====================
        // 酒店查询智能体（当路由到hotelAgent时执行）
        planAgent.addRouteChoiceAgent(new AINodeAgent(
                "请根据用户的行程需求:#[input.query]，查询并推荐合适的酒店。" +
                "需要考虑：地理位置、价格区间、用户评分、配套设施等因素。" +
                "给出至少3个推荐选项，并说明理由。")
                .setAgentId("hotelAgent")
                .setAgentName("酒店查询智能体")
                .setAgentOutput(agentOutput)
                .setToolsRegist(toolsRegist));

        // 机票查询智能体（当路由到flightAgent时执行）
        planAgent.addRouteChoiceAgent(new AINodeAgent(
                "请根据用户的行程需求:#[input.query]，查询并推荐合适的航班。" +
                "需要考虑：出发时间、到达时间、航空公司、价格、准点率等因素。" +
                "给出至少3个推荐选项，并说明理由。")
                .setAgentId("flightAgent").setAgentName("机票查询智能体").setAgentOutput(agentOutput)
                .setToolsRegist(toolsRegist));

        // ==================== 阶段3：并行查询智能体（都要的场景） ====================
        // 当用户同时需要酒店和机票时，并行执行查询
        AIParrelAgent bothAgent = new AIParrelAgent(planAgent)
                .setAgentId("bothAgent").setAgentName("并行查询智能体");

        bothAgent.addAgent(new AINodeAgent(
                "请根据用户的行程需求:#[input.query]，查询并推荐合适的酒店。" +
                "需要考虑：地理位置（尽量靠近市中心或商务区）、价格区间、用户评分、配套设施等因素。" +
                "给出至少3个推荐选项，并说明理由。")
                .setAgentId("parrelHotelAgent").setAgentName("并行酒店查询") 
                .setToolsRegist(toolsRegist));

        bothAgent.addAgent(new AINodeAgent(
                "请根据用户的行程需求:#[input.query]，查询并推荐合适的航班。" +
                "需要考虑：出发时间、到达时间、航空公司、价格、准点率等因素。" +
                "给出至少3个推荐选项，并说明理由。")
                .setAgentId("parrelFlightAgent").setAgentName("并行机票查询") 
                .setToolsRegist(toolsRegist));
        
        bothAgent.setAgentOutput(agentOutput);

        planAgent.addRouteChoiceAgent(bothAgent);

        // ==================== 阶段4：默认智能体 ====================
        // 当路由匹配不上时，直接回答用户问题
        planAgent.addDefaultRouteChoiceAgent(new AINodeAgent(
                "请根据用户的问题:#[input.query]，提供有帮助的行程和预定相关建议。")
                .setAgentId("defaultAgent").setAgentName("默认智能体").setAgentOutput(agentOutput));

        // ==================== 阶段5：汇总智能体 ====================
        // 汇总前面所有节点的结果，给出最终的预定建议
        planAgent.addAgent(new AINodeAgent(
                "请综合前面的查询结果，为用户提供一份完整的预定建议报告。" +
                "报告需要包含：1)推荐的酒店及理由 2)推荐的航班及理由 3)总预算估算 4)最终操作建议。" +
                "请用清晰的中文输出。")
                .setAgentId("summaryAgent").setAgentName("汇总建议智能体").setAgentOutput(agentOutput));

        // ==================== 阶段6：执行工作流 ====================
        // 开始对话，执行对话流程，并返回会话结果
        LastSessionMessage lastSessionMessage = planAgent.chat();

        // 输出会话结果
        if(lastSessionMessage != null) {
            String data = lastSessionMessage.getData();
            logger.info("========== 酒店和飞机预定工作流执行结果 ==========");
            logger.info("{}", data);
        }
    }
}
