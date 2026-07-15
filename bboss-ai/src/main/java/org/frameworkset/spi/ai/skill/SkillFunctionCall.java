package org.frameworkset.spi.ai.skill;
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
import org.frameworkset.spi.ai.model.FunctionCall;
import org.frameworkset.spi.ai.model.FunctionCallException;
import org.frameworkset.spi.ai.model.FunctionTool;
import org.frameworkset.spi.ai.model.TraceMessage;
import org.frameworkset.spi.ai.store.SessionMessage;
import org.frameworkset.spi.ai.tool.AgentTraceHolder;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 *
 * @author biaoping.yin
 * @Date 2026/7/15
 */
public class SkillFunctionCall implements FunctionCall {
	private Map<String,Skill> skillsMap;
	public SkillFunctionCall(Map<String, Skill> skillsMap) {
		this.skillsMap = skillsMap;
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
						.put("toolCallArgs",   functionTool.getArguments()  )
						.put("role", SessionMessage.MESSAGE_TYPE_TOOLCALL_MESSAGE_NAME);
			}
			Map<String,Object> arguments = functionTool.getArguments();
			String skillName = (String)arguments.get("skillName");
			if(skillName == null || skillName.length() == 0){
				throw new FunctionCallException("skillName is null");
			}
			Skill skill = this.skillsMap.get(skillName);
			StringBuilder builder = new StringBuilder();
			if (skill != null) {
				
				builder.append("Base directory for this skill: ");
				builder.append(skill.getBasePath()).append("\n\n");
				builder.append(skill.getContent());
//			return "Base directory for this skill: %s\n\n%s".formatted(skill.basePath(), skill.content());
				
			}
			else{
				builder.append("Skill not found: " ).append(skillName);
			}
			String result = builder.toString();
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
