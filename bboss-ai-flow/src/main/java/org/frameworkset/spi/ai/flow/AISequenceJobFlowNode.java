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

import org.frameworkset.tran.jobflow.ExecuteResult;
import org.frameworkset.tran.jobflow.JobFlowCyclicBarrier;
import org.frameworkset.tran.jobflow.SequenceJobFlowNode;
import org.frameworkset.tran.jobflow.context.JobFlowNodeExecuteContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author biaoping.yin
 * @Date 2026/5/3
 */
public class AISequenceJobFlowNode extends SequenceJobFlowNode {

    private static Logger logger = LoggerFactory.getLogger(AISequenceJobFlowNode.class);
    private AISequenceAgent sequenceAgent;
    public AISequenceJobFlowNode(AISequenceAgent sequenceAgent){
        this.sequenceAgent = sequenceAgent;
        setNodeId(sequenceAgent.getAgentId());
        setNodeName(sequenceAgent.getAgentName());
    }
    /**
     * 启动流程当前节点
     */
    @Override
    public ExecuteResult execute(JobFlowNodeExecuteContext jobFlowNodeExecuteContext, JobFlowCyclicBarrier barrier){
        sequenceAgent.reactMessage(sequenceAgent.getPlanAgent().getAgentMessage());
        ExecuteResult result = super.execute(jobFlowNodeExecuteContext,barrier);       
        
        return result;

    }
}
