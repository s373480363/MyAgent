# 架构单一真相清理 v1 设计

## 1. 总体设计原则

本变更的核心原则是：每个业务事实只能有一个正式来源。

因此，本次不接受“保留旧字段做兼容”“保留旧环境变量做初始化”“文档保留历史说明”“前端继续手工维护一份枚举”等折中方案。旧路径如果不是正式需求，就删除；需要保留的业务能力必须通过当前正式模型表达。

## 2. 模型供应商与启动行为

### 2.1 正式事实源

模型供应商和模型供应项的正式事实源只来自数据库中的 `ModelProvider` 和 `ModelOffering`。

系统不再存在默认供应商、默认模型或默认供应项。

### 2.2 启动规则

- 没有任何模型供应商时，系统必须可以启动。
- 没有任何模型供应项时，系统页面必须可以访问。
- 需要模型调用的功能在解析不到可用 `modelOfferingKey` 时失败，并返回明确中文错误。
- 应用启动期不得因为缺少模型配置而失败。

### 2.3 环境变量规则

正式部署环境变量不再包含：

- `AGENT_STUDIO_OPENAI_API_KEY`
- `AGENT_STUDIO_OPENAI_BASE_URL`
- `AGENT_STUDIO_OPENAI_DEFAULT_MODEL`

`AGENT_STUDIO_SECRET_KEY` 继续保留，用于供应商 API Key 加解密。

### 2.4 迁移规则

原先从 `AGENT_STUDIO_OPENAI_*` 初始化默认供应商的迁移逻辑必须删除。

当前系统不保留旧数据，所有旧数据都会被删除。因此本变更不设计旧模型供应商数据迁移，不保留从旧环境变量到新供应商目录的导入路径。

### 2.5 Spring AI 使用边界

多 provider 场景不能依赖 Spring Boot OpenAI starter 自动装配出的静态 OpenAI 客户端。

原因是 starter 的主要价值是把部署配置绑定成一个应用级默认模型 bean，而本项目的正式路由需要在每次调用时根据 `modelOfferingKey` 解析出不同 provider 的 `baseUrl`、`apiKey`、`upstreamModelName` 和默认参数。应用级默认 bean 会把模型供应商重新固定到启动配置层，和本次删除默认供应商的目标冲突。

后续实现可以继续使用 Spring AI 的 OpenAI-compatible 客户端和模型构造能力，但必须使用显式动态构造方式：

- 每次模型调用先从数据库解析 `ResolvedModelRoute`。
- 使用该 route 的 `baseUrl` 和解密后的 API Key 构造客户端。
- 使用该 route 的 `upstreamModelName` 和请求参数构造模型选项。
- 不通过 `spring.ai.openai.*`、环境变量或自动装配默认 bean 决定供应商。

如果当前依赖只能通过 `spring-ai-starter-model-openai` 引入相关类，开发人员应改成非 starter 的 Spring AI OpenAI 模块或等价客户端库。最终判断标准不是依赖名，而是系统中不得存在静态 OpenAI 自动配置入口。

## 3. EvalCase 统一合同

### 3.1 正式 EvalCase 字段

文档和接口统一使用：

- `caseNo`
- `title`
- `input`
- `referenceAnswer`
- `assertions`
- `scoreRule`
- `critical`
- `confirmStatus`
- `description`
- 来源字段：`sourceRunId`、`sourceNodeRunId`、`sourceWorkflowVersionId`、`sourceNodeId`

### 3.2 删除旧概念

以下表述不再是正式产品概念：

- `assertionRules`
- `judgeRules`
- `isCritical`
- `confirmationStatus`
- “机器评分规则”
- “人工判断规则”

如果文档中需要解释历史字段，只能出现在迁移说明或审计记录中，不能出现在正式产品对象定义里。

### 3.3 scoreRule 规则

`scoreRule` 表达可选 LLM 辅助评分。

`scoreRule.modelOfferingKey` 是唯一模型供应项字段。`scoreRule.model` 不属于正式接口契约，不允许新写入。

## 4. 工作流默认边合同

### 4.1 正式表达

CONDITION 默认边只用：

```json
{
  "type": "DEFAULT"
}
```

显式条件边只用：

```json
{
  "type": "CONDITION",
  "condition": {
    "left": "$.field",
    "operator": "EQUALS",
    "valueType": "STRING",
    "right": "value"
  }
}
```

### 4.2 删除字段

`WorkflowEdgeDefinition.isDefault` 必须删除。

OpenAPI schema、前端生成类型、前端页面逻辑、后端测试、历史文档都不得再出现 `isDefault`。

### 4.3 旧数据处理

当前系统不处理旧数据迁移，所有旧数据都会被删除。

因此，开发人员只需要删除 `isDefault` 的代码、接口、前端和测试残留，不需要编写 `isDefault` 到 `type=DEFAULT` 的数据修复脚本。验收环境必须以清空旧数据后的新库为准。

## 5. Metadata 接口

### 5.1 接口定位

metadata 接口是前端枚举选项的长期单一真相。

正式接口：

`GET /api/platform-metadata`

### 5.2 响应结构

建议响应：

```json
{
  "nodeTypes": [
    { "value": "START", "label": "开始节点" }
  ],
  "modelNodeTypes": [
    { "value": "LLM", "label": "LLM 节点" }
  ],
  "edgeTypes": [
    { "value": "DEFAULT", "label": "默认边" }
  ],
  "conditionOperators": [
    { "value": "EQUALS", "label": "等于" }
  ],
  "conditionValueTypes": [
    { "value": "STRING", "label": "字符串" }
  ],
  "evalAssertionTypes": [
    { "value": "JSON_PATH_EXISTS", "label": "JSONPath 存在" }
  ]
}
```

正式 DTO 应使用后端枚举或统一常量构造，不能在 Controller 中再次手写一份无来源列表。

### 5.3 前端规则

- 工作流编辑器中的节点类型、LLM 类节点判断、条件操作符、条件值类型必须来自 `GET /api/platform-metadata`。
- 验收用例编辑中的断言类型必须来自 `GET /api/platform-metadata`。
- 前端可以为了 UI 排序保留展示顺序，但不能自行新增或删除业务枚举值。
- metadata 加载失败时，页面应显示明确错误，不使用内置旧列表兜底。

## 6. Trace 配置

`agent.studio.trace.persist-full-model-content` 不是正式需求。

所有正式文档必须删除该配置。代码不得新增该配置。

Trace 当前保存策略按现有真实实现描述，不写不存在的开关。

## 7. 注释规范

开发规范调整为：

- 类、字段、公共业务方法、复杂私有业务方法需要中文 Javadoc 或必要中文注释。
- getter/setter 不需要 Javadoc。
- 构造函数和简单 DTO 结构方法不强制要求 Javadoc。
- 方法内部注释应解释业务流程、状态转换、错误处理和副作用，不写机械重复代码含义的注释。

## 8. 文档覆盖要求

必须同步更新：

- `0_specifications/develop_specification.md`
- `3_product_design/Agent管理平台-产品设计-07-节点验收设计-v1.md`
- `3_product_design/Agent管理平台-产品设计-03-核心概念与领域模型-v1.md`
- `4_arch_design/06-节点体系架构设计-v1.md`
- `4_arch_design/08-Trace追踪与调试架构设计-v1.md`
- `4_arch_design/10-节点验收架构设计-v1.md`
- `4_arch_design/11-数据结构架构设计-v1.md`
- `4_arch_design/12-公共接口架构设计-v1.md`
- `4_arch_design/13-部署安全与质量架构设计-v1.md`
- `6_schema_design/02-领域对象与嵌入结构-v1.md`
- `7_interface_design/02-对外REST接口-v1.md`
- `11_code/README.md`
- `README.md`
- `13_release/`
- `14_user_manual/` 中涉及模型配置、节点验收或工作流配置的说明

早前变更包中和本方案冲突的内容，以本方案为准。开发人员不应继续按旧变更包中的 `AGENT_STUDIO_OPENAI_*`、`isDefault` 或 trace 假配置要求实现。
