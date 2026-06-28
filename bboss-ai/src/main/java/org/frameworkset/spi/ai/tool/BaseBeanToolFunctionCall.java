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
import org.frameworkset.spi.ai.model.FunctionTool;
import org.frameworkset.spi.ai.model.FunctionToolDefine;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;

/**
 * 工具方法
 * @author biaoping.yin
 * @Date 2026/5/20
 */
public abstract class BaseBeanToolFunctionCall<T> implements FunctionCall<T> {
    protected Method toolMethod;
    protected Object toolBean;
    protected FunctionToolDefine functionToolDefine;
    protected Parameter[] parameters;
    protected boolean emptyParameters;

    public BaseBeanToolFunctionCall(Method toolMethod, Object toolBean,
                                    FunctionToolDefine functionToolDefine,
                                    Parameter[] parameters){
        this.toolMethod = toolMethod;
        this.toolBean = toolBean;
        this.functionToolDefine = functionToolDefine;
        this.parameters = parameters;
        this.emptyParameters = parameters == null || parameters.length == 0;
    }

    public boolean isEmptyParameters() {
        return emptyParameters;
    }

    protected Object[] getArgs(FunctionTool functionTool){
        if(emptyParameters){
            return null;
        }
        Map<String, Object> arguments = functionTool.getArguments();
        Object[] args = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            String name = parameter.getName();
            if(arguments.containsKey(name)) {
                args[i] = arguments.get(name);
            }
            else{
                args[i] = null;
            }
        }
        return args;
    }
    
}
