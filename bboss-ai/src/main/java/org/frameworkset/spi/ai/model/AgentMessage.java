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
import org.frameworkset.spi.ai.callback.ChatContext;
import org.frameworkset.spi.remote.http.ClientConfiguration;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author biaoping.yin
 * @Date 2026/1/4
 */
public class AgentMessage<T extends AgentMessage> {



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
    public ChatObject buildChatObject(ClientConfiguration clientConfiguration, AgentAdapter agentAdapter, AIAgent aiAgent, boolean fromStreamAPI, ChatContext chatCallback){
        return null;
    }
    public T setPrompt(String prompt) {
        this.prompt = prompt;
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

    public LastSessionMessage addAgentResultSessionMessage(TokenMetrics tokenMetrics,String message,AIAgent aiAgent){
        return aiAgent.addAgentResultSessionMessage(  tokenMetrics,message);
        /**
         AgentSessionStore mainSessionStore = aiAgent.getMainSessionStore();
         //        initSessionStore();
         if(mainSessionStore == null){
         return null;
         }
         String agentId = aiAgent != null ?aiAgent.getAgentId():null;
         AgentSessionStore agentSessionStore = getAgentSessionStore(  agentId);
         if(agentSessionStore == null){
         return null;
         }
         LastSessionMessage lastSessionMessage = agentSessionStore.addAgentResultSessionMessage(message);
         if(!aiAgent.isDisablePush2ParentLastSubMessage()) {
         //        if(!aiAgent.isDisableGloableStore()) {
         agentSessionStore.setParentAgentLastSessionMessage(lastSessionMessage);
         }
         return lastSessionMessage;
         */
    }


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
}
