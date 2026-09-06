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

import com.frameworkset.util.JsonUtil;
import com.frameworkset.util.SimpleStringUtil;
import org.frameworkset.spi.ai.model.AIConstants;
import org.frameworkset.spi.ai.model.AIRuntimeException;
import org.frameworkset.spi.remote.http.ClientConfiguration;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author biaoping.yin
 * @Date 2026/1/4
 */
public class AgentAdapterFactory {
	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AgentAdapterFactory.class);
    private static Map<String,AgentAdapter> agentAdapters = new LinkedHashMap<>();
    static{
        agentAdapters.put(AIConstants.AI_MODEL_TYPE_DOUBAO,new DoubaoAgentAdapter().initAgentAdapter());
        agentAdapters.put(AIConstants.AI_MODEL_TYPE_QWEN,new QwenAgentAdapter().initAgentAdapter());
        agentAdapters.put(AIConstants.AI_MODEL_TYPE_DEEPSEEK,new DeepseekAgentAdapter().initAgentAdapter());
        agentAdapters.put(AIConstants.AI_MODEL_TYPE_KIMI,new KimiAgentAdapter().initAgentAdapter());
        agentAdapters.put(AIConstants.AI_MODEL_TYPE_NONE,new NoneAgentAdapter().initAgentAdapter());
        agentAdapters.put(AIConstants.AI_MODEL_TYPE_SILICONFLOW,new SiliconflowAgentAdapter().initAgentAdapter());
        agentAdapters.put(AIConstants.AI_MODEL_TYPE_OPENAI,new OpenaiAgentAdapter().initAgentAdapter());
        agentAdapters.put(AIConstants.AI_MODEL_TYPE_BAIDU,new BaiduAgentAdapter().initAgentAdapter());

        agentAdapters.put(AIConstants.AI_MODEL_TYPE_JIUTIAN,new JiutianAgentAdapter().initAgentAdapter());
        agentAdapters.put(AIConstants.AI_MODEL_TYPE_ZHIPU,new ZhipuAgentAdapter().initAgentAdapter());
        agentAdapters.put(AIConstants.AI_MODEL_TYPE_MINIMAX,new MiniMaxAgentAdapter().initAgentAdapter());
        agentAdapters.put(AIConstants.AI_MODEL_TYPE_HUNYUAN,new TencentHYAgentAdapter().initAgentAdapter());
        agentAdapters.put(AIConstants.AI_MODEL_TYPE_XINFERENCE,new XinferenceAgentAdapter().initAgentAdapter());
        
        

    }

    /**
     * 注册新的模型适配器
     * @param modelType
     * @param agentAdapter
     */
    public static void registerAgentAdapter(ClientConfiguration clientConfiguration,String modelType, AgentAdapter agentAdapter){
		if(agentAdapters.containsKey(modelType)) {
//			throw new AIRuntimeException("modelType:[" + modelType + "] has been registered.");
			if(log.isInfoEnabled()) {
				log.info("modelType:[{}] agentAdapter[{}]  used in [{}] has been registered.",modelType,agentAdapter.getClass().getName(),clientConfiguration.getBeanName());
			}
			return;
		}
        agentAdapters.put(modelType,agentAdapter.initAgentAdapter());
    }

    /**
     * 注册新的模型适配器
     * @param modelType
     * @param agentAdapterClass
     */
    public static void registerAgentAdapter(ClientConfiguration clientConfiguration,String modelType, Class<? extends AgentAdapter> agentAdapterClass){
        AgentAdapter agentAdapter = null;
        try {
            agentAdapter = agentAdapterClass.newInstance();
            registerAgentAdapter(   clientConfiguration, modelType,   agentAdapter);
        } catch (Exception e) {
            throw new AIRuntimeException(e);
        }  
       
    }

    /**
     * 注册新的模型适配器
     * @param modelType
     * @param agentAdapterClass
     */
    public static void registerAgentAdapter(ClientConfiguration clientConfiguration,String modelType, String agentAdapterClass){
        AgentAdapter agentAdapter = null;
        try {
            Class<? extends AgentAdapter> agentAdapterClass_ = (Class<? extends AgentAdapter>) Class.forName(agentAdapterClass);
            registerAgentAdapter(  clientConfiguration,  modelType,   agentAdapterClass_);
        } catch (AIRuntimeException e) {
            throw e;
        }catch (Exception e) {
            throw new AIRuntimeException(e);
        }

    }
    
    public static AgentAdapter getAgentAdapter(String modelType) {
        AgentAdapter agentAdapter = null;
        if (SimpleStringUtil.isNotEmpty(modelType)) {
            agentAdapter = agentAdapters.get(modelType);
        } else {
            agentAdapter = agentAdapters.get(AIConstants.AI_MODEL_TYPE_NONE);
        }
		if(log.isDebugEnabled()){
			log.debug("all supper model types are:{}",JsonUtil.object2json(agentAdapters.keySet()));
		}
        if(agentAdapter == null){
			
            throw new AIRuntimeException("modelType:["+modelType+"] is not supported,all supper model types are:"+ JsonUtil.object2json(agentAdapters.keySet()));
        }
        return agentAdapter;
    }

    
    public static AgentAdapter getAgentAdapter(ClientConfiguration clientConfiguration,Object message) {
        String modelType = clientConfiguration.getModelType();
        if(modelType == null || modelType.equals("")){
            modelType = clientConfiguration.getHosts();
        }

        return AgentAdapterFactory.getAgentAdapter(modelType);
    }
	
	
	public static AgentAdapter getAgentAdapter(ClientConfiguration clientConfiguration) {
		String modelType = clientConfiguration.getModelType();
		if(modelType == null || modelType.equals("")){
			modelType = clientConfiguration.getHosts();
		}
		
		return AgentAdapterFactory.getAgentAdapter(modelType);
	}

}
