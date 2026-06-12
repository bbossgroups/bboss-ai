/**
 *  Copyright 2008-2010 biaoping.yin
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.frameworkset.spi.ai.model;

/**
 * <p>Title: AgentSessionCondition</p> <p>Description: 会话管理查询条件实体类 </p>
 * <p>asiainfo</p> <p>Copyright (c) 2007</p> @Date 2026-06-12 14:16:51 @author
 * yinbp @version v1.0
 */
public class AgentSessionCondition implements java.io.Serializable {
	/**
	 * 代理id
	 */
	private String agentid;

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    /**
     * 业务领域
     */
    private String domain;
	/**
	 * 用户id
	 */
	private String userId;
	private String sortKey;
	private boolean sortDesc;
	public AgentSessionCondition() {
	}
	public void setAgentid(String agentid) {
		this.agentid = agentid;
	}

	public String getAgentid() {
		return agentid;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getUserId() {
		return userId;
	}

	public void setSortKey(String sortKey) {
		this.sortKey = sortKey;
	}

	public String getSortKey() {
		return sortKey;
	}

	public void setSortDesc(boolean sortDesc) {
		this.sortDesc = sortDesc;
	}

	public boolean getSortDesc() {
		return sortDesc;
	}

}