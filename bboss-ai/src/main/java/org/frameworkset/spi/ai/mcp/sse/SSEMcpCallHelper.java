package org.frameworkset.spi.ai.mcp.sse;
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
import org.frameworkset.spi.ai.mcp.model.*;
import org.frameworkset.spi.remote.http.HttpRequestProxy;
import org.frameworkset.util.concurrent.ThreadPoolFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * @author biaoping.yin
 * @Date 2026/2/28
 */
public class SSEMcpCallHelper {
	private Logger logger = LoggerFactory.getLogger(SSEMcpCallHelper.class);
	private Map<String, McpCallObject> mcpCallObjects = new ConcurrentHashMap<>();
	private ExecutorService executor ;
	public void init(MCPSSEClient client){
		executor = ThreadPoolFactory.buildThreadPool(client.getBulkProcessorName()+"-"+client.getMcpServer(), client.getBulkRejectMessage(), 
				client.getWorkThreads(),client.getWorkThreadQueue(),
				client.getBlockedWaitTimeout()
				,client.getWarnMultsRejects());
	}
	public void destory(){
		if(executor != null){
			executor.shutdown();
		}
		clearCalls();
	}
	
	public void clearCalls(){
		Iterator<Map.Entry<String, McpCallObject>> iterator = mcpCallObjects.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<String, McpCallObject> entry = iterator.next();
			McpCallObject mcpCallObject = entry.getValue();
			mcpCallObject.setException(new McpCallException("MCP call destoried"));
			mcpCallObject.countDown();
		}
		mcpCallObjects.clear();
	}
	
	public MCPInitializedToolResponse initializationCall(MCPSSEClient mcpClient , McpInitializedToolRequest mcpInitializedToolRequest){
		McpCallObject<MCPInitializedToolResponse> mcpCallObject = new McpCallObject<>(MCPInitializedToolResponse.class);
		mcpCallObject.setRequestId(mcpInitializedToolRequest.getId());
        String requestId = mcpCallObject.getRequestId()+"";
        mcpCallObjects.put(requestId, mcpCallObject);
        try {
             HttpRequestProxy.sendJsonBody(mcpClient.getMcpServer(),mcpInitializedToolRequest, mcpClient.getMessagePath(),String.class);

            return handleResponse(mcpCallObject);
        }
        catch (Exception e){
           
            throw new McpCallException(e);
        }
        finally {
            this.removeMcpCallObject(requestId);
        }
		
		
		 
	}
	public void handleMcpSSEMessage(String message){
		executor.submit(() -> {
			
			
			try {
				if(logger.isDebugEnabled()) {
					logger.debug(message);
				}
				Map map = JsonUtil.json2Object(message, Map.class);
				Object requestId =  map.get("id");
				if(requestId != null){
					
					McpCallObject mcpCallObject = getMcpCallObject(requestId.toString());
					if(mcpCallObject != null){
						Class<?> responseType = mcpCallObject.getResponseType();
						try {
							Object data = JsonUtil.json2Object(message, responseType);
							mcpCallObject.setResponse(data);
						}
						catch (Throwable e){
							mcpCallObject.setException(new McpCallException(e));
						}
						mcpCallObject.countDown();
					}
					
				}
				else {
					
					logger.info(message);
				}
			} catch (Exception e) {
				logger.error("处理MCP SSE消息异常: " + message, e);
			}
		});
		
	}
	private McpCallObject getMcpCallObject(String requestId){
		return mcpCallObjects.remove(requestId);
	}

    private McpCallObject removeMcpCallObject(String requestId){
        return mcpCallObjects.remove(requestId);
    }


    public MCPListToolResponse listTools(MCPSSEClient mcpClient, McpListToolRequest mcpToolRequest) {
		McpCallObject<MCPListToolResponse> mcpCallObject = new McpCallObject<>(MCPListToolResponse.class);
		mcpCallObject.setRequestId(mcpToolRequest.getId());
        String requestId = mcpCallObject.getRequestId()+"";
		mcpCallObjects.put(requestId, mcpCallObject);
        try {
            HttpRequestProxy.sendJsonBody(mcpClient.getMcpServer(), mcpToolRequest, mcpClient.getMessagePath(), String.class);
            return handleResponse(mcpCallObject);
           
        }
        catch (Exception e){
           
            throw new McpCallException(e);
        }
        finally {
            this.removeMcpCallObject(requestId);
        }
       
	}
	private <T> T handleResponse(McpCallObject<T> mcpCallObject){
		mcpCallObject.await();
		if(mcpCallObject.getException() != null){
			throw mcpCallObject.getException();
		}
		return mcpCallObject.getResponse();
	}

	/**
     * 调用MCP工具
     *
     * @param mcpToolCallRequest
     * @return
     */
    public MCPToolCallResponse toolsCall(MCPSSEClient mcpClient, McpToolCallRequest mcpToolCallRequest) {
		McpCallObject<MCPToolCallResponse> mcpCallObject = new McpCallObject<>(MCPToolCallResponse.class);
		mcpCallObject.setRequestId(mcpToolCallRequest.getId());
        String requestId = mcpCallObject.getRequestId()+"";
		mcpCallObjects.put(requestId, mcpCallObject);
         
		try {
            HttpRequestProxy.sendJsonBody(mcpClient.getMcpServer(),mcpToolCallRequest, mcpClient.getMessagePath(),String.class);
            MCPToolCallResponse mcpToolCallResponse = handleResponse(  mcpCallObject);
             
            return mcpToolCallResponse;
        }
		catch (McpCallException e){
			
			throw e;
		}
        catch (Exception e){                   
           
            throw new McpCallException(e);
        }
        finally {
            this.removeMcpCallObject(requestId);
        }
        
        
       
    }
}
