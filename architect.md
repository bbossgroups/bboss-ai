# bboss-ai 项目架构分析

## 一、项目概述

**bboss-ai** 是一个轻量级 Java AI Agent 开发客户端，基于 Apache HttpClient5、HttpCore5 以及 Project Reactor 构建。该项目提供了对大语言模型（LLM）和多模态模型的统一对接能力，支持同步调用和流式调用两种模式，并内置智能体工作流编排、会话管理、工具搜索等企业级特性。

![image-20260302140629787](/architect.png)

### 核心功能
- 智能问答（Chat Completion）
- 图片识别与生成（Vision/Image Generation）
- 语音识别与合成（Speech-to-Text/Text-to-Speech）
- 视频识别与生成（Video Understanding/Generation）
- 向量嵌入（Embedding）
- 重排序（Rerank）
- 工具调用（Function Calling）
- MCP（Model Context Protocol）服务发现和调用，支持 SSE 和 Streamable HTTP 两种传输模式
- 智能体工作流编排，支持串行、并行、条件分支、路由、判断等节点类型
- Skills 技能模块，支持沙箱隔离和动态加载
- 工具搜索（Tool Searcher），支持基于关键词和语义的工具过滤
- 会话管理，支持内存存储和数据库持久化
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
│       ├── model/           # 核心模型定义
│       ├── mcp/model/       # MCP 协议模型
│       ├── skill/           # Skill 技能接口
│       └── tools/           # 工具注册接口
│
├── bboss-ai/                # 核心实现模块
│   └── src/main/java/org/frameworkset/spi/
│       ├── ai/
│       │   ├── AIAgent.java           # 智能体主入口类
│       │   ├── UserAgent.java         # 用户代理类
│       │   ├── adapter/               # 平台适配器
│       │   ├── callback/              # 回调接口（ChatContext、AgentOutput）
│       │   ├── material/              # 素材处理（文件下载等）
│       │   ├── mcp/                   # MCP 客户端实现
│       │   ├── model/                 # 消息模型
│       │   ├── store/                 # 会话存储（内存 + DB）
│       │   ├── tool/                  # 工具注册与搜索
│       │   └── util/                  # 工具类
│       └── reactor/                   # Reactor 流式处理组件
│
├── bboss-ai-flow/           # 智能体工作流编排模块
│   └── src/main/java/org/frameworkset/spi/ai/
│       ├── flow/            # 工作流节点定义
│       ├── prompt/          # Prompt 资源管理
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

#### Skill 技能模块

| 类名 | 作用 |
|------|------|
| `Skill` | Skill 运行时接口，代表一个可执行的能力包 |
| `SkillDefine` | Skill 定义元数据 |
| `SkillRegist` | Skill 注册接口 |
| `SandboxContext` | 沙箱上下文，隔离 Skill 运行环境 |
| `SandboxPolicy` | 沙箱安全策略 |

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
- 飞书 MCP 集成（`FeishuMCPClient`、`FeishuMCPStreamableClient`）

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

`AgentSessionStore` 体系提供智能体会话的存储和管理能力：

| 类名 | 作用 |
|------|------|
| `AgentSessionStore` | 会话存储接口 |
| `AgentSessionStoreMemory` | 内存会话存储 |
| `AgentSessionStoreDB` | 数据库持久化会话存储 |
| `AgentSessionStoreBuilder` | 会话存储构建器 |
| `AgentSessionService` | 会话管理服务接口 |
| `SessionMessage` | 会话消息实体 |
| `LastSessionMessage` | 最新会话消息 |
| `StoreContext` | 存储上下文 |

#### 3.2.7 工具注册与搜索

| 类名 | 作用 |
|------|------|
| `ToolSearcher` | 工具搜索接口，根据 query 筛选相关工具 |
| `KeywordToolSearcher` | 基于关键词的工具搜索实现 |
| `BeanToolsRegist` | Bean 工具注册 |
| `BeanToolFunctionCall` | Bean 工具函数调用 |
| `BaseBeanToolFunctionCall` | Bean 工具调用基类 |
| `BeanToolHandle` | Bean 工具解析处理 |

#### 多次调用工具（Loop Tool Call）

对于需要多步骤执行的复杂任务，智能体可能需要**多次调用工具**才能完成。bboss-ai 内置了循环工具调用机制，允许模型根据前序工具执行结果，自动决策是否继续调用下一个工具。

**核心 API：**

| 方法 | 说明 |
|------|------|
| `AIAgent.setEnableLoopToolCall(boolean)` | 启用/禁用循环工具调用，默认 `false` |
| `AIAgent.setMaxLoopToolCalls(int)` | 设置最大循环次数，防止无限循环 |

**典型使用场景：**
- 运维场景：获取 OS 信息 → 生成脚本 → 执行命令 → 核对结果
- 数据分析：查询数据 → 清洗处理 → 统计计算 → 生成图表
- 业务流程：参数校验 → 调用服务 A → 根据 A 结果调用服务 B → 汇总输出

**注意事项：**
- 循环调用过程中，每次工具执行结果都会作为上下文反馈给模型
- 模型自主决定何时终止工具调用并输出最终答案
- 建议配合 `setRetry(int)` 使用，增强容错能力

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
| `TokenMetrics` | Token 用量指标，包含 model、totalTokens、promptTokens、completionTokens、completionReasoningTokens、reasoningData、elapsed 等 |
| `SessionMessage` | 会话消息类型体系，定义了 18 种消息类型常量，覆盖用户输入、LLM 输入输出、Embedding、Rerank、工具搜索、MCP 调用、Trace 等 |
| `AgentMessageTypeConvertor` | 角色（role）到 messageType 的转换器，支持用户自定义扩展（101 起） |

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
| `AIJudgeAgent` | 判断节点，调取智能体执行记忆判断输出结果是否满足条件 |
| `AIKeywordsRouteAgent` | 关键词路由，基于关键词匹配进行路由选择 |
| `AIBaseNodeAgent` | 工作流节点智能体基类 |
| `AIFlowNode` | 普通流程节点（非智能体节点） |
| `AIContainerAgent` | 容器智能体接口，定义节点添加和条件分支管理方法 |

#### 3.3.2 工作流构建器

| 类名 | 作用 |
|------|------|
| `AIJobFlowBuilder` | JobFlow 工作流构建器 |
| `AIJobFlow` | AI 工作流执行体 |
| `AIAgentNodeBuilder` | 智能体节点构建器 |
| `AISequenceJobFlowNodeBuilder` | 串行节点构建器 |
| `AIParrelJobFlowNodeBuilder` | 并行节点构建器 |
| `AIRouterNodeBuilder` | 路由节点构建器 |
| `AIJudgeNodeBuilder` | 判断节点构建器 |
| `AIFlowNodeBuilder` | 普通流程节点构建器 |

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
AgentSessionStore（主存储）
  ├── AgentSessionStoreMemory（内存）
  └── AgentSessionStoreDB（数据库）
       ↓
  子智能体会话隔离
       ↓
  LastSessionMessage（消息引用链）
```

**会话管理特点：**
- 支持内存和数据库两种持久化方式
- 父子智能体会话记忆自动加载和传递
- 可配置是否引用父智能体历史消息
- 支持会话大小限制和历史消息裁剪
- 支持 Token 用量统计和追踪

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
- 通过 `Skill` 接口封装一组相关工具
- `SandboxContext` 和 `SandboxPolicy` 提供沙箱隔离
- 支持动态加载和卸载

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
| Jackson | 2.x | JSON 处理 |
| bboss-http5 | 6.5.x | 负载均衡 HTTP 组件 |
| bboss-core-entity | 6.5.x | 基础实体类 |
| bboss-datatran-jdbc | 6.5.x | 工作流引擎（JobFlow） |
| bboss-feishu | 6.5.x | 飞书集成 |
| bboss-persistent | 6.5.x | 数据库持久化 |
| flexmark | 0.64.x | Markdown 处理 |
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

```java
public class CalculatorSkill implements Skill {
    @Override
    public SkillDefine getSkillDefine() {
        return new SkillDefine("calculator", "计算器", "提供数学计算能力");
    }

    @Override
    public List<FunctionToolDefine> getToolDefines() {
        // 返回工具定义列表
    }

    @Override
    public FunctionCall getFunctionCall(String toolName) {
        // 返回对应工具的调用实现
    }
}
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

---

## 七、总结

bboss-ai 是一个功能完善的 Java AI 客户端框架，具有以下特点：

1. **多平台支持**：统一适配国内主流大模型平台及 OpenAI 兼容接口
2. **多模态能力**：支持文本、图片、音频、视频、Embedding、Rerank 的全方位处理
3. **流式响应**：基于 Reactor 实现真正的流式调用
4. **企业级特性**：内置负载均衡、故障转移、服务发现、会话持久化
5. **工作流编排**：支持串行、并行、条件分支、路由、判断等丰富的节点类型
6. **工具扩展**：支持 Function Calling、MCP 协议（SSE/Streamable HTTP）、Skill 技能模块
7. **工具搜索**：支持基于关键词和语义的工具过滤，减少上下文占用
8. **轻量级设计**：模块化结构，依赖精简

该框架适合需要集成多种 AI 能力的 Java 企业级应用使用。
