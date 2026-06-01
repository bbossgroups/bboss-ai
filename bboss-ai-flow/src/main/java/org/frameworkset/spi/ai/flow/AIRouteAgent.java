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

import org.frameworkset.spi.ai.model.AgentMessage;
import org.frameworkset.tran.jobflow.builder.JobFlowNodeBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * @author biaoping.yin
 * @Date 2026/4/14
 */
public class AIRouteAgent 
        extends AIBaseNodeAgent<AIRouteAgent> {
    private int retryTimes = 3;
    public AIRouteAgent( String prompt ) {
        super( prompt);
        this.disablePush2ParentLastSubMessage = true;
        this.disableReferenceParentLastSubMessage = true;
    }

    public AIRouteAgent(  ) {
        super(  );
        this.disablePush2ParentLastSubMessage = true;
        this.disableReferenceParentLastSubMessage = true;
    }
    public AIRouteAgent setAgentMessage(AgentMessage agentMessage) {
        this.agentMessage = agentMessage;
        return this;
    }

 

    private List<RouteChoice> routeChoiceList;

    public AIRouteAgent setRouteChoiceList(List<RouteChoice> routeChoiceList) {
        this.routeChoiceList = routeChoiceList;
        return this;
    }
    @Override
    protected JobFlowNodeBuilder builderJobFlowNodeBuilder(){
        return new AIRouterNodeBuilder(this );
    }
    

    public AIRouteAgent addRoutingChoice(String agentId, String description){
        if(routeChoiceList == null){
            routeChoiceList = new ArrayList<>();
        }
        routeChoiceList.add(new RouteChoice(agentId,description));
        return this;
    }

    public AIRouteAgent addDefaultRoutingChoice(String agentId,String description){
        if(routeChoiceList == null){
            routeChoiceList = new ArrayList<>();
        }
        routeChoiceList.add(new RouteChoice(agentId,description));
        return  this;
    }

    public List<RouteChoice> getRouteChoiceList() {
        return routeChoiceList;
    }

    public AIRouteAgent setRetryTimes(int retryTimes) {
        this.retryTimes = retryTimes;
        return this;
    }

    public int getRetryTimes() {
        return retryTimes;
    }
}
