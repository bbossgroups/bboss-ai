package org.frameworkset.spi.ai.tool;
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

import org.frameworkset.spi.ai.model.FunctionCall;
import org.frameworkset.spi.ai.model.FunctionToolDefine;
import org.frameworkset.spi.ai.tools.ToolsRegist;

import java.util.List;

/**
 * @author biaoping.yin
 * @Date 2026/5/21
 */
public class BeanToolsRegist implements ToolsRegist {
    private Object toolBean;
    private List<FunctionToolDefine> functionToolDefines;
    public BeanToolsRegist(Object toolBean){
        this.toolBean = toolBean;
    }
    
    @Override
    public void init(){
        functionToolDefines = BeanToolHandle.parserTools(toolBean);
    }

    @Override
    public List<FunctionToolDefine> registTools() {
        return functionToolDefines;
    }

    /**
     * beanTools无需实现本方法，
     * @param functionName
     * @return
     */
    @Override
    public FunctionCall getFunctionCall(String functionName) {
        return null;
    }
}
