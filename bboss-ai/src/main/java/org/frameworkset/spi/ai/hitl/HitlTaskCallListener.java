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

/**
 * 人工介入任务调用结果监听器，用于监听人工介入任务调用结果，当人工介入任务调用结果返回时，会调用HitlTaskHelper.handleHitlCallTask方法处理人工介入任务调用结果
 * @author biaoping.yin
 * @Date 2026/7/19
 */
public interface HitlTaskCallListener {
	/**
	 * 启动人工介入任务处理监听器
	 */
	void start();
	default  void destroy(){
		
	}
	
}
