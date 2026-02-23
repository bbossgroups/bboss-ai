package org.frameworkset.spi.reactor;
/**
 * Copyright 2025 bboss
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

import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ClassicHttpResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author biaoping.yin
 * @Date 2025/10/19
 */
public class FluxSinkStatus {
    private InputStream inputStream = null;
    private InputStreamReader inputStreamReader = null;
    private BufferedReader reader = null;
    private HttpUriRequestBase httpUriRequestBase;
    private ClassicHttpResponse response;
    private AtomicBoolean isCancelled = new AtomicBoolean(false);
    private AtomicBoolean isDispose = new AtomicBoolean(false);
    
    private AtomicBoolean releaseResources = new AtomicBoolean(false);
    
    public FluxSinkStatus(ClassicHttpResponse response,HttpUriRequestBase httpUriRequestBase) throws IOException {
        this.inputStream = response.getEntity().getContent();
        this.inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        this.reader = new BufferedReader(inputStreamReader);
        this.response = response;
        this.httpUriRequestBase = httpUriRequestBase;
    }
    
    public String readLine() throws IOException {
        return reader.readLine();
    }
    
    public void releaseResources(){
        if(releaseResources.get()){
            return;
        }
        else {
            synchronized (releaseResources) {
                if(releaseResources.get()){
                    return;
                }
                releaseResources.set(true);
            }
        }
//        Schedulers.boundedElastic().dispose();
        if(httpUriRequestBase !=  null){
            
            try {
                httpUriRequestBase.abort();
            } catch (Exception e) {
//                    throw new RuntimeException(e);
            }
        }
        if(inputStream != null){

            try {
                inputStream.close();
            } catch (Exception e) {
//                    throw new RuntimeException(e);
            }
        }
        if(reader != null){
            try {
                reader.close();
            } catch (Exception e) {
            }
        }
        if(inputStreamReader != null){
            try {
                inputStreamReader.close();
            } catch (Exception e) {
            }
        }
         

        
   
        try {
            response.close();
        } catch (Exception e) {
//                    throw new RuntimeException(e);
        }
        
        
    }
    public void cancel(){
        isCancelled.set(true);
    }
    public void dispose(){
        isDispose.set(true);
    }
    public boolean isCancelled(){
        return isCancelled.get();
    }
    public boolean isDispose(){
        return isDispose.get();
    }

}
