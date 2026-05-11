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

import java.util.Map;

/**
 * @author biaoping.yin
 * @Date 2026/5/7
 */
public class TokenMetrics {
    private String model;
    private long totalTokens;
    private long promptTokens;
    private long cachedTokens;
    private long completionTokens;
    private long reasoningTokens;
    /**
     * 非stream模式下，有值
     */
    private Map usage;

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public long getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(long totalTokens) {
        this.totalTokens = totalTokens;
    }
    public void increaseTotalTokens(long totalTokens) {
        this.totalTokens += totalTokens;
    }

    public long getPromptTokens() {
        return promptTokens;
    }
    public void increasePromptTokens(long promptTokens) {
        this.promptTokens += promptTokens;
    }
    public void setPromptTokens(long promptTokens) {
        this.promptTokens = promptTokens;
    }

    public long getCachedTokens() {
        return cachedTokens;
    }

    public void setCachedTokens(long cachedTokens) {
        this.cachedTokens = cachedTokens;
    }
    public void increaseCachedTokens(long cachedTokens) {
        this.cachedTokens += cachedTokens;
    }
    public long getCompletionTokens() {
        return completionTokens;
    }

    public void setCompletionTokens(long completionTokens) {
        this.completionTokens = completionTokens;
    }
    public void increaseCompletionTokens(long completionTokens) {
        this.completionTokens += completionTokens;
    }
    public long getReasoningTokens() {
        return reasoningTokens;
    }

    public void setReasoningTokens(long reasoningTokens) {
        this.reasoningTokens = reasoningTokens;
    }
    public void increaseReasoningTokens(long reasoningTokens) {
        this.reasoningTokens += reasoningTokens;
    }

    public Map getUsage() {
        return usage;
    }

    public void setUsage(Map usage) {
        this.usage = usage;
    }
}
