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
import org.frameworkset.spi.ai.flow.util.AIFlowUtil;
import org.frameworkset.spi.ai.model.LastSessionMessage;
import org.frameworkset.spi.ai.model.ServerEvent;
import org.frameworkset.spi.ai.store.AgentSessionStore;
import org.frameworkset.tran.jobflow.JobFlowNode;
import org.frameworkset.tran.jobflow.NodeTrigger;
import org.frameworkset.tran.jobflow.builder.JobFlowNodeBuilder;
import org.frameworkset.tran.jobflow.context.JobFlowNodeExecuteContext;
import org.frameworkset.tran.jobflow.listener.JobFlowNodeListener;
import org.frameworkset.tran.jobflow.script.TriggerScriptAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 智能体流程编排:并行智能体编排
 * 当加入到工作流后，需要设置
 * @author biaoping.yin
 * @Date 2026/4/12
 */
public class AISequenceAgent extends AIBaseNodeAgent<AISequenceAgent>  implements AIContainerAgent<AISequenceAgent> {
    private static Logger logger = LoggerFactory.getLogger(AISequenceAgent.class);
    private AISequenceJobFlowNodeBuilder sequenceJobFlowNodeBuilder;
    private AIAgent headerAgent;
    public AISequenceAgent(AIPlanAgent planAgent) {
        
        this.planAgent = planAgent;
//        this.disableStream = true;
        //不引用全局会话存储，但是要保存到全局会话记忆中
//        this.disableGloableStore = true;

        this.disablePush2ParentLastSubMessage = false;
        /**
         * 如果后续增加推理能力，则需要引用上游记录
         */
        this.disableReferenceParentLastSubMessage = true;
    }

    @Override
    protected AgentSessionStore buildAgentSessionStore(AgentSessionStore parentSessionStore,int sessionSize){
        return new SequenceAgentSessionStoreMemory(parentSessionStore,sessionSize);
    }


    private void initAISequenceJobFlowNodeBuilder( ){
        if(sequenceJobFlowNodeBuilder == null){
            
            sequenceJobFlowNodeBuilder = new AISequenceJobFlowNodeBuilder(this);
            logger.info("保存和激发（流处理）串行智能体任务节点[{},{}]中子智能体节点消息",this.getAgentId(),this.getAgentName());
            //聚合和保存并行智能体任务节点中子智能体节点消息
            sequenceJobFlowNodeBuilder.addJobFlowNodeListener(new JobFlowNodeListener() {
                @Override
                public void beforeExecute(JobFlowNodeExecuteContext jobFlowNodeExecuteContext) {
                    
                }

                @Override
                public void afterExecute(JobFlowNodeExecuteContext jobFlowNodeExecuteContext, Throwable throwable) {
 
                    LastSessionMessage lastSessionMessage = getLastSessionMessage();
                    if(lastSessionMessage != null) {
                         
                        String data = lastSessionMessage.getData();
                        AISequenceAgent.this.addAgentResultSessionMessage(data);
                        ServerEvent serverEvent = new ServerEvent();
                        serverEvent.setDone(true);
                        serverEvent.setAgent(AISequenceAgent.this);
                        serverEvent.setData(data);
                        serverEvent.setContent(data);
                        AIFlowUtil.outputResult(AISequenceAgent.this, serverEvent,  jobFlowNodeExecuteContext);
                        if(planAgent.isStream() && !isDisableStream()){
                            
                            getAgentFluxSink().next(serverEvent);
                        }
                    }
                }

                @Override
                public void afterEnd(JobFlowNode jobFlowNode) {

                }
            });
        }
        
    }
    
    private void setHeaderAgent(AIAgent headerAgent){
        if(this.headerAgent == null) {
            this.headerAgent = headerAgent;
            headerAgent.setSequenceHeaderNode(true);
        }
    }

//    /**
//     * 添加并行智能体节点，并设置条件触发器
//     */
//    public AISequenceAgent addParrelAgent(AIParrelAgent parrelAgent, TriggerScriptAPI triggerScriptAPI){
//        initAISequenceJobFlowNodeBuilder( );
//        setHeaderAgent(parrelAgent);
//        parrelAgent.setParentAgent(this);
//        if(parrelAgent.getPlanAgent() == null){
//            parrelAgent.setPlanAgent(this.getPlanAgent());
//        }
//        sequenceJobFlowNodeBuilder.addJobFlowNodeBuilder(parrelAgent.getParrelJobFlowNodeBuilder().setTriggerScriptAPI(triggerScriptAPI));
//        return this;
//    }

//    /**
//     * 添加并行智能体节点
//     */
//    public AISequenceAgent addParrelAgent(AIParrelAgent parrelAgent){
//
//        return addParrelAgent(  parrelAgent, (TriggerScriptAPI)null);
//    }


//    /**
//     * 添加串行智能体节点，并设置条件触发器
//     */
//    public AISequenceAgent addSequenceAgent(AISequenceAgent sequenceAgent, TriggerScriptAPI triggerScriptAPI){
//        initAISequenceJobFlowNodeBuilder();
//        setHeaderAgent(sequenceAgent);
//        sequenceAgent.setParentAgent(this);
//        if(sequenceAgent.getPlanAgent() == null){
//            sequenceAgent.setPlanAgent(this.getPlanAgent());
//        }
//        sequenceJobFlowNodeBuilder.addJobFlowNodeBuilder(sequenceAgent.getSequenceJobFlowNodeBuilder().setTriggerScriptAPI(triggerScriptAPI));
//        return this;
//    }

//    /**
//     * 添加串行智能体节点
//     */
//    public AISequenceAgent addSequenceAgent(AISequenceAgent sequenceAgent){
//
//        return addSequenceAgent(  sequenceAgent, (TriggerScriptAPI)null);
//    }
    public AISequenceJobFlowNodeBuilder getSequenceJobFlowNodeBuilder() {
        return sequenceJobFlowNodeBuilder;
    }

//    /**
//     * 添加智能体工作流节点
//     * @param aiAgent
//     * @return
//     */
//    public AISequenceAgent addAgent(AIBaseNodeAgent aiAgent) {
//        return addAgent(aiAgent,( TriggerScriptAPI )null);
//    }
     

//    /**
//     * 添加智能体工作流节点
//     * @param aiAgent
//     * @return
//     */
//    public AISequenceAgent addAgent(AIBaseNodeAgent aiAgent, TriggerScriptAPI triggerScriptAPI) {
//        this.initAISequenceJobFlowNodeBuilder( );
//        setHeaderAgent(aiAgent);
//        aiAgent.setPlanAgent(planAgent);
//        aiAgent.setParentAgent(this);
////        aiAgent.setDisableStream(true);
//        sequenceJobFlowNodeBuilder.addJobFlowNodeBuilder(new AIAgentNodeBuilder(aiAgent ).setTriggerScriptAPI(triggerScriptAPI));
//        return this;
//    }
 
   

    

  //*************************************//




//    /**
//     * 添加路由网关节点：负责根据用户问题决定后续的路由节点，AI进行自主决策
//     * @param aiRouteAgent
//     * @return
//     */
//    public AISequenceAgent addAIRouteAgent(AIRouteAgent aiRouteAgent) {
//        this.initAISequenceJobFlowNodeBuilder( );
//        setHeaderAgent(aiRouteAgent);
//        aiRouteAgent.setPlanAgent(planAgent);
//        aiRouteAgent.setParentAgent(this);
//        aiRouteAgent.setDisableStream(true);
//        sequenceJobFlowNodeBuilder.addJobFlowNodeBuilder(new AIRouterNodeBuilder(aiRouteAgent));
//        return this;
//    }

// 
//    /**
//     * 添加路由节点
//     * @param aiRouteChoiceAgent
//     * @return
//     */
//    public AISequenceAgent addRouteChoiceAgent(AIBaseNodeAgent aiRouteChoiceAgent) {
//        this.initAISequenceJobFlowNodeBuilder( );
//        setHeaderAgent(aiRouteChoiceAgent);
//        aiRouteChoiceAgent.setPlanAgent(planAgent);
//        aiRouteChoiceAgent.setParentAgent(this);
////        aiRouteChoiceAgent.setDisableStream(true);
//        sequenceJobFlowNodeBuilder.addConditionJobFlowNodeBuilder(new AIRouterChoiceNodeBuilder(aiRouteChoiceAgent )
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
//        return this;
//    }


//    /**
//     * 添加默认路由节点
//     * @param defaultRouteChoiceAgent
//     * @return
//     */
//    public AISequenceAgent addDefaultRouteChoiceAgent(AINodeAgent defaultRouteChoiceAgent) {
//        this.initAISequenceJobFlowNodeBuilder( );
//        setHeaderAgent(defaultRouteChoiceAgent);
//        defaultRouteChoiceAgent.setPlanAgent(planAgent);
//        defaultRouteChoiceAgent.setParentAgent(this);
////        aiAgent.setDisableStream(true);
//        sequenceJobFlowNodeBuilder.addConditionJobFlowNodeBuilder(new AIRouterChoiceNodeBuilder(defaultRouteChoiceAgent ),true);
//        return this;
//    }
//    /**
//     * 添加工作流节点
//     * @param judgeAgent
//     * @return
//     */
//    public AISequenceAgent addJudgeAgent(AIJudgeAgent judgeAgent) {
//        this.initAISequenceJobFlowNodeBuilder( );
//        setHeaderAgent(judgeAgent);
//        judgeAgent.setPlanAgent(planAgent);
//        judgeAgent.setParentAgent(this);
//        judgeAgent.setDisableStream(true);
//        sequenceJobFlowNodeBuilder.addJobFlowNodeBuilder(new AIJudgeNodeBuilder(judgeAgent ));
//        return this;
//    }
 
    /**
     * 添加智能体工作流节点：为当前作业节点添加后续条件分支，可以连续添加多个,通过conditionNodeTrigger指定条件
     * @param conditionNodeId
     * @return
     */
    public AISequenceAgent addConditionFlowNode(String conditionNodeId, TriggerScriptAPI conditionNodeTrigger){
        this.initAISequenceJobFlowNodeBuilder( );
        sequenceJobFlowNodeBuilder.addConditionJobFlowNodeBuilder(conditionNodeId,conditionNodeTrigger);
        return this;
    }

    /**
     * 添加智能体工作流节点：为当前作业节点添加后续条件分支，可以连续添加多个,通过conditionNodeTrigger指定条件
     * @param conditionNodeId
     * @return
     */
    public AISequenceAgent addConditionFlowNode(boolean allCondtionNodeMatchedfailedContinue,String conditionNodeId, TriggerScriptAPI conditionNodeTrigger){
        return addConditionFlowNode(  allCondtionNodeMatchedfailedContinue,  conditionNodeId,   conditionNodeTrigger,false);
    }
    public AISequenceAgent addConditionFlowNode(boolean allCondtionNodeMatchedfailedContinue,String conditionNodeId, TriggerScriptAPI conditionNodeTrigger,boolean defautlConditionNode){
        this.initAISequenceJobFlowNodeBuilder( );
       
        sequenceJobFlowNodeBuilder.addConditionJobFlowNodeBuilder(allCondtionNodeMatchedfailedContinue,conditionNodeId,conditionNodeTrigger,defautlConditionNode);
        return this;
    }
//    /**
//     * 添加智能体工作流节点：为当前作业节点添加后续条件分支，可以连续添加多个,通过conditionNodeTrigger指定条件
//     * @param aiAgent
//     * @return
//     */
//    public AISequenceAgent addConditionFlowNode(AIBaseNodeAgent aiAgent  ){
//        return addConditionFlowNode(  aiAgent , (TriggerScriptAPI)null);
//    }
//
//    public AISequenceAgent addConditionFlowNode(boolean allCondtionNodeMatchedfailedContinue,AIBaseNodeAgent aiAgent , TriggerScriptAPI conditionNodeTrigger){
//        return addConditionFlowNode(  allCondtionNodeMatchedfailedContinue,  aiAgent ,   conditionNodeTrigger,false);
//    }
//
//    public AISequenceAgent addConditionFlowNode(boolean allCondtionNodeMatchedfailedContinue,AIBaseNodeAgent aiAgent , TriggerScriptAPI conditionNodeTrigger,boolean defautlConditionNode){
//        this.initAISequenceJobFlowNodeBuilder( );
//        JobFlowNodeBuilder jobFlowNodeBuilder = sequenceJobFlowNodeBuilder.getJobFlowNodeBuilder(aiAgent.getAgentId());
//        if(jobFlowNodeBuilder == null) {
//            setHeaderAgent(aiAgent);
//            aiAgent.setPlanAgent(planAgent);
//            aiAgent.setParentAgent(this);
//            jobFlowNodeBuilder = new AIAgentNodeBuilder(aiAgent);
//        }
////        aiAgent.setDisableStream(true);
//        sequenceJobFlowNodeBuilder.addConditionJobFlowNodeBuilder(allCondtionNodeMatchedfailedContinue,jobFlowNodeBuilder,conditionNodeTrigger,defautlConditionNode);
//        
//        return this;
//    }
//    public AISequenceAgent addConditionFlowNode(AIBaseNodeAgent aiAgent , TriggerScriptAPI conditionNodeTrigger){
//        this.initAISequenceJobFlowNodeBuilder( );
//        JobFlowNodeBuilder jobFlowNodeBuilder = sequenceJobFlowNodeBuilder.getJobFlowNodeBuilder(aiAgent.getAgentId());
//        if(jobFlowNodeBuilder == null) {
//            setHeaderAgent(aiAgent);
//            aiAgent.setPlanAgent(planAgent);
//            aiAgent.setParentAgent(this);
//            jobFlowNodeBuilder = new AIAgentNodeBuilder(aiAgent);
////        aiAgent.setDisableStream(true);
//        }
//        sequenceJobFlowNodeBuilder.addConditionJobFlowNodeBuilder(jobFlowNodeBuilder,conditionNodeTrigger);
//        return this;
//    }

    /**
     * 添加智能体工作流节点：为当前作业节点添加后续条件分支，可以连续添加多个，直接使用conditionNodeId自带条件
     * @param conditionNodeId
     * @return
     */
    public AISequenceAgent addConditionFlowNode(String conditionNodeId){
        this.initAISequenceJobFlowNodeBuilder( );
        sequenceJobFlowNodeBuilder.addConditionJobFlowNodeBuilder(conditionNodeId);
        return this;
    }
 

//
//    /**
//     * 主干流程管理：为当前作业节点添加后续条件分支，如果当前节点是一个复合条件节点，则为在该复合条件节点后新加一个条件复合节点，新复合节点后续条件分支就可以直接调用
//     * addConditionJobFlowNodeBuilder方法添加
//     * @param baseNodeAgent
//     * @param defaultConditionNode 是否默认条件节点,条件节点必须配置一个默认流程节点
//     * @return 条件复合节点唯一ID
//     */
//    public String addAnotherConditionJobFlowNodeAgent(AIBaseNodeAgent baseNodeAgent, NodeTrigger conditionNodeTrigger,boolean defaultConditionNode){
//        this.initAISequenceJobFlowNodeBuilder( );
//        JobFlowNodeBuilder jobFlowNodeBuilder = sequenceJobFlowNodeBuilder.getJobFlowNodeBuilder(baseNodeAgent.getAgentId());
//
//        String cid = null;
//        if(jobFlowNodeBuilder == null){
//            
//            setHeaderAgent(baseNodeAgent);
//            baseNodeAgent.setPlanAgent(planAgent);
//            baseNodeAgent.setParentAgent(this);
//            jobFlowNodeBuilder = new AIAgentNodeBuilder(baseNodeAgent);
//           
////            throw new JobFlowBuilderException("Can not find job flow node builder for agentId:"+aiAgent.getAgentId());
//        }
//        cid = sequenceJobFlowNodeBuilder.addAnotherConditionJobFlowNodeBuilder(jobFlowNodeBuilder,   conditionNodeTrigger,  defaultConditionNode);
//        return cid;
//    }

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
        initAISequenceJobFlowNodeBuilder();
        return sequenceJobFlowNodeBuilder.addAnotherConditionJobFlowNodeBuilder(  conditionNodeId,   conditionNodeTrigger,   defaultConditionNode);
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
        initAISequenceJobFlowNodeBuilder();
        return sequenceJobFlowNodeBuilder.addAnotherConditionJobFlowNodeBuilder(  conditionNodeId,  defaultConditionNode);
    }

    /**/
    /**
     * 添加路由节点
     * @param aiRouteChoiceAgent
     * @return
     */
    public AISequenceAgent addRouteChoiceAgent(AppendToParentAgent aiRouteChoiceAgent) {
        initAISequenceJobFlowNodeBuilder();
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
        return  this;
    }


    /**
     * 添加默认路由节点
     * @param defaultRouteChoiceAgent
     * @return
     */
    public AISequenceAgent addDefaultRouteChoiceAgent(AppendToParentAgent defaultRouteChoiceAgent) {
        initAISequenceJobFlowNodeBuilder();
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
//        initAIContainerJobFlowNodeBuilder();
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
    public AISequenceAgent addAgent(AppendToParentAgent aiAgent) {
        return addAgent(aiAgent,(TriggerScriptAPI)null);
    }
 
 
 
    /**
     * 添加智能体工作流节点：为当前作业节点添加后续条件分支，可以连续添加多个,通过conditionNodeTrigger指定条件
     * @param aiAgent
     * @return
     */
    public String addConditionFlowNode(AppendToParentAgent aiAgent  ){
        return addConditionFlowNode(  aiAgent , (TriggerScriptAPI)null);
    }
    public void addConditionFlowNode(AppendToParentAgent agent, boolean defaultConditionNode) {
        initAISequenceJobFlowNodeBuilder();

        setHeaderAgent(this);
        agent.appendConditionJobFlowNodeToParentAgent(this,defaultConditionNode);

    }
    public String addConditionFlowNode(boolean allCondtionNodeMatchedfailedContinue,AppendToParentAgent aiAgent , TriggerScriptAPI conditionNodeTrigger){
        return addConditionFlowNode(  allCondtionNodeMatchedfailedContinue,  aiAgent ,   conditionNodeTrigger,false);
    }

    public String addConditionFlowNode(boolean allCondtionNodeMatchedfailedContinue,AppendToParentAgent aiAgent , TriggerScriptAPI conditionNodeTrigger,boolean defautlConditionNode) {
        this.initAISequenceJobFlowNodeBuilder();

        setHeaderAgent(this);
        return aiAgent.appendConditionJobFlowNodeToParentAgent(allCondtionNodeMatchedfailedContinue,this,conditionNodeTrigger,defautlConditionNode);

//        JobFlowNodeBuilder jobFlowNodeBuilder = jobFlowBuilder.getJobFlowNodeBuilder(aiAgent.getAgentId());
//        if (jobFlowNodeBuilder == null) {
//            aiAgent.setPlanAgent(this);
//            aiAgent.setParentAgent(this);
//            jobFlowNodeBuilder = new AIAgentNodeBuilder(aiAgent);
////            throw new JobFlowBuilderException("Can not find job flow node builder for agentId:"+aiAgent.getAgentId());
//        }
//        jobFlowBuilder.addConditionJobFlowNodeBuilder(allCondtionNodeMatchedfailedContinue, jobFlowNodeBuilder, conditionNodeTrigger, defautlConditionNode);
    }
    public String addConditionFlowNode(AppendToParentAgent aiAgent , TriggerScriptAPI conditionNodeTrigger){
        this.initAISequenceJobFlowNodeBuilder();

        setHeaderAgent(this);
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
        return aiAgent.appendConditionJobFlowNodeToParentAgent(this,conditionNodeTrigger);
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

    public String addAnotherConditionJobFlowNodeAgent(boolean allCondtionNodeMathfailedContinue,AppendToParentAgent baseNodeAgent, NodeTrigger conditionNodeTrigger){
        return addAnotherConditionJobFlowNodeAgent(  allCondtionNodeMathfailedContinue,baseNodeAgent,   conditionNodeTrigger,false);
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

    public String addAnotherConditionJobFlowNodeAgent(AppendToParentAgent baseNodeAgent, TriggerScriptAPI conditionNodeTrigger,boolean defaultConditionNode){
        NodeTrigger nodeTrigger = null;
        if(conditionNodeTrigger != null)
            nodeTrigger = new NodeTrigger(conditionNodeTrigger);
        return addAnotherConditionJobFlowNodeAgent(  baseNodeAgent,  nodeTrigger, defaultConditionNode);
    }

    public String addAnotherConditionJobFlowNodeAgent(boolean allCondtionNodeMathfailedContinue,AppendToParentAgent baseNodeAgent, TriggerScriptAPI conditionNodeTrigger,boolean defaultConditionNode){
        NodeTrigger nodeTrigger = null;
        if(conditionNodeTrigger != null)
            nodeTrigger = new NodeTrigger(conditionNodeTrigger);
        return addAnotherConditionJobFlowNodeAgent( allCondtionNodeMathfailedContinue, baseNodeAgent,  nodeTrigger, defaultConditionNode);
    }


    /**
     * 主干流程管理：为当前作业节点添加后续条件分支，如果当前节点是一个复合条件节点，则为在该复合条件节点后新加一个条件复合节点，新复合节点后续条件分支就可以直接调用
     * addConditionJobFlowNodeBuilder方法添加
     * @param baseNodeAgent
     * @param defaultConditionNode 是否默认条件节点,条件节点必须配置一个默认流程节点
     * @return 条件复合节点唯一ID
     */
    public String addAnotherConditionJobFlowNodeAgent(AppendToParentAgent baseNodeAgent, NodeTrigger conditionNodeTrigger,boolean defaultConditionNode){
        this.initAISequenceJobFlowNodeBuilder();
        setHeaderAgent(this);
        
        return baseNodeAgent.addAnotherConditionJobFlowNodeAgent(this,   conditionNodeTrigger,  defaultConditionNode);
    }


    /**
     * 主干流程管理：为当前作业节点添加后续条件分支，如果当前节点是一个复合条件节点，则为在该复合条件节点后新加一个条件复合节点，新复合节点后续条件分支就可以直接调用
     * addConditionJobFlowNodeBuilder方法添加
     * @param baseNodeAgent
     * @param defaultConditionNode 是否默认条件节点,条件节点必须配置一个默认流程节点
     * @return 条件复合节点唯一ID
     */
    public String addAnotherConditionJobFlowNodeAgent(boolean allCondtionNodeMathfailedContinue,AppendToParentAgent baseNodeAgent, NodeTrigger conditionNodeTrigger,boolean defaultConditionNode){
        this.initAISequenceJobFlowNodeBuilder();
        setHeaderAgent(this);

        return baseNodeAgent.addAnotherConditionJobFlowNodeAgent(allCondtionNodeMathfailedContinue,this,   conditionNodeTrigger,  defaultConditionNode);
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

    


//    /**
//     * 添加智能体工作流节点
//     * @param aiAgent
//     * @return
//     */
//    public AIPlanAgent addAgent(AIBaseNodeAgent aiAgent, TriggerScriptAPI triggerScriptAPI) {
//        initAIContainerJobFlowNodeBuilder();
//        aiAgent.setPlanAgent(this);
//        aiAgent.setParentAgent(this);
//        jobFlowBuilder.addJobFlowNodeBuilder(new AIAgentNodeBuilder(aiAgent ).setTriggerScriptAPI(triggerScriptAPI));
//        return this;
//    }

//    /**
//     * 添加并行智能体节点，并设置条件触发器
//     */
//    public AIPlanAgent addParrelAgent(AIParrelAgent parrelAgent, TriggerScriptAPI triggerScriptAPI){
//        initAIContainerJobFlowNodeBuilder();
//        parrelAgent.setParentAgent(this);
//        if(parrelAgent.getPlanAgent() == null){
//            parrelAgent.setPlanAgent(this);
//        }
//        jobFlowBuilder.addJobFlowNodeBuilder(parrelAgent.getParrelJobFlowNodeBuilder().setTriggerScriptAPI(triggerScriptAPI));
//        return this;
//    }

    public AISequenceAgent addAgent(AppendToParentAgent aiAgent, TriggerScriptAPI triggerScriptAPI) {
        initAISequenceJobFlowNodeBuilder();

        setHeaderAgent((AIAgent) aiAgent);
        aiAgent.appendToParentAgent(this,triggerScriptAPI);
        return  this;
    }



////////////////////////////原生工作流节点添加方法：开始/////////////////////////

    public AISequenceAgent addJobFlowNodeBuilder(JobFlowNodeBuilder jobFlowNodeBuilder){
        this.initAISequenceJobFlowNodeBuilder();
        sequenceJobFlowNodeBuilder.addJobFlowNodeBuilder(jobFlowNodeBuilder);
        return this;
    }

    public AISequenceAgent addJobFlowNodeBuilder(JobFlowNodeBuilder jobFlowNodeBuilder,TriggerScriptAPI triggerScriptAPI){
        this.initAISequenceJobFlowNodeBuilder();
        sequenceJobFlowNodeBuilder.addJobFlowNodeBuilder(jobFlowNodeBuilder.setTriggerScriptAPI(triggerScriptAPI));
        return this;
    }

    @Override
    public String addConditionJobFlowNodeBuilder(JobFlowNodeBuilder jobFlowNodeBuilder, boolean defaultNode) {
        initAISequenceJobFlowNodeBuilder();
        return this.sequenceJobFlowNodeBuilder.addConditionJobFlowNodeBuilder(jobFlowNodeBuilder, defaultNode);
    }

    @Override
    public String addConditionJobFlowNodeBuilder(JobFlowNodeBuilder jobFlowNodeBuilder, TriggerScriptAPI triggerScriptAPI) {
        initAISequenceJobFlowNodeBuilder();
        return this.sequenceJobFlowNodeBuilder.addConditionJobFlowNodeBuilder(jobFlowNodeBuilder, triggerScriptAPI);
    }

    @Override
    public String addConditionJobFlowNodeBuilder(boolean allCondtionNodeMatchedfailedContinue, JobFlowNodeBuilder jobFlowNodeBuilder, TriggerScriptAPI triggerScriptAPI, boolean defautlConditionNode) {
        initAISequenceJobFlowNodeBuilder();
        return this.sequenceJobFlowNodeBuilder.addConditionJobFlowNodeBuilder(allCondtionNodeMatchedfailedContinue, jobFlowNodeBuilder, triggerScriptAPI, defautlConditionNode);
    }

    @Override
    public JobFlowNodeBuilder getJobFlowNodeBuilder(String nodeId) {
        initAISequenceJobFlowNodeBuilder();
        return this.sequenceJobFlowNodeBuilder.getJobFlowNodeBuilder(nodeId);
    }

    @Override
    public String addAnotherConditionJobFlowNodeBuilder(JobFlowNodeBuilder jobFlowNodeBuilder, NodeTrigger conditionNodeTrigger, boolean defaultConditionNode) {
        initAISequenceJobFlowNodeBuilder();
        return this.sequenceJobFlowNodeBuilder.addAnotherConditionJobFlowNodeBuilder(jobFlowNodeBuilder, conditionNodeTrigger, defaultConditionNode);
    }

    @Override
    public String addAnotherConditionJobFlowNodeBuilder(boolean allCondtionNodeMathfailedContinue, JobFlowNodeBuilder jobFlowNodeBuilder, NodeTrigger conditionNodeTrigger, boolean defaultConditionNode) {
        initAISequenceJobFlowNodeBuilder();
        return this.sequenceJobFlowNodeBuilder.addAnotherConditionJobFlowNodeBuilder(allCondtionNodeMathfailedContinue,jobFlowNodeBuilder, conditionNodeTrigger, defaultConditionNode);
    }

//    public AISequenceAgent addJobFlowNodeListener(JobFlowNodeListener jobFlowNodeListener){
//        this.initAISequenceJobFlowNodeBuilder();
//        sequenceJobFlowNodeBuilder.addJobFlowNodeListener(jobFlowNodeListener);
//        return this;
//    }

    ////////////////////////////原生工作流节点添加方法：结束/////////////////////////
    protected JobFlowNodeBuilder builderJobFlowNodeBuilder(){
        return this.getSequenceJobFlowNodeBuilder();
    }
    
}
