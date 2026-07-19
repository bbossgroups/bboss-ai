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

import java.util.concurrent.CountDownLatch;

/**
 *
 * @author biaoping.yin
 * @Date 2026/7/16
 */
public class HitlCallObject<T> {
	private HitlCallTask hitlCallTask;
	private CountDownLatch countDownLatch;
	private Throwable hitlCallException;
	private Class<T> responseType;
	private int taskStatus = HitlCallTask.TASK_STATUS_UNHANDLED;
	
	private T response;
	
	/**
	 * 人工介入任务处理超时时间，默认-1秒，-1秒表示无限期等待
	 */
	private long timeout = -1L;
	
	public String getHitlTaskId(){
		return hitlCallTask.getHitlTaskId();
	}
	private boolean fromHumanCountDown = false;
	private boolean fromDestoryCountDown = false;
	public HitlCallObject(){
		countDownLatch = new CountDownLatch(1);
	}
	public void await() {
		
		try {
			if(timeout > 0) {
				countDownLatch.await(timeout, java.util.concurrent.TimeUnit.MILLISECONDS);
			}
			else{
				countDownLatch.await();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
	
	public boolean isFromHumanCountDown() {
		return fromHumanCountDown;
	}
	
	public boolean isFromDestoryCountDown() {
		return fromDestoryCountDown;
	}
	
	public void countDown(){
		fromHumanCountDown = true;
		if(countDownLatch != null){
			
			countDownLatch.countDown();
		}
	}
	
	public void destory(){
		fromDestoryCountDown = true;
		if(countDownLatch != null){
			
			countDownLatch.countDown();
		}
	}
	
	public HitlCallTask getHitlCallTask() {
		return hitlCallTask;
	}
	
	public void setHitlCallTask(HitlCallTask hitlCallTask) {
		this.hitlCallTask = hitlCallTask;
	}
	
	public Throwable getHitlCallException() {
		return hitlCallException;
	}
	
	public void setHitlCallException(Throwable hitlCallException) {
		this.hitlCallException = hitlCallException;
	}
	
	public Class<T> getResponseType() {
		return responseType;
	}
	
	public void setResponseType(Class<T> responseType) {
		this.responseType = responseType;
	}
	
	public T getResponse() {
		return response;
	}
	
	public void setResponse(T response) {
		this.response = response;
	}
	
	public long getTimeout() {
		return timeout;
	}
	
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}
	
	public void setTaskStatus(int taskStatus) {
		this.taskStatus = taskStatus;
	}
	
	public int getTaskStatus() {
		return taskStatus;
	}
}
