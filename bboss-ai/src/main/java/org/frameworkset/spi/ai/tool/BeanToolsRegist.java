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

import org.frameworkset.spi.ai.model.FunctionToolDefine;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * @author biaoping.yin
 * @Date 2026/5/21
 */
public class BeanToolsRegist extends BaseBeanToolsRegist {
  
    public BeanToolsRegist(Object toolBean){
        super(toolBean);
    }

    protected BaseBeanToolFunctionCall buildBeanToolFunctionCall(Method toolMethod, Object toolBean, FunctionToolDefine functionToolDefine, Parameter[] parameters){
        return _buildBeanToolFunctionCall(  toolMethod,   toolBean,   functionToolDefine,  parameters);
    }


    public static BaseBeanToolFunctionCall _buildBeanToolFunctionCall(Method toolMethod, Object toolBean, FunctionToolDefine functionToolDefine, Parameter[] parameters){
        return new BeanToolFunctionCall(toolMethod,toolBean,functionToolDefine,parameters);
    }
    

 
}
