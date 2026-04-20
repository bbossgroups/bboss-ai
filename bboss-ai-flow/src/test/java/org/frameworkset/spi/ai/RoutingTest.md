我来深度分析这个AI智能体路由测试案例的功能和架构。

---

## RoutingTest.java 案例分析

### 一、案例概述

这是一个**多智能体协同工作流**的测试案例，展示了bboss-ai-flow模块的**智能路由编排能力**。案例实现了根据用户意图自动路由到不同专业智能体的功能。

---

### 二、核心功能流程

```
┌─────────────────────────────────────────────────────────────────┐
│                         用户输入                                 │
│              "查询长沙市天气，根据天气情况给出穿衣建议"              │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                    AIPlanAgent (工作流智能体)                     │
│                    会话管理 + 流程编排中枢                         │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                   AIRouteAgent (路由规则智能体)                   │
│     系统提示："将用户查询路由到正确的后续任务，不需要回答问题"        │
│     ├─ routingChoice: weatherAgent → "查询城市天气"               │
│     ├─ routingChoice: docAgent → "操作飞书文档"                  │
│     └─ defaultAgent → 默认处理                                   │
└─────────────────────────────────────────────────────────────────┘
                              ↓
                    ┌─────────┴─────────┐
                    ↓                   ↓
        ┌──────────────────┐   ┌──────────────────┐
        │   weatherAgent   │   │    docAgent      │
        │   天气查询智能体   │   │  飞书文档智能体   │
        │  (MCPToolsRegist)│   │ (FeishuMcpRegist)│
        └──────────────────┘   └──────────────────┘
```


---

### 三、核心类解析

| 类名 | 角色 | 职责 |
|------|------|------|
| `AIPlanAgent` | 工作流编排器 | 管理整个对话流程，协调多个子智能体 |
| `AIRouteAgent` | 路由决策器 | 根据用户意图选择执行路径 |
| `AINodeAgent` | AI执行节点 | 基础AI交互节点 |
| `UserNodeAgent` | 用户任务节点 | 封装带工具能力的用户任务智能体 |
| `MCPToolsRegist` | 工具注册器 | 注册MCP工具（天气查询等） |
| `FeishuMcpRegist` | 飞书MCP注册器 | 注册飞书文档操作能力 |
| `StoreContext` | 会话存储上下文 | 管理会话状态持久化 |

---

### 四、代码逐段解析

#### 4.1 初始化配置

```java
// 启动HTTP连接池（AI服务调用）
HttpRequestProxy.startHttpPools("application-stream.properties");

// 启动MCP服务连接池
HttpRequestProxy.startHttpPools("mcpserver.properties");
```


**作用**：初始化bboss-http5的负载均衡HTTP客户端，用于调用大模型API和MCP服务。

#### 4.2 数据库初始化

```java
SQLUtil.startPool("visualops",
    "com.mysql.cj.jdbc.Driver",
    "jdbc:mysql://192.168.137.1:3306/bboss?...",
    "root", "123456",
    "select 1"
);
```


**作用**：配置MySQL数据源，用于**会话状态持久化**（长短期记忆存储）。

#### 4.3 构建工作流智能体

```java
// 1. 定义用户消息
ChatAgentMessage chatAgentMessage = new ChatAgentMessage()
    .setModel("qwen3.6-plus")      // 使用通义千问模型
    .setMaas("qwenvlplus")          // MaaS平台标识
    .setPrompt("介绍一下solon");     // 用户问题

// 2. 创建工作流智能体
AIPlanAgent aiPlanAgent = new AIPlanAgent(
    new StoreContext()
        .setSessionId(sessionId)           // 会话ID
        .setUserId("user123")              // 用户ID
        .setSessionSize(100)               // 短期记忆窗口大小
        .setStoreType(StoreContext.STORE_TYPE_DB)  // 持久化到数据库
        .setDataSource("visualops")        // 数据源名称
)
.setAgentMessage(chatAgentMessage)
.setAgentName("工作流智能体")
.setAgentId("workflowAgent");
```


#### 4.4 路由规则配置

```java
// 添加路由决策智能体
aiPlanAgent.addAIRouteAgent(
    new AIRouteAgent()
        .setAgentId("Router")
        .setAgentName("路由规则智能体")
        .setSystemPrompt("你是一个路由智能体。你的目标是将用户查询路由到正确的后续任务...")
        // 定义路由选项
        .addRoutingChoice("weatherAgent", "查询城市天气，并给出穿衣出行建议")
        .addRoutingChoice("docAgent", "操作飞书文档")
);
```


**路由机制**：
- LLM根据用户问题与routingChoice描述的语义匹配度进行决策
- 返回匹配的智能体ID（如`weatherAgent`或`docAgent`）

#### 4.5 业务智能体注册

```java
// 天气查询智能体（带MCP工具）
aiPlanAgent.addRouteChoiceAgent(
    new UserNodeAgent(new MCPToolsRegist("visualops"))
        .setAgentId("weatherAgent")
        .setAgentName("天气查询智能体")
);

// 飞书文档智能体（带飞书MCP工具）
aiPlanAgent.addRouteChoiceAgent(
    new UserNodeAgent(
        new FeishuMcpRegist("feishumcp")
            .setAppId("cli_a9d43b87aff89cd1")
            .setAppSecret("gIhy0EbVfgQGlpNBN8r10gtqMKMnYCJs")
            .setTools("search-user,get-user,fetch-file,search-doc,create-doc,...")
    )
    .setAgentId("docAgent")
    .setAgentName("飞书文档智能体")
);

// 默认兜底智能体
aiPlanAgent.addDefaultRouteChoiceAgent(
    new AINodeAgent()
        .setAgentId("defaultAgent")
        .setAgentName("默认智能体")
);
```


#### 4.6 执行对话

```java
// 执行完整对话流程
LastSessionMessage lastSessionMessage = aiPlanAgent.chat();

// 输出结果
logger.info("serverEvent:{}", lastSessionMessage.getData());
```


---

### 五、架构设计亮点

#### 5.1 三层记忆管理

```
┌─────────────────────────────────────────┐
│         流程上下文 (Process Context)      │
│    AIPlanAgent级别 - 跨节点长时记忆        │
├─────────────────────────────────────────┤
│         复合节点上下文 (Node Context)       │
│    RouteAgent级别 - 路由决策中间状态        │
├─────────────────────────────────────────┤
│         节点上下文 (Agent Context)         │
│    UserNodeAgent级别 - 单次执行状态        │
└─────────────────────────────────────────┘
```


#### 5.2 MCP工具集成

| MCP类型 | 注册类 | 功能 |
|---------|--------|------|
| 通用工具 | `MCPToolsRegist` | 天气查询、计算等通用能力 |
| 飞书办公 | `FeishuMcpRegist` | 文档操作、用户管理、评论等 |

**MCP协议流程**：
```
1. SSE连接建立 ←→ MCP Server
2. initialize 协议握手
3. tools/list 获取可用工具
4. tools/call  调用具体工具
```


#### 5.3 持久化机制

```java
StoreContext.STORE_TYPE_DB  // 数据库存储
```


**存储内容**：
- 会话历史消息
- 中间执行状态
- 工具调用结果
- 用户偏好设置

---

### 六、执行时序图

```
用户    AIPlanAgent    AIRouteAgent    UserNodeAgent    MCP Server    LLM
 │          │               │                │              │          │
 │─────────→│               │                │              │          │
 │   chat() │               │                │              │          │
 │          │──────────────→│                │              │          │
 │          │   路由决策请求  │                │              │          │
 │          │               │──────────────────────────────────→       │
 │          │               │           语义匹配routingChoice          │
 │          │               │←──────────────────────────────────       │
 │          │               │   返回: weatherAgent                     │
 │          │←──────────────│                │              │          │
 │          │   路由结果      │                │              │          │
 │          │───────────────┼───────────────→│              │          │
 │          │               │   执行天气查询   │              │          │
 │          │               │                │─────────────→│          │
 │          │               │                │  MCP工具调用   │          │
 │          │               │                │←─────────────│          │
 │          │               │                │   工具结果     │          │
 │          │               │←───────────────│              │          │
 │          │               │   执行结果      │              │          │
 │          │←──────────────┼───────────────┘              │          │
 │←─────────│   最终回复     │                             │          │
 │          │               │                             │          │
```


---

### 七、应用场景

| 场景 | 说明 |
|------|------|
| **智能客服** | 根据问题类型路由到售后/技术/销售部门 |
| **企业助手** | 文档操作、日程管理、信息查询统一入口 |
| **多Agent系统** | 专业Agent分工协作（代码/写作/分析） |
| **工作流自动化** | 条件判断 + 工具调用的自动化流程 |

---

### 八、技术特色总结

1. **声明式编排**：通过链式API定义复杂工作流
2. **语义路由**：利用LLM理解意图进行智能分发
3. **工具生态**：MCP协议标准化工具集成
4. **状态持久化**：支持长时间运行的多轮对话
5. **可扩展架构**：易于添加新的Agent类型和工具

这是一个典型的**LLM驱动的Agentic Workflow**实现，展示了bboss-ai-flow在企业级AI应用开发中的强大能力。