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

import java.util.List;

 

/**
 * Result from abstract filesystem ls (directory listing) operations.
 */
public class LsResult {
	/**
	 * error message on failure, {@code null} on success
	 */
	private String error;
	/**
	 * list of file info entries on success, {@code null} on failure
	 */
	private List<FileInfo> entries;
	
	public LsResult() {
	}
	
	public LsResult(String error, List<FileInfo> entries) {
		this.error = error;
		this.entries = entries;
	}
	
	public String getError() {
		return error;
	}
	
	public void setError(String error) {
		this.error = error;
	}
	
	public List<FileInfo> getEntries() {
		return entries;
	}
	
	public void setEntries(List<FileInfo> entries) {
		this.entries = entries;
	}
	
	public boolean isSuccess() {
		return error == null;
	}
	
	public static LsResult success(List<FileInfo> entries) {
		return new LsResult(null, entries);
	}
	
	public static LsResult fail(String error) {
		return new LsResult(error, null);
	}
}
