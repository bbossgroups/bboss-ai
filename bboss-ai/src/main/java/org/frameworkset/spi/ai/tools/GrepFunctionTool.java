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
import org.frameworkset.spi.ai.audit.Auditor;
import org.frameworkset.spi.ai.model.annotation.Tool;
import org.frameworkset.spi.ai.model.annotation.ToolParam;
import org.frameworkset.spi.ai.util.FileToolUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 跨平台文本搜索工具类（Grep）。
 * <p>
 * 通过调用操作系统原生命令实现文本搜索：Linux/Mac 使用 {@code grep}，Windows 使用 {@code findstr}。
 * 支持正则表达式、递归目录搜索、大小写控制、文件扩展名过滤、行号显示、超时控制等特性。
 * </p>
 * <p>
 * 适用于智能体通过 {@link org.frameworkset.spi.ai.model.annotation.Tool} 注解暴露为可调用工具的场景。
 * </p>
 *
 * @author biaoping.yin
 * @Date 2026/8/17
 */
public class GrepFunctionTool extends BaseAuditorTool<GrepFunctionTool> {
	private static final Logger logger = LoggerFactory.getLogger(GrepFunctionTool.class);
	
	/** 独立线程池，避免长时间搜索阻塞 ForkJoinPool.commonPool */
	private static final AtomicInteger threadCounter = new AtomicInteger(0);
	private static final ExecutorService GREP_EXECUTOR = Executors.newCachedThreadPool(r -> {
		Thread t = new Thread(r, "grep-executor-" + threadCounter.getAndIncrement());
		t.setDaemon(true);
		return t;
	});
	
	/** 默认命令执行超时时间（秒） */
	private long timeout = 60;
	
	/** 允许操作的基目录，为空则不限制 */
	private List<String> baseDirectories;
	
	/** 是否使用 -R 递归（跟随符号链接），默认使用 -r（不跟随） */
	private boolean followSymlinks = false;
	
	public GrepFunctionTool() {
	}
	
	public GrepFunctionTool(long timeout) {
		this.timeout = timeout;
	}
	
	public GrepFunctionTool(Auditor auditor) {
		super(auditor);
	}
	
	public GrepFunctionTool(long timeout, Auditor auditor) {
		super(auditor);
		this.timeout = timeout;
	}
	
	public GrepFunctionTool(String baseDirectory) {
		baseDirectories = new ArrayList<String>();
		baseDirectories.add(baseDirectory);
	}
	
	public GrepFunctionTool(Auditor auditor, String baseDirectory) {
		super(auditor);
		baseDirectories = new ArrayList<String>();
		baseDirectories.add(baseDirectory);
	}
	
	public GrepFunctionTool addBaseDirectory(String... baseDirectory) {
		if (baseDirectories == null) {
			baseDirectories = new ArrayList<String>();
		}
		if (baseDirectory != null && baseDirectory.length > 0) {
			for (String baseDirectoryItem : baseDirectory) {
				if (!SimpleStringUtil.isEmpty(baseDirectoryItem)) {
					baseDirectories.add(baseDirectoryItem);
				}
			}
		}
		return this;
	}
	
	public GrepFunctionTool setTimeout(long timeout) {
		this.timeout = timeout;
		return this;
	}
	
	public GrepFunctionTool setFollowSymlinks(boolean followSymlinks) {
		this.followSymlinks = followSymlinks;
		return this;
	}
	
	// ==================== 公开工具方法 ====================
	
	/**
	 * 在文件或目录中搜索匹配的文本内容。
	 * <p>
	 * Linux/Mac 下调用 {@code grep} 命令，Windows 下调用 {@code findstr} 命令，
	 * 通过操作系统原生命令实现高效的文本搜索。
	 * </p>
	 */
	@Tool(name = "grep", description = "跨平台文本搜索工具，Linux/Mac下调用grep命令、Windows下调用findstr命令（注意：Windows findstr正则语法较为有限，不支持+、?、|等高级元字符），" +
			"在文件或目录中搜索匹配指定模式的文本行。支持正则表达式、递归目录搜索、大小写控制、文件扩展名过滤、行号显示等。")
	public Map<String, Object> grep(
			@ToolParam(name = "pattern", description = "搜索模式，支持正则表达式", required = true) String pattern,
			@ToolParam(name = "path", description = "搜索的起始路径，可以是文件或目录", required = true) String path,
			@ToolParam(name = "recursive", description = "路径为目录时是否递归搜索子目录，默认true", required = false) Boolean recursive,
			@ToolParam(name = "caseSensitive", description = "是否区分大小写，默认true", required = false) Boolean caseSensitive,
			@ToolParam(name = "fileExtensions", description = "限定搜索的文件扩展名（如\"*.java\"或\"*.java,*.xml\"），为空则搜索所有文件", required = false) String fileExtensions,
			@ToolParam(name = "maxCountPerFile", description = "每个文件最多返回的匹配行数，0表示不限制，默认0", required = false) Integer maxCountPerFile,
			@ToolParam(name = "showLineNumbers", description = "是否显示匹配行的行号，默认true", required = false) Boolean showLineNumbers) {
		
		Map<String, Object> result = new HashMap<String, Object>();
		
		if (SimpleStringUtil.isEmpty(pattern)) {
			result.put("success", false);
			result.put("message", "搜索模式不能为空");
			return result;
		}
		
		// 审计
		if (auditor != null) {
			Map<String, Object> toolInfo = new LinkedHashMap<String, Object>();
			toolInfo.put("pattern", pattern);
			toolInfo.put("path", path);
			toolInfo.put("recursive", recursive);
			toolInfo.put("caseSensitive", caseSensitive);
			toolInfo.put("fileExtensions", fileExtensions);
			Map<String, Object> auditResult = audit("grep", toolInfo);
			if (auditResult != null)
				return auditResult;
		}
		
		File target = null;
		try {
			target = FileToolUtil.validateAndGetFile(this.baseDirectories, path, false);
			if (!target.exists()) {
				result.put("success", false);
				result.put("message", "路径不存在: " + path);
				return result;
			}
		} catch (Exception e) {
			result.put("success", false);
			result.put("message", e.getMessage());
			return result;
		}
		
		boolean isRecursive = recursive == null || recursive;
		boolean isCaseSensitive = caseSensitive == null || caseSensitive;
		boolean showLines = showLineNumbers == null || showLineNumbers;
		int maxPerFile = (maxCountPerFile != null && maxCountPerFile > 0) ? maxCountPerFile : 0;
		
		// 构建操作系统命令
		List<String> command = buildCommand(pattern, target.getAbsolutePath(), isRecursive, isCaseSensitive,
				fileExtensions, maxPerFile, showLines);
		
		if (logger.isDebugEnabled()) {
			logger.debug("grep command: {}", JsonUtil.object2json(command));
		}
		
		// 异步执行命令
		CompletableFuture<ProcessOutcome> future = CompletableFuture.supplyAsync(() -> doExecute(command), GREP_EXECUTOR);
		
		ProcessOutcome outcome;
		try {
			if (timeout > 0L) {
				outcome = future.get(timeout, TimeUnit.SECONDS);
			} else {
				outcome = future.get();
			}
		} catch (TimeoutException e) {
			future.cancel(true);
			logger.error("grep command timed out after {} seconds: pattern={}, path={}", timeout, pattern, path);
			result.put("success", false);
			result.put("message", "搜索命令执行超时（限制 " + timeout + " 秒）");
			return result;
		} catch (InterruptedException e) {
			future.cancel(true);
			Thread.currentThread().interrupt();
			logger.error("grep command interrupted: pattern={}, path={}", pattern, path, e);
			result.put("success", false);
			result.put("message", "搜索命令被中断: " + e.getMessage());
			return result;
		} catch (ExecutionException e) {
			Throwable cause = e.getCause() != null ? e.getCause() : e;
			logger.error("grep command execution failed: pattern={}, path={}", pattern, path, cause);
			result.put("success", false);
			result.put("message", "搜索命令执行失败: " + cause.getMessage());
			return result;
		}
		
		if (logger.isDebugEnabled()) {
			logger.debug("grep command exit code: {}, output length: {}", outcome.exitCode, outcome.output.length());
		}
		
		// 解析输出结果
		List<Map<String, Object>> matches = parseOutput(outcome.output, showLines);
		
		// Windows findstr 不支持 maxCountPerFile 限制，在 Java 侧进行后处理截断
		String osName = System.getProperty("os.name").toLowerCase();
		boolean truncated = false;
		if (osName.contains("win") && maxPerFile > 0) {
			int beforeSize = matches.size();
			matches = applyMaxCountPerFile(matches, maxPerFile);
			truncated = matches.size() < beforeSize;
		}
		
		result.put("success", true);
		result.put("pattern", pattern);
		result.put("path", target.getAbsolutePath());
		result.put("matches", matches);
		result.put("totalMatches", matches.size());
		result.put("exitCode", outcome.exitCode);
		if (truncated) {
			result.put("message", "搜索完成，每文件最多保留 " + maxPerFile + " 处匹配（已截断），共返回 " + matches.size() + " 处匹配");
			result.put("truncated", true);
		} else {
			result.put("message", "搜索完成，找到 " + matches.size() + " 处匹配");
		}
		if(logger.isDebugEnabled()) {
			logger.debug("grep command completed: {}", JsonUtil.object2json(result));
		}
		return result;
	}
	
	/**
	 * 统计文件或目录中匹配模式的行数（不返回具体匹配内容，仅返回计数）。
	 * <p>适用于快速统计匹配数量，不关心具体匹配内容的场景。</p>
	 */
	@Tool(name = "grepCount", description = "统计文件或目录中匹配指定模式的文本行数（仅返回计数，不返回具体内容），" +
			"Linux/Mac下调用grep命令、Windows下调用findstr命令。支持正则表达式、递归目录搜索、大小写控制、文件扩展名过滤。")
	public Map<String, Object> grepCount(
			@ToolParam(name = "pattern", description = "搜索模式，支持正则表达式", required = true) String pattern,
			@ToolParam(name = "path", description = "搜索的起始路径，可以是文件或目录", required = true) String path,
			@ToolParam(name = "recursive", description = "路径为目录时是否递归搜索子目录，默认true", required = false) Boolean recursive,
			@ToolParam(name = "caseSensitive", description = "是否区分大小写，默认true", required = false) Boolean caseSensitive,
			@ToolParam(name = "fileExtensions", description = "限定搜索的文件扩展名（如\"*.java\"或\"*.java,*.xml\"），为空则搜索所有文件", required = false) String fileExtensions) {
		
		Map<String, Object> result = new HashMap<String, Object>();
		
		if (SimpleStringUtil.isEmpty(pattern)) {
			result.put("success", false);
			result.put("message", "搜索模式不能为空");
			return result;
		}
		

		
		if (auditor != null) {
			Map<String, Object> toolInfo = new LinkedHashMap<String, Object>();
			toolInfo.put("pattern", pattern);
			toolInfo.put("path", path);
			toolInfo.put("recursive", recursive);
			toolInfo.put("caseSensitive", caseSensitive);
			toolInfo.put("fileExtensions", fileExtensions);
			Map<String, Object> auditResult = audit("grepCount", toolInfo);
			if (auditResult != null)
				return auditResult;
		}
		
		File target = null;
		try {
			target = FileToolUtil.validateAndGetFile(this.baseDirectories, path, false);
			if (!target.exists()) {
				result.put("success", false);
				result.put("message", "路径不存在: " + path);
				return result;
			}
		} catch (Exception e) {
			result.put("success", false);
			result.put("message", e.getMessage());
			return result;
		}
		
		boolean isRecursive = recursive == null || recursive;
		boolean isCaseSensitive = caseSensitive == null || caseSensitive;
		
		// 构建计数命令
		List<String> command = buildCountCommand(pattern, target.getAbsolutePath(), isRecursive, isCaseSensitive, fileExtensions);
		
		if (logger.isDebugEnabled()) {
			logger.debug("grepCount command: {}", command);
		}
		
		CompletableFuture<ProcessOutcome> future = CompletableFuture.supplyAsync(() -> doExecute(command), GREP_EXECUTOR);
		
		ProcessOutcome outcome;
		try {
			if (timeout > 0L) {
				outcome = future.get(timeout, TimeUnit.SECONDS);
			} else {
				outcome = future.get();
			}
		} catch (TimeoutException e) {
			future.cancel(true);
			logger.error("grepCount command timed out after {} seconds: pattern={}, path={}", timeout, pattern, path);
			result.put("success", false);
			result.put("message", "统计命令执行超时（限制 " + timeout + " 秒）");
			return result;
		} catch (InterruptedException e) {
			future.cancel(true);
			Thread.currentThread().interrupt();
			logger.error("grepCount command interrupted: pattern={}, path={}", pattern, path, e);
			result.put("success", false);
			result.put("message", "统计命令被中断: " + e.getMessage());
			return result;
		} catch (ExecutionException e) {
			Throwable cause = e.getCause() != null ? e.getCause() : e;
			logger.error("grepCount command execution failed: pattern={}, path={}", pattern, path, cause);
			result.put("success", false);
			result.put("message", "统计命令执行失败: " + cause.getMessage());
			return result;
		}
		
		// 解析计数结果
		int totalMatches = parseCountOutput(outcome.output);
		
		result.put("success", true);
		result.put("pattern", pattern);
		result.put("path", target.getAbsolutePath());
		result.put("totalMatches", totalMatches);
		result.put("exitCode", outcome.exitCode);
		result.put("message", "统计完成，找到 " + totalMatches + " 处匹配");
		
		return result;
	}
	
	// ==================== 命令构建方法 ====================
	
	/**
	 * 根据操作系统构建 grep/findstr 搜索命令
	 * <p>Windows 上：纯 ASCII 模式使用 findstr（通过 cmd /c 展开通配符）；
	 * 包含非 ASCII 字符（如中文）时改用 PowerShell Select-String，
	 * 因为 findstr 使用系统代码页（如 GBK）编码搜索模式，无法匹配 UTF-8 文件中的非 ASCII 内容。</p>
	 */
	private List<String> buildCommand(String pattern, String path, boolean recursive, boolean caseSensitive,
									  String fileExtensions, int maxPerFile, boolean showLineNumbers) {
		String os = System.getProperty("os.name").toLowerCase();
		boolean isWindows = os.contains("win");
		
		List<String> command = new ArrayList<String>();
		
		if (isWindows) {
			if (containsNonAscii(pattern)) {
				buildPowerShellCommand(command, pattern, path, recursive, caseSensitive, fileExtensions, showLineNumbers);
			} else {
				buildFindstrCommand(command, pattern, path, recursive, caseSensitive, fileExtensions, showLineNumbers);
			}
		} else {
			buildGrepCommand(command, pattern, path, recursive, caseSensitive, fileExtensions, maxPerFile, showLineNumbers);
		}
		
		return command;
	}
	
	/**
	 * 构建 Linux/Mac grep 命令
	 * <p>优化点：
	 * <ul>
	 *   <li>使用 -H 强制显示文件名，便于统一解析</li>
	 *   <li>使用 --color=never 避免 ANSI 转义字符干扰解析</li>
	 *   <li>使用 -D skip 跳过设备文件，避免阻塞</li>
	 *   <li>递归搜索使用 -r 或 -R，根据 followSymlinks 配置决定</li>
	 *   <li>使用 -I 忽略二进制文件，避免输出乱码和性能问题</li>
	 * </ul>
	 * </p>
	 */
	private void buildGrepCommand(List<String> command, String pattern, String path,
								  boolean recursive, boolean caseSensitive,
								  String fileExtensions, int maxPerFile, boolean showLineNumbers) {
		command.add("grep");
		
		// 避免 ANSI 颜色转义字符干扰输出解析
		command.add("--color=never");
		
		// 跳过设备文件，避免阻塞（如 /dev/zero）
		command.add("-D");
		command.add("skip");
		
		// 忽略二进制文件，避免输出乱码和性能问题
		command.add("-I");
		
		// 强制显示文件名，确保单文件搜索时也包含文件名
		command.add("-H");
		
		// 行号
		if (showLineNumbers) {
			command.add("-n");
		}
		
		// 大小写不敏感
		if (!caseSensitive) {
			command.add("-i");
		}
		
		// 递归搜索
		if (recursive) {
			// -r: 递归搜索目录，不跟随符号链接
			// -R: 递归搜索目录，跟随所有符号链接
			// 默认使用 -r 避免循环，用户可通过 setFollowSymlinks 配置
			if (followSymlinks) {
				command.add("-R");
			} else {
				command.add("-r");
			}
		}
		
		// 使用扩展正则表达式
		command.add("-E");
		
		// 扩展名过滤：使用 --include 参数
		// GNU grep 和 BSD grep（Mac）都支持 --include
		if (!SimpleStringUtil.isEmpty(fileExtensions)) {
			String[] exts = fileExtensions.split(",");
			for (String ext : exts) {
				String trimmed = ext.trim();
				if (trimmed.length() > 0) {
					// 确保通配符格式正确
					if (!trimmed.startsWith("*.")) {
						if (trimmed.startsWith(".")) {
							trimmed = "*" + trimmed;
						} else {
							trimmed = "*." + trimmed;
						}
					}
					command.add("--include=" + trimmed);
				}
			}
		}
		
		// 每文件最大匹配数：-m 参数
		if (maxPerFile > 0) {
			command.add("-m");
			command.add(String.valueOf(maxPerFile));
		}
		
		// 搜索模式
		command.add(pattern);
		
		// 搜索路径
		command.add(path);
	}
	
	/**
	 * 构建 Windows findstr 命令（仅适用于纯 ASCII 搜索模式）
	 * <p>findstr 语法：{@code findstr [/n] [/i] [/s] [/r] "pattern" [file...]}</p>
	 * <p>注意：findstr 不支持 grep -m 的每文件最大匹配数限制，maxPerFile 参数在此忽略，
	 * 由 {@link #applyMaxCountPerFile} 在 Java 侧进行后处理截断。</p>
	 * <p>注意：findstr 使用系统代码页编码搜索模式，非 ASCII 字符请使用
	 * {@link #buildPowerShellCommand} 代替。</p>
	 */
	private void buildFindstrCommand(List<String> command, String pattern, String path,
									 boolean recursive, boolean caseSensitive,
									 String fileExtensions, boolean showLineNumbers) {
		command.add("findstr");
		
		// 行号
		if (showLineNumbers) {
			command.add("/n");
		}
		
		// 大小写不敏感
		if (!caseSensitive) {
			command.add("/i");
		}
		
		// 递归搜索
		if (recursive) {
			command.add("/s");
		}
		
		// 使用 /r 正则模式
		command.add("/r");
		
		// 搜索模式（用引号包裹，防止特殊字符被 shell 解释）
		command.add(pattern);
		
		// 构建文件通配符搜索路径
		String searchPath = buildFindstrSearchPath(path, fileExtensions);
		command.add(searchPath);
	}
	
	/**
	 * 构建 findstr 的搜索路径，支持文件扩展名过滤
	 */
	private String buildFindstrSearchPath(String path, String fileExtensions) {
		// 确保路径以分隔符结尾
		String normalizedPath = path.replace('\\', '/');
		if (!normalizedPath.endsWith("/")) {
			normalizedPath += "/";
		}
		
		if (!SimpleStringUtil.isEmpty(fileExtensions)) {
			String[] exts = fileExtensions.split(",");
			List<String> validExts = new ArrayList<String>();
			for (String ext : exts) {
				String trimmed = ext.trim();
				if (trimmed.length() > 0) {
					if (!trimmed.startsWith("*.")) {
						if (trimmed.startsWith(".")) {
							trimmed = "*" + trimmed;
						} else {
							trimmed = "*." + trimmed;
						}
					}
					validExts.add(trimmed);
				}
			}
			if (!validExts.isEmpty()) {
				// findstr 支持多个文件通配符，用空格分隔，每个用引号包裹
				StringBuilder sb = new StringBuilder();
				for (String ext : validExts) {
					if (sb.length() > 0) {
						sb.append(" ");
					}
					sb.append("\"").append(normalizedPath).append(ext).append("\"");
				}
				return sb.toString();
			}
		}
		
		// 没有扩展名过滤，搜索所有文件
		return "\"" + normalizedPath + "*\"";
	}
	
	/**
	 * 构建 Windows PowerShell Select-String 命令。
	 * <p>当搜索模式包含非 ASCII 字符（如中文）时使用此方法，
	 * 因为 findstr 使用系统代码页（如 GBK）编码搜索模式，无法正确匹配 UTF-8 编码文件中的非 ASCII 内容。
	 * PowerShell Select-String 原生支持 UTF-8 编码，能正确处理多字节字符搜索。</p>
	 * <p>输出格式与 findstr /n 一致：{@code 文件名:行号:匹配内容}，便于 {@link #parseOutput} 统一解析。</p>
	 */
	private void buildPowerShellCommand(List<String> command, String pattern, String path,
										boolean recursive, boolean caseSensitive,
										String fileExtensions, boolean showLineNumbers) {
		command.add("powershell");
		command.add("-NoProfile");
		command.add("-NonInteractive");
		command.add("-Command");
		
		StringBuilder psCmd = new StringBuilder();
		
		// 处理路径：转换为 Windows 格式
		String searchPath = path.replace('/', '\\');
		if (searchPath.endsWith("\\")) {
			searchPath = searchPath.substring(0, searchPath.length() - 1);
		}
		
		String includePattern = buildPowerShellIncludePattern(fileExtensions);
		
		// 构建 PowerShell 命令
		psCmd.append("$ErrorActionPreference='Stop'; ");
		
		// Get-ChildItem 获取文件列表
		psCmd.append("Get-ChildItem -Path '").append(searchPath).append("'");
		
		if (recursive) {
			psCmd.append(" -Recurse");
		}
		
		if (!SimpleStringUtil.isEmpty(includePattern)) {
			psCmd.append(" -Include ").append(includePattern);
		}
		
		// 只搜索文件
		psCmd.append(" -File");
		
		// 管道到 Select-String
		psCmd.append(" | Select-String");
		
		// 使用 -SimpleMatch（字面量匹配），因为非 ASCII 模式下 findstr 不支持正则
		psCmd.append(" -SimpleMatch");
		
		// 搜索模式
		psCmd.append(" -Pattern '").append(pattern.replace("'", "''")).append("'");
		
		// 大小写
		if (!caseSensitive) {
			psCmd.append(" -CaseSensitive:$false");
		} else {
			psCmd.append(" -CaseSensitive");
		}
		
		// 格式化输出：文件名:行号:内容 或 文件名:内容
		if (showLineNumbers) {
			psCmd.append(" | ForEach-Object { \"$($_.Path):$($_.LineNumber):$($_.Line)\" }");
		} else {
			psCmd.append(" | ForEach-Object { \"$($_.Path):$($_.Line)\" }");
		}
		
		command.add(psCmd.toString());
	}
	
	/**
	 * 构建 PowerShell 的 -Include 参数
	 */
	private String buildPowerShellIncludePattern(String fileExtensions) {
		if (SimpleStringUtil.isEmpty(fileExtensions)) {
			return "";
		}
		
		String[] exts = fileExtensions.split(",");
		List<String> validExts = new ArrayList<String>();
		for (String ext : exts) {
			String trimmed = ext.trim();
			if (trimmed.length() > 0) {
				if (!trimmed.startsWith("*.")) {
					if (trimmed.startsWith(".")) {
						trimmed = "*" + trimmed;
					} else {
						trimmed = "*." + trimmed;
					}
				}
				validExts.add("'" + trimmed + "'");
			}
		}
		
		if (validExts.isEmpty()) {
			return "";
		}
		
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < validExts.size(); i++) {
			if (i > 0) sb.append(",");
			sb.append(validExts.get(i));
		}
		return sb.toString();
	}
	
	// ==================== 计数命令构建 ====================
	
	/**
	 * 根据操作系统构建 grep/findstr 计数命令
	 */
	private List<String> buildCountCommand(String pattern, String path, boolean recursive,
										   boolean caseSensitive, String fileExtensions) {
		String os = System.getProperty("os.name").toLowerCase();
		boolean isWindows = os.contains("win");
		
		List<String> command = new ArrayList<String>();
		
		if (isWindows) {
			if (containsNonAscii(pattern)) {
				buildPowerShellCountCommand(command, pattern, path, recursive, caseSensitive, fileExtensions);
			} else {
				buildFindstrCountCommand(command, pattern, path, recursive, caseSensitive, fileExtensions);
			}
		} else {
			buildGrepCountCommand(command, pattern, path, recursive, caseSensitive, fileExtensions);
		}
		
		return command;
	}
	
	/**
	 * 构建 Linux/Mac grep 计数命令
	 * <p>使用 -c 参数统计匹配行数，支持递归和扩展名过滤</p>
	 */
	private void buildGrepCountCommand(List<String> command, String pattern, String path,
									   boolean recursive, boolean caseSensitive, String fileExtensions) {
		command.add("grep");
		
		// 避免 ANSI 颜色转义字符
		command.add("--color=never");
		
		// 跳过设备文件
		command.add("-D");
		command.add("skip");
		
		// 忽略二进制文件
		command.add("-I");
		
		// 大小写不敏感
		if (!caseSensitive) {
			command.add("-i");
		}
		
		// 递归搜索
		if (recursive) {
			if (followSymlinks) {
				command.add("-R");
			} else {
				command.add("-r");
			}
		}
		
		// 扩展名过滤
		if (!SimpleStringUtil.isEmpty(fileExtensions)) {
			String[] exts = fileExtensions.split(",");
			for (String ext : exts) {
				String trimmed = ext.trim();
				if (trimmed.length() > 0) {
					if (!trimmed.startsWith("*.")) {
						if (trimmed.startsWith(".")) {
							trimmed = "*" + trimmed;
						} else {
							trimmed = "*." + trimmed;
						}
					}
					command.add("--include=" + trimmed);
				}
			}
		}
		
		// -c 只输出匹配行数
		command.add("-c");
		command.add("-E");
		command.add(pattern);
		command.add(path);
	}
	
	/**
	 * 构建 Windows findstr 计数命令
	 * findstr 没有直接的计数选项，需要在 Java 侧解析输出计数
	 */
	private void buildFindstrCountCommand(List<String> command, String pattern, String path,
										  boolean recursive, boolean caseSensitive, String fileExtensions) {
		buildFindstrCommand(command, pattern, path, recursive, caseSensitive, fileExtensions, false);
	}
	
	/**
	 * 构建 PowerShell 计数命令
	 */
	private void buildPowerShellCountCommand(List<String> command, String pattern, String path,
											 boolean recursive, boolean caseSensitive, String fileExtensions) {
		command.add("powershell");
		command.add("-NoProfile");
		command.add("-NonInteractive");
		command.add("-Command");
		
		StringBuilder psCmd = new StringBuilder();
		
		String searchPath = path.replace('/', '\\');
		if (searchPath.endsWith("\\")) {
			searchPath = searchPath.substring(0, searchPath.length() - 1);
		}
		
		String includePattern = buildPowerShellIncludePattern(fileExtensions);
		
		psCmd.append("$ErrorActionPreference='Stop'; ");
		psCmd.append("(Get-ChildItem -Path '").append(searchPath).append("'");
		
		if (recursive) {
			psCmd.append(" -Recurse");
		}
		
		if (!SimpleStringUtil.isEmpty(includePattern)) {
			psCmd.append(" -Include ").append(includePattern);
		}
		
		psCmd.append(" -File");
		psCmd.append(" | Select-String -SimpleMatch -Pattern '").append(pattern.replace("'", "''")).append("'");
		
		if (!caseSensitive) {
			psCmd.append(" -CaseSensitive:$false");
		} else {
			psCmd.append(" -CaseSensitive");
		}
		
		psCmd.append(").Count");
		
		command.add(psCmd.toString());
	}
	
	// ==================== 命令执行方法 ====================
	
	/**
	 * 执行操作系统命令并返回输出结果
	 */
	private ProcessOutcome doExecute(List<String> command) {
		String os = System.getProperty("os.name").toLowerCase();
		boolean isWindows = os.contains("win");
		
		List<String> effectiveCommand;
		if (isWindows && !command.isEmpty() && "powershell".equalsIgnoreCase(command.get(0))) {
			effectiveCommand = encodePowerShellCommand(command);
		} else if (isWindows) {
			// Windows 命令需要通过 cmd /c 包装，以便展开通配符
			effectiveCommand = new ArrayList<String>();
			effectiveCommand.add("cmd");
			effectiveCommand.add("/c");
			
			// 将整个命令合并为一个字符串，避免参数被二次解析
			StringBuilder cmdLine = new StringBuilder();
			for (int i = 0; i < command.size(); i++) {
				String arg = command.get(i);
				// 如果参数包含空格、通配符或特殊字符，用引号包裹
				boolean needsQuote = arg.contains(" ") || arg.contains("*") || arg.contains("?") ||
						arg.contains("&") || arg.contains("|") || arg.contains("<") || arg.contains(">");
				if (needsQuote && !(arg.startsWith("\"") && arg.endsWith("\""))) {
					cmdLine.append("\"").append(arg).append("\"");
				} else {
					cmdLine.append(arg);
				}
				if (i < command.size() - 1) {
					cmdLine.append(" ");
				}
			}
			effectiveCommand.add(cmdLine.toString());
		} else {
			effectiveCommand = command;
		}
		
		ProcessBuilder processBuilder = new ProcessBuilder(effectiveCommand);
		processBuilder.redirectErrorStream(true);
		
		// 设置工作目录
		if (isWindows) {
			processBuilder.directory(new File(System.getProperty("user.dir")));
		}
		
		Process proc = null;
		try {
			proc = processBuilder.start();
			// Linux 使用 UTF-8，Windows 使用 GBK
			String charset = isWindows ? "GBK" : StandardCharsets.UTF_8.name();
			StringBuilder output = new StringBuilder();
			try (BufferedReader reader = new BufferedReader(
					new InputStreamReader(proc.getInputStream(), charset))) {
				String line;
				while ((line = reader.readLine()) != null) {
					output.append(line).append("\n");
				}
			}
			int exitCode = proc.waitFor();
			String outputStr = output.toString();
			if(logger.isDebugEnabled()) {
				logger.debug("命令输出: {}, 命令退出码: {}", outputStr, exitCode);
			}
			return new ProcessOutcome(outputStr, exitCode);
		} catch (Exception e) {
			throw new RuntimeException("搜索命令执行失败: " + effectiveCommand, e);
		} finally {
			if (proc != null && proc.isAlive()) {
				proc.destroyForcibly();
			}
		}
	}
	
	/**
	 * 将 PowerShell 命令列表中的 -Command 参数转换为 -EncodedCommand，
	 * 使用 Base64(UTF-16LE) 编码脚本文本，完全避免命令行引号解析问题。
	 * <p>编码后的参数仅包含 ASCII 字符，ProcessBuilder 不会对其添加引号，
	 * powershell.exe 能正确解码并执行原始脚本。</p>
	 */
	private List<String> encodePowerShellCommand(List<String> command) {
		List<String> encoded = new ArrayList<String>(command.size());
		int i = 0;
		while (i < command.size()) {
			String arg = command.get(i);
			if ("-Command".equalsIgnoreCase(arg) && i + 1 < command.size()) {
				String script = command.get(i + 1);
				// PowerShell -EncodedCommand 要求 Base64 编码的 UTF-16LE 字节
				byte[] utf16Bytes = script.getBytes(StandardCharsets.UTF_16LE);
				String base64 = Base64.getEncoder().encodeToString(utf16Bytes);
				encoded.add("-EncodedCommand");
				encoded.add(base64);
				i += 2;
			} else {
				encoded.add(arg);
				i++;
			}
		}
		return encoded;
	}
	
	// ==================== 结果解析方法 ====================
	
	/**
	 * 解析 grep/findstr 的输出为结构化结果。
	 * <p>
	 * grep 输出格式（带 -H -n）：{@code 文件名:行号:匹配内容}
	 * findstr 输出格式（带 /n）：{@code 文件名:行号:匹配内容} 或 {@code 文件名:匹配内容}
	 * </p>
	 */
	private List<Map<String, Object>> parseOutput(String output, boolean showLineNumbers) {
		List<Map<String, Object>> matches = new ArrayList<Map<String, Object>>();
		
		if (SimpleStringUtil.isEmpty(output)) {
			return matches;
		}
		
		String[] lines = output.split("\n");
		for (String line : lines) {
			line = line.trim();
			if (line.length() == 0) {
				continue;
			}
			
			Map<String, Object> match = new LinkedHashMap<String, Object>();
			
			// 尝试解析 "文件名:行号:内容" 或 "文件名:内容" 格式
			int firstColon = findFirstColon(line);
			if (firstColon > 0) {
				// 检查是否是 Windows 盘符（如 C:\）
				if (firstColon == 1 && line.length() > 2 && line.charAt(1) == ':') {
					// Windows 绝对路径，继续找下一个冒号
					int secondColon = line.indexOf(':', firstColon + 2);
					if (secondColon > 0) {
						// 检查是否是 "C:\path:行号:内容" 格式
						int thirdColon = line.indexOf(':', secondColon + 1);
						if (thirdColon > 0 && showLineNumbers) {
							String possibleLineNum = line.substring(secondColon + 1, thirdColon);
							if (isNumeric(possibleLineNum)) {
								match.put("file", line.substring(0, secondColon + 1));
								match.put("lineNumber", Integer.parseInt(possibleLineNum));
								match.put("content", line.substring(thirdColon + 1).trim());
								matches.add(match);
								continue;
							}
						}
						// 没有行号或无法解析行号
						match.put("file", line.substring(0, secondColon + 1));
						match.put("content", line.substring(secondColon + 1).trim());
						matches.add(match);
						continue;
					}
				}
				
				// 非 Windows 路径的情况
				if (showLineNumbers) {
					// 尝试解析 "xxx:行号:内容"
					int secondColon = line.indexOf(':', firstColon + 1);
					if (secondColon > 0) {
						String possibleLineNum = line.substring(firstColon + 1, secondColon);
						if (isNumeric(possibleLineNum)) {
							match.put("file", line.substring(0, firstColon));
							match.put("lineNumber", Integer.parseInt(possibleLineNum));
							match.put("content", line.substring(secondColon + 1).trim());
							matches.add(match);
							continue;
						}
					}
				}
				
				// 没有行号的情况： "文件名:内容"
				match.put("file", line.substring(0, firstColon));
				match.put("content", line.substring(firstColon + 1).trim());
				matches.add(match);
			} else {
				// 无法解析格式，直接作为匹配内容
				match.put("content", line);
				matches.add(match);
			}
		}
		
		return matches;
	}
	
	/**
	 * 解析 grep -c 的计数输出
	 * <p>
	 * grep -c 输出格式：
	 * <ul>
	 *   <li>单文件：{@code 匹配数}</li>
	 *   <li>多文件：每行 {@code 文件名:匹配数}</li>
	 *   <li>递归搜索：每行 {@code 目录/文件名:匹配数}</li>
	 * </ul>
	 * </p>
	 */
	private int parseCountOutput(String output) {
		if (SimpleStringUtil.isEmpty(output)) {
			return 0;
		}
		
		int total = 0;
		String[] lines = output.split("\n");
		for (String line : lines) {
			line = line.trim();
			if (line.length() == 0) {
				continue;
			}
			
			// 对于 PowerShell 计数命令，直接返回数字
			if (isNumeric(line)) {
				total += Integer.parseInt(line);
				continue;
			}
			
			// grep -c 输出格式：filename:count
			// 注意：文件名本身可能包含冒号（如 Windows 路径 C:\），
			// 所以从最后一个冒号分割
			int lastColon = line.lastIndexOf(':');
			if (lastColon > 0) {
				String countStr = line.substring(lastColon + 1).trim();
				if (isNumeric(countStr)) {
					total += Integer.parseInt(countStr);
				}
			} else if (isNumeric(line)) {
				total += Integer.parseInt(line);
			}
		}
		
		return total;
	}
	
	// ==================== 私有辅助方法 ====================
	
	/**
	 * 对搜索结果应用每文件最大匹配数限制（用于 Windows findstr 的后处理，
	 * 因为 findstr 不支持 grep -m 参数）
	 */
	private List<Map<String, Object>> applyMaxCountPerFile(List<Map<String, Object>> matches, int maxPerFile) {
		List<Map<String, Object>> filtered = new ArrayList<Map<String, Object>>();
		Map<String, Integer> fileCounts = new HashMap<String, Integer>();
		for (Map<String, Object> match : matches) {
			String file = (String) match.get("file");
			if (file != null) {
				int count = fileCounts.containsKey(file) ? fileCounts.get(file) : 0;
				if (count >= maxPerFile) {
					continue;
				}
				fileCounts.put(file, count + 1);
			}
			filtered.add(match);
		}
		return filtered;
	}
	
	/**
	 * 查找字符串中第一个冒号的位置，跳过 Windows 盘符（如 C:）
	 */
	private int findFirstColon(String line) {
		int idx = line.indexOf(':');
		// 如果第一个冒号在位置1（Windows 盘符 C:），则找下一个冒号
		if (idx == 1 && line.length() > 2 && (line.charAt(2) == '\\' || line.charAt(2) == '/')) {
			return line.indexOf(':', idx + 1);
		}
		return idx;
	}
	
	/**
	 * 判断字符串是否包含非 ASCII 字符（如中文、日文等多字节字符）。
	 * <p>Windows findstr 的 /r 正则模式不支持多字节字符，
	 * 当搜索模式包含非 ASCII 字符时必须禁用 /r 改用字面量搜索。</p>
	 */
	private boolean containsNonAscii(String str) {
		if (str == null) {
			return false;
		}
		for (int i = 0; i < str.length(); i++) {
			if (str.charAt(i) > 127) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 判断字符串是否为纯数字
	 */
	private boolean isNumeric(String str) {
		if (str == null || str.length() == 0) {
			return false;
		}
		for (int i = 0; i < str.length(); i++) {
			if (!Character.isDigit(str.charAt(i))) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * 命令执行结果
	 */
	private static class ProcessOutcome {
		final String output;
		final int exitCode;
		
		ProcessOutcome(String output, int exitCode) {
			this.output = output;
			this.exitCode = exitCode;
		}
	}
}