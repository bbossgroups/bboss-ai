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

import com.frameworkset.util.SimpleStringUtil;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ParseException;
import org.frameworkset.spi.ai.AIAgent;
import org.frameworkset.spi.ai.adapter.AgentAdapter;
import org.frameworkset.spi.ai.adapter.AgentAdapterFactory;
import org.frameworkset.spi.ai.callback.ChatContext;
import org.frameworkset.spi.ai.material.ReponseStoreFilePathFunction;
import org.frameworkset.spi.ai.material.StoreFilePathFunction;
import org.frameworkset.spi.ai.model.*;
import org.frameworkset.spi.reactor.*;
import org.frameworkset.spi.remote.http.*;
import org.frameworkset.util.RetryCallback;
import org.frameworkset.util.RetryUtil;
import org.frameworkset.util.concurrent.BooleanWrapperInf;
import org.frameworkset.util.concurrent.NoSynBooleanWrapper;
import org.slf4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI智能体工具类
 * @author biaoping.yin
 * @Date 2026/1/11
 */
public class AIAgentUtil {
    private static Logger logger = org.slf4j.LoggerFactory.getLogger(AIAgentUtil.class);

    
    /**
     * 创建流式调用的Flux，使用默认数据源
     */
    public static Flux<String> streamChatCompletion(Object message, AIAgent aiAgent) {
        return streamChatCompletion((String)null , message,   aiAgent);
    }
    public static Flux<String> streamChatCompletion(String poolName,Object chatMessage, AIAgent aiAgent ){
        return streamChatCompletion(  poolName,  chatMessage,   aiAgent,(ChatContext)null);
    }
    /**
     * 创建流式调用的Flux,在指定的数据源上执行
     */
    public static Flux<String> streamChatCompletion(String poolName,Object chatMessage, AIAgent aiAgent,ChatContext chatContext) {
        long startTime = System.currentTimeMillis();
        ClientConfiguration clientConfiguration = ClientConfiguration.getClientConfiguration(poolName);
        AgentAdapter agentAdapter = AgentAdapterFactory.getAgentAdapter(clientConfiguration,chatMessage);
        final ChatObject chatObject = agentAdapter.buildOpenAIRequestParameter(clientConfiguration,chatMessage,   aiAgent,true,chatContext);
        chatObject.getStreamDataBuilder().setStartTime(startTime);
        BaseStreamDataHandler<String> streamDataHandler = new BaseStreamDataHandler<String>() {
 

            @Override
            public boolean handle(String line, FluxSink<String> sink, BooleanWrapperInf firstEventTag, FluxSinkStatus fluxSinkStatus) {
                return AIResponseUtil.handleStringData( this.agentAdapter, line, sink,   firstEventTag,chatObject.getStreamDataBuilder());

            }
            @Override

            public boolean handleException(Object requestBody,Throwable throwable, FluxSink<String> sink, BooleanWrapperInf firstEventTag){
                boolean result = AIResponseUtil.handleStringExceptionData(  throwable, sink,   firstEventTag);
                return result;
            }

            
        };
        streamDataHandler.setStream(chatObject.isStream());
        streamDataHandler.setAgentAdapter(agentAdapter);
        streamDataHandler.setChatObject(chatObject);
        return buildFlux(  clientConfiguration,   chatObject ,  streamDataHandler);

    }


    

    /**
     * 调用图片生成模型，生成图片
     * @param poolName
     * @param message
     * @return
     */
    public static ImageEvent multimodalImageGeneration(String poolName,  ImageAgentMessage message, StoreFilePathFunction storeFilePathFunction,AIAgent aiAgent) {
        ImageEvent imageEvent = null;       

        try {
            ClientConfiguration config = ClientConfiguration.getClientConfiguration(poolName);
            AgentAdapter agentAdapter = AgentAdapterFactory.getAgentAdapter(config,message);
            StoreChatObject storeChatObject = agentAdapter.buildGenImageRequestParameter(config,message,aiAgent);
            storeChatObject.setStoreFilePathFunction(storeFilePathFunction);
            
            Map data = HttpRequestProxy.sendJsonBody(config,storeChatObject.getMessage(),agentAdapter.getGenImageCompletionsUrl(config,message),Map.class);
            imageEvent = agentAdapter.buildGenImageResponse(config,message,storeChatObject, data);
        }
        catch(Exception e){
            imageEvent = new ImageEvent();
            imageEvent.setCode(ResponseStatus.ERROR_CODE);
            imageEvent.setMessage(SimpleStringUtil.exceptionToString(e));
        }

        return imageEvent;

    }

    /**
     * 调用图片生成模型，生成图片
     * @param message
     * @return
     */
    public static ImageEvent multimodalImageGeneration( ImageAgentMessage message,AIAgent aiAgent) {

        return multimodalImageGeneration(null,  message,  (StoreFilePathFunction) null,aiAgent) ;
    }
    /**
     * 调用音频合成模型，流式生成音频，实时播放
     * @param poolName
     * @param audioAgentMessage
     * @return
     */
    public static Flux<ServerEvent> streamAudioGenerationEvent(String poolName,   AudioAgentMessage audioAgentMessage, StoreFilePathFunction storeFilePathFunction, AIAgent aiAgent) {       
      

            return AIAgentUtil.streamChatCompletionEvent(poolName,   audioAgentMessage,   storeFilePathFunction,   aiAgent);
            
    }
    /**
     * 调用音频合成模型，生成音频
     * @param poolName
     * @param message
     * @return
     */
    public static AudioEvent multimodalAudioGeneration(String poolName,  AudioAgentMessage message, StoreFilePathFunction storeFilePathFunction,AIAgent aiAgent) {
        return multimodalAudioGeneration(  poolName,    message,   storeFilePathFunction,  aiAgent,(ChatContext)null);
    }
        /**
         * 调用音频合成模型，生成音频
         * @param poolName
         * @param message
         * @return
         */
    public static AudioEvent multimodalAudioGeneration(String poolName,  AudioAgentMessage message, StoreFilePathFunction storeFilePathFunction,AIAgent aiAgent,ChatContext chatContext) {
        
        ClientConfiguration config = ClientConfiguration.getClientConfiguration(poolName);
        AgentAdapter agentAdapter = AgentAdapterFactory.getAgentAdapter(config, message);
        StoreChatObject storeChatObject = agentAdapter.buildGenAudioRequestParameter(config, message,aiAgent,  chatContext);
        storeChatObject.setStoreFilePathFunction(storeFilePathFunction);
        AudioEvent audioEvent = null;
        try {
//            StoreFilePathFunction storeFilePathFunction = storeChatObject.getStoreFilePathFunction();
            if (storeFilePathFunction != null && storeFilePathFunction instanceof ReponseStoreFilePathFunction) {
                String audioUrl = HttpRequestProxy.sendJsonBody(config, storeChatObject.getMessage(), agentAdapter.getGenAudioCompletionsUrl(config,message), AIResponseUtil.buildDownAudioHttpClientResponseHandler(config, message,storeChatObject));
                audioEvent = new AudioEvent();
                audioEvent.setAudioUrl(audioUrl);

            } else {
                Map data = HttpRequestProxy.sendJsonBody(config, storeChatObject.getMessage(), agentAdapter.getGenAudioCompletionsUrl(config,message), Map.class);
                audioEvent = agentAdapter.buildGenAudioResponse(config, message, storeChatObject,data);

            }
        }
        catch(Exception e){
            audioEvent = new AudioEvent();
            audioEvent.setCode(ResponseStatus.ERROR_CODE);
            audioEvent.setMessage(SimpleStringUtil.exceptionToString(e));
        }
        return audioEvent;
//        Map data = HttpRequestProxy.sendJsonBody(poolName,message,url,Map.class);
//        Map output = (Map)data.get("output");
//        Map audio = (Map)output.get("audio");
//        String finishReason = (String)output.get("finish_reason");
//
//        if(audio == null && finishReason == null)
//            return null;
//        AudioEvent audioEvent = new AudioEvent();
//        audioEvent.setFinishReason(finishReason);
//        String audioUrl = (String)audio.get("url");
//        String auditData = (String)audio.get("data");
//        Object expiresAt_ = audio.get("expires_at");
//        if(expiresAt_ != null) {
//            if (expiresAt_ instanceof Long) {
//                audioEvent.setExpiresAt((Long) expiresAt_);
//            } else {
//                audioEvent.setExpiresAt((Integer) expiresAt_);
//            }
//        }
//        audioEvent.setAudioBase64(auditData);
//        audioEvent.setAudioUrl(audioUrl);


//        return audioEvent;
    }

    /**
     * 调用音频合成模型，生成音频
     * @param message
     * @return
     */
    public static AudioEvent multimodalAudioGeneration( AudioAgentMessage message,AIAgent aiAgent) {

        return multimodalAudioGeneration(null,  message, (StoreFilePathFunction) null,  aiAgent) ;
    }
    /**
     * 创建流式调用的Flux，使用默认数据源
     */
    public static Flux<ServerEvent> streamChatCompletionEvent( Object message, AIAgent aiAgent) {
        return streamChatCompletionEvent((String)null , message, (StoreFilePathFunction) null,   aiAgent);
    }

    public static <T> void streamChatCompletionEvent(ClientConfiguration clientConfiguration, ToolAgentMessage toolAgentMessage,
                                                     FluxSink<T> sink,DisposeEventHandler disposeEventHandler, AIAgent aiAgent ){
        streamChatCompletionEvent(  clientConfiguration,   toolAgentMessage,
                 sink,  disposeEventHandler,   aiAgent,(ChatContext)null);
    }

    public static <T> void streamChatCompletionEvent(ClientConfiguration clientConfiguration, ToolAgentMessage toolAgentMessage,
                                                     FluxSink<T> sink,DisposeEventHandler disposeEventHandler, AIAgent aiAgent,ChatContext chatContext) {
        long startTime = System.currentTimeMillis();
        AgentAdapter agentAdapter = AgentAdapterFactory.getAgentAdapter(clientConfiguration,toolAgentMessage);

        final ChatObject chatObject = agentAdapter.buildOpenAIRequestParameter(clientConfiguration,toolAgentMessage,   aiAgent,true,chatContext);
        chatObject.getStreamDataBuilder().setStartTime(startTime);
        BaseStreamDataHandler<ServerEvent> streamDataHandler = new BaseStreamDataHandler<ServerEvent>() {
 
            @Override
            public boolean handle(String line, FluxSink<ServerEvent> sink, BooleanWrapperInf firstEventTag, FluxSinkStatus fluxSinkStatus) {
                boolean result = AIResponseUtil.handleServerEventData(this.agentAdapter, this.isStream(),
                        line, sink, firstEventTag, (BaseStreamDataBuilder)chatObject.getStreamDataBuilder(),   fluxSinkStatus);
                return result;
            }
            @Override

            public boolean handleException(Object requestBody,Throwable throwable, FluxSink<ServerEvent> sink, BooleanWrapperInf firstEventTag){
                boolean result = AIResponseUtil.handleServerEventExceptionData(  throwable, sink,   (BaseStreamDataBuilder)chatObject.getStreamDataBuilder(), firstEventTag);
                return result;
            }


        };
        streamDataHandler.setStream(chatObject.isStream());
        streamDataHandler.setAgentAdapter(agentAdapter);
        streamDataHandler.setChatObject(chatObject);
//        buildFlux(  clientConfiguration,    chatObject ,  streamDataHandler);
        executeTools(  clientConfiguration,   chatObject,   streamDataHandler,   sink,  disposeEventHandler);
        
    }
    
    static <T> void executeTools(ClientConfiguration clientConfiguration, ChatObject chatObject, 
								 BaseStreamDataHandler streamDataHandler, FluxSink<T> sink,DisposeEventHandler disposeEventHandler){
        Object data = null;
        Object message = chatObject.getMessage();

        BaseStreamDataBuilder baseStreamDataBuilder = (BaseStreamDataBuilder) streamDataHandler.getStreamDataBuilder();
        try {


            BaseURLResponseHandler responseHandler = new BaseURLResponseHandler<Void>() {
                @Override
                public Void handleResponse(ClassicHttpResponse response) throws IOException, ParseException {
                    streamDataHandler.setHttpUriRequestBase(httpUriRequestBase);
                    AIResponseUtil.handleStreamResponse(url, response, sink, streamDataHandler,disposeEventHandler);
                    return null;

                }
            };
            if (chatObject.getAIChatRequestType() == null || chatObject.getAIChatRequestType().equals(AIConstants.AI_CHAT_REQUEST_BODY_JSON)){
                Map header = new LinkedHashMap();

                if (chatObject.isStream()) {
                    chatObject.getSseHeaderSetFunction().setSSEHeaders( header);
//                                header.put("Accept", "text/event-stream");
//                                header.put("X-DashScope-SSE", "enable");
                }

                if (message != null) {
                    if (message instanceof String) {
                        data =  message;
                    } else {
                        data = SimpleStringUtil.object2json(message);
                    }
                }

                HttpRequestProxy.sendJsonBody(clientConfiguration, (String)data, chatObject.getCompletionsUrl(), header, responseHandler);
//                            if(baseStreamDataBuilder.)
            }
            else if (chatObject.getAIChatRequestType().equals(AIConstants.AI_CHAT_REQUEST_POST_FORM)){
                Map header = new LinkedHashMap();
                if (chatObject.isStream()) {
//                                header.put("Accept", "text/event-stream");
                    chatObject.getSseHeaderSetFunction().setSSEHeaders( header);
                }
                data = message;
                Map<String,File> files = chatObject.getFiles();
                if(files == null) {
                    HttpRequestProxy.httpPost(clientConfiguration, chatObject.getCompletionsUrl(),message,  header, responseHandler);
                }
                else{
                    HttpRequestProxy.httpPost(clientConfiguration, chatObject.getCompletionsUrl(),message,files,  header, responseHandler);
                }
            }
            else {
                throw new ReactorCallException("Unsupported request type: "+chatObject.getAIChatRequestType());
            }

            List<FunctionTool> functionTools = baseStreamDataBuilder.getFunctionTools();


            if (functionTools != null && functionTools.size() > 0) {
                streamDataHandler.streamChatCompletionEvent(clientConfiguration,chatObject,baseStreamDataBuilder,sink,disposeEventHandler);

                //                    Flux<ServerEvent> innerflux = streamChatCompletionEvent(poolName, toolAgentMessage);
                //                    // 使用concatWith确保顺序执行：先完成当前事件，再执行工具调用
                //                    return innerflux;

            }

        } catch (ReactorCallException e) {
//                        logger.error("流式请求失败：poolName["+poolName +"],url["+url +"],data:" + data);
            streamDataHandler.handleException(data,e,sink,new NoSynBooleanWrapper( true));
//                        sink.error(e);
        } catch (Exception e) {
            streamDataHandler.handleException(data,e,sink,new NoSynBooleanWrapper( true));
//                        sink.error(new ReactorCallException("流式请求失败：poolName["+poolName +"],url["+url +"],", e));
        }
        catch (Throwable e) {
            streamDataHandler.handleException(data,e,sink,new NoSynBooleanWrapper( true));
//                        sink.error(new ReactorCallException("流式请求失败：poolName["+poolName +"],url["+url +"],", e));
        }
    }
    public static Flux<ServerEvent> streamChatCompletionEvent(String poolName,Object chatMessage,  AIAgent aiAgent){
        return  streamChatCompletionEvent(  poolName,  chatMessage,    aiAgent, (ChatContext)null);
    }
    public static Flux<ServerEvent> streamChatCompletionEvent(String poolName,Object chatMessage,  AIAgent aiAgent, ChatContext chatStreamCallback){
        return  streamChatCompletionEvent(poolName,chatMessage,(StoreFilePathFunction) null, aiAgent,chatStreamCallback);
    }
    public static Flux<ServerEvent> streamChatCompletionEvent(String poolName,Object chatMessage, StoreFilePathFunction storeFilePathFunction, AIAgent aiAgent) {
        return  streamChatCompletionEvent(poolName,chatMessage,storeFilePathFunction, aiAgent,(ChatContext)null);
    }
    /**
     * 创建流式调用的Flux,在指定的数据源上执行
     */
    public static Flux<ServerEvent> streamChatCompletionEvent(String poolName,Object chatMessage, StoreFilePathFunction storeFilePathFunction, AIAgent aiAgent, ChatContext chatStreamCallback) {
        long startTime = System.currentTimeMillis();
        ClientConfiguration clientConfiguration = ClientConfiguration.getClientConfiguration(poolName);
        AgentAdapter agentAdapter = AgentAdapterFactory.getAgentAdapter(clientConfiguration,chatMessage);
         
        final ChatObject chatObject = agentAdapter.buildOpenAIRequestParameter(clientConfiguration,chatMessage,   aiAgent,true,chatStreamCallback);
        chatObject.setStoreFilePathFunction(storeFilePathFunction);
        chatObject.getStreamDataBuilder().setStartTime(startTime);
        chatObject.setChatContext(chatStreamCallback);
        BaseStreamDataHandler<ServerEvent> streamDataHandler = new BaseStreamDataHandler<ServerEvent>() {
           
            @Override
            public boolean handle(String line, FluxSink<ServerEvent> sink, BooleanWrapperInf firstEventTag, FluxSinkStatus fluxSinkStatus) {
                return AIResponseUtil.handleServerEventData(this.agentAdapter, this.isStream(), line, sink, firstEventTag, (BaseStreamDataBuilder)chatObject.getStreamDataBuilder(),   fluxSinkStatus);

            }
            @Override

            public boolean handleException(Object requestBody,Throwable throwable, FluxSink<ServerEvent> sink, BooleanWrapperInf firstEventTag){
                boolean result = AIResponseUtil.handleServerEventExceptionData(  throwable, sink,   (BaseStreamDataBuilder)chatObject.getStreamDataBuilder(),firstEventTag);
                return result;
            }

             
        };
        streamDataHandler.setStream(chatObject.isStream());
        streamDataHandler.setAgentAdapter(agentAdapter);
        streamDataHandler.setChatObject(chatObject);
        return buildFlux(  clientConfiguration,    chatObject ,  streamDataHandler);

    }
    
     
     
    
//    
//
//    /**
//     * 创建流式调用的Flux,在指定的数据源上执行
//     */
//    public static Flux<ServerEvent> streamChatCompletionEventWithTool(String poolName,AgentMessage chatMessage,boolean toolStream, AIAgent aiAgent) {
//         
//            
//        if(!toolStream) {
//            Boolean stream = chatMessage.getStream();
//            chatMessage.setStream(false);
//            ServerEvent serverEvent = AIAgentUtil.chatCompletionEvent(poolName, chatMessage, true,   aiAgent);
//            chatMessage.setStream(stream);
//            List<FunctionTool> functionTools = serverEvent.getFunctionTools();
//            if (functionTools != null && functionTools.size() > 0) {
//                ChatAgentMessage _chatMessage = (ChatAgentMessage) chatMessage;
//                _chatMessage.addAssistantSessionMessage(serverEvent,   aiAgent);
//                ToolAgentMessage toolAgentMessage = new ToolAgentMessage(_chatMessage, functionTools);
//                return streamChatCompletionEvent(poolName, toolAgentMessage,   aiAgent);
//
//            } else {
//                return buildFlux(serverEvent);
//            }
//        }
//        else{
//            Flux<ServerEvent> flux = AIAgentUtil.streamChatCompletionEvent(poolName, chatMessage,   aiAgent);
//            return flux;
////            final Flux<ServerEvent> newflux = flux
//////                    .doOnNext(serverEvent -> {
////////                System.out.print(serverEvent.getData());
//////            })
////            .switchMap(serverEvent -> {
//////                System.out.print(serverEvent.getData());
////                List<FunctionTool> functionTools = serverEvent.getFunctionTools();
////                if(serverEvent.isDone()){
////                    functionTools = serverEvent.getFunctionTools();
////                }
////                if(serverEvent.finished()){
////                    functionTools = serverEvent.getFunctionTools();
////                    //
////                }
////                
////                if (functionTools != null && functionTools.size() > 0) {
//////                    ChatAgentMessage _chatMessage = (ChatAgentMessage) chatMessage;
//////                    _chatMessage.addAssistantSessionMessage(serverEvent);
//////                    ToolAgentMessage toolAgentMessage = new ToolAgentMessage(_chatMessage, functionTools);
//////                    Flux<ServerEvent> innerflux = streamChatCompletionEvent(poolName, toolAgentMessage);
//////                    // 使用concatWith确保顺序执行：先完成当前事件，再执行工具调用
//////                    return innerflux;
////                    return Flux.just(serverEvent);
////                } else {
////                    // 如果没有工具调用，直接返回当前事件
////                    return Flux.just(serverEvent);
////                }
////            }); //打印流式调用返回的问题答案片段
////                   
////            return newflux;
//           
//            
//        }
//
//       
//
//    }
    private static <T> void executeSink(DisposeEventHandler disposeEventHandler,FluxSink<T> sink,ClientConfiguration clientConfiguration,ChatObject chatObject ,BaseStreamDataHandler<T> streamDataHandler,boolean fromAgentFlow){
        Object data = null;
        try {

            Object message = chatObject.getMessage();

            BaseStreamDataBuilder baseStreamDataBuilder = (BaseStreamDataBuilder) streamDataHandler.getStreamDataBuilder();

//            DisposeEventHandler disposeEventHandler = new DisposeEventHandler();

            BaseURLResponseHandler responseHandler = new BaseURLResponseHandler<Void>() {
                @Override
                public Void handleResponse(ClassicHttpResponse response) throws IOException, ParseException {
                    streamDataHandler.setHttpUriRequestBase(httpUriRequestBase);
                    AIResponseUtil.handleStreamResponse(url, response, sink, streamDataHandler,disposeEventHandler);
                    return null;

                }
            };
            if (chatObject.getAIChatRequestType() == null || chatObject.getAIChatRequestType().equals(AIConstants.AI_CHAT_REQUEST_BODY_JSON)){
                Map header = new LinkedHashMap();

                if (chatObject.isStream()) {
                    chatObject.getSseHeaderSetFunction().setSSEHeaders( header);
//                                header.put("Accept", "text/event-stream");
//                                header.put("X-DashScope-SSE", "enable");
                }

                if (message != null) {
                    if (message instanceof String) {
                        data = (String) message;
                    } else {
                        data = SimpleStringUtil.object2json(message);
                    }
                }
                AgentMessage agentMessage = chatObject.getAgentMessage();
                int retry = agentMessage.getRetry();
                if(retry <= 0) {
                    HttpRequestProxy.sendJsonBody(clientConfiguration, (String) data, chatObject.getCompletionsUrl(), header, responseHandler);
                }
                else{
                    final String _data = (String)data;
                    RetryUtil.retry(retry, agentMessage.getRetryInterval(), (RetryCallback<Void>) () -> {
                         HttpRequestProxy.sendJsonBody(clientConfiguration, (String) _data, chatObject.getCompletionsUrl(), header, responseHandler);
                         return null;
                    });
                }
//                            if(baseStreamDataBuilder.)
            }
            else if (chatObject.getAIChatRequestType().equals(AIConstants.AI_CHAT_REQUEST_POST_FORM)){
                Map header = new LinkedHashMap();
                if (chatObject.isStream()) {
//                                header.put("Accept", "text/event-stream");
                    chatObject.getSseHeaderSetFunction().setSSEHeaders( header);
                }
                data = message;
                Map<String,File> files = chatObject.getFiles();
                if(files == null) {
                    HttpRequestProxy.httpPost(clientConfiguration, chatObject.getCompletionsUrl(),message,  header, responseHandler);
                }
                else{
                    HttpRequestProxy.httpPost(clientConfiguration, chatObject.getCompletionsUrl(),message,files,  header, responseHandler);
                }
            }
            else {
                throw new ReactorCallException("Unsupported request type: "+chatObject.getAIChatRequestType());
            }

            List<FunctionTool> functionTools = baseStreamDataBuilder.getFunctionTools();


            if (functionTools != null && functionTools.size() > 0) {
                streamDataHandler.streamChatCompletionEvent(clientConfiguration,chatObject,baseStreamDataBuilder,sink,disposeEventHandler);

//                    Flux<ServerEvent> innerflux = streamChatCompletionEvent(poolName, toolAgentMessage);
//                    // 使用concatWith确保顺序执行：先完成当前事件，再执行工具调用
//                    return innerflux;

            }

        } catch (ReactorCallException e) {
//                        logger.error("流式请求失败：poolName["+poolName +"],url["+url +"],data:" + data);
            streamDataHandler.handleException(data,e,sink,new NoSynBooleanWrapper( true));
//                        sink.error(e);
        } catch (Exception e) {
            streamDataHandler.handleException(data,e,sink,new NoSynBooleanWrapper( true));
//                        sink.error(new ReactorCallException("流式请求失败：poolName["+poolName +"],url["+url +"],", e));
        }
        catch (Throwable e) {
            streamDataHandler.handleException(data,e,sink,new NoSynBooleanWrapper( true));
//                        sink.error(new ReactorCallException("流式请求失败：poolName["+poolName +"],url["+url +"],", e));
        }
        finally {
            if(!fromAgentFlow) {
                sink.complete();
            }
        }
    }

    private static <T> Flux<T> buildFlux(ClientConfiguration clientConfiguration,ChatObject chatObject ,BaseStreamDataHandler<T> streamDataHandler) {
        AIAgent aiAgent = chatObject.getAiAgent();
        if(aiAgent != null){
            FluxSink<ServerEvent> fluxSink = aiAgent.getAgentFluxSink();
            if(fluxSink != null){
                executeSink(aiAgent.getDisposeEventHandler(),(FluxSink<T>)fluxSink,  clientConfiguration,  chatObject ,  streamDataHandler,true);
                return aiAgent.getFlux();
            }
            
        }
        return Flux.<T>create(sink -> {
            DisposeEventHandler disposeEventHandler = new DisposeEventHandler();
                    disposeEventHandler.onDispose(sink);
            executeSink(disposeEventHandler,sink,  clientConfiguration,  chatObject ,  streamDataHandler,false);
		}, FluxSink.OverflowStrategy.BUFFER)
		.subscribeOn(Schedulers.boundedElastic()) // 在弹性线程池中执行阻塞IO
//		.timeout(Duration.ofSeconds(60)) // 设置超时
		.onErrorResume(throwable -> {
//                    String error = SimpleStringUtil.exceptionToString(throwable);
//                    System.err.println("流式处理错误: " + throwable.getMessage());
//                    String error = SimpleStringUtil.exceptionToString(throwable);
			if(logger.isDebugEnabled()) {
				logger.debug(throwable.getMessage(), throwable);
			}
			// 修改此处，将错误信息作为Flux输出
			return Flux.empty();
		});
    }

    private static   Flux<String> buildFlux(ClientConfiguration clientConfiguration,String url,Object message ,String method ) {
        return Flux.<String>create(sink -> {
                    String data = null;
                     CommonStreamDataHandler<String> streamDataHandler = new BaseCommonStreamDataHandler<String>() {
                         /**
                          * 处理异常，如果数据已经返回完毕，则返回true，指示关闭对话，否则返回false
                          *
                          * @param requestBody
                          * @param throwable   异常
                          * @param sink        数据行处理结果
                          * @return
                          */
                         @Override
                         public void handleException(Object requestBody, Throwable throwable, FluxSink<String> sink) {
                         }
                     };
                    try {

                        if (message != null) {
                            if (message instanceof String) {
                                data = (String) message;
                            } else {
                                data = SimpleStringUtil.object2json(message);
                            }
                        }
                        
                        final String _data = data;

                        BaseURLResponseHandler responseHandler = new BaseURLResponseHandler<Void>() {
                            @Override
                            public Void handleResponse(ClassicHttpResponse response) throws IOException, ParseException {
                                streamDataHandler.setHttpUriRequestBase(httpUriRequestBase);
                                 AIResponseUtil.handleStreamResponse(url, response, sink,_data, streamDataHandler);
                                 return null;

                            }
                        };
                        
                      
                        if(method.equals(HttpMethodName.HTTP_GET)) {
                            HttpRequestProxy.httpGet(clientConfiguration, url, responseHandler);
                        }
                        else if(method.equals(HttpMethodName.HTTP_POST)) {
                            HttpRequestProxy.sendJsonBody(clientConfiguration,  data, url,  responseHandler);
                        }

//                        HttpRequestProxy.sendJsonBody(clientConfiguration, (String)data, url, header, responseHandler);
                       
                    } catch (ReactorCallException e) {
//                        logger.error("流式请求失败：poolName["+poolName +"],url["+url +"],data:" + data);
                        streamDataHandler.handleException(data,e,sink );
                        sink.error(e);
//                        sink.error(e);
                    } catch (Exception e) {
                        streamDataHandler.handleException(data,e,sink );
                        sink.error(e);
//                        sink.error(new ReactorCallException("流式请求失败：poolName["+poolName +"],url["+url +"],", e));
                    }
                    catch (Throwable e) {
                        streamDataHandler.handleException(data,e,sink );
                        sink.error(e);
//                        sink.error(new ReactorCallException("流式请求失败：poolName["+poolName +"],url["+url +"],", e));
                    }
                    finally {
                        sink.complete();
                    }
                }, FluxSink.OverflowStrategy.BUFFER)
                .subscribeOn(Schedulers.boundedElastic()) // 在弹性线程池中执行阻塞IO
//                .timeout(Duration.ofSeconds(60)) // 设置超时
                .onErrorResume(throwable -> {
//                    String error = SimpleStringUtil.exceptionToString(throwable);
//                    System.err.println("流式处理错误: " + throwable.getMessage());
//                    String error = SimpleStringUtil.exceptionToString(throwable);
                    if(logger.isErrorEnabled()) {
                        logger.error(throwable.getMessage(), throwable);
                    }
                    // 修改此处，将错误信息作为Flux输出
                    return Flux.empty();
                });
    }
	
	private static   Flux<String> buildMcpSSEFlux(ClientConfiguration clientConfiguration,String url   ) {
		return Flux.<String>create(sink -> {
					CommonStreamDataHandler<String> streamDataHandler = new BaseCommonStreamDataHandler<String>() {
						/**
						 * 处理异常，如果数据已经返回完毕，则返回true，指示关闭对话，否则返回false
						 *
						 * @param requestBody
						 * @param throwable   异常
						 * @param sink        数据行处理结果
						 * @return
						 */
						@Override
						public void handleException(Object requestBody, Throwable throwable, FluxSink<String> sink) {
						}
					};
					try {
						 
						
						BaseURLResponseHandler responseHandler = new BaseURLResponseHandler<Void>() {
							@Override
							public Void handleResponse(ClassicHttpResponse response) throws IOException, ParseException {
								streamDataHandler.setHttpUriRequestBase(httpUriRequestBase);
								AIResponseUtil.handleStreamResponse(url, response, sink,"", streamDataHandler);
								return null;
								
							}
						};
						
						Map header = new LinkedHashMap();

						header.put("Accept", "text/event-stream");
						header.put("Cache-Control", "no-cache");
                        header.put("Connection", "keep-alive");
                        
						 
						HttpRequestProxy.httpGet(clientConfiguration, url,header, responseHandler);
						 

//                        HttpRequestProxy.sendJsonBody(clientConfiguration, (String)data, url, header, responseHandler);
						
					} catch (ReactorCallException e) {
//                        logger.error("流式请求失败：poolName["+poolName +"],url["+url +"],data:" + data);
						streamDataHandler.handleException("",e,sink );
						sink.error(e);
//                        sink.error(e);
					} catch (Exception e) {
						streamDataHandler.handleException("",e,sink );
						sink.error(e);
//                        sink.error(new ReactorCallException("流式请求失败：poolName["+poolName +"],url["+url +"],", e));
					}
					catch (Throwable e) {
						streamDataHandler.handleException("",e,sink );
						sink.error(e);
//                        sink.error(new ReactorCallException("流式请求失败：poolName["+poolName +"],url["+url +"],", e));
					}
					finally {
						sink.complete();
					}
				}, FluxSink.OverflowStrategy.BUFFER)
				.subscribeOn(Schedulers.boundedElastic()) // 在弹性线程池中执行阻塞IO
//				.timeout(Duration.ofSeconds(120)) // 设置超时
				.onErrorResume(throwable -> {
//                    String error = SimpleStringUtil.exceptionToString(throwable);
//                    System.err.println("流式处理错误: " + throwable.getMessage());
//                    String error = SimpleStringUtil.exceptionToString(throwable);
					if(logger.isErrorEnabled()) {
						logger.error(throwable.getMessage(), throwable);
					}
					// 修改此处，将错误信息作为Flux输出
					return Flux.empty();
				});
	}

    private static <T> Flux<T> buildFlux(ServerEvent serverEvent) {
        return Flux.<T>create(sink -> {
                     sink.next((T)serverEvent);
                }, FluxSink.OverflowStrategy.BUFFER)
                .subscribeOn(Schedulers.boundedElastic()) // 在弹性线程池中执行阻塞IO
//                .timeout(Duration.ofSeconds(60)) // 设置超时
                .onErrorResume(throwable -> {
//                    String error = SimpleStringUtil.exceptionToString(throwable);
//                    System.err.println("流式处理错误: " + throwable.getMessage());
//                    String error = SimpleStringUtil.exceptionToString(throwable);
                    if(logger.isDebugEnabled()) {
                        logger.debug(throwable.getMessage(), throwable);
                    }
                    // 修改此处，将错误信息作为Flux输出
                    return Flux.empty();
                });
    }
    
    /**
     * 同步调用模型服务，返回问答内容
     */
    public static ServerEvent imageParser(Object message, AIAgent aiAgent) {
        return imageParser(  (String)null, message,   aiAgent);


    }

    /**
     * 同步调用模型服务，返回问答内容
     */
    public static ServerEvent imageParser(String poolName,Object message, AIAgent aiAgent) {
        return chatCompletionEvent(poolName,message,   aiAgent);
        


    }
    
    /**
     * 同步调用模型服务，返回问答内容
     */
    public static ServerEvent videoParser(String poolName,VideoVLAgentMessage message, AIAgent aiAgent) {
        return chatCompletionEvent(poolName,message,   aiAgent);



    }
    /**
     * 同步调用模型服务，返回问答内容
     */
    public static ServerEvent audioParser(String poolName,Object message, AIAgent aiAgent) {
        return chatCompletionEvent(poolName,message,   aiAgent);



    }
    /**
     * 同步调用模型服务，返回问答内容
     */
    public static ServerEvent chatCompletionEvent(Object message, AIAgent aiAgent) {
        return chatCompletionEvent(  (String)null,  message,   aiAgent);


    }
    public static ServerEvent chatCompletionEvent(String poolName, Object chatMessage , AIAgent aiAgent ) {
        return chatCompletionEvent(  poolName,   chatMessage ,   aiAgent,(ChatContext)null);
    }
    

    public static ServerEvent chatCompletionEvent(String poolName, Object chatMessage , AIAgent aiAgent,ChatContext chatContext) {
        long startTime = System.currentTimeMillis();
        ClientConfiguration config = ClientConfiguration.getClientConfiguration(poolName);
        AgentAdapter agentAdapter = AgentAdapterFactory.getAgentAdapter(config,chatMessage);
        ChatObject chatObject = agentAdapter.buildOpenAIRequestParameter(config,chatMessage,aiAgent,false,chatContext);
        chatObject.getStreamDataBuilder().setStartTime(startTime);
        Object message = chatObject.getMessage();
        String data = null;
        ServerEvent serverEvent = null;
        BaseURLResponseHandler<ServerEvent> responseHandler = new BaseURLResponseHandler<ServerEvent>() {
            @Override
            public ServerEvent handleResponse(ClassicHttpResponse response) throws IOException, ParseException {
                return   AIResponseUtil.handleChatResponse(agentAdapter, chatObject.getCompletionsUrl(), response, chatObject.getStreamDataBuilder());
            }
        };

        if (chatObject.getAIChatRequestType() == null || chatObject.getAIChatRequestType().equals(AIConstants.AI_CHAT_REQUEST_BODY_JSON)) {
            if (message != null) {
                if (message instanceof String) {
                    data = (String) message;
                } else {
                    data = SimpleStringUtil.object2json(message);
                }
            }
            serverEvent = HttpRequestProxy.sendJsonBody(config, data, chatObject.getCompletionsUrl(), (Map)null, responseHandler);
        }
        else if (chatObject.getAIChatRequestType().equals(AIConstants.AI_CHAT_REQUEST_POST_FORM)){

            Map<String,File> files = chatObject.getFiles();
            if(files == null) {
                serverEvent =  HttpRequestProxy.httpPost(config, chatObject.getCompletionsUrl(), message, (Map) null, responseHandler);
            }
            else{
                serverEvent = HttpRequestProxy.httpPost(config, chatObject.getCompletionsUrl(), message, files, (Map) null,responseHandler);
            }
        }
        else {
            throw new ReactorCallException("Unsupported request type: "+chatObject.getAIChatRequestType());
        }
        if(serverEvent == null) {
            throw new ReactorCallException("ServerEvent is null");
        }
        serverEvent.setDone(true);
        serverEvent.setFirst(true);
//        if(fromStreamChat)
//            return serverEvent;
        List<FunctionTool> functionTools = serverEvent.getFunctionTools();
        if(functionTools != null && functionTools.size() > 0){
            ChatAgentMessage _chatMessage = (ChatAgentMessage) chatMessage;
            _chatMessage.addAssistantSessionMessage(serverEvent ,aiAgent);
            if(serverEvent.getData() != null && serverEvent.getData().length() > 0) {
                if(logger.isDebugEnabled()) {
                    logger.debug(serverEvent.getData());
                }
            }
            ToolAgentMessage toolAgentMessage = new ToolAgentMessage(_chatMessage,functionTools);
            return chatCompletionEvent(  poolName,toolAgentMessage,aiAgent);

        }
        else {
            return serverEvent;
        }


    }
    public static <T> Flux<T> streamChatCompletion(Object message,BaseStreamDataHandler<T> streamDataHandler, AIAgent aiAgent){
        return streamChatCompletion((String)null ,   message, streamDataHandler,   aiAgent);
    }
    /**
     * 创建流式调用的Flux,在指定的数据源上执行
     */
    public static <T> Flux<T> streamChatCompletion(String poolName,Object chatMessage,BaseStreamDataHandler<T> streamDataHandler, AIAgent aiAgent){
        return streamChatCompletion(poolName,chatMessage,streamDataHandler, aiAgent,(ChatContext)null);
    }

    /**
     * 创建流式调用的Flux,在指定的数据源上执行
     */
    public static <T> Flux<T> streamChatCompletion(String poolName,Object chatMessage,BaseStreamDataHandler<T> streamDataHandler, AIAgent aiAgent,ChatContext chatContext) {
        long startTime = System.currentTimeMillis();
        ClientConfiguration clientConfiguration = ClientConfiguration.getClientConfiguration(poolName);
        AgentAdapter agentAdapter = AgentAdapterFactory.getAgentAdapter(clientConfiguration,chatMessage);
        final ChatObject chatObject = agentAdapter.buildOpenAIRequestParameter(clientConfiguration,chatMessage,   aiAgent,true,chatContext);
        chatObject.getStreamDataBuilder().setStartTime(startTime);
        streamDataHandler.setStream(chatObject.isStream());
        streamDataHandler.setAgentAdapter(agentAdapter);
        streamDataHandler.setChatObject(chatObject);
        return buildFlux(  clientConfiguration,    chatObject ,  streamDataHandler);
    }

    public static VideoTask submitVideoTask(String maasName,  VideoAgentMessage videoAgentMessage,AIAgent aiAgent) {
        ClientConfiguration clientConfiguration = ClientConfiguration.getClientConfiguration(maasName);
        AgentAdapter agentAdapter = AgentAdapterFactory.getAgentAdapter(clientConfiguration,videoAgentMessage);
        StoreChatObject storeChatObject = agentAdapter.buildVideoRequestParameter(clientConfiguration,videoAgentMessage,  aiAgent);
        Map taskInfo = HttpRequestProxy.sendJsonBody(maasName,storeChatObject.getMessage(),agentAdapter.getSubmitVideoTaskUrl(clientConfiguration,videoAgentMessage),videoAgentMessage.getHeaders(),Map.class);
        VideoTask task = agentAdapter.buildVideoResponseTask(clientConfiguration,videoAgentMessage,  taskInfo);
        
        return task;
    }

    public static VideoTask submitVideoTask( VideoAgentMessage videoAgentMessage,  AIAgent aiAgent) {
        return submitVideoTask(null,   videoAgentMessage,  aiAgent) ;
    }
    
    public static VideoGenResult getVideoTaskResult(String maasName, VideoStoreAgentMessage videoStoreAgentMessage, StoreFilePathFunction storeFilePathFunction) {
        ClientConfiguration clientConfiguration = ClientConfiguration.getClientConfiguration(maasName);
        AgentAdapter agentAdapter = AgentAdapterFactory.getAgentAdapter(clientConfiguration,null);
        StoreChatObject storeChatObject = new StoreChatObject();
        agentAdapter._buildGetVideoResultRquestMap(videoStoreAgentMessage,storeChatObject,clientConfiguration);
        storeChatObject.setStoreFilePathFunction(storeFilePathFunction);
        Map taskInfo = HttpRequestProxy.httpGetforObject(maasName,agentAdapter.getVideoTaskResultUrl(clientConfiguration,videoStoreAgentMessage),Map.class);
        VideoGenResult videoGenResult = agentAdapter.buildVideoGenResult(clientConfiguration,videoStoreAgentMessage,storeChatObject,taskInfo);
        
//        Map output = (Map)taskInfo.get("output");
//        result.put("taskId",output.get("task_id"));
//        result.put("taskStatus",output.get("task_status"));
//        result.put("videoUrl",output.get("video_url"));
//        result.put("requestId",taskInfo.get("request_id"));
        return videoGenResult;
    }

    /**
     * 创建流式调用的Flux,在指定的数据源上执行
     */
    public static  Flux<String> stream(String poolName,String url,Object chatMessage ,String method) {
        ClientConfiguration clientConfiguration = ClientConfiguration.getClientConfiguration(poolName);      
      
      
        return buildFlux(  clientConfiguration,  url, chatMessage  ,  method);

    }
	
	/**
	 * 创建流式调用的Flux,在指定的数据源上执行
	 */
	public static  Flux<String> mcpSSE(String poolName,String url  ) {
		ClientConfiguration clientConfiguration = ClientConfiguration.getClientConfiguration(poolName);
		
		
		return buildMcpSSEFlux( clientConfiguration,  url );
		
	}
	
	public static  void stream(String poolName, String url, Object message , String method, DataCollector dataCollector) {
		ClientConfiguration clientConfiguration = ClientConfiguration.getClientConfiguration(poolName);
		
		
		String data = null;
		 
		try {
			CommonStreamDataHandler<String> streamDataHandler = new BaseCommonStreamDataHandler<String>() {
				/**
				 * 处理异常，如果数据已经返回完毕，则返回true，指示关闭对话，否则返回false
				 *
				 * @param requestBody
				 * @param throwable   异常
				 * @param sink        数据行处理结果
				 * @return
				 */
				@Override
				public void handleException(Object requestBody, Throwable throwable, FluxSink<String> sink) {
				}
			};
			if (message != null) {
				if (message instanceof String) {
					data = (String) message;
				} else {
					data = SimpleStringUtil.object2json(message);
				}
			}
			
			final String _data = data;
			
			BaseURLResponseHandler responseHandler = new BaseURLResponseHandler<Void>() {
				@Override
				public Void handleResponse(ClassicHttpResponse response) throws IOException, ParseException {
					streamDataHandler.setHttpUriRequestBase(httpUriRequestBase);
					AIResponseUtil.handleStreamResponse(url, response, _data, dataCollector, streamDataHandler);
					return null;
					
				}
			};
			
			Map header = new LinkedHashMap();
			
			header.put("Accept", "text/event-stream");
			header.put("Cache-Control", "no-cache");
			header.put("Connection", "keep-alive");
			if(method.equals(HttpMethodName.HTTP_GET)) {
				HttpRequestProxy.httpGet(clientConfiguration, url, responseHandler);
			}
			else if(method.equals(HttpMethodName.HTTP_POST)) {
				HttpRequestProxy.sendJsonBody(clientConfiguration,  data, url, header, responseHandler);
			}

//                        HttpRequestProxy.sendJsonBody(clientConfiguration, (String)data, url, header, responseHandler);
			
		} catch (ReactorCallException e) {
//                        logger.error("流式请求失败：poolName["+poolName +"],url["+url +"],data:" + data);
			 
//                        sink.error(e);
		} catch (Exception e) {
			 
//                        sink.error(new ReactorCallException("流式请求失败：poolName["+poolName +"],url["+url +"],", e));
		}
		catch (Throwable e) {
		}
		finally {
		}
		
	}

    /**
     * 创建流式调用的Flux,在指定的数据源上执行
     */
    public static  Flux<String> stream(String poolName,String url,String method) {
        return stream(  poolName,  url,null ,  method);

    }


    public static float[] embedding(EmbeddingMessage embeddingMessage,AIAgent agent) {
        ClientConfiguration config = ClientConfiguration.getClientConfiguration(embeddingMessage.getMaas());
        AgentAdapter agentAdapter = AgentAdapterFactory.getAgentAdapter(config,embeddingMessage);
        Map<String,Object> params = agentAdapter.buildEmbeddingMessage(config,embeddingMessage,agent);
        float[] embedding = null;
        int retry = embeddingMessage.getRetry();
        if(retry <=  0 ){
            embedding = agentAdapter.embedding(  config,embeddingMessage,agent,params);
        }
        else{
            embedding = RetryUtil.retry(retry, embeddingMessage.getRetryInterval(), () -> agentAdapter.embedding(  config,embeddingMessage,agent,params));

        }
        return embedding;
//        EmbeddingResponse result = HttpRequestProxy.sendJsonBody(embeddingMessage.getMaas(), params, agentAdapter.getEmbeddingUrl(embeddingMessage), EmbeddingResponse.class);
//        if(result != null){
//            return result.embedding();
//        }
//        return null;
    }

    public static   List<RerankedDocument> rerank(RerankMessage rerankMessage, AIAgent  agent) {
        ClientConfiguration config = ClientConfiguration.getClientConfiguration(rerankMessage.getMaas());
        AgentAdapter agentAdapter = AgentAdapterFactory.getAgentAdapter(config,rerankMessage);
        Map<String,Object> params = agentAdapter.buildRerankMessage(config,rerankMessage,agent);
        
        List<RerankedDocument> rerankedDocuments = null;
        int retry = rerankMessage.getRetry();
        if(retry <=  0 ){
            rerankedDocuments = agentAdapter.rerank(config, rerankMessage, agent, params);
        }
        else{
            rerankedDocuments = RetryUtil.retry(retry, rerankMessage.getRetryInterval(), () -> agentAdapter.rerank(config, rerankMessage, agent, params));
            
        }
         
       
        if(rerankedDocuments != null && rerankedDocuments.size() > 0) {
            List<RerankedDocument> relevanceScoreDocuments = new ArrayList<>();
            List<RerankedDocument> topKDocuments = new ArrayList<>();
            Double relevanceScore = rerankMessage.getRelevanceScore();
            Integer topK = rerankMessage.getTopK();
            if(relevanceScore == null && topK == null){
                return rerankedDocuments;
            }
            if (relevanceScore != null && relevanceScore > 0d) {
                rerankedDocuments.forEach(rerankedDocument -> {
                    if(rerankedDocument.getRelevanceScore() >= relevanceScore)
                        relevanceScoreDocuments.add(rerankedDocument);
                });
            }
            
            if(topK != null && topK > 0){
               
                if(relevanceScoreDocuments.size() > 0){
                    for( int i = 0; i < topK && i < relevanceScoreDocuments.size(); i++){
                        topKDocuments.add(relevanceScoreDocuments.get(i));
                    }
                }
                else{
                    for( int i = 0; i < topK && i < rerankedDocuments.size(); i++){
                        topKDocuments.add(rerankedDocuments.get(i));
                    }
                }
            }
            if(topKDocuments.size() > 0){
                return topKDocuments;
            }
            else if(relevanceScoreDocuments.size() > 0) {
                return relevanceScoreDocuments;
            }
            
        }
        return null;
        
        
    }
}
