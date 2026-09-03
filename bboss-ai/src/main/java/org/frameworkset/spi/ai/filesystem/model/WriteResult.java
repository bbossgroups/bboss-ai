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
 * Result of a file write operation.
 */
public class WriteResult {
	/**
	 * absolute path of written file, {@code null} on failure
	 */
	private String path;
	/**
	 * error message on failure, {@code null} on success
	 */
	private String error;
	
	public WriteResult() {
	}
	
	public WriteResult(String path, String error) {
		this.path = path;
		this.error = error;
	}
	
	public String getPath() {
		return path;
	}
	
	public void setPath(String path) {
		this.path = path;
	}
	
	public String getError() {
		return error;
	}
	
	public void setError(String error) {
		this.error = error;
	}
	
	public boolean isSuccess() {
		return error == null;
	}
	
	public static WriteResult ok(String path) {
		return new WriteResult(path, null);
	}
	
	public static WriteResult fail(String error) {
		return new WriteResult(null, error);
	}
}
