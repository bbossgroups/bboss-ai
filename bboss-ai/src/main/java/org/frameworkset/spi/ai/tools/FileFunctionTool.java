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

import com.frameworkset.util.JsonUtil;
import com.frameworkset.util.SimpleStringUtil;
import org.frameworkset.spi.ai.model.annotation.Tool;
import org.frameworkset.spi.ai.model.annotation.ToolParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.*;

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
public class FileFunctionTool {
    private static Logger logger = LoggerFactory.getLogger(FileFunctionTool.class);

    private static final String DEFAULT_CHARSET = java.nio.charset.StandardCharsets.UTF_8.name();

    /** 允许操作的基目录，为空则不限制 */
    private List<String> baseDirectories;

    public FileFunctionTool() {
    }

    public FileFunctionTool(String baseDirectory) {
		baseDirectories = new ArrayList<>();
        baseDirectories.add(baseDirectory);
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

    // ==================== 公开工具方法 ====================
    /**
     * 拷贝文件或目录
     */
    @Tool(name = "copyFile", description = "拷贝文件或目录到目标路径，支持文件和目录的拷贝，目标父目录不存在时自动创建")
    public Map copyFile(@ToolParam(name = "source", description = "源文件或目录路径", required = true) String source,
                        @ToolParam(name = "target", description = "目标文件或目录路径", required = true) String target,
                        @ToolParam(name = "overwrite", description = "目标已存在时是否覆盖，默认false", required = false) Boolean overwrite) {
        Map result = new HashMap();
        try {
            java.io.File srcFile = validateAndGetFile(source);
            java.io.File tgtFile = validateAndGetFile(target);
            if (!srcFile.exists()) {
                result.put("success", false);
                result.put("message", "源路径不存在: " + source);
                return result;
            }
            boolean isOverwrite = overwrite != null && overwrite;
            if (srcFile.isFile()) {
                java.io.File destFile = tgtFile.isDirectory() ? new java.io.File(tgtFile, srcFile.getName()) : tgtFile;
                if (destFile.exists() && !isOverwrite) {
                    result.put("success", false);
                    result.put("message", "目标文件已存在且未启用覆盖: " + destFile.getAbsolutePath());
                    return result;
                }
                java.io.File parentDir = destFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }
                copyFileContent(srcFile, destFile);
                result.put("success", true);
                result.put("source", srcFile.getAbsolutePath());
                result.put("target", destFile.getAbsolutePath());
                result.put("message", "文件拷贝成功");
            } else if (srcFile.isDirectory()) {
                java.io.File destDir = tgtFile;
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
            java.io.File file = validateAndGetFile(path);
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
            java.io.File file = validateAndGetFile(path);
            if (!file.exists() || !file.isFile()) {
                result.put("success", false);
                result.put("message", "文件不存在或不是普通文件");
                return result;
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
     */
    @Tool(name = "readFile", description = "读取指定文件的内容，支持指定字符编码，未指定时自动识别编码")
    public Map readFile(@ToolParam(name = "path", description = "文件路径", required = true) String path,
                        @ToolParam(name = "charset", description = "字符编码，如UTF-8、GBK等，为空则自动识别", required = false) String charset) {
        Map result = new HashMap();
        StringBuilder content = new StringBuilder();
        try {
            java.io.File file = validateAndGetFile(path);
            if (!file.exists() || !file.isFile()) {
                result.put("success", false);
                result.put("message", "文件不存在或不是普通文件");
                return result;
            }
            if (SimpleStringUtil.isEmpty(charset)) {
                charset = doDetectEncoding(file);
            }
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(new java.io.FileInputStream(file), charset))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            }
            result.put("success", true);
            result.put("content", content.toString());
            result.put("charset", charset);
            result.put("message", "文件读取成功");
        } catch (Exception e) {
            logger.error("读取文件失败: " + path, e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 遍历目录读取目录下所有文件内容
     */
    @Tool(name = "readDirectoryFiles", description = "遍历指定目录，读取目录下所有文件的内容，返回文件路径与内容的集合")
    public Map readDirectoryFiles(
            @ToolParam(name = "path", description = "目录路径", required = true) String path,
            @ToolParam(name = "recursive", description = "是否递归遍历子目录，默认false", required = false) Boolean recursive,
            @ToolParam(name = "charset", description = "字符编码，为空则逐个自动识别", required = false) String charset) {
        Map result = new HashMap();
        List<Map<String, String>> fileContents = new ArrayList<>();
        try {
            java.io.File dir = validateAndGetFile(path);
            if (!dir.exists() || !dir.isDirectory()) {
                result.put("success", false);
                result.put("message", "目录不存在或不是有效目录");
                return result;
            }
            boolean isRecursive = recursive != null && recursive;
            collectFileContents(dir, isRecursive, charset, fileContents);
            result.put("success", true);
            result.put("files", fileContents);
            result.put("count", fileContents.size());
            result.put("message", "共读取 " + fileContents.size() + " 个文件");
        } catch (Exception e) {
            logger.error("遍历读取目录失败: " + path, e);
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
            java.io.File file = validateAndGetFile(path);
            if (content == null) {
                content = "";
            }
            if (SimpleStringUtil.isEmpty(charset)) {
                charset = DEFAULT_CHARSET;
            }
            boolean isAppend = append != null && append;
            java.io.File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
			if(!file.exists()){
				file.createNewFile();
			}
            try (java.io.BufferedWriter writer = new java.io.BufferedWriter(
                    new java.io.OutputStreamWriter(new java.io.FileOutputStream(file, isAppend), charset))) {
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
            java.io.File file = validateAndGetFile(path);
            boolean dirFlag = isDirectory != null && isDirectory;
            boolean created;
            if (dirFlag) {
                created = file.mkdirs();
            } else {
                java.io.File parent = file.getParentFile();
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
            java.io.File file = validateAndGetFile(path);
            if (!file.exists()) {
                result.put("success", false);
                result.put("message", "文件或目录不存在");
                return result;
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
            java.io.File file = validateAndGetFile(path);
            if (!file.exists()) {
                result.put("success", false);
                result.put("message", "文件或目录不存在");
                return result;
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
     * 校验路径是否在允许的基目录范围内，并返回 File 对象
     */
    private java.io.File validateAndGetFile(String path) {
        if (SimpleStringUtil.isEmpty(path)) {
            throw new IllegalArgumentException("路径不能为空");
        }
        java.io.File file = new java.io.File(path);
		
        if (baseDirectories != null && baseDirectories.size() > 0) {
			boolean isOk = false;
			for(String baseDirectory:baseDirectories) {
				java.io.File base = new java.io.File(baseDirectory);
				String basePath = base.getAbsolutePath();
				String targetPath = file.getAbsolutePath();
				// 统一分隔符并规范化路径，防止路径穿越
				String normalizedBase = basePath.replace('\\', '/').replaceAll("/+", "/");
				String normalizedTarget = targetPath.replace('\\', '/').replaceAll("/+", "/");
				if (normalizedTarget.startsWith(normalizedBase)) {
					isOk = true;
				}
			}
			if (!isOk) {
				throw new IllegalArgumentException("路径"+path+"超出允许的操作范围，只允许操作以下目录: " + JsonUtil.object2json(baseDirectories));
			}
        }
        return file;
    }

    /**
     * 递归收集目录下所有文件的内容
     */
    private void collectFileContents(java.io.File dir, boolean recursive, String charset, List<Map<String, String>> fileContents) {
        java.io.File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (java.io.File file : files) {
            if (file.isDirectory() && recursive) {
                collectFileContents(file, true, charset, fileContents);
            } else if (file.isFile()) {
                try {
                    String fileCharset = SimpleStringUtil.isEmpty(charset) ? doDetectEncoding(file) : charset;
                    StringBuilder content = new StringBuilder();
                    try (java.io.BufferedReader reader = new java.io.BufferedReader(
                            new java.io.InputStreamReader(new java.io.FileInputStream(file), fileCharset))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            content.append(line).append("\n");
                        }
                    }
                    Map<String, String> item = new HashMap<>();
                    item.put("path", file.getAbsolutePath());
                    item.put("content", content.toString());
                    item.put("charset", fileCharset);
                    fileContents.add(item);
                } catch (Exception e) {
                    logger.warn("读取文件内容失败，跳过: " + file.getAbsolutePath(), e);
                }
            }
        }
    }

    /**
     * 检测文件字符编码
     */
    private String doDetectEncoding(java.io.File file) throws IOException {
        byte[] buffer = new byte[4096];
        int len;
        try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
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
    private boolean deleteDirectory(java.io.File directory) {
        java.io.File[] files = directory.listFiles();
        if (files != null) {
            for (java.io.File f : files) {
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
    private void copyFileContent(java.io.File src, java.io.File dest) throws IOException {
        try (java.io.InputStream in = new java.io.FileInputStream(src);
             java.io.OutputStream out = new java.io.FileOutputStream(dest)) {
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
    private void copyDirectory(java.io.File srcDir, java.io.File destDir) throws IOException {
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
        java.io.File[] files = srcDir.listFiles();
        if (files == null) {
            return;
        }
        for (java.io.File f : files) {
            java.io.File target = new java.io.File(destDir, f.getName());
            if (f.isDirectory()) {
                copyDirectory(f, target);
            } else {
                copyFileContent(f, target);
            }
        }
    }

}
