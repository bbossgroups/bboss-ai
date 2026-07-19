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
import org.frameworkset.spi.ai.hitl.HitlTaskCallNotifier;

/**
 *
 * @author biaoping.yin
 * @Date 2026/7/19
 */
public class RedisHitlTaskCallNotifier implements HitlTaskCallNotifier {
	private String redis;
	private String channel = RedisHitlTaskCallListener.DEFAULT_CHANNEL;
	public RedisHitlTaskCallNotifier(String redis) {
		this.redis = redis;
	}
	public RedisHitlTaskCallNotifier(String redis,String channel) {
		this.redis = redis;
		if(SimpleStringUtil.isNotEmpty(channel))
			this.channel = channel;
	}
	/**
	 * 人工介入任务调用结果通知
	 * @param hitlTaskId 人工介入任务id
	 */
	public void notifyHitlTaskCallResult(String hitlTaskId){
		RedisTool redisTool = RedisTool.getInstance(redis);//获取指定名称的redis数据源
		redisTool.publish(channel,hitlTaskId);//发布人工介入任务调用结果通知
	}
	
	
}
