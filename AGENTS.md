# AGENTS.md

本文件为 AI 代理（Coding Agent）在本仓库中协作时提供指引，遵循 [Agent QA 通用规范](https://agents.md/)。

## 项目概述

**bboss-ai** 是一个轻量级多模态 Java 大模型智能体客户端，基于 Apache HttpClient5、HttpCore5 以及 Project Reactor 构建。支持同步与流式两种调用模式，兼容主流 LLM 与多模态模型平台，并提供智能体工作流编排、会话管理、MCP 协议支持、工具调用、技能（Skills）等企业级特性。

- **GroupId / ArtifactId**: `com.bbossgroups:bboss-ai`
- **当前版本**: 6.5.6（见 [gradle.properties](gradle.properties)）
- **Java 版本**: 1.8（源码与目标编译级别）
- **License**: Apache License 2.0
- **官方文档**: https://esdoc.bbossgroups.com/#/bboss-ai
- **架构文档**: [architect.md](architect.md)

## 模块结构

项目由三个 Gradle 子模块组成（见 [settings.gradle](settings.gradle)）：

| 模块 | 路径 | 职责 |
| --- | --- | --- |
| `bboss-ai-model` | [bboss-ai-model/](bboss-ai-model/) | 基础模型与接口定义（最小依赖），包含 MCP 协议模型、工具注解（`@Tool`/`@ToolParam`）、`ToolsRegist`、`FunctionCall` 等 |
| `bboss-ai` | [bboss-ai/](bboss-ai/) | 核心实现模块，包含 `AIAgent` 主入口、平台适配器、MCP 客户端/服务端、会话存储、内置工具、技能加载器等 |
| `bboss-ai-flow` | [bboss-ai-flow/](bboss-ai-flow/) | 多智能体工作流编排模块，支持串行、并行、条件分支、路由、判断等节点类型 |

依赖关系：`bboss-ai` → `bboss-ai-model`；`bboss-ai-flow` → `bboss-ai`。

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
- **会话存储**: 通过 [StoreContext](bboss-ai/src/main/java/org/frameworkset/spi/ai/store/StoreContext.java) 配置存储类型（`memory` 或 `db`），由 [DefaultAgentSessionStoreBuilder](bboss-ai/src/main/java/org/frameworkset/spi/ai/store/DefaultAgentSessionStoreBuilder.java) 创建对应实现。DB 模式支持 MySQL/Oracle/DM/SQL Server/PostgreSQL/SQLite/ClickHouse。
- **工具调用**: 使用 `@Tool` / `@ToolParam` 注解标注工具方法，通过 `BeanToolsRegist` 注册；MCP 工具使用 `MCPToolsRegist`，关联 `mcpserver.properties` 中的连接池名。
- **MCP 协议**: 同时支持客户端（`MCPSSEClient` / `MCPStreamableClient`）与服务端（`MCPToolServiceImpl`），两种传输模式（SSE 与 Streamable HTTP）。
- **技能（Skill）**: 通过 `SKILL.md` 文件 + Front Matter 定义，由 [SkillUtils](bboss-ai/src/main/java/org/frameworkset/spi/ai/skill/SkillUtils.java) 加载，[SkillsToolRegist](bboss-ai/src/main/java/org/frameworkset/spi/ai/skill/SkillsToolRegist.java) 聚合为单个 `Skill` 工具暴露给模型。详见 [bboss ai技能(Skills)使用文档.md](bboss%20ai技能(Skills)使用文档.md)。
- **构建器模式**: `AIAgent`、工作流节点、会话存储等大量采用链式 Builder（如 [AIJobFlowBuilder](bboss-ai-flow/src/main/java/org/frameworkset/spi/ai/flow/AIJobFlowBuilder.java)、[BeanToolFunctionCallBuilder](bboss-ai/src/main/java/org/frameworkset/spi/ai/tool/BeanToolFunctionCallBuilder.java)）。

## 关键入口与参考

| 任务 | 入口 / 参考 |
| --- | --- |
| 智能体主 API | [AIAgent.java](bboss-ai/src/main/java/org/frameworkset/spi/ai/AIAgent.java) |
| 平台适配器扩展点 | [AgentAdapter.java](bboss-ai/src/main/java/org/frameworkset/spi/ai/adapter/AgentAdapter.java) / [AgentAdapterFactory.java](bboss-ai/src/main/java/org/frameworkset/spi/ai/adapter/AgentAdapterFactory.java) |
| MCP 客户端实现 | [MCPClient.java](bboss-ai/src/main/java/org/frameworkset/spi/ai/mcp/MCPClient.java) |
| MCP 服务端实现 | [MCPToolServiceImpl.java](bboss-ai/src/main/java/org/frameworkset/spi/ai/mcp/tools/server/MCPToolServiceImpl.java) |
| 会话存储扩展 | [AgentSessionStore.java](bboss-ai/src/main/java/org/frameworkset/spi/ai/store/AgentSessionStore.java) / [StoreContext.java](bboss-ai/src/main/java/org/frameworkset/spi/ai/store/StoreContext.java) |
| 工作流编排 | [AIJobFlow.java](bboss-ai-flow/src/main/java/org/frameworkset/spi/ai/flow/AIJobFlow.java) / [AIFlowNode.java](bboss-ai-flow/src/main/java/org/frameworkset/spi/ai/flow/AIFlowNode.java) |
| 技能加载 | [SkillUtils.java](bboss-ai/src/main/java/org/frameworkset/spi/ai/skill/SkillUtils.java) |
| 内置工具 | [CLIShellFunctionTool](bboss-ai/src/main/java/org/frameworkset/spi/ai/tools/CLIShellFunctionTool.java)、[CodeExecuteFunctionTool](bboss-ai/src/main/java/org/frameworkset/spi/ai/tools/CodeExecuteFunctionTool.java)、[FileFunctionTool](bboss-ai/src/main/java/org/frameworkset/spi/ai/tools/FileFunctionTool.java)、[GetOSFunctionTool](bboss-ai/src/main/java/org/frameworkset/spi/ai/tools/GetOSFunctionTool.java) |
| 完整使用示例 | [StreamTest.java](bboss-ai/src/test/java/org/frameworkset/spi/ai/StreamTest.java) |

## 开发注意事项

### 安全与密钥
- **严禁提交真实密钥**: 测试资源中的 `application-stream.properties`、`mcpserver.properties` 含有各平台 apiKey，属于测试样例。新增配置时请使用占位符或脱敏值。
- **GPG 签名**: [gradle.properties](gradle.properties) 中包含 GPG 签名信息用于发布到 Maven Central，不要泄露或修改。
- **沙箱执行**: 技能工具支持 [SandboxContext](bboss-ai-model/src/main/java/org/frameworkset/spi/ai/skill/SandboxContext.java) 隔离运行环境，新增工具时应遵循沙箱策略。

### 日志与可观测性
- 使用 SLF4J + Log4j2（测试用 [log4j2.xml](bboss-ai/src/test/resources/log4j2.xml)）。
- 智能体全链路 Trace 通过 [AgentTraceHolder](bboss-ai/src/main/java/org/frameworkset/spi/ai/tool/AgentTraceHolder.java) 实现，覆盖 LLM 调用、工具执行、工作流编排。

### 数据库会话存储
- ClickHouse 模式需指定集群名，并为每个节点定义 `shard` 和 `replica` 宏变量。
- ClickHouse 模式下会话续问续答时**不会**更新 `lastAccessTime`（受限于 ClickHouse 不支持高频 UPDATE）。
- 首次使用时自动建表（`agent_session`、`agent_session_message`、`agent_session_message_ref`），表结构见 [agentSession.xml](bboss-ai/src/main/java/org/frameworkset/spi/ai/store/db/agentSession.xml) 与 [clickhouse-agent.xml](bboss-ai/src/main/java/org/frameworkset/spi/ai/store/db/clickhouse-agent.xml)。

### 依赖管理
- 版本统一在 [gradle.properties](gradle.properties) 中定义（如 `BBOSS_VERSION`、`jacksonversion` 等）。
- `bboss-ai-model` 仅依赖 Jackson 注解与 `bboss-core-entity`，保持最小依赖；新增依赖应慎重，避免污染下游。
- `bboss-ai` 依赖 `bboss-feishu`（飞书集成）与 `bboss-persistent`（数据库持久化）。

### 文档维护
- 中文文档与代码中文注释需保持同步更新。
- 关键文档：[README.md](README.md)、[architect.md](architect.md)、[bboss ai内置工具使用文档.md](bboss%20ai内置工具使用文档.md)、[bboss ai技能(Skills)使用文档.md](bboss%20ai技能(Skills)使用文档.md)。

## 支持的 AI 平台

通过适配器模式对接以下平台（见 [adapter/](bboss-ai/src/main/java/org/frameworkset/spi/ai/adapter/) 目录）：

- DeepSeek、Kimi（Moonshot）、智谱 AI、阿里百炼/通义千问（Qwen）、字节豆包/火山引擎、百度、硅基流动（Siliconflow）、九天（Jiutian）、MiniMax、腾讯混元、Xinference、OpenAI 兼容接口

新增平台适配器时，参考 [NoneAgentAdapter](bboss-ai/src/main/java/org/frameworkset/spi/ai/adapter/NoneAgentAdapter.java) 与 [OpenaiAgentAdapter](bboss-ai/src/main/java/org/frameworkset/spi/ai/adapter/OpenaiAgentAdapter.java) 的实现方式。

## 联系方式

- 技术交流群: 21220580, 166471282
- 官方文档: https://esdoc.bbossgroups.com/#/bboss-ai
