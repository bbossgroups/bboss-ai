package org.frameworkset.spi.ai.adapter;
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

import org.frameworkset.spi.ai.model.ChatAgentMessage;
import org.frameworkset.spi.remote.http.ClientConfiguration;

import java.util.Map;

/**
 * Deepseek模型智能体适配器
 * @author biaoping.yin
 * @Date 2026/1/4
 */
public class DeepseekAgentAdapter extends QwenAgentAdapter{
    @Override
    public String getChatCompletionsUrl(ClientConfiguration clientConfiguration, ChatAgentMessage chatAgentMessage) {
        return "/chat/completions";
    }
	
 
	
	@Override
	public String getListModelsUrl(ClientConfiguration config, Map params) {
		StringBuilder sb = new StringBuilder();
		sb.append("/models");
		if(params != null && params.size() > 0){
			sb.append("?");
			int i = 0;
			for (Object key : params.keySet()) {
				if(i > 0) sb.append("&");
				sb.append(key).append("=").append(params.get(key));
				i ++;
			}
		}
		return sb.toString();
	}
}
