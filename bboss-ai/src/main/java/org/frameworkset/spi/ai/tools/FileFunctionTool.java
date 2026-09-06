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

import com.frameworkset.util.SimpleStringUtil;
import org.frameworkset.spi.ai.audit.Auditor;
import org.frameworkset.spi.ai.context.ChatContext;
import org.frameworkset.spi.ai.filesystem.AbstractFilesystem;
import org.frameworkset.spi.ai.filesystem.local.LocalFilesystem;
import org.frameworkset.spi.ai.filesystem.model.*;
import org.frameworkset.spi.ai.filesystem.WorkspacePathNormalizer;
import org.frameworkset.spi.ai.model.ChatObject;
import org.frameworkset.spi.ai.model.annotation.Tool;
import org.frameworkset.spi.ai.model.annotation.ToolParam;
import org.frameworkset.spi.ai.tool.AgentTraceHolder;
import org.frameworkset.spi.ai.util.FileToolUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 文件系统操作工具类。
 * <p>
 * 提供文件与目录的增删改查、内容读写、编码自动识别、属性获取等能力，
 * 支持通过 {@code baseDirectory} 限制操作范围以防止路径穿越，
 * 适用于智能体通过 {@link Tool} 注解暴露为可调用工具的场景。
 * </p>
 *
 * @author biaoping.yin
 * @Date 2026/7/2
 */
public class FileFunctionTool  extends BaseAuditorTool<FileFunctionTool>{
    private static Logger logger = LoggerFactory.getLogger(FileFunctionTool.class);
	private AbstractFilesystem abstractFilesystem;
	private WorkspacePathNormalizer pathNormalizer;

    private static final String DEFAULT_CHARSET = java.nio.charset.StandardCharsets.UTF_8.name();

    /** 默认最大读取文件大小：2MB，防止读取超大文件导致 OOM */
    private static final long DEFAULT_MAX_READ_SIZE = 2L * 1024 * 1024;

    /** 允许操作的基目录，为空则不限制 */
    private List<String> baseDirectories;

    /** 单次读取文件的最大字节数，超过此限制将截断并提示，默认 2MB */
    private long maxReadSize = DEFAULT_MAX_READ_SIZE;

    public FileFunctionTool() {
		abstractFilesystem = new LocalFilesystem(".");
		pathNormalizer =   WorkspacePathNormalizer.of(".");
    }
	public FileFunctionTool(Auditor auditor) {
		super(auditor);
	}
	
	public FileFunctionTool(Auditor auditor,String baseDirectory) {
		super(auditor);
		baseDirectories = new ArrayList<>();
        baseDirectories.add(baseDirectory);
		abstractFilesystem = new LocalFilesystem(baseDirectory);
		pathNormalizer =  WorkspacePathNormalizer.of(baseDirectory);
	}
    public FileFunctionTool(String baseDirectory) {
		baseDirectories = new ArrayList<>();
        baseDirectories.add(baseDirectory);
		abstractFilesystem = new LocalFilesystem(baseDirectory);
		pathNormalizer =  WorkspacePathNormalizer.of(baseDirectory);
    }

    public FileFunctionTool addBaseDirectory(String... baseDirectory) {
		if(baseDirectories == null){
			baseDirectories = new ArrayList<>();
		}
		if(baseDirectory != null && baseDirectory.length > 0) {
			for (String baseDirectoryItem : baseDirectory) {
				if (!SimpleStringUtil.isEmpty(baseDirectoryItem)) {
					baseDirectories.add(baseDirectoryItem);
					
				}
			}
		}
        return this;
    }

    /**
     * 设置单次读取文件的最大字节数，防止读取超大文件导致 OOM。
     * @param maxReadSize 最大读取字节数，如 2 * 1024 * 1024 表示 2MB
     * @return this
     */
    public FileFunctionTool setMaxReadSize(long maxReadSize) {
        if (maxReadSize > 0) {
            this.maxReadSize = maxReadSize;
        }
        return this;
    }
	private String norm(String path) {
		return pathNormalizer != null ? pathNormalizer.normalize(path) : path;
	}

    // ==================== 公开工具方法 ====================
	
	@Tool(name = "glob_files",  description = "Find files matching a glob pattern.")
	public String globFiles(			 
			@ToolParam(name = "pattern", description = "Glob pattern (e.g., **/*.java)")
			String pattern,
			@ToolParam(
					name = "path",
					description = "Base directory to search from",
					required = false)
			String path) {
//		File srcFile = FileToolUtil.validateAndGetFile(this.baseDirectories,path, true);
		ChatObject chatObject = AgentTraceHolder.getChatObject();
		GlobResult r = abstractFilesystem.glob(chatObject.getChatContext(), pattern, norm(path));
		if (!r.isSuccess()) {
			return "Error: " + r.getError();
		}
		List<FileInfo> files = r.getMatches();
		if (files == null || files.isEmpty()) {
			return "No matching files found";
		}
		return files.stream()
				.map(f -> f.getPath() + (f.isDirectory() ? "/" : " (" + f.getSize() + " bytes)"))
				.collect(Collectors.joining("\n"));
	}
	
	@Tool(
			name = "list_files",
			readOnly = true,
			description = "List files and directories at the given path.")
	public String listFiles(
			ChatContext runtimeContext,
			@ToolParam(name = "path", description = "Directory path to list") String path) {
		LsResult r = abstractFilesystem.ls(runtimeContext, norm(path));
		if (!r.isSuccess()) {
			return "Error: " + r.getError();
		}
		List<FileInfo> infos = r.getEntries();
		if (infos == null || infos.isEmpty()) {
			return "Empty or not a directory: " + path;
		}
		return infos.stream()
				.map(
						f ->
								(f.isDirectory() ? "[DIR]  " : "[FILE] ")
										+ f.getPath()
										+ (f.isDirectory() ? "" : " (" + f.getSize() + " bytes)"))
				.collect(Collectors.joining("\n"));
	}
	
	@Tool(
			name = "grep_files",
			readOnly = true,
			description = "Search file contents for a literal text pattern.")
	public String grepFiles(
			ChatContext runtimeContext,
			@ToolParam(name = "pattern", description = "Literal text pattern to search for")
			String pattern,
			@ToolParam(name = "path", description = "Directory or file to search", required = false)
			String path,
			@ToolParam(
					name = "glob",
					description = "Optional file glob filter (e.g., *.java)",
					required = false)
			String glob) {
		GrepResult r = abstractFilesystem.grep(runtimeContext, pattern, norm(path), glob);
		if (!r.isSuccess()) {
			return "Error: " + r.getError();
		}
		List<GrepMatch> matches = r.getMatches();
		if (matches == null || matches.isEmpty()) {
			return "No matches found";
		}
		return matches.stream()
				.map(m -> m.getPath() + ":" + m.getLine() + ":" + m.getText())
				.collect(Collectors.joining("\n"));
	}
    /**
     * 拷贝文件或目录
     */
    @Tool(name = "copyFile", description = "拷贝文件或目录到目标路径，支持文件和目录的拷贝，目标父目录不存在时自动创建")
    public Map copyFile(@ToolParam(name = "source", description = "源文件或目录路径", required = true) String source,
                        @ToolParam(name = "target", description = "目标文件或目录路径", required = true) String target,
                        @ToolParam(name = "overwrite", description = "目标已存在时是否覆盖，默认false", required = false) Boolean overwrite) {
        Map result = new HashMap();
        try {
            File srcFile = FileToolUtil.validateAndGetFile(this.baseDirectories,source, true);
            File tgtFile = FileToolUtil.validateAndGetFile(this.baseDirectories,target, true);
            if (!srcFile.exists()) {
                result.put("success", false);
                result.put("message", "源路径不存在: " + source);
                return result;
            }
			if(auditor != null) {
				Map toolInfo = new LinkedHashMap();
				toolInfo.put("source", source);
				toolInfo.put("target", target);
				toolInfo.put("overwrite", overwrite);
				Map<String, Object> auditResult = audit("copyFile", toolInfo);
				if (auditResult != null)
					return auditResult;
			}
            boolean isOverwrite = overwrite != null && overwrite;
            if (srcFile.isFile()) {
                File destFile = tgtFile.isDirectory() ? new File(tgtFile, srcFile.getName()) : tgtFile;
                if (destFile.exists() && !isOverwrite) {
                    result.put("success", false);
                    result.put("message", "目标文件已存在且未启用覆盖: " + destFile.getAbsolutePath());
                    return result;
                }
                File parentDir = destFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }
                copyFileContent(srcFile, destFile);
                result.put("success", true);
                result.put("source", srcFile.getAbsolutePath());
                result.put("target", destFile.getAbsolutePath());
                result.put("message", "文件拷贝成功");
            } else if (srcFile.isDirectory()) {
                File destDir = tgtFile;
                if (destDir.exists() && !isOverwrite) {
                    result.put("success", false);
                    result.put("message", "目标目录已存在且未启用覆盖: " + destDir.getAbsolutePath());
                    return result;
                }
                copyDirectory(srcFile, destDir);
                result.put("success", true);
                result.put("source", srcFile.getAbsolutePath());
                result.put("target", destDir.getAbsolutePath());
                result.put("message", "目录拷贝成功");
            }
        } catch (Exception e) {
            logger.error("拷贝文件失败: " + source + " -> " + target, e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 检查文件或目录是否存在
     */
    @Tool(name = "fileExists", description = "检查指定路径的文件或目录是否存在")
    public Map fileExists(@ToolParam(name = "path", description = "文件或目录路径", required = true) String path) {
        Map result = new HashMap();
        try {
            File file = FileToolUtil.validateAndGetFile(this.baseDirectories,path, true);
			if(auditor != null) {				 
				Map<String, Object> auditResult = audit("fileExists", path);
				if (auditResult != null)
					return auditResult;
			}
            boolean exists = file.exists();
            result.put("success", true);
            result.put("exists", exists);
            result.put("message", exists ? "路径存在" : "路径不存在");
        } catch (Exception e) {
            logger.error("检查文件是否存在失败: " + path, e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 识别文件内容的字符编码
     */
    @Tool(name = "detectFileEncoding", description = "识别文件内容的字符编码，支持通过BOM和字节特征检测UTF-8、UTF-16、GBK等常见编码")
    public Map detectFileEncoding(@ToolParam(name = "path", description = "文件路径", required = true) String path) {
        Map result = new HashMap();
        try {
            File file = FileToolUtil.validateAndGetFile(this.baseDirectories,path, true);
            if (!file.exists() || !file.isFile()) {
                result.put("success", false);
                result.put("message", "文件不存在或不是普通文件");
                return result;
            }
			if(auditor != null) {
				Map<String, Object> auditResult = audit("detectFileEncoding", path);
				if (auditResult != null)
					return auditResult;
			}
            String encoding = doDetectEncoding(file);
            result.put("success", true);
            result.put("encoding", encoding);
            result.put("message", "编码识别成功");
        } catch (Exception e) {
            logger.error("识别文件编码失败: " + path, e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 读取文件内容
     * <p>注意：为防止 OOM，单次最多读取 {@link #maxReadSize} 字节（默认 2MB），
     * 超出部分将被截断并在返回结果中标注 {@code truncated=true}。</p>
     */
    @Tool(name = "readFile", description = "读取指定文件的内容，支持指定字符编码，未指定时自动识别编码。为防止内存溢出，单次最多读取2MB内容，超出部分截断")
    public Map readFile(@ToolParam(name = "path", description = "文件路径", required = true) String path,
                        @ToolParam(name = "charset", description = "字符编码，如UTF-8、GBK等，为空则自动识别" ) String charset) {
        Map result = new HashMap();
        StringBuilder content = new StringBuilder();
        try {
            File file = FileToolUtil.validateAndGetFile(this.baseDirectories,path, true);
            if (!file.exists() || !file.isFile()) {
                result.put("success", false);
                result.put("message", "文件不存在或不是普通文件");
                return result;
            }
			if(auditor != null) {
				Map<String, Object> auditResult = audit("readFile", path);
				if (auditResult != null)
					return auditResult;
			}
            // 预检文件大小，超出限制时提前拦截，避免无谓的编码检测与流式读取
            long fileLength = file.length();
            boolean truncated = false;
            if (fileLength > maxReadSize) {
                truncated = true;
                logger.warn("文件 {} 大小 {} 字节超过最大读取限制 {} 字节，将截断读取",
                        path, fileLength, maxReadSize);
            }
            if (SimpleStringUtil.isEmpty(charset)) {
                charset = doDetectEncoding(file);
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file), charset))) {
                String line;
                long bytesRead = 0;
                while ((line = reader.readLine()) != null) {
                    // 按字符数近似估算字节数（保守按 UTF-8 最大 3 字节/字符计算）
                    bytesRead += line.length() * 3L + 1;
                    if (bytesRead > maxReadSize) {
                        truncated = true;
                        break;
                    }
                    content.append(line).append("\n");
                }
            }
            result.put("success", true);
            result.put("content", content.toString());
            result.put("charset", charset);
            if (truncated) {
                result.put("truncated", true);
                result.put("maxReadSize", maxReadSize);
                result.put("message", "文件读取成功，但内容超出最大读取限制（" + formatFileSize(maxReadSize)
                        + "），已截断。如需读取完整内容，请分段读取或调大 maxReadSize");
            } else {
                result.put("message", "文件读取成功");
            }
        } catch (Exception e) {
            logger.error("读取文件失败: " + path, e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

   

    /**
     * 写入内容到文件
     */
    @Tool(name = "writeFile", description = "将内容写入到指定文件，支持指定字符编码和追加模式,如果文件不存在，会自动创建")
    public Map writeFile(@ToolParam(name = "path", description = "文件路径", required = true) String path,
                         @ToolParam(name = "content", description = "要写入的文件内容", required = true) String content,
                         @ToolParam(name = "charset", description = "字符编码，默认UTF-8", required = false) String charset,
                         @ToolParam(name = "append", description = "是否追加写入，默认false覆盖写入", required = false) Boolean append) {
        Map result = new HashMap();
        try {
            File file = FileToolUtil.validateAndGetFile(this.baseDirectories,path, true);
			if(auditor != null) {
				Map toolInfo = new LinkedHashMap();
				toolInfo.put("path", path);
				toolInfo.put("content", content);
				toolInfo.put("append", append);
				Map<String, Object> auditResult = audit("writeFile", toolInfo);
				if (auditResult != null)
					return auditResult;
			}
            if (content == null) {
                content = "";
            }
            if (SimpleStringUtil.isEmpty(charset)) {
                charset = DEFAULT_CHARSET;
            }
            boolean isAppend = append != null && append;
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
			if(!file.exists()){
				file.createNewFile();
			}
            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(file, isAppend), charset))) {
                writer.write(content);
            }
            result.put("success", true);
            result.put("message", "文件写入成功");
            result.put("path", file.getAbsolutePath());
        } catch (Exception e) {
            logger.error("写入文件失败: " + path, e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    /**
     * 新建文件或目录
     */
    @Tool(name = "createFile", description = "新建文件或目录，若父目录不存在则自动创建")
    public Map createFile(@ToolParam(name = "path", description = "文件或目录路径", required = true) String path,
                          @ToolParam(name = "isDirectory", description = "是否创建目录，true创建目录，false创建文件，默认false", required = false) Boolean isDirectory) {
        Map result = new HashMap();
        try {
            File file = FileToolUtil.validateAndGetFile(this.baseDirectories,path, true);
			if(auditor != null) {
				Map toolInfo = new LinkedHashMap();
				toolInfo.put("path", path);
				toolInfo.put("isDirectory", isDirectory);
				Map<String, Object> auditResult = audit("createFile", toolInfo);
				if (auditResult != null)
					return auditResult;
			}
            boolean dirFlag = isDirectory != null && isDirectory;
            boolean created;
            if (dirFlag) {
                created = file.mkdirs();
            } else {
                File parent = file.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                created = file.createNewFile();
            }
            result.put("success", created);
            result.put("message", created ? "创建成功" : "创建失败，目标可能已存在");
            result.put("path", file.getAbsolutePath());
        } catch (Exception e) {
            logger.error("创建文件失败: " + path, e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 删除文件或目录
     */
    @Tool(name = "deleteFile", description = "删除指定的文件或目录，支持递归删除目录及其子内容")
    public Map deleteFile(@ToolParam(name = "path", description = "文件或目录路径", required = true) String path,
                          @ToolParam(name = "recursive", description = "删除目录时是否递归删除子文件，默认false", required = false) Boolean recursive) {
        Map result = new HashMap();
        try {
            File file = FileToolUtil.validateAndGetFile(this.baseDirectories,path, true);
            if (!file.exists()) {
                result.put("success", false);
                result.put("message", "文件或目录不存在");
                return result;
            }
			if(auditor != null) {
				Map toolInfo = new LinkedHashMap();
				toolInfo.put("path", path);
				toolInfo.put("recursive", recursive);
				Map<String, Object> auditResult = audit("deleteFile", toolInfo);
				if (auditResult != null)
					return auditResult;
			}
            boolean isRecursive = recursive != null && recursive;
            boolean deleted;
            if (file.isDirectory() && isRecursive) {
                deleted = deleteDirectory(file);
            } else {
                deleted = file.delete();
            }
            result.put("success", deleted);
            result.put("message", deleted ? "删除成功" : "删除失败");
        } catch (Exception e) {
            logger.error("删除文件失败: " + path, e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 获取文件属性信息
     */
    @Tool(name = "getFileAttributes", description = "获取文件或目录的属性信息，包括大小、修改时间、创建时间、是否隐藏、是否可读可写等")
    public Map getFileAttributes(@ToolParam(name = "path", description = "文件或目录路径", required = true) String path) {
        Map result = new HashMap();
        try {
            File file = FileToolUtil.validateAndGetFile(this.baseDirectories,path, true);
            if (!file.exists()) {
                result.put("success", false);
                result.put("message", "文件或目录不存在");
                return result;
            }
			if(auditor != null) {
			 
				Map<String, Object> auditResult = audit("getFileAttributes", path);
				if (auditResult != null)
					return auditResult;
			}
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            result.put("success", true);
            result.put("path", file.getAbsolutePath());
            result.put("name", file.getName());
            result.put("isFile", file.isFile());
            result.put("isDirectory", file.isDirectory());
            result.put("size", file.length());
            result.put("sizeReadable", formatFileSize(file.length()));
            result.put("lastModified", sdf.format(new Date(file.lastModified())));
            result.put("canRead", file.canRead());
            result.put("canWrite", file.canWrite());
            result.put("canExecute", file.canExecute());
            result.put("isHidden", file.isHidden());
            result.put("parent", file.getParent());
            if (file.isDirectory()) {
                String[] list = file.list();
                result.put("childrenCount", list != null ? list.length : 0);
            }
            try {
                java.nio.file.Path nioPath = file.toPath();
                BasicFileAttributes attrs = java.nio.file.Files.readAttributes(nioPath, BasicFileAttributes.class);
                FileTime createTime = attrs.creationTime();
                if (createTime != null) {
                    result.put("creationTime", sdf.format(new Date(createTime.toMillis())));
                }
            } catch (Exception ex) {
                // 部分系统可能不支持创建时间
            }
            result.put("message", "获取属性成功");
        } catch (Exception e) {
            logger.error("获取文件属性失败: " + path, e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // ==================== 私有辅助方法 ====================

     
   

    /**
     * 检测文件字符编码
     */
    private String doDetectEncoding(File file) throws IOException {
        byte[] buffer = new byte[4096];
        int len;
        try (FileInputStream fis = new FileInputStream(file)) {
            len = fis.read(buffer);
        }
        if (len < 0) {
            return DEFAULT_CHARSET;
        }

        // 检查 BOM
        if (len >= 3 && (buffer[0] & 0xFF) == 0xEF && (buffer[1] & 0xFF) == 0xBB && (buffer[2] & 0xFF) == 0xBF) {
            return "UTF-8";
        }
        if (len >= 2 && (buffer[0] & 0xFF) == 0xFE && (buffer[1] & 0xFF) == 0xFF) {
            return "UTF-16BE";
        }
        if (len >= 2 && (buffer[0] & 0xFF) == 0xFF && (buffer[1] & 0xFF) == 0xFE) {
            return "UTF-16LE";
        }

        // 尝试 UTF-8 严格解码
        if (isValidUtf8(buffer, len)) {
            return "UTF-8";
        }

        // 尝试 GBK
        if (isValidCharset(buffer, len, "GBK")) {
            return "GBK";
        }

        // 尝试系统默认编码
        String defaultEncoding = java.nio.charset.Charset.defaultCharset().name();
        if (!"UTF-8".equalsIgnoreCase(defaultEncoding) && isValidCharset(buffer, len, defaultEncoding)) {
            return defaultEncoding;
        }

        return DEFAULT_CHARSET;
    }

    /**
     * 校验字节数组是否为合法的 UTF-8
     */
    private boolean isValidUtf8(byte[] bytes, int length) {
        CharsetDecoder decoder = java.nio.charset.StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            decoder.decode(java.nio.ByteBuffer.wrap(bytes, 0, length));
            return true;
        } catch (CharacterCodingException e) {
            return false;
        }
    }

    /**
     * 校验字节数组是否能用指定编码正确解码
     */
    private boolean isValidCharset(byte[] bytes, int length, String charsetName) {
        try {
            CharsetDecoder decoder = java.nio.charset.Charset.forName(charsetName).newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT);
            decoder.decode(java.nio.ByteBuffer.wrap(bytes, 0, length));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 递归删除目录
     */
    private boolean deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteDirectory(f);
                } else {
                    f.delete();
                }
            }
        }
        return directory.delete();
    }

    /**
     * 格式化文件大小为可读字符串
     */
    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else if (size < 1024L * 1024 * 1024) {
            return String.format("%.2f MB", size / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
        }
    }
    
        /**
     * 拷贝单个文件内容
     */
    private void copyFileContent(File src, File dest) throws IOException {
        try (InputStream in = new FileInputStream(src);
             OutputStream out = new FileOutputStream(dest)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
        }
    }

    /**
     * 递归拷贝目录
     */
    private void copyDirectory(File srcDir, File destDir) throws IOException {
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
        File[] files = srcDir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            File target = new File(destDir, f.getName());
            if (f.isDirectory()) {
                copyDirectory(f, target);
            } else {
                copyFileContent(f, target);
            }
        }
    }

}
