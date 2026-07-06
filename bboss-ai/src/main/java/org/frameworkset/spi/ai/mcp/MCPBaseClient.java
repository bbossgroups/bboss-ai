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

import com.frameworkset.util.JsonUtil;
import com.frameworkset.util.SimpleStringUtil;
import org.frameworkset.spi.ai.mcp.model.*;
import org.frameworkset.spi.ai.model.FunctionCallException;
import org.frameworkset.spi.ai.model.FunctionTool;
import org.frameworkset.spi.ai.model.TraceMessage;
import org.frameworkset.spi.ai.store.SessionMessage;
import org.frameworkset.spi.ai.tool.AgentTraceHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @author biaoping.yin
 * @Date 2026/2/26
 */
public abstract class MCPBaseClient<T extends MCPClientInf> implements MCPClientInf {
    protected String mcpServer;
 
    
    /**
     * Mcp-Session-Id
     */
	protected String sessionId;
     
    private final static Logger baselogger = LoggerFactory.getLogger(MCPBaseClient.class);
    public MCPBaseClient(String mcpServer){
        this.mcpServer = mcpServer;
    }
    public String getMcpServer() {
        return mcpServer;
    }
    public T setMcpServer(String mcpServer) {
        this.mcpServer = mcpServer;
        return (T) this;
    }
	
	
 
         
	
    protected RequestId requestId = new RequestId();
	protected void validateResponse(MCPToolCallResponse mcpToolCallResponse) throws FunctionCallException {
		Map result = mcpToolCallResponse.getResult();
		Boolean isError = (Boolean) result.get("isError");
		if(isError != null && isError){
			
			throw new FunctionCallException(JsonUtil.object2json(mcpToolCallResponse));
		}
	}
	protected abstract MCPToolCallResponse executeToolsCall(McpToolCallRequest mcpToolCallRequest);
	public MCPToolCallResponse toolsCall(FunctionTool functionTool){
        McpToolCallRequest mcpToolCallRequest = new McpToolCallRequest();
        mcpToolCallRequest.setId(this.requestId.nextReqNo());
        mcpToolCallRequest.functionName(functionTool.getFunctionName());
        mcpToolCallRequest.arguments(functionTool.getArguments());
		TraceMessage traceMessage = null;
		if(AgentTraceHolder.isToolTrace()) {
			traceMessage = new TraceMessage();
			traceMessage.setStartTime(System.currentTimeMillis())
					.put("mcpserver", getMcpServer())
					.put("mcpToolCallRequest", mcpToolCallRequest)
					.put("role", SessionMessage.MESSAGE_TYPE_MCPCALL_MESSAGE_NAME);
		}
		try {
			MCPToolCallResponse mcpToolCallResponse = executeToolsCall(  mcpToolCallRequest);
			validateResponse(mcpToolCallResponse    );
			if (AgentTraceHolder.isToolTrace()) {
				traceMessage.setEndTime(System.currentTimeMillis())
						.put("mcpToolCallResponse", mcpToolCallResponse);
				
				AgentTraceHolder.trace(traceMessage);
			}
			return mcpToolCallResponse;
		}
		catch (RuntimeException e){
			if(AgentTraceHolder.isToolTrace() && traceMessage != null) {
				try {
					traceMessage.setEndTime(System.currentTimeMillis())
							.put("mcpToolCallException", SimpleStringUtil.exceptionToString(e));
					AgentTraceHolder.trace(traceMessage);
				} catch (Exception te) {
					
				}
			}
			throw e;
		}
    }
	protected abstract MCPListToolResponse executeListTools(McpListToolRequest mcpToolRequest);
    public MCPListToolResponse listTools(){
         
//        String listTools = """
//                {
//                  "jsonrpc": "2.0",
//                  "id": 1,
//                  "method": "tools/list"
//                }
//                """;
		McpListToolRequest mcpToolRequest = new McpListToolRequest();
		mcpToolRequest.setId(this.requestId.nextReqNo());
		MCPListToolResponse mcpListToolResponse = executeListTools(  mcpToolRequest);
//        this.sseStreamCompleted = true;
		return mcpListToolResponse;
//        String listTools = "";
//        Map<String,String> headers = new LinkedHashMap<>();
//        headers.put("Mcp-Session-Id", sessionId);
       
//		StringBuilder builder = new StringBuilder();
//		List<String> results = new ArrayList<>();
//		// 使用 collectList() 收集所有数据，然后处理
//		AIAgentUtil.stream(mcpServer, messagePath, listTools, HttpMethodName.HTTP_POST, new DataCollector() {
//			@Override
//			public void collector(String data) {
//				results.add(data);
//			}
//		});
//		
//		
//		
//		// 处理收集到的结果
//		if (results != null) {
//			for (String chunk : results) {
//				handlListToolEvent(chunk, builder);
//			}
//		}
//		logger.info("listTools:{}", builder.toString());
//		return builder.toString();
    }
	
	protected abstract String executeNotificationsInitialized(McpToolRequest notificationsInitialized );
    public String notificationsInitialized()
    {
//        String notificationsInitialized = """
//                {
//                  "jsonrpc": "2.0",
//                  "method": "notifications/initialized"
//                }
//                """;
//        String notificationsInitialized = "";
//        Map<String,String> headers = new LinkedHashMap<>();
//        headers.put("Mcp-Session-Id", sessionId);
		McpToolRequest notificationsInitialized = new McpToolRequest();
		notificationsInitialized.setMethod("notifications/initialized");
        String data = executeNotificationsInitialized(  notificationsInitialized );
         if(baselogger.isDebugEnabled()) {
             baselogger.debug("{} notificationsInitialized:{}",mcpServer, data);
         }
		return data;
    }
	
	public String getSessionId() {
		return sessionId;
	}
	protected abstract MCPInitializedToolResponse executeInitialization(McpInitializedToolRequest mcpInitializedToolRequest );
	public MCPInitializedToolResponse initialization(){
//        String initJson = """
//              {"jsonrpc":"2.0","id":0,"method":"initialize",
//              "params":{"protocolVersion":"2025-06-18",
//              "capabilities":{"elicitation":{}},
//              "clientInfo":{"name":"mcp-client","version":"1.0.0"}}}
//        """;
		McpInitializedToolRequest mcpInitializedToolRequest = new McpInitializedToolRequest();
		mcpInitializedToolRequest.setId(requestId.nextReqNo());
		mcpInitializedToolRequest.clientInfo("mcp-client","1.0.0");		
		mcpInitializedToolRequest.protocolVersion("2025-06-18");
		mcpInitializedToolRequest.setJsonrpc("2.0");
		return executeInitialization(mcpInitializedToolRequest);
		
////        String initJson = "";
////        Map<String,String> headers = new LinkedHashMap<>();
////        headers.put("Mcp-Session-Id", sessionId);
//        String data = HttpRequestProxy.sendJsonBody(mcpServer,mcpInitializedToolRequest, messagePath,String.class);
//        if(true)
//            return data;
//         
//		logger.info("initialization:{}", builder.toString());
//        return builder.toString();

    }

}
