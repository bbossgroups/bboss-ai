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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * @author biaoping.yin
 * @Date 2026/3/31
 */
public class FeishuTokenHolder {
    private String token;
    private long expireTime;
    private RefreshTokenFunction refreshTokenFunction;
    private Thread refreshThread;
    private ReadWriteLock readWriteLock = new java.util.concurrent.locks.ReentrantReadWriteLock();
    private Lock readLock = readWriteLock.readLock();
    private Lock writeLock = readWriteLock.writeLock();
    private Logger logger = LoggerFactory.getLogger(FeishuTokenHolder.class);
    
    private boolean refreshFailed;
    private boolean stopped;
    
    public FeishuTokenHolder(RefreshTokenFunction refreshTokenFunction,long expireTime) {
        this.expireTime = expireTime;
        this.refreshTokenFunction = refreshTokenFunction;
        refreshToken(false);
        refreshThread = new Thread(() -> {
            while (true) {
                try {
                    if(stopped)
                        break;
                    Thread.sleep(expireTime);
                } catch (InterruptedException e) {
                    break;
                }
                if(stopped)
                    break;
                try {
                    refreshToken(false);
                }
                catch (Exception e){
                    logger.error("refreshToken error",e);
                }
            }
            
        });
        refreshThread.setDaemon(true);
        refreshThread.start();
    }
    
    private void refreshToken(boolean fromGetToken){
        writeLock.lock();
        try {
            if(fromGetToken && !refreshFailed){
                return;
            }
            token = refreshTokenFunction.refreshToken();
            if (refreshFailed) {                
                refreshFailed = false;
            }
        }
        catch (Exception e){
            logger.error("refreshToken error",e);
            refreshFailed = true;
        }
        finally {
            writeLock.unlock();
        }
    }
    public void destroy(){
        stopped = true;
        refreshThread.interrupt();
        try {
            refreshThread.join();
        } catch (InterruptedException e) {
        }
    }
    
    public String getToken() {
        // 如果刷新失败，则再次刷新，避免使用无效的token
        if(refreshFailed){
            refreshToken(true);
        }
        readLock.lock();
        try {
            
            return token;
        }
        finally {
            readLock.unlock();
        }
    }
     
    
    
    
}
