package org.frameworkset.spi.ai.mcp.model;
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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author biaoping.yin
 * @Date 2026/2/28
 */
public class McpToolRequest {
	private String jsonrpc = "2.0";
	private Long id;
	private String method;
	protected Map params;
	public McpToolRequest() {
		this.params = new LinkedHashMap<>();
	}
	
	public McpToolRequest(boolean needParams) {
		if(needParams)
			this.params = new LinkedHashMap<>();
	}
	
	public String getJsonrpc() {
		return jsonrpc;
	}
	
	public void setJsonrpc(String jsonrpc) {
		this.jsonrpc = jsonrpc;
	}
	
	public Long getId() {
		return id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}
	
	public String getMethod() {
		return method;
	}
	
	public void setMethod(String method) {
		this.method = method;
	}
	
	public Map getParams() {
		return params;
	}
	
	public void setParams(Map params) {
		this.params = params;
	}
	
}
