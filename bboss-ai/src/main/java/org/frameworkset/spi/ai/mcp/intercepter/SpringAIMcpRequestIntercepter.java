package org.frameworkset.spi.ai.mcp.intercepter;
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

import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.frameworkset.spi.remote.http.callback.ClientConfigurationHttpRequestInterceptor;

import java.io.IOException;

/**
 * 
 * spring ai mcp 服务兼容mcp请求拦截器，配置方式：
 * visualops.http.httpRequestInterceptors=org.frameworkset.spi.ai.SpringAIMcpRequestIntercepter
 * @author biaoping.yin
 * @Date 2026/7/31
 */
public class SpringAIMcpRequestIntercepter extends ClientConfigurationHttpRequestInterceptor {
	
	@Override
	public void process(HttpRequest httpRequest, EntityDetails entityDetails, HttpContext httpContext) throws HttpException, IOException {
		try {
			
			httpRequest.addHeader("Accept", "application/json, text/event-stream");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
	}
	
}
