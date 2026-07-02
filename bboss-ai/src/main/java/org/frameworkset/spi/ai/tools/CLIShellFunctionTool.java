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
    
 
	@Tool(name ="executeBash",description = "执行shell脚本：可以通过Java Process调用cmd或者sh来执行shell脚本，返回执行结果，支持linux、unix、mac以及windows等系统的shell脚本执行")
    public Map executeBash(@ToolParam(name = "command",description = "合法的可执行的shell脚本",required = true) String command){
        String executeResult = null;       
        try {
            if(SimpleStringUtil.isEmpty(command)){
                Map result = new java.util.HashMap();
                result.put("executeResult","没有输入命令，忽略执行!");
                return result;
            }
            java.util.concurrent.CompletableFuture<String> future = java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                java.io.File tempScript = null;
                try {
                    String os = System.getProperty("os.name").toLowerCase();
                    boolean isWindows = os.contains("win");
//                    boolean isScript = command != null && (command.contains("\n") || command.contains("\r"));
                    // 根据操作系统选择对应的字符集
                    String charset = isWindows ? "GBK" : java.nio.charset.StandardCharsets.UTF_8.name();

                    ProcessBuilder processBuilder;
//                    if (isScript) {
//                        String suffix = isWindows ? ".bat" : ".sh";
//                        tempScript = java.io.File.createTempFile("cli_script_", suffix);
//                        java.nio.file.Files.write(tempScript.toPath(),
//                                shell.getBytes(java.nio.charset.StandardCharsets.UTF_8));
//                        if (!isWindows) {
//                            tempScript.setExecutable(true);
//                        }
//                        if (isWindows) {
//                            processBuilder = new ProcessBuilder("cmd", "/c", tempScript.getAbsolutePath());
//                        } else {
//                            processBuilder = new ProcessBuilder("sh", tempScript.getAbsolutePath());
//                        }
//                    } else {
                        if (isWindows) {
                            processBuilder = new ProcessBuilder("cmd", "/c",  "chcp", "65001", ">", "nul", "&&",command);
                        } else {
                            processBuilder = new ProcessBuilder("sh", "-c", command);
                        }
//                    }

                    processBuilder.redirectErrorStream(true);
                    Process proc = processBuilder.start();
                    StringBuilder output = new StringBuilder();
                    try (java.io.BufferedReader reader = new java.io.BufferedReader(
                            new java.io.InputStreamReader(proc.getInputStream(), charset))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            output.append(line).append("\n");
                        }
                    }
                    proc.waitFor();
                    
                    return output.toString();
                } catch (Exception e) {
                    throw new RuntimeException("Command execution failed: " + command, e);
                } finally {
                    if (tempScript != null && tempScript.exists()) {
                        tempScript.delete();
                    }
                }
            });
			
			if(timeout > 0L) {
				executeResult = future.get(timeout, java.util.concurrent.TimeUnit.SECONDS);
			}
			else{
				executeResult = future.get();
			}
            if(logger.isDebugEnabled()) {
                logger.info("Command executed successfully: {}", command);
                logger.info("Command output: {}", executeResult);
            }
        } catch (Exception e) {
            logger.error("Error executing command: " + command, e);
        }
        Map result = new java.util.HashMap();
        result.put("executeResult",executeResult);
        return result;
    }

}
