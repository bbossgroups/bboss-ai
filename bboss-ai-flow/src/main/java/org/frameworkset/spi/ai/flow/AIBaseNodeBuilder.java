package org.frameworkset.spi.ai.flow;
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

import org.frameworkset.spi.ai.model.AIRuntimeException;
import org.frameworkset.spi.ai.model.AgentMessage;
import org.frameworkset.spi.ai.model.ChatAgentMessage;
import org.frameworkset.spi.ai.model.ServerEvent;
import org.frameworkset.tran.jobflow.builder.CallableJobFlowNodeBuilder;
import org.frameworkset.tran.jobflow.context.JobFlowExecuteContext;
import org.frameworkset.tran.jobflow.context.JobFlowNodeExecuteContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author biaoping.yin
 * @Date 2026/4/14
 */
public class AIBaseNodeBuilder extends CallableJobFlowNodeBuilder {
    private static Logger logger = LoggerFactory.getLogger(AIBaseNodeBuilder.class);
    protected AIPlanAgent planAgent;

    protected AIBaseNodeAgent agent;



    public AIBaseNodeBuilder(AIBaseNodeAgent agent) {
        super( agent.getAgentId(), agent.getAgentName());
        this.agent = agent;
        this.planAgent = agent.getPlanAgent();
    }
    public AIBaseNodeBuilder(String nodeName) {
        super(nodeName);
    }

    public AIBaseNodeBuilder(String nodeId, String nodeName) {
        super(nodeId, nodeName);
    } 

 

    
    @Override
    public Object call(JobFlowNodeExecuteContext jobFlowNodeExecuteContext) throws Exception {      
      
        
        JobFlowNodeExecuteContext containerJobFlowNodeExecuteContext = jobFlowNodeExecuteContext.getContainerJobFlowNodeExecuteContext();
        JobFlowExecuteContext jobFlowExecuteContext = jobFlowNodeExecuteContext.getJobFlowExecuteContext();
        AgentMessage agentMessage = agent.getAgentMessage() != null ? agent.getAgentMessage() : planAgent.getAgentMessage();
        if(agentMessage == null){
            throw new AIRuntimeException("agentMessage is null");
        }
        if(!planAgent.isStream()) {
            ServerEvent serverEvent = agent.chat((ChatAgentMessage) agentMessage);


            if (serverEvent != null) {
                if (logger.isInfoEnabled()) {
                    logger.info("agentMessage id :{},agentResult:{}", agent.getAgentId(), serverEvent.getData());
                }
                if (containerJobFlowNodeExecuteContext != null) {
                    containerJobFlowNodeExecuteContext.addContextData(agent.getAgentId() + ".agentResult", serverEvent);
                } else {
                    jobFlowExecuteContext.addContextData(agent.getAgentId() + ".agentResult", serverEvent);
                }
            }
        }
        else{
            agent.streamChat((ChatAgentMessage) agentMessage);
        }
        return null;
    }
 
}
