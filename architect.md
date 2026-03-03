# agent-ai 项目架构分析

## 一、项目概述

**agent-ai** 是一个轻量级 Java AI Agent 开发客户端，基于 Apache HttpClient5、HttpCore5 以及 Project Reactor 构建。该项目提供了对大语言模型（LLM）和多模态模型的统一对接能力，支持同步调用和流式调用两种模式。

![image-20260302140629787](/architect.png)

### 核心功能
- 智能问答（Chat Completion）
- 图片识别与生成（Vision/Image Generation）
- 语音识别与合成（Speech-to-Text/Text-to-Speech）
- 视频识别与生成（Video Understanding/Generation）
- 工具调用（Function Calling）
- MCP（Model Context Protocol）服务发现和调用
- 引入智能体工作流，支持串行、并行、条件智能体节点，还需要扩展有向循环图能力（待实现），基于流程上下文实现智能体协同机制（待实现）
- 引入Skills技能模块，正在规划中

### 支持的平台
- DeepSeek
- Kimi（Moonshot）
- 智谱 AI（Zhipu）
- 阿里百炼/通义千问（Qwen）
- 字节豆包/火山引擎（Doubao）
- 百度（Baidu）
- 硅基流动（Siliconflow）
- 九天（Jiutian）
- OpenAI 兼容接口

---

## 二、项目结构

```
agent-ai/
├── agent-ai-model/          # 模型定义模块（基础模型和接口）
│   └── src/main/java/org/frameworkset/spi/ai/
│       ├── model/           # 核心模型定义
│       ├── mcp/model/       # MCP 协议模型
│       └── tools/           # 工具注册接口
│
├── agent-ai/                # 核心实现模块
│   └── src/main/java/org/frameworkset/spi/
│       ├── ai/
│       │   ├── AIAgent.java           # 智能体主入口类
│       │   ├── adapter/               # 平台适配器
│       │   ├── model/                 # 消息模型
│       │   ├── mcp/                   # MCP 客户端实现
│       │   ├── material/              # 素材处理（文件下载等）
│       │   └── util/                  # 工具类
│       └── reactor/                   # Reactor 流式处理组件
│
├── build.gradle             # 根项目构建配置
├── settings.gradle          # 项目模块配置
└── gradle.properties        # Gradle 属性配置
```

---

## 三、模块详解

### 3.1 agent-ai-model 模块

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

---

### 3.2 agent-ai 核心模块

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
    ├── OpenaiAgentAdapter
    └── NoneAgentAdapter
```

**AgentAdapter 核心职责：**
- 构建各类请求参数（Chat、Image、Audio、Video）
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
| `ToolAgentMessage` | 工具调用消息 |
| `AgentMessage` | 消息基类，包含通用属性 |

#### 3.2.4 MCP 客户端

`MCPClient` 实现了 Model Context Protocol 协议客户端：

**核心功能：**
- SSE（Server-Sent Events）连接管理
- 会话生命周期管理（initialize、notifications/initialized）
- 工具列表获取（tools/list）
- 工具调用（tools/call）

**工作流程：**
1. 通过 SSE 端点建立连接
2. 接收 endpoint 事件获取 messagePath 和 sessionId
3. 发送 initialize 请求进行协议初始化
4. 发送 notifications/initialized 通知服务端
5. 正常进行工具列表查询和调用

#### 3.2.5 Reactor 流式处理

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

#### 3.2.6 Util 工具类

| 类名 | 作用 |
|------|------|
| `AIAgentUtil` | AI 智能体核心工具类，封装所有 HTTP 调用逻辑 |
| `AIResponseUtil` | 响应解析工具，处理流式和同步响应 |
| `MessageBuilder` | 消息构建工具 |
| `StreamDataBuilder` | 流数据构建器 |
| `BaseStreamDataBuilder` | 基础流数据构建器 |

---

## 四、架构设计特点

### 4.1 分层架构

```
┌─────────────────────────────────────────┐
│           AIAgent (入口层)               │
├─────────────────────────────────────────┤
│         AgentAdapter (适配层)            │
│    (Deepseek/Kimi/Qwen/...)            │
├─────────────────────────────────────────┤
│         AgentMessage (模型层)            │
│  (Chat/Image/Audio/Video/Tool)         │
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
3. **构建者模式（Builder）**：`MessageBuilder`、`StreamDataBuilder` 构建复杂对象
4. **策略模式（Strategy）**：`StreamDataHandler` 处理不同类型的流数据

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

---

## 七、总结

bboss-ai 是一个功能完善的 Java AI 客户端框架，具有以下特点：

1. **多平台支持**：统一适配国内主流大模型平台
2. **多模态能力**：支持文本、图片、音频、视频的全方位处理
3. **流式响应**：基于 Reactor 实现真正的流式调用
4. **企业级特性**：内置负载均衡、故障转移、服务发现
5. **工具扩展**：支持 Function Calling 和 MCP 协议
6. **轻量级设计**：模块化结构，依赖精简

该框架适合需要集成多种 AI 能力的 Java 企业级应用使用。
