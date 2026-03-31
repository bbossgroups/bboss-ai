package org.frameworkset.spi.ai.mcp.feishu;
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
 * @author biaoping.yin
 * @Date 2026/3/23
 */
public interface BaseFeishuConfigInf<T extends BaseFeishuConfigInf>   {

    long getAccessTokenExpireTime();
    T setAccessTokenExpireTime(long accessTokenExpireTime);
    default String getMcpTools(){
        return null;
    } 

    default T setMcpTools(String mcpTools){
        return (T)this;
    }


    Map<String, Object> getHttpConfigs() ;

    String getSearchUrl();


    String getUserIdType() ;

    T setUserIdType(String userIdType) ;
    default void build() {
        
    }
    
    void initFeishHelper();
    
    void destroy();

    FeishuHelper getFeishuHelper() ;
 
 
     

    T addHttpConfig(String property, Object value);
    

    String getFeishuDataSource() ;

    T setFeishuDataSource(String feishuDataSource);

    String getFeishuTableId() ;

    T setFeishuTableId(String feishuTableId);

    String getFeishuTableAppToken() ;

    T setFeishuTableAppToken(String feishuTableAppToken);

    String getFeishuAppId();

    T setFeishuAppId(String feishuAppId) ;

    String getFeishAppSecret() ;

    T setFeishAppSecret(String feishAppSecret) ;

    String getAccessTokenKey() ;

    T setAccessTokenKey(String accessTokenKey) ;

    String getFeishuViewId() ;

    T setFeishuViewId(String feishuViewId) ;

    String getRecordIdFieldName() ;

    T setRecordIdFieldName(String recordIdFieldName) ;
}
