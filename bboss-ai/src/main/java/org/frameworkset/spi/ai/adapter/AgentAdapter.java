package org.frameworkset.spi.ai.adapter;
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
import com.frameworkset.util.JsonUtil;
import com.frameworkset.util.SimpleStringUtil;
import org.frameworkset.spi.ai.AIAgent;
import org.frameworkset.spi.ai.context.AgentRuntimeContext;
import org.frameworkset.spi.ai.context.ChatContext;
import org.frameworkset.spi.ai.hitl.HitlCallResult;
import org.frameworkset.spi.ai.hitl.HitlTaskHelper;
import org.frameworkset.spi.ai.hitl.HitlTaskToolInf;
import org.frameworkset.spi.ai.material.GenFileDownload;
import org.frameworkset.spi.ai.material.GenMaterialFileDownload;
import org.frameworkset.spi.ai.mcp.model.MCPToolCallResponse;
import org.frameworkset.spi.ai.model.*;
import org.frameworkset.spi.ai.model.tool.PermissionRules;
import org.frameworkset.spi.ai.model.tool.ToolCallAsk;
import org.frameworkset.spi.ai.model.tool.ToolCallAskResult;
import org.frameworkset.spi.ai.model.tool.ToolCallState;
import org.frameworkset.spi.ai.permission.*;
import org.frameworkset.spi.ai.store.SessionMessage;
import org.frameworkset.spi.ai.tool.AgentTraceHolder;
import org.frameworkset.spi.ai.tool.ToolBase;
import org.frameworkset.spi.ai.tool.ToolCallContext;
import org.frameworkset.spi.ai.tools.HitlAssistant;
import org.frameworkset.spi.ai.util.*;
import org.frameworkset.spi.reactor.SSEHeaderSetFunction;
import org.frameworkset.spi.remote.http.ClientConfiguration;
import org.frameworkset.spi.remote.http.HttpRequestProxy;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * 智能体适配器：针对不同厂家的模型平台服务进行适配，包括请求参数转换、结果转换等
 * @author biaoping.yin
 * @Date 2026/1/4
 */
public abstract class AgentAdapter implements CompletionsUrlInterface{
    private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AgentAdapter.class);
    protected GenFileDownload genFileDownload;
    private boolean inited;

    public String getReasoningContent( Map delta ){
		String reasoning_content = (String) delta.get("reasoning_content");
		return reasoning_content;
	} 
    protected AgentAdapter initAgentAdapter(){
        if(inited)
            return this;
        inited = true;
        genFileDownload = new GenMaterialFileDownload();
        return this;
    }

    public GenFileDownload getGenFileDownload() {
        return genFileDownload;
    }
	
	public String getListModelsUrl(ClientConfiguration config) {
        return getListModelsUrl(  config,null);
    }
	
	public String getListModelsUrl(ClientConfiguration config,Map params) {
		StringBuilder sb = new StringBuilder();
		sb.append("/api/v1/models");
		if(params != null && params.size() > 0){
			sb.append("?");
			int i = 0;
			for (Object key : params.keySet()) {
				if(i > 0) sb.append("&");
				sb.append(key).append("=").append(params.get(key));
				i ++;
			}
		}
		return sb.toString();
	}
	
	
	/**
     * 构建生成图片请求参数
     * @param imageAgentMessage
     * @return
     */
    protected abstract Map buildGenImageRequestMap(ImageAgentMessage imageAgentMessage,AIAgent aiAgent);

    
 	protected void buildincludeUsage(Boolean stream,AgentMessage agentMessage,Map<String, Object> requestMap){
		if(stream != null && stream) {
			Boolean includeUsage = agentMessage.getIncludeUsage();
			if (includeUsage != null) {
				Map streamOptions = new HashMap();
				streamOptions.put("include_usage", includeUsage);
				requestMap.put("stream_options", streamOptions);
				
			}
		}
	}
    protected void buildTools(ChatContext chatContext,AgentMessage agentMessage,AIAgent agent,Map<String, Object> requestMap){
        agent.init();
        List<FunctionToolDefine> tools = agent.getToolsByToolSearch(chatContext,agentMessage);
        if(tools != null && tools.size() > 0){
            chatContext.setAgentTools(tools);
//            Object tools = aiAgent.getTools();
            requestMap.put("tools",   tools);       
            if(agent.getEnableLoopToolCall() != null && agent.getEnableLoopToolCall())
                requestMap.put("tool_choice", "auto");            
            chatContext.setChatWithToolcall(true);
        }
    }

    public   float[] embedding(ClientConfiguration config,EmbeddingMessage embeddingMessage,AIAgent agent,Map<String,Object> params) {
        EmbeddingResponse result = HttpRequestProxy.sendJsonBody(embeddingMessage.getMaas(), params, getEmbeddingUrl(config,embeddingMessage), EmbeddingResponse.class);
        if(result != null){
            return result.embedding();
        }
        return null;
    }
    protected void filterParameters(ChatContext chatContext,AgentMessage agentMessage,AIAgent aiAgent,Map<String, Object> requestMap, Map<String, Object> parameters) {
        Boolean stream = chatContext.getStreamable();
		if(stream == null){
			stream = agentMessage.getStream();
		}
		if(SimpleStringUtil.isEmpty( parameters)){
            if( stream != null){
                requestMap.put("stream", stream);
            }

            if( agentMessage.getTemperature() != null){
                requestMap.put("temperature", agentMessage.getTemperature());
            }
            if(agentMessage.getMaxTokens() != null)
                requestMap.put("max_tokens", agentMessage.getMaxTokens());
            
        }
        else {
            requestMap.putAll( parameters);
            //设置默认参数
            if(!parameters.containsKey("stream") && stream != null){
                requestMap.put("stream", stream);
            }

            if(!parameters.containsKey("temperature") && agentMessage.getTemperature() != null){
                requestMap.put("temperature", agentMessage.getTemperature());
            }
            if(!parameters.containsKey("max_tokens") && agentMessage.getMaxTokens() != null){
                requestMap.put("max_tokens", agentMessage.getMaxTokens());
            }
            
        }
		//"stream_options": {"include_usage": true}
		buildincludeUsage(  stream,  agentMessage, requestMap);
        buildTools(chatContext,agentMessage,  aiAgent, requestMap);
    }
    protected Object handleImageParserMessages(List<LinkedMessageMap<String, Object>> messages){
        return messages;
    }

    protected String getSystemPrompt(AgentMessage agentMessage, AIAgent aiAgent){
        return aiAgent.evalSystemPrompt(  agentMessage);
    }
    protected String getPrompt(AgentMessage agentMessage, AIAgent aiAgent){
        return aiAgent.evalPrompt(agentMessage);
    }
    public Map buildVideoVLRequestMap(VideoVLAgentMessage videoVLAgentMessage, AIAgent aiAgent,ChatContext chatContext) {
		// 构建消息历史列表，包含之前的会话记忆
		
		List<LinkedMessageMap<String, Object>> sessionMemory = aiAgent.getSessionMemory(true);
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("model",videoVLAgentMessage.getModel());
        List<String > videoUrls = videoVLAgentMessage.getVideoUrls();
		
		LinkedMessageMap<String, Object> userMessage = null;
		LinkedMessageMap<String, Object> systemMessage = null;
        String prompt = getPrompt(  videoVLAgentMessage,   aiAgent);
        if(chatContext != null){
            prompt = chatContext.evalPrompt(prompt);
            
        }
        if(videoUrls != null && videoUrls.size() > 0) {
            userMessage = buildInputVideosMessage(prompt, videoUrls.toArray(new String[]{}));
        }
        else{
            userMessage = buildInputVideosMessage(prompt, (String[])null);
        }
      
        List<LinkedMessageMap<String, Object>> messages = null;
        if(sessionMemory != null){
            if(sessionMemory.size() == 0){
                String systemPrompt = getSystemPrompt(videoVLAgentMessage,aiAgent);
                if(systemPrompt != null){
                    if(chatContext != null){
                        systemPrompt = chatContext.evalSystemPrompt(systemPrompt);

                    }
                    systemMessage = MessageBuilder.buildSystemMessage(systemPrompt);
                    videoVLAgentMessage.addSessionMessage(systemMessage,aiAgent);
                }
            }
            videoVLAgentMessage.addSessionMessage(userMessage,aiAgent);
            messages = new ArrayList<>(sessionMemory);
        }
        else{
            messages = new ArrayList<>();
            String systemPrompt = getSystemPrompt(videoVLAgentMessage,aiAgent);
            if(systemPrompt != null){
                if(chatContext != null){
                    systemPrompt = chatContext.evalSystemPrompt(systemPrompt);

                }
                systemMessage = MessageBuilder.buildSystemMessage(systemPrompt);
                messages.add(systemMessage);
            }
            messages.add(userMessage);
        }


        requestMap.put("messages", handleImageParserMessages(messages));
        Map parameters = videoVLAgentMessage.getParameters();

        filterParameters(chatContext,videoVLAgentMessage,aiAgent,requestMap,parameters);

        return requestMap;
    }
    
    public Map buildImageVLRequestMap(ImageVLAgentMessage imageAgentMessage, AIAgent aiAgent, ChatContext chatContext) {
		
		// 构建消息历史列表，包含之前的会话记忆
		
		List<LinkedMessageMap<String, Object>> sessionMemory = aiAgent.getSessionMemory(true);
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("model",imageAgentMessage.getModel());
        List<String > imageUrls = imageAgentMessage.getImageUrls();
		
		LinkedMessageMap<String, Object> userMessage = null;
		LinkedMessageMap<String, Object> systemMessage = null;
        String prompt = getPrompt(  imageAgentMessage,   aiAgent);
        if(chatContext != null){
            prompt = chatContext.evalPrompt(prompt);
        }
        if(imageUrls != null && imageUrls.size() > 0) {           
            userMessage = buildInputImagesMessage(prompt, imageUrls.toArray(new String[]{}));
        }
        else{
            userMessage = buildInputImagesMessage(prompt, (String[])null);
        }
 
        List<LinkedMessageMap<String, Object>> messages = null;
        if(sessionMemory != null){
            if(sessionMemory.size() == 0){
                String systemPrompt = getSystemPrompt(imageAgentMessage,aiAgent);
                if(systemPrompt != null){
                    if(chatContext != null){
                        systemPrompt = chatContext.evalSystemPrompt(systemPrompt);
                    }
                    systemMessage = MessageBuilder.buildSystemMessage(systemPrompt);
                    imageAgentMessage.addSessionMessage(systemMessage,aiAgent);
                }
            }
            imageAgentMessage.addSessionMessage(userMessage,aiAgent);
            messages = new ArrayList<>(sessionMemory);
        }
        else{
            messages = new ArrayList<>();
            String systemPrompt = getSystemPrompt(imageAgentMessage,aiAgent);
            if(systemPrompt != null){
                if(chatContext != null){
                    systemPrompt = chatContext.evalSystemPrompt(systemPrompt);
                }
                systemMessage = MessageBuilder.buildSystemMessage(systemPrompt);
                messages.add(systemMessage);
            }
            messages.add(userMessage);
        }
         

        requestMap.put("messages", handleImageParserMessages(messages));
        Map parameters = imageAgentMessage.getParameters();

        filterParameters(chatContext,imageAgentMessage,aiAgent,requestMap,parameters);

        return requestMap;
    }
    public boolean isDone(String data){
        return "[DONE]".equals(data);

    }
    
    public String getDoneData(){
        return "data:[DONE]";
    }
    public boolean isVideoParserDone(String data){
        return isDone(  data);

    }

    public String getVideoParserDoneData(){
        return getDoneData();
    }

    public boolean isImageParserDone(String data){
        return isDone(  data);

    }

    public String getImageParserDoneData(){
        return getDoneData();
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
    public StreamData parseAudioGenStreamContentFromData(Map data){
        return AIResponseUtil.parseQianwenAudioGenStreamContentFromData(this,data);
    }

    /**
     * 语音识别：data:{"output":{"choices":[{"message":{"annotations":[{"type":"audio_info","language":"zh","emotion":"neutral"}],"content":[{"text":"欢迎与"}],"role":"assistant"},"finish_reason":"null"}]},"usage":{"output_tokens_details":{"text_tokens":6},"input_tokens_details":{"text_tokens":16},"seconds":1},"request_id":"e84128d5-4bae-4e7e-91ab-6fb33504d2e3"}
     * LLM和图像识别：data: {"id":"ccf32be6-ad2f-4658-963a-fc3c22346e6b","object":"chat.completion.chunk","created":1761725211,"model":"deepseek-reasoner","system_fingerprint":"fp_ffc7281d48_prod0820_fp8_kvcache","choices":[{"index":0,"delta":{"content":null,"reasoning_content":"在"},"logprobs":null,"finish_reason":null}]}
     * @param data
     * @return
     */
    public StreamData parseStreamContentFromData(BaseStreamDataBuilder streamDataBuilder, Map data){
        return AIResponseUtil.parseStreamContentFromData(this,streamDataBuilder,data);
    }

    /**
     * 语音识别：data:{"output":{"choices":[{"message":{"annotations":[{"type":"audio_info","language":"zh","emotion":"neutral"}],"content":[{"text":"欢迎与"}],"role":"assistant"},"finish_reason":"null"}]},"usage":{"output_tokens_details":{"text_tokens":6},"input_tokens_details":{"text_tokens":16},"seconds":1},"request_id":"e84128d5-4bae-4e7e-91ab-6fb33504d2e3"}
     * LLM和图像识别：data: {"id":"ccf32be6-ad2f-4658-963a-fc3c22346e6b","object":"chat.completion.chunk","created":1761725211,"model":"deepseek-reasoner","system_fingerprint":"fp_ffc7281d48_prod0820_fp8_kvcache","choices":[{"index":0,"delta":{"content":null,"reasoning_content":"在"},"logprobs":null,"finish_reason":null}]}
     * @param data
     * @return
     */
    public StreamData parseImageParserStreamContentFromData(BaseStreamDataBuilder streamDataBuilder,Map data){
        return AIResponseUtil.parseStreamContentFromData(this,streamDataBuilder,data);
    }

    public StreamData parseVideoParserStreamContentFromData(BaseStreamDataBuilder streamDataBuilder,Map data){
        return AIResponseUtil.parseStreamContentFromData(this,streamDataBuilder,data);
    }

    /**
     * 语音识别数据解析
     * @param data
     * @return
     */
    public StreamData parseAudioStreamContentFromData(StreamDataBuilder streamDataBuilder,Map data){
        return AIResponseUtil.parseAudioStreamContentFromData( this, streamDataBuilder,data);
    }

    
    
    /**
     * 获取图片识别模型智能问答请求参数类型
     * @return
     */
    public String getAIImageParserRequestType(){
        return AIConstants.AI_CHAT_REQUEST_BODY_JSON;
        
    }

    /**
     * 获取图片识别模型智能问答请求参数类型
     * @return
     */
    public String getAIVideoParserRequestType(){
        return AIConstants.AI_CHAT_REQUEST_BODY_JSON;

    }

    /**
     * 获取音频识别模型智能问答请求参数类型
     * @return
     */
    public String getAIAudioParsertRequestType(){
        return AIConstants.AI_CHAT_REQUEST_BODY_JSON;

    }

    /**
     * 获取智能问答请求参数类型
     * @return
     */
    public String getAIChatRequestType(){
        return AIConstants.AI_CHAT_REQUEST_BODY_JSON;

    }
    protected LinkedMessageMap<String, Object> buildInputVideosMessage(String message,String... videoUrls) {
        return MessageBuilder.buildInputVideosMessage(message,videoUrls);
    }
    
    protected LinkedMessageMap<String, Object> buildInputImagesMessage(String message,String... imageUrls) {
        return MessageBuilder.buildInputImagesMessage(message,imageUrls);
    }
	
	/**
	 * Run every tool call through the permission gate.
	 *
	 * <p>When the agent's {link PermissionContextState} is trivial
	 * (default mode, no rules, no working directories — i.e. the user has not opted into the
	 * permission system) we fall back to the lightweight pre-2.0 path: the tool's own
	 * {link ToolBase#checkPermissions} ASK gates a confirmation, anything else is approved.
	 *
	 * <p>Otherwise we engage the full {link PermissionEngine} pipeline so deny/ask/allow rules
	 * and EXPLORE/ACCEPT_EDITS/BYPASS/DONT_ASK modes are honoured before execution. Legacy
	 * {link AgentTool}s that do not extend {link ToolBase} always pass through approved.
	 */
	private PermissionGate evaluatePermissions( List<FunctionTool> toolCalls,ChatObject chatObject) {
		if (toolCalls == null || toolCalls.isEmpty()) {
			return null;
		}
		AgentRuntimeContext agentRuntimeContext = chatObject.getChatContext().getAgentRuntimeContext();
		boolean useEngine = agentRuntimeContext != null && !agentRuntimeContext.isTrivial();
		PermissionEngine permissionEngine =  new PermissionEngine(chatObject);
		Map<String,PermissionVerdict> denied = new LinkedHashMap();
		List<PermissionVerdict> pending = new ArrayList<>();
		for (FunctionTool toolCall : toolCalls) {
			PermissionVerdict permissionVerdict = evaluateOne(toolCall,useEngine, permissionEngine,chatObject );
			FunctionTool functionTool = permissionVerdict.getFunctionTool();
			switch (permissionVerdict.getBehavior()) {
				case DENY: denied.put(functionTool.getId(), permissionVerdict);break;
				case ASK : pending.add(permissionVerdict);break;
				case ALLOW:
				case PASSTHROUGH:
					// auto-approved; falls through to execution
				
			}
//			
		}
		return new PermissionGate(pending, denied,permissionEngine);
	}
	
	private PermissionVerdict evaluateOne(FunctionTool use, boolean useEngine,PermissionEngine permissionEngine, ChatObject chatObject) {
		// Tools already promoted to ALLOWED by user confirmation skip the engine entirely.
//		if (use.getToolCallState() == ToolCallState.ALLOWED) {
//			return new PermissionVerdict(use, PermissionBehavior.ALLOW,null);
//		}
		Map<String, Object> input = use.getArguments() == null ? Collections.emptyMap() : use.getArguments	();
	 
		PermissionDecision sessionDecision = permissionEngine.checkSessionAlwaysPermission(use, input);
		if (sessionDecision != null) {
			return new PermissionVerdict(use, sessionDecision.getBehavior(),sessionDecision);
		}
		
		ToolBase toolBase = use.getToolBase();
		ToolCallPermissionManager toolCallPermissionManager = null;
		AgentRuntimeContext context = chatObject.getChatContext().getAgentRuntimeContext();
		if(context != null ){
			toolCallPermissionManager = context.getToolCallPermissionManager();
		}
		if(toolBase == null && !useEngine && toolCallPermissionManager == null){
			return new PermissionVerdict(use, PermissionBehavior.ALLOW,null);
		}
		 
//		AgentTool tool = toolkit.getTool(use.getName());
//		if (!(tool instanceof ToolBase tb)) {
//			return Mono.just(new PermissionVerdict(use, PermissionBehavior.ALLOW));
//		}
		
		
		PermissionDecision permissionDecision = null;
		PermissionVerdict permissionVerdict = null;
		if (useEngine) {			
			permissionDecision = permissionEngine
					.checkPermission(use, input);
			permissionVerdict = new PermissionVerdict(
					use,
					permissionDecision == null
							? PermissionBehavior.ASK
							: permissionDecision.getBehavior(),permissionDecision);
			return permissionVerdict;
//					.map(
//							decision ->
//									new PermissionVerdict(
//											use,
//											decision == null
//													? PermissionBehavior.ASK
//													: decision.getBehavior()));
		}
		else if(toolBase != null) {
			permissionDecision = toolBase.checkPermissions(use, use.getArguments(), chatObject);
			if (permissionDecision == null) {
				return new PermissionVerdict(use, PermissionBehavior.ALLOW, permissionDecision );
			}
		}
		else {
			permissionDecision = toolCallPermissionManager.checkPermissions(use, use.getArguments(), chatObject);
			if (permissionDecision == null) {
				return new PermissionVerdict(use, PermissionBehavior.ALLOW, permissionDecision );
			}
		}
		// In the legacy lightweight path only an explicit ASK from the tool
		// gates execution; PASSTHROUGH and ALLOW both run, DENY is
		// honoured.
		switch (permissionDecision.getBehavior()) {
			case ASK: permissionVerdict = new PermissionVerdict(use, PermissionBehavior.ASK,permissionDecision );
			break;
			case DENY: permissionVerdict = new PermissionVerdict(use, PermissionBehavior.DENY,permissionDecision );
			break;
			default : permissionVerdict = new PermissionVerdict(use, PermissionBehavior.ALLOW,permissionDecision );
		};
		return permissionVerdict;
//				.map(
//						decision -> {
//							if (decision == null) {
//								return new PermissionVerdict(use, PermissionBehavior.ALLOW);
//							}
//							// In the legacy lightweight path only an explicit ASK from the tool
//							// gates execution; PASSTHROUGH and ALLOW both run, DENY is
//							// honoured.
//							return switch (decision.getBehavior()) {
//								case ASK -> new PermissionVerdict(use, PermissionBehavior.ASK);
//								case DENY ->
//										new PermissionVerdict(use, PermissionBehavior.DENY);
//								default -> new PermissionVerdict(use, PermissionBehavior.ALLOW);
//							};
//						});
	}
	
//	/**
//	 * Synthesise DENIED ToolResultBlocks for tools that were rejected by deny rules and append
//	 * them to context so the conversation reflects the rejection (and resume doesn't see them
//	 * as pending).
//	 */
//	private void writeAutoDeniedResults(List<ToolUseBlock> toolCalls, Set<String> deniedIds) {
//		for (ToolUseBlock tc : toolCalls) {
//			if (!deniedIds.contains(tc.getId())) {
//				continue;
//			}
//			ToolResultBlock denied =
//					ToolResultBlock.text("Permission denied by rules")
//							.withIdAndName(tc.getId(), tc.getName())
//							.withState(ToolResultState.DENIED);
//			Msg deniedMsg = ToolResultMessageBuilder.buildToolResultMsg(denied, tc, getName());
//			state.contextMutable().add(deniedMsg);
//		}
//	}
	
	private ToolCallAskResult getToolCallAskResult(String toolId, List<ToolCallAskResult> toolCallAskResults){
		if(toolCallAskResults == null || toolCallAskResults.size() == 0)
			return null;	
		for(ToolCallAskResult toolCallAskResult:toolCallAskResults){
			if(toolCallAskResult.getToolId().equals(toolId)){
				return toolCallAskResult;
			}
		}
		return null;	
	}
	
	private List<ToolCallAskResult> toolCallAsk(ChatObject chatObject,AgentRuntimeContext agentRuntimeContext,List<PermissionVerdict> pendingAsk){
		if(pendingAsk == null || pendingAsk.size() == 0)
			return null;	
		List<ToolCallAskResult> toolCallAskResults = null;
		ToolCallContext toolCallContext = new ToolCallContext();
		List<ToolCallAsk> askTools = new ArrayList<>();
		for (PermissionVerdict permissionVerdict : pendingAsk) {
			FunctionTool tool = permissionVerdict.getFunctionTool();
			PermissionDecision permissionDecision = permissionVerdict.getPermissionDecision();	
			ToolCallAsk toolData = new ToolCallAsk();
			toolData.setToolId(tool.getId());
			toolData.setToolName(tool.getFunctionName());
			toolData.setInput(tool.getArguments());
			toolData.setSuggestedRules(permissionDecision.getSuggestedRules());
			askTools.add(toolData);
		}
		HitlCallResult<List> hitlCallResult = HitlTaskHelper.hitlTaskTool(new HitlTaskToolInf() {
			@Override
			public String getTimeoutAction() {
				return HitlTaskToolInf.TIMEOUT_ACTION_REJECTED;
			}
			
			@Override
			public long getHitlTaskTimeout() {
				return agentRuntimeContext.getPermissionHitlTaskTimeout();
			}
			
			
			
			@Override
			public HitlAssistant<Object,List<ToolCallAskResult>> getHitlAssistant() {
				return new HitlAssistant<Object,List<ToolCallAskResult>>() {
					@Override
					public Map<String, Object> getHumanAssistantDatas(ToolCallContext toolCallContext) {
						Map<String, Object> humanAssistantDatas = new HashMap<>();
						
						humanAssistantDatas.put(HitlAssistant.HITL_TASK_PERMISSION_ASK_TOOLS, askTools);
						humanAssistantDatas.put(HitlAssistant.HITL_TASK_TYPE_KEY, HitlAssistant.HITL_TASK_TYPE_TOOL_CALL_PERMISSION_ASK);
						return humanAssistantDatas;
					}
					
					@Override
					public void handleHumanSubbmitDatas(Object humanSubbmitDatas, ToolCallContext toolCallContext) {
						
					}
					
					@Override
					public List<ToolCallAskResult> timeOutHandle(Map<String, Object> humanAssistantDatas) {
						List<ToolCallAsk> askTools = (List<ToolCallAsk>) humanAssistantDatas.get(HitlAssistant.HITL_TASK_PERMISSION_ASK_TOOLS);
						
						List<ToolCallAskResult> toolCallAskResults = new ArrayList<>(askTools.size());
						for(ToolCallAsk askTool : askTools) {
							ToolCallAskResult toolCallAskResult = new ToolCallAskResult();
							toolCallAskResult.setToolId(askTool.getToolId());
							toolCallAskResult.setToolName(askTool.getToolName());
							toolCallAskResult.setApproved(false);//审批通过，放行工具操作，如果返回false则拒绝操作
							toolCallAskResult.setHitlConfirm("超时导致拒绝操作");
//							List<PermissionRule> suggestedRules = askTool.getSuggestedRules();
//							if (suggestedRules != null && suggestedRules.size() > 0){
//								toolCallAskResult.setChoosedAlwaysPermissionRule(suggestedRules.get(0)); //从建议规则列表中选择一个即可，这里选择第一个规则
//							}
//							toolCallAskResult.setUpdateInput(askTool.getInput());//模拟修改工具入参，这里不做任何修改直接放回去
							toolCallAskResults.add(toolCallAskResult);
						}
						return toolCallAskResults;
					}
				};
			}
			
		},chatObject, "需要用户授权是否执行工具", toolCallContext,List.class,ToolCallAskResult.class);
		if(hitlCallResult != null && hitlCallResult.getResult() != null){
			toolCallAskResults = hitlCallResult.getResult();
		}
		return toolCallAskResults;
	}
	
    protected List<LinkedMessageMap<String, Object>> buildInputToolMessages(ToolAgentMessage toolAgentMessage,AIAgent agent,ChatObject chatObject) {
        List<FunctionTool> tools = toolAgentMessage.getFunctionTools();
        List<LinkedMessageMap<String, Object>> toolMessages = new ArrayList<>(tools.size());
		PermissionEngine permissionEngine = null;
		boolean needStorePermissionEngine = false;
		
		try {
			PermissionGate permissionGate = null;
			AgentRuntimeContext agentRuntimeContext = null;
			List<ToolCallAskResult> toolCallAskResults = null;
            AgentTraceHolder.setChatObject(chatObject);
			List<PermissionVerdict> pendingAsk = null;
			Map<String, PermissionVerdict> autoDeniedIds = null;
			boolean error = false;
			String errorMessage = null;
			long startTime = System.currentTimeMillis();
			try {
				
				agentRuntimeContext = chatObject.getChatContext().getAgentRuntimeContext();
				permissionGate = evaluatePermissions(tools, chatObject);
				
			
//			Map<String,PermissionEngine> toolPermissionEngines = new LinkedHashMap<>();
				
				if (permissionGate != null) {
					pendingAsk = permissionGate.getPendingAsk();
					autoDeniedIds = permissionGate.getAutoDeniedIds();
					permissionEngine = permissionGate.getPermissionEngine();
				}
//			if(pendingAsk != null){
//				for(PermissionVerdict permissionVerdict : pendingAsk)
//					if(permissionVerdict.getPermissionEngine() != null) {
//						toolPermissionEngines.put(permissionVerdict.getFunctionTool().getId(), permissionVerdict.getPermissionEngine());
//					}
//			}
				// Handle pending ask
				toolCallAskResults = this.toolCallAsk(chatObject, agentRuntimeContext, pendingAsk);
			}
			catch (Exception e) {
				logger.error("Evaluate permissions failed:", e);
				errorMessage = "HITL Evaluate permissions ask failed:" + e.getMessage();	
				error = true;
				//记录模型调用异常轨迹消息
				TraceMessage traceMessage = new TraceMessage();
				LinkedMessageMap  message = new LinkedMessageMap();
				message.put("error", SimpleStringUtil.exceptionToString(e));
//        tracemessage.put("role", SessionMessage.MESSAGE_TYPE_LLMINPUTMESSAGE_NAME);
				message.put("role", SessionMessage.MESSAGE_TYPE_LLMCALLERROR_MESSAGE_NAME);
				traceMessage.setMessage(message);
				traceMessage.setStartTime(startTime);
				traceMessage.setEndTime(System.currentTimeMillis());
				agent.recordTraceMessage(traceMessage);
				
			}
			if(!error) {
				for (FunctionTool tool : tools) {
					String toolId = tool.getId();
					String functionName = tool.getFunctionName();
					FunctionCall functionCall = agent.getFunctionCall(functionName);
//				PermissionEngine permissionEngine = toolPermissionEngines.get(toolId);
					LinkedMessageMap<String, Object> toolMessage = null;
					
					
					if (autoDeniedIds != null) {
						PermissionVerdict denyPermissionVerdict = autoDeniedIds.get(toolId);
						if (denyPermissionVerdict != null) {
							toolMessage = MessageBuilder.buildToolMessage("Permission denied by rules",
									toolId, tool, ToolCallState.DENIED);
							toolMessages.add(toolMessage);
							continue;
						}
					}
					ToolCallAskResult toolCallAskResult = getToolCallAskResult(toolId, toolCallAskResults);
					ToolCallState toolCallState = null;
					if (toolCallAskResult != null) {
						if (!toolCallAskResult.isApproved()) {
							String toolCallAskResultHitlConfirm = toolCallAskResult.getHitlConfirm();
							String deniedReason = toolCallAskResultHitlConfirm != null ? "Tool call ask denied: " + toolCallAskResultHitlConfirm : "Tool call ask denied";
							toolMessage = MessageBuilder.buildToolMessage(deniedReason, toolId, tool, ToolCallState.DENIED);
							toolMessages.add(toolMessage);
							continue;
						} else {
							Map<String, Object> updateInput = toolCallAskResult.getUpdateInput();
							if (updateInput != null) {
								tool.setArguments(updateInput);
							}
							PermissionRule choosedAlwaysPermissionRule = toolCallAskResult.getChoosedAlwaysPermissionRule();
							if (choosedAlwaysPermissionRule != null) {
								
								if (permissionEngine != null) {
									permissionEngine.addRule(choosedAlwaysPermissionRule);
									needStorePermissionEngine = true;
								}
							}
							
						}
					}
					
					Object result = null;
					try {
						if (functionCall == null) {
							result = "Function[" + functionName + "]'s function call is undefined.";
							toolCallState = ToolCallState.FAILED;
						} else {
							result = functionCall.call(tool);
							toolCallState = ToolCallState.FINISHED;
							if (result == null) {
								result = "Call function return null.";
//							throw new FunctionCallException("FunctionCall of " + functionName + " return null:" + JsonUtil.object2json(tool));
							}
						}


//                return toolMessage;
						
					} catch (Exception e) {
						logger.error("Call function[" + functionName + "] failed:", e);
						toolCallState = ToolCallState.FAILED;
						result = "Call function failed:" + e.getMessage();
//                    throw new FunctionCallException("Call tool function[" + functionName + "] failed:", e);
					}
					
					if (result instanceof MCPToolCallResponse) {
						result = ((MCPToolCallResponse) result).getResult();
					}
					String _result = null;
					if (result instanceof String) {
						_result = (String) result;
					} else {
						try {
							_result = JsonUtil.object2json(result);
						} catch (Exception e) {
							logger.error("Convert result to json failed:", e);
							toolCallState = ToolCallState.FAILED;
							_result = "Convert result to json failed:" + e.getMessage();
						}
					}
					toolMessage = MessageBuilder.buildToolMessage(_result, toolId, tool, toolCallState);
					toolMessages.add(toolMessage);
				}
				//记录用户设置的总是允许或拒绝或ask的权限规则，不保存静态规则，避免静态规则变化后不能让最新的静态规则生效
				if (permissionEngine != null && needStorePermissionEngine) {
					TraceMessage agentToolPermissionRules = new TraceMessage();
					LinkedMessageMap tracemessage = new LinkedMessageMap();
					tracemessage.setAgentId(agent.getAgentId());
					PermissionRules permissionRules = permissionEngine.getSessionAlwaysPermissionRules();
					tracemessage.put(PermissionRules.PERMISSION_RULES_KEY, JsonUtil.object2json(permissionRules));
					tracemessage.put("role", SessionMessage.MESSAGE_TYPE_AGENTTOOLPERMISSIONRULES_MESSAGE_NAME);
					agentToolPermissionRules.setMessage(tracemessage);
					agent.recordTraceMessage(agentToolPermissionRules);
				}
			}
			else{
				LinkedMessageMap<String, Object> toolMessage = null;
				for (FunctionTool tool : tools) {
					String toolId = tool.getId();
					toolMessage = MessageBuilder.buildToolMessage(errorMessage,
							toolId, tool, ToolCallState.FAILED);
					toolMessages.add(toolMessage);
				}
				 
			}
			
		}
        finally {
            AgentTraceHolder.removeChatObject();
        }
        return toolMessages;
    }
	/**
    protected Map<String, Object> buildInputToolMessage(ToolAgentMessage toolAgentMessage,AIAgent aiAgent) {
        FunctionTool tool = toolAgentMessage.getFunctionTool();
        String toolId = tool.getId();
        String functionName = tool.getFunctionName();
        FunctionCall functionCall = aiAgent.getFunctionCall(functionName);
        try {
            if(functionCall == null){
                throw new FunctionCallException("FunctionCall of "+ functionName +" is null.");
            }
            Object result = functionCall.call(tool);
            if(result == null){
                throw new FunctionCallException("FunctionCall of "+ functionName +" return null:"+JsonUtil.object2json(tool));
            }
            Map<String,Object> toolMessage = null;
            if(result instanceof String)
                toolMessage = MessageBuilder.buildToolMessage((String)result,toolId,tool);
            else if (result instanceof MCPToolCallResponse){
                result = ((MCPToolCallResponse)result).getResult();
                toolMessage = MessageBuilder.buildToolMessage(JsonUtil.object2json(result),toolId,tool);
            }
			else {
				toolMessage = MessageBuilder.buildToolMessage(JsonUtil.object2json(result),toolId,tool);
			}
            return toolMessage;
        } catch (Exception e) {
            throw new FunctionCallException("Call tool function["+ functionName +"] failed:",e);
        }
    }
	 */
    /**
     * 构建智能问答请求参数
     * @param toolAgentMessage
     * @return
     */
    public Map buildOpenAIRequestMapWithTool(ToolAgentMessage toolAgentMessage, AIAgent agent,ChatObject chatObject,ChatContext chatContext){
//        Map<String, Object> userMessage = buildInputToolMessage(  toolAgentMessage,aiAgent);
		List<LinkedMessageMap<String, Object>> sessionMemory = agent.getSessionMemory(true);
        List<LinkedMessageMap<String, Object>> userMessages = buildInputToolMessages(  toolAgentMessage,agent,chatObject);
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("model", toolAgentMessage.getModel());

        List<LinkedMessageMap<String, Object>> messages = null;
   
        if(sessionMemory != null){
            // 构建消息历史列表，包含之前的会话记忆           
            for(LinkedMessageMap<String, Object> userMessage : userMessages) {

                // 添加当前用户消息
                toolAgentMessage.addSessionMessage(userMessage, agent);
            }
			agent.compact(chatContext,agent,sessionMemory);
//			if(compactionManager != null && !persistentMessage.isAgentResultMessage()){
//				List<LinkedMessageMap<String,Object>> compactMessages = compactionManager.compact(agent,sessionMemory );
//				if(sessionMemory != compactMessages){
//					sessionMemory.clear();
//					sessionMemory.addAll(compactMessages);
//				}
//			}
            messages = new ArrayList<>(sessionMemory);


        }
        else{
            messages = new ArrayList<>();
            for(LinkedMessageMap<String, Object> userMessage : userMessages) {
                messages.add(userMessage);
                // 添加当前用户消息
//                toolAgentMessage.addSessionMessage(userMessage, aiAgent);
            }
            
        }
		
        requestMap.put("messages", messages);
		Boolean stream = chatContext.getStreamable();
		if(stream == null){
			stream = toolAgentMessage.getStream();
		}
        Map parameters = toolAgentMessage.getParameters();
        if(SimpleStringUtil.isNotEmpty( parameters)){

            requestMap.putAll(parameters);
            if(!parameters.containsKey("stream") && stream != null){
                requestMap.put("stream", stream);
            }
            if(!parameters.containsKey("temperature") && toolAgentMessage.getTemperature() != null){
                requestMap.put("temperature", toolAgentMessage.getTemperature());
            }

            if(!parameters.containsKey("max_tokens") && toolAgentMessage.getMaxTokens() != null){
                requestMap.put("max_tokens", toolAgentMessage.getMaxTokens());
            }
        }
        else {
            //设置默认参数
            if( stream != null){
                requestMap.put("stream", stream);
            }

            if( toolAgentMessage.getTemperature() != null){
                requestMap.put("temperature", toolAgentMessage  .getTemperature());
            }
            if( toolAgentMessage.getMaxTokens() != null){
                requestMap.put("max_tokens", toolAgentMessage.getMaxTokens());
            }
        }
		//"stream_options": {"include_usage": true}
		buildincludeUsage(  stream,  toolAgentMessage, requestMap);
        buildThinking(  toolAgentMessage, chatObject, requestMap);
        if(agent.getEnableLoopToolCall() != null && agent.getEnableLoopToolCall()) {
            int maxLoopToolCalls = agent.getMaxLoopToolCalls();
            boolean buildTools = true;
            if(maxLoopToolCalls > 0){
                int loopToolCalls = chatContext.increamentLoopToolCalls();
                //判断工具调用轮次是否超过最大值，如果超过最大值，将不再往上下文中添加工具调用信息
                if(loopToolCalls > maxLoopToolCalls){                    
                    buildTools = false;
                    logger.info("Loop tool calls exceeds max loop tool calls {} and stop loop tool call.",  maxLoopToolCalls);
                }
            }
            if(buildTools) {
                buildTools(chatContext, toolAgentMessage, agent, requestMap);
            }
        }
        return requestMap;
    }
    
    protected void buildThinking(ChatAgentMessage chatAgentMessage,ChatObject chatObject,Map<String, Object> requestMap){
//        Map parameters = chatAgentMessage.getParameters();
        Boolean thinking = chatAgentMessage.getThinking();
        ChatContext chatContext = chatObject.getChatContext();
        if(chatContext != null && chatContext.getThinking() != null){
            thinking = chatContext.getThinking();
            
        }		
		if(thinking != null){
			if( thinking == false) {
				Map data = new LinkedHashMap();
				data.put("type", "disabled");
				requestMap.put("thinking", data);
				chatObject.setThinking(false);
			}
			else{
				Map data = new LinkedHashMap();
				data.put("type", "enabled");
				requestMap.put("thinking", data);
				chatObject.setThinking(true);
			}
		}
        
//        if(thinking != null){
//            if(parameters != null) {
//                if (!parameters.containsKey("thinking")) {
//                    Map data =  new LinkedHashMap();
//                    data.put("type", thinking?"enabled":"disabled");
//                    requestMap.put("thinking", data);
//                }
//            }
//            else{
////                chatAgentMessage.addMapParameter("thinking", "type", "enabled");//kimi-k2.5禁用思维模式,启用：enabled
//                Map data =  new LinkedHashMap();
//                data.put("type", thinking?"enabled":"disabled");
//                requestMap.put("thinking", data);
//            }
//            chatObject.setThinking(thinking);
//        }   
//        else{
//            if (parameters !=null && parameters.containsKey("thinking")) {
//                Map data =  (Map) parameters.get("thinking");
//                String type = (String)data.get("type");
//                if("enabled".equals(type)){
//                    chatObject.setThinking(true);
//                }
//                else{
//                    chatObject.setThinking(false);
//                }
//              
//            }
//        }
        
        
        
    }
    /**
     * 构建智能问答请求参数
     * @param chatAgentMessage
     * @return
     */
    public Map buildOpenAIRequestMap(ChatAgentMessage chatAgentMessage, AIAgent agent,ChatObject chatObject, ChatContext chatContext) {
		
		List<LinkedMessageMap<String, Object>> sessionMemory = agent.getSessionMemory(true);
        String agentId = agent.getAgentId();
        String message = getPrompt(  chatAgentMessage,   agent);
        if(SimpleStringUtil.isEmpty(message)){
            throw new AIRuntimeException("Prompt message is empty.");
        }
		
        if(chatContext != null){
            message = chatContext.evalPrompt(message);
        }
		LinkedMessageMap<String, Object> userMessage = MessageBuilder.buildUserMessage( message);
		LinkedMessageMap<String,Object> systemMessage = null;
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("model", chatAgentMessage.getModel());

        List<LinkedMessageMap<String, Object>> messages = null;
        if(sessionMemory != null){
            // 构建消息历史列表，包含之前的会话记忆           

            if(sessionMemory.size() == 0){
                String systemPrompt = getSystemPrompt(chatAgentMessage,agent);
                if(systemPrompt != null){
                    if(chatContext != null){
                        systemPrompt = chatContext.evalSystemPrompt(systemPrompt);
                    }
                    systemMessage = MessageBuilder.buildSystemMessage(systemPrompt);
                    chatAgentMessage.addSessionMessage(systemMessage,message,agent);
                }
            }
            // 添加当前用户消息
            chatAgentMessage.addSessionMessage(userMessage,agentId,agent);	
			if(agent.getAgentId() != null && agent.getAgentId().equals("__summary__")){
				logger.info("Using summary agent to generate summary.");
			}
		
			agent.compact(chatContext,agent,sessionMemory);
            messages = new ArrayList<>(sessionMemory);
            
            
        }
        else{
            messages = new ArrayList<>();
            String systemPrompt = getSystemPrompt(chatAgentMessage,agent);
            if(systemPrompt != null){
                if(chatContext != null){
                    systemPrompt = chatContext.evalSystemPrompt(systemPrompt);
                }
                systemMessage = MessageBuilder.buildSystemMessage(systemPrompt);                 
                messages.add(systemMessage);
            }
            messages.add(userMessage);
        }
        
        requestMap.put("messages", messages);
        Map parameters = chatAgentMessage.getParameters();
		Boolean stream = chatContext.getStreamable();
		if(stream == null){
			stream = chatAgentMessage.getStream();
		}
        if(SimpleStringUtil.isNotEmpty( parameters)){

            requestMap.putAll(parameters);
            if(!parameters.containsKey("stream") && stream != null){
                requestMap.put("stream", stream);
            }
            if(!parameters.containsKey("temperature") && chatAgentMessage.getTemperature() != null){
                requestMap.put("temperature", chatAgentMessage.getTemperature());
            }

            if(!parameters.containsKey("max_tokens") && chatAgentMessage.getMaxTokens() != null){
                requestMap.put("max_tokens", chatAgentMessage.getMaxTokens());
            }
        }
        else {
            //设置默认参数
            if( stream != null){
                requestMap.put("stream", stream);
            }
            
            if( chatAgentMessage.getTemperature() != null){
                requestMap.put("temperature", chatAgentMessage.getTemperature());
            }
            if( chatAgentMessage.getMaxTokens() != null){
                requestMap.put("max_tokens", chatAgentMessage.getMaxTokens());
            }
        }
		//"stream_options": {"include_usage": true}
		buildincludeUsage(  stream,  chatAgentMessage, requestMap);
        buildThinking(  chatAgentMessage,chatObject, requestMap);
        buildTools(chatContext,chatAgentMessage,agent, requestMap);
        return requestMap;
    }
    public abstract ImageEvent buildGenImageResponse(ClientConfiguration config, ImageAgentMessage imageAgentMessage,StoreChatObject storeChatObject,Map imageData);
   
  
    public StoreChatObject buildGenImageRequestParameter(ClientConfiguration clientConfiguration, Object imageAgentMessage,AIAgent aiAgent){
        StoreChatObject storeChatObject = new StoreChatObject();
        if(imageAgentMessage instanceof ImageAgentMessage){
            ImageAgentMessage temp = (ImageAgentMessage)imageAgentMessage;
            imageAgentMessage = buildGenImageRequestMap(temp,aiAgent);
//            temp.setGenImageCompletionsUrl(this.getGenImageCompletionsUrl(temp));
            storeChatObject.setGenFileStoreDir(clientConfiguration.getExtendConfig("genFileStoreDir"));
            storeChatObject.setEndpoint(clientConfiguration.getExtendConfig("endpoint"));
            storeChatObject.setStoreImageType(clientConfiguration.getExtendConfig("storeImageType"));

            if(storeChatObject.getGenFileStoreDir() != null)
                storeChatObject.setGenFileStoreDir(storeChatObject.getGenFileStoreDir().trim());
            if(storeChatObject.getEndpoint() != null)
                storeChatObject.setEndpoint(storeChatObject.getEndpoint().trim());
            if(storeChatObject.getStoreImageType() != null)
                storeChatObject.setStoreImageType(storeChatObject.getStoreImageType().trim());
        }
        storeChatObject.setMessage(imageAgentMessage);
        return storeChatObject;
        
    }
    
 
    public SSEHeaderSetFunction getAudioGenSSEHeaderSetFunction(){
        return SSEHeaderSetFunction.DEFAULT_SSEHEADERSETFUNCTION;
    }
    public Map<String,Object> buildEmbeddingMessage(ClientConfiguration config,EmbeddingMessage embeddingMessage,AIAgent aiAgent){
        Map params = new HashMap();
        params.put("input", embeddingMessage.getInput());//设置将要向量化的数据
        params.put("model", embeddingMessage.getModel());
        if(embeddingMessage.getParameters() != null && embeddingMessage.getParameters().size() > 0){
            params.putAll(embeddingMessage.getParameters());
        }
        return params;
    }
    public ChatObject buildOpenAIRequestParameter(ClientConfiguration clientConfiguration,Object agentMessage, AIAgent aiAgent,ChatContext chatCallback){
        AgentMessage _agentMessage = null;
        if(agentMessage instanceof AgentMessage){
            _agentMessage =  ((AgentMessage)agentMessage);
        }          
        else if (agentMessage instanceof Map){
            _agentMessage = new MapAgentMessage((Map)agentMessage);
        }
        else{
            _agentMessage = new ObjectAgentMessage(agentMessage);
        }
        ChatObject chatObject = _agentMessage.buildChatObject(clientConfiguration,this,   aiAgent,   chatCallback);
        return chatObject;
         
 
    }
    protected abstract Map<String, Object> buildGenAudioRequestMap(AudioAgentMessage audioAgentMessage,AIAgent aiAgent, ChatContext chatContext);
  
    public Map<String, Object> _buildGenAudioRequestMap(AudioAgentMessage audioAgentMessage,StoreChatObject storeChatObject,
                                                        ClientConfiguration clientConfiguration,AIAgent aiAgent, ChatContext chatContext){

        if(storeChatObject.getGenFileStoreDir() == null)
            storeChatObject.setGenFileStoreDir(clientConfiguration.getExtendConfig("genFileStoreDir"));
        if(storeChatObject.getEndpoint() == null)
            storeChatObject.setEndpoint(clientConfiguration.getExtendConfig("endpoint"));
        if(storeChatObject.getStoreAudioType() == null){
            storeChatObject.setStoreAudioType(clientConfiguration.getExtendConfig("storeAudioType"));
        }
		Boolean stream = chatContext.getStreamable();
		if(stream == null){
			stream = audioAgentMessage.getStream();
		}
		
        Map params = buildGenAudioRequestMap(audioAgentMessage,aiAgent,   chatContext);
//        audioAgentMessage.setGenAudioCompletionsUrl(getGenAudioCompletionsUrl(audioAgentMessage));
        if(stream != null){
            params.put("stream", stream);
			
        }
		//"stream_options": {"include_usage": true}
		buildincludeUsage(  stream,  audioAgentMessage, params);
        return params;
    }

    public Map<String, Object> _buildGetVideoResultRquestMap(VideoStoreAgentMessage videoStoreAgentMessage,StoreChatObject storeChatObject,ClientConfiguration clientConfiguration){

        if(storeChatObject.getGenFileStoreDir() == null)
            storeChatObject.setGenFileStoreDir(clientConfiguration.getExtendConfig("genFileStoreDir"));
        if(storeChatObject.getEndpoint() == null)
            storeChatObject.setEndpoint(clientConfiguration.getExtendConfig("endpoint"));
        if(storeChatObject.getStoreVideoType() == null){
            storeChatObject.setStoreVideoType(clientConfiguration.getExtendConfig("storeVideoType"));
        }
       
        return buildGetVideoResultRquestMap(  videoStoreAgentMessage);
    }

    protected abstract Map<String, Object> buildGetVideoResultRquestMap(VideoStoreAgentMessage videoStoreAgentMessage);

    /**
     * 构建音频生成请求参数
     * @param clientConfiguration
     * @param audioAgentMessage
     * @return
     */
    public StoreChatObject buildGenAudioRequestParameter(ClientConfiguration clientConfiguration, Object audioAgentMessage,AIAgent aiAgent, ChatContext chatCallback) {
        StoreChatObject storeChatObject = new StoreChatObject();
        if(audioAgentMessage instanceof AudioAgentMessage){
            AudioAgentMessage temp = (AudioAgentMessage)audioAgentMessage;
            audioAgentMessage = this._buildGenAudioRequestMap(temp,storeChatObject,clientConfiguration,aiAgent,   chatCallback);
             
           
        }
        storeChatObject.setMessage(audioAgentMessage);
        return storeChatObject;
    }

    public abstract AudioEvent buildGenAudioResponse(ClientConfiguration config, AudioAgentMessage message,StoreChatObject storeChatObject, Map data);

    public Map buildAudioSTTRequestMap(AudioSTTAgentMessage audioSTTAgentMessage, AIAgent aiAgent,ChatContext chatContext) {

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("model", audioSTTAgentMessage.getModel());
		List<LinkedMessageMap<String, Object>> sessionMemory = aiAgent.getSessionMemory(true);
        // 构建消息历史列表，包含之前的会话记忆
        List<LinkedMessageMap<String, Object>> messages = sessionMemory !=  null?
                new ArrayList<>(sessionMemory):new ArrayList<>();
        Object audio = audioSTTAgentMessage.getAudio();
        // 添加当前用户消息
		LinkedMessageMap<String, Object> userMessage = null;
        String prompt = getPrompt(  audioSTTAgentMessage,   aiAgent);
        if(chatContext != null){
            prompt = chatContext.evalPrompt(prompt);
        }
        if(audio != null) {
            userMessage = MessageBuilder.buildAudioSystemMessage(prompt);
        }
        else{
            userMessage = MessageBuilder.buildAudioUserMessage(prompt);
        }
        messages.add(userMessage);
        audioSTTAgentMessage.addSessionMessage(userMessage,aiAgent);
       
        if(audio != null) {
            AudioDataBuilder audioDataBuilder = audioSTTAgentMessage.getAudioDataBuilder();
            if (audioDataBuilder == null) {
                audioDataBuilder = () -> {
                    String base64Audio = null;


                    if (audio instanceof File) {

                        try {
                            byte[] audioBytes = FileUtil.getBytes((File) audio);
                            String contentType = audioSTTAgentMessage.getContentType();
                            if (contentType == null) {
                                contentType = "audio/wav";
                            }
                            base64Audio = "data:" + contentType + ";base64," +
                                    Base64.getEncoder().encodeToString(audioBytes);
                        } catch (IOException e) {
                            throw new AIRuntimeException(e);
                        }

                    } else if (audio instanceof byte[]) {
                        base64Audio = "data:" + audioSTTAgentMessage.getContentType() + ";base64," +
                                Base64.getEncoder().encodeToString((byte[]) audio);
                    } else if (audio instanceof String) {
                        base64Audio = (String) audio;
                    }
                    return base64Audio;

                };
            }

            //直接设置音频url地址
//        MessageBuilder.buildAudioMessage("https://dashscope.oss-cn-beijing.aliyuncs.com/audios/welcome.mp3");
            //将音频文件转换为base64编码
            userMessage = MessageBuilder.buildAudioMessage(audioDataBuilder);

            messages.add(userMessage);
        }
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("messages", messages);
        requestMap.put("input", input);
        Map parameters = audioSTTAgentMessage.getParameters();
        if(parameters != null) {
            requestMap.put("parameters", parameters);
        }
		Boolean stream = chatContext.getStreamable();
		if(stream == null){
			stream = audioSTTAgentMessage.getStream();
		}
        if(stream!= null){
            requestMap.put("stream", stream);
        }
		//"stream_options": {"include_usage": true}
		buildincludeUsage(  stream,  audioSTTAgentMessage, requestMap);
        if(audioSTTAgentMessage.getResultFormat() != null)
            requestMap.put("result_format", audioSTTAgentMessage.getResultFormat());
        return requestMap;
    }
    protected abstract Object buildGenVideoRequestMap(VideoAgentMessage videoAgentMessage,ClientConfiguration clientConfiguration,AIAgent aiAgent);
  
    public StoreChatObject buildVideoRequestParameter(ClientConfiguration clientConfiguration, VideoAgentMessage videoAgentMessage,AIAgent aiAgent) {
        StoreChatObject storeChatObject = new StoreChatObject();
        storeChatObject.setSubmitVideoTaskUrl(getSubmitVideoTaskUrl(  clientConfiguration,  videoAgentMessage));
        storeChatObject.setMessage(this.buildGenVideoRequestMap(videoAgentMessage,clientConfiguration,aiAgent));
        return storeChatObject;
    }

    public abstract VideoTask buildVideoResponseTask(ClientConfiguration clientConfiguration, VideoAgentMessage videoAgentMessage,Map taskInfo);

    public VideoGenResult buildVideoGenResult(ClientConfiguration clientConfiguration,VideoStoreAgentMessage videoStoreAgentMessage,StoreChatObject storeChatObject,Map taskInfo) {
        return null;
    }


 
    public Boolean getCustomThinking(Map parameters) {
        Map thinking = (Map)parameters.get("thinking");
        if(thinking != null){
            String type = (String)thinking.get("type");
            if(type != null ){
                if(type.equals("enabled")) {
                    return true;
                }
                else{
                    return false;
                }
            }
        }
        return null;
    }


    public Map<String, Object> buildRerankMessage(ClientConfiguration config, RerankMessage rerankMessage, AIAgent agent) {
        Map rerankParams = new LinkedHashMap();
        rerankParams.put("model", rerankMessage.getModel());  // 使用项目规范的 rerank 模型
        rerankParams.put("documents", rerankMessage.convertDocuments());
        rerankParams.put("query", rerankMessage.getQuery());
        rerankParams.put("return_documents", rerankMessage.isReturnDocuments());  // 如需返回原始文本可开启
        if(rerankMessage.getParameters() != null && rerankMessage.getParameters().size() > 0)
            rerankParams.put("parameters", rerankMessage.getParameters());
        return rerankParams;
    }

    public List<RerankedDocument> rerank(ClientConfiguration clientConfiguration,RerankMessage rerankMessage, AIAgent agent, Map<String, Object> params) {
        Map response = HttpRequestProxy.sendJsonBody(rerankMessage.getMaas(), params, this.getRerankUrl( clientConfiguration, rerankMessage), Map.class);
        if(logger.isDebugEnabled()) {
            logger.debug("Rerank 响应: {}", JsonUtil.object2json(response));
        }
        List<RerankedDocument> rerankedDocuments = null;
       
        // 解析 Rerank 结果
        if (response != null && response.containsKey("results")) {
            List<Map<String, Object>> rerankResults = (List<Map<String, Object>>) response.get("results");
            if(logger.isDebugEnabled()) {
                logger.debug("========== Rerank 排序结果 ==========");
            }
            List<RerankDocument> rerankDatas = rerankMessage.getRerankDocuments();
            RerankedDocument rerankedDocument = null;
            RerankDocument rerankDocument = null;
            rerankedDocuments = new ArrayList<>();
            for (int i = 0; i < rerankResults.size(); i++) {
                rerankedDocument = new RerankedDocument();
                Map<String, Object> result = rerankResults.get(i);
                int index = (Integer) result.get("index");
                rerankedDocument.setIndex(index);
                double relevanceScore = (Double) result.get("relevance_score");
                rerankedDocument.setRelevanceScore(relevanceScore);
                rerankDocument = rerankDatas.get(index);
                rerankedDocument.setDocument(rerankDocument.getDocument());
                rerankedDocument.setMetadata(rerankDocument.getMetadata());
                rerankedDocument.setVectorScore(rerankDocument.getVectorScore());
                rerankedDocument.setBm25Score(rerankDocument.getBm25Score());
                if(logger.isDebugEnabled()) {
                    logger.debug("[{}] RrfScore: {}, relevance_score: {}, content: {}", i, rerankedDocument.getVectorScore(), relevanceScore,
                            rerankedDocument.getDocument());
                }
                rerankedDocuments.add(rerankedDocument);
                
            }
        }
        return rerankedDocuments;
    }

    public StreamData buildErrorStreamData(Map map, TokenMetrics tokenMetrics) {
        String code =  (String)map.get("code");
        String message = (String) map.get("message");

        if(code != null) {
            return new StreamData(ServerEvent.CONTENT, message, code).setStreamTokenMetrics(tokenMetrics);
        }
		else{
			return new StreamData(ServerEvent.CONTENT, JsonUtil.object2json(map), "error").setStreamTokenMetrics(tokenMetrics);
		}
    }
}
