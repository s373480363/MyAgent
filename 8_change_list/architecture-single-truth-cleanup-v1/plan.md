# 架构单一真相清理 v1 修改计划

## 1. 范围

本次变更覆盖 6 类问题：

| 编号 | 问题 | 处理方向 |
| --- | --- | --- |
| 1 | `AGENT_STUDIO_OPENAI_*` 仍作为默认供应商和部署入口存在 | 全量移除，不做替代默认供应商 |
| 2 | EvalCase 产品文档与接口、架构、代码不一致 | 统一为当前 `scoreRule` 和正式 EvalCase 合同 |
| 3 | 工作流默认边 `isDefault` 与 `type=DEFAULT` 双轨 | 删除 `isDefault`，只保留 `type=DEFAULT` |
| 4 | 前后端手工维护多套枚举 | 新增后端只读 metadata 接口作为长期真相 |
| 5 | `agent.studio.trace.persist-full-model-content` 文档假配置 | 从文档中删除，不实现 |
| 6 | 注释规范与实际口径不一致 | 规范明确 getter/setter 不需要 Javadoc |

## 2. 影响模块

- 部署入口：`compose.yaml`、根 `README.md`、`11_code/README.md`、`13_release/`。
- 后端配置：`application.yml`、OpenAI 旧环境变量守卫、Spring AI OpenAI starter 自动配置入口、相关测试。
- 模型供应商目录：启动后允许空目录，运行和发布阶段按已有目录解析供应项。
- 节点验收：产品文档、架构文档、接口文档、DTO 校验和测试说明。
- 工作流：`WorkflowEdgeDefinition`、工作流校验、运行时边选择、前端工作流编辑器、OpenAPI 生成类型和测试。
- 公共接口：新增 metadata 只读接口。
- 前端：枚举选项来源、工作流页面、必要的 API client 调用。
- 规范文档：开发规范中的 Javadoc 要求。

## 3. 执行阶段

### 阶段一：移除错误部署和模型初始化真相

- 删除 `compose.yaml` 中全部 `AGENT_STUDIO_OPENAI_*` 环境变量。
- 删除 `application.yml` 中 `spring.ai.openai.*` 对 `AGENT_STUDIO_OPENAI_*` 的映射。
- 删除或重写 `LegacyOpenAiEnvironmentGuard` 及其测试。
- 删除从环境变量创建默认供应商或默认模型供应项的初始化路径。
- 移除 Spring Boot OpenAI starter 自动配置入口；模型调用只允许按数据库供应商路由动态构造客户端。
- 确认全新数据库启动后模型目录为空是合法状态。

### 阶段二：统一 EvalCase 产品和接口文档

- 产品文档删除旧字段和旧概念。
- 统一使用 `caseNo`、`title`、`input`、`referenceAnswer`、`assertions`、`scoreRule`、`critical`、`confirmStatus`。
- 删除“机器评分规则”和“人工判断规则”两套概念，只保留 `scoreRule` 表达可选 LLM 辅助评分。
- 文档中不得继续把 `scoreRule.model` 描述成可写字段。

### 阶段三：激进删除 `isDefault`

- 后端领域对象删除 `isDefault` 字段和 getter/setter。
- 工作流校验只根据 `edge.type == DEFAULT` 判断默认边。
- 运行时分支选择只根据 `edge.type == DEFAULT` 判断默认边。
- 前端删除 `isDefaultEdge` 中对 `definition.isDefault` 的判断。
- 前端保存、回填、测试全部只使用 `type=DEFAULT`。
- OpenAPI 重新生成后不得包含 `isDefault`。
- 不编写旧数据迁移脚本；当前系统旧数据会被删除，开发和验收只面向清空旧数据后的正式结构。

### 阶段四：新增 metadata 单一真相接口

- 后端新增只读接口 `GET /api/platform-metadata`。
- 接口返回前端需要展示的枚举选项，至少包含：
  - 工作流节点类型。
  - LLM 类节点类型。
  - 工作流边类型。
  - CONDITION 操作符。
  - CONDITION 值类型。
  - Eval 断言类型。
- 每个枚举项必须包含 `value` 和中文 `label`，必要时包含 `description` 和 `category`。
- 前端工作流编辑器删除页面内手工枚举真相，改为读取 metadata。

### 阶段五：删除 trace 假配置

- 删除所有 `agent.studio.trace.persist-full-model-content` 文档表述。
- 不新增任何同名配置项。
- 如果文档需要描述 Trace 保存策略，只描述当前真实行为。

### 阶段六：注释规范调整

- 修改 `0_specifications/develop_specification.md`。
- 明确 getter/setter、构造函数、简单 DTO 结构方法不要求 Javadoc。
- 保留类、字段、业务方法、复杂私有方法需要中文 Javadoc 或必要中文注释的要求。

## 4. 风险

- 删除 `AGENT_STUDIO_OPENAI_*` 会使旧部署脚本失效，这是预期行为，不作为兼容缺陷处理。
- 删除 `isDefault` 后不处理旧工作流数据；旧数据会被删除，这是本次方案的前提。
- 移除 Spring AI OpenAI starter 自动配置后，需要确保动态构造 OpenAI-compatible 客户端的代码仍可编译、可测试、可按 provider 路由。
- metadata 接口会引入前端启动时的数据依赖，需要明确加载失败时的 UI 错误状态。
- 注释规范调整会改变后续代码审查口径，需要同步给开发人员。

## 5. 已确认决策

用户已确认以下细节：

- metadata 接口路径采用 `GET /api/platform-metadata`。
- 当前系统不存在旧数据迁移问题，所有旧数据都会被删除。
- Spring AI OpenAI starter 的自动配置方式不适合多 provider 动态路由；后续实现应移除 starter 自动配置入口，保留或替换为可按供应商路由显式构造客户端的库用法。
