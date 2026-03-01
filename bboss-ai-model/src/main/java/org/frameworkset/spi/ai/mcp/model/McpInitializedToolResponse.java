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

import java.util.Map;

/**
 * @author biaoping.yin
 * @Date 2026/2/28
 */
public class McpInitializedToolResponse extends McpToolResponse{
 
	public String protocolVersion(){		
		return (String) result.get("protocolVersion");
	}
	public boolean listChanged(){
		Map capabilities = (Map) result.get("capabilities");
		Map tools = (Map) capabilities.get("tools");
		return (boolean) tools.get("listChanged");
	}
	
	public Map resources(){
		Map capabilities = (Map) result.get("capabilities");
		Map resources = (Map) capabilities.get("resources");
		return resources;
	}
	
	public McpServerInfo serverInfo(){
		Map serverInfo = (Map) result.get("serverInfo");
		McpServerInfo mcpServerInfo = new McpServerInfo();
		mcpServerInfo.setName((String) serverInfo.get("name"));
		mcpServerInfo.setVersion((String) serverInfo.get("version"));
		return mcpServerInfo;	
	}
	 
	
	
	 
}
