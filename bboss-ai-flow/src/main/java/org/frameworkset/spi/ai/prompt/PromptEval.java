package org.frameworkset.spi.ai.prompt;
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

import com.frameworkset.util.VariableHandler;
import org.frameworkset.spi.ai.model.AIFlowConst;
import org.frameworkset.spi.ai.model.AIRuntimeException;
import org.frameworkset.tran.jobflow.context.JobFlowNodeExecuteContext;
import org.slf4j.Logger;

import java.util.List;

/**
 * @author biaoping.yin
 * @Date 2026/5/12
 */
public class PromptEval {
    private static Logger logger = org.slf4j.LoggerFactory.getLogger(PromptEval.class);
    private static String pretoken = "#\\[";
    private static String endtoken = "\\]";
    
    static class PromptVariable extends VariableHandler.Variable {

        private int scope = AIFlowConst.AIFLOW_VAR_SCOPE_FLOW;

        public int getScope() {
            return scope;
        }

        @Override
        /**
         * 变量属性解析完毕后，对变量属性信息进行额外处理
         */
        public void afterSetAttribute(){
            if(this.attributes != null) {
//				int pos = this.attributes.indexOf(",");
                String[] ts = attributes.split(",");

                for (int i = 0; i < ts.length; i ++) {
                    String t = ts[i];
                    if (t.startsWith("scope=")) {
                        String q = t.substring("scope=".length()).trim();
                        if(q.equals("node"))
                            scope = AIFlowConst.AIFLOW_VAR_SCOPE_NODE;
                        else if(q.equals("flow"))
                            scope = AIFlowConst.AIFLOW_VAR_SCOPE_FLOW;
                        else if(q.equals("container"))
                            scope = AIFlowConst.AIFLOW_VAR_SCOPE_CONTAINER;
                        else{
                            throw new AIRuntimeException("scope must be node,flow or container:"+q+" in variable:"+this.getVariableName());
                        }
                    }
                  

                }
 

            }
        }
    }
    static class PromptStructionBuiler extends VariableHandler.URLStructionBuiler {
        @Override
        public VariableHandler.Variable buildVariable() {
            return new PromptVariable();
        }

    }
    public String eval(String prompt, JobFlowNodeExecuteContext jobFlowNodeExecuteContext){
        
        VariableHandler.URLStruction a = VariableHandler.parserStruction(prompt,new PromptStructionBuiler());
        StringBuilder newPrompt = new StringBuilder();
        if(a != null){
           
            List<VariableHandler.Variable> variables = a.getVariables();
            List<String> tokens = a.getTokens();
            for (int k = 0; tokens != null && k < tokens.size(); k++) {
                newPrompt.append(tokens.get(k));
                if(variables != null && k < variables.size()){
                    PromptVariable variable = (PromptVariable) variables.get(k);
                    int scope = variable.getScope();
                    Object value = null;
                    if(scope == AIFlowConst.AIFLOW_VAR_SCOPE_FLOW){
                         value = jobFlowNodeExecuteContext.getJobFlowContextData(variable.getVariableName());                        
                    }
                    else if(scope == AIFlowConst.AIFLOW_VAR_SCOPE_CONTAINER){
                         value = jobFlowNodeExecuteContext.getContainerJobFlowNodeContextData(variable.getVariableName());                         
                    }
                    else if(scope == AIFlowConst.AIFLOW_VAR_SCOPE_NODE){
                        value = jobFlowNodeExecuteContext.getContextData(variable.getVariableName());
                    }
                    if(value != null){
                        newPrompt.append(value);
                    }
                    else{
                        if(variable.getAttributes() != null) {
                            newPrompt.append("#[").append(variable.getVariableName()).append(",").append(variable.getAttributes()).append("]");
                        }
                        else{
                            newPrompt.append("#[").append(variable.getVariableName()).append("]");
                        }
                    }
                }                
            }         
 
        }
        if(newPrompt.length() > 0){
            prompt = newPrompt.toString();
        }
        logger.info("new prompt:{}",prompt);
        return prompt;
       
    }
}
