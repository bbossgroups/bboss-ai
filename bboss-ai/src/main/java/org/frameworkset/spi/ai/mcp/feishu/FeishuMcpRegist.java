package org.frameworkset.spi.ai.mcp.feishu;
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

import com.frameworkset.util.SimpleStringUtil;
import org.frameworkset.spi.ai.mcp.MCPClient;
import org.frameworkset.spi.ai.mcp.tools.MCPToolsRegist;
import org.frameworkset.spi.feishu.BaseFeishuConfig;
import org.frameworkset.spi.feishu.FeishuAuthorTokenFunction;
import org.frameworkset.spi.remote.http.ClientConfiguration;

/**
 * @author biaoping.yin
 * @Date 2026/3/31
 */
public class FeishuMcpRegist extends MCPToolsRegist {
    private BaseFeishuConfig baseFeishuConfig;
    private String tools;
    private String appId;
    private String appSecret;
    public FeishuMcpRegist(String mcpServer ) {
        super(mcpServer);
        ClientConfiguration clientConfiguration = ClientConfiguration.getClientConfiguration(mcpServer);
        appId = clientConfiguration.getExtendConfig("appId");
        appSecret = clientConfiguration.getExtendConfig("appSecret");
        tools = clientConfiguration.getExtendConfig("tools");
    }
	public FeishuMcpRegist(String mcpServer,BaseFeishuConfig baseFeishuConfig) {
		super(mcpServer);
        this.baseFeishuConfig = baseFeishuConfig;
	}

    public FeishuMcpRegist(String mcpServer,String appId,String appSecret,String tools) {
        super(mcpServer);
        this.tools = tools;
        this.appId = appId;
        this.appSecret = appSecret;
    }

    public FeishuMcpRegist setAppId(String appId) {
        this.appId = appId;
        return this;
    }

    public FeishuMcpRegist setAppSecret(String appSecret) {
        this.appSecret = appSecret;
        return this;
    }

    public FeishuMcpRegist setTools(String tools) {
        this.tools = tools;
        return this;
    }

    @Override
    protected MCPClient buildMCPClient(){
        return new FeishuMCPClient(mcpServer,baseFeishuConfig);
    }

    @Override
    public void init() {
        ClientConfiguration clientConfiguration = ClientConfiguration.getClientConfiguration(mcpServer);
        if(clientConfiguration.getAuthorTokenExpiredTime() > 1500000L)
            clientConfiguration.setAuthorTokenExpiredTime(1500000L);
        if(baseFeishuConfig == null){
            BaseFeishuConfig baseFeishuConfig = new BaseFeishuConfig();
//            bboss应用
            baseFeishuConfig.setFeishuAppId(appId)
                    .setFeishAppSecret(appSecret);
 
            String feishuDatasource = SimpleStringUtil.getUUID32();
            baseFeishuConfig.addHttpConfig("http.poolNames", feishuDatasource)
                    .addHttpConfig(feishuDatasource+ ".http.hosts", "https://open.feishu.cn")
                    .addHttpConfig(feishuDatasource+ ".http.maxTotal", 10)
                    .addHttpConfig(feishuDatasource+ ".http.defaultMaxPerRoute", 10)
                    .addHttpConfig(feishuDatasource+ ".http.timeoutConnection", clientConfiguration.getTimeoutConnection())
                    .addHttpConfig(feishuDatasource+ ".http.connectionRequestTimeout", clientConfiguration.getConnectionRequestTimeout())
//                    #socket通讯超时时间，如果在通讯过程中出现sockertimeout异常，可以适当调整timeoutSocket参数值，单位：毫秒
                    .addHttpConfig(feishuDatasource+ ".http.timeoutSocket", clientConfiguration.getTimeoutSocket())
                    .addHttpConfig(feishuDatasource+ ".http.authorTokenFunction","org.frameworkset.spi.feishu.FeishuAuthorTokenFunction")
                    .addHttpConfig(feishuDatasource+ ".http.authorTokenExpiredTime",clientConfiguration.getAuthorTokenExpiredTime())
                    .addHttpConfig(feishuDatasource+ ".http.extendConfigs.appId",appId)
                    .addHttpConfig(feishuDatasource+ ".http.extendConfigs.appSecret", appSecret)
                    
                    .setMcpTools(tools);
            this.baseFeishuConfig = baseFeishuConfig;
        }
        baseFeishuConfig.build();
        
        if(clientConfiguration != null && clientConfiguration.getAuthorTokenFunctionObject() != null) {
            FeishuMCPAuthorTokenFunction feishuAuthorTokenFunction = (FeishuMCPAuthorTokenFunction) clientConfiguration.getAuthorTokenFunctionObject();
            if(feishuAuthorTokenFunction != null) {
                feishuAuthorTokenFunction.setFeishuDatasource(baseFeishuConfig.getFeishuDataSource());
            }
        }
        baseFeishuConfig.initFeishHelper();
        super.init();
        
    }

 

    @Override
    public void destroy() {
        try {
            super.destroy();
        }
        catch (Exception e){
            
        }
        try {
            baseFeishuConfig.destroy();
        }
        catch (Exception e){
            
        }
    }
}
