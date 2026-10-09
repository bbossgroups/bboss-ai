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
import com.frameworkset.util.JsonUtil;
import com.frameworkset.util.ListInfo;
import com.frameworkset.util.SimpleStringUtil;
import org.frameworkset.spi.ai.AIAgent;
import org.frameworkset.spi.ai.hitl.HitlCallTask;
import org.frameworkset.spi.ai.model.AgentSessionCondition;
import org.frameworkset.spi.ai.model.memory.AgentDayMemory;
import org.frameworkset.spi.ai.model.memory.AgentMemory;
import org.frameworkset.spi.ai.store.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * <p>Title: AgentSessionServiceImpl</p> <p>Description: 会话管理业务处理类 </p>
 *  <p>Copyright (c) 2007</p> @Date 2026-06-12 14:16:51 @author
 * yinbp @version v1.0
 */
public class AgentSessionServiceImpl implements AgentSessionService {
    private String datasource ;
	/**
	 * 人工介入任务数据库表数据源
	 */
	private String hitlDatasource  ;	
	private String clickhouseCluster = StoreContext.DEFAULT_CLICKHOUSE_CLUSTER ;
	private int mutationsSync = AgentSessionService.MUTATIONS_SYNC_MODE_0;
    private static Logger log = LoggerFactory
            .getLogger(AgentSessionServiceImpl.class);

    private ConfigSQLExecutor executor ;
    private AgentSessionStoreDBConfig agentSessionStoreDBConfig;

    private Object lock = new Object();
	private boolean inited = false;
    public void init(){
		if(inited )
				return;
		synchronized (lock) {
			if(inited)
				return;
			if (executor == null) {
				executor = new ConfigSQLExecutor("org/frameworkset/spi/ai/store/db/agentSession.xml");
			}
			agentSessionStoreDBConfig = new AgentSessionStoreDBConfig();
			if(hitlDatasource == null)
				hitlDatasource = datasource;
			agentSessionStoreDBConfig.init(clickhouseCluster, hitlDatasource, datasource);
			inited = true;
		}

    }
	
	/**
	 * 获取人工任务
	 * @param hitlTaskId
	 * @return
	 */
	public HitlCallTask getHitlCallTask(String hitlTaskId){
		init();
		try {
			return executor.queryObjectWithDBName(HitlCallTask.class,hitlDatasource, "getHitlCallTask",  hitlTaskId);
		} catch (SQLException e) {
			throw new AgentSessionException("getHitlCallTask failed::hitlTaskId=" + hitlTaskId, e);
		}
	}
	public void persistentHitlCallTask(HitlCallTask hitlCallTask){
		init();
		// 1. 保存人工介入任务到数据库表中
		// 2. 发送人工介入任务到客户端
		try {
			executor.insertBean(hitlDatasource, "insertHitlCallTask", hitlCallTask);
		} catch (SQLException e) {
			throw new AgentSessionException("persistentHitlCallTask failed::sessionid=" + JsonUtil.object2json(hitlCallTask), e);
		}
		
	}
	
	/**
	 * 处理人工任务
	 * @param hitlTaskData
	 * @param hitlTaskId
	 */
	public String handledHitlCallTask(Object  hitlTaskData,Throwable throwable,String hitlTaskId){
		init();
		String _hitlTaskData = null;
		try {
			
			if(hitlTaskData != null){
				if(hitlTaskData instanceof String){
					_hitlTaskData = (String)hitlTaskData;
				}
				else{
					_hitlTaskData = JsonUtil.object2json(hitlTaskData);
				}
			}
			String _throwable = null;
			if(throwable != null){
				_throwable = throwable.getMessage();
			}
			/**
			 * ALTER TABLE agent_hitl_calltask_local on cluster vops_3shards_1replicas
			 *         UPDATE
			 *             hitlTaskStatus = 1,
			 *             hitlTaskData = ?,
			 *             exception = ?,
			 *             hitlTaskHandleTime = ?
			 *         WHERE hitlTaskId = ?
			 *         SETTINGS mutations_sync = 0
			 */
			Map params = new HashMap();
			params.put("clusterName", clickhouseCluster	);
			params.put("mutationsSync", mutationsSync		);
			params.put("hitlTaskData", _hitlTaskData);
			params.put("exception", _throwable);
			params.put("hitlTaskHandleTime", new Date()	);
			params.put("hitlTaskId", hitlTaskId	);
			executor.updateBean(hitlDatasource, "handledHitlCallTask", params);
		} catch (SQLException e) {
			throw new AgentSessionException("handledHitlCallTask failed::hitlTaskId=" + hitlTaskId + ",hitlTaskData=" + _hitlTaskData, e);
		}
		return _hitlTaskData;
		
	}
	
	/**
	 * 拒绝人工任务
	 * @param hitlTaskContent
	 * @param hitlTaskId
	 */
	public String refusedHitlCallTask(Object  hitlTaskContent,Throwable throwable,String hitlTaskId){
		init();
		String _hitlTaskContent = null;
		try {
			if(hitlTaskContent != null){
				if(hitlTaskContent instanceof String){
					_hitlTaskContent = (String)hitlTaskContent;
				}
				else{
					_hitlTaskContent = JsonUtil.object2json(hitlTaskContent);
				}
			}
			String _throwable = null;
			if(throwable != null){
				_throwable = throwable.getMessage();
			}
			Map params = new HashMap();
			params.put("clusterName", clickhouseCluster	);
			params.put("mutationsSync", mutationsSync		);
			params.put("hitlTaskData", _hitlTaskContent);
			params.put("exception", _throwable);
			params.put("hitlTaskHandleTime", new Date()	);
			params.put("hitlTaskId", hitlTaskId	);	
			executor.updateBean(hitlDatasource, "refusedHitlCallTask", params);
		} catch (SQLException e) {
			throw new AgentSessionException("refusedHitlCallTask failed::hitlTaskId=" + hitlTaskId + ",hitlTaskContent=" + _hitlTaskContent, e);
		}
		return _hitlTaskContent;
		
	}
	
	/**
	 *  ALTER TABLE agent_hitl_calltask_local on cluster $clusterName
	 *         UPDATE
	 *             hitlTaskStatus = 5,
	 *             hitlTaskHandleResult = #[hitlTaskHandleResult],
	 *             hitlTaskCompleteTime = #[hitlTaskCompleteTime]
	 *         WHERE hitlTaskId = #[hitlTaskId]
	 *         SETTINGS mutations_sync = $mutationsSync
	 * 完成人工任务
	 * @param hitlTaskHandleResult
	 * @param hitlTaskId
	 */
	public void completeHitlCallTask(String  hitlTaskHandleResult,String hitlTaskId){
		init();
		try {
			Map params = new HashMap();
			params.put("clusterName", clickhouseCluster	);
			params.put("mutationsSync", mutationsSync		);
			params.put("hitlTaskHandleResult", hitlTaskHandleResult);
			params.put("hitlTaskId", hitlTaskId);
			params.put("hitlTaskCompleteTime", new Date());	
			executor.updateBean(hitlDatasource, "completeHitlCallTask", params);
		} catch (SQLException e) {
			throw new AgentSessionException("completeHitlCallTask failed::hitlTaskId=" + hitlTaskId + ",hitlTaskContent=" + hitlTaskHandleResult, e);
		}
		
	}
	
	
	
	/**
	 *  ALTER TABLE agent_hitl_calltask_local on cluster $clusterName
	 *         UPDATE
	 *             hitlTaskStatus = 3,
	 *             hitlTaskHandleResult = #[hitlTaskHandleResult],
	 *             hitlTaskCompleteTime = #[hitlTaskCompleteTime]
	 *         WHERE hitlTaskId = #[hitlTaskId]
	 *         SETTINGS mutations_sync = $mutationsSync
	 * 完成人工任务
	 * @param hitlTaskHandleResult
	 * @param hitlTaskId
	 */
	public void timeoutHitlCallTask(String  hitlTaskHandleResult,String hitlTaskId){
		init();
		try {
			Map params = new HashMap();
			params.put("clusterName", clickhouseCluster	);
			params.put("mutationsSync", mutationsSync		);
			params.put("hitlTaskHandleResult", hitlTaskHandleResult);
			params.put("hitlTaskId", hitlTaskId);
			params.put("hitlTaskCompleteTime", new Date());
			executor.updateBean(hitlDatasource, "timeoutHitlCallTask",params);
		} catch (SQLException e) {
			throw new AgentSessionException("timeoutHitlCallTask failed::hitlTaskId=" + hitlTaskId + ",hitlTaskContent=" + hitlTaskHandleResult, e);
		}
		
	}
	
	/**
	 *  ALTER TABLE agent_hitl_calltask_local on cluster $clusterName
	 *         UPDATE
	 *             hitlTaskStatus = 4,
	 *             hitlTaskHandleResult = #[hitlTaskHandleResult],
	 *             hitlTaskCompleteTime = #[hitlTaskCompleteTime]
	 *         WHERE hitlTaskId = #[hitlTaskId]    
	 *         SETTINGS mutations_sync = $mutationsSync
	 * 销毁人工任务
	 * @param reason
	 * @param hitlTaskId
	 */
	public void destroyHitlCallTask(String reason, String hitlTaskId){
		init();
		try {
			Map params = new HashMap();
			params.put("clusterName", clickhouseCluster	);
			params.put("mutationsSync", mutationsSync		);
			params.put("hitlTaskHandleResult", reason);
			params.put("hitlTaskId", hitlTaskId);
			params.put("hitlTaskCompleteTime", new Date());		
			executor.updateBean(hitlDatasource, "destroyHitlCallTask", params);
		} catch (SQLException e) {
			throw new AgentSessionException("destroyHitlCallTask failed::hitlTaskId=" + hitlTaskId + ",reason=" + reason, e);
		}
		
	}
	
	/** 
	 * 删除和归档人工任务
	 * @param archiveTime
	 */
	public void deleteCompleteHitlCallTaskSQLWithCompleteTime(Date archiveTime){
		init();
		try {
			Map params = new HashMap();
			params.put("clusterName", clickhouseCluster	);
			params.put("mutationsSync", mutationsSync		);
			params.put("hitlTaskCompleteTime", archiveTime);
			executor.deleteBean(hitlDatasource, "deleteCompleteHitlCallTaskSQLWithCompleteTime", params);
		} catch (SQLException e) {
			throw new AgentSessionException("deleteCompleteHitlCallTaskSQLWithCompleteTime failed::archiveTime=" + archiveTime, e);
		}
		
	}
	
	/**
	 * 修改会话标题
	 * @param sessionId
	 * @param newTitle
	 * @throws AgentSessionException
	 */
	public void updateAgentSessionTitle(String sessionId,String newTitle) throws AgentSessionException{
		init();
		try {
			Map params = new HashMap();
			params.put("clusterName", clickhouseCluster	);
			params.put("mutationsSync", mutationsSync		);
			params.put("sessionId", sessionId);
			params.put("newTitle", newTitle);
			executor.updateBean(datasource, "updateAgentSessionTitle", params);
		} catch (SQLException e) {
			throw new AgentSessionException("updateAgentSessionTitle failed::sessionid=" + sessionId + ",newTitle=" + newTitle, e);
		}
	}
	/**
	 * 重置会话，只保留session记录，message记录全部清除掉
	 * @param sessionid
	 * @throws AgentSessionException
	 */
	public void resetAgentSession(String sessionid) throws AgentSessionException {
		init();
		TransactionManager tm = new TransactionManager();
		try {
			tm.begin();
			if (log.isInfoEnabled()) {
				log.info("Reset AgentSession start::sessionid={}", sessionid);
			}
			 
			executor.deleteWithDBName(datasource, "deleteAgentSessionMessageByKey", sessionid);
			executor.deleteWithDBName(datasource, "deleteAgentSessionMessageRefByKey", sessionid);
			if(hitlDatasource != null) {
				executor.deleteWithDBName(this.hitlDatasource, "deleteHitlCallTaskBySessionId", sessionid);
			}
			else{
				executor.deleteWithDBName(this.datasource, "deleteHitlCallTaskBySessionId", sessionid);
			}
			tm.commit();
			
			
			if (log.isInfoEnabled()) {
				log.info("Reset AgentSession success::sessionid={}", sessionid);
			}
		} catch (Exception e) {
			log.error("Reset AgentSession failed::sessionid={}", sessionid, e);
			throw new AgentSessionException("Reset AgentSession failed::sessionid=" + sessionid, e);
		}finally {
			tm.release();
		}
	}
  
    public void deleteAgentSession(String sessionid) throws AgentSessionException

    {
        init();
        TransactionManager tm = new TransactionManager();
        try {
            tm.begin();
            if (log.isInfoEnabled()) {
                log.info("delete AgentSession start::sessionid={}", sessionid);
            }
			Map params = new HashMap();
			params.put("sessionIds", Arrays.asList(sessionid))	;
			params.put("clusterName", clickhouseCluster	);
			params.put("mutationsSync", mutationsSync		);
            executor.deleteBean(datasource, "deleteByKey", params);
            executor.deleteBean(datasource, "deleteAgentSessionMessageByKey", params);
            executor.deleteBean(datasource, "deleteAgentSessionMessageRefByKey", params);
////			if(hitlDatasource != null) {
//				executor.deleteWithDBName(this.hitlDatasource, "deleteHitlCallTaskBySessionId", sessionid);
//			}
//			else{
				executor.deleteBean(this.datasource, "deleteHitlCallTaskBySessionId", params);
				executor.deleteBean(this.datasource, "deleteAgentToolCallRulesBySessionId", params);
			
//			}
            tm.commit();
            
            
            if (log.isInfoEnabled()) {
                log.info("delete AgentSession success::sessionid={}", sessionid);
            }
        } catch (Exception e) {
            log.error("delete AgentSession failed::sessionid={}", sessionid, e);
            throw new AgentSessionException("delete AgentSession failed::sessionid=" + sessionid, e);
        }finally {
            tm.release();
        }

    }
    public void deleteBatchAgentSession(String... sessionids) throws AgentSessionException

    {
        if(sessionids == null || sessionids.length == 0) {
            return;
        }
        init();
        if (log.isInfoEnabled()) {
            log.info("deleteBatchAgentSession start::sessionids count={}", sessionids != null ? sessionids.length : 0);
        }
//        TransactionManager tm = new TransactionManager();
        try {
//            tm.begin();
			Map params = new HashMap();
			params.put("sessionIds", Arrays.asList(sessionids));
			params.put("clusterName", clickhouseCluster	);
			params.put("mutationsSync", mutationsSync		);
            executor.deleteBean(datasource, "deleteByKey", params);
            executor.deleteBean(datasource, "deleteAgentSessionMessageByKey", params);
            executor.deleteBean(datasource, "deleteAgentSessionMessageRefByKey", params	);
//			if(hitlDatasource != null) {
//				executor.deleteBean(this.hitlDatasource, "deleteHitlCallTaskBySessionId", params);
//			}
//			else{
				executor.deleteBean(this.datasource, "deleteHitlCallTaskBySessionId", params);
//			}
//            tm.commit();
            if (log.isInfoEnabled()) {
                log.info("deleteBatchAgentSession success::sessionids count={}", sessionids != null ? sessionids.length : 0);
            }
        } catch (Exception e) {
            log.error("batch delete AgentSession failed::sessionids count={}", sessionids != null ? sessionids.length : 0, e);
            throw new AgentSessionException("batch delete AgentSession failed::sessionids=" + JsonUtil.object2json(sessionids), e);
        } finally {
//            tm.release();
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

    /**
     * 判断会话是否存在
     *
     * @param sessionid
     * @return
     * @throws AgentSessionException
     */
    @Override
    public boolean existAgentSession(String sessionid) throws AgentSessionException {
        init();
        int count = 0;
        try {
            count = executor.queryObjectWithDBName(Integer.class, datasource, "existAgentSession", sessionid);
        } catch (SQLException e) {
            throw new AgentSessionException(e);
        }
        return count > 0;
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
        init();
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
	public String getClickhouseCluster() {
		return clickhouseCluster;
	}
	
	public void setClickhouseCluster(String clickhouseCluster) {
		this.clickhouseCluster = clickhouseCluster;
	}
	
	public void setHitlDatasource(String hitlDatasource) {
		this.hitlDatasource = hitlDatasource;
	}
	
	
	public int getMutationsSync() {
		return mutationsSync;
	}
	
	public void setMutationsSync(int mutationsSync) {
		this.mutationsSync = mutationsSync;
	}
	
	/**
	 * 获取用户智能体记忆流水	
	 * @param agentId
	 * @param userId
	 * @param memoryDay
	 * @return
	 */
	public AgentDayMemory getDayMemory(String agentId,String userId,String memoryDay) {
		
		init();
		if (log.isDebugEnabled()) {
			log.debug("getDayMemory start::agentId={}, userId={}, memoryDay={}", agentId, userId, memoryDay	);
		}
		try {
			AgentDayMemory agentDayMemory = executor.queryObjectWithDBName(AgentDayMemory.class,datasource,"getDayMemory",agentId,userId,memoryDay);
			return agentDayMemory;
		} catch (Exception e) {
			log.error("getDayMemory failed::agentId={}, userId={}, memoryDay={}", agentId, userId, memoryDay, e);
			throw new AgentSessionException("getDayMemory failed:", e);
		}
	}
	
	/**
	 * 获取用户智能体记忆总账
	 * @param agentId
	 * @param userId
	 * @return
	 */
	public AgentMemory getMemory(String agentId, String userId) {
		init();
		if (log.isDebugEnabled()) {
			log.debug("getMemory start::agentId={}, userId={} ", agentId, userId 	);
		}
		try {
			AgentMemory agentMemory = executor.queryObjectWithDBName(AgentMemory.class,datasource,"getMemory",agentId,userId);
			return agentMemory;
		} catch (Exception e) {
			log.error("getMemory failed::agentId={}, userId={} ", agentId, userId,  e);
			throw new AgentSessionException("getMemory failed:", e);
		}	
	}
	
	/**
	 * 追加用户消息到总账,如果总账不存在则新增总账
	 * @param agent
	 * @param section
	 */
	public void createOrUpdateMemory(AIAgent agent, String section) {
		init();
		if (log.isDebugEnabled()) {
			log.debug("createOrUpdateMemory start::agentId={}, userId={} ", agent.getAgentId(), agent.getUserId());
		}
		try {
			AgentMemory agentMemory = executor.queryObjectWithDBName(AgentMemory.class, datasource, "getMemory", agent.getAgentId(), agent.getUserId());
			if (agentMemory == null) {
				agentMemory = new AgentMemory();
				agentMemory.setAgentId(agent.getAgentId());
				agentMemory.setUserId(agent.getUserId());
				agentMemory.setMemoryId(SimpleStringUtil.getUUID32());
				agentMemory.setCreateTime(LocalDateTime.now());
				agentMemory.setUpdateTime(agentMemory.getCreateTime());
				agentMemory.setContent(section);
				agentMemory.setParentAgentId(agent.getParentAgentId());
				agentMemory.setSessionId(agent.getSessionId());
				executor.insertWithDBName(datasource, "insertMemory", agentMemory);
			} else {
				agentMemory.setUpdateTime(LocalDateTime.now());
				agentMemory.setContent(agentMemory.getContent() + section);
				executor.updateWithDBName(datasource, "updateMemory", agentMemory);
			}
			
		} catch (Exception e) {
			log.error("createOrUpdateMemory failed::agentId={}, userId={} ", agent.getAgentId(), agent.getUserId(), e);
			throw new AgentSessionException("createOrUpdateMemory failed:", e);
		}
		
	}
	
	/**
	 * 追加用户消息到日流水账,如果总账不存在则新增日流水账
	 * @param agent
	 * @param section
	 */
	public void createOrUpdateDayMemory(AIAgent agent, String section,String memoryDay) {
		init();
		if (log.isDebugEnabled()) {
			log.debug("createOrUpdateDayMemory start::agentId={}, userId={}, memoryDay={} ", agent.getAgentId(), agent.getUserId(), memoryDay	);
		}
		try {
			AgentDayMemory agentDayMemory = executor.queryObjectWithDBName(AgentDayMemory.class, datasource, "getDayMemory", agent.getAgentId(), agent.getUserId(), memoryDay	);
			if (agentDayMemory == null) {
				agentDayMemory = new AgentDayMemory();
				agentDayMemory.setAgentId(agent.getAgentId());
				agentDayMemory.setUserId(agent.getUserId());
				agentDayMemory.setMemoryId(SimpleStringUtil.getUUID32());
				agentDayMemory.setCreateTime(LocalDateTime.now());
				agentDayMemory.setUpdateTime(agentDayMemory.getCreateTime());
				agentDayMemory.setContent(section);
				agentDayMemory.setParentAgentId(agent.getParentAgentId());
				agentDayMemory.setMemoryDay(memoryDay);
				agentDayMemory.setSessionId(agent.getSessionId());
				executor.insertWithDBName(datasource, "insertDayMemory", agentDayMemory);
			} else {
				agentDayMemory.setUpdateTime(LocalDateTime.now());
				agentDayMemory.setContent(agentDayMemory.getContent() + section);
				executor.updateWithDBName(datasource, "updateDayMemory", agentDayMemory);
			}
			
		} catch (Exception e) {
			log.error("createOrUpdateDayMemory failed::agentId={}, userId={}, memoryDay={} ", agent.getAgentId(), agent.getUserId(), memoryDay, e);
			throw new AgentSessionException("createOrUpdateDayMemory failed:", e);
		}
		
	}
}