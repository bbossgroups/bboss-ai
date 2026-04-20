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

/**
 * @author biaoping.yin
 * @Date 2026/4/14
 */
public class AIRouterChoiceNodeBuilder extends CallableJobFlowNodeBuilder {
    private AIPlanAgent aiPlanAgent;

    private AIBaseRouteChoiceAgent aiAgent;
    

    public AIRouterChoiceNodeBuilder(AIBaseRouteChoiceAgent aiAgent ) {
        super( aiAgent.getAgentId(),aiAgent.getAgentName());
        this.aiAgent = aiAgent;
        this.aiPlanAgent = aiAgent.getPlanAgent();
    }
    public AIRouterChoiceNodeBuilder(String nodeName) {
        super(nodeName);
    }

    public AIRouterChoiceNodeBuilder(String nodeId, String nodeName) {
        super(nodeId, nodeName);
    } 

 

    
    @Override
    public Object call(JobFlowNodeExecuteContext jobFlowNodeExecuteContext) throws Exception {      
      
        
        JobFlowNodeExecuteContext containerJobFlowNodeExecuteContext = jobFlowNodeExecuteContext.getContainerJobFlowNodeExecuteContext();
        JobFlowExecuteContext jobFlowExecuteContext = jobFlowNodeExecuteContext.getJobFlowExecuteContext();
        AgentMessage agentMessage = aiAgent.getAgentMessage() != null ? aiAgent.getAgentMessage() : aiPlanAgent.getAgentMessage();
        if(agentMessage == null){
            throw new AIRuntimeException("agentMessage is null");
        }
        ServerEvent serverEvent = aiAgent.chat((ChatAgentMessage)agentMessage);
       
        
        if(serverEvent != null){
            
            if(containerJobFlowNodeExecuteContext != null){
                containerJobFlowNodeExecuteContext.addContextData("agentResult",serverEvent);
            }
            else{
                jobFlowExecuteContext.addContextData("agentResult",serverEvent);
            }
        }
        return null;
    }
 
}
