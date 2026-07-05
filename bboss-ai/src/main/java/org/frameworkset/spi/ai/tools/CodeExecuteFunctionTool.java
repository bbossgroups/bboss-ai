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
import org.frameworkset.spi.ai.model.annotation.Tool;
import org.frameworkset.spi.ai.model.annotation.ToolParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 多语言代码执行工具类。
 * <p>
 * 提供 Java、Python、JavaScript 代码的动态执行能力，支持编译、运行与超时控制，
 * 适用于智能体通过 {@link Tool} 注解暴露为可调用工具的场景。
 * <ul>
 *   <li>Java：通过 {@link JavaCompiler} 编译后反射执行 {@code main} 方法</li>
 *   <li>Python：通过系统进程调用 {@code python} / {@code python3} 解释器执行</li>
 *   <li>JavaScript：优先使用 JDK 内置 {@code Nashorn} 脚本引擎，引擎不可用时回退到 {@code node} 进程</li>
 * </ul>
 * </p>
 *
 * @author biaoping.yin
 * @Date 2026/7/2
 */
public class CodeExecuteFunctionTool {
	private static final Logger logger = LoggerFactory.getLogger(CodeExecuteFunctionTool.class);
	
	/** 提取代码中 public class 类名的正则 */
	private static final Pattern PUBLIC_CLASS_PATTERN = Pattern.compile("public\\s+class\\s+(\\w+)");
	
	/** 独立线程池，避免长时间代码执行阻塞 ForkJoinPool.commonPool */
	private static final ExecutorService CODE_EXECUTOR = Executors.newCachedThreadPool(r -> {
		Thread t = new Thread(r, "code-execute-executor");
		t.setDaemon(true);
		return t;
	});
	
	/** 默认超时时间（秒） */
	private long timeout = 60;
	
	/** 临时编译输出目录 */
	private String workspaceDir = System.getProperty("java.io.tmpdir");
	
	/** 临时文件根目录前缀 */
	private static final String TEMP_DIR_PREFIX = "code_execute_";
	
	public CodeExecuteFunctionTool() {
	}
	
	public CodeExecuteFunctionTool(long timeout) {
		this.timeout = timeout;
	}
	
	public CodeExecuteFunctionTool setTimeout(long timeout) {
		this.timeout = timeout;
		return this;
	}
	
	public CodeExecuteFunctionTool setWorkspaceDir(String workspaceDir) {
		this.workspaceDir = workspaceDir;
		return this;
	}
	
	// ==================== 公开工具方法 ====================
	
	/**
	 * 执行 Java 代码。
	 * <p>
	 * 若代码中已包含 {@code public class XXX}，则直接编译并运行该类；
	 * 否则自动将代码包装到 {@code public class Main} 的 {@code main} 方法中执行。
	 * </p>
	 */
	@Tool(name = "executeJava",
			description = "编译并执行 Java 代码。若代码中已包含 public class，则直接编译运行；否则自动包装到 Main 类的 main 方法中执行。返回编译错误或运行输出。")
	public Map<String, Object> executeJava(
			@ToolParam(name = "code", description = "待执行的 Java 源代码", required = true) String code) {
		Map<String, Object> result = new HashMap<>();
		if (SimpleStringUtil.isEmpty(code)) {
			result.put("success", false);
			result.put("message", "代码不能为空");
			return result;
		}
		
		String className = extractPublicClassName(code);
		String finalCode;
		if (className == null) {
			className = "Main";
			finalCode = wrapJavaCode(code);
		} else {
			finalCode = code;
		}
		
		String baseDir = getBaseTempDir();
		final String outputDir = baseDir + "/java_" + UUID.randomUUID().toString().replace("-", "");
		final String className_ = className;
		CompletableFuture<ExecutionOutcome> future = CompletableFuture.supplyAsync(
				() -> doExecuteJava(finalCode, className_, outputDir), CODE_EXECUTOR);
		
		return handleFuture(future, result, "Java");
	}
	
	/**
	 * 执行 Python 代码。
	 */
	@Tool(name = "executePython",
			description = "通过系统 python/python3 解释器执行 Python 代码，返回标准输出与标准错误。")
	public Map<String, Object> executePython(
			@ToolParam(name = "code", description = "待执行的 Python 源代码", required = true) String code) {
		Map<String, Object> result = new HashMap<>();
		if (SimpleStringUtil.isEmpty(code)) {
			result.put("success", false);
			result.put("message", "代码不能为空");
			return result;
		}
		
		String baseDir = getBaseTempDir();
		final String workDir = baseDir + "/python_" + UUID.randomUUID().toString().replace("-", "");
		CompletableFuture<ExecutionOutcome> future = CompletableFuture.supplyAsync(
				() -> doExecutePython(code, workDir), CODE_EXECUTOR);
		
		return handleFuture(future, result, "Python");
	}
	
	/**
	 * 执行 JavaScript 代码。
	 * <p>
	 * 优先使用 JDK 内置 Nashorn 脚本引擎执行；若引擎不可用，则回退到系统 {@code node} 命令。
	 * </p>
	 */
	@Tool(name = "executeJavaScript",
			description = "执行 JavaScript 代码。优先使用 JDK 内置 Nashorn 引擎，引擎不可用时回退到系统 node 命令。")
	public Map<String, Object> executeJavaScript(
			@ToolParam(name = "code", description = "待执行的 JavaScript 源代码", required = true) String code) {
		Map<String, Object> result = new HashMap<>();
		if (SimpleStringUtil.isEmpty(code)) {
			result.put("success", false);
			result.put("message", "代码不能为空");
			return result;
		}
		
		String baseDir = getBaseTempDir();
		final String workDir = baseDir + "/js_" + UUID.randomUUID().toString().replace("-", "");
		CompletableFuture<ExecutionOutcome> future = CompletableFuture.supplyAsync(
				() -> doExecuteJavaScript(code, workDir), CODE_EXECUTOR);
		
		return handleFuture(future, result, "JavaScript");
	}
	
	// ==================== 私有执行方法 ====================
	
	private ExecutionOutcome doExecuteJava(String code, String className, String outputDir) {
		File dir = new File(outputDir);
		if (!dir.exists() && !dir.mkdirs()) {
			throw new RuntimeException("无法创建编译输出目录: " + outputDir);
		}
		
		try {
			// 1. 编译
			JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
			if (compiler == null) {
				throw new RuntimeException("当前环境未提供 JavaCompiler，无法编译 Java 代码");
			}
			
			DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
			StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8);
			
			JavaFileObject sourceFile = new StringJavaFileObject(className, code);
			Iterable<? extends JavaFileObject> compilationUnits = Arrays.asList(sourceFile);
			
			Iterable<String> options = Arrays.asList("-d", outputDir, "-encoding", "UTF-8");
			
			JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, options, null, compilationUnits);
			boolean success = task.call();
			fileManager.close();
			
			if (!success) {
				StringBuilder errorMsg = new StringBuilder("编译失败:\n");
				for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
					errorMsg.append(diagnostic.getLineNumber()).append(": ").append(diagnostic.getMessage(null)).append("\n");
				}
				return new ExecutionOutcome(errorMsg.toString(), 1);
			}
			
			// 2. 加载并执行
			URLClassLoader classLoader = new URLClassLoader(new URL[]{dir.toURI().toURL()}, this.getClass().getClassLoader());
			try {
				Class<?> clazz = Class.forName(className, true, classLoader);
				Method mainMethod = clazz.getMethod("main", String[].class);
				
				ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
				ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();
				PrintStream originalOut = System.out;
				PrintStream originalErr = System.err;
				
				synchronized (CodeExecuteFunctionTool.class) {
					System.setOut(new PrintStream(outBuffer, true, "UTF-8"));
					System.setErr(new PrintStream(errBuffer, true, "UTF-8"));
					try {
						mainMethod.invoke(null, (Object) new String[0]);
					} finally {
						System.setOut(originalOut);
						System.setErr(originalErr);
					}
				}
				
				String output = outBuffer.toString("UTF-8");
				String error = errBuffer.toString("UTF-8");
				String fullOutput = output + (error.isEmpty() ? "" : "\n[STDERR]\n" + error);
				return new ExecutionOutcome(fullOutput, 0);
			} finally {
				try {
					classLoader.close();
				} catch (IOException ignored) {
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("Java 代码执行失败: " + e.getMessage(), e);
		} finally {
			// 清理临时编译产物
			deleteDirectory(dir);
		}
	}
	
	private ExecutionOutcome doExecutePython(String code, String workDir) {
		String pythonCmd = findPythonCommand();
		if (pythonCmd == null) {
			throw new RuntimeException("未找到 python 或 python3 解释器，请确保已安装 Python 并将其加入系统 PATH");
		}
		
		File workDirFile = new File(workDir);
		if (!workDirFile.exists() && !workDirFile.mkdirs()) {
			throw new RuntimeException("无法创建工作目录: " + workDir);
		}
		
		File tempFile = null;
		try {
			tempFile = new File(workDir, "script_" + UUID.randomUUID().toString().replace("-", "") + ".py");
			try (Writer writer = new OutputStreamWriter(new java.io.FileOutputStream(tempFile), StandardCharsets.UTF_8)) {
				writer.write(code);
			}
			
			ProcessBuilder pb = new ProcessBuilder(pythonCmd, tempFile.getAbsolutePath());
			pb.directory(workDirFile);
			pb.redirectErrorStream(true);
			Process proc = pb.start();
			
			StringBuilder output = new StringBuilder();
			try (java.io.BufferedReader reader = new java.io.BufferedReader(
					new java.io.InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					output.append(line).append("\n");
				}
			}
			
			int exitCode = proc.waitFor();
			return new ExecutionOutcome(output.toString(), exitCode);
		} catch (Exception e) {
			throw new RuntimeException("Python 代码执行失败: " + e.getMessage(), e);
		} finally {
			if (tempFile != null && tempFile.exists()) {
				tempFile.delete();
			}
			// 清理工作目录
			deleteDirectory(workDirFile);
		}
	}
	
	private ExecutionOutcome doExecuteJavaScript(String code, String workDir) {
		// 优先尝试 JDK 内置 Nashorn 引擎
		javax.script.ScriptEngine engine = tryGetNashornEngine();
		if (engine != null) {
			try {
				ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
				ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();
				PrintStream originalOut = System.out;
				PrintStream originalErr = System.err;
				
				Object result;
				synchronized (CodeExecuteFunctionTool.class) {
					System.setOut(new PrintStream(outBuffer, true, "UTF-8"));
					System.setErr(new PrintStream(errBuffer, true, "UTF-8"));
					try {
						result = engine.eval(code);
					} finally {
						System.setOut(originalOut);
						System.setErr(originalErr);
					}
				}
				
				String output = outBuffer.toString("UTF-8");
				String error = errBuffer.toString("UTF-8");
				StringBuilder fullOutput = new StringBuilder();
				if (result != null) {
					fullOutput.append("[返回值] ").append(result.toString()).append("\n");
				}
				fullOutput.append(output);
				if (!error.isEmpty()) {
					fullOutput.append("\n[STDERR]\n").append(error);
				}
				return new ExecutionOutcome(fullOutput.toString(), 0);
			} catch (Exception e) {
				throw new RuntimeException("JavaScript 引擎执行失败: " + e.getMessage(), e);
			}
		}
		
		// 回退到 node 命令
		return executeJavaScriptByNode(code, workDir);
	}
	
	private ExecutionOutcome executeJavaScriptByNode(String code, String workDir) {
		File workDirFile = new File(workDir);
		if (!workDirFile.exists() && !workDirFile.mkdirs()) {
			throw new RuntimeException("无法创建工作目录: " + workDir);
		}
		
		File tempFile = null;
		try {
			tempFile = new File(workDir, "script_" + UUID.randomUUID().toString().replace("-", "") + ".js");
			try (Writer writer = new OutputStreamWriter(new java.io.FileOutputStream(tempFile), StandardCharsets.UTF_8)) {
				writer.write(code);
			}
			
			ProcessBuilder pb = new ProcessBuilder("node", tempFile.getAbsolutePath());
			pb.directory(workDirFile);
			pb.redirectErrorStream(true);
			Process proc = pb.start();
			
			StringBuilder output = new StringBuilder();
			try (java.io.BufferedReader reader = new java.io.BufferedReader(
					new java.io.InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					output.append(line).append("\n");
				}
			}
			
			int exitCode = proc.waitFor();
			return new ExecutionOutcome(output.toString(), exitCode);
		} catch (Exception e) {
			throw new RuntimeException("Node 执行 JavaScript 失败: " + e.getMessage(), e);
		} finally {
			if (tempFile != null && tempFile.exists()) {
				tempFile.delete();
			}
			// 清理工作目录
			deleteDirectory(workDirFile);
		}
	}
	
	// ==================== 通用辅助方法 ====================
	
	/**
	 * 获取基础临时目录
	 */
	private String getBaseTempDir() {
		String baseDir = workspaceDir;
		if (baseDir == null || baseDir.trim().isEmpty()) {
			baseDir = System.getProperty("java.io.tmpdir");
		}
		// 去除末尾分隔符
		while (baseDir.endsWith("/") || baseDir.endsWith("\\")) {
			baseDir = baseDir.substring(0, baseDir.length() - 1);
		}
		return baseDir;
	}
	
	private Map<String, Object> handleFuture(CompletableFuture<ExecutionOutcome> future,
	                                         Map<String, Object> result, String language) {
		ExecutionOutcome outcome;
		try {
			if (timeout > 0L) {
				outcome = future.get(timeout, TimeUnit.SECONDS);
			} else {
				outcome = future.get();
			}
		} catch (TimeoutException e) {
			future.cancel(true);
			logger.error("{} 代码执行超时（限制 {} 秒）", language, timeout);
			result.put("success", false);
			result.put("message", language + " 代码执行超时（限制 " + timeout + " 秒）");
			return result;
		} catch (InterruptedException e) {
			future.cancel(true);
			Thread.currentThread().interrupt();
			logger.error("{} 代码执行被中断", language, e);
			result.put("success", false);
			result.put("message", language + " 代码执行被中断: " + e.getMessage());
			return result;
		} catch (ExecutionException e) {
			Throwable cause = e.getCause() != null ? e.getCause() : e;
			logger.error("{} 代码执行出错", language, cause);
			result.put("success", false);
			result.put("message", language + " 代码执行失败: " + cause.getMessage());
			return result;
		}
		
		boolean success = outcome.exitCode == 0;
		result.put("success", success);
		result.put("exitCode", outcome.exitCode);
		result.put("output", outcome.output);
		result.put("message", success ? language + " 代码执行成功" : language + " 代码执行返回非零退出码: " + outcome.exitCode);
		return result;
	}
	
	private String extractPublicClassName(String code) {
		Matcher matcher = PUBLIC_CLASS_PATTERN.matcher(code);
		if (matcher.find()) {
			return matcher.group(1);
		}
		return null;
	}
	
	private String wrapJavaCode(String code) {
		return "import java.util.*;\n" +
				"import java.io.*;\n" +
				"import java.math.*;\n" +
				"import java.nio.*;\n" +
				"import java.time.*;\n" +
				"import java.util.stream.*;\n" +
				"\n" +
				"public class Main {\n" +
				"    public static void main(String[] args) throws Exception {\n" +
				"        " + code.replace("\n", "\n        ") + "\n" +
				"    }\n" +
				"}\n";
	}
	
	private String findPythonCommand() {
		if (commandExists("python3")) {
			return "python3";
		}
		if (commandExists("python")) {
			return "python";
		}
		return null;
	}
	
	private boolean commandExists(String cmd) {
		try {
			String os = System.getProperty("os.name").toLowerCase();
			ProcessBuilder pb;
			if (os.contains("win")) {
				pb = new ProcessBuilder("cmd", "/c", "where", cmd);
			} else {
				pb = new ProcessBuilder("which", cmd);
			}
			pb.redirectErrorStream(true);
			Process proc = pb.start();
			int exitCode = proc.waitFor();
			return exitCode == 0;
		} catch (Exception e) {
			return false;
		}
	}
	
	private javax.script.ScriptEngine tryGetNashornEngine() {
		try {
			javax.script.ScriptEngineManager manager = new javax.script.ScriptEngineManager();
			javax.script.ScriptEngine engine = manager.getEngineByName("nashorn");
			if (engine == null) {
				engine = manager.getEngineByName("JavaScript");
			}
			return engine;
		} catch (Exception e) {
			return null;
		}
	}
	
	private void deleteDirectory(File directory) {
		if (directory == null || !directory.exists()) {
			return;
		}
		File[] files = directory.listFiles();
		if (files != null) {
			for (File f : files) {
				if (f.isDirectory()) {
					deleteDirectory(f);
				} else {
					if (!f.delete()) {
						logger.warn("无法删除临时文件: {}", f.getAbsolutePath());
					}
				}
			}
		}
		if (!directory.delete()) {
			logger.warn("无法删除临时目录: {}", directory.getAbsolutePath());
		}
	}
	
	// ==================== 内部数据类 ====================
	
	private static class ExecutionOutcome {
		final String output;
		final int exitCode;
		
		ExecutionOutcome(String output, int exitCode) {
			this.output = output;
			this.exitCode = exitCode;
		}
	}
	
	/**
	 * 基于字符串的 JavaFileObject，用于内存编译
	 */
	private static class StringJavaFileObject extends SimpleJavaFileObject {
		private final String code;
		
		StringJavaFileObject(String name, String code) {
			super(new File(name + ".java").toURI(), Kind.SOURCE);
			this.code = code;
		}
		
		@Override
		public CharSequence getCharContent(boolean ignoreEncodingErrors) {
			return code;
		}
	}
}