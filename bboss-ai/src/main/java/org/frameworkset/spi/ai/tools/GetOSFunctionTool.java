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
import org.slf4j.Logger;

import java.util.Map;

/**
 * 操作系统信息查询工具类。
 * <p>
 * 提供获取当前运行环境操作系统名称及版本信息的能力，
 * 适用于智能体通过 {@link Tool} 注解暴露为可调用工具的场景。
 * </p>
 *
 * @author biaoping.yin
 * @Date 2026/6/23
 */
public class GetOSFunctionTool {
    private static Logger logger = org.slf4j.LoggerFactory.getLogger(GetOSFunctionTool.class);
	/** Default timeout in seconds*/
	private long timeout = 60;
	public GetOSFunctionTool(){
		
	}
	public GetOSFunctionTool(long timeout){
		this.timeout = timeout;
	}
	
	public GetOSFunctionTool setTimeout(long timeout) {
		this.timeout = timeout;
		return this;
	}
    @Tool(name="getOS2ndCpu",description = "获取OS、OS版本、OS架构以及CPU信息")
    public Map getOS2ndCpu(){
        String os = System.getProperty("os.name");
        String osVersion = System.getProperty("os.version");
        String osArch = System.getProperty("os.arch");
        int cpuCores = Runtime.getRuntime().availableProcessors();
        String cpuName = getCpuName();
        Map result = new java.util.HashMap();
        result.put("os", os);
        result.put("osVersion", osVersion);
        result.put("osArch", osArch);
        result.put("cpuCores", cpuCores);
        result.put("cpuName", cpuName);
        return result;
    }
    
    /**
     * 获取CPU名称/型号信息
     */
    private String getCpuName(){
        try {
            String osName = System.getProperty("os.name");
            if (osName != null && osName.toLowerCase().contains("win")) {
                Process process = Runtime.getRuntime().exec(new String[]{"wmic", "cpu", "get", "Name"});
                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty() && !line.equalsIgnoreCase("Name")) {
                        reader.close();
                        process.destroy();
                        return line;
                    }
                }
                reader.close();
                process.destroy();
            } else if (osName != null && (osName.toLowerCase().contains("linux") || osName.toLowerCase().contains("mac"))) {
                Process process = Runtime.getRuntime().exec(new String[]{"uname", "-p"});
                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()));
                String cpuInfo = reader.readLine();
                reader.close();
                process.destroy();
                if (cpuInfo != null && !cpuInfo.trim().isEmpty() && !cpuInfo.trim().equals("unknown")) {
                    return cpuInfo.trim();
                }
                // Linux下尝试读取/proc/cpuinfo
                if (osName.toLowerCase().contains("linux")) {
                    Process proc = Runtime.getRuntime().exec(new String[]{"cat", "/proc/cpuinfo"});
                    java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(proc.getInputStream()));
                    String cpuLine;
                    while ((cpuLine = br.readLine()) != null) {
                        if (cpuLine.startsWith("model name")) {
                            String[] parts = cpuLine.split(":", 2);
                            br.close();
                            proc.destroy();
                            return parts.length > 1 ? parts[1].trim() : cpuLine;
                        }
                    }
                    br.close();
                    proc.destroy();
                }
            }
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("获取CPU信息失败", e);
            }
        }
        return System.getProperty("os.arch");
    }

}
