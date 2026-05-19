package org.frameworkset.spi.ai.flow.util;
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
import org.frameworkset.spi.ai.AIAgent;
import org.frameworkset.spi.ai.model.ServerEvent;
import org.frameworkset.tran.jobflow.context.JobFlowExecuteContext;
import org.frameworkset.tran.jobflow.context.JobFlowNodeExecuteContext;

/**
 * @author biaoping.yin
 * @Date 2026/5/7
 */
public class AIFlowUtil {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AIFlowUtil.class);
    public static void outputResult(AIAgent agent, ServerEvent serverEvent, JobFlowNodeExecuteContext jobFlowNodeExecuteContext){
        if(serverEvent != null) {
            JobFlowNodeExecuteContext containerJobFlowNodeExecuteContext = jobFlowNodeExecuteContext.getContainerJobFlowNodeExecuteContext();
            JobFlowExecuteContext jobFlowExecuteContext = jobFlowNodeExecuteContext.getJobFlowExecuteContext();
            if (logger.isDebugEnabled()) {
                logger.debug("agentMessage id :{},agentResult:{}", agent.getAgentId(), serverEvent.getData());
            }
            String outputVaribleName = agent.getOutputVaribleName();
            String data = serverEvent.getData();
            if(data == null){
                data = serverEvent.getFullStreamData();
            }
            if(data != null) {
                if(agent.getAgentOutput() != null){
                    try {
                        agent.getAgentOutput().output(serverEvent);
                    }
                    catch (Exception e) {
                        logger.error("Output agent[id="+agent.getAgentId()+",name="+agent.getAgentName()+"] result error:",e);
                    }
                }
                if (SimpleStringUtil.isNotEmpty(outputVaribleName)) {


                    if (agent.isFlowOutputVaribleScope()) {
                        jobFlowExecuteContext.addContextData(outputVaribleName, data);
                    } else if (agent.isContainerOutputVaribleScope()) {
                        if (containerJobFlowNodeExecuteContext != null) {
                            containerJobFlowNodeExecuteContext.addContextData(outputVaribleName, data);
                        }
                    } else if (agent.isNodeOutputVaribleScope()) {
                        jobFlowNodeExecuteContext.addContextData(outputVaribleName, data);
                    } else {//默认将变量输出到作业流作用域
                        jobFlowExecuteContext.addContextData(outputVaribleName, data);
                    }
                }
            }
        }
    }

    public static void outputResult(AIAgent agent, String result, JobFlowNodeExecuteContext jobFlowNodeExecuteContext){
        if(result != null) {
            JobFlowNodeExecuteContext containerJobFlowNodeExecuteContext = jobFlowNodeExecuteContext.getContainerJobFlowNodeExecuteContext();
            JobFlowExecuteContext jobFlowExecuteContext = jobFlowNodeExecuteContext.getJobFlowExecuteContext();
            if (logger.isDebugEnabled()) {
                logger.debug("agentMessage id :{},agentResult:{}", agent.getAgentId(), result);
            }
            String outputVaribleName = agent.getOutputVaribleName();

            if(SimpleStringUtil.isNotEmpty(outputVaribleName)) {
                if(agent.isFlowOutputVaribleScope()){
                    jobFlowExecuteContext.addContextData(outputVaribleName, result);
                }
                else if(agent.isContainerOutputVaribleScope()){
                    if (containerJobFlowNodeExecuteContext != null) {
                        containerJobFlowNodeExecuteContext.addContextData(outputVaribleName, result);
                    }
                }
                else if(agent.isNodeOutputVaribleScope()){
                    jobFlowNodeExecuteContext.addContextData(outputVaribleName, result);
                }
                else {//默认将变量输出到作业流作用域
                    jobFlowExecuteContext.addContextData(outputVaribleName, result);
                }
            }
        }
    }
}
