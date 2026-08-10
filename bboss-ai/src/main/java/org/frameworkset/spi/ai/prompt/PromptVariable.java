package org.frameworkset.spi.ai.prompt;
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

import com.frameworkset.util.VariableHandler;
import org.frameworkset.spi.ai.model.AIFlowConst;
import org.frameworkset.spi.ai.model.AIRuntimeException;

/**
 *
 * @author biaoping.yin
 * @Date 2026/8/10
 */
public class PromptVariable  extends VariableHandler.TypeDefaultValueVariable {
	
	private int scope = AIFlowConst.AIFLOW_VAR_SCOPE_FLOW;
	private String type = AIFlowConst.AIFLOW_VAR_TYPE_TEXT;
	private Object cacheValue = null;
	private Object lock = new Object();
	
	
	
	private String httpproxy;
	private String beanservice;
	
	
	/**
	 * 缓存标识需放置在变量属性字符串的最后
	 */
	private boolean cache = true;
	/**
	 * 变量值字符集，当type为file、url、resource时起作用
	 */
	private String charset = "UTF-8"; // Default character set
	
	public int getScope() {
		return scope;
	}
	
	public String getCharset() {
		return charset;
	}
	
	public String getType() {
		return type;
	}
	
	public Object getLock() {
		return lock;
	}
	
	public void setCacheValue(Object cacheValue) {
		this.cacheValue = cacheValue;
	}
	
	public Object getCacheValue() {
		return cacheValue;
	}
	public boolean isCache() {
		return cache;
	}
	
	public String getHttpproxy() {
		return httpproxy;
	}
	
	public String getBeanservice() {
		return beanservice;
	}
	@Override
	/**
	 * 变量属性解析完毕后，对变量属性信息进行额外处理
	 */
	public void afterSetAttribute(){
		if(this.attributes != null) {
//				int pos = this.attributes.indexOf(",");
			String[] ts = attributes.split(",");
			
			for (int i = 0; i < ts.length; i ++) {
				String t = ts[i];
				if (t.startsWith("scope=")) {
					String q = t.substring("scope=".length()).trim();
					if(q.equals("node"))
						scope = AIFlowConst.AIFLOW_VAR_SCOPE_NODE;
					else if(q.equals("flow"))
						scope = AIFlowConst.AIFLOW_VAR_SCOPE_FLOW;
					else if(q.equals("container"))
						scope = AIFlowConst.AIFLOW_VAR_SCOPE_CONTAINER;
					else{
						throw new AIRuntimeException("scope must be node,flow or container:"+q+" in variable:"+this.getVariableName());
					}
				}
				else if (t.startsWith("type=")) {
					String q = t.substring("type=".length()).trim();
					if(q.equals("text"))
						type = AIFlowConst.AIFLOW_VAR_TYPE_TEXT;
					else if(q.equals("file")){
						type = AIFlowConst.AIFLOW_VAR_TYPE_FILE;
						
					} else if(q.equals("url")){
						cache = false;
						type = AIFlowConst.AIFLOW_VAR_TYPE_URL;
					} else if(q.equals("resource")){
						type = AIFlowConst.AIFLOW_VAR_TYPE_RESOURCE;
					} else if(q.equals("service")){
						cache = false;
						type = AIFlowConst.AIFLOW_VAR_TYPE_SERVICE;
					}
					else{
						throw new AIRuntimeException("type must be text,file or url:"+q+" in variable:"+this.getVariableName());
					}
					
				}
				else if (t.startsWith("beanservice=")) {
					this.beanservice = t.substring("beanservice=".length()).trim();
				}
				else if (t.startsWith("httpproxy=")) {
					this.httpproxy = t.substring("httpproxy=".length()).trim();
				}
				else if (t.startsWith("cache=")) {
					this.cache = Boolean.parseBoolean(t.substring("cache=".length()).trim());
				}
				else if (t.startsWith("charset=")) {
					this.charset = t.substring("charset=".length()).trim();
				}
				else{
					parserTypeAndDefaultObjectValue(t);
				}
				
				
			}
			
			
		}
	}
}
