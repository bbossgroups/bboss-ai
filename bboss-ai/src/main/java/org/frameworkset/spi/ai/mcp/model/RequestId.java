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

/**
 * @author biaoping.yin
 * @Date 2026/2/28
 */
public class RequestId {
	private long reqNo;
	/**
	 * 获取下一个请求编号
	 * 线程安全的方法，用于生成递增的请求ID，当编号达到Long.MAX_VALUE时自动重置为0
	 * @return 当前请求编号，返回后编号自动递增
	 */
	public synchronized long nextReqNo(){
        if(reqNo == Long.MAX_VALUE){
            reqNo = 0;
        }
		return reqNo ++;
	}

	
	
}
