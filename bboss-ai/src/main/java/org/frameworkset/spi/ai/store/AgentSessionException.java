/**
 *  Copyright 2008-2010 biaoping.yin
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.frameworkset.spi.ai.store;

/**
 * <p>Title: AgentSessionException</p> <p>Description: 会话管理异常处理类
 * 
 * </p> <p>asiainfo</p> <p>Copyright (c) 2007</p> @Date 2026-06-12
 * 14:16:51 @author yinbp @version v1.0
 */
public class AgentSessionException extends RuntimeException {

	public AgentSessionException() {
		super();
	}
	public AgentSessionException(String message, Throwable cause) {
		super(message, cause);
	}

	public AgentSessionException(String message) {
		super(message);
	}

	public AgentSessionException(Throwable cause) {
		super(cause);
	}

}