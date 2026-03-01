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
import org.frameworkset.spi.ai.material.DownImageBase64HttpClientResponseHandler;
import org.frameworkset.spi.ai.material.DownFileHttpClientResponseHandler;
import org.frameworkset.spi.ai.mcp.DataCollector;
import org.frameworkset.spi.ai.model.*;
import org.frameworkset.spi.reactor.*;
import org.frameworkset.spi.remote.http.ClientConfiguration;
import org.frameworkset.spi.remote.http.proxy.BBossEntityUtils;
import org.frameworkset.util.concurrent.BooleanWrapperInf;
import org.frameworkset.util.concurrent.NoSynBooleanWrapper;
import reactor.core.publisher.FluxSink;

import java.io.IOException;
import java.util.*;

import static java.lang.Thread.sleep;

/**
 * @author biaoping.yin
 * @Date 2026/1/11
 */
public class AIResponseUtil {
    private static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AIResponseUtil.class);
 


  public static HttpClientResponseHandler<String>  buildDownImageHttpClientResponseHandler(ClientConfiguration config, ImageAgentMessage imageAgentMessage, String imageUrl){
      String type  = imageAgentMessage.getStoreImageType();
      HttpClientResponseHandler<String> handler = null;
      if(type == null || type.equals(AIConstants.STORETYPE_BASE64) || type.equals(AIConstants.STORETYPE_URL)){
          handler = new DownImageBase64HttpClientResponseHandler();
      }
      else if(type.equals(AIConstants.STORETYPE_FILE)){
          handler = new DownFileHttpClientResponseHandler( config,imageAgentMessage,  imageUrl);
      }
      if(handler == null){
          logger.warn("unsupport StoreImageType:{}", type);
          throw new AIRuntimeException("unsupport StoreImageType:"+type);
      }
      return handler;
      
  }

    public static HttpClientResponseHandler<String>  buildDownAudioHttpClientResponseHandler(ClientConfiguration config, AudioAgentMessage audioAgentMessage, String audioUrl){
         
        return new DownFileHttpClientResponseHandler( config,audioAgentMessage,  audioUrl);
        

    }

    public static HttpClientResponseHandler<String>  buildDownVideoHttpClientResponseHandler(ClientConfiguration config, 
                                                                                             VideoStoreAgentMessage videoStoreAgentMessage, String videoUrl){

        return new DownFileHttpClientResponseHandler( config,videoStoreAgentMessage,  videoUrl);


    }

    public static HttpClientResponseHandler<String>  buildDownAudioHttpClientResponseHandler(ClientConfiguration config, AudioAgentMessage audioAgentMessage){

        return new DownFileHttpClientResponseHandler( config,audioAgentMessage,  (String)null);


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

    public static boolean handleServerEventExceptionData(Throwable throwable,FluxSink<ServerEvent> sink, BooleanWrapperInf firstEventTag){
        if(logger.isWarnEnabled()) {
            logger.warn("服务端异常：", throwable);
        }

        String error = SimpleStringUtil.exceptionToString(throwable);
        ServerEvent serverEvent = new ServerEvent();
        if(firstEventTag.get()) {
            firstEventTag.set(false);
            serverEvent.setFirst(true);
        }
        serverEvent.setData(error);
        serverEvent.setType(ServerEvent.ERROR);
        sink.next(serverEvent);

        serverEvent = new ServerEvent();

        serverEvent.setDone( true);
        sink.next(serverEvent);
//        sink.complete();
        return true;

    }

    /**
     * 处理音频识别流数据
     * @param data
     * @return
     */
    public static StreamData parseAudioStreamContentFromData(StreamDataBuilder streamDataBuilder,String data){
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
    public static StreamData parseQianwenAudioGenStreamContentFromData(String data){
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
    public static StreamData parseStreamContentFromData(BaseStreamDataBuilder streamDataBuilder,String data) {
        try {
            Map map = SimpleStringUtil.json2Object(data,Map.class);
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
                               if(tool_call != null){
                                   return streamDataBuilder.functionToolsChunk((List<Map>) tool_call, finishReason);
                               }
                                
                               else if (SimpleStringUtil.isNotEmpty(reasoning_content)) {
                                   return new StreamData(ServerEvent.REASONING_CONTENT, reasoning_content, finishReason);
                               } else {
                                   return new StreamData(ServerEvent.CONTENT, content, finishReason);
                               }

                           } else {
                               Map message = (Map) choice.get("message");
                               if (message != null) {
                                   String reasoning_content = (String) message.get("reasoning_content");
                                   String content = (String) message.get("content");
                                   if (SimpleStringUtil.isNotEmpty(reasoning_content)) {
                                       return new StreamData(ServerEvent.REASONING_CONTENT, reasoning_content, finishReason);
                                   } else {
                                       return new StreamData(ServerEvent.CONTENT, content, finishReason);
                                   }
                               }
                               if (logger.isDebugEnabled())
                                   logger.debug("choices message null: {}", data);
                           }
                       }
                       else{
                           Map delta = (Map) choice.get("delta");
                           if (delta != null) {
                               return new StreamData( (List<FunctionTool>)null,(List<Map>)null, finishReason);
                           }
                           else {
                               Map message = (Map) choice.get("message");
                               if (message != null) {
                                   StreamData streamData = streamDataBuilder.functionTools((List<Map>) message.get("tool_calls"), finishReason);
                                   if (streamData != null) {
                                       String reasoning_content = (String) message.get("reasoning_content");
                                       if (reasoning_content == null) {
                                           return streamData
                                                   .setContent((String) message.get("content"))
                                                   .setRole((String) message.get("role"));
                                       } else {
                                           return streamData
                                                   .setContent((String) message.get("content"))
                                                   .setReasoningContent(reasoning_content)
                                                   .setRole((String) message.get("role"));
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
                String code =  (String)map.get("code");
                String message = (String) map.get("message");
                if(SimpleStringUtil.isNotEmpty(code)) {
                    return new StreamData(ServerEvent.CONTENT, message, code);
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

    private static <T> void processStreamResponse(ClassicHttpResponse response, FluxSink<T> sink, BaseStreamDataHandler<T> streamDataHandler) throws IOException {

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
//            final FluxSinkStatus fluxSinkStatus_ = fluxSinkStatus;
            // 添加处置监听器
//            sink.onDispose(() -> {
//                // 当 sink 被处置时执行（包括正常完成、错误和取消）
//                if(logger.isDebugEnabled()) {
//                    logger.debug("Sink disposed");
//                }
//                fluxSinkStatus_.dispose();
//                // 执行清理工作
//                fluxSinkStatus_.releaseResources();
//
//            });
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
        }
    }

    private static void processStreamResponse(ClassicHttpResponse response, 
                                              FluxSink<String> sink,
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
           
            while (  (line = fluxSinkStatus.readLine()) != null ) {
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
            int waitTimes = 1;//值为空时，等待10次
			do{
                line = fluxSinkStatus.readLine();
                if(line == null) {
                    try {
                        if(waitTimes <= 0)
                            break;
                        sleep(1000L);
                        waitTimes --;
                    } catch (InterruptedException e) {
//                        throw new RuntimeException(e);
                    }
                   
                }
                if(line != null) {
                    dataCollector.collector(line);
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
                                                FluxSink<T> sink, StreamDataHandler<T> streamDataHandler)
            throws IOException, ParseException {

        int status = response.getCode();

        if (org.frameworkset.spi.remote.http.ResponseUtil.isHttpStatusOK( status)) {
            processStreamResponse(response, sink,(BaseStreamDataHandler<T>) streamDataHandler);
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
                return true;
            }
            if (!data.isEmpty()) {
                if(firstEventTag.get()) {
                    firstEventTag.set(false);
                }
                StreamData content = streamDataBuilder.build(agentAdapter,data);
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
            StreamData content = streamDataBuilder.build(agentAdapter,line);
            if (content != null) {

                serverEvent = new ServerEvent();
          
                serverEvent.setData(content.getContent());
                serverEvent.setGenUrl(content.getUrl());
                serverEvent.setFinishReason(content.getFinishReason());
                
                serverEvent.setType(ServerEvent.DATA);
                serverEvent.setFunctionTools(content.getFunctionTools());
                serverEvent.setToolCalls(content.getToolCalls());
                serverEvent.setContentType(content.getType());
                serverEvent.setRole(content.getRole());
                serverEvent.setContent(content.getContent());
                serverEvent.setReasoningContent(content.getReasoningContent());


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
                if(firstEventTag.get()) {
                    firstEventTag.set(false);
                    serverEvent.setFirst(true);
                }
                serverEvent.setType(ServerEvent.DATA);
                serverEvent.setDone(true);
          
                sink.next(serverEvent);
                return true;
            }
            StreamData content = streamDataBuilder.build(agentAdapter,data);
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
        
    }
    private static void buildServerEvent( BooleanWrapperInf firstEventTag,StreamData content,
                                                 BaseStreamDataBuilder streamDataBuilder,AgentAdapter agentAdapter,FluxSink<ServerEvent> sink)
    {
        ServerEvent serverEvent = new ServerEvent();
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
        serverEvent.setData(content.getContent());
        serverEvent.setGenUrl(content.getUrl());
        serverEvent.setFinishReason(content.getFinishReason());
        serverEvent.setType(ServerEvent.DATA);
        serverEvent.setContentType(content.getType());
        serverEvent.setDone(content.isDone());

        serverEvent.setRole(content.getRole());
        serverEvent.setContent(content.getContent());
        serverEvent.setReasoningContent(content.getReasoningContent());
        streamDataBuilder.handleServerEvent(agentAdapter,serverEvent);
        sink.next(serverEvent);
        
    }

}
