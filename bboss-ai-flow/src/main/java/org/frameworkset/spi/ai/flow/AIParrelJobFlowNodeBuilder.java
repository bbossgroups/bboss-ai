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

import org.frameworkset.tran.jobflow.ParrelJobFlowNode;
import org.frameworkset.tran.jobflow.builder.ParrelJobFlowNodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author biaoping.yin
 * @Date 2026/5/3
 */
public class AIParrelJobFlowNodeBuilder extends ParrelJobFlowNodeBuilder{
    private static Logger logger = LoggerFactory.getLogger(AIParrelJobFlowNodeBuilder.class);
    protected AIPlanAgent planAgent;
    protected AIParrelAgent parrelAgent;
    public AIParrelJobFlowNodeBuilder(AIParrelAgent parrelAgent){
        this.parrelAgent = parrelAgent;
        this.planAgent = parrelAgent.getPlanAgent();
    }

    protected ParrelJobFlowNode buildParrelJobFlowNode(){
        return new AIParrelJobFlowNode(parrelAgent);
    }
    
}
