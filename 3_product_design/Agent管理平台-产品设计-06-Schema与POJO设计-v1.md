# Agent 管理平台 - 06 Schema 与 POJO 设计 v1

## 1. 设计目标

平台需要在动态工作流、Java 方法、LLM 结构化输出、外部 Agent、前端表单和节点验收之间传递结构化数据。V1 统一使用 JSON Schema 表达结构，Java POJO 只在执行边界进行转换。

## 2. 核心原则

| 原则 | 说明 |
|---|---|
| Schema 是跨层契约 | 前端、后端、节点、Java 方法、模型输出都围绕 Schema。 |
| POJO 不进入工作流定义 | 工作流保存 Schema 引用和映射，不保存 Java 对象。 |
| Java 类型只在执行边界使用 | JAVA_METHOD 节点执行时才把 JSON 转为 POJO。 |
| 发布版本引用不可变 Schema | 已发布工作流引用的 Schema 版本必须只读。 |
| Schema 通过新版本演进 | 结构变更创建新版本，草稿工作流可切换。 |

## 3. Schema 使用位置

| 使用位置 | 作用 |
|---|---|
| START 节点 | 定义 Agent 调用输入结构，驱动调试表单。 |
| END 节点 | 定义 Agent 最终输出结构。 |
| LLM 节点 | 定义模型结构化输出，配置后强制校验。 |
| JAVA_METHOD 节点 | 定义 Java 方法入参和出参。 |
| EXTERNAL_AGENT 节点 | 可选定义外部 Agent 输入和输出。 |
| TOOL 节点 | 定义工具参数和返回值。 |
| AGENT_CALL 节点 | 定义目标 Agent 输入输出映射。 |
| 节点验收 | 定义验收用例输入和断言字段路径。 |

## 4. SchemaDefinition

建议字段：

- id
- schemaKey
- name
- version
- description
- jsonSchema
- javaType
- createdFrom
- status
- createdAt
- updatedAt

`createdFrom` 可选值：

- USER_CREATED
- JAVA_METHOD_SCAN
- TOOL_DEFINITION
- SYSTEM_BUILTIN
- AGENT_INPUT
- AGENT_OUTPUT

## 5. Schema 版本规则

- Schema 一旦被已发布 WorkflowVersion 引用，该版本只读。
- 修改结构必须创建新版本。
- 草稿工作流可以切换 Schema 版本。
- 历史运行记录必须能追溯当时的 Schema 版本。
- 不允许直接破坏已发布工作流的结构契约。

## 6. 输入映射和输出映射

每个节点应显式声明输入映射和输出映射。

示例：

```json
{
  "inputMapping": {
    "orderNo": "$.input.orderNo"
  },
  "outputMapping": {
    "$.orderInfo": "$.result"
  }
}
```

规则：

- 输入映射从工作流上下文中取值。
- 映射后的输入必须通过 inputSchema 校验。
- 节点输出必须通过 outputSchema 校验。
- 输出映射将节点结果写回工作流上下文。
- 映射语法在接口设计阶段确定。

## 7. Java POJO 交互流程

```text
工作流上下文 JSON
  -> inputMapping 提取输入
  -> inputSchema 校验
  -> JSON 转 Java POJO
  -> 调用 Java 方法
  -> Java POJO 转 JSON
  -> outputSchema 校验
  -> outputMapping 写回上下文
```

## 8. LLM 结构化输出

- LLM 节点配置 outputSchema 时，平台要求模型输出符合该 Schema。
- LLM 节点未配置 outputSchema 时，按普通文本输出处理。
- outputSchema 校验失败时，节点运行失败并记录字段级错误。
- REVIEW 和 SUMMARY 节点也遵守相同规则。

## 9. 外部 Agent 输出

EXTERNAL_AGENT 节点的 outputSchema 是可选能力。

- 配置 outputSchema 时，外部 Agent 最终结果必须通过校验。
- 未配置 outputSchema 时，只保存结果摘要或原始最终结果。
- 外部 Agent 的 stdout、stderr、Git diff、产物采集都是可选调试能力。

## 10. 前端表单渲染

前端可以基于 Schema 渲染：

- Agent 调试输入表单。
- START 节点输入配置。
- 节点验收用例输入表单。
- Java 方法参数查看。
- 工具参数查看。

## 11. 校验失败处理

Schema 校验失败时必须记录：

- runId
- nodeRunId
- nodeId
- schemaKey
- schemaVersion
- 字段路径
- 错误原因
- 原始输入或输出摘要

错误提示必须使用中文，并明确指出需要修正的字段。
