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
import org.apache.commons.lang3.StringUtils;
import org.frameworkset.spi.ai.model.AgentSessionCondition;
import org.frameworkset.spi.ai.store.AgentSession;
import org.frameworkset.spi.ai.store.AgentSessionService;
import org.frameworkset.spi.ai.store.db.AgentSessionServiceImpl;
import org.slf4j.Logger;

import java.util.List;

/**
 * @author biaoping.yin
 * @Date 2026/6/23
 */
public class AgentSessionServiceTest {
    private static Logger logger = org.slf4j.LoggerFactory.getLogger(AgentSessionServiceTest.class);
    public static void initDB(){


        SQLUtil.startPool("visualops",//数据源名称
                "com.mysql.cj.jdbc.Driver",//oracle驱动
                "jdbc:mysql://192.168.137.1:3306/bboss?useUnicode=true&characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true",//mysql链接串
                "root","123456",//数据库账号和口令
                "select 1 " //数据库连接校验sql
        );
    }
    public static void main(String[] args){
        initDB();
        String[] domains = new String[]{"bookingWorkflowAgent","workflowAgent"};
        AgentSessionService agentSessionService = new AgentSessionServiceImpl();
        agentSessionService.setDatasource("visualops");
        
        AgentSessionCondition agentSessionCondition = new AgentSessionCondition();
        String title = "长沙去北京出差";
        if(StringUtils.isNotEmpty(title)){
            agentSessionCondition.setTitle("%"+title+"%");
        }
        if(domains != null && domains.length > 0) {
            agentSessionCondition.setDomains(domains);
        }
        agentSessionCondition.setSortKey("lastAccessTime");
        agentSessionCondition.setSortDesc(true);
        List<AgentSession> sessions = agentSessionService.queryListAgentSessions(agentSessionCondition);
        logger.info("sessions:{}",sessions);
    }

}
