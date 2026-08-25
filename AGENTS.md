# AGENTS.md

本文件为 AI 代理（Coding Agent）在本仓库中协作时提供指引，遵循 [Agent QA 通用规范](https://agents.md/)。

## 项目概述

**bboss-ai** 是一个轻量级多模态 Java 大模型智能体开发框架，基于 Apache HttpClient5、HttpCore5 以及 Project Reactor 构建。支持同步与流式两种调用模式，兼容主流 LLM 与多模态模型平台，并提供智能体工作流编排、会话管理、MCP 协议支持、工具调用、技能（Skills）、人工介入（HitL）、工具审计、全链路 Trace 可观测性等企业级特性。

- **GroupId / ArtifactId**: `com.bbossgroups:bboss-ai`
- **当前版本**: 6.5.6（见 [gradle.properties](gradle.properties)）
- **Java 版本**: 1.8（源码与目标编译级别）
- **License**: Apache License 2.0
- **官方文档**: https://esdoc.bbossgroups.com/#/bboss-ai
- **架构文档**: [architect.md](architect.md)

### 核心功能一览

- 智能问答（Chat Completion）、图片识别/生成、语音识别/合成、视频识别/生成
- 向量嵌入（Embedding）、重排序（Rerank）
- 工具调用（Function Calling），支持 `@Tool`/`@ToolParam` 注解快速发布工具服务和 MCP 服务
- MCP（Model Context Protocol）服务发现和调用，支持 SSE 和 Streamable HTTP 两种传输模式，同时支持客户端和服务端
- 智能体工作流编排，支持串行、并行、条件分支、路由、判断、关键词路由等节点类型
- Skills 技能模块，通过 `SKILL.md` + Front Matter 定义
- 工具搜索（Tool Searcher），支持基于关键词的工具过滤
- 会话管理，支持内存存储和数据库持久化（MySQL、Oracle、达梦 DM、SQL Server、PostgreSQL、SQLite、ClickHouse）
- 多轮工具调用（Loop Tool Call），支持智能体自主决策多步骤任务执行
- 人工介入（HitL，Human-in-the-Loop），支持单节点内存共享和集群 Redis 发布/订阅两种模式
- 工具审计系统（Auditor），支持工具调用前审计拦截
- 智能体全链路 Trace 可观测性，覆盖 LLM 调用、工具执行、工作流编排
- 内置工具：Shell 执行、代码执行（Java/Python/JavaScript）、文件操作、系统信息查询、文本搜索（Grep）、人工介入

## 模块结构

项目由三个 Gradle 子模块组成（见 [settings.gradle](settings.gradle)）：

| 模块 | 路径 | 职责 |
| --- | --- | --- |
| `bboss-ai-model` | [bboss-ai-model/](bboss-ai-model/) | 基础模型与接口定义（最小依赖），包含 MCP 协议模型、工具注解（`@Tool`/`@ToolParam`）、`ToolsRegist`、`FunctionCall`、`FunctionToolDefine` 等 |
| `bboss-ai` | [bboss-ai/](bboss-ai/) | 核心实现模块，包含 `AIAgent` 主入口、平台适配器、MCP 客户端/服务端、会话存储、内置工具、技能加载器、HitL 人工介入、审计系统、回调机制、素材处理等 |
| `bboss-ai-flow` | [bboss-ai-flow/](bboss-ai-flow/) | 多智能体工作流编排模块，支持串行、并行、条件分支、路由、判断、关键词路由等节点类型，内置提示词变量解析引擎 |

依赖关系：`bboss-ai` → `bboss-ai-model`；`bboss-ai-flow` → `bboss-ai`。

### bboss-ai 模块核心包结构

| 包路径 | 职责 |
| --- | --- |
| `org.frameworkset.spi.ai` | `AIAgent` 主入口类、`UserAgent` 用户代理类 |
| `org.frameworkset.spi.ai.adapter` | 平台适配器（17 个适配器类） |
| `org.frameworkset.spi.ai.audit` | 工具审计系统（`Auditor`、`AuditContext`、`AuditResult`） |
| `org.frameworkset.spi.ai.callback` | 回调接口（`ChatContext`、`ChatCallback`、`ChatStreamCallback`、`AgentOutput`） |
| `org.frameworkset.spi.ai.hitl` | 人工介入功能（`BaseHitlTaskTool`、`HitlTaskHelper`、`HitlTaskToolInf`、集群 Redis 支持） |
| `org.frameworkset.spi.ai.material` | 素材处理（文件下载、图片/视频处理） |
| `org.frameworkset.spi.ai.mcp` | MCP 客户端/服务端（SSE、Streamable HTTP、飞书 MCP 集成、Spring AI 兼容拦截器） |
| `org.frameworkset.spi.ai.model` | 消息模型（40+ 个消息类） |
| `org.frameworkset.spi.ai.skill` | 技能加载与管理（`SkillUtils`、`SkillsToolRegist`、`Skill`、`SkillFilter`） |
| `org.frameworkset.spi.ai.store` | 会话存储（内存 + DB 持久化，含 `AgentSessionService` 会话管理 API） |
| `org.frameworkset.spi.ai.tool` | 工具注册与搜索（`BeanToolsRegist`、`ToolSearcher`、`KeywordToolSearcher`、`ToolCallContext`、`AgentTraceHolder`） |
| `org.frameworkset.spi.ai.tool.permission` | 工具权限类型（`PermissionType`：ASK/ALLOW/DENY） |
| `org.frameworkset.spi.ai.tools` | 内置工具实现（Shell/代码执行/文件操作/系统信息/文本搜索/HITL） |
| `org.frameworkset.spi.ai.util` | 工具类（`AIAgentUtil`、`AIResponseUtil`、`MessageBuilder` 等） |
| `org.frameworkset.spi.reactor` | Reactor 流式处理组件 |

## 构建与测试

### 环境要求
- JDK 1.8+
- Gradle 8+（建议使用 IDE 内置 Gradle wrapper）

### 常用命令

```bash
# 清理并发布到本地 Maven 仓库（构建发布版本）
gradle clean publishToMavenLocal

# 仅构建 jar（跳过测试，根项目默认 skipTest=true）
gradle clean build

# 运行测试
gradle test

# 仅构建单个模块
gradle :bboss-ai-model:build
gradle :bboss-ai:build
gradle :bboss-ai-flow:build

# 仅运行指定模块的测试
gradle :bboss-ai:test
gradle :bboss-ai-flow:test
```

### 测试说明
- 测试代码位于各模块的 `src/test/java` 目录，包路径与源码一致。
- 测试资源位于 `src/test/resources`，其中：
  - [bboss-ai/src/test/resources/application-stream.properties](bboss-ai/src/test/resources/application-stream.properties) - 各大模型平台连接池配置（含 apiKey，请勿提交真实密钥）
  - [bboss-ai/src/test/resources/mcpserver.properties](bboss-ai/src/test/resources/mcpserver.properties) - MCP 服务端连接配置
  - [bboss-ai/src/test/resources/skills/](bboss-ai/src/test/resources/skills/) - 示例技能目录
- 测试用例多依赖外部大模型服务（deepseek、qwen、kimi 等），无网络/无密钥环境下可能无法直接运行；修改相关代码时请关注网络与密钥依赖。
- 入口示例：[StreamTest.java](bboss-ai/src/test/java/org/frameworkset/spi/ai/StreamTest.java)。

## 代码风格与约定

### 语言与基础规范
- **语言**: Java 1.8（不要使用 9+ 语法，如 `var`、`record`、`switch` 表达式等）。
- **包路径根**: `org.frameworkset.spi.ai.*`（核心模块）、`org.frameworkset.spi.reactor.*`（流式处理组件）。
- **编码**: UTF-8；换行符 LF。
- **注释与文档**: 类与公共方法使用 Javadoc 注释，中文描述业务含义。代码中的中英文混排应保持现有风格。
- **资源组织**: Java 源码与同名资源位于同一 `src/main/java` 目录下（通过 `sourceSets` 配置同时作为 resources），例如 `openai.json` 与适配器同目录，便于按包路径加载。

### 命名约定
- 类名采用大驼峰，方法与字段采用小驼峰。
- 适配器命名: `XxxAgentAdapter`（继承 [AgentAdapter](bboss-ai/src/main/java/org/frameworkset/spi/ai/adapter/AgentAdapter.java)）。
- 消息模型命名: `XxxAgentMessage`（继承 [AgentMessage](bboss-ai/src/main/java/org/frameworkset/spi/ai/model/AgentMessage.java)）。
- 工作流节点: `AIXxxAgent` / `AIXxxNodeBuilder`（见 [bboss-ai-flow](bboss-ai-flow/src/main/java/org/frameworkset/spi/ai/flow/)）。
- 配置 key 前缀: `http.poolNames` 列出所有服务名，每个服务以 `{name}.http.*` 形式定义参数（hosts、apiKeyId、modelType、extendConfigs 等）。

### 关键设计约定
- **适配器模式**: 所有 AI 平台对接通过 [AgentAdapter](bboss-ai/src/main/java/org/frameworkset/spi/ai/adapter/AgentAdapter.java) 抽象基类实现，新增平台需继承并实现请求构建/响应解析方法，通过 `modelType` 注册到 [AgentAdapterFactory](bboss-ai/src/main/java/org/frameworkset/spi/ai/adapter/AgentAdapterFactory.java)。
- **流式响应**: 基于 Project Reactor 的 `Flux<ServerEvent>` / `FluxSink<ServerEvent>`；同步调用复用流式处理并阻塞聚合结果。
- **会话存储**: 通过 [StoreContext](bboss-ai/src/main/java/org/frameworkset/spi/ai/store/StoreContext.java) 配置存储类型（`memory` 或 `db`），由 [DefaultAgentSessionStoreBuilder](bboss-ai/src/main/java/org/frameworkset/spi/ai/store/DefaultAgentSessionStoreBuilder.java) 创建对应实现。DB 模式支持 MySQL/Oracle/DM/SQL Server/PostgreSQL/SQLite/ClickHouse。提供独立的 [AgentSessionService](bboss-ai/src/main/java/org/frameworkset/spi/ai/store/AgentSessionService.java) 会话管理 API（查询、删除、存在判断、分页查询）。
- **工具调用**: 使用 `@Tool` / `@ToolParam` 注解标注工具方法，通过 `BeanToolsRegist` 注册；MCP 工具使用 `MCPToolsRegist`，关联 `mcpserver.properties` 中的连接池名。工具调用上下文通过 [ToolCallContext](bboss-ai/src/main/java/org/frameworkset/spi/ai/tool/ToolCallContext.java) 传递。
- **工具搜索**: 通过 [ToolSearcher](bboss-ai/src/main/java/org/frameworkset/spi/ai/tool/ToolSearcher.java) 接口按 query 过滤工具，减少上下文占用；[KeywordToolSearcher](bboss-ai/src/main/java/org/frameworkset/spi/ai/tool/KeywordToolSearcher.java) 为基于关键词的实现。
- **工具审计**: 内置工具继承 [BaseAuditorTool](bboss-ai/src/main/java/org/frameworkset/spi/ai/tools/BaseAuditorTool.java)，通过 [Auditor](bboss-ai/src/main/java/org/frameworkset/spi/ai/audit/Auditor.java) 接口实现工具调用前审计拦截，返回非 null 结果可阻止工具执行。
- **MCP 协议**: 同时支持客户端（`MCPSSEClient` / `MCPStreamableClient`）与服务端（`MCPToolServiceImpl`），两种传输模式（SSE 与 Streamable HTTP）。支持飞书 MCP 集成（`FeishuMCPClient`、`FeishuMCPStreamableClient`）。提供 Spring AI MCP 兼容请求拦截器 [SpringAIMcpRequestIntercepter](bboss-ai/src/main/java/org/frameworkset/spi/ai/mcp/intercepter/SpringAIMcpRequestIntercepter.java)。
- **技能（Skill）**: 通过 `SKILL.md` 文件 + Front Matter 定义，由 [SkillUtils](bboss-ai/src/main/java/org/frameworkset/spi/ai/skill/SkillUtils.java) 加载，[SkillsToolRegist](bboss-ai/src/main/java/org/frameworkset/spi/ai/skill/SkillsToolRegist.java) 聚合为单个 `Skill` 工具暴露给模型。详见 [bboss ai技能(Skills)使用文档.md](bboss%20ai技能(Skills)使用文档.md)。
- **人工介入（HitL）**: 通过 [BaseHitlTaskTool](bboss-ai/src/main/java/org/frameworkset/spi/ai/hitl/BaseHitlTaskTool.java) / [HitlTaskcallTool](bboss-ai/src/main/java/org/frameworkset/spi/ai/tools/HitlTaskcallTool.java) 实现内置 Hitl 工具，亦可继承 `BaseHitlTaskTool` 实现自定义 Hitl 工具。单节点模式通过内存共享数据，集群模式通过 Redis 发布/订阅实现。支持 [HitlAssistant](bboss-ai/src/main/java/org/frameworkset/spi/ai/tools/HitlAssistant.java) 接口提供人工干预辅助信息和处理人工提交数据。详见 [bboss ai人工介入Hitl功能使用文档.md](bboss%20ai人工介入Hitl功能使用文档.md)。
- **多轮工具调用**: 通过 `AIAgent.setEnableLoopToolCall(true)` 启用，`setMaxLoopToolCalls(int)` 设置最大循环次数（默认 80），模型自主决策多步骤任务执行。
- **构建器模式**: `AIAgent`、工作流节点、会话存储等大量采用链式 Builder（如 [AIJobFlowBuilder](bboss-ai-flow/src/main/java/org/frameworkset/spi/ai/flow/AIJobFlowBuilder.java)、[BeanToolFunctionCallBuilder](bboss-ai/src/main/java/org/frameworkset/spi/ai/tool/BeanToolFunctionCallBuilder.java)）。

## 关键入口与参考

| 任务 | 入口 / 参考 |
| --- | --- |
| 智能体主 API | [AIAgent.java](bboss-ai/src/main/java/org/frameworkset/spi/ai/AIAgent.java) |
| 工作流编排主入口 | [AIPlanAgent.java](bboss-ai-flow/src/main/java/org/frameworkset/spi/ai/flow/AIPlanAgent.java) / [AIJobFlow.java](bboss-ai-flow/src/main/java/org/frameworkset/spi/ai/flow/AIJobFlow.java) |
| 平台适配器扩展点 | [AgentAdapter.java](bboss-ai/src/main/java/org/frameworkset/spi/ai/adapter/AgentAdapter.java) / [AgentAdapterFactory.java](bboss-ai/src/main/java/org/frameworkset/spi/ai/adapter/AgentAdapterFactory.java) |
| MCP 客户端实现 | [MCPClient.java](bboss-ai/src/main/java/org/frameworkset/spi/ai/mcp/MCPClient.java) |
| MCP 服务端实现 | [MCPToolServiceImpl.java](bboss-ai/src/main/java/org/frameworkset/spi/ai/mcp/tools/server/MCPToolServiceImpl.java) |
| 会话存储扩展 | [AgentSessionStore.java](bboss-ai/src/main/java/org/frameworkset/spi/ai/store/AgentSessionStore.java) / [StoreContext.java](bboss-ai/src/main/java/org/frameworkset/spi/ai/store/StoreContext.java) |
| 会话管理服务 | [AgentSessionService.java](bboss-ai/src/main/java/org/frameworkset/spi/ai/store/AgentSessionService.java) / [AgentSessionServiceImpl.java](bboss-ai/src/main/java/org/frameworkset/spi/ai/store/db/AgentSessionServiceImpl.java) |
| 人工介入（HitL） | [BaseHitlTaskTool.java](bboss-ai/src/main/java/org/frameworkset/spi/ai/hitl/BaseHitlTaskTool.java) / [HitlTaskHelper.java](bboss-ai/src/main/java/org/frameworkset/spi/ai/hitl/HitlTaskHelper.java) / [HitlTaskToolInf.java](bboss-ai/src/main/java/org/frameworkset/spi/ai/hitl/HitlTaskToolInf.java) |
| 工具审计系统 | [Auditor.java](bboss-ai/src/main/java/org/frameworkset/spi/ai/audit/Auditor.java) / [BaseAuditorTool.java](bboss-ai/src/main/java/org/frameworkset/spi/ai/tools/BaseAuditorTool.java) |
| 工具注册与搜索 | [BeanToolsRegist.java](bboss-ai/src/main/java/org/frameworkset/spi/ai/tool/BeanToolsRegist.java) / [ToolSearcher.java](bboss-ai/src/main/java/org/frameworkset/spi/ai/tool/ToolSearcher.java) |
| 技能加载 | [SkillUtils.java](bboss-ai/src/main/java/org/frameworkset/spi/ai/skill/SkillUtils.java) / [SkillsToolRegist.java](bboss-ai/src/main/java/org/frameworkset/spi/ai/skill/SkillsToolRegist.java) |
| 内置工具 | [CLIShellFunctionTool](bboss-ai/src/main/java/org/frameworkset/spi/ai/tools/CLIShellFunctionTool.java)、[CodeExecuteFunctionTool](bboss-ai/src/main/java/org/frameworkset/spi/ai/tools/CodeExecuteFunctionTool.java)、[FileFunctionTool](bboss-ai/src/main/java/org/frameworkset/spi/ai/tools/FileFunctionTool.java)、[GetOSFunctionTool](bboss-ai/src/main/java/org/frameworkset/spi/ai/tools/GetOSFunctionTool.java)、[GrepFunctionTool](bboss-ai/src/main/java/org/frameworkset/spi/ai/tools/GrepFunctionTool.java)、[HitlTaskcallTool](bboss-ai/src/main/java/org/frameworkset/spi/ai/tools/HitlTaskcallTool.java) |
| 全链路 Trace | [AgentTraceHolder.java](bboss-ai/src/main/java/org/frameworkset/spi/ai/tool/AgentTraceHolder.java) |
| 完整使用示例 | [StreamTest.java](bboss-ai/src/test/java/org/frameworkset/spi/ai/StreamTest.java) |

## 开发注意事项

### 安全与密钥
- **严禁提交真实密钥**: 测试资源中的 `application-stream.properties`、`mcpserver.properties` 含有各平台 apiKey，属于测试样例。新增配置时请使用占位符或脱敏值。
- **GPG 签名**: [gradle.properties](gradle.properties) 中包含 GPG 签名信息用于发布到 Maven Central，不要泄露或修改。
- **高危工具警示**: `CLIShellFunctionTool.executeBash`、`CodeExecuteFunctionTool.executeJava/executePython/executeJavaScript` 会真实执行任意代码，应在隔离容器中使用低权限账户运行。
- **路径安全**: `FileFunctionTool` 和 `GrepFunctionTool` 支持设置 `baseDirectory` 限制操作范围，防止路径穿越攻击。

### 日志与可观测性
- 使用 SLF4J + Log4j2（测试用 [log4j2.xml](bboss-ai/src/test/resources/log4j2.xml)）。
- 智能体全链路 Trace 通过 [AgentTraceHolder](bboss-ai/src/main/java/org/frameworkset/spi/ai/tool/AgentTraceHolder.java) 实现，覆盖 LLM 调用、工具执行、工作流编排。Trace 消息与业务消息统一存储在 `agent_session_message` 表中，通过 `messageType` 和 `role` 区分类型。
- 流式模式下，Trace 事件通过 `ServerEvent.TYPE_TRACE` 进入 Flux 流，前端可实时接收。

### 数据库会话存储
- ClickHouse 模式需指定集群名，并为每个节点定义 `shard` 和 `replica` 宏变量。
- ClickHouse 模式下会话续问续答时**不会**更新 `lastAccessTime`（受限于 ClickHouse 不支持高频 UPDATE）。
- 首次使用时自动建表（`agent_session`、`agent_session_message`、`agent_session_message_ref`），表结构见 [agentSession.xml](bboss-ai/src/main/java/org/frameworkset/spi/ai/store/db/agentSession.xml) 与 [clickhouse-agent.xml](bboss-ai/src/main/java/org/frameworkset/spi/ai/store/db/clickhouse-agent.xml)。
- ClickHouse 模式自动创建本地表（`*_local`，`ReplicatedMergeTree` 引擎）和分布式表（`Distributed` 引擎），消息表采用 `sipHash64(sessionId)` 哈希分片。

### 人工介入（HitL）
- 单节点模式：通过内存共享数据实现智能体中断和唤醒。
- 集群模式：通过 Redis 发布/订阅模式实现数据共享，进行智能体中断和唤醒处理。
- 如果接收人工提交数据的节点就是中断智能体所在的节点，直接通过内存共享数据。
- 内置 `HitlTaskcallTool` 实现 Hitl 功能，亦可继承 `BaseHitlTaskTool` 实现自定义 Hitl 工具。
- 支持任务超时处理（`hitlTaskTimeout`），超时动作可配置为 `continue`（继续执行）或 `rejected`（拒绝执行）。
- 集群模式相关类位于 [hitl/cluster/](bboss-ai/src/main/java/org/frameworkset/spi/ai/hitl/cluster/) 目录（`RedisHitlTaskCallListener`、`RedisHitlTaskCallNotifier`）。

### 工具审计
- 所有内置工具继承 `BaseAuditorTool`，支持通过 `Auditor` 接口在工具调用前进行审计拦截。
- `Auditor.audit(AuditContext)` 返回非 null 的 `AuditResult` 时，工具执行被阻止，审计结果直接返回给模型。
- 适用于敏感操作的审批、权限控制等场景。

### 依赖管理
- 版本统一在 [gradle.properties](gradle.properties) 中定义（如 `BBOSS_VERSION=6.3.5`、`BBOSS_HTTP5_VERSION=6.5.5`、`jacksonversion=2.22.1` 等）。
- `bboss-ai-model` 仅依赖 Jackson 注解与 `bboss-core-entity`，保持最小依赖；新增依赖应慎重，避免污染下游。
- `bboss-ai` 依赖 `bboss-feishu`（飞书集成）、`bboss-persistent`（数据库持久化）、`bboss-datatran-jdbc`（工作流引擎）、`bboss-data`。
- `bboss-ai-flow` 依赖 `flexmark-all`（Markdown 处理）、可选 `groovy`（脚本支持）。

### 文档维护
- 中文文档与代码中文注释需保持同步更新。
- 关键文档：[README.md](README.md)、[architect.md](architect.md)、[bboss ai内置工具使用文档.md](bboss%20ai内置工具使用文档.md)、[bboss ai技能(Skills)使用文档.md](bboss%20ai技能(Skills)使用文档.md)、[bboss ai人工介入Hitl功能使用文档.md](bboss%20ai人工介入Hitl功能使用文档.md)、[bboss ai文件检索工具使用指南.md](bboss%20ai文件检索工具使用指南.md)、[bboss-ai 智能体工作流与多智能体自由组合.md](bboss-ai%20智能体工作流与多智能体自由组合.md)。

## 支持的 AI 平台

通过适配器模式对接以下平台（见 [adapter/](bboss-ai/src/main/java/org/frameworkset/spi/ai/adapter/) 目录）：

- DeepSeek、Kimi（Moonshot）、智谱 AI、阿里百炼/通义千问（Qwen）、字节豆包/火山引擎、百度、硅基流动（Siliconflow）、九天（Jiutian）、MiniMax、腾讯混元、Xinference、OpenAI 兼容接口

新增平台适配器时，参考 [NoneAgentAdapter](bboss-ai/src/main/java/org/frameworkset/spi/ai/adapter/NoneAgentAdapter.java) 与 [OpenaiAgentAdapter](bboss-ai/src/main/java/org/frameworkset/spi/ai/adapter/OpenaiAgentAdapter.java) 的实现方式。

## 联系方式

- 技术交流群: 21220580, 166471282
- 官方文档: https://esdoc.bbossgroups.com/#/bboss-ai
