package org.frameworkset.spi.ai.model.memory;
import java.time.LocalDateTime;

/**
 * 智能体日记忆（本地）
 * 对应表：agent_day_memory_local
 */
public class AgentDayMemory {
	
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
	 * 记忆内容，一个智能体+用户一天一条记录
	 */
	private String content;
	
	/**
	 * 记忆时间 yyyy-MM-dd
	 */
	private String memoryDay;
	
	public AgentDayMemory() {
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
	
	public String getMemoryDay() {
		return memoryDay;
	}
	
	public void setMemoryDay(String memoryDay) {
		this.memoryDay = memoryDay;
	}
	
	@Override
	public String toString() {
		return "AgentDayMemoryLocal{" +
				"memoryId='" + memoryId + '\'' +
				", createTime=" + createTime +
				", updateTime=" + updateTime +
				", agentId='" + agentId + '\'' +
				", parentAgentId='" + parentAgentId + '\'' +
				", userId='" + userId + '\'' +
				", sessionId='" + sessionId + '\'' +
				", content='" + content + '\'' +
				", memoryDay='" + memoryDay + '\'' +
				'}';
	}
}