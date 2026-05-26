package org.frameworkset.spi.ai.mcp.tools;
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

import org.frameworkset.spi.ai.mcp.MCPClient;
import org.frameworkset.spi.ai.mcp.model.MCPListToolResponse;
import org.frameworkset.spi.ai.model.FunctionCall;
import org.frameworkset.spi.ai.model.FunctionToolDefine;
import org.frameworkset.spi.ai.tools.ToolsRegist;

import java.util.List;

/**
 * mcp tools regist
 * @author biaoping.yin
 * @Date 2026/3/2
 */
public class MCPToolsRegist implements ToolsRegist {
	protected String mcpServer;
    protected MCPClient mcpClient;
	public MCPToolsRegist(String mcpServer){
		this.mcpServer = mcpServer;
	}
    protected MCPClient buildMCPClient(){
		return new MCPClient(mcpServer);
	}
	public void init(){
		mcpClient = buildMCPClient();
		mcpClient.init();
	}
	public void destroy(){
		if(mcpClient != null){
			mcpClient.destroy();
		}
	}
	
	@Override
	public List<FunctionToolDefine> registTools() {
		MCPListToolResponse mcpListToolResponse = mcpClient.listTools();
		List<FunctionToolDefine> functionToolDefines = MCPToolsUtils.convertMcpTools2FunctionTools(mcpListToolResponse);
		return functionToolDefines;
	}	 
	
	@Override
	public FunctionCall getFunctionCall(String functionName) {
		return new MCPToolFunctionCall(mcpClient);
	}

    @Override
    public FunctionToolDefine getFunctionToolDefine(String functionName) {
        throw new UnsupportedOperationException("MCPToolsRegist does not support getFunctionToolDefine");
    }
}
