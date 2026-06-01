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
import org.frameworkset.tran.jobflow.script.TriggerScriptAPI;

/**
 * @author biaoping.yin
 * @Date 2026/5/13
 */
public interface AppendToParentAgent {

    void setDisableStream(boolean disableStream);
    /**
     * 主干流程管理：为当前作业节点添加后续条件分支，如果当前节点是一个复合条件节点，则为在该复合条件节点后新加一个条件复合节点，新复合节点后续条件分支就可以直接调用
     * addConditionJobFlowNodeBuilder方法添加
     * 返回条件复合节点唯一ID
     * @return 条件复合节点唯一ID
     */
   String addAnotherConditionJobFlowNodeAgent(AIContainerAgent parentAgent);

    /**
     * 主干流程管理：为当前作业节点添加后续条件分支，如果当前节点是一个复合条件节点，则为在该复合条件节点后新加一个条件复合节点，新复合节点后续条件分支就可以直接调用
     * addConditionJobFlowNodeBuilder方法添加
     * @param parentAgent
     * @param defaultConditionNode 是否默认条件节点,条件节点必须配置一个默认流程节点
     * @return 条件复合节点唯一ID
     */

    String addAnotherConditionJobFlowNodeAgent(AIContainerAgent parentAgent,boolean defaultConditionNode);
    /**
     * 主干流程管理：为当前作业节点添加后续条件分支，如果当前节点是一个复合条件节点，则为在该复合条件节点后新加一个条件复合节点，新复合节点后续条件分支就可以直接调用
     * addConditionJobFlowNodeBuilder方法添加
     * @param parentAgent
     * @param defaultConditionNode 是否默认条件节点,条件节点必须配置一个默认流程节点
     * @return 条件复合节点唯一ID
     */
    String addAnotherConditionJobFlowNodeAgent(AIContainerAgent parentAgent, NodeTrigger conditionNodeTrigger,boolean defaultConditionNode);
    String addAnotherConditionJobFlowNodeAgent(boolean allCondtionNodeMatchfailedContinue,AIContainerAgent parentAgent, NodeTrigger conditionNodeTrigger,boolean defaultConditionNode);
    String appendConditionJobFlowNodeToParentAgent(boolean allCondtionNodeMatchfailedContinue,AIContainerAgent parentAgent, TriggerScriptAPI triggerScriptAPI,boolean defautlConditionNode);
    String appendConditionJobFlowNodeToParentAgent(AIContainerAgent parentAgent, TriggerScriptAPI triggerScriptAPI);
    String appendConditionJobFlowNodeToParentAgent(AIContainerAgent parentAgent, boolean defaultNode);
    /**
     * 添加并行智能体节点，并设置条件触发器
     */
     void appendToParentAgent(AIContainerAgent parentAgent, TriggerScriptAPI triggerScriptAPI);
     String getAgentId();
}
