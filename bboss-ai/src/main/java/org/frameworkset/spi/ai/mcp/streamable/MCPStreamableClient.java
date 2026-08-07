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

import org.frameworkset.spi.ai.mcp.MCPBaseClient;
import org.frameworkset.spi.ai.mcp.model.*;
import org.frameworkset.spi.remote.http.ClientConfiguration;
import org.frameworkset.spi.remote.http.HttpRequestProxy;
import org.frameworkset.util.RetryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @author biaoping.yin
 * @Date 2026/2/26
 */
public class MCPStreamableClient extends MCPBaseClient<MCPStreamableClient> {
     
    private final static Logger logger = LoggerFactory.getLogger(MCPStreamableClient.class);
   
    public MCPStreamableClient(String mcpServer){
        super(mcpServer);
    }
    
    protected Map buildHeaders() {
         return null;
    }
     
    @Override
    protected MCPToolCallResponse executeToolsCall(McpToolCallRequest mcpToolCallRequest) {
        Map headers = buildHeaders();
		MCPToolCallResponse mcpToolCallResponse = null;
		Integer toolCallRetry = this.getToolCallRetry();
		if(toolCallRetry == null) {
			mcpToolCallResponse = HttpRequestProxy.sendJsonBody(getMcpServer(),
					mcpToolCallRequest, headers, streamablePath,
					MCPToolCallResponse.class);
		}
		else{
			mcpToolCallResponse = RetryUtil.executeWithRetry("executeToolsCall",toolCallRetry, 100, () -> {
				MCPToolCallResponse mcpToolCallResponse1 = HttpRequestProxy.sendJsonBody(getMcpServer(),
						mcpToolCallRequest, headers, streamablePath,
						MCPToolCallResponse.class);
				return mcpToolCallResponse1;
			});
		}
		
		return mcpToolCallResponse;
        
    }

    private String streamablePath ;
	
    public void init(){
        // 初始化MCPClient
        ClientConfiguration clientConfiguration = ClientConfiguration.getClientConfiguration(this.mcpServer);
        streamablePath = clientConfiguration.getExtendConfig("streamableendpoint");
        connect();
    }
    public void destroy(){
         
    }

    
    private void connect(){
         
        
        initialization();
        notificationsInitialized();
         

    }
   

    @Override
    protected MCPListToolResponse executeListTools(McpListToolRequest mcpToolRequest) {
        Map headers = buildHeaders();
		MCPListToolResponse mcpListToolResponse = null;
		Integer toolCallRetry = this.getToolCallRetry();
		if(toolCallRetry == null) {
			mcpListToolResponse = HttpRequestProxy.sendJsonBody(getMcpServer(),
					mcpToolRequest,headers,streamablePath,
					MCPListToolResponse.class);
		}
		else{
			mcpListToolResponse = RetryUtil.executeWithRetry("executeListTools",toolCallRetry, 100, () -> {
				MCPListToolResponse mcpListToolResponse1 = HttpRequestProxy.sendJsonBody(getMcpServer(),
						mcpToolRequest,headers,streamablePath,
						MCPListToolResponse.class);
				return mcpListToolResponse1;
			});
		}
        
//        MCPToolCallResponse mcpToolCallResponse = this.sseMcpCallHelper.toolsCall(this, mcpToolCallRequest);
        return mcpListToolResponse;
    }
 

    @Override
    protected String executeNotificationsInitialized(McpToolRequest notificationsInitialized) {
        Map headers = buildHeaders();
		
		String data = RetryUtil.executeWithRetry("executeInitialization",3, 100, () -> {
			String data1 = HttpRequestProxy.sendJsonBody(getMcpServer(),
					notificationsInitialized,headers,streamablePath,
					String.class);
			return data1;
		});
       
        if(logger.isDebugEnabled()) {
            logger.debug("{} notificationsInitialized:{}",mcpServer, data);
        }
//        MCPToolCallResponse mcpToolCallResponse = this.sseMcpCallHelper.toolsCall(this, mcpToolCallRequest);
        return data;
    }

 

    @Override
    protected MCPInitializedToolResponse executeInitialization(McpInitializedToolRequest mcpInitializedToolRequest) {
        Map headers = buildHeaders();
		MCPInitializedToolResponse mcpInitializedToolResponse = RetryUtil.executeWithRetry("executeInitialization",3, 100, () -> {
			MCPInitializedToolResponse mcpInitializedToolResponse1 = HttpRequestProxy.sendJsonBody(getMcpServer(),
					mcpInitializedToolRequest,headers,streamablePath,
					MCPInitializedToolResponse.class);
//        MCPToolCallResponse mcpToolCallResponse = this.sseMcpCallHelper.toolsCall(this, mcpToolCallRequest);
			return mcpInitializedToolResponse1;
		});
        
        return mcpInitializedToolResponse;
    }
 

}
