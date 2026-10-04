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
 * Result of a file edit (string replacement) operation.
 
 */
public class EditResult  {
	/**
	 * absolute path of edited file, {@code null} on failure
	 */
	private String path;
	/**
	 * error message on failure, {@code null} on success
	 */
	private String error;
	/**
	 * number of replacements made, {@code null} on failure
	 */
	private Integer occurrences;
	
	public EditResult(String path, String error, Integer occurrences) {
		this.path = path;
		this.error = error;
		this.occurrences = occurrences;
	}
	public EditResult() {
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
	
	public Integer getOccurrences() {
		return occurrences;
	}
	
	public void setOccurrences(Integer occurrences) {
		this.occurrences = occurrences;
	}
	
	public static EditResult ok(String path, int occurrences) {
        return new EditResult(path, null, occurrences);
    }

    public static EditResult fail(String error) {
        return new EditResult(null, error, null);
    }

    public boolean isSuccess() {
        return error == null;
    }
}
