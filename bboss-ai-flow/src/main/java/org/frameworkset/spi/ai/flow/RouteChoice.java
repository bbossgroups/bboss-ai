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

/**
 * @author biaoping.yin
 * @Date 2026/4/14
 */
public class RouteChoice {
    private String agentId;
    private String description;
    private String[] keywords;
    public RouteChoice(){
        
    }

    public RouteChoice(String agentId, String description) {
        this.agentId = agentId;
        this.description = description;
    }

    public RouteChoice(String agentId, String description,String[] keywords) {
        this.agentId = agentId;
        this.description = description;
        this.keywords = keywords;
    }

    public String[] getKeywords() {
        return keywords;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
 
}
