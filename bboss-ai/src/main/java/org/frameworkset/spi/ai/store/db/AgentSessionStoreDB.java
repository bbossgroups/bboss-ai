package org.frameworkset.spi.ai.store.db;
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

import com.frameworkset.common.poolman.SQLExecutor;
import com.frameworkset.util.JsonUtil;
import com.frameworkset.util.SimpleStringUtil;
import org.frameworkset.spi.ai.model.AIRuntimeException;
import org.frameworkset.spi.ai.store.AgentSession;
import org.frameworkset.spi.ai.store.AgentSessionStoreMemory;
import org.frameworkset.spi.ai.store.SessionMessage;
import org.frameworkset.spi.ai.store.StoreContext;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author biaoping.yin
 * @Date 2026/4/5
 */
public class AgentSessionStoreDB extends AgentSessionStoreMemory<AgentSessionStoreDB> {
    private static org.slf4j.Logger logger = LoggerFactory.getLogger(AgentSessionStoreDB.class);
    private AgentSessionStoreDBConfig agentSessionStoreDBConfig;
    private AgentSession agentSession;
    
    /**
     * 持久化对话记录的数据源名称
     */
    private String dataSource;
    public AgentSessionStoreDB(List<Map<String, Object>> sessionMemory) {
        super(sessionMemory);
        agentSessionStoreDBConfig = new AgentSessionStoreDBConfig();
    }

    public AgentSessionStoreDB(List<Map<String, Object>> sessionMemory, int sessionSize) {
        super(sessionMemory, sessionSize);
        agentSessionStoreDBConfig = new AgentSessionStoreDBConfig();
    }

    public AgentSessionStoreDB(int sessionSize) {
        super(sessionSize);
    }
    
    public AgentSessionStoreDB(StoreContext storeContext) {
        super(storeContext);          
        this.dataSource = storeContext.getDataSource();
        agentSessionStoreDBConfig = new AgentSessionStoreDBConfig();
        agentSessionStoreDBConfig.setSessionTableName(storeContext.getSessionTableName());
        agentSessionStoreDBConfig.setSessionMessageTableName(storeContext.getSessionMessageTableName());
        init();
        
    }
 
    public void init(){
        agentSessionStoreDBConfig.init();
        try {
            SQLExecutor.queryObjectWithDBName(int.class, this.dataSource, agentSessionStoreDBConfig.getExistSQL());
        }
        catch (Exception exception){
            try {
                logger.info("Creating session table {}...", agentSessionStoreDBConfig.getSessionTableName());
                SQLExecutor.updateWithDBName(dataSource,agentSessionStoreDBConfig.evalCreateSessionTableSQL(this.dataSource));
            } catch (SQLException e) {
                throw new AIRuntimeException("Failed to create session table", e);
            }
        }

        try {
            SQLExecutor.queryObjectWithDBName(int.class, this.dataSource, agentSessionStoreDBConfig.getExistMessageSQL());
        }
        catch (Exception exception){
            try {
                logger.info("Creating session message table {}...", agentSessionStoreDBConfig.getSessionMessageTableName());
                SQLExecutor.updateWithDBName(dataSource,agentSessionStoreDBConfig.evalCreateSessionMessageTableSQL(this.dataSource));
            } catch (SQLException e) {
                throw new AIRuntimeException("Failed to create session message table", e);
            }
        }
        
        
    }

    public AgentSessionStoreDB setDataSource(String dataSource) {
        this.dataSource = dataSource;
        return this;
    }
    private void loadSessionMemory(Map<String, Object> userMessage){
        if(agentSession == null) {

            String prompt = (String) userMessage.get("content");
            if (prompt == null) {
                prompt = (String) userMessage.get("reasoning_content");
            }
            loadSessionMemory(prompt,getAgentId());
        }
            
    }
    private void loadSessionMemory(String prompt,String agentId){
        if(agentSession == null) {
            try {
                if(this.sessionMemory == null) {
                    this.sessionMemory = new ArrayList<>();

                }
                agentSession = SQLExecutor.queryObjectWithDBName(AgentSession.class, dataSource, agentSessionStoreDBConfig.getSelectSessionBySessionIdSQL(), this.getSessionId());
                if (agentSession == null) {//不存在session，则创建一个session
                    agentSession = new AgentSession();
                    agentSession.setSessionId(this.getSessionId());
                    agentSession.setUserId(this.getUserId());
                    agentSession.setAgentId(agentId);
                    agentSession.setTitle(prompt);
                    agentSession.setCreateTime(new java.util.Date());
                    agentSession.setLastAccessTime(agentSession.getCreateTime());
                    //创建session
                    //sessionId, createTime, useId, agentId, title
                    SQLExecutor.insertWithDBName(dataSource, agentSessionStoreDBConfig.getInsertSessionSQL(),
                            getSessionId(),
                            new Date(),
                            getUserId(),
                            agentId,//session由哪个agentId对应的agent创建
                            prompt.length() > 30 ? prompt.substring(0, 30) : prompt);
                } else {
                    List<SessionMessage> sessionMessages = SQLExecutor.queryListWithDBName(SessionMessage.class, dataSource,
                            agentSessionStoreDBConfig.getSelectSessionMessageBySessionIdSQL(), this.getSessionId());
                    
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
                
                
                
            } catch (SQLException e) {
                throw new AIRuntimeException("load session error",e);
            }
        }
    }

    @Override
    public void addSessionMessage( Map<String, Object> systemMessage,String prompt,String agentId){
        try {

            loadSessionMemory(prompt,agentId);
            //msgId,createTime,sessionId,seqNo,message,role
            SQLExecutor.insertWithDBName(dataSource, agentSessionStoreDBConfig.getInsertSessionMessageSQL(),
                    SimpleStringUtil.getUUID32(),new Date(),this.getSessionId(),agentId, 0, JsonUtil.object2json(systemMessage),
                    systemMessage.get("role"));
        } catch (SQLException e) {
            throw new AIRuntimeException("add session message error",e);
        }
        super.addSessionMessage(systemMessage);
    }

    @Override
    public List<Map<String, Object>>  getAgentSessionMessage(Map<String,Object> lastMessage,String agentId,int agentSessionSize){
        try {
            List<SessionMessage> agentSessionMessages = SQLExecutor.queryListWithDBName(SessionMessage.class, dataSource,
                    agentSessionStoreDBConfig.getSelectSessionMessageBySessionId2ndAgentIdSQL(), this.getSessionId(),agentId);
            List<Map<String, Object>> _agentSessionMessages = new ArrayList<>();
            int dataSize = agentSessionMessages.size();
            if(agentSessionMessages != null && agentSessionMessages.size() > 0){
                Map<String,Object> _storeLastMessage = agentSessionMessages.get(agentSessionMessages.size() -1).getMessage();
                String lastContent = null;
                if(lastMessage != null) {
                    lastContent = (String) lastMessage.get("content");
                    if (lastContent == null) {
                        lastContent = (String) lastMessage.get("reasoning_content");
                    }
                }
                String storeLastContent = (String) _storeLastMessage.get("content");
                if(storeLastContent == null){
                    storeLastContent = (String) _storeLastMessage.get("reasoning_content");
                }
                boolean same = (lastContent == null && storeLastContent == null) || lastContent.equals(storeLastContent);
                
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
        catch (Exception exception){
            throw new AIRuntimeException("getAgentSessionMessage: agentId="+agentId + ",agentSessionSize="+agentSessionSize,exception);
        }
        
    }
    @Override
    public void addSessionMessage(Map<String, Object> message) {
        try {
            
            loadSessionMemory(message);
            //msgId,createTime,sessionId,seqNo,message,role
            SQLExecutor.insertWithDBName(dataSource, agentSessionStoreDBConfig.getInsertSessionMessageSQL(),
                    SimpleStringUtil.getUUID32(),new Date(),this.getSessionId(), this.getAgentId(),0, JsonUtil.object2json(message),
                    message.get("role"));
        } catch (SQLException e) {
            throw new AIRuntimeException("add session message error",e);
        }
        super.addSessionMessage(message);
    }

    @Override
    public void addSessionMessage(Map<String, Object> message,String agentId){
        try {

            loadSessionMemory(message);
            //msgId,createTime,sessionId,seqNo,message,role
            SQLExecutor.insertWithDBName(dataSource, agentSessionStoreDBConfig.getInsertSessionMessageSQL(),
                    SimpleStringUtil.getUUID32(),new Date(),this.getSessionId(), agentId,0, JsonUtil.object2json(message),
                    message.get("role"));
        } catch (SQLException e) {
            throw new AIRuntimeException("add session message error",e);
        }
        super.addSessionMessage(message);
    }
    
    @Override
    public Map<String,Object> getLastMessage(String prompt,String agentId){
        this.loadSessionMemory(prompt,agentId);
        return super.getLastMessage(prompt,  agentId);
    }
    

     
 
  
}
