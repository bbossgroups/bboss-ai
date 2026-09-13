package org.frameworkset.spi.ai.store;
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
import org.frameworkset.spi.ai.AIAgent;
import org.frameworkset.spi.ai.compaction.CompactionConfig;
import org.frameworkset.spi.ai.compaction.CompactionManager;
import org.frameworkset.spi.ai.compaction.CompactionManagerInf;
import org.frameworkset.spi.ai.compaction.WindowsCompactionManager;
import org.frameworkset.spi.ai.context.ChatContext;
import org.frameworkset.spi.ai.model.*;
import org.frameworkset.spi.ai.store.db.AgentMemoryStoreDB;
import org.frameworkset.spi.ai.util.BaseStreamDataBuilder;
import org.frameworkset.spi.ai.util.MessageBuilder;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.frameworkset.spi.ai.store.SessionMessage.MESSAGE_TYPE_AGENT_RESULTMESSAGE;
import static org.frameworkset.spi.ai.store.SessionMessage.MESSAGE_TYPE_TRACE_MESSAGE;

/**
 * @author biaoping.yin
 * @Date 2026/4/2
 */
public abstract class BaseAgentSessionStore<T extends BaseAgentSessionStore> implements AgentSessionStore<T>{
    private static Logger log = org.slf4j.LoggerFactory.getLogger(BaseAgentSessionStore.class);

    private final static AgentMessageTypeConvertor DEFAULT_AGENTMESSAGETYPECONVERTOR = new AgentMessageTypeConvertor();
    protected AgentMessageTypeConvertor agentMessageTypeConvertor = DEFAULT_AGENTMESSAGETYPECONVERTOR;
    /**
     * 在内存中持久化用户消息
     */
    protected boolean persistentSessionMemory;
     
    /**
     * 用户会话id
     */

    protected String sessionId;



    /**
     * 前端用户请求id，每次请求生成一个
     */
    protected String requestId;
    /**
     * 用户id，可选
     */
    private String userId;

    private String traceId;
    /**
     * 会话对应的agentId
     */
    protected String agentId;
    protected StoreContext storeContext;
	protected CompactionManagerInf compactionManager;
    protected AgentSessionStore parentAgentSessionStore;

    protected AIAgent agent;


    public String genSubAgentId(AgentIdAssign agentIdAssign){

        return this.getAgentId()+"-"+agentIdAssign.getAgentId();
    }

    public void setAgentMessageTypeConvertor(AgentMessageTypeConvertor agentMessageTypeConvertor) {
        if(agentMessageTypeConvertor != null)
            this.agentMessageTypeConvertor = agentMessageTypeConvertor;
    }

    protected AgentSessionStore mainAgentSessionStore;
    /** 短期记忆：使用静态变量存储会话记忆（实际项目中建议使用缓存或数据库）*/
    protected List<LinkedMessageMap<String, Object>> sessionMemory;
    public BaseAgentSessionStore(List<LinkedMessageMap<String, Object>> sessionMemory){
        this.sessionMemory = sessionMemory;

    }
    @Override
    public StoreContext getStoreContext(){
        if(this.storeContext != null){
            return storeContext;
        }
        if(this.mainAgentSessionStore != null && this.mainAgentSessionStore != this){
            return mainAgentSessionStore.getStoreContext();
        }
        return null;
    }
    @Override
    public T setAIAgent(AIAgent aiAgent) {
        this.agent = aiAgent;
        return (T)this;
    }

    @Override
    public AIAgent getAgent() {
        return agent;
    }

 

    public T setSessionMemory(List<LinkedMessageMap<String, Object>> sessionMemory) {
        this.sessionMemory = sessionMemory;
        return (T) this;
    }

    public BaseAgentSessionStore(List<LinkedMessageMap<String, Object>> sessionMemory, int sessionSize){
        this.sessionMemory = sessionMemory;
        this.sessionSize = sessionSize;

    }
    public BaseAgentSessionStore( int sessionSize){
        this.sessionMemory = new ArrayList<>();
        this.sessionSize = sessionSize;

    }
    public BaseAgentSessionStore( AgentSessionStore parentAgentSessionStore,int sessionSize){
        this.parentAgentSessionStore = parentAgentSessionStore;
        this.sessionMemory = new ArrayList<>();
        this.sessionSize = sessionSize;

    }
    public void addSelfSessionMessage(Map<String, Object> message){
        
    }
    public BaseAgentSessionStore(){
        this.sessionMemory = new ArrayList<>();

    }

    public AgentSessionStore getParentAgentSessionStore() {
        return parentAgentSessionStore;
    }

    public BaseAgentSessionStore(StoreContext storeContext,AIAgent agent){
		this.agentMemoryStore = new AgentMemoryStoreDB(storeContext);
		 
        this.persistentSessionMemory = true;
        this.storeContext = storeContext;
        this.sessionId = storeContext.getSessionId();   
        if(this.storeContext.getAgentMessageTypeConvertor() != null){
            this.agentMessageTypeConvertor = storeContext.getAgentMessageTypeConvertor();
        }
        this.userId = storeContext.getUserId();
        if(agent != null) {
            this.agentId = agent.getAgentId();
        }
        this.requestId = storeContext.getRequestId();
        this.traceId = storeContext.getTraceId();
        if(agentId == null){
            this.agentId = "agentId-0";
        }
        this.sessionMemory = storeContext.getSessionMemory();
        this.sessionSize = storeContext.getSessionSize();
        if(sessionMemory == null){
            this.sessionMemory = new ArrayList<>();
        }
		CompactionConfig compactionConfig = storeContext.getCompactionConfig();
	 	if(compactionConfig != null) {
			if (compactionConfig.getCompactionPolicy() == CompactionConfig.COMPACTION_POLICY_WINDOWSIZE) {
				this.compactionManager = new WindowsCompactionManager(compactionConfig);
			} else {
				this.compactionManager = new CompactionManager(compactionConfig);
			}
		}
		 else if(sessionSize > 0){
			 int triggerSessionSize = storeContext.getTriggerSessionSize();
			 // 如果 triggerSessionSize 小于等于 sessionSize，设置一个合理的默认值：sessionSize * 2
			 if (triggerSessionSize <= sessionSize) {
				 triggerSessionSize = sessionSize * 2;
			 }
			this.compactionManager = new WindowsCompactionManager(new CompactionConfig()
					.setCompactModel(storeContext.getCompactModelInfo())
					.setKeepMessages(sessionSize)
					.setTriggerMessages(triggerSessionSize));
		}

    }

    public String getTraceId() {
        return traceId;
    }

    /**
     * 子任务会话记忆
     */
    private Map<String,AgentSessionStore> subTaskSessionMemorys;
	private AgentMemoryStore agentMemoryStore;

    public BaseAgentSessionStore(String sessionId, String userId, String agentId ) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.agentId = agentId;
    }


    public List<LinkedMessageMap<String, Object>> getSessionMemory() {
        return sessionMemory;
    }
    @Override
    public void addSubTaskSessionMemory(String agentId,AgentSessionStore subTaskSessionMemory) {
        if(subTaskSessionMemorys == null){
            subTaskSessionMemorys = new LinkedMessageMap<>();
        }
		if(!subTaskSessionMemorys.containsKey(agentId))
        	this.subTaskSessionMemorys.put(agentId, subTaskSessionMemory);
    }


    public String getParantAgentId(){
        if(parentAgentSessionStore != null)
            return parentAgentSessionStore.getAgentId();
        else if(mainAgentSessionStore != null){
            return mainAgentSessionStore.getAgentId();
        }
        return null;
    }

    @Override
    public AgentSessionStore getSubTaskSessionMemory(String agentId) {
        if(subTaskSessionMemorys == null){
            return null;
        }
        return subTaskSessionMemorys.get(agentId);
    }

 
   
 
    @Override
    public void appendSessionMessageFromParent(AIAgent agent,
											   LinkedMessageMap<String,Object> persistentMessage ){
        appendSessionMessage(agent, persistentMessage);
        
    }
	@Override
	public List<LinkedMessageMap<String, Object>> compact(ChatContext chatContext, AIAgent agent, List<LinkedMessageMap<String, Object>> sessionMemory){
		if(compactionManager != null  ){
			try {
				List<LinkedMessageMap<String,Object>> compactMessages = compactionManager.compact(  chatContext,agent,sessionMemory );
				if(sessionMemory != compactMessages){
					sessionMemory.clear();
					for(int i = 0; i < compactMessages.size() ; i++ ) {
						sessionMemory.add(compactMessages.get(i));
					}
				}
			}
			catch (Exception e){
				log.warn("Compact agent["+agent.getAgentId()+"] session memory error: ignore compact.", e);
				
			}
			
		}
		return sessionMemory;
	} 
    protected void appendSessionMessage(AIAgent agent, 
										LinkedMessageMap<String,Object> persistentMessage ){
        if(sessionMemory == null){
            return ;
        }
//        if(agentId != null && agentId.equals("parrelHotelAgent")){
//            log.info("appendSessionMessage ");
//        }
        sessionMemory.add(persistentMessage);

//		if(persistentMessage.isAgentResultMessage()){
//			//记录消息流水账
//			
//		}
        
    }
    @Override
    public void recordTraceMessage(TraceMessage traceMessage) {
        PersistentMessage persistentMessage = new PersistentMessage();

		persistentMessage.setGroupId(traceMessage.getGroupId());
		persistentMessage.setParentGroupId(traceMessage.getParentGroupId());
		
		LinkedMessageMap<String, Object> message = traceMessage.getMessage();
        persistentMessage.setMessage(message);
        TokenMetrics tokenMetrics = new TokenMetrics();
        tokenMetrics.setStartTime(traceMessage.getStartTime());
        tokenMetrics.setEndTime(traceMessage.getEndTime());
        persistentMessage.setTokenMetrics(tokenMetrics);
        String metadata = null;
        if(traceMessage.getMetaData() != null){
            metadata = JsonUtil.object2json(traceMessage.getMetaData());
        }
		
		String messageType = message.getMessageType();
		if(messageType == null) {
			String role = (String) message.get("role");
			messageType = SimpleStringUtil.isNotEmpty(role) ? messageType(role) : MESSAGE_TYPE_TRACE_MESSAGE;
		}
        
        this.persistentSessionMessage(persistentMessage,traceMessage.getAgentId(), traceMessage.getParentAgentId(), 
                traceMessage.getAgentNodeType(),traceMessage.getSubAgentIdBy(),
                (String)null, metadata, messageType);
    }


    @Override
    public void recordTraceMessage(TraceMessage traceMessage,TokenMetrics tokenMetrics){
        PersistentMessage persistentMessage = new PersistentMessage();
		persistentMessage.setGroupId(traceMessage.getGroupId());
		persistentMessage.setParentGroupId(traceMessage.getParentGroupId());
		LinkedMessageMap<String, Object> message = traceMessage.getMessage();
        persistentMessage.setMessage(message);
        
        tokenMetrics.setStartTime(traceMessage.getStartTime());
        tokenMetrics.setEndTime(traceMessage.getEndTime());
        persistentMessage.setTokenMetrics(tokenMetrics);
        String metadata = null;
        if(traceMessage.getMetaData() != null){
            metadata = JsonUtil.object2json(traceMessage.getMetaData());
        }
		String messageType = message.getMessageType();
        if(messageType == null) {
			String role = (String) message.get("role");
			messageType = SimpleStringUtil.isNotEmpty(role) ? messageType(role) : MESSAGE_TYPE_TRACE_MESSAGE;
		}

        this.persistentSessionMessage(persistentMessage,traceMessage.getAgentId(), traceMessage.getParentAgentId(),
                traceMessage.getAgentNodeType(),traceMessage.getSubAgentIdBy(),
                (String)null, metadata, messageType);
    }

    /**
     * 将角色转换为消息类型messageType，对应agent_session_message表中的messageType字段
     * @param role
     * @return
     */

    private String messageType(String role){
        return this.agentMessageTypeConvertor.convertMessageType(role);
         
    }
	
	
	@Override
	public void saveSummeryMessage(PersistentMessage persistentMessage){
		LinkedMessageMap<String,Object> message = persistentMessage.getMessage();
		String messageType = message.getMessageType();
		if(messageType == null) {
			String role = (String) message.get("role");
			messageType = messageType(role);
		}
		
		if(mainAgentSessionStore != null){
			mainAgentSessionStore.persistentSessionMessage(persistentMessage, agentId,this.getParantAgentId(),this.getAgent().getAgentNodeType(),null,null,null, messageType);
		}
		else if(this.persistentSessionMemory){
			persistentSessionMessage(persistentMessage, agentId,this.getParantAgentId(),this.getAgent().getAgentNodeType(),null,null,null, messageType);
		}
	}
	
	private LinkedMessageMap<String, Object> getPreSummerySessionMessage(List<LinkedMessageMap<String, Object>> sessionMessages,LinkedMessageMap<String, Object> sessionMessage){
		for(LinkedMessageMap<String, Object> sessionMessagePre : sessionMessages){
			if(sessionMessagePre.getMessageType().equals(SessionMessage.MESSAGE_TYPE_SUMMARY_MESSAGE)){
				if(sessionMessagePre.getNextMsgId().equals(sessionMessage.getId())){
					return sessionMessagePre;
				}
				
			}
		}
		return null;
	}
	private SessionMessage getPreSummerySessionMessage(List<SessionMessage> sessionMessages,SessionMessage sessionMessage){
		for(SessionMessage sessionMessagePre : sessionMessages){
			if(sessionMessagePre.getMessageType().equals(SessionMessage.MESSAGE_TYPE_SUMMARY_MESSAGE)){
				if(sessionMessagePre.getSeqNo() == sessionMessage.getSeqNo()){
					return sessionMessagePre;
				}
				
			}
		}
		return null;
	}
	
	/**
	 * 还原压缩状态消息窗口
	 * @param sessionMessages
	 * @return
	 */
	protected List<LinkedMessageMap<String, Object>> refactorLinkedMessageMapSessionMessages(List<LinkedMessageMap<String, Object>> sessionMessages) {
		//将摘要信息移动到对应的消息前面
		List<LinkedMessageMap<String, Object>> sessionMessagesNew = new ArrayList<>();
		LinkedMessageMap<String, Object> systemSessionMessage = sessionMessages.get(0);
		boolean isSystemSessionMessage = systemSessionMessage.getMessageType().equals(SessionMessage.MESSAGE_TYPE_SYSTEM_MESSAGE);
		int position = isSystemSessionMessage ? 1 : 0;
		if(!isSystemSessionMessage){
			systemSessionMessage = null;
		}
		//将摘要信息放到队列中对应的位置
		int lastSummaryPosition = -1;
		for(int i = position; i < sessionMessages.size(); i++ ){
			LinkedMessageMap<String, Object> sessionMessage = sessionMessages.get(i);
			if(!sessionMessage.getMessageType().equals(SessionMessage.MESSAGE_TYPE_SUMMARY_MESSAGE)){
				
				LinkedMessageMap<String, Object> preSummerySessionMessage = getPreSummerySessionMessage(sessionMessages,	sessionMessage);
				if(preSummerySessionMessage != null){
					sessionMessagesNew.add(preSummerySessionMessage);
					lastSummaryPosition = sessionMessagesNew.size() - 1;
					
				}
				sessionMessagesNew.add(sessionMessage);
			}
			
			
		}
		if(lastSummaryPosition > -1){
			if(lastSummaryPosition > 0){
				sessionMessagesNew = sessionMessagesNew.subList(lastSummaryPosition, sessionMessagesNew.size());
			}
		}
		if(systemSessionMessage != null){
			sessionMessagesNew.add(0,systemSessionMessage);
		}
		//获取最后摘要位置（或者摘要前一个system消息）开始的队列并返回		
		return sessionMessagesNew;
	}
	
	protected List<SessionMessage> refactorSessionMessages(List<SessionMessage> sessionMessages) {
		//将摘要信息移动到对应的消息前面
		List<SessionMessage> sessionMessagesNew = new ArrayList<>();
		SessionMessage systemSessionMessage = sessionMessages.get(0);
		boolean isSystemSessionMessage = systemSessionMessage.getMessageType().equals(SessionMessage.MESSAGE_TYPE_SYSTEM_MESSAGE);
		int position = isSystemSessionMessage ? 0 : 1;
		//将摘要信息放到队列中对应的位置
		int lastSummaryPosition = -1;
		for(int i = position; i < sessionMessages.size(); i++ ){
			SessionMessage sessionMessage = sessionMessages.get(i);
			if(!sessionMessage.getMessageType().equals(SessionMessage.MESSAGE_TYPE_SUMMARY_MESSAGE)){
				
				SessionMessage preSummerySessionMessage = getPreSummerySessionMessage(sessionMessages,	sessionMessage);
				if(preSummerySessionMessage != null){
					sessionMessagesNew.add(preSummerySessionMessage);
					
				}
				sessionMessagesNew.add(sessionMessage);
			}
			else {
				lastSummaryPosition = i;
			}
			
		}
		if(lastSummaryPosition > -1){
			if(lastSummaryPosition > 0){				
				sessionMessagesNew = sessionMessagesNew.subList(lastSummaryPosition, sessionMessagesNew.size());
			}
		}
		if(systemSessionMessage != null){
			sessionMessagesNew.add(0,systemSessionMessage);
		}
		//获取最后摘要位置（或者摘要前一个system消息）开始的队列并返回		
		return sessionMessagesNew;
	}
    @Override
    public void addSessionMessage(PersistentMessage persistentMessage){

        if(sessionMemory == null){
            return ;
        }
		LinkedMessageMap<String,Object> message = persistentMessage.getMessage();
		String messageType = message.getMessageType();
		if(messageType == null) {
			String role = (String) message.get("role");
			messageType = messageType(role);
		}
		appendSessionMessage(persistentMessage.getAgent(),message );
		
        if(mainAgentSessionStore != null){
            mainAgentSessionStore.persistentSessionMessage(persistentMessage, agentId,this.getParantAgentId(),this.getAgent().getAgentNodeType(),null,null,null, messageType);
        }
        else if(this.persistentSessionMemory){
            persistentSessionMessage(persistentMessage, agentId,this.getParantAgentId(),this.getAgent().getAgentNodeType(),null,null,null, messageType);
        }
         
    }

    @Override
    public LastSessionMessage addAgentResultSessionMessage(ServerEvent serverEvent){
		LinkedMessageMap<String, Object> assistantMessage = MessageBuilder.buildAssistantMessage(serverEvent.getData());
        
        AgentResultSessionMessageContext agentResultSessionMessageContext = new AgentResultSessionMessageContext();
        agentResultSessionMessageContext.setTokenMetrics(serverEvent.getTokenMetrics());
        return addAgentResultSessionMessage(assistantMessage, agentResultSessionMessageContext);
    }

     
    private LastSessionMessage addAgentResultSessionMessage(LinkedMessageMap<String, Object> assistantMessage, 
															AgentResultSessionMessageContext agentResultSessionMessageContext){
//        Map<String, Object> assistantMessage = MessageBuilder.buildAssistantMessage(serverEvent.getData());
		if(assistantMessage.getMessageType() == null){
			assistantMessage.setMessageType(MESSAGE_TYPE_AGENT_RESULTMESSAGE);
		}
        LastSessionMessage lastSubAgentSessionMessage = null;
       
        if(sessionMemory == null){
            TokenMetrics tokenMetrics_ = agentResultSessionMessageContext.getTokenMetrics();
            long elapsed = 0l;

            if(tokenMetrics_ != null){
                if(tokenMetrics_.getStartTime() != null && tokenMetrics_.getEndTime() != null){
                    elapsed = tokenMetrics_.getEndTime() - tokenMetrics_.getStartTime();
                }
            }
            lastSubAgentSessionMessage = new LastSessionMessage();
            lastSubAgentSessionMessage.setLastSessionMessage(assistantMessage);
            lastSubAgentSessionMessage.setRequestId(this.getRequestId());
            if(agent != null){
				lastSubAgentSessionMessage.setGroupId(agent.getGroupId());
				lastSubAgentSessionMessage.setParentGroupId(agent.getParentGroupId());
			}
            lastSubAgentSessionMessage.setTokenMetrics(agentResultSessionMessageContext.getTokenMetrics());
            lastSubAgentSessionMessage.setSubAgentIdBy(agentResultSessionMessageContext.getSubAgentIdBy());
            lastSubAgentSessionMessage.setElapsed(elapsed);
            return lastSubAgentSessionMessage;
        }
		
//        message.setMessage(assistantMessage);
        appendSessionMessage(agentResultSessionMessageContext.getAgent(),assistantMessage );

        if(parentAgentSessionStore != null){
            if(agent != null ){
                if(!agent.isDisablePush2ParentLastSubMessage()) {
//                if(!aiAgent.isDisableGloableStore()) {
//                    lastSubAgentSessionMessage = parentAgentSessionStore.addAgentResultSessionMessage(assistantMessage, agentId, this.getParantAgentId());
                    parentAgentSessionStore.addAgentResultSessionMessage(assistantMessage, agentId, this.getParantAgentId(),
							agentResultSessionMessageContext.getAgent() );
                }
                else{

                }
            }
            else{
//                lastSubAgentSessionMessage = parentAgentSessionStore.addAgentResultSessionMessage(assistantMessage, agentId, this.getParantAgentId());
                parentAgentSessionStore.addAgentResultSessionMessage(assistantMessage, agentId, this.getParantAgentId(),
						agentResultSessionMessageContext.getAgent() );
            }


        }
//        else{
//            lastSubAgentSessionMessage = new LastSessionMessage();
//            lastSubAgentSessionMessage.setLastSessionMessage(assistantMessage);
//            lastSubAgentSessionMessage.setRequestId(this.getRequestId());
//        }

        if(mainAgentSessionStore != null){
            PersistentMessage persistentMessage = new PersistentMessage();
            persistentMessage.setMessage(assistantMessage);
            persistentMessage.setTokenMetrics(agentResultSessionMessageContext.getTokenMetrics());
			persistentMessage.setGroupId(agent.getGroupId());
			persistentMessage.setParentGroupId(agent.getParentGroupId());
            lastSubAgentSessionMessage = mainAgentSessionStore.persistentSessionMessage(persistentMessage, agentId, 
                    this.getParantAgentId(),this.getAgent().getAgentNodeType(),agentResultSessionMessageContext.getSubAgentIdBy(),null,null, MESSAGE_TYPE_AGENT_RESULTMESSAGE);
        }
        else{
            TokenMetrics tokenMetrics_ = agentResultSessionMessageContext.getTokenMetrics();
            long elapsed = 0l;

            if(tokenMetrics_ != null){
                if(tokenMetrics_.getStartTime() != null && tokenMetrics_.getEndTime() != null){
                    elapsed = tokenMetrics_.getEndTime() - tokenMetrics_.getStartTime();
                }
            }
            lastSubAgentSessionMessage = new LastSessionMessage();
            lastSubAgentSessionMessage.setLastSessionMessage(assistantMessage);
            lastSubAgentSessionMessage.setTokenMetrics(agentResultSessionMessageContext.getTokenMetrics());
            lastSubAgentSessionMessage.setElapsed(elapsed);
            lastSubAgentSessionMessage.setAgentNodeType(this.getAgent().getAgentNodeType());
			if(agent != null){
				lastSubAgentSessionMessage.setGroupId(agent.getGroupId());
				lastSubAgentSessionMessage.setParentGroupId(agent.getParentGroupId());
			}
            lastSubAgentSessionMessage.setRequestId(this.getRequestId());
        }


       
        return lastSubAgentSessionMessage;
    }
    @Override
    public LastSessionMessage addAgentResultSessionMessage(AgentResultSessionMessageContext agentResultSessionMessageContext,String message){
		LinkedMessageMap<String, Object> assistantMessage = MessageBuilder.buildAssistantMessage(message);
         
        return   addAgentResultSessionMessage(assistantMessage, agentResultSessionMessageContext);
    }
    /**
     * 添加子智能体结果消息
     * @param persistentMessage
     * @param agentId
     * @param parentAgentId
     */
    @Override
    public LastSessionMessage addAgentResultSessionMessage(LinkedMessageMap<String, Object> persistentMessage//Map<String, Object> message
                                                            ,String agentId,String parentAgentId, AIAgent aiAgent ){

        LastSessionMessage lastSessionMessage = null;
//        if(this.mainAgentSessionStore != null) {//需要通过主智能体持久化消息
////            loadSessionMemory(message,  agentId);
//            //msgId,createTime,sessionId,seqNo,message,role
//            lastSessionMessage  = mainAgentSessionStore.persistentSessionMessage(persistentMessage, agentId,parentAgentId,null,null,MESSAGE_TYPE_AGENTRESULTMESSAGE);
//            
//        }
//        else if(this.persistentSessionMemory){//主智能体直接持久化消息
////            loadSessionMemory(message,  agentId);
//            lastSessionMessage  = persistentSessionMessage(persistentMessage, agentId,parentAgentId,null,null,MESSAGE_TYPE_AGENTRESULTMESSAGE);
//            
//
//        }
        //msgId,createTime,sessionId,seqNo,message,role


        appendSessionMessage(aiAgent,persistentMessage );
        
        return lastSessionMessage;

    }

    @Override
    public void addSessionMessage( LinkedMessageMap<String, Object> message//Map<String, Object> systemMessage
                                    ,String prompt,String agentId,String parentAgentId,String agentNodeType, AIAgent aiAgent ){
		String messageType = message.getMessageType();
		if(messageType == null) {
			String role = (String) message.get("role");
			messageType = messageType(role);
		}
        if(this.mainAgentSessionStore != null) {//需要通过主智能体持久化消息
            //msgId,createTime,sessionId,seqNo,message,role
            PersistentMessage persistentMessage = new PersistentMessage();
            persistentMessage.setMessage(message);
			persistentMessage.setGroupId(aiAgent.getGroupId());
			persistentMessage.setParentGroupId(aiAgent.getParentGroupId());
            mainAgentSessionStore.persistentSessionMessage(persistentMessage, agentId,parentAgentId,agentNodeType,null,null,null, messageType);
        }
        else if(this.persistentSessionMemory){//主智能体直接持久化消息
            PersistentMessage persistentMessage = new PersistentMessage();
            persistentMessage.setMessage(message);
			persistentMessage.setGroupId(aiAgent.getGroupId());
			persistentMessage.setParentGroupId(aiAgent.getParentGroupId());
            persistentSessionMessage(persistentMessage, agentId,parentAgentId,agentNodeType,null,null,null, messageType);//0 代表子智能体辅助消息， 1 代表子智能体输出结果 2 代表用户输入消息 3 智能体系统消息 5 智能体跟踪消息

            
        }
//            SQLExecutor.insertWithDBName(dataSource, agentSessionStoreDBConfig.getInsertSessionMessageSQL(),
//                    SimpleStringUtil.getUUID32(),new Date(),this.getSessionId(),agentId, integerCount.increament(), JsonUtil.object2json(systemMessage),
//                    systemMessage.get("role"));

        appendSessionMessage(aiAgent, message );
    }
 
//    @Override
//    public Map<String, Object> addAssistantSessionMessage(String message){
//        if(sessionMemory == null){
//            return null;
//        }
//        Map<String, Object> assistantMessage = MessageBuilder.buildAssistantMessage(message);
//        PersistentMessage persistentMessage = new PersistentMessage();
//        persistentMessage.setMessage(assistantMessage);
//        addSessionMessage(assistantMessage);
//        return assistantMessage;
//    }
    @Override
    public LinkedMessageMap<String, Object> addAssistantSessionMessage(ServerEvent serverEvent){
        if(sessionMemory == null){
            return null;
        }
		LinkedMessageMap<String, Object> assistantMessage = MessageBuilder.buildAssistantMessage(serverEvent);
        PersistentMessage persistentMessage = new PersistentMessage();
        persistentMessage.setMessage(assistantMessage);
        persistentMessage.setTokenMetrics(serverEvent.getTokenMetrics());
		persistentMessage.setParentGroupId(serverEvent.getParentGroupId());
		persistentMessage.setGroupId(serverEvent.getGroupId());
        addSessionMessage(persistentMessage);
        return assistantMessage;
    }
    @Override
    public LinkedMessageMap<String, Object> addAssistantSessionMessage(BaseStreamDataBuilder baseStreamDataBuilder){
        if(sessionMemory == null){
            return null;
        }
		
		LinkedMessageMap<String, Object> assistantMessage = MessageBuilder.buildAssistantMessage(  baseStreamDataBuilder);
		AIAgent agent = baseStreamDataBuilder.getChatObject().getAgent();
        StreamData streamData = baseStreamDataBuilder.getToolCallsStreamData();
        PersistentMessage persistentMessage = new PersistentMessage();
        persistentMessage.setMessage(assistantMessage);
        persistentMessage.setTokenMetrics(streamData.getStreamTokenMetrics());
        persistentMessage.setTotalTokenMetrics(streamData.getTotalTokenMetrics());
		persistentMessage.setGroupId(agent.getGroupId());
		persistentMessage.setParentGroupId(agent.getParentGroupId());
		
        addSessionMessage(persistentMessage);
        return assistantMessage;
    }

//    /**
//     * 主agent初始化记忆消息，如果未加载记忆消息，则进行加载
//     * @param userMessage
//     * @return
//     */
//    protected abstract boolean loadSessionMemory(Map<String, Object> userMessage,String agentId);
//    /**
//     * 根据prompt和agentId加载记忆消息，如果未加载记忆消息，则进行加载
//     * @param prompt
//     * @param agentId
//     * @return
//     */
//    public abstract boolean loadSessionMemory(String prompt,String agentId);

    protected LastSessionMessage lastSubAgentSessionMessage;

    /**
     * 并行节点：所有并行分支执行完毕后的结果集合
     */
    protected List<LastSessionMessage> lastSubAgentSessionMessages;

    public List<LastSessionMessage> getLastSubAgentSessionMessages() {
        return lastSubAgentSessionMessages;
    }

    @Override
    public LastSessionMessage getLastSubAgentSessionMessage(){
//        this.loadSessionMemory(prompt,agentId);
        return lastSubAgentSessionMessage;
         
        /**
        if(this.loadSessionMemory(prompt,agentId))//如果是从历史数据中加载，则无需返回最近消息，否则需返回最新消息给子智能体
            return null;
        if(sessionMemory == null || sessionMemory.size() == 0){
            return null;
        }
        LastSessionMessage lastSessionMessage = new LastSessionMessage();
        lastSessionMessage.setLastSessionMessage(sessionMemory.get(sessionMemory.size() - 1));
        lastSessionMessage.setFreshMessage(true);
        return lastSessionMessage;
         */
    }
    /**
     * 会话窗口大小，默认20
     */
    protected int sessionSize = 50;
    @Override
    public T setSessionSize(int sessionSize) {
        this.sessionSize = sessionSize;
        return (T) this;
    }
    @Override
    public int getSessionSize() {
        return sessionSize;
    }

    
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAgentId() {
        return agentId;
    }

    public T setAgentId(String agentId) {
        this.agentId = agentId;

        return (T) this;
    }

    public AgentSessionStore getMainAgentSessionStore() {
        return mainAgentSessionStore;
    }

    public T setMainAgentSessionStore(AgentSessionStore mainAgentSessionStore) {
        this.mainAgentSessionStore = mainAgentSessionStore;
        return (T) this;
    }

    public void setParentAgentLastSessionMessage(LastSessionMessage lastSubAgentSessionMessage){
        if(this.parentAgentSessionStore != null){
            parentAgentSessionStore.setSubAgentLastSessionMessage(lastSubAgentSessionMessage);
        }
        else {
            this.lastSubAgentSessionMessage = lastSubAgentSessionMessage;
        }
    }
    
    public void setSubAgentLastSessionMessage(LastSessionMessage lastSubAgentSessionMessage){
        this.lastSubAgentSessionMessage = lastSubAgentSessionMessage;
        //todo 如果当前子智能体所属的父智能体是父智能体对应的上级智能体的的最后一个子智能体，那么需要级联设置
    }

    public void cleanLastSessionMessages(){
        if(this.lastSubAgentSessionMessages != null){
            lastSubAgentSessionMessages.clear();
        }
    }

    @Override
    public String genSubAgentName(String agentId) {
        return this.getAgent().getAgentName() + "-" + agentId;
    }

    public String getRequestId() {
        return requestId;
    }

    public T setRequestId(String requestId) {
        this.requestId = requestId;
        return (T) this;
    }
	
	@Override
	public AgentMemoryStore getAgentMemoryStore() {
		if(agentMemoryStore == null && parentAgentSessionStore != null && parentAgentSessionStore != this)
			return this.parentAgentSessionStore.getAgentMemoryStore();
		return agentMemoryStore;
	}
	
	public void setAgentMemoryStore(AgentMemoryStore agentMemoryStore) {
		this.agentMemoryStore = agentMemoryStore;
	}
	public abstract int getNextSeqNo();
}
