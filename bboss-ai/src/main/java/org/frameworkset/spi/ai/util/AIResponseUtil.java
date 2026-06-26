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
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.frameworkset.spi.ai.adapter.AgentAdapter;
import org.frameworkset.spi.ai.callback.ChatContext;
import org.frameworkset.spi.ai.callback.ChatStreamCallback;
import org.frameworkset.spi.ai.material.DownFileHttpClientResponseHandler;
import org.frameworkset.spi.ai.material.DownImageBase64HttpClientResponseHandler;
import org.frameworkset.spi.ai.material.DownVideoImageFileHttpClientResponseHandler;
import org.frameworkset.spi.ai.model.*;
import org.frameworkset.spi.reactor.*;
import org.frameworkset.spi.remote.http.ClientConfiguration;
import org.frameworkset.spi.remote.http.proxy.BBossEntityUtils;
import org.frameworkset.util.concurrent.BooleanWrapperInf;
import org.frameworkset.util.concurrent.NoSynBooleanWrapper;
import reactor.core.publisher.FluxSink;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author biaoping.yin
 * @Date 2026/1/11
 */
public class AIResponseUtil {
    private static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AIResponseUtil.class);
 


  public static HttpClientResponseHandler<String>  buildDownImageHttpClientResponseHandler(ClientConfiguration config, ImageAgentMessage imageAgentMessage,StoreChatObject storeChatObject, String imageUrl){
      String type  = storeChatObject.getStoreImageType();
      HttpClientResponseHandler<String> handler = null;
      if(type == null || type.equals(AIConstants.STORETYPE_BASE64) || type.equals(AIConstants.STORETYPE_URL)){
          handler = new DownImageBase64HttpClientResponseHandler();
      }
      else if(type.equals(AIConstants.STORETYPE_FILE)){
          handler = new DownFileHttpClientResponseHandler( config,imageAgentMessage,  storeChatObject,  imageUrl);
      }
      if(handler == null){
          logger.warn("unsupport StoreImageType:{}", type);
          throw new AIRuntimeException("unsupport StoreImageType:"+type);
      }
      return handler;
      
  }

    public static HttpClientResponseHandler<String>  buildDownVideoImageHttpClientResponseHandler(ClientConfiguration config, VideoStoreAgentMessage videoStoreAgentMessage, StoreChatObject storeChatObject,String imageUrl){
        String type  = storeChatObject.getStoreVideoType();
        HttpClientResponseHandler<String> handler = null;
        if(type == null || type.equals(AIConstants.STORETYPE_BASE64) || type.equals(AIConstants.STORETYPE_URL)){
            handler = new DownImageBase64HttpClientResponseHandler();
        }
        else if(type.equals(AIConstants.STORETYPE_FILE)){
            handler = new DownVideoImageFileHttpClientResponseHandler( config,videoStoreAgentMessage, storeChatObject,  imageUrl);
        }
        if(handler == null){
            logger.warn("unsupport StoreImageType:{}", type);
            throw new AIRuntimeException("unsupport StoreImageType:"+type);
        }
        return handler;

    }

    public static HttpClientResponseHandler<String>  buildDownAudioHttpClientResponseHandler(ClientConfiguration config, AudioAgentMessage audioAgentMessage, StoreChatObject storeChatObject,String audioUrl){
         
        return new DownFileHttpClientResponseHandler( config,audioAgentMessage, storeChatObject, audioUrl);
        

    }

    public static HttpClientResponseHandler<String>  buildDownVideoHttpClientResponseHandler(ClientConfiguration config, 
                                                                                             VideoStoreAgentMessage videoStoreAgentMessage,StoreChatObject storeChatObject, String videoUrl){

        return new DownFileHttpClientResponseHandler( config,videoStoreAgentMessage, storeChatObject, videoUrl);


    }

    public static HttpClientResponseHandler<String>  buildDownAudioHttpClientResponseHandler(ClientConfiguration config, AudioAgentMessage audioAgentMessage,StoreChatObject storeChatObject){

        return new DownFileHttpClientResponseHandler( config,audioAgentMessage, storeChatObject, (String)null);


    }

  

    public static boolean handleStringExceptionData(Throwable throwable,FluxSink<String> sink, BooleanWrapperInf firstEventTag){
        if(logger.isWarnEnabled()) {
            logger.warn("服务端异常：", throwable);
        }
        if(firstEventTag.get()) {
            firstEventTag.set(false);
        }
        String error = SimpleStringUtil.exceptionToString(throwable);
        sink.next(error);
//        sink.complete();
        return true;

    }

    public static boolean handleServerEventExceptionData(Throwable throwable,FluxSink<ServerEvent> sink, BaseStreamDataBuilder streamDataBuilder,BooleanWrapperInf firstEventTag){
        if(logger.isWarnEnabled()) {
            logger.warn("服务端异常：", throwable);
        }

        String error = SimpleStringUtil.exceptionToString(throwable);
        ServerEvent serverEvent = new ServerEvent();
        serverEvent.setTokenMetrics(streamDataBuilder.getTokenMetrics());
        ChatObject chatObject = streamDataBuilder.getChatObject();        
        serverEvent.setAgent(chatObject.getAgent());
        if(firstEventTag.get()) {
            firstEventTag.set(false);
            serverEvent.setFirst(true);
        }
        if(streamDataBuilder != null) {
            serverEvent.setToolCallResponse(chatObject.isToolCall());
        }
        else{
            serverEvent.setToolCallResponse(false);
        }
        serverEvent.setData(error);
        serverEvent.setType(ServerEvent.TYPE_ERROR);
        sink.next(serverEvent);

        serverEvent = new ServerEvent();
        serverEvent.setAgent(chatObject.getAgent());
        serverEvent.setDone( true);
        serverEvent.setTokenMetrics(streamDataBuilder.getTokenMetrics());
        sink.next(serverEvent);
//        sink.complete();
        return true;

    }

    /**
     * 处理音频识别流数据
     * @param data
     * @return
     */
    public static StreamData parseAudioStreamContentFromData(AgentAdapter agentAdapter,StreamDataBuilder streamDataBuilder,String data){
        try {
            Map map = SimpleStringUtil.json2Object(data, Map.class);
            Map output = (Map) map.get("output");
            
            Object choices_ = output.get("choices");
            if (choices_ != null ) {
                if (choices_ instanceof List) {
                    List<Map> choices = (List<Map>) choices_;
                    if (choices.size() > 0) {
                        Map choice = choices.get(0);
                        Map message = (Map) choice.get("message");

                        if(message != null) {
                            List<Map> content_ = (List) message.get("content");

                            String content = content_ != null && content_.size() > 0? (String) content_.get(0).get("text"):null;
                            List<Map> reasoning_content_ = (List) message.get("reasoning_content");
                            String reasoning_content = reasoning_content_ != null && reasoning_content_.size() > 0?(String) reasoning_content_.get(0).get("text"):null;
                            String finishReason = (String) choice.get("finish_reason");
                            if (SimpleStringUtil.isNotEmpty(reasoning_content)) {
                                return new StreamData(ServerEvent.REASONING_CONTENT, reasoning_content, finishReason);
                            } else {
                                return new StreamData(ServerEvent.CONTENT, content, finishReason);
                            }
                        }
                        else{
                            if(logger.isDebugEnabled())
                                logger.debug("choices list message null");
                        }

                    }
                    else {
                        if(logger.isDebugEnabled())
                            logger.debug("choices list size is 0");
                    }

                }
                else{
                    if (logger.isDebugEnabled())
                        logger.debug("choices is not list:{}");
                }
            }

        } catch (Exception e) {
            throw new ReactorCallException("ParseAudioStreamContentFromData failed:",e);
        }
        return null;
    }

    /**
     * 处理智谱音频识别流数据
     * stream:
     * {"id":"2026012723020247e5f256dc1248d0","created":1769526122,"model":"glm-asr-2512","delta":"诗歌","type":"transcript.text.delta"}
     * 同步：
     * @param data
     * @return
     */
    public static StreamData parseZhipuAudioStreamContentFromData(StreamDataBuilder streamDataBuilder,String data){
        try {
            Map output = SimpleStringUtil.json2Object(data, Map.class);

            if(streamDataBuilder.getChatObject().isStream()) {
                String delta = (String) output.get("delta");

                if (delta != null) {

                    return new StreamData(ServerEvent.CONTENT, delta, null);

                } else {
                    String finishReason = (String) output.get("type");
                    if (finishReason != null) {
                        if (finishReason.equals("transcript.text.done")) {
                            return new StreamData(ServerEvent.CONTENT, null, "stop", true);
                        } else {
                            logger.info("audio data is empty:{},finishReason:{}", data, finishReason);
                            //                        return new StreamData(ServerEvent.CONTENT, audioData, finishReason);
                        }
                    } else {
//                {"error":{"code":"1214","message":"音色id不存在"}}
                        Map error = (Map) output.get("error");
                        if (error != null) {
                            throw new ReactorCallException("ParseAudioStreamContentFromData failed:" + data);
                        } else {
                            logger.info("audio data:", data);
                        }
                    }
                }
            }
            else{
                String text = (String) output.get("text");
                return new StreamData(ServerEvent.CONTENT, text, "stop",true);                
            }
           

                     

        }catch (ReactorCallException e) {
            throw e;
        } catch (Exception e) {
            throw new ReactorCallException("ParseAudioStreamContentFromData failed:",e);
        }
        return null;
    }

    /**
     * 处理音频识别流数据
     * {"output":{"audio":{"data":"xxxx",
     *   "expires_at":1769158890,
     *   "id":"audio_66356352-8808-49bd-9c9c-d0283a3e2eb1"},
     *   "finish_reason":"null"},
     *   "usage":{"characters":53},
     *   "request_id":"66356352-8808-49bd-9c9c-d0283a3e2eb1"}
     * @param data
     * @return
     */
    public static StreamData parseQianwenAudioGenStreamContentFromData(AgentAdapter agentAdapter,String data){
        try {
            Map _data = SimpleStringUtil.json2Object(data,Map.class);
            Map output = (Map)_data.get("output");
            Map audio = (Map)output.get("audio");
            String finishReason = (String)output.get("finish_reason");
            if (audio != null ) {
                String audioData = (String)audio.get("data");
                if(SimpleStringUtil.isNotEmpty(audioData)) {
                    return new StreamData(ServerEvent.CONTENT, audioData, finishReason);
                }
                else{
                    if(finishReason != null && finishReason.equals("stop")) {
                        String url = (String)audio.get("url");
                        return new StreamData(ServerEvent.CONTENT, audioData,url, finishReason, true);
                    }
                    else {
                        logger.info("audio data is empty:{},finishReason:{}", audioData, finishReason);
//                        return new StreamData(ServerEvent.CONTENT, audioData, finishReason);
                    }
                }
            }
            else{
                logger.info("audio data is null.");
            }

        } catch (Exception e) {
            throw new ReactorCallException("ParseAudioStreamContentFromData failed:",e);
        }
        return null;
    }

    /**
     * 处理音频识别流数据
     * {"id":"2026012618501535d155fd2f884b93","created":1769424615,"model":"glm-tts",
     * "choices":[{"index":0,"delta":{"role":"assistant","content":"","return_sample_rate":24000,"return_format":"pcm"}}]}
     * @param data
     * @return
     */
    public static StreamData parseZhipuAudioGenStreamContentFromData(String data){
        try {
            Map _data = SimpleStringUtil.json2Object(data,Map.class);
            List<Map> choices = (List<Map>)_data.get("choices");
            if(choices != null && choices.size() > 0){
                Map choice = choices.get(0);
                String finishReason = (String)choice.get("finish_reason");
                Map delta = (Map)choice.get("delta");
                if(delta == null){
                    if(finishReason != null && finishReason.equals("stop")) {

                        return new StreamData(ServerEvent.CONTENT, (String)null,(String)null, finishReason, true);
                    }
                    else {
                        logger.info("delta is null:{}", data);

                        return null;
                    }
                }
                String audioData = (String)delta.get("content");
                if(SimpleStringUtil.isNotEmpty(audioData)) {
                    return new StreamData(ServerEvent.CONTENT, audioData, finishReason);
                }
                else{
                    if(finishReason != null && finishReason.equals("stop")) {
                        
                        return new StreamData(ServerEvent.CONTENT, audioData,(String)null, finishReason, true);
                    }
                    else {
                        logger.info("audio data is empty:{},finishReason:{}", audioData, finishReason);
//                        
                    }
                }
            }
          
            else{
//                {"error":{"code":"1214","message":"音色id不存在"}}
                Map error = (Map)_data.get("error");
                if(error != null) {
                    throw new ReactorCallException("ParseAudioStreamContentFromData failed:"+data);
                }
                else {
                    logger.info("audio data:", data);
                }
            }

        }  catch (ReactorCallException e) {
            throw e;
        }catch (Exception e) {
            throw new ReactorCallException("ParseAudioStreamContentFromData failed:",e);
        }
        return null;
    }

    /**
     * 语音识别：data:{"output":{"choices":[{"message":{"annotations":[{"type":"audio_info","language":"zh","emotion":"neutral"}],"content":[{"text":"欢迎与"}],"role":"assistant"},"finish_reason":"null"}]},"usage":{"output_tokens_details":{"text_tokens":6},"input_tokens_details":{"text_tokens":16},"seconds":1},"request_id":"e84128d5-4bae-4e7e-91ab-6fb33504d2e3"}
     * LLM和图像识别：data: {"id":"ccf32be6-ad2f-4658-963a-fc3c22346e6b","object":"chat.completion.chunk","created":1761725211,"model":"deepseek-reasoner","system_fingerprint":"fp_ffc7281d48_prod0820_fp8_kvcache","choices":[{"index":0,"delta":{"content":null,"reasoning_content":"在"},"logprobs":null,"finish_reason":null}]}
     * @param data
     * @return
     */
    public static StreamData parseStreamContentFromData(AgentAdapter agentAdapter,BaseStreamDataBuilder streamDataBuilder,String data) {
        try {
            Map map = JsonUtil.json2Object(data,Map.class);
            String model = (String) map.get("model");
            Map usage = (Map) map.get("usage");
            TokenMetrics tokenMetrics = null;
            if(usage != null)
            {
                tokenMetrics = streamDataBuilder.buildTokenMetrics(usage);
                tokenMetrics.setModel(model);
                tokenMetrics.setMaas(streamDataBuilder.getMaas());
                tokenMetrics.setStartTime(streamDataBuilder.getStartTime());
            }
            
            Object choices_ = map.get("choices");
     
            if (choices_ != null ) {
                if (choices_ instanceof List) {
                    List<Map> choices = (List<Map>) choices_;
                    if (choices.size() > 0) {
                        Map choice = choices.get(0);
                        String finishReason = (String) choice.get("finish_reason");
                       if(!streamDataBuilder.isToolCall(finishReason)) {
                           Map delta = (Map) choice.get("delta");
                           if (delta != null) {
 
                               String reasoning_content = (String) delta.get("reasoning_content");
                               String content = (String) delta.get("content");
                               Object tool_call = delta.get("tool_calls");
                               if(tokenMetrics != null){
                                   tokenMetrics.setEndTime(System.currentTimeMillis());
                               }
                               if(tool_call != null){
                                   return streamDataBuilder.functionToolsChunk((List<Map>) tool_call, finishReason).setStreamTokenMetrics(tokenMetrics);
                               }
                                
                               else if (SimpleStringUtil.isNotEmpty(reasoning_content)) {
                                   return new StreamData(ServerEvent.REASONING_CONTENT, reasoning_content, finishReason).setStreamTokenMetrics(tokenMetrics);
                               } else {
                                   return new StreamData(ServerEvent.CONTENT, content, finishReason).setStreamTokenMetrics(tokenMetrics);
                               }

                           } else {
                               Map message = (Map) choice.get("message");
                               if (message != null) {
                                   String reasoning_content = (String) message.get("reasoning_content");
                                   String content = (String) message.get("content");
                                   if(tokenMetrics != null){
                                       tokenMetrics.setEndTime(System.currentTimeMillis());
                                   }
                                   if (SimpleStringUtil.isNotEmpty(reasoning_content)) {
                                       return new StreamData(ServerEvent.REASONING_CONTENT, reasoning_content, finishReason).setStreamTokenMetrics(tokenMetrics);
                                   } else {
                                       return new StreamData(ServerEvent.CONTENT, content, finishReason).setStreamTokenMetrics(tokenMetrics);
                                   }
                               }
                               if (logger.isDebugEnabled())
                                   logger.debug("choices message null: {}", data);
                           }
                       }
                       else{
                           Map delta = (Map) choice.get("delta");
                           if (delta != null) {
                               Object tool_call = delta.get("tool_calls");
                               if(tokenMetrics != null){
                                   tokenMetrics.setEndTime(System.currentTimeMillis());
                               }
                               if(tool_call != null){
                                   List<Map> tool_call_ = (List<Map>) tool_call;
                                   
                                   return new StreamData( tool_call_.get(0), finishReason).setStreamTokenMetrics(tokenMetrics)   ;
//                                   return streamDataBuilder.functionToolsChunk((List<Map>) tool_call, finishReason);
                               }
                               else{
                                     return new StreamData( (List<FunctionTool>)null,(List<Map>)null, finishReason).setStreamTokenMetrics(tokenMetrics);
                               }
                           }
                           else {
                               Map message = (Map) choice.get("message");
                               if (message != null) {
                                   StreamData streamData = streamDataBuilder.functionTools((List<Map>) message.get("tool_calls"), finishReason);
                                   if (streamData != null) {
                                       String reasoning_content = (String) message.get("reasoning_content");
                                       if(tokenMetrics != null){
                                           tokenMetrics.setEndTime(System.currentTimeMillis());
                                       }
                                       if (reasoning_content == null) {
                                           return streamData
                                                   .setContent((String) message.get("content"))
                                                   .setRole((String) message.get("role")).setStreamTokenMetrics(tokenMetrics);
                                       } else {
                                           return streamData
                                                   .setContent((String) message.get("content"))
                                                   .setReasoningContent(reasoning_content)
                                                   .setRole((String) message.get("role")).setStreamTokenMetrics(tokenMetrics);
                                       }
                                   } else {
                                       if (logger.isDebugEnabled())
                                           logger.debug("choice message tool_calls null: {}", data);
                                   }

                               } else {
                                   if (logger.isDebugEnabled())
                                       logger.debug("choice message null: {}", data);
                               }
                           }
                       }
                    }
                    else {
                        if(logger.isDebugEnabled())
                            logger.debug("choices list size is 0: {}",data);
                    }

                }
                else{
                    if (logger.isDebugEnabled())
                        logger.debug("choices is not list:{}", data);
                }
            }
            else {
                if(tokenMetrics != null){
                    tokenMetrics.setEndTime(System.currentTimeMillis());
                }
                StreamData streamData = agentAdapter.buildErrorStreamData(map,tokenMetrics);
//                String code =  (String)map.get("code");
//                String message = (String) map.get("message");
//                
                if(streamData != null) {
                    return 
                            streamData;
                   
//                    return new StreamData(ServerEvent.CONTENT, message, code).setStreamTokenMetrics(tokenMetrics);
                }
                else {
                    if(logger.isDebugEnabled())
                        logger.debug("-----------no choices:{}",data);
                }

            }
            if(tokenMetrics != null){
                tokenMetrics.setEndTime(System.currentTimeMillis());
                streamDataBuilder.setTokenMetrics(tokenMetrics);
            }
        } catch (Exception e) {
            throw new ReactorCallException(data,e);
        }
        return null;
    }

    /**
     * 语音识别：data:{"output":{"choices":[{"message":{"annotations":[{"type":"audio_info","language":"zh","emotion":"neutral"}],"content":[{"text":"欢迎与"}],"role":"assistant"},"finish_reason":"null"}]},"usage":{"output_tokens_details":{"text_tokens":6},"input_tokens_details":{"text_tokens":16},"seconds":1},"request_id":"e84128d5-4bae-4e7e-91ab-6fb33504d2e3"}
     * LLM和图像识别：
     * data:{"parts":[{"role":"assistant","id":"9db6ab71-b12b-4426-a631-1d92757194bc","content":{"delta":"一只","type":"text","text":"一只","status":"init"},"status":"init"}]}
     *
     * data:{"parts":[{"role":"assistant","id":"9db6ab71-b12b-4426-a631-1d92757194bc","content":{"delta":"白色的","type":"text","text":"一只白色的","status":"init"},"status":"init"}]}
     *
     * data:{"parts":[{"role":"assistant","id":"9db6ab71-b12b-4426-a631-1d92757194bc","content":{"delta":"狗","type":"text","text":"一只白色的狗","status":"init"},"status":"init"}]}
     *
     * ......
     * ......
     * data:{"parts":[{"role":"assistant","id":"9db6ab71-b12b-4426-a631-1d92757194bc","content":{"delta":" ","type":"text","text":"一只白色的狗坐在一块石头上，背景是草地。 ","status":"init"},"status":"init"}]}
     *
     * data:{"usage":{"completion_tokens":12,"prompt_tokens":29,"total_tokens":41},"parts":[{"role":"assistant","id":"9db6ab71-b12b-4426-a631-1d92757194bc","content":{"delta":"[EOS]","history":[{"input":" Ref OCR: [] 描述图片","upload_img":"iVBORw0KGgoAAAANSUhEUgAAAwYAAAI5CAIAAACU/7pPAAAACXBIWXMAABJ0AAASdAHeZh94AAAgAElEQVR42lS8948kTXrn9657d9+KcYcW7WaMYVcLBrpG3z9SioZxHE8Cx4alQ2zfBJHIol4SM7H2YBBACAqlhm4ICSQCPF/lGU5mpamHpoAAAAASUVORK5CYII=","label":"3-2","revise_prompt":"一只白色的狗坐在一块石头上，背景是草地。 "}],"type":"text","text":"一只白色的狗坐在一块石头上，背景是草地。 ","status":"finish"},"status":"finish"}],"finished":"Stop","completionMsg":{"modelId":"LLMImage2Text","modelVersion":"4"}}
     * @param data
     * @return
     */
    public static StreamData parseJiutianImageParserStreamContentFromData(StreamDataBuilder streamDataBuilder,String data) {
        try {
            Map map = SimpleStringUtil.json2Object(data,Map.class);
            Object choices_ = map.get("parts");
            String finishReason = (String) map.get("finished");
      
            if (choices_ != null ) {
                if (choices_ instanceof List) {
                    List<Map> choices = (List<Map>) choices_;
                    if (choices.size() > 0) {
                        Map choice = choices.get(0);
                      
                        Map content = (Map) choice.get("content");
                        if (content != null) {
//                            String content = (String)delta.get("content");
//                            return content;
                            String reasoning_content = (String)content.get("reasoning_content");
                            String delta = (String) content.get("delta");
                            if(SimpleStringUtil.isNotEmpty(reasoning_content)){
                                return new StreamData(ServerEvent.REASONING_CONTENT,reasoning_content,finishReason);
                            }
                            else{
                                if(!delta.equals("[EOS]")) {
                                    return new StreamData(ServerEvent.CONTENT, delta, finishReason);
                                }
                                else{
                                    return new StreamData(ServerEvent.CONTENT, delta, finishReason,true);
                                }
                            }

                        }
                        else{
                            Map message = (Map) choice.get("message");
                            if(message != null) {
                                String reasoning_content = (String)message.get("reasoning_content");
                                String delta = (String) message.get("delta");
                                if(SimpleStringUtil.isNotEmpty(reasoning_content)){
                                    return new StreamData(ServerEvent.REASONING_CONTENT,reasoning_content,finishReason);
                                }
                                else{
                                    return new StreamData(ServerEvent.CONTENT,delta,finishReason);
                                }
                            }
                            if(logger.isDebugEnabled())
                                logger.debug("choices list delta null: {}",data);
                        }
                    }
                    else {
                        if(logger.isDebugEnabled())
                            logger.debug("choices list size is 0: {}",data);
                    }

                }
                else{
                    if (logger.isDebugEnabled())
                        logger.debug("choices is not list:{}", data);
                }
            }
            else {
                Object code =  map.get("code");
//                String message = (String) map.get("message");
                Map result = (Map) map.get("result");
                String message = (String) result.get("text");
                if(code != null) {
                    
                    return new StreamData(ServerEvent.CONTENT, message, String.valueOf(code));
                }
                else {
                    if(logger.isDebugEnabled())
                        logger.debug("-----------no choices:{}",data);
                }

            }
        } catch (Exception e) {
            throw new ReactorCallException(data,e);
        }
        return null;
    }

    private static <T> void processStreamResponse(ClassicHttpResponse response, FluxSink<T> sink, BaseStreamDataHandler<T> streamDataHandler,DisposeEventHandler disposeEventHandler) throws IOException {

        FluxSinkStatus fluxSinkStatus = null;
        try  {
            fluxSinkStatus = new FluxSinkStatus(response,streamDataHandler.getHttpUriRequestBase());
			
//            // 添加取消监听器
//            sink.onCancel(() -> {
//                // 当订阅被取消时执行
//                logger.info("Subscription cancelled");
//                fluxSinkStatus.cancel();
//                // 执行清理工作
//            });
            final FluxSinkStatus fluxSinkStatus_ = fluxSinkStatus;

            disposeEventHandler.addFluxSinkStatus(fluxSinkStatus);
            /*
			if(!disposeEventHandler.containFluxSinkStatus()) {
				disposeEventHandler.addFluxSinkStatus(fluxSinkStatus);
				sink.onDispose(() -> {
					// 当 sink 被处置时执行（包括正常完成、错误和取消）
					if (logger.isDebugEnabled()) {
						logger.debug("Sink disposed");
					}
//					fluxSinkStatus_.dispose();
//					// 执行清理工作
//					fluxSinkStatus_.releaseResources();
					disposeEventHandler.dispose();
					
				});
			}
			else{
				disposeEventHandler.addFluxSinkStatus(fluxSinkStatus);
			}
			*/
             
              
            String line;
            boolean needBreak = false;
            BooleanWrapperInf firstEventTag = new NoSynBooleanWrapper(true);
            while (!sink.isCancelled() && (line = fluxSinkStatus.readLine()) != null ) {
                if(fluxSinkStatus.isDispose()){
                    break;
                }
                needBreak = streamDataHandler.handle(line, sink,   firstEventTag,fluxSinkStatus);
                if(needBreak ){
                    
                    break;
                }


            }
            if(!needBreak){

                BaseStreamDataBuilder streamDataBuilder = (BaseStreamDataBuilder)streamDataHandler.getStreamDataBuilder();
                if(streamDataBuilder.getToolCallsStreamData() ==  null) {
                    streamDataHandler.handle(streamDataHandler.getDoneData(), sink,   firstEventTag,fluxSinkStatus);
                     
                }
            }
        }
        finally {
            fluxSinkStatus.releaseResources();
			disposeEventHandler.removeFluxSinkStatus(fluxSinkStatus.getSeqNo());
        }
    }

    private static void processStreamResponse(ClassicHttpResponse response, 
                                              FluxSink<String> sink,
                                              CommonStreamDataHandler<String> streamDataHandler) throws IOException {

        FluxSinkStatus fluxSinkStatus = null;
        try  {
            fluxSinkStatus = new FluxSinkStatus(response,streamDataHandler.getHttpUriRequestBase());
            FluxSinkStatus _fluxSinkStatus = fluxSinkStatus;
            // 添加取消监听器
            sink
//                    .onCancel(() -> {
//                // 当订阅被取消时执行
//                logger.info("Sink cancelled");
//                _fluxSinkStatus.dispose();
//                _fluxSinkStatus.releaseResources();
//                // 执行清理工作
//            })
                    .onDispose(() -> {
                // 当 sink 被处置时执行（包括正常完成、错误和取消）
                if(logger.isDebugEnabled()) {
                    logger.debug("Sink disposed");
                }
                _fluxSinkStatus.dispose();
                _fluxSinkStatus.releaseResources();
                // 执行清理工作
            });

            String line;
           
            while ( !sink.isCancelled() && (line = fluxSinkStatus.readLine()) != null ) {
                if(fluxSinkStatus.isDispose()){
                    break;
                }
                sink.next(line);
//                logger.info(line);
                
                


            }
           
             
        }
        finally {
            fluxSinkStatus.releaseResources();
        }
    }
	
	private static void processStreamResponse(ClassicHttpResponse response,
											   DataCollector dataCollector,
											  CommonStreamDataHandler<String> streamDataHandler) throws IOException {
		
		FluxSinkStatus fluxSinkStatus = null;
		try  {
			fluxSinkStatus = new FluxSinkStatus(response,streamDataHandler.getHttpUriRequestBase());
//            // 添加取消监听器
//            sink.onCancel(() -> {
//                // 当订阅被取消时执行
//                logger.info("Subscription cancelled");
//                fluxSinkStatus.cancel();
//                // 执行清理工作
//            });
			
			String line;
            
			do{
                line = fluxSinkStatus.readLine();
                
                if(line != null) {
                    dataCollector.collector(line);
                }
				else{
					break;
				}
            }while (true);
//			while (  (line = fluxSinkStatus.readLine()) != null ) {
//				if(fluxSinkStatus.isDispose()){
//					break;
//				}
//				dataCollector.collector(line);
////                logger.info(line);
//				
//				
//				
//				
//			}
			
			
		}
		finally {
			fluxSinkStatus.releaseResources();
		}
	}


    public static ServerEvent handleChatResponse(AgentAdapter agentAdapter,String url, ClassicHttpResponse response, StreamDataBuilder streamDataBuilder)
            throws IOException, ParseException {

        int status = response.getCode();

        if (org.frameworkset.spi.remote.http.ResponseUtil.isHttpStatusOK( status)) {
            HttpEntity entity = response.getEntity();
            String line = entity != null ? BBossEntityUtils.toString(entity) : null;
            if(line == null || line.equals("")){
                return null;
            }
            return handleServerEventData( agentAdapter, line,   streamDataBuilder);
        } else {
            HttpEntity entity = response.getEntity();
            if (entity != null ) {
                if (logger.isDebugEnabled()) {
                    logger.debug(new StringBuilder().append("Request url:").append(url).append(",status:").append(status).toString());
                }
                throw new ReactorCallException(new StringBuilder().append("Request url:")
                        .append(url).append(",error,").append("status=").append(status).append(":").append(EntityUtils.toString(entity)).toString());
//                sink.error(new ReactorCallException(new StringBuilder().append("Request url:").append(url).append(",error,").append("status=").append(status).append(":").append(EntityUtils.toString(entity)).toString()));
            }
            else {
                throw new ReactorCallException(new StringBuilder().append("Request url:").append(url).append(",Unexpected response status: ").append(status).toString());
//                sink.error(new ReactorCallException(new StringBuilder().append("Request url:").append(url).append(",Unexpected response status: ").append(status).toString()));
            }
        }
    }
    public static <T> void handleStreamResponse(String url, ClassicHttpResponse response,
                                                FluxSink<T> sink, StreamDataHandler<T> streamDataHandler,DisposeEventHandler disposeEventHandler)
            throws IOException, ParseException {

        int status = response.getCode();

        if (org.frameworkset.spi.remote.http.ResponseUtil.isHttpStatusOK( status)) {
            processStreamResponse(response, sink,(BaseStreamDataHandler<T>) streamDataHandler,disposeEventHandler);
        } else {
            HttpEntity entity = response.getEntity();
            String data = SimpleStringUtil.object2jsonPretty(streamDataHandler.getChatObject().getMessage());
            if (entity != null ) {
                if (logger.isDebugEnabled()) {
                    logger.debug(new StringBuilder().append("Request url:").append(url).append(",status:").append(status).toString());
                }
                throw new ReactorCallException(new StringBuilder().append("Request url:")
                        .append(url).append(",error,").append("status=")
                        .append(status).append(":")
                        .append(EntityUtils.toString(entity))
                        .append(",\r\n use message:").append( data).toString());
//                sink.error(new ReactorCallException(new StringBuilder().append("Request url:").append(url).append(",error,").append("status=").append(status).append(":").append(EntityUtils.toString(entity)).toString()));
            }
            else {
                throw new ReactorCallException(new StringBuilder().append("Request url:").append(url).append(",Unexpected response status: ").append(status)
                        .append(",\r\n use message:").append( data).toString());
//                sink.error(new ReactorCallException(new StringBuilder().append("Request url:").append(url).append(",Unexpected response status: ").append(status).toString()));
            }
        }
    }
	
	public static <T> void handleStreamResponse(String url, ClassicHttpResponse response,
												String data, DataCollector dataCollector,CommonStreamDataHandler<String> streamDataHandler)
			throws IOException, ParseException {
		
		int status = response.getCode();
		
		if (org.frameworkset.spi.remote.http.ResponseUtil.isHttpStatusOK( status)) {
			processStreamResponse(response,  dataCollector,streamDataHandler);
		} else {
			HttpEntity entity = response.getEntity();
			
			if (entity != null ) {
				if (logger.isDebugEnabled()) {
					logger.debug(new StringBuilder().append("Request url:").append(url).append(",status:").append(status).toString());
				}
				throw new ReactorCallException(new StringBuilder().append("Request url:")
						.append(url).append(",error,").append("status=")
						.append(status).append(":")
						.append(EntityUtils.toString(entity))
						.append(",\r\n use message:").append( data).toString());
//                sink.error(new ReactorCallException(new StringBuilder().append("Request url:").append(url).append(",error,").append("status=").append(status).append(":").append(EntityUtils.toString(entity)).toString()));
			}
			else {
				throw new ReactorCallException(new StringBuilder().append("Request url:").append(url).append(",Unexpected response status: ").append(status)
						.append(",\r\n use message:").append( data).toString());
//                sink.error(new ReactorCallException(new StringBuilder().append("Request url:").append(url).append(",Unexpected response status: ").append(status).toString()));
			}
		}
	}

    public static void handleStreamResponse(String url, ClassicHttpResponse response,
                                                FluxSink<String> sink,String message,CommonStreamDataHandler<String> streamDataHandler)
            throws IOException, ParseException {

        int status = response.getCode();

        if (org.frameworkset.spi.remote.http.ResponseUtil.isHttpStatusOK( status)) {
            processStreamResponse(response, sink,streamDataHandler );
        } else {
            HttpEntity entity = response.getEntity();
            String data = message;
            if (entity != null ) {
                if (logger.isDebugEnabled()) {
                    logger.debug(new StringBuilder().append("Request url:").append(url).append(",status:").append(status).toString());
                }
                throw new ReactorCallException(new StringBuilder().append("Request url:")
                        .append(url).append(",error,").append("status=")
                        .append(status).append(":")
                        .append(EntityUtils.toString(entity))
                        .append(",\r\n use message:").append( data).toString());
//                sink.error(new ReactorCallException(new StringBuilder().append("Request url:").append(url).append(",error,").append("status=").append(status).append(":").append(EntityUtils.toString(entity)).toString()));
            }
            else {
                throw new ReactorCallException(new StringBuilder().append("Request url:").append(url).append(",Unexpected response status: ").append(status)
                        .append(",\r\n use message:").append( data).toString());
//                sink.error(new ReactorCallException(new StringBuilder().append("Request url:").append(url).append(",Unexpected response status: ").append(status).toString()));
            }
        }
    }

    /**
     * line：遵循openai规范
     * @param line
     * @param sink
     * @param firstEventTag
     * @return
     */
    public static   boolean handleStringData(AgentAdapter agentAdapter ,String line,FluxSink<String> sink, BooleanWrapperInf firstEventTag, StreamDataBuilder streamDataBuilder){
        if(logger.isDebugEnabled()){
            logger.debug("line: " + line);
        }
        if (line.startsWith("data: ") || line.startsWith("data:")) {
            String data = line.substring(5).trim();

            if (streamDataBuilder.isDone( agentAdapter,   data)) {
                streamDataBuilder.addAgentResultSessionMessage(null);
                return true;
            }
            if (!data.isEmpty()) {
                if(firstEventTag.get()) {
                    firstEventTag.set(false);
                }
                StreamData content = streamDataBuilder.buildWrapped(agentAdapter,data);
                if (content != null && !content.isEmpty()) {
                    sink.next(content.getContent());
                }
            }
        }
        else{
            if(logger.isDebugEnabled()) {
                logger.debug("streamChatCompletion: " + line);
            }
        }
        return false;
    }
    /**
     * line：遵循openai规范
     * @param line
     * @return
     */
    public static   ServerEvent handleServerEventData(AgentAdapter agentAdapter ,String line, StreamDataBuilder streamDataBuilder){
        if(logger.isDebugEnabled()){
            logger.debug("line: " + line);
        }
        ServerEvent serverEvent = null;
        if (SimpleStringUtil.isNotEmpty(line)) {
            StreamData content = streamDataBuilder.buildWrapped(agentAdapter,line);
            if (content != null) {

                serverEvent = new ServerEvent();

                ChatObject chatObject = streamDataBuilder.getChatObject();
                serverEvent.setAgent(chatObject.getAgent());
                serverEvent.setData(content.getContent());
                serverEvent.setGenUrl(content.getUrl());
                serverEvent.setFinishReason(content.getFinishReason());
                
                serverEvent.setType(ServerEvent.TYPE_DATA);
                serverEvent.setFunctionTools(content.getFunctionTools());
                serverEvent.setToolCalls(content.getToolCalls());
                serverEvent.setContentType(content.getType());
                serverEvent.setRole(content.getRole());
                serverEvent.setContent(content.getContent());
                serverEvent.setReasoningContent(content.getReasoningContent());
                serverEvent.setToolCallResponse(chatObject.isToolCall());
                serverEvent.setTokenMetrics(streamDataBuilder.getTokenMetrics());

            }

        }
        return serverEvent;

    }


    /**
     * line：遵循openai规范
     * @param stream
     * @param line
     * @param sink
     * @param firstEventTag
     * @return
     */
    public static boolean handleServerEventData(AgentAdapter agentAdapter, 
                                                boolean stream, String line, FluxSink<ServerEvent> sink, 
                                                BooleanWrapperInf firstEventTag,
                                                BaseStreamDataBuilder streamDataBuilder, FluxSinkStatus fluxSinkStatus){
        if(logger.isDebugEnabled()){
            logger.debug("line: " + line);
        }

//        if(logger.isInfoEnabled()){
//            logger.info("line: " + line);
//        }
        String data = null;
        if(stream){
            if (line.startsWith("data: ")||line.startsWith("data:")) {
                data = line.substring(5).trim();
            }
            else{
                if(logger.isDebugEnabled()) {
                    logger.debug("streamChatCompletion: {}",line);
                }
            }
        }
        else{
            if (line.startsWith("data: ")||line.startsWith("data:")) {
                data = line.substring(5).trim();
            }
            else{
                data = line;
            }

        }
        if(SimpleStringUtil.isNotEmpty( data)){
            if (streamDataBuilder.isDone( agentAdapter, data)) {

                ServerEvent serverEvent = new ServerEvent();

                ChatObject chatObject = streamDataBuilder.getChatObject();
                serverEvent.setAgent(chatObject.getAgent());
                if(firstEventTag.get()) {
                    firstEventTag.set(false);
                    serverEvent.setFirst(true);
                }
                serverEvent.setType(ServerEvent.TYPE_DATA);
                serverEvent.setToolCallResponse(chatObject.isToolCall());
                serverEvent.setDone(true);
                TokenMetrics tokenMetrics = streamDataBuilder.getTokenMetrics();
                if(tokenMetrics != null) {
                    serverEvent.setTokenMetrics(tokenMetrics);
                    tokenMetrics.setEndTime(System.currentTimeMillis());
                }
                ChatContext chatContext = chatObject.getChatContext();
                String fullStreamData = null;
                if(chatContext.isChatWithToolcall() ) {
//                    if(chatContext.getToolCallStage() == ChatContext.TOOL_CALL_STAGE_SEARCH_TOOL) {
                    List<FunctionTool> functionTools = streamDataBuilder.getFunctionTools();//接下来，还需要执行工具调用
                    if(functionTools != null && functionTools.size() > 0){
                        fullStreamData = streamDataBuilder.addChatWithToolCallSessionMessage(tokenMetrics);
                    }
                    else{
                        fullStreamData = streamDataBuilder.addAgentResultSessionMessage(tokenMetrics);
                    }
                }
                else{
                    fullStreamData = streamDataBuilder.addAgentResultSessionMessage(tokenMetrics);
                }
                serverEvent.setFullStreamData(fullStreamData);
                try {
                    ChatStreamCallback chatStreamCallback = chatObject.getChatStreamCallback();
                    if (chatStreamCallback != null) {
                        chatStreamCallback.streamDone(serverEvent);
                    }
                }
                catch (Exception e){
                    logger.error("chatStreamCallback streamDone error",e);
                }
                sink.next(serverEvent);
                return true;
            }
            StreamData content = streamDataBuilder.buildWrapped(agentAdapter,data);
            if (content != null) {
                if( !content.isEmpty()) {
                    buildServerEvent(   firstEventTag,  content,
                            streamDataBuilder,  agentAdapter,  sink);
                    if(content.isContent() || content.isReasoning()){
                        streamDataBuilder.appendToolCallThinkingStreamData(content);
                    }
                    return content.isDone();
                }
                else if(content.isToolCalls()){
                   
                    if(content.isBuildToolCallsFinished()){
                        //构建完整的toolCalls对象
                        if(content.getToolCalls() != null || content.getToolCallsChunk() != null){
                            streamDataBuilder.appendToolCallsStreamData( content);
                        }
                        buildServerEvent(   firstEventTag,  streamDataBuilder.getToolCallsStreamData(),
                                  streamDataBuilder,  agentAdapter,  sink);
                        return content.isDone();
                    }
                    else{
                        streamDataBuilder.appendToolCallsStreamData( content);
                    }
                }
                
                else if(content.getFinishReason() != null && content.getFinishReason().length() > 0){
                    buildServerEvent(   firstEventTag,  content,
                              streamDataBuilder,  agentAdapter,  sink);
                    return content.isDone();
                }
            }
        }

        return false;
    }

    /**
     * {"choices":[{"delta":{"content":null,"reasoning_content":null,"tool_calls":[{"index":0,"id":"","type":"function","function":{"arguments":"{\"params\": "}}]},"finish_reason":null,"index":0,"logprobs":null}],"object":"chat.completion.chunk","usage":null,"created":1771930232,"system_fingerprint":null,"model":"qwen3.5-plus","id":"chatcmpl-be905ede-8111-989a-948f-97b3f9c2c440"}
   
     * data: {"choices":[{"delta":{"content":null,"reasoning_content":null,"tool_calls":[{"index":0,"id":"","type":"function","function":{"arguments":"{\"subuser"}}]},"finish_reason":null,"index":0,"logprobs":null}],"object":"chat.completion.chunk","usage":null,
     * "created":1771930232,"system_fingerprint":null,"model":"qwen3.5-plus","id":"chatcmpl-be905ede-8111-989a-948f-97b3f9c2c440"}
     * @param content
     * @return
     */
    private static void buildToolCalls(BaseStreamDataBuilder streamDataBuilder,StreamData content,ServerEvent serverEvent){
        List<Map> toolCalls = new ArrayList<>();
        Map firstToolCall = content.getToolCallsChunk();
        StreamData streamDataChunk = null;
        StringBuilder argumentsBuilder = new StringBuilder();
        FunctionTool functionTool = streamDataBuilder.functionTool(argumentsBuilder,firstToolCall);
        List<StreamData> _tools = content.getToolCallsStreamDatas();
        
        for(int i = 0; _tools != null && i < _tools.size(); i++){
            streamDataChunk = _tools.get(i);             
            streamDataBuilder.appendArguments(argumentsBuilder,streamDataChunk.getToolCallsChunk());
             
        }
        String arguments = argumentsBuilder.toString();
        Map function = (Map)firstToolCall.get("function");
        function.put("arguments", arguments);
        functionTool.setArguments(JsonUtil.json2Object(arguments, Map.class));
        toolCalls.add(firstToolCall);
        List<FunctionTool> functionTools = new ArrayList<>();
        functionTools.add(functionTool);
        serverEvent.setToolCalls(toolCalls);
        content.setToolCalls(toolCalls);
        serverEvent.setFunctionTools(functionTools);
        content.setFunctionTools(functionTools);
        streamDataBuilder.setToolResolved(true);
        
    }
    private static void buildServerEvent( BooleanWrapperInf firstEventTag,StreamData content,
                                                 BaseStreamDataBuilder streamDataBuilder,AgentAdapter agentAdapter,FluxSink<ServerEvent> sink)
    {
        if(streamDataBuilder.isToolResolved()){
            return;
        }
        ServerEvent serverEvent = new ServerEvent();

        serverEvent.setTokenMetrics(streamDataBuilder.getTokenMetrics());
        ChatObject chatObject = streamDataBuilder.getChatObject();
        serverEvent.setAgent(chatObject.getAgent());
        if (firstEventTag.get()) {
            firstEventTag.set(false);
            serverEvent.setFirst(true);
        }
        if(content.isToolCalls()){
            buildToolCalls(  streamDataBuilder,  content,  serverEvent);
        }
        else {
            
            serverEvent.setFunctionTools(content.getFunctionTools());
            serverEvent.setToolCalls(content.getToolCalls());
        }
        serverEvent.setToolCallResponse(chatObject.isToolCall());
        serverEvent.setData(content.getContent());
        serverEvent.setGenUrl(content.getUrl());
        serverEvent.setFinishReason(content.getFinishReason());
        serverEvent.setType(ServerEvent.TYPE_DATA);
        serverEvent.setContentType(content.getType());
        serverEvent.setDone(content.isDone());

        serverEvent.setRole(content.getRole());
        serverEvent.setContent(content.getContent());
        serverEvent.setReasoningContent(content.getReasoningContent());
        streamDataBuilder.handleServerEvent(agentAdapter,serverEvent);
        sink.next(serverEvent);
        
    }

}
