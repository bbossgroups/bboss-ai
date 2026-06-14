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

import org.frameworkset.spi.ai.model.AgentSessionCondition;
import com.frameworkset.util.ListInfo;
import java.util.List;

/**
 * <p>Title: AgentSessionService</p> <p>Description: 会话管理服务接口 </p>
 *  <p>Copyright (c) 2015</p> @Date 2026-06-12 14:16:51 @author
 * yinbp @version v1.0
 */
public interface AgentSessionService {
	 
	void deleteAgentSession(String sessionid) throws AgentSessionException

	;
	void deleteBatchAgentSession(String... sessionids) throws AgentSessionException
 

	;
	AgentSession getAgentSession(String sessionid) throws AgentSessionException

	;
	ListInfo queryListInfoAgentSessions(AgentSessionCondition conditions, long offset, int pagesize)
			throws AgentSessionException

	;
	List<AgentSession> queryListAgentSessions(AgentSessionCondition conditions) throws AgentSessionException

	;
    List<SessionMessage> queryListSessionMessages(String sessionid) throws AgentSessionException;
    List<SessionMessage> queryListSessionMessages(String sessionid,String agentId) throws AgentSessionException;
    void setDatasource(String datasource);
}