package org.frameworkset.spi.ai.util;
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

import org.frameworkset.spi.ai.AIAgent;
import org.frameworkset.spi.ai.hitl.HitlCallTask;
import org.frameworkset.spi.ai.model.ServerEvent;

/**
 *
 * @author biaoping.yin
 * @Date 2026/7/16
 */
public class ServerEventUtil {
	public static void buildServerEventAgentInfo(ServerEvent serverEvent, AIAgent agent) {
		serverEvent.setAgentId(agent.getAgentId());
		serverEvent.setAgentName(agent.getAgentName());
		serverEvent.setAgentNodeType(agent.getAgentNodeType());
		serverEvent.setParentAgentId(agent.getParentAgentId());
		serverEvent.setParentAgentName(agent.getParentAgentName());
		serverEvent.setSessionId(agent.getSessionId());
		serverEvent.setRequestId(agent.getRequestId());		
		serverEvent.setGroupId(agent.getGroupId());
		serverEvent.setParentGroupId(agent.getParentGroupId());
		serverEvent.setUserId(agent.getUserId());
	}
	
	public static void buildHiltTaskAgentInfo(HitlCallTask hitlCallTask, AIAgent agent) {
		hitlCallTask.setAgentId(agent.getAgentId());
		hitlCallTask.setAgentNodeType(agent.getAgentNodeType());
		hitlCallTask.setAgentName(agent.getAgentName());
		hitlCallTask.setParentAgentId(agent.getParentAgentId());
		hitlCallTask.setParentAgentName(agent.getParentAgentName());
		hitlCallTask.setSessionId(agent.getSessionId());
		hitlCallTask.setRequestId(agent.getRequestId());
		hitlCallTask.setUserId(agent.getUserId());
	}
	
}
