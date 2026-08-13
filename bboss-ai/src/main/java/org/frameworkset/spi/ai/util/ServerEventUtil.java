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
import org.frameworkset.spi.ai.model.AgentInfoInf;
import org.frameworkset.spi.ai.model.ChatObject;
import org.frameworkset.spi.ai.model.ServerEvent;
import reactor.core.publisher.FluxSink;

/**
 *
 * @author biaoping.yin
 * @Date 2026/7/16
 */
public class ServerEventUtil {
	public static void buildServerEventAgentInfo(ServerEvent serverEvent, AgentInfoInf agent) {
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
	public static void buildServerEventFlowNodeInfo(ServerEvent serverEvent, AgentInfoInf agentInfoInf) {
		serverEvent.setAgentId(agentInfoInf.getAgentId());
		serverEvent.setAgentName(agentInfoInf.getAgentName());
		serverEvent.setAgentNodeType(agentInfoInf.getAgentNodeType());
		serverEvent.setParentAgentId(agentInfoInf.getParentAgentId());
		serverEvent.setParentAgentName(agentInfoInf.getParentAgentName());
		serverEvent.setSessionId(agentInfoInf.getSessionId());
		serverEvent.setRequestId(agentInfoInf.getRequestId());
		serverEvent.setGroupId(agentInfoInf.getGroupId());
		serverEvent.setParentGroupId(agentInfoInf.getParentGroupId());
		serverEvent.setUserId(agentInfoInf.getUserId());
	}
	public static void buildHiltTaskAgentInfo(HitlCallTask hitlCallTask, AgentInfoInf agent) {
		hitlCallTask.setAgentId(agent.getAgentId());
		hitlCallTask.setAgentNodeType(agent.getAgentNodeType());
		hitlCallTask.setAgentName(agent.getAgentName());
		hitlCallTask.setParentAgentId(agent.getParentAgentId());
		hitlCallTask.setParentAgentName(agent.getParentAgentName());
		hitlCallTask.setSessionId(agent.getSessionId());
		hitlCallTask.setRequestId(agent.getRequestId());
		hitlCallTask.setUserId(agent.getUserId());
	}
	
	/**
	 * 向客户端推送步骤信号
	 * @param chatObject
	 */
	public static void emitterStepEvent(ChatObject chatObject){
		ServerEvent stepServerEvent = new ServerEvent(); 
		stepServerEvent.setType(ServerEvent.TYPE_STEP);
		ServerEventUtil.buildServerEventAgentInfo(stepServerEvent, chatObject.getAgent());
		FluxSink<ServerEvent> sink = chatObject.getAgentFluxSink();
		sink.next(stepServerEvent);
	}
	
	public static void emitterStepEvent(AgentInfoInf agentInfoInf){
		ServerEvent stepServerEvent = new ServerEvent();
		stepServerEvent.setType(ServerEvent.TYPE_STEP);
		ServerEventUtil.buildServerEventAgentInfo(stepServerEvent, agentInfoInf);
		FluxSink<ServerEvent> sink = agentInfoInf.getAgentFluxSink();
		sink.next(stepServerEvent);
	}
	
}
