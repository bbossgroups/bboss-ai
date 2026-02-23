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
        ZHIPU(AI_MODEL_TYPE_ZHIPU,"智谱");
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
