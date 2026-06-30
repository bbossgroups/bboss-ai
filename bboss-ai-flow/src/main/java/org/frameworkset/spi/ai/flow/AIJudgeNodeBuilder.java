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
import org.frameworkset.spi.ai.callback.ChatContext;
import org.frameworkset.spi.ai.callback.ChatStreamCallback;
import org.frameworkset.spi.ai.flow.util.AIFlowUtil;
import org.frameworkset.spi.ai.model.*;
import org.frameworkset.spi.ai.prompt.FlowPromptEval;
import org.frameworkset.tran.jobflow.context.JobFlowNodeExecuteContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.FluxSink;

/**
 * @author biaoping.yin
 * @Date 2026/4/14
 */
public class AIJudgeNodeBuilder extends AIBaseNodeBuilder {
    private static Logger logger = LoggerFactory.getLogger(AIJudgeNodeBuilder.class);
    private AIJudgeAgent judgeAgent;
    public AIJudgeNodeBuilder(AIJudgeAgent judgeAgent) {
        super( judgeAgent);
        this.judgeAgent = judgeAgent;
       
    }
    public AIJudgeNodeBuilder(String nodeName) {
        super(nodeName);
    }

    public AIJudgeNodeBuilder(String nodeId, String nodeName) {
        super(nodeId, nodeName);
    }


    private String judgePrompt = "评估结果是否回答了问题:\n#[input.query,scope=node]\n# 问题答案：\n#[answer,scope=node],回答请回复：是，否则回复：否";


    @Override
    public Object call(JobFlowNodeExecuteContext jobFlowNodeExecuteContext) throws Exception {

        String judgePrompt = judgeAgent.getPrompt();
        if(judgePrompt == null){
            judgePrompt = this.judgePrompt;
        }
        
        String userPrompt = judgeAgent.getParentAgent().getFirstSubAgentPrompt();
        String input = (String) jobFlowNodeExecuteContext.getJobFlowContextData("input.query");
        if(SimpleStringUtil.isNotEmpty(input)){
            jobFlowNodeExecuteContext.addContextData("input.query", input);
            
        }
        else{
            jobFlowNodeExecuteContext.addContextData("input.query", userPrompt);
        }
         
        
//        boolean hasAnswerVar = false;
        LastSessionMessage lastSessionMessage = judgeAgent.getParentAgent().getLastSessionMessage() ;
        if(lastSessionMessage != null && lastSessionMessage.getData() != null){
            jobFlowNodeExecuteContext.addContextData("answer", lastSessionMessage.getData());
        }
//        if(judgePrompt.indexOf("${answer}") > 0){
//            judgePrompt = SimpleStringUtil.replace(judgePrompt, "${answer}", lastSessionMessage.getData());
//            hasAnswerVar = true;
//        }
//        StringBuilder prompt = new StringBuilder();
//        if(!hasPromptVar){
//            
//            prompt.append(judgePrompt);
//            prompt.append("\r");
//            prompt.append("# 用户问题：\r").append(judgeAgent.getParentAgent().getFirstSubAgentPrompt());
//        }
//        
//        if(!hasAnswerVar){
//            if(prompt.length() == 0){
//                prompt.append(judgePrompt);
//            }
//            prompt.append("\r");
//
//            if(lastSessionMessage != null)
//                prompt.append("# 问题答案：\r").append(lastSessionMessage.getData());
//            else{
//                prompt.append("# 问题答案：\r");
//            }
//        }
        
//        if(prompt.length() > 0){
//            judgePrompt = prompt.toString();
//        }

        judgeAgent.setPrompt(judgePrompt);
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
        JobFlowNodeExecuteContext containerJobFlowNodeExecuteContext = jobFlowNodeExecuteContext.getContainerJobFlowNodeExecuteContext();
        AgentMessage agentMessage = judgeAgent.getAgentMessage() != null ? judgeAgent.getAgentMessage() : planAgent.getAgentMessage();
        if(agentMessage == null){
            throw new AIRuntimeException("agentMessage is null");
        }
        ServerEvent serverEvent = judgeAgent.chat((ChatAgentMessage)agentMessage,chatContext);
        
        if(serverEvent != null){
            FluxSink<ServerEvent> fluxSink = judgeAgent.getAgentFluxSink();
            if(fluxSink != null){
                fluxSink.next(serverEvent);
            }
            else {
                if(logger.isInfoEnabled()) {
                    logger.info("{} judge result:{}", judgeAgent.getAgentId(), serverEvent.getData());
                }
            }
           String varName = judgeAgent.getOutputVaribleName();
            
            if(SimpleStringUtil.isEmpty(varName )){
                varName = judgeAgent.getAgentId()+".judgeResult";
                int varScope = AIFlowConst.AIFLOW_VAR_SCOPE_FLOW;
                if(containerJobFlowNodeExecuteContext != null){
                    varScope = AIFlowConst.AIFLOW_VAR_SCOPE_CONTAINER;
                }
                else{
                    varScope = AIFlowConst.AIFLOW_VAR_SCOPE_FLOW;
                }
                judgeAgent.setOutputVaribleName(varName,varScope);
                
            }
            AIFlowUtil.outputResult( judgeAgent, serverEvent,  jobFlowNodeExecuteContext);
        }
        return null;
    }




}
