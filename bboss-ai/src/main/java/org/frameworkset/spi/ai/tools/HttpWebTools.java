/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.frameworkset.spi.ai.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.frameworkset.util.JsonUtil;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpException;
import org.frameworkset.spi.ai.model.annotation.Tool;
import org.frameworkset.spi.ai.model.annotation.ToolParam;
import org.frameworkset.spi.ai.tools.model.WebToolResult;
import org.frameworkset.spi.remote.http.BaseURLResponseHandler;
import org.frameworkset.spi.remote.http.HttpRequestProxy;
import org.frameworkset.spi.remote.http.ResponseUtil;
import org.frameworkset.spi.remote.http.proxy.HttpProxyRequestException;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builtin web tools ({@code web_fetch}, {@code web_search}) for Managed Agents / Harness.
 *
 * <p>{@code web_search} uses the Tavily API when {@code TAVILY_API_KEY} (or vault-injected env) is
 * present; otherwise returns a clear configuration error.
 */
public final class HttpWebTools {
	private  String httpWebfetchToolProxy = "http_webfetch_tool";
	private  String httpWebSearchToolProxy = "http_websearch_tool";
	public final static Map<String, String> headers = new LinkedHashMap();
	static {
		headers.put("User-Agent", "bboss-Harness-WebFetch/1.0");
	}

    public HttpWebTools() {}

}
