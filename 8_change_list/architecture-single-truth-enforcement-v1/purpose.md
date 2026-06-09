# 架构单一真相强制落地 v1 变更目的

## 1. 背景

本次变更来自全局架构审查后的用户确认。当前项目已经存在 `architecture-single-truth-cleanup-v1` 方案，但该方案仍处于待开发状态，正式代码、发布文档和部分设计文档继续保留旧路径。

这些旧路径包括：

- `AGENT_STUDIO_OPENAI_*` 仍作为 Docker 启动、默认供应商初始化和默认模型初始化入口。
- fresh install 会自动创建默认模型供应商和默认模型供应项。
- 工作流默认边同时存在 `type=DEFAULT` 和 `isDefault` 两种表达。
- 前端工作流编辑器和 Eval 页面继续维护本地枚举。
- `agent.studio.trace.persist-full-model-content` 作为不存在的配置项出现在正式文档。
- 普通用户仍可能通过高级 JSON 完成正式节点配置。

这些问题的共同本质是：系统中存在多个事实来源，开发人员可以从不同文档或代码中得到互相冲突的结论。按照项目规范，这属于双轨真相和兼容/止血式隐藏技术债务，必须优先清理。

## 2. 用户已确认的最高优先级决策

用户已明确确认：

1. `architecture-single-truth-cleanup-v1` 是必须执行的最高优先级修复。
2. 彻底废除 fresh install 默认模型供应商。
3. 当前发布目标是“系统先启动，用户页面配置供应商”。
4. 高级 JSON 当前作为普通用户配置正式入口是问题，必须补正式结构化 UI。

本变更将上述决策整理为开发落地变更包。后续开发不得继续引用旧变更包中与本变更冲突的结论。

## 3. 目标

本次变更必须达成：

- 正式部署不再要求 `AGENT_STUDIO_OPENAI_API_KEY`、`AGENT_STUDIO_OPENAI_BASE_URL`、`AGENT_STUDIO_OPENAI_DEFAULT_MODEL`。
- 空库 fresh install 后系统可以启动，模型供应商和模型供应项允许为空。
- 模型调用只从数据库模型供应商目录解析，不存在默认供应商、默认模型或环境变量回退。
- 删除 `WorkflowEdgeDefinition.isDefault` 及所有读取、保存、OpenAPI、前端类型和测试残留。
- 新增 `GET /api/platform-metadata`，由后端提供工作流和 Eval 所需枚举。
- 前端工作流编辑器和 Eval 页面不得继续手工维护业务枚举真相。
- 删除正式文档中的 `agent.studio.trace.persist-full-model-content`。
- 普通用户配置节点时使用结构化 UI，高级 JSON 不能作为主配置入口。
- 开发规范调整为 getter/setter 不强制 Javadoc。
- 所有正式设计、接口、发布和用户文档与本变更保持一致。

## 4. 非目标

- 不保留 `AGENT_STUDIO_OPENAI_*` 到模型供应商目录的兼容导入路径。
- 不为 fresh install 创建默认模型供应商、默认模型供应项或默认模型。
- 不实现 `agent.studio.trace.persist-full-model-content`。
- 不保留 `isDefault` 历史读取兼容。
- 不为 `isDefault` 编写旧数据迁移脚本。
- 不把 metadata 加载失败时的本地硬编码枚举作为兜底。
- 不在本变更中引入登录、权限、多租户、供应商模型自动同步。
- 不重做 Eval judge rule 主链；`llm-node-eval-judge-rule-v1` 已完成并作为当前 Eval 正式语义。

## 5. 成功标准

- 除历史变更记录和历史验收报告外，正式代码和正式文档不再命中 `AGENT_STUDIO_OPENAI`。
- 除历史变更记录和历史验收报告外，正式代码、OpenAPI、前端生成类型和正式文档不再命中 `isDefault`。
- 除历史变更记录和历史验收报告外，正式文档不再命中 `persist-full-model-content`。
- 空库 Docker 正式入口可以在没有任何模型供应商配置的情况下启动。
- 页面可以访问模型供应商管理功能，并允许用户创建供应商和模型供应项。
- 未配置模型供应项时，LLM、REVIEW、SUMMARY 和 Eval judge 相关运行失败必须返回明确中文错误。
- 工作流普通用户主路径不依赖高级 JSON 配置核心字段。
- 前端业务枚举来自 `GET /api/platform-metadata`。
- `npm run openapi:check`、后端测试、前端测试和 Docker 正式入口验收通过。
