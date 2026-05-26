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

import org.frameworkset.spi.ai.model.FunctionToolDefine;
import org.frameworkset.spi.ai.tool.BaseBeanToolsRegist;

import java.util.List;
import java.util.Map;

/**
 * @author biaoping.yin
 * @Date 2026/5/26
 */
public class MCPApiKeyServiceImpl implements MCPApiKeyService{
    private Map<String,MCPBeanToolsRegist> mcpServerApiKeyInfo = new java.util.concurrent.ConcurrentHashMap();

    @Override
    public void registMcpBeanTool(String[] apiKeys,Object bean) {
        MCPBeanToolsRegist mcpBeanToolsRegist = new MCPBeanToolsRegist(bean);
        mcpBeanToolsRegist.init();
        for(String apiKey : apiKeys){
            mcpServerApiKeyInfo.put(apiKey,mcpBeanToolsRegist);
        }
        
    }

    @Override
    public void registMcpBeanTool(String apiKey, Object bean) {
        MCPBeanToolsRegist mcpBeanToolsRegist = new MCPBeanToolsRegist(bean);
        mcpBeanToolsRegist.init();
        mcpServerApiKeyInfo.put(apiKey,mcpBeanToolsRegist);

    }

    /**
     * 校验apiKey是否存在
     *
     * @param apiKey
     * @return
     */
    @Override
    public Boolean auth(String apiKey) {
        return true;
    }

    /**
     * 校验apiKey是否有访问functionName的权限
     *
     * @param functionName
     * @param apiKey
     * @return
     */
    @Override
    public Boolean auth(String functionName, String apiKey) {
        return true;
    }

    @Override
    public List<FunctionToolDefine> getMcpServerApiKeyInfo(String apiKey) {
        MCPBeanToolsRegist mcpBeanToolsRegist = mcpServerApiKeyInfo.get(apiKey);
        return mcpBeanToolsRegist.registTools();    
    }

    @Override
    public FunctionToolDefine getFunctionToolDefine(String apiKey, String functionName) {
        BaseBeanToolsRegist mcpBeanToolsRegist = mcpServerApiKeyInfo.get(apiKey);
        return mcpBeanToolsRegist.getFunctionToolDefine(functionName);
    }
}
