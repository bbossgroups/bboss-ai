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
 * @author biaoping.yin
 * @Date 2026/5/7
 */
public class AIFlowConst {
    
    public static final int AIFLOW_VAR_SCOPE_FLOW = 1;
    public static final int AIFLOW_VAR_SCOPE_CONTAINER = 2;
    public static final int AIFLOW_VAR_SCOPE_NODE = 3;

    /**
     * 变量类型：文本类型，直接获取变量值即可，默认值
     */
    public static final String AIFLOW_VAR_TYPE_TEXT = "text";

    /**
     * 变量类型：资源类型，变了名称代表资源相对路径，变量内容从当前classpath下的资源文件读取
     */
    public static final String AIFLOW_VAR_TYPE_RESOURCE = "resource";

    /**
     * 变量类型：文件类型，变量名称代表一个文件路径，解析变量值时，会读取文件内容
     */
    public static final String AIFLOW_VAR_TYPE_FILE = "file";

    /**
     * 变量类型：URL类型，变量名称代表一个URL地址，解析变量值时，会读URL对应的资源内容
     */
    public static final String AIFLOW_VAR_TYPE_URL = "url";

}
