package org.frameworkset.spi.ai.mcp;
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

import org.frameworkset.spi.ai.mcp.model.MCPListToolResponse;
import org.frameworkset.spi.ai.mcp.model.MCPToolCallResponse;
import org.frameworkset.spi.ai.mcp.sse.MCPSSEClient;
import org.frameworkset.spi.ai.mcp.streamable.MCPStreamableClient;
import org.frameworkset.spi.ai.model.FunctionTool;
import org.frameworkset.spi.remote.http.ClientConfiguration;

/**
 * @author biaoping.yin
 * @Date 2026/2/26
 */
public class MCPClient {
    private MCPClientInf mcpClientInf;
    private String mcpServer;
    public MCPClient(String mcpServer){
        this.mcpServer = mcpServer;
       
    }
    
	
	
    public void init(){
        ClientConfiguration clientConfiguration = ClientConfiguration.getClientConfiguration(mcpServer);
        String ssePath = clientConfiguration.getExtendConfig("sseendpoint");
        if(ssePath != null){
            mcpClientInf = new MCPSSEClient(mcpServer);
        }
        else{
            mcpClientInf = new MCPStreamableClient(mcpServer);
        }
        mcpClientInf.init();
    }
    public void destroy(){
        mcpClientInf.destroy();
    }
 
	public MCPToolCallResponse toolsCall(FunctionTool functionTool){
        return mcpClientInf.toolsCall(functionTool);
    }
    public MCPListToolResponse listTools(){
         return mcpClientInf.listTools();
    }

     
 
}
