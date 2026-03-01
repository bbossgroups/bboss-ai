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

import com.frameworkset.util.JsonUtil;
import com.frameworkset.util.SimpleStringUtil;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ParseException;
import org.frameworkset.spi.ai.adapter.AgentAdapter;
import org.frameworkset.spi.ai.adapter.AgentAdapterFactory;
import org.frameworkset.spi.ai.material.ReponseStoreFilePathFunction;
import org.frameworkset.spi.ai.material.StoreFilePathFunction;
import org.frameworkset.spi.ai.mcp.DataCollector;
import org.frameworkset.spi.ai.model.*;
import org.frameworkset.spi.reactor.*;
import org.frameworkset.spi.remote.http.*;
import org.frameworkset.util.annotations.HttpMethod;
import org.frameworkset.util.concurrent.BooleanWrapperInf;
import org.frameworkset.util.concurrent.NoSynBooleanWrapper;
import org.slf4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
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
    public static Flux<String> streamChatCompletion(Object message) {
        return streamChatCompletion((String)null , message);
    }

    /**
     * 创建流式调用的Flux,在指定的数据源上执行
     */
    public static Flux<String> streamChatCompletion(String poolName,Object chatMessage) {
        ClientConfiguration clientConfiguration = ClientConfiguration.getClientConfiguration(poolName);
        AgentAdapter agentAdapter = AgentAdapterFactory.getAgentAdapter(clientConfiguration,chatMessage);
        final ChatObject chatObject = agentAdapter.buildOpenAIRequestParameter(clientConfiguration,chatMessage);
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
    public static ImageEvent multimodalImageGeneration(String poolName,  ImageAgentMessage message) {
        ImageEvent imageEvent = null;       

        try {
            ClientConfiguration config = ClientConfiguration.getClientConfiguration(poolName);
            AgentAdapter agentAdapter = AgentAdapterFactory.getAgentAdapter(config,message);
            Object newmessage = agentAdapter.buildGenImageRequestParameter(config,message);
            
            Map data = HttpRequestProxy.sendJsonBody(config,newmessage,message.getGenImageCompletionsUrl(),Map.class);
            imageEvent = agentAdapter.buildGenImageResponse(config,message, data);
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
    public static ImageEvent multimodalImageGeneration( ImageAgentMessage message) {

        return multimodalImageGeneration(null,  message) ;
    }
    /**
     * 调用音频合成模型，流式生成音频，实时播放
     * @param poolName
     * @param audioAgentMessage
     * @return
     */
    public static Flux<ServerEvent> streamAudioGenerationEvent(String poolName,   AudioAgentMessage audioAgentMessage) {       
      

            return AIAgentUtil.streamChatCompletionEvent(poolName,   audioAgentMessage);
            
    }

    /**
     * 调用音频合成模型，生成音频
     * @param poolName
     * @param message
     * @return
     */
    public static AudioEvent multimodalAudioGeneration(String poolName,  AudioAgentMessage message) {
        
        ClientConfiguration config = ClientConfiguration.getClientConfiguration(poolName);
        AgentAdapter agentAdapter = AgentAdapterFactory.getAgentAdapter(config, message);
        Object newmessage = agentAdapter.buildGenAudioRequestParameter(config, message);
        AudioEvent audioEvent = null;
        try {
            StoreFilePathFunction storeFilePathFunction = message.getStoreFilePathFunction();
            if (storeFilePathFunction != null && storeFilePathFunction instanceof ReponseStoreFilePathFunction) {
                String audioUrl = HttpRequestProxy.sendJsonBody(config, newmessage, message.getGenAudioCompletionsUrl(), AIResponseUtil.buildDownAudioHttpClientResponseHandler(config, message));
                audioEvent = new AudioEvent();
                audioEvent.setAudioUrl(audioUrl);

            } else {
                Map data = HttpRequestProxy.sendJsonBody(config, newmessage, message.getGenAudioCompletionsUrl(), Map.class);
                audioEvent = agentAdapter.buildGenAudioResponse(config, message, data);

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
    public static AudioEvent multimodalAudioGeneration( AudioAgentMessage message) {

        return multimodalAudioGeneration(null,  message) ;
    }
    /**
     * 创建流式调用的Flux，使用默认数据源
     */
    public static Flux<ServerEvent> streamChatCompletionEvent( Object message) {
        return streamChatCompletionEvent((String)null , message);
    }

    public static <T> void streamChatCompletionEvent(ClientConfiguration clientConfiguration, ToolAgentMessage toolAgentMessage,FluxSink<T> sink) {
        AgentAdapter agentAdapter = AgentAdapterFactory.getAgentAdapter(clientConfiguration,toolAgentMessage);

        final ChatObject chatObject = agentAdapter.buildOpenAIRequestParameter(clientConfiguration,toolAgentMessage);
        BaseStreamDataHandler<ServerEvent> streamDataHandler = new BaseStreamDataHandler<ServerEvent>() {
 
            @Override
            public boolean handle(String line, FluxSink<ServerEvent> sink, BooleanWrapperInf firstEventTag, FluxSinkStatus fluxSinkStatus) {
                return AIResponseUtil.handleServerEventData(this.agentAdapter, this.isStream(), line, sink, firstEventTag, (BaseStreamDataBuilder)chatObject.getStreamDataBuilder(),   fluxSinkStatus);

            }
            @Override

            public boolean handleException(Object requestBody,Throwable throwable, FluxSink<ServerEvent> sink, BooleanWrapperInf firstEventTag){
                boolean result = AIResponseUtil.handleServerEventExceptionData(  throwable, sink,   firstEventTag);
                return result;
            }


        };
        streamDataHandler.setStream(chatObject.isStream());
        streamDataHandler.setAgentAdapter(agentAdapter);
        streamDataHandler.setChatObject(chatObject);
//        buildFlux(  clientConfiguration,    chatObject ,  streamDataHandler);
        executeTools(  clientConfiguration,   chatObject,   streamDataHandler,   sink);
        
    }
    
    static <T> void executeTools(ClientConfiguration clientConfiguration, ChatObject chatObject, BaseStreamDataHandler streamDataHandler, FluxSink<T> sink){
        Object data = null;
        Object message = chatObject.getMessage();

        BaseStreamDataBuilder baseStreamDataBuilder = (BaseStreamDataBuilder) streamDataHandler.getStreamDataBuilder();
        try {


            BaseURLResponseHandler responseHandler = new BaseURLResponseHandler<Void>() {
                @Override
                public Void handleResponse(ClassicHttpResponse response) throws IOException, ParseException {
                    streamDataHandler.setHttpUriRequestBase(httpUriRequestBase);
                    AIResponseUtil.handleStreamResponse(url, response, sink, streamDataHandler);
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
                streamDataHandler.streamChatCompletionEvent(clientConfiguration,chatObject,baseStreamDataBuilder,sink);

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
    /**
     * 创建流式调用的Flux,在指定的数据源上执行
     */
    public static Flux<ServerEvent> streamChatCompletionEvent(String poolName,Object chatMessage) {
 
        ClientConfiguration clientConfiguration = ClientConfiguration.getClientConfiguration(poolName);
        AgentAdapter agentAdapter = AgentAdapterFactory.getAgentAdapter(clientConfiguration,chatMessage);
         
        final ChatObject chatObject = agentAdapter.buildOpenAIRequestParameter(clientConfiguration,chatMessage);
        BaseStreamDataHandler<ServerEvent> streamDataHandler = new BaseStreamDataHandler<ServerEvent>() {
           
            @Override
            public boolean handle(String line, FluxSink<ServerEvent> sink, BooleanWrapperInf firstEventTag, FluxSinkStatus fluxSinkStatus) {
                return AIResponseUtil.handleServerEventData(this.agentAdapter, this.isStream(), line, sink, firstEventTag, (BaseStreamDataBuilder)chatObject.getStreamDataBuilder(),   fluxSinkStatus);

            }
            @Override

            public boolean handleException(Object requestBody,Throwable throwable, FluxSink<ServerEvent> sink, BooleanWrapperInf firstEventTag){
                boolean result = AIResponseUtil.handleServerEventExceptionData(  throwable, sink,   firstEventTag);
                return result;
            }

             
        };
        streamDataHandler.setStream(chatObject.isStream());
        streamDataHandler.setAgentAdapter(agentAdapter);
        streamDataHandler.setChatObject(chatObject);
        return buildFlux(  clientConfiguration,    chatObject ,  streamDataHandler);

    }
    
     
     
    
    

    /**
     * 创建流式调用的Flux,在指定的数据源上执行
     */
    public static Flux<ServerEvent> streamChatCompletionEventWithTool(String poolName,AgentMessage chatMessage,boolean toolStream) {
         
            
        if(!toolStream) {
            Boolean stream = chatMessage.getStream();
            chatMessage.setStream(false);
            ServerEvent serverEvent = AIAgentUtil.chatCompletionEvent(poolName, chatMessage, true);
            chatMessage.setStream(stream);
            List<FunctionTool> functionTools = serverEvent.getFunctionTools();
            if (functionTools != null && functionTools.size() > 0) {
                ChatAgentMessage _chatMessage = (ChatAgentMessage) chatMessage;
                _chatMessage.addAssistantSessionMessage(serverEvent);
                ToolAgentMessage toolAgentMessage = new ToolAgentMessage(_chatMessage, functionTools);
                return streamChatCompletionEvent(poolName, toolAgentMessage);

            } else {
                return buildFlux(serverEvent);
            }
        }
        else{
            Flux<ServerEvent> flux = AIAgentUtil.streamChatCompletionEvent(poolName, chatMessage);
            return flux;
//            final Flux<ServerEvent> newflux = flux
////                    .doOnNext(serverEvent -> {
//////                System.out.print(serverEvent.getData());
////            })
//            .switchMap(serverEvent -> {
////                System.out.print(serverEvent.getData());
//                List<FunctionTool> functionTools = serverEvent.getFunctionTools();
//                if(serverEvent.isDone()){
//                    functionTools = serverEvent.getFunctionTools();
//                }
//                if(serverEvent.finished()){
//                    functionTools = serverEvent.getFunctionTools();
//                    //
//                }
//                
//                if (functionTools != null && functionTools.size() > 0) {
////                    ChatAgentMessage _chatMessage = (ChatAgentMessage) chatMessage;
////                    _chatMessage.addAssistantSessionMessage(serverEvent);
////                    ToolAgentMessage toolAgentMessage = new ToolAgentMessage(_chatMessage, functionTools);
////                    Flux<ServerEvent> innerflux = streamChatCompletionEvent(poolName, toolAgentMessage);
////                    // 使用concatWith确保顺序执行：先完成当前事件，再执行工具调用
////                    return innerflux;
//                    return Flux.just(serverEvent);
//                } else {
//                    // 如果没有工具调用，直接返回当前事件
//                    return Flux.just(serverEvent);
//                }
//            }); //打印流式调用返回的问题答案片段
//                   
//            return newflux;
           
            
        }

       

    }

    private static <T> Flux<T> buildFlux(ClientConfiguration clientConfiguration,ChatObject chatObject ,BaseStreamDataHandler<T> streamDataHandler) {
        return Flux.<T>create(sink -> {
            Object data = null;
            try {
                   
                    Object message = chatObject.getMessage();

                    BaseStreamDataBuilder baseStreamDataBuilder = (BaseStreamDataBuilder) streamDataHandler.getStreamDataBuilder();
                    


                        BaseURLResponseHandler responseHandler = new BaseURLResponseHandler<Void>() {
                            @Override
                            public Void handleResponse(ClassicHttpResponse response) throws IOException, ParseException {
                                streamDataHandler.setHttpUriRequestBase(httpUriRequestBase);
                                AIResponseUtil.handleStreamResponse(url, response, sink, streamDataHandler);
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
                            streamDataHandler.streamChatCompletionEvent(clientConfiguration,chatObject,baseStreamDataBuilder,sink);
                            
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
                        sink.complete();
                    }
                }, FluxSink.OverflowStrategy.BUFFER)
                .subscribeOn(Schedulers.boundedElastic()) // 在弹性线程池中执行阻塞IO
                .timeout(Duration.ofSeconds(60)) // 设置超时
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
                .timeout(Duration.ofSeconds(60)) // 设置超时
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
						
//						Map header = new LinkedHashMap();
//						
//						header.put("Accept", "text/event-stream");
//						header.put("Cache-Control", "no-cache");
						 
						HttpRequestProxy.httpGet(clientConfiguration, url, responseHandler);
						 

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
				.timeout(Duration.ofSeconds(60)) // 设置超时
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
                .timeout(Duration.ofSeconds(60)) // 设置超时
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
    public static ServerEvent imageParser(Object message) {
        return imageParser(  (String)null, message);


    }

    /**
     * 同步调用模型服务，返回问答内容
     */
    public static ServerEvent imageParser(String poolName,Object message) {
        return chatCompletionEvent(poolName,message);
        


    }
    
    /**
     * 同步调用模型服务，返回问答内容
     */
    public static ServerEvent videoParser(String poolName,VideoVLAgentMessage message) {
        return chatCompletionEvent(poolName,message);



    }
    /**
     * 同步调用模型服务，返回问答内容
     */
    public static ServerEvent audioParser(String poolName,Object message) {
        return chatCompletionEvent(poolName,message);



    }
    /**
     * 同步调用模型服务，返回问答内容
     */
    public static ServerEvent chatCompletionEvent(Object message) {
        return chatCompletionEvent(  (String)null,  message);


    }

    /**
     * 同步调用模型服务，返回问答内容
     */
    public static ServerEvent chatCompletionEvent(String poolName,Object chatMessage) {
        return chatCompletionEvent( poolName, chatMessage,false);


    }

    public static ServerEvent chatCompletionEvent(String poolName,Object chatMessage,boolean fromStreamChat) {
        ClientConfiguration config = ClientConfiguration.getClientConfiguration(poolName);
        AgentAdapter agentAdapter = AgentAdapterFactory.getAgentAdapter(config,chatMessage);
        ChatObject chatObject = agentAdapter.buildOpenAIRequestParameter(config,chatMessage);
        Object message = chatObject.getMessage();
        String data = null;
        ServerEvent serverEvent = null;
        BaseURLResponseHandler<ServerEvent> responseHandler = new BaseURLResponseHandler<ServerEvent>() {
            @Override
            public ServerEvent handleResponse(ClassicHttpResponse response) throws IOException, ParseException {
                return AIResponseUtil.handleChatResponse(agentAdapter, chatObject.getCompletionsUrl(), response, chatObject.getStreamDataBuilder());
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
        if(fromStreamChat)
            return serverEvent;
        List<FunctionTool> functionTools = serverEvent.getFunctionTools();
        if(functionTools != null && functionTools.size() > 0){
            ChatAgentMessage _chatMessage = (ChatAgentMessage) chatMessage;
            _chatMessage.addAssistantSessionMessage(serverEvent );
            ToolAgentMessage toolAgentMessage = new ToolAgentMessage(_chatMessage,functionTools);
            return chatCompletionEvent(  poolName,toolAgentMessage);
            /**
             FunctionTool tool = functionTools.get(0);
             String toolId = tool.getId();
             String functionName = tool.getFunctionName();
             FunctionCall functionCall = chatMessage.getFunctionCall(functionName);
             try {
             Object result = functionCall.call(tool);
             Map<String,Object> toolMessage =MessageBuilder.buildToolMessage(JsonUtil.object2json(result),toolId);
             } catch (Exception e) {
             throw new RuntimeException(e);
             }
             */
        }
        else {
            return serverEvent;
        }


    }
    public static <T> Flux<T> streamChatCompletion(Object message,BaseStreamDataHandler<T> streamDataHandler){
        return streamChatCompletion((String)null ,   message, streamDataHandler);
    }

    /**
     * 创建流式调用的Flux,在指定的数据源上执行
     */
    public static <T> Flux<T> streamChatCompletion(String poolName,Object chatMessage,BaseStreamDataHandler<T> streamDataHandler) {
        ClientConfiguration clientConfiguration = ClientConfiguration.getClientConfiguration(poolName);
        AgentAdapter agentAdapter = AgentAdapterFactory.getAgentAdapter(clientConfiguration,chatMessage);
        final ChatObject chatObject = agentAdapter.buildOpenAIRequestParameter(clientConfiguration,chatMessage);
        streamDataHandler.setStream(chatObject.isStream());
        streamDataHandler.setAgentAdapter(agentAdapter);
        streamDataHandler.setChatObject(chatObject);
        return buildFlux(  clientConfiguration,    chatObject ,  streamDataHandler);
    }

    public static VideoTask submitVideoTask(String maasName,  VideoAgentMessage videoAgentMessage) {
        ClientConfiguration clientConfiguration = ClientConfiguration.getClientConfiguration(maasName);
        AgentAdapter agentAdapter = AgentAdapterFactory.getAgentAdapter(clientConfiguration,videoAgentMessage);
        Object params = agentAdapter.buildVideoRequestParameter(clientConfiguration,videoAgentMessage);
        Map taskInfo = HttpRequestProxy.sendJsonBody(maasName,params,videoAgentMessage.getSubmitVideoTaskUrl(),videoAgentMessage.getHeaders(),Map.class);
        VideoTask task = agentAdapter.buildVideoResponseTask(clientConfiguration,videoAgentMessage,  taskInfo);
        
        return task;
    }

    public static VideoTask submitVideoTask( VideoAgentMessage videoAgentMessage) {
        return submitVideoTask(null,   videoAgentMessage) ;
    }
    
    public static VideoGenResult getVideoTaskResult(String maasName, VideoStoreAgentMessage videoStoreAgentMessage) {
        ClientConfiguration clientConfiguration = ClientConfiguration.getClientConfiguration(maasName);
        AgentAdapter agentAdapter = AgentAdapterFactory.getAgentAdapter(clientConfiguration,null);
        agentAdapter._buildGetVideoResultRquestMap(videoStoreAgentMessage,clientConfiguration);
        Map taskInfo = HttpRequestProxy.httpGetforObject(maasName,videoStoreAgentMessage.getVideoTaskResultUrl(),Map.class);
        VideoGenResult videoGenResult = agentAdapter.buildVideoGenResult(clientConfiguration,videoStoreAgentMessage,taskInfo);
        
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
	
 
}
