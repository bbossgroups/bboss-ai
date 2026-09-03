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

import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpException;
import org.frameworkset.spi.ai.model.annotation.Tool;
import org.frameworkset.spi.ai.model.annotation.ToolParam;
import org.frameworkset.spi.ai.tools.model.WebToolResult;
import org.frameworkset.spi.remote.http.BaseURLResponseHandler;
import org.frameworkset.spi.remote.http.HttpRequestProxy;
import org.frameworkset.spi.remote.http.ResponseUtil;

import java.io.IOException;

import static org.frameworkset.spi.ai.tools.HttpWebTools.headers;

/**
 *
 * @author biaoping.yin
 * @Date 2026/8/28
 */
public class WebFetchTool {
	private  String httpWebfetchToolProxy = "http_webfetch_tool";
	@Tool(
			name = "web_fetch",
			readOnly = true,
			description =
					"Fetch content from an HTTP(S) URL and return a truncated text preview.")
	public String webFetch(
			@ToolParam(name = "url", description = "HTTP or HTTPS URL to fetch",required = true) String url,
			@ToolParam(
					name = "max_chars",
					description = "Max characters to return (default 20000)",
					required = false)
			Integer maxChars) {
		if (url == null || url.isEmpty()) {
			return "Error: url is required";
		}
		String trimmed = url.trim();
		if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
			return "Error: only http/https URLs are allowed";
		}
		int limit = maxChars != null && maxChars > 0 ? Math.min(maxChars, 100_000) : -1;
		try {
			
			WebToolResult<String> webToolResult = HttpRequestProxy.httpGet(httpWebfetchToolProxy, trimmed, headers,new BaseURLResponseHandler<WebToolResult<String>>() {
				/**
				 * Processes an {@link ClassicHttpResponse} and returns some value
				 * corresponding to that response.
				 *
				 * @param response The response to process
				 * @return A value determined by the response
				 * @throws IOException   in case of a problem or the connection was aborted
				 * @throws HttpException in case of an HTTP protocol violation.
				 */
				@Override
				public WebToolResult<String> handleResponse(ClassicHttpResponse response) throws HttpException, IOException {
					WebToolResult<String> webToolResult = new WebToolResult<String>();
					int status = response.getCode();
					String data = ResponseUtil.handleStringResponse(  url,response);
					webToolResult
							.setBody(data);
					webToolResult
							.setStateCode(status);
					return webToolResult;
				}
			});
			String body = webToolResult.getBody();
//                HttpRequest request =
//                        HttpRequest.newBuilder()
//                                .uri(URI.create(trimmed))
//                                .timeout(Duration.ofSeconds(30))
//                                .header("User-Agent", "AgentScope-Harness-WebFetch/1.0")
//                                .GET()
//                                .build();
//                HttpResponse<String> response =
//                        client.send(request, HttpResponse.BodyHandlers.ofString());
//                String body = response.body() == null ? "" : response.body();
			if (limit > 0 && body.length() > limit) {
				body = body.substring(0, limit) + "\n...[truncated]";
			}
			return "status=" + webToolResult.getStateCode() + "\n\n" + body;
		} catch (Exception e) {
			return "Error: web_fetch failed: " + e.getMessage();
		}
	}
}
