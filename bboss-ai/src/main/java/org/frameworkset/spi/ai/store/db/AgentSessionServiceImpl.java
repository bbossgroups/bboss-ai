/**
 *  Copyright 2008-2010 biaoping.yin
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.frameworkset.spi.ai.store.db;

import com.frameworkset.common.poolman.ConfigSQLExecutor;
import com.frameworkset.orm.transaction.TransactionManager;
import com.frameworkset.util.ListInfo;
import org.frameworkset.spi.ai.model.AgentSessionCondition;
import org.frameworkset.spi.ai.store.AgentSession;
import org.frameworkset.spi.ai.store.AgentSessionException;
import org.frameworkset.spi.ai.store.AgentSessionService;
import org.frameworkset.spi.ai.store.SessionMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * <p>Title: AgentSessionServiceImpl</p> <p>Description: 会话管理业务处理类 </p>
 *  <p>Copyright (c) 2007</p> @Date 2026-06-12 14:16:51 @author
 * yinbp @version v1.0
 */
public class AgentSessionServiceImpl implements AgentSessionService {
    private String datasource = "ecop";

    private static Logger log = LoggerFactory
            .getLogger(AgentSessionServiceImpl.class);

    private ConfigSQLExecutor executor ;
    private AgentSessionStoreDBConfig agentSessionStoreDBConfig;

    private Object lock = new Object();
    private void init(){
        if(executor == null) {
            synchronized (lock) {
                if (executor == null) {
                    executor = new ConfigSQLExecutor("org/frameworkset/spi/ai/store/db/agentSession.xml");
                }
                agentSessionStoreDBConfig = new AgentSessionStoreDBConfig();
                agentSessionStoreDBConfig.init();
            }
        }

    }

    public void deleteAgentSession(String sessionid) throws AgentSessionException

    {
        try {
            init();
            if (log.isInfoEnabled()) {
                log.info("delete AgentSession start::sessionid={}", sessionid);
            }
            executor.deleteWithDBName(datasource, "deleteByKey", sessionid);
            if (log.isInfoEnabled()) {
                log.info("delete AgentSession success::sessionid={}", sessionid);
            }
        } catch (Exception e) {
            log.error("delete AgentSession failed::sessionid={}", sessionid, e);
            throw new AgentSessionException("delete AgentSession failed::sessionid=" + sessionid, e);
        }

    }
    public void deleteBatchAgentSession(String... sessionids) throws AgentSessionException

    {
        init();
        if (log.isInfoEnabled()) {
            log.info("deleteBatchAgentSession start::sessionids count={}", sessionids != null ? sessionids.length : 0);
        }
        TransactionManager tm = new TransactionManager();
        try {
            tm.begin();
            executor.deleteByKeysWithDBName(datasource, "deleteByKey", sessionids);
            tm.commit();
            if (log.isInfoEnabled()) {
                log.info("deleteBatchAgentSession success::sessionids count={}", sessionids != null ? sessionids.length : 0);
            }
        } catch (Exception e) {
            log.error("batch delete AgentSession failed::sessionids count={}", sessionids != null ? sessionids.length : 0, e);
            throw new AgentSessionException("batch delete AgentSession failed::sessionids=" + sessionids, e);
        } finally {
            tm.release();
        }

    }

    public AgentSession getAgentSession(String sessionid) throws AgentSessionException

    {
        init();
        if (log.isDebugEnabled()) {
            log.debug("getAgentSession start::sessionid={}", sessionid);
        }
        try {
            AgentSession bean = executor.queryObjectWithDBName(AgentSession.class, datasource, "selectById", sessionid);
            if (log.isDebugEnabled()) {
                log.debug("getAgentSession success::sessionid={}, result={}", sessionid, bean != null);
            }
            return bean;
        } catch (Exception e) {
            log.error("get AgentSession failed::sessionid={}", sessionid, e);
            throw new AgentSessionException("get AgentSession failed::sessionid=" + sessionid, e);
        }

    }
    public ListInfo queryListInfoAgentSessions(AgentSessionCondition conditions, long offset, int pagesize)
            throws AgentSessionException

    {
        init();
        if (log.isDebugEnabled()) {
            log.debug("queryListInfoAgentSessions start::offset={}, pagesize={}", offset, pagesize);
        }
        ListInfo datas = null;
        try {
            datas = executor.queryListInfoBeanWithDBName(AgentSession.class, datasource, "queryListAgentSession", offset,
                    pagesize, conditions);
            if (log.isDebugEnabled()) {
                log.debug("queryListInfoAgentSessions success::offset={}, pagesize={}, resultSize={}", offset, pagesize, datas != null ? datas.getSize() : 0);
            }
        } catch (Exception e) {
            log.error("pagine query AgentSession failed::offset={}, pagesize={}", offset, pagesize, e);
            throw new AgentSessionException("pagine query AgentSession failed:", e);
        }
        return datas;

    }
    public List<AgentSession> queryListAgentSessions(AgentSessionCondition conditions) throws AgentSessionException

    {
        init();
        if (log.isDebugEnabled()) {
            log.debug("queryListAgentSessions start");
        }
        try {
            List<AgentSession> beans = executor.queryListBeanWithDBName(AgentSession.class, datasource,
                    "queryListAgentSession", conditions);
            if (log.isDebugEnabled()) {
                log.debug("queryListAgentSessions success::resultSize={}", beans != null ? beans.size() : 0);
            }
            return beans;
        } catch (Exception e) {
            log.error("query AgentSession failed", e);
            throw new AgentSessionException("query AgentSession failed:", e);
        }

    }

    public List<SessionMessage> queryListSessionMessages(String sessionid,String agentId) throws AgentSessionException {
        init();
        if (log.isDebugEnabled()) {
            log.debug("queryListSessionMessages start::sessionid={}, agentId={}", sessionid, agentId);
        }
        try {
            //获取主智能体记忆记录
            List<SessionMessage> sessionMessages = null;
            if (agentId== null) {
                sessionMessages = executor.queryListWithDBName(SessionMessage.class, datasource,
                        "queryListSessionMessages", sessionid);
            } else {
                sessionMessages = executor.queryListWithDBName(SessionMessage.class, datasource,
                        "queryListAgentSessionMessages", sessionid, agentId, agentId, sessionid, agentId);
            }
            if (log.isDebugEnabled()) {
                log.debug("queryListSessionMessages success::sessionid={}, agentId={}, resultSize={}", sessionid, agentId, sessionMessages != null ? sessionMessages.size() : 0);
            }
            return sessionMessages;
        } catch (Exception e) {
            log.error("query List SessionMessages failed::sessionid={}, agentId={}", sessionid, agentId, e);
            throw new AgentSessionException("query List SessionMessages failed:", e);
        }

    }

    public List<SessionMessage> queryListSessionMessages(String sessionid) throws AgentSessionException {
        if (log.isDebugEnabled()) {
            log.debug("queryListSessionMessages start::sessionid={}", sessionid);
        }
        //获取主智能体记忆记录
        return queryListSessionMessages(  sessionid,null );


    }

    public void setDatasource(String datasource) {
        this.datasource = datasource;
    }

    public void setExecutor(ConfigSQLExecutor executor) {
        this.executor = executor;
    }
}