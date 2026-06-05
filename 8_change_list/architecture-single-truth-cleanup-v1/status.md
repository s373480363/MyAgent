# 架构单一真相清理 v1 状态

当前状态：方案已根据用户补充决策更新完成，待进入开发。

## 已完成

- 已根据用户最新决策整理正式修复方案。
- 已明确排除审计问题 7，本变更不处理 LLM 输出波动性问题。
- 已明确 `AGENT_STUDIO_OPENAI_*` 全量移除，不保留默认供应商和默认模型。
- 已明确 `isDefault` 激进清理，不保留读取兼容分支。
- 已明确 metadata 只读接口作为前端枚举长期单一真相。
- 已明确 `agent.studio.trace.persist-full-model-content` 是错误文档项，只删除不实现。
- 已明确 getter/setter 不需要 Javadoc，开发规范需要调整。
- 已明确 metadata 接口路径采用 `GET /api/platform-metadata`。
- 已明确当前系统不处理旧数据迁移，旧数据会被删除。
- 已明确 Spring Boot OpenAI starter 自动配置不适合多 provider 动态路由，后续应改为显式动态客户端构造。

## 已确认

- metadata 接口路径采用 `GET /api/platform-metadata`。
- 所有旧数据都会被删除，因此不编写旧数据迁移脚本。
- 模型调用必须支持多 provider 动态配置，不依赖 starter 自动装配的静态默认 OpenAI 客户端。

## 与旧变更包的关系

早前变更包中关于以下内容的结论，以本变更为准：

- `AGENT_STUDIO_OPENAI_*` 作为正式部署变量或默认供应商初始化来源。
- `isDefault` 作为历史读取兼容字段。
- `agent.studio.trace.persist-full-model-content` 作为预留配置。

这些旧结论已经被用户否定，后续开发不得继续引用。
