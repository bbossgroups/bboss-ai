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

import com.fasterxml.jackson.databind.JsonNode;
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
 *
 * @author biaoping.yin
 * @Date 2026/8/28
 */
public class WebSearchTool {
	private  String httpWebSearchToolProxy = "http_websearch_tool";
	//        private static final String TAVILY_API = "https://api.tavily.com/search";
	private static final String TAVILY_API = "/search";
	
	
	@Tool(
			name = "web_search",
			readOnly = true,
			description =
					"Search the web for information. Requires TAVILY_API_KEY. Returns titles,"
							+ " URLs and snippets.")
	public String webSearch(
			@ToolParam(name = "query", description = "Search query",required = true) String query,
			@ToolParam(
					name = "max_results",
					description = "Maximum results (default 5)",
					required = false)
			Integer maxResults) {
		if (query == null || query.isEmpty()) {
			return "Error: query is required";
		}
//		String apiKey = System.getenv("TAVILY_API_KEY");
//		if (apiKey == null || apiKey.isEmpty()) {
//			return "Error: TAVILY_API_KEY is not set. Configure the key via Environment vault"
//					+ " credentials or process env to enable web_search.";
//		}
		int limit = maxResults != null && maxResults > 0 ? Math.min(maxResults, 10) : 5;
		Map params = new LinkedHashMap();
		params.put("query", query);
		params.put("max_results", limit);
		params.put("search_depth", "basic");
//            String body =
//                    "{\"query\":"
//                            + mapper.valueToTree(query)
//                            + ",\"max_results\":"
//                            + limit
//                            + ",\"search_depth\":\"basic\"}";
		try {
			WebToolResult<Map> webToolResult = HttpRequestProxy.sendJsonBody(httpWebSearchToolProxy, JsonUtil.object2json(params), TAVILY_API, HttpWebTools.headers,new BaseURLResponseHandler<WebToolResult<Map>>() {
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
				public WebToolResult<Map> handleResponse(ClassicHttpResponse response) throws HttpException, IOException {
					WebToolResult<Map> webToolResult = new WebToolResult<Map>();
					int status = response.getCode();
					Map data = ResponseUtil.handleResponse(  url,response,Map.class);
					webToolResult
							.setBody(data);
					webToolResult
							.setStateCode(status);
					return webToolResult;
				}
			});
//                HttpRequest request =
//                        HttpRequest.newBuilder()
//                                .uri(URI.create(TAVILY_API))
//                                .timeout(Duration.ofSeconds(30))
//                                .header("Content-Type", "application/json")
//                                .header("Authorization", "Bearer " + apiKey)
//                                .POST(HttpRequest.BodyPublishers.ofString(body))
//                                .build();
//                HttpResponse<String> response =
//                        client.send(request, HttpResponse.BodyHandlers.ofString());
			if (webToolResult.getStateCode() != 200) {
				return "Error: Tavily API returned " + webToolResult.getStateCode();
			}
			List<Map> results = (List<Map>)webToolResult.getBody().get("results");
			if (results == null || results.isEmpty()) {
				return "No results.";
			}
			else {
				StringBuilder sb = new StringBuilder();
				int i = 1;
				for (Map result : results) {
					sb.append(i++)
							.append(". ")
							.append(result.get("title"))
							.append("\n   ")
							.append(result.get("url"))
							.append("\n   ")
							.append(result.get("content"))
							.append("\n\n");
				}
				return sb.toString().trim();
			}

//                JsonNode root = mapper.readTree(response.body());
//                JsonNode results = root.path("results");
//                if (!results.isArray() || results.isEmpty()) {
//                    return "No results.";
//                }
//                StringBuilder sb = new StringBuilder();
//                int i = 1;
//                for (JsonNode r : results) {
//                    sb.append(i++)
//                            .append(". ")
//                            .append(r.path("title").asText(""))
//                            .append("\n   ")
//                            .append(r.path("url").asText(""))
//                            .append("\n   ")
//                            .append(r.path("content").asText(""))
//                            .append("\n\n");
//                }
//                return sb.toString().strip();
		} catch (HttpProxyRequestException e) {
			return "Error: web_search failed: " + e.getMessage()+ ",status:" + e.getHttpStatusCode();
		}catch ( Exception e) {
			return "Error: web_search failed: " + e.getMessage();
		}
	}
}
