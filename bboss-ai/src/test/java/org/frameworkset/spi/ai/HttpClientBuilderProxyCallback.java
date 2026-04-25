package org.frameworkset.spi.ai;

import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.routing.DefaultProxyRoutePlanner;
import org.apache.hc.core5.http.HttpHost;
import org.frameworkset.spi.remote.http.ClientConfiguration;
import org.frameworkset.spi.remote.http.callback.HttpClientBuilderCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpClientBuilderProxyCallback implements HttpClientBuilderCallback {
    private static Logger logger = LoggerFactory.getLogger(HttpClientBuilderProxyCallback.class);
    
    public HttpClientBuilder customizeHttpClient(HttpClientBuilder builder, ClientConfiguration clientConfiguration) {
        logger.info("配置 HttpClient5 代理...");
        
        // 创建代理服务器地址
        HttpHost proxy = new HttpHost("http", "127.0.0.1", 7897);
        
        // 创建代理路由规划器
        DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
        
        // 设置路由规划器
        builder.setRoutePlanner(routePlanner);
        
        return builder;
    }
}
