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
import org.frameworkset.tran.jobflow.JobFlow;
import org.frameworkset.tran.jobflow.builder.JobFlowBuilder;
import org.frameworkset.tran.jobflow.schedule.JobFlowScheduleConfig;
import org.frameworkset.tran.jobflow.script.TriggerScriptAPI;
import reactor.core.publisher.Flux;

/**
 * 智能体流程编排
 * @author biaoping.yin
 * @Date 2026/4/12
 */
public class AIPlanAgent extends AIAgent<AIPlanAgent> {

    private AgentSessionStore mainSessionStore;
    private StoreContext storeContext;
    private AgentSessionStoreBuilder agentSessionStoreBuilder = new DefaultAgentSessionStoreBuilder();
    private JobFlowBuilder jobFlowBuilder;
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
     * 添加工作流节点
     * @param aiAgent
     * @return
     */
    public AIPlanAgent addAgent(AIBaseNodeAgent aiAgent) {
        return addAgent(aiAgent,( TriggerScriptAPI )null);
    }

    /**
     * 添加工作流节点
     * @param aiAgent
     * @return
     */
    public AIPlanAgent addAgent(AIBaseNodeAgent aiAgent, TriggerScriptAPI triggerScriptAPI) {
        initAIJobFlowBuilder();
        aiAgent.setPlanAgent(this);
        aiAgent.setParentAgent(this);
        jobFlowBuilder.addJobFlowNodeBuilder(new AINodeBuilder(aiAgent ).setTriggerScriptAPI(triggerScriptAPI));
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
        if(jobFlowBuilder != null){
            initSessionStore();
            JobFlowScheduleConfig jobFlowScheduleConfig = new JobFlowScheduleConfig();
            jobFlowScheduleConfig.setExecuteOneTime(true);
            jobFlowBuilder.setJobFlowScheduleConfig(jobFlowScheduleConfig);
            JobFlow jobflow = jobFlowBuilder.build();
            jobflow.execute();
           
        }
        return null;
    }
    
    
}
