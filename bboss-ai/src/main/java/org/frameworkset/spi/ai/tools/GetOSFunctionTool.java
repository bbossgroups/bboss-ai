package org.frameworkset.spi.ai.tools;
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

import org.frameworkset.spi.ai.model.annotation.Tool;
import org.frameworkset.spi.ai.model.annotation.ToolParam;
import org.slf4j.Logger;

import java.util.Map;

/**
 * @author biaoping.yin
 * @Date 2026/6/23
 */
public class GetOSFunctionTool {
    private static Logger logger = org.slf4j.LoggerFactory.getLogger(GetOSFunctionTool.class);
	/** Default timeout in seconds*/
	private long timeout = 60;
	public GetOSFunctionTool(){
		
	}
	public GetOSFunctionTool(long timeout){
		this.timeout = timeout;
	}
	
	public GetOSFunctionTool setTimeout(long timeout) {
		this.timeout = timeout;
		return this;
	}
    
	@Tool(name="getOS",description = "获取OS及OS版本信息")
    public Map getOS(){
        String os = System.getProperty("os.name");
        Map result = new java.util.HashMap();
        result.put("os",os);      
        return result;
    } 
}
