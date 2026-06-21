package org.frameworkset.spi.ai.model;
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

/**
 * 模型类型常量
 * @author biaoping.yin
 * @Date 2026/1/4
 */
public class AIConstants {
    public static final String AI_MODEL_TYPE_QWEN = "qwen";
    public static final String AI_MODEL_TYPE_DOUBAO = "doubao";
    public static final String AI_MODEL_TYPE_DEEPSEEK = "deepseek";
    public static final String AI_MODEL_TYPE_KIMI = "kimi";
    public static final String AI_MODEL_TYPE_NONE = "none";
    public static final String AI_MODEL_TYPE_BAIDU = "baidu";
    public static final String AI_MODEL_TYPE_OPENAI = "openai";
    public static final String AI_MODEL_TYPE_SILICONFLOW = "siliconflow";
    public static final String AI_MODEL_TYPE_JIUTIAN = "jiutian";
    public static final String AI_MODEL_TYPE_ZHIPU = "zhipu";
    public static final String AI_MODEL_TYPE_MINIMAX = "minimax";    
    public static final String AI_MODEL_TYPE_HUNYUAN = "hunyuan";
    public static final String AI_MODEL_TYPE_XINFERENCE = "xinference";

    public static final String AI_MODEL_TYPE_QWEN_URL = "https://dashscope.aliyuncs.com";
    public static final String AI_MODEL_TYPE_DOUBAO_URL = "https://ark.cn-beijing.volces.com";
    public static final String AI_MODEL_TYPE_DEEPSEEK_URL = "https://api.deepseek.com";
    public static final String AI_MODEL_TYPE_KIMI_URL = "https://api.moonshot.cn";
    public static final String AI_MODEL_TYPE_SILICONFLOW_URL = "https://api.siliconflow.cn";
    public static final String AI_MODEL_TYPE_JIUTIAN_URL = "https://jiutian.30086.cn";
    public static final String AI_MODEL_TYPE_ZHIPU_URL = "https://open.bigmodel.cn";
    public static final String AI_MODEL_TYPE_MINIMAX_URL = "https://api.minimaxi.com";
    public static final String AI_MODEL_TYPE_HUNYUAN_URL = "https://api.hunyuan.cloud.tencent.com";
    public static final String AI_MODEL_TYPE_OPENAI_URL = "https://api.openai.com";
    
    

    /**
     * 根据maas平台地址，返回对应平台适配器类型
     * @param apiurl
     * @return
     */
    public static String getModelTypeByUrl(String apiurl){
        if(apiurl == null)
            return null;
        if(apiurl.startsWith(AI_MODEL_TYPE_QWEN_URL))
            return AI_MODEL_TYPE_QWEN;
        else if(apiurl.startsWith(AI_MODEL_TYPE_DOUBAO_URL))
            return AI_MODEL_TYPE_DOUBAO;
        else if(apiurl.startsWith(AI_MODEL_TYPE_DEEPSEEK_URL))
            return AI_MODEL_TYPE_DEEPSEEK;
        else if(apiurl.startsWith(AI_MODEL_TYPE_KIMI_URL))
            return AI_MODEL_TYPE_KIMI;
        else if(apiurl.startsWith(AI_MODEL_TYPE_SILICONFLOW_URL))
            return AI_MODEL_TYPE_SILICONFLOW;
        else if(apiurl.startsWith(AI_MODEL_TYPE_JIUTIAN_URL))
            return AI_MODEL_TYPE_JIUTIAN;
        else if(apiurl.startsWith(AI_MODEL_TYPE_ZHIPU_URL))
            return AI_MODEL_TYPE_ZHIPU;
        else if(apiurl.startsWith(AI_MODEL_TYPE_MINIMAX_URL))
            return AI_MODEL_TYPE_MINIMAX;
        else if(apiurl.startsWith(AI_MODEL_TYPE_HUNYUAN_URL))
            return AI_MODEL_TYPE_HUNYUAN;
        else if(apiurl.startsWith(AI_MODEL_TYPE_OPENAI_URL))
            return AI_MODEL_TYPE_OPENAI;
        else
            return null;
    }

    public enum ModelType{
        QWEN(AI_MODEL_TYPE_QWEN,"通义千问"),
        DOUBAO(AI_MODEL_TYPE_DOUBAO,"字节火山引擎"),
        DEEPSEEK(AI_MODEL_TYPE_DEEPSEEK,"深度思索"),
        KIMI(AI_MODEL_TYPE_KIMI,"月之暗面"),
        NONE(AI_MODEL_TYPE_NONE,"通用"),
        BAIDU(AI_MODEL_TYPE_BAIDU,"百度"),
        OPENAI(AI_MODEL_TYPE_OPENAI,"OpenAI"),
        SILICONFLOW(AI_MODEL_TYPE_SILICONFLOW,"硅基流程"),
        JIUTIAN(AI_MODEL_TYPE_JIUTIAN,"九天平台")        ,
        ZHIPU(AI_MODEL_TYPE_ZHIPU,"智谱")    ,
        MINIMAX(AI_MODEL_TYPE_MINIMAX,"Minimax"),
        HUNYUAN(AI_MODEL_TYPE_HUNYUAN,"腾讯混元");
        private String type;
        private String name;
        ModelType(String type,String name){
            this.type = type;
            this.name = name;
        }

        public String getName() {
            return name;
        }
        public String getType() {
            return type;
        }
    }

    
    
    public static final String AI_CHAT_REQUEST_BODY_JSON = "bodyJson";


    public static final String AI_CHAT_REQUEST_POST_FORM = "postForm";

    /**
     * file: 下载到本地目录
     * storeImageType = file
     * base64: 下载为base64编码
     * #storeImageType = base64  
     * url: 不下载，不适用于九天图片生成模型
     * #storeImageType = url
     */
    public static final String STORETYPE_BASE64 = "base64";
    /**
     * file: 下载到本地目录
     * storeImageType = file
     * base64: 下载为base64编码
     * #storeImageType = base64  
     * url: 不下载，不适用于九天图片生成模型
     * #storeImageType = url
     */
    public static final String STORETYPE_FILE = "file";
    /**
     * file: 下载到本地目录
     * storeImageType = file
     * base64: 下载为base64编码
     * #storeImageType = base64  
     * url: 不下载，不适用于九天图片生成模型
     * #storeImageType = url
     */
    public static final String STORETYPE_URL = "url";

}
