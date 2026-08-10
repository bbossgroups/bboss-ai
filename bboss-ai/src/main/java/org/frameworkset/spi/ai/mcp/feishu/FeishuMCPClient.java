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

import org.frameworkset.spi.ai.mcp.MCPClient;
import org.frameworkset.spi.ai.mcp.MCPClientInf;
import org.frameworkset.spi.feishu.BaseFeishuConfig;
import org.frameworkset.spi.feishu.FeishuHelper;

/**
 * @author biaoping.yin
 * @Date 2026/3/31
 */
public class FeishuMCPClient extends MCPClient {
    private FeishuHelper feishuHelper;
    private BaseFeishuConfig baseFeishuConfig;
    public FeishuMCPClient(String mcpServer,BaseFeishuConfig baseFeishuConfig) {
        super(mcpServer);
        this.baseFeishuConfig = baseFeishuConfig;
    }
    
    @Override
    public void init() {      
        this.feishuHelper = baseFeishuConfig.getFeishuHelper();
		MCPClientInf mcpClientInf = new FeishuMCPStreamableClient(mcpServer,baseFeishuConfig);
		mcpClientInf.setToolCallRetry(this.getToolCallRetry());
		mcpClientInf.init();
		this.mcpClientInf = mcpClientInf;
    }
}
