package org.frameworkset.spi.ai;
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
import org.frameworkset.spi.ai.flow.*;
import org.frameworkset.spi.ai.model.ChatAgentMessage;
import org.frameworkset.spi.ai.model.LastSessionMessage;
import org.frameworkset.spi.ai.store.StoreContext;
import org.frameworkset.spi.remote.http.HttpRequestProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author biaoping.yin
 * @Date 2026/4/13
 */
public class ParrelTest {
    private static Logger logger = LoggerFactory.getLogger(ParrelTest.class);
    public static void main(String[] args) throws InterruptedException, IOException {


        HttpRequestProxy.startHttpPools("application-stream.properties");


        HttpRequestProxy.startHttpPools("mcpserver.properties");


        multiagentIntroduceProvinces("kimi","介绍省份智能体","kimi-k2.6",null);

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

    public static void multiagentIntroduceProvinces(String maas, String prompt, String model, String sessionId) throws InterruptedException {
        initDB();
        //定义会话实体：设置模型、maas平台，用户问题
        ChatAgentMessage chatAgentMessage = new ChatAgentMessage()                            
                .setModel(model).setThinking(false)
                .setMaas(maas).setPrompt(prompt);

        //定义工作流智能体，设置会话存储机制为DB，设置DB数据源、当前会id以及用户id
        // 设置短期会话窗口
        AIPlanAgent aiPlanAgent = new AIPlanAgent(new StoreContext()
                .setSessionId(sessionId).setUserId("user123").setSessionSize(100)                 
                .setStoreType(StoreContext.STORE_TYPE_DB).setRequestId("request123")
                .setDataSource("visualops"))
                .setAgentMessage(chatAgentMessage)
                .setAgentName("工作流智能体").setAgentId("workflowAgent")
                 ;

        aiPlanAgent.addAgent(new AINodeAgent("用200字介绍中国有多少个省份和直辖市").setAgentName("介绍中国省份和直辖市").setAgentId("introduceProvinces"));
        //构建并行智能体
        AIParrelAgent aiParrelAgent = new AIParrelAgent(aiPlanAgent).setAgentId("aiParrelAgent").setAgentName("共享任务节点");
        aiParrelAgent.addAgent(new AINodeAgent("用50字介绍湖南").setAgentId("jieshaohunan").setAgentName("用50字介绍湖南"));
        aiParrelAgent.addAgent(new UserNodeAgent("用50字介绍湖北").setAgentId("jieshaohubei").setAgentName("用50字介绍湖北"));
        aiParrelAgent.addAgent(new UserNodeAgent("用50字介绍江西").setAgentId("jieshaojiangxi").setAgentName("用50字介绍江西"));   
        aiPlanAgent.addParrelAgent(aiParrelAgent);
        
        //开始对话，执行对话流程，并返回会话结果   
        LastSessionMessage lastSessionMessage = aiPlanAgent.chat();
        
        //输出会话结果        
        if(lastSessionMessage != null) {
            String data = lastSessionMessage.getData();
            logger.info("serverEvent:{}", data);
        }


    }

}
