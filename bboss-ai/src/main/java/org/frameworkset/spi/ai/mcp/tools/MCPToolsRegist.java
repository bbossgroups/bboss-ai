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
import org.frameworkset.spi.remote.http.ClientConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * mcp tools regist
 * @author biaoping.yin
 * @Date 2026/3/2
 */
public class MCPToolsRegist implements ToolsRegist {
	private static final Logger logger = LoggerFactory.getLogger(MCPToolsRegist.class);
	protected String mcpServer;
    protected MCPClient mcpClient;
	protected Object lock = new Object();
	protected boolean initialized ;
	protected Integer toolCallRetry;
	public MCPToolsRegist(String mcpServer){
		this.mcpServer = mcpServer;
		ClientConfiguration clientConfiguration = ClientConfiguration.getClientConfiguration(mcpServer);
		String toolCallRetry_ = clientConfiguration.getExtendConfig("toolCallRetry");
		if(toolCallRetry_ != null){
			try {
				toolCallRetry = Integer.parseInt(toolCallRetry_);
			} catch (Exception e) {
				logger.warn("toolCallRetry config error:toolCallRetry="+toolCallRetry_,e);
			}
			
		}
		
	}
	
	public Integer getToolCallRetry() {
		return toolCallRetry;
	}
	
	public void setToolCallRetry(Integer toolCallRetry) {
		this.toolCallRetry = toolCallRetry;
	}
	
	protected MCPClient buildMCPClient(){
		return new MCPClient(mcpServer);
	}
	public void init(){
		if(initialized == true){
			return;
		}
		synchronized (lock) {
			if (initialized) {
				return;
			}
			MCPClient mcpClient = buildMCPClient();
			mcpClient.setToolCallRetry(this.getToolCallRetry());
			mcpClient.init();
			this.mcpClient = mcpClient;
			this.initialized = true;
		}
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
		if(functionToolDefines != null && !functionToolDefines.isEmpty()) {
			for (FunctionToolDefine functionToolDefine : functionToolDefines) {
				functionToolDefine.setToolsRegist(this);
			}
		}
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
