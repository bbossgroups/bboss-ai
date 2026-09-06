package org.frameworkset.spi.ai.model;
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

import org.frameworkset.spi.ai.AIAgent;
import org.frameworkset.spi.ai.adapter.AgentAdapter;
import org.frameworkset.spi.ai.context.AgentRuntimeContext;
import org.frameworkset.spi.ai.context.ChatContext;
import org.frameworkset.spi.ai.util.BBOSSAIVersion;
import org.frameworkset.spi.remote.http.ClientConfiguration;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author biaoping.yin
 * @Date 2026/1/4
 */
public class AgentMessage<T extends AgentMessage> {
    private static final BBOSSAIVersion BBOSSAIVersion = new BBOSSAIVersion();
	

	private AgentRuntimeContext agentRuntimeContext;
    /**
     * 存放智能体上下文数据,用于在多个智能体之间分享变量数据
     */
    private Map<String,Object> contextData ;

    /**
     * 模型调用报错时，是否重试
     * > 0时重试
     */
    private int retry;
    /**
     * 重试时间间隔
     */
    private long retryInterval = 500L;
    private Boolean thinking;
	
	/**
	 *
	 "stream_options": {"include_usage": true}
	 */
	private Boolean includeUsage;
	
	/**
	 * 九天模型
	 * 控制思考的力度，设置effort后，将忽略enabled的值，effort共有6种取值：
	 * "xhigh" - 为思考分配最大比例的令牌（约占最大令牌数的 95%）
	 * "high" - 为思考分配较大比例的令牌（约占最大令牌数的 80%）
	 * "medium" - 分配中等比例的令牌（约占最大令牌数的 50%）
	 * "low" - 分配较小比例的令牌（约占最大令牌数的 20%）
	 * "minimal" - 分配更小比例的令牌（约占最大令牌数的 10%）
	 * "none" - 完全禁用思考功能
	 */
	private String effort;
    /**
     * maas平台数据源名称
     */
    private String maas;
    /**
     * 提示词工程
     */
    private String prompt;

 
    /**
     * 默认角色提示词工程
     */
    private String systemPrompt;
    

    public String getNegativePrompt() {
        return negativePrompt;
    }

    public T setNegativePrompt(String negativePrompt) {
        this.negativePrompt = negativePrompt;
        return (T)this;
    }
    
    

    /**
     * 反向提示词工程
     */
    private String negativePrompt ;
    private String model ;
    private Map parameters;
	
	
	/**
	 * 上下文窗口大小
	 */
	private Long contextSize;
	/**
	 * 最大生成令牌数
	 */	
    private Long maxTokens; 
    private Boolean stream;
    private Double temperature;
    private Map headers = null;
//        header.put("X-DashScope-Async","enable");
    /**
     * 消息级别模型类型，优先级高于模型服务级别配置，取值参考：
     * public class AIConstants {
     *     public static final String AI_MODEL_TYPE_QWEN = "qwen";
     *     public static final String AI_MODEL_TYPE_DOUBAO = "doubao";
     *     public static final String AI_MODEL_TYPE_DEEPSEEK = "deepseek";
     *     public static final String AI_MODEL_TYPE_KIMI = "kimi";
     *     public static final String AI_MODEL_TYPE_NONE = "none";
     *
     * }
     */
//    private String modelType;

    

    public String getPrompt() {
        return prompt;
    }
    public String getSystemPrompt() {
        return systemPrompt;
    }

    public T setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
        return (T)this;
    }
	public T setSystemPrompt(String systemPrompt,boolean setInputSystemVariable) {
		this.systemPrompt = systemPrompt;
		if(setInputSystemVariable){
			addContextData("input.system", systemPrompt);
		}
		return (T)this;
	}
    public T addHeader(String key,String value){
        if(headers == null){
            headers = new LinkedHashMap<>();
        }
        headers.put(key, value);
        return (T)this;
    }

    public Map getHeaders() {
        return headers;
    }
    
    public boolean containsHeader(String key){
        if(headers == null)
            return false;
        return headers.containsKey(key);
    }

    /**
     * 构建流式风格接口ChatObject对象
     * @param clientConfiguration
     * @param agentAdapter
     * @return
     */
    public ChatObject buildChatObject(ClientConfiguration clientConfiguration, AgentAdapter agentAdapter, AIAgent aiAgent,  ChatContext chatCallback){
        return null;
    }
    public T setPrompt(String prompt) {
        return setPrompt(prompt, true);
    }
	
	public T setPrompt(String prompt,boolean setInputQueryVariable) {
		this.prompt = prompt;
		if(setInputQueryVariable){
			addContextData("input.query", prompt);
		}
		return (T)this;
	}

    public String getModel() {
        return model;
    }

    public T setModel(String model) {
        this.model = model;
        return (T)this;
    }

    public Map getParameters() {
        return parameters;
    }

    public T setParameters(Map parameters) {
        this.parameters = parameters;
        return (T)this;
    }

    public T addParameter(String key,Object value){
        if(parameters == null){
            parameters = new LinkedHashMap<>();
        }
        parameters.put(key, value);
        return (T)this;
    }

    /**
     * 往值类型为Map的参数中添加key和value对
     * @param mapKey
     * @param key
     * @param value
     * @return
     */
    public T addMapParameter(String mapKey,String key,Object value){
        Map data = null;
        if(parameters == null){
            parameters = new LinkedHashMap<>();
            data = new LinkedHashMap();
            parameters.put(mapKey,data);
        }
        else{
            data = (Map)parameters.get(mapKey);
            if(data == null) {
                data = new LinkedHashMap();
                parameters.put(mapKey, data);
            }
        }

        data.put(key, value);
        return (T)this;
    }
//    
//    public String getModelType() {
//		return modelType;
//	}
//    
//    public T setModelType(String modelType) {
//		this.modelType = modelType;
//		return (T)this;
//	}

    public Boolean getStream() {
        return stream;
    }

    public T setStream(Boolean stream) {
        this.stream = stream;
        return (T)this;
    }
    
    public Double getTemperature() {
		return temperature;
	}

    public T setTemperature(Double temperature) {
        this.temperature = temperature;
        return (T)this;
    }
    public Long getMaxTokens() {
        return maxTokens;
    }

    public T setMaxTokens(Long maxTokens) {
        this.maxTokens = maxTokens;
        return (T)this;
    }
    

    public Boolean getThinking() {
        return thinking;
    }

    public T setThinking(Boolean thinking) {
        this.thinking = thinking;
        return (T)this;
    }
	 

    public String getMaas() {
        return maas;
    }

    public T setMaas(String maas) {
        this.maas = maas;
        return (T)this;
    }

//    public LastSessionMessage addAgentResultSessionMessage(TokenMetrics tokenMetrics,String message,AIAgent aiAgent){
//        return aiAgent.addAgentResultSessionMessage(  tokenMetrics,message);
//        /**
//         AgentSessionStore mainSessionStore = aiAgent.getMainSessionStore();
//         //        initSessionStore();
//         if(mainSessionStore == null){
//         return null;
//         }
//         String agentId = aiAgent != null ?aiAgent.getAgentId():null;
//         AgentSessionStore agentSessionStore = getAgentSessionStore(  agentId);
//         if(agentSessionStore == null){
//         return null;
//         }
//         LastSessionMessage lastSessionMessage = agentSessionStore.addAgentResultSessionMessage(message);
//         if(!aiAgent.isDisablePush2ParentLastSubMessage()) {
//         //        if(!aiAgent.isDisableGloableStore()) {
//         agentSessionStore.setParentAgentLastSessionMessage(lastSessionMessage);
//         }
//         return lastSessionMessage;
//         */
//    }


    public int getRetry() {
        return retry;
    }

    public T setRetry(int retry) {
        this.retry = retry;
        return (T)this;
    }

    public long getRetryInterval() {
        return retryInterval;
    }

    public T setRetryInterval(long retryInterval) {
        this.retryInterval = retryInterval;
        return (T)this;
    }

    public Map<String, Object> getContextData() {
        return contextData;
    }
	
	public Object getContextData(String name) {
		return contextData == null ? null : contextData.get(name);
	}
	
	
	public T setContextData(Map<String, Object> contextData) {
        this.contextData = contextData;
        return (T)this;
    }
	
	public T addContextData(String name,Object value) {
		if(contextData == null){
			contextData = new LinkedHashMap();
		}
		contextData.put(name, value);
		return (T)this;
	}
	
	public String getEffort() {
		return effort;
	}
	
	public T setEffort(String effort) {
		this.effort = effort;
		return (T)this;
	}
	
	public Boolean getIncludeUsage() {
		return includeUsage;
	}
	
	public T setIncludeUsage(Boolean includeUsage) {
		this.includeUsage = includeUsage;
		return (T)this;
	}
	
	public AgentRuntimeContext getAgentRuntimeContext() {
		return agentRuntimeContext;
	}
	
	public void setAgentRuntimeContext(AgentRuntimeContext agentRuntimeContext) {
		this.agentRuntimeContext = agentRuntimeContext;
	}
	
}
