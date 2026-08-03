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

import org.frameworkset.tran.jobflow.builder.CallableJobFlowNodeBuilder;
import org.frameworkset.tran.jobflow.context.JobFlowNodeExecuteContext;

/**
 * @author biaoping.yin
 * @Date 2026/4/14
 */
public class AIFlowNodeVoidBuilder extends CallableJobFlowNodeBuilder<AIFlowNodeVoidBuilder> {
  
    protected AIFlowNodeVoid agent;
    protected AIPlanAgent planAgent;

    public AIFlowNodeVoidBuilder(AIFlowNodeVoid agent) {
        super(agent.getNodeId(), agent.getNodeName());
        this.agent = agent;
        this.planAgent = agent.getPlanAgent();
        
    }
    public AIFlowNodeVoidBuilder(String nodeName) {
        super(nodeName);
    }

    public AIFlowNodeVoidBuilder(String nodeId, String nodeName) {
        super(nodeId, nodeName);
    }

    @Override
    public Object call(JobFlowNodeExecuteContext jobFlowNodeExecuteContext) throws Exception {
		agent.setGroupId(jobFlowNodeExecuteContext.getGroupId());
		agent.setParentGroupId(jobFlowNodeExecuteContext.getParentGroupId());
		agent.call(  jobFlowNodeExecuteContext);
          return null;
    }


    public AIFlowNodeVoid getAgent() {
        return agent;
    }

    public AIPlanAgent getPlanAgent() {
        return planAgent;
    }
    
}
