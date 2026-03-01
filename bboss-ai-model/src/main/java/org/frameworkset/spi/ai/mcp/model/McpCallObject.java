package org.frameworkset.spi.ai.mcp.model;
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

import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * @author biaoping.yin
 * @Date 2026/2/28
 */
public class McpCallObject<T> {
	private long requestId;
	private T response;
	private CountDownLatch countDownLatch;
	private Long timeout = 60000L;
	private Class<T> responseType;
	private McpCallException exception;
	public McpCallObject(Class<T> responseType) {
		this.responseType = responseType;
	}
	public long getRequestId() {
		return requestId;
	}
	
	public void setRequestId(long requestId) {
		this.requestId = requestId;
	}
	
	 
	
 
	public void await() {
		countDownLatch = new CountDownLatch(1);
		try {
			countDownLatch.await(timeout, java.util.concurrent.TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			 
		}
	}
	public void countDown(){
		if(countDownLatch != null){
			countDownLatch.countDown();
		}
	}
	
	public void setResponse(T response) {
		// TODO: Convert Map to T

		this.response = response;
	}
	
	public T getResponse() {
		return response;
	}
	
	public Class<T> getResponseType() {
		return responseType;
	}
	
	public McpCallException getException() {
		return exception;
	}
	public void setException(McpCallException exception) {
		this.exception = exception;
	}	
}
