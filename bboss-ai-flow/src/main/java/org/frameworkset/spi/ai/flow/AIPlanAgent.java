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
import reactor.core.publisher.Flux;

/**
 * 智能体流程编排
 * @author biaoping.yin
 * @Date 2026/4/12
 */
public class AIPlanAgent extends AIAgent<AIPlanAgent> {

    private String prompt;
    private AgentSessionStore mainSessionStore;
    private AgentMessage agentMessage;
    private StoreContext storeContext;
    private AgentSessionStoreBuilder agentSessionStoreBuilder = new DefaultAgentSessionStoreBuilder();
    private JobFlowBuilder jobFlowBuilder;
    public AIPlanAgent(StoreContext storeContext) {
        this.storeContext = storeContext;
        
    }
    
    public LastSessionMessage getLastSessionMessage(){
        return mainSessionStore.getLastSubAgentSessionMessage();
    }

    public JobFlowBuilder getJobFlowBuilder() {
        return jobFlowBuilder;
    }

    private void initSessionStore(){
        if(mainSessionStore == null && storeContext != null){
            mainSessionStore = this.agentSessionStoreBuilder.build(storeContext);
            if(agentMessage != null && agentMessage instanceof SessionAgentMessage){
                ((SessionAgentMessage)agentMessage).setMainSessionStore(mainSessionStore);
            }
//            mainSessionStore = sessionStore;
        }
    }

    public AgentMessage getAgentMessage() {
        return agentMessage;
    }

    public AIPlanAgent setAgentMessage(AgentMessage agentMessage) {
        this.agentMessage = agentMessage;
        return this;
    }

    /**
     * 添加路由网关节点：负责根据用户问题决定后续的路由节点，AI进行自主决策
     * @param aiRouteAgent
     * @return
     */
    public AIPlanAgent addAIRouteAgent(AIRouteAgent aiRouteAgent) {
        if(jobFlowBuilder == null){
            jobFlowBuilder = new JobFlowBuilder();
        }
        aiRouteAgent.setAiPlanAgent(this);
        jobFlowBuilder.addJobFlowNodeBuilder(new AIRouterNodeBuilder(aiRouteAgent));
        return this;
    }

    public String getPrompt() {
        return prompt;
    }

    public AIPlanAgent setPrompt(String prompt) {
        this.prompt = prompt;
        return this;
    }

    /**
     * 添加路由节点
     * @param aiRouteChoiceAgent
     * @return
     */
    public AIPlanAgent addRouteChoiceAgent(AIBaseRouteChoiceAgent aiRouteChoiceAgent) {
        if(jobFlowBuilder == null){
            jobFlowBuilder = new JobFlowBuilder();
            
        }
        aiRouteChoiceAgent.setPlanAgent(this);
       
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
    public AIPlanAgent addDefaultRouteChoiceAgent(AIRouteChoiceAgent defaultRouteChoiceAgent) {
        if(jobFlowBuilder == null){
            jobFlowBuilder = new JobFlowBuilder();
        }
        defaultRouteChoiceAgent.setPlanAgent(this);
        jobFlowBuilder.addConditionJobFlowNodeBuilder(new AIRouterChoiceNodeBuilder(defaultRouteChoiceAgent ),true);
        return this;
    }

    public LastSessionMessage chat( ) {
        
        if(jobFlowBuilder != null){
            initSessionStore();
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
