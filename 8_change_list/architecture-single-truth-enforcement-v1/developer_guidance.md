# 架构单一真相强制落地 v1 开发说明

## 1. 本次变更的本质

本次变更不是普通重构，也不是兼容旧部署方式。用户已经明确要求把架构单一真相清理作为最高优先级修复。

开发人员必须把本变更理解为“删除旧事实源”，不是“给旧事实源套一层新名字”。

## 2. 不要继续保留默认模型供应商

错误做法：

- Compose 继续要求 `AGENT_STUDIO_OPENAI_API_KEY`。
- Flyway 继续读取 `AGENT_STUDIO_OPENAI_BASE_URL`。
- 空库启动自动创建 `openai-default`。
- 模型调用失败时回退到 `gpt-4.1-mini`。
- 供应商不可用时自动选择另一个默认供应商。

正确做法：

- 系统无模型供应商也能启动。
- 用户进入页面创建供应商。
- 用户创建供应项。
- 运行时只能通过 `modelOfferingKey` 找到具体供应项。
- 找不到就失败，并说明如何配置。

## 3. 不要把启动成功和模型调用成功混在一起

当前发布目标是“系统先启动，用户页面配置供应商”。

因此：

- 缺少模型 API Key 不应该阻止系统启动。
- 缺少模型供应项应该阻止 LLM 类能力执行。
- 错误发生的位置应该是发布校验、运行调用、供应商连接测试或 Eval judge，而不是 Docker 启动。

## 4. 不要保留 `isDefault`

`isDefault` 已被用户否定。不要写读取兼容，不要写迁移脚本，不要在前端保存时 delete 旧字段。

正确心智模型：

- 默认边就是 `type=DEFAULT`。
- 显式条件边就是 `type=CONDITION + condition`。
- 旧数据会删除，代码不负责理解旧格式。

## 5. metadata 不能有本地兜底

metadata 的目的不是减少几行前端常量，而是让后端成为枚举单一真相。

如果前端在 metadata 加载失败时使用本地旧数组，双轨真相仍然存在。

正确处理：

- metadata 成功时渲染选项。
- metadata 失败时显示中文错误。
- 前端可以保留展示排序，但不能自行新增或删除业务枚举。

## 6. 高级 JSON 不能继续做普通用户主入口

高级 JSON 适合专家排障，不适合普通用户配置正式工作流。

如果某个核心字段只能通过高级 JSON 配置，就说明 UI 契约没有完成。

开发时应逐项检查：

- 用户是否能不写 JSON 完成 LLM 节点配置。
- 用户是否能不写 JSON 完成 CONDITION 分支配置。
- 用户是否能不写 JSON 选择 Java 方法、工具、内部 Agent、外部 Agent。
- 用户是否能通过表单完成 Schema 和映射配置。

高级 JSON 可以保留为只读预览或专家模式，但不能绕过正式校验。

## 7. Trace 假配置不要补实现

`agent.studio.trace.persist-full-model-content` 不是需求。

不要为了让文档和代码一致而新增这个配置。正确做法是删除正式文档中的该配置。

如果未来需要 Trace 脱敏、清理或关闭全文保存，应另开变更，重新设计数据保留、安全边界和访问控制。

## 8. Eval 语义以后续已完成变更为准

`architecture-single-truth-cleanup-v1` 早期文档中提过 `scoreRule` 等旧目标，但后续 `llm-node-eval-judge-rule-v1` 已经完成真实验收。

当前 Eval 正式语义是：

- `judgeRule`
- `referenceSample`
- `hardChecks`
- `judgeResult`
- `judgeRawText`
- `judgeModelOfferingKey`
- `judgePromptVersion`

不要恢复 `referenceAnswer`、`assertions`、`scoreRule` 作为当前 LLM 节点 Eval 主链。

## 9. 推荐开发顺序

1. 先删除 `AGENT_STUDIO_OPENAI_*` 部署、配置和迁移路径。
2. 再确认模型调用只按数据库 route 动态执行。
3. 删除 `isDefault`。
4. 新增 metadata 接口并改前端。
5. 补结构化 UI。
6. 删除 Trace 假配置文档。
7. 调整注释规范。
8. 最后统一跑静态检索、OpenAPI、自动化测试和 Docker 空库验收。

## 10. 验收时不要接受的解释

- “为了兼容旧数据先保留一下 `isDefault`。”
- “为了首次体验先创建一个默认 OpenAI 供应商。”
- “metadata 失败时先用本地枚举兜底。”
- “高级 JSON 已经能配，所以表单可以后续再补。”
- “Trace 配置文档已经写了，所以顺手实现一下。”

这些都是本次变更明确禁止的隐藏技术债务。
