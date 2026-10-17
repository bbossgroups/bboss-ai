package org.frameworkset.spi.ai.interceptor.impl;
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
import org.frameworkset.spi.ai.context.AgentRuntimeContext;
import org.frameworkset.spi.ai.filesystem.*;
import org.frameworkset.spi.ai.filesystem.local.LocalFilesystemWithShell;
import org.frameworkset.spi.ai.filesystem.model.LocalFsMode;
import org.frameworkset.spi.ai.filesystem.sandbox.AbstractSandboxFilesystem;
import org.frameworkset.spi.ai.interceptor.AgentInterceptor;
import org.frameworkset.spi.ai.model.ChatObject;
import org.frameworkset.spi.ai.model.memory.AgentMemory;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author biaoping.yin
 * @Date 2026/10/14
 */
public class HarnessAgentInterceptor implements AgentInterceptor {
	private static final String SESSION_CONTEXT_SECTION_TEMPLATE =
			"## Agent Runtime Context\n" +
					"This is the %s. We are setting up the context for our chat.\n" +
					"Today's date is %s.\n" +
					"My operating system is: %s\n" +
					"The workspace directory is: %s\n" +
					"The project's temporary directory is: %s\n" +
					"%s\n";
	
	private static final String DOMAIN_KNOWLEDGE_GUIDANCE =
			"## Domain Knowledge\n" +
					"The workspace `knowledge/` tree holds many detailed reference documents (not only a single summary file). " +
					"When the task needs specs, procedures, schemas, or domain facts, treat that directory as the source of truth.\n" +
					"Below, `<domain_knowledge_context>` already includes what you need to navigate it: " +
					"injected `knowledge/KNOWLEDGE.md` (if present) plus a **full list of knowledge file paths** under `knowledge/` — " +
					"use that as the catalog of what exists and where.\n" +
					"For content not inlined here, open only the paths you need with read_file, grep, or glob " +
					"(prefer targeted reads over loading entire trees into the reply).\n";
	
	private static final String MEMORY_RECALL_GUIDANCE =
			"## Memory Recall\n" +
					"Before answering questions about prior work, decisions, dates, people, or preferences: " +
					"run memory_search on MEMORY.md + memory/*.md, then memory_get for needed lines. " +
					"Include Source: <path#line> citations when helpful.\n";
	
	private static final String MEMORY_PERSISTENCE_HEADER =
			"## Memory Persistence\n" +
					"You have a persistent MEMORY.md in db store. Update it proactively when:\n" +
					"- User shares preferences, project context, or decisions\n" +
					"- Important outcomes or action items are established\n";
	
	private static final String MEMORY_PERSISTENCE_HEADER_HOOKS_ONLY =
			"## Memory Persistence\n" +
					"You have a persistent MEMORY.md that the harness maintains automatically.\n";
	
	private static final String MEMORY_SAVE_TOOL_GUIDANCE =
			"Use the **memory_save** tool to persist memories — it atomically updates " +
					"both MEMORY.md and the daily ledger. Do NOT use write_file or edit_file on " +
					"MEMORY.md or any path under memory/ — always use memory_save instead.\n";
	
	private static final String MEMORY_WRITE_FILE_GUARD =
			"Do NOT use write_file or edit_file on MEMORY.md or any path under memory/ — " +
					"the harness owns those files.\n";
	
	private static final String MEMORY_AUTO_EXTRACT_GUIDANCE =
			"Memory is also automatically extracted at conversation end.\n";
	
//	private static final String WORKSPACE_FILES_NOTICE_WITH_MEMORY =
//			"## Workspace Files (Injected)\n" +
//					"The following <loaded_context> was loaded in from files in your workspace.\n" +
//					"These files (for example, `AGENTS.md`,  and `knowledge/KNOWLEDGE.md`) contain guidelines,user-specific details and domain context for this agent.\n" +
//					"These datas (for example, `MEMORY.md`,  and `memory/YYYY-MM-DD.md`) contain memory, facts, preferences and guidelines learned from prior interactions with user.\n";
//	
private static final String WORKSPACE_FILES_NOTICE_WITH_MEMORY =
		"## Workspace Files (Injected)\n" +
				"The following <loaded_context> was loaded in from files in your workspace.\n" +
				"These files (for example, `AGENTS.md`,  and `knowledge/KNOWLEDGE.md`) contain guidelines,user-specific details and domain context for this agent.\n" +
				"## Memory Data (Injected)\n" +
				"The following memory data was injected from a relational database or ClickHouse, NOT from files in your workspace.\n" +
				"This memory data (for example, `MEMORY.md`,  and `memory/YYYY-MM-DD.md`) contains memory, facts, preferences and guidelines learned from prior interactions with user.\n" +
				"## Memory Context Tag\n" +
				"Within <loaded_context>, the <memory_context> tag is reserved exclusively for memory data loaded from the memory store (a relational database or ClickHouse). It does NOT contain workspace files.\n" +
				"## Session History and Summaries\n" +
				"Historical session messages and session compression summaries are stored in a relational database or ClickHouse, NOT in your workspace.\n" +
				"You can retrieve message records within the current session using the `current_session_search` tool.\n" +
				"You can retrieve historical session message records for the current user using the `user_sessions_search` tool.\n" +
				"You can retrieve the original message content corresponding to the message ids contained in the <summaryMessageIds> tag of a session summary using the `summary_search` tool.\n";
	private static final String WORKSPACE_FILES_NOTICE_WITHOUT_MEMORY =
			"## Workspace Files (Injected)\n" +
					"The following <loaded_context> was loaded in from files in your workspace.\n" +
					"These files (for example, `AGENTS.md` and `knowledge/KNOWLEDGE.md`) contain guidelines and domain context for this agent.\n";
	
	private static final String TRUNCATION_NOTICE_WITH_SEARCH =
			"\n\n... (memory truncated — use memory_search for older entries) ...\n";
	
	private static final String TRUNCATION_NOTICE_PLAIN =
			"\n\n... (memory truncated) ...\n";
	
	private static final int DEFAULT_MAX_CONTEXT_TOKENS = 8000;
	private   String environmentMemory;
	private   int maxContextTokens = DEFAULT_MAX_CONTEXT_TOKENS;
	private WorkspaceManager workspaceManager;
	public HarnessAgentInterceptor(WorkspaceManager workspaceManager){
		this.workspaceManager = workspaceManager;
	}
	/**
	 * Transform the system prompt string (pipeline pattern).
	 *
	 * <p>Multiple middlewares are applied sequentially; each receives the
	 * output of the previous one.
	 *
	 * @param agent         the agent instance
	 * @param chatObject           per-call runtime context (session, user, attributes)
	 * @param systemPrompt the current system prompt
	 * @return the (possibly transformed) system prompt
	 */
	@Override
	public String onSystemPrompt(AIAgent agent, ChatObject chatObject, String systemPrompt) {
						 
							 
			String base = systemPrompt != null ? systemPrompt : "";
				String section = buildWorkspaceSection(agent, chatObject);
			String separator = base.isEmpty() || base.endsWith("\n") ? "" : "\n";
			return base + separator + section;
					 
			 
	}
	private String buildWorkspaceSection(AIAgent agent, ChatObject chatObject) {
		WorkspaceManager workspaceManager = agent.getMainSessionStore().getWorkspaceManager();
		String agentsContent = workspaceManager.readAgentsMd(  agent,   chatObject).trim() ;
		boolean includeMemoryContext = includeMemoryContext(chatObject);
		String memoryContent = null;
		if(includeMemoryContext){
			AgentMemory agentMemory = agent.getMainSessionStore().getAgentSessionService().getMemory(agent.getAgentId(), agent.getUserId());
			if(agentMemory != null){
				memoryContent = agentMemory.getContent();
			}
		}
		else{
			memoryContent = "";//workspaceManager.readMemoryMd(rc).strip();	
		}
		String knowledgeContent = workspaceManager.readKnowledgeMd(agent,   chatObject).trim();
		String sessionContext = buildSessionContextSection(agent , chatObject	);
		Path workspace = workspaceManager.getWorkspace();
		String knowledgeBlock = buildKnowledgeBlock(agent,   chatObject, knowledgeContent, workspace);
		String additionalBlock = buildAdditionalContextBlock(agent,   chatObject);
		
		int fixedTokens =
				estimateTokens(sessionContext)
						+ estimateTokens(agentsContent)
						+ estimateTokens(knowledgeBlock)
						+ estimateTokens(additionalBlock);
		if (includeMemoryContext) {
			int memoryTokens = estimateTokens(memoryContent);
			int available = maxContextTokens - fixedTokens;
			if (available > 0 && memoryTokens > available) {
				memoryContent = truncateToTokenBudget(memoryContent, available);
			}
		}
		
		String workspaceParagraph =
				buildWorkspaceParagraph(
						workspace,  workspaceManager.getFilesystem(), false);
		String loadedContext =
				buildLoadedContextSection(chatObject,
						agentsContent, memoryContent, knowledgeBlock, additionalBlock);
		return assembleSection(sessionContext, buildGuidance(), workspaceParagraph, loadedContext);
	}
	
	/**
	 * Inject {@code MEMORY.md} unless both memory tools and hooks are disabled — at that point
	 * the harness memory surface is fully off and the file should not appear as model context.
	 */
	private boolean includeMemoryContext( ChatObject chatObject) {
		AgentRuntimeContext agentRuntimeContext = chatObject.getChatContext().getAgentRuntimeContext();
		 
		return !(agentRuntimeContext.isDisableMemoryTools() && agentRuntimeContext.isDisableMemoryHooks()	);
	}
	
	private String buildGuidance() {
		StringBuilder sb = new StringBuilder();
		sb.append(DOMAIN_KNOWLEDGE_GUIDANCE.trim()).append("\n\n");
		if (!workspaceManager.isDisableMemoryTools()) {
			sb.append(MEMORY_RECALL_GUIDANCE.trim()).append("\n\n");
		}
		String persistence = buildMemoryPersistenceGuidance();
		if (!persistence.isEmpty()) {
			sb.append(persistence.trim()).append("\n\n");
		}
		return workspaceManager.stripTrailing(sb.toString()) + "\n";
	}
	
	private String buildMemoryPersistenceGuidance() {
		if (workspaceManager.isDisableMemoryTools() && workspaceManager.isDisableMemoryHooks()	) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		if (!workspaceManager.isDisableMemoryTools()) {
			sb.append(MEMORY_PERSISTENCE_HEADER.trim()).append("\n");
			sb.append(MEMORY_SAVE_TOOL_GUIDANCE.trim()).append("\n");
		} else {
			sb.append(MEMORY_PERSISTENCE_HEADER_HOOKS_ONLY.trim()).append("\n");
			sb.append(MEMORY_WRITE_FILE_GUARD.trim()).append("\n");
		}
		if (!workspaceManager.isDisableMemoryHooks()) {
			sb.append(MEMORY_AUTO_EXTRACT_GUIDANCE.trim()).append("\n");
		}
		return sb.toString();
	}
	
	private static String assembleSection(
			String sessionContext,
			String guidance,
			String workspaceParagraph,
			String loadedContextSection) {
		StringBuilder sb = new StringBuilder();
		if (!sessionContext.isEmpty()) {
			sb.append(sessionContext).append("\n\n");
		}
		sb.append(guidance);
		if (!workspaceParagraph.isEmpty()) {
			sb.append("\n").append(workspaceParagraph);
		}
		sb.append("\n").append(loadedContextSection);
		return sb.toString();
	}
	
	/**
	 * Builds the {@code ## Workspace} paragraph, branching by the active filesystem type so the
	 * LLM sees a description that matches its real deployment surface.
	 *
	 * <ul>
	 *   <li><b>Local overlay</b> ({@link OverlayFilesystem} wrapping
	 *       {@link LocalFilesystemWithShell}) — renders Project + Workspace as two lines plus
	 *       overlay/shell semantics.
	 *   <li><b>Sandbox</b> ({@link AbstractSandboxFilesystem} not wrapped in an overlay) —
	 *       describes the isolated container view and how host files reach it.
	 *   <li><b>Remote</b> ({@link CompositeFilesystem}) — describes the distributed store-backed
	 *       workspace and the fact that there is no host filesystem to fall back to.
	 *   <li><b>Other</b> — single-line legacy "working directory is X" form for plain
	 *       {link LocalFilesystem} or anything we
	 *       don't recognize.
	 * </ul>
	 */
	private static String buildWorkspaceParagraph(Path workspace, AbstractFilesystem filesystem,		 
			boolean artifactDeliveryEnabled) {
		StringBuilder sb = new StringBuilder("## Workspace\n");
		LocalFilesystemWithShell localUpper = detectLocalUpper(filesystem);
		Path project = localUpper != null ? localUpper.getShellCwd() : null;
//		Path project = workspace;
		if (project != null) {
			sb.append("Project (the user's source tree you're assisting with): ")
					.append(project.toAbsolutePath())
					.append("\n");
			sb.append("Workspace (your home base —  skills data): ")
					.append(workspace.toAbsolutePath())
					.append("\n");
			List<Path> extraRoots = extraRootsOf(localUpper, project, workspace);
			if (!extraRoots.isEmpty()) {
				sb.append("Additional roots: ");
				for (int i = 0; i < extraRoots.size(); i++) {
					if (i > 0) {
						sb.append(", ");
					}
					sb.append(extraRoots.get(i).toAbsolutePath());
				}
				sb.append("\n");
			}
			LocalFsMode mode = localUpper.getMode();
			sb.append("Path access policy: ")
					.append(describeMode(mode))
					.append(". File tools reject absolute paths outside the roots above")
					.append(mode == LocalFsMode.ROOTED ? " with a security error" : "")
					.append(".\n");
			if (filesystem instanceof ProjectAwareOverlay) {
				sb.append(
						"File tools write project files (code, configs, etc.) to the project"
								+ " directory. Workspace metadata paths ( skills,"
								+ " agents, knowledge) are written to the workspace.\n");
			} else {
				sb.append(
						"Relative paths resolve under the workspace and read-fall-back to"
								+ " the project (overlay copy-on-write).\n");
			}
			sb.append("Shell commands run with `pwd` set to the project directory.\n");
			} else if (filesystem instanceof AbstractSandboxFilesystem 
				&& !(filesystem instanceof OverlayFilesystem)) {
			AbstractSandboxFilesystem sandbox = (AbstractSandboxFilesystem) filesystem;	
			sb.append("Sandbox root: /workspace (container id: ")
					.append(sandbox.id())
					.append(")\n");
			if (artifactDeliveryEnabled) {
				sb.append(
						"Files are isolated inside this container. The host filesystem is not"
								+ " directly accessible — see the File Isolation Notice below for"
								+ " how to deliver files out of the sandbox.\n");
			} else {
				sb.append(
						"Files are isolated inside this container. The host filesystem is not"
								+ " accessible and there is no mechanism for moving files across"
								+ " the boundary.\n");
			}
		} else if (filesystem instanceof CompositeFilesystem) {
			sb.append("Distributed workspace template root: ")
					.append(workspace.toAbsolutePath())
					.append("\n");
			sb.append(
					"Runtime data ( skills) lives in a shared remote"
							+ " store, not on the local host. Reads of project-authored template"
							+ " files fall back to the workspace template root above.\n");
		} else {
			sb.append("Your working directory is: ")
					.append(workspace.toAbsolutePath())
					.append("\n");
			sb.append(
					"Treat this directory as the single global workspace for file operations"
							+ " unless explicitly instructed otherwise.\n");
		}
		sb.append(
				"AGENTS.md defines persona and local conventions — honor them when consistent"
						+ " with safety and policy.\n");
		if (artifactDeliveryEnabled
				&& filesystem instanceof AbstractSandboxFilesystem
				&& !(filesystem instanceof OverlayFilesystem)) {
			sb.append(
					"**File Isolation Notice**\n"
							+ "Files inside this container are isolated from the host filesystem"
							+ " and are not directly accessible from outside. If your work"
							+ " produces any final deliverables—such as documents, reports,"
							+ " images, spreadsheets, archives, audio/video files, code"
							+ " artifacts, or similar—you **must** call deliver_artifact"
							+ " automatically when you finish the task to export them to their"
							+ " configured external destination. Deliver it silently: do not ask"
							+ " the user whether they want it delivered — the tool call itself lets"
							+ " the user see and retrieve the artifact directly, so do not mention"
							+ " the delivery or the deliver_artifact tool in your reply.\n"
							+ "\n"
							+ "**Important Notes**:\n"
							+ "- Only deliver the final output of your task. **Do not** deliver"
							+ " temporary files, working copies, internal intermediate files, or"
							+ " any sensitive information (e.g., credentials, keys, personal"
							+ " data).\n"
							+ "- Do not simply print the file path as a reference; the user"
							+ " cannot access your container's filesystem directly.\n");
		}
		return sb.toString();
	}
	
	/**
	 * Best-effort: returns the upper {@link LocalFilesystemWithShell} when {@code fs} is an
	 * overlay constructed by {@code LocalFilesystemSpec}, otherwise {@code null}. Used to pull
	 * project / mode / policy metadata for the prompt without leaking those into other
	 * filesystem types.
	 */
	private static LocalFilesystemWithShell detectLocalUpper(AbstractFilesystem fs) {
		if (fs instanceof OverlayFilesystem  
				) {
			AbstractFilesystem upper = ((OverlayFilesystem) fs).getUpper();
			if (upper instanceof LocalFilesystemWithShell ) {
				return (LocalFilesystemWithShell) upper;
			}
		}
		return null;
	}
	
	/**
	 * Extra allow-list roots beyond the project and workspace (which the LLM already sees).
	 * Filters out exact matches and ancestors to keep the prompt focused on truly additional
	 * locations.
	 */
	private static List<Path> extraRootsOf(
			LocalFilesystemWithShell upper, Path project, Path workspace) {
		PathPolicy policy = upper.getPathPolicy();
		if (policy == null || policy.isEmpty()) {
			return Collections.emptyList();
		}
		Path projectAbs = project.toAbsolutePath().normalize();
		Path workspaceAbs = workspace.toAbsolutePath().normalize();
		List<Path> extras = new java.util.ArrayList<>();
		for (Path root : policy.roots()) {
			if (root.equals(projectAbs) || root.equals(workspaceAbs)) {
				continue;
			}
			extras.add(root);
		}
		return extras;
	}
	
	private static String describeMode(LocalFsMode mode) {
		if (mode == null) {
			return "ROOTED (default)";
		}
		switch (mode) {
			case SANDBOXED:return "SANDBOXED (all paths anchored to the workspace; `..` blocked)";
			case ROOTED:return "ROOTED (absolute paths accepted only inside the roots above)";
			case UNRESTRICTED: return"UNRESTRICTED (absolute paths pass through unchanged)";
			default: return"ROOTED (default)";
		}
	}
	
	private String buildSessionContextSection(AIAgent agent,ChatObject chatObject) {
		String today = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE MMM d, yyyy"));
		String platform = System.getProperty("os.name") + " " + System.getProperty("os.version");
		String tempDir = System.getProperty("java.io.tmpdir");
		String dynamicPart = buildSessionDynamicPart(  agent,  chatObject);
		String workspace = workspaceManager.getWorkspace() != null?
				workspaceManager.getWorkspace().toAbsolutePath().toString() : ""	;
		return String.format(
						SESSION_CONTEXT_SECTION_TEMPLATE,
						agent.getAgentName(),
						today,
						platform,
						workspace,
						tempDir,
						dynamicPart)
				.trim();
	}
	
	private String buildSessionDynamicPart(AIAgent agent,ChatObject chatObject) {
		List<String> parts = new ArrayList<>();
		if (agent != null && agent.getSessionId() != null) {
			parts.add("Agent Session ID: " + agent.getSessionId());
		}
		if (environmentMemory != null && !environmentMemory.isEmpty()) {
			parts.add(environmentMemory);
		}
		return parts.isEmpty() ? "" : String.join("\n", parts);
	}
	
	private String buildLoadedContextSection(ChatObject chatObject,
			String agentsContent,
			String memoryContent,
			String knowledgeBlock,
			String additionalBlock) {
		StringBuilder sb = new StringBuilder();
		sb.append(workspaceFilesNotice(  chatObject));
		sb.append("\n");
		sb.append("<loaded_context>\n");
		sb.append(buildXmlContext("agents_context", agentsContent));
		if (includeMemoryContext(  chatObject)) {
			sb.append(buildXmlContext("memory_context", memoryContent));
		}
		sb.append(buildXmlContext("domain_knowledge_context", knowledgeBlock));
		if (!additionalBlock.isEmpty()) {
			sb.append(additionalBlock);
		}
		sb.append("</loaded_context>\n");
		return sb.toString();
	}
	
	private String workspaceFilesNotice(ChatObject chatObject) {
		return includeMemoryContext(  chatObject)
				? WORKSPACE_FILES_NOTICE_WITH_MEMORY
				: WORKSPACE_FILES_NOTICE_WITHOUT_MEMORY;
	}
	
	private static String buildXmlContext(String tagName, String content) {
		if (content == null || content.isEmpty()) {
			return "  <" + tagName + "></" + tagName + ">\n";
		}
		return "  <" + tagName + ">\n" + indentByTwo(content.trim()) + "\n  </" + tagName + ">\n";
	}
	
	private static String indentByTwo(String text) {
		if (text.isEmpty()) {
			return "";
		}
		String[] lines = text.split("\\R", -1); // -1 保留末尾空行
		if (lines.length > 0 && lines[lines.length - 1].isEmpty()) {
			lines = Arrays.copyOf(lines, lines.length - 1); // 去掉最后的空串，模拟 lines() 行为
		}
		return Arrays.stream(lines)
				.map(line -> "  " + line)
				.collect(Collectors.joining("\n"));
//		return text.lines().map(line -> "  " + line).collect(Collectors.joining("\n"));
	}
	
	private String buildAdditionalContextBlock(AIAgent agent, ChatObject chatObject) {
		  List<String> additionalContextFiles = workspaceManager.getAdditionalContextFiles();
		if (additionalContextFiles.isEmpty()) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (String relPath : additionalContextFiles) {
			String content = workspaceManager.readManagedWorkspaceFileUtf8(chatObject, relPath);
			if (content != null && !content.isEmpty()) {
				String tag = relPath.replace("/", "_").replace(".", "_").toLowerCase();
				sb.append("  <").append(tag).append(">\n");
				sb.append(indentByTwo(content.trim())).append("\n");
				sb.append("  </").append(tag).append(">\n");
			}
		}
		return sb.toString();
	}
	
	private static int estimateTokens(String text) {
		return text == null || text.isEmpty() ? 0 : text.length() / 4;
	}
	
	private String truncateToTokenBudget(String text, int maxTokens) {
		int maxChars = maxTokens * 4;
		if (text.length() <= maxChars) {
			return text;
		}
		String notice =
				workspaceManager.isDisableMemoryTools() ? TRUNCATION_NOTICE_PLAIN : TRUNCATION_NOTICE_WITH_SEARCH;
		return text.substring(0, maxChars) + notice;
	}
	
	private String buildKnowledgeBlock(AIAgent agent, ChatObject chatObject, String knowledgeContent, Path workspace) {
		WorkspaceManager workspaceManager = agent.getMainSessionStore().getWorkspaceManager();
		List<Path> knowledgeFiles = workspaceManager.listKnowledgeFiles(  agent,   chatObject);
		StringBuilder sb = new StringBuilder();
		
		if (!knowledgeContent.isEmpty()) {
			sb.append(knowledgeContent.trim()).append("\n");
		}
		
		if (!knowledgeFiles.isEmpty()) {
			if (sb.length() > 0) {
				sb.append("\n");
			}
			sb.append("Knowledge files:\n");
			sb.append(
					knowledgeFiles.stream()
							.map(f -> "- " + workspace.relativize(f))
							.collect(Collectors.joining("\n")));
			sb.append("\n");
		}
		
		return sb.toString();
	}
}
