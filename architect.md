# bboss-ai 项目架构分析

## 一、项目概述

**bboss-ai** 是一个轻量级 Java AI Agent 多模态智能体开发框架，基于 Apache HttpClient5、HttpCore5 以及 Project Reactor 构建。该项目提供了对大语言模型（LLM）和多模态模型的统一对接能力，支持同步调用和流式调用两种模式，并内置智能体工作流编排、会话管理、工具搜索等企业级特性。能够快速集成各大主流 AI 模型平台，实现智能问答、图片识别/生成、语音识别/生成、视频识别/生成等功能

![image-20260302140629787](/architect.png)

> **ClickHouse 生产级会话存储**：bboss-ai 支持基于 ClickHouse 分布式集群的生产级会话持久化能力。使用时需要指定 ClickHouse 集群名称，并为每个集群节点定义名为 `shard` 和 `replica` 的两个宏变量。ClickHouse 模式下会话续问续答时不会更新最后访问时间（受限于 ClickHouse 不支持高频 UPDATE）。详细使用方式参见 [3.2.6 会话管理](#326-会话管理session-store) 章节。

**核心功能**
- 智能问答（Chat Completion）
- 图片识别与生成（Vision/Image Generation）
- 语音识别与合成（Speech-to-Text/Text-to-Speech）
- 视频识别与生成（Video Understanding/Generation）
- 向量嵌入（Embedding）
- 重排序（Rerank）
- 工具调用（Function Calling），支持 `@Tool`/`@ToolParam` 注解快速发布工具服务和 MCP 服务
- MCP（Model Context Protocol）服务发现和调用，支持 SSE 和 Streamable HTTP 两种传输模式，同时支持客户端和服务端，支持飞书 MCP 集成和 Spring AI MCP 兼容
- 智能体工作流编排，支持串行、并行、条件分支、路由、判断、关键词路由等节点类型
- Skills 技能模块，通过 `SKILL.md` + Front Matter 定义，由 `SkillUtils` 加载
- 工具搜索（Tool Searcher），支持基于关键词的工具过滤，减少上下文占用
- 会话管理，支持内存存储和数据库持久化（MySQL、Oracle、达梦 DM、SQL Server、PostgreSQL、SQLite、ClickHouse）
- 多轮工具调用（Loop Tool Call），支持智能体自主决策多步骤任务执行
- 智能体全链路 Trace 可观测性，覆盖 LLM 调用、工具执行、工作流编排
- 内置工具体系：Shell 执行、代码执行（Java/Python/JavaScript）、文件操作、系统信息查询、文本搜索（Grep）、人工介入
- 工具审计系统（Auditor），支持工具调用前审计拦截，适用于敏感操作审批
- 支持人工介入Hitl（Human-in-the-Loop）功能，即在智能体执行过程中，用户可以介入并参与到任务执行中，人工反馈数据和智能体之间数据交互机制：
  - 智能体采用单节点单进程模式部署时，用户和智能体检通过内存共享数据，进行智能体中断和唤醒处理；
  - 智能体采用集群模式部署时，用户和智能体之间数据交互通过redis发布/订阅模式实现数据共享，进行智能体中断和唤醒处理
  - 如果接收人工提交数据的节点就是中断智能体所在的节点时，无需通过redis发布/订阅模式进行数据交互，直接通过内存共享数据即可
  - 采用内置人工介入工具HitlTaskcallTool，实现Hitl功能，亦可以自定义人工介入工具（继承 BaseHitlTaskTool），实现自定义Hitl功能
  - 支持 HitlAssistant 接口，提供人工干预辅助信息和处理人工提交数据
- 定时调度执行能力

### 支持的平台
- DeepSeek
- Kimi（Moonshot）
- 智谱 AI（Zhipu）
- 阿里百炼/通义千问（Qwen）
- 字节豆包/火山引擎（Doubao）
- 百度（Baidu）
- 硅基流动（Siliconflow）
- 九天（Jiutian）
- MiniMax
- 腾讯混元（TencentHY）
- Xinference
- OpenAI 兼容接口

---

## 二、项目结构

```
bboss-ai/
├── bboss-ai-model/          # 模型定义模块（基础模型和接口）
│   └── src/main/java/org/frameworkset/spi/ai/
│       ├── model/           # 核心模型定义（含 annotation/ 注解包）
│       ├── mcp/model/       # MCP 协议模型
│       └── tools/           # 工具注册接口
│
├── bboss-ai/                # 核心实现模块
│   └── src/main/java/org/frameworkset/spi/
│       ├── ai/
│       │   ├── AIAgent.java           # 智能体主入口类
│       │   ├── UserAgent.java         # 用户代理类
│       │   ├── adapter/               # 平台适配器
│       │   ├── audit/                 # 工具审计系统（Auditor、AuditContext、AuditResult）
│       │   ├── callback/              # 回调接口（ChatContext、ChatCallback、AgentOutput）
│       │   ├── hitl/                  # 人工介入功能（BaseHitlTaskTool、HitlTaskHelper、cluster/）
│       │   ├── material/              # 素材处理（文件下载等）
│       │   ├── mcp/                   # MCP 客户端/服务端（sse/、streamable/、feishu/、intercepter/）
│       │   ├── model/                 # 消息模型
│       │   ├── skill/                 # 技能加载与管理（SkillUtils、SkillsToolRegist、Skill）
│       │   ├── store/                 # 会话存储（内存 + DB）
│       │   ├── tool/                  # 工具注册与搜索（含 permission/）
│       │   ├── tools/                 # 内置工具实现
│       │   └── util/                  # 工具类
│       └── reactor/                   # Reactor 流式处理组件
│
├── bboss-ai-flow/           # 智能体工作流编排模块
│   └── src/main/java/org/frameworkset/spi/ai/
│       ├── flow/            # 工作流节点定义（串行/并行/路由/判断/关键词路由/规划等）
│       ├── prompt/          # Prompt 资源管理与变量解析
│       └── util/            # 流程工具类
│
├── build.gradle             # 根项目构建配置
├── settings.gradle          # 项目模块配置
└── gradle.properties        # Gradle 属性配置
```

---

## 三、模块详解

### 3.1 bboss-ai-model 模块

该模块定义了所有与 AI 交互的基础模型和接口，作为最小依赖被外部使用。

#### 核心类

| 类名 | 作用 |
|------|------|
| `ServerEvent` | 流式调用数据报文封装，包含数据类型、内容、工具调用等信息 |
| `FunctionTool` | 函数工具定义，用于工具调用场景 |
| `FunctionToolDefine` | 函数工具定义描述，包含名称、描述、参数等 |
| `FunctionCall` | 函数调用接口，定义工具执行方法 |
| `MultimodalGeneration` | 多模态生成基础类 |
| `ToolsRegist` | 工具注册接口，支持动态加载模型工具 |

#### MCP 模型

| 类名 | 作用 |
|------|------|
| `McpListToolRequest/Response` | MCP 工具列表请求/响应 |
| `McpToolRequest/Response` | MCP 工具调用请求/响应 |
| `McpInitializedToolRequest/Response` | MCP 初始化请求/响应 |
| `McpClientInfo/McpServerInfo` | MCP 客户端/服务端信息 |
| `McpCapabilities` | MCP 能力描述 |
| `RequestId` | 请求 ID 生成器 |

> **说明**：Skill 技能模块的实现位于 `bboss-ai` 模块的 `skill` 包中，不在 `bboss-ai-model` 模块。详见 [3.2.x 技能系统](#技能skill系统) 章节。

#### 注解

| 注解 | 作用 |
|------|------|
| `@Tool` | 标注方法为可调用工具 |
| `@ToolParam` | 标注工具参数 |

---

### 3.2 bboss-ai 核心模块

#### 3.2.1 AIAgent - 智能体入口类

`AIAgent` 是用户与框架交互的主要入口，提供了简洁的 API 来调用各种 AI 能力：

**流式调用方法：**
- `streamChat()` - 流式智能问答
- `streamImageParser()` - 流式图片识别
- `streamVideoParser()` - 流式视频识别
- `streamAudioParser()` - 流式音频识别
- `streamAudioGen()` - 流式音频生成

**同步调用方法：**
- `chat()` - 同步智能问答
- `imageParser()` - 同步图片识别
- `videoParser()` - 同步视频识别
- `audioParser()` - 同步音频识别
- `genImage()` - 图片生成
- `genAudio()` - 音频生成
- `submitVideoTask()` / `getVideoTaskResult()` - 视频生成任务
- `embedding()` - 向量嵌入
- `rerank()` - 重排序

#### 3.2.2 Adapter 适配器层

采用适配器模式对不同 AI 平台进行适配：

```
AgentAdapter (抽象基类)
    ├── DeepseekAgentAdapter
    ├── KimiAgentAdapter
    ├── ZhipuAgentAdapter
    ├── QwenAgentAdapter
    ├── DoubaoAgentAdapter
    ├── BaiduAgentAdapter
    ├── JiutianAgentAdapter
    ├── SiliconflowAgentAdapter
    ├── MiniMaxAgentAdapter
    ├── TencentHYAgentAdapter
    ├── XinferenceAgentAdapter
    ├── OpenaiAgentAdapter
    └── NoneAgentAdapter
```

**AgentAdapter 核心职责：**
- 构建各类请求参数（Chat、Image、Audio、Video、Embedding、Rerank）
- 解析响应数据
- 处理流式数据解析
- 管理端点 URL

#### 3.2.3 Model 消息模型层

定义了各类消息类型，用于封装不同类型的 AI 请求：

| 类名 | 用途 |
|------|------|
| `ChatAgentMessage` | 智能问答消息 |
| `ImageAgentMessage` | 图片生成消息 |
| `ImageVLAgentMessage` | 图片识别（Vision）消息 |
| `VideoAgentMessage` | 视频生成消息 |
| `VideoVLAgentMessage` | 视频识别消息 |
| `AudioAgentMessage` | 音频生成消息 |
| `AudioSTTAgentMessage` | 语音识别消息 |
| `EmbeddingMessage` | 向量嵌入消息 |
| `RerankMessage` | 重排序消息 |
| `ToolAgentMessage` | 工具调用消息 |
| `SessionAgentMessage` | 带会话存储的消息 |
| `AgentMessage` | 消息基类，包含通用属性 |

#### 3.2.4 MCP 客户端

`MCPClient` 实现了 Model Context Protocol 协议客户端，支持两种传输模式：

**核心功能：**
- SSE（Server-Sent Events）连接管理（`MCPSSEClient`）
- Streamable HTTP 连接管理（`MCPStreamableClient`）
- 会话生命周期管理（initialize、notifications/initialized）
- 工具列表获取（tools/list）
- 工具调用（tools/call）
- 飞书 MCP 集成（`FeishuMCPClient`、`FeishuMCPStreamableClient`、`FeishuMcpRegist`）
- Spring AI MCP 兼容请求拦截器（`SpringAIMcpRequestIntercepter`），配置方式：`{poolName}.http.httpRequestInterceptors=org.frameworkset.spi.ai.mcp.intercepter.SpringAIMcpRequestIntercepter`

**工作流程：**
1. 通过 SSE 端点或 Streamable HTTP 端点建立连接
2. 接收 endpoint 事件获取 messagePath 和 sessionId
3. 发送 initialize 请求进行协议初始化
4. 发送 notifications/initialized 通知服务端
5. 正常进行工具列表查询和调用

**Spring Boot 客户端集成示例：**

```java
// 1. 配置 mcpserver.properties
// http.poolNames = test_mcp_server
// test_mcp_server.http.hosts = 127.0.0.1:8889
// test_mcp_server.http.apiKeyId = 123456
// test_mcp_server.http.extendConfigs.streamableendpoint = /test-biz-srv/mcp/streamable

// 2. 启动时初始化 MCP 连接池
@Component
public class AgentBootrap {
    @PostConstruct
    public void start() {
        // 初始化大模型 maas 服务
        HttpRequestProxy.startHttpPools("maas.properties");
        // 初始化 mcp 服务
        HttpRequestProxy.startHttpPools("mcpserver.properties");
    }
}

// 3. Spring Boot 配置类注册 MCP 工具
@Configuration
public class TestAgentMcpClientFactory {
    @Bean("testConfigMcpServerToolsRegist")
    public ToolsRegist buildTestConfigMcpServerToolsRegist() {
        // MCPToolsRegist 通过服务名称关联到 mcpserver.properties 配置
        ToolsRegist toolsRegist = new MCPToolsRegist("test_mcp_server");
        return toolsRegist;
    }
}

// 4. 在 AIAgent 中使用 MCP 工具
@Service
public class RagQAService {
    @Autowired
    @Qualifier("testConfigMcpServerToolsRegist")
    private ToolsRegist mcpToolsRegist;

    public ServerEvent chat(String question) {
        AIAgent agent = new AIAgent();
        agent.setToolsRegist(mcpToolsRegist);

        ChatAgentMessage message = new ChatAgentMessage();
        message.setPrompt(question);
        message.setModel("deepseek-chat");

        return agent.chat("maasName", message);
    }
}
```

**配置说明：**

| 配置项 | 说明 |
|--------|------|
| `http.poolNames` | 连接池名称列表，对应服务标识 |
| `{poolName}.http.hosts` | MCP 服务端地址 |
| `{poolName}.http.apiKeyId` | 访问 MCP 服务端的 API 密钥 |
| `{poolName}.http.extendConfigs.streamableendpoint` | Streamable HTTP 端点路径 |
| `{poolName}.http.extendConfigs.sseendpoint` | SSE 端点路径（可选） |

**MCPToolsRegist 工作原理：**

1. `MCPToolsRegist` 通过服务名称（如 `test_mcp_server`）关联到 `mcpserver.properties` 中的连接池配置
2. 初始化时向 MCP 服务端发送 `initialize` 请求进行协议握手
3. 调用 `tools/list` 获取服务端暴露的工具列表
4. 将工具列表转换为 `FunctionToolDefine` 供 `AIAgent` 使用
5. 当 AI 模型需要调用工具时，`MCPToolsRegist` 通过 `tools/call` 将请求转发到 MCP 服务端执行

#### 3.2.5 MCP 服务端

bboss-ai 不仅可以作为 MCP 客户端调用外部工具，还可以作为 MCP 服务端对外暴露工具能力：

**核心组件：**

| 类名 | 作用 |
|------|------|
| `MCPToolService` / `MCPToolServiceImpl` | MCP 服务端接口实现，提供 SSE 和 Streamable HTTP 两种服务端接口 |
| `MCPApiKeyService` / `MCPApiKeyServiceImpl` | API 密钥认证与工具注册管理 |
| `MCPBeanToolsRegist` / `MCPBeanToolFunctionCall` | Bean 工具服务端注册与调用 |
| `MCPApiRequestUtil` | 服务端请求响应构建工具 |

**服务端能力：**
- **SSE 模式**：`sse()` 建立 SSE 长连接，`message()` 处理客户端消息
- **Streamable HTTP 模式**：`streamable()` 处理无状态请求
- **API 密钥认证**：支持按 apiKey 注册和授权访问工具
- **工具权限控制**：支持按 functionName + apiKey 细粒度授权
- **支持的方法**：initialize、notifications/initialized、tools/list、tools/call

**服务端工作流程：**
1. 客户端通过 `registMcpBeanTool(apiKey, bean)` 注册 Bean 工具到指定密钥
2. 客户端通过 SSE 端点建立连接或发送 Streamable HTTP 请求
3. 服务端校验 apiKey 合法性
4. 根据请求方法返回工具列表或执行工具调用
5. 通过 SSE Sink 或 HTTP Response 返回结果

**Spring Boot 集成示例：**

```java
// 1. 使用 @Tool 注解定义工具
@Service
public class Hotel2ndFlightBookTool {
    @Tool(name="hotelQuery", description="根据用户的行程需求，查询合适的酒店。")
    public List<Map> hotelQuery(
        @ToolParam(name="startDay", description="入驻时间,例如：5月25日", required=true) String startDay,
        @ToolParam(name="endDay", description="离房时间,例如：5月28日", required=true) String endDay
    ) {
        // 业务逻辑实现
        return hotels;
    }

    @Tool(name="flightQuery", description="根据用户的行程需求，查询合适的航班机票。")
    public List<Map> flightQuery(
        @ToolParam(name="bookDay", description="出发时间,例如：5月25日", required=true) String bookDay,
        @ToolParam(name="arriveDay", description="到达时间,例如：5月28日", required=true) String arriveDay,
        @ToolParam(name="fromStation", description="出发地,例如：长沙", required=true) String fromStation,
        @ToolParam(name="toStation", description="到达地,例如：北京", required=true) String toStation
    ) {
        // 业务逻辑实现
        return flights;
    }
}

// 2. Spring Boot 配置类注册 MCP 服务
@Configuration
public class TestAgentMcpServiceFactory {
    @Autowired
    private Hotel2ndFlightBookTool hotel2ndFlightBookTool;

    @Bean("testConfigMCPToolService")
    public MCPToolService buildTestConfigMCPToolService() {
        MCPApiKeyServiceImpl mcpApiKeyService = new MCPApiKeyServiceImpl();
        // 将组件中定义的工具方法注册为 MCP 服务
        mcpApiKeyService.registMcpBeanTool("123456", hotel2ndFlightBookTool);

        MCPToolServiceImpl mcpService = new MCPToolServiceImpl();
        mcpService.setMcpApiKeyService(mcpApiKeyService);
        return mcpService;
    }
}

// 3. Spring Boot Controller 暴露 MCP 端点
@RestController
@RequestMapping("/mcp")
public class MCPServerController {
    @Autowired
    @Qualifier("testConfigMCPToolService")
    private MCPToolService testConfigMCPToolService;

    // SSE 模式端点
    @GetMapping("/sse")
    public Flux<String> sse(@RequestHeader(name="Authorization") String authorizationHeader) {
        String apiKey = HttpRequestProxy.extractApiKeyFromBearer(authorizationHeader);
        return testConfigMCPToolService.sse(apiKey);
    }

    // SSE 消息处理端点
    @PostMapping("/message")
    public String message(
        @RequestHeader(name="Authorization") String authorizationHeader,
        @RequestParam(name = "sessionId") String sessionId,
        @RequestBody String requestBody
    ) {
        String apiKey = HttpRequestProxy.extractApiKeyFromBearer(authorizationHeader);
        return testConfigMCPToolService.message(apiKey, sessionId, requestBody);
    }

    // Streamable HTTP 模式端点
    @PostMapping("/streamable")
    public Object streamable(
        @RequestHeader(name="Authorization") String authorizationHeader,
        @RequestBody String requestBody
    ) {
        String apiKey = HttpRequestProxy.extractApiKeyFromBearer(authorizationHeader);
        return testConfigMCPToolService.streamable(apiKey, requestBody);
    }
}
```

#### 3.2.6 会话管理（Session Store）

`AgentSessionStore` 体系提供智能体会话的存储和管理能力，支持内存和多种关系型/列式数据库持久化，并提供独立的会话管理服务 API。

**核心类：**

| 类名 | 作用 |
|------|------|
| `AgentSessionStore` | 会话存储接口 |
| `BaseAgentSessionStore` | 会话存储抽象基类，封装公共持久化逻辑 |
| `AgentSessionStoreMemory` | 内存会话存储 |
| `AgentSessionStoreDB` | 数据库持久化会话存储（支持 MySQL/Oracle/DM/SQLServer/PostgreSQL/SQLite/ClickHouse） |
| `AgentSessionStoreDBConfig` | 数据库会话存储配置，管理各数据库方言的建表 SQL 和 CRUD SQL |
| `AgentSessionStoreBuilder` | 会话存储构建器接口 |
| `DefaultAgentSessionStoreBuilder` | 默认会话存储构建器，根据 `StoreContext.storeType` 创建对应存储实现 |
| `AgentSessionService` | 会话管理服务接口（查询、删除、判断存在等） |
| `AgentSessionServiceImpl` | 会话管理服务实现，基于 `ConfigSQLExecutor` |
| `AgentSession` | 会话实体（sessionId、userId、agentId、domain、title、createTime、lastAccessTime） |
| `AgentSessionCondition` | 会话查询条件（多 domain、标题模糊、时间范围、排序字段可配置） |
| `SessionMessage` | 会话消息实体（含 19 种消息类型常量） |
| `LastSessionMessage` | 最新会话消息（子智能体输出结果传递载体） |
| `StoreContext` | 存储上下文（sessionId、userId、dataSource、clickhouseCluster、domain 等） |
| `AgentMessageTypeConvertor` | 角色（role）到 messageType 的转换器 |
| `AgentSessionException` | 会话管理异常 |
| `AgentIdAssign` | 智能体 ID 分配器 |

**支持的数据库：**

`AgentSessionStoreDBConfig` 通过 `DBUtil.getDBAdapter(dbName)` 自动识别数据库类型并选用对应方言的建表 SQL，首次使用时自动创建以下三张表：

| 表名 | 作用 | 支持数据库 |
|------|------|-----------|
| `agent_session` | 会话基本信息（sessionId、userId、agentId、title、domain、createTime、lastAccessTime） | MySQL、Oracle、DM、SQL Server、PostgreSQL、SQLite |
| `agent_session_message` | 会话消息记录（msgId、message、tokenMetrics、role、messageType、agentNodeType、subAgentIdBy 等） | 同上 |
| `agent_session_message_ref` | 智能体间消息引用关系（msgId、msgAgentId、refAgentId） | 同上 |

**会话管理服务 API（`AgentSessionService`）：**

| 方法 | 说明 |
|------|------|
| `deleteAgentSession(sessionid)` | 删除单个会话（含消息和引用关系，事务操作） |
| `deleteBatchAgentSession(sessionids...)` | 批量删除会话 |
| `getAgentSession(sessionid)` | 获取会话基本信息 |
| `existAgentSession(sessionid)` | 判断会话是否存在 |
| `queryListInfoAgentSessions(conditions, offset, pagesize)` | 分页查询会话列表 |
| `queryListAgentSessions(conditions)` | 查询会话列表（不分页） |
| `queryListSessionMessages(sessionid)` | 查询会话所有消息 |
| `queryListSessionMessages(sessionid, agentId)` | 查询指定智能体的会话消息 |

**会话查询条件（`AgentSessionCondition`）：**

| 字段 | 说明 |
|------|------|
| `userId` | 用户 ID 精确匹配 |
| `agentid` | 智能体 ID 精确匹配 |
| `domain` | 单领域精确匹配 |
| `domains` | 多领域联合查询（IN 条件） |
| `title` | 会话标题模糊查询（LIKE） |
| `timeConditionField` | 时间条件字段名，默认 `createTime`，可设为 `lastAccessTime` |
| `timeStart` / `timeEnd` | 时间范围（作用于 `timeConditionField` 指定的字段） |
| `sortKey` | 排序字段，默认 `createTime`，可设为 `lastAccessTime` |
| `sortDesc` | 是否降序 |

**存储上下文（`StoreContext`）核心属性：**

| 属性 | 说明 |
|------|------|
| `storeType` | 存储类型：`memory`（默认）或 `db` |
| `sessionId` | 会话 ID，为 null 时自动生成 UUID |
| `userId` | 用户 ID |
| `agentId` | 智能体 ID |
| `domain` | 会话所属业务领域 |
| `dataSource` | 数据源名称（db 模式必填） |
| `clickhouseCluster` | ClickHouse 集群名称（ClickHouse 模式必填） |
| `sessionTableName` | 会话表名，默认 `agent_session` |
| `sessionMessageTableName` | 会话消息表名，默认 `agent_session_message` |
| `sessionSize` | 会话记忆窗口大小，默认 20 |
| `requestId` | 请求 ID |
| `traceId` | 链路追踪 ID |
| `resetSession` | 是否重置会话 |
| `mainSessionStore` | 主会话存储（用于共享） |
| `agentMessageTypeConvertor` | 消息类型转换器（支持自定义扩展） |

##### ClickHouse 生产级会话存储

bboss-ai 支持基于 ClickHouse 分布式集群的生产级会话存储，适用于海量会话数据的高吞吐写入和查询场景。

**集群前置条件：**

1. 部署 ClickHouse 集群（如 `vops_3shards_1replicas`）
2. 为每个集群节点定义两个宏变量：
   - `shard`：分片编号
   - `replica`：副本编号

   ClickHouse 配置文件示例：
   ```xml
   <macros>
       <shard>01</shard>
       <replica>node01</replica>
   </macros>
   ```

3. 在 bboss 数据源配置中注册 ClickHouse 数据源（`visualops`）

**表结构（自动创建）：**

ClickHouse 模式下，框架会自动创建本地表（`*_local`）和分布式表两张表：

| 表类型 | 表名 | 引擎 | 说明 |
|--------|------|------|------|
| 本地表 | `agent_session_local` | `ReplicatedMergeTree` | 按分片存储，`ORDER BY (sessionId)` |
| 分布式表 | `agent_session` | `Distributed` | 路由表，`rand()` 随机分片 |
| 本地表 | `agent_session_message_local` | `ReplicatedMergeTree` | `ORDER BY (sessionId, createTime, seqNo)` |
| 分布式表 | `agent_session_message` | `Distributed` | `sipHash64(sessionId)` 哈希分片，保证同会话消息落在同一分片 |
| 本地表 | `agent_session_message_ref_local` | `ReplicatedMergeTree` | `ORDER BY (sessionId)` |
| 分布式表 | `agent_session_message_ref` | `Distributed` | `sipHash64(sessionId)` 哈希分片 |

建表 SQL 模板（节选自 [clickhouse-agent.xml](bboss-ai/src/main/java/org/frameworkset/spi/ai/store/db/clickhouse-agent.xml)）：

```sql
-- 本地表
CREATE TABLE agent_session_local ON CLUSTER vops_3shards_1replicas
(
    sessionId String COMMENT '会话id',
    createTime DateTime COMMENT '创建时间',
    lastAccessTime DateTime COMMENT '最后访问时间',
    userId String COMMENT '用户id',
    agentId String COMMENT '代理id',
    title String COMMENT '会话标题',
    domain String COMMENT '会话所属领域'
)
ENGINE = ReplicatedMergeTree('/clickhouse/tables/{shard}/{database}/{table}', '{replica}')
ORDER BY (sessionId);

-- 分布式表
CREATE TABLE agent_session ON CLUSTER vops_3shards_1replicas AS agent_session_local
ENGINE = Distributed(vops_3shards_1replicas, currentDatabase(), agent_session_local, rand());
```

**ClickHouse 模式特殊行为：**

1. **不更新最后访问时间**：ClickHouse 不支持高频 UPDATE，因此会话续问续答时不会执行 `UPDATE lastAccessTime` 操作（`AgentSessionStoreDBConfig.isClickhouse()` 判断后跳过）
2. **分片策略**：会话消息表采用 `sipHash64(sessionId)` 哈希分片，确保同一会话的所有消息落在同一分片，避免跨分片 JOIN
3. **建表语句**：通过 `ON CLUSTER` 在所有节点上创建，使用 `{shard}` 和 `{replica}` 宏变量自动适配副本路径
4. **自动识别**：`AgentSessionStoreDBConfig.isClickhouse(dataSource)` 通过数据库适配器自动识别 ClickHouse 类型（`clickhouse` 或 `yandex_clickhouse`）

**使用示例：**

```java
// 1. 初始化 ClickHouse 数据源
HttpRequestProxy.startHttpPools("clickhouse-datasource.properties");
// 或者通过 SQLUtil.startPool 初始化
SQLUtil.startPool("visualops",
    "com.clickhouse.jdbc.ClickHouseDriver",
    "jdbc:clickhouse://127.0.0.1:8123/bboss",
    "default", "", "select 1");

// 2. 配置带 ClickHouse 集群的存储上下文
StoreContext storeContext = new StoreContext()
    .setSessionId(sessionId)
    .setUserId("user123")
    .setSessionSize(100)
    .setStoreType(StoreContext.STORE_TYPE_DB)
    .setRequestId("request123")
    .setClickhouseCluster("vops_3shards_1replicas")  // 必填，否则建表报错
    .setDataSource("visualops")
    .setDomain("ops");

// 3. 在 AIAgent 中使用
ChatAgentMessage message = new ChatAgentMessage();
message.setPrompt("帮我分析系统日志");
message.setModel("deepseek-chat");
message.setMaas("deepseek");
message.setStoreContext(storeContext);

AIAgent agent = new AIAgent();
agent.setEnableLoopToolCall(true);
agent.setMaxLoopToolCalls(80);
Flux<ServerEvent> flux = agent.streamChat(message);
```

**注意事项：**

- `clickhouseCluster` 必须设置，否则建表 SQL 会报错；如果表已经创建好，可以不用设置
- ClickHouse 模式下 `lastAccessTime` 字段不会随续问更新，排序和查询建议使用 `createTime`
- 建表语句通过 `ON CLUSTER` 在集群所有节点执行，需要对应集群已在 ClickHouse 中配置
- 会话消息表的分布式表采用 `sipHash64(sessionId)` 分片，保证同会话消息在同一分片便于查询

#### 3.2.7 工具注册与搜索

**核心类：**

| 类名 | 作用 |
|------|------|
| `ToolSearcher` | 工具搜索接口，根据 query 筛选相关工具 |
| `KeywordToolSearcher` | 基于关键词的工具搜索实现 |
| `ToolCallContext` | 工具调用上下文，封装工具参数传递给其他流程使用 |
| `BeanToolsRegist` | Bean 工具注册（本地智能体调用） |
| `BeanToolFunctionCall` | Bean 工具函数调用（本地，带 Trace 记录） |
| `MCPBeanToolsRegist` | MCP 服务端 Bean 工具注册（对外暴露） |
| `MCPBeanToolFunctionCall` | MCP 服务端 Bean 工具调用（返回 `List<Map>`） |
| `BaseBeanToolsRegist` | Bean 工具注册抽象基类，封装注解解析和工具索引管理 |
| `BaseBeanToolFunctionCall` | Bean 工具调用抽象基类，封装参数解析和反射调用 |
| `BeanToolHandle` | Bean 工具解析处理，扫描 `@Tool`/`@ToolParam` 注解构建 `FunctionToolDefine` |
| `BeanToolFunctionCallBuilder` | Bean 工具函数调用构建器接口 |
| `PermissionType` | 工具权限类型枚举（ASK/ALLOW/DENY），位于 `tool.permission` 包 |

##### 基于注解快速发布工具服务

bboss-ai 提供 `@Tool` 和 `@ToolParam` 两个注解（位于 `org.frameworkset.spi.ai.model.annotation` 包），允许开发者将普通 Java Bean 方法快速发布为可供 LLM 调用的工具服务。框架在运行时通过反射扫描注解，自动生成符合 OpenAI Function Calling 规范的 `FunctionToolDefine` 和 JSON Schema 参数定义，无需手写复杂的工具描述。

**`@Tool` 注解（方法级）：**

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `name` | String | ""（取方法名） | 工具名称，为空时使用方法名 |
| `description` | String | 必填 | 工具描述，供 LLM 理解工具用途 |
| `type` | String | `"function"` | 工具类型 |
| `strict` | boolean | `true` | 是否严格模式（严格校验参数） |
| `additionalProperties` | boolean | `false` | 是否允许额外参数 |

**`@ToolParam` 注解（参数级）：**

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `name` | String | 必填 | 参数名称 |
| `description` | String | 必填 | 参数描述 |
| `type` | String | `"object"` | 参数类型（自动推断：String→string、Number→number、Integer→integer、Boolean→boolean、List/Set→array、Map/Object→object） |
| `required` | boolean | `false` | 是否必填 |
| `bean` | boolean | `false` | 参数为 Bean 时递归解析子参数 |
| `format` | String | "" | 预定义格式校验（email、hostname、ipv4、ipv6、uuid） |
| `pattern` | String | "" | 正则表达式校验 |
| `enumValues` | String[] | {} | 枚举值约束 |
| `arrayItemType` | String | "" | 数组元素类型 |
| `arrayItemDescription` | String | "" | 数组元素描述 |
| `constValue` | String | "" | 固定常数值 |
| `defaultValue` | String | "" | 默认值 |
| `minimum` / `maximum` | String | "" | 最小/最大值（number/integer） |
| `exclusiveMinimum` / `exclusiveMaximum` | String | "" | 不小于/不大于 |
| `multipleOf` | String | "" | 倍数约束 |

**工作原理：**

`BeanToolHandle.parserTools()` 扫描 Bean 中所有标注 `@Tool` 的方法，对每个方法的参数标注 `@ToolParam` 的参数进行解析：

1. 根据 Java 参数类型自动推断 JSON Schema 类型（String→string、Number→number、List→array 等）
2. 将 `@ToolParam` 的 `description`、`format`、`pattern`、`enumValues`、`minimum`、`maximum` 等属性映射到 `Property` 对象
3. 收集 `required=true` 的参数组成 `required` 数组
4. 构建 `FunctionToolDefine` 对象，包含函数名、描述、参数 schema 和 `FunctionCall` 调用实现
5. 通过 `BaseBeanToolsRegist` 管理工具索引，支持按名称查找和去重

**本地工具服务发布示例（供智能体直接调用）：**

```java
@Service
public class WeatherService {
    @Tool(name = "get_weather", description = "获取指定城市的天气")
    public String getWeather(
        @ToolParam(name = "city", description = "城市名称", required = true) String city,
        @ToolParam(name = "days", description = "预报天数", minimum = "1", maximum = "7") Integer days
    ) {
        // 业务逻辑实现
        return "晴天，25°C";
    }

    @Tool(name = "get_weather_by_type", description = "按天气类型查询城市")
    public List<String> getCitiesByWeatherType(
        @ToolParam(name = "weatherType", description = "天气类型",
                   enumValues = {"sunny", "rainy", "cloudy", "snowy"}, required = true) String weatherType
    ) {
        // 业务逻辑实现
        return Arrays.asList("北京", "上海");
    }
}

// 在智能体中注册并使用
AIAgent agent = new AIAgent();
agent.registBeanTool(new WeatherService());  // 自动扫描 @Tool 注解方法

ChatAgentMessage message = new ChatAgentMessage();
message.setPrompt("北京今天天气怎么样？");
ServerEvent result = agent.chat("maasName", message);
```

`agent.registBeanTool(beanTool)` 内部调用 `BeanToolHandle.parserTools()` 扫描注解，将扫描到的 `FunctionToolDefine` 添加到 `tools` 列表，随请求一起传递给 LLM。调用时由 `BeanToolFunctionCall` 通过反射执行方法，并自动记录 Trace（`toolCallArgs`、`toolCallResponse`、`toolCallException`）。

##### 基于注解快速发布 MCP 服务

基于同一套 `@Tool`/`@ToolParam` 注解，开发者可以将 Bean 方法快速发布为 MCP 服务端工具，供外部 MCP 客户端（如 Claude Desktop、其他 AI 智能体）调用。

**MCP 服务端核心类：**

| 类名 | 作用 |
|------|------|
| `MCPToolService` / `MCPToolServiceImpl` | MCP 服务端接口，提供 SSE 和 Streamable HTTP 两种服务端接口 |
| `MCPApiKeyService` / `MCPApiKeyServiceImpl` | API 密钥认证与工具注册管理 |
| `MCPBeanToolsRegist` | MCP 服务端 Bean 工具注册器，继承 `BaseBeanToolsRegist` |
| `MCPBeanToolFunctionCall` | MCP 服务端 Bean 工具调用，返回 `List<Map>` 类型 |
| `MCPApiRequestUtil` | 服务端请求响应构建工具 |
| `MCPSSEServer` | MCP SSE 服务端实现 |

**MCP 服务端工作流程：**

1. **注册工具**：通过 `MCPApiKeyServiceImpl.registMcpBeanTool(apiKey, bean)` 注册 Bean 工具到指定 API 密钥
2. **认证校验**：客户端请求时通过 `auth(apiKey)` 校验密钥，通过 `auth(functionName, apiKey)` 校验工具访问权限
3. **协议握手**：客户端发送 `initialize` 请求，服务端返回能力描述
4. **工具列表**：客户端发送 `tools/list`，服务端通过 `getMcpServerApiKeyInfo(apiKey)` 返回工具列表
5. **工具调用**：客户端发送 `tools/call`，服务端通过 `getFunctionToolDefine(apiKey, functionName)` 获取定义并通过 `MCPBeanToolFunctionCall.call()` 执行

**MCP 服务端发布示例：**

```java
// 1. 使用 @Tool 注解定义工具 Bean
@Service
public class HotelFlightBookTool {
    @Tool(name = "hotelQuery", description = "根据用户的行程需求，查询合适的酒店。")
    public List<Map> hotelQuery(
        @ToolParam(name = "startDay", description = "入驻时间,例如：5月25日", required = true) String startDay,
        @ToolParam(name = "endDay", description = "离房时间,例如：5月28日", required = true) String endDay
    ) {
        // 业务逻辑实现
        return hotels;
    }

    @Tool(name = "flightQuery", description = "根据用户的行程需求，查询合适的航班机票。")
    public List<Map> flightQuery(
        @ToolParam(name = "bookDay", description = "出发时间,例如：5月25日", required = true) String bookDay,
        @ToolParam(name = "arriveDay", description = "到达时间,例如：5月28日", required = true) String arriveDay,
        @ToolParam(name = "fromStation", description = "出发地,例如：长沙", required = true) String fromStation,
        @ToolParam(name = "toStation", description = "到达地,例如：北京", required = true) String toStation
    ) {
        // 业务逻辑实现
        return flights;
    }
}

// 2. Spring Boot 配置类注册 MCP 服务
@Configuration
public class TestAgentMcpServiceFactory {
    @Autowired
    private HotelFlightBookTool hotelFlightBookTool;

    @Bean("testConfigMCPToolService")
    public MCPToolService buildTestConfigMCPToolService() {
        MCPApiKeyServiceImpl mcpApiKeyService = new MCPApiKeyServiceImpl();
        // 将 @Tool 注解方法注册为 MCP 服务工具
        mcpApiKeyService.registMcpBeanTool("123456", hotelFlightBookTool);
        // 支持注册多个 Bean 到同一 apiKey
        // mcpApiKeyService.registMcpBeanTool("123456", anotherToolBean);
        // 支持注册同一 Bean 到多个 apiKey
        // mcpApiKeyService.registMcpBeanTool(new String[]{"key1", "key2"}, toolBean);

        MCPToolServiceImpl mcpService = new MCPToolServiceImpl();
        mcpService.setMcpApiKeyService(mcpApiKeyService);
        return mcpService;
    }
}

// 3. Spring Boot Controller 暴露 MCP 端点
@RestController
@RequestMapping("/mcp")
public class MCPServerController {
    @Autowired
    @Qualifier("testConfigMCPToolService")
    private MCPToolService testConfigMCPToolService;

    // SSE 模式端点
    @GetMapping("/sse")
    public Flux<String> sse(@RequestHeader(name = "Authorization") String authorizationHeader) {
        String apiKey = HttpRequestProxy.extractApiKeyFromBearer(authorizationHeader);
        return testConfigMCPToolService.sse(apiKey);
    }

    @PostMapping("/message")
    public String message(
        @RequestHeader(name = "Authorization") String authorizationHeader,
        @RequestParam(name = "sessionId") String sessionId,
        @RequestBody String requestBody
    ) {
        String apiKey = HttpRequestProxy.extractApiKeyFromBearer(authorizationHeader);
        return testConfigMCPToolService.message(apiKey, sessionId, requestBody);
    }

    // Streamable HTTP 模式端点
    @PostMapping("/streamable")
    public Object streamable(
        @RequestHeader(name = "Authorization") String authorizationHeader,
        @RequestBody String requestBody
    ) {
        String apiKey = HttpRequestProxy.extractApiKeyFromBearer(authorizationHeader);
        return testConfigMCPToolService.streamable(apiKey, requestBody);
    }
}
```

**MCP 服务端注册 API 说明：**

| 方法 | 说明 |
|------|------|
| `registMcpBeanTool(apiKey, bean)` | 注册 Bean 工具到单个 apiKey，可多次调用注册多个 Bean |
| `registMcpBeanTool(apiKeys[], bean)` | 注册 Bean 工具到多个 apiKey |
| `auth(apiKey)` | 校验 apiKey 是否存在 |
| `auth(functionName, apiKey)` | 校验 apiKey 是否有访问指定工具的权限 |
| `getMcpServerApiKeyInfo(apiKey)` | 获取 apiKey 对应的工具列表 |
| `getFunctionToolDefine(apiKey, functionName)` | 获取指定工具定义 |

**本地调用与 MCP 服务端调用的区别：**

| 维度 | 本地工具（`BeanToolsRegist`） | MCP 服务端工具（`MCPBeanToolsRegist`） |
|------|------------------------------|--------------------------------------|
| 调用方式 | 智能体通过反射直接调用 | 通过 MCP 协议（SSE/Streamable HTTP）远程调用 |
| 返回类型 | `Object`（任意类型） | `List<Map>`（MCP 协议规范） |
| Trace 记录 | `BeanToolFunctionCall` 自动记录 Trace | 由 MCP 客户端记录调用轨迹 |
| 适用场景 | 单进程内的智能体工具调用 | 跨进程/跨系统工具服务暴露 |
| 认证 | 无需认证 | 基于 apiKey 认证和工具级权限控制 |

#### 多次调用工具（Loop Tool Call）

对于需要多步骤执行的复杂任务，智能体可能需要**多次调用工具**才能完成。bboss-ai 内置了循环工具调用机制，允许模型根据前序工具执行结果，自动决策是否继续调用下一个工具。该机制同时适用于单智能体和多智能体（非工作流）场景。

**核心 API：**

| 方法 | 说明 |
|------|------|
| `AIAgent.setEnableLoopToolCall(boolean)` | 启用/禁用循环工具调用，默认 `false` |
| `AIAgent.setMaxLoopToolCalls(int)` | 设置最大循环次数，防止无限循环，默认 `80` 轮 |

**典型使用场景：**
- 运维场景：获取 OS 信息 → 生成脚本 → 执行命令 → 核对结果
- 数据分析：查询数据 → 清洗处理 → 统计计算 → 生成图表
- 业务流程：参数校验 → 调用服务 A → 根据 A 结果调用服务 B → 汇总输出

**注意事项：**
- 循环调用过程中，每次工具执行结果都会作为上下文反馈给模型
- 模型自主决定何时终止工具调用并输出最终答案
- 建议配合 `setRetry(int)` 使用，增强容错能力
- `SequenceAgent` 多智能体场景也支持循环工具调用机制
- 工具调用过程中可通过 `emitterServerEvent()` 向客户端实时推送中间数据

#### 3.2.12 内置工具体系

bboss-ai 内置了完整的工具体系，位于 `org.frameworkset.spi.ai.tools` 包下，覆盖 Shell 执行、多语言代码执行、文件系统操作、操作系统信息查询、文本搜索、人工介入六大场景。所有内置工具均继承 `BaseAuditorTool`，支持通过 `Auditor` 接口实现工具调用前审计拦截。

##### 工具总览

| 工具类 | 功能域 | 暴露工具数 | 主要能力 |
|--------|--------|------------|----------|
| `CLIShellFunctionTool` | Shell 执行 | 1 | 跨平台执行 cmd/sh 脚本，超时控制 |
| `CodeExecuteFunctionTool` | 代码执行 | 3 | 动态编译运行 Java，调用 Python/Node 执行 Python/JavaScript |
| `FileFunctionTool` | 文件系统 | 9 | 文件读写、拷贝、删除、属性查询、编码识别、目录遍历 |
| `GetOSFunctionTool` | 系统信息 | 1 | 获取 OS 名称/版本/架构及 CPU 核数/型号 |
| `GrepFunctionTool` | 文本搜索 | 1 | 跨平台文本搜索（Linux/Mac 用 grep，Windows 用 findstr），支持正则、递归目录、文件扩展名过滤、超时控制 |
| `HitlTaskcallTool` | 人工介入 | 1 | HITL（Human-in-the-Loop）人工介入工具，继承 `BaseHitlTaskTool`，当 AI 无法独立完成任务时调用 |

##### 通用配置约定

| 配置项 | 适用工具 | 说明 |
|--------|----------|------|
| **超时** | `CLIShellFunctionTool`、`CodeExecuteFunctionTool`、`GetOSFunctionTool` | 支持构造器 `new XxxTool(long timeout)` 或 `setTimeout(long)`，单位秒，默认 60 秒；超时后任务被取消并返回提示信息 |
| **线程池** | Shell 与代码执行工具 | 使用独立的守护线程池（`cli-shell-executor` / `code-execute-executor`），避免阻塞 `ForkJoinPool.commonPool` |
| **链式配置** | 所有工具 | `setTimeout` / `setBaseDirectory` / `setCompileOutputDir` 均返回 `this`，支持链式调用 |

##### CLIShellFunctionTool —— Shell 命令执行工具

**功能说明**：跨平台（Windows/Linux/Unix/Mac）执行 Shell 脚本。
- **Windows**：调用 `cmd /c`
- **其他平台**：调用 `sh -c`
- **字符编码**：自动合并 stdout 与 stderr，Windows 使用 GBK 解码，其余平台使用 UTF-8

**工具方法**：`executeBash(command)` —— 执行 Shell 命令

##### CodeExecuteFunctionTool —— 多语言代码执行工具

**功能说明**：支持 Java、Python、JavaScript 三种语言的动态执行，统一返回标准化的执行结果结构（`success`、`exitCode`、`output`、`message`）。

**工具方法**：
- `executeJava(code)` —— 编译并执行 Java 代码，自动包装到 `Main` 类的 `main` 方法中
- `executePython(code)` —— 通过系统 Python 解释器执行 Python 代码
- `executeJavaScript(code)` —— 优先使用 JDK 内置 Nashorn 引擎，回退到系统 `node` 命令

**执行机制**：
- **Java**：使用 `JavaCompiler` 内存编译 → `URLClassLoader` 加载 → 反射调用 `main` 方法 → 执行后自动清理临时目录
- **Python**：写入临时 `.py` 文件 → 调用系统 Python → 执行后删除临时文件
- **JavaScript**：Nashorn 模式直接 `ScriptEngine.eval()`，Node 模式写入临时 `.js` 文件执行

##### FileFunctionTool —— 文件系统操作工具

**功能说明**：提供文件与目录的增删改查、内容读写、编码识别、属性获取等 9 个工具方法，支持通过 `baseDirectory` 限制操作范围防止路径穿越攻击。

**工具方法分类**：

| 类别 | 方法 | 功能 |
|------|------|------|
| **文件操作** | `copyFile`、`deleteFile`、`createFile`、`fileExists` | 拷贝、删除、创建、检查存在 |
| **文件读写** | `readFile`、`writeFile`、`readDirectoryFiles` | 读取、写入、遍历目录读取 |
| **文件信息** | `getFileAttributes`、`detectFileEncoding` | 获取属性、检测编码 |

**路径安全**：设置 `baseDirectory` 后，所有路径经规范化后必须以基目录开头，否则抛出 `IllegalArgumentException`。

##### GetOSFunctionTool —— 操作系统信息查询工具

**功能说明**：获取当前运行环境的操作系统及 CPU 信息，无入参。继承 `BaseAuditorTool`，支持审计拦截。

**工具方法**：`getOS2ndCpu()` —— 返回 `os`、`osVersion`、`osArch`、`cpuCores`、`cpuName`

**CPU 型号获取机制**：

| 平台 | 命令/来源 |
|------|----------|
| Windows | `wmic cpu get Name` |
| Linux | 读取 `/proc/cpuinfo` 的 `model name` |
| Mac / Unix | `uname -p` |
| 失败回退 | `os.arch` |

##### GrepFunctionTool —— 跨平台文本搜索工具

**功能说明**：跨平台文本搜索工具，通过调用操作系统原生命令实现文本搜索：Linux/Mac 使用 `grep`，Windows 使用 `findstr`。继承 `BaseAuditorTool`，支持审计拦截。

**支持特性**：
- 正则表达式搜索
- 递归目录搜索
- 大小写控制
- 文件扩展名过滤
- 行号显示
- 超时控制（默认 60 秒）
- 基目录限制（防止路径穿越攻击）

**独立线程池**：使用独立的守护线程池（`grep-executor`），避免长时间搜索阻塞 `ForkJoinPool.commonPool`。

##### HitlTaskcallTool —— 人工介入工具（HITL）

**功能说明**：HITL（Human-in-the-Loop）人工介入工具，继承 `BaseHitlTaskTool`（进而继承 `BaseAuditorTool`），当 AI 无法独立完成任务、遇到关键决策点、需要人工审批或验证时调用。

**类继承关系**：
```
BaseAuditorTool  →  BaseHitlTaskTool  →  HitlTaskcallTool
```

**核心接口与类**：

| 类名/接口 | 作用 |
|------------|------|
| `BaseHitlTaskTool` | Hitl 工具抽象基类，继承 `BaseAuditorTool`，实现 `HitlTaskToolInf` 接口 |
| `HitlTaskToolInf` | Hitl 工具接口，定义超时动作常量（`continue`/`rejected`）、超时时间、HitlAssistant 管理 |
| `HitlAssistant` | 人工干预辅助接口，提供 `getHumanAssistantDatas()` 向前端提供辅助信息、`handleHumanSubbmitDatas()` 处理人工提交数据 |
| `HitlTaskHelper` | Hitl 任务辅助类，管理人工介入任务的创建、中断、唤醒 |
| `HitlTaskCallListener` | 任务调用监听器（单节点内存模式） |
| `HitlTaskCallNotifier` | 任务调用通知器（单节点内存模式） |
| `RedisHitlTaskCallListener` | Redis 集群任务调用监听器 |
| `RedisHitlTaskCallNotifier` | Redis 集群任务调用通知器 |

**超时处理**：
- `hitlTaskTimeout`：任务超时时间（毫秒），默认 -1（不超时）
- `timeoutAction`：超时处理方式，默认 `rejected`（拒绝执行），可设为 `continue`（继续执行）

**适用场景**：
1. 复杂问题需要人类专业判断
2. 敏感操作需要人工确认
3. 任务执行结果不符合预期需要人工介入调整
4. 超出 AI 权限范围的操作

**工具方法**：`hitlTaskTool(hitlTaskReason)` —— 发起人工介入请求

**参数说明**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `hitlTaskReason` | String | 是 | 人工介入原因，需包含：1.任务背景与已执行步骤 2.当前卡住的具体原因 3.建议人类关注的关键点 4.期望人类提供的具体帮助 |

**上下文内容要求**：精简聚焦，包含三要素——已执行步骤、卡住原因、建议关注要点，让人类在 3 秒内快速理解并做出决策。

**异常处理机制**：
- **参数 null/空白校验**：防止空任务传递给人工
- **chatObject null 检查**：防御非对话上下文调用
- **HitlTaskHelper null 检查**：防御组件未初始化场景
- **异常捕获**：记录错误日志并返回友好提示，不中断智能体执行

**技能中的声明方式**：

在 SKILL.md 技能文件中，通过自然语言描述触发条件和调用逻辑，LLM 会自动识别并检测已注册的工具：

```markdown
## step 5: 问题修复
- 请生成修复后的代码
- 调用文件处理工具保存修改后的代码
- 如果存在人工介入工具hitlTaskTool，则调用人工介入任务工具hitlTaskTool，通知人工介入确认后，才能保存修复后的代码到原始文件，否则忽略保存代码到原始文件。
- 修复问题时，要指出问题的位置和原因。
- 修复后，要检查是否解决了问题。
```

**工具检测与调用流程**：
1. **LLM 解析**：读取技能内容，识别到 `hitlTaskTool` 工具名
2. **工具列表检查**：查询已注册的 `FunctionToolDefine` 列表
3. **名称匹配**：通过名称精确匹配查找工具
4. **条件判断**：根据"如果存在...则调用"的描述执行条件判断
5. **生成调用请求**：如果找到匹配工具，生成 `FunctionTool` 调用对象
6. **执行工具调用**：`BeanToolFunctionCall.call()` 执行实际的工具方法

##### 通用注册方式

```java
AIAgent agent = new AIAgent();

// 注册所有内置工具（推荐方式）
agent.registBeanTool(new GetOSFunctionTool(60));          // 60 秒超时
agent.registBeanTool(new CLIShellFunctionTool(60));
agent.registBeanTool(new CodeExecuteFunctionTool(60));
agent.registBeanTool(new FileFunctionTool("/data/safe"));  // 限制文件操作基目录
agent.registBeanTool(new GrepFunctionTool(60));             // 文本搜索工具
agent.registBeanTool(new HitlTaskcallTool());              // 人工介入工具
```

##### 工具审计系统（Auditor）

bboss-ai 内置了工具审计系统，位于 `org.frameworkset.spi.ai.audit` 包下，所有内置工具均继承 `BaseAuditorTool`，支持在工具调用前进行审计拦截。

**核心类**：

| 类名 | 作用 |
|------|------|
| `Auditor` | 审计接口，定义 `audit(AuditContext)` 方法，返回 `AuditResult` |
| `AuditContext` | 审计上下文，封装工具名称、参数等 |
| `AuditResult` | 审计结果，返回非 null 时阻止工具执行，结果直接返回给模型 |
| `BaseAuditorTool` | 审计工具抽象基类，所有内置工具继承此类 |

**工作机制**：
1. 工具调用前，`BaseAuditorTool` 调用 `auditor.audit(auditContext)` 进行审计
2. 如果 `AuditResult` 非 null，工具执行被阻止，审计结果直接返回给模型
3. 如果 `AuditResult` 为 null，工具正常执行
4. 如果未设置 `Auditor`（`auditor` 为 null），工具正常执行

**适用场景**：
- 敏感操作的审批拦截（如删除文件、执行脚本）
- 权限控制和访问管理
- 工具调用的合规审计记录

##### 安全风险与注意事项

**高危工具警示**：

| 工具方法 | 风险等级 | 说明 |
|----------|----------|------|
| `executeBash` | 🔴 高危 | 会真实执行任意 Shell 命令 |
| `executeJava` | 🔴 高危 | 会动态编译并执行任意 Java 代码 |
| `executePython` | 🔴 高危 | 会调用系统解释器执行任意 Python 代码 |
| `executeJavaScript` | 🔴 高危 | 会执行任意 JavaScript 代码 |

**安全建议**：
- 运行在隔离容器中
- 使用专用低权限系统账户
- 限制网络访问
- 开启严格的资源配额
- 为 `FileFunctionTool` 设置 `baseDirectory` 限制操作范围

**环境依赖**：

| 工具 | 依赖要求 |
|------|----------|
| `executeJava` | JDK（非 JRE），需提供 `JavaCompiler` |
| `executePython` | 系统 PATH 中包含 `python3` 或 `python` |
| `executeJavaScript` | JDK 内置 Nashorn 引擎或系统 PATH 中包含 `node` |
| `executeBash` | Windows 需 `cmd.exe`；其他平台需 `/bin/sh` |

#### 3.2.8 回调机制

| 类名 | 作用 |
|------|------|
| `ChatContext` | 聊天上下文，承载回调和中间状态 |
| `ChatStreamCallback` | 流式回调接口 |
| `ChatCallback` | 同步回调接口 |
| `AgentOutput` | 智能体输出处理 |

#### 3.2.9 Reactor 流式处理

基于 Project Reactor 实现响应式流式处理：

| 类名 | 作用 |
|------|------|
| `StreamDataHandler` | 流数据处理接口 |
| `BaseStreamDataHandler` | 基础流数据处理实现 |
| `BaseCommonStreamDataHandler` | 通用流数据处理 |
| `CommonStreamDataHandler` | 公共流数据处理接口 |
| `FluxSinkStatus` | FluxSink 状态管理 |
| `ReactorCallException` | Reactor 调用异常 |
| `SSEHeaderSetFunction` | SSE 头设置函数式接口 |
| `DisposeEventHandler` | 流dispose事件处理 |

#### 3.2.10 Util 工具类

| 类名 | 作用 |
|------|------|
| `AIAgentUtil` | AI 智能体核心工具类，封装所有 HTTP 调用逻辑 |
| `AIResponseUtil` | 响应解析工具，处理流式和同步响应 |
| `MessageBuilder` | 消息构建工具 |
| `StreamDataBuilder` | 流数据构建器 |
| `BaseStreamDataBuilder` | 基础流数据构建器 |
| `AudioDataBuilder` | 音频数据构建器 |

#### 3.2.11 智能体 Trace 可观测性

bboss-ai 内置了一套全链路、多维度的智能体 Trace 可观测性体系，覆盖 LLM 调用、工具执行、工作流编排等全部环节，支持内存和数据库两种持久化方式，并可通过流式通道实时推送观测事件到前端。

**核心模型：**

| 类名 | 作用 |
|------|------|
| `TraceMessage` | Trace 消息载体，包含消息内容、起止时间、agentId、parentAgentId、traceId、metaData 等 |
| `TokenMetrics` | Token 用量指标，包含 model、maas、totalTokens、promptTokens、promptCachedTokens、promptCacheHitTokens、promptCacheMissTokens、promptTextTokens、completionTokens、completionReasoningTokens、completionTextTokens、reasoningData、startTime、endTime、elapsed 等 |
| `SessionMessage` | 会话消息类型体系，定义了 19 种消息类型常量，覆盖用户输入、LLM 输入输出、Embedding、Rerank、工具搜索、MCP 调用、Trace 等 |
| `AgentMessageTypeConvertor` | 角色（role）到 messageType 的转换器，支持用户自定义扩展（101 起） |

**TokenMetrics 指标说明：**

| 指标 | 说明 |
|------|------|
| `maas` | 大模型服务平台名称 |
| `model` | 模型名称 |
| `totalTokens` | 总 Token 数 |
| `promptTokens` | 输入提示 Token 数 |
| `promptCachedTokens` | 命中缓存的输入 Token 数 |
| `promptCacheHitTokens` | Prompt 缓存命中 Token 数 |
| `promptCacheMissTokens` | Prompt 缓存未命中 Token 数 |
| `promptTextTokens` | 输入文本 Token 数 |
| `completionTokens` | 输出 Token 数 |
| `completionReasoningTokens` | 输出推理 Token 数（思维链） |
| `completionTextTokens` | 输出文本 Token 数 |
| `reasoningData` | 思考数据 |
| `startTime` / `endTime` | 模型执行开始/结束时间戳 |
| `elapsed` | 模型执行耗时（毫秒） |

**消息类型对照表：**

消息类型体系采用 `messageType` 数值编码与 `role` 字符串名称双标识设计，覆盖用户交互、模型调用、工具执行、观测追踪等全链路场景。其中 0–17 为框架内置标准类型，18 为未映射类型的兜底编码；业务如需自定义扩展，建议从 101 开始编码，并在 `AgentMessageTypeConvertor` 中注册映射关系。

| 常量 | 值 | role 名称 | 说明 |
|------|-----|-----------|------|
| `MESSAGE_TYPE_ASSISTANT_MESSAGE` | 0 | assistant | 智能体辅助消息 |
| `MESSAGE_TYPE_AGENT_RESULTMESSAGE` | 1 | agentresult | 智能体输出结果 |
| `MESSAGE_TYPE_USER_MESSAGE` | 2 | user | 用户输入消息（提交给模型） |
| `MESSAGE_TYPE_SYSTEM_MESSAGE` | 3 | system | 系统消息 |
| `MESSAGE_TYPE_TOOL_MESSAGE` | 4 | tool | 工具调用结果消息（需提交给大模型） |
| `MESSAGE_TYPE_TRACE_MESSAGE` | 5 | trace | 智能体跟踪消息 |
| `MESSAGE_TYPE_RAG_MESSAGE` | 6 | rag | RAG 知识库资料消息 |
| `MESSAGE_TYPE_REFUSE_MESSAGE` | 7 | refuse | 拒答消息 |
| `MESSAGE_TYPE_USER_INPUTMESSAGE` | 8 | userinput | 用户原始输入（含文件、图片描述） |
| `MESSAGE_TYPE_LLM_INPUTMESSAGE` | 9 | llminput | 提交给 LLM 的完整请求报文 |
| `MESSAGE_TYPE_LLM_OUTPUTMESSAGE` | 10 | llmoutput | LLM 返回的完整响应报文 |
| `MESSAGE_TYPE_EMBEDDING_INPUTMESSAGE` | 11 | embeddinginput | Embedding 模型输入 |
| `MESSAGE_TYPE_RERANK_INPUTMESSAGE` | 12 | rerankinput | Rerank 模型输入 |
| `MESSAGE_TYPE_EMBEDDING_OUTPUTMESSAGE` | 13 | embeddingoutput | Embedding 模型输出 |
| `MESSAGE_TYPE_RERANK_OUTPUTMESSAGE` | 14 | rerankoutput | Rerank 模型输出 |
| `MESSAGE_TYPE_TOOLSEARCH_MESSAGE` | 15 | toolsearch | 工具搜索匹配消息 |
| `MESSAGE_TYPE_MCPCALL_MESSAGE` | 16 | mcpcall | MCP 服务调用消息 |
| `MESSAGE_TYPE_TOOLCALL_MESSAGE` | 17 | toolcall | 工具服务调用消息 |
| `MESSAGE_TYPE_OTHER_MESSAGE` | 18 | 角色名称（实际设置值） | 其他未映射消息类型（需在 AgentMessageTypeConvertor 中建立映射） |

**自动 Trace 采集：**

`AIAgentUtil` 在各类模型调用前后自动插入 Trace，无需业务代码介入：

- `traceLLMInput()`：记录提交给模型的完整请求报文（LLM、Embedding、Rerank）
- `traceLLMOutput()`：记录模型返回的完整响应报文
- 覆盖场景：同步/流式聊天、图片生成、音频生成、视频任务提交、Embedding、Rerank

**流式 Token 计量：**

`BaseStreamDataBuilder` 在流式响应过程中通过 `computeTokens()` 累加各数据段的 Token 用量，最终在 `addChatWithToolCallSessionMessage()` 中将完整结果与 `TokenMetrics` 一并持久化。

**手动 Trace 记录：**

- **`AIAgent.recordTraceMessage()`**：智能体入口方法，自动补全 agentId 和 parentAgentId，写入主会话存储
- **`AgentTraceHolder`**：基于 ThreadLocal 的 Trace 上下文持有者，支持在工具调用、异步执行等跨线程场景中安全记录 Trace；提供 `trace()` 和 `emitterServerEvent()` 方法
- **`AIFlowNode.recordTraceMessage()` / `AIFlowNodeVoid.recordTraceMessage()`**：工作流普通节点支持手动记录 Trace，自动绑定 nodeId 和 parentAgentId

**工作流编排 Trace：**

工作流引擎在关键决策点自动记录 Trace：

- **`AIRouterNodeBuilder`**：AI 路由节点记录路由选择结果（匹配成功/失败、重试次数）
- **`AIKeywordsRouterNodeBuilder`**：关键词路由记录匹配到的智能体 ID 及描述
- **自定义节点**：业务可通过 `recordTraceMessage()` 记录循环控制、条件判断等自定义轨迹（如循环次数、修复标记）

**实时流式推送：**

`ServerEvent` 支持 `TYPE_TRACE = 2` 类型，可将 Trace 信息通过 Flux/SSE 实时推送给前端。流式场景下，路由失败、重试等关键事件可即时反馈到用户界面。

**存储与持久化：**

```
AgentSessionStore（接口）
  ├── AgentSessionStoreMemory（内存存储）
  └── AgentSessionStoreDB（数据库存储）
       └── agent_session_message 表
```

`AgentSessionStoreDB` 自动完成建表，核心字段包括：msgId、createTime、sessionId、parentAgentId、agentId、messageType、seqNo、message（JSON）、role、marks、metadata、requestId、tokenMetrics（JSON）、elapsed、traceId。

Trace 消息与业务消息统一存储在同一张表中，通过 `messageType = 5` 和 `role = trace` 区分，便于按会话维度进行全链路回放和分析。

---

### 3.3 bboss-ai-flow 工作流编排模块

该模块基于 `bboss-datatran-jdbc` 的 JobFlow 引擎，提供智能体工作流编排能力，支持串行、并行、条件分支、路由、判断等多种节点类型。

#### 3.3.1 核心节点类

| 类名 | 作用 |
|------|------|
| `AIPlanAgent` | 流程编排主入口，支持同步 `chat()` 和流式 `chatStream()` 执行，支持定时调度 |
| `AISequenceAgent` | 串行智能体编排，子智能体按顺序依次执行 |
| `AIParrelAgent` | 并行智能体编排，子智能体同时执行，结果聚合后输出 |
| `AIRouteAgent` | 路由智能体，根据用户问题 AI 自主决策后续路由节点 |
| `AIKeywordsRouteAgent` | 关键词路由，基于关键词匹配进行路由选择 |
| `AIJudgeAgent` | 判断节点，调取智能体执行记忆判断输出结果是否满足条件 |
| `AIBaseNodeAgent` | 工作流节点智能体基类 |
| `AIFlowNode` / `AIFlowNodeVoid` | 普通流程节点（非智能体节点），支持手动 Trace 记录 |
| `AIContainerAgent` | 容器智能体接口，定义节点添加和条件分支管理方法 |
| `AINodeAgent` | 节点智能体接口 |
| `StandaloneAgent` | 独立智能体，封装单个 AIAgent 的独立执行 |
| `UserNodeAgent` | 用户节点，处理用户输入交互 |
| `AppendToParentAgent` | 追加到父智能体的节点 |
| `RouteChoice` | 路由选择项，封装路由分支的智能体 ID 和描述 |
| `MarkdownJsonExtractor` | Markdown JSON 提取器，从 Markdown 响应中提取 JSON 结构化数据 |
| `DynamicParrelAgentBuilder` | 动态并行智能体构建器 |
| `ParrelAgentSessionStoreMemory` | 并行智能体内存会话存储 |
| `SequenceAgentSessionStoreMemory` | 串行智能体内存会话存储 |

#### 3.3.2 工作流构建器

| 类名 | 作用 |
|------|------|
| `AIJobFlowBuilder` | JobFlow 工作流构建器 |
| `AIJobFlow` | AI 工作流执行体 |
| `AIAgentNodeBuilder` | 智能体节点构建器 |
| `AISequenceJobFlowNodeBuilder` | 串行节点构建器 |
| `AIParrelJobFlowNodeBuilder` | 并行节点构建器 |
| `AIRouterNodeBuilder` | 路由节点构建器 |
| `AIKeywordsRouterNodeBuilder` | 关键词路由节点构建器 |
| `AIJudgeNodeBuilder` | 判断节点构建器 |
| `AIFlowNodeBuilder` / `AIFlowNodeVoidBuilder` | 普通流程节点构建器 |
| `AIBaseNodeBuilder` | 工作流节点基础构建器 |

#### 3.3.3 提示词工程与外部资源加载

`bboss-ai-flow` 模块内置了强大的提示词变量解析和外部资源加载能力，支持在提示词中嵌入动态变量和引用外部资源文件。

**核心组件：**

| 类名 | 作用 |
|------|------|
| `PromptEval` | 提示词变量解析与求值引擎 |
| `PromptResourceCache` | 外部资源缓存管理器（文件、classpath、URL） |
| `ClasspathResourceReader` | classpath 和 URL 资源读取工具 |

**变量语法：**

提示词中使用 `#[变量名]` 或 `#[变量名, 属性1=值1, 属性2=值2]` 声明变量：

```
你是一个专业的技术文档助手，请参考以下资料回答问题：
#[docs, type=resource, scope=flow, charset=UTF-8]

用户问题：#[query, scope=node]
```

**变量属性说明：**

| 属性 | 可选值 | 说明 |
|------|--------|------|
| `scope` | `flow`（默认）/ `container` / `node` | 变量作用域 |
| `type` | `text`（默认）/ `file` / `resource` / `url` | 变量类型 |
| `charset` | `UTF-8`（默认） | 字符集编码 |

**变量类型说明：**

- **`text`**：普通文本变量，从对应作用域的上下文中获取变量值
- **`file`**：文件类型，变量名代表文件路径，自动读取文件内容替换
- **`resource`**：classpath 资源类型，变量名代表 classpath 下的资源路径，自动读取内容替换
- **`url`**：URL 类型，变量名代表 URL 地址，自动获取远程资源内容替换

**作用域说明：**

- **`flow`**：流程级变量，从工作流全局上下文中获取
- **`container`**：容器级变量，从当前容器（如 AISequenceAgent/AIParrelAgent）上下文中获取
- **`node`**：节点级变量，从当前执行节点上下文中获取

**外部资源缓存：**

`PromptResourceCache` 对加载的外部资源进行单例缓存，避免重复读取：
- `fileCache`：文件资源缓存
- `resourceCache`：classpath 资源缓存
- `urlCache`：URL 资源缓存

**嵌套解析与防循环：**

`PromptEval` 支持递归解析外部资源中引用的变量，但禁止嵌套引用同一资源（防止循环依赖），检测到嵌套引用会抛出 `AIRuntimeException`。

**使用示例：**

```java
// PromptEval 在工作流节点执行时自动调用
AIPlanAgent planAgent = new AIPlanAgent(storeContext);
planAgent.setPrompt("请根据文档回答问题：#[doc, type=resource, charset=UTF-8]");
// doc 变量会自动从 classpath 加载对应资源文件内容
```

---

#### 3.3.4 流程编排示例

**串行编排：**

```java
AIPlanAgent planAgent = new AIPlanAgent(storeContext);
planAgent.addAgent(new AIAgent("步骤1：提取关键词"))
         .addAgent(new AIAgent("步骤2：生成摘要"))
         .addAgent(new AIAgent("步骤3：输出结果"));
LastSessionMessage result = planAgent.chat();
```

**并行编排：**

```java
AIPlanAgent planAgent = new AIPlanAgent(storeContext);
AIParrelAgent parrelAgent = new AIParrelAgent(planAgent);
parrelAgent.addAgent(new AIAgent("并行任务A"))
           .addAgent(new AIAgent("并行任务B"))
           .addAgent(new AIAgent("并行任务C"));
planAgent.addAgent(parrelAgent);
LastSessionMessage result = planAgent.chat();
```

**条件分支编排：**

```java
AIPlanAgent planAgent = new AIPlanAgent(storeContext);
planAgent.addConditionFlowNode(new AIAgent("条件分支A"), context -> {
    return "A".equals(context.getFlowContextData("type"));
});
planAgent.addConditionFlowNode(new AIAgent("条件分支B"), context -> {
    return "B".equals(context.getFlowContextData("type"));
});
```

**路由编排：**

```java
AIPlanAgent planAgent = new AIPlanAgent(storeContext);
AIRouteAgent routeAgent = new AIRouteAgent("根据问题选择处理方向");
routeAgent.addRoutingChoice("agentA", "处理技术问题")
          .addRoutingChoice("agentB", "处理业务问题");
planAgent.addAgent(routeAgent);
planAgent.addRouteChoiceAgent(new AIAgent("技术处理"));
planAgent.addDefaultRouteChoiceAgent(new AIAgent("默认处理"));
```

---

## 四、架构设计特点

### 4.1 分层架构

```
┌─────────────────────────────────────────┐
│        AIPlanAgent (工作流编排层)         │
│   (AISequence/AIParrel/AIRoute/...)    │
├─────────────────────────────────────────┤
│           AIAgent (入口层)               │
├─────────────────────────────────────────┤
│         AgentAdapter (适配层)            │
│    (Deepseek/Kimi/Qwen/...)            │
├─────────────────────────────────────────┤
│         AgentMessage (模型层)            │
│  (Chat/Image/Audio/Video/Tool)         │
├─────────────────────────────────────────┤
│      AgentSessionStore (会话层)          │
│    (Memory/DB 持久化)                   │
├─────────────────────────────────────────┤
│         AIAgentUtil (工具层)             │
├─────────────────────────────────────────┤
│    HttpRequestProxy (HTTP 传输层)        │
│    (bboss-http5 负载均衡组件)            │
├─────────────────────────────────────────┤
│         Reactor Flux (响应式层)          │
└─────────────────────────────────────────┘
```

### 4.2 设计模式

1. **适配器模式（Adapter）**：通过 `AgentAdapter` 适配不同 AI 平台的接口差异
2. **工厂模式（Factory）**：`AgentAdapterFactory` 根据配置创建对应适配器
3. **构建者模式（Builder）**：`MessageBuilder`、`StreamDataBuilder`、工作流构建器构建复杂对象
4. **策略模式（Strategy）**：`StreamDataHandler` 处理不同类型的流数据；`ToolSearcher` 提供多种工具搜索策略
5. **责任链模式（Chain of Responsibility）**：工作流节点按链条顺序执行，条件分支决定流转方向
6. **组合模式（Composite）**：`AISequenceAgent`、`AIParrelAgent` 作为容器组合多个子智能体

### 4.3 负载均衡与容错

基于 `bboss-http5` 组件提供企业级 HTTP 调用能力：

- **负载均衡**：RoundRobin 算法
- **健康检查**：自动检测服务节点健康状态
- **故障恢复**：服务容灾和自动故障转移
- **服务发现**：支持 Apollo、Nacos 等配置中心
- **主备路由**：主节点故障时自动切换到备用节点

### 4.4 流式处理机制

```
用户请求 → AIAgentUtil.streamChatCompletionEvent()
              ↓
    创建 Flux.create(sink -> {...})
              ↓
    HTTP 请求 → 流式响应处理
              ↓
    StreamDataHandler.handle() 逐行处理
              ↓
    AIResponseUtil.handleServerEventData() 解析数据
              ↓
    Flux 流返回给调用方
```

### 4.5 工作流编排机制

基于 `bboss-datatran-jdbc` 的 JobFlow 引擎实现：

```
用户请求 → AIPlanAgent.chat() / chatStream()
              ↓
    AIJobFlowBuilder 构建工作流
              ↓
    JobFlow.execute() 执行工作流
              ↓
    按节点类型执行：
      - AISequenceAgent：顺序执行子节点
      - AIParrelAgent：并行执行子节点，聚合结果
      - AIRouteAgent：AI 决策后路由到指定分支
      - AIJudgeAgent：判断条件后决定流转
              ↓
    结果汇总 → 会话存储 → 返回给用户
```

**工作流特点：**
- 支持同步执行和流式输出
- 支持定时调度（cron 表达式）
- 父子智能体间会话记忆自动传递
- 条件分支支持脚本触发器和默认分支
- 主干流程管理支持动态追加条件分支

### 4.6 会话管理机制

```
AIAgent
  ↓
AgentSessionStoreBuilder（构建器）
  ↓
AgentSessionStore（主存储）
  ├── AgentSessionStoreMemory（内存）
  └── AgentSessionStoreDB（数据库）
       ├── MySQL / Oracle / DM / SQL Server / PostgreSQL / SQLite
       └── ClickHouse 分布式集群（本地表 + 分布式表）
       ↓
  子智能体会话隔离
       ↓
  LastSessionMessage（消息引用链）
```

**会话管理特点：**
- 支持内存和数据库两种持久化方式，数据库支持 7 种数据库方言
- ClickHouse 模式提供生产级分布式存储能力，支持分片和副本
- 父子智能体会话记忆自动加载和传递
- 可配置是否引用父智能体历史消息
- 支持会话大小限制和历史消息裁剪
- 支持 Token 用量统计和追踪
- 提供独立的 `AgentSessionService` 会话管理 API（查询、删除、存在判断）
- 支持多领域会话联合查询、标题模糊查询、时间范围查询
- 查询排序字段和时间条件字段可配置（`createTime` / `lastAccessTime`）
- ClickHouse 模式下会话续问续答时不更新最后访问时间（避免高频 UPDATE）

### 4.7 工具扩展机制

**Function Calling 工具：**
- 通过 `ToolsRegist` 动态注册工具
- 通过 `ToolSearcher` 按 query 过滤工具，减少上下文占用
- 通过 `@Tool` / `@ToolParam` 注解自动解析 Bean 方法为工具

**MCP 协议工具：**
- 支持 SSE 和 Streamable HTTP 两种传输模式
- 支持飞书 MCP 集成
- 支持作为 MCP 服务端对外提供工具

**Skill 技能模块：**
- 通过 `SKILL.md` 文件 + Front Matter 定义技能
- `SkillUtils` 加载技能文件，`SkillsToolRegist` 聚合为单个工具暴露给模型
- `SkillFilter` 支持技能过滤
- 所有技能内容通过提示词传递给模型，由模型自主决策执行步骤

### 4.8 可观测性机制

bboss-ai 的可观测性体系以会话存储为数据底座，以 Trace 消息为观测单元，实现了从底层模型调用到上层工作流编排的全链路覆盖。

**Trace 数据流：**

```
用户请求 / 工作流执行
  ↓
AIAgentUtil.traceLLMInput() / AIAgent.recordTraceMessage()
  ↓
TraceMessage（携带 TokenMetrics）
  ↓
AgentSessionStore.recordTraceMessage()
  ↓
BaseAgentSessionStore → PersistentMessage
  ↓
  ├── AgentSessionStoreMemory → 内存会话列表
  └── AgentSessionStoreDB → agent_session_message 表
              ↓
      按 sessionId + agentId 维度查询与回放
```

**自动采集点：**

框架在以下关键节点自动记录 Trace，业务无需干预：

1. **模型调用前**：`traceLLMInput()` 记录完整请求报文（LLM、Embedding、Rerank）
2. **模型调用后**：`traceLLMOutput()` 记录完整响应报文
3. **流式响应结束**：`BaseStreamDataBuilder` 汇总 Token 用量并持久化
4. **路由决策**：`AIRouterNodeBuilder` / `AIKeywordsRouterNodeBuilder` 记录路由选择结果
5. **工具调用**：`AgentTraceHolder` 在工具执行线程中记录调用轨迹

**实时观测：**

流式模式下，Trace 事件通过 `ServerEvent.TYPE_TRACE` 进入 Flux 流，前端可实时接收并展示：
- 路由匹配过程与重试状态
- 工具调用中间结果
- 循环控制与条件分支执行轨迹

**持久化与查询：**

所有 Trace 消息与业务消息统一存储在 `agent_session_message` 表中，通过 `messageType` 和 `role` 区分类型。借助 `traceId` 和 `parentAgentId` 可构建完整的调用树，支持：
- 单会话全链路回放
- 父子智能体调用链分析
- Token 用量与耗时统计
- 异常定位与故障排查

---

## 五、技术栈

| 技术 | 版本/说明 | 用途 |
|------|----------|------|
| Java | 兼容 JDK 8+ | 开发语言 |
| Apache HttpClient5 | 5.x | HTTP 客户端 |
| Apache HttpCore5 | 5.x | HTTP 核心 |
| Project Reactor | 3.x | 响应式编程 |
| Jackson | 2.22.1 | JSON 处理 |
| bboss-http5 | 6.5.5 | 负载均衡 HTTP 组件 |
| bboss-core-entity | 6.3.5 | 基础实体类 |
| bboss-datatran-jdbc | 7.5.7 | 工作流引擎（JobFlow） |
| bboss-feishu | 6.5.5 | 飞书集成 |
| bboss-persistent | 6.3.5 | 数据库持久化 |
| bboss-data | 6.3.8 | 数据处理 |
| flexmark | 0.64.8 | Markdown 处理 |
| Groovy（可选） | 4.0.28 | 脚本支持 |
| Gradle | 构建工具 | 项目构建 |

---

## 六、使用示例

### 6.1 流式智能问答

```java
AIAgent agent = new AIAgent();
ChatAgentMessage message = new ChatAgentMessage();
message.setPrompt("你好，请介绍一下自己");
message.setModel("deepseek-chat");

Flux<ServerEvent> flux = agent.streamChat("maasName", message);
flux.subscribe(event -> {
    System.out.print(event.getData());
});
```

### 6.2 图片识别

```java
ImageVLAgentMessage message = new ImageVLAgentMessage();
message.setPrompt("描述这张图片");
message.setModel("qwen-vl-plus");
message.addImageUrl("https://example.com/image.jpg");

Flux<ServerEvent> flux = agent.streamImageParser("maasName", message);
```

### 6.3 工具调用

```java
ChatAgentMessage message = new ChatAgentMessage();
message.setPrompt("杭州今天天气怎么样？");
message.setTools(toolDefines);  // 设置工具定义
message.registFunctionCall("get_weather", new WeatherFunctionCall());

ServerEvent result = agent.chat("maasName", message);
```

### 6.4 向量嵌入

```java
AIAgent agent = new AIAgent();
EmbeddingMessage embeddingMessage = new EmbeddingMessage();
embeddingMessage.setModel("bge-large-zh");
embeddingMessage.setInput("这是一段需要嵌入的文本");

float[] embeddings = agent.embedding(embeddingMessage);
```

### 6.5 重排序

```java
AIAgent agent = new AIAgent();
RerankMessage rerankMessage = new RerankMessage();
rerankMessage.setModel("bge-reranker-large");
rerankMessage.setQuery("查询问题");
rerankMessage.setDocuments(documents);

List<RerankedDocument> results = agent.rerank(rerankMessage);
```

### 6.6 工作流编排

```java
StoreContext storeContext = new StoreContext();
storeContext.setSessionId("session-001");

AIPlanAgent planAgent = new AIPlanAgent(storeContext);
planAgent.setPrompt("分析以下文本并生成摘要和关键词");

// 添加串行智能体节点
planAgent.addAgent(new AIAgent("提取关键词"))
         .addAgent(new AIAgent("生成摘要"))
         .addAgent(new AIAgent("输出最终结果"));

// 同步执行
LastSessionMessage result = planAgent.chat();
System.out.println(result.getData());

// 流式执行
Flux<ServerEvent> flux = planAgent.chatStream();
flux.subscribe(event -> System.out.print(event.getData()));
```

### 6.7 Bean 工具注册

```java
public class WeatherService {
    @Tool(name = "get_weather", description = "获取指定城市的天气")
    public String getWeather(
        @ToolParam(description = "城市名称") String city
    ) {
        return "晴天，25°C";
    }
}

AIAgent agent = new AIAgent();
agent.registBeanTool(new WeatherService());

ChatAgentMessage message = new ChatAgentMessage();
message.setPrompt("北京今天天气怎么样？");
ServerEvent result = agent.chat("maasName", message);
```

### 6.8 Skill 使用

bboss-ai 的技能（Skill）通过 `SKILL.md` 文件 + Front Matter 定义，由 `SkillUtils` 加载，`SkillsToolRegist` 聚合为单个 `Skill` 工具暴露给模型。`Skill` 类包含 `name`、`description`、`basePath`、`frontMatter`（扩展属性）、`content`（技能内容）等属性。

**SKILL.md 文件示例**：

```markdown
---
name: code-review
description: 代码审查技能，分析代码质量问题
---

## 步骤
1. 读取待审查的代码文件
2. 分析代码结构和逻辑
3. 识别潜在问题
4. 生成审查报告
```

**注册 Skill 工具**：

```java
AIAgent agent = new AIAgent();

// 加载技能目录下的所有 SKILL.md 文件
SkillsToolRegist skillToolsRegist = new SkillsToolRegist();
skillToolsRegist.setBasePath("/path/to/skills");  // 技能目录路径
agent.setToolsRegist(skillToolsRegist);

ChatAgentMessage message = new ChatAgentMessage();
message.setPrompt("请审查这段代码的质量");
message.setModel("deepseek-chat");

Flux<ServerEvent> flux = agent.streamChat("maasName", message);
```

### 6.9 多次调用工具完成复杂任务

以下示例演示如何让智能体自动执行多步骤运维任务（获取系统信息、查找端口进程、关闭进程并核对结果）。

**内存存储模式：**

```java
AIAgent agent = new AIAgent();
agent.setEnableLoopToolCall(true);  // 启用循环工具调用
agent.setMaxLoopToolCalls(80);      // 最大循环 80 次

// 注册获取操作系统信息工具
agent.registBeanTool(new GetOSFunctionTool(60));
// 注册脚本执行工具（根据 OS 类型自动选择 cmd/sh）
agent.registBeanTool(new CLIShellFunctionTool(60));

ChatAgentMessage message = new ChatAgentMessage();
message.setModel("deepseek-v4-pro");
message.setMaas("deepseek");
message.setRetry(3);
// 使用外部资源文件定义多步骤任务
message.setPrompt("#[loopprompt.txt,type=resource]");
message.setSystemPrompt("你是一个专家，可以根据用户要求生成符合要求的、完整的、可执行的 shell 脚本" +
        "，并将生成的脚本交由工具执行，输出执行结果。注意事项：通过 Java Process 调用 cmd 或者 sh 来执行脚本，" +
        "确保脚本在目标操作系统上能够正常运行。");

Flux<ServerEvent> flux = agent.streamChat(message);
flux.subscribe(event -> System.out.print(event.getData()));
```

**DB 持久化模式：**

```java
// 初始化数据库连接池
SQLUtil.startPool("visualops", "com.mysql.cj.jdbc.Driver",
    "jdbc:mysql://localhost:3306/bboss?useUnicode=true&characterEncoding=utf-8&useSSL=false",
    "root", "123456", "select 1");

ChatAgentMessage message = new ChatAgentMessage();
// ... 模型参数与内存模式相同
message.setPrompt("#[loopprompt.txt,type=resource]");
message.setSystemPrompt("...");

// 配置数据库存储上下文
message.setStoreContext(new StoreContext()
    .setUserId("user123")
    .setSessionSize(100)
    .setRequestId("request123")
    .setStoreType(StoreContext.STORE_TYPE_DB)
    .setDataSource("visualops"));

AIAgent agent = new AIAgent();
agent.setEnableLoopToolCall(true);
agent.setMaxLoopToolCalls(80);
agent.registBeanTool(new GetOSFunctionTool(60));
agent.registBeanTool(new CLIShellFunctionTool(60));

Flux<ServerEvent> flux = agent.streamChat(message);
```

**提示词资源文件（loopprompt.txt）：**

```text
请依次执行以下命令：
1.获取OS版本信息
2.获取CPU信息
3.打印OS和CPU信息
4.查找端口808的进程
5.如果存在对应进程，关闭进程
6.打印端口进程信息和关闭核对结果
```

**涉及工具说明：**

| 工具类 | 作用 |
|--------|------|
| `GetOSFunctionTool` | 获取当前操作系统名称，辅助生成适配的脚本命令 |
| `CLIShellFunctionTool` | 根据 OS 类型自动选择 `cmd /c` 或 `sh -c` 执行脚本，支持超时控制（秒） |

### 6.10 ClickHouse 生产级会话存储

以下示例演示如何基于 ClickHouse 分布式集群实现生产级会话持久化，适用于海量会话数据的高吞吐写入场景。

**前置准备：**

1. ClickHouse 集群已部署（如 `vops_3shards_1replicas`），每个节点已配置 `shard` 和 `replica` 宏变量
2. 通过 bboss 数据源配置文件初始化 ClickHouse 连接池

**完整示例：**

```java
// 1. 初始化 ClickHouse 数据源（visualops）
SQLUtil.startPool("visualops",
    "com.clickhouse.jdbc.ClickHouseDriver",
    "jdbc:clickhouse://127.0.0.1:8123/bboss",
    "default", "", "select 1");

String sessionId = "session-clickhouse-001";

// 2. 配置 ClickHouse 存储上下文
StoreContext storeContext = new StoreContext()
    .setSessionId(sessionId)
    .setUserId("user123")
    .setSessionSize(100)
    .setStoreType(StoreContext.STORE_TYPE_DB)
    .setRequestId("request123")
    .setClickhouseCluster("vops_3shards_1replicas")  // ClickHouse 集群名称，必填
    .setDataSource("visualops")
    .setDomain("ops");

// 3. 构建消息并执行
ChatAgentMessage message = new ChatAgentMessage();
message.setPrompt("帮我分析系统日志，查找异常并生成处理建议");
message.setModel("deepseek-chat");
message.setMaas("deepseek");
message.setRetry(3);
message.setStoreContext(storeContext);

AIAgent agent = new AIAgent();
agent.setEnableLoopToolCall(true);
agent.setMaxLoopToolCalls(80);

// 首次调用：框架自动创建本地表和分布式表
Flux<ServerEvent> flux = agent.streamChat(message);
flux.subscribe(event -> System.out.print(event.getData()));

// 4. 续问续答（复用同一 sessionId，自动加载历史会话记忆）
ChatAgentMessage continueMessage = new ChatAgentMessage();
continueMessage.setPrompt("针对上面发现的问题，给出详细的修复步骤");
continueMessage.setModel("deepseek-chat");
continueMessage.setMaas("deepseek");
continueMessage.setStoreContext(storeContext);

Flux<ServerEvent> flux2 = agent.streamChat(continueMessage);
flux2.subscribe(event -> System.out.print(event.getData()));
```

**会话管理服务查询示例：**

```java
// 创建会话管理服务
AgentSessionServiceImpl sessionService = new AgentSessionServiceImpl();
sessionService.setDatasource("visualops");

// 判断会话是否存在
boolean exists = sessionService.existAgentSession(sessionId);

// 分页查询会话列表（支持多领域联合查询）
AgentSessionCondition condition = new AgentSessionCondition();
condition.setUserId("user123");
condition.setDomains(new String[]{"ops", "dev", "prod"});
condition.setTitle("%异常%");
condition.setTimeConditionField("createTime");  // 时间字段
condition.setTimeStart(startDate);
condition.setTimeEnd(endDate);
condition.setSortKey("createTime");
condition.setSortDesc(true);

ListInfo result = sessionService.queryListInfoAgentSessions(condition, 0, 20);
List<AgentSession> sessions = result.getDatas();

// 查询会话消息
List<SessionMessage> messages = sessionService.queryListSessionMessages(sessionId);

// 删除会话（事务操作，同时删除会话、消息、引用关系）
sessionService.deleteAgentSession(sessionId);
```

**注意事项：**

- ClickHouse 模式下 `clickhouseCluster` 必须设置，否则建表 SQL 会报错；如果表已经创建好可以不设置
- ClickHouse 模式下会话续问续答时不会更新 `lastAccessTime`，因此排序和查询建议使用 `createTime`
- 会话消息表分布式表采用 `sipHash64(sessionId)` 分片，确保同一会话消息落在同一分片
- 建表 SQL 通过 `ON CLUSTER` 在集群所有节点执行，首次调用时自动完成建表

---

## 七、总结

bboss-ai 是一个功能完善的 Java AI 智能体开发框架，具有以下特点：

1. **多平台支持**：统一适配国内主流大模型平台及 OpenAI 兼容接口
2. **多模态能力**：支持文本、图片、音频、视频、Embedding、Rerank 的全方位处理
3. **流式响应**：基于 Reactor 实现真正的流式调用
4. **企业级特性**：内置负载均衡、故障转移、服务发现、会话持久化
5. **工作流编排**：支持串行、并行、条件分支、路由、判断、关键词路由等丰富的节点类型
6. **工具扩展**：支持 Function Calling、MCP 协议（SSE/Streamable HTTP）、Skill 技能模块，支持 `@Tool`/`@ToolParam` 注解快速发布工具服务和 MCP 服务
7. **工具搜索**：支持基于关键词的工具过滤，减少上下文占用
8. **多轮工具调用**：支持智能体自主决策多步骤任务执行，默认最大 80 轮
9. **人工介入（HitL）**：支持单节点内存共享和集群 Redis 发布/订阅两种模式，内置 `HitlTaskcallTool` 和自定义 Hitl 工具扩展
10. **工具审计**：内置 `Auditor` 审计系统，支持工具调用前审计拦截，适用于敏感操作审批
11. **生产级会话存储**：支持 ClickHouse 分布式集群，提供高吞吐会话持久化能力
12. **全链路可观测性**：内置 Trace 体系，覆盖 LLM 调用、工具执行、工作流编排全链路
13. **内置工具体系**：Shell 执行、代码执行（Java/Python/JavaScript）、文件操作、系统信息查询、文本搜索（Grep）、人工介入
14. **轻量级设计**：模块化结构，依赖精简

该框架适合需要集成多种 AI 能力的 Java 企业级应用使用。
