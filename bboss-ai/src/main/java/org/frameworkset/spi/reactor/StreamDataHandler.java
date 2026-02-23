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
import org.frameworkset.spi.ai.adapter.AgentAdapter;
import org.frameworkset.spi.ai.model.ChatObject;
import org.frameworkset.util.concurrent.BooleanWrapperInf;
import reactor.core.publisher.FluxSink;

/**
 * @author biaoping.yin
 * @Date 2025/10/10
 */
public interface StreamDataHandler<T> {
    boolean isStream() ;

    void setStream(boolean stream) ;
    /**
     * 处理数据行,如果数据已经返回完毕，则返回true，指示关闭对话，否则返回false
     * @param line 数据行
     * @param sink 数据行处理结果
     * @param firstEventTag 是否是第一个事件标记，需要具体实现设置，如果为true，则表示当前数据行是第一个事件标记，否则不是第一个事件标记。
     *                      在接口方法实现中，在发送消息时，需检测是否为true，如果为true，需设置标记为false，同时将ServerEvent的first标记设置为true
     * @return
     */
    boolean handle(String line, FluxSink<T> sink, BooleanWrapperInf firstEventTag);
    /**
     * 处理异常，如果数据已经返回完毕，则返回true，指示关闭对话，否则返回false
     * @param throwable 异常
     * @param sink 数据行处理结果
     * @param firstEventTag 是否是第一个事件标记，需要具体实现设置，如果为true，则表示当前数据行是第一个事件标记，否则不是第一个事件标记。
     *                      在接口方法实现中，在发送消息时，需检测是否为true，如果为true，需设置标记为false，同时将ServerEvent的first标记设置为true
     * @return
     */
    boolean handleException(Object requestBody,Throwable throwable, FluxSink<T> sink, BooleanWrapperInf firstEventTag);
    void setHttpUriRequestBase(HttpUriRequestBase httpUriRequestBase);
    HttpUriRequestBase getHttpUriRequestBase();
    AgentAdapter getAgentAdapter();
    
    ChatObject getChatObject();
    String getDoneData() ;
}
