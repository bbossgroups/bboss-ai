package org.frameworkset.spi.ai.memory;
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

import org.frameworkset.util.concurrent.ThreadPoolFactory;

import java.util.concurrent.ExecutorService;

/**
 *
 * @author biaoping.yin
 * @Date 2026/10/9
 */
public class MemoryUtil {
	
//	private static final int MEMORY_FLUSH_TASK_CORE_POOL_SIZE = 100;
	private static final int MEMORY_FLUSH_TASK_MAX_POOL_SIZE = 500;
	private static final int MEMORY_FLUSH_TASK_QUEUE_CAPACITY = 500;
	private static final long MEMORY_FLUSH_TASK_KEEP_ALIVE_SECONDS = 60L;
	private static final long BLOCKEDWAITTIMEOUT = 50L	;
	private static final int WARNMULTSREJECTS = 1000;
	private static final boolean PRESTARTALLCORETHREADS = false;
	private static final boolean DAEMON = true;
	private static final ExecutorService MEMORY_FLUSH_TASK_EXECUTOR = ThreadPoolFactory.buildThreadPool("Agent-MemoryFlushMiddleware-task","Agent-MemoryFlushMiddleware-task-reject-message" ,
			MEMORY_FLUSH_TASK_MAX_POOL_SIZE,MEMORY_FLUSH_TASK_QUEUE_CAPACITY,MEMORY_FLUSH_TASK_KEEP_ALIVE_SECONDS * 1000L,
			BLOCKEDWAITTIMEOUT,WARNMULTSREJECTS,PRESTARTALLCORETHREADS,DAEMON);
	
	private static final ExecutorService MEMORY_MAINTAIN_TASK_EXECUTOR = ThreadPoolFactory.buildThreadPool("Agent-MemoryMaintainMiddleware-task","Agent-MemoryMaintainMiddleware-task-reject-message" ,
			MEMORY_FLUSH_TASK_MAX_POOL_SIZE,MEMORY_FLUSH_TASK_QUEUE_CAPACITY,MEMORY_FLUSH_TASK_KEEP_ALIVE_SECONDS * 1000L,
			BLOCKEDWAITTIMEOUT,WARNMULTSREJECTS,PRESTARTALLCORETHREADS,DAEMON);
	public static boolean isDayMemoryPath(String path){
		if(path.equals("MEMORY.md"))
			return false;
		else if(path.startsWith("memory/")){
			return true;
		}
		return false;
	}
	
	public static ExecutorService getMemoryFlushTaskExecutor() {
		return MEMORY_FLUSH_TASK_EXECUTOR;
	}
	
	public static ExecutorService getMemoryMaintainTaskExecutor() {
		return MEMORY_MAINTAIN_TASK_EXECUTOR;
	}
}
