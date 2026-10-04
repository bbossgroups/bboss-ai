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
package org.frameworkset.spi.ai.filesystem.model;

/**
 * Result of code/shell execution.
 *
 
 */
public class ExecuteResponse {
	
	/**
	 * combined stdout and stderr output of the executed command
	 */
	private String output;
	/**
	 * the process exit code (0 indicates success, non-zero indicates failure)
	 */
	private Integer exitCode;
	/**
	 * whether the output was truncated due to filesystem limitations
	 */
	private boolean truncated;
	public ExecuteResponse(String output, Integer exitCode, boolean truncated) {
		this.output = output;
		this.exitCode = exitCode;
		this.truncated = truncated;
	}
	public String getOutput() {
		return output;
	}
	public void setOutput(String output) {
		this.output = output;
	}
	public Integer getExitCode() {
		return exitCode;
	}
	public void setExitCode(Integer exitCode) {
		this.exitCode = exitCode;
	}
	public boolean isTruncated() {
		return truncated;
	}
	public void setTruncated(boolean truncated) {
		this.truncated = truncated;
	}
    public boolean isSuccess() {
        return exitCode != null && exitCode == 0;
    }
}
