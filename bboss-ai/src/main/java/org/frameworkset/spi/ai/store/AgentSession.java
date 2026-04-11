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
import org.frameworkset.spi.ai.model.LastSessionMessage;

import java.util.*;

/**
 * @author biaoping.yin
 * @Date 2026/4/3
 */
public class AgentSession {


    private Date createTime;
    private Date lastAccessTime;
    private String sessionId;
    private String userId;
    private String agentId;

    private String title;
    private List<SessionMessage> assistantMessages;

    
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
 
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * 记录智能体引用的历史消息清单
     */
    private Map<String,List<LastSessionMessage>> agentReferenceSessionMessages = new ConcurrentHashMap();
    
    public void saveLastSessionMessage(LastSessionMessage lastSessionMessage, String refAgentId) {
        
        
        synchronized (agentReferenceSessionMessages) {
            List<LastSessionMessage> lastSessionMessages = agentReferenceSessionMessages.get(refAgentId);
            if (lastSessionMessages == null) {

                lastSessionMessages = new ArrayList<>();
                agentReferenceSessionMessages.put(refAgentId, lastSessionMessages);
            }

            lastSessionMessages.add(lastSessionMessage);
        }
        
    }
    private boolean isReferMessage(SessionMessage assistantMessage,List<LastSessionMessage> lastSessionMessages){
        if(lastSessionMessages == null || lastSessionMessages.size() == 0)
            return false;
        for(LastSessionMessage lastSessionMessage : lastSessionMessages){
            if(lastSessionMessage.getMsgId().equals(assistantMessage.getMsgId()))
                return true;
        }
        return false;
    }
    //msgId,msgAgentId,refAgentId,sessionId
    /**
     * 根据agentId获取agentId的历史消息
     * @param agentId
     * @return
     */
    public List<SessionMessage> getAgentSessionMessage(String agentId) {
        if(assistantMessages == null || assistantMessages.size() == 0)
            return null;
        List<SessionMessage> agentMessages = null;
        List<LastSessionMessage> lastSessionMessages = this.agentReferenceSessionMessages.get(agentId);
        for(SessionMessage assistantMessage : assistantMessages) {
            String messageAgentId = assistantMessage.getAgentId();
            String messageParentId = assistantMessage.getParentAgentId();
            if(isReferMessage(assistantMessage,lastSessionMessages)){
                if (agentMessages == null)
                    agentMessages = new ArrayList<>();
                agentMessages.add(assistantMessage);
            }
            else if (messageAgentId != null && messageAgentId.equals(agentId) ) {
                if (agentMessages == null)
                    agentMessages = new ArrayList<>();
                agentMessages.add(assistantMessage);
            }
            else if(messageParentId != null && messageParentId.equals(agentId) && assistantMessage.getAgentResultMessage().equals("1")) {
                if (agentMessages == null)
                    agentMessages = new ArrayList<>();
                agentMessages.add(assistantMessage);
            }
            
        }
        
        if(lastSessionMessages != null && lastSessionMessages.size() > 0) {
            for (LastSessionMessage lastSessionMessage : lastSessionMessages) {
                if (agentMessages == null)
                    agentMessages = new ArrayList<>();
                SessionMessage sessionMessage = new SessionMessage();
                sessionMessage.setMessage(lastSessionMessage.getLastSessionMessage());
                agentMessages.add(sessionMessage);
            }
        }
        return agentMessages;
    }

    public void setAssistantMessages(List<SessionMessage> assistantMessages) {
        this.assistantMessages = assistantMessages;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getLastAccessTime() {
        return lastAccessTime;
    }

    public void setLastAccessTime(Date lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }


    public synchronized List<SessionMessage> getMainAgentMessage(String agentId) {
        if(assistantMessages == null || assistantMessages.size() == 0)
            return null;
        List<SessionMessage> mainAgentMessages = null;
        if(agentId == null) {
            for (SessionMessage assistantMessage : assistantMessages) {
                String messageAgentId = assistantMessage.getAgentId();
                String messageParentId = assistantMessage.getParentAgentId();
                if (messageAgentId == null) {
                    if (mainAgentMessages == null)
                        mainAgentMessages = new ArrayList<>();
                    mainAgentMessages.add(assistantMessage);
                }
                else if (messageParentId == null && assistantMessage.getAgentResultMessage().equals("1")) {
                    if (mainAgentMessages == null)
                        mainAgentMessages = new ArrayList<>();
                    mainAgentMessages.add(assistantMessage);
                }
            }
        }
        else{
            for (SessionMessage assistantMessage : assistantMessages) {
                String messageAgentId = assistantMessage.getAgentId();
                String messageParentId = assistantMessage.getParentAgentId();
                if (messageAgentId != null && messageAgentId.equals(agentId)) {
                    if (mainAgentMessages == null)
                        mainAgentMessages = new ArrayList<>();
                    mainAgentMessages.add(assistantMessage);
                }
                else if (messageParentId != null && messageParentId.equals(agentId) && assistantMessage.getAgentResultMessage().equals("1")) {//子agent的输出结果消息也是主agent的消息
                    if (mainAgentMessages == null)
                        mainAgentMessages = new ArrayList<>();
                    mainAgentMessages.add(assistantMessage);
                }
            }
        }
        return mainAgentMessages;
    }

    public synchronized void addSessionMessage(SessionMessage sessionMessage) {
        if(assistantMessages == null)
            assistantMessages = new ArrayList<>();
        assistantMessages.add(sessionMessage);
    }

    public synchronized int getMaxSeqNo() {
        if(assistantMessages == null || assistantMessages.size() == 0)
            return 0;
        int maxSeqNo = 0;
        for (SessionMessage assistantMessage : assistantMessages) {
            if(assistantMessage.getSeqNo() > maxSeqNo)
                maxSeqNo = assistantMessage.getSeqNo();
        }
        return maxSeqNo;
    }


}
