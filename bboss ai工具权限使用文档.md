# bboss ai 工具权限（Permission）使用文档

模型每次调用工具前，权限引擎对本次工具调用做裁决：

- **ALLOW**：放行执行
- **DENY**：拒绝，结果直接返回模型
- **ASK**：触发人工审批（HITL），等待用户确认

## 1. 配置权限规则

在 `AgentRuntimeContext` 上按工具名配置三类规则，再传入 `AIAgent`：

```java
AgentRuntimeContext ctx = new AgentRuntimeContext();
ctx.addAllowRule("get_weather", "city=北京", "userSettings") // 允许
        .addDenyRule("deleteFile", "userSettings")          // 拒绝
        .addAskRule("writeFile", "userSettings")            // 询问人工
        .setPermissionHitlTaskTimeout(60000);               // 人工确认超时 60s

new AIAgent().setAgentRuntimeContext(ctx);
```

**参数说明**

| 参数 | 含义 |
| --- | --- |
| toolName | 规则作用的工具名 |
| ruleContent | 条件匹配串，规则仅作用于匹配的工具调用；`null` 表示工具级全匹配 |
| source | 规则来源，如 `userSettings`（用户设置）、`suggested`（模型/工具建议） |

`ruleContent` 的匹配逻辑由工具自身的 `matchRule(ruleContent, toolInput)` 实现，如 `FileFunctionTool` 只支持 `null`（工具名级规则），自定义工具可重写该方法实现精细化匹配。

## 2. ASK 规则触发人工审批

到达 ASK 的工具调用会进入 HITL 任务，流式响应中收到 `chunk.isHitl()` 事件，通过任务类型区分：

```java
if (chunk.isHitl()) {
    String hitlTaskId = chunk.getHitlTaskId();
    String hitlTaskType = (String) chunk.hitlAssistant(HitlAssistant.HITL_TASK_TYPE_KEY);

    if (HitlAssistant.HITL_TASK_TYPE_TOOL_CALL_PERMISSION_ASK.equals(hitlTaskType)) {
        // 1. 取出待审批的工具列表
        List<ToolCallAsk> askTools =
                (List<ToolCallAsk>) chunk.hitlAssistant(HitlAssistant.HITL_TASK_PERMISSION_ASK_TOOLS);

        // 2. 逐个构造审批结果
        List<ToolCallAskResult> results = new ArrayList<>();
        for (ToolCallAsk askTool : askTools) {
            ToolCallAskResult r = new ToolCallAskResult();
            r.setToolId(askTool.getToolId());
            r.setToolName(askTool.getToolName());
            r.setApproved(true);                    // 审批通过；false 则拒绝本次操作
            r.setHitlConfirm("确认修改文件");       // 给模型的确认意见
            r.setUpdateInput(askTool.getInput());   // 可修改工具入参，不改则原样放回
            // 可选：从建议规则中选择一条“始终允许”，后续不再询问
            if (askTool.getSuggestedRules() != null && !askTool.getSuggestedRules().isEmpty()) {
                r.setChoosedAlwaysPermissionRule(askTool.getSuggestedRules().get(0));
            }
            results.add(r);
        }

        // 3. 提交审批，唤醒被中断的智能体继续执行
        HitlTaskHelper.handleHitlCallTask(results, null, hitlTaskId);
    }
}
```

关键点：

- 每个待审批工具对应一个 [ToolCallAsk](bboss-ai/src/main/java/org/frameworkset/spi/ai/model/tool/ToolCallAsk.java)，审批结果用同名的 `ToolCallAskResult` 回填。
- 勾选 `choosedAlwaysPermissionRule` 后，该规则会沉淀为“始终允许”，后续调用免审批。
- 审批**超时**由 `setPermissionHitlTaskTimeout` 控制；如不希望人工介入被绕过，可配合 `PermissionMode.DONT_ASK` 在无人值守时将 ASK 降级为 DENY。
- 审批提交后，智能体在其所在节点通过内存（单机）或 Redis 发布/订阅（集群）唤醒续跑，处理方式与通用 HITL 任务一致，详见《bboss ai人工介入Hitl功能使用文档》。

## 3. 权限模式（PermissionMode）

引擎支持全局模式快速切换，通过 `PermissionMode` 配置：

| 模式 | 行为 |
| --- | --- |
| `default` | 默认，所有操作均需规则放行 |
| `accept_edits` | 工作目录内的文件编辑自动放行 |
| `explore` | 只读模式，修改类工具一律拒绝 |
| `bypass` | 全部放行，不校验规则 |
| `dont_ask` | ASK 降级为 DENY，适用于无人值守运行 |

## 4. 完整参考

- 权限裁决链路：[permission/](bboss-ai/src/main/java/org/frameworkset/spi/ai/permission/)（`PermissionEngine`、`PermissionGate`、`PermissionRule`、`PermissionBehavior`、`PermissionMode`）
- 规则配置入口：[AgentRuntimeContext](bboss-ai/src/main/java/org/frameworkset/spi/ai/context/AgentRuntimeContext.java)
- 完整示例：[PermissionChecklistCodeViewAgentHitlResisTest.java](bboss-ai/src/test/java/org/frameworkset/spi/ai/permission/PermissionChecklistCodeViewAgentHitlResisTest.java)