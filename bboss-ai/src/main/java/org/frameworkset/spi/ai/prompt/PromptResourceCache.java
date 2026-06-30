package org.frameworkset.spi.ai.prompt;
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

import com.frameworkset.util.FileUtil;
import org.frameworkset.spi.ai.model.AIRuntimeException;
import org.frameworkset.spi.ai.util.ClasspathResourceReader;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author biaoping.yin
 * @Date 2026/6/28
 */
public class PromptResourceCache {
    private static final PromptResourceCache instance = new PromptResourceCache();
    private Map<String, String> fileCache = new HashMap<String, String>();

    private Map<String, String> resourceCache  = new HashMap<String, String>();
    private Map<String, String> urlCache  = new HashMap<String, String>();
    
    public static PromptResourceCache getInstance() {
        return instance;
    }
    public String cacheFileContent(String file,String charset){
        String value = null;
        try {
            value = fileCache.get(file);
            if(value == null){
                synchronized (fileCache){
                    value = fileCache.get(file);
                    if(value == null){
                        value = FileUtil.getFileContent(file,charset);
                        fileCache.put(file, value);
                    }
                }
            }
            return value;
           
        } catch (IOException e) {
            throw new AIRuntimeException(" file:" + file + " charset:" + charset   , e);
        }
       
    }


    public String cacheClasspathResource(String resource,String charset){
        String value = null;
        try {
            value = resourceCache.get(resource);
            if(value == null){
                synchronized (resourceCache){
                    value = resourceCache.get(resource);
                    if(value == null){
                        value = ClasspathResourceReader.readClasspathResource(resource, charset);
                        resourceCache.put(resource, value);
                    }
                }
            }
            return value;

        } catch (IOException e) {
            throw new AIRuntimeException(" resource:" + resource + " charset:" + charset   , e);
        }

    }

    public String cacheUrlResource(String url,String charset){
        String value = null;
        try {
            value = urlCache.get(url);
            if(value == null){
                synchronized (urlCache){
                    value = urlCache.get(url);
                    if(value == null){
                        value = ClasspathResourceReader.readURL(url, charset);
                        urlCache.put(url, value);
                    }
                }
            }
            return value;

        } catch (IOException e) {
            throw new AIRuntimeException(" url:" + url + " charset:" + charset   , e);
        }

    }
}
