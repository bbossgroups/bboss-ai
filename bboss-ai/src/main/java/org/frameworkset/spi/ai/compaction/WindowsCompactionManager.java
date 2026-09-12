package org.frameworkset.spi.ai.compaction;
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

import org.frameworkset.spi.ai.AIAgent;
import org.frameworkset.spi.ai.context.ChatContext;
import org.frameworkset.spi.ai.model.AIRuntimeException;
import org.frameworkset.spi.ai.model.LinkedMessageMap;
import org.frameworkset.spi.ai.util.MessageBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author biaoping.yin
 * @Date 2026/9/8
 */
public class WindowsCompactionManager extends BaseCompactionManager{
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(WindowsCompactionManager.class);
	private int sessionSize;
	private int triggerSessionSize;
	public WindowsCompactionManager(
			CompactionConfig config) {
		super(config);
		sessionSize = config.getKeepMessages();
		triggerSessionSize = config.getTriggerMessages();
		if(sessionSize > triggerSessionSize){
			throw new AIRuntimeException("triggerSessionSize("+triggerSessionSize+") must be greater than or equal to sessionSize("+sessionSize+")");
		}
	}
	
	/**
	 * 压缩原则：
	 * 1.确保工具调用结果和对应的入参都在keep messages窗口内
	 * 2.保留system消息，不参与keep messages统计
	 * 3.新的摘要消息添加到system和tails消息之间，不参与keep messages统计
	 * 
	 * 压缩后 LLM 实际看到的列表长度 ≈ 1(SYSTEM) + 1(新摘要) + tail条数,而 tail条数 → keepMessages 只是个软目标——这也意味着单看“压缩后上下文还有几条消息”，不能直接反推出 keepMessages 的配置值。
	 * @param agent
	 * @param messages
	 * @return
	 */
	@Override
	public List<LinkedMessageMap<String, Object>> compact(ChatContext chatContext, AIAgent agent,
														  List<LinkedMessageMap<String, Object>> messages) {
		if(messages == null || messages.size() == 0)
			return messages;
		LinkedMessageMap<String, Object> systemMessage = messages.get(0);
		String systemRole = (String) systemMessage.get("role");
		List<LinkedMessageMap<String, Object>> compactedMessages = null;
		if(systemRole != null && systemRole.equals(MessageBuilder.ROLE_SYSTEM)) {
			compactedMessages = messages.subList(1, messages.size());
		}
		else{
			compactedMessages = messages;
			systemMessage = null;
		}
		if(triggerSessionSize > 0 && compactedMessages.size() >= triggerSessionSize){
			 
		 
			int removePosition = compactedMessages.size() - sessionSize;
			
			if(removePosition == 0){
				return messages;
			}
			//确保工具调用结果和对应的入参都在keep messages窗口内
			Map<String, Object> toolCallIds = new LinkedHashMap<>();
			for(int j = compactedMessages.size() - 1; j >= removePosition; j --){
				LinkedMessageMap<String, Object> message = compactedMessages.get(j);
				String role = (String) message.get("role");
				if(role.equals(MessageBuilder.ROLE_TOOL)) { //处理工具调用结果
					String id = (String) message.get("tool_call_id");
					
					toolCallIds.put(id, 1);
				}
				else{
					//获取工具入参
					List<Map<String, Object>> toolCalls = (List<Map<String, Object>>) message.get("tool_calls");
					if(toolCalls != null && !toolCalls.isEmpty()){ //工具入参
						for(Map toolCall : toolCalls){
							String toolCallId = (String) toolCall.get("id");
							if(toolCallIds.containsKey(toolCallId)){ //在keep messages窗口内找到工具入参消息，直接清理掉
								toolCallIds.remove(toolCallId);
							}
							 
						}
						 
					}
				}
			}
			if(!toolCallIds.isEmpty()){ //意味着工具入参消息在窗口之外，需要将这些工具入参消息添加到messages中
				int k = 0;
				for(k = removePosition -1 ; k >= 0 ; k --){
					LinkedMessageMap<String, Object> message = compactedMessages.get(k);
					String role = (String) message.get("role");
					if(role.equals(MessageBuilder.ROLE_TOOL)) { //在回溯过程中，又碰到了工具调用结果，还需继续进行回溯
						String id = (String) message.get("tool_call_id");
						toolCallIds.put(id, 1);
					}
					else {
						//获取工具入参
						List<Map<String, Object>> toolCalls = (List<Map<String, Object>>) message.get("tool_calls");
						if (toolCalls != null && !toolCalls.isEmpty()) { //工具入参
							for (Map toolCall : toolCalls) {
								String toolCallId = (String) toolCall.get("id");
								if (toolCallIds.containsKey(toolCallId)) { //在keep messages窗口内找到工具入参消息，直接清理掉
									toolCallIds.remove(toolCallId);
								}								
							}							
						}						
					}
					if (toolCallIds.isEmpty()) {						
						break;
					}
				}				 
				removePosition = k < 0 ? 0 : k;
			}
					
			 
			List<LinkedMessageMap<String, Object>> newMessages = null;
			if(removePosition > 0 && removePosition < compactedMessages.size()) {
				newMessages = new ArrayList<>(compactedMessages.subList(removePosition, compactedMessages.size()));
				List<LinkedMessageMap<String, Object>> summeryMessage = compactedMessages.subList(0, removePosition);
				if(logger.isInfoEnabled()){
					logger.info("为卸载压缩的消息生成摘要，卸载记录数：{}",summeryMessage.size());
				}
				String summery = SummeryUtils.summarizePrefix(summeryMessage, config,chatContext);
				LinkedMessageMap<String,Object> summaryMessage = SummeryUtils.buildSummaryMessage(summery,null,newMessages.get(0));
				 
				agent.saveSummeryMessage(summaryMessage);
				newMessages.add(0, summaryMessage);
				
			}
			else{
				newMessages = compactedMessages;
			}
			
			
			if(systemMessage != null) {
				newMessages.add(0, systemMessage);
			}
			logger.info("压缩前消息记录size：{},压缩后消息压缩前消息记录size: {}，cuttoff position: {}",messages.size(), newMessages.size(), removePosition); // Log the size of the compacted messages
			return newMessages;
		}
		return messages;
	}
}
