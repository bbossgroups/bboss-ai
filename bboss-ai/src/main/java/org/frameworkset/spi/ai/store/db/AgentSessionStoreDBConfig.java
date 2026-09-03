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
import com.frameworkset.common.poolman.SQLExecutor;
import com.frameworkset.orm.adapter.DB;
import org.frameworkset.spi.ai.model.AIRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

/**
 * 数据库会话存储配置：sql语句
 * @author biaoping.yin
 * @Date 2026/4/5
 */
public class AgentSessionStoreDBConfig {
	private static Logger logger = LoggerFactory.getLogger(AgentSessionStoreDBConfig.class);
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
            .append( "domain varchar(100),")  //会话所属领域             
            .append( "constraint $sessionTableName_PK primary key(sessionId))") //增量状态同步表主键
            .toString();

    public static final String postgresql_createSessionTableSQL = new StringBuilder().append("CREATE TABLE $sessionTableName (sessionId varchar(100) NOT NULL," )
            .append( "createTime timestamp NOT NULL,")  //创建时间
            .append(" lastAccessTime timestamp NOT NULL, " )
            .append( "userId varchar(100),")  //用户id
            .append( " agentId varchar(100)  NOT NULL,") //代理id
            .append( "title varchar(500) ,")  //会话标题
            .append( "domain varchar(100),")  //会话所属领域     
            .append( "primary key(sessionId))")//增量状态同步表主键
            .toString();

    public static final String postgresql_addCommentsToSessionTableSQL = new StringBuilder()
            .append("COMMENT ON COLUMN $sessionTableName.sessionId IS '会话id';")
            .append("COMMENT ON COLUMN $sessionTableName.createTime IS '创建时间';")
            .append("COMMENT ON COLUMN $sessionTableName.userId IS '用户id';")
            .append("COMMENT ON COLUMN $sessionTableName.agentId IS '代理id';")
            .append("COMMENT ON COLUMN $sessionTableName.title IS '会话标题';")
            .toString();

    public static String sqlite_createHitlCallTaskTableSQL = new StringBuilder().append("create table $hitlCallTaskTableName (hitlTaskId varchar(100),")  //人工介入任务id
            .append( "traceId varchar(100),")  //trace id
            .append( "agentId varchar(100),")  //智能体id
            .append( "agentName varchar(200),")  //智能体名称
            .append( "parentAgentId varchar(100),")  //父智能体id
            .append( "parentAgentName varchar(200),")  //父智能体名称
            .append( "sessionId varchar(100),")  //会话id
            .append( "requestId varchar(100),")  //请求id
            .append( "userId varchar(100),")  //用户id
            .append( "hitlTaskReason text,")  //人工介入任务内容,LLM生成	
			.append(" hitlTaskData text,")  //人工介入任务内容,人工辅助提供
			.append(" exception text,")  //异常信息
            .append( "hitlTaskStatus int,")  //人工介入任务状态：0 待处理 1 已处理 2 已拒绝 3 超时忽略 5 已结束
            .append( "hitlTaskHandleResult text,")  //人工介入任务处理结果说明
            .append( "hitlTaskCreateTime number(20),")  //人工介入任务创建时间
            .append( "hitlTaskHandleTime number(20),")  //人工介入任务处理时间
            .append( "hitlTaskCompleteTime number(20),")  //人工介入任务完成时间
            .append( "PRIMARY KEY (hitlTaskId))").toString();

    public static final String mysql_createHitlCallTaskTableSQL = new StringBuilder().append("CREATE TABLE $hitlCallTaskTableName ( hitlTaskId varchar(100) NOT NULL comment '人工介入任务id'," )
            .append(" traceId varchar(100) comment 'trace id', " )
            .append(" agentId varchar(100) comment '智能体id', " )
            .append(" agentName varchar(200) comment '智能体名称', " )
            .append(" parentAgentId varchar(100) comment '父智能体id', " )
            .append(" parentAgentName varchar(200) comment '父智能体名称', " )
            .append(" sessionId varchar(100) comment '会话id', " )
            .append(" requestId varchar(100) comment '请求id', " )
            .append(" userId varchar(100) comment '用户id', " )
            .append(" hitlTaskReason LONGTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci comment '人工介入任务内容，LLM生成', " )			
			.append(" hitlTaskData LONGTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci comment '人工介入任务内容，人工辅助提供', ")  //人工介入任务内容,人工辅助提供
			.append(" exception LONGTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci comment '异常信息', ")  //异常信息
            .append(" hitlTaskStatus int comment '人工介入任务状态：0 待处理 1 已处理 2 已拒绝 3 超时忽略 5 已结束', " )
            .append(" hitlTaskHandleResult LONGTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci comment '人工介入任务处理结果说明', " )
            .append(" hitlTaskCreateTime datetime comment '人工介入任务创建时间', " )
            .append(" hitlTaskHandleTime datetime comment '人工介入任务处理时间', " )
            .append(" hitlTaskCompleteTime datetime comment '人工介入任务完成时间', " )
            .append( "PRIMARY KEY(hitlTaskId)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci").toString();

    public static final String oracle_createHitlCallTaskTableSQL = new StringBuilder().append("CREATE TABLE $hitlCallTaskTableName ( hitlTaskId varchar2(100) NOT NULL," )
            .append(" traceId varchar2(100),")
            .append(" agentId varchar2(100),")
            .append(" agentName varchar2(200),")
            .append(" parentAgentId varchar2(100),")
            .append(" parentAgentName varchar2(200),")
            .append(" sessionId varchar2(100),")
            .append(" requestId varchar2(100),")
            .append(" userId varchar2(100),")
            .append(" hitlTaskReason clob,")  //人工介入任务内容,LLM生成
			.append(" hitlTaskData clob,")  //人工介入任务内容,人工辅助提供
			.append(" exception clob, ")  //异常信息
            .append(" hitlTaskStatus int,")
            .append(" hitlTaskHandleResult clob,")
            .append(" hitlTaskCreateTime timestamp,")
            .append(" hitlTaskHandleTime timestamp,")
            .append(" hitlTaskCompleteTime timestamp,")
            .append( "constraint $hitlCallTaskTableName_PK primary key(hitlTaskId))").toString();

    public static final String oracle_addCommentsToHitlCallTaskTableSQL = new StringBuilder()
            .append("COMMENT ON COLUMN $hitlCallTaskTableName.hitlTaskId IS '人工介入任务id';")
            .append("COMMENT ON COLUMN $hitlCallTaskTableName.traceId IS 'trace id';")
            .append("COMMENT ON COLUMN $hitlCallTaskTableName.agentId IS '智能体id';")
            .append("COMMENT ON COLUMN $hitlCallTaskTableName.agentName IS '智能体名称';")
            .append("COMMENT ON COLUMN $hitlCallTaskTableName.parentAgentId IS '父智能体id';")
            .append("COMMENT ON COLUMN $hitlCallTaskTableName.parentAgentName IS '父智能体名称';")
            .append("COMMENT ON COLUMN $hitlCallTaskTableName.sessionId IS '会话id';")
            .append("COMMENT ON COLUMN $hitlCallTaskTableName.requestId IS '请求id';")
            .append("COMMENT ON COLUMN $hitlCallTaskTableName.userId IS '用户id';")
            .append("COMMENT ON COLUMN $hitlCallTaskTableName.hitlTaskReason IS '人工介入任务内容，LLM生成';")  //人工介入任务内容,LLM生成
			.append("COMMENT ON COLUMN $hitlCallTaskTableName.hitlTaskData IS '人工介入任务内容，人工辅助提供';")  //人工介入任务内容,人工辅助提供	
			.append("COMMENT ON COLUMN $hitlCallTaskTableName.exception IS '异常信息';")  //异常信息
            .append("COMMENT ON COLUMN $hitlCallTaskTableName.hitlTaskStatus IS '人工介入任务状态：0 待处理 1 已处理 2 已拒绝 3 超时忽略 5 已结束';")
            .append("COMMENT ON COLUMN $hitlCallTaskTableName.hitlTaskHandleResult IS '人工介入任务处理结果说明';")
            .append("COMMENT ON COLUMN $hitlCallTaskTableName.hitlTaskCreateTime IS '人工介入任务创建时间';")
            .append("COMMENT ON COLUMN $hitlCallTaskTableName.hitlTaskHandleTime IS '人工介入任务处理时间';")
            .append("COMMENT ON COLUMN $hitlCallTaskTableName.hitlTaskCompleteTime IS '人工介入任务完成时间';")
            .toString();

    public static final String dm_createHitlCallTaskTableSQL = new StringBuilder().append("CREATE TABLE $hitlCallTaskTableName ( hitlTaskId varchar2(100) NOT NULL," )
            .append(" traceId varchar2(100),")
            .append(" agentId varchar2(100),")
            .append(" agentName varchar2(200),")
            .append(" parentAgentId varchar2(100),")
            .append(" parentAgentName varchar2(200),")
            .append(" sessionId varchar2(100),")
            .append(" requestId varchar2(100),")
            .append(" userId varchar2(100),")
            .append(" hitlTaskReason clob,")  //人工介入任务内容,LLM生成
			.append(" hitlTaskData clob,")  //人工介入任务内容,人工辅助提供
			.append(" exception clob, ")  //异常信息
            .append(" hitlTaskStatus int,")
            .append(" hitlTaskHandleResult clob,")
            .append(" hitlTaskCreateTime timestamp,")
            .append(" hitlTaskHandleTime timestamp,")
            .append(" hitlTaskCompleteTime timestamp,")
            .append( "constraint $hitlCallTaskTableName_PK primary key(hitlTaskId))").toString();

    public static final String sqlserver_createHitlCallTaskTableSQL = new StringBuilder().append("CREATE TABLE $hitlCallTaskTableName " )
            .append( "( hitlTaskId varchar(100) NOT NULL," ) //人工介入任务id
            .append( "traceId varchar(100),")  //trace id
            .append(" agentId varchar(100),")  //智能体id
            .append(" agentName varchar(200),")  //智能体名称
            .append(" parentAgentId varchar(100),")  //父智能体id
            .append(" parentAgentName varchar(200),")  //父智能体名称
            .append(" sessionId varchar(100),")  //会话id
            .append(" requestId varchar(100),")  //请求id
            .append(" userId varchar(100),")  //用户id
            .append(" hitlTaskReason nvarchar(max),")  //人工介入任务内容,LLM生成
			.append(" hitlTaskData nvarchar(max),")  //人工介入任务内容,人工辅助提供
			.append(" exception nvarchar(max),")  //异常信息
            .append(" hitlTaskStatus int,")  //人工介入任务状态：0 待处理 1 已处理 2 已拒绝 3 超时忽略 5 已结束
            .append(" hitlTaskHandleResult nvarchar(max),")  //人工介入任务处理结果说明
            .append(" hitlTaskCreateTime datetime,")  //人工介入任务创建时间
            .append(" hitlTaskHandleTime datetime,")  //人工介入任务处理时间
            .append(" hitlTaskCompleteTime datetime,")  //人工介入任务完成时间
            .append( "constraint $hitlCallTaskTableName_PK primary key(hitlTaskId))") //主键
            .toString();

    public static final String postgresql_createHitlCallTaskTableSQL = new StringBuilder().append("CREATE TABLE $hitlCallTaskTableName (hitlTaskId varchar(100) NOT NULL," )
            .append( "traceId varchar(100),")  //trace id
            .append(" agentId varchar(100),")  //智能体id
            .append(" agentName varchar(200),")  //智能体名称
            .append(" parentAgentId varchar(100),")  //父智能体id
            .append(" parentAgentName varchar(200),")  //父智能体名称
            .append(" sessionId varchar(100),")  //会话id
            .append(" requestId varchar(100),")  //请求id
            .append(" userId varchar(100),")  //用户id
            .append(" hitlTaskReason text,")  //人工介入任务内容,LLM生成	
			.append(" hitlTaskData text,")  //人工介入任务内容,人工辅助生成
			.append(" exception text,")  //异常信息
            .append(" hitlTaskStatus int,")  //人工介入任务状态：0 待处理 1 已处理 2 已拒绝 3 超时忽略 5 已结束
            .append(" hitlTaskHandleResult text,")  //人工介入任务处理结果说明
            .append(" hitlTaskCreateTime timestamp,")  //人工介入任务创建时间
            .append(" hitlTaskHandleTime timestamp,")  //人工介入任务处理时间
            .append(" hitlTaskCompleteTime timestamp,")  //人工介入任务完成时间
            .append( "primary key(hitlTaskId))")//主键
            .toString();

    public static final String postgresql_addCommentsToHitlCallTaskTableSQL = new StringBuilder()
            .append("COMMENT ON COLUMN $hitlCallTaskTableName.hitlTaskId IS '人工介入任务id';")
            .append("COMMENT ON COLUMN $hitlCallTaskTableName.traceId IS 'trace id';")
            .append("COMMENT ON COLUMN $hitlCallTaskTableName.agentId IS '智能体id';")
            .append("COMMENT ON COLUMN $hitlCallTaskTableName.agentName IS '智能体名称';")
            .append("COMMENT ON COLUMN $hitlCallTaskTableName.parentAgentId IS '父智能体id';")
            .append("COMMENT ON COLUMN $hitlCallTaskTableName.parentAgentName IS '父智能体名称';")
            .append("COMMENT ON COLUMN $hitlCallTaskTableName.sessionId IS '会话id';")
            .append("COMMENT ON COLUMN $hitlCallTaskTableName.requestId IS '请求id';")
            .append("COMMENT ON COLUMN $hitlCallTaskTableName.userId IS '用户id';")
            .append("COMMENT ON COLUMN $hitlCallTaskTableName.hitlTaskReason IS '人工介入任务内容,LLM生成';")  //人工介入任务内容,LLM生成
			.append("COMMENT ON COLUMN $hitlCallTaskTableName.hitlTaskData IS '人工介入任务内容,人工辅助提供';")  //人工介入任务内容,人工辅助提供
			.append("COMMENT ON COLUMN $hitlCallTaskTableName.exception IS '异常信息';")  //异常信息
            .append("COMMENT ON COLUMN $hitlCallTaskTableName.hitlTaskStatus IS '人工介入任务状态：0 待处理 1 已处理 2 已拒绝 3 超时忽略 5 已结束';")
            .append("COMMENT ON COLUMN $hitlCallTaskTableName.hitlTaskHandleResult IS '人工介入任务处理结果说明';")
            .append("COMMENT ON COLUMN $hitlCallTaskTableName.hitlTaskCreateTime IS '人工介入任务创建时间';")
            .append("COMMENT ON COLUMN $hitlCallTaskTableName.hitlTaskHandleTime IS '人工介入任务处理时间';")
            .append("COMMENT ON COLUMN $hitlCallTaskTableName.hitlTaskCompleteTime IS '人工介入任务完成时间';")
            .toString();
	
	public static final String clickhouse_createLocalSessionTableSQL = new StringBuilder().append("CREATE TABLE ${sessionTableName}_local  ON CLUSTER $clickhouseCluster ")
			.append(" (")
                .append("sessionId String  COMMENT '会话id',  ")  
                 .append("createTime DateTime COMMENT '创建时间',")
                 .append("lastAccessTime DateTime COMMENT '最后访问时间',")
                 .append("userId Nullable(String) COMMENT '用户id',")
                 .append("agentId Nullable(String) COMMENT '代理id',")
                 .append("title Nullable(String) COMMENT '会话标题',")   
                 .append("domain Nullable(String) COMMENT '会话所属领域'         ")
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
			.append( "groupId varchar(100),")  //消息分组id，隶属于并行分支中时，该字段有值
			.append( "parentGroupId varchar(100),")  //消息父分组id，分组所在的并行分支隶属于父并行分支中时，该字段有值，为父并行节点的分组id

            .append( "tokenMetrics text, " )  //token消耗统计
            .append( "elapsed INTEGER,")  //耗时
            .append( "role varchar(100),")
            .append( "marks varchar(500),")
            .append( "metadata text,")
			.append( "name varchar(200), " )  //消息名称
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
			.append( "groupId varchar(100),")  //消息分组id，隶属于并行分支中时，该字段有值
			.append( "parentGroupId varchar(100),")  //消息父分组id，分组所在的并行分支隶属于父并行分支中时，该字段有值，为父并行节点的分组id
            .append( "role varchar(100) NOT NULL, " )
            .append( "tokenMetrics LONGTEXT  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci, " )  //token消耗统计
            .append( "elapsed BIGINT,")  //耗时
            .append( "marks varchar(500),")
            .append( "metadata  LONGTEXT  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,")
			.append( "name varchar(200), " )  //消息名称
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
			.append( "groupId varchar2(100),")  //消息分组id，隶属于并行分支中时，该字段有值
			.append( "parentGroupId varchar2(100),")  //消息父分组id，分组所在的并行分支隶属于父并行分支中时，该字段有值，为父并行节点的分组id
            .append( "seqNo int NOT NULL, " )  //消息序号
            .append( "message clob NOT NULL, " )  //消息正文
            .append( "tokenMetrics clob , " )  //token消耗统计
            .append( "elapsed NUMBER(19,0),")  //耗时
            
            .append( "role varchar2(100) NOT NULL, " )

            .append( "marks varchar2(500),")
            .append( "metadata clob,")
			.append( "name varchar2(200), " )  //消息名称
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
			.append( "groupId varchar2(100),")  //消息分组id，隶属于并行分支中时，该字段有值
			.append( "parentGroupId varchar2(100),")  //消息父分组id，分组所在的并行分支隶属于父并行分支中时，该字段有值，为父并行节点的分组id
            .append( "tokenMetrics clob , " )  //token消耗统计
            .append( "elapsed NUMBER(19,0),")  //耗时
            .append( "role varchar2(100) NOT NULL, " )

            .append( "marks varchar2(500),")
            .append( "metadata clob,")
			.append( "name varchar2(200), " )  //消息名称
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
			.append( "groupId varchar(100),")  //消息分组id，隶属于并行分支中时，该字段有值
			.append( "parentGroupId varchar(100),")  //消息父分组id，分组所在的并行分支隶属于父并行分支中时，该字段有值，为父并行节点的分组id
            .append( "tokenMetrics nvarchar(max),")  //token消耗统计
            .append( "elapsed BIGINT,")  //耗时
            .append( "role varchar(100) NOT NULL,")

            .append( "marks varchar(500),")
            .append( "metadata nvarchar(max),")
			.append( "name nvarchar(200), " )  //消息名称
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
			.append( "groupId varchar(100),")  //消息分组id，隶属于并行分支中时，该字段有值
			.append( "parentGroupId varchar(100),")  //消息父分组id，分组所在的并行分支隶属于父并行分支中时，该字段有值，为父并行节点的分组id
            .append( "tokenMetrics text,")  //token消耗统计
            .append( "elapsed BIGINT,")  //耗时
            .append( "role varchar(100) NOT NULL,")//消息角色名称
            .append( "marks varchar(500),") //消息标记，冗余备用字段，暂未使用
            .append( "metadata text,") //消息元数据
			.append( "name varchar(200), " )  //消息名称
            .append( "primary key(msgId))").toString();
	
	public static final String clickhouse_createLocalSessionMessageTableSQL = new StringBuilder().append("CREATE TABLE ${sessionMessageTableName}_local  ON CLUSTER $clickhouseCluster ")
			.append("(")
			.append("msgId String  COMMENT '消息id',")
			.append("createTime DateTime COMMENT '创建时间',")
			.append("sessionId String COMMENT '会话id',")
			.append("requestId Nullable(String) COMMENT '请求id',")
			.append("traceId Nullable(String) COMMENT 'trace id',")
			.append("parentAgentId Nullable(String) COMMENT '父智能体id',")
			.append("agentId Nullable(String) COMMENT '创建或者消息所属的agentid,如果节点类型是串行容器智能体节点（sequence）、并行容器智能体节点（parallel），对应创建消息的agentid为subAgentIdBy对应的值',")
			.append("agentNodeType Nullable(String) COMMENT '智能体节点类型：标准化智能体节点（standard）、串行容器智能体节点（sequence）、并行容器智能体节点（parallel）',")
			.append("subAgentIdBy Nullable(String) COMMENT '创建消息的子agentid，节点类型是串行容器智能体节点（sequence）、并行容器智能体节点（parallel）有值',")
			.append("messageType Nullable(String) COMMENT '0 代表子智能体辅助消息， 1 代表子智能体输出结果 2 代表用户输入消息 3 智能体系统消息 5 智能体跟踪消息 是否是agent的最终结果消息（messageType=1），需要加载到父agent的记忆消息中',")
			.append("seqNo Int32 COMMENT '消息序号',")
			.append("message Nullable(String) COMMENT '消息正文',")
			.append( "groupId Nullable(String) COMMENT '消息分组id，隶属于并行分支中时，该字段有值',")  //消息分组id，隶属于并行分支中时，该字段有值
			.append( "parentGroupId Nullable(String) COMMENT '消息父分组id，分组所在的并行分支隶属于父并行分支中时，该字段有值，为父并行节点的分组id',")  //消息父分组id，分组所在的并行分支隶属于父并行分支中时，该字段有值，为父并行节点的分组id
			.append("tokenMetrics Nullable(String) COMMENT 'token消耗统计',")
			.append("elapsed Int32 COMMENT '耗时',")
			.append("role Nullable(String) COMMENT '角色',")
			.append("marks Nullable(String) COMMENT '消息标记',")
			.append("metadata Nullable(String) COMMENT '消息元数据',")
			.append( "name Nullable(String) COMMENT '消息名称'" )  //消息名称
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
			.append("requestId Nullable(String) COMMENT '请求id'")
			.append(")")
			.append("ENGINE = ReplicatedMergeTree('/clickhouse/tables/{shard}/{database}/{table}', '{replica}')")
			.append("ORDER BY (sessionId)").toString();
	
	public static final String clickhouse_createClusterSessionMessageReferenceTableSQL = new StringBuilder().append("CREATE TABLE ${sessionMessageReferenceTableName} on cluster $clickhouseCluster AS ${sessionMessageReferenceTableName}_local ENGINE = Distributed($clickhouseCluster, currentDatabase(), ${sessionMessageReferenceTableName}_local, sipHash64(sessionId))").toString();
	
	// ============ SQLite ============
	public static String sqlite_createMemoryTableSQL = new StringBuilder()
			.append("create table $memoryTableName (")
			.append("memoryId varchar(100) PRIMARY KEY,")          // 记录id，主键
			.append("agentId varchar(100) not null,")              // 智能体id
			.append("parentAgentId varchar(100),")                 // 父智能体id
			.append("userId varchar(100) not null,")               // 用户id
			.append("sessionId varchar(100) not null,")            // 会话id
			.append("content text,")                               // 记忆内容
			.append("memoryDay varchar(20),")                      // 记忆时间 yyyy-MM-dd
			.append("memoryType varchar(20) default 'day')")      // 记忆类型 day/longterm
			.toString();
	
	
	// ============ MySQL ============
	public static final String mysql_createMemoryTableSQL = new StringBuilder()
			.append("CREATE TABLE $memoryTableName (")
			.append("memoryId varchar(100) NOT NULL COMMENT '记录id，主键',")
			.append("agentId varchar(100) NOT NULL COMMENT '智能体id',")
			.append("parentAgentId varchar(100) COMMENT '父智能体id',")
			.append("userId varchar(100) NOT NULL COMMENT '用户id',")
			.append("sessionId varchar(100) NOT NULL COMMENT '会话id',")
			.append("content text COMMENT '记忆内容',")
			.append("memoryDay varchar(20) COMMENT '记忆时间 yyyy-MM-dd',")
			.append("memoryType varchar(20) DEFAULT 'day' COMMENT '记忆类型 day/longterm',")
			.append("PRIMARY KEY(memoryId),")
			.append("KEY idx_agent_user (agentId, userId),")
			.append("KEY idx_session (sessionId),")
			.append("KEY idx_memory_day (memoryDay)")
			.append(") COMMENT='智能体记忆表' ENGINE=InnoDB DEFAULT CHARSET=utf8mb4")
			.toString();
	
	
	// ============ Oracle ============
	public static final String oracle_createMemoryTableSQL = new StringBuilder()
			.append("CREATE TABLE $memoryTableName (")
			.append("memoryId varchar2(100) NOT NULL,")            // 记录id，主键
			.append("agentId varchar2(100) NOT NULL,")             // 智能体id
			.append("parentAgentId varchar2(100),")                // 父智能体id
			.append("userId varchar2(100) NOT NULL,")              // 用户id
			.append("sessionId varchar2(100) NOT NULL,")           // 会话id
			.append("content clob,")                               // 记忆内容
			.append("memoryDay varchar2(20),")                     // 记忆时间 yyyy-MM-dd
			.append("memoryType varchar2(20) DEFAULT 'day',")      // 记忆类型 day/longterm
			.append("CONSTRAINT $memoryTableName_PK PRIMARY KEY(memoryId))")
			.toString();
	
	public static final String oracle_addCommentsToMemoryTableSQL = new StringBuilder()
			.append("COMMENT ON COLUMN $memoryTableName.memoryId IS '记录id，主键';")
			.append("COMMENT ON COLUMN $memoryTableName.agentId IS '智能体id';")
			.append("COMMENT ON COLUMN $memoryTableName.parentAgentId IS '父智能体id';")
			.append("COMMENT ON COLUMN $memoryTableName.userId IS '用户id';")
			.append("COMMENT ON COLUMN $memoryTableName.sessionId IS '会话id';")
			.append("COMMENT ON COLUMN $memoryTableName.content IS '记忆内容';")
			.append("COMMENT ON COLUMN $memoryTableName.memoryDay IS '记忆时间 yyyy-MM-dd';")
			.append("COMMENT ON COLUMN $memoryTableName.memoryType IS '记忆类型 day/longterm';")
			.toString();
	
	
	// ============ 达梦数据库 (DM) ============
	public static final String dm_createMemoryTableSQL = new StringBuilder()
			.append("CREATE TABLE $memoryTableName (")
			.append("memoryId varchar2(100) NOT NULL,")            // 记录id，主键
			.append("agentId varchar2(100) NOT NULL,")             // 智能体id
			.append("parentAgentId varchar2(100),")                // 父智能体id
			.append("userId varchar2(100) NOT NULL,")              // 用户id
			.append("sessionId varchar2(100) NOT NULL,")           // 会话id
			.append("content text,")                               // 记忆内容
			.append("memoryDay varchar2(20),")                     // 记忆时间 yyyy-MM-dd
			.append("memoryType varchar2(20) DEFAULT 'day',")      // 记忆类型 day/longterm
			.append("CONSTRAINT $memoryTableName_PK PRIMARY KEY(memoryId))")
			.toString();
	
	
	// ============ SQL Server ============
	public static final String sqlserver_createMemoryTableSQL = new StringBuilder()
			.append("CREATE TABLE $memoryTableName (")
			.append("memoryId varchar(100) NOT NULL,")             // 记录id，主键
			.append("agentId varchar(100) NOT NULL,")              // 智能体id
			.append("parentAgentId varchar(100),")                 // 父智能体id
			.append("userId varchar(100) NOT NULL,")               // 用户id
			.append("sessionId varchar(100) NOT NULL,")            // 会话id
			.append("content nvarchar(max),")                      // 记忆内容
			.append("memoryDay varchar(20),")                      // 记忆时间 yyyy-MM-dd
			.append("memoryType varchar(20) DEFAULT 'day',")       // 记忆类型 day/longterm
			.append("CONSTRAINT $memoryTableName_PK PRIMARY KEY(memoryId))")
			.toString();
	
	
	// ============ PostgreSQL ============
	public static final String postgresql_createMemoryTableSQL = new StringBuilder()
			.append("CREATE TABLE $memoryTableName (")
			.append("memoryId varchar(100) NOT NULL,")             // 记录id，主键
			.append("agentId varchar(100) NOT NULL,")              // 智能体id
			.append("parentAgentId varchar(100),")                 // 父智能体id
			.append("userId varchar(100) NOT NULL,")               // 用户id
			.append("sessionId varchar(100) NOT NULL,")            // 会话id
			.append("content text,")                               // 记忆内容
			.append("memoryDay varchar(20),")                      // 记忆时间 yyyy-MM-dd
			.append("memoryType varchar(20) DEFAULT 'day',")       // 记忆类型 day/longterm
			.append("PRIMARY KEY(memoryId))")
			.toString();
	
	public static final String postgresql_addCommentsToMemoryTableSQL = new StringBuilder()
			.append("COMMENT ON COLUMN $memoryTableName.memoryId IS '记录id，主键';")
			.append("COMMENT ON COLUMN $memoryTableName.agentId IS '智能体id';")
			.append("COMMENT ON COLUMN $memoryTableName.parentAgentId IS '父智能体id';")
			.append("COMMENT ON COLUMN $memoryTableName.userId IS '用户id';")
			.append("COMMENT ON COLUMN $memoryTableName.sessionId IS '会话id';")
			.append("COMMENT ON COLUMN $memoryTableName.content IS '记忆内容';")
			.append("COMMENT ON COLUMN $memoryTableName.memoryDay IS '记忆时间 yyyy-MM-dd';")
			.append("COMMENT ON COLUMN $memoryTableName.memoryType IS '记忆类型 day/longterm';")
			.toString();
	
	
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
	
	private String existHitlCallTaskSQL;
	
	private String existMemorySQL;

    private String insertHitlCallTaskSQL;

    private String handledHitlCallTaskSQL;
	private String refusedHitlCallTaskSQL;
    private String completeHitlCallTaskSQL;
	private String timeoutHitlCallTaskSQL;

    private String deleteCompleteHitlCallTaskSQLWithCompleteTimeSQL;
	
	public String getInsertSessionMessageRerenceSQL() {
        return insertSessionMessageRerenceSQL;
    }

    private String insertSessionMessageRerenceSQL;




    private String deleteSessionMessageRerenceBySessionIdSQL;
 
    /**
     * 会话基本信息存储表名称
     */
    private String sessionTableName = "agent_session";
	
	/**
	 * 智能体记忆存储表名称:记录智能体流水账和记忆摘要
	 */
	private String memoryTableName = "agent_memory";

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

    /**
     * 人工介入任务表名称
     */
    private String hitlCallTaskTableName = "agent_hitl_calltask";

 

    public void setSessionTableName(String sessionTableName) {
        this.sessionTableName = sessionTableName;
    }

    public String getSessionTableName() {
        return sessionTableName;
    }
    public String getDeleteSessionMessageRerenceBySessionIdSQL() {
        return deleteSessionMessageRerenceBySessionIdSQL;
    }
	private volatile boolean inited;
	private Object lock = new Object();
	public void initTable(String clickhouseCluster, String hitlDatasource,String dataSource){
		try {
			SQLExecutor.queryObjectWithDBName(int.class, dataSource, getExistSQL());
		}
		catch (Exception exception){
			try {
				logger.info("Creating session table {}...", getSessionTableName());
				
				if(!isClickhouse(dataSource)) {
					SQLExecutor.updateWithDBName(dataSource,evalCreateSessionTableSQL(dataSource));
				}
				else{
					SQLExecutor.updateWithDBName(dataSource, evalCreateClickhouseLocalSessionTableSQL(clickhouseCluster));
					SQLExecutor.updateWithDBName(dataSource, evalCreateClusterSessionTableSQL(clickhouseCluster));
				}
			} catch (SQLException e) {
				throw new AIRuntimeException("Failed to create session table", e);
			}
		}
		
		try {
			SQLExecutor.queryObjectWithDBName(int.class, dataSource, getExistMessageSQL());
		}
		catch (Exception exception){
			try {
				logger.info("Creating session message table {}...", getSessionMessageTableName());
				
				if(!isClickhouse(dataSource)) {
					SQLExecutor.updateWithDBName(dataSource,evalCreateSessionMessageTableSQL(dataSource));
				}
				else{
					SQLExecutor.updateWithDBName(dataSource, evalCreateClickhouseLocalSessionMessageTableSQL(clickhouseCluster));
					SQLExecutor.updateWithDBName(dataSource, evalCreateClusterSessionMessageTableSQL(clickhouseCluster));
				}
			} catch (SQLException e) {
				throw new AIRuntimeException("Failed to create session message table", e);
			}
		}
		//创建记忆表，暂时屏蔽，后续放开
		/** 
		try {
			SQLExecutor.queryObjectWithDBName(int.class, dataSource, getExistMemorySQL());
		}
		catch (Exception exception){
			try {
				logger.info("Creating memory table {}...", getMemoryTableName());
				
				if(!isClickhouse(dataSource)) {
					SQLExecutor.updateWithDBName(dataSource,evalCreateAgentMemoryTableSQL(dataSource));
				}
				else{
					//暂时不支持Clickhouse保存记忆和流水账数据
//					SQLExecutor.updateWithDBName(dataSource, evalCreateClickhouseLocalSessionMessageTableSQL(clickhouseCluster));
//					SQLExecutor.updateWithDBName(dataSource, evalCreateClusterSessionMessageTableSQL(clickhouseCluster));
				}
			} catch (SQLException e) {
				throw new AIRuntimeException("Failed to create session message table", e);
			}
		}
		 */
		
		try {
			SQLExecutor.queryObjectWithDBName(int.class, dataSource, getExistMessageReferenceSQL());
		}
		catch (Exception exception){
			try {
				logger.info("Creating session message reference table {}...", getSessionMessageReferenceTableName());
				if(!isClickhouse(dataSource)) {
					SQLExecutor.updateWithDBName(dataSource, evalCreateSessionMessageReferenceTableSQL(dataSource));
				}
				else{
					SQLExecutor.updateWithDBName(dataSource, evalCreateClickhouseLocalSessionMessageReferenceTableSQL(clickhouseCluster));
					SQLExecutor.updateWithDBName(dataSource, evalCreateClusterSessionMessageReferenceTableSQL(clickhouseCluster));
				}
			} catch (SQLException e) {
				throw new AIRuntimeException("Failed to create session message reference table "+getSessionMessageReferenceTableName(), e);
			}
		}
		String sqlhitl = evalCreateHitlCallTaskTableSQL(hitlDatasource);
		if(sqlhitl != null) {
			try {
				SQLExecutor.queryObjectWithDBName(int.class, hitlDatasource, getExistHitlCallTaskSQL());
			} catch (Exception exception) {
				try {
					logger.info("Creating HitlCallTaskTable table {}...", getHitlCallTaskTableName());
					
					SQLExecutor.updateWithDBName(hitlDatasource, evalCreateHitlCallTaskTableSQL(hitlDatasource));
					
				} catch (SQLException e) {
					throw new AIRuntimeException("Failed to create HitlCallTaskTable table", e);
				}
			}
		}
	
	}
    public void init(String clickhouseCluster, String hitlDatasource,String dataSource){
		if(inited )
			return;
		synchronized (lock) {
			if (inited)
				return;
			existSQL = new StringBuilder().append("select 1 from ").append(sessionTableName).toString();
			existMessageSQL = new StringBuilder().append("select 1 from ").append(sessionMessageTableName).toString();
			existMemorySQL = new StringBuilder().append("select 1 from ").append(memoryTableName).toString();	
			existMessageReferenceSQL = new StringBuilder().append("select 1 from ").append(sessionMessageReferenceTableName).toString();
			existHitlCallTaskSQL = new StringBuilder().append("select 1 from ").append(hitlCallTaskTableName).toString();
			
			insertHitlCallTaskSQL = new StringBuilder().append("insert into ").append(hitlCallTaskTableName)
					.append(" (hitlTaskId,traceId,agentId,agentName,parentAgentId,parentAgentName,")
					.append("sessionId,requestId,userId,hitlTaskReason,hitlTaskStatus,hitlTaskHandleResult,")
					.append("hitlTaskCreateTime,hitlTaskHandleTime,hitlTaskCompleteTime")
					.append(") values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)").toString();
			
			//处理人工介入任务：更新状态为已处理(1)或已拒绝(2)，记录处理结果和处理时间
			handledHitlCallTaskSQL = new StringBuilder().append("update ").append(hitlCallTaskTableName)
					.append(" set hitlTaskStatus=1,hitlTaskReason=?,hitlTaskHandleTime=?")
					.append(" where hitlTaskId=?").toString();
			
			refusedHitlCallTaskSQL = new StringBuilder().append("update ").append(hitlCallTaskTableName)
					.append(" set hitlTaskStatus=2,hitlTaskReason=?,hitlTaskHandleTime=?")
					.append(" where hitlTaskId=?").toString();
			
			//完成人工介入任务：更新状态为已结束(5)或者已超时(3)，记录完成时间   ：hitlTaskStatus 0 待处理 1 已处理 2 已拒绝 3 超时忽略 5 已结束
			completeHitlCallTaskSQL = new StringBuilder().append("update ").append(hitlCallTaskTableName)
					.append(" set hitlTaskStatus=5,hitlTaskHandleResult=?,hitlTaskCompleteTime=?")
					.append(" where hitlTaskId=?").toString();
			
			timeoutHitlCallTaskSQL = new StringBuilder().append("update ").append(hitlCallTaskTableName)
					.append(" set hitlTaskStatus=3,hitlTaskHandleResult=?,hitlTaskCompleteTime=?")
					.append(" where hitlTaskId=?").toString();
			
			//根据完成时间清理已结束(5)的人工介入任务
			deleteCompleteHitlCallTaskSQLWithCompleteTimeSQL = new StringBuilder().append("delete from ")
					.append(hitlCallTaskTableName)
					.append(" where hitlTaskStatus=5 and hitlTaskCompleteTime<?").toString();
			
			insertSessionSQL = "INSERT INTO " + sessionTableName + " (sessionId, createTime, lastAccessTime,userId, agentId, title,domain) \n" +
					"VALUES (?, ?, ?, ?, ?,?,?)";
			updateSessionLastAccessTimeSQL = "UPDATE " + sessionTableName + " SET lastAccessTime = ? WHERE sessionId = ?";
			
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
					.append(",agentNodeType,subAgentIdBy,groupId,parentGroupId")
					.append(") values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)").toString();
			
			insertSessionMessageRerenceSQL = "INSERT INTO " + sessionMessageReferenceTableName + " (msgId,msgAgentId,refAgentId,sessionId,requestId) " +
					"VALUES (?, ?, ?, ?, ?)";
			deleteSessionMessageSQL = "DELETE FROM " + sessionMessageTableName + " where msgId=? and jobType=?";
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
					.append("and messageType in ('0','1','2','3','4'))")
					.toString();
			
			selectSessionMessageBySessionId2ndAgentIdSQL1 = new StringBuilder()
					.append(" order  by createTime, seqNo asc").toString();
			
			selectAgentSessionMessageReferenceIdsBySessionIdSQL = new StringBuilder()
					.append("select msgId from ")
					.append(sessionMessageReferenceTableName)
					.append(" where sessionId=? and refAgentId = ?")
					.toString();
			initTable(  clickhouseCluster,   hitlDatasource,  dataSource);
			inited = true;
		}
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
	
	public String evalCreateAgentMemoryTableSQL(String dbName) {
		DB adaptor  = DBUtil.getDBAdapter(dbName);
		String type = adaptor.getDBTYPE();
		String sql = null;
		if ("mysql".equalsIgnoreCase(type)) {
			sql = mysql_createMemoryTableSQL;
		} else if ("oracle".equalsIgnoreCase(type)) {
			sql = oracle_createMemoryTableSQL;
		} else if ("dm".equalsIgnoreCase(type)) {
			sql = dm_createMemoryTableSQL;
		} else if ("sqlserver".equalsIgnoreCase(type)) {
			sql = sqlserver_createMemoryTableSQL;
		} else if ("postgresql".equalsIgnoreCase(type)) {
			sql = postgresql_createMemoryTableSQL;
		}
		else if("sqlite".equalsIgnoreCase(type)) {
			sql = sqlite_createMemoryTableSQL;
		}
		
		return sql.replace("$memoryTableName", memoryTableName);
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

    public String getHitlCallTaskTableName() {
        return hitlCallTaskTableName;
    }

    public void setHitlCallTaskTableName(String hitlCallTaskTableName) {
        this.hitlCallTaskTableName = hitlCallTaskTableName;
    }

    public String evalCreateHitlCallTaskTableSQL(String dbName) {
        DB adaptor  = DBUtil.getDBAdapter(dbName);
        String type = adaptor.getDBTYPE();
        String sql = null;
        if ("mysql".equalsIgnoreCase(type)) {
            sql = mysql_createHitlCallTaskTableSQL;
        } else if ("oracle".equalsIgnoreCase(type)) {
            sql = oracle_createHitlCallTaskTableSQL;
        } else if ("dm".equalsIgnoreCase(type)) {
            sql = dm_createHitlCallTaskTableSQL;
        } else if ("sqlserver".equalsIgnoreCase(type)) {
            sql = sqlserver_createHitlCallTaskTableSQL;
        } else if ("postgresql".equalsIgnoreCase(type)) {
            sql = postgresql_createHitlCallTaskTableSQL;
        }
        else if("sqlite".equalsIgnoreCase(type)) {
            sql = sqlite_createHitlCallTaskTableSQL;
        }
		if(sql == null)
			return null;
        return sql.replace("$hitlCallTaskTableName", hitlCallTaskTableName);
    }

    public String evalOracleAddCommentsToHitlCallTaskTableSQL() {
        return oracle_addCommentsToHitlCallTaskTableSQL.replace("$hitlCallTaskTableName", hitlCallTaskTableName);
    }

    public String evalPostgresqlAddCommentsToHitlCallTaskTableSQL() {
        return postgresql_addCommentsToHitlCallTaskTableSQL.replace("$hitlCallTaskTableName", hitlCallTaskTableName);
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
	
	public String getExistMemorySQL() {
		return existMemorySQL;
	}
	
	public String getMemoryTableName() {
		return memoryTableName;
	}
	
	public void setSessionMessageTableName(String sessionMessageTableName) {
        this.sessionMessageTableName = sessionMessageTableName;
    }

    public String getExistMessageReferenceSQL() {
        return existMessageReferenceSQL;
    }
	
	public String getExistHitlCallTaskSQL() {
		return existHitlCallTaskSQL;
	}

	public String getInsertHitlCallTaskSQL() {
		return insertHitlCallTaskSQL;
	}

	public String getHandledHitlCallTaskSQL() {
		return handledHitlCallTaskSQL;
	}

	public String getCompleteHitlCallTaskSQL() {
		return completeHitlCallTaskSQL;
	}

	public String getDeleteCompleteHitlCallTaskSQLWithCompleteTimeSQL() {
		return deleteCompleteHitlCallTaskSQLWithCompleteTimeSQL;
	}
	
	public String getSelectAgentSessionMessageReferenceIdsBySessionIdSQL() {
		return selectAgentSessionMessageReferenceIdsBySessionIdSQL;
	}
	
	public String getRefusedHitlCallTaskSQL() {
		return refusedHitlCallTaskSQL;
	}
	
	public String getTimeoutHitlCallTaskSQL() {
		return timeoutHitlCallTaskSQL;
	}
}
