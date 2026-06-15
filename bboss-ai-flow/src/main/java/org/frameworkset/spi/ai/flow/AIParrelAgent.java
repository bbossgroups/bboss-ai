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

import com.frameworkset.util.SimpleStringUtil;
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

import java.util.List;

/**
 * 智能体流程编排:并行智能体编排
 * 当加入到工作流后，需要设置
 * @author biaoping.yin
 * @Date 2026/4/12
 */
public class AIParrelAgent extends AIBaseNodeAgent<AIParrelAgent>  implements AIContainerAgent<AIParrelAgent>{
    private static Logger logger = LoggerFactory.getLogger(AIParrelAgent.class);
    private AIParrelJobFlowNodeBuilder parrelJobFlowNodeBuilder;
    public AIParrelAgent( AIPlanAgent planAgent) {
        
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
        return new ParrelAgentSessionStoreMemory(parentSessionStore,sessionSize);
    }

    /**
     * 默认构造并行任务输出方法，可以重载方法实现自定义输出格式
     * @param lastSessionMessages
     * @return
     */
    public String buildResult(List<LastSessionMessage> lastSessionMessages){
        StringBuilder builder = new StringBuilder();
        for(LastSessionMessage lastSessionMessage:lastSessionMessages){
            if(builder.length() > 0)
                builder.append("\n");
//            builder.append(lastSessionMessage.getMsgAgentId()).append(":").append(lastSessionMessage.getData());
            builder.append(lastSessionMessage.getData());
        }
        String data = builder.toString();
        return data;
    }
    
    private void initAIParrelJobFlowNodeBuilder(){
        if(parrelJobFlowNodeBuilder == null){
            parrelJobFlowNodeBuilder = new AIParrelJobFlowNodeBuilder(this);
            logger.info("聚合、保存和激发并行智能体任务节点[{},{}]中子智能体节点消息",this.getAgentId(),this.getAgentName());
            //聚合和保存并行智能体任务节点中子智能体节点消息
            parrelJobFlowNodeBuilder.addJobFlowNodeListener(new JobFlowNodeListener() {
                @Override
                public void beforeExecute(JobFlowNodeExecuteContext jobFlowNodeExecuteContext) {
                    cleanLastSessionMessages();
                }

                @Override
                public void afterExecute(JobFlowNodeExecuteContext jobFlowNodeExecuteContext, Throwable throwable) {
 
                    List<LastSessionMessage> lastSessionMessages = getLastSessionMessages();
                    if(SimpleStringUtil.isNotEmpty(lastSessionMessages)) {                      
                        String data = buildResult(  lastSessionMessages);
                        AIParrelAgent.this.addAgentResultSessionMessage(null,data);
                        ServerEvent serverEvent = new ServerEvent();
                        serverEvent.setDone(true);
                        serverEvent.setAgent(AIParrelAgent.this);
                        serverEvent.setData(data);
                        serverEvent.setContent(data);
                        AIFlowUtil.outputResult(AIParrelAgent.this, serverEvent,  jobFlowNodeExecuteContext);
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

    public AIParrelJobFlowNodeBuilder getParrelJobFlowNodeBuilder() {
        return parrelJobFlowNodeBuilder;
    }


 
 
    public AIParrelAgent addAgent(AppendToParentAgent aiAgent, TriggerScriptAPI triggerScriptAPI) {
        initAIParrelJobFlowNodeBuilder();
        aiAgent.setDisableStream(true);
        aiAgent.appendToParentAgent(this,triggerScriptAPI);
        return  this;
    }







 



    ////////////////////////////原生工作流节点添加方法：开始/////////////////////////
    public AIParrelAgent addJobFlowNodeBuilder(JobFlowNodeBuilder jobFlowNodeBuilder){
        this.initAIParrelJobFlowNodeBuilder();
        parrelJobFlowNodeBuilder.addJobFlowNodeBuilder(jobFlowNodeBuilder);
        return this;
    }

    public AIParrelAgent addJobFlowNodeBuilder(JobFlowNodeBuilder jobFlowNodeBuilder,TriggerScriptAPI triggerScriptAPI){
        this.initAIParrelJobFlowNodeBuilder();
        parrelJobFlowNodeBuilder.addJobFlowNodeBuilder(jobFlowNodeBuilder.setTriggerScriptAPI(triggerScriptAPI));
        return this;
    }

//    public AIParrelAgent addJobFlowNodeListener(JobFlowNodeListener jobFlowNodeListener){
//        this.initAIParrelJobFlowNodeBuilder();
//        parrelJobFlowNodeBuilder.addJobFlowNodeListener(jobFlowNodeListener);
//        return this;
//    }

    ////////////////////////////原生工作流节点添加方法：结束/////////////////////////
    @Override
    protected JobFlowNodeBuilder builderJobFlowNodeBuilder(){
        return this.getParrelJobFlowNodeBuilder();
    }
    /**
     * 添加并行智能体节点，并设置条件触发器
     */
    public void appendToParentAgent(AIContainerAgent parentAgent, TriggerScriptAPI triggerScriptAPI){
        if(this.getAgentId() == null){
            this.agentId = parentAgent.genSubAgentId();
            this.agentName = parentAgent.genSubAgentName(agentId);
        }
        this.setParentAgent((AIAgent)parentAgent);
        if(this.getPlanAgent() == null){
             
             this.setPlanAgent(parentAgent.getPlanAgent());
             
        }
        parentAgent.addJobFlowNodeBuilder(this.getParrelJobFlowNodeBuilder(),triggerScriptAPI);
    }
    
    /**1111111111111111111111111*/

 

 




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

 


//    /**
//     * 添加智能体工作流节点：为当前作业节点添加后续条件分支，可以连续添加多个,通过conditionNodeTrigger指定条件
//     * @param aiAgent
//     * @return
//     */
//    public AISequenceAgent addConditionFlowNode(AIBaseNodeAgent aiAgent  ){
//        return addConditionFlowNode(  aiAgent , (TriggerScriptAPI)null);
//    }
//
//    public AISequenceAgent addConditionFlowNode(boolean allCondtionNodeMathfailedContinue,AIBaseNodeAgent aiAgent , TriggerScriptAPI conditionNodeTrigger){
//        return addConditionFlowNode(  allCondtionNodeMathfailedContinue,  aiAgent ,   conditionNodeTrigger,false);
//    }
//
//    public AISequenceAgent addConditionFlowNode(boolean allCondtionNodeMathfailedContinue,AIBaseNodeAgent aiAgent , TriggerScriptAPI conditionNodeTrigger,boolean defautlConditionNode){
//        this.initAISequenceJobFlowNodeBuilder( );
//        JobFlowNodeBuilder jobFlowNodeBuilder = sequenceJobFlowNodeBuilder.getJobFlowNodeBuilder(aiAgent.getAgentId());
//        if(jobFlowNodeBuilder == null) {
//            setHeaderAgent(aiAgent);
//            aiAgent.setPlanAgent(planAgent);
//            aiAgent.setParentAgent(this);
//            jobFlowNodeBuilder = new AIAgentNodeBuilder(aiAgent);
//        }
////        aiAgent.setDisableStream(true);
//        sequenceJobFlowNodeBuilder.addConditionJobFlowNodeBuilder(allCondtionNodeMathfailedContinue,jobFlowNodeBuilder,conditionNodeTrigger,defautlConditionNode);
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
    public AIParrelAgent addAgent(AppendToParentAgent aiAgent) {
        return addAgent(aiAgent,(TriggerScriptAPI)null);
    }


 
   
 



////////////////////////////原生工作流节点添加方法：开始/////////////////////////
 

 

    @Override
    public String addConditionJobFlowNodeBuilder(JobFlowNodeBuilder jobFlowNodeBuilder, boolean defaultNode) {
        throw new UnsupportedOperationException("AI parrel agent Not supported addConditionJobFlowNodeBuilder.");
    }

    @Override
    public String addConditionJobFlowNodeBuilder(JobFlowNodeBuilder jobFlowNodeBuilder, TriggerScriptAPI triggerScriptAPI) {
        throw new UnsupportedOperationException("AI parrel agent Not supported addConditionJobFlowNodeBuilder.");
    }

    @Override
    public String addConditionJobFlowNodeBuilder(boolean allCondtionNodeMathfailedContinue, JobFlowNodeBuilder jobFlowNodeBuilder, TriggerScriptAPI triggerScriptAPI, boolean defautlConditionNode) {
        throw new UnsupportedOperationException("AI parrel agent Not supported addConditionJobFlowNodeBuilder.");
    }

    @Override
    public JobFlowNodeBuilder getJobFlowNodeBuilder(String nodeId) {
//        throw new UnsupportedOperationException("AI parrel agent Not supported getJobFlowNodeBuilder.");
        return null;
    }

    @Override
    public String addAnotherConditionJobFlowNodeBuilder(JobFlowNodeBuilder jobFlowNodeBuilder, NodeTrigger conditionNodeTrigger, boolean defaultConditionNode) {
        throw new UnsupportedOperationException("AI parrel agent Not supported addAnotherConditionJobFlowNodeBuilder.");
    }

    @Override
    public String addAnotherConditionJobFlowNodeBuilder(boolean allCondtionNodeMathfailedContinue, JobFlowNodeBuilder jobFlowNodeBuilder, NodeTrigger conditionNodeTrigger, boolean defaultConditionNode) {
        throw new UnsupportedOperationException("AI parrel agent Not supported addAnotherConditionJobFlowNodeBuilder.");
    }


    ////////////////////////////原生工作流节点添加方法：结束/////////////////////////
    
}
