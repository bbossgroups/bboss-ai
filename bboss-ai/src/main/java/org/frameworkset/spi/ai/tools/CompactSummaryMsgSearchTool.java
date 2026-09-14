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

import java.util.List;

/**
 * 压缩摘要查询工具
 * @author biaoping.yin
 * @Date 2026/9/13
 */
public class CompactSummaryMsgSearchTool {
	@Tool(name = "summary_search",readOnly = true, description = "查询压缩摘要消息对应的原始消息。" +
			"如果当前会话消息记录中包含有压缩摘要消息，且压缩摘要消息包含summaryMessageIds，以下情况可以使用该工具查询摘要消息对应的原始消息：\r\n" +
			"1.了解或者回顾智能体之前都做了什么\r\n" +
			"2.需从历史消息中获取与用户问题相关的关键事实\r\n" 
			+"3.完成当前任务，当前上下文中信息不全，需要从压缩摘要消息对应的原始消息内容中了解更多信息\r\n" )
	public String summarySearch(@ToolParam(name = "summary_message_ids", 
			description = "压缩摘要消息ID列表",required = true) 
									String[] summaryMessageIds){
		if(summaryMessageIds == null || summaryMessageIds.length == 0)
			return "没有需要查询的压缩摘要消息ID列表";
		ChatObject chatObject = AgentTraceHolder.getChatObject();
		AIAgent agent = chatObject.getAgent();
		String sessionId = agent.getSessionId();
		
		List<SessionMessage> sessionMessages = agent.getMainSessionStore().getSessionMessages(sessionId, summaryMessageIds);
		if(sessionMessages == null || sessionMessages.size() == 0)
			return "没有找到对应的会话消息";
		else
			return SummeryUtils.formatSessionMessagesForSummary(sessionMessages);
		
		
	}
}
