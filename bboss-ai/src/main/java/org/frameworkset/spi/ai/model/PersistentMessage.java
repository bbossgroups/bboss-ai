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
 * @Date 2026/5/23
 */
public class PersistentMessage {
    private String stringMessage;
	private String groupId;
	private String parentGroupId;
    private TokenMetrics tokenMetrics;

    private TokenMetrics totalTokenMetrics;
    private Map<String, Object> message;

    public TokenMetrics getTokenMetrics() {
        return tokenMetrics;
    }

    public void setTokenMetrics(TokenMetrics tokenMetrics) {
        this.tokenMetrics = tokenMetrics;
    }

    public Map<String, Object> getMessage() {
        return message;
    }

    public void setMessage(Map<String, Object> message) {
        this.message = message;
    }

    public String getStringMessage() {
        return stringMessage;
    }

    public void setStringMessage(String stringMessage) {
        this.stringMessage = stringMessage;
    }

    public TokenMetrics getTotalTokenMetrics() {
        return totalTokenMetrics;
    }

    public void setTotalTokenMetrics(TokenMetrics totalTokenMetrics) {
        this.totalTokenMetrics = totalTokenMetrics;
    }
	
	public String getGroupId() {
		return groupId;
	}
	
	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}
	
	public String getParentGroupId() {
		return parentGroupId;
	}
	
	public void setParentGroupId(String parentGroupId) {
		this.parentGroupId = parentGroupId;
	}
}
