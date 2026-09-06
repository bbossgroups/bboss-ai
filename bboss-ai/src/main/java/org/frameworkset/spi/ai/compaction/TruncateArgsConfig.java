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

/**
 * Configuration for the lightweight argument-truncation pass that runs before
 * summarization.
 *
 * <p>When triggered, large string arguments of {@code ToolUseBlock}s in older messages
 * (before the keep window) are clipped to {@link #getMaxArgLength()} characters.
 * This is a cheap, non-LLM operation that prevents context ballooning from verbose
 * tool invocations (e.g., {@code write_file}, {@code edit_file}).
 *
 * <p>Defaults (when enabled via {link Builder#truncateArgs(TruncateArgsConfig)}):
 * <ul>
 *   <li>Trigger at 25 messages or 40 000 tokens</li>
 *   <li>Keep the 20 most recent messages untouched</li>
 *   <li>Max argument length: 2 000 characters</li>
 * </ul>
 */

public class TruncateArgsConfig {
	private int triggerMessages = 25;
	private int triggerTokens = 40_000;
	private int keepMessages = 20;
	private int keepTokens = 0;
	private int maxArgLength = 2_000;
	private String truncationText = "...(argument truncated)";
	
	public TruncateArgsConfig setTriggerMessages(int triggerMessages) {
		this.triggerMessages = triggerMessages;
		return this;
	}
	
	public TruncateArgsConfig setTriggerTokens(int triggerTokens) {
		this.triggerTokens = triggerTokens;
		return this;
	}
	
	public TruncateArgsConfig setKeepMessages(int keepMessages) {
		
		this.keepMessages = keepMessages;
		return this;
	}
	
	public TruncateArgsConfig setKeepTokens(int keepTokens) {
		this.keepTokens = keepTokens;
		return this;
	}
	
	public TruncateArgsConfig setMaxArgLength(int maxArgLength) {
		this.maxArgLength = maxArgLength;
		return this;
	}
	
	public TruncateArgsConfig setTruncationText(String truncationText) {
		this.truncationText = truncationText;
		return this;
	}
	
	public int getTriggerMessages() {
		return triggerMessages;
	}
	
	public int getTriggerTokens() {
		return triggerTokens;
	}
	
	public int getKeepMessages() {
		return keepMessages;
	}
	
	public int getKeepTokens() {
		return keepTokens;
	}
	
	/** Maximum character length of any single tool argument value (default 2 000). */
	public int getMaxArgLength() {
		return maxArgLength;
	}
	
	/** Suffix appended after the first 20 characters of a truncated argument. */
	public String getTruncationText() {
		return truncationText;
	}
}
