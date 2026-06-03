/**
 *  Copyright 2008-2010 biaoping.yin
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.frameworkset.spi.ai.mcp.tools.server;


import org.frameworkset.spi.ai.model.FunctionToolDefine;

import java.util.List;

public interface MCPApiKeyService {
    /**
     * 注册mcp服务端工具到多个key，可多次调用，注册多个bean tool
      * @param apiKeys
     * @param bean
     */  
	void registMcpBeanTool(String[] apiKeys,Object bean);

    /**
     * 注册mcp服务端工具到单个key，可多次调用，注册多个bean tool
     * @param apiKey
     * @param bean
     */
    void registMcpBeanTool(String apiKey,Object bean);
	/**
	 * 校验apiKey是否存在
	 * @param apiKey
	 * @return
	 */
	Boolean auth(String apiKey);

    /**
     * 校验apiKey是否有访问functionName的权限
     * @param apiKey
     * @return
     */
    Boolean auth(String functionName,String apiKey);
    List<FunctionToolDefine> getMcpServerApiKeyInfo(String apiKey);

    FunctionToolDefine getFunctionToolDefine(String apiKey, String functionName);
}