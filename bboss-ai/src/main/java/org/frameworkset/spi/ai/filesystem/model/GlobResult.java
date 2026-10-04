package org.frameworkset.spi.ai.filesystem.model;
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

/**
 *
 * @author biaoping.yin
 * @Date 2026/8/24
 */

import java.util.List;

/**
 * Result from abstract filesystem glob operations.
 *
 
 */
public class GlobResult {
	/**
	 * error message on failure, {@code null} on success
	 */
	private String error;
	/**
	 * list of matching file info entries on success, {@code null} on failure
	 */
	private List<FileInfo> matches;
	public GlobResult(){
		
	}
	public GlobResult(String error, List<FileInfo> matches) {
		this.error = error;
		this.matches = matches;
	}

	public static GlobResult success(List<FileInfo> matches) {
		return new GlobResult(null, matches);
	}
	
	public static GlobResult fail(String error) {
		return new GlobResult(error, null);
	}
	
	public boolean isSuccess() {
		return error == null;
	}
	
	public String getError() {
		return error;
	}
	
	public void setError(String error) {
		this.error = error;
	}
	
	public List<FileInfo> getMatches() {
		return matches;
	}
	
	public void setMatches(List<FileInfo> matches) {
		this.matches = matches;
	}
}

