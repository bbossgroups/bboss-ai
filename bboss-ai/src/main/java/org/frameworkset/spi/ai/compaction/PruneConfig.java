package org.frameworkset.spi.ai.compaction;
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

import java.util.HashSet;
import java.util.Set;

/**
 * Configuration for aggregate tool-result pruning.
 *
 * <p>Prune walks backward through tool-result messages, protecting the most recent
 * {@link #getProtectTokens()} tokens of tool output. Older tool results beyond that
 * protection window are replaced with a head+tail preview when the total prunable
 * amount exceeds {@link #getMinimumTokens()}.
 *
 * <p>This is a lightweight, non-LLM operation that runs inside
 * {link ConversationCompactor#compactIfNeeded} before summarization.
 */
public class PruneConfig {
	private int protectTokens = 40_000;
	private int minimumTokens = 20_000;
	private int maxOutputChars = 2_000;
	private Set<String> excludedTools;
	
	{
		excludedTools = new HashSet<>();
		excludedTools.add("read_file");
		excludedTools.add("memory_search");
		excludedTools.add("memory_get");
		excludedTools.add("session_search");
	}
	
	public int getProtectTokens() {
		return protectTokens;
	}
	
	public PruneConfig setProtectTokens(int protectTokens) {
		this.protectTokens = protectTokens;
		return this;
	}
	
	public int getMinimumTokens() {
		return minimumTokens;
	}
	
	public PruneConfig setMinimumTokens(int minimumTokens) {
		this.minimumTokens = minimumTokens;
		return this;
	}
	
	public int getMaxOutputChars() {
		return maxOutputChars;
	}
	
	public PruneConfig setMaxOutputChars(int maxOutputChars) {
		this.maxOutputChars = maxOutputChars;
		return this;
	}
	
	public Set<String> getExcludedTools() {
		return excludedTools;
	}
	
	public PruneConfig setExcludedTools(Set<String> excludedTools) {
		this.excludedTools = excludedTools;
		return this;
	}
}
