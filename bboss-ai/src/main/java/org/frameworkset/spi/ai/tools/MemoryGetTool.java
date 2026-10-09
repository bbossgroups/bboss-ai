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
import org.frameworkset.spi.ai.memory.MemoryUtil;
import org.frameworkset.spi.ai.model.ChatObject;
import org.frameworkset.spi.ai.model.annotation.Tool;
import org.frameworkset.spi.ai.model.annotation.ToolParam;
import org.frameworkset.spi.ai.model.memory.AgentDayMemory;
import org.frameworkset.spi.ai.model.memory.AgentMemory;
import org.frameworkset.spi.ai.store.AgentSessionService;
import org.frameworkset.spi.ai.tool.AgentTraceHolder;

import java.util.Arrays;
import java.util.List;

/**
 * Tool for reading specific lines from memory files, typically used after
 * {@link MemoryGetTool} to fetch surrounding context.
 */
public class MemoryGetTool {


    public MemoryGetTool( ) {
    }

    @Tool(
            name = "memory_get",
            readOnly = true,
            description =
                    "Read specific lines from a long-term memory. Use after memory_search to pull"
                            + " full context around matched lines. Path is relative to workspace.")
    public String memoryGet(
            @ToolParam(
                            name = "path",
                            description =
                                    "Relative path to the memory file (e.g., MEMORY.md or"
                                            + " memory/2026-04-01.md)")
                    String path,
            @ToolParam(name = "startLine", description = "Start line number (1-based, inclusive)")
                    int startLine,
            @ToolParam(name = "endLine", description = "End line number (1-based, inclusive)")
                    int endLine) {
        if (path == null || path.isEmpty()) {
            return "Error: path is required";
        }
		//		path is MEMORY.md 访问智能体当前用户的总账
		//		path is memory/2026-04-01.md 访问智能体当前用户对应日期的流水账
		ChatObject chatObject = AgentTraceHolder.getChatObject();
		AIAgent agent = chatObject.getAgent();
		AgentSessionService agentSessionService = agent.getMainSessionStore().getAgentSessionService();
		String text = null;
		if(MemoryUtil.isDayMemoryPath(path)) {
			AgentDayMemory agentDayMemory = agentSessionService.getDayMemory(agent.getAgentId(), agent.getUserId(), path);
			if (agentDayMemory == null) {
				return "Error: day memory not found: " + path;
			}
			text = agentDayMemory.getContent();
		}
		else{
			AgentMemory agentMemory = agentSessionService.getMemory(agent.getAgentId(), agent.getUserId());
			if (agentMemory == null) {
				return "Error: memory not found: " + path;
			}
			text = agentMemory.getContent();
		}
		 
		
//        Path resolved = workspaceManager.getWorkspace().resolve(path).normalize();
//        if (!resolved.startsWith(workspaceManager.getWorkspace())) {
//            return "Error: path traversal not allowed";
//        }
//
//        RuntimeContext rc = runtimeContext != null ? runtimeContext : RuntimeContext.empty();
//        String text = workspaceManager.readManagedWorkspaceFileUtf8(rc, path);
        if (text == null || text.isEmpty()) {
            return "Error: file not found: " + path;
        }

        List<String> lines = Arrays.asList(text.split("\n", -1));
        int start = Math.max(0, startLine - 1);
        int end = Math.min(lines.size(), endLine);

        if (start >= lines.size()) {
            return "Error: startLine " + startLine + " exceeds file length " + lines.size();
        }

        StringBuilder sb = new StringBuilder();
        for (int i = start; i < end; i++) {
            sb.append(String.format("%d|%s%n", i + 1, lines.get(i)));
        }
        return sb.toString();
    }
}
