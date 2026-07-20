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

import com.frameworkset.util.JsonUtil;
import com.frameworkset.util.SimpleStringUtil;
import org.frameworkset.spi.ai.hitl.cluster.RedisHitlTaskCallListener;
import org.frameworkset.spi.ai.hitl.cluster.RedisHitlTaskCallNotifier;
import org.frameworkset.spi.ai.model.ChatObject;
import org.frameworkset.spi.ai.model.ServerEvent;
import org.frameworkset.spi.ai.model.TraceMessage;
import org.frameworkset.spi.ai.store.AgentSessionService;
import org.frameworkset.spi.ai.store.SessionMessage;
import org.frameworkset.spi.ai.tool.AgentTraceHolder;
import org.frameworkset.spi.ai.util.ServerEventUtil;
import org.slf4j.Logger;
import reactor.core.publisher.FluxSink;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author biaoping.yin
 * @Date 2026/7/16
 */
public class HitlTaskHelper {	
	private static Logger logger = org.slf4j.LoggerFactory.getLogger(HitlTaskHelper.class);
	private Map<String, HitlCallObject> hitlCallObjects = new ConcurrentHashMap<>();
	private static  HitlTaskHelper hitlTaskHelper ;
	private HitlTaskCallListener hitlTaskCallListener;
	
	
	
	private HitlTaskCallNotifier hitlTaskCallNotifier;
	
	private static Object lock = new Object();
	private AgentSessionService agentSessionService;
	
	public HitlTaskHelper setAgentSessionService(AgentSessionService agentSessionService) {
		this.agentSessionService = agentSessionService;
		return this;
	}
	private   HitlCallObject _getHitlCallObject(String hitlTaskId) {
		return hitlCallObjects.get(hitlTaskId);
	}
	public static HitlCallObject getHitlCallObject(String hitlTaskId) {
		return hitlTaskHelper._getHitlCallObject(hitlTaskId);
	}
	private volatile boolean initialized = false;
	private Object lockInit = new Object();
	public HitlTaskHelper init(){
		if (initialized)
			return this;
		synchronized (lockInit) {
			if (initialized)
				return this;
			if(agentSessionService != null){
				agentSessionService.init();
			}
			if(this.hitlTaskCallListener != null){
				this.hitlTaskCallListener.start();
			}
			initialized = true;
		}
		
		return this;
	}
	public static void destory(){
		if(hitlTaskHelper != null){
			hitlTaskHelper._destory();
		}
	}
	
	private void _destory(){
		if (this.hitlCallObjects != null){
			Iterator<Map.Entry<String, HitlCallObject>> iterator = this.hitlCallObjects.entrySet().iterator();
			while (iterator.hasNext()) {
				Map.Entry<String, HitlCallObject> entry = iterator.next();
				HitlCallObject hitlCallObject = entry.getValue();
				hitlCallObject.countDown();
			}
			this.hitlCallObjects.clear();
		}
		if(this.hitlTaskCallListener != null){
			this.hitlTaskCallListener.destroy();
		}
	}
	public static HitlTaskHelper getHitlTaskHelper() {
		if(hitlTaskHelper != null){
			return hitlTaskHelper;
		}
		synchronized (lock) {
			if (hitlTaskHelper == null) {
				hitlTaskHelper = new HitlTaskHelper();
			}
		}
		return hitlTaskHelper;
	}
	/**
	 * 获取人工任务
	 * @param hitlTaskId
	 * @return
	 */
	public static HitlCallTask getHitlCallTask(String hitlTaskId){
		 return getHitlTaskHelper()._getHitlCallTask(hitlTaskId);
	}
	/**
	 * 获取人工任务
	 * @param hitlTaskId
	 * @return
	 */
	private HitlCallTask _getHitlCallTask(String hitlTaskId){
		 return agentSessionService.getHitlCallTask(hitlTaskId);
	}
	/**
	 * 处理人工任务
	 * @param hitlTaskData
	 * @param hitlTaskId
	 */
	public static void handleHitlCallTask(Object  hitlTaskData,Throwable throwable, String hitlTaskId){
		
		getHitlTaskHelper()._handleHitlCallTask(false,hitlTaskData, throwable, hitlTaskId);
	}
	
	/**
	 * 拒绝人工任务
	 * @param hitlTaskData
	 * @param hitlTaskId
	 */
	public static void refuseHitlCallTask(Object  hitlTaskData,Throwable throwable, String hitlTaskId){
		
		getHitlTaskHelper()._handleHitlCallTask(true,hitlTaskData, throwable, hitlTaskId);
	}
	
	/**
	 * 从消息中间件接收和处理人工任务
	 * @param hitlTaskId
	 */
	public static void handleHitlCallTask(String hitlTaskId){
		getHitlTaskHelper()._handleHitlCallTask(hitlTaskId);
	}
	private void _handleHitlCallTask(String hitlTaskId){
		HitlCallObject hitlCallObject = this.removeHitlCallObject(hitlTaskId);
		if(hitlCallObject == null)
			return;
		
		HitlCallTask hitlCallTask = this.agentSessionService.getHitlCallTask(hitlTaskId);
		try {
			String _hitlTaskData = hitlCallTask.getHitlTaskData();
			String exception = hitlCallTask.getException();
			Class responseType = hitlCallObject.getResponseType();
			Object reponse = null;
			if(_hitlTaskData != null) {
				 
				if(responseType != null) {
					reponse = JsonUtil.json2Object(_hitlTaskData, responseType);
				}
				else
					reponse = _hitlTaskData;
					
				 
			}
			if(exception != null)
				hitlCallObject.setHitlCallException(new HitlCallException(exception));
			hitlCallObject.setResponse(reponse);
			
		}
		catch (HitlCallException e){
			throw e;
		}
		catch (Exception e){
			
			throw new HitlCallException(e);
		}
		finally {
			hitlCallObject.countDown();
		}
	}
	/**
	 * 处理人工任务
	 * @param hitlTaskData
	 * @param hitlTaskId
	 */
	private void _handleHitlCallTask(boolean refused,Object  hitlTaskData,Throwable throwable, String hitlTaskId){
		String _hitlTaskData = null;
		if(!refused)
			_hitlTaskData = agentSessionService.handledHitlCallTask(hitlTaskData,throwable, hitlTaskId);
		else
			_hitlTaskData = agentSessionService.refusedHitlCallTask(hitlTaskData,throwable, hitlTaskId);
		//模拟监听到人工任务确认消息		
		HitlCallObject hitlCallObject = getHitlCallObject(hitlTaskId);
		//如果缓存对象就在本机，无需推送消息，直接处理相应即可
		if(hitlCallObject != null){	 		 
			
			try {
				
				Class responseType = hitlCallObject.getResponseType();
				Object reponse = null;
				if(_hitlTaskData != null) {
					if (hitlTaskData instanceof String) {
						_hitlTaskData = (String) hitlTaskData;
						if(responseType != null) {
							reponse = JsonUtil.json2Object(_hitlTaskData, responseType);
						}
						else
							reponse = hitlTaskData;
						
					} else {
						 
						reponse = hitlTaskData;
					}
				}
				if(throwable != null)
					hitlCallObject.setHitlCallException(throwable);
				hitlCallObject.setResponse(reponse);
			 
			}
			catch (HitlCallException e){
				throw e;
			}
			catch (Exception e){
				
				throw new HitlCallException(e);
			}
			finally {
				hitlCallObject.countDown();
				this.removeHitlCallObject(hitlTaskId);
			}
		}
		else{
			if(this.hitlTaskCallNotifier != null){
				this.hitlTaskCallNotifier.notifyHitlTaskCallResult(hitlTaskId);
			}
		}
		 
	}
	 
	private void persistentHitlCallTask(HitlCallTask hitlCallTask){
		// 1. 保存人工介入任务到数据库表中
		// 2. 发送人工介入任务到客户端
		agentSessionService.persistentHitlCallTask(hitlCallTask);
	}
	public static Map<String,Object> createHitlCallTask(String hitlTaskReason , ChatObject chatObject){
		
		return getHitlTaskHelper()._createHitlCallTask(hitlTaskReason, chatObject);
		
		
	}
	
	private  Map<String,Object> _createHitlCallTask(String hitlTaskReason , ChatObject chatObject){
		
		HitlCallObject<Map> hitlCallObject = new HitlCallObject<>();
		HitlCallTask hitlCallTask = new HitlCallTask();
		hitlCallTask.setHitlTaskReason(hitlTaskReason);
		String hitlTaskId = SimpleStringUtil.getUUID();
		
		hitlCallTask.setHitlTaskCreateTime(LocalDateTime.now());
		hitlCallTask.setHitlTaskId(hitlTaskId);
		ServerEventUtil.buildHiltTaskAgentInfo(hitlCallTask, chatObject.getAgent());
		
		hitlCallObject.setHitlCallTask(hitlCallTask);
		hitlCallObject.setTimeout(chatObject.getAgent().getHitlTaskTimeout());
		hitlCallObject.setResponseType(Map.class);
	
		try {
			
			persistentHitlCallTask( hitlCallTask);
			this.hitlCallObjects.put(hitlCallObject.getHitlTaskId(), hitlCallObject);
			long startTime = System.currentTimeMillis();
			if(AgentTraceHolder.isToolTrace()) {
				TraceMessage traceMessage = new TraceMessage();
				traceMessage.setStartTime(startTime)
						.put("hitlTaskReason", hitlTaskReason)
						.put("hitlTaskId", hitlTaskId)
						.put("role", SessionMessage.MESSAGE_TYPE_HITL_MESSAGE_NAME);
				AgentTraceHolder.trace(traceMessage);
			}
			FluxSink<ServerEvent> sink = chatObject.getAgentFluxSink();
			if(sink != null) {
				//推送人工消息到客户端
				ServerEvent serverEvent = new ServerEvent();//向客户端推送人工介入消息
				serverEvent.setData(hitlTaskReason);
				serverEvent.setHitlTaskId(hitlTaskId);
				serverEvent.setType(ServerEvent.TYPE_HITL);
				ServerEventUtil.buildServerEventAgentInfo(serverEvent, chatObject.getAgent());
				sink.next(serverEvent);			 
				
			}
			hitlCallObject.await();
			Map<String,Object> result = hitlCallObject.getResponse();
			
			if(!hitlCallObject.isFromHumanCountDown()){
				if(!hitlCallObject.isFromDestoryCountDown()) {
					agentSessionService.timeoutHitlCallTask("任务处理超时", hitlTaskId);
				}
				else {
					logger.info("任务被销毁:hitlTaskId={}",hitlTaskId);
//					agentSessionService.destroyHitlCallTask("任务被销毁", hitlTaskId);
				}
			}
			else{
				agentSessionService.completeHitlCallTask("任务完成",hitlTaskId);
			}
			Throwable hitlCallException = hitlCallObject.getHitlCallException();
			
			if(hitlCallException != null) {
				if(AgentTraceHolder.isToolTrace()) {
					TraceMessage traceMessage = new TraceMessage();
					traceMessage.setStartTime(startTime).setEndTime(System.currentTimeMillis())
							.put("hitlTaskHandlerException", hitlCallException)							
							.put("hitlTaskId", hitlTaskId)
							.put("role", SessionMessage.MESSAGE_TYPE_HITL_HANDLE_MESSAGE_NAME);
					if(result != null){
						traceMessage.put("hitlTaskHandleData", result);
					}
					AgentTraceHolder.trace(traceMessage);
				}
				if(hitlCallException instanceof HitlCallException) {
					throw (HitlCallException) hitlCallException;
				}
				else
					throw new HitlCallException(hitlCallException);
			}
			else{
				if(AgentTraceHolder.isToolTrace()) {
					TraceMessage traceMessage = new TraceMessage();
					traceMessage.setStartTime(startTime).setEndTime(System.currentTimeMillis())
							.put("hitlTaskHandleData", result)
							.put("role", SessionMessage.MESSAGE_TYPE_HITL_HANDLE_MESSAGE_NAME);
					AgentTraceHolder.trace(traceMessage);
				}
			}
			return result;
		}
		catch (HitlCallException e){
			throw e;
		}
		catch (Exception e){
			
			throw new HitlCallException(e);
		}
		finally {
			this.removeHitlCallObject(hitlTaskId);
		}
		
		
		
	}
	
 
 
 
	
	
	private HitlCallObject removeHitlCallObject(String hitlTaskId){
		return hitlCallObjects.remove(hitlTaskId);
	}
	public HitlTaskCallNotifier getHitlTaskCallNotifier() {
		return hitlTaskCallNotifier;
	}
	
	public HitlTaskHelper setRedisChannel(String redis,String channel) {
		this.hitlTaskCallListener = new RedisHitlTaskCallListener(redis,channel);
		this.hitlTaskCallNotifier = new RedisHitlTaskCallNotifier(redis,channel);
		return this;
	}
	public HitlTaskHelper setHitlTaskCallNotifier(HitlTaskCallNotifier hitlTaskCallNotifier) {
		this.hitlTaskCallNotifier = hitlTaskCallNotifier;
		return this;
	}
	
	public HitlTaskCallListener getHitlTaskCallListener() {
		return hitlTaskCallListener;
	}
	
	public HitlTaskHelper setHitlTaskCallListener(HitlTaskCallListener hitlTaskCallListener) {
		this.hitlTaskCallListener = hitlTaskCallListener;
		return this;
	}
}
