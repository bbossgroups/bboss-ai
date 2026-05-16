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
import org.frameworkset.spi.ai.model.LastSessionMessage;
import org.frameworkset.spi.ai.model.ServerEvent;
import org.frameworkset.spi.ai.store.StoreContext;
import org.frameworkset.spi.ai.util.AIResponseUtil;
import org.frameworkset.spi.reactor.DisposeEventHandler;
import org.frameworkset.spi.reactor.ReactorCallException;
import org.frameworkset.tran.jobflow.JobFlow;
import org.frameworkset.tran.jobflow.JobParams;
import org.frameworkset.tran.jobflow.NodeTrigger;
import org.frameworkset.tran.jobflow.builder.JobFlowBuilder;
import org.frameworkset.tran.jobflow.builder.JobFlowNodeBuilder;
import org.frameworkset.tran.jobflow.listener.JobFlowListener;
import org.frameworkset.tran.jobflow.schedule.JobFlowScheduleConfig;
import org.frameworkset.tran.jobflow.script.TriggerScriptAPI;
import org.frameworkset.util.concurrent.NoSynBooleanWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Schedulers;

/**
 * 智能体流程编排
 * @author biaoping.yin
 * @Date 2026/4/12
 */
public class AIPlanAgent extends AIAgent<AIPlanAgent> implements AIContainerAgent<AIPlanAgent> {
    private static Logger logger = LoggerFactory.getLogger(AIPlanAgent.class);
    
    
    private AIJobFlowBuilder jobFlowBuilder;
    public AIPlanAgent(StoreContext storeContext) {
        super(storeContext);
        
    }
    
    public LastSessionMessage getLastSessionMessage(){
        return mainSessionStore.getLastSubAgentSessionMessage();
    }
    private void initAIJobFlowBuilder(){
        if(jobFlowBuilder == null){
            jobFlowBuilder = new AIJobFlowBuilder(this);
        }
    }

    public JobFlowBuilder getJobFlowBuilder() {
        return jobFlowBuilder;
    }



 

 

//    /**
//     * 添加路由网关节点：负责根据用户问题决定后续的路由节点，AI进行自主决策
//     * @param aiRouteAgent
//     * @return
//     */
//    public AIPlanAgent addAIRouteAgent(AIRouteAgent aiRouteAgent) {
//        initAIJobFlowBuilder();
//        aiRouteAgent.setPlanAgent(this);
//        aiRouteAgent.setParentAgent(this);
//        jobFlowBuilder.addJobFlowNodeBuilder(new AIRouterNodeBuilder(aiRouteAgent));
//        return this;
//    }


    @Override
    public FluxSink<ServerEvent> getAgentFluxSink(){
        return sink;
    }

    @Override
    public DisposeEventHandler getDisposeEventHandler(){
        return disposeEventHandler;
    }
    /**
     * 添加路由节点
     * @param aiRouteChoiceAgent
     * @return
     */
    public AIPlanAgent addRouteChoiceAgent(AppendToParentAgent aiRouteChoiceAgent) {
        initAIJobFlowBuilder();
        aiRouteChoiceAgent.appendConditionJobFlowNodeToParentAgent(this,nodeTriggerContext -> {
            String agentId = (String) nodeTriggerContext.getContainerContextData("routeChoice",true);
            if(agentId == null){
                agentId = (String) nodeTriggerContext.getFlowContextData("routeChoice");
            }
            if(agentId != null && agentId.equals(aiRouteChoiceAgent.getAgentId())){
                return true;
            }
            return false;
        });
//        aiRouteChoiceAgent.setPlanAgent(this);
//        aiRouteChoiceAgent.setParentAgent(this);
//        
//        jobFlowBuilder.addConditionJobFlowNodeBuilder(new AIRouterChoiceNodeBuilder(aiRouteChoiceAgent )
//                .setTriggerScriptAPI(nodeTriggerContext -> {
//                    String agentId = (String) nodeTriggerContext.getContainerContextData("routeChoice",true);
//                    if(agentId == null){
//                        agentId = (String) nodeTriggerContext.getFlowContextData("routeChoice");
//                    }
//                    if(agentId != null && agentId.equals(aiRouteChoiceAgent.getAgentId())){
//                        return true;
//                    }
//                    return false;
//                }));
        return this;
    }


    /**
     * 添加默认路由节点
     * @param defaultRouteChoiceAgent
     * @return
     */
    public AIPlanAgent addDefaultRouteChoiceAgent(AppendToParentAgent defaultRouteChoiceAgent) {
        initAIJobFlowBuilder();
        defaultRouteChoiceAgent.appendConditionJobFlowNodeToParentAgent(this,true);
//        defaultRouteChoiceAgent.setPlanAgent(this);
//        defaultRouteChoiceAgent.setParentAgent(this);
//        jobFlowBuilder.addConditionJobFlowNodeBuilder(new AIRouterChoiceNodeBuilder(defaultRouteChoiceAgent ),true);
        return this;
    }
//    /**
//     * 添加工作流节点
//     * @param judgeAgent
//     * @return
//     */
//    public AIPlanAgent addJudgeAgent(AIJudgeAgent judgeAgent) {
//        initAIJobFlowBuilder();
//        judgeAgent.setPlanAgent(this);
//        judgeAgent.setParentAgent(this);
//        jobFlowBuilder.addJobFlowNodeBuilder(new AIJudgeNodeBuilder(judgeAgent ));
//        return this;
//    }
    /**
     * 添加智能体工作流节点
     * @param aiAgent
     * @return
     */
    public AIPlanAgent addAgent(AppendToParentAgent aiAgent) {
        return addAgent(aiAgent,( TriggerScriptAPI )null);
    }
    /**
     * 添加智能体工作流节点：为当前作业节点添加后续条件分支，可以连续添加多个,通过conditionNodeTrigger指定条件
     * @param conditionNodeId
     * @return
     */
    public AIPlanAgent addConditionFlowNode(String conditionNodeId, TriggerScriptAPI conditionNodeTrigger){
        initAIJobFlowBuilder();       
        jobFlowBuilder.addConditionJobFlowNodeBuilder(conditionNodeId,conditionNodeTrigger);
        return this;
    }

    /**
     * 添加智能体工作流节点：为当前作业节点添加后续条件分支，可以连续添加多个,通过conditionNodeTrigger指定条件
     * @param conditionNodeId
     * @return
     */
    public AIPlanAgent addConditionFlowNode(boolean allCondtionNodeMatchedfailedContinue,String conditionNodeId, TriggerScriptAPI conditionNodeTrigger){
        return addConditionFlowNode(  allCondtionNodeMatchedfailedContinue,  conditionNodeId,   conditionNodeTrigger,false);
    }
    public AIPlanAgent addConditionFlowNode(boolean allCondtionNodeMatchedfailedContinue,String conditionNodeId, TriggerScriptAPI conditionNodeTrigger,boolean defaultConditionNode){
        initAIJobFlowBuilder();
        jobFlowBuilder.addConditionJobFlowNodeBuilder(allCondtionNodeMatchedfailedContinue,conditionNodeId,conditionNodeTrigger,defaultConditionNode);
        return this;
    }
    /**
     * 添加智能体工作流节点：为当前作业节点添加后续条件分支，可以连续添加多个,通过conditionNodeTrigger指定条件
     * @param agent
     * @return
     */
    public AIPlanAgent addConditionFlowNode(AppendToParentAgent agent  ){
        return addConditionFlowNode(  agent , (TriggerScriptAPI)null);
    }
    public void addConditionFlowNode(AppendToParentAgent agent, boolean defaultConditionNode) {
        initAIJobFlowBuilder();
        agent.appendConditionJobFlowNodeToParentAgent(this,defaultConditionNode);

    }
    
    public AIPlanAgent addConditionFlowNode(boolean allCondtionNodeMatchedfailedContinue,AppendToParentAgent aiAgent , TriggerScriptAPI conditionNodeTrigger){
        return addConditionFlowNode(  allCondtionNodeMatchedfailedContinue,  aiAgent ,   conditionNodeTrigger,false);
    }

    public AIPlanAgent addConditionFlowNode(boolean allCondtionNodeMatchedfailedContinue,AppendToParentAgent aiAgent , TriggerScriptAPI conditionNodeTrigger,boolean defaultConditionNode) {
        initAIJobFlowBuilder();
        aiAgent.appendConditionJobFlowNodeToParentAgent(allCondtionNodeMatchedfailedContinue,this,conditionNodeTrigger,defaultConditionNode);

//        JobFlowNodeBuilder jobFlowNodeBuilder = jobFlowBuilder.getJobFlowNodeBuilder(aiAgent.getAgentId());
//        if (jobFlowNodeBuilder == null) {
//            aiAgent.setPlanAgent(this);
//            aiAgent.setParentAgent(this);
//            jobFlowNodeBuilder = new AIAgentNodeBuilder(aiAgent);
////            throw new JobFlowBuilderException("Can not find job flow node builder for agentId:"+aiAgent.getAgentId());
//        }
//        jobFlowBuilder.addConditionJobFlowNodeBuilder(allCondtionNodeMatchedfailedContinue, jobFlowNodeBuilder, conditionNodeTrigger, defaultConditionNode);
        return this;
    }
    public AIPlanAgent addConditionFlowNode(AppendToParentAgent aiAgent , TriggerScriptAPI conditionNodeTrigger){
        initAIJobFlowBuilder();
//        JobFlowNodeBuilder jobFlowNodeBuilder = jobFlowBuilder.getJobFlowNodeBuilder(aiAgent.getAgentId());
//        
//        
//        if(jobFlowNodeBuilder == null){
//            aiAgent.setPlanAgent(this);
//            aiAgent.setParentAgent(this);
//            jobFlowNodeBuilder = new AIAgentNodeBuilder(aiAgent);
//          
////            throw new JobFlowBuilderException("Can not find job flow node builder for agentId:"+aiAgent.getAgentId());
//        }
//        jobFlowBuilder.addConditionJobFlowNodeBuilder(jobFlowNodeBuilder, conditionNodeTrigger);
        aiAgent.appendConditionJobFlowNodeToParentAgent(this,conditionNodeTrigger);
        return this;
    }

    /**
     * 添加智能体工作流节点：为当前作业节点添加后续条件分支，可以连续添加多个，直接使用conditionNodeId自带条件
     * @param conditionNodeId
     * @return
     */
    public AIPlanAgent addConditionFlowNode(String conditionNodeId){
        initAIJobFlowBuilder();
        jobFlowBuilder.addConditionJobFlowNodeBuilder(conditionNodeId);
        return this;
    }

 
    /**
     * 主干流程管理：为当前作业节点添加后续条件分支，如果当前节点是一个复合条件节点，则为在该复合条件节点后新加一个条件复合节点，新复合节点后续条件分支就可以直接调用
     * addConditionJobFlowNodeBuilder方法添加
     * 返回条件复合节点唯一ID
     * @param baseNodeAgent
     * @return 条件复合节点唯一ID
     */
    public String addAnotherConditionJobFlowNodeAgent(AppendToParentAgent baseNodeAgent, NodeTrigger conditionNodeTrigger){
        return addAnotherConditionJobFlowNodeAgent(baseNodeAgent,   conditionNodeTrigger,false);
    }

    public String addAnotherConditionJobFlowNodeAgent(boolean allCondtionNodeMatchedfailedContinue,AppendToParentAgent baseNodeAgent, NodeTrigger conditionNodeTrigger){
        return addAnotherConditionJobFlowNodeAgent( allCondtionNodeMatchedfailedContinue,baseNodeAgent,   conditionNodeTrigger,false);
    }
    
    /**
     * 主干流程管理：为当前作业节点添加后续条件分支，如果当前节点是一个复合条件节点，则为在该复合条件节点后新加一个条件复合节点，新复合节点后续条件分支就可以直接调用
     * addConditionJobFlowNodeBuilder方法添加
     * 返回条件复合节点唯一ID
     * @param baseNodeAgent
     * @return 条件复合节点唯一ID
     */
    public String addAnotherConditionJobFlowNodeAgent(AppendToParentAgent baseNodeAgent, TriggerScriptAPI conditionNodeTrigger){
        return addAnotherConditionJobFlowNodeAgent(  baseNodeAgent,   conditionNodeTrigger,false);
    }

    public String addAnotherConditionJobFlowNodeAgent(boolean allCondtionNodeMatchedfailedContinue,AppendToParentAgent baseNodeAgent, TriggerScriptAPI conditionNodeTrigger){
        NodeTrigger nodeTrigger = null;
        if(conditionNodeTrigger != null){
            nodeTrigger = new NodeTrigger(conditionNodeTrigger);
        }
        return addAnotherConditionJobFlowNodeAgent(   allCondtionNodeMatchedfailedContinue, baseNodeAgent,   nodeTrigger,false);
    }

    public String addAnotherConditionJobFlowNodeAgent(AppendToParentAgent baseNodeAgent, TriggerScriptAPI conditionNodeTrigger,boolean defaultConditionNode){
        NodeTrigger nodeTrigger = null;
        if(conditionNodeTrigger != null){
            nodeTrigger = new NodeTrigger(conditionNodeTrigger);
        }
        return addAnotherConditionJobFlowNodeAgent(  baseNodeAgent, nodeTrigger, defaultConditionNode);
    }

    public String addAnotherConditionJobFlowNodeAgent(AppendToParentAgent baseNodeAgent, NodeTrigger conditionNodeTrigger,boolean defaultConditionNode){
        return  addAnotherConditionJobFlowNodeAgent(false,  baseNodeAgent,  conditionNodeTrigger,  defaultConditionNode);
    }
    /**
     * 主干流程管理：为当前作业节点添加后续条件分支，如果当前节点是一个复合条件节点，则为在该复合条件节点后新加一个条件复合节点，新复合节点后续条件分支就可以直接调用
     * addConditionJobFlowNodeBuilder方法添加
     * @param baseNodeAgent
     * @param defaultConditionNode 是否默认条件节点,条件节点必须配置一个默认流程节点
     * @return 条件复合节点唯一ID
     */
    public String addAnotherConditionJobFlowNodeAgent(boolean allCondtionNodeMatchedfailedContinue,AppendToParentAgent baseNodeAgent, NodeTrigger conditionNodeTrigger,boolean defaultConditionNode){
        this.initAIJobFlowBuilder();
        return baseNodeAgent.addAnotherConditionJobFlowNodeAgent(allCondtionNodeMatchedfailedContinue,this, conditionNodeTrigger, defaultConditionNode);
//        JobFlowNodeBuilder jobFlowNodeBuilder = jobFlowBuilder.getJobFlowNodeBuilder(baseNodeAgent.getAgentId());
//
//        String cid = null;
//        if(jobFlowNodeBuilder == null){
//            baseNodeAgent.(this);
//            baseNodeAgent.setParentAgent(this);
//            jobFlowNodeBuilder = new AIAgentNodeBuilder(baseNodeAgent);
//            
////            throw new JobFlowBuilderException("Can not find job flow node builder for agentId:"+aiAgent.getAgentId());
//        }
//        cid = jobFlowBuilder.addAnotherConditionJobFlowNodeBuilder(jobFlowNodeBuilder,   conditionNodeTrigger,  defaultConditionNode);
//        return cid;
    }

    /**
     * 主干流程管理：为当前作业节点添加后续条件分支，如果当前节点是一个复合条件节点，则为在该复合条件节点后新加一个条件复合节点，新复合节点后续条件分支就可以直接调用
     * addConditionJobFlowNodeBuilder方法添加
     * 返回条件复合节点唯一ID
     * @param conditionNodeId 条件节点ID
     * @return 条件复合节点唯一ID
     */
    public String addAnotherConditionJobFlowNodeAgent(String conditionNodeId, NodeTrigger conditionNodeTrigger){
        return addAnotherConditionJobFlowNodeAgent(conditionNodeId,   conditionNodeTrigger,false);
    }


    /**
     * 主干流程管理：为当前作业节点添加后续条件分支，如果当前节点是一个复合条件节点，则为在该复合条件节点后新加一个条件复合节点，新复合节点后续条件分支就可以直接调用
     * addConditionJobFlowNodeBuilder方法添加
     * @param conditionNodeId 条件节点ID
     * @param defaultConditionNode 是否默认条件节点,条件节点必须配置一个默认流程节点
     * @return 条件复合节点唯一ID
     */
    public String addAnotherConditionJobFlowNodeAgent(String conditionNodeId, NodeTrigger conditionNodeTrigger, boolean defaultConditionNode){
         this.initAIJobFlowBuilder();
        return jobFlowBuilder.addAnotherConditionJobFlowNodeBuilder(  conditionNodeId,   conditionNodeTrigger,   defaultConditionNode);
    }

 

 

 

    /**
     * 主干流程管理：为当前作业节点添加后续条件分支，如果当前节点是一个复合条件节点，则为在该复合条件节点后新加一个条件复合节点，新复合节点后续条件分支就可以直接调用
     * addConditionJobFlowNodeBuilder方法添加
     * 返回条件复合节点唯一ID
     * @param baseNodeAgent
     * @return 条件复合节点唯一ID
     */
    public String addAnotherConditionJobFlowNodeAgent(AppendToParentAgent baseNodeAgent){
        return addAnotherConditionJobFlowNodeAgent(baseNodeAgent,false);
    }
    /**
     * 主干流程管理：为当前作业节点添加后续条件分支，如果当前节点是一个复合条件节点，则为在该复合条件节点后新加一个条件复合节点，新复合节点后续条件分支就可以直接调用
     * addConditionJobFlowNodeBuilder方法添加
     * @param jobFlowNodeBuilder
     * @param defaultConditionNode 是否默认条件节点,条件节点必须配置一个默认流程节点
     * @return 条件复合节点唯一ID
     */
    public String addAnotherConditionJobFlowNodeAgent(AppendToParentAgent jobFlowNodeBuilder,boolean defaultConditionNode){
        return addAnotherConditionJobFlowNodeAgent(  jobFlowNodeBuilder, (NodeTrigger) null,  defaultConditionNode);
    }

    /**
     * 主干流程管理：为当前作业节点添加后续条件分支，如果当前节点是一个复合条件节点，则为在该复合条件节点后新加一个条件复合节点，新复合节点后续条件分支就可以直接调用
     * addConditionJobFlowNodeBuilder方法添加
     * 返回条件复合节点唯一ID
     * @param conditionNodeId 条件节点ID
     * @return 条件复合节点唯一ID
     */
    public String addAnotherConditionJobFlowNodeAgent(String conditionNodeId){
        return addAnotherConditionJobFlowNodeAgent(conditionNodeId,false);
    }
    /**
     * 主干流程管理：为当前作业节点添加后续条件分支，如果当前节点是一个复合条件节点，则为在该复合条件节点后新加一个条件复合节点，新复合节点后续条件分支就可以直接调用
     * addConditionJobFlowNodeBuilder方法添加
     * @param conditionNodeId 条件节点ID
     * @param defaultConditionNode 是否默认条件节点,条件节点必须配置一个默认流程节点
     * @return 条件复合节点唯一ID
     */
    public String addAnotherConditionJobFlowNodeAgent(String conditionNodeId,boolean defaultConditionNode){
     
        return jobFlowBuilder.addAnotherConditionJobFlowNodeBuilder(  conditionNodeId,  defaultConditionNode);
    }
 

 
 
//    /**
//     * 添加智能体工作流节点
//     * @param aiAgent
//     * @return
//     */
//    public AIPlanAgent addAgent(AIBaseNodeAgent aiAgent, TriggerScriptAPI triggerScriptAPI) {
//        initAIJobFlowBuilder();
//        aiAgent.setPlanAgent(this);
//        aiAgent.setParentAgent(this);
//        jobFlowBuilder.addJobFlowNodeBuilder(new AIAgentNodeBuilder(aiAgent ).setTriggerScriptAPI(triggerScriptAPI));
//        return this;
//    }

//    /**
//     * 添加并行智能体节点，并设置条件触发器
//     */
//    public AIPlanAgent addParrelAgent(AIParrelAgent parrelAgent, TriggerScriptAPI triggerScriptAPI){
//        initAIJobFlowBuilder();
//        parrelAgent.setParentAgent(this);
//        if(parrelAgent.getPlanAgent() == null){
//            parrelAgent.setPlanAgent(this);
//        }
//        jobFlowBuilder.addJobFlowNodeBuilder(parrelAgent.getParrelJobFlowNodeBuilder().setTriggerScriptAPI(triggerScriptAPI));
//        return this;
//    }
    
    public AIPlanAgent addAgent(AppendToParentAgent aiAgent, TriggerScriptAPI triggerScriptAPI) {
        initAIJobFlowBuilder();
        aiAgent.appendToParentAgent(this,triggerScriptAPI);
        return this;
    }

//    /**
//     * 添加并行智能体节点
//     */
//    public AIPlanAgent addParrelAgent(AIParrelAgent parrelAgent){
// 
//        return addParrelAgent(  parrelAgent, (TriggerScriptAPI)null);
//    }


//    /**
//     * 添加串行智能体节点，并设置条件触发器
//     */
//    public AIPlanAgent addSequenceAgent(AISequenceAgent sequenceAgent, TriggerScriptAPI triggerScriptAPI){
//        initAIJobFlowBuilder();
//        sequenceAgent.setParentAgent(this);
//        if(sequenceAgent.getPlanAgent() == null){
//            sequenceAgent.setPlanAgent(this);
//        }
//        jobFlowBuilder.addJobFlowNodeBuilder(sequenceAgent.getSequenceJobFlowNodeBuilder().setTriggerScriptAPI(triggerScriptAPI));
//        return this;
//    }

//    /**
//     * 添加串行智能体节点
//     */
//    public AIPlanAgent addSequenceAgent(AISequenceAgent sequenceAgent){
//
//        return addSequenceAgent(  sequenceAgent, (TriggerScriptAPI)null);
//    }

//    /**
//     * 添加工作流节点
//     * @param flowNode
//     * @return
//     */
//    public AIPlanAgent addFlowNode(AIFlowNode flowNode) {
//        return addFlowNode(flowNode,( TriggerScriptAPI )null);
//    }


//    /**
//     * 添加工作流节点
//     * @param flowNode
//     * @param triggerScriptAPI 
//     * @return
//     */
//    public AIPlanAgent addFlowNode(AIFlowNode flowNode, TriggerScriptAPI triggerScriptAPI) {
//        initAIJobFlowBuilder();
//        flowNode.setPlanAgent(this);
//        jobFlowBuilder.addJobFlowNodeBuilder(new AIFlowNodeBuilder(flowNode ).setTriggerScriptAPI(triggerScriptAPI));
//        return this;
//    }

    public LastSessionMessage chat( ) {
        
        if(jobFlowBuilder != null){
            initSessionStore();
            String inputMessage = this.evalPrompt(this.agentMessage);
            String inputSystemMessage = this.evalSystemPrompt(this.agentMessage);
            JobFlowScheduleConfig jobFlowScheduleConfig = new JobFlowScheduleConfig();
            jobFlowScheduleConfig.setExecuteOneTime(true);
            jobFlowBuilder.setJobFlowScheduleConfig(jobFlowScheduleConfig);
            jobFlowBuilder.setJobFlowId(this.getAgentId());
            jobFlowBuilder.setJobFlowName(this.getAgentName());
            JobFlow jobflow = jobFlowBuilder.build();
            JobParams jobParams = null;
            if(inputMessage != null || inputSystemMessage != null){
                jobParams = new JobParams();
                jobParams.addParam("input.query", inputMessage);
                jobParams.addParam("input.system", inputSystemMessage);
                
            }
            jobflow.execute(jobParams);
            return this.getLastSessionMessage();
            
        }
        return null;
    }


    public Flux<ServerEvent> chatStream( ) {
        this.stream = true;
        return flux = buildFlux(  );
        
    }

    private FluxSink<ServerEvent> sink;
    private DisposeEventHandler disposeEventHandler;
    private Flux<ServerEvent> flux;
    @Override
    public Flux<ServerEvent> getFlux() {
        return flux;
    }
    private boolean stream;

    public boolean isStream() {
        return stream;
    }

 

    public void setSink(FluxSink<ServerEvent> sink) {
        this.sink = sink;
    }

    public void setDisposeEventHandler(DisposeEventHandler disposeEventHandler) {
        this.disposeEventHandler = disposeEventHandler;
    }

    public   Flux<ServerEvent> buildFlux(  ) {
        return Flux.<ServerEvent>create(sink -> {
            try {
                
                if(jobFlowBuilder != null){
                    DisposeEventHandler disposeEventHandler = new DisposeEventHandler();
                    disposeEventHandler.onDispose(sink);
                    this.setDisposeEventHandler(disposeEventHandler);
                    this.setSink(sink);
                    initSessionStore();
                    JobFlowScheduleConfig jobFlowScheduleConfig = new JobFlowScheduleConfig();
                    jobFlowScheduleConfig.setExecuteOneTime(true);
                    jobFlowBuilder.setJobFlowScheduleConfig(jobFlowScheduleConfig);
                    AIJobFlow jobflow = (AIJobFlow)jobFlowBuilder.build();

                    jobflow.execute();

                }
                

            } catch (ReactorCallException e) {
//                        logger.error("流式请求失败：poolName["+poolName +"],url["+url +"],data:" + data);
                AIResponseUtil.handleServerEventExceptionData(  e, sink,   null,new NoSynBooleanWrapper( true));
//                        sink.error(e);
            } catch (Exception e) {
                AIResponseUtil.handleServerEventExceptionData(  e, sink,   null,    new NoSynBooleanWrapper( true));
//                        sink.error(new ReactorCallException("流式请求失败：poolName["+poolName +"],url["+url +"],", e));
            }
            catch (Throwable e) {
                AIResponseUtil.handleServerEventExceptionData(  e, sink,   null,new NoSynBooleanWrapper( true));
//                        sink.error(new ReactorCallException("流式请求失败：poolName["+poolName +"],url["+url +"],", e));
            }
            finally {
                sink.complete();
            }
        }, FluxSink.OverflowStrategy.BUFFER)
        .subscribeOn(Schedulers.boundedElastic()) // 在弹性线程池中执行阻塞IO
//		.timeout(Duration.ofSeconds(60)) // 设置超时
        .onErrorResume(throwable -> {
//                    String error = SimpleStringUtil.exceptionToString(throwable);
//                    System.err.println("流式处理错误: " + throwable.getMessage());
//                    String error = SimpleStringUtil.exceptionToString(throwable);
            if(logger.isDebugEnabled()) {
                logger.debug(throwable.getMessage(), throwable);
            }
            // 修改此处，将错误信息作为Flux输出
            return Flux.empty();
        });
    }

    public AIPlanAgent addJobFlowListener(JobFlowListener jobFlowListener) {
        initAIJobFlowBuilder();
        this.jobFlowBuilder.addJobFlowListener(jobFlowListener);
        return this;
    }

    @Override
    public AIPlanAgent addJobFlowNodeBuilder(JobFlowNodeBuilder jobFlowNodeBuilder, TriggerScriptAPI triggerScriptAPI) {
        initAIJobFlowBuilder();
        this.jobFlowBuilder.addJobFlowNodeBuilder(jobFlowNodeBuilder.setTriggerScriptAPI(triggerScriptAPI));
        return this;
    }


    @Override
    public String addConditionJobFlowNodeBuilder(JobFlowNodeBuilder jobFlowNodeBuilder, TriggerScriptAPI triggerScriptAPI){
        initAIJobFlowBuilder();
        return this.jobFlowBuilder.addConditionJobFlowNodeBuilder(jobFlowNodeBuilder.setTriggerScriptAPI(triggerScriptAPI));
    }

    @Override
    public String  addConditionJobFlowNodeBuilder(boolean allCondtionNodeMatchedfailedContinue,JobFlowNodeBuilder jobFlowNodeBuilder,TriggerScriptAPI triggerScriptAPI,boolean defaultConditionNode){
        initAIJobFlowBuilder();
        return this.jobFlowBuilder.addConditionJobFlowNodeBuilder(allCondtionNodeMatchedfailedContinue,jobFlowNodeBuilder,triggerScriptAPI,defaultConditionNode);
    }
    @Override
    public String  addConditionJobFlowNodeBuilder(JobFlowNodeBuilder jobFlowNodeBuilder, boolean defaultNode){
        initAIJobFlowBuilder();
        return this.jobFlowBuilder.addConditionJobFlowNodeBuilder(jobFlowNodeBuilder,defaultNode);
    }

    @Override
    public String addAnotherConditionJobFlowNodeBuilder(JobFlowNodeBuilder jobFlowNodeBuilder, NodeTrigger conditionNodeTrigger, boolean defaultConditionNode){
        initAIJobFlowBuilder();
        return this.jobFlowBuilder.addAnotherConditionJobFlowNodeBuilder(jobFlowNodeBuilder,conditionNodeTrigger,defaultConditionNode);
    }

    public String addAnotherConditionJobFlowNodeBuilder(boolean allCondtionNodeMatchfailedContinue,JobFlowNodeBuilder jobFlowNodeBuilder, NodeTrigger conditionNodeTrigger, boolean defaultConditionNode){
        initAIJobFlowBuilder();
        return this.jobFlowBuilder.addAnotherConditionJobFlowNodeBuilder(allCondtionNodeMatchfailedContinue,jobFlowNodeBuilder,conditionNodeTrigger,defaultConditionNode);
    }
    @Override
    public AIPlanAgent addJobFlowNodeBuilder(JobFlowNodeBuilder jobFlowNodeBuilder) {
        initAIJobFlowBuilder();
        this.jobFlowBuilder.addJobFlowNodeBuilder(jobFlowNodeBuilder);
        return this;
    }

    @Override
    public AIPlanAgent getPlanAgent() {
        return this;
    }

    @Override
    public JobFlowNodeBuilder getJobFlowNodeBuilder(String nodeId) {
        return jobFlowBuilder.getJobFlowNodeBuilder(nodeId);
    }

  
}
