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
import org.frameworkset.spi.ai.model.AIRuntimeException;
import org.frameworkset.spi.ai.model.AgentMessage;
import org.frameworkset.spi.ai.model.ServerEvent;
import org.frameworkset.spi.ai.model.TraceMessage;
import org.frameworkset.spi.ai.prompt.PromptEval;
import org.frameworkset.spi.ai.store.SessionMessage;
import org.frameworkset.tran.jobflow.context.JobFlowExecuteContext;
import org.frameworkset.tran.jobflow.context.JobFlowNodeExecuteContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.FluxSink;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.frameworkset.spi.ai.model.ServerEvent.TYPE_TRACE;

/**
 * @author biaoping.yin
 * @Date 2026/4/14
 */
public class AIKeywordsRouterNodeBuilder extends AIBaseNodeBuilder {
    private static Logger logger = LoggerFactory.getLogger(AIKeywordsRouterNodeBuilder.class); 
     

    private AIKeywordsRouteAgent routeAgent;
    public AIKeywordsRouterNodeBuilder(AIKeywordsRouteAgent routeAgent) {
        super( routeAgent);
        this.routeAgent = routeAgent;
       
    }
    public AIKeywordsRouterNodeBuilder(String nodeName) {
        super(nodeName);
    }

    public AIKeywordsRouterNodeBuilder(String nodeId, String nodeName) {
        super(nodeId, nodeName);
    }

 
 
    
    @Override
    public Object call(JobFlowNodeExecuteContext jobFlowNodeExecuteContext) throws Exception {
        long start = System.currentTimeMillis();
        List<RouteChoice> routeChoiceList = routeAgent.getRouteChoiceList();
        
		AgentMessage agentMessage = routeAgent.getAgentMessage() != null ? routeAgent.getAgentMessage() : planAgent.getAgentMessage();
		if(agentMessage == null){
			throw new AIRuntimeException("agentMessage is null");
		}
		String prompt = routeAgent.evalPrompt(agentMessage);
         
        jobFlowNodeExecuteContext.addContextData("route.ChoiceList", JsonUtil.object2json(routeChoiceList));
        
        PromptEval promptEval = new PromptEval();
        prompt = promptEval.eval(prompt, jobFlowNodeExecuteContext);
        RouteChoice result = null;
        for(RouteChoice routeChoice: routeChoiceList) {
			String keywords[] = routeChoice.getKeywords();
            for (String kw : keywords) {
                if (prompt.contains(kw)) {
                    result = routeChoice;
                    break;
                }
            }
            if(result != null){
                break;
            }
        }
        recordRoutechoice(  result,  jobFlowNodeExecuteContext,  start);      
         
        
        return null;
    }
    
    protected void recordRoutechoice(RouteChoice result,JobFlowNodeExecuteContext jobFlowNodeExecuteContext,long start){
        if(result != null) {
            
            JobFlowNodeExecuteContext containerJobFlowNodeExecuteContext = jobFlowNodeExecuteContext.getContainerJobFlowNodeExecuteContext();
            JobFlowExecuteContext jobFlowExecuteContext = jobFlowNodeExecuteContext.getContainerJobFlowExecuteContext();
            if (containerJobFlowNodeExecuteContext != null) {
                containerJobFlowNodeExecuteContext.addContextData("routeChoice", result.getAgentId());
            } else {
                jobFlowExecuteContext.addContextData("routeChoice", result.getAgentId());
            }
        }
        ServerEvent traceServerEvent = new ServerEvent();

        String message = result == null? "未匹配到智能体" : "匹配到智能体："+result.getAgentId()+","+result.getDescription();
        traceServerEvent.setData(message);
        traceServerEvent.setType(TYPE_TRACE);
        TraceMessage traceMessage = new TraceMessage();
        Map<String, Object> messageMap = new LinkedHashMap<>();
        messageMap.put("text",message);
		messageMap.put("role", SessionMessage.MESSAGE_TYPE_TRACE_MESSAGE_NAME);

        traceMessage.setMessage(messageMap);
        traceMessage.setStartTime(start);
        traceMessage.setEndTime(System.currentTimeMillis());
        routeAgent.recordTraceMessage(traceMessage);
        FluxSink<ServerEvent> fluxSink = routeAgent.getAgentFluxSink();
        if (fluxSink != null) {
            fluxSink.next(traceServerEvent);
        } else {
            if (logger.isInfoEnabled()) {
                logger.info("agentMessage id :{},agentResult:{}", agent.getAgentId(), traceServerEvent.getData());
            }
        }
    }

 


}
