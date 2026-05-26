package org.frameworkset.spi.ai.mcp.tools.server;
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
import org.frameworkset.spi.ai.model.FunctionCallException;
import org.frameworkset.spi.ai.model.FunctionTool;
import org.frameworkset.spi.ai.model.FunctionToolDefine;
import org.frameworkset.spi.ai.tool.BaseBeanToolFunctionCall;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Map;

/**
 * 工具方法
 * @author biaoping.yin
 * @Date 2026/5/20
 */
public class MCPBeanToolFunctionCall extends BaseBeanToolFunctionCall<List<Map>> implements FunctionCall<List<Map>> {
 

    public MCPBeanToolFunctionCall(Method toolMethod, Object toolBean,
                                   FunctionToolDefine functionToolDefine,
                                   Parameter[] parameters){
        super(toolMethod,toolBean,functionToolDefine,parameters);
    }
 
    @Override
    public List<Map> call(FunctionTool functionTool) throws FunctionCallException {
        try {
            return (List<Map>)toolMethod.invoke(toolBean,getArgs(  functionTool));
        } catch (IllegalAccessException e) {
            throw new FunctionCallException(e);
        } catch (InvocationTargetException e) {
            throw new FunctionCallException(e);
        }
    }
}
