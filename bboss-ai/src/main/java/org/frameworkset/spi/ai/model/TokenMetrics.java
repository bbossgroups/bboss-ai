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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * @author biaoping.yin
 * @Date 2026/5/7
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TokenMetrics {

    private String maas;
    private String model;
    private long totalTokens;
    private long promptTokens;
    private long promptCachedTokens;
	
	
	private long promptCacheHitTokens;
	
	
	private long promptCacheMissTokens;

    private long promptTextTokens;
    private long completionTokens;
    private long completionReasoningTokens;


    /**
     * 思考数据
     */
    private String reasoningData;



    private long completionTextTokens;
    
    private Long startTime;
    private Long endTime;
    /**
     * 非stream模式下，有值
     */
    @JsonIgnore
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

    public long getPromptCachedTokens() {
        return promptCachedTokens;
    }

    public void setPromptCachedTokens(long promptCachedTokens) {
        this.promptCachedTokens = promptCachedTokens;
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

    public void increasePromptCachedTokens(long cachedTokens) {
        this.promptCachedTokens += cachedTokens;
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
    public long getCompletionReasoningTokens() {
        return completionReasoningTokens;
    }

    public void setCompletionReasoningTokens(long completionReasoningTokens) {
        this.completionReasoningTokens = completionReasoningTokens;
    }
    public void increaseCompletionReasoningTokens(long reasoningTokens) {
        this.completionReasoningTokens += reasoningTokens;
    }

    public void increaseCompletionTextTokens(long textTokens) {
        this.completionTextTokens += textTokens;
    }

    public void increasePromptTextTokens(long promptTextTokens) {
        this.promptTextTokens += promptTextTokens;
    }
	
	public void increasePromptCacheMissTokens(long promptCacheMissTokens) {
		this.promptCacheMissTokens += promptCacheMissTokens;
	}
	
	public void increasePromptCacheHitTokens(long promptCacheHitTokens) {
		this.promptCacheHitTokens += promptCacheHitTokens;
	}

    public Map getUsage() {
        return usage;
    }

    public void setUsage(Map usage) {
        this.usage = usage;
    }
    public long getCompletionTextTokens() {
        return completionTextTokens;
    }

    public void setCompletionTextTokens(long completionTextTokens) {
        this.completionTextTokens = completionTextTokens;
    }

    public long getPromptTextTokens() {
        return promptTextTokens;
    }

    public void setPromptTextTokens(long promptTextTokens) {
        this.promptTextTokens = promptTextTokens;
    }

    public String getMaas() {
        return maas;
    }

    public void setMaas(String maas) {
        this.maas = maas;
    }

    public String getReasoningData() {
        return reasoningData;
    }

    public void setReasoningData(String reasoningData) {
        this.reasoningData = reasoningData;
    }
	
	public long getPromptCacheHitTokens() {
		return promptCacheHitTokens;
	}
	
	public void setPromptCacheHitTokens(long promptCacheHitTokens) {
		this.promptCacheHitTokens = promptCacheHitTokens;
	}
	
	public long getPromptCacheMissTokens() {
		return promptCacheMissTokens;
	}
	
	public void setPromptCacheMissTokens(long promptCacheMissTokens) {
		this.promptCacheMissTokens = promptCacheMissTokens;
	}
}
