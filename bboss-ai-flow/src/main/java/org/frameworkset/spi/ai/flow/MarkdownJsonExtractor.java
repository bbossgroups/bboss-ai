package org.frameworkset.spi.ai.flow;
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
 * @Date 2026/4/18
 */
import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.ast.IndentedCodeBlock;
import com.vladsch.flexmark.ast.Paragraph;
import com.vladsch.flexmark.ast.Text;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownJsonExtractor {

    private static final Pattern INLINE_JSON_PATTERN = Pattern.compile(
            "\\{(?:[^{}]|\\{(?:[^{}]|\\{[^{}]*\\})*\\})*\\}|" +
                    "\\[(?:[^\\[\\]]|\\[(?:[^\\[\\]]|\\[[^\\[\\]]*\\])*\\])*\\]"
    );

    private final Parser parser;

    public MarkdownJsonExtractor() {
        MutableDataSet options = new MutableDataSet();
        this.parser = Parser.builder(options).build();
    }

    /**
     * 从 Markdown 中提取所有 JSON 字符串
     */
    public List<String> extractAll(String markdown) {
        List<String> jsonList = new ArrayList<>();
        Node document = parser.parse(markdown);
        extractFromNode(document, jsonList);
        return jsonList;
    }

    private void extractFromNode(Node node, List<String> jsonList) {
        // 1. 提取代码块中的 JSON
        if (node instanceof FencedCodeBlock) {
            FencedCodeBlock codeBlock = (FencedCodeBlock) node;
            String info = codeBlock.getInfo().toString();
            String content = codeBlock.getContentChars().toString();

            if (isJsonType(info) && isValidJson(content)) {
                jsonList.add(content.trim());
            }
        }
        // 2. 提取缩进代码块
        else if (node instanceof IndentedCodeBlock) {
            String content = ((IndentedCodeBlock) node).getContentChars().toString();
            if (looksLikeJson(content) && isValidJson(content)) {
                jsonList.add(content.trim());
            }
        }
        // 3. 提取行内 JSON
        else if (node instanceof Paragraph) {
            String text = getTextContent(node);
            Matcher matcher = INLINE_JSON_PATTERN.matcher(text);
            while (matcher.find()) {
                String candidate = matcher.group();
                if (isValidJson(candidate)) {
                    jsonList.add(candidate);
                }
            }
        }

        // 递归子节点
        Node child = node.getFirstChild();
        while (child != null) {
            extractFromNode(child, jsonList);
            child = child.getNext();
        }
    }

    private boolean isJsonType(String info) {
        if (info == null || info.isEmpty()) return false;
        String lower = info.toLowerCase();
        return lower.contains("json") || lower.contains("jsonc");
    }

    private boolean looksLikeJson(String content) {
        String trimmed = content.trim();
        return (trimmed.startsWith("{") && trimmed.endsWith("}")) ||
                (trimmed.startsWith("[") && trimmed.endsWith("]"));
    }

    private boolean isValidJson(String str) {
        try {
            // 使用 Gson 验证
//            com.google.gson.JsonParser.parseString(str);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String getTextContent(Node node) {
        StringBuilder sb = new StringBuilder();
        Node child = node.getFirstChild();
        while (child != null) {
            if (child instanceof Text) {
                sb.append(((Text) child).getChars());
            }
            child = child.getNext();
        }
        return sb.toString();
    }

//    // ========== 使用示例 ==========
//    public static void main(String[] args) {
//        String markdown = """
//            # 配置文档
//            
//            基础配置：
//            ```json
//            {
//                "appName": "MyApp",
//                "version": "1.0.0"
//            }
//            ```
//            
//            行内配置示例：{"debug": true, "timeout": 30}
//            
//            数组配置：
//            ```json
//            ["item1", "item2", "item3"]
//            ```
//            """;
//
//        MarkdownJsonExtractor extractor = new MarkdownJsonExtractor();
//        List<String> jsonList = extractor.extractAll(markdown);
//
//        System.out.println("共提取到 " + jsonList.size() + " 个 JSON：");
//        for (int i = 0; i < jsonList.size(); i++) {
//            System.out.println("\n--- JSON " + (i + 1) + " ---");
//            System.out.println(jsonList.get(i));
//        }
//    }
}