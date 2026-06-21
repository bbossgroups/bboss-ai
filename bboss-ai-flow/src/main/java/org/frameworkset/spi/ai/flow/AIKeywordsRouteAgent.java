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

import org.frameworkset.tran.jobflow.builder.JobFlowNodeBuilder;

import java.util.ArrayList;

/**
 * @author biaoping.yin
 * @Date 2026/4/14
 */
public class AIKeywordsRouteAgent
        extends AIRouteAgent {
    public AIKeywordsRouteAgent(String prompt ) {
        super( prompt);
        
    }

    public AIKeywordsRouteAgent(  ) {
        super(  );
         
    }

    @Override
    protected JobFlowNodeBuilder builderJobFlowNodeBuilder() {
        return new AIKeywordsRouterNodeBuilder(this);
    }

    public AIKeywordsRouteAgent addRoutingChoice(String agentId, String description){
        throw new UnsupportedOperationException("AIKeywordsRouteAgent must use method:addRoutingChoice(String agentId,String[] keywords, String description)");
    }
    

    public AIKeywordsRouteAgent addRoutingChoice(String agentId,String[] keywords, String description){
        if(routeChoiceList == null){
            routeChoiceList = new ArrayList<>();
        }
        routeChoiceList.add(new RouteChoice(agentId,description,keywords));
        return this;
    }


  
}
