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
 * Result from abstract filesystem read operations.
 *
 * @param error error message on failure, {@code null} on success
 * @param fileData file data on success, {@code null} on failure
 */

import org.frameworkset.spi.ai.filesystem.model.FileData;

/**
 * Result from abstract filesystem read operations.
 */
public class ReadResult {
	/**
	 * error message on failure, {@code null} on success
	 */
	private String error;
	/**
	 * file data on success, {@code null} on failure
	 */
	private FileData fileData;
	
	public ReadResult() {
	}
	
	public ReadResult(String error, FileData fileData) {
		this.error = error;
		this.fileData = fileData;
	}
	
	public String getError() {
		return error;
	}
	
	public void setError(String error) {
		this.error = error;
	}
	
	public FileData getFileData() {
		return fileData;
	}
	
	public void setFileData(FileData fileData) {
		this.fileData = fileData;
	}
	
	public boolean isSuccess() {
		return error == null;
	}
	
	public static ReadResult success(FileData fileData) {
		return new ReadResult(null, fileData);
	}
	
	public static ReadResult fail(String error) {
		return new ReadResult(error, null);
	}
}
