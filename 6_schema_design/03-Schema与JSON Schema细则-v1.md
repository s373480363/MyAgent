# 03 Schema 与 JSON Schema 细则 v1

## 1. 设计目标

本文件定义 SchemaDefinition 的详细规则、JSON Schema 子集、JSONPath 映射规则、校验结果结构和版本演进方式。

核心要求是让前端表单、工作流节点、Java 方法、LLM 结构化输出和节点验收共享同一套结构契约。

## 2. SchemaDefinition 语义

SchemaDefinition 是平台内的结构化契约对象，不是某个单独表单的字段集合。

它可以被以下场景引用：

- START 节点输入。
- END 节点输出。
- LLM、REVIEW、SUMMARY 节点结构化输出。
- JAVA_METHOD 节点入参和出参。
- TOOL 节点参数和返回值。
- EXTERNAL_AGENT 节点输出。
- 节点验收用例输入和断言字段定位。

## 3. JSON Schema 结构建议

SchemaDefinition 的 `json_schema` 以对象型结构为主。

### 3.1 推荐根结构

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "type": "object",
  "title": "Agent 输入",
  "description": "用于工作流输入的结构化数据",
  "properties": {
    "question": {
      "type": "string"
    }
  },
  "required": ["question"],
  "additionalProperties": false
}
```

### 3.2 推荐约束

- 根节点优先使用 `object`。
- 结构契约默认显式声明 `required`。
- 业务输出默认尽量关闭 `additionalProperties`，避免结构漂移。
- 文本类字段用 `string`。
- 数值类字段用 `number` 或 `integer`。
- 布尔类字段用 `boolean`。
- 列表类字段用 `array`。
- 嵌套结构用 `object`。

## 4. JSON Schema 子集

v1 只支持受控子集，避免把 Schema 变成任意脚本语言。

| 能力 | 支持情况 | 说明 |
|------|----------|------|
| `type` | 支持 | object、string、number、integer、boolean、array、null |
| `properties` | 支持 | 对象字段定义 |
| `required` | 支持 | 必填字段集合 |
| `items` | 支持 | 数组元素定义 |
| `enum` | 支持 | 枚举约束 |
| `format` | 支持 | 仅用于展示和基础校验 |
| `minLength` / `maxLength` | 支持 | 字符串约束 |
| `minimum` / `maximum` | 支持 | 数值约束 |
| `pattern` | 支持 | 正则约束 |
| `additionalProperties` | 支持 | 建议默认 false |
| `oneOf` / `anyOf` | 受限支持 | 仅在明确场景使用 |
| `$ref` | 受限支持 | 只允许引用受控内部定义 |
| `if/then/else` | 不建议 | v1 默认不作为主要建模手段 |
| 自定义脚本 | 不支持 | 不允许在 Schema 内嵌脚本 |
| 任意函数调用 | 不支持 | 不允许 |

## 5. JSONPath 映射规则

v1 采用受控 JSONPath 子集进行输入映射和输出写回。

### 5.1 支持能力

| 能力 | 支持情况 | 示例 |
|------|----------|------|
| 根对象读取 | 支持 | `$` |
| 字段路径读取 | 支持 | `$.input.question` |
| 数组下标读取 | 支持 | `$.items[0].name` |
| 字段写回 | 支持 | `$.result.summary` |
| 过滤表达式 | 不支持 | `?()` |
| 函数调用 | 不支持 | `length()` |
| 任意脚本 | 不支持 | 不允许 |
| 递归通配 | 不支持 | `..` |

### 5.2 inputMapping 规则

`inputMapping` 的语义是：从工作流上下文中读取数据，组装成节点输入对象。

推荐形式：

```json
{
  "question": "$.input.question",
  "content": "$.input.content"
}
```

规则：

- Key 表示节点输入字段名。
- Value 表示上下文读取路径。
- 读取后的对象必须满足 `inputSchemaRef`。

### 5.3 outputMapping 规则

`outputMapping` 的语义是：把节点输出写回工作流上下文。

推荐形式：

```json
{
  "$.result.summary": "$.nodeOutput.summary",
  "$.result.score": "$.nodeOutput.score"
}
```

规则：

- Key 表示写回目标路径。
- Value 表示节点输出来源路径。
- 写回前必须先校验节点输出是否满足 `outputSchemaRef`。

## 6. 校验时机

| 时机 | 触发点 | 说明 |
|------|--------|------|
| START | 接收运行输入 | 校验调用入参 |
| LLM / REVIEW / SUMMARY | 配置了 outputSchema 时 | 校验模型输出 |
| JAVA_METHOD | 调用前后 | 校验入参和返回值 |
| TOOL | 调用前后 | 校验工具参数和返回值 |
| AGENT_CALL | 子 Agent 输入和输出 | 校验父子链路两侧结构 |
| EXTERNAL_AGENT | 输出写回前 | 可选校验外部结果 |
| END | 运行收口前 | 校验最终结果结构 |
| 发布校验 | 发布工作流时 | 校验引用 Schema 是否存在 |

## 7. 校验失败结果结构

`schema_validation_result_json` 建议采用以下结构：

```json
{
  "valid": false,
  "schemaKey": "agent.input",
  "schemaVersion": 1,
  "errors": [
    {
      "path": "$.input.question",
      "keyword": "required",
      "message": "请输入问题"
    }
  ]
}
```

规则：

- `path` 必须定位到具体字段。
- `message` 必须为中文。
- 失败结果应尽量包含可修复提示，而不是只返回抽象错误码。

## 8. 版本演进规则

- 每次结构变更都创建新版本。
- 版本号整数递增，从 1 开始。
- 已被发布工作流引用的版本只读。
- 草稿工作流可以切换到新版本。
- 历史运行记录必须保留当时使用的 `schema_key + version` 语义。

## 9. 创建和锁定流程

```text
创建 Schema 草稿
  -> 编写 JSON Schema
  -> 保存为 DRAFT
  -> 被工作流草稿引用
  -> 工作流发布时将引用版本标记为 ACTIVE，并由发布引用关系派生 locked=true
  -> 结构变更创建新版本
```

规则：

- `status` 表达生命周期：`DRAFT`、`ACTIVE`、`ARCHIVED`。
- `locked` 表达是否被已发布工作流引用，只能由系统派生维护。
- 只有 `status=DRAFT` 且 `locked=false` 的版本允许更新。
- 已锁定版本需要结构变更时，必须基于旧版本创建新版本。

## 10. Java 类型约束

`java_type` 不是跨层契约的主来源，只在执行边界和展示层使用。

建议约束：

- 仅记录完全限定类名。
- 仅用于可选的 POJO 转换或界面提示。
- 不允许把 Java 类型直接当作工作流持久化契约。

## 11. 前端渲染规则

前端可以基于 SchemaDefinition 生成或驱动以下表单：

- Agent 调试输入表单。
- START 节点输入配置表单。
- 节点验收用例输入表单。
- Java 方法参数查看表单。
- 工具参数查看表单。

渲染层只负责输入体验，校验仍以后端 Schema 校验为准。
