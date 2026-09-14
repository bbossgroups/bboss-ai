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

import org.frameworkset.spi.ai.model.LinkedMessageMap;
import org.frameworkset.spi.ai.store.SessionMessage;
import org.frameworkset.spi.ai.util.MessageBuilder;

import java.util.List;
import java.util.Map;

/**
 *
 * @author biaoping.yin
 * @Date 2026/9/10
 */
public class SessionUtils {
	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SessionUtils.class);
	 
	public static void buildSearchableText(List<String> results, SessionMessage sessionMessage,String lowerQuery) {
		LinkedMessageMap<String, Object> entry = sessionMessage.getMessage();
		
		String role = (String) entry.get("role");
//		String content = searchableText(entry);
//		if (content != null && content.toLowerCase().contains(lowerQuery)) {
//			String preview =
//					content.length() > 200 ? content.substring(0, 200) + "..." : content;
//			String roleLabel =
//					entry instanceof SessionEntry.MessageEntry me
//							? me.getRole()
//							: entry instanceof SessionEntry.ToolUseEntry
//							? "TOOL_USE"
//							: entry instanceof SessionEntry.ToolResultEntry
//							? "TOOL_RESULT"
//							: entry.getClass().getSimpleName();
//			results.add(
//					String.format(
//							"  [%s] %s — [%s]: %s",
//							relPath, entry.getId(), roleLabel, preview));
//		}
		String content = null;
		String roleLabel = null;
		if (!role.equals(MessageBuilder.ROLE_TOOL)) {
			List<Map<String,Object>> toolCalls = (List<Map<String,Object>>) entry.get("tool_calls");
			
			if(toolCalls != null && !toolCalls.isEmpty()){
				// Append tool calls if any
				StringBuilder sb = new StringBuilder();
				for(Map toolCall : toolCalls){
					if(sb.length() > 0) sb.append("\n");
					Map<String, Object> function = (Map<String, Object>) toolCall.get("function");
					String name = (String) function.get("name");
					String arguments = (String) function.get("arguments");
					sb.append(name).append(" ").append(arguments != null ? arguments : "");
//					texts.add(sb.toString());
//					sb.setLength(0);
//				sb.append(renderToolUse(toolCall));
				}
				content = sb.toString();
				roleLabel = "TOOL_USE";
				
			}
			else {
				content = (String) entry.get("content");
				roleLabel = sessionMessage.getRole();
			}
		}
		else{
			content = (String) entry.get("content");
			roleLabel = "TOOL_RESULT";
		}
		if(content != null && !content.isEmpty()) {
			if (lowerQuery.equals("") || content.toLowerCase().contains(lowerQuery)) {
				String preview =
						content.length() > 200 ? content.substring(0, 200) + "..." : content;
				results.add(String.format(
						"  [%s] %s — [%s]: %s",
						sessionMessage.getSessionId(), sessionMessage.getMsgId(), roleLabel, preview));
			}
		}
		else{
			log.debug("content is empty");
		}
//		if (entry instanceof SessionEntry.ToolUseEntry use) {
//			return use.getName() + " " + (use.getInput() != null ? use.getInput().toString() : "");
//		}
//		if (entry instanceof SessionEntry.ToolResultEntry result) {
//			return (result.getName() != null ? result.getName() + " " : "")
//					+ (result.getOutput() != null ? result.getOutput() : "");
//		}
	}
	 
}
