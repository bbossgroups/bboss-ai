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

import com.frameworkset.common.poolman.SQLExecutor;
import org.frameworkset.spi.ai.model.AgentSessionCondition;
import org.frameworkset.spi.ai.store.AgentSession;
import org.frameworkset.spi.ai.store.AgentSessionException;
import org.frameworkset.spi.ai.store.AgentSessionService;
import com.frameworkset.util.ListInfo;
import com.frameworkset.common.poolman.ConfigSQLExecutor;
import org.frameworkset.spi.ai.store.SessionMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import com.frameworkset.orm.transaction.TransactionManager;

/**
 * <p>Title: AgentSessionServiceImpl</p> <p>Description: 会话管理业务处理类 </p>
 * <p>asiainfo</p> <p>Copyright (c) 2007</p> @Date 2026-06-12 14:16:51 @author
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
			executor.deleteWithDBName(datasource, "deleteByKey", sessionid);
		} catch (Exception e) {
			throw new AgentSessionException("delete AgentSession failed::sessionid=" + sessionid, e);
		}

	}
	public void deleteBatchAgentSession(String... sessionids) throws AgentSessionException

	{
        init();
		TransactionManager tm = new TransactionManager();
		try {
			tm.begin();
			executor.deleteByKeysWithDBName(datasource, "deleteByKey", sessionids);
			tm.commit();
		} catch (Exception e) {

			throw new AgentSessionException("batch delete AgentSession failed::sessionids=" + sessionids, e);
		} finally {
			tm.release();
		}

	}
	 
	public AgentSession getAgentSession(String sessionid) throws AgentSessionException

	{
        init();
		try {
			AgentSession bean = executor.queryObjectWithDBName(AgentSession.class, datasource, "selectById", sessionid);
			return bean;
		} catch (Exception e) {
			throw new AgentSessionException("get AgentSession failed::sessionid=" + sessionid, e);
		}

	}
	public ListInfo queryListInfoAgentSessions(AgentSessionCondition conditions, long offset, int pagesize)
			throws AgentSessionException

	{
        init();
		ListInfo datas = null;
		try {
			datas = executor.queryListInfoBeanWithDBName(AgentSession.class, datasource, "queryListAgentSession", offset,
					pagesize, conditions);
		} catch (Exception e) {
			throw new AgentSessionException("pagine query AgentSession failed:", e);
		}
		return datas;

	}
	public List<AgentSession> queryListAgentSessions(AgentSessionCondition conditions) throws AgentSessionException

	{
        init();
		try {
			List<AgentSession> beans = executor.queryListBeanWithDBName(AgentSession.class, datasource,
					"queryListAgentSession", conditions);
			return beans;
		} catch (Exception e) {
			throw new AgentSessionException("query AgentSession failed:", e);
		}

	}
    
    public List<SessionMessage> queryListSessionMessages(String sessionid,String agentId) throws AgentSessionException {
        init();
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
            return sessionMessages;
        } catch (Exception e) {
            throw new AgentSessionException("query List SessionMessages failed:", e);
        }
        
    }

    public List<SessionMessage> queryListSessionMessages(String sessionid) throws AgentSessionException {
        
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