package org.frameworkset.spi.ai.hitl.cluster;
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

import com.frameworkset.util.SimpleStringUtil;
import org.frameworkset.nosql.redis.RedisTool;
import org.frameworkset.spi.ai.hitl.HitlTaskCallListener;
import org.frameworkset.spi.ai.hitl.HitlTaskHelper;
import redis.clients.jedis.JedisPubSub;

/**
 * 基于redis的人工介入任务调用结果监听器，用于监听人工介入任务调用结果，当人工介入任务调用结果返回时，会调用HitlTaskHelper.handleHitlCallTask方法处理人工介入任务调用结果
 * @author biaoping.yin
 * @Date 2026/7/19
 */
public class RedisHitlTaskCallListener implements HitlTaskCallListener {
	private String redis;
	private Thread thread;
	public static final String DEFAULT_CHANNEL = "event:hitlTaskCallResult";
	private String channel = DEFAULT_CHANNEL;
	public RedisHitlTaskCallListener(String redis) {
		this.redis = redis;
	}
	public RedisHitlTaskCallListener(String redis,String channel) {
		this.redis = redis;
		if(SimpleStringUtil.isNotEmpty(channel))
			this.channel = channel;
	}
	public void start(){
		if(thread != null && thread.isAlive())
			return;
		RedisTool redisTool = RedisTool.getInstance(redis);//获取指定名称的redis数据源
		thread = new Thread(() -> redisTool.subscribe(new   JedisPubSub() {
			@Override
			public void onMessage(String channel, String message) {
				HitlTaskHelper.handleHitlCallTask(message);
			}			 
	 
		}, channel));
		thread.setName("RedisHitlTaskCallListener");
		thread.setDaemon(true);
		thread.start();
	}
	public void destroy(){
		if(thread != null && thread.isAlive()) {
			thread.interrupt();
			try {
				thread.join();
			} catch (InterruptedException e) {
				 Thread.currentThread().interrupt();
			}
		}
		
	}
	
}
