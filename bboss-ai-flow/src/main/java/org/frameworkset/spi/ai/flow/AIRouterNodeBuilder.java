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

import com.frameworkset.util.JsonUtil;
import com.frameworkset.util.SimpleStringUtil;
import org.frameworkset.spi.ai.model.AIRuntimeException;
import org.frameworkset.spi.ai.model.AgentMessage;
import org.frameworkset.spi.ai.model.ChatAgentMessage;
import org.frameworkset.spi.ai.model.ServerEvent;
import org.frameworkset.tran.jobflow.builder.CallableJobFlowNodeBuilder;
import org.frameworkset.tran.jobflow.context.JobFlowExecuteContext;
import org.frameworkset.tran.jobflow.context.JobFlowNodeExecuteContext;

import java.util.List;

/**
 * @author biaoping.yin
 * @Date 2026/4/14
 */
public class AIRouterNodeBuilder extends AIBaseNodeBuilder {
    private String prompt2 = "# 用户问题：${prompt}\r# 根据用户问题，从以下智能体列表中选择一个最合适的智能体记录\r${routeChoiceList}\r# 输出要求：\r" +
            "1.如果匹配到智能体将匹配的智能体JSON串包含在```json和```中输出，将其他文字都去除\r" +
            "2.如果未匹配到智能体，请返回空内容\r" 
            ;
    // 新增：极简提示词，直接要求纯 JSON
    private String promptMinimal = "根据用户问题选择最合适的智能体。\n\n" +
            "用户问题：${prompt}\n\n" +
            "候选智能体：${routeChoiceList}\n\n" +
            "直接输出JSON，不要任何其他文字：";

    private String prompt = prompt2;
    private String prompt1 = "# 用户问题：${prompt}\r# 根据用户问题，从以下智能体列表中选择一个最合适的智能体记录\r${routeChoiceList}\r# 输出要求：\r" +
            "只返回匹配的智能体JSON串，将其他文字都去除"
            ;

    private AIRouteAgent routeAgent;
    public AIRouterNodeBuilder(AIRouteAgent routeAgent) {
        super( routeAgent);
        this.routeAgent = routeAgent;
       
    }
    public AIRouterNodeBuilder(String nodeName) {
        super(nodeName);
    }

    public AIRouterNodeBuilder(String nodeId, String nodeName) {
        super(nodeId, nodeName);
    }

 

    public AIRouterNodeBuilder setPrompt(String prompt) {
        this.prompt = prompt;
        return this;
    }
 
    
    @Override
    public Object call(JobFlowNodeExecuteContext jobFlowNodeExecuteContext) throws Exception {      
        List<RouteChoice> routeChoiceList = routeAgent.getRouteChoiceList();
        String prompt = routeAgent.getPrompt();
        if(SimpleStringUtil.isEmpty(prompt)){
            prompt = this.prompt;
            
        }
        AgentMessage agentMessage = routeAgent.getAgentMessage() != null ? routeAgent.getAgentMessage() : planAgent.getAgentMessage();
        if(agentMessage == null){
            throw new AIRuntimeException("agentMessage is null");
        }
        prompt = prompt.replace("${prompt}",agentMessage.getPrompt()).replace("${routeChoiceList}",JsonUtil.object2json(routeChoiceList));
        routeAgent.setPrompt(prompt);
        ServerEvent serverEvent = routeAgent.chat((ChatAgentMessage)agentMessage);
        JobFlowNodeExecuteContext containerJobFlowNodeExecuteContext = jobFlowNodeExecuteContext.getContainerJobFlowNodeExecuteContext();
        JobFlowExecuteContext jobFlowExecuteContext = jobFlowNodeExecuteContext.getContainerJobFlowExecuteContext();
        String data  = serverEvent.getData();
        RouteChoice result = null;
        if(data != null){
            MarkdownJsonExtractor extractor = new MarkdownJsonExtractor();
            List<String> jsonList = extractor.extractAll(data);
            if(jsonList != null && jsonList.size() > 0) {
                result = JsonUtil.json2Object(jsonList.get(jsonList.size() - 1), RouteChoice.class);
                if (containerJobFlowNodeExecuteContext != null) {
                    containerJobFlowNodeExecuteContext.addContextData("routeChoice", result.getAgentId());
                } else {
                    jobFlowExecuteContext.addContextData("routeChoice", result.getAgentId());
                }
            }
        }
       
        
        return null;
    }

 


}
