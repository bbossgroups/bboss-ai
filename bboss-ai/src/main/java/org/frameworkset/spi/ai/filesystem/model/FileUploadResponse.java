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
 * Result of a single file upload operation.
 */
public class FileUploadResponse {
	/**
	 * the file path that was requested
	 */
	private String path;
	/**
	 * error description on failure, {@code null} on success
	 */
	private String error;
	
	public FileUploadResponse() {
	}
	
	public FileUploadResponse(String path, String error) {
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
	
	public static FileUploadResponse success(String path) {
		return new FileUploadResponse(path, null);
	}
	
	public static FileUploadResponse fail(String path, String error) {
		return new FileUploadResponse(path, error);
	}
}