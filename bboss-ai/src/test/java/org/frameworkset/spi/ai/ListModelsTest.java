package org.frameworkset.spi.ai;
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

import org.frameworkset.spi.remote.http.HttpRequestProxy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author biaoping.yin
 * @Date 2026/8/19
 */
public class ListModelsTest {
	public static void main(String[] args){
		HttpRequestProxy.startHttpPools("application-stream.properties");
		AIAgent agent = new AIAgent();
		Map params = new LinkedHashMap();
		params.put("model","qwen3.7-plus");
//		List<Map<String, Object>> models = agent.listModels("qwenvlplus",params);
//		String models = agent.listModelsString("qwenvlplus",params);
//		System.out.println(models);
//		
//		 models = agent.listModelsString("deepseek");
//		System.out.println(models);
//		
//		models = agent.listModelsString("kimi");
//		System.out.println(models);
//		
//		models = agent.listModelsString("zhipu");
//		System.out.println(models);
		
		String models = agent.listModelsString("volcengineapi");
		System.out.println(models);
	}
}
