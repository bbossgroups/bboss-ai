package org.frameworkset.spi.ai.model;
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
 * @Date 2026/8/30
 */
public class ModelInfo {
	private String maas;
	private String model;
	/**
	 * Returns the model's context window size in tokens, or {@code 0} if unknown.
	 *
	 * <p>Used by the compaction middleware to dynamically compute when to trigger
	 * conversation summarization. Implementations should return the total context
	 * window (input + output) for the configured model.
	 *
	 * @return context window size in tokens, or {@code 0} if not available
	 */
	private int contextWindowSize;
	/**
	 * Returns the model's context window size in tokens, or {@code 0} if unknown.
	 *
	 * <p>Used by the compaction middleware to dynamically compute when to trigger
	 * conversation summarization. Implementations should return the total context
	 * window (input + output) for the configured model.
	 *
	 * @return context window size in tokens, or {@code 0} if not available
	 */
	public int getContextWindowSize() {
		return contextWindowSize;
	}
	/**
	 * Returns the model's context window size in tokens, or {@code 0} if unknown.
	 *
	 * <p>Used by the compaction middleware to dynamically compute when to trigger
	 * conversation summarization. Implementations should return the total context
	 * window (input + output) for the configured model.
	 *
	 */
	public void setContextWindowSize(int contextWindowSize) {
		this.contextWindowSize = contextWindowSize;
	}
	
	public String getMaas() {
		return maas;
	}
	
	public void setMaas(String maas) {
		this.maas = maas;
	}
	
	public String getModel() {
		return model;
	}
	
	public void setModel(String model) {
		this.model = model;
	}
}
