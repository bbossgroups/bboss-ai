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
import org.frameworkset.spi.ai.memory.MemoryManager;
import org.frameworkset.spi.ai.model.AIRuntimeException;
import org.frameworkset.spi.ai.model.LinkedMessageMap;
import org.frameworkset.spi.ai.model.ModelInfo;
import org.frameworkset.spi.ai.model.tool.PermissionRules;
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
			 
		 
			int cutoffIndex = compactedMessages.size() - sessionSize;
			
			if(cutoffIndex == 0){
				return messages;
			}
			
			cutoffIndex = ConversationCompactor.findSafeCutoffPoint( compactedMessages,  cutoffIndex);
				
			 
			List<LinkedMessageMap<String, Object>> newMessages = null;
			if(cutoffIndex > 0 && cutoffIndex < compactedMessages.size()) {
				newMessages = new ArrayList<>(compactedMessages.subList(cutoffIndex, compactedMessages.size()));
				List<LinkedMessageMap<String, Object>> summeryMessage = compactedMessages.subList(0, cutoffIndex);
				if(logger.isInfoEnabled()){
					logger.info("为卸载压缩的消息生成摘要，卸载记录数：{}",summeryMessage.size());
				}
				// Step 2: Flush long-term memories only from newly compacted raw messages (best-effort).
				if(config.isFlushBeforeCompact()) {
					List<LinkedMessageMap<String, Object>> flushInput = ConversationCompactor.filterSummaryMessages(summeryMessage);
					ModelInfo model = config.getCompactModel();
					if(model == null)
						model = chatContext.getModelInfo();
					MemoryManager flushManager =
							new MemoryManager(model);
					flushManager
							.flushMemories(  agent, flushInput);
				}
				String summery = SummeryUtils.summarizePrefix(summeryMessage, config,chatContext);
				LinkedMessageMap<String,Object> summaryMessage = SummeryUtils.buildSummaryMessage(chatContext,agent,summery,summeryMessage,null,newMessages.get(0));
				 
				agent.saveSummeryMessage(summaryMessage);
				newMessages.add(0, summaryMessage);
				
			}
			else{
				newMessages = compactedMessages;
			}
			
			
			if(systemMessage != null) {
				newMessages.add(0, systemMessage);
			}
			logger.info("压缩前消息记录size：{},压缩后消息记录size: {}，cuttoff position: {}",messages.size(), newMessages.size(), cutoffIndex); // Log the size of the compacted messages
			return newMessages;
		}
		return messages;
	}
}
