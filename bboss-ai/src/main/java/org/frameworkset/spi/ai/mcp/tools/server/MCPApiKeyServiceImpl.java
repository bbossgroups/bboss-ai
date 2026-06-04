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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * @author biaoping.yin
 * @Date 2026/5/26
 */
public class MCPApiKeyServiceImpl implements MCPApiKeyService{
    private static Logger logger = LoggerFactory.getLogger(MCPApiKeyServiceImpl.class);
    private Map<String,MCPBeanToolsRegist> mcpServerApiKeyInfo = new java.util.concurrent.ConcurrentHashMap();

    @Override
    public void registMcpBeanTool(String[] apiKeys,Object bean) {
        MCPBeanToolsRegist mcpBeanToolsRegist = new MCPBeanToolsRegist(bean);
        mcpBeanToolsRegist.init();
        if(logger.isInfoEnabled())
            logger.info("Regist MCP bean tool for apiKeys count: {}", apiKeys != null ? apiKeys.length : 0);
        for(String apiKey : apiKeys){
            mcpServerApiKeyInfo.put(apiKey,mcpBeanToolsRegist);
            if(logger.isInfoEnabled())
                logger.info("Regist MCP bean tool success for apiKey: {}", apiKey);
        }
        
    }

    private Object registLock = new Object();
    @Override
    public void registMcpBeanTool(String apiKey, Object bean) {
        MCPBeanToolsRegist mcpBeanToolsRegist = mcpServerApiKeyInfo.get(apiKey);
        if(mcpBeanToolsRegist == null) {
            synchronized (registLock) {
                mcpBeanToolsRegist = mcpServerApiKeyInfo.get(apiKey);
                if(mcpBeanToolsRegist == null) {
                    mcpBeanToolsRegist = new MCPBeanToolsRegist(bean);
                    mcpBeanToolsRegist.init();

                    mcpServerApiKeyInfo.put(apiKey, mcpBeanToolsRegist);
                    if(logger.isInfoEnabled())
                        logger.info("Regist MCP bean tool success for apiKey: {}", apiKey);
                    return;
                }
            }
        }
        mcpBeanToolsRegist.registBeanTools(bean);
        if(logger.isInfoEnabled())
            logger.info("Regist bean tools to existing MCPBeanToolsRegist for apiKey: {}", apiKey);
        

    }

    /**
     * 校验apiKey是否存在
     *
     * @param apiKey
     * @return
     */
    @Override
    public Boolean auth(String apiKey) {
        if(apiKey == null){
            if(logger.isWarnEnabled())
                logger.warn("apiKey is null,auth result is false.");
            return false;
        }
        boolean result = mcpServerApiKeyInfo.containsKey(apiKey);
        if(logger.isDebugEnabled())
            logger.debug("Auth apiKey: {}, result: {}", apiKey, result);
        return result;
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
        BaseBeanToolsRegist mcpBeanToolsRegist = mcpServerApiKeyInfo.get(apiKey);
        if(mcpBeanToolsRegist == null){
            if(logger.isDebugEnabled())
                logger.debug("Auth functionName: {}, apiKey: {} failed, apiKey not found.", functionName, apiKey);
            return false;
        }
        if(mcpBeanToolsRegist.getFunctionToolDefine(functionName) == null){
            if(logger.isDebugEnabled())
                logger.debug("Auth functionName: {}, apiKey: {} failed, function not found.", functionName, apiKey);
            return false;
        }
        if(logger.isDebugEnabled())
            logger.debug("Auth functionName: {}, apiKey: {} success.", functionName, apiKey);
        return true;
    }

    @Override
    public List<FunctionToolDefine> getMcpServerApiKeyInfo(String apiKey) {
        MCPBeanToolsRegist mcpBeanToolsRegist = mcpServerApiKeyInfo.get(apiKey);
        List<FunctionToolDefine> tools = mcpBeanToolsRegist.registTools();
        if(logger.isDebugEnabled())
            logger.debug("Get MCP server apiKey info, apiKey: {}, tools count: {}", apiKey, tools != null ? tools.size() : 0);
        return tools;
    }

    @Override
    public FunctionToolDefine getFunctionToolDefine(String apiKey, String functionName) {
        BaseBeanToolsRegist mcpBeanToolsRegist = mcpServerApiKeyInfo.get(apiKey);
        if(mcpBeanToolsRegist == null){
            if(logger.isDebugEnabled())
                logger.debug("Get function tool define failed, apiKey: {} not found.", apiKey);
            return null;
        }
        FunctionToolDefine toolDefine = mcpBeanToolsRegist.getFunctionToolDefine(functionName);
        if(logger.isDebugEnabled())
            logger.debug("Get function tool define, apiKey: {}, functionName: {}, found: {}", apiKey, functionName, toolDefine != null);
        return toolDefine;
    }
}
