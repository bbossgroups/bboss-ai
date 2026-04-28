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
import org.frameworkset.spi.ai.model.*;
import org.frameworkset.spi.ai.store.AgentSessionStore;
import org.frameworkset.spi.ai.store.AgentSessionStoreBuilder;
import org.frameworkset.spi.ai.store.DefaultAgentSessionStoreBuilder;
import org.frameworkset.spi.ai.store.StoreContext;
import org.frameworkset.spi.ai.util.AIResponseUtil;
import org.frameworkset.spi.reactor.DisposeEventHandler;
import org.frameworkset.spi.reactor.ReactorCallException;
import org.frameworkset.tran.jobflow.JobFlow;
import org.frameworkset.tran.jobflow.builder.JobFlowBuilder;
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
public class AIPlanAgent extends AIAgent<AIPlanAgent> {
    private static Logger logger = LoggerFactory.getLogger(AIPlanAgent.class);
    private AgentSessionStore mainSessionStore;
    private StoreContext storeContext;
    private AgentSessionStoreBuilder agentSessionStoreBuilder = new DefaultAgentSessionStoreBuilder();
    private AIJobFlowBuilder jobFlowBuilder;
    public AIPlanAgent(StoreContext storeContext) {
        this.storeContext = storeContext;
        
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

    private void initSessionStore(){
        if(mainSessionStore == null && storeContext != null){
            mainSessionStore = this.agentSessionStoreBuilder.build(storeContext);
            mainSessionStore.setAIAgent(this);
            if(agentMessage != null && agentMessage instanceof SessionAgentMessage){
                ((SessionAgentMessage)agentMessage).setMainSessionStore(mainSessionStore);
            }
        }
    }

 

 

    /**
     * 添加路由网关节点：负责根据用户问题决定后续的路由节点，AI进行自主决策
     * @param aiRouteAgent
     * @return
     */
    public AIPlanAgent addAIRouteAgent(AIRouteAgent aiRouteAgent) {
        initAIJobFlowBuilder();
        aiRouteAgent.setPlanAgent(this);
        aiRouteAgent.setParentAgent(this);
        jobFlowBuilder.addJobFlowNodeBuilder(new AIRouterNodeBuilder(aiRouteAgent));
        return this;
    }


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
    public AIPlanAgent addRouteChoiceAgent(AIBaseNodeAgent aiRouteChoiceAgent) {
        initAIJobFlowBuilder();
        aiRouteChoiceAgent.setPlanAgent(this);
        aiRouteChoiceAgent.setParentAgent(this);
       
        jobFlowBuilder.addConditionJobFlowNodeBuilder(new AIRouterChoiceNodeBuilder(aiRouteChoiceAgent )
                .setTriggerScriptAPI(nodeTriggerContext -> {
                    String agentId = (String) nodeTriggerContext.getContainerContextData("routeChoice",true);
                    if(agentId == null){
                        agentId = (String) nodeTriggerContext.getFlowContextData("routeChoice");
                    }
                    if(agentId != null && agentId.equals(aiRouteChoiceAgent.getAgentId())){
                        return true;
                    }
                    return false;
                }));
        return this;
    }


    /**
     * 添加默认路由节点
     * @param defaultRouteChoiceAgent
     * @return
     */
    public AIPlanAgent addDefaultRouteChoiceAgent(AINodeAgent defaultRouteChoiceAgent) {
        initAIJobFlowBuilder();
        defaultRouteChoiceAgent.setPlanAgent(this);
        defaultRouteChoiceAgent.setParentAgent(this);
        jobFlowBuilder.addConditionJobFlowNodeBuilder(new AIRouterChoiceNodeBuilder(defaultRouteChoiceAgent ),true);
        return this;
    }
    /**
     * 添加工作流节点
     * @param judgeAgent
     * @return
     */
    public AIPlanAgent addJudgeAgent(AIJudgeAgent judgeAgent) {
        initAIJobFlowBuilder();
        judgeAgent.setPlanAgent(this);
        judgeAgent.setParentAgent(this);
        jobFlowBuilder.addJobFlowNodeBuilder(new AIJudgeNodeBuilder(judgeAgent ));
        return this;
    }
    /**
     * 添加智能体工作流节点
     * @param aiAgent
     * @return
     */
    public AIPlanAgent addAgent(AIBaseNodeAgent aiAgent) {
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
     * 添加智能体工作流节点
     * @param aiAgent
     * @return
     */
    public AIPlanAgent addAgent(AIBaseNodeAgent aiAgent, TriggerScriptAPI triggerScriptAPI) {
        initAIJobFlowBuilder();
        aiAgent.setPlanAgent(this);
        aiAgent.setParentAgent(this);
        jobFlowBuilder.addJobFlowNodeBuilder(new AIAgentNodeBuilder(aiAgent ).setTriggerScriptAPI(triggerScriptAPI));
        return this;
    }

    /**
     * 添加工作流节点
     * @param flowNode
     * @return
     */
    public AIPlanAgent addFlowNode(AIFlowNode flowNode) {
        return addFlowNode(flowNode,( TriggerScriptAPI )null);
    }


    /**
     * 添加工作流节点
     * @param flowNode
     * @param triggerScriptAPI 
     * @return
     */
    public AIPlanAgent addFlowNode(AIFlowNode flowNode, TriggerScriptAPI triggerScriptAPI) {
        initAIJobFlowBuilder();
        flowNode.setPlanAgent(this);
        jobFlowBuilder.addJobFlowNodeBuilder(new AIFlowNodeBuilder(flowNode ).setTriggerScriptAPI(triggerScriptAPI));
        return this;
    }

    public LastSessionMessage chat( ) {
        
        if(jobFlowBuilder != null){
            initSessionStore();
            this.evalPrompt(this.agentMessage);
            this.evalSystemPrompt(this.agentMessage);
            JobFlowScheduleConfig jobFlowScheduleConfig = new JobFlowScheduleConfig();
            jobFlowScheduleConfig.setExecuteOneTime(true);
            jobFlowBuilder.setJobFlowScheduleConfig(jobFlowScheduleConfig);
            jobFlowBuilder.setJobFlowId(this.getAgentId());
            jobFlowBuilder.setJobFlowName(this.getAgentName());
            JobFlow jobflow = jobFlowBuilder.build();
       
            jobflow.execute();
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

    public FluxSink<ServerEvent> getSink() {
        return sink;
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
    
    
}
