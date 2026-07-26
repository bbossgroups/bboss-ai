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

/**
 *
 * @author biaoping.yin
 * @Date 2026/7/26
 */
public class AuditResult {
	/**
	 * 审计是否通过
	 */
	private boolean success;
	/**
	 * 审计结果描述
	 */
	private String message;
	/**
	 * 审计阻断后的下一步操作指引
	 */
	private String nextAction;
	/**
	 * 获取审计是否通过
	 * @return the success
	 */
	public boolean isSuccess() {
		return success;
	}
	/**
	 * 设置审计是否通过
	 * @param success the success to set
	 */
	public void setSuccess(boolean success) {
		this.success = success;
	}
	/**
	 * 获取审计结果描述
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}
	/**
	 * 设置审计结果描述
	 * @param message the message to set
	 */
	public void setMessage(String message) {
		this.message = message;
	}
	
	public String getNextAction() {
		return nextAction;
	}
	
	public void setNextAction(String nextAction) {
		this.nextAction = nextAction;
	}
}
