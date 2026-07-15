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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * CLI Shell 命令执行工具类。
 * <p>
 * 提供跨平台（Windows、Linux、Unix、Mac）的 shell 命令执行能力，支持同步阻塞与超时控制，
 * 适用于智能体通过 {@link Tool} 注解暴露为可调用工具的场景。
 * </p>
 *
 * @author biaoping.yin
 * @Date 2026/6/23
 */
public class CLIShellFunctionTool {
    private static final Logger logger = LoggerFactory.getLogger(CLIShellFunctionTool.class);

    /** 独立线程池，避免长时间 shell 命令阻塞 ForkJoinPool.commonPool */
    private static final ExecutorService SHELL_EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "cli-shell-executor");
        t.setDaemon(true);
        return t;
    });

    /** Default timeout in seconds */
    private long timeout = 60;

    public CLIShellFunctionTool() {
    }

    public CLIShellFunctionTool(long timeout) {
        this.timeout = timeout;
    }

    public CLIShellFunctionTool setTimeout(long timeout) {
        this.timeout = timeout;
        return this;
    }

    @Tool(name = "executeBash", description = "shell工具，执行shell脚本：可以通过Java Process调用cmd或者sh来执行shell脚本，返回执行结果，支持linux、unix、mac以及windows等系统的shell脚本执行")
    public Map<String, Object> executeBash(@ToolParam(name = "command", description = "合法的可执行的shell脚本", required = true) String command) {
        Map<String, Object> result = new HashMap<>();

        if (SimpleStringUtil.isEmpty(command)) {
            result.put("executeResult", "没有输入命令，忽略执行!");
            return result;
        }

        CompletableFuture<ProcessOutcome> future = CompletableFuture.supplyAsync(() -> doExecute(command), SHELL_EXECUTOR);

        ProcessOutcome outcome;
        try {
            if (timeout > 0L) {
                outcome = future.get(timeout, TimeUnit.SECONDS);
            } else {
                outcome = future.get();
            }
        } catch (TimeoutException e) {
            future.cancel(true);
            logger.error("Command execution timed out after {} seconds: {}", timeout, command);
            result.put("executeResult", "命令执行超时（限制 " + timeout + " 秒）");
            return result;
        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            logger.error("Command execution interrupted: {}", command, e);
            result.put("executeResult", "命令执行被中断: " + e.getMessage());
            return result;
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            logger.error("Error executing command: {}", command, cause);
            result.put("executeResult", "命令执行失败: " + cause.getMessage());
            return result;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Command executed successfully: {}", command);
            logger.debug("Command output: {}", outcome.output);
        }

        result.put("executeResult", outcome.output);
        return result;
    }

    private ProcessOutcome doExecute(String command) {
        String os = System.getProperty("os.name").toLowerCase();
        boolean isWindows = os.contains("win");
        String charset = isWindows ? "GBK" : StandardCharsets.UTF_8.name();

        ProcessBuilder processBuilder;
        if (isWindows) {
            processBuilder = new ProcessBuilder("cmd", "/c", command);
        } else {
            processBuilder = new ProcessBuilder("sh", "-c", command);
        }

        processBuilder.redirectErrorStream(true);
        Process proc = null;
        try {
            proc = processBuilder.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream(), charset))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            int exitCode = proc.waitFor();
            return new ProcessOutcome(output.toString(), exitCode);
        } catch (Exception e) {
            throw new RuntimeException("Command execution failed: " + command, e);
        } finally {
            if (proc != null && proc.isAlive()) {
                proc.destroyForcibly();
            }
        }
    }

    private static class ProcessOutcome {
        final String output;
        final int exitCode;

        ProcessOutcome(String output, int exitCode) {
            this.output = output;
            this.exitCode = exitCode;
        }
    }
}
