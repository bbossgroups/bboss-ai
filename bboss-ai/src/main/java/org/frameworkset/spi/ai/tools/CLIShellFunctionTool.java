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

import org.frameworkset.spi.ai.model.annotation.Tool;
import org.frameworkset.spi.ai.model.annotation.ToolParam;
import org.slf4j.Logger;

import java.util.Map;

/**
 * @author biaoping.yin
 * @Date 2026/6/23
 */
public class CLIShellFunctionTool {
    private static Logger logger = org.slf4j.LoggerFactory.getLogger(CLIShellFunctionTool.class);
	/** Default timeout in seconds*/
	private long timeout = 60;
	public CLIShellFunctionTool(){
		
	}
	public CLIShellFunctionTool(long timeout){
		this.timeout = timeout;
	}
	
	public CLIShellFunctionTool setTimeout(long timeout) {
		this.timeout = timeout;
		return this;
	}
	
	@Tool(name ="executeShell",description = "执行shell脚本:支持linux 和windows shell脚本执行，并返回执行结果，注意参数shell不能为空！")
    public Map executeShell(@ToolParam(name = "shell",description = "shell脚本",required = true) String shell){
        Map result = new java.util.LinkedHashMap<>();
        try {
            java.util.concurrent.CompletableFuture<String> future = java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                java.io.File tempScript = null;
                try {
                    String os = System.getProperty("os.name").toLowerCase();
                    boolean isWindows = os.contains("win");
                    boolean isScript = shell != null && (shell.contains("\n") || shell.contains("\r"));

                    ProcessBuilder processBuilder;
                    if (isScript) {
                        String suffix = isWindows ? ".bat" : ".sh";
                        tempScript = java.io.File.createTempFile("cli_script_", suffix);
                        java.nio.file.Files.write(tempScript.toPath(),
                                shell.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        if (!isWindows) {
                            tempScript.setExecutable(true);
                        }
                        if (isWindows) {
                            processBuilder = new ProcessBuilder("cmd", "/c", tempScript.getAbsolutePath());
                        } else {
                            processBuilder = new ProcessBuilder("sh", tempScript.getAbsolutePath());
                        }
                    } else {
                        if (isWindows) {
                            processBuilder = new ProcessBuilder("cmd", "/c", shell);
                        } else {
                            processBuilder = new ProcessBuilder("sh", "-c", shell);
                        }
                    }

                    processBuilder.redirectErrorStream(true);
                    Process proc = processBuilder.start();
                    StringBuilder output = new StringBuilder();
                    try (java.io.BufferedReader reader = new java.io.BufferedReader(
                            new java.io.InputStreamReader(proc.getInputStream(), "UTF-8"))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            output.append(line).append("\n");
                        }
                    }
                    proc.waitFor();
                    return output.toString();
                } catch (Exception e) {
                    throw new RuntimeException("Command execution failed: " + shell, e);
                } finally {
                    if (tempScript != null && tempScript.exists()) {
                        tempScript.delete();
                    }
                }
            });
			String executeResult = null;
			if(timeout > 0L) {
				executeResult = future.get(timeout, java.util.concurrent.TimeUnit.SECONDS);
			}
			else{
				executeResult = future.get();
			}
            result.put("executeResult", executeResult);
        } catch (Exception e) {
            logger.error("Error executing command: " + shell, e);
            result.put("executeResult", "Error: " + e.getMessage());
        }
        return result;
    }

}
