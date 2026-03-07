package org.frameworkset.spi.ai.mcp.streamable;
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


import org.frameworkset.spi.ai.mcp.model.*;
import org.frameworkset.spi.ai.mcp.tools.MCPToolsUtils;
import org.frameworkset.spi.ai.model.FunctionToolDefine;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author biaoping.yin
 * @Date 2026/3/3
 */
public class MCPStreamableRequestUtil {
	 
	
	public static MCPToolCallResponse sendToolCallResponse(
                                               McpToolRequest mcpToolRequest,
                                               List<Map> content){
		MCPToolCallResponse mcpToolCallResponse = new MCPToolCallResponse();
		mcpToolCallResponse.setId(mcpToolRequest.getId());
		Map result = new LinkedHashMap();
		result.put("content",content);
		mcpToolCallResponse.setResult(result);
		return mcpToolCallResponse;
	}
	
	public static MCPInitializedToolResponse sendInitializeResponse( McpToolRequest mcpRequest,
                                                 SSEInitializeResponse sseInitializeResponse) {
		 MCPInitializedToolResponse mcpInitializedToolResponse = new MCPInitializedToolResponse();
		 mcpInitializedToolResponse.setId(mcpRequest.getId());
		mcpInitializedToolResponse.putProtocolVersion(sseInitializeResponse.getProtocolVersion());
		mcpInitializedToolResponse.putListChanged(sseInitializeResponse.isListChanged());
		mcpInitializedToolResponse.putResources(sseInitializeResponse.getResources()    );
		mcpInitializedToolResponse.putServerInfo(sseInitializeResponse.getServerName(),sseInitializeResponse.getServerVersion());
		return mcpInitializedToolResponse;
	}

     
    
 
    
    public static MCPListToolResponse sendListToolResponse(
                                               McpToolRequest mcpToolRequest, List<FunctionToolDefine> functionToolDefines){
        MCPListToolResponse listToolResponse = MCPToolsUtils.convertFunctionTools2McpTools(functionToolDefines);
        listToolResponse.setId(mcpToolRequest.getId());
        return listToolResponse;
    }
}
