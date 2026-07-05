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

import org.frameworkset.spi.ai.model.FunctionToolDefine;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于关键词匹配的工具检索器
 * 匹配规则：工具名称 或 描述 中包含 query 中的任意关键词即命中
 */
public class KeywordToolSearcher implements ToolSearcher {
    /**
     * 关键词清单
     */
    private String[] keywords;

    public KeywordToolSearcher(String... keywords){
        this.keywords = keywords;
    }

    @Override
    public List<FunctionToolDefine> search(List<FunctionToolDefine> allTools, String query) {
        if (allTools == null || allTools.isEmpty() || keywords == null || keywords.length == 0) {
            return allTools;
        }
        String[] keywords = this.keywords;
        List<FunctionToolDefine> matched = new ArrayList<>();
        for (FunctionToolDefine tool : allTools) {
//            if (tool.getFunction() == null) continue;
            String name = tool.getFunction().getName();
            String desc = tool.getFunction().getDescription();
            for (String kw : keywords) {
                if (name.contains(kw) || desc.contains(kw)) {
                    matched.add(tool);
                    break;
                }
            }
        }
        return matched.isEmpty() ? allTools : matched;
    }
}
