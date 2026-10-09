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
import org.frameworkset.spi.ai.model.WorkspaceConstants;
import org.frameworkset.spi.ai.model.annotation.Tool;
import org.frameworkset.spi.ai.model.annotation.ToolParam;
import org.frameworkset.spi.ai.store.AgentSessionService;
import org.frameworkset.spi.ai.tool.AgentTraceHolder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

/**
 * Dedicated tool for persisting user memories to both {@code MEMORY.md} and
 * today's daily ledger ({@code memory/YYYY-MM-DD.md}).
 *
 * <p>This is the <b>only</b> sanctioned entry point for the agent to write long-term
 * memories. The system prompt directs the LLM to use this tool instead of
 * {@code write_file}/{@code edit_file} on memory paths, eliminating the
 * non-determinism of the LLM choosing arbitrary file names or skipping the
 * daily ledger.
 *
 * <p>Writes are append-only; deduplication and size-trimming are handled by the
 * periodic {link MemoryConsolidator}.
 */
public class MemorySaveTool {


    public MemorySaveTool() {
    }

    @Tool(
            name = "memory_save",
            description =
                    "Persist one or more facts to long-term memory. Use whenever the user asks you"
                        + " to remember something, or when you observe important preferences,"
                        + " decisions, or context worth keeping across conversations. Do NOT use"
                        + " write_file or edit_file on MEMORY.md — always use this tool instead.")
    public String memorySave(
             
            @ToolParam(
                            name = "content",
                            description =
                                    "Markdown bullet list of facts to remember. Each bullet should"
                                            + " be a concise, self-contained fact. Example:\\n"
                                            + "- User prefers dark mode\\n"
                                            + "- Project deadline is 2026-07-01")
                    String content) {
        if (content == null || content.isEmpty()) {
            return "Error: content is required";
        }


        String section = "\n" + content.trim() + "\n";
		ChatObject chatObject = AgentTraceHolder.getChatObject();
		AIAgent agent = chatObject.getAgent();
		AgentSessionService agentSessionService = agent.getMainSessionStore().getAgentSessionService();
		
		agentSessionService.createOrUpdateMemory(agent, section);

        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String dailyPath = WorkspaceConstants.MEMORY_DIR + "/" + today + ".md";
        String dailyEntry =
                String.format(
                        "\n## Memory Save — %s\n%s\n", Instant.now().toString(), content.trim());
		agentSessionService.createOrUpdateDayMemory(agent	, dailyPath, dailyEntry);

//        long count = content.trim().lines().filter(l -> l.stripLeading().startsWith("-")).count();
		long count = Arrays.stream(content.trim().split("\\r\\n|\\r|\\n"))
				.filter(l -> l.replaceAll("^\\s+", "").startsWith("-"))
				.count();
        if (count == 0) {
            count = 1;
        }
        return "Saved " + count + " memor" + (count == 1 ? "y" : "ies") + " to MEMORY.md";
    }
}
