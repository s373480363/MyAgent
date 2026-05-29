# 6 Schema 设计总览 v1

## 1. 目录目标

本目录用于定义平台 v1 的全部数据结构设计，覆盖以下内容：

- 领域对象和嵌入结构。
- JSON Schema 与 JSONPath 约束。
- 核心业务表与运行追踪表。
- 节点验收数据结构。
- PostgreSQL DDL 与索引设计。

本目录的内容直接服务于后端实现、前端表单渲染、工作流运行、Trace 追踪和节点验收，不做概念性空转设计。

## 2. 文档清单

| 编号 | 文档 | 主要内容 |
|------|------|----------|
| 01 | `01-数据结构设计总则-v1.md` | 数据分层、命名约束、版本规则、统一存储原则 |
| 02 | `02-领域对象与嵌入结构-v1.md` | Agent、Workflow、Schema、节点配置等领域对象 |
| 03 | `03-Schema与JSON Schema细则-v1.md` | SchemaDefinition、JSON Schema 子集、JSONPath 映射规则 |
| 04 | `04-核心业务表DDL-v1.md` | Agent、Workflow、Schema、方法、工具、外部 Agent、系统设置表 |
| 05 | `05-运行追踪与验收表DDL-v1.md` | AgentRun、NodeRun、TraceEvent、AgentMessage、Eval 表 |

## 3. 编写顺序

建议按以下顺序阅读和实现：

1. `01-数据结构设计总则-v1.md`
2. `02-领域对象与嵌入结构-v1.md`
3. `03-Schema与JSON Schema细则-v1.md`
4. `04-核心业务表DDL-v1.md`
5. `05-运行追踪与验收表DDL-v1.md`

## 4. 设计边界

- PostgreSQL 是唯一持久化存储。
- 结构化契约以 JSON Schema 为统一来源。
- 已发布版本不可原地修改。
- 运行数据、Trace 数据和验收数据必须可追溯到当时的版本。
- 前端表单和后端校验必须共享同一套字段定义。
