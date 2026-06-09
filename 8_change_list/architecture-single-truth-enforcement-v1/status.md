# 架构单一真相强制落地 v1 状态

当前状态：方案编写完成，待架构审核。

## 已完成

- 已根据用户最新确认创建本变更包。
- 已明确 `architecture-single-truth-cleanup-v1` 是最高优先级修复。
- 已明确彻底废除 fresh install 默认模型供应商。
- 已明确当前发布目标是“系统先启动，用户页面配置供应商”。
- 已明确高级 JSON 不能作为普通用户正式配置主入口，必须补结构化 UI。
- 已明确 `llm-node-eval-judge-rule-v1` 是当前 Eval 正式语义来源。

## 当前正式决策

- 不再使用 `AGENT_STUDIO_OPENAI_*` 作为部署、迁移、默认模型或默认供应商入口。
- 不再自动创建默认模型供应商和默认模型供应项。
- 不再保留 `WorkflowEdgeDefinition.isDefault`。
- 必须新增 `GET /api/platform-metadata`。
- 前端不得维护本地业务枚举兜底。
- 不实现 `agent.studio.trace.persist-full-model-content`。
- 普通用户工作流配置主路径必须是结构化 UI。

## 待执行

- 开发落地。
- 文档同步。
- OpenAPI 更新。
- 自动化测试。
- Docker 空库正式入口验收。
- 页面配置模型供应商验收。

## 与旧变更包关系

`architecture-single-truth-cleanup-v1` 中关于以下内容的结论继续有效：

- 删除 `AGENT_STUDIO_OPENAI_*`。
- 删除 `isDefault`。
- 新增 metadata 接口。
- 删除 Trace 假配置。
- 调整 getter/setter Javadoc 规则。

`architecture-single-truth-cleanup-v1` 中与 `llm-node-eval-judge-rule-v1` 冲突的 Eval 旧字段目标不再作为当前正式语义。当前 Eval 正式语义以 `llm-node-eval-judge-rule-v1` 已完成方案为准，即 `judgeRule`、`referenceSample`、`hardChecks`。

## 架构审核关注点

- 是否彻底消除了默认供应商和默认模型初始化路径。
- 是否没有保留 `isDefault` 兼容读取。
- metadata 是否真正成为前端枚举单一真相。
- 高级 JSON 是否不再是普通用户配置核心字段的唯一入口。
- 文档是否已经同步到当前发布目标。
