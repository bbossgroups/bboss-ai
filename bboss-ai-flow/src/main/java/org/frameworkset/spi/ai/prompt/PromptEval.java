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

import com.frameworkset.util.FileUtil;
import com.frameworkset.util.VariableHandler;
import org.frameworkset.spi.ai.model.AIFlowConst;
import org.frameworkset.spi.ai.model.AIRuntimeException;
import org.frameworkset.spi.ai.util.ClasspathResourceReader;
import org.frameworkset.tran.jobflow.context.JobFlowNodeExecuteContext;
import org.frameworkset.util.io.ClassPathResource;
import org.slf4j.Logger;

import java.io.IOException;
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
        private String type = AIFlowConst.AIFLOW_VAR_TYPE_TEXT;
        private Object cacheValue = null;
        private Object lock = new Object();
        /**
         * 变量值字符集，当type为file、url、resource时起作用
         */
        private String charset = "UTF-8"; // Default character set

        public int getScope() {
            return scope;
        }

        public String getCharset() {
            return charset;
        }

        public String getType() {
            return type;
        }

        public Object getLock() {
            return lock;
        }

        public void setCacheValue(Object cacheValue) {
            this.cacheValue = cacheValue;
        }

        public Object getCacheValue() {
            return cacheValue;
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
                    else if (t.startsWith("type=")) {
                        String q = t.substring("type=".length()).trim();
                        if(q.equals("text"))
                            type = AIFlowConst.AIFLOW_VAR_TYPE_TEXT;
                        else if(q.equals("file")){
                            type = AIFlowConst.AIFLOW_VAR_TYPE_FILE;
                           
                        } else if(q.equals("url")){
                            type = AIFlowConst.AIFLOW_VAR_TYPE_URL;
                        } else if(q.equals("resource")){
                            type = AIFlowConst.AIFLOW_VAR_TYPE_RESOURCE;
                        }
                        else{
                            throw new AIRuntimeException("type must be text,file or url:"+q+" in variable:"+this.getVariableName());
                        }
                    
                    }
                    else if (t.startsWith("charset=")) {
                        this.charset = t.substring("charset=".length()).trim();
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
                    String type = variable.getType();
                    Object value = null;
                    if(type == null || type.equals(AIFlowConst.AIFLOW_VAR_TYPE_TEXT)) {
                        int scope = variable.getScope();
                       
                        if (scope == AIFlowConst.AIFLOW_VAR_SCOPE_FLOW) {
                            value = jobFlowNodeExecuteContext.getJobFlowContextData(variable.getVariableName());
                        } else if (scope == AIFlowConst.AIFLOW_VAR_SCOPE_CONTAINER) {
                            value = jobFlowNodeExecuteContext.getContainerJobFlowNodeContextData(variable.getVariableName());
                        } else if (scope == AIFlowConst.AIFLOW_VAR_SCOPE_NODE) {
                            value = jobFlowNodeExecuteContext.getContextData(variable.getVariableName());
                        }
                    } else if (type.equals(AIFlowConst.AIFLOW_VAR_TYPE_FILE)) {
                        
                        value = PromptResourceCache.getInstance().cacheFileContent(variable.getVariableName(), variable.getCharset());
                            
                        
                    }
                    else if (type.equals(AIFlowConst.AIFLOW_VAR_TYPE_RESOURCE)) {
                        value = PromptResourceCache.getInstance().cacheClasspathResource(variable.getVariableName(), variable.getCharset());
                         
                    }

                    else if (type.equals(AIFlowConst.AIFLOW_VAR_TYPE_URL)) {
                        value = PromptResourceCache.getInstance().cacheUrlResource(variable.getVariableName(), variable.getCharset());

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
        if(logger.isDebugEnabled()) {
            logger.debug("new prompt:{}", prompt);
        }
        return prompt;
       
    }
}
