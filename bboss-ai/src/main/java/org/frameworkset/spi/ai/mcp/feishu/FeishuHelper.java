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

import com.frameworkset.util.JsonUtil;
import com.frameworkset.util.SimpleStringUtil;
import org.frameworkset.spi.remote.http.HttpRequestProxy;
import org.frameworkset.util.ResourceStartResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author biaoping.yin
 * @Date 2026/3/23
 */
public class FeishuHelper {
    private static final Logger logger = LoggerFactory.getLogger(FeishuHelper.class);

    private String appId;
    private String appSecret;
    private String feishuDatasource;
    private BaseFeishuConfigInf baseFeishuConfig;
    private FeishuTokenHolder feishuTokenHolder;
    private ResourceStartResult resourceStartResult;

    public FeishuHelper(BaseFeishuConfigInf baseFeishuConfig){
        this.feishuDatasource = baseFeishuConfig.getFeishuDataSource();
        this.appId = baseFeishuConfig.getFeishuAppId();
        this.appSecret = baseFeishuConfig.getFeishAppSecret();         
        this.baseFeishuConfig = baseFeishuConfig;

    }
    
    public void init(){
        if(baseFeishuConfig.getHttpConfigs() != null){
            resourceStartResult = HttpRequestProxy.startHttpPools(baseFeishuConfig.getHttpConfigs());
        }
    }
    
    public void destroy(){
        if(feishuTokenHolder != null){
            feishuTokenHolder.destroy();
        }
        if(resourceStartResult != null) {
            HttpRequestProxy.stopHttpClients(resourceStartResult);
        }
    }
    
    public void buildMcpToolsHeader(Map headers,String tools){
        headers.put("X-Lark-MCP-Allowed-Tools",tools);
        headers.put("X-Lark-MCP-TAT",this.getTenantAccessToken());
    }

 

 
    public Map listFields(String accessToken,String listFieldsUrl){
//		String listFields = "/open-apis/bitable/v1/apps/F1JHb8MOjaZAYPsvhNkcLS3Kn7b/tables/tbl1s9brDeonLhvu/fields";
        Map headers = buildHeaders(accessToken);

        Map listFieldsResult = HttpRequestProxy.httpGetforObject(feishuDatasource,listFieldsUrl,headers,Map.class);
        return listFieldsResult;
    }
    
    private Map buildHeaders(String accessToken){
        if(accessToken == null) {
            accessToken = getTenantAccessToken();
        }
        Map headers = new LinkedHashMap();
        headers.put("Authorization","Bearer "+accessToken);
        return headers;
    }

    public Map searchData(String accessToken, String searchUrl,String requestBody){
        Map headers = buildHeaders(accessToken);
         

        Map listFieldsResult = HttpRequestProxy.sendJsonBody(feishuDatasource,requestBody,searchUrl,headers,Map.class);

//        logger.info("推送多维表格结果:{}", JsonUtil.object2json(message_));

        //是否成功 非0为不成功
        if(listFieldsResult != null && Integer.valueOf(0).equals(listFieldsResult.get("code"))){
        }else{
            throw new FeishuException("查询数据失败："+ JsonUtil.object2json(listFieldsResult));
        }
        return listFieldsResult;
    }

    public Map deleteData( String accessToken,String deleteUrl,Map requestBody){
        Map headers = buildHeaders(accessToken);

        Map deleteResult = HttpRequestProxy.sendJsonBody(feishuDatasource,requestBody,deleteUrl,headers,Map.class);
        return deleteResult;
    }
    
    public String getRecordIdByField(String accessToken,String fieldName, Object value){
        StringBuilder requestBody = new StringBuilder();
        requestBody.append("{")
                .append("\"automatic_fields\": false,")
                .append("\"field_names\": [")
                .append("],")
                .append("\"filter\": {")
                .append("\"conditions\": [")
                .append("{")
                .append("\"field_name\": \"").append(fieldName).append("\",")
                .append("\"operator\": \"is\",")
                .append("\"value\": [");
        if(value instanceof String) {
            requestBody.append("\"").append(value).append("\"");
        }
        else{
            requestBody.append(value);
        }
        requestBody.append("]")
                .append("}		")
                .append("],")
                .append("\"conjunction\": \"and\"")
                .append("},")
                .append("\"sort\": [")
                .append("],")
                .append("\"view_id\": \"").append(baseFeishuConfig.getFeishuViewId()).append("\"")
                .append("}");

        //用INDICATOR_ID替换变量${INDICATOR_ID}
        

        Map datas = searchData(accessToken, baseFeishuConfig.getSearchUrl(),  requestBody.toString());
        String recordId = null;
        if(datas != null ){
            Map data = (Map)datas.get("data");
            if(data != null) {
                List<Map> items = (List<Map>) data.get("items");
                if (items != null && items.size() > 0) {
                    for (Map item : items) {
                        recordId = (String) item.get("record_id");
                    }
                }
            }

        }
        return recordId;
    }

    public List<String> getRecordIdsByField(String accessToken, String fieldName, Object value){
        StringBuilder requestBody = new StringBuilder();
        requestBody.append("{")
                .append("\"automatic_fields\": false,")
                .append("\"field_names\": [")
                .append("],")
                .append("\"filter\": {")
                .append("\"conditions\": [")
                .append("{")
                .append("\"field_name\": \"").append(fieldName).append("\",")
                .append("\"operator\": \"is\",")
                .append("\"value\": [");
        if(value instanceof String) {
            requestBody.append("\"").append(value).append("\"");
        }
        else{
            requestBody.append(value);
        }
        requestBody.append("]")
                .append("}		")
                .append("],")
                .append("\"conjunction\": \"and\"")
                .append("},")
                .append("\"sort\": [")
                .append("],")
                .append("\"view_id\": \"").append(baseFeishuConfig.getFeishuViewId()).append("\"")
                .append("}");

        //用INDICATOR_ID替换变量${INDICATOR_ID}


        Map datas = searchData(accessToken,baseFeishuConfig.getSearchUrl(),  requestBody.toString());
        List<String> recordIds = null;
        if(datas != null ){
            Map data = (Map)datas.get("data");
            if(data != null) {
                List<Map> items = (List<Map>) data.get("items");
                if (items != null && items.size() > 0) {
                    recordIds = new ArrayList<>();
                    for (Map item : items) {
                        recordIds.add((String) item.get("record_id"));
                    }
                }
            }

        }
        return recordIds;
    }

    /**
     * 根据条件获取id集合
     * @return
     */
    public List<String> getRecordIdsByField(String accessToken,List<FieldName2ndValues> fieldName2ndValues){
        StringBuilder requestBody = new StringBuilder();
        requestBody.append("{")
                .append("\"automatic_fields\": false,")
                .append("\"field_names\": [")
                .append("],")
                .append("\"filter\": {")
                .append("\"conditions\": [");
        for(int i = 0; i < fieldName2ndValues.size(); i++) {
            FieldName2ndValues _fieldName2ndValues = fieldName2ndValues.get(i);
            String fieldName = _fieldName2ndValues.getFieldName();
            Object[] values = _fieldName2ndValues.getValues();
            if(i > 0){
                requestBody.append(",");
            }
            
            requestBody.append("{")
                    .append("\"field_name\": \"").append(fieldName).append("\",")
                    .append("\"operator\": \"").append(_fieldName2ndValues.getOperator()).append("\",")
                    .append("\"value\": [");
            for(int j = 0; j < values.length; j++) {
                Object value = values[j];
                if(j > 0){                    
                    requestBody.append(",");
                }
                if (value instanceof String) {
                    requestBody.append("\"").append(value).append("\"");
                } else {
                    requestBody.append(value);
                }
            }
            
            requestBody.append("]")
                    .append("}		");
        }
        requestBody.append("],")
                .append("\"conjunction\": \"and\"")
                .append("},")
                .append("\"sort\": [")
                .append("],")
                .append("\"view_id\": \"").append(baseFeishuConfig.getFeishuViewId()).append("\"")
                .append("}");

        //用INDICATOR_ID替换变量${INDICATOR_ID}


        Map datas = searchData(  accessToken, baseFeishuConfig.getSearchUrl(),  requestBody.toString());
        List<String> recordIds = null;
        if(datas != null ){
            Map data = (Map)datas.get("data");
            if(data != null) {
                List<Map> items = (List<Map>) data.get("items");
                if (items != null && items.size() > 0) {
                    recordIds = new ArrayList<>();
                    for (Map item : items) {
                        recordIds.add((String) item.get("record_id"));
                    }
                }
            }

        }
        return recordIds;
    }

    private ReentrantLock reentrantLock = new ReentrantLock();
    
    private void initFeishuTokenHolder(){
        if(feishuTokenHolder != null){
            return;
        }

        reentrantLock.lock();
        try{
            if(feishuTokenHolder != null){
                return ;
            }

            FeishuTokenHolder feishuTokenHolder = new FeishuTokenHolder(new RefreshTokenFunction() {
                @Override
                public String refreshToken() {
                    return requestTenantAccessToken();
                }
            }, baseFeishuConfig.getAccessTokenExpireTime());
            this.feishuTokenHolder = feishuTokenHolder;

        }catch(Exception e){
            throw new FeishuException("get tenant access token failed:",e);
        }
        finally{
            reentrantLock.unlock();
        }
    }
    /**
     * 获取飞书租户访问令牌
     * @return
     */
    public String getTenantAccessToken( ){
        initFeishuTokenHolder();
        return feishuTokenHolder.getToken();
        
    }
    
    public String requestTenantAccessToken(){
        if(SimpleStringUtil.isEmpty(appId) || SimpleStringUtil.isEmpty(appSecret)){
            throw new FeishuException("app id or app secret is empty:appId="+appId+",appSecret="+appSecret);
        }
        String url = "/open-apis/auth/v3/tenant_access_token/internal";
        Map<String,Object> params = new LinkedHashMap<>();
        params.put("app_id",appId);
        params.put("app_secret",appSecret);
        Map tenantAccessToken = null;
        int times = 10;
        do {
            try {
                tenantAccessToken = HttpRequestProxy.sendJsonBody(feishuDatasource,params,url,Map.class);
                break;
            } catch (Exception e) {
                times--;
                if(times < 0){
                    throw new FeishuException("get tenant access token failed:",e);
                }
//						throw new DataImportException("get tenant access token failed:", e);
            }
        }while(true);
        return (String)tenantAccessToken.get("tenant_access_token");
    }


    

    /**
     * String url = "/open-apis/bitable/v1/apps/N0tMboDHOaSWAwsXh0ucIoARnnc/tables/tblCzBSEvUXKYMTI/records";
     * 推送发送结果到飞书
     */
    public Map sendRequest( String accessToken, Map record, String url){
//        logger.info(accessToken);
//        String url = "/open-apis/bitable/v1/apps/N0tMboDHOaSWAwsXh0ucIoARnnc/tables/tblCzBSEvUXKYMTI/records";
        //多维表格地址
        //https://asiainfo.feishu.cn/base/N0tMboDHOaSWAwsXh0ucIoARnnc?table=tblCzBSEvUXKYMTI&view=vewFoeaJxt

        Map headers = buildHeaders(accessToken);
        Map message_ = HttpRequestProxy.sendJsonBody(feishuDatasource,record,url,headers,Map.class);
//        logger.info("推送多维表格结果:{}", JsonUtil.object2json(message_));

        //是否成功 非0为不成功
        if(message_ != null && Integer.valueOf(0).equals(message_.get("code"))){
//            logger.info("推送多维表格成功");
        }else{
//            logger.info("推送多维表格结果:{}", JsonUtil.object2json(message_));
//            logger.error("推送多维表格失败，问题数据:{}", JsonUtil.object2json(record));
            StringBuilder builder = new StringBuilder();
            builder.append("推送多维表格结果:{}").append(JsonUtil.object2json(message_)).append("\n")
                    .append("推送多维表格失败，问题数据:").append(JsonUtil.object2json(record));
            throw new FeishuException( builder.toString());
        }
        return message_;
    }
}
