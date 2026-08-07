package org.frameworkset.spi.ai.tool;
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

import java.util.Map;

/**
 * 将工具参数封装为上下文，传递给其他流程使用
 * @author biaoping.yin
 * @Date 2026/8/7
 */
public class ToolCallContext {
	private Map<String,Object> params;
	public Map<String,Object> getParams() {
		return params;
	}
	public void setParams(Map<String,Object> params) {
		this.params = params;
	}
	public ToolCallContext addParam(String key,Object value){
		if(params == null){
			params = new java.util.LinkedHashMap<>();
		}
		params.put(key, value);
		return this;
	}
	
	public ToolCallContext addParams(Map<String,Object> params){
		if(this.params == null){
			this.params = new java.util.LinkedHashMap<>();
		}
		this.params.putAll(params);
		return this;
	}
	
	public Object getParam(String key){
		if(params == null){
			return null;
		}
		return params.get(key);
	}
	
	public Boolean getBooleanParam(String key){
		if(params == null){
			return null;
		}
		return (Boolean)params.get(key);
	}
	
}
