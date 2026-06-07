# Agent 管理平台架构设计总纲 v1

## 1. 文档目的

本文档集定义 Agent 管理平台 v1 的架构基线，用于约束后续数据结构设计、公共接口设计、前端 UI 设计、测试计划和具体开发实现。

架构设计覆盖完整 v1 产品能力。本文档中的实现顺序只表达开发落地先后，不作为产品版本边界，也不减少 V1 全量范围。

## 2. 输入依据

- `3_product_design\Agent管理平台-产品设计-总纲-v1.md`
- `3_product_design\Agent管理平台-产品设计-01-产品定位与交付边界-v1.md`
- `3_product_design\Agent管理平台-产品设计-02-用户场景与功能范围-v1.md`
- `3_product_design\Agent管理平台-产品设计-03-核心概念与领域模型-v1.md`
- `3_product_design\Agent管理平台-产品设计-04-工作流画布与版本规则-v1.md`
- `3_product_design\Agent管理平台-产品设计-05-节点体系设计-v1.md`
- `3_product_design\Agent管理平台-产品设计-06-Schema与POJO设计-v1.md`
- `3_product_design\Agent管理平台-产品设计-07-节点验收设计-v1.md`
- `3_product_design\Agent管理平台-产品设计-08-运行调用与Trace设计-v1.md`
- `3_product_design\Agent管理平台-产品设计-09-信息架构与验收标准-v1.md`
- `0_specifications\develop_specification.md`
- `0_specifications\ui_deisgn_specification.md`

## 3. 已确认约束

| 主题 | 约束 |
|------|------|
| 部署边界 | 本机或内网单用户部署，可在信任环境内共享访问，不做公网 SaaS 安全边界 |
| 用户权限 | v1 不做登录、角色、权限、多租户 |
| 模型供应商 | v1 通过页面维护 OpenAI-compatible 供应商目录，非 OpenAI-compatible 协议放到后续版本 |
| 前端画布 | 默认采用 React Flow |
| 工作流执行 | 使用 LangGraph4j |
| AI 能力底座 | 使用 Spring AI |
| 数据库 | PostgreSQL |
| 结构契约 | JSON Schema 是前端、后端、工作流、Java POJO、LLM 输出和验收用例的统一契约 |
| Trace 内容 | V1 默认保存模型提示词和模型输出全文，用于调试和复盘；成立前提是本机或内网单用户部署 |
| 示例数据 | V1 不强制内置示例数据；如需降低首次使用门槛，可在用户手册或后续模板能力中提供示例 |
| 版本规则 | 每个 Agent 一个当前草稿；每次发布生成不可变 WorkflowVersion |
| Schema 规则 | 已发布工作流引用的 Schema 版本只读，结构变更创建新版本 |
| Schema 版本类型 | 使用整数递增版本号，唯一键为 `schemaKey + version` |
| 前后端契约 | 后端输出 OpenAPI 契约，前端 TypeScript 类型由 OpenAPI 生成，禁止手工维护双份接口类型 |

## 4. v1 完整架构范围

v1 架构必须覆盖以下能力：

- Agent 管理：AgentDefinition、草稿、发布版本、停用、调试入口。
- 工作流画布：React Flow 节点、边、配置面板、发布校验、JSON 视图。
- 工作流运行：LangGraph4j 编译执行、同步调用、超时、最大步数、失败状态。
- v1 节点体系：START、END、LLM、CONDITION、JAVA_METHOD、AGENT_CALL、EXTERNAL_AGENT、TOOL、REVIEW、SUMMARY。
- Schema 契约：SchemaDefinition、版本锁定、JSONPath 映射、运行时校验。
- Java 方法：注解注册、Spring Bean 调用、入参出参 Schema、禁止任意反射。
- Agent 协作：AGENT_CALL、父子 AgentRun、AgentMessage、最大调用深度。
- 外部 Agent：Codex、OpenCode、Custom CLI、Custom HTTP 适配器。
- TOOL 节点：ToolDefinition、ToolRegistry、直接工具调用和 LLM 允许工具列表。
- 节点验收：EvalSuite、EvalCase、EvalRun、自然语言 judgeRule、可选 hardChecks、judgeResult。
- Trace：AgentRun、NodeRun、TraceEvent、模型请求响应全文、错误定位。
- 公共接口契约：REST API 由 OpenAPI 描述，前端基于契约生成类型。

## 5. 实现顺序建议

| 阶段 | 启用能力 | 说明 |
|------|----------|------|
| 第一批 | START、END、LLM、CONDITION、JAVA_METHOD | 先跑通工作流画布、OpenAI 调用、Java 方法调用、条件分支、同步运行和基础 Trace |
| 第二批 | AGENT_CALL、EXTERNAL_AGENT | 补齐平台内 Agent 互调和外部 Agent 轻量接入 |
| 第三批 | TOOL、REVIEW、SUMMARY、节点验收 | 补齐工具能力、LLM 专业节点和节点级回归验收 |

## 6. 总体架构

```text
浏览器
  -> React 管理端
      -> Agent 管理、React Flow 画布、Schema 表单、运行调试、Trace 查看、节点验收
  -> Spring Boot REST API
      -> Application Service：用例编排、事务边界
      -> Domain：Agent、WorkflowVersion、SchemaDefinition、AgentRun、EvalSuite
      -> Runtime：WorkflowCompiler、WorkflowValidator、NodeExecutor、TraceWriter
      -> Registry：JavaMethodRegistry、ModelOfferingRegistry、ToolRegistry、ExternalAgentRegistry
      -> Infrastructure：PostgreSQL、OpenAI-compatible 客户端、JSON Schema、JSONPath、Jackson、CLI/HTTP Adapter
  -> PostgreSQL
  -> OpenAI-compatible 模型供应商
  -> 已注册 Java 方法 / 已注册工具 / 外部 Agent
```

## 7. 子文档索引

| 编号 | 文档 | 主要内容 |
------|------|----------|
| 01 | `01-技术选型与架构约束-v1.md` | 技术栈、明确不选、统一约束 |
| 02 | `02-总体架构与模块边界-v1.md` | 分层架构、模块依赖、事务边界 |
| 03 | `03-前端架构设计-v1.md` | React、React Flow、路由、状态管理 |
| 04 | `04-后端架构设计-v1.md` | 后端包结构、应用服务、注册目录 |
| 05 | `05-工作流运行内核设计-v1.md` | WorkflowCompiler、NodeExecutor、运行流程 |
| 06 | `06-节点体系架构设计-v1.md` | 全部 v1 节点职责与校验规则 |
| 07 | `07-Schema与数据契约架构设计-v1.md` | SchemaDefinition、JSONPath、POJO 边界 |
| 08 | `08-Trace追踪与调试架构设计-v1.md` | AgentRun、NodeRun、TraceEvent |
| 09 | `09-Agent协作与外部Agent架构设计-v1.md` | AGENT_CALL、EXTERNAL_AGENT |
| 10 | `10-节点验收架构设计-v1.md` | EvalSuite、EvalCase、EvalRun |
| 11 | `11-数据结构架构设计-v1.md` | 核心表、枚举、关系 |
| 12 | `12-公共接口架构设计-v1.md` | REST API 分组、响应和错误规范 |
| 13 | `13-部署安全与质量架构设计-v1.md` | 部署、配置、安全边界、测试策略 |

## 8. 当前不需要产品澄清的问题

当前架构设计没有新的产品阻塞问题。剩余事项属于架构和开发侧详细设计，例如具体依赖版本、数据库索引、接口字段明细、前端组件拆分和测试用例设计。
