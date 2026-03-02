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

import org.frameworkset.spi.ai.mcp.model.MCPListToolResponse;
import org.frameworkset.spi.ai.model.Function;
import org.frameworkset.spi.ai.model.FunctionToolDefine;
import org.frameworkset.spi.ai.model.Parameters;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author biaoping.yin
 * @Date 2026/3/2
 */
public class MCPToolsUtils {
	/**
	 * 将mcp服务转换为function工具定义
	 * 		FunctionToolDefine functionToolDefine = new FunctionToolDefine();
	 * //        functionToolDefine.setType("function");
	 * 		functionToolDefine.funtionName2ndDescription("weather_info_query","天气查询服务，根据城市查询当地温度和天气信息")
	 * //                            .putParametersType("object")
	 * 				.requiredParameters("location")
	 * 				.addSubParameter("params","location","string","城市或者地州, 例如：上海市")
	 * 				.setFunctionCall(new ToolFunctionCall() );
	 * @return
	 */
	public static List<FunctionToolDefine> convertMcpTools2FunctionTools(MCPListToolResponse mcpListToolResponse){
		
		List<Map> mcpTools = mcpListToolResponse.tools();
		if(mcpTools != null && mcpTools.size() > 0) {
			List<FunctionToolDefine> functionToolDefines = new ArrayList<>(mcpTools.size());
			for (Map mcpTool : mcpTools) {
				FunctionToolDefine functionToolDefine = new FunctionToolDefine();
				functionToolDefine.funtionName2ndDescription((String)mcpTool.get("name"), (String)mcpTool.get("description"));
				Map inputSchema = (Map)mcpTool.get("inputSchema");
				Parameters parameters = new Parameters();
				parameters.setType((String)inputSchema.get("type"));
				parameters.setRequired((List<String>)inputSchema.get("required"));
				parameters.setProperties((Map)inputSchema.get("properties"));
				functionToolDefine.putParameters(parameters);			
//				functionToolDefine.requiredParameters((String[])mcpTool.get("inputSchema"))			 
				 
				functionToolDefines.add(functionToolDefine);
			}
			return functionToolDefines;
		}
		return null;
	}
	private static Map convertFunction2Tool(Function function){
		Map tool = new LinkedHashMap();
		tool.put("name", function.getName());
		tool.put("title", function.getName());
		tool.put("description", function.getDescription());
		tool.put("inputSchema", function.getParameters());
		return tool;
	}
	public static MCPListToolResponse convertFunctionTools2McpTools(List<FunctionToolDefine> functionToolDefines){
		MCPListToolResponse mcpListToolResponse = new MCPListToolResponse();
		
		if(functionToolDefines != null && functionToolDefines.size() > 0){
			List<Map> tools = new ArrayList<>(functionToolDefines.size());
			for (FunctionToolDefine functionToolDefine : functionToolDefines){
				Function function = functionToolDefine.getFunction();
				Map tool = convertFunction2Tool(function);
				tools.add(tool);
			}
			mcpListToolResponse.putTools(tools);
		}
		else{
			mcpListToolResponse.putTools(null);
		}	
		

		return mcpListToolResponse;
	}
}
