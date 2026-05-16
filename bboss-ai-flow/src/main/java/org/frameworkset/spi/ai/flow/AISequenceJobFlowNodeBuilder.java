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

import org.frameworkset.tran.jobflow.SequenceJobFlowNode;
import org.frameworkset.tran.jobflow.builder.SequenceJobFlowNodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author biaoping.yin
 * @Date 2026/5/5
 */
public class AISequenceJobFlowNodeBuilder extends SequenceJobFlowNodeBuilder {
    private static Logger logger = LoggerFactory.getLogger(AISequenceJobFlowNodeBuilder.class);
    protected AIPlanAgent planAgent;
    protected AISequenceAgent sequenceAgent;
    public AISequenceJobFlowNodeBuilder(AISequenceAgent sequenceAgent){
        this.sequenceAgent = sequenceAgent;
        this.planAgent = sequenceAgent.getPlanAgent();
    }

    @Override
    protected SequenceJobFlowNode buildSequenceJobFlowNode(){
        return new AISequenceJobFlowNode(sequenceAgent);
    }



}
