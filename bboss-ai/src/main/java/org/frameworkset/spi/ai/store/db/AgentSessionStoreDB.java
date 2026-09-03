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
import org.frameworkset.spi.ai.AIAgent;
import org.frameworkset.spi.ai.model.*;
import org.frameworkset.spi.ai.store.AgentSession;
import org.frameworkset.spi.ai.store.AgentSessionStoreMemory;
import org.frameworkset.spi.ai.store.SessionMessage;
import org.frameworkset.spi.ai.store.StoreContext;
import org.frameworkset.spi.ai.util.MessageBuilder;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.LocalDateTime;
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
    

    private String clickhouseCluster;
    /**
     * 持久化对话记录的数据源名称
     */
    private String dataSource;
	/**
	 * 人工介入任务数据库表数据源
	 */
	private String hitlDatasource = "bboss";
    public AgentSessionStoreDB(List<LinkedMessageMap<String, Object>> sessionMemory) {
        super(sessionMemory);
        agentSessionStoreDBConfig = new AgentSessionStoreDBConfig();
        persistentSessionMemory = true;
        init();
    }

    public AgentSessionStoreDB(List<LinkedMessageMap<String, Object>> sessionMemory, int sessionSize) {
        super(sessionMemory, sessionSize);
        agentSessionStoreDBConfig = new AgentSessionStoreDBConfig();
        persistentSessionMemory = true;
        init();
    }

    public AgentSessionStoreDB(int sessionSize) {
        super(sessionSize);
        agentSessionStoreDBConfig = new AgentSessionStoreDBConfig();
        persistentSessionMemory = true;
        init();
    }
    
    public AgentSessionStoreDB(StoreContext storeContext, AIAgent agent) {
        super(storeContext,   agent);
        this.dataSource = storeContext.getDataSource();
		this.hitlDatasource = storeContext.getHitlDatasource();
		if(hitlDatasource == null){
			hitlDatasource = dataSource;
		}
		this.clickhouseCluster = storeContext.getClickhouseCluster();
        agentSessionStoreDBConfig = new AgentSessionStoreDBConfig();
        agentSessionStoreDBConfig.setSessionTableName(storeContext.getSessionTableName());
        agentSessionStoreDBConfig.setSessionMessageTableName(storeContext.getSessionMessageTableName());
        
    }
 
    @Override
    public void init(){
		if(hitlDatasource == null){
			hitlDatasource = dataSource;
		}
        agentSessionStoreDBConfig.init( clickhouseCluster,   hitlDatasource,  dataSource);        
        
        if(this.sessionId != null){

            try {
                logger.info("Get session maxSeqNo of sessionId {} from table {}...", sessionId, agentSessionStoreDBConfig.getSessionMessageTableName());
                int maxSeqNo = SQLExecutor.queryObjectWithDBName(int.class, this.dataSource, agentSessionStoreDBConfig.getSelectMaxSeqNoBySessionIdSQL(), sessionId);
                this.integerCount.setStartValue(maxSeqNo);
                logger.info("Get session maxSeqNo of sessionId {} from table {} maxSeqNo:{}", sessionId, agentSessionStoreDBConfig.getSessionMessageTableName(), maxSeqNo);
            } catch (SQLException e) {
                throw new AIRuntimeException("Failed to Get session maxSeqNo of sessionId "+sessionId+" from table "+agentSessionStoreDBConfig.getSessionMessageTableName(), e);
            }
            
        }
        else{
            
            sessionId = SimpleStringUtil.getUUID32();
            logger.info("SessionId is null and create new sessionId:{}",sessionId);
        }
        
        
    }

    public AgentSessionStoreDB setDataSource(String dataSource) {
        this.dataSource = dataSource;
		this.clickhouseCluster = storeContext.getClickhouseCluster();
        return this;
    }
	
	public AgentSessionStoreDB setHitlDatasource(String hitlDatasource) {
		this.hitlDatasource = hitlDatasource;
		return this;
	}
	
	@Override
    public boolean loadSessionMemory(String prompt,String agentId){
        String domain = this.storeContext != null ?this.storeContext.getDomain():null;
        return loadSessionMemory(  prompt,domain,  agentId);
    }
    @Override
    public boolean loadSessionMemory(String prompt,String domain,String agentId){
        if(agentSession != null){
            return false;
        }
        boolean newSession = false;
        synchronized (lockLoadSessionMemory) {
            if (agentSession == null) {//未加载
                try {
                    if (this.sessionMemory == null) {
                        this.sessionMemory = new ArrayList<>();

                    }
                    agentSession = SQLExecutor.queryObjectWithDBName(AgentSession.class, dataSource, agentSessionStoreDBConfig.getSelectSessionBySessionIdSQL(), this.getSessionId());
                    if (agentSession == null) {//不存在session，则创建一个session
                        agentSession = new AgentSession();
                        agentSession.setSessionId(this.getSessionId());
                        agentSession.setUserId(this.getUserId());
                        agentSession.setAgentId(this.getAgentId() != null ? this.getAgentId() : agentId);
                        agentSession.setTitle(prompt);
                        agentSession.setCreateTime(LocalDateTime.now());
                        agentSession.setLastAccessTime(agentSession.getCreateTime());
                        agentSession.setDomain(domain);
                        //创建session
                        //sessionId, createTime, userId, agentId, title
                        SQLExecutor.insertWithDBName(dataSource, agentSessionStoreDBConfig.getInsertSessionSQL(),
                                getSessionId(),
                                new Date(),
                                new Date(),
                                getUserId(),
                                this.getAgentId() != null ? getAgentId() : agentId,//如果主agentId存在，则使用主agentId，否则session由agentId对应的agent创建
                                prompt.length() > 50 ? prompt.substring(0, 50) : prompt,domain);
                        newSession = true;
                    } else {
						//只有非clickhouse数据库才需要更新session最后访问时间，clickhouse数据库不支持更新最后访问时间
						if(!agentSessionStoreDBConfig.isClickhouse(this.dataSource)) {
							SQLExecutor.updateWithDBName(
									dataSource,
									agentSessionStoreDBConfig.getUpdateSessionLastAccessTimeSQL(),
									new Date(),
									getSessionId()
							);
						}
                        //获取主智能体记忆记录
                        List<SessionMessage> sessionMessages = null;
                        if (this.getAgentId() == null) {
                            sessionMessages = SQLExecutor.queryListWithDBName(SessionMessage.class, dataSource,
                                    agentSessionStoreDBConfig.getSelectSessionMessageBySessionIdSQL(), this.getSessionId());
                        } else {
							List<String> agentRefMsgIds = getAgentRefMsgIds(this.getSessionId(),agentId);
                            sessionMessages = SQLExecutor.queryListWithDBName(SessionMessage.class, dataSource,
                                    agentSessionStoreDBConfig.getSelectSessionMessageBySessionId2ndAgentIdSQL(agentRefMsgIds), this.getSessionId(), this.getAgentId(), this.getAgentId());
                        }


                        if (sessionMessages != null && !sessionMessages.isEmpty()) {
                            int sessionSize = this.getSessionSize();
                            int dataSize = sessionMessages.size();

                            if (sessionSize > 0 && dataSize > sessionSize) {

                                sessionMessages = sessionMessages.subList(dataSize - sessionSize, dataSize);

                            }
                            for (SessionMessage sessionMessage : sessionMessages) {
                                
                                appendSessionMessageFromParent(sessionMessage.getMessage());
                            }

                        }
                    }


                } catch (SQLException e) {
                    throw new AIRuntimeException("load session error", e);
                }                
            }  
        }
        return newSession;
    }

    @Override
    public LastSessionMessage persistentSessionMessage(PersistentMessage persistentMessage,
                                                       String agentId, String parentAgentId,
													   String agentNodeType,String subAgentIdBy, String marks, String metadata, String messageType){
        try { 
//            loadSessionMemory(message, agentId);
            //msgId,createTime,sessionId,seqNo,message,role
			LinkedMessageMap<String, Object> message = persistentMessage.getMessage();
            String msgId = SimpleStringUtil.getUUID32();
            String role = (String) message.get("role");
//            if(agentResultMessage != null && !agentResultMessage.equals("1")){
//                if(role.equals(MessageBuilder.ROLE_USER)){
//                    agentResultMessage = SessionMessage.MESSAGE_TYPE_USER_MESSAGE;
//                }
//            }
            TokenMetrics tokenMetrics_ = persistentMessage.getTokenMetrics();
            String tokenMetrics = null;
            long elapsed = 0l;
            if(tokenMetrics_ != null){
                tokenMetrics = JsonUtil.object2json(tokenMetrics_);
                if(tokenMetrics_.getStartTime() != null && tokenMetrics_.getEndTime() != null){
                    elapsed = tokenMetrics_.getEndTime() - tokenMetrics_.getStartTime();
                }
            }
           
            
            SQLExecutor.insertWithDBName(dataSource, agentSessionStoreDBConfig.getInsertSessionMessageSQL(),
                    msgId,new Date(),this.getSessionId(),
                    parentAgentId, agentId,messageType,integerCount.increament(), JsonUtil.object2json(message),
                    role,marks,metadata,this.getRequestId(), tokenMetrics,elapsed,this.getTraceId(),agentNodeType,
					subAgentIdBy,persistentMessage.getGroupId(),persistentMessage.getParentGroupId()
//					,message.getName()
			);

            if(messageType != null && messageType.equals("1")) {
                LastSessionMessage lastSessionMessage = new LastSessionMessage();
                lastSessionMessage.setMsgId(msgId);
                lastSessionMessage.setLastSessionMessage(message);
                lastSessionMessage.setFreshMessage(true);
                lastSessionMessage.setSessionId(getSessionId());
                lastSessionMessage.setRequestId(this.getRequestId());
                lastSessionMessage.setMsgAgentId(agentId);
                lastSessionMessage.setTokenMetrics(persistentMessage.getTokenMetrics());
                lastSessionMessage.setMsgParentAgentId(parentAgentId);
                lastSessionMessage.setElapsed(elapsed);
                lastSessionMessage.setAgentNodeType(agentNodeType);
                lastSessionMessage.setSubAgentIdBy(subAgentIdBy);
				lastSessionMessage.setGroupId(persistentMessage.getGroupId());
				lastSessionMessage.setParentGroupId(persistentMessage.getParentGroupId());
                return lastSessionMessage;
            }
            else{
                return null;
            }
        } catch (SQLException e) {
            throw new AIRuntimeException("add session message error",e);
        }
    }
	
	/**
	 * 查询会话中智能体引用的消息记录清空
	 * @param sessionId
	 * @param agentId
	 * @return
	 */
	public List<String> getAgentRefMsgIds(String sessionId,String agentId){
		try {
			List<String> agentRefMsgIds = SQLExecutor.queryListWithDBName(String.class, dataSource,
					agentSessionStoreDBConfig.getSelectAgentSessionMessageReferenceIdsBySessionIdSQL(), sessionId,agentId);
			return agentRefMsgIds;
		}
		catch (Exception exception){
			throw new AIRuntimeException("getAgentRefMsgIds: agentId="+agentId + ",sessionId="+sessionId,exception);
		}
	}
    @Override
    public List<LinkedMessageMap<String, Object>>  getAgentSessionMessage(LastSessionMessage lastSubAgentSessionMessage,String agentId,int agentSessionSize){
        try {
			List<String> agentRefMsgIds = getAgentRefMsgIds(this.getSessionId(),agentId);
            List<SessionMessage> agentSessionMessages = SQLExecutor.queryListWithDBName(SessionMessage.class, dataSource,
                    agentSessionStoreDBConfig.getSelectSessionMessageBySessionId2ndAgentIdSQL(agentRefMsgIds), this.getSessionId(),agentId,agentId);
            return resolve(lastSubAgentSessionMessage,agentId,   agentSessionMessages,   agentSessionSize);
           
        }
		catch (AIRuntimeException exception){
			throw exception;
		}
        catch (Exception exception){
            throw new AIRuntimeException("getAgentSessionMessage: agentId="+agentId + ",agentSessionSize="+agentSessionSize,exception);
        }
        
    }

    @Override
    public void saveLastSessionMessage(LastSessionMessage lastSessionMessage,String refAgentId){
        //msgId,msgAgentId,refAgentId,sessionId
        try {
            SQLExecutor.insertWithDBName(dataSource, agentSessionStoreDBConfig.getInsertSessionMessageRerenceSQL(),
                    lastSessionMessage.getMsgId(),lastSessionMessage.getMsgAgentId(),
                    refAgentId, getSessionId(),lastSessionMessage.getRequestId());
        } catch (SQLException e) {
            throw new AIRuntimeException("saveLastSessionMessage error",e);
        }
    }

    public void removeSession(String sessionId){
        try {
            SQLExecutor.deleteWithDBName(dataSource, agentSessionStoreDBConfig.getDeleteSessionBySessionIdSQL(), sessionId);
            SQLExecutor.deleteWithDBName(dataSource, agentSessionStoreDBConfig.getDeleteSessionMessageBySessionIdSQL(), sessionId);
            SQLExecutor.deleteWithDBName(dataSource, agentSessionStoreDBConfig.getDeleteSessionMessageRerenceBySessionIdSQL(), sessionId);
        } catch (SQLException e) {
            throw new AIRuntimeException("removeSession error",e);
        }
    }
 
    

     
 
  
}
