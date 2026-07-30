package org.frameworkset.spi.ai.hitl;
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

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * 人在回路:human in the loop call object
 * 数据库需要定期归档已经结束的人工介入任务，避免数据库表数据量过大
 * @author biaoping.yin
 * @Date 2026/7/16
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HitlCallTask {
	/**
	 * 人工介入任务状态：0 待处理 1 已处理 2 已拒绝 3 超时忽略 4 销毁任务 5 已结束
	 */
	/**
	 * 人工介入任务状态：0 待处理  
	 */
	public static final int TASK_STATUS_UNHANDLED = 0;
	/**
	 * 人工介入任务状态：1 已处理
	 */
	public static final int TASK_STATUS_HANDLED = 1;
	/**
	 * 人工介入任务状态：2 已拒绝
	 */
	public static final int TASK_STATUS_REJECTED = 2;
	/**
	 * 人工介入任务状态：3 超时忽略
	 */
	public static final int TASK_STATUS_TIMEOUT_IGNORED = 3;
	/**
	 * 人工介入任务状态：3 超时忽略
	 */
	public static final int TASK_STATUS_DESTROYED = 4;
	/**
	 * 人工介入任务状态：5 已结束
	 */
	public static final int TASK_STATUS_COMPLETED = 5;
	private String traceId;
	/**
	 * 步骤消息：智能体id
	 */
	private String agentId;
	

	
	private String agentNodeType;
	/**
	 * 步骤消息：智能体名称
	 */
	private String agentName;
	
	/**
	 * 步骤消息：父智能体id
	 */
	private String parentAgentId;
	/**
	 * 步骤消息：父智能体名称
	 */
	private String parentAgentName;
	
	/**
	 * 步骤消息：会话id
	 */
	private String sessionId;
	/**
	 * 步骤消息：请求id
	 */
	private String requestId;
	/**
	 * 步骤消息：用户id
	 */
	private String userId;
	/**
	 * 人工介入任务id，唯一标识，将保存到人工介入任务状态数据库表中，初始状态为待处理
	 * 同时发送到客户端，客户处理完毕后需与用户数据一起回传到服务端，服务端接收到信息后，进行相应处理数据后
	 * 更新人工介入任务状态为已处理
	 * 
	 * 处于中断状态的智能体需要监听任务处理状态，当任务状态更新为已处理时，需要继续执行智能体
	 */
	private String hitlTaskId;
	/**
	 * 人工介入任务内容，LLM生成
	 * 将作为人工介入任务的提示信息显示给用户
	 */
	private String hitlTaskReason;
	

	
	/**
	 * 人工介入任务内容，人工辅助提供，返回给LLM
	 */
	private String hitlTaskData;
	
	/**
	 * 人工介入任务异常信息
	 */
	private String exception;
	/**
	 * 人工介入任务状态：0 待处理 1 已处理 2 已拒绝 3 超时忽略 5 已结束
	 * 其中状态 3 5 都表示人工介入任务已结束
	 */
	private int hitlTaskStatus;
	/**
	 * 人工介入任务处理结果说明，将作为中断任务的执行结果返回给智能体
	 */
	private String hitlTaskHandleResult;
	
	private LocalDateTime hitlTaskCreateTime;
	
	private LocalDateTime hitlTaskHandleTime;
	

	private LocalDateTime hitlTaskCompleteTime;
	
	
	
	public String getTraceId() {
		return traceId;
	}
	
	public void setTraceId(String traceId) {
		this.traceId = traceId;
	}
	
	public String getAgentId() {
		return agentId;
	}
	
	public void setAgentId(String agentId) {
		this.agentId = agentId;
	}
	
	public String getAgentName() {
		return agentName;
	}
	
	public void setAgentName(String agentName) {
		this.agentName = agentName;
	}
	
	public String getParentAgentId() {
		return parentAgentId;
	}
	
	public void setParentAgentId(String parentAgentId) {
		this.parentAgentId = parentAgentId;
	}
	
	public String getParentAgentName() {
		return parentAgentName;
	}
	
	public void setParentAgentName(String parentAgentName) {
		this.parentAgentName = parentAgentName;
	}
	
	public String getSessionId() {
		return sessionId;
	}
	
	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}
	
	public String getRequestId() {
		return requestId;
	}
	
	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}
	
	public String getUserId() {
		return userId;
	}
	
	public void setUserId(String userId) {
		this.userId = userId;
	}
	
	public String getHitlTaskId() {
		return hitlTaskId;
	}
	
	public void setHitlTaskId(String hitlTaskId) {
		this.hitlTaskId = hitlTaskId;
	}
	
	public String getHitlTaskReason() {
		return hitlTaskReason;
	}
	
	public void setHitlTaskReason(String hitlTaskReason) {
		this.hitlTaskReason = hitlTaskReason;
	}
	
	public int getHitlTaskStatus() {
		return hitlTaskStatus;
	}
	
	public void setHitlTaskStatus(int hitlTaskStatus) {
		this.hitlTaskStatus = hitlTaskStatus;
	}
	
	public String getHitlTaskHandleResult() {
		return hitlTaskHandleResult;
	}
	
	public void setHitlTaskHandleResult(String hitlTaskHandleResult) {
		this.hitlTaskHandleResult = hitlTaskHandleResult;
	}
	
	public LocalDateTime getHitlTaskCreateTime() {
		return hitlTaskCreateTime;
	}
	
	public void setHitlTaskCreateTime(LocalDateTime hitlTaskCreateTime) {
		this.hitlTaskCreateTime = hitlTaskCreateTime;
	}
	
	public LocalDateTime getHitlTaskHandleTime() {
		return hitlTaskHandleTime;
	}
	
	public void setHitlTaskHandleTime(LocalDateTime hitlTaskHandleTime) {
		this.hitlTaskHandleTime = hitlTaskHandleTime;
	}
	
	public LocalDateTime getHitlTaskCompleteTime() {
		return hitlTaskCompleteTime;
	}
	
	public void setHitlTaskCompleteTime(LocalDateTime hitlTaskCompleteTime) {
		this.hitlTaskCompleteTime = hitlTaskCompleteTime;
	}
	public String getException() {
		return exception;
	}
	public void setException(String exception) {
		this.exception = exception;
	}
	public String getHitlTaskData() {
		return hitlTaskData;
	}
	
	public void setHitlTaskData(String hitlTaskData) {
		this.hitlTaskData = hitlTaskData;
	}
	public String getAgentNodeType() {
		return agentNodeType;
	}
	
	public void setAgentNodeType(String agentNodeType) {
		this.agentNodeType = agentNodeType;
	}
}
