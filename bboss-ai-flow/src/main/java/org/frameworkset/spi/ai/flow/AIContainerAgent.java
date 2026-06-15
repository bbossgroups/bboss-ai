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

import org.frameworkset.tran.jobflow.NodeTrigger;
import org.frameworkset.tran.jobflow.builder.JobFlowNodeBuilder;
import org.frameworkset.tran.jobflow.script.TriggerScriptAPI;

/**
 * @author biaoping.yin
 * @Date 2026/5/13
 */
public interface AIContainerAgent<T extends AIContainerAgent> {
    String genSubAgentId();
    String genSubAgentName(String agentId);
    T addJobFlowNodeBuilder(JobFlowNodeBuilder jobFlowNodeBuilder);
    T addJobFlowNodeBuilder(JobFlowNodeBuilder jobFlowNodeBuilder, TriggerScriptAPI triggerScriptAPI);

    String addConditionJobFlowNodeBuilder(JobFlowNodeBuilder jobFlowNodeBuilder, boolean defaultNode);
    String addConditionJobFlowNodeBuilder(JobFlowNodeBuilder jobFlowNodeBuilder, TriggerScriptAPI triggerScriptAPI);
    String addConditionJobFlowNodeBuilder(boolean allCondtionNodeMathfailedContinue,JobFlowNodeBuilder jobFlowNodeBuilder,TriggerScriptAPI triggerScriptAPI,boolean defautlConditionNode);
    AIPlanAgent getPlanAgent();

    JobFlowNodeBuilder getJobFlowNodeBuilder(String nodeId);

    String addAnotherConditionJobFlowNodeBuilder(JobFlowNodeBuilder jobFlowNodeBuilder, NodeTrigger conditionNodeTrigger, boolean defaultConditionNode);


    String addAnotherConditionJobFlowNodeBuilder(boolean allCondtionNodeMathfailedContinue,JobFlowNodeBuilder jobFlowNodeBuilder, NodeTrigger conditionNodeTrigger, boolean defaultConditionNode);
}
