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

import com.frameworkset.common.poolman.DBUtil;
import com.frameworkset.orm.adapter.DB;
import org.frameworkset.persitent.util.SQLUtil;

/**
 * 数据库会话存储配置：sql语句
 * @author biaoping.yin
 * @Date 2026/4/5
 */
public class AgentSessionStoreDBConfig {
    public static String sqlitex_createSessionTableSQL = new StringBuilder().append("create table $sessionTableName (sessionId varchar(100),")  //会话id
            .append( "createTime number(20),") //创建时间
            .append( "useId varchar(100),")  //用户id
            .append( "agentId varchar(100),")  //代理id
            .append( "title varchar(500),")  //会话标题         
            .append( "PRIMARY KEY (sessionId))").toString();

    public static String sqlitex_createSessionMessageTableSQL = new StringBuilder().append("create table $sessionMessageTableName (msgId varchar(100),")  //消息id
            .append( "createTime number(20),") //创建时间
            .append( "parentAgentId varchar(100),")  //父agentid
            .append( "agentId varchar(100),")  //创建消息的agentid
            .append( "agentResultMessage varchar(1),")  //是否是agent的最终结果消息，需要加载到父agent的记忆消息中 0：否 1：是
            .append( "sessionId varchar(100),")  //会话id
            .append( "seqNo int,")  //消息序号
            .append( "message text,")  //消息正文
            .append( "role varchar(100),")
            .append( "PRIMARY KEY (msgId))").toString();
    
    public static final String mysql_createSessionTableSQL = new StringBuilder().append("CREATE TABLE $sessionTableName ( sessionId varchar(100) NOT NULL comment '会话id'," )
            .append(" createTime datetime NOT NULL comment '创建时间', " )
            .append( "useId varchar(100) comment '用户id',")  //用户id
            .append( "agentId varchar(100) comment '代理id',")  //代理id
            .append( "title varchar(500) comment '会话标题',")  //会话标题     
            .append( "PRIMARY KEY(sessionId)) comment '增量状态同步表主键' ENGINE=InnoDB").toString();
    
    public static final String oracle_createSessionTableSQL = new StringBuilder().append("CREATE TABLE $sessionTableName ( sessionId varchar2(100) NOT NULL," )
            .append(" createTime timestamp NOT NULL,")
            .append(" useId varchar2(100) NOT NULL, " )
            .append( "agentId varchar2(100) NOT NULL, " )
            .append("title varchar2(500) NOT NULL,")              
            .append( "constraint $sessionTableName_PK primary key(sessionId))").toString();


    public static final String oracle_addCommentsToSessionTableSQL = new StringBuilder()
            .append("COMMENT ON COLUMN $sessionTableName.sessionId IS '会话id';")
            .append("COMMENT ON COLUMN $sessionTableName.createTime IS '创建时间';")
            .append("COMMENT ON COLUMN $sessionTableName.useId IS '用户id';")
            .append("COMMENT ON COLUMN $sessionTableName.agentId IS '代理id';")
            .append("COMMENT ON COLUMN $sessionTableName.title IS '会话标题';")
            .toString();
    
    public static final String dm_createSessionTableSQL = new StringBuilder().append("CREATE TABLE $sessionTableName ( sessionId varchar2(100) NOT NULL," )
            .append(" createTime timestamp NOT NULL,")
            .append(" useId varchar2(100) NOT NULL, " )
            .append( "agentId varchar2(100) NOT NULL, " )
            .append("title varchar2(500) NOT NULL,")
            .append( "constraint $sessionTableName_PK primary key(sessionId))").toString();
    public static final String sqlserver_createSessionTableSQL = new StringBuilder().append("CREATE TABLE $sessionTableName ( sessionId varchar(100) NOT NULL," )
            .append( "createTime datetime NOT NULL,")  //创建时间
            .append("useId varchar(100) NOT NULL,") //用户id
            .append( "agentId varchar(100) NOT NULL,")  //代理id
            .append( "title varchar(500) NOT NULL,")  //会话标题           
            .append( "constraint $sessionTableName_PK primary key(sessionId))") //增量状态同步表主键
            .toString();

    public static final String postgresql_createSessionTableSQL = new StringBuilder().append("CREATE TABLE $sessionTableName (sessionId varchar(100) NOT NULL," )
            .append( "createTime timestamp NOT NULL,")  //创建时间
            .append( "useId varchar(100),")  //用户id
            .append( " agentId varchar(100)  NOT NULL,") //代理id
            .append( "title varchar(500) ,")  //会话标题
            .append( "primary key(sessionId))")//增量状态同步表主键
            .toString();

    public static final String postgresql_addCommentsToSessionTableSQL = new StringBuilder()
            .append("COMMENT ON COLUMN $sessionTableName.sessionId IS '会话id';")
            .append("COMMENT ON COLUMN $sessionTableName.createTime IS '创建时间';")
            .append("COMMENT ON COLUMN $sessionTableName.useId IS '用户id';")
            .append("COMMENT ON COLUMN $sessionTableName.agentId IS '代理id';")
            .append("COMMENT ON COLUMN $sessionTableName.title IS '会话标题';")
            .toString();
    
    public static final String mysql_createSessionMessageTableSQL = new StringBuilder().append("CREATE TABLE $sessionMessageTableName ( msgId varchar(100) NOT NULL comment '消息id'," )
            .append(" createTime datetime NOT NULL comment '创建时间', " )
            .append( "sessionId varchar(100) NOT NULL, " )  //会话id
            .append( "parentAgentId varchar(100),")  //父agentid
            .append( "agentId varchar(100),")  //创建消息的agentid
            .append( "agentResultMessage varchar(1),")  //是否是agent的最终结果消息，需要加载到父agent的记忆消息中
            .append( "seqNo int NOT NULL, " )  //消息序号
            .append( "message LONGTEXT  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL, " )  //消息正文
            .append( "role varchar(100) NOT NULL, " )
            .append( "primary key(msgId)) comment '消息表主键' ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci").toString();
    
    public static final String oracle_createSessionMessageTableSQL = new StringBuilder().append("CREATE TABLE $sessionMessageTableName ( msgId varchar2(100) NOT NULL," )
            .append(" createTime timestamp NOT NULL,")
            .append(" sessionId varchar2(100) NOT NULL, " )
            .append( "parentAgentId varchar2(100),")  //父agentid
            .append( "agentId varchar2(100),")  //创建消息的agentid
            .append( "agentResultMessage varchar2(1),")  //是否是agent的最终结果消息，需要加载到父agent的记忆消息中
            .append( "seqNo int NOT NULL, " )  //消息序号
            .append( "message clob NOT NULL, " )  //消息正文
            .append( "role varchar2(100) NOT NULL, " )
            .append( "constraint $sessionMessageTableName_PK primary key(msgId))").toString();
    public static final String dm_createSessionMessageTableSQL = new StringBuilder().append("CREATE TABLE $sessionMessageTableName ( msgId varchar2(100) NOT NULL," )
            .append(" createTime timestamp NOT NULL,")
            .append(" sessionId varchar2(100) NOT NULL, " )
            .append( "parentAgentId varchar2(100),")  //父agentid
            .append( "agentId varchar2(100),")  //创建消息的agentid
            .append( "agentResultMessage varchar2(1),")  //是否是agent的最终结果消息，需要加载到父agent的记忆消息中
            .append( "seqNo int NOT NULL, " )  //消息序号
            .append( "message clob NOT NULL, " )  //消息正文
            .append( "role varchar2(100) NOT NULL, " )
            .append( "constraint $sessionMessageTableName_PK primary key(msgId))").toString();
    public static final String sqlserver_createSessionMessageTableSQL = new StringBuilder().append("CREATE TABLE $sessionMessageTableName ( msgId varchar(100) NOT NULL," )
            .append( "createTime datetime NOT NULL,")  //创建时间
            .append("sessionId varchar(100) NOT NULL,") //会话id
            .append( "parentAgentId varchar(100),")  //父agentid
            .append( "agentId varchar(100),")  //创建消息的agentid
            .append( "agentResultMessage varchar(1),")  //是否是agent的最终结果消息，需要加载到父agent的记忆消息中
            .append( "seqNo int NOT NULL,")  //消息序号
            .append( "message nvarchar(max) NOT NULL,")  //消息正文
            .append( "role varchar(100) NOT NULL,") 
            .append( "primary key(msgId))").toString();
    public static final String postgresql_createSessionMessageTableSQL = new StringBuilder().append("CREATE TABLE $sessionMessageTableName (msgId varchar(100) NOT NULL," )
            .append( "createTime timestamp NOT NULL,")  //创建时间
            .append( "sessionId varchar(100) NOT NULL,")  //会话id
            .append( "parentAgentId varchar(100),")  //父agentid
            .append( "agentId varchar(100),")  //创建消息的agentid
            .append( "agentResultMessage varchar(1),")  //是否是agent的最终结果消息，需要加载到父agent的记忆消息中
            .append( "seqNo int NOT NULL,")  //消息序号
            .append( "message text NOT NULL,")  //消息正文
            .append( "role varchar(100) NOT NULL,") 
            .append( "primary key(msgId))").toString();
    private String insertSessionSQL;
    private String deleteSessionSQL;
    private String deleteSessionByUserIdSQL;
    private String deleteSessionBySessionIdSQL;
    private String selectSessionByUserIdSQL;
    private String selectSessionBySessionIdSQL;

    private String insertSessionMessageSQL;
    private String deleteSessionMessageSQL;
    private String deleteSessionMessageByUserIdSQL;
    private String deleteSessionMessageBySessionIdSQL;
    private String selectSessionMessageByUserIdSQL;
    private String selectSessionMessageBySessionIdSQL;

    private String selectMaxSeqNoBySessionIdSQL;

    private String selectSessionMessageBySessionId2ndAgentIdSQL;
    
    private String existSQL;

    private String existMessageSQL;
    private String dataSource;
    /**
     * 会话基本信息存储表名称
     */
    private String sessionTableName = "agent_session";

    /**
     * 会话消息记录存储表名称
     */
    private String sessionMessageTableName = "agent_session_message";

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public String getDataSource() {
        return dataSource;
    }

    public void setSessionTableName(String sessionTableName) {
        this.sessionTableName = sessionTableName;
    }

    public String getSessionTableName() {
        return sessionTableName;
    }

    public void init(){
        existSQL = new StringBuilder().append("select 1 from ").append(sessionTableName).toString();
        existMessageSQL = new StringBuilder().append("select 1 from ").append(sessionMessageTableName).toString();
        insertSessionSQL = "INSERT INTO "+sessionTableName+" (sessionId, createTime, useId, agentId, title) \n" +
                "VALUES (?, ?, ?, ?, ?)";
        
        deleteSessionByUserIdSQL = new StringBuilder().append("delete from ")
                .append(sessionTableName).append(" where  useId=?").toString();

        deleteSessionBySessionIdSQL = new StringBuilder().append("delete from ")
                .append(sessionTableName).append(" where sessionId=? ").toString();

        selectSessionByUserIdSQL = new StringBuilder().append("select * from ")
                .append(sessionTableName).append(" where useId=?").toString();

        selectSessionBySessionIdSQL = new StringBuilder().append("select * from ")
                .append(sessionTableName).append(" where sessionId=? ").toString();

        /**
         *         .append( "parentAgentId varchar(100),")  //父agentid
         *             .append( "agentId varchar(100),")  //创建消息的agentid
         *             .append( "agentResultMessage varchar(1),")  //是否是agent的最终结果消息，需要加载到父agent的记忆消息中
         */
        insertSessionMessageSQL = new StringBuilder().append("insert into ").append(sessionMessageTableName)
                .append(" (msgId,createTime,sessionId,parentAgentId,agentId,agentResultMessage,seqNo,message,role) values(?,?,?,?,?,?,?,?,?)").toString();
        deleteSessionMessageSQL = "DELETE FROM "+sessionMessageTableName+" where msgId=? and jobType=?";
        deleteSessionMessageByUserIdSQL = new StringBuilder().append("delete from ")
                .append(sessionMessageTableName).append(" where useId=? ").toString();

        deleteSessionMessageBySessionIdSQL = new StringBuilder().append("delete from ")
                .append(sessionMessageTableName).append(" where sessionId=? ").toString();

        selectSessionMessageByUserIdSQL = new StringBuilder().append("select *  from ")
                .append(sessionMessageTableName).append(" where useId=? ").toString();

        /**
         * 查询最近的消息
         */
        selectSessionMessageBySessionIdSQL = new StringBuilder().append("select *  from ")
                .append(sessionMessageTableName).append(" where sessionId=? and (agentId is null or (parentAgentId is null and agentResultMessage = '1')) order by createTime asc").toString();

        selectMaxSeqNoBySessionIdSQL = new StringBuilder().append("select max(seqNo) from ")
                .append(sessionMessageTableName).append(" where sessionId=? ").toString();

        selectSessionMessageBySessionId2ndAgentIdSQL = new StringBuilder().append("select *  from ")
                .append(sessionMessageTableName).append(" where sessionId=? and (agentId= ? or (parentAgentId= ? and agentResultMessage = '1')) order by createTime asc").toString();
    }

    public String getSelectMaxSeqNoBySessionIdSQL() {
        return selectMaxSeqNoBySessionIdSQL;
    }

    public String getSelectSessionMessageBySessionId2ndAgentIdSQL() {
        return selectSessionMessageBySessionId2ndAgentIdSQL;
    }

    public String evalCreateSessionTableSQL(String dbName) {
        DB adaptor  = DBUtil.getDBAdapter(dbName);
        String type = adaptor.getDBTYPE();
        String sql = null;
        if ("mysql".equalsIgnoreCase(type)) {
            sql = mysql_createSessionTableSQL;
        } else if ("oracle".equalsIgnoreCase(type)) {
            sql = oracle_createSessionTableSQL;
        } else if ("dm".equalsIgnoreCase(type   )) {
            sql = dm_createSessionTableSQL;
        } else if ("sqlserver".equalsIgnoreCase(type)) {
            sql = sqlserver_createSessionTableSQL;
        } else if ("postgresql".equalsIgnoreCase(type)) {
            sql = postgresql_createSessionTableSQL;
        }
        return sql.replace("$sessionTableName", sessionTableName);
    }
    
    public String evalCreateSessionMessageTableSQL(String dbName) {
        DB adaptor  = DBUtil.getDBAdapter(dbName);
        String type = adaptor.getDBTYPE();
        String sql = null;
        if ("mysql".equalsIgnoreCase(type)) {
            sql = mysql_createSessionMessageTableSQL;
        } else if ("oracle".equalsIgnoreCase(type)) {
            sql = oracle_createSessionMessageTableSQL;
        } else if ("dm".equalsIgnoreCase(type)) {
            sql = dm_createSessionMessageTableSQL;
        } else if ("sqlserver".equalsIgnoreCase(type)) {
            sql = sqlserver_createSessionMessageTableSQL;
        } else if ("postgresql".equalsIgnoreCase(type)) {
            sql = postgresql_createSessionMessageTableSQL;
        }
        return sql.replace("$sessionMessageTableName", sessionMessageTableName);
    }


    public String getInsertSessionSQL() {
        return insertSessionSQL;
    }

    public String getDeleteSessionSQL() {
        return deleteSessionSQL;
    }

    public String getDeleteSessionByUserIdSQL() {
        return deleteSessionByUserIdSQL;
    }

    public String getDeleteSessionBySessionIdSQL() {
        return deleteSessionBySessionIdSQL;
    }

    public String getSelectSessionByUserIdSQL() {
        return selectSessionByUserIdSQL;
    }

    public String getSelectSessionBySessionIdSQL() {
        return selectSessionBySessionIdSQL;
    }

    public String getInsertSessionMessageSQL() {
        return insertSessionMessageSQL;
    }

    public String getDeleteSessionMessageSQL() {
        return deleteSessionMessageSQL;
    }

    public String getDeleteSessionMessageByUserIdSQL() {
        return deleteSessionMessageByUserIdSQL;
    }

    public String getDeleteSessionMessageBySessionIdSQL() {
        return deleteSessionMessageBySessionIdSQL;
    }

    public String getSelectSessionMessageByUserIdSQL() {
        return selectSessionMessageByUserIdSQL;
    }

    public String getSelectSessionMessageBySessionIdSQL() {
        return selectSessionMessageBySessionIdSQL;
    }

    public String getExistSQL() {
        return existSQL;
    }

    public String getSessionMessageTableName() {
        return sessionMessageTableName;
    }

    public String getExistMessageSQL() {
        return existMessageSQL;
    }

    public void setSessionMessageTableName(String sessionMessageTableName) {
        this.sessionMessageTableName = sessionMessageTableName;
    }
}
