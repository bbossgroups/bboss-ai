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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

    public List<SessionMessage> getAssistantMessages(String agentId) {
        if(assistantMessages == null || assistantMessages.size() == 0)
            return null;
        List<SessionMessage> agentMessages = null;
        for(SessionMessage assistantMessage : assistantMessages) {
            String messageAgentId = assistantMessage.getAgentId();
            String messageParentId = assistantMessage.getParentAgentId();
            if (messageAgentId != null && messageAgentId.equals(agentId) ) {
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
                else if (messageParentId != null && messageParentId.equals(agentId) && assistantMessage.getAgentResultMessage().equals("1")) {
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
            assistantMessages = new ArrayList<SessionMessage>();
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
