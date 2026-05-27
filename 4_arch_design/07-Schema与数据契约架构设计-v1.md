# 07-Schema 与数据契约架构设计 v1

## 1. 设计原则

SchemaDefinition 是平台唯一跨层结构契约。前端表单、工作流节点、Java POJO、LLM 结构化输出、工具参数、Agent 互调和节点验收都必须围绕同一份 SchemaDefinition。

## 2. SchemaDefinition

| 字段 | 说明 |
|------|------|
| schemaId | 内部主键 |
| schemaKey | 稳定业务标识 |
| version | Schema 版本，整数递增 |
| name | 中文名称 |
| jsonSchema | 完整 JSON Schema 内容 |
| javaType | 可选 Java 类型全限定名 |
| createdFrom | USER_CREATED、JAVA_METHOD_SCAN、TOOL_DEFINITION、SYSTEM_BUILTIN、AGENT_INPUT、AGENT_OUTPUT |
| status | DRAFT、LOCKED、ARCHIVED |
| locked | 是否被发布工作流引用并锁定 |

## 3. 版本规则

- 已发布工作流引用的 Schema 版本只读。
- Schema 结构变更创建新版本。
- Schema 版本号使用整数递增，从 1 开始。
- 同一 schemaKey 下 version 唯一。
- 草稿工作流可以切换到新 Schema 版本。
- 历史运行记录必须能追溯当时使用的 Schema 版本。
- Java POJO 不作为跨层契约，只在执行边界使用。

## 4. 映射规则

v1 使用受控 JSONPath 子集：

| 能力 | 支持情况 | 示例 |
|------|----------|------|
| 读取根对象 | 支持 | `$` |
| 读取字段路径 | 支持 | `$.input.question` |
| 读取数组下标 | 支持 | `$.items[0].name` |
| 写回字段路径 | 支持 | `$.result.summary` |
| 过滤表达式 | 不支持 | 不支持 `?()` |
| 函数调用 | 不支持 | 不支持脚本函数 |
| 任意脚本 | 不支持 | 复杂转换使用 JAVA_METHOD |

## 5. 校验时机

- START：校验调用输入。
- LLM、REVIEW、SUMMARY：配置 outputSchema 时校验模型输出。
- JAVA_METHOD：调用前校验入参，返回后校验出参。
- TOOL：调用前校验工具参数，返回后校验工具输出。
- AGENT_CALL：调用前校验目标 Agent 输入，返回后校验目标 Agent 输出。
- EXTERNAL_AGENT：配置 outputSchema 时校验外部 Agent 输出。
- END：校验最终返回结构。

## 6. POJO 边界

- 工作流定义保存 Schema 和映射，不保存 Java 类型对象。
- Java 类型只在 JAVA_METHOD 执行边界使用。
- Jackson 负责 JSON 与 POJO 转换。
- 转换失败视为节点失败，写入 NODE_ERROR。

## 7. 校验失败处理

- 节点状态为 FAILED。
- 写入 SCHEMA_VALIDATION TraceEvent。
- 写入 NODE_ERROR TraceEvent。
- 错误信息包含字段路径和中文说明。
- 同步运行响应返回 runId，用户可进入运行详情查看。
