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

package org.frameworkset.spi.ai.store;

import com.frameworkset.util.JsonUtil;
import org.frameworkset.spi.ai.hitl.HitlCallTask;
import org.frameworkset.spi.ai.model.AgentSessionCondition;
import com.frameworkset.util.ListInfo;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

/**
 * <p>Title: AgentSessionService</p> <p>Description: 会话管理服务接口 </p>
 *  <p>Copyright (c) 2015</p> @Date 2026-06-12 14:16:51 @author
 * yinbp @version v1.0
 */
public interface AgentSessionService {
	void setClickhouseCluster(String clickhouseCluster);
	
	void setHitlDatasource(String hitlDatasource);
	
	void persistentHitlCallTask(HitlCallTask hitlCallTask);
	
	HitlCallTask getHitlCallTask(String hitlTaskId);
	
	/**
	 * 处理人工任务
	 * @param hitlTaskData
	 * @param hitlTaskId
	 */
	String handledHitlCallTask(Object  hitlTaskData,Throwable throwable,String hitlTaskId);
	
	/**
	 * 拒绝人工任务
	 * @param hitlTaskData
	 * @param hitlTaskId
	 */
	String refusedHitlCallTask(Object  hitlTaskData,Throwable throwable,String hitlTaskId);
	
	/**
	 * 完成人工任务
	 * @param hitlTaskHandleResult
	 * @param hitlTaskId
	 */
	void completeHitlCallTask(String  hitlTaskHandleResult,String hitlTaskId);
	
	/**
	 * 人工任务处理超时
	 * @param hitlTaskHandleResult
	 * @param hitlTaskId
	 */
	void timeoutHitlCallTask(String  hitlTaskHandleResult,String hitlTaskId);
	
	/**
	 * 销毁人工任务
	 * @param reason
	 * @param hitlTaskId
	 */
	void destroyHitlCallTask(String reason, String hitlTaskId);
	void deleteAgentSession(String sessionid) throws AgentSessionException

	;
	void deleteBatchAgentSession(String... sessionids) throws AgentSessionException
 

	;
	AgentSession getAgentSession(String sessionid) throws AgentSessionException

	;

    /**
     * 判断会话是否存在
     * @param sessionid
     * @return
     * @throws AgentSessionException
     */
    boolean existAgentSession(String sessionid) throws AgentSessionException;

    /**
     * 分页查询会话记录
     * @param conditions
     * @param offset
     * @param pagesize
     * @return
     * @throws AgentSessionException
     */
	ListInfo queryListInfoAgentSessions(AgentSessionCondition conditions, long offset, int pagesize)
			throws AgentSessionException

	;

    /**
     * 查询会话列表
     * @param conditions
     * @return
     * @throws AgentSessionException
     */
	List<AgentSession> queryListAgentSessions(AgentSessionCondition conditions) throws AgentSessionException

	;
    List<SessionMessage> queryListSessionMessages(String sessionid) throws AgentSessionException;
    List<SessionMessage> queryListSessionMessages(String sessionid,String agentId) throws AgentSessionException;
    void setDatasource(String datasource);
	
	void init();
	

}