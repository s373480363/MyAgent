# 架构单一真相强制落地 v1 修改计划

## 1. 范围

本变更覆盖 7 类问题：

| 编号 | 问题 | 处理方向 |
| --- | --- | --- |
| 1 | 最高优先级架构清理未落地 | 将本变更作为当前正式开发依据，取代旧冲突结论 |
| 2 | `AGENT_STUDIO_OPENAI_*` 和默认供应商仍存在 | 全量移除，不保留环境变量初始化或默认目录 |
| 3 | 发布目标仍要求 OpenAI Key 才能启动 | 改为系统先启动，用户页面配置供应商 |
| 4 | `isDefault` 与 `type=DEFAULT` 双轨 | 删除 `isDefault`，只保留 `type=DEFAULT` |
| 5 | 前后端枚举多处维护 | 新增 `GET /api/platform-metadata` 作为枚举单一真相 |
| 6 | Trace 假配置仍在文档 | 删除文档表述，不实现配置 |
| 7 | 高级 JSON 作为普通用户正式配置入口 | 补齐结构化 UI，高级 JSON 降级为非主路径 |

## 2. 影响模块

- 部署入口：`11_code/compose.yaml`、根 `README.md`、`11_code/README.md`、`13_release/`。
- 后端配置：`application.yml`、旧环境变量守卫、Spring AI starter 自动配置入口、配置契约测试。
- Flyway：`V4__model_provider_catalog_routing_v1` 不得读取环境变量创建默认供应商。
- 模型供应商：页面创建供应商和供应项成为唯一配置入口。
- 模型调用：继续使用数据库路由解析和动态客户端构造，不依赖默认 provider。
- 工作流：边定义、校验、运行时、前端编辑器、OpenAPI 生成类型。
- 平台 metadata：后端只读接口、前端 API client、工作流页面、Eval 页面。
- Trace 文档：架构、接口、发布、测试说明。
- UI：工作流节点配置面板、边配置面板、普通用户配置流程。
- 规范：`0_specifications/develop_specification.md`。

## 3. 执行阶段

### 阶段一：冻结本变更为当前正式依据

- 将本变更记录为当前架构清理的执行变更。
- 明确 `architecture-single-truth-cleanup-v1` 中与本变更一致的内容继续有效。
- 明确 `architecture-single-truth-cleanup-v1` 中与 `llm-node-eval-judge-rule-v1` 冲突的 Eval 旧字段目标不再作为当前正式语义。
- 后续开发不得继续按旧变更包中的默认供应商、`isDefault` 或 Trace 假配置实现。

### 阶段二：移除默认模型供应商和 OpenAI 环境变量入口

- 删除 Docker Compose 中的 `AGENT_STUDIO_OPENAI_API_KEY`、`AGENT_STUDIO_OPENAI_BASE_URL`、`AGENT_STUDIO_OPENAI_DEFAULT_MODEL`。
- 删除 `application.yml` 中 `spring.ai.openai.*` 对上述环境变量的映射。
- 删除或改造旧环境变量守卫，使其不再把 `AGENT_STUDIO_OPENAI_*` 当成正式变量。
- 修改 Flyway V4 或后续迁移逻辑，禁止读取 `System.getenv("AGENT_STUDIO_OPENAI_*")`。
- 确认空库启动时不会自动创建 `openai-default`、`openai.gpt_4_1_mini` 或任何默认供应项。
- 模型调用解析不到 `modelOfferingKey` 时失败并返回明确中文错误。

### 阶段三：调整发布目标和文档

- 发布说明改为“系统先启动，用户页面配置供应商”。
- 发布检查清单不再要求 OpenAI Key。
- 用户手册补充首次启动后的模型供应商配置流程。
- 测试环境说明增加空模型目录启动验收。

### 阶段四：删除 `isDefault`

- 删除后端 `WorkflowEdgeDefinition.isDefault` 字段和访问器。
- 工作流校验和运行时只根据 `type=DEFAULT` 判断默认边。
- 前端删除 `isDefault` 读取、删除、归一化和测试构造。
- 重新生成 OpenAPI 和前端 schema。
- 不编写旧数据迁移脚本；旧工作流数据按用户确认清理。

### 阶段五：新增 metadata 单一真相接口

- 新增 `GET /api/platform-metadata`。
- 至少返回节点类型、模型节点类型、边类型、条件操作符、条件值类型、hardCheck 类型。
- 每个枚举项包含 `value`、中文 `label`，必要时包含 `description`。
- 前端工作流和 Eval 页面读取 metadata。
- metadata 加载失败时显示错误，不使用本地业务枚举兜底。

### 阶段六：补齐结构化 UI

- 普通用户配置节点时，核心字段必须通过结构化表单完成。
- LLM/REVIEW/SUMMARY：模型供应项、系统提示词、用户提示词、温度、结构化输出、Schema 引用等。
- CONDITION：默认分支、显式分支、JSONPath、操作符、值类型、右值。
- JAVA_METHOD、TOOL、AGENT_CALL、EXTERNAL_AGENT：对应目录选择器和必要配置字段。
- START/END：输入输出 Schema 和必要说明。
- 高级 JSON 不作为普通用户主配置入口，可保留为只读预览或专家模式，但不能绕过正式表单校验。

### 阶段七：删除 Trace 假配置并调整注释规范

- 删除所有正式文档中的 `agent.studio.trace.persist-full-model-content`。
- 不新增同名代码配置。
- 修改开发规范，明确 getter/setter、构造函数、简单 DTO 结构方法不强制 Javadoc。

## 4. 风险

- 旧部署依赖 `AGENT_STUDIO_OPENAI_*` 会失效，这是预期行为。
- 空库首次启动后不能直接执行模型调用，必须先由用户配置供应商，这是当前发布目标。
- 删除 `isDefault` 后旧数据无法读取，这是用户已确认的取舍。
- metadata 成为前端页面加载依赖，需要设计清晰错误状态。
- 结构化 UI 改造影响面大，必须按节点类型逐步补齐并加强前端测试。

## 5. 依赖和前提

- `llm-node-eval-judge-rule-v1` 已完成，Eval 当前正式语义为 `judgeRule`、`referenceSample`、`hardChecks`。
- 现有模型供应商页面、供应项管理、密钥接口和动态模型路由继续复用。
- OpenAPI 仍是前后端接口契约生成来源。
- 正式验收必须使用 Docker Compose 入口。
