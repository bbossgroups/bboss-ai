package org.frameworkset.spi.ai.util;
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
import org.frameworkset.spi.ai.model.ChatObject;
import org.frameworkset.spi.ai.model.LinkedMessageMap;
import org.frameworkset.spi.ai.model.ServerEvent;
import org.frameworkset.spi.remote.http.ResponseUtil;
import org.frameworkset.util.RetryCallback;
import org.frameworkset.util.RetryContext;
import org.frameworkset.util.beans.ObjectHolder;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 *
 * @author biaoping.yin
 * @Date 2026/10/27
 */
public class AgentLLMRetryCallback implements RetryCallback<ServerEvent> {
	private static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AgentLLMRetryCallback.class);
	private ObjectHolder<String> objectHolder;
	private String _data;
	private Object message;
	private ChatObject chatObject;
	private Function<Object, ServerEvent> function;
	public AgentLLMRetryCallback(  String _data, ObjectHolder<String> objectHolder, Object message, ChatObject chatObject, Function<Object, ServerEvent> function ){
		this.objectHolder = objectHolder;
		this._data = _data;
		this.chatObject = chatObject;
		this.message = message;
		this.function = function;
	}
	public ServerEvent doRetry (Throwable throwable	,   int retryCount, RetryContext retryContext)  {
		
		String tmp = null;
		boolean compacted = false;
		if(objectHolder.getObject() == null){
			tmp = _data;
		}
		else{
			tmp = objectHolder.getObject();
			compacted = true;
		}
		
		if( AIResponseUtil.isContextOverflowError(throwable) && message instanceof Map){
			if(compacted){
				retryContext.setTerminateRetry(true);
				logger.warn("压缩后仍然检测到溢出→不再重试");
				return null;
			}
			logger.warn("检测到溢出→开始压缩消息");
			
			Map messageMap = (Map)message;
			List<LinkedMessageMap<String,Object>> messages = (List<LinkedMessageMap<String, Object>>) messageMap.get("messages");
			List<LinkedMessageMap<String,Object>> compact;
			try {
				compact = AIAgentUtil.forceCompact(  chatObject.getAgent(),  messages,   chatObject);
			} catch (RuntimeException e) {
				logger.warn("上下文压缩失败，终止重试", e);
				retryContext.setTerminateRetry(true);
				return null;
			}
			
			if(compact != messages ){
				logger.warn("Context overflow error, retrying with compacted messages");
				messageMap.put("messages", compact);
				tmp = SimpleStringUtil.object2json(messageMap);
				objectHolder.setObject(tmp);
			}
			else{ //如果消息未压缩，是否需要重试呢：不需要，终止重试
				retryContext.setTerminateRetry(true);
				return null;
			}
		}
		return function.apply(tmp);
//		HttpRequestProxy.sendJsonBody(clientConfiguration,  tmp, chatObject.getCompletionsUrl(), header, responseHandler);
	}
	
	@Override
	public boolean assertRetryException(Throwable throwable) {
		if(chatObject.isLlmCallOk()){
			return false;
		}
		if(throwable == null || throwable instanceof NullPointerException){
			return false;
		}
		Integer statusCode = ResponseUtil.getHttpReponseStatusCodeFromThrowable(throwable);
		if(statusCode == null){
			return true;
		}
		if (statusCode == 400) {
			return AIResponseUtil.isContextOverflowError(throwable) && message instanceof Map;   // 只有溢出才值得再走一轮
		}
		if(statusCode == 413 || statusCode == 408 || statusCode == 429 || statusCode >= 500) 
			return true;
//		//401/403/404
//		if (statusCode == 401 || statusCode == 403 || statusCode == 404
//		) {
//			return false; // 客户端错误，不重试
//		}
		return false;
	}
}
