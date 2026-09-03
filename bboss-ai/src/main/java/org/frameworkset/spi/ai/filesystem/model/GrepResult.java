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
 * Result from abstract filesystem grep operations.
 */
public class GrepResult {
	/**
	 * error message on failure, {@code null} on success
	 */
	private String error;
	/**
	 * list of grep matches on success, {@code null} on failure
	 */
	private List<GrepMatch> matches;
	
	public GrepResult() {
	}
	
	public GrepResult(String error, List<GrepMatch> matches) {
		this.error = error;
		this.matches = matches;
	}
	
	public String getError() {
		return error;
	}
	
	public void setError(String error) {
		this.error = error;
	}
	
	public List<GrepMatch> getMatches() {
		return matches;
	}
	
	public void setMatches(List<GrepMatch> matches) {
		this.matches = matches;
	}
	
	public boolean isSuccess() {
		return error == null;
	}
	
	public static GrepResult success(List<GrepMatch> matches) {
		return new GrepResult(null, matches);
	}
	
	public static GrepResult fail(String error) {
		return new GrepResult(error, null);
	}
}
