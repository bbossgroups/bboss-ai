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

import java.util.List;

/**
 * 数据库会话存储配置：sql语句
 * @author biaoping.yin
 * @Date 2026/4/5
 */
public class AgentSessionStoreDBConfig {
    public static String sqlite_createSessionTableSQL = new StringBuilder().append("create table $sessionTableName (sessionId varchar(100),")  //会话id
            .append( "createTime number(20),") //创建时间
            .append(" lastAccessTime number(20), " )
            .append( "userId varchar(100),")  //用户id
            .append( "agentId varchar(100),")  //代理id
            .append( "title varchar(500),")  //会话标题     
            .append( "domain varchar(100),")  //会话所属领域       
            .append( "PRIMARY KEY (sessionId))").toString();


    public static final String mysql_createSessionTableSQL = new StringBuilder().append("CREATE TABLE $sessionTableName ( sessionId varchar(100) NOT NULL comment '会话id'," )
            .append(" createTime datetime NOT NULL comment '创建时间', " )
            .append(" lastAccessTime datetime NOT NULL comment '最后访问时间', " )
            .append( "userId varchar(100) comment '用户id',")  //用户id
            .append( "agentId varchar(100) comment '代理id',")  //代理id
            .append( "title varchar(500) comment '会话标题',")  //会话标题  
            .append( "domain varchar(100) comment '业务领域',")  //会话所属领域      
            .append( "PRIMARY KEY(sessionId)) comment '增量状态同步表主键' ENGINE=InnoDB").toString();
    
    public static final String oracle_createSessionTableSQL = new StringBuilder().append("CREATE TABLE $sessionTableName ( sessionId varchar2(100) NOT NULL," )
            .append(" createTime timestamp NOT NULL,")
            .append(" lastAccessTime timestamp NOT NULL, " )
            .append(" userId varchar2(100) NOT NULL, " )
            .append( "agentId varchar2(100) NOT NULL, " )
            .append("title varchar2(500) NOT NULL,")
            .append( "domain varchar2(100),")  //会话所属领域          
            .append( "constraint $sessionTableName_PK primary key(sessionId))").toString();


    public static final String oracle_addCommentsToSessionTableSQL = new StringBuilder()
            .append("COMMENT ON COLUMN $sessionTableName.sessionId IS '会话id';")
            .append("COMMENT ON COLUMN $sessionTableName.createTime IS '创建时间';")
            .append("COMMENT ON COLUMN $sessionTableName.userId IS '用户id';")
            .append("COMMENT ON COLUMN $sessionTableName.agentId IS '代理id';")
            .append("COMMENT ON COLUMN $sessionTableName.title IS '会话标题';")

            .append("COMMENT ON COLUMN $sessionTableName.domain IS '会话所属领域';")
            .toString();
    
    public static final String dm_createSessionTableSQL = new StringBuilder().append("CREATE TABLE $sessionTableName ( sessionId varchar2(100) NOT NULL," )
            .append(" createTime timestamp NOT NULL,")
            .append(" lastAccessTime timestamp NOT NULL, " )
            .append(" userId varchar2(100) NOT NULL, " )
            .append( "agentId varchar2(100) NOT NULL, " )
            .append("title varchar2(500) NOT NULL,")
            .append( "domain varchar2(100),")  //会话所属领域     
            .append( "constraint $sessionTableName_PK primary key(sessionId))").toString();
    public static final String sqlserver_createSessionTableSQL = new StringBuilder().append("CREATE TABLE $sessionTableName " )
            .append( "( sessionId varchar(100) NOT NULL," ) //会话id
            .append( "createTime datetime NOT NULL,")  //创建时间
            .append(" lastAccessTime datetime NOT NULL, " ) //最后访问时间
            .append("userId varchar(100) NOT NULL,") //用户id
            .append( "agentId varchar(100) NOT NULL,")  //代理id
            .append( "title varchar(500) NOT NULL,")  //会话标题   
            .append( "domain varchar2(100),")  //会话所属领域             
            .append( "constraint $sessionTableName_PK primary key(sessionId))") //增量状态同步表主键
            .toString();

    public static final String postgresql_createSessionTableSQL = new StringBuilder().append("CREATE TABLE $sessionTableName (sessionId varchar(100) NOT NULL," )
            .append( "createTime timestamp NOT NULL,")  //创建时间
            .append(" lastAccessTime timestamp NOT NULL, " )
            .append( "userId varchar(100),")  //用户id
            .append( " agentId varchar(100)  NOT NULL,") //代理id
            .append( "title varchar(500) ,")  //会话标题
            .append( "domain varchar2(100),")  //会话所属领域     
            .append( "primary key(sessionId))")//增量状态同步表主键
            .toString();

    public static final String postgresql_addCommentsToSessionTableSQL = new StringBuilder()
            .append("COMMENT ON COLUMN $sessionTableName.sessionId IS '会话id';")
            .append("COMMENT ON COLUMN $sessionTableName.createTime IS '创建时间';")
            .append("COMMENT ON COLUMN $sessionTableName.userId IS '用户id';")
            .append("COMMENT ON COLUMN $sessionTableName.agentId IS '代理id';")
            .append("COMMENT ON COLUMN $sessionTableName.title IS '会话标题';")
            .toString();
	
	public static final String clickhouse_createLocalSessionTableSQL = new StringBuilder().append("CREATE TABLE ${sessionTableName}_local  ON CLUSTER $clickhouseCluster ")
			.append(" (")
                .append("sessionId String  COMMENT '会话id',  ")  
                 .append("createTime DateTime COMMENT '创建时间',")
                 .append("lastAccessTime DateTime COMMENT '最后访问时间',")
                 .append("userId String COMMENT '用户id',")
                 .append("agentId String COMMENT '代理id',")
                 .append("title String COMMENT '会话标题',")   
                 .append("domain String COMMENT '会话所属领域'         ")
			.append(")")
			.append("ENGINE = ReplicatedMergeTree('/clickhouse/tables/{shard}/{database}/{table}', '{replica}')")
			.append("ORDER BY (sessionId)").toString();
	
	public static final String clickhouse_createClusterSessionTableSQL = new StringBuilder().append("CREATE TABLE ${sessionTableName} on cluster $clickhouseCluster AS ${sessionTableName}_local").append("  ENGINE = Distributed($clickhouseCluster, currentDatabase(), ${sessionTableName}_local, rand())").toString();
	
    public static String sqlitex_createSessionMessageTableSQL = new StringBuilder().append("create table $sessionMessageTableName (msgId varchar(100),")  //消息id
            .append( "createTime number(20),") //创建时间
            .append( "parentAgentId varchar(100),")  //父agentid
            .append( "agentId varchar(100),")  //创建或者消息所属的agentid,如果节点类型是串行容器智能体节点（sequence）、并行容器智能体节点（parallel），对应创建消息的agentid为subAgentIdBy对应的值
            .append( "agentNodeType varchar(100),")  //智能体节点类型：标准化智能体节点（standard）、串行容器智能体节点（sequence）、并行容器智能体节点（parallel）
            .append( "subAgentIdBy varchar(100),")  //创建消息的子agentid，节点类型是串行容器智能体节点（sequence）、并行容器智能体节点（parallel）有值
            .append( "messageType varchar(50),")  //0 代表子智能体辅助消息， 1 代表子智能体输出结果 2 代表用户输入消息 3 智能体系统消息 5 智能体跟踪消息 是否是agent的最终结果消息（messageType=1），需要加载到父agent的记忆消息中
            .append( "sessionId varchar(100),")  //会话id
            .append( "requestId varchar(100), " )  //请求id
            .append( "traceId varchar(100), " )  //trace id
            .append( "seqNo int,")  //消息序号
            .append( "message text,")  //消息正文

            .append( "tokenMetrics text, " )  //token消耗统计
            .append( "elapsed INTEGER,")  //耗时
            .append( "role varchar(100),")
            .append( "marks varchar(500),")
            .append( "metadata text,")
            .append( "PRIMARY KEY (msgId))").toString();
	


    public static final String mysql_createSessionMessageTableSQL = new StringBuilder().append("CREATE TABLE $sessionMessageTableName ( msgId varchar(100) NOT NULL comment '消息id'," )
            .append(" createTime datetime NOT NULL comment '创建时间', " )
            .append( "sessionId varchar(100) NOT NULL, " )  //会话id
            .append( "requestId varchar(100), " )  //请求id
            .append( "traceId varchar(100), " )  //trace id
            .append( "parentAgentId varchar(100),")  //父agentid
            .append( "agentId varchar(100),")  //创建或者消息所属的agentid,如果节点类型是串行容器智能体节点（sequence）、并行容器智能体节点（parallel），对应创建消息的agentid为subAgentIdBy对应的值
            .append( "agentNodeType varchar(100),")  //智能体节点类型：标准化智能体节点（standard）、串行容器智能体节点（sequence）、并行容器智能体节点（parallel）
            .append( "subAgentIdBy varchar(100),")  //创建消息的子agentid，节点类型是串行容器智能体节点（sequence）、并行容器智能体节点（parallel）有值
            .append( "messageType varchar(50),")  //0 代表子智能体辅助消息， 1 代表子智能体输出结果 2 代表用户输入消息 3 智能体系统消息 5 智能体跟踪消息 是否是agent的最终结果消息（messageType=1），需要加载到父agent的记忆消息中
            .append( "seqNo int NOT NULL, " )  //消息序号
            .append( "message LONGTEXT  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL, " )  //消息正文
            .append( "role varchar(100) NOT NULL, " )
            .append( "tokenMetrics LONGTEXT  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci, " )  //token消耗统计
            .append( "elapsed BIGINT,")  //耗时
            .append( "marks varchar(500),")
            .append( "metadata  LONGTEXT  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,")
            .append( "primary key(msgId)) comment '消息表主键' ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci").toString();

   
    public static final String oracle_createSessionMessageTableSQL = new StringBuilder().append("CREATE TABLE $sessionMessageTableName ( msgId varchar2(100) NOT NULL," )
            .append(" createTime timestamp NOT NULL,")
            .append(" sessionId varchar2(100) NOT NULL, " )
            .append( "requestId varchar2(100), " )  //请求id
            .append( "traceId varchar2(100), " )  //trace id
            .append( "parentAgentId varchar2(100),")  //父agentid
            .append( "agentId varchar2(100),")  //创建或者消息所属的agentid,如果节点类型是串行容器智能体节点（sequence）、并行容器智能体节点（parallel），对应创建消息的agentid为subAgentIdBy对应的值
            .append( "agentNodeType varchar2(100),")  //智能体节点类型：标准化智能体节点（standard）、串行容器智能体节点（sequence）、并行容器智能体节点（parallel）
            .append( "subAgentIdBy varchar2(100),")  //创建消息的子agentid，节点类型是串行容器智能体节点（sequence）、并行容器智能体节点（parallel）有值
            .append( "messageType varchar2(50),")  //0 代表子智能体辅助消息， 1 代表子智能体输出结果 2 代表用户输入消息 3 智能体系统消息 5 智能体跟踪消息 是否是agent的最终结果消息（messageType=1），需要加载到父agent的记忆消息中
            .append( "seqNo int NOT NULL, " )  //消息序号
            .append( "message clob NOT NULL, " )  //消息正文
            .append( "tokenMetrics clob , " )  //token消耗统计
            .append( "elapsed NUMBER(19,0),")  //耗时
            
            .append( "role varchar2(100) NOT NULL, " )

            .append( "marks varchar2(500),")
            .append( "metadata clob,")
            .append( "constraint $sessionMessageTableName_PK primary key(msgId))").toString();
    public static final String dm_createSessionMessageTableSQL = new StringBuilder().append("CREATE TABLE $sessionMessageTableName ( msgId varchar2(100) NOT NULL," )
            .append(" createTime timestamp NOT NULL,")
            .append(" sessionId varchar2(100) NOT NULL, " )
            .append( "requestId varchar2(100), " )  //请求id
            .append( "traceId varchar2(100), " )  //trace id
            .append( "parentAgentId varchar2(100),")  //父agentid
            .append( "agentId varchar2(100),")  //创建或者消息所属的agentid,如果节点类型是串行容器智能体节点（sequence）、并行容器智能体节点（parallel），对应创建消息的agentid为subAgentIdBy对应的值
            .append( "agentNodeType varchar2(100),")  //智能体节点类型：标准化智能体节点（standard）、串行容器智能体节点（sequence）、并行容器智能体节点（parallel）
            .append( "subAgentIdBy varchar2(100),")  //创建消息的子agentid，节点类型是串行容器智能体节点（sequence）、并行容器智能体节点（parallel）有值
            .append( "messageType varchar2(50),")  //0 代表子智能体辅助消息， 1 代表子智能体输出结果 2 代表用户输入消息 3 智能体系统消息 5 智能体跟踪消息 是否是agent的最终结果消息（messageType=1），需要加载到父agent的记忆消息中
            .append( "seqNo int NOT NULL, " )  //消息序号
            .append( "message clob NOT NULL, " )  //消息正文
            .append( "tokenMetrics clob , " )  //token消耗统计
            .append( "elapsed NUMBER(19,0),")  //耗时
            .append( "role varchar2(100) NOT NULL, " )

            .append( "marks varchar2(500),")
            .append( "metadata clob,")
            .append( "constraint $sessionMessageTableName_PK primary key(msgId))").toString();
    public static final String sqlserver_createSessionMessageTableSQL = new StringBuilder().append("CREATE TABLE $sessionMessageTableName ( msgId varchar(100) NOT NULL," )
            .append( "createTime datetime NOT NULL,")  //创建时间
            .append("sessionId varchar(100) NOT NULL,") //会话id
            .append( "requestId varchar(100), " )  //请求id
            .append( "traceId varchar(100), " )  //trace id
            .append( "parentAgentId varchar(100),")  //父agentid
            .append( "agentId varchar(100),")  //创建或者消息所属的agentid,如果节点类型是串行容器智能体节点（sequence）、并行容器智能体节点（parallel），对应创建消息的agentid为subAgentIdBy对应的值
            .append( "agentNodeType varchar(100),")  //智能体节点类型：标准化智能体节点（standard）、串行容器智能体节点（sequence）、并行容器智能体节点（parallel）
            .append( "subAgentIdBy varchar(100),")  //创建消息的子agentid，节点类型是串行容器智能体节点（sequence）、并行容器智能体节点（parallel）有值
            .append( "messageType varchar(50),")  //0 代表子智能体辅助消息， 1 代表子智能体输出结果 2 代表用户输入消息 3 智能体系统消息 5 智能体跟踪消息 是否是agent的最终结果消息（messageType=1），需要加载到父agent的记忆消息中
            .append( "seqNo int NOT NULL,")  //消息序号
            .append( "message nvarchar(max) NOT NULL,")  //消息正文
            .append( "tokenMetrics nvarchar(max),")  //token消耗统计
            .append( "elapsed BIGINT,")  //耗时
            .append( "role varchar(100) NOT NULL,")

            .append( "marks varchar(500),")
            .append( "metadata nvarchar(max),")
            .append( "primary key(msgId))").toString();
    public static final String postgresql_createSessionMessageTableSQL = new StringBuilder().append("CREATE TABLE $sessionMessageTableName (msgId varchar(100) NOT NULL," )
            .append( "createTime timestamp NOT NULL,")  //创建时间
            .append( "sessionId varchar(100) NOT NULL,")  //会话id
            .append( "requestId varchar(100), " )  //请求id
            .append( "traceId varchar(100), " )  //trace id
            .append( "parentAgentId varchar(100),")  //父agentid
            .append( "agentId varchar(100),")  //创建或者消息所属的agentid,如果节点类型是串行容器智能体节点（sequence）、并行容器智能体节点（parallel），对应创建消息的agentid为subAgentIdBy对应的值
            .append( "agentNodeType varchar(100),")  //智能体节点类型：标准化智能体节点（standard）、串行容器智能体节点（sequence）、并行容器智能体节点（parallel）
            .append( "subAgentIdBy varchar(100),")  //创建消息的子agentid，节点类型是串行容器智能体节点（sequence）、并行容器智能体节点（parallel）有值
            .append( "messageType varchar(50),")  //0 代表子智能体辅助消息， 1 代表子智能体输出结果 2 代表用户输入消息 3 智能体系统消息 5 智能体跟踪消息 是否是agent的最终结果消息（messageType=1），需要加载到父agent的记忆消息中
            .append( "seqNo int NOT NULL,")  //消息序号
            .append( "message text NOT NULL,")  //消息正文
            .append( "tokenMetrics text,")  //token消耗统计
            .append( "elapsed BIGINT,")  //耗时
            .append( "role varchar(100) NOT NULL,")
            .append( "marks varchar(500),")
            .append( "metadata text,")
            .append( "primary key(msgId))").toString();
	
	public static final String clickhouse_createLocalSessionMessageTableSQL = new StringBuilder().append("CREATE TABLE ${sessionMessageTableName}_local  ON CLUSTER $clickhouseCluster ")
			.append("(")
			.append("    msgId String  COMMENT '消息id',")
			.append("createTime DateTime COMMENT '创建时间',")
			.append("sessionId String COMMENT '会话id',")
			.append("requestId String COMMENT '请求id',")
			.append("traceId String COMMENT 'trace id',")
			.append("parentAgentId String COMMENT '父智能体id',")
			.append("agentId String COMMENT '创建或者消息所属的agentid,如果节点类型是串行容器智能体节点（sequence）、并行容器智能体节点（parallel），对应创建消息的agentid为subAgentIdBy对应的值',")
			.append("agentNodeType String COMMENT '智能体节点类型：标准化智能体节点（standard）、串行容器智能体节点（sequence）、并行容器智能体节点（parallel）',")
			.append("subAgentIdBy String COMMENT '创建消息的子agentid，节点类型是串行容器智能体节点（sequence）、并行容器智能体节点（parallel）有值',")
			.append("messageType String COMMENT '0 代表子智能体辅助消息， 1 代表子智能体输出结果 2 代表用户输入消息 3 智能体系统消息 5 智能体跟踪消息 是否是agent的最终结果消息（messageType=1），需要加载到父agent的记忆消息中',")
			.append("seqNo Int32 COMMENT '消息序号',")
			.append("message String COMMENT '消息正文',")
			.append("tokenMetrics String COMMENT 'token消耗统计',")
			.append("elapsed Int32 COMMENT '耗时',")
			.append("role String COMMENT '角色',")
			.append("marks String COMMENT '消息标记',")
			.append("metadata String COMMENT '消息元数据'")
			.append(")")
			.append("ENGINE = ReplicatedMergeTree('/clickhouse/tables/{shard}/{database}/{table}', '{replica}')")
			.append("ORDER BY (sessionId, createTime,seqNo)").toString();
	
	public static final String clickhouse_createClusterSessionMessageTableSQL = new StringBuilder().append("CREATE TABLE ${sessionMessageTableName} on cluster $clickhouseCluster AS ${sessionMessageTableName}_local ENGINE = Distributed($clickhouseCluster, currentDatabase(), ${sessionMessageTableName}_local, sipHash64(sessionId))").toString();
	
	/**
     * 智能体之间消息引用关系表sqlite：后续智能体会引用前一个智能体的输出消息
     */
   public static final String sqlite_createSessionMessageReferenceTableSQL = new StringBuilder().append("CREATE TABLE $sessionMessageReferenceTableName ( msgId TEXT NOT NULL, " )
           .append(" msgAgentId varchar(100) NOT NULL, " )
           .append( "refAgentId varchar(100) NOT NULL,)" )
            .append( "requestId varchar(100), " )  //请求id   
            .append("sessionId varchar(100) NOT NULL") //会话id
            .toString();

    /**
     * 智能体之间消息引用关系表mysql：后续智能体会引用前一个智能体的输出消息
     */
    public static final String mysql_createSessionMessageReferenceTableSQL = new StringBuilder().append("CREATE TABLE $sessionMessageReferenceTableName ( msgId varchar(100) NOT NULL comment '消息id'," )
            .append(" msgAgentId varchar(100) NOT NULL comment '消息所属智能体agentId', " )
            .append( "refAgentId varchar(100) NOT NULL comment '引用消息智能体agentId'," )   
            .append("sessionId varchar(100) NOT NULL,") //会话id
            .append( "requestId varchar(100), " )  //请求id          
            .append("UNIQUE INDEX uk_msg_ref (msgId, refAgentId)")
            .append( ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci").toString();

    /**
     * 智能体之间消息引用关系表oracle：后续智能体会引用前一个智能体的输出消息
     */
    public static final String oracle_createSessionMessageReferenceTableSQL = new StringBuilder()
        .append("CREATE TABLE $sessionMessageReferenceTableName ( ")
        .append("msgId VARCHAR2(100) NOT NULL, ")                           // Oracle中使用VARCHAR2而不是varchar
        .append("msgAgentId VARCHAR2(100) NOT NULL, ")                       // 消息所属智能体agentId
        .append("refAgentId VARCHAR2(100) NOT NULL, ")                       // 引用消息智能体agentId
            .append("sessionId varchar2(100) NOT NULL,") //会话id
            .append( "requestId varchar2(100), " )  //请求id

        .append("CONSTRAINT uk_msg_ref UNIQUE (msgId, refAgentId) ")        // 唯一约束
        .append(")")
        .toString();

    /**
     * 智能体之间消息引用关系表dm：后续智能体会引用前一个智能体的输出消息
     */
    public static final String dm_createSessionMessageReferenceTableSQL = new StringBuilder()
            .append("CREATE TABLE $sessionMessageReferenceTableName ( ")
            .append("msgId VARCHAR2(100) NOT NULL, ")                           // Oracle中使用VARCHAR2而不是varchar
            .append("msgAgentId VARCHAR2(100) NOT NULL, ")                       // 消息所属智能体agentId
            .append("refAgentId VARCHAR2(100) NOT NULL, ")                       // 引用消息智能体agentId
            .append("sessionId varchar2(100) NOT NULL,") //会话id
            .append( "requestId varchar(100), " )  //请求id
 
            .append("CONSTRAINT uk_msg_ref UNIQUE (msgId, refAgentId) ")        // 唯一约束
            .append(")")
            .toString();
// ... existing code ...
/**
 * 智能体之间消息引用关系表sqlserver：后续智能体会引用前一个智能体的输出消息
 */
public static final String sqlserver_createSessionMessageReferenceTableSQL = new StringBuilder()
        .append("CREATE TABLE $sessionMessageReferenceTableName ( ")
        .append("msgId VARCHAR(100) NOT NULL, ")                           
        .append("msgAgentId VARCHAR(100) NOT NULL, ")                       // 消息所属智能体agentId
        .append("refAgentId VARCHAR(100) NOT NULL, ")                       // 引用消息智能体agentId
        .append("sessionId varchar(100) NOT NULL,") //会话id
        .append( "requestId varchar(100), " )  //请求id
      
        .append("CONSTRAINT uk_msg_ref UNIQUE (msgId, refAgentId) ")     // 唯一约束
        .append(")")
        .toString();
// ... existing code ...

     
   /**
    * 智能体之间消息引用关系表postgresql：后续智能体会引用前一个智能体的输出消息
    */
   public static final String postgresql_createSessionMessageReferenceTableSQL = new StringBuilder()
           .append("CREATE TABLE $sessionMessageReferenceTableName ( ")
           .append("msgId VARCHAR(100) NOT NULL, ")                           // PostgreSQL使用VARCHAR而不是VARCHAR2
           .append("msgAgentId VARCHAR(100) NOT NULL, ")                       // 消息所属智能体agentId
           .append("refAgentId VARCHAR(100) NOT NULL, ")                       // 引用消息智能体agentId
           .append("sessionId varchar(100) NOT NULL,") //会话id
           .append( "requestId varchar(100), " )  //请求id
 
           .append("CONSTRAINT uk_msg_ref UNIQUE (msgId, refAgentId)           ") // 唯一约束
           .append(")")
           .toString();
	
	public static final String clickhouse_createLocalSessionMessageReferenceTableSQL = new StringBuilder()
			.append("CREATE TABLE ${sessionMessageReferenceTableName}_local  ON CLUSTER $clickhouseCluster ")
			.append("( msgId String  COMMENT  '消息id',")
			.append("msgAgentId String COMMENT '消息所属智能体agentId', ")
			.append("refAgentId String COMMENT '引用消息智能体agentId',")
			.append("sessionId String COMMENT '会话id',")
			.append("requestId String COMMENT '请求id'")
			.append(")")
			.append("ENGINE = ReplicatedMergeTree('/clickhouse/tables/{shard}/{database}/{table}', '{replica}')")
			.append("ORDER BY (sessionId)").toString();
	
	public static final String clickhouse_createClusterSessionMessageReferenceTableSQL = new StringBuilder().append("CREATE TABLE ${sessionMessageReferenceTableName} on cluster $clickhouseCluster AS ${sessionMessageReferenceTableName}_local ENGINE = Distributed($clickhouseCluster, currentDatabase(), ${sessionMessageReferenceTableName}_local, sipHash64(sessionId))").toString();
	
	private String insertSessionSQL;
    private String updateSessionLastAccessTimeSQL;
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

    private String selectSessionMessageBySessionId2ndAgentIdSQL0;
	
	
	private String selectSessionMessageBySessionId2ndAgentIdSQL1;
	
	
	 
	
	
	private String selectAgentSessionMessageReferenceIdsBySessionIdSQL;
    
    private String existSQL;

    private String existMessageSQL;
    
    private String existMessageReferenceSQL;

    public String getInsertSessionMessageRerenceSQL() {
        return insertSessionMessageRerenceSQL;
    }

    private String insertSessionMessageRerenceSQL;




    private String deleteSessionMessageRerenceBySessionIdSQL;
    private String dataSource;
    /**
     * 会话基本信息存储表名称
     */
    private String sessionTableName = "agent_session";

    /**
     * 会话消息记录存储表名称
     */
    private String sessionMessageTableName = "agent_session_message";

    public String getSessionMessageReferenceTableName() {
        return sessionMessageReferenceTableName;
    }

    /**
     * 会话消息记录引用关系表名称
     */
    private String sessionMessageReferenceTableName = "agent_session_message_ref";

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
    public String getDeleteSessionMessageRerenceBySessionIdSQL() {
        return deleteSessionMessageRerenceBySessionIdSQL;
    }
    public void init(){
        existSQL = new StringBuilder().append("select 1 from ").append(sessionTableName).toString();
        existMessageSQL = new StringBuilder().append("select 1 from ").append(sessionMessageTableName).toString();
        existMessageReferenceSQL = new StringBuilder().append("select 1 from ").append(sessionMessageReferenceTableName).toString();
        insertSessionSQL = "INSERT INTO "+sessionTableName+" (sessionId, createTime, lastAccessTime,userId, agentId, title,domain) \n" +
                "VALUES (?, ?, ?, ?, ?,?,?)";
        updateSessionLastAccessTimeSQL = "UPDATE "+sessionTableName+" SET lastAccessTime = ? WHERE sessionId = ?";
        
        deleteSessionByUserIdSQL = new StringBuilder().append("delete from ")
                .append(sessionTableName).append(" where  userId=?").toString();

        deleteSessionBySessionIdSQL = new StringBuilder().append("delete from ")
                .append(sessionTableName).append(" where sessionId=? ").toString();

        selectSessionByUserIdSQL = new StringBuilder().append("select * from ")
                .append(sessionTableName).append(" where userId=? order by createTime desc").toString();

        selectSessionBySessionIdSQL = new StringBuilder().append("select * from ")
                .append(sessionTableName).append(" where sessionId=? ").toString();

        /**
         *         .append( "parentAgentId varchar(100),")  //父agentid
         *             .append( "agentId varchar(100),")  //创建消息的agentid
         *             .append( "messageType varchar(50),")  //0 代表子智能体辅助消息， 1 代表子智能体输出结果 2 代表用户输入消息 3 智能体系统消息 5 智能体跟踪消息 是否是agent的最终结果消息（messageType=1），需要加载到父agent的记忆消息中
         *             .append( "agentNodeType varchar(100),")  //智能体节点类型：标准化智能体节点（standard）、串行容器智能体节点（sequence）、并行容器智能体节点（parallel）
         *             .append( "subAgentIdBy varchar(100),")  //创建消息的子agentid，节点类型是串行容器智能体节点（sequence）、并行容器智能体节点（parallel）有值
         */
        insertSessionMessageSQL = new StringBuilder().append("insert into ").append(sessionMessageTableName)
                .append(" (msgId,createTime,sessionId,parentAgentId,agentId,messageType,")
                .append("seqNo,message,role,marks,metadata,requestId,tokenMetrics,elapsed,traceId")
                .append(",agentNodeType,subAgentIdBy")
                .append(") values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)").toString();

        insertSessionMessageRerenceSQL = "INSERT INTO "+sessionMessageReferenceTableName+" (msgId,msgAgentId,refAgentId,sessionId,requestId) " +
                                                    "VALUES (?, ?, ?, ?, ?)";
        deleteSessionMessageSQL = "DELETE FROM "+sessionMessageTableName+" where msgId=? and jobType=?";
        deleteSessionMessageByUserIdSQL = new StringBuilder().append("delete from ")
                .append(sessionMessageTableName).append(" where userId=? ").toString();

        deleteSessionMessageBySessionIdSQL = new StringBuilder().append("delete from ")
                .append(sessionMessageTableName).append(" where sessionId=? ").toString();

        deleteSessionMessageRerenceBySessionIdSQL = new StringBuilder().append("delete from ")
                .append(sessionMessageReferenceTableName).append(" where sessionId=? ").toString();

        selectSessionMessageByUserIdSQL = new StringBuilder().append("select *  from ")
                .append(sessionMessageTableName).append(" where userId=? order by createTime,seqNo desc").toString();

        /**
         * 查询最近的消息,恢复到对话中 
         * 0 代表子智能体辅助消息， 1 代表子智能体输出结果 2 代表用户输入消息 3 智能体系统消息 5 智能体跟踪消息 是否是agent的最终结果消息（messageType=1），需要加载到父agent的记忆消息中
         * 排除掉智能体跟踪消息
         */
        selectSessionMessageBySessionIdSQL = new StringBuilder().append("select *  from ")
                .append(sessionMessageTableName).append(" where sessionId=? and (agentId is null or (parentAgentId is null and messageType = '1')) ")
                .append("and messageType in ('0','1','2','3','4') order by createTime,seqNo asc").toString();

        selectMaxSeqNoBySessionIdSQL = new StringBuilder().append("select max(seqNo) from ")
                .append(sessionMessageTableName).append(" where sessionId=? ").toString();
		
//		selectSessionMessageBySessionId2ndAgentIdSQL0 = new StringBuilder()
//				.append("select *  from ")
//				.append(sessionMessageTableName)
//				.append(" where (sessionId=? and (agentId= ? or (parentAgentId= ? and messageType = '1')) ")
//				.append("and messageType in ('0','1','2','3','4'))" )
//				.append(" or msgId in (select msgId from ")
//				.append(sessionMessageReferenceTableName)
//				.append(" where sessionId=? and refAgentId = ?) " )
//				.append("order  by createTime, seqNo asc").toString();

        selectSessionMessageBySessionId2ndAgentIdSQL0 = new StringBuilder()
                .append("select *  from ")
                .append(sessionMessageTableName)
                .append(" where (sessionId=? and (agentId= ? or (parentAgentId= ? and messageType = '1')) ")
                .append("and messageType in ('0','1','2','3','4'))" )               
                 .toString();
		 
		selectSessionMessageBySessionId2ndAgentIdSQL1 = new StringBuilder()
				.append(" order  by createTime, seqNo asc").toString();
		
		selectAgentSessionMessageReferenceIdsBySessionIdSQL = new StringBuilder()
				.append("select msgId from ")
				.append(sessionMessageReferenceTableName)
                .append(" where sessionId=? and refAgentId = ?")
                .toString();
    }

	
    public String getSelectMaxSeqNoBySessionIdSQL() {
        return selectMaxSeqNoBySessionIdSQL;
    }

    public String getSelectSessionMessageBySessionId2ndAgentIdSQL(List<String > refMsgIds) {
		StringBuilder sql = new StringBuilder();		
		sql.append(selectSessionMessageBySessionId2ndAgentIdSQL0);
		if(refMsgIds != null && refMsgIds.size() > 0){
				sql.append(" or  msgId in (");
				for(int i = 0; i < refMsgIds.size(); i ++){
					if(i > 0)
						sql.append(",");
					sql.append("'").append(refMsgIds.get(i)).append("'");
				}				 
				sql.append(") ");
		}
		sql.append(selectSessionMessageBySessionId2ndAgentIdSQL1);		
        return sql.toString();
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
        else if("sqlite".equalsIgnoreCase(type))
            sql = this.sqlite_createSessionTableSQL;
        return sql.replace("$sessionTableName", sessionTableName);
    }
	
	public String evalCreateClickhouseLocalSessionTableSQL( String clickhouseCluster) {
		
		String sql = this.clickhouse_createLocalSessionTableSQL;
		return sql.replace("${sessionTableName}", sessionTableName).replace("$clickhouseCluster", clickhouseCluster);
		
	}
	
	public String evalCreateClusterSessionTableSQL( String clickhouseCluster) {
		
		String	sql = this.clickhouse_createClusterSessionTableSQL;
		
		return sql.replace("${sessionTableName}", sessionTableName).replace("$clickhouseCluster", clickhouseCluster);
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
        else if("sqlite".equalsIgnoreCase(type)) {
            sql = sqlitex_createSessionMessageTableSQL;
        }
        
        return sql.replace("$sessionMessageTableName", sessionMessageTableName);
    }
	
	public String evalCreateClickhouseLocalSessionMessageTableSQL( String clickhouseCluster) {
		
		String sql = this.clickhouse_createLocalSessionMessageTableSQL;
		return sql.replace("${sessionMessageTableName}", sessionMessageTableName).replace("$clickhouseCluster", clickhouseCluster);
		
	}
	
	public String evalCreateClusterSessionMessageTableSQL( String clickhouseCluster) {
		
		String	sql = this.clickhouse_createClusterSessionMessageTableSQL;
		
		return sql.replace("${sessionMessageTableName}", sessionMessageTableName).replace("$clickhouseCluster", clickhouseCluster);
	}
	public boolean isClickhouse(String dbName){
		DB adaptor  = DBUtil.getDBAdapter(dbName);
		String type = adaptor.getDBTYPE();
		if("clickhouse".equalsIgnoreCase(type) || "yandex_clickhouse".equalsIgnoreCase(type))
			return true;
		return false;
	}

    public String evalCreateSessionMessageReferenceTableSQL(String dbName) {
        DB adaptor  = DBUtil.getDBAdapter(dbName);
        String type = adaptor.getDBTYPE();
        String sql = null;
        if ("mysql".equalsIgnoreCase(type)) {
            sql = mysql_createSessionMessageReferenceTableSQL;
        } else if ("oracle".equalsIgnoreCase(type)) {
            sql = oracle_createSessionMessageReferenceTableSQL;
        } else if ("dm".equalsIgnoreCase(type)) {
            sql = dm_createSessionMessageReferenceTableSQL;
        } else if ("sqlserver".equalsIgnoreCase(type)) {
            sql = sqlserver_createSessionMessageReferenceTableSQL;
        } else if ("postgresql".equalsIgnoreCase(type)) {
            sql = postgresql_createSessionMessageReferenceTableSQL;
        }
        else if("sqlite".equalsIgnoreCase(type)) {
            sql = sqlite_createSessionMessageReferenceTableSQL;
        }
		else if("clickhouse".equalsIgnoreCase(type) || "yandex_clickhouse".equalsIgnoreCase(type)) {
			sql = this.clickhouse_createLocalSessionMessageReferenceTableSQL;
			return sql.replace("${sessionMessageReferenceTableName}", sessionMessageReferenceTableName);
		}

        return sql.replace("$sessionMessageReferenceTableName", sessionMessageReferenceTableName);
    }
	
	public String evalCreateClickhouseLocalSessionMessageReferenceTableSQL( String clickhouseCluster) {
		 
		String sql = this.clickhouse_createLocalSessionMessageReferenceTableSQL;
		return sql.replace("${sessionMessageReferenceTableName}", sessionMessageReferenceTableName).replace("$clickhouseCluster", clickhouseCluster);
	 
	}
	
	public String evalCreateClusterSessionMessageReferenceTableSQL( String clickhouseCluster) {
	 
		String	sql = this.clickhouse_createClusterSessionMessageReferenceTableSQL;
		
		return sql.replace("${sessionMessageReferenceTableName}", sessionMessageReferenceTableName).replace("$clickhouseCluster", clickhouseCluster);
	}


    public String getInsertSessionSQL() {
        return insertSessionSQL;
    }

    public String getUpdateSessionLastAccessTimeSQL() {
        return updateSessionLastAccessTimeSQL;
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

    public String getExistMessageReferenceSQL() {
        return existMessageReferenceSQL;
    }
	
	public String getSelectAgentSessionMessageReferenceIdsBySessionIdSQL() {
		return selectAgentSessionMessageReferenceIdsBySessionIdSQL;
	}
}
