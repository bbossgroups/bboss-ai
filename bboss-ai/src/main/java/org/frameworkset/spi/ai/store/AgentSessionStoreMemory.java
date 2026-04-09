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
import org.frameworkset.spi.ai.model.AIRuntimeException;
import org.frameworkset.util.concurrent.IntegerCount;

import java.util.ArrayList;
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
     * 在内存中持久化用户消息
     */
    protected boolean persistentSessionMemory;
    public AgentSessionStoreMemory(List<Map<String, Object>> sessionMemory) {
        super(sessionMemory);
    }

    public AgentSessionStoreMemory(List<Map<String, Object>> sessionMemory, int sessionSize) {
        super(sessionMemory, sessionSize);
    }

    public AgentSessionStoreMemory(boolean persistentSessionMemory,List<Map<String, Object>> sessionMemory, int sessionSize) {
        super(sessionMemory, sessionSize);
        this.persistentSessionMemory = persistentSessionMemory;
        if(persistentSessionMemory)
            init();
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

    public AgentSessionStoreMemory(StoreContext storeContext) {
        super(storeContext);
    }

  

    @Override
    public void addSessionMessage( Map<String, Object> systemMessage,String prompt,String agentId,String parentAgentId,boolean agentResultMessage){
        if(this.persistentSessionMemory) {

            loadSessionMemory(prompt, agentId);
            //msgId,createTime,sessionId,seqNo,message,role
            persistentSessionMessage(systemMessage, agentId,parentAgentId,agentResultMessage?"1":"0");
        }
//            SQLExecutor.insertWithDBName(dataSource, agentSessionStoreDBConfig.getInsertSessionMessageSQL(),
//                    SimpleStringUtil.getUUID32(),new Date(),this.getSessionId(),agentId, integerCount.increament(), JsonUtil.object2json(systemMessage),
//                    systemMessage.get("role"));

        super.addSessionMessage(systemMessage);
    }

    public static void removeSession(String sessionId){
        agentSessions.remove(sessionId);
    }


    @Override
    public void addSessionMessage(Map<String, Object> message,String agentId,boolean appendSelfAndParent,String parentAgentId,boolean agentResultMessage){
        if(this.persistentSessionMemory) {
            loadSessionMemory(message);
            persistentSessionMessage(message, agentId,parentAgentId,agentResultMessage?"1":"0");
        }
        //msgId,createTime,sessionId,seqNo,message,role


        super.addSessionMessage(message);
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
    protected boolean loadSessionMemory(Map<String, Object> userMessage){
        if(agentSession == null) {

            String prompt = (String) userMessage.get("content");
            if (prompt == null) {
                prompt = (String) userMessage.get("reasoning_content");
            }
            return loadSessionMemory(prompt,getAgentId());
        }
        return false;

    }
    protected boolean loadSessionMemory(String prompt,String agentId){
        if(agentSession == null) {//未加载
            if(this.sessionMemory == null) {
                this.sessionMemory = new ArrayList<>();

            }
            agentSession = agentSessions.get(this.getSessionId());
            if (agentSession == null) {//不存在session，则创建一个session
                agentSession = new AgentSession();
                agentSession.setSessionId(this.getSessionId());
                agentSession.setUserId(this.getUserId());
                agentSession.setAgentId(this.getAgentId() !=null?getAgentId():agentId);
                agentSession.setTitle(prompt.length() > 30 ? prompt.substring(0, 30) : prompt);
                agentSession.setCreateTime(new java.util.Date());
                agentSession.setLastAccessTime(agentSession.getCreateTime());
                agentSessions.put(this.getSessionId(), agentSession);
               
            } else {
                //获取主智能体记忆记录
                List<SessionMessage> sessionMessages = null;
                if(this.getAgentId() == null) {
                    sessionMessages = agentSession.getMainAgentMessage(null);
//                        sessionMessages = SQLExecutor.queryListWithDBName(SessionMessage.class, dataSource,
//                                agentSessionStoreDBConfig.getSelectSessionMessageBySessionIdSQL(), this.getSessionId());
                }
                else{

                    sessionMessages = agentSession.getMainAgentMessage(this.getAgentId());
//                        sessionMessages = SQLExecutor.queryListWithDBName(SessionMessage.class, dataSource,
//                                agentSessionStoreDBConfig.getSelectSessionMessageBySessionId2ndAgentIdSQL(), this.getSessionId(),this.getAgentId());
                }



                if (sessionMessages != null && !sessionMessages.isEmpty()) {
                    int sessionSize = this.getSessionSize();
                    int dataSize = sessionMessages.size();

                    if(sessionSize > 0 && dataSize > sessionSize){

                        sessionMessages = sessionMessages.subList(dataSize - sessionSize, dataSize);

                    }
                    for (SessionMessage sessionMessage : sessionMessages) {
                        appendSessionMessageFromParent(sessionMessage.getMessage());
                    }

                }
            }
            return true;
        }
        else {//已经加载过
            return false;
        }
    }

    @Override
    public void persistentSessionMessage(Map<String, Object> message, String agentId,String parentAgentId,String agentResultMessage){

        loadSessionMemory(message);
        SessionMessage sessionMessage = new SessionMessage();
        sessionMessage.setMessage(message);
        sessionMessage.setRole((String)message.get("role"));
        sessionMessage.setSeqNo(integerCount.increament());
        sessionMessage.setCreateTime(new java.util.Date());
        sessionMessage.setSessionId(this.getSessionId());
        sessionMessage.setAgentId(agentId);
        sessionMessage.setMsgId(SimpleStringUtil.getUUID32());
        agentSession.addSessionMessage(sessionMessage);
            //msgId,createTime,sessionId,seqNo,message,role
//            SQLExecutor.insertWithDBName(dataSource, agentSessionStoreDBConfig.getInsertSessionMessageSQL(),
//                    SimpleStringUtil.getUUID32(),new Date(),this.getSessionId(), agentId,integerCount.increament(), JsonUtil.object2json(message),
//                    message.get("role"));
         
    }
    @Override
    public List<Map<String, Object>> getAgentSessionMessage(Map<String, Object> lastMessage, String agentId, int agentSessionSize) {
        try {
            if (this.agentSession == null) {
                return null;
            }
            synchronized (agentSession){
                List<SessionMessage> agentSessionMessages = this.agentSession.getAssistantMessages(agentId);
                return resolve(lastMessage, agentSessionMessages, agentSessionSize);
            }
          
        }
        catch (Exception exception){
            throw new AIRuntimeException("getAgentSessionMessage: agentId="+agentId + ",agentSessionSize="+agentSessionSize,exception);
        }
    }
    
    protected List<Map<String, Object>> resolve(Map<String,Object> lastMessage, List<SessionMessage> agentSessionMessages, int agentSessionSize){
        if(agentSessionMessages == null || agentSessionMessages.size() == 0){
            if(lastMessage != null){
                List<Map<String, Object>> _agentSessionMessages = new ArrayList<>();
                _agentSessionMessages.add(lastMessage);
                return _agentSessionMessages;
            }
            return null;
        }
        List<Map<String, Object>> _agentSessionMessages = new ArrayList<>();
        int dataSize = agentSessionMessages.size();
        if(agentSessionMessages != null && agentSessionMessages.size() > 0){
            String lastContent = null;
            String storeLastContent = null;
            boolean same = true;
            if(lastMessage != null) {
                Map<String, Object> _storeLastMessage = agentSessionMessages.get(agentSessionMessages.size() - 1).getMessage();

                if (lastMessage != null) {
                    lastContent = (String) lastMessage.get("content");
                    if (lastContent == null) {
                        lastContent = (String) lastMessage.get("reasoning_content");
                    }
                }
                storeLastContent = (String) _storeLastMessage.get("content");
                if (storeLastContent == null) {
                    storeLastContent = (String) _storeLastMessage.get("reasoning_content");
                }
                same = (lastContent == null && storeLastContent == null) || lastContent.equals(storeLastContent);
            }


            if(agentSessionSize > 0 && dataSize > agentSessionSize){
                int start = 0;
                if(same){
                    start = dataSize - agentSessionSize ;
                }
                else{
                    start = dataSize - agentSessionSize + 1;
                }
                agentSessionMessages = agentSessionMessages.subList(start, dataSize);

            }



            for (int i = 0; i < agentSessionMessages.size(); i++) {
                SessionMessage sessionMessage = agentSessionMessages.get(i);

                _agentSessionMessages.add(sessionMessage.getMessage());
            }
            if(!same){
                _agentSessionMessages.add(lastMessage);
            }

        }
        else{
            if(lastMessage != null){
                _agentSessionMessages.add(lastMessage);
            }
        }
        return _agentSessionMessages;
    } 

//    @Override
//    public Map<String,Object> getLastMessage(String prompt,String agentId){
//        this.loadSessionMemory(prompt,agentId);
//        return super.getLastMessage(prompt,  agentId);
//    }

 
}
