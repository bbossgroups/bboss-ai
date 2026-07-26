package org.frameworkset.spi.ai.audit;
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

import org.frameworkset.spi.ai.AIAgent;
import org.frameworkset.spi.ai.model.ChatObject;

import java.util.Map;

/**
 * 审计上下文：包含审计内容、审计代理和工具信息，用于审计工具的参数传递
 * @author biaoping.yin
 * @Date 2026/7/26
 */
public class AuditContext {
	private String content;
	private ChatObject chatObject;
	private Map toolInfo;
	private String toolName;
	public String getToolName() {
		return toolName;
	}
	public void setToolName(String toolName) {
		this.toolName = toolName;
	}
	
	public String getContent() {
		return content;
	}
	
	public void setContent(String content) {
		this.content = content;
	}
	
	public AIAgent getAgent() {
		return chatObject.getAgent();
	}
	
	public void setChatObject(ChatObject chatObject) {
		this.chatObject = chatObject;
	}
	
	public Map getToolInfo() {
		return toolInfo;
	}
	
	public void setToolInfo(Map toolInfo) {
		this.toolInfo = toolInfo;
	}
}
