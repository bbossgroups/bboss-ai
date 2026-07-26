package org.frameworkset.spi.ai.tools;
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

import com.frameworkset.util.JsonUtil;
import com.frameworkset.util.SimpleStringUtil;
import org.frameworkset.spi.ai.audit.AuditContext;
import org.frameworkset.spi.ai.audit.AuditResult;
import org.frameworkset.spi.ai.audit.Auditor;
import org.frameworkset.spi.ai.tool.AgentTraceHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 审计工具类,所有需要审核的工具方法，都需可以继承该类
 * @author biaoping.yin
 * @Date 2026/7/26
 */
public abstract class BaseAuditorTool<T extends BaseAuditorTool> {
	private static final Logger logger = LoggerFactory.getLogger(BaseAuditorTool.class);
	/**
	 * 审计工具接口
	 */
	protected Auditor auditor;
	
	public BaseAuditorTool(Auditor auditor){
		this.auditor = auditor;
	}
	public BaseAuditorTool(){
		
	}
	
	public T setAuditor(Auditor auditor) {
		this.auditor = auditor;
		return (T)this;
	}
	
	/**
	 * 工具审核方法:对工具方法进行稽核，如果稽核不通过，需要通过nextAction指定下一步智能体和大模型操作
	 * @param toolName
	 * @param content
	 * @return
	 */
	protected Map<String, Object> audit(String toolName,String content){
		if(auditor == null)
			return null;
		AuditContext auditContext = new AuditContext();
		auditContext.setChatObject(AgentTraceHolder.getChatObject());
		auditContext.setContent(content);
		auditContext.setToolName(toolName);
		AuditResult auditResult = auditor.audit(auditContext);
		if(!auditResult.isSuccess()){
			if(logger.isWarnEnabled()) {
				logger.warn("hitlTaskTool: audit toolName {},audit content {} failed for reason: {}", toolName,content.length() > 300 ? content.substring(0, 300) + "..." : content, auditResult.getMessage());
			}
			Map<String, Object> result = new HashMap<>();
		 
			result.put("auditResult", "rejected");
			result.put("message",  "audit  toolName " + toolName +  ",failed for reason: " + auditResult.getMessage());
			// 添加nextAction
			if(auditResult.getNextAction() != null)
				result.put("nextAction", auditResult.getNextAction());
			return result;
		 
		}
		return null;
	}
	
	/**
	 * 工具审核方法
	 * @param toolName
	 * @return
	 */
	protected Map<String, Object> audit(String toolName){		 
		return audit(  toolName,(String)null);
	}
	
	protected Map<String, Object> audit(String toolName,Map toolInfo){
		if(auditor == null)
			return null;
		AuditContext auditContext = new AuditContext();
		auditContext.setChatObject(AgentTraceHolder.getChatObject());
		auditContext.setToolInfo(toolInfo);
		auditContext.setToolName(toolName);
		AuditResult auditResult = auditor.audit(auditContext);
		if(!auditResult.isSuccess()){
			if(logger.isWarnEnabled()) {				
				logger.warn("hitlTaskTool: audit toolName {} failed for reason: {}", toolName, auditResult.getMessage());
			}
			Map<String, Object> result = new HashMap<>();
			
			result.put("auditResult", "rejected");
			result.put("message",  "audit  toolName " + toolName +  ",failed for reason: " + auditResult.getMessage());
			// 添加nextAction
			if(auditResult.getNextAction() != null)
				result.put("nextAction", auditResult.getNextAction());
			return result;
			
		}
		return null;
	}
	
}
