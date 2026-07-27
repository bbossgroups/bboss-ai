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

import com.frameworkset.util.SimpleStringUtil;
import org.frameworkset.spi.ai.AIAgent;
import org.frameworkset.spi.ai.callback.ChatContext;
import org.frameworkset.spi.ai.model.AgentMessage;
import org.frameworkset.spi.ai.model.ChatAgentMessage;
import org.frameworkset.spi.ai.model.ImageVLAgentMessage;
import org.frameworkset.spi.remote.http.ClientConfiguration;

import java.util.HashMap;
import java.util.Map;

/**
 * 硅基流动模型智能体适配器
 * @author biaoping.yin
 * @Date 2026/1/4
 */
public class SiliconflowAgentAdapter extends QwenAgentAdapter{
    @Override
    public String getImageVLCompletionsUrl(ClientConfiguration clientConfiguration, ImageVLAgentMessage imageVLAgentMessage) {
        return "/chat/completions";
    }

    @Override
    public String getChatCompletionsUrl(ClientConfiguration clientConfiguration,ChatAgentMessage chatAgentMessage) {
        return "/v1/chat/completions";
    }

    @Override
    protected void filterParameters(ChatContext chatContext, AgentMessage agentMessage, AIAgent aiAgent, Map<String, Object> requestMap, Map<String, Object> parameters) {
		Boolean stream = chatContext.getStreamable();
		if(stream == null){
			stream = agentMessage.getStream();
		}
		if(SimpleStringUtil.isEmpty( parameters)){
            if( stream != null){
                requestMap.put("stream", stream);
            }
             

            if( agentMessage.getTemperature() != null){
                requestMap.put("temperature", agentMessage.getTemperature());
            }

            if( agentMessage.getMaxTokens() != null){
                requestMap.put("max_tokens", agentMessage.getMaxTokens());
            }
            
        }
        else {
           
            parameters.remove("enable_thinking");
            parameters.remove("thinking_budget");
            requestMap.putAll( parameters);
            if(!parameters.containsKey("stream") && stream != null){
                requestMap.put("stream", stream);
            }
            if(!parameters.containsKey("temperature") && agentMessage.getTemperature() != null){
                requestMap.put("temperature", agentMessage.getTemperature());
            }
            if(!parameters.containsKey("max_tokens") && agentMessage.getMaxTokens() != null){
                requestMap.put("max_tokens", agentMessage.getMaxTokens());
            }
        }
		//"stream_options": {"include_usage": true}
		Boolean includeUsage = agentMessage.getIncludeUsage();
		if(includeUsage != null){
			Map streamOptions = new HashMap();
			streamOptions.put("include_usage", includeUsage);
			requestMap.put("stream_options", streamOptions);
			
		}
        buildTools(chatContext, agentMessage,aiAgent , requestMap);
    }
  

}
