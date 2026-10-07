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
import org.apache.commons.lang3.StringUtils;
import org.frameworkset.spi.ai.hitl.cluster.RedisHitlTaskCallListener;
import org.frameworkset.spi.ai.hitl.cluster.RedisHitlTaskCallNotifier;
import org.frameworkset.spi.ai.model.ChatObject;
import org.frameworkset.spi.ai.model.ServerEvent;
import org.frameworkset.spi.ai.model.TraceMessage;
import org.frameworkset.spi.ai.model.annotation.ToolParam;
import org.frameworkset.spi.ai.store.AgentSessionService;
import org.frameworkset.spi.ai.store.SessionMessage;
import org.frameworkset.spi.ai.tool.AgentTraceHolder;
import org.frameworkset.spi.ai.tool.ToolCallContext;
import org.frameworkset.spi.ai.tools.HitlAssistant;
import org.frameworkset.spi.ai.util.ServerEventUtil;
import org.slf4j.Logger;
import reactor.core.publisher.FluxSink;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
	
	public static void setHitlTaskHelper(HitlTaskHelper hitlTaskHelper) {
		HitlTaskHelper.hitlTaskHelper = hitlTaskHelper;	
	}
	
	public HitlTaskHelper setAgentSessionService(AgentSessionService agentSessionService) {
		if (this.agentSessionService == null) {
			this.agentSessionService = agentSessionService;
		}
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
	/**
	 * 获取HitlTaskHelper实例
	 * 需要全局初始化Hitl
	 * @return
	 */
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
	
	public static HitlTaskHelper getHitlTaskHelperOnly() {
//		if(hitlTaskHelper != null){
//			return hitlTaskHelper;
//		}
//		synchronized (lock) {
//			if (hitlTaskHelper == null) {
//				hitlTaskHelper = new HitlTaskHelper();
//			}
//		}
 
		return hitlTaskHelper;
	}
	
	public static Object getLock() {
		return lock;
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
	 * 
	 */
	private static void handleHitlCallTask(Object  hitlTaskData,Throwable throwable, String hitlTaskId){
		
		getHitlTaskHelper()._handleHitlCallTask(false,hitlTaskData, throwable, hitlTaskId);
	}
	
 
	
	/**
	 * 拒绝人工任务
	 * @param hitlTaskData
	 * @param hitlTaskId
	 */
	private static void refuseHitlCallTask(Object  hitlTaskData,Throwable throwable, String hitlTaskId){
		
		getHitlTaskHelper()._handleHitlCallTask(true,hitlTaskData, throwable, hitlTaskId);
	}
	
	/**
	 * 如果人工提交的数据是集合和数组，则在提交数据时需将数据放置到key为hitlTaskHandleData的map结构中提交，例如：
	 * {"hitlTaskHandleData":[{...},{...}]}	
	 * hitlTaskHandleData为保留key，如果提交map结构数据时，不要使用该key作为其他用途
	 * 如果hitlTaskHandleData包含hitlTaskHandleDatakey则将hitlTaskHandleData key对应的值作为人工任务参数提交
	 * @param hitlTaskHandleData
	 * @param hitlTaskId
	 */
	public static void handleHitlTask(Map<String,Object>  hitlTaskHandleData , String hitlTaskId){
		
		handleHitlTask(hitlTaskHandleData, null, hitlTaskId);
		 
	}
	
	/**
	 * 如果人工提交的数据是集合和数组，则在提交数据时需将数据放置到key为hitlTaskHandleData的map结构中提交，例如：
	 * {"hitlTaskHandleData":[{...},{...}]}
	 * hitlTaskHandleData为保留key，如果提交map结构数据时，不要使用该key作为其他用途
	 * 如果hitlTaskHandleData包含hitlTaskHandleDatakey则将hitlTaskHandleData key对应的值作为人工任务参数提交
	 * @param hitlTaskHandleData
	 * @param hitlTaskId
	 */
	public static void handleHitlTask(Map<String,Object>  hitlTaskHandleData ,Throwable throwable, String hitlTaskId){
		
		Object taskData = 	hitlTaskHandleData.get("hitlTaskHandleData");
		if(taskData == null) {
			//兼容老版本
			taskData = hitlTaskHandleData;
			
		}
		handleHitlCallTask(taskData, throwable, hitlTaskId);
		
	}
	
	/**
	 * 如果人工提交的数据是集合和数组，则在提交数据时需将数据放置到key为hitlTaskHandleData的map结构中提交，例如：
	 * {"hitlTaskHandleData":[{...},{...}]}
	 * hitlTaskHandleData为保留key，如果提交map结构数据时，不要使用该key作为其他用途
	 * @param hitlTaskHandleData  如果hitlTaskHandleData中包含type，且type为ok，则表示接受，调用handleHitlCallTask方法，否则表示拒绝，调用refuseHitlCallTask方法。
	 *                               如果hitlTaskHandleData包含hitlTaskHandleDatakey则将hitlTaskHandleData key对应的值作为人工任务参数提交，忽略Refuse处理
	 * @param hitlTaskId
	 */
	public static void handle2ndRefuseHitlTask(Map<String,Object>  hitlTaskHandleData,String hitlTaskId){
		
		handle2ndRefuseHitlTask(hitlTaskHandleData, null, hitlTaskId);	
		
	}
	/**
	 * 如果人工提交的数据是集合和数组，则在提交数据时需将数据放置到key为hitlTaskHandleData的map结构中提交，例如：
	 * {"hitlTaskHandleData":[{...},{...}]}
	 * hitlTaskHandleData为保留key，如果提交map结构数据时，不要使用该key作为其他用途	 
	 * @param hitlTaskHandleData  如果hitlTaskHandleData中包含type，且type为ok，则表示接受，调用handleHitlCallTask方法，否则表示拒绝，调用refuseHitlCallTask方法。
	 *                               如果hitlTaskHandleData包含hitlTaskHandleDatakey则将hitlTaskHandleData key对应的值作为人工任务参数提交，忽略Refuse处理
	 * @param throwable
	 * @param hitlTaskId
	 */
	public static void handle2ndRefuseHitlTask(Map<String,Object>  hitlTaskHandleData,Throwable throwable, String hitlTaskId){
		
		Object taskData = 	hitlTaskHandleData.get("hitlTaskHandleData");
		Map<String,Object> taskDataMap = null;
		if(taskData == null) {
			//兼容老版本
			taskDataMap = hitlTaskHandleData;
			String type = (String) taskDataMap.get("type");
			if (StringUtils.isEmpty(type) || type.equals("ok")) {
				handleHitlCallTask(taskDataMap, throwable, hitlTaskId);
			} else {
				refuseHitlCallTask(taskDataMap, throwable, hitlTaskId);
			}
		}
		else  {
			handleHitlCallTask(taskData, throwable, hitlTaskId);
		}
		
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
			Class elementType = hitlCallObject.getElementType();
			Object reponse = null;
			if(_hitlTaskData != null) {
				 
				if(responseType != null) {
					if(elementType == null) {
						reponse = JsonUtil.json2Object(_hitlTaskData, responseType);
					}
					else{
						reponse = JsonUtil.json2TypeObject(_hitlTaskData, responseType,elementType);
					}
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
			
			throw new HitlCallException(true,e);
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
				Class elementType = hitlCallObject.getElementType();
				Object reponse = null;
				if(_hitlTaskData != null) {
					
					if(responseType != null) {
						if(elementType == null) {
							reponse = JsonUtil.json2Object(_hitlTaskData, responseType);
						}
						else{
							reponse = JsonUtil.json2TypeObject(_hitlTaskData, responseType,elementType);
						}
					}
					else
						reponse = hitlTaskData;
					
					
				}
//				if(_hitlTaskData != null) {
//					if (hitlTaskData instanceof String) {
//						_hitlTaskData = (String) hitlTaskData;
//						if(responseType != null) {
//							if(elementType == null) {
//								reponse = JsonUtil.json2Object(_hitlTaskData, responseType);
//							}
//							else{
//								reponse = JsonUtil.json2TypeObject(_hitlTaskData, responseType,elementType);
//							}
//						}
//						else
//							reponse = hitlTaskData;
//						
//					} else {
//						 
//						reponse = hitlTaskData;
//					}
//				}
				if(throwable != null)
					hitlCallObject.setHitlCallException(throwable);
				hitlCallObject.setResponse(reponse);
			 
			}
			catch (HitlCallException e){
				throw e;
			}
			catch (Exception e){
				
				throw new HitlCallException(true,e);
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
	public static <T> HitlCallResult<T> createHitlCallTask(HitlTaskToolInf hitlTaskcallTool,String hitlTaskReason , 
														   ChatObject chatObject, ToolCallContext toolCallContext,
														   Class<T> responseType){
		
		return getHitlTaskHelper()._createHitlCallTask(  hitlTaskcallTool,hitlTaskReason, chatObject,   toolCallContext,  responseType);
		
		
	}
	
	public static <C,T> HitlCallResult<C> createHitlCallTask(HitlTaskToolInf hitlTaskcallTool,String hitlTaskReason ,
														   ChatObject chatObject, ToolCallContext toolCallContext,Class<C> containerType,
														   Class<T> responseType){
		
		return getHitlTaskHelper()._createHitlCallTask(  hitlTaskcallTool,hitlTaskReason, chatObject,   toolCallContext, containerType, responseType);
		
		
	}
	
	private <T> HitlCallResult<T>	_createHitlCallTask(HitlTaskToolInf hitlTaskcallTool, String hitlTaskReason , 
													ChatObject chatObject, ToolCallContext toolCallContext,Class<T> responseType){
		
		return _createHitlCallTask(  hitlTaskcallTool,   hitlTaskReason ,
				  chatObject,   toolCallContext,  responseType,null);
		
		
		
	}
	
	private <C,T> HitlCallResult<C>	_createHitlCallTask(HitlTaskToolInf hitlTaskcallTool, String hitlTaskReason ,
														 ChatObject chatObject, ToolCallContext toolCallContext,Class<C> containerType,Class<T> elemetType){
		
		HitlCallObject<C> hitlCallObject = new HitlCallObject<>();
		HitlCallTask hitlCallTask = new HitlCallTask();
		hitlCallTask.setHitlTaskReason(hitlTaskReason);
		String hitlTaskId = SimpleStringUtil.getUUID();
		
		hitlCallTask.setHitlTaskCreateTime(LocalDateTime.now());
		hitlCallTask.setHitlTaskId(hitlTaskId);
		ServerEventUtil.buildHiltTaskAgentInfo(hitlCallTask, chatObject.getAgent());
		
		hitlCallObject.setHitlCallTask(hitlCallTask);
		long timeout = hitlTaskcallTool.getHitlTaskTimeout();
		if(timeout <= 0l ){
			timeout = chatObject.getAgent().getHitlTaskTimeout();
		}
		hitlCallObject.setTimeout(timeout);
		hitlCallObject.setResponseType(containerType);
		hitlCallObject.setElementType(elemetType);
		FluxSink<ServerEvent> sink = chatObject.getAgentFluxSink();
		HitlAssistant<C,?> hitlAssistant = hitlTaskcallTool.getHitlAssistant();
		try {
			
			persistentHitlCallTask( hitlCallTask);
			this.hitlCallObjects.put(hitlCallObject.getHitlTaskId(), hitlCallObject);
			long startTime = System.currentTimeMillis();
			Map<String,Object> humanAssistantDatas = null;
			if(hitlAssistant != null ){
				humanAssistantDatas = hitlAssistant.getHumanAssistantDatas(toolCallContext);
			}
			if(AgentTraceHolder.isToolTrace()) {
				TraceMessage traceMessage = new TraceMessage();
				traceMessage.setStartTime(startTime)
						.put("hitlTaskReason", hitlTaskReason)
						.put("hitlTaskId", hitlTaskId)
						.put("role", SessionMessage.MESSAGE_TYPE_HITL_MESSAGE_NAME);
				
				if(humanAssistantDatas != null)
					traceMessage.put("hitlAssistant", humanAssistantDatas);
				
				AgentTraceHolder.trace(traceMessage);
			}
			
			if(sink != null) {
				//推送人工消息到客户端
				ServerEvent serverEvent = new ServerEvent();//向客户端推送人工介入消息
				serverEvent.setData(hitlTaskReason);
				serverEvent.setHitlTaskId(hitlTaskId);
				serverEvent.setType(ServerEvent.TYPE_HITL);
				if(humanAssistantDatas != null){
					serverEvent.setHitlAssistant( humanAssistantDatas);
				}
				ServerEventUtil.buildServerEventAgentInfo(serverEvent, chatObject.getAgent());
				sink.next(serverEvent);
				
			}
			hitlCallObject.await();
			C result = hitlCallObject.getResponse();
			HitlCallResult<C> hitlCallResult = new HitlCallResult<>();
			hitlCallResult.setResult(result);
			if(!hitlCallObject.isFromHumanCountDown()){
				if(!hitlCallObject.isFromDestoryCountDown()) {
					agentSessionService.timeoutHitlCallTask("任务处理超时,等待超时时间:"+hitlCallObject.getTimeout()+"毫秒", hitlTaskId);
					if(hitlAssistant != null) {
						result = (C) hitlAssistant.timeOutHandle(humanAssistantDatas);
						hitlCallResult.setResult(result);
					}
					if(result == null){
						
						if(!hitlTaskcallTool.getTimeoutAction().equals(HitlTaskToolInf.TIMEOUT_ACTION_CONTINUE)) {
							hitlCallResult.setCompleteReason("人工任务处理超时，等待超时时间:" + hitlCallObject.getTimeout() + "毫秒，如任务涉及处理操作，则忽略或者取消相关操作！");
//							result.put("error", "人工任务处理超时，等待超时时间:" + hitlCallObject.getTimeout() + "毫秒，如任务涉及处理操作，则忽略或者取消相关操作！");
						}
						else{
//							result.put("warn", "人工任务处理超时，等待超时时间:" + hitlCallObject.getTimeout() + "毫秒，如任务涉及处理操作，请继续执行完成！");
							hitlCallResult.setCompleteReason("人工任务处理超时，等待超时时间:" + hitlCallObject.getTimeout() + "毫秒，如任务涉及处理操作，请继续执行完成！");
						}
					}
				}
				else {
					logger.info("任务被销毁:hitlTaskId={}",hitlTaskId);
					hitlCallResult.setCompleteReason("任务被销毁,如任务涉及处理操作，则忽略或者取消相关操作！");
//						result.put("error", "任务被销毁,如任务涉及处理操作，则忽略或者取消相关操作！");

//					agentSessionService.destroyHitlCallTask("任务被销毁", hitlTaskId);
				}
				
			}
			else{
				agentSessionService.completeHitlCallTask("任务完成",hitlTaskId);
				if(hitlAssistant != null){
					hitlAssistant.handleHumanSubbmitDatas(result,toolCallContext);
				}
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
					throw new HitlCallException(true,hitlCallException);
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
			return hitlCallResult;
		}
		catch (HitlCallException e){
			throw e;
		}
		catch (Exception e){
			
			throw new HitlCallException(true,e);
		}
		finally {
//			ServerEvent stepServerEvent = new ServerEvent();//向客户端推送人工介入完成的步骤信号
//			stepServerEvent.setType(ServerEvent.TYPE_STEP);
//			ServerEventUtil.buildServerEventAgentInfo(stepServerEvent, chatObject.getAgent());
//			sink.next(stepServerEvent);
			ServerEventUtil.emitterStepEvent(chatObject);
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
		if(initialized){
			return this;
		}
		synchronized (lock) {
			if(!initialized) {
				this.hitlTaskCallListener = new RedisHitlTaskCallListener(redis, channel);
				this.hitlTaskCallNotifier = new RedisHitlTaskCallNotifier(redis, channel);
			}
		}
		return this;
	}
	public HitlTaskHelper setHitlTaskCallNotifier(HitlTaskCallNotifier hitlTaskCallNotifier) {
		if(initialized){
			return this;
		}
		synchronized (lock) {
			if(!initialized) {
				this.hitlTaskCallNotifier = hitlTaskCallNotifier;
			}
		}
		
		return this;
	}
	
	public HitlTaskCallListener getHitlTaskCallListener() {
		return hitlTaskCallListener;
	}
	
	public HitlTaskHelper setHitlTaskCallListener(HitlTaskCallListener hitlTaskCallListener) {
		if(initialized){
			return this;
		}
		synchronized (lock) {
			if(!initialized) {
				this.hitlTaskCallListener = hitlTaskCallListener;
			}
		}	
		return this;
	}
	
	public static <T> HitlCallResult<T> hitlTaskTool(HitlTaskToolInf hitlTaskToolInf,ChatObject chatObject ,
													 String hitlTaskReason,ToolCallContext toolCallContext,Class<T> responseType){
		HitlCallResult<T> hitlCallResult = null;
		try {
			HitlTaskHelper helper = HitlTaskHelper.getHitlTaskHelper();
			
			hitlCallResult = helper.createHitlCallTask(hitlTaskToolInf,hitlTaskReason, chatObject,toolCallContext, responseType);
//			T hitlTaskResult = hitlCallResult != null? hitlCallResult.getResult():null;
//			String completeReason = hitlCallResult != null? hitlCallResult.getCompleteReason():null;
			
//			// 返回结果 null 保护
//			if (hitlCallResult == null) {
//				if(logger.isDebugEnabled()) {
//					logger.debug("hitlTaskTool: createHitlCallTask returned null for reason: {}",
//							hitlTaskReason.length() > 500 ? hitlTaskReason.substring(0, 500) + "..." : hitlTaskReason);
//				}
//				return null;
//			}
			
			
			return hitlCallResult;
		} catch (Exception e) {
			if(logger.isErrorEnabled()) {
				logger.error("hitlTaskTool: failed to create HITL task for reason: {}", hitlTaskReason.length() > 500 ? hitlTaskReason.substring(0, 500) + "..." : hitlTaskReason, e);
			}
			String errorMessage = e.getCause() == null?e.getMessage():e.getCause().getMessage();
			if(errorMessage == null || errorMessage.equals("")){
				errorMessage = "Exception: failed to create HITL task";
			}
			hitlCallResult = new HitlCallResult<>();
			hitlCallResult.setCompleteReason("hitl failed:"+ errorMessage + ", ignore operation and continue.");
			return hitlCallResult;
		} 
	}
	
	
	public static <C,T> HitlCallResult<C> hitlTaskTool(HitlTaskToolInf hitlTaskToolInf,ChatObject chatObject ,
													 String hitlTaskReason,ToolCallContext toolCallContext,Class<C> containerType,Class<T> responseType){
		HitlCallResult<C> hitlCallResult = null;
		try {
			HitlTaskHelper helper = HitlTaskHelper.getHitlTaskHelper();
			
			hitlCallResult = helper.createHitlCallTask(hitlTaskToolInf,hitlTaskReason, chatObject,toolCallContext,containerType, responseType);
//			T hitlTaskResult = hitlCallResult != null? hitlCallResult.getResult():null;
//			String completeReason = hitlCallResult != null? hitlCallResult.getCompleteReason():null;

//			// 返回结果 null 保护
//			if (hitlCallResult == null) {
//				if(logger.isDebugEnabled()) {
//					logger.debug("hitlTaskTool: createHitlCallTask returned null for reason: {}",
//							hitlTaskReason.length() > 500 ? hitlTaskReason.substring(0, 500) + "..." : hitlTaskReason);
//				}
//				return null;
//			}
			
			
			return hitlCallResult;
		} catch (Exception e) {
			if(logger.isErrorEnabled()) {
				logger.error("hitlTaskTool: failed to create HITL task for reason: {}", hitlTaskReason.length() > 500 ? hitlTaskReason.substring(0, 500) + "..." : hitlTaskReason, e);
			}
			String errorMessage = e.getCause() == null?e.getMessage():e.getCause().getMessage();
			if(errorMessage == null || errorMessage.equals("")){
				errorMessage = "Exception: failed to create HITL task";
			}
			hitlCallResult = new HitlCallResult<>();
			hitlCallResult.setCompleteReason("hitl failed:"+ errorMessage + ", ignore operation and continue.");
			return hitlCallResult;
		}
	}
}
