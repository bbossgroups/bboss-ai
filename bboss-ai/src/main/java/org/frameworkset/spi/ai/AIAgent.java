package org.frameworkset.spi.ai;
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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.frameworkset.util.SimpleStringUtil;
import org.apache.commons.collections.CollectionUtils;
import org.frameworkset.spi.ai.callback.AgentOutput;
import org.frameworkset.spi.ai.compaction.CompactionManagerInf;
import org.frameworkset.spi.ai.context.AgentRuntimeContext;
import org.frameworkset.spi.ai.context.ChatContext;
import org.frameworkset.spi.ai.context.HarnessAgentRuntimeContext;
import org.frameworkset.spi.ai.filesystem.WorkspaceManager;
import org.frameworkset.spi.ai.harness.subagents.DefaultAgentManager;
import org.frameworkset.spi.ai.interceptor.AgentInterceptor;
import org.frameworkset.spi.ai.interceptor.impl.HarnessAgentInterceptor;
import org.frameworkset.spi.ai.material.StoreFilePathFunction;
import org.frameworkset.spi.ai.memory.*;
import org.frameworkset.spi.ai.model.*;
import org.frameworkset.spi.ai.model.tool.AgentToolCallRules;
import org.frameworkset.spi.ai.model.tool.PermissionRules;
import org.frameworkset.spi.ai.store.*;
import org.frameworkset.spi.ai.tool.*;
import org.frameworkset.spi.ai.tools.*;
import org.frameworkset.spi.ai.util.AIAgentUtil;
import org.frameworkset.spi.reactor.DisposeEventHandler;
import org.slf4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.io.Closeable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 多模态智能体对象
 * @author biaoping.yin
 * @Date 2026/1/4
 */
public class AIAgent<T extends AIAgent> implements AgentInfoInf ,AutoCloseable{
	
	
	protected List<AgentInterceptor> agentInterceptors;
	protected DefaultAgentManager defaultAgentManager	;
	
	public List<AgentInterceptor> getAgentInterceptors() {
		return agentInterceptors;
	}
	
	private AgentRuntimeContext agentRuntimeContext;
	/**
	 * 当节点直接隶属于并行节点时，会被赋值为自己的节点id，当所在的并行节点也隶属于其他并行节点时，parentGroupId会被赋值为并行节点的groupid信息
	 * 并行分组展示消息所属并行分支id
	 * 属于同一组的并行任务消息，独立并行展示
	 * 步骤消息：智能体组id
	 */
	private String groupId;
	
	/**
	 * 当节点直接隶属于并行节点时，groupId会被赋值为自己的节点id，当所在的并行节点也隶属于其他并行节点时，parentGroupId会被赋值为并行节点的groupid信息
	 * 并行分组展示消息所属父并行分支id
	 * 属于同一组的并行任务消息，独立并行展示
	 * 步骤消息：智能体组id
	 */
	private String parentGroupId;
    /**
     * 工作流中的智能体节点类型：
	 * 标准化智能体节点、串行容器智能体节点、并行容器智能体节点、条件智能体节点、路由智能体节点
	 * 裁判智能体节点
	 * 
     */
    /**标准化智能体节点*/
    public static final String AGENT_NODE_TYPE_SINGLE = "standard";
    /**
     * 串行容器智能体节点
     */
    public static final String AGENT_NODE_TYPE_SEQUENCE = "sequence";
    /**
     * 并行容器智能体节点
     */
    public static final String AGENT_NODE_TYPE_PARALLEL = "parallel";
    /**
     * 条件智能体节点
     */
    public static final String AGENT_NODE_TYPE_CONDITION = "condition";	
 
	/**路由智能体节点*/
	public static final String AGENT_NODE_TYPE_ROUTE = "route";
	
	/**裁判智能体节点*/
	public static final String AGENT_NODE_TYPE_JUDGE = "judge";
    
    private static Logger logger = org.slf4j.LoggerFactory.getLogger(AIAgent.class);
    protected String prompt;
    protected String systemPrompt;
    /**
     * 智能体节点类型
     */
    protected String agentNodeType = AGENT_NODE_TYPE_SINGLE;
	
	/**通用流程节点*/
	public static final String AGENT_NODE_TYPE_FLOWNODE = "flownode";
//    protected int sessionSize;
	
	/**
	 * 人工介入任务超时时间，单位毫秒，默认-1毫秒,一直不超时
	 */
	protected long hitlTaskTimeout = -1L;
	
	
	
	/**
	 * 自定义参数，用于在智能体运行时，传递额外的参数
	 */	
	protected Map<String,Object> params;
    /**
     * 启用多轮工具调用，true 启用，null或者false不启用
     */
    protected Boolean enableLoopToolCall;

    

    /**
     * 工具调用最大轮数，超过最大轮数后，终止工具调用，直接进行总结
     * 当enableLoopToolCall为true时，该参数才有效
     */
    protected int maxLoopToolCalls;
    

    public String genSubAgentName(String agentId) {
        return getAgentName() + "-" + agentId;
    }
	
	public long getHitlTaskTimeout() {
		return hitlTaskTimeout;
	}
	/**
	 * 设置人工介入任务超时时间，单位毫秒，默认-1毫秒,一直不超时
	 */
	public T setHitlTaskTimeout(long hitlTaskTimeout) {
		this.hitlTaskTimeout = hitlTaskTimeout;
		return (T) this;
	}
	
	/**
     * 输出变量名
     */
    protected String outputVaribleName;

    /**
     * 输出变量名
     */
    protected int outputVaribleScope = AIFlowConst.AIFLOW_VAR_SCOPE_FLOW;
    
    protected AgentOutput agentOutput;
	
	/**
	 * 虚拟主智能体对象，对应的Agent对象，可以是planAgent，可以是不存在的agent
	 */
    protected volatile AgentStore mainSessionStore;
    protected StoreContext storeContext;

    public AIAgent(StoreContext storeContext) {
        this.storeContext = storeContext;

    }
    private Object mainSessionStoreLock = new Object();
    protected void initSessionStore(){
        if(mainSessionStore != null)
            return;
        synchronized (mainSessionStoreLock) {
            if(mainSessionStore != null)
                return;
           
			if(storeContext != null) {
				AgentSessionStoreBuilder agentSessionStoreBuilder = new DefaultAgentSessionStoreBuilder();
				AgentStore mainSessionStore = agentSessionStoreBuilder.build(storeContext, this);
				mainSessionStore.setAIAgent(this);
				mainSessionStore.initMiddlewares(this);
				if (agentMessage != null && agentMessage instanceof SessionAgentMessage) {
					((SessionAgentMessage) agentMessage).setMainSessionStore(mainSessionStore);
				}
				if (storeContext.isResetSession() && storeContext.getSessionId() != null) {
					mainSessionStore.removeSession(storeContext.getSessionId());
				}
				this.mainSessionStore = mainSessionStore;
				this.agentStore = mainSessionStore;
			}
			else{
				AgentStore mainSessionStore = null;
				if(parentAgent != null){
					mainSessionStore = parentAgent.getMainSessionStore();
				}
				if(mainSessionStore == null && parentSessionStore != null){
					mainSessionStore = parentSessionStore.getMainAgentSessionStore();
					if(mainSessionStore == null){
						mainSessionStore = parentSessionStore;
					}
				}
				this.mainSessionStore = mainSessionStore;
				
			}
            
        }
    }
 


    /**
     * true 智能体会话记录不会被记录到父智能体的会议记忆
     * false 会记录
     *
     */
    protected boolean disablePush2ParentLastSubMessage;

    /**
     * true 控制智能体不会加载父智能体的会话记忆
     * false 会加载
     *
     */
    protected boolean disableReferenceParentLastSubMessage;

    /**
     * 工具清单，标准工具规范格式
     */
    protected List<FunctionToolDefine> tools;



    protected boolean disableStream;
    protected boolean sequenceHeaderNode;

    @JsonIgnore
    protected Map<String,FunctionCall> toolCalls;

    @JsonIgnore
    protected ToolsRegist toolsRegist;
	
	@JsonIgnore
	protected List<ToolsRegist> toolsRegists;
    @JsonIgnore
    protected ToolSearcher toolSearcher;
    


    @JsonIgnore
    protected AgentStore agentStore;
    @JsonIgnore
    protected AgentStore parentSessionStore;
    protected String agentId;

    public T setParentAgent(AIAgent parentAgent) {
        this.parentAgent = parentAgent;
        if(this.agentId == null){
            agentId = parentAgent.genSubAgentId();
        }
        return (T)this;
    }

    public boolean isSequenceHeaderNode() {
        return sequenceHeaderNode;
    }

    public T setSequenceHeaderNode(boolean sequenceHeaderNode) {
        this.sequenceHeaderNode = sequenceHeaderNode;
        return (T)this;
    }

    protected AIAgent parentAgent;


    protected String agentName;
    @JsonIgnore
    protected AgentMessage agentMessage;
    public AIAgent(){
        this((String)null);
    }
    public float[] embedding(EmbeddingMessage embeddingMessage){
        return AIAgentUtil.embedding(embeddingMessage,this);
    }
    public List<RerankedDocument> rerank(RerankMessage rerankMessage){
        return AIAgentUtil.rerank(rerankMessage,this);
    }
    public FluxSink<ServerEvent> getAgentFluxSink(){
        return null;
    }
    
    public DisposeEventHandler getDisposeEventHandler(){
        return null;
    }

    public Flux<ServerEvent> getFlux(){
        return null;
    }
    
    public AIAgent getParentAgent(){
        if(parentAgent != null){
            return parentAgent;
        }
        if(this.parentSessionStore != null){
            return parentSessionStore.getAgent();
        }
        return null;
    }
	public String getParentAgentName(){
		AIAgent parent = getParentAgent();
		if(parent != null){
			return parent.getAgentName();
		}
		return null;
	}

    public T setMainSessionStore(AgentStore mainSessionStore) {
        this.mainSessionStore = mainSessionStore;
        return (T)this;
    }

    public T setAgentSessionStore(AgentStore agentStore) {
        this.agentStore = agentStore;
        return (T)this;
    }
    

    public LastSessionMessage getLastSessionMessage(){
        return agentStore != null? agentStore.getLastSubAgentSessionMessage():null;
    }

    public void cleanLastSessionMessages(){
        if(this.agentStore != null){
            agentStore.cleanLastSessionMessages();
        }
    }
    public List<LastSessionMessage> getLastSessionMessages(){
        return agentStore != null? agentStore.getLastSubAgentSessionMessages():null;
    }

    public T setAgentMessage(AgentMessage agentMessage) {
        this.agentMessage = agentMessage;
		if(agentMessage != null && agentMessage.getAgentRuntimeContext() != null && this.agentRuntimeContext == null){
			this.agentRuntimeContext = agentMessage.getAgentRuntimeContext();
			
		}
        return (T)this;
    }

    public AgentMessage getAgentMessage() {
        return agentMessage;
    }

 

    public T setPrompt(String prompt) {
        this.prompt = prompt;
        return (T)this;
    }

    protected AgentIdAssign agentIdAssign = new AgentIdAssign();
    public String genSubAgentId(){
        return this.agentId + "-"+agentIdAssign.getAgentId();
    }
    public String getPrompt() {
        return prompt;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public T setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
        return (T)this;
    }

    public T setParentSessionStore(AgentStore parentSessionStore) {
        this.parentSessionStore = parentSessionStore;
        return (T)this;
    }

    public AgentStore getParentSessionStore() {
        return parentSessionStore;
    }

    public AgentStore getAgentSessionStore() {
        return agentStore;
    }

    public AgentStore getMainSessionStore() {
        if(mainSessionStore != null)
            return mainSessionStore;
        if(parentAgent != null){
            return parentAgent.getMainSessionStore();
        }
        if(storeContext != null){
            initSessionStore();
        }
        return mainSessionStore;
    }

    public AIAgent(String prompt,  ToolsRegist toolsRegist, Integer sessionSize){
        this.prompt = prompt;
        this.toolsRegist = toolsRegist;
//        this.agentId = SimpleStringUtil.getUUID32();
//        if(sessionSize != null ){
//            this.sessionSize = sessionSize;
//        }
//        this.agentId = SimpleStringUtil.getUUID32();
        this.agentName = this.getClass().getName();
//            agentSessionStore = new AgentSessionStoreMemory(sessionSize);
    }

    public T setAgentId(String agentId) {
        this.agentId = agentId;
        return (T)this;
    }

 

    public AIAgent(String prompt,ToolsRegist toolsRegist){
        this(  prompt,   toolsRegist,null);
    }

 

    public AIAgent(ToolsRegist toolsRegist){
        this(  null,   toolsRegist,null);
    }

    public AIAgent(String prompt,String type){
        this(  prompt,  null,null);
    }

    public AIAgent(String prompt){
        this(  prompt,   null,null);
    }

    public AIAgent(String prompt,int sessionSize){
        this(  prompt,   null,sessionSize);
    }
    
    protected LastSessionMessage getLastSubAgentSessionMessage(AgentStore mainSessionStore, AgentMessage agentMessage){
        //UserAgent无需追加父智能体中产生的最新的消息作
        LastSessionMessage lastSubAgentSessionMessage = null;
        if(parentSessionStore != null) {
            lastSubAgentSessionMessage = parentSessionStore.getLastSubAgentSessionMessage( );
        }
        else{
            lastSubAgentSessionMessage = mainSessionStore.getLastSubAgentSessionMessage( );
        }
        return lastSubAgentSessionMessage;
    }
	
	/**
	 * 恢复缓存的智能体权限规则
	 */
	public void restorePermissionRules(ChatObject chatObject){
		if(mainSessionStore != null) {
			PermissionRules permissionRules = mainSessionStore.getAgentPermissionRules(this.getSessionId(), this.agentId);
			if (permissionRules != null) {
				chatObject.restoreCachedPermissionRule(permissionRules);
			}
		}
//		Map<String, Object> permissionRules = null;
//		for(LinkedMessageMap<String,Object> sessionMemoryItem : sessionMemory){
//			String msgAgentId = sessionMemoryItem.getAgentId();
//			//只恢复智能体自身的工具权限规则
//			if(msgAgentId == null || !msgAgentId.equals(this.agentId)){
//				continue;
//			}
//			Map<String, Object> permissionRules_ = (Map<String, Object>)	 sessionMemoryItem.getMetaValue(PermissionRules.PERMISSION_RULES_KEY);
//			if(permissionRules_ != null){
//				permissionRules = permissionRules_;
//			}
//			
//		}
//		if(permissionRules != null){
//			String json = JsonUtil.object2json(permissionRules);
//			agentRuntimeContext.setPermissionRules(JsonUtil.json2Object(json, PermissionRules.class));
//		}
	}
    protected void loadHistoryMessages(AgentStore mainSessionStore, AgentMessage agentMessage){
        if(agentStore == null){
            return;
        }
        List<LinkedMessageMap<String,Object>> sessionMemory = agentStore.getSessionMemory();
        boolean empty = sessionMemory.isEmpty();
//                sessionAgentMessage.setSessionStore(agentSessionStore);
        mainSessionStore.addSubTaskSessionMemory(agentId, agentStore);
		LastSessionMessage lastSubAgentSessionMessage = null;
		if(!isDisableReferenceParentLastSubMessage()) {
//        if(!isDisableGloableStore()) {
            //UserAgent无需追加父智能体中产生的最新的消息作
            lastSubAgentSessionMessage = getLastSubAgentSessionMessage(mainSessionStore, agentMessage);            
        }
		if (empty) {
			//加载历史消息
			List<LinkedMessageMap<String, Object>> sessionMessages = mainSessionStore.getAgentSessionMessage(this,lastSubAgentSessionMessage, agentId);
			
			if (sessionMessages != null && sessionMessages.size() > 0) {
				for (LinkedMessageMap<String, Object> sessionMessage : sessionMessages) {
					agentStore.appendSessionMessageFromParent(this,sessionMessage);
				}
				
				
			}
		} else if (lastSubAgentSessionMessage != null) {//不为空，直接append主智能体中的最后一条消息
			//如果父智能体的最后一个子智能体消息就是智能体自己产生消息,无需添加到自己的消息列表中（因为结果生成后，已经添加到消息列表）
//			restorePermissionRules(    sessionMemory);
			if(!lastSubAgentSessionMessage.getMsgAgentId().equals(this.getAgentId())) {
				
				agentStore.appendSessionMessageFromParent(this,lastSubAgentSessionMessage.getLastSessionMessage());
				//记录消息引用关系
				mainSessionStore.saveLastSessionMessage(lastSubAgentSessionMessage, agentId);
			}
			
		}
    }
	protected boolean inited;
	private void initIntercetors(WorkspaceManager workspaceManager){
		if(inited)
			return;
		synchronized (initLock) {
			if (inited)
				return;
			this.addAgentInterceptor(0,new HarnessAgentInterceptor(workspaceManager))	;
			inited = true;
		}
		
	}
    public void buildAgent(AgentMessage agentMessage){
//		if(agentMessage.getAgentRuntimeContext() != null){
//			AgentRuntimeContext agentRuntimeContext1
//		}
		if(this.agentRuntimeContext == null){
			this.agentRuntimeContext = agentMessage.getAgentRuntimeContext();
		}
		
		
        AgentStore mainSessionStore = this.getMainSessionStore();
        SessionAgentMessage sessionAgentMessage = null;
        if( agentMessage instanceof SessionAgentMessage) {
            sessionAgentMessage = (SessionAgentMessage)agentMessage;
            if(mainSessionStore == null) {
                mainSessionStore = sessionAgentMessage.getMainSessionStore(this);
                
            }
            
        }
        if(this.mainSessionStore == null){
            this.mainSessionStore = mainSessionStore;
        }
        
        if(agentId == null){
            if(parentAgent != null){
                agentId = parentAgent.genSubAgentId();
            }
            else{
                agentId = mainSessionStore != null?mainSessionStore.genSubAgentId(this.agentIdAssign): SimpleStringUtil.getUUID32();
            }
        }
	 
        if(mainSessionStore != null) {
			if(agentRuntimeContext != null){
				agentRuntimeContext.init(mainSessionStore.getWorkspaceManager());
				if(agentRuntimeContext instanceof HarnessAgentRuntimeContext){
					initIntercetors(mainSessionStore.getWorkspaceManager());
					if(!agentRuntimeContext.isDisableMemoryTools()){
						this.registBeanTool(new CompactSummaryMsgSearchTool());
						this.registBeanTool(new SessionSearchTool());
						this.registBeanTool(new MemorySaveTool());
						this.registBeanTool(new MemoryGetTool());
						this.registBeanTool(new MemorySearchTool());
					}
					if(!agentRuntimeContext.isDisableFilesystemTools()){
						
						this.registBeanTool(new FileFunctionTool(mainSessionStore!= null?mainSessionStore.getWorkspaceManager():null
								,agentRuntimeContext.getBaseDirectories()));
					}
					
					if(!agentRuntimeContext.isDisableShellTool()){
						this.registBeanTool(new CLIShellFunctionTool());
					}
					if(!agentRuntimeContext.isDisableWebTools()){
						this.registBeanTool(new WebFetchTool(agentRuntimeContext.getHttpWebfetchToolProxy()));
						this.registBeanTool(new WebSearchTool(agentRuntimeContext.getHttpWebSearchToolProxy()));
					}
				}
				
			}
			
			
            String title  = this.evalTitle(agentMessage);
            AIAgent parentAgent = this.getParentAgent();
			if(title == null) {
				if (parentAgent != null) {
					String tmp = parentAgent.getFirstSubAgentPrompt();
					if (tmp != null) {
						title = tmp;
					}
				}
			}

            mainSessionStore.createOrUpdateSession(title, agentId,this);
            if(parentSessionStore == null && this.parentAgent != null){
                this.parentSessionStore = parentAgent.getAgentSessionStore();
            }
            if(agentStore == null){
                if(parentSessionStore != null)
                    agentStore = buildAgentSessionStore(parentSessionStore );
                else
                    agentStore = buildAgentSessionStore(mainSessionStore );
                agentStore.setAgentId(agentId);
                agentStore.setAIAgent(this);
                agentStore.setMainAgentSessionStore(mainSessionStore);
            }
            loadHistoryMessages(  mainSessionStore,  agentMessage);
			

        }
        
    }
    
    protected AgentStore buildAgentSessionStore(AgentStore parentSessionStore ){
        return new AgentStoreMemory(parentSessionStore );
    }

    /**
     * 实现图片生成功能
     * @param maasName
     * @param imageAgentMessage
     * @return
     */
    public ImageEvent genImage(String maasName, ImageAgentMessage imageAgentMessage ){
        return genImage(  maasName,   imageAgentMessage, (StoreFilePathFunction)null);
    }
    /**
     * 实现图片生成功能
     * @param maasName
     * @param imageAgentMessage
     * @return
     */
    public ImageEvent genImage(String maasName, ImageAgentMessage imageAgentMessage, StoreFilePathFunction storeFilePathFunction){
        buildAgent(  imageAgentMessage);
        return AIAgentUtil.multimodalImageGeneration(maasName, imageAgentMessage,   storeFilePathFunction,this);
    }
    public ImageEvent genImage( ImageAgentMessage imageAgentMessage){
        return genImage(imageAgentMessage.getMaas(), imageAgentMessage );
    }

    /**
     * 提交视频生成任务
     * @param maasName
     * @param videoAgentMessage
     * @return
     */
    public VideoTask submitVideoTask(String maasName,VideoAgentMessage videoAgentMessage){
        buildAgent(  videoAgentMessage);
        return AIAgentUtil.submitVideoTask(maasName,videoAgentMessage,this);
    }
    public VideoTask submitVideoTask(VideoAgentMessage videoAgentMessage){
        buildAgent(  videoAgentMessage);
        return AIAgentUtil.submitVideoTask(videoAgentMessage.getMaas(),videoAgentMessage,this);
    }

    public VideoGenResult getVideoTaskResult(String maasName, VideoStoreAgentMessage videoStoreAgentMessage ){
        return getVideoTaskResult(  maasName,  videoStoreAgentMessage, (StoreFilePathFunction)null);
    }
    public VideoGenResult getVideoTaskResult(String maasName, VideoStoreAgentMessage videoStoreAgentMessage, StoreFilePathFunction storeFilePathFunction){
        if(maasName == null){
            maasName = videoStoreAgentMessage.getMaas();
        }
        return    AIAgentUtil.getVideoTaskResult(maasName,videoStoreAgentMessage,storeFilePathFunction);
    }
    

    public VideoGenResult getVideoTaskResult( VideoStoreAgentMessage videoStoreAgentMessage){
        return AIAgentUtil.getVideoTaskResult(videoStoreAgentMessage.getMaas(),videoStoreAgentMessage,(StoreFilePathFunction)null);
    }
    /**
     * 调用音频合成模型，生成音频
     * @param audioAgentMessage
     * @return
     */
    public AudioEvent genAudio(  AudioAgentMessage audioAgentMessage){
        buildAgent(  audioAgentMessage);
        return AIAgentUtil.multimodalAudioGeneration( audioAgentMessage.getMaas(),audioAgentMessage,(StoreFilePathFunction)null,this);
    }
    public AudioEvent genAudio(  AudioAgentMessage audioAgentMessage, StoreFilePathFunction storeFilePathFunction){
        return genAudio(  audioAgentMessage.getMaas(),  audioAgentMessage,   storeFilePathFunction);
    }
    /**
     * 调用音频合成模型，生成音频
     * @param audioAgentMessage
     * @return
     */
    public Flux<ServerEvent> streamAudioGen(AudioAgentMessage audioAgentMessage){
        buildAgent(  audioAgentMessage);
        return AIAgentUtil.streamAudioGenerationEvent(audioAgentMessage.getMaas(),audioAgentMessage,(StoreFilePathFunction)null,this);
    }

    /**
     * 调用音频合成模型，生成音频
     * @param maasName
     * @param audioAgentMessage
     * @return
     */
    public Flux<ServerEvent> streamAudioGen(String maasName,  AudioAgentMessage audioAgentMessage, StoreFilePathFunction storeFilePathFunction){
        buildAgent(  audioAgentMessage);
        return AIAgentUtil.streamAudioGenerationEvent(maasName,audioAgentMessage,storeFilePathFunction,this);
    }
    public Flux<ServerEvent> streamAudioGen(String maasName,  AudioAgentMessage audioAgentMessage ){
        return streamAudioGen(maasName, audioAgentMessage, (StoreFilePathFunction)null);
    }
    /**
     * 调用音频合成模型，生成音频
     * @param maasName
     * @param audioAgentMessage
     * @return
     */
    public AudioEvent genAudio(String maasName,   AudioAgentMessage audioAgentMessage){
        return genAudio(  maasName,     audioAgentMessage, (StoreFilePathFunction)null);
    }
    /**
     * 调用音频合成模型，生成音频
     * @param maasName
     * @param audioAgentMessage
     * @return
     */
    public AudioEvent genAudio(String maasName,   AudioAgentMessage audioAgentMessage, StoreFilePathFunction storeFilePathFunction){
        buildAgent(  audioAgentMessage);
        return AIAgentUtil.multimodalAudioGeneration(maasName, audioAgentMessage,storeFilePathFunction,this);
    }

    /**
     * 实现流式音频识别处理
     * @param maasName
     * @param audioSTTAgentMessage
     * @return
     */
    public Flux<ServerEvent> streamAudioParser(String maasName,  AudioSTTAgentMessage audioSTTAgentMessage){
        buildAgent(  audioSTTAgentMessage);
//        audioSTTAgentMessage.init();

        return AIAgentUtil.streamChatCompletionEvent(maasName, audioSTTAgentMessage,this);
    }
    /**
     * 实现流式图片识别处理
     * @param maasName
     * @param imageVLAgentMessage
     * @return
     */
    public Flux<ServerEvent> streamImageParser(String maasName,   ImageVLAgentMessage imageVLAgentMessage){
        buildAgent(  imageVLAgentMessage);
//        imageVLAgentMessage.init();
   
        return AIAgentUtil.streamChatCompletionEvent(maasName, imageVLAgentMessage,this);
    }
    /**
     * 实现流式图片识别处理
     * @param videoVLAgentMessage
     * @return
     */
    public Flux<ServerEvent> streamVideoParser(   VideoVLAgentMessage videoVLAgentMessage){
        buildAgent(  videoVLAgentMessage);
        return AIAgentUtil.streamChatCompletionEvent(videoVLAgentMessage.getMaas(), videoVLAgentMessage,this);
    }
    
    /**
     * 实现流式视频识别处理
     * @param maasName
     * @param videoVLAgentMessage
     * @return
     */
    public Flux<ServerEvent> streamVideoParser(String maasName,   VideoVLAgentMessage videoVLAgentMessage){
        buildAgent(  videoVLAgentMessage);

//        videoVLAgentMessage.init();

        return AIAgentUtil.streamChatCompletionEvent(maasName, videoVLAgentMessage,this);
    }
    /**
     * 实现流式图片识别处理
     * @param imageVLAgentMessage
     * @return
     */
    public Flux<ServerEvent> streamImageParser(   ImageVLAgentMessage imageVLAgentMessage){
        buildAgent(  imageVLAgentMessage);
        return AIAgentUtil.streamChatCompletionEvent(imageVLAgentMessage.getMaas(), imageVLAgentMessage,this);
    }
//    /**
//     * 实现流式智能问答功能,在指定的数据源上执行
//     */
//    public Flux<ServerEvent> streamChat(String maasName,   ChatAgentMessage chatAgentMessage){
//    
//        return streamChat(  maasName,     chatAgentMessage,true);
//    }

    /**
     * 实现流式智能问答功能,在指定的数据源上执行
     */
    public Flux<ServerEvent> streamChat(String maasName,   ChatAgentMessage chatAgentMessage ){
        buildAgent(  chatAgentMessage);
//        chatAgentMessage.init();

        return AIAgentUtil.streamChatCompletionEvent(maasName, chatAgentMessage,this);
    }
	
	public Flux<ServerEvent> streamChat(String maasName, String prompt,  ChatAgentMessage chatAgentMessage ){
		if(prompt != null && prompt.length() > 0){
			chatAgentMessage.setPrompt(prompt);
		}
 
		
		return streamChat(  maasName,   chatAgentMessage );
	}
	
	/**
	 * 实现流式智能问答功能,在指定的数据源上执行
	 */
	public Flux<ServerEvent> streamChatWithQuestion(String question, ChatAgentMessage chatAgentMessage){
		return streamChat(chatAgentMessage.getMaas(),   question,   chatAgentMessage );
	}
	/**
     * 实现流式智能问答功能,在指定的数据源上执行
     */
    public Flux<ServerEvent> streamChat( ChatAgentMessage chatAgentMessage){
        buildAgent(  chatAgentMessage);
        return AIAgentUtil.streamChatCompletionEvent(chatAgentMessage.getMaas(), chatAgentMessage,this);
    }

    /**
     * 实现流式智能问答功能,在指定的数据源上执行
     */
    public Flux<ServerEvent> streamChat(String maasName,   ChatAgentMessage chatAgentMessage, ChatContext chatContext ){
        buildAgent(  chatAgentMessage);
//        chatAgentMessage.init();
		
        return AIAgentUtil.streamChatCompletionEvent(maasName, chatAgentMessage,this,chatContext);
    }

    /**
     * 实现流式智能问答功能,在指定的数据源上执行
     */
    public Flux<ServerEvent> streamChat(ChatAgentMessage chatAgentMessage, ChatContext chatContext){
        return streamChat(chatAgentMessage.getMaas(),   chatAgentMessage, chatContext );
    }
    
    /**
     * 实现同步智能问答,在指定的数据源上执行
     * @deprecated 请使用chat方法
     */
    @Deprecated
    public ServerEvent chatCompletionEvent(String maasName,  ChatAgentMessage chatAgentMessage){
        buildAgent(  chatAgentMessage);
        return AIAgentUtil.chatCompletionEvent(maasName,chatAgentMessage,this);
    }
    
    


    public ServerEvent chat(String maasName,  ChatAgentMessage chatAgentMessage ){
		if(this.agentRuntimeContext == null){
			this.agentRuntimeContext = chatAgentMessage.getAgentRuntimeContext();
		}
        ChatContext chatContext = AIAgentUtil.getChatContext( maasName, chatAgentMessage, this);
        return chat(  maasName,   chatAgentMessage,chatContext);
    }
	public ServerEvent chatWithQuestion( String question, ChatAgentMessage chatAgentMessage ){
		return chat(chatAgentMessage.getMaas(),   question,   chatAgentMessage );
	}
	public ServerEvent chat(String maasName, String question, ChatAgentMessage chatAgentMessage ){
		if(question != null && question.length() > 0){
			chatAgentMessage.setPrompt(question);
		}
		 
		return chat(  maasName,   chatAgentMessage);
	}
    /**
     * 实现同步智能问答,在指定的数据源上执行
     */
    public ServerEvent chat(String maasName,  ChatAgentMessage chatAgentMessage,ChatContext chatContext){
        buildAgent(  chatAgentMessage);
        
        ServerEvent serverEvent = AIAgentUtil.chatCompletionEvent(maasName,chatAgentMessage,this,chatContext);
        if(serverEvent != null && serverEvent.getData() != null){
//            Map<String,Object> message = chatAgentMessage.addAssistantSessionMessage(serverEvent.getData());
//            addAgentResultSessionMessage(serverEvent.getData());
            addAgentResultSessionMessage(serverEvent);
            
            if(chatContext != null && chatContext.getChatStreamCallback() != null){
                chatContext.getChatStreamCallback().streamDone(serverEvent);
            }
        }
		 
        return serverEvent;
//        return AIAgentUtil.chatCompletionEvent(maasName,chatAgentMessage);
    }
	

    
    public LastSessionMessage addAgentResultSessionMessage(AgentResultSessionMessageContext agentResultSessionMessageContext,String message){
        LastSessionMessage lastSubAgentSessionMessage = null;
        if(this.agentStore != null ) {

            lastSubAgentSessionMessage = this.agentStore.addAgentResultSessionMessage(    agentResultSessionMessageContext,message);
//            if( !isDisableGloableStore()) {
            if( !isDisablePush2ParentLastSubMessage()) {
                this.agentStore.setParentAgentLastSessionMessage(lastSubAgentSessionMessage);
            }
        }
        return  lastSubAgentSessionMessage;
    }

    public LastSessionMessage addAgentResultSessionMessage(ServerEvent serverEvent){
        LastSessionMessage lastSubAgentSessionMessage = null;
        if(this.agentStore != null ) {

            lastSubAgentSessionMessage = this.agentStore.addAgentResultSessionMessage(serverEvent);
//            if( !isDisableGloableStore()) {
            if( !isDisablePush2ParentLastSubMessage()) {
                this.agentStore.setParentAgentLastSessionMessage(lastSubAgentSessionMessage);
            }
        }
        return  lastSubAgentSessionMessage;
    }

    /**
     * 实现同步智能问答,在指定的数据源上执行
     * @deprecated 请使用chat方法
     */
    @Deprecated
    public ServerEvent chatCompletionEvent(  ChatAgentMessage chatAgentMessage){
        buildAgent(  chatAgentMessage);
        return chat(chatAgentMessage.getMaas(),chatAgentMessage);
    }

    /**
     * 实现同步智能问答,在指定的数据源上执行
     */
    public ServerEvent chat(  ChatAgentMessage chatAgentMessage){
        return chat(chatAgentMessage.getMaas(),    chatAgentMessage);
       
    }

    /**
     * 实现同步智能问答,在指定的数据源上执行
     */
    public ServerEvent chat(ChatAgentMessage chatAgentMessage, ChatContext chatCallback){
        return chat(chatAgentMessage.getMaas(),    chatAgentMessage, chatCallback);

    }

    

    /**
     * 实现同步图片识别处理
     * @param imageVLAgentMessage
     * @return
     */
    public ServerEvent imageParser(  ImageVLAgentMessage imageVLAgentMessage){
        buildAgent( imageVLAgentMessage);
        return AIAgentUtil.imageParser(imageVLAgentMessage.getMaas(), imageVLAgentMessage,this);
    }

    /**
     * 实现同步图片识别处理
     * @param maasName
     * @param imageVLAgentMessage
     * @return
     */
    public ServerEvent imageParser(String maasName,  ImageVLAgentMessage imageVLAgentMessage){
        buildAgent( imageVLAgentMessage);
        return AIAgentUtil.imageParser(maasName, imageVLAgentMessage,this);
    }
    /**
     * 实现同步音频识别处理
     * @param maasName
     * @param audioSTTAgentMessage
     * @return
     */
    public ServerEvent audioParser(String maasName, AudioSTTAgentMessage audioSTTAgentMessage){
        buildAgent( audioSTTAgentMessage);
        return AIAgentUtil.audioParser(maasName,audioSTTAgentMessage,this);
    }
    /**
     * 实现同步音频识别处理
     * @param videoVLAgentMessage
     * @return
     */
    public ServerEvent videoParser( VideoVLAgentMessage videoVLAgentMessage){
        buildAgent( videoVLAgentMessage);
        return AIAgentUtil.videoParser(videoVLAgentMessage.getMaas(),videoVLAgentMessage,this);
    }
    /**
     * 实现同步音频识别处理
     * @param maasName
     * @param videoVLAgentMessage
     * @return
     */
    public ServerEvent videoParser(String maasName, VideoVLAgentMessage videoVLAgentMessage){
        buildAgent( videoVLAgentMessage);
        return AIAgentUtil.videoParser(maasName,videoVLAgentMessage,this);
    }


    public String getAgentId() {
        return this.agentId != null ? this.agentId : agentStore != null?this.agentStore.getAgentId():null;
    }

    public String getParentAgentId() {
        if(parentAgent != null){
            return parentAgent.getAgentId();
        }
        if(agentStore != null) {
            return this.agentStore.getParantAgentId();
        }
        return null;
    }

    public List<LinkedMessageMap<String, Object>> getSessionMemory() {
        if(this.agentStore != null) {
            return this.agentStore.getSessionMemory();
        }
        return getSessionMemory(false);
    }

	public List<LinkedMessageMap<String, Object>> getSessionMemory(boolean create) {
		
		if(agentStore == null && create){
			this.agentStore = new AgentStoreMemory(new ArrayList<>());
			this.agentStore.setAIAgent(this);
			this.mainSessionStore = this.agentStore;
			
		}
		if(this.agentStore == null) {
			return null;
		}
		return this.agentStore.getSessionMemory();
	}

    /**
     * 获取经过搜索过滤后的工具列表
     * 如果未设置 toolSearcher，或 query 为空，则返回全部工具
     */
    public List<FunctionToolDefine> getToolsByToolSearch(ChatContext chatContext,AgentMessage agentMessage) {
        String query = null;
        List<FunctionToolDefine> allTools = getTools();
        if (toolSearcher != null && allTools != null && !allTools.isEmpty()) {
            query = chatContext.evalPrompt(this.evalPrompt(agentMessage));
            if(query != null && !query.trim().isEmpty()) {
                return toolSearcher.search(allTools, query);
            }
        }
        return allTools;
    }
    public List<FunctionToolDefine> getTools() {
        return tools;
    }

    public T setTools(List<FunctionToolDefine> tools) {
        reset();
        this.tools = tools;
        return (T)this;
    }

    public T registTools(List<FunctionToolDefine> tools) {
        reset();
        if(this.tools == null){
            this.tools = new ArrayList<>();
        }
        this.tools.addAll( tools);
        return (T)this;
    }
    private volatile boolean toolInited = false;
    private Object initLock = new Object();

    /**
     * 构建子代理
     * @return
     */
    public AIAgent buildSubAgent(){
        AIAgent agent = new AIAgent();
        agent.setParentAgent(this)
                .setAgentId(genSubAgentId())
                .setAgentName(genSubAgentName(agent.getAgentId()))
                .setMainSessionStore(this.mainSessionStore)
                .setParentSessionStore(this.agentStore);
        
        return agent;
    }
	protected CompactionManagerInf compactionManager;
	protected MemoryMaintenance memoryMaintenance;
	protected MemoryFlushMiddleware memoryFlushMiddleware;
	protected AgentStore parentAgentStore;
	protected WorkspaceManager workspaceManager;
	 
    public void initToolKits(){
        if(toolInited)
            return;
        synchronized (initLock) {
            if(toolInited ){
                return;
            }
            if (this.toolsRegist != null) {
                toolsRegist.init();
                List<FunctionToolDefine> functionToolDefines = this.toolsRegist.registTools();
                if (functionToolDefines != null && functionToolDefines.size() > 0) {
                    FunctionCall functionCall = null;
                    for (FunctionToolDefine functionToolDefine : functionToolDefines) {
                        functionCall = functionToolDefine.getFunctionCall();
                        if (functionCall == null) {
                            functionCall = toolsRegist.getFunctionCall(functionToolDefine.getFunction().getName());
                            if (functionCall != null) {
                                functionToolDefine.setFunctionCall(functionCall);
                            }
                        }
                    }
                    this.registTools(functionToolDefines);
                }
            }

            if (this.toolCalls != null && toolCalls.size() > 0) {
                for (Map.Entry<String, FunctionCall> entry : toolCalls.entrySet()) {
                    FunctionCall functionCall = entry.getValue();
                    String toolName = entry.getKey();
                    if (tools != null && tools.size() > 0) {
                        for (FunctionToolDefine functionToolDefine : tools) {
                            if (functionToolDefine.getFunction().getName().equals(toolName)) {
                                functionToolDefine.setFunctionCall(functionCall);
                                break;
                            }
                        }
                    }

                }
            }
            if (tools != null && tools.size() > 0) {

                for (FunctionToolDefine functionToolDefine : tools) {
                    FunctionCall functionCall = functionToolDefine.getFunctionCall();
                    if (functionCall != null) {
                        String toolName = functionToolDefine.getFunction().getName();
                        if (toolCalls == null) {
                            toolCalls = new LinkedHashMap<>();
                        }
                        if (!toolCalls.containsKey(toolName))
                            toolCalls.put(toolName, functionCall);
                    }

                }
            }
            
            toolInited = true;
        }
    }

//    public boolean isToolThinkingMessage(){
//        init();
//        return this.tools != null && tools.size() > 0 ;
//    }

    public FunctionCall getFunctionCall(String toolName){
        initToolKits();
        if(toolCalls == null)
            return null;
        return toolCalls.get(toolName);
    }

    public T setToolsRegist(ToolsRegist toolsRegist) {     
        return registTools(toolsRegist);
    }
	
	public T registTools(ToolsRegist toolsRegist) {
		reset();
		toolsRegist.init();
		List<FunctionToolDefine> functionToolDefines = toolsRegist.registTools();
		if(functionToolDefines != null && functionToolDefines.size() > 0){	
			 
			FunctionCall functionCall = null;
			for (FunctionToolDefine functionToolDefine : functionToolDefines) {
				functionCall = functionToolDefine.getFunctionCall();
				if (functionCall == null) {
					functionCall = toolsRegist.getFunctionCall(functionToolDefine.getFunction().getName());
					if (functionCall != null) {
						functionToolDefine.setFunctionCall(functionCall);
					}
				}
			}
			this.registTools(functionToolDefines);
			 
		}
		if(this.toolsRegists == null){
			this.toolsRegists = new ArrayList<>();
		}
		this.toolsRegists.add( toolsRegist);
		return (T)this;
	}
    public void destroy(){
        reset();

    }
    private void reset(){
        if(toolInited) {
            synchronized (initLock) {
                if (!toolInited){
                    return;
                }
                if (this.tools != null) {
                    tools.clear();
                }
                if (toolsRegist != null) {
                    toolsRegist.destroy();
                    toolsRegist = null;
                }
                if (this.toolCalls != null) {
                    toolCalls.clear();
                }
                toolInited = false;
            }
        }
    }
    public T registBeanTool(Object beanTool) {
        List<FunctionToolDefine> functionToolDefines = BeanToolHandle.parserTools(beanTool,new BeanToolFunctionCallBuilder() {
            @Override
            public BaseBeanToolFunctionCall buildBeanToolFunctionCall(Method toolMethod, Object toolBean, FunctionToolDefine functionToolDefine, Parameter[] parameters) {
                return BeanToolsRegist._buildBeanToolFunctionCall(  toolMethod,   toolBean,   functionToolDefine,   parameters);
            }
        });
        if(CollectionUtils.isNotEmpty(functionToolDefines)){
            reset();
            if (this.tools == null) {
                tools = new ArrayList<>();
            }
            tools.addAll(functionToolDefines);
        }
        

        return (T) this;
    }
    public T registTool(FunctionToolDefine functionToolDefine) {
        reset();
        if (this.tools == null) {
            tools = new ArrayList<>();
        }
        tools.add(functionToolDefine);

        return (T) this;
    }

    public T registToolCalls(Map<String,FunctionCall> toolCalls) {
        reset();
        if(this.toolCalls == null){
            this.toolCalls = new LinkedHashMap<>();
        }
        this.toolCalls.putAll(toolCalls);
        return (T)this;
    }
    public T registToolCall(String toolName,FunctionCall functionCall) {
        reset();
        if(this.toolCalls == null){
            toolCalls = new LinkedHashMap<>();
        }
        this.toolCalls.put(toolName, functionCall);
        return (T)this;
    }

    public String getAgentName() {
        return agentName;
    }

    public T setAgentName(String agentName) {
        this.agentName = agentName;
        return (T)this;
    }


    public  String evalSystemPrompt(AgentMessage agentMessage){
        String systemPrompt = this.getSystemPrompt();
        if(systemPrompt == null){           
            systemPrompt = agentMessage.getSystemPrompt();            
        }
        if(systemPrompt != null ){
            if(this.getParentAgent() != null) {
                this.getParentAgent().setFirstSubAgentSystemPrompt(systemPrompt);
            }
            else{
                this.setFirstSubAgentSystemPrompt(systemPrompt);
            }
        }
        return systemPrompt;
    }
    public  String evalPrompt(AgentMessage agentMessage){
        String prompt = this.getPrompt();
        if(prompt == null){           
            
            prompt = agentMessage.getPrompt();
            
        }
        if(prompt != null ){
            if(this.getParentAgent() != null) {
                this.getParentAgent().setFirstSubAgentPrompt(prompt);
            }
            else{
                this.setFirstSubAgentPrompt(prompt);
            }
        }
        return prompt;
    }
	
	public  String evalTitle(AgentMessage agentMessage){
		String inputQuery = (String)agentMessage.getContextData("input.query");
		if(inputQuery != null){
			return inputQuery;
		}
		String prompt = this.getPrompt();
		if(prompt == null){
			
			prompt = agentMessage.getPrompt();
			
		}
		if(prompt != null ){
			if(this.getParentAgent() != null) {
				this.getParentAgent().setFirstSubAgentPrompt(prompt);
			}
			else{
				this.setFirstSubAgentPrompt(prompt);
			}
		}
		return prompt;
	}

    private String firstSubAgentPrompt;
    
    public String getFirstSubAgentPrompt(){
        return this.firstSubAgentPrompt;
    }

    public T setFirstSubAgentPrompt(String firstSubAgentPrompt) {
        if(this.firstSubAgentPrompt == null && firstSubAgentPrompt != null) {
            this.firstSubAgentPrompt = firstSubAgentPrompt;
        }
        return (T)this;
    }

    private String firstSubAgentSystemPrompt;
    public T setFirstSubAgentSystemPrompt(String systemPrompt) {
        if(firstSubAgentSystemPrompt == null){
            this.firstSubAgentSystemPrompt = systemPrompt;
        }
        return (T)this;
    }
    public String getFirstSubAgentSystemPrompt(){
        return this.firstSubAgentSystemPrompt;
    }
    public boolean isDisableStream() {
        if(disableStream) {
            return disableStream;
        }
        else if(this.parentAgent != null && this.parentAgent.isDisableStream()){
            return true;
            
        }
        return false;
    }

    public T setDisableStream(boolean disableStream) {
        this.disableStream = disableStream;
        return (T)this;
    }

    public boolean isDisablePush2ParentLastSubMessage() {
        return disablePush2ParentLastSubMessage;
    }

    public T setDisablePush2ParentLastSubMessage(boolean disablePush2ParentLastSubMessage) {
        this.disablePush2ParentLastSubMessage = disablePush2ParentLastSubMessage;
        return (T)this;
    }

    public boolean isDisableReferenceParentLastSubMessage() {
        return disableReferenceParentLastSubMessage;
    }

    public T setDisableReferenceParentLastSubMessage(boolean disableReferenceParentLastSubMessage) {
        this.disableReferenceParentLastSubMessage = disableReferenceParentLastSubMessage;
        return (T)this;
    }

    public String getOutputVaribleName() {
        return outputVaribleName;
    }
    
    public T setOutputVaribleName(String outputVaribleName, int outputVaribleScope) {
        this.outputVaribleName = outputVaribleName;
        this.outputVaribleScope = outputVaribleScope;
        return (T)this;
    }

    public int getOutputVaribleScope() {
        return outputVaribleScope;
    }

    /**
     * 变量范围：流程
     * @return
     */
    public boolean isFlowOutputVaribleScope(){
        return outputVaribleScope == AIFlowConst.AIFLOW_VAR_SCOPE_FLOW;
    }

    /**
     * 变量范围：容器级别
     * @return
     */
    public boolean isContainerOutputVaribleScope(){
        return outputVaribleScope == AIFlowConst.AIFLOW_VAR_SCOPE_CONTAINER;
    }

    /**
     * 变量范围：节点级别
     * @return
     */
    public boolean isNodeOutputVaribleScope(){
        return outputVaribleScope == AIFlowConst.AIFLOW_VAR_SCOPE_NODE;
    }

    public T setAgentOutput(AgentOutput agentOutput) {
        this.agentOutput = agentOutput;
        return (T)this;
    }

    public AgentOutput getAgentOutput() {
        return agentOutput;
    }
	
	/**
	 * 创建或者更新会话
	 * @param prompt
	 * @return
	 */
	
	public boolean createOrUpdateSession(String prompt ){
		String domain = null;
		if(storeContext != null){
			domain = storeContext.getDomain();
		}
		return createOrUpdateSession(  prompt,domain );
	}
	
	/**
	 * 创建或者更新会话
	 * @param prompt
	 * @param domain
	 * @return
	 */
	
	public boolean createOrUpdateSession(String prompt ,String domain){
		this.initSessionStore();
		if(this.mainSessionStore != null)
			return this.mainSessionStore.createOrUpdateSession(prompt,domain,this.agentId,this);
		return false;
	}
    /**
     * 创建或者更新会话
     * @param prompt
     * @return
	 * @Deprecated 请使用createOrUpdateSession方法
     */
    @Deprecated
    public boolean loadSessionMemory(String prompt ){
        String domain = null;
        if(storeContext != null){
            domain = storeContext.getDomain();
        }
        return createOrUpdateSession(  prompt,domain );
    }

	/**
     * 创建或者更新会话
     * @param prompt
     * @param domain
     * @return
	 * @Deprecated 请使用createOrUpdateSession方法
	 */
	@Deprecated
    public boolean loadSessionMemory(String prompt ,String domain){
        
        return this.createOrUpdateSession(prompt,domain);
    }
    
    public T recordTraceMessage(TraceMessage traceMessage){
        try {
            this.initSessionStore();
            if (this.mainSessionStore != null) {
                traceMessage.setAgentId(this.getAgentId());
                traceMessage.setParentAgentId(this.getParentAgentId());
                traceMessage.setAgentNodeType(this.getAgentNodeType());
				traceMessage.setGroupId(this.getGroupId());
				traceMessage.setParentGroupId(this.getParentGroupId());
                this.mainSessionStore.recordTraceMessage(traceMessage);
            }
           
        }
        catch (Exception e){
            if(logger.isDebugEnabled()){
                logger.debug("recordTraceMessage error",e);
            }
        }
        return (T) this;
    }

    public T recordTraceMessage(TraceMessage traceMessage,TokenMetrics tokenMetrics){
        try {
            this.initSessionStore();
            if(this.mainSessionStore != null) {
                traceMessage.setAgentId(this.getAgentId());
                traceMessage.setParentAgentId(this.getParentAgentId());
                traceMessage.setAgentNodeType(this.getAgentNodeType());
				traceMessage.setGroupId(this.getGroupId());
				traceMessage.setParentGroupId(this.getParentGroupId());
                this.mainSessionStore.recordTraceMessage(traceMessage,tokenMetrics);
            }
        }
        catch (Exception e){
            if(logger.isDebugEnabled()){
                logger.debug("recordTraceMessage error",e);
            }
        }
        return (T) this;
    }

    public T setToolSearcher(ToolSearcher toolSearcher) {
        this.toolSearcher = toolSearcher;
        return (T)this;
    }
	
	public T setKeywordToolSearcher(String ...keywords) {
		this.toolSearcher = new KeywordToolSearcher(keywords);
		return (T)this;
	}

    public ToolSearcher getToolSearcher() {
        return toolSearcher;
    }

    public Boolean getEnableLoopToolCall() {
        return enableLoopToolCall;
    }
    /**
     * 启用多轮工具调用，true 启用，null或者false不启用
     */
    public T setEnableLoopToolCall(Boolean enableLoopToolCall) {
        this.enableLoopToolCall = enableLoopToolCall;
        return (T)this;
    }
    /**
     * 工具调用最大轮数，超过最大轮数后，终止工具调用，直接进行总结
     * 当enableLoopToolCall为true时，该参数才有效
     */
    public int getMaxLoopToolCalls() {
        return maxLoopToolCalls;
    }
    /**
     * 工具调用最大轮数，超过最大轮数后，终止工具调用，直接进行总结
     * 当enableLoopToolCall为true时，该参数才有效
     */
    public T setMaxLoopToolCalls(int maxLoopToolCalls) {
        this.maxLoopToolCalls = maxLoopToolCalls;
        return (T)this;
    }

    public String getAgentNodeType() {
        return agentNodeType;
    }
	
	public T addParam(String key,Object value){
		if(params == null){
			params = new java.util.LinkedHashMap<>();
		}
		params.put(key,value);
		return (T)this;
	}
	public Object getParam(String key){
		if(params == null){
			return null;
		}
		return params.get(key);
	}
	
	public Map<String, Object> getParams() {
		return params;
	}
	
	public T addAllParams(Map<String, Object> params) {
		if(this.params == null){
			this.params = new java.util.LinkedHashMap<>();
		}
		this.params.putAll(params);
		return (T)this;
	}
	
	public String getSessionId() {
		if(this.mainSessionStore != null){
			return this.mainSessionStore.getSessionId();
		}
		return null;
	}
	public String getRequestId() {
		if(this.mainSessionStore != null){
			return this.mainSessionStore.getRequestId();
		}
		return null;
	}
	
	public String getUserId() {
		if(this.mainSessionStore != null){
			return this.mainSessionStore.getUserId();
		}
		return null;
	}
	
	public String getGroupId() {
		return groupId;
	}
	
	public String getParentGroupId() {
		return parentGroupId;
	}
	public T setGroupId(String groupId) {
		this.groupId = groupId;
		return (T)this;
	}
	public T setParentGroupId(String parentGroupId) {
		this.parentGroupId = parentGroupId;
		return (T)this;
	}
	
	public Map listModels(String maas,Map params){
		return AIAgentUtil.listModels(maas,params);
	}
	
	public Map listModels(String maas ){
		return AIAgentUtil.listModels(maas );
	}
	
	public static String listModelsString(String maas,Map params){
		return AIAgentUtil.listModelsString(maas,params);
	}
	
	public static String listModelsString(String maas ){
		return AIAgentUtil.listModelsString(maas ,null);
	}
	
	public AgentRuntimeContext getAgentRuntimeContext() {
		if(this.agentRuntimeContext != null) {
			return agentRuntimeContext;
		}
		else if(parentAgent != null && parentAgent != this){
			
			return this.parentAgent.getAgentRuntimeContext();
		}
		return null;
	}
	
	public T setAgentRuntimeContext(AgentRuntimeContext agentRuntimeContext) {
		this.agentRuntimeContext = agentRuntimeContext;
		return (T)this;
	}
	
	public List<LinkedMessageMap<String, Object>> compact(ChatContext chatContext,AIAgent agent, List<LinkedMessageMap<String, Object>> messages) {
		if(getMainSessionStore() != null) {
			return getMainSessionStore().compact(chatContext,agent, messages);
		}
		else{
			return messages;
		}
	}
	
	public void saveSummeryMessage(LinkedMessageMap<String, Object> summaryMessage) {
		PersistentMessage persistentMessage = new PersistentMessage();
		persistentMessage.setMessage(summaryMessage);
		persistentMessage.setGroupId(getGroupId());
		persistentMessage.setParentGroupId(getParentGroupId());
		this.agentStore.saveSummeryMessage(persistentMessage);
	}
	
	public int getNextSeqNo() {
		return this.getMainSessionStore().getNextSeqNo();
	}
	
	public void addAgentToolCallRules(AgentToolCallRules agentToolCallRules){
		if(this.getMainSessionStore() != null)
			this.getMainSessionStore().addAgentToolCallRules(agentToolCallRules);
		
	}
	
	public void updateAgentToolCallRules(AgentToolCallRules agentToolCallRules){
		if(this.getMainSessionStore() != null)
			this.getMainSessionStore().updateAgentToolCallRules(agentToolCallRules);	
		
		
	}
	
	public AgentToolCallRules getAgentToolCallRules(String sessionId, String agentId){
		
		if(this.getMainSessionStore() != null)
			return this.getMainSessionStore().getAgentToolCallRules(sessionId, agentId);	
		return null;
		
	}
	
	
	public String getAgentUUID(){
		if(this.parentAgent != null){
			return parentAgent.getAgentUUID()+":" + this.agentId;
		}
		return agentId;
	}
	public String toString() {
		return getAgentUUID(); 
	}
	
	
	/**
	 * Closes this resource, relinquishing any underlying resources.
	 * This method is invoked automatically on objects managed by the
	 * {@code try}-with-resources statement.
	 *
	 * @throws Exception if this resource cannot be closed
	 * @apiNote While this interface method is declared to throw {@code
	 * Exception}, implementers are <em>strongly</em> encouraged to
	 * declare concrete implementations of the {@code close} method to
	 * throw more specific exceptions, or to throw no exception at all
	 * if the close operation cannot fail.
	 *
	 * <p> Cases where the close operation may fail require careful
	 * attention by implementers. It is strongly advised to relinquish
	 * the underlying resources and to internally <em>mark</em> the
	 * resource as closed, prior to throwing the exception. The {@code
	 * close} method is unlikely to be invoked more than once and so
	 * this ensures that the resources are released in a timely manner.
	 * Furthermore it reduces problems that could arise when the resource
	 * wraps, or is wrapped, by another resource.
	 *
	 * <p><em>Implementers of this interface are also strongly advised
	 * to not have the {@code close} method throw {@link
	 * InterruptedException}.</em>
	 * <p>
	 * This exception interacts with a thread's interrupted status,
	 * and runtime misbehavior is likely to occur if an {@code
	 * InterruptedException} is {@linkplain Throwable#addSuppressed
	 * suppressed}.
	 * <p>
	 * More generally, if it would cause problems for an
	 * exception to be suppressed, the {@code AutoCloseable.close}
	 * method should not throw it.
	 *
	 * <p>Note that unlike the {@link Closeable#close close}
	 * method of {@link Closeable}, this {@code close} method
	 * is <em>not</em> required to be idempotent.  In other words,
	 * calling this {@code close} method more than once may have some
	 * visible side effect, unlike {@code Closeable.close} which is
	 * required to have no effect if called more than once.
	 * <p>
	 * However, implementers of this interface are strongly encouraged
	 * to make their {@code close} methods idempotent.
	 */
	@Override
	public void close() throws Exception {
		MemoryBackgroundTasks.awaitQuiescence(
				5, java.util.concurrent.TimeUnit.SECONDS);
	}
	
	public String onSystemPrompt(ChatObject chatObject,String currentSystem){
		if(agentInterceptors != null) {
			
			if (agentInterceptors != null && agentInterceptors.size() > 0) {
				for (AgentInterceptor agentInterceptor : agentInterceptors) {
					currentSystem = agentInterceptor.onSystemPrompt(this, chatObject, currentSystem);
				}
			}
		}
		return currentSystem;
	}
	
	public T setAgentInterceptors(List<AgentInterceptor> agentInterceptors) {
		this.agentInterceptors = agentInterceptors;
		return (T)this;
	}
	public T addAgentInterceptor(AgentInterceptor agentInterceptor) {
		if(this.agentInterceptors == null)
			this.agentInterceptors = new java.util.ArrayList<AgentInterceptor>();
		this.agentInterceptors.add(agentInterceptor);
		return (T)this;
	}
	public T addAgentInterceptor(int index,AgentInterceptor agentInterceptor) {
		if(this.agentInterceptors == null)
			this.agentInterceptors = new java.util.ArrayList<AgentInterceptor>();
		this.agentInterceptors.add(index,agentInterceptor);
		return (T)this;
	}
	
	public static class Builder {
		
		public AIAgent build(){
			return null;
		}
	}
	public static Builder builder(){
		return new Builder();
	}
	
	/**
	 * The internal {@link DefaultAgentManager} able to
	 * re-materialize this agent's subagents, or {@code null} when none is owned (e.g. session-mode
	 * external subagent tool). Used to wire a gateway materializer for cross-node recovery.
	 */
	public DefaultAgentManager getSubagentAgentManager() {
//		if (subagentMiddleware instanceof SubagentsMiddleware sm) {
//			return sm.getAgentManager();
//		}
//		if (subagentMiddleware instanceof DynamicSubagentsMiddleware dm) {
//			return dm.getAgentManager();
//		}
		return this.defaultAgentManager;
	}
	
}
