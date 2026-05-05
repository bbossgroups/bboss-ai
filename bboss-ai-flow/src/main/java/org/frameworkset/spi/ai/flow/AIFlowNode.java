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
import org.frameworkset.spi.ai.model.ServerEvent;
import org.frameworkset.spi.reactor.DisposeEventHandler;
import org.frameworkset.tran.jobflow.context.JobFlowNodeExecuteContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

/**
 * 普通流程节点，非智能体节点
 * @author biaoping.yin
 * @Date 2026/4/14
 */
public abstract class AIFlowNode<T extends AIFlowNode> {
    protected String nodeId;
    protected String nodeName;
    protected AIPlanAgent planAgent;
 
    public FluxSink<ServerEvent> getAgentFluxSink(){
        return planAgent.getAgentFluxSink();
    }

    public DisposeEventHandler getDisposeEventHandler(){
        return planAgent.getDisposeEventHandler();
    }
    public Flux<ServerEvent> getAgentFlux(){
        return planAgent.getFlux();
    }   
     
    public AIFlowNode(  ) {
        super(  );
        this.nodeId = SimpleStringUtil.getUUID32();
        this.nodeName = "AIFlowNode";
    }

    public AIPlanAgent getPlanAgent() {
        return planAgent;
    }

    public T setPlanAgent(AIPlanAgent aiPlanAgent) {
        this.planAgent = aiPlanAgent;
        return (T)this;
    }

    /**
     * 由子类继承和实现
     * @param jobFlowNodeExecuteContext
     * @return
     */
    public abstract Object call(JobFlowNodeExecuteContext jobFlowNodeExecuteContext);

    public String getNodeId() {
        return nodeId;
    }

    public T setNodeId(String nodeId) {
        this.nodeId = nodeId;
        return (T)this;
    }
        

    public String getNodeName() {
        return nodeName;
    }

    public T setNodeName(String nodeName) {
        this.nodeName = nodeName;
        return (T)this;
    }
}
