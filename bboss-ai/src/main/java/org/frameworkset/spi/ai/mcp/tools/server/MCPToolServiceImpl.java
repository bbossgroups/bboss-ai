package org.frameworkset.spi.ai.mcp.tools.server;
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

 
import com.frameworkset.util.JsonUtil;
import org.apache.commons.lang3.StringUtils;
import org.frameworkset.spi.ai.mcp.MCPMethods;
import org.frameworkset.spi.ai.mcp.model.McpCallException;
import org.frameworkset.spi.ai.mcp.model.McpToolRequest;
import org.frameworkset.spi.ai.mcp.sse.MCPSSERequestUtil;
import org.frameworkset.spi.ai.mcp.sse.MCPSSEServer;
import org.frameworkset.spi.ai.mcp.sse.McpSSESink;
import org.frameworkset.spi.ai.mcp.streamable.MCPStreamableRequestUtil;
import org.frameworkset.spi.ai.model.FunctionCall;
import org.frameworkset.spi.ai.model.FunctionTool;
import org.frameworkset.spi.ai.model.FunctionToolDefine;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * 提供Mcp服务请求调用
 * @author biaoping.yin
 * @Date 2026/3/3
 */
public class MCPToolServiceImpl implements MCPToolService {
	private static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MCPToolServiceImpl.class);
	private MCPApiKeyService mcpApiKeyService;

	private MCPSSEServer mcpsseServer = new MCPSSEServer();
	
	@Override
	public Flux<String> sse(String apiKey) {
		if (!mcpApiKeyService.auth( apiKey)) {
			throw new MCPApiException("401:密钥无效");			
		} else {			
			return mcpsseServer.sse(apiKey, "/mcp/message.api");
		}
	}
	public Object streamable(  String apiKey,
						String requestBody
	){
		if (!mcpApiKeyService.auth( apiKey)) {
			throw new McpCallException( "{code:401:,error:\"apiKey["+apiKey+"]密钥无效\"}");
			
		} else {
			
			
			 
			
			if(requestBody == null || requestBody.equals("")){
				throw new McpCallException( "{code:400,error:\"请求体不能为空\"}");
			}
			McpToolRequest mcpRequest = JsonUtil.json2Object(requestBody, McpToolRequest.class);
			String method = mcpRequest.getMethod();
			 
			if(method == null || method.equals("")){
				throw new McpCallException( "{code:400,error:\"method 不能为空\"}");
			}
			else if(method.equals(MCPMethods.METHOD_INITIALIZE)){
				
				return MCPApiRequestUtil.sendInitializeResponse( mcpRequest);
				
			}
			else if(method.equals(MCPMethods.METHOD_NOTIFICATIONS_INITIALIZED)){
				
				 ;
				return "Accepted";
			}
			else if(method.equals(MCPMethods.METHOD_TOOLS_LIST)){
				 
				List<FunctionToolDefine> functionToolDefines = mcpApiKeyService.getMcpServerApiKeyInfo(apiKey);
				return MCPStreamableRequestUtil.sendListToolResponse( mcpRequest, functionToolDefines);
			}
			else if(method.equals(MCPMethods.METHOD_TOOLS_CALL)){
                
				String functionName = mcpRequest.functionName();
                if (StringUtils.isEmpty(functionName)) {
                    throw new McpCallException("functionName 参数不能为空");
                }
				Map arguments = mcpRequest.functionArguments();
                FunctionToolDefine functionToolDefine = mcpApiKeyService.getFunctionToolDefine(apiKey,functionName);
                FunctionCall<List<Map>> functionCall = functionToolDefine.getFunctionCall();
                FunctionTool functionTool = new FunctionTool();
                functionTool.setArguments(arguments);
                functionTool.setFunctionName(functionName);
                
                if (mcpApiKeyService.auth(functionName, apiKey)) {
                    List<Map> data = functionCall.call(functionTool);
                    return MCPStreamableRequestUtil.sendToolCallResponse(mcpRequest, data);

                } else {
                    throw new McpCallException(
                            (String.format("密钥%s无效", apiKey.substring(0,4)+"****************************"
                                    +apiKey.substring(apiKey.length()-4))));
                }
                
				
				
		 
				
				
			}
		}
		return "Accepted";
		
	
	}
	public   String message(  String apiKey, String sessionId,
			 String requestBody
	) {
		
		if (!mcpApiKeyService.auth( apiKey)) {
			throw new McpCallException( "{code:401:,error:\"apiKey["+apiKey+"]密钥无效\"}");
			
		} else {
			
			 
			if(sessionId == null ){
				throw new McpCallException( "{code:401:,error:\"sessionId["+sessionId+"]无效\"}");
			}
		 
			if(requestBody == null || requestBody.equals("")){
				throw new McpCallException( "{code:400,error:\"请求体不能为空\"}");
			}
			McpToolRequest mcpRequest = JsonUtil.json2Object(requestBody, McpToolRequest.class);
			String method = mcpRequest.getMethod();
			McpSSESink mcpSSESink = mcpsseServer.getMcpSSESink(apiKey, sessionId);
			if(method == null || method.equals("")){
				throw new McpCallException( "{code:400,error:\"method 不能为空\"}");
			}
			else if(method.equals(MCPMethods.METHOD_INITIALIZE)){
				 
				MCPApiRequestUtil.sendSSEInitializeResponse(mcpSSESink, mcpRequest);
				
			}
			else if(method.equals(MCPMethods.METHOD_NOTIFICATIONS_INITIALIZED)){
				
				MCPSSERequestUtil.notificationsInitialized(mcpSSESink);
				return "Accepted";
			}
			else if(method.equals(MCPMethods.METHOD_TOOLS_LIST)){
				MCPSSERequestUtil.validateMcpSSESink(mcpSSESink);
				List<FunctionToolDefine> functionToolDefines = mcpApiKeyService.getMcpServerApiKeyInfo(apiKey);
				MCPSSERequestUtil.sendSSEListToolResponse(mcpSSESink, mcpRequest, functionToolDefines);
				return "Accepted";
			}
			else if(method.equals(MCPMethods.METHOD_TOOLS_CALL)){
				MCPSSERequestUtil.validateMcpSSESink(mcpSSESink);
                String functionName = mcpRequest.functionName();
                if (StringUtils.isEmpty(functionName)) {
                    throw new McpCallException("functionName 参数不能为空");
                }
                Map arguments = mcpRequest.functionArguments();
                FunctionToolDefine functionToolDefine = mcpApiKeyService.getFunctionToolDefine(apiKey,functionName);
                FunctionCall<List<Map>> functionCall = functionToolDefine.getFunctionCall();
                FunctionTool functionTool = new FunctionTool();
                functionTool.setArguments(arguments);
                functionTool.setFunctionName(functionName);

                if (mcpApiKeyService.auth(functionName, apiKey)) {
                    List<Map> data = functionCall.call(functionTool);
                    MCPSSERequestUtil.sendSSEToolCallResponse(mcpSSESink, mcpRequest, data);

                } else {
                    throw new McpCallException(
                            (String.format("密钥%s无效", apiKey.substring(0,4)+"****************************"
                                    +apiKey.substring(apiKey.length()-4))));
                }
                
			 
			}
		}
		return "Accepted";
		
	}

    public void setMcpApiKeyService(MCPApiKeyService mcpApiKeyService) {
        this.mcpApiKeyService = mcpApiKeyService;
    }
}
