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

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;
import com.frameworkset.util.SimpleStringUtil;
import org.frameworkset.spi.ai.AIAgent;
import org.frameworkset.spi.ai.model.*;
import org.frameworkset.spi.ai.util.MessageBuilder;
import org.frameworkset.util.concurrent.IntegerCount;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author biaoping.yin
 * @Date 2026/1/4
 */
public class AgentSessionStoreMemory<T extends AgentSessionStoreMemory> extends BaseAgentSessionStore<T>{
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AgentSessionStoreMemory.class);
    protected AgentSession agentSession;
    private static Map<String,AgentSession> agentSessions = new ConcurrentHashMap();
    protected IntegerCount integerCount = new IntegerCount();

    /**
     * 为统一流程中的智能体分配一个唯一的智能体ID
     */
    protected IntegerCount agentIdCount = new IntegerCount();

    public AgentSessionStoreMemory(List<Map<String, Object>> sessionMemory) {
        super(sessionMemory);
    }

    public AgentSessionStoreMemory(List<Map<String, Object>> sessionMemory, int sessionSize) {
        super(sessionMemory, sessionSize);
    }

     

    public AgentSessionStoreMemory(int sessionSize) {
        super(sessionSize);
    }

    public AgentSessionStoreMemory(AgentSessionStore parentAgentSessionStore,int sessionSize) {
        super(parentAgentSessionStore,sessionSize);
    }

    

    public AgentSessionStoreMemory() {
    }

    public AgentSessionStoreMemory(String sessionId, String userId, String agentId) {
        super(sessionId, userId, agentId );
    }

    public AgentSessionStoreMemory(StoreContext storeContext, AIAgent agent) {
        super(storeContext,  agent);
        
    }

  


    @Override
    public void removeSession(String sessionId){
        if(agentSessions != null) {
            agentSessions.remove(sessionId);
        }
    }


    
    
    public void init(){
        if(this.sessionId == null){
            this.sessionId = SimpleStringUtil.getUUID32();
        }
        else{             
                
            AgentSession agentSession = agentSessions.get(this.sessionId);
            if(agentSession != null) {
                int maxSeqNo = agentSession.getMaxSeqNo();
                this.integerCount.setStartValue(maxSeqNo);
            }
            

        }


    }

  
    protected Object lockLoadSessionMemory = new Object();
    public boolean loadSessionMemory(String prompt,String agentId){
        String domain = null;
        StoreContext storeContext = this.getStoreContext();
        if(storeContext != null){
            domain = storeContext.getDomain();
        }
        return loadSessionMemory(prompt, domain,   agentId);
    }

    /**
     * 根据prompt和agentId加载记忆消息，如果未加载记忆消息，则进行加载
     * 如果会话不存在 则创建会话
     *
     * @param prompt
     * @param domain
     * @param agentId
     * @return
     */
    @Override
    public boolean loadSessionMemory(String prompt, String domain, String agentId) {
        if(agentSession != null){
            return false;
        }
        boolean newSession = false;
        synchronized (lockLoadSessionMemory) {

            if (agentSession == null) {//未加载
                if (this.sessionMemory == null) {
                    this.sessionMemory = new ArrayList<>();

                }
                String sessionId = this.getSessionId();
                if(sessionId == null){
                    sessionId = SimpleStringUtil.getUUID32();
                    this.setSessionId(sessionId);
                }
                agentSession = agentSessions.get(sessionId);
                if (agentSession == null) {//不存在session，则创建一个session
                    agentSession = new AgentSession();
                    agentSession.setSessionId(sessionId);
                    agentSession.setUserId(this.getUserId());
                    agentSession.setAgentId(this.getAgentId() != null ? getAgentId() : agentId);
                    agentSession.setTitle(prompt.length() > 50 ? prompt.substring(0, 50) : prompt);
                    agentSession.setCreateTime(LocalDateTime.now());
                    agentSession.setLastAccessTime(agentSession.getCreateTime());
                    agentSession.setDomain(domain);
                    agentSessions.put(sessionId, agentSession);
                    newSession = true;

                } else {
                    agentSession.setLastAccessTime(LocalDateTime.now());
                    //获取主智能体记忆记录
                    List<SessionMessage> sessionMessages = null;
                    if (this.getAgentId() == null) {
                        sessionMessages = agentSession.getMainAgentMessage(null);
//                        sessionMessages = SQLExecutor.queryListWithDBName(SessionMessage.class, dataSource,
//                                agentSessionStoreDBConfig.getSelectSessionMessageBySessionIdSQL(), this.getSessionId());
                    } else {

                        sessionMessages = agentSession.getMainAgentMessage(this.getAgentId());
//                        sessionMessages = SQLExecutor.queryListWithDBName(SessionMessage.class, dataSource,
//                                agentSessionStoreDBConfig.getSelectSessionMessageBySessionId2ndAgentIdSQL(), this.getSessionId(),this.getAgentId());
                    }


                    if (sessionMessages != null && !sessionMessages.isEmpty()) {
                        int sessionSize = this.getSessionSize();
                        int dataSize = sessionMessages.size();

                        if (sessionSize > 0 && dataSize > sessionSize) {

                            sessionMessages = sessionMessages.subList(dataSize - sessionSize, dataSize);

                        }
                        for (SessionMessage sessionMessage : sessionMessages) {
                            PersistentMessage persistentMessage = new PersistentMessage();
                            persistentMessage.setMessage(sessionMessage.getMessage());
                            persistentMessage.setTokenMetrics(sessionMessage.getTokenMetrics());
                            appendSessionMessageFromParent(sessionMessage.getMessage());
                        }

                    }
                }
            }
        }
        return newSession;
    }

    @Override
    public LastSessionMessage persistentSessionMessage(PersistentMessage persistentMessage,// Map<String, Object> message,
                                                       String agentId, String parentAgentId,String agentNodeType,String subAgentIdBy, String marks, String metadata,
                                                       String messageType){

//        loadSessionMemory(message,  agentId);
        Map<String, Object> message = persistentMessage.getMessage();
        SessionMessage sessionMessage = new SessionMessage();
        sessionMessage.setMsgId(SimpleStringUtil.getUUID32());
        sessionMessage.setMessage(message);
        String role = (String) message.get("role");
        
//        if(messageType != null && !messageType.equals("1")){
//            if(role.equals(MessageBuilder.ROLE_USER)){
//                messageType = SessionMessage.MESSAGE_TYPE_USER_MESSAGE;
//            }
//        }
        sessionMessage.setRole(role);
        sessionMessage.setSeqNo(integerCount.increament());
        sessionMessage.setCreateTime(LocalDateTime.now());
        sessionMessage.setSessionId(this.getSessionId());
        sessionMessage.setRequestId(this.getRequestId());
        sessionMessage.setAgentId(agentId);
        sessionMessage.setParentAgentId(parentAgentId);
        sessionMessage.setMessageType(messageType);
        sessionMessage.setMarks(marks);
        sessionMessage.setMetadata(metadata);
        sessionMessage.setTraceId(this.getTraceId());
        sessionMessage.setAgentNodeType(agentNodeType);
        sessionMessage.setSubAgentIdBy(subAgentIdBy);
        TokenMetrics tokenMetrics_ = persistentMessage.getTokenMetrics();
        long elapsed = 0l;

        if(tokenMetrics_ != null){
            if(tokenMetrics_.getStartTime() != null && tokenMetrics_.getEndTime() != null){
                elapsed = tokenMetrics_.getEndTime() - tokenMetrics_.getStartTime();
            }
        }
        sessionMessage.setElapsed(elapsed);
        sessionMessage.setTokenMetrics(tokenMetrics_);
        sessionMessage.setMsgId(SimpleStringUtil.getUUID32());
		if(agentSession != null) {
			agentSession.addSessionMessage(sessionMessage);
		}
       
        
        if(messageType != null && messageType.equals("1")) {
            LastSessionMessage lastSessionMessage = new LastSessionMessage();
            lastSessionMessage.setMsgId(sessionMessage.getMsgId());
            lastSessionMessage.setLastSessionMessage(message);
            lastSessionMessage.setFreshMessage(true);
            lastSessionMessage.setRequestId(this.getRequestId());
            lastSessionMessage.setSessionId(getSessionId());
            lastSessionMessage.setMsgAgentId(agentId);
            lastSessionMessage.setTokenMetrics(tokenMetrics_);
            lastSessionMessage.setElapsed(elapsed);
            lastSessionMessage.setMsgParentAgentId(parentAgentId);
            lastSessionMessage.setAgentNodeType(agentNodeType);
            lastSessionMessage.setSubAgentIdBy(subAgentIdBy);
            return lastSessionMessage;
        }
        else{
            return null;
        }
            //msgId,createTime,sessionId,seqNo,message,role
//            SQLExecutor.insertWithDBName(dataSource, agentSessionStoreDBConfig.getInsertSessionMessageSQL(),
//                    SimpleStringUtil.getUUID32(),new Date(),this.getSessionId(), agentId,integerCount.increament(), JsonUtil.object2json(message),
//                    message.get("role"));
         
    }
    @Override
    public List<Map<String, Object>> getAgentSessionMessage(LastSessionMessage lastSubAgentSessionMessage, String agentId, int agentSessionSize) {
        try {
            if (this.agentSession == null) {
                return null;
            }
            synchronized (agentSession){
                List<SessionMessage> agentSessionMessages = this.agentSession.getAgentSessionMessage(agentId);
                return resolve(lastSubAgentSessionMessage, agentId,agentSessionMessages, agentSessionSize);
            }
          
        }
        catch (Exception exception){
            throw new AIRuntimeException("getAgentSessionMessage: agentId="+agentId + ",agentSessionSize="+agentSessionSize,exception);
        }
    }



    @Override
    public void saveLastSessionMessage(LastSessionMessage lastSessionMessage,String refAgentId){
        this.agentSession.saveLastSessionMessage(lastSessionMessage,refAgentId);
        //msgId,msgAgentId,refAgentId,sessionId
//        try {
//            SQLExecutor.insertWithDBName(dataSource, agentSessionStoreDBConfig.getInsertSessionMessageRerenceSQL(),
//                    lastSessionMessage.getMsgId(),lastSessionMessage.getMsgAgentId(),
//                    refAgentId, getSessionId());
//        } catch (SQLException e) {
//            throw new AIRuntimeException("saveLastSessionMessage error",e);
//        }
    }
    
    protected List<Map<String, Object>> resolve(LastSessionMessage lastSubAgentSessionMessage, String agentId, List<SessionMessage> agentSessionMessages, int agentSessionSize){
        if(agentSessionMessages == null || agentSessionMessages.size() == 0){
            if(lastSubAgentSessionMessage != null){
                List<Map<String, Object>> _agentSessionMessages = new ArrayList<>();
                _agentSessionMessages.add(lastSubAgentSessionMessage.getLastSessionMessage());
                //构建引用关系
                saveLastSessionMessage(lastSubAgentSessionMessage, agentId);                
                return _agentSessionMessages;
            }
            return null;
        }
        List<Map<String, Object>> _agentSessionMessages = new ArrayList<>();
        int dataSize = agentSessionMessages.size();
 


        if(agentSessionSize > 0 && dataSize > agentSessionSize){
            int start = dataSize - agentSessionSize ;
 
            agentSessionMessages = agentSessionMessages.subList(start, dataSize);

        }

        boolean contain = false;

        for (int i = 0; i < agentSessionMessages.size(); i++) {
            SessionMessage sessionMessage = agentSessionMessages.get(i);

            if(lastSubAgentSessionMessage != null) {
                if(lastSubAgentSessionMessage.getMsgId().equals(sessionMessage.getMsgId()))
                    contain = true;
            }
            _agentSessionMessages.add(sessionMessage.getMessage());
        }
        if(!contain && lastSubAgentSessionMessage != null){
            _agentSessionMessages.add(lastSubAgentSessionMessage.getLastSessionMessage());
            //构建引用关系
            saveLastSessionMessage(lastSubAgentSessionMessage, agentId);            
            
        }

         
        return _agentSessionMessages;
    } 

//    @Override
//    public Map<String,Object> getLastMessage(String prompt,String agentId){
//        this.loadSessionMemory(prompt,agentId);
//        return super.getLastMessage(prompt,  agentId);
//    }

 
}
