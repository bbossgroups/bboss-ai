package org.frameworkset.spi.ai.model;
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

/**
 *
 * @author biaoping.yin
 * @Date 2026/8/31
 */
public class Memory {
	/**
	 * 智能体每天流水账
	 */
	public static final String MEMORY_TYPE_DAY = "day";
	/**
	 * 智能体长期记忆
	 */
	public static final String MEMORY_TYPE_LONGTERM = "longterm";
	/**
	 * 记录id，主键
	 */
	private String memoryId;
	/**
	 * 智能体id
	 */
	private String agentId;
	/**
	 * 父智能体id
	 */
	private String parentAgentId;
	/**
	 * 用户id
	 */
	private String userId;
	/**
	 * 会话id
	 */
	private String sessionId;
	/**
	 * 记忆内容
	 */
	private String content;
	/**
	 * 记忆时间:memoryType为day时，memoryDay格式为yyyy-MM-dd
	 */
	private String memoryDay;
	/**
	 * day,longterm
	 */
	private String memoryType = "day";
	/**
	 * 获取记录id，主键
	 * @return the memoryId
	 */
	public String getMemoryId() {
		return memoryId;
	}
	/**
	 * 设置记录id，主键
	 * @param memoryId the memoryId to set
	 */
	public void setMemoryId(String memoryId) {
		this.memoryId = memoryId;
	}
	/**
	 * 获取智能体id
	 * @return the agentId
	 */
	public String getAgentId() {
		return agentId;
	}
	/**
	 * 设置智能体id
	 * @param agentId the agentId to set
	 */
	public void setAgentId(String agentId) {
		this.agentId = agentId;
	}
	/**
	 * 获取父智能体id
	 * @return the parentAgentId
	 */
	public String getParentAgentId() {
		return parentAgentId;
	}
	/**
	 * 设置父智能体id
	 * @param parentAgentId the parentAgentId to set
	 */
	public void setParentAgentId(String parentAgentId) {
		this.parentAgentId = parentAgentId;
	}
	/**
	 * 获取用户id
	 * @return the userId
	 */
	public String getUserId() {
		return userId;
	}
	/**
	 * 设置用户id
	 * @param userId the userId to set
	 */
	public void setUserId(String userId) {
		this.userId = userId;
	}
	/**
	 * 获取会话id
	 * @return the sessionId
	 */
	public String getSessionId() {
		return sessionId;
	}
	/**
	 * 设置会话id
	 * @param sessionId the sessionId to set
	 */
	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}
	/**
	 * 获取记忆内容
	 * @return the content
	 */
	public String getContent() {
		return content;
	}
	/**
	 * 设置记忆内容
	 * @param content the content to set
	 */
	public void setContent(String content) {
		this.content = content;
	}
	/**
	 * 获取记忆时间:memoryType为day时，memoryDay格式为yyyy-MM-dd
	 * @return the memoryDay
	 */
	public String getMemoryDay() {
		return memoryDay;
	}
	/**
	 * 设置记忆时间:memoryType为day时，memoryDay格式为yyyy-MM-dd
	 * @param memoryDay the memoryDay to set
	 */
	public void setMemoryDay(String memoryDay) {
		this.memoryDay = memoryDay;
	}
	/**
	 * 获取day,longterm
	 * @return the memoryType
	 */
	public String getMemoryType() {
		return memoryType;
	}
	/**
	 * 设置day,longterm
	 * @param memoryType the memoryType to set
	 */
	public void setMemoryType(String memoryType) {
		this.memoryType = memoryType;
	}
}
