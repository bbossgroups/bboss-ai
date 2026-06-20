package org.frameworkset.spi.ai.tool;
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


import org.frameworkset.spi.ai.AIAgent;
import org.frameworkset.spi.ai.model.FunctionToolDefine;
import java.util.List;

/**
 * 工具检索器：根据用户输入 query，从全部工具中筛选出相关工具
 */
public interface ToolSearcher {
    /**
     * @param allTools 注册的全部工具
     * @param query    用户输入/提示词
     * @return 命中的工具列表，返回 null 或空列表表示不启用过滤（全部返回）
     */
    List<FunctionToolDefine> search(List<FunctionToolDefine> allTools, String query); 
}

