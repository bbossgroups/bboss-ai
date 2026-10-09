package org.frameworkset.spi.ai.model.memory;

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
import java.time.LocalDateTime;
/**
 *
 * @author biaoping.yin
 * @Date 2026/10/9
 
 * 智能体记忆（本地）
 * 对应表：agent_memory_local
 */
public class AgentMemory {
	
	/**
	 * 记录id，主键
	 */
	private String memoryId;
	
	/**
	 * 创建时间
	 */
	private LocalDateTime createTime;
	
	/**
	 * 更新时间
	 */
	private LocalDateTime updateTime;
	
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
	 * 总账记忆内容，一个智能体+用户一条记录
	 */
	private String content;
	
	public AgentMemory() {
	}
	
	public String getMemoryId() {
		return memoryId;
	}
	
	public void setMemoryId(String memoryId) {
		this.memoryId = memoryId;
	}
	
	public LocalDateTime getCreateTime() {
		return createTime;
	}
	
	public void setCreateTime(LocalDateTime createTime) {
		this.createTime = createTime;
	}
	
	public LocalDateTime getUpdateTime() {
		return updateTime;
	}
	
	public void setUpdateTime(LocalDateTime updateTime) {
		this.updateTime = updateTime;
	}
	
	public String getAgentId() {
		return agentId;
	}
	
	public void setAgentId(String agentId) {
		this.agentId = agentId;
	}
	
	public String getParentAgentId() {
		return parentAgentId;
	}
	
	public void setParentAgentId(String parentAgentId) {
		this.parentAgentId = parentAgentId;
	}
	
	public String getUserId() {
		return userId;
	}
	
	public void setUserId(String userId) {
		this.userId = userId;
	}
	
	public String getSessionId() {
		return sessionId;
	}
	
	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}
	
	public String getContent() {
		return content;
	}
	
	public void setContent(String content) {
		this.content = content;
	}
	
	@Override
	public String toString() {
		return "AgentMemoryLocal{" +
				"memoryId='" + memoryId + '\'' +
				", createTime=" + createTime +
				", updateTime=" + updateTime +
				", agentId='" + agentId + '\'' +
				", parentAgentId='" + parentAgentId + '\'' +
				", userId='" + userId + '\'' +
				", sessionId='" + sessionId + '\'' +
				", content='" + content + '\'' +
				'}';
	}
}
