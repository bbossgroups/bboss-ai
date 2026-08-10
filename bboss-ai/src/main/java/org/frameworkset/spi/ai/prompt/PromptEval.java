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

import com.frameworkset.util.SimpleStringUtil;
import com.frameworkset.util.VariableHandler;
import org.frameworkset.spi.ai.callback.ChatContext;
import org.frameworkset.spi.ai.model.AIFlowConst;
import org.frameworkset.spi.ai.model.AIRuntimeException;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author biaoping.yin
 * @Date 2026/5/12
 */
public class PromptEval {
    private static Logger logger = org.slf4j.LoggerFactory.getLogger(PromptEval.class);
    
 

    /**
     * 递归解析提示词中引用的外部资源包含提示词变量
     * @param evaledResources
     * @param prompt
     * @param chatContext
     * @return
     */
    private String evalResource(Map<String,Object> evaledResources, String prompt, ChatContext chatContext){
        VariableHandler.URLStruction a = VariableHandler.parserStruction(prompt,new PromptStructionBuiler());
        StringBuilder newPrompt = new StringBuilder();
        if(a != null){

             
            List<VariableHandler.Variable> variables = a.getVariables();
            List<String> tokens = a.getTokens();
            for (int k = 0; tokens != null && k < tokens.size(); k++) {
                newPrompt.append(tokens.get(k));
                if(variables != null && k < variables.size()){
                    PromptVariable variable = (PromptVariable) variables.get(k);
					boolean cache = variable.isCache();
                    String type = variable.getType();
                   
                    Object value = null;
                    String varName = variable.getVariableName();
                    if(type == null || type.equals(AIFlowConst.AIFLOW_VAR_TYPE_TEXT)) {
                        String defaultValue = variable.getDefaultValue();

                        value = chatContext.getContextData(varName);
                        
                        if(value == null && defaultValue != null){
                            value = defaultValue;
                        }
                    } else if (type.equals(AIFlowConst.AIFLOW_VAR_TYPE_FILE)) {

                        if(evaledResources.containsKey(varName)){
                            throw new AIRuntimeException("外部资源[" + varName + "]存在嵌套引用：不允许嵌套引用外部文件资源！");
                        }
                        String value_ = cache?PromptResourceCache.getInstance().cacheFileContent(varName, variable.getCharset()):
								PromptResourceCache.getInstance().getFileContent(varName, variable.getCharset());
                        evaledResources.put(varName, DUMP);
                        if(SimpleStringUtil.isNotEmpty(value_)) {
                            value_ = this.evalResource(evaledResources, value_, chatContext);
                        }
                        value = value_;

                    }
                    else if (type.equals(AIFlowConst.AIFLOW_VAR_TYPE_RESOURCE)) {
                        if(evaledResources.containsKey(varName)){
                            throw new AIRuntimeException("外部资源[" + varName + "]存在嵌套引用：不允许嵌套引用外部classpath文件资源！");
                        }
                        String value_ = cache?PromptResourceCache.getInstance().cacheClasspathResource(varName, variable.getCharset()):
								PromptResourceCache.getInstance().getClasspathResource(varName, variable.getCharset());
                        evaledResources.put(varName, DUMP);
                        if(SimpleStringUtil.isNotEmpty(value_)) {
                            value_ = this.evalResource(evaledResources, value_, chatContext);
                        }
                        value = value_;

                    }

                    else if (type.equals(AIFlowConst.AIFLOW_VAR_TYPE_URL)) {
                        if(evaledResources.containsKey(varName)){
                            throw new AIRuntimeException("外部资源[" + varName + "]存在嵌套引用：不允许嵌套引用外部url资源！");
                        }
						String value_ =  PromptResourceCache.getInstance().getUrlResource(variable) ;
						
                        evaledResources.put(varName, DUMP);
                        if(SimpleStringUtil.isNotEmpty(value_)) {
                            value_ = this.evalResource(evaledResources, value_, chatContext);
                        }
                        value = value_;

                    }
					
					else if (type.equals(AIFlowConst.AIFLOW_VAR_TYPE_SERVICE)) {
						if(evaledResources.containsKey(varName)){
							throw new AIRuntimeException("外部资源[" + varName + "]存在嵌套引用：不允许嵌套引用外部url资源！");
						}
						String value_ = PromptResourceCache.getInstance().getServiceResource(variable);
						evaledResources.put(varName, DUMP);
						if(SimpleStringUtil.isNotEmpty(value_)) {
							value_ = this.evalResource(evaledResources, value_, chatContext);
						}
						value = value_;
						
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
    protected static final Object DUMP = new Object();
    public String eval(String prompt, ChatContext chatContext){
        
         return evalResource(new HashMap<>(),prompt,chatContext);
       
    }
}
