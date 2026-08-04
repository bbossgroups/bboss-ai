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

import org.frameworkset.tran.jobflow.JobFlow;
import org.frameworkset.tran.jobflow.builder.JobFlowBuilder;

/**
 * @author biaoping.yin
 * @Date 2026/4/20
 */
public class AIJobFlowBuilder extends JobFlowBuilder {
    private AIPlanAgent planAgent;
    public AIJobFlowBuilder(AIPlanAgent planAgent)
    {
        this.planAgent = planAgent;
    }
    @Override
    protected JobFlow buildJobFlow(){
		AIJobFlow jobflow = new AIJobFlow(planAgent);
		jobflow.setJobFlowId(planAgent.getAgentId());
		jobflow.setJobFlowName(planAgent.getAgentName());
		return jobflow;
    }
}
