# 架构单一真相强制落地 v1 设计

## 1. 总体原则

每个业务事实只能有一个正式来源。本变更不接受“兼容旧字段”“保留默认值兜底”“先用高级 JSON 顶住”“metadata 失败时用本地数组兜底”等折中方案。

本变更的正式优先级高于早前变更包中与之冲突的内容。

## 2. 模型供应商与启动行为

### 2.1 正式事实源

模型供应商和模型供应项只来自数据库中的 `model_provider` 和 `model_offering`。

不存在以下正式事实源：

- `AGENT_STUDIO_OPENAI_API_KEY`
- `AGENT_STUDIO_OPENAI_BASE_URL`
- `AGENT_STUDIO_OPENAI_DEFAULT_MODEL`
- `spring.ai.openai.*`
- Flyway 迁移中的默认 provider 初始化逻辑

### 2.2 fresh install 行为

空库首次启动后：

- 系统必须启动成功。
- Web 页面必须可访问。
- 模型供应商列表允许为空。
- 模型供应项列表允许为空。
- 不自动创建默认供应商。
- 不自动创建默认模型供应项。
- 不要求部署者提供模型 API Key。

### 2.3 模型调用行为

模型调用必须先解析 `modelOfferingKey`：

1. LLM/REVIEW/SUMMARY 节点优先使用节点配置的 `modelOfferingKey`。
2. 节点未配置时，可以使用 Agent 的 `defaultModelOfferingKey`，前提是该字段本身来自页面配置的供应项。
3. 如果最终没有可用 `modelOfferingKey`，发布校验或运行调用必须失败。
4. 失败消息必须明确提示用户配置模型供应商和模型供应项。

禁止行为：

- 禁止回退到部署默认模型。
- 禁止回退到 `AGENT_STUDIO_OPENAI_DEFAULT_MODEL`。
- 禁止供应商不可用时自动选择另一个默认供应商。
- 禁止在启动期因缺少模型配置失败。

### 2.4 Spring AI 使用边界

可以继续使用 Spring AI 的 OpenAI-compatible 客户端能力，但不能使用 starter 自动装配出的静态默认 OpenAI 客户端作为运行入口。

正式调用方式必须是：

- 从数据库解析 `ResolvedModelRoute`。
- 用 route 的 `baseUrl`、解密后的 API Key、`upstreamModelName` 和温度参数显式构造客户端或请求。
- 每次调用按 route 动态决定供应商。

如果当前依赖 `spring-ai-starter-model-openai` 会保留静态自动配置入口，应改为非 starter 模块或等价客户端库。

## 3. 工作流默认边合同

### 3.1 正式表达

默认边只允许：

```json
{
  "type": "DEFAULT"
}
```

显式条件边只允许：

```json
{
  "type": "CONDITION",
  "condition": {
    "left": "$.input.score",
    "operator": "GREATER_THAN",
    "valueType": "NUMBER",
    "right": 80
  }
}
```

### 3.2 删除字段

`WorkflowEdgeDefinition.isDefault` 不属于正式合同。

后端 DTO、领域对象、OpenAPI、前端生成类型、前端页面逻辑、测试数据和正式文档都不得出现 `isDefault`。

### 3.3 旧数据

用户已确认旧数据会被删除。本变更不处理旧数据兼容，不做 `isDefault -> type=DEFAULT` 的迁移脚本。

## 4. Metadata 接口

### 4.1 接口

正式接口：

`GET /api/platform-metadata`

### 4.2 响应结构

建议结构：

```json
{
  "nodeTypes": [
    { "value": "START", "label": "开始节点" }
  ],
  "modelNodeTypes": [
    { "value": "LLM", "label": "LLM 节点" }
  ],
  "edgeTypes": [
    { "value": "DEFAULT", "label": "默认分支" }
  ],
  "conditionOperators": [
    { "value": "EQUALS", "label": "等于" }
  ],
  "conditionValueTypes": [
    { "value": "STRING", "label": "字符串" }
  ],
  "hardCheckTypes": [
    { "value": "JSON_PATH_EXISTS", "label": "JSONPath 存在" }
  ]
}
```

后端必须从枚举或统一常量构造响应，不得在 Controller 里散写第二套字符串。

### 4.3 前端规则

- 工作流节点类型来自 metadata。
- 模型类节点判断来自 metadata。
- 边类型来自 metadata。
- 条件操作符和值类型来自 metadata。
- Eval hardChecks 类型来自 metadata。
- metadata 加载失败时页面显示中文错误，不使用本地旧数组兜底。

## 5. 结构化 UI

### 5.1 普通用户主路径

普通用户配置工作流节点时，必须通过结构化表单完成核心字段配置。

高级 JSON 不得作为普通用户主配置入口。

### 5.2 节点配置要求

LLM、REVIEW、SUMMARY：

- `modelOfferingKey`
- `systemPromptTemplate`
- `userPromptTemplate`
- `temperature`
- `structuredOutput`
- 输入/输出 Schema 引用
- 输入/输出映射

CONDITION：

- 默认分支选择。
- 显式条件分支配置。
- `left` JSONPath。
- `operator`。
- `valueType`。
- `right`。

JAVA_METHOD：

- `methodKey` 远程选择。
- 输入/输出 Schema 展示。
- 输入/输出映射。

TOOL：

- `toolKey` 远程选择。
- 输入/输出 Schema 展示。
- 输入/输出映射。

AGENT_CALL：

- `targetAgentKey` 远程选择。
- 调用深度和发布状态提示。
- 输入/输出映射。

EXTERNAL_AGENT：

- `adapterKey` 远程选择。
- resultSource 展示或配置入口。
- 输入/输出映射。

START、END：

- 输入或输出 Schema 引用。
- 必要说明。

### 5.3 高级 JSON 定位

高级 JSON 可以保留为以下任一形式：

- 只读预览。
- 专家模式。
- 排障用视图。

但高级 JSON 不能绕过结构化表单校验，不能成为用户完成核心配置的唯一方式。

## 6. Trace 配置

`agent.studio.trace.persist-full-model-content` 不是正式需求。

正式文档只描述当前真实保存行为，不声明不存在的开关。

## 7. 注释规范

开发规范调整为：

- 类、字段、公共业务方法、复杂私有业务方法需要中文 Javadoc 或必要中文注释。
- getter/setter 不强制 Javadoc。
- 构造函数和简单 DTO 结构方法不强制 Javadoc。
- 方法内部注释解释业务流程、状态转换、错误处理和副作用，不机械复述代码。

## 8. 文档一致性

必须同步更新：

- `0_specifications/develop_specification.md`
- `3_product_design/`
- `4_arch_design/`
- `5_ui_design/`
- `6_schema_design/`
- `7_interface_design/`
- `11_code/README.md`
- 根 `README.md`
- `13_release/`
- `14_user_manual/`

历史变更包和历史验收记录可以保留原文，但当前正式文档不得继续扩散旧结论。
