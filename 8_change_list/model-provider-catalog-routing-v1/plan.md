# 模型供应商目录与运行路由 v1 计划

## 1. 范围

本次变更覆盖产品、前端、后端、数据结构、REST 接口、迁移和验收。

受影响模块：

- 后端 `model` 模块：供应商目录、模型供应项、运行路由。
- 后端 `agent` 模块：默认模型字段迁移为默认模型供应项。
- 后端 `workflow` 模块：LLM 类节点配置和发布校验。
- 后端 `runtime` 模块：模型调用请求解析供应项。
- 后端 `settings` 模块：移除默认模型作为长期配置项。
- 前端 `model-providers` 页面：供应商和模型供应项管理。
- 前端 `agents` 页面：默认模型供应项选择器。
- 前端 `workflow` 页面：LLM 类节点模型供应项选择器。
- 数据库迁移：新增模型供应商和模型供应项表，并迁移旧配置。

## 2. 分阶段工作

### 2.1 数据模型与迁移

1. 新增 `model_provider` 表。
2. 新增 `model_offering` 表，使用 `model_key` 字段表达跨供应商模型身份。
3. 将现有 `agent_definition.default_model` 迁移为 `default_model_offering_key` 或等价字段。
4. 将历史工作流节点 `config.model` 迁移为 `config.modelOfferingKey`。
5. 将历史 Eval `scoreRule.model` 迁移为 `scoreRule.modelOfferingKey`。
6. 从 `AGENT_STUDIO_OPENAI_*` 初始化内置供应商和默认供应项。

### 2.2 后端能力

1. 新增模型供应商应用服务和 REST API。
2. 新增模型供应项应用服务和 REST API。
3. 模型网关根据 `modelOfferingKey` 解析供应商连接信息。
4. 发布校验统一校验模型供应项引用。
5. 移除 LLM 类节点新写入 `config.model` 的成功路径。
6. 将模型调用请求、内部解析对象和 Trace 安全 DTO 分离。

### 2.3 前端能力

1. 增加模型供应商页面。
2. 支持供应商创建、编辑、启停、密钥更新和连接测试。
3. 支持供应项创建、编辑、启停和按 key 回填已绑定项。
4. Agent 默认值表单改为模型供应项选择器。
5. Workflow LLM 类节点改为模型供应项选择器。

### 2.4 文档与验收

1. 同步产品、架构、数据结构和接口文档。
2. 增加后端单元测试、迁移测试、API 测试和前端测试。
3. 增加 Docker 启动初始化默认供应商的复验。

## 3. 迁移策略

- 初次启动时，如果模型目录为空，使用 `AGENT_STUDIO_OPENAI_BASE_URL`、`AGENT_STUDIO_OPENAI_API_KEY` 和 `AGENT_STUDIO_OPENAI_DEFAULT_MODEL` 创建默认供应商和默认模型供应项。
- 旧 `agent_definition.default_model` 如果等于某个已创建供应项的上游模型名，则迁移到该供应项；否则创建同名默认供应项后迁移。
- 历史 `workflow_version.nodes_json[*].config.model` 迁移为 `modelOfferingKey`。
- 历史 `eval_case.score_rule_json.model` 迁移为 `modelOfferingKey`。
- 迁移完成后，新保存路径不得继续写 `config.model`。
- 迁移完成后，新运行路径不得继续读 `scoreRule.model`。
- 如果历史节点无法解析到模型供应项，发布校验必须失败并定位具体节点，不能静默回退到部署默认模型。

## 4. 风险

- API Key 存储和回显必须严格控制，不能通过普通接口泄露。
- 旧模型自由文本迁移必须可追溯，不能把不同供应商的同名模型错误合并。
- 运行 Trace 需要保留模型路由元数据，但不能记录密钥。
- 前端选择器必须从模型供应项目录加载，不能继续允许普通用户手写模型名作为成功路径。
- 已绑定但当前不可选的供应项必须能按 key 查询回填，不能依赖分页列表偶然命中。
- Eval LLM 评分必须和普通 LLM 节点共用模型供应项路由，不能继续读取 `scoreRule.model`。

## 5. 不做的复杂化

- 不做多协议抽象框架，只实现 `OPENAI_COMPATIBLE`。
- 不做模型能力自动探测。
- 不做自动拉取供应商模型列表。
- 不做用户级 API Key。
- 不做独立模型定义目录；`model_offering.model_key` 足够表达 V1 跨供应商模型身份。
- 不引入独立密钥服务；本机/内网单用户部署下由数据库保存可逆密文和展示掩码，运行时通过后端统一组件解密。
