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

import org.frameworkset.spi.ai.AIAgent;
import org.frameworkset.spi.ai.compaction.SessionUtils;
import org.frameworkset.spi.ai.model.ChatObject;
import org.frameworkset.spi.ai.model.annotation.Tool;
import org.frameworkset.spi.ai.model.annotation.ToolParam;
import org.frameworkset.spi.ai.store.SessionMessage;
import org.frameworkset.spi.ai.tool.AgentTraceHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 会话检索工具
 * @author biaoping.yin
 * @Date 2026/9/13
 */
public class SessionSearchTool {
	private static final Logger log = LoggerFactory.getLogger(SessionSearchTool.class);
	
	@Tool(
			name = "current_session_search",
			readOnly = true,
			description = "Search the CURRENT session's transcript for a keyword or short phrase. "
					+ "Scope: only the current session identified by agent.getSessionId(); it does NOT "
					+ "search the user's other past sessions (use `user_sessions_search` for that). "
					+ "Matching: case-insensitive substring match against the searchable text of each "
					+ "session message (role, content, and any tool/attachment text exposed by the "
					+ "session store). Messages are scanned in their natural chronological order. "
					+ "Returns: a plain-text report containing the number of matches and, for each "
					+ "match, the session context (session id / role / timestamp when available) plus "
					+ "a short snippet around the matched keyword. If the session has no messages or "
					+ "nothing matches, returns a clear, human-readable \"no result\" message. "
					+ "Use this tool when the user asks things like \"where in this conversation did "
					+ "I mention X?\", \"find the part about Y in our current chat\", or "
					+ "\"what did I say about Z earlier in this session?\". "
					+ "Do NOT use it to search across sessions, for real-time data, or for web search. "
					+ "This tool is read-only and never modifies the session transcript."
	)
	public String currentSessionSearch(
			@ToolParam(
					name = "query",
					description = "Search query: a keyword or short phrase. Case-insensitive substring "
							+ "match. Can be empty and return current user session context messages; blank queries are allowd.",
					required = false
			) String query) {
		
		// ---- 1. 参数校验 ----
//		if (query == null || query.trim().isEmpty()) {
//			return "Error: query must not be empty. Please provide a keyword or phrase to search.";
//		}
		if (query == null ) {
//			return "Error: query must not be empty. Please provide a keyword or phrase to search.";
			log.info("query is null,and will be ignored.");
			query = "";
		}
		
		final String keyword = query.trim();
		
		// ---- 2. 上下文获取（防御 null）----
		ChatObject chatObject = AgentTraceHolder.getChatObject();
		if (chatObject == null || chatObject.getAgent() == null) {
			return "Error: no active agent context available for session search.";
		}
		AIAgent agent = chatObject.getAgent();
		
		String sessionId = agent.getSessionId();
		if (sessionId == null || sessionId.isEmpty()) {
			return "Error: current session id is not available.";
		}
		if (agent.getMainSessionStore() == null) {
			return "Error: session store is not available.";
		}
		
		// ---- 3. 拉取当前会话全部消息 ----
		List<SessionMessage> sessionMessages =
				agent.getMainSessionStore().getAllAgentSessionMessage(sessionId);
		
		if (sessionMessages == null || sessionMessages.isEmpty()) {
			return String.format(
					"No messages found in the current session (sessionId=%s).", sessionId);
		}
		
		// ---- 4. 逐条构建可搜索文本并匹配 ----
		// 统一交给 SessionUtils 处理大小写、snippet 和上下文拼接。
		List<String> results = new ArrayList<String>();
		for (SessionMessage sessionMessage : sessionMessages) {
			SessionUtils.buildMessageText(results, sessionMessage, keyword);
		}
		
		// ---- 5. 统一返回格式 ----
		if (results.isEmpty()) {
			return String.format(
					"No matches for \"%s\" in the current session (sessionId=%s, scanned %d message(s)).",
					keyword, sessionId, sessionMessages.size());
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append(String.format(
				"Found %d match(es) for \"%s\" in the current session (sessionId=%s):\n\n",
				results.size(), keyword, sessionId));
		for (String result : results) {
			sb.append(result).append('\n');
		}
		return sb.toString();
	}
 
	
	@Tool(
			name = "user_sessions_search",
			readOnly = true,
			description = "Search the CURRENT user's past session transcripts for a keyword or phrase. "
					+ "Scope: only messages belonging to the current user (agent.getUserId()) and "
					+ "the current session store; other users' or other agents' sessions are never searched. "
					+ "Matching: case-insensitive substring match against the searchable text of each "
					+ "session message (role, content, and any tool/attachment text that the session "
					+ "store exposes). The most recent `limit` sessions are scanned, newest first. "
					+ "Returns: a plain-text report containing the number of matches and, for each "
					+ "match, the session context (session id / timestamp / role) plus a short snippet "
					+ "around the matched keyword. If nothing matches, returns a clear \"no match\" message. "
					+ "Use this tool when the user asks things like \"did we discuss X before?\", "
					+ "\"find where I mentioned Y\", or \"what did I say about Z in earlier sessions?\". "
					+ "Do NOT use it for real-time data, web search, or non-session content."
	)
	public String userSessionsSearch(
			@ToolParam(
					name = "query",
					description = "Search query: a keyword or short phrase. Case-insensitive substring match. "
							+ "Can be empty; empty/blank queries are allowed, and return current users session context messages.",
					required = false
			) String query,
			@ToolParam(
					name = "limit",
					description = "Maximum number of most-recent sessions to scan. "
							+ "Default 10 when <= 0 is passed. Hard cap 100 to protect context size.",
					required = false
			) int limit
	) {
		// ---- 1. 参数校验 ----
//		if (query == null || query.trim().isEmpty()) {
//			return "Error: query must not be empty. Please provide a keyword or phrase to search.";
//		}
		
		if (query == null  ) {
//			return "Error: query must not be empty. Please provide a keyword or phrase to search.";
			log.info("query is null,and will be ignored.");
			query = "";
		}
		final String keyword = query.trim().toLowerCase();
		
		// ---- 2. limit 归一化 + 上限保护 ----
		if (limit <= 0) {
			limit = 10;
		}
		final int MAX_LIMIT = 100;
		if (limit > MAX_LIMIT) {
			limit = MAX_LIMIT;
		}
		
		// ---- 3. 上下文获取（防御 null）----
		ChatObject chatObject = AgentTraceHolder.getChatObject();
		if (chatObject == null || chatObject.getAgent() == null) {
			return "Error: no active agent context available for session search.";
		}
		AIAgent agent = chatObject.getAgent();
		String userId = agent.getUserId();
		if (userId == null || agent.getMainSessionStore() == null) {
			return "Error: user session store is not available.";
		}
		
		// ---- 4. 拉取最近 limit 条会话消息 ----
		List<SessionMessage> sessionMessages =
				agent.getMainSessionStore().getAllAgentSessionMessageOfUser(userId, limit);
		
		if (sessionMessages == null || sessionMessages.isEmpty()) {
			return String.format("No past session messages found for user (limit=%d).", limit);
		}
		
		// ---- 5. 逐条构建可搜索文本并匹配 ----
		List<String> results = new ArrayList<>();
		for (SessionMessage sessionMessage : sessionMessages) {
			SessionUtils.buildMessageText(results, sessionMessage, keyword);
		}
		
		// ---- 6. 统一返回格式 ----
		if (results.isEmpty()) {
			return String.format(
					"No matches for \"%s\" in the latest %d session(s) of the current user.",
					keyword, limit
			);
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append(String.format(
				"Found %d match(es) for \"%s\" in the latest %d session(s):\n\n",
				results.size(), keyword, limit
		));
		for (String result : results) {
			sb.append(result).append('\n');
		}
		return sb.toString();
	}
}
