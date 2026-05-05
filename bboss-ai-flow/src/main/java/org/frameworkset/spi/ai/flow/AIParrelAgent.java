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
import org.frameworkset.spi.ai.model.AgentMessage;
import org.frameworkset.spi.ai.model.LastSessionMessage;
import org.frameworkset.spi.ai.model.ServerEvent;
import org.frameworkset.spi.ai.store.AgentSessionStore;
import org.frameworkset.spi.reactor.DisposeEventHandler;
import org.frameworkset.tran.jobflow.JobFlowNode;
import org.frameworkset.tran.jobflow.context.JobFlowNodeExecuteContext;
import org.frameworkset.tran.jobflow.listener.JobFlowNodeListener;
import org.frameworkset.tran.jobflow.script.TriggerScriptAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.List;

/**
 * 智能体流程编排:并行智能体编排
 * 当加入到工作流后，需要设置
 * @author biaoping.yin
 * @Date 2026/4/12
 */
public class AIParrelAgent extends AIBaseNodeAgent<AIParrelAgent> {
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
 

    private void initAIParrelJobFlowNodeBuilder(){
        if(parrelJobFlowNodeBuilder == null){
            parrelJobFlowNodeBuilder = new AIParrelJobFlowNodeBuilder(this);
            logger.info("聚合和保存并行智能体任务节点[{},{}]中子智能体节点消息",this.getAgentId(),this.getAgentName());
            //聚合和保存并行智能体任务节点中子智能体节点消息
            parrelJobFlowNodeBuilder.addJobFlowNodeListener(new JobFlowNodeListener() {
                @Override
                public void beforeExecute(JobFlowNodeExecuteContext jobFlowNodeExecuteContext) {
                    
                }

                @Override
                public void afterExecute(JobFlowNodeExecuteContext jobFlowNodeExecuteContext, Throwable throwable) {
 
                    List<LastSessionMessage> lastSessionMessages = getLastSessionMessages();
                    if(SimpleStringUtil.isNotEmpty(lastSessionMessages)) {
                        StringBuilder builder = new StringBuilder();
                        for(LastSessionMessage lastSessionMessage:lastSessionMessages){
                            if(builder.length() > 0)
                                builder.append("\n");
                            builder.append(lastSessionMessage.getData());
                        }
                        String data = builder.toString();
                        AIParrelAgent.this.addAgentResultSessionMessage(data);
                        if(planAgent.isStream() && !isDisableStream()){
                            ServerEvent serverEvent = new ServerEvent();
                            serverEvent.setDone(true);
                            serverEvent.setAgent(AIParrelAgent.this);
                            serverEvent.setData(data);
                            serverEvent.setContent(data);
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

    @Override
    public FluxSink<ServerEvent> getAgentFluxSink(){
        return planAgent.getAgentFluxSink();
    }

    @Override
    public DisposeEventHandler getDisposeEventHandler(){
        return planAgent.getDisposeEventHandler();
    }
      
    /**
     * 添加智能体工作流节点
     * @param aiAgent
     * @return
     */
    public AIParrelAgent addAgent(AIBaseNodeAgent aiAgent) {
        return addAgent(aiAgent,( TriggerScriptAPI )null);
    }
     

    /**
     * 添加智能体工作流节点
     * @param aiAgent
     * @return
     */
    public AIParrelAgent addAgent(AIBaseNodeAgent aiAgent, TriggerScriptAPI triggerScriptAPI) {
        this.initAIParrelJobFlowNodeBuilder();
        aiAgent.setPlanAgent(planAgent);
        aiAgent.setParentAgent(this);
        aiAgent.setDisableStream(true);
        parrelJobFlowNodeBuilder.addJobFlowNodeBuilder(new AIAgentNodeBuilder(aiAgent ).setTriggerScriptAPI(triggerScriptAPI));
        return this;
    }

    /**
     * 添加工作流节点
     * @param flowNode
     * @return
     */
    public AIParrelAgent addFlowNode(AIFlowNode flowNode) {
        return addFlowNode(flowNode,( TriggerScriptAPI )null);
    }

    @Override
    public void reactMessage(AgentMessage agentMessage) {
        super.reactMessage(agentMessage);
    }

  
    
    /**
     * 添加工作流节点
     * @param flowNode
     * @param triggerScriptAPI 
     * @return
     */
    public AIParrelAgent addFlowNode(AIFlowNode flowNode, TriggerScriptAPI triggerScriptAPI) {
        initAIParrelJobFlowNodeBuilder();
        flowNode.setPlanAgent(planAgent);
        
        parrelJobFlowNodeBuilder.addJobFlowNodeBuilder(new AIFlowNodeBuilder(flowNode ).setTriggerScriptAPI(triggerScriptAPI));
        return this;
    }
  
    @Override
    public Flux<ServerEvent> getFlux() {
        return planAgent.getFlux();
    }
     
 
  
    
    
}
