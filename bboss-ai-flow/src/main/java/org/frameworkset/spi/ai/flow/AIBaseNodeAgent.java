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
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

/**
 * @author biaoping.yin
 * @Date 2026/4/14
 */
public class AIBaseNodeAgent<T extends AIBaseNodeAgent> 
        extends AIAgent<T> {
    protected AIPlanAgent planAgent;
    public AIBaseNodeAgent(ToolsRegist mcpToolsRegist ) {
        super(   mcpToolsRegist);
    }
    public FluxSink<ServerEvent> getAgentFluxSink(){
        return planAgent.getAgentFluxSink();
    }
    public Flux<ServerEvent> getAgentFlux(){
        return planAgent.getFlux();
    }   
    public AIBaseNodeAgent( String prompt ) {
        super( prompt);
    }
    public AIBaseNodeAgent(  ) {
        super(  );
    }

    public AIPlanAgent getPlanAgent() {
        return planAgent;
    }

    public T setPlanAgent(AIPlanAgent aiPlanAgent) {
        this.planAgent = aiPlanAgent;
        return (T)this;
    }


//    public  String evalSystemPrompt(AgentMessage agentMessage){
//        String systemPrompt = this.getSystemPrompt();
//        if(systemPrompt == null){
////            AIAgent parent = this.getParentAgent();
////            if(parent != null) {
////                systemPrompt = parent.evalSystemPrompt(agentMessage);
////            }
////            else {
////                systemPrompt = this.planAgent.getSystemPrompt();
////                if(systemPrompt == null) {
//                    systemPrompt = agentMessage.getSystemPrompt();
////                }
////            }
//            
//        }
//        if(this.getParentAgent() != null) {
//            this.getParentAgent().setFirstSubAgentSystemPrompt(systemPrompt);
//        }
//        return systemPrompt;
//    }
//    public  String evalPrompt(AgentMessage agentMessage){
//        String prompt = this.getPrompt();
//        if(prompt == null){
////            AIAgent parent = this.getParentAgent();
////            if(parent != null) {
////                prompt = parent.evalPrompt(agentMessage);
////            }
////            else {
////                prompt = this.planAgent.getPrompt();
////                if(prompt == null) {
//                    prompt = agentMessage.getPrompt();
////                }
////            }
//        }
//        return prompt;
//    }

 
}
