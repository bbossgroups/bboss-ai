/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.frameworkset.spi.ai.tools;

 
import org.frameworkset.spi.ai.AIAgent;
import org.frameworkset.spi.ai.model.ChatObject;
import org.frameworkset.spi.ai.model.annotation.Tool;
import org.frameworkset.spi.ai.model.annotation.ToolParam;
import org.frameworkset.spi.ai.model.memory.AgentDayMemory;
import org.frameworkset.spi.ai.model.memory.AgentMemory;
import org.frameworkset.spi.ai.store.AgentSessionService;
import org.frameworkset.spi.ai.tool.AgentTraceHolder;
import org.frameworkset.spi.ai.tools.util.KeywordMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.StringJoiner;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Tool for searching through persisted memories (MEMORY.md and memory/*.md files).
 *
 * <p>Uses keyword-based search through all memory files visible via the configured
 * {link AbstractFilesystem} (works across Local,
 * Sandbox, and Store stores).
 */
public class MemorySearchTool {

    private static final Logger log = LoggerFactory.getLogger(MemorySearchTool.class);


    public MemorySearchTool( ) {
        
    }

    @Tool(
            name = "memory_search",
            readOnly = true,
            description =
					"Search through long-term memory files (MEMORY.md and memory/*.md) for"
							+ " relevant information. Use before answering questions about prior"
							+ " work, decisions, dates, people, preferences, or todos.")
    public String memorySearch(
			@ToolParam(
					name = "query",
					description =
							"Literal phrase, or whitespace-separated keywords when"
									+ " matchMode is all/any; no automatic Chinese word"
									+ " segmentation")
			String query,
			@ToolParam(
					name = "matchMode",
					description =
							"phrase (default): exact substring; all: every keyword in the"
									+ " same memory line; any: at least one keyword in that"
									+ " line. Case-insensitive literal matching.",
					required = false)
			String matchMode) {
        if (query == null || query.isEmpty()) {
            return "No query provided";
        }
		Predicate<String> matcher;
		try {
			matcher =
					KeywordMatcher.compile(
							query,
							matchMode,
							term ->
									Pattern.compile(Pattern.quote(term), Pattern.CASE_INSENSITIVE)
											.asPredicate());
		} catch (IllegalArgumentException e) {
			return "Error: " + e.getMessage();
		}
        return keywordSearch( query,matcher);
    }

    private String keywordSearch(  String query,Predicate<String> matcher) {
		ChatObject chatObject = AgentTraceHolder.getChatObject();
		AIAgent agent = chatObject.getAgent();
		AgentSessionService agentSessionService = agent.getMainSessionStore().getAgentSessionService();
        StringJoiner results = new StringJoiner("\n");
        int matchCount = 0;

        List<AgentDayMemory> agentDayMemorys = agentSessionService.listAgentUserDayMemorys(agent);
        Pattern pattern = Pattern.compile(Pattern.quote(query), Pattern.CASE_INSENSITIVE);
		AgentMemory agentMemory = agentSessionService.getMemory(agent.getAgentId(), agent.getUserId());
		if(agentMemory != null){
			String content = agentMemory.getContent();
			String[] lines = content.split("\n", -1);
			for (int i = 0; i < lines.length; i++) {
				if (matcher.test(lines[i])) {
					results.add(String.format("Source: %s#%d: %s", "MEMORY.md", i + 1, lines[i]));
					matchCount++;
				}
			}
		}
        for (AgentDayMemory agentDayMemory	 : agentDayMemorys) {
            String content = agentDayMemory.getContent();
            if (content == null || content.isEmpty()) {
                continue;
            }
            String[] lines = content.split("\n", -1);
            for (int i = 0; i < lines.length; i++) {
				if (matcher.test(lines[i])) {
					results.add(String.format("Source: %s#%d: %s", agentDayMemory.getMemoryDay(), i + 1, lines[i]));
					matchCount++;
				}
                 
            }
        }

        if (matchCount == 0) {
            return "No matching memories found for: " + query;
        }
        return "Found " + matchCount + " matches:\n\n" + results;
    }
}
