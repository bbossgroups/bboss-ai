package org.frameworkset.spi.ai.filesystem;
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
import org.frameworkset.spi.ai.filesystem.local.LocalFilesystem;
import org.frameworkset.spi.ai.filesystem.model.FileInfo;
import org.frameworkset.spi.ai.filesystem.model.GlobResult;
import org.frameworkset.spi.ai.filesystem.model.ReadResult;
import org.frameworkset.spi.ai.model.ChatObject;
import org.frameworkset.spi.ai.store.StoreContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

import static org.frameworkset.spi.ai.model.WorkspaceConstants.*;

/**
 *
 * @author biaoping.yin
 * @Date 2026/10/15
 */
public class WorkspaceManager {
	private static final Logger log = LoggerFactory.getLogger(WorkspaceManager.class);
	private AbstractFilesystem filesystem;
	
	private   boolean disableMemoryTools;
	private   boolean disableMemoryHooks;
	private  Path workspace;
	
	private List<String> additionalContextFiles = Collections.emptyList();
	public WorkspaceManager( ){
		
	}
	
	/**
	 * Reads a UTF-8 file under the workspace, using the two-layer pattern:
	 * filesystem first, then local disk fallback.
	 */
	public String readManagedWorkspaceFileUtf8(ChatObject chatObject, String relativePath) {
		if (relativePath == null || relativePath.isEmpty()) {
			return "";
		}
		String normalized = normalizeRelativePath(relativePath);
		if (normalized.isEmpty()) {
			return "";
		}
		if(workspace == null){
			return "";
		}
		Path resolved = workspace.resolve(normalized).normalize();
		if (!resolved.startsWith(workspace)) {
			return "";
		}
		return readWithOverride(chatObject, normalized);
	}
	public void init(StoreContext storeContext,AIAgent agent){
		String workspace = agent.getAgentRuntimeContext().getWorkspace();
		this.additionalContextFiles = storeContext.getAdditionalContextFiles();
		if(workspace != null) {
			this.workspace = Paths.get(workspace);
		}
		else{
			this.workspace = Paths.get(".bboss");
		}
		if(storeContext.getFilesystem() != null){
			filesystem = storeContext.getFilesystem();
		}
		else {
			if(workspace != null)
				filesystem = new LocalFilesystem(workspace);
		}
		this.disableMemoryTools = agent.getAgentRuntimeContext().isDisableMemoryTools();
		this.disableMemoryHooks = agent.getAgentRuntimeContext().isDisableMemoryHooks();
	}
	public AbstractFilesystem getFilesystem() {
		return filesystem;
	}
	/** Reads AGENTS.md content, returns empty string if not found. */
	public String readAgentsMd(AIAgent agent, ChatObject chatObject) {
		AgentRuntimeContext agentRuntimeContext = agent.getAgentRuntimeContext();
		return readWithOverride(chatObject, AGENTS_MD);
	}
	
	/** Reads KNOWLEDGE.md content from the knowledge directory. */
	public String readKnowledgeMd(AIAgent agent, ChatObject chatObject) {
		return readWithOverride(chatObject, KNOWLEDGE_DIR + "/" + KNOWLEDGE_MD);
	}
	/**
	 * Two-layer read: filesystem first (namespaced by {@link
	 * NamespaceFactory}), local disk fallback.
	 */
	private String readWithOverride(ChatObject chatObject , String relativePath) {
		String fsContent = readTextThroughFilesystem(chatObject, relativePath);
		if (!fsContent.isEmpty()) {
			return fsContent;
		}
		if(workspace == null){
			return "";
		}
		return readFileQuietly(workspace.resolve(relativePath));
	}
	
	private String readFileQuietly(Path path) {
		if (!Files.isRegularFile(path)) {
			return "";
		}
		try {
			return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
		} catch (IOException e) {
			log.warn("Failed to read {}: {}", path, e.getMessage());
			return "";
		}
	}
	private String readTextThroughFilesystem(ChatObject chatObject , String filePath) {
		if (filesystem == null) {
			return "";
		}
		ReadResult r = filesystem.read(chatObject.getChatContext(), filePath, 0, 0);
		if (!r.isSuccess() || r.getFileData() == null) {
			return "";
		}
		String c = r.getFileData().getContent();
		return c != null ? c : "";
	}
	
	public Path getWorkspace() {
		return workspace;
	}
	static String normalizeRelativePath(String relativePath) {
		if (relativePath == null || relativePath.isEmpty()) {
			return "";
		}
		String s = stripLeading(relativePath.replace('\\', '/'));
		while (s.startsWith("/")) {
			s = s.substring(1);
		}
		return s;
	}
	public Path getKnowledgeDir() {
		if(workspace == null){
			return null;
		}
		return workspace.resolve(KNOWLEDGE_DIR);
	}
	public List<Path> listKnowledgeFiles(AIAgent agent, ChatObject chatObject) {
		Set<String> relativePaths = new LinkedHashSet<>();
		
		if (filesystem != null) {
			GlobResult glob = filesystem.glob(chatObject.getChatContext(), "*", KNOWLEDGE_DIR);
			if (glob.isSuccess() && glob.getMatches() != null) {
				for (FileInfo fi : glob.getMatches()) {
					if (fi.getPath() != null && !fi.getPath().isEmpty()) {
						relativePaths.add(normalizeRelativePath(fi.getPath().trim()));
					}
				}
			}
		}
		if(workspace == null) return Collections.emptyList();	
		Path dir = getKnowledgeDir();
		if(dir == null) 
			return Collections.emptyList();
		if (Files.isDirectory(dir)) {
			try (Stream<Path> walk = Files.walk(dir)) {
				walk.filter(Files::isRegularFile)
						.forEach(
								p -> {
									String rel =
											workspace
													.relativize(p.normalize())
													.toString()
													.replace('\\', '/');
									relativePaths.add(rel);
								});
			} catch (IOException e) {
				log.warn("Failed to list knowledge files: {}", e.getMessage());
			}
		}
		
		List<Path> result = new ArrayList<>();
		for (String rel : relativePaths) {
			result.add(workspace.resolve(rel));
		}
		return result;
	}
	
	public boolean isDisableMemoryHooks() {
		return disableMemoryHooks;
	}
	public static String stripTrailing(String s) {
		int end = s.length();
		while (end > 0 && Character.isWhitespace(s.charAt(end - 1))) {
			end--;
		}
		return s.substring(0, end);
	}
	
	public static String stripLeading(String s) {
		int start = 0;
		while (start < s.length() && Character.isWhitespace(s.charAt(start))) {
			start++;
		}
		return s.substring(start);
	}
	public boolean isDisableMemoryTools() {
		return disableMemoryTools;
	}
	
	public List<String> getAdditionalContextFiles() {
		return additionalContextFiles;
	}
	
 
}
