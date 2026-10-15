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
import org.frameworkset.spi.ai.filesystem.model.*;
import org.frameworkset.spi.ai.filesystem.sandbox.AbstractSandboxFilesystem;
import org.frameworkset.spi.ai.model.ChatObject;

import java.util.*;

/**
 * A layered filesystem that overlays a user-specific "upper" layer on top of a shared "lower"
 * layer, providing copy-on-write semantics.
 *
 * <p>Read operations check the upper layer first and fall back to the lower layer. Write operations
 * always target the upper layer. This enables per-user customization of shared content (e.g.
 * skills, subagents) without modifying the shared originals.
 *
 * <p>Merge semantics:
 *
 * <ul>
 *   <li>{@code ls} — union of both layers; upper entries take precedence on path collision
 *   <li>{@code read} — upper first, then lower
 *   <li>{@code write}/{@code edit} — always to upper (copy-on-write)
 *   <li>{@code delete} — removes from upper only; shared-layer files cannot be deleted
 *   <li>{@code grep}/{@code glob} — searches both layers; upper results override lower on path
 *       collision
 *   <li>{@code exists} — true if present in either layer
 * </ul>
 */
public class OverlayFilesystem implements AbstractFilesystem {

    private final AbstractFilesystem upper;
    private final AbstractFilesystem lower;

    /**
     * Creates an overlay filesystem.
     *
     * @param upper the user-specific layer (read/write); takes precedence on conflicts
     * @param lower the shared layer (read-only from the overlay's perspective)
     */
    public OverlayFilesystem(AbstractFilesystem upper, AbstractFilesystem lower) {
        if (upper == null) {
            throw new IllegalArgumentException("upper layer must not be null");
        }
        if (lower == null) {
            throw new IllegalArgumentException("lower layer must not be null");
        }
        this.upper = upper;
        this.lower = lower;
    }

    /** Returns the upper (writable) layer. */
    public AbstractFilesystem upper() {
        return upper;
    }

    /** Returns the lower (shared) layer. */
    public AbstractFilesystem lower() {
        return lower;
    }

    /**
     * Builds an overlay that automatically exposes shell execution when {@code upper} supports
     * it. Returns a {link AbstractSandboxFilesystem}-implementing subclass when
     * {@code upper instanceof AbstractSandboxFilesystem}, otherwise a plain
     * {@link OverlayFilesystem}.
     *
     * <p>Use this factory in preference to the constructor when {@code upper} may carry shell
     * capability (e.g. {link LocalFilesystemWithShell})
     * — it lets callers like
     * {@code ReActAgent.Builder} keep their {@code instanceof AbstractSandboxFilesystem} check
     * working through the overlay.
     *
     * @param upper user-specific layer (read/write); takes precedence on conflicts
     * @param lower shared layer (read-only from the overlay's perspective)
     * @return overlay; either plain or shell-aware depending on {@code upper}
     */
    public static AbstractFilesystem of(AbstractFilesystem upper, AbstractFilesystem lower) {
        if (upper instanceof AbstractSandboxFilesystem ) {
            return new ShellAwareOverlay((AbstractSandboxFilesystem)upper, lower);
        }
        return new OverlayFilesystem(upper, lower);
    }

    /**
     * Overlay subtype that delegates shell {@link #execute} and {@link #id} to a shell-capable
     * upper store. Filesystem operations inherit the standard overlay semantics from
     * {@link OverlayFilesystem}.
     */
    private static final class ShellAwareOverlay extends OverlayFilesystem
            implements AbstractSandboxFilesystem {

        private final AbstractSandboxFilesystem shellBackend;

        ShellAwareOverlay(AbstractSandboxFilesystem upper, AbstractFilesystem lower) {
            super(upper, lower);
            this.shellBackend = upper;
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
    }

    @Override
    public LsResult ls(ChatContext runtimeContext, String path) {
			LsResult upperResult = upper.ls(runtimeContext, path);
        LsResult lowerResult = lower.ls(runtimeContext, path);

        if (!upperResult.isSuccess() && !lowerResult.isSuccess()) {
            return lowerResult;
        }

        Map<String, FileInfo> merged = new LinkedHashMap<>();
        if (lowerResult.isSuccess() && lowerResult.getEntries() != null) {
            for (FileInfo fi : lowerResult.getEntries()) {
                merged.put(fi.getPath(), fi);
            }
        }
        if (upperResult.isSuccess() && upperResult.getEntries() != null) {
            for (FileInfo fi : upperResult.getEntries()) {
                merged.put(fi.getPath(), fi);
            }
        }
        return LsResult.success(new ArrayList<>(merged.values()));
    }

    @Override
    public ReadResult read(ChatContext runtimeContext, String filePath, int offset, int limit) {
        if (upper.exists(runtimeContext, filePath)) {
            return upper.read(runtimeContext, filePath, offset, limit);
        }
        return lower.read(runtimeContext, filePath, offset, limit);
    }

    @Override
    public WriteResult write(ChatContext runtimeContext, String filePath, String content) {
        return upper.write(runtimeContext, filePath, content);
    }

    @Override
    public EditResult edit(
			ChatContext runtimeContext,
            String filePath,
            String oldString,
            String newString,
            boolean replaceAll) {
        if (upper.exists(runtimeContext, filePath)) {
            return upper.edit(runtimeContext, filePath, oldString, newString, replaceAll);
        }
        if (lower.exists(runtimeContext, filePath)) {
            ReadResult src = lower.read(runtimeContext, filePath, 0, Integer.MAX_VALUE);
            if (!src.isSuccess()) {
                return EditResult.fail("Cannot read shared file for copy-on-write: " + filePath);
            }
            upper.write(runtimeContext, filePath, src.getFileData().getContent());
            return upper.edit(runtimeContext, filePath, oldString, newString, replaceAll);
        }
        return EditResult.fail("File not found: " + filePath);
    }

    @Override
    public GrepResult grep(
			ChatContext runtimeContext, String pattern, String path, String glob) {
        GrepResult upperResult = upper.grep(runtimeContext, pattern, path, glob);
        GrepResult lowerResult = lower.grep(runtimeContext, pattern, path, glob);

        if (!upperResult.isSuccess() && !lowerResult.isSuccess()) {
            return lowerResult;
        }

        Map<String, GrepMatch> merged = new LinkedHashMap<>();
        if (lowerResult.isSuccess() && lowerResult.getMatches() != null) {
            for (GrepMatch m : lowerResult.getMatches	()) {
                merged.put(m.getPath() + ":" + m.getLine(), m);
            }
        }
        if (upperResult.isSuccess() && upperResult.getMatches() != null) {
            for (GrepMatch m : upperResult.getMatches()) {
                merged.put(m.getPath() + ":" + m.getLine(), m);
            }
        }
        return GrepResult.success(new ArrayList<>(merged.values()));
    }

    @Override
    public GlobResult glob(ChatContext runtimeContext, String pattern, String path) {
        GlobResult upperResult = upper.glob(runtimeContext, pattern, path);
        GlobResult lowerResult = lower.glob(runtimeContext, pattern, path);

        if (!upperResult.isSuccess() && !lowerResult.isSuccess()) {
            return lowerResult;
        }

        Map<String, FileInfo> merged = new LinkedHashMap<>();
        if (lowerResult.isSuccess() && lowerResult.getMatches() != null) {
            for (FileInfo fi : lowerResult.getMatches()) {
                merged.put(fi.getPath(), fi);
            }
        }
        if (upperResult.isSuccess() && upperResult.getMatches() != null) {
            for (FileInfo fi : upperResult.getMatches()) {
                merged.put(fi.getPath(), fi);
            }
        }
        return GlobResult.success(new ArrayList<>(merged.values()));
    }

    @Override
    public List<FileUploadResponse> uploadFiles(
            ChatContext runtimeContext, List<Map.Entry<String, byte[]>> files) {
        return upper.uploadFiles(runtimeContext, files);
    }

    @Override
    public List<FileDownloadResponse> downloadFiles(
            ChatContext	 runtimeContext, List<String> paths) {
        List<FileDownloadResponse> results = new ArrayList<>();
        for (String path : paths) {
            if (upper.exists(runtimeContext, path)) {
                results.addAll(upper.downloadFiles(runtimeContext, Arrays.asList(path)));
            } else {
                results.addAll(lower.downloadFiles(runtimeContext, Arrays.asList(path)));
            }
        }
        return results;
    }

    @Override
    public WriteResult delete(ChatContext runtimeContext, String path) {
        if (upper.exists(runtimeContext, path)) {
            return upper.delete(runtimeContext, path);
        }
        if (lower.exists(runtimeContext, path)) {
            return WriteResult.fail("Cannot delete shared file: " + path);
        }
        return WriteResult.ok(path);
    }

    @Override
    public WriteResult move(ChatContext runtimeContext, String fromPath, String toPath) {
        if (upper.exists(runtimeContext, fromPath)) {
            return upper.move(runtimeContext, fromPath, toPath);
        }
        if (lower.exists(runtimeContext, fromPath)) {
            ReadResult src = lower.read(runtimeContext, fromPath, 0, Integer.MAX_VALUE);
            if (!src.isSuccess()) {
                return WriteResult.fail("Cannot read source for move: " + fromPath);
            }
            return upper.write(runtimeContext, toPath, src.getFileData().getContent	());
        }
        return WriteResult.fail("Source not found: " + fromPath);
    }

    @Override
    public boolean exists(ChatContext runtimeContext, String path) {
        return upper.exists(runtimeContext, path) || lower.exists(runtimeContext, path);
    }

    /** Returns the user-specific (read/write) layer. */
    public AbstractFilesystem getUpper() {
        return upper;
    }

    /** Returns the shared (read-only-through-this-overlay) layer. */
    public AbstractFilesystem getLower() {
        return lower;
    }
}
