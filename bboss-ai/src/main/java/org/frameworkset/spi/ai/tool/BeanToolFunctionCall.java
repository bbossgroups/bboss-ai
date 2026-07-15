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

import com.frameworkset.util.SimpleStringUtil;
import org.frameworkset.spi.ai.model.*;
import org.frameworkset.spi.ai.store.SessionMessage;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

/**
 * 工具方法
 * @author biaoping.yin
 * @Date 2026/5/20
 */
public class BeanToolFunctionCall extends  BaseBeanToolFunctionCall implements FunctionCall {
 

    public BeanToolFunctionCall(Method toolMethod, Object toolBean, 
                                FunctionToolDefine functionToolDefine,
                                Parameter[] parameters){
         super(toolMethod,toolBean,functionToolDefine,parameters);
    }
 
    @Override
    public Object call(FunctionTool functionTool) throws FunctionCallException {
        TraceMessage traceMessage = null;
        try {
           
            if(AgentTraceHolder.isToolTrace()) {
                traceMessage = new TraceMessage();
                traceMessage.setStartTime(System.currentTimeMillis())
						.put("toolName",functionTool.getFunctionName())
						.put("id",functionTool.getId())
						.put("type",functionTool.getType())
						.put("index",functionTool.getIndex())
                        .put("toolCallArgs", !isEmptyParameters() ? functionTool.getArguments() : null)
                        .put("role", SessionMessage.MESSAGE_TYPE_TOOLCALL_MESSAGE_NAME);
            }
            Object result = toolMethod.invoke(toolBean,getArgs(  functionTool));
            if(AgentTraceHolder.isToolTrace()) {
                traceMessage.setEndTime(System.currentTimeMillis())
                        .put("toolCallResponse", result);
              
                AgentTraceHolder.trace(traceMessage);
            }
            return result;
        } 
		catch (FunctionCallException e) {
			if(AgentTraceHolder.isToolTrace() && traceMessage != null) {
				try {
					traceMessage.setEndTime(System.currentTimeMillis())
							.put("toolCallException", SimpleStringUtil.exceptionToString(e));
					
					AgentTraceHolder.trace(traceMessage);
				} catch (Exception te) {
					
				}
			}
			throw  e;
		}
		catch (Exception e) {
			if(AgentTraceHolder.isToolTrace() && traceMessage != null) {
				try {
					traceMessage.setEndTime(System.currentTimeMillis())
							.put("toolCallException", SimpleStringUtil.exceptionToString(e));
					
					AgentTraceHolder.trace(traceMessage);
				} catch (Exception te) {
					
				}
			}
			throw new FunctionCallException(e);
		}
    }
}
