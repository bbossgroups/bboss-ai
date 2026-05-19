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

import org.frameworkset.spi.ai.AIAgent;
import org.frameworkset.spi.ai.model.ServerEvent;
import org.frameworkset.spi.ai.tools.ToolsRegist;
import org.frameworkset.spi.reactor.DisposeEventHandler;
import org.frameworkset.tran.jobflow.NodeTrigger;
import org.frameworkset.tran.jobflow.builder.JobFlowNodeBuilder;
import org.frameworkset.tran.jobflow.listener.JobFlowNodeListener;
import org.frameworkset.tran.jobflow.script.TriggerScriptAPI;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.ArrayList;
import java.util.List;

/**
 * @author biaoping.yin
 * @Date 2026/4/14
 */
public abstract class AIBaseNodeAgent<T extends AIBaseNodeAgent> 
        extends AIAgent<T> implements AppendToParentAgent{
    protected AIPlanAgent planAgent;
    public AIBaseNodeAgent(ToolsRegist mcpToolsRegist ) {
        super(   mcpToolsRegist);
    }
    public AIBaseNodeAgent( String prompt ) {
        super( prompt);
    }
    public AIBaseNodeAgent(  ) {
        super(  );
    }

    public AIBaseNodeAgent(String prompt, String type, ToolsRegist toolsRegist, Integer sessionSize) {
        super(prompt, type, toolsRegist, sessionSize);
    }

    public AIBaseNodeAgent(String prompt, String type, ToolsRegist toolsRegist) {
        super(prompt, type, toolsRegist);
    }

    public AIBaseNodeAgent(String prompt, ToolsRegist toolsRegist) {
        super(prompt, toolsRegist);
    }

    public AIBaseNodeAgent(String prompt, ToolsRegist toolsRegist, int sessionSize) {
        super(prompt, toolsRegist, sessionSize);
    }

    public AIBaseNodeAgent(String prompt, String type) {
        super(prompt, type);
    }
    public AIBaseNodeAgent(String prompt, int sessionSize) {
        super(prompt, sessionSize);
    }
    protected List<JobFlowNodeListener> jobFlowNodeListeners;
    public T addJobFlowNodeListener(JobFlowNodeListener jobFlowNodeListener){
        if(this.jobFlowNodeListeners == null){
            this.jobFlowNodeListeners = new ArrayList<JobFlowNodeListener>();
        }
        this.jobFlowNodeListeners.add(jobFlowNodeListener);
        return (T)this;
    }
    public FluxSink<ServerEvent> getAgentFluxSink(){
        return planAgent.getAgentFluxSink();
    }

    @Override
    public DisposeEventHandler getDisposeEventHandler(){
        return planAgent.getDisposeEventHandler();
    }
 

    @Override
    public Flux<ServerEvent> getFlux() {
        return planAgent.getFlux();
    }
    public AIPlanAgent getPlanAgent() {
        return planAgent;
    }

    public T setPlanAgent(AIPlanAgent aiPlanAgent) {
        this.planAgent = aiPlanAgent;
        return (T)this;
    }
    
    protected JobFlowNodeBuilder builderJobFlowNodeBuilder(){
        AIAgentNodeBuilder aiAgentNodeBuilder = new AIAgentNodeBuilder(this);
       
        return aiAgentNodeBuilder;
    }


    protected JobFlowNodeBuilder _builderJobFlowNodeBuilder(){
        JobFlowNodeBuilder aiAgentNodeBuilder = builderJobFlowNodeBuilder();
        if(this.jobFlowNodeListeners != null)
            aiAgentNodeBuilder.addJobFlowNodeListeners(this.jobFlowNodeListeners);
        return aiAgentNodeBuilder;
    }
    
    

    /**
     * 添加并行智能体节点，并设置条件触发器
     *
     * @param parentAgent
     * @param triggerScriptAPI
     */
    @Override
    public void appendToParentAgent(AIContainerAgent parentAgent, TriggerScriptAPI triggerScriptAPI) {
        JobFlowNodeBuilder jobFlowNodeBuilder = parentAgent.getJobFlowNodeBuilder(this.getAgentId());
        if(jobFlowNodeBuilder == null){

            if(this.getPlanAgent() == null){

                this.setPlanAgent(parentAgent.getPlanAgent());

            }

            this.setParentAgent((AIAgent) parentAgent);
            jobFlowNodeBuilder = _builderJobFlowNodeBuilder();

//            throw new JobFlowBuilderException("Can not find job flow node builder for agentId:"+aiAgent.getAgentId());
        }
        
        parentAgent.addJobFlowNodeBuilder(jobFlowNodeBuilder,triggerScriptAPI);
    }

    @Override
    public String appendConditionJobFlowNodeToParentAgent(AIContainerAgent parentAgent, boolean defaultNode) {
        JobFlowNodeBuilder jobFlowNodeBuilder = parentAgent.getJobFlowNodeBuilder(this.getAgentId());


        if(jobFlowNodeBuilder == null){
           
            if(this.getPlanAgent() == null){

                this.setPlanAgent(parentAgent.getPlanAgent());

            }
            this.setParentAgent((AIAgent)parentAgent);
            jobFlowNodeBuilder = _builderJobFlowNodeBuilder();

//            throw new JobFlowBuilderException("Can not find job flow node builder for agentId:"+aiAgent.getAgentId());
        }
//        jobFlowBuilder.addConditionJobFlowNodeBuilder(jobFlowNodeBuilder, conditionNodeTrigger);


       return parentAgent.addConditionJobFlowNodeBuilder(jobFlowNodeBuilder,defaultNode);
    }

    @Override
    public String appendConditionJobFlowNodeToParentAgent(AIContainerAgent parentAgent, TriggerScriptAPI triggerScriptAPI) {

        JobFlowNodeBuilder jobFlowNodeBuilder = parentAgent.getJobFlowNodeBuilder(this.getAgentId());


        if(jobFlowNodeBuilder == null){
             
            if(this.getPlanAgent() == null){

                this.setPlanAgent(parentAgent.getPlanAgent());

            }
            this.setParentAgent((AIAgent)parentAgent);
            jobFlowNodeBuilder = _builderJobFlowNodeBuilder();

//            throw new JobFlowBuilderException("Can not find job flow node builder for agentId:"+aiAgent.getAgentId());
        }
//        jobFlowBuilder.addConditionJobFlowNodeBuilder(jobFlowNodeBuilder, conditionNodeTrigger);
        
      
        return parentAgent.addConditionJobFlowNodeBuilder(jobFlowNodeBuilder,triggerScriptAPI);
    }
    
    

    @Override
    public String appendConditionJobFlowNodeToParentAgent(boolean allCondtionNodeMathfailedContinue,AIContainerAgent parentAgent, TriggerScriptAPI triggerScriptAPI,boolean defautlConditionNode){
        JobFlowNodeBuilder jobFlowNodeBuilder = parentAgent.getJobFlowNodeBuilder(this.getAgentId());


        if(jobFlowNodeBuilder == null){
           
            if(this.getPlanAgent() == null){

                this.setPlanAgent(parentAgent.getPlanAgent());

            }
            this.setParentAgent((AIAgent)parentAgent);
            jobFlowNodeBuilder = _builderJobFlowNodeBuilder();

//            throw new JobFlowBuilderException("Can not find job flow node builder for agentId:"+aiAgent.getAgentId());
        }
//        jobFlowBuilder.addConditionJobFlowNodeBuilder(jobFlowNodeBuilder, conditionNodeTrigger);


        return parentAgent.addConditionJobFlowNodeBuilder(allCondtionNodeMathfailedContinue,jobFlowNodeBuilder,triggerScriptAPI,defautlConditionNode);
    }

    /**
     * 主干流程管理：为当前作业节点添加后续条件分支，如果当前节点是一个复合条件节点，则为在该复合条件节点后新加一个条件复合节点，新复合节点后续条件分支就可以直接调用
     * addConditionJobFlowNodeBuilder方法添加
     * 返回条件复合节点唯一ID
     * @return 条件复合节点唯一ID
     */
    @Override
    public String addAnotherConditionJobFlowNodeAgent(AIContainerAgent parentAgent){
        return addAnotherConditionJobFlowNodeAgent(parentAgent,false);
    }

    /**
     * 主干流程管理：为当前作业节点添加后续条件分支，如果当前节点是一个复合条件节点，则为在该复合条件节点后新加一个条件复合节点，新复合节点后续条件分支就可以直接调用
     * addConditionJobFlowNodeBuilder方法添加
     * @param parentAgent
     * @param defaultConditionNode 是否默认条件节点,条件节点必须配置一个默认流程节点
     * @return 条件复合节点唯一ID
     */
    
    @Override
    public String addAnotherConditionJobFlowNodeAgent(AIContainerAgent parentAgent,boolean defaultConditionNode){
        return addAnotherConditionJobFlowNodeAgent(  parentAgent, (NodeTrigger) null,  defaultConditionNode);
    }

    /**
     * 主干流程管理：为当前作业节点添加后续条件分支，如果当前节点是一个复合条件节点，则为在该复合条件节点后新加一个条件复合节点，新复合节点后续条件分支就可以直接调用
     * addConditionJobFlowNodeBuilder方法添加
     * @param parentAgent
     * @param defaultConditionNode 是否默认条件节点,条件节点必须配置一个默认流程节点
     * @return 条件复合节点唯一ID
     */
    @Override
    public String addAnotherConditionJobFlowNodeAgent(AIContainerAgent parentAgent, NodeTrigger conditionNodeTrigger,boolean defaultConditionNode){
        JobFlowNodeBuilder jobFlowNodeBuilder = parentAgent.getJobFlowNodeBuilder(this.getAgentId());

        if(jobFlowNodeBuilder == null){
            
            this.setPlanAgent(parentAgent.getPlanAgent());
            this.setParentAgent((AIAgent) parentAgent);
            jobFlowNodeBuilder = _builderJobFlowNodeBuilder();

//            throw new JobFlowBuilderException("Can not find job flow node builder for agentId:"+aiAgent.getAgentId());
        }
        return parentAgent.addAnotherConditionJobFlowNodeBuilder(jobFlowNodeBuilder,   conditionNodeTrigger,  defaultConditionNode);
    }

    @Override
    public String addAnotherConditionJobFlowNodeAgent(boolean allCondtionNodeMatchfailedContinue,AIContainerAgent parentAgent, NodeTrigger conditionNodeTrigger,boolean defaultConditionNode){
        JobFlowNodeBuilder jobFlowNodeBuilder = parentAgent.getJobFlowNodeBuilder(this.getAgentId());

        if(jobFlowNodeBuilder == null){

            this.setPlanAgent(parentAgent.getPlanAgent());
            this.setParentAgent((AIAgent) parentAgent);
            jobFlowNodeBuilder = _builderJobFlowNodeBuilder();

//            throw new JobFlowBuilderException("Can not find job flow node builder for agentId:"+aiAgent.getAgentId());
        }
        return parentAgent.addAnotherConditionJobFlowNodeBuilder(allCondtionNodeMatchfailedContinue,jobFlowNodeBuilder,   conditionNodeTrigger,  defaultConditionNode);
    }
    




}
