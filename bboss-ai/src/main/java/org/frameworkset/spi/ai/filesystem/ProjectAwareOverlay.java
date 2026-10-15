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
package org.frameworkset.spi.ai.filesystem;

 

import org.frameworkset.spi.ai.context.ChatContext;
import org.frameworkset.spi.ai.filesystem.local.LocalFilesystem;
import org.frameworkset.spi.ai.filesystem.model.*;
import org.frameworkset.spi.ai.filesystem.sandbox.AbstractSandboxFilesystem;
import org.frameworkset.spi.ai.model.ChatObject;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Overlay variant that routes writes to the <em>project</em> directory for non-workspace paths,
 * while keeping workspace metadata (memory, sessions, skills, etc.) in the upper (workspace) layer.
 *
 * <p>Produced exclusively by {link LocalFilesystemSpec}
 * when {@code projectWritable(true)} is set. Other filesystem specs ({@code RemoteFilesystemSpec},
 * {@code SandboxFilesystemSpec}) are not affected.
 *
 * <p>Every routed path is first normalized to its agent-visible relative form: a workspace-rooted
 * absolute path (e.g. {@code <workspace>/src/Foo.java}) has the workspace root and, when namespace
 * scoping is configured, the caller's namespace prefix stripped, so the absolute and the relative
 * spelling of the same path behave identically in every operation. Absolute paths outside the
 * workspace root pass through unchanged and are never classified as workspace metadata.
 *
 * <p>Read operations retain standard overlay semantics: check upper (workspace) first, fall back
 * to lower (project). Shell {@code execute()} delegates to the upper layer as before.
 */
public class ProjectAwareOverlay extends OverlayFilesystem implements AbstractSandboxFilesystem {
	
	private static final Set<String> WORKSPACE_PREFIXES;
	
	static {
		Set<String> set = new HashSet<>();
		set.add("MEMORY.md");
		set.add("memory");
		set.add("AGENTS.md");
		set.add("agents");
		set.add("skills");
		set.add("knowledge");
		set.add("rules");
		set.add("tools.json");
		set.add("subagents");
		set.add("plans");
		set.add(".index");
		set.add(".skills-cache");
		set.add("large_tool_results");
		WORKSPACE_PREFIXES = Collections.unmodifiableSet(set);
	}

    private final AbstractSandboxFilesystem shellBackend;
    private final LocalFilesystem projectFs;
    private final Path workspaceRoot;
    private final NamespaceFactory namespaceFactory;

    /**
     * @param upper shell-capable workspace filesystem (read-write, workspace root)
     * @param lower read-only project filesystem (overlay fallback)
     * @param projectFs writable project filesystem for non-workspace writes
     * @param workspaceRoot absolute path of the workspace, used to classify absolute paths
     * @param namespaceFactory namespace factory scoping the workspace and project filesystems;
     *     used to strip the caller's namespace prefix from workspace-rooted absolute paths;
     *     {@code null} when no namespace scoping is configured
     */
    public ProjectAwareOverlay(
            AbstractSandboxFilesystem upper,
            AbstractFilesystem lower,
            LocalFilesystem projectFs,
            Path workspaceRoot,
            NamespaceFactory namespaceFactory) {
        super(upper, lower);
        this.shellBackend = upper;
        this.projectFs = projectFs;
        this.workspaceRoot = workspaceRoot.toAbsolutePath().normalize();
        this.namespaceFactory = namespaceFactory;
    }

    /**
     * Namespace-unaware variant: workspace-rooted absolute paths keep their namespace segment and
     * may misroute under namespace scoping.
     *
     * @deprecated use {@link #ProjectAwareOverlay(AbstractSandboxFilesystem, AbstractFilesystem,
     *     LocalFilesystem, Path, NamespaceFactory)} so workspace-rooted absolute paths resolve
     *     identically to their relative spelling under namespace scoping
     * @param upper shell-capable workspace filesystem (read-write, workspace root)
     * @param lower read-only project filesystem (overlay fallback)
     * @param projectFs writable project filesystem for non-workspace writes
     * @param workspaceRoot absolute path of the workspace, used to classify absolute paths
     */
    @Deprecated
    public ProjectAwareOverlay(
            AbstractSandboxFilesystem upper,
            AbstractFilesystem lower,
            LocalFilesystem projectFs,
            Path workspaceRoot) {
        this(upper, lower, projectFs, workspaceRoot, null);
    }

    @Override
    public String id() {
        return shellBackend.id();
    }

    @Override
    public ExecuteResponse execute(
			ChatObject runtimeContext, String command, Integer timeoutSeconds) {
        return shellBackend.execute(runtimeContext, command, timeoutSeconds);
    }

    // ==================== Write routing ====================

    @Override
    public WriteResult write(ChatContext runtimeContext, String filePath, String content) {
        String target = toWorkspaceRelativePath(runtimeContext, filePath);
        if (isWorkspacePath(runtimeContext, filePath)) {
            return upper().write(runtimeContext, target, content);
        }
        return projectFs.write(runtimeContext, target, content);
    }

    @Override
    public EditResult edit(
			ChatContext runtimeContext,
            String filePath,
            String oldString,
            String newString,
            boolean replaceAll) {
        String target = toWorkspaceRelativePath(runtimeContext, filePath);
        if (isWorkspacePath(runtimeContext, filePath)) {
            return super.edit(runtimeContext, target, oldString, newString, replaceAll);
        }
        if (projectFs.exists(runtimeContext, target)) {
            return projectFs.edit(runtimeContext, target, oldString, newString, replaceAll);
        }
        // Fallback: file may exist only in upper (written before projectWritable was enabled)
        return super.edit(runtimeContext, target, oldString, newString, replaceAll);
    }

    @Override
    public WriteResult delete(ChatContext runtimeContext, String path) {
        String target = toWorkspaceRelativePath(runtimeContext, path);
        if (isWorkspacePath(runtimeContext, path)) {
            return super.delete(runtimeContext, target);
        }
        if (projectFs.exists(runtimeContext, target)) {
            return projectFs.delete(runtimeContext, target);
        }
        return super.delete(runtimeContext, target);
    }

    @Override
    public List<FileUploadResponse> uploadFiles(
			ChatContext runtimeContext, List<Map.Entry<String, byte[]>> files) {
        List<Map.Entry<String, byte[]>> workspaceFiles = new ArrayList<>();
        List<Map.Entry<String, byte[]>> projectFiles = new ArrayList<>();
        for (Map.Entry<String, byte[]> entry : files) {
            if (isWorkspacePath(runtimeContext, entry.getKey())) {
                workspaceFiles.add(entry);
            } else {
                projectFiles.add(
						new AbstractMap.SimpleImmutableEntry(
                                toWorkspaceRelativePath(runtimeContext, entry.getKey()),
                                entry.getValue()));
            }
        }
        List<FileUploadResponse> results = new ArrayList<>();
        if (!workspaceFiles.isEmpty()) {
            results.addAll(upper().uploadFiles(runtimeContext, workspaceFiles));
        }
        if (!projectFiles.isEmpty()) {
            results.addAll(projectFs.uploadFiles(runtimeContext, projectFiles));
        }
        return results;
    }

    // ==================== Read / search / move routing ====================

    @Override
    public ReadResult read(ChatContext runtimeContext, String filePath, int offset, int limit) {
        return super.read(
                runtimeContext, toWorkspaceRelativePath(runtimeContext, filePath), offset, limit);
    }

    @Override
    public boolean exists(ChatContext runtimeContext, String path) {
        return super.exists(runtimeContext, toWorkspaceRelativePath(runtimeContext, path));
    }

    @Override
    public List<FileDownloadResponse> downloadFiles(
			ChatContext runtimeContext, List<String> paths) {
		List<String> targets =
				paths.stream()
						.map(path -> toWorkspaceRelativePath(runtimeContext, path))
						.collect(Collectors.toList());
        return super.downloadFiles(runtimeContext, targets);
    }

    @Override
    public LsResult ls(ChatContext runtimeContext, String path) {
        return super.ls(runtimeContext, toWorkspaceRelativePath(runtimeContext, path));
    }

    @Override
    public GrepResult grep(
			ChatContext runtimeContext, String pattern, String path, String glob) {
        return super.grep(
                runtimeContext, pattern, toWorkspaceRelativePath(runtimeContext, path), glob);
    }

    @Override
    public GlobResult glob(ChatContext runtimeContext, String pattern, String path) {
        return super.glob(runtimeContext, pattern, toWorkspaceRelativePath(runtimeContext, path));
    }

    @Override
    public WriteResult move(ChatContext runtimeContext, String fromPath, String toPath) {
        return super.move(
                runtimeContext,
                toWorkspaceRelativePath(runtimeContext, fromPath),
                toWorkspaceRelativePath(runtimeContext, toPath));
    }

    // ==================== Path classification ====================

    boolean isWorkspacePath(ChatContext runtimeContext, String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return true;
        }
        String normalized = normalizeSeparators(filePath);
        String rel = workspaceRelative(runtimeContext, normalized);
        if (rel != null) {
            return matchesWorkspacePrefix(rel);
        }
        if (Paths.get(normalized).isAbsolute()) {
            return false;
        }
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return matchesWorkspacePrefix(normalized);
    }

    private boolean matchesWorkspacePrefix(String rel) {
        for (String prefix : WORKSPACE_PREFIXES) {
            if (rel.equals(prefix) || rel.startsWith(prefix + "/")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the agent-visible relative form of an absolute path that falls under {@link
     * #workspaceRoot}: the workspace root and the caller's namespace prefix are stripped, so the
     * absolute and the relative spelling of the same path are interchangeable. Returns {@code
     * null} when {@code normalized} is relative, equals the workspace root, or lives outside the
     * workspace root.
     */
    private String workspaceRelative(ChatContext runtimeContext, String normalized) {
        Path absolute = Paths.get(normalized);
        if (!absolute.isAbsolute()) {
            return null;
        }
        Path normalizedPath = absolute.normalize();
        if (!normalizedPath.startsWith(workspaceRoot)) {
            return null;
        }
        String rel = stripNamespacePrefix(runtimeContext, workspaceRoot.relativize(normalizedPath));
        return rel.isEmpty() ? null : rel;
    }

    /**
     * Removes the caller's namespace prefix from a workspace-relative path when the path starts
     * with it; without namespace scoping (or when the prefix does not match) the path is returned
     * unchanged.
     */
    private String stripNamespacePrefix(ChatContext runtimeContext, Path workspaceRelative) {
        String rel = workspaceRelative.toString().replace('\\', '/');
        if (namespaceFactory == null) {
            return rel;
        }
        List<String> namespace = namespaceFactory.getNamespace(runtimeContext);
        if (namespace == null || namespace.isEmpty()) {
            return rel;
        }
        String prefix = String.join("/", namespace);
        return rel.startsWith(prefix + "/") ? rel.substring(prefix.length() + 1) : rel;
    }

    /**
     * Rewrites a workspace-rooted absolute path to its agent-visible relative form (see {@link
     * #workspaceRelative}); every other path is returned unchanged. All routed operations pass
     * their inputs through this method so that both spellings of the same path resolve identically
     * in the workspace, project, and overlay layers.
     */
    private String toWorkspaceRelativePath(ChatContext runtimeContext, String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return filePath;
        }
        String rel = workspaceRelative(runtimeContext, normalizeSeparators(filePath));
        return rel != null ? rel : filePath;
    }

    private static String normalizeSeparators(String filePath) {
        return filePath.replace('\\', '/').trim();
    }
}
