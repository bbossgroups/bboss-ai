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

/**
 * @author biaoping.yin
 * @Date 2026/2/28
 */
public class McpInitializedToolRequest extends McpToolRequest{
	public McpInitializedToolRequest( ){
		super();
		McpCapabilities capabilities = new McpCapabilities();
		
		this.params.put("capabilities",capabilities);
		
		this.setMethod("initialize");
		
		 
	}
	public McpInitializedToolRequest protocolVersion(String protocolVersion){
		if(params == null){
			params = new LinkedHashMap<>();
		}
		
		params.put("protocolVersion", protocolVersion);
		return this;
	}
	public McpInitializedToolRequest elicitation(String key,Object value){
		if(params == null){
			params = new LinkedHashMap<>();
		}
		McpCapabilities capabilities = (McpCapabilities) params.get("capabilities");
		if(capabilities == null){
			capabilities = new McpCapabilities();
			params.put("capabilities", capabilities);
		}
		
		capabilities.addElicitation(key, value);
		return this;
	}
	
	public McpInitializedToolRequest clientInfo(String name,String version){
		if(params == null){
			params = new LinkedHashMap<>();
		}
		McpClientInfo mcpClientInfo = new McpClientInfo();
		mcpClientInfo.setName(name);
		mcpClientInfo.setVersion(version);
		this.params.put("clientInfo", mcpClientInfo);
		return this;
	}
	
	
	 
}
