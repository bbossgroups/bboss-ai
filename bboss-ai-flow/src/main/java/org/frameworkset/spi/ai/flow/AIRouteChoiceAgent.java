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
import org.frameworkset.spi.ai.tools.ToolsRegist;

/**
 * @author biaoping.yin
 * @Date 2026/4/14
 */
public class AIRouteChoiceAgent<T extends AIRouteChoiceAgent> 
        extends AIAgent<T> {
    private AIPlanAgent aiPlanAgent;
    public AIRouteChoiceAgent(ToolsRegist mcpToolsRegist ) {
        super(   mcpToolsRegist);
    }

    public AIRouteChoiceAgent(  ) {
        super(  );
    }

    public AIPlanAgent getAiPlanAgent() {
        return aiPlanAgent;
    }

    public T setAiPlanAgent(AIPlanAgent aiPlanAgent) {
        this.aiPlanAgent = aiPlanAgent;
        return (T)this;
    }

 
}
