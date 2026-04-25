package org.frameworkset.spi.reactor;
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
import reactor.core.publisher.FluxSink;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author biaoping.yin
 * @Date 2026/3/2
 */
public class DisposeEventHandler {
    private static Logger logger = LoggerFactory.getLogger(DisposeEventHandler.class);
	private Map<String,FluxSinkStatus> fluxSinkStatusIdx = new LinkedHashMap<>();
	private int seqNo;
//	private volatile boolean containFluxSinkStatus;
    private boolean disposed;
	public void dispose(){
        if(disposed ){
            return;
        }
       
        synchronized (lock) {
            if (disposed)
                return;
            if (fluxSinkStatusIdx != null) {
                for (Map.Entry<String, FluxSinkStatus> entry : fluxSinkStatusIdx.entrySet()) {
                    FluxSinkStatus fluxSinkStatus = entry.getValue();
                    fluxSinkStatus.dispose();
                    fluxSinkStatus.releaseResources();

                }
                fluxSinkStatusIdx.clear();
                fluxSinkStatusIdx = null;
            }
            disposed = true;
        }
	}
    private boolean registedDispose;
    public boolean onDispose(FluxSink fluxSink){
        if(disposed){
            return false;
        }
        if(registedDispose ){
            return false;
        }
        synchronized (lock) {
            if(registedDispose ){
                return false;
            }
            fluxSink.onDispose(() -> {
                // 当 sink 被处置时执行（包括正常完成、错误和取消）
                if (logger.isDebugEnabled()) {
                    logger.debug("Sink disposed");
                }
//					fluxSinkStatus_.dispose();
//					// 执行清理工作
//					fluxSinkStatus_.releaseResources();
                dispose();

            });
            registedDispose = true;
        }
        return true;
    }
//	public boolean containFluxSinkStatus(){
//        synchronized (lock) {
//            return containFluxSinkStatus;
//        }
//	}
    private Object lock = new Object();
	public void addFluxSinkStatus(FluxSinkStatus fluxSinkStatus){
        if(disposed)
            return;
        if(!registedDispose){
            return;
        }
        synchronized (lock){
            if(disposed)
                return;
            if(!registedDispose){
                return;
            }
			fluxSinkStatus.setSeqNo(String.valueOf(seqNo++));
			fluxSinkStatusIdx.put(fluxSinkStatus.getSeqNo(),fluxSinkStatus);
//			containFluxSinkStatus = true;
        }
	}
	public void removeFluxSinkStatus(String seqNo){
        synchronized (lock){
            if(fluxSinkStatusIdx != null) {
                fluxSinkStatusIdx.remove(seqNo);
            }
        }   
		
	}
	
	
}
