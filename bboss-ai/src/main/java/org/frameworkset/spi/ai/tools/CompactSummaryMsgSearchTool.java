package org.frameworkset.spi.ai.tools;
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
import org.frameworkset.spi.ai.AIAgent;
import org.frameworkset.spi.ai.compaction.SummeryUtils;
import org.frameworkset.spi.ai.model.ChatObject;
import org.frameworkset.spi.ai.model.annotation.Tool;
import org.frameworkset.spi.ai.model.annotation.ToolParam;
import org.frameworkset.spi.ai.store.SessionMessage;
import org.frameworkset.spi.ai.tool.AgentTraceHolder;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 压缩摘要查询工具
 * @author biaoping.yin
 * @Date 2026/9/13
 */
public class CompactSummaryMsgSearchTool {
//	@Tool(name = "summary_search",readOnly = true, description = "查询压缩摘要消息对应的原始消息。" +
//			"如果当前会话消息记录中包含有压缩摘要消息，且压缩摘要消息包含summaryMessageIds，以下情况可以使用该工具查询摘要消息对应的原始消息：\r\n" +
//			"1.了解或者回顾智能体之前都做了什么\r\n" +
//			"2.需从历史消息中获取与用户问题相关的关键事实\r\n" 
//			+"3.完成当前任务，当前上下文中信息不全，需要从压缩摘要消息对应的原始消息内容中了解更多信息\r\n" )
//	public String summarySearch(@ToolParam(name = "summary_message_ids", 
//			description = "压缩摘要消息ID列表",required = true) 
//									String[] summaryMessageIds){
//		if(summaryMessageIds == null || summaryMessageIds.length == 0)
//			return "没有需要查询的压缩摘要消息ID列表";
//		ChatObject chatObject = AgentTraceHolder.getChatObject();
//		AIAgent agent = chatObject.getAgent();
//		String sessionId = agent.getSessionId();
//		
//		List<SessionMessage> sessionMessages = agent.getMainSessionStore().getSessionMessages(sessionId, summaryMessageIds);
//		if(sessionMessages == null || sessionMessages.size() == 0)
//			return "没有找到对应的会话消息";
//		else
//			return SummeryUtils.formatSessionMessagesForSummary(sessionMessages);
//		
//		
//	}
	
	@Tool(
			name = "summary_search",
			readOnly = true,
			description = "查询压缩摘要消息对应的原始消息。" +
					"如果当前会话消息记录中包含有压缩摘要消息，且压缩摘要消息包含summaryMessageIds，" +
					"以下情况可以使用该工具查询摘要消息对应的原始消息：\r\n" +
					"1. 了解或者回顾智能体之前都做了什么\r\n" +
					"2. 需从历史消息中获取与用户问题相关的关键事实\r\n" +
					"3. 完成当前任务，当前上下文中信息不全，需要从压缩摘要消息对应的原始消息内容中了解更多信息\r\n"
	)
	public String summarySearch(
			@ToolParam(name = "summary_message_ids",
					description = "压缩摘要消息ID列表，不能为空",
					required = true)
			String[] summaryMessageIds) {
		
		// 1. 参数校验
		if (summaryMessageIds == null || summaryMessageIds.length == 0) {
			return "参数校验失败：没有需要查询的压缩摘要消息ID列表。";
		}
		
		// 过滤空字符串 / null 元素
		List<String> validIds = Arrays.stream(summaryMessageIds)
				.filter(Objects::nonNull)
				.map(String::trim)
				.filter(id -> !id.isEmpty())
				.distinct()
				.collect(Collectors.toList());
		
		if (validIds.isEmpty()) {
			return "参数校验失败：压缩摘要消息ID列表为空或全部为无效ID。";
		}
		
		try {
			// 2. 获取上下文
			ChatObject chatObject = AgentTraceHolder.getChatObject();
			if (chatObject == null || chatObject.getAgent() == null) {
				return "查询失败：当前会话上下文不存在，无法获取智能体信息。";
			}
			AIAgent agent = chatObject.getAgent();
			String sessionId = agent.getSessionId();
			if (sessionId == null || sessionId.isEmpty()) {
				return "查询失败：当前会话ID为空，无法查询摘要消息。";
			}
			
			// 3. 查询原始会话消息
			List<SessionMessage> sessionMessages = agent.getMainSessionStore()
					.getSessionMessages(sessionId, validIds.toArray(new String[0]));
			
			if (sessionMessages == null || sessionMessages.isEmpty()) {
				return String.format("未查询到对应的会话消息，请求的摘要消息ID列表：%s", validIds);
			}
			
			// 4. 格式化返回
			String formatted = SummeryUtils.formatSessionMessagesForSummary(sessionMessages);
			if (formatted == null || formatted.trim().isEmpty()) {
				return "查询到会话消息，但格式化结果为空。";
			}
			
			// 5. 截断保护，避免返回内容过长导致模型上下文溢出
			return truncateIfNeeded(formatted);
			
		} catch (Exception e) {
			// 6. 异常兜底，避免工具异常导致上层流程中断
			return "查询压缩摘要消息对应的原始消息时发生异常：" + e.getMessage();
		}
	}
	
	/**
	 * 返回内容最大字符数限制
	 */
	private static final int MAX_RETURN_LENGTH = 8000;
	
	/**
	 * 返回内容截断提示保留的尾部说明长度余量
	 */
	private static final int TRUNCATE_HINT_RESERVE = 100;
	
	/**
	 * 如果内容超过最大长度限制，则进行截断，并在末尾追加提示信息。
	 * 截断时会尽量在换行符处断开，避免切断单条消息的可读性。
	 *
	 * @param content 原始内容
	 * @return 截断后的内容（若未超限则原样返回）
	 */
	private String truncateIfNeeded(String content) {
		if (content == null || content.length() <= MAX_RETURN_LENGTH) {
			return content;
		}
		
		// 预留提示信息所占长度，保证最终结果不超过 MAX_RETURN_LENGTH
		int limit = MAX_RETURN_LENGTH - TRUNCATE_HINT_RESERVE;
		if (limit <= 0) {
			limit = MAX_RETURN_LENGTH / 2;
		}
		
		// 优先在换行符处断开
		int cutIndex = content.lastIndexOf('\n', limit);
		if (cutIndex <= 0) {
			cutIndex = limit;
		}
		
		String truncated = content.substring(0, cutIndex);
		
		String hint = String.format(
				"\n...(内容过长已截断，仅展示前 %d 字符，原始共 %d 字符，如需更多信息请缩小查询的 summaryMessageIds 范围)",
				cutIndex, content.length());
		
		return truncated + hint;
	}
}
