package org.frameworkset.spi.ai.util;
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
 * @Date 2026/6/27
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;

/**
 * 类路径资源读取工具（改进版）
 */
public class ClasspathResourceReader {

    /**
     * 从 classpath 读取资源文件，并以指定字符集返回文本内容
     *
     * @param resourcePath 资源路径（自动兼容前导斜杠）
     * @param charsetName  字符集名称（例如 "UTF-8"），为 null 则使用系统默认字符集
     * @return 资源文件的文本内容
     * @throws IOException 如果资源不存在或读取失败
     */
    public static String readClasspathResource(String resourcePath, String charsetName) throws IOException {
        if (resourcePath == null) {
            throw new IllegalArgumentException("resourcePath must not be null");
        }
        if(charsetName == null){
            charsetName = "UTF-8";
        }
        Charset charset = Charset.forName(charsetName);

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = ClasspathResourceReader.class.getClassLoader();
        }

        String normalizedPath = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;

        // ★ 关键点：字符集在这里指定（包装 InputStreamReader）
        try (InputStream is = classLoader.getResourceAsStream(normalizedPath);
             InputStreamReader isr = new InputStreamReader(is, charset);  // 在此绑定字符集
             BufferedReader reader = new BufferedReader(isr)) {

            if (is == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }

            // 使用 StringBuilder 逐行拼接（适合普通文本）
            StringBuilder sb = new StringBuilder();
            char[] buffer = new char[8192];
            int len;
            while ((len = reader.read(buffer)) != -1) {
                sb.append(buffer, 0, len);
            }
            return sb.toString();
        } catch (NullPointerException e) {
            // 如果 is 为 null，InputStreamReader 构造会抛出 NPE，这里转为明确的业务异常
            throw new IOException("Resource not found: " + resourcePath, e);
        }
    }

    /**
     * 从 URL 读取资源内容，并以指定字符集返回文本
     *
     * @param url     资源 URL 地址
     * @param charset 字符集名称（例如 "UTF-8"），为 null 则使用 UTF-8
     * @return URL 资源的文本内容
     * @throws IOException 如果 URL 无效或读取失败
     */
    public static String readURL(String url, String charset) throws IOException {
        if (url == null) {
            throw new IllegalArgumentException("url must not be null");
        }
        if (charset == null) {
            charset = "UTF-8";
        }
        Charset charsetObj = Charset.forName(charset);

        try (InputStream is = new URL(url).openStream();
             InputStreamReader isr = new InputStreamReader(is, charsetObj);
             BufferedReader reader = new BufferedReader(isr)) {

            StringBuilder sb = new StringBuilder();
            char[] buffer = new char[8192];
            int len;
            while ((len = reader.read(buffer)) != -1) {
                sb.append(buffer, 0, len);
            }
            return sb.toString();
        }
    }
}
