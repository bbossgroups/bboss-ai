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
import org.frameworkset.spi.ai.callback.ChatContext;
import org.frameworkset.spi.ai.callback.ChatStreamCallback;
import org.frameworkset.spi.ai.flow.util.AIFlowUtil;
import org.frameworkset.spi.ai.model.*;
import org.frameworkset.spi.ai.prompt.FlowPromptEval;
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
public class AIRouterNodeBuilder extends AIBaseNodeBuilder {
    private static Logger logger = LoggerFactory.getLogger(AIRouterNodeBuilder.class);
    private String prompt2 = "# 用户问题：#[input.query]\n# 根据用户问题，从以下智能体列表中选择一个最合适的智能体记录\n#[route.ChoiceList,scope=node]\n# 输出要求：\r" +
            "1.如果匹配到智能体将匹配的智能体JSON串包含在```json和```中输出，将其他文字都去除\r" +
            "2.如果未匹配到智能体，请返回空内容\r"
            ;
            ;
//    // 新增：极简提示词，直接要求纯 JSON
//    private String promptMinimal = "根据用户问题选择最合适的智能体。\n\n" +
//            "用户问题：${prompt}\n\n" +
//            "候选智能体：${routeChoiceList}\n\n" +
//            "直接输出JSON，不要任何其他文字：";

    private String prompt = prompt2;
//    private String prompt1 = "# 用户问题：${prompt}\r# 根据用户问题，从以下智能体列表中选择一个最合适的智能体记录\r${routeChoiceList}\r# 输出要求：\r" +
//            "只返回匹配的智能体JSON串，将其他文字都去除"
//            ;

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
        jobFlowNodeExecuteContext.addContextData("route.ChoiceList", JsonUtil.object2json(routeChoiceList));
        routeAgent.setPrompt(prompt);
        ChatContext chatContext = new ChatContext();
        chatContext.setChatStreamCallback(new ChatStreamCallback() {
            /**
             * 提示词预处理
             *
             * @param prompt
             * @return
             */
            @Override
            public String evalPrompt(String prompt) {
                FlowPromptEval flowPromptEval = new FlowPromptEval();
                return flowPromptEval.eval(prompt, jobFlowNodeExecuteContext);
            }

            @Override
            public void streamDone(ServerEvent serverEvent) {
                AIFlowUtil.outputResult( agent, serverEvent,  jobFlowNodeExecuteContext);
            }
        });
        int retry = 0;
        do {
            long start = System.currentTimeMillis();
            ServerEvent serverEvent = routeAgent.chat((ChatAgentMessage) agentMessage, chatContext);
            if (serverEvent != null) {
                
                JobFlowNodeExecuteContext containerJobFlowNodeExecuteContext = jobFlowNodeExecuteContext.getContainerJobFlowNodeExecuteContext();
                JobFlowExecuteContext jobFlowExecuteContext = jobFlowNodeExecuteContext.getContainerJobFlowExecuteContext();

                String data = serverEvent.getData();
                FluxSink<ServerEvent> fluxSink = routeAgent.getAgentFluxSink();
               
                RouteChoice result = null;
                if (data != null) {
                    MarkdownJsonExtractor extractor = new MarkdownJsonExtractor();
                    List<String> jsonList = extractor.extractAll(data);
                    if (jsonList != null && jsonList.size() > 0) {
                        String json = jsonList.get(jsonList.size() - 1);
                        try {
                            result = JsonUtil.json2Object(json, RouteChoice.class);
                        }
                        catch (Exception e){
                            try {
                                List<RouteChoice> results = JsonUtil.json2ListObject(json, RouteChoice.class);
                                if (results != null && results.size() > 1) {
                                    throw new AIRuntimeException("route choice json size > 1:" + json + ". please check your prompt and route choice list");
                                }
                                result = results.get(0);
                            }
                            catch (Exception e1){
                                if (logger.isInfoEnabled()) {
                                    logger.info("agent id :{},json:{}", agent.getAgentId(), json);
                                }
                            }
                            
                        }
                        if(result != null) {
                            if (containerJobFlowNodeExecuteContext != null) {
                                containerJobFlowNodeExecuteContext.addContextData("routeChoice", result.getAgentId());
                            } else {
                                jobFlowExecuteContext.addContextData("routeChoice", result.getAgentId());
                            }
                            if (fluxSink != null) {
                                fluxSink.next(serverEvent);
                            } else {
                                if (logger.isInfoEnabled()) {
                                    logger.info("agent id :{},agentResult:{}", agent.getAgentId(), serverEvent.getData());
                                }
                            }
                        }
                        break;
                    }
                    else{
                        ServerEvent traceServerEvent = new ServerEvent();
                        String message = null;
                        if(retry > 0)
                            message = ("重试" + retry + "次，未匹配到智能体："+data);
                        else{
                            message =  ("未匹配到智能体："+data);
                        }
                        traceServerEvent.setData(message);
                        traceServerEvent.setType(TYPE_TRACE);
                        TraceMessage traceMessage = new TraceMessage();
                        Map<String, Object> messageMap = new LinkedHashMap<>();
                        messageMap.put("text",message);
                        messageMap.put("data",data);
						
						messageMap.put("role", SessionMessage.MESSAGE_TYPE_TRACE_MESSAGE_NAME);
                        traceMessage.setMessage(messageMap);
                        traceMessage.setStartTime(start);
                        traceMessage.setEndTime(System.currentTimeMillis());
                        routeAgent.recordTraceMessage(traceMessage);
                        if (fluxSink != null) {
                            fluxSink.next(traceServerEvent);
                        } else {
                            if (logger.isInfoEnabled()) {
                                logger.info("agentMessage id :{},agentResult:{}", agent.getAgentId(), traceServerEvent.getData());
                            }
                        }
                         
                    }
                }
                retry ++;
            }
        } while (retry < routeAgent.getRetryTimes());
       
        
        return null;
    }
     
 


}
