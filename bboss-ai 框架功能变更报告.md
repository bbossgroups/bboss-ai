# bboss-ai 框架功能变更报告

> 报告范围：`c4fd0ba`（2026-10-04，architect.md 最后更新点）之后至当前工作区的全部变更
> 版本：**6.5.7**（`BBOSSAIVersion.version=657`，releaseDate `20261008`）
> 对应架构文档：[architect.md](architect.md)（本次已同步完善）

## 一、变更概览

| 序号 | 提交 | 日期 | 变更主题 | 影响面 |
| --- | --- | --- | --- | --- |
| 1 | `60f2945` | 2026-10-07 | 工具权限：会话级 always 规则与静态规则分离，两阶段评估 | 核心 |
| 2 | `4ebf6a5` | 2026-10-08 | 完善授权功能 + 修复 ClickHouse 会话消息查询排序失效 | 核心 |
| 3 | `2e87ee4` | 2026-10-08 | 新增记忆/摘要/压缩设计文档 | 文档 |
| 4 | `85c5d04` | 2026-10-09 | HitL 接口定义调整与完善 + SQL 配置整理 + ModelInfo 调整 | 核心 |
| 5 | `89968be` | 2026-10-09 | 长短期记忆读写检索工具 + 基于上下文窗口的摘要压缩 | 核心/新增 |
| 6 | `5b9619d` | 2026-10-09 | 整理 SQL 配置文件 | 配置 |
| 7 | `def80ed` | 2026-10-09 | 记忆检索限制最近 180 天流水 + 新增 MultiModalTool（占位） | 核心/新增 |

## 二、详细变更

### 2.1 智能体长短期记忆系统（新增，核心）

**背景**：原记忆实现基于工作区本地文件（`MEMORY.md` / `memory/*.md`），本次重构为**数据库持久化的两层记忆模型**。

**新增包结构**

- `org.frameworkset.spi.ai.memory`：`MemoryManager`（由 `compaction` 包迁移而来，负责压缩前用 LLM 抽取长期记忆并 Flush）、`MemoryUtil`（记忆路径判定 `isDayMemoryPath` 等）。
- `org.frameworkset.spi.ai.model.memory`：记忆实体
  - `AgentMemory` —— 长期记忆总账，表 `agent_memory`，一个「智能体 + 用户」一条；
  - `AgentDayMemory` —— 日流水账，表 `agent_day_memory`，一个「智能体 + 用户 + 日」一条，`memoryDay` 形如 `memory/yyyy-MM-dd.md`；
  - `AgentConsolidationState` —— 记忆规范化（consolidation）水位，表 `agent_consolidation_state`。

**新增记忆工具（`tools` 包，需显式 `registBeanTool`）**

| 工具 | 工具名 | 说明 |
| --- | --- | --- |
| `MemorySaveTool` | `memory_save` | 主动保存记忆：追加到长期总账 `agent_memory`，同时以 `## Memory Save — {时间戳}` 段落追加到当日流水账；是 Agent 写长期记忆的**唯一授权入口** |
| `MemoryGetTool` | `memory_get` | 读取长期总账与指定日期流水账 |
| `MemorySearchTool` | `memory_search` | 检索记忆总账与近期流水账，支持 `phrase`/`all`/`any` 匹配模式；**限制最多检索最近 180 天流水**（`def80ed`） |

**存储层新增 API**（`AgentSessionService` / `AgentSessionServiceImpl`）：

`getMemory`、`createOrUpdateMemory`、`getDayMemory`、`createOrUpdateDayMemory`、`listAgentUserDayMemorys`、`getAgentPermissionRules`。

**删除内容**：`agentMemory.xml`、原文件式记忆存储实现 `AgentMemoryStore` / `AgentMemoryStoreDB`。

**新增数据库表**（详见 [agentSession.xml](bboss-ai/src/main/java/org/frameworkset/spi/ai/store/db/agentSession.xml)）

| 表名 | 用途 |
| --- | --- |
| `agent_memory` | 长期记忆总账 |
| `agent_day_memory` | 日流水账 |
| `agent_consolidation_state` | 记忆规范化水位 |

> 说明：`MultiModalTool`（`def80ed` 新增，约 1732 行）当前为**全注释占位代码**，尚未启用、未注册，故未纳入架构文档内置工具表。

### 2.2 会话记忆压缩：上下文窗口驱动的摘要压缩（核心）

- **压缩策略更名**：`COMPACTION_POLICY_SUMMARY` → **`COMPACTION_POLICY_TOKENS`**（`=1`，摘要/Token 压缩为默认策略）；`COMPACTION_POLICY_WINDOWSIZE`（`=0`）为窗口压缩。
- **动态阈值**：`CompactionConfig.triggerTokens` 默认 0 表示动态模式，`CompactionManager.resolveEffectiveConfig` 按 `ModelInfo.getContextWindowSize() - reserved` 计算触发阈值与保留尾部预算；模型未报告上下文窗口时回退 160000。`ModelInfo.contextWindowSize` 默认 **200000**。
- **新增配置项** `offloadBeforeCompact`（默认 true）：压缩前将原始消息卸载存档到 session 记录（永不压缩），与会话检索工具联动。
- **记忆 Flush 迁址**：`MemoryManager` 从 `compaction` 包迁移到 `memory` 包，压缩前将待压缩消息交给 LLM 抽取长期记忆，写入当日日流水账（DB）。

### 2.3 工具权限管控：会话级 always 规则与静态规则分离（核心）

- **两阶段评估**：先执行 `checkSessionAlwaysPermission`（会话运行期用户动态设置的 always 规则），命中则直接放行/拒绝；未命中再执行 `checkPermission`（静态配置规则）。
- **规则模型扩展**：`PermissionRules` 支持会话级与静态规则独立保存与判断；`AgentMessageTypeConvertor` 新增消息类型 **25**（`MESSAGE_TYPE_AGENTTOOLPERMISSIONRULES_MESSAGE`），会话级 always 规则随消息持久化并在续问时自动还原。
- **新增/完善类**：`PermissionDecision`、`PermissionVerdict`（评估结论模型）；`PermissionEngine` 评估管线调整。
- `AgentAdapter`（约 296 行改动）、`FileFunctionTool`、`ToolBase`、`FunctionTool` 等配合调整。

### 2.4 HitL 人工介入接口调整（核心）

- `HitlTaskHelper.handleHitlTask` 参数类型改为 `Object`（提升调用灵活性）。
- `HitlTaskToolInf` 增加 default setter（接口默认方法），降低自定义 Hitl 工具实现成本。
- `HitlAssistant` 接口同步调整。

### 2.5 ClickHouse 会话消息查询排序修复 + SQL 配置整理（核心/配置）

- 修复 `4ebf6a5`：**ClickHouse 会话消息查询排序不生效**问题，涉及 `AgentSessionStoreDB` / `AgentSessionStoreDBConfig`。
- `5b9619d` / `85c5d04`：整理 SQL 配置文件，`AgentSessionStoreDBConfig`（约 535 行改动）、`clickhouse-agent.xml`（约 162 行改动）完善分布式表与查询配置。

### 2.6 版本与依赖升级

- `PROJ_VERSION`：→ **6.5.7**
- `BBOSS_HTTP5_VERSION`：→ **6.5.6**
- `BBOSSAIVersion`：`version=657`，`releaseDate=20261008`

## 三、数据库表结构变更汇总

| 表名 | 变更类型 | 说明 |
| --- | --- | --- |
| `agent_memory` | 新增 | 智能体长期记忆总账 |
| `agent_day_memory` | 新增 | 智能体日流水账 |
| `agent_consolidation_state` | 新增 | 记忆规范化水位 |
| `agent_tool_call_rules` | 已有 | 权限 always 规则持久化，本次扩展会话级规则还原 |
| `agent_session_message` | 已有 | 消息类型扩展至 25（`MESSAGE_TYPE_AGENTTOOLPERMISSIONRULES_MESSAGE`），摘要类型 23 |

> 首次使用时自动建表；ClickHouse 模式自动创建本地表（`*_local`，`ReplicatedMergeTree`）与分布式表（`Distributed`）。

## 四、兼容性与升级建议

1. **记忆存储迁移**：原基于 `MEMORY.md` / `memory/*.md` 文件的记忆方案已移除，升级后记忆统一落库，请确保数据库连接（`StoreContext` 的 db 模式）可用。
2. **压缩策略常量更名**：若代码中引用了 `COMPACTION_POLICY_SUMMARY`，需改为 `COMPACTION_POLICY_TOKENS`（语义不变，仍为摘要压缩）。
3. **记忆工具需手动注册**：`MemorySaveTool` / `MemoryGetTool` / `MemorySearchTool` 不会自动注册，需 `agent.registBeanTool(...)`；仅 `SessionSearchTool`、`CompactSummaryMsgSearchTool` 在 `enableMemorySearch=true` 时自动注册。
4. **HitL 自定义实现**：`HitlTaskHelper.handleHitlTask` 参数类型由具体类型改为 `Object`，自定义 `HitlTaskToolInf` 实现需同步核对方法签名。
5. **权限规则**：会话级 always 规则优先于静态规则评估，若存在静态规则与运行期规则冲突场景，需按新优先级语义验证。

## 五、架构文档同步清单（architect.md）

本次已同步完善以下内容，使文档与代码一致：

- 概述 bullet：压缩（上下文窗口 Token 触发）、记忆（两层模型 + 三表 + 记忆工具）、权限（会话级/静态规则分离）
- 项目结构：新增 `memory/` 包，`model/` 补充 `model/memory` 子包，store 描述调整为 `AgentSessionService`
- 模型核心类：删除旧 `Memory` 行，指向 `model.memory` 子包
- 会话存储核心类与支持的表：三表扩展至包含记忆三表 + 权限规则表，补充 ClickHouse 本地表说明
- `AgentSessionService` API：新增记忆/权限规则相关 API
- 3.2.14 压缩章节：策略更名、动态阈值、`offloadBeforeCompact`、`MemoryManager` 迁址
- 3.2.15 智能体记忆章节：完全重写（实体 + 表结构 + 读写 API）
- 3.2.17 权限章节：两阶段评估 + `PermissionDecision`/`PermissionVerdict`/`PermissionRules`
- 内置工具表与注册说明：新增三个记忆工具及其注册要求
- 4.1 分层架构图、4.6 会话管理机制、4.7 工具扩展机制、总结章节：同步记忆/压缩/权限描述

## 六、待跟进事项

- `MultiModalTool` 目前为全注释占位，待其启用并注册后补充进内置工具表与工具扩展章节。
- `KeywordMatcher`（新增，`tools/util/KeywordMatcher.java`）已暂存但尚未接入，待确认用途后补充文档。