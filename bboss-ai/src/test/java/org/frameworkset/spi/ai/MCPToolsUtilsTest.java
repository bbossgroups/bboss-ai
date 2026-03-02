package org.frameworkset.spi.ai;
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
import org.frameworkset.spi.ai.mcp.model.MCPListToolResponse;
import org.frameworkset.spi.ai.mcp.tools.MCPToolsUtils;
import org.frameworkset.spi.ai.model.FunctionToolDefine;
import org.frameworkset.spi.remote.http.HttpRequestProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author biaoping.yin
 * @Date 2026/3/2
 */
public class MCPToolsUtilsTest {
	private static Logger logger = LoggerFactory.getLogger(MCPToolsUtilsTest.class);
	public static void main(String[] args){
		Map properties = new HashMap();
		
		//deepseek为的Deepseek服务数据源名称
		properties.put("http.poolNames","tool");	
		
		
		properties.put("tool.http.hosts","127.0.0.1:8080");///设置tool服务地址
		properties.put("tool.http.apiKeyId","17689048891086XsDsJVgwiQcmKhOdh23DX4NT");//设置apiKey
		properties.put("tool.http.timeoutSocket","60000");
		properties.put("tool.http.timeoutConnection","40000");
		properties.put("tool.http.connectionRequestTimeout","70000");
		properties.put("tool.http.maxTotal","200");
		properties.put("tool.http.defaultMaxPerRoute","100");
		HttpRequestProxy.startHttpPools(properties);
		
		
		List<FunctionToolDefine> functionToolDefines = HttpRequestProxy.httpGetforList("tool","/function/userTools.api",FunctionToolDefine.class);
		
		MCPListToolResponse mcpListToolResponse = MCPToolsUtils.convertFunctionTools2McpTools(functionToolDefines);
		
		logger.info(JsonUtil.object2jsonPretty(mcpListToolResponse));
		
	}
	
}
