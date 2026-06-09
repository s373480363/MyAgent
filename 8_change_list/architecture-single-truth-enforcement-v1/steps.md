# 架构单一真相强制落地 v1 执行步骤

## 1. 准备

1. 阅读本变更的 `purpose.md`、`plan.md`、`design.md`。
2. 阅读 `0_specifications/develop_specification.md` 和 `0_specifications/code_review_specification.md`。
3. 阅读 `8_change_list/llm-node-eval-judge-rule-v1`，确认 Eval 当前正式语义是 `judgeRule`、`referenceSample`、`hardChecks`。
4. 执行静态检索：

```powershell
rg -n "AGENT_STUDIO_OPENAI|isDefault|persist-full-model-content|platform-metadata" D:\myproject\MyAgent
```

5. 区分历史变更记录和当前正式代码/文档。历史记录可以保留，当前正式内容必须清理。

## 2. 移除默认模型供应商入口

1. 修改 `11_code/compose.yaml`：
   - 删除 `AGENT_STUDIO_OPENAI_API_KEY`。
   - 删除 `AGENT_STUDIO_OPENAI_BASE_URL`。
   - 删除 `AGENT_STUDIO_OPENAI_DEFAULT_MODEL`。
2. 修改 `11_code/backend/src/main/resources/application.yml`：
   - 删除 `spring.ai.openai.api-key` 对 `AGENT_STUDIO_OPENAI_API_KEY` 的映射。
   - 确认没有 `spring.ai.openai.*` 静态默认 provider 配置。
3. 删除或重写 `LegacyOpenAiEnvironmentGuard`：
   - 不再把 `AGENT_STUDIO_OPENAI_*` 当作正式替代变量。
   - 不再提示用户改用 `AGENT_STUDIO_OPENAI_*`。
4. 修改相关测试：
   - `LegacyOpenAiEnvironmentGuardTests`
   - `ApplicationConfigurationContractTests`
   - 任何断言 Compose 必须包含 OpenAI Key 的测试。
5. 修改 `V4__model_provider_catalog_routing_v1`：
   - 禁止读取 `System.getenv("AGENT_STUDIO_OPENAI_*")`。
   - 禁止创建默认 provider/offering。
   - 只保留结构迁移和必要字段调整。
6. 检查 `ModelCatalogBootstrapDefaults` 是否仍有必要。
   - 如果只服务默认供应商初始化，删除。
   - 如果仍有通用 normalize 能力，改名并移除默认 provider 语义。

## 3. 保证模型调用只走数据库路由

1. 检查 `DefaultOpenAiModelGateway` 和 `OpenAiCompatibleModelInvoker`。
2. 保留按 `ResolvedModelRoute` 动态构造客户端的能力。
3. 如果 `spring-ai-starter-model-openai` 会触发静态自动配置入口，改为非 starter 模块或等价客户端库。
4. 确认 `modelOfferingKey` 缺失时返回明确中文错误。
5. 确认供应项禁用、供应商禁用、密钥缺失时不回退默认 provider。

## 4. 更新发布目标

1. 修改 `13_release/02-V1部署说明-v1.md`：
   - 删除 OpenAI Key 启动必填说明。
   - 增加“首次启动后在页面配置模型供应商和模型供应项”。
2. 修改 `13_release/01-V1发布前检查清单-v1.md`：
   - 删除 OpenAI Key 启动检查。
   - 增加空模型目录启动检查。
   - 增加页面配置供应商检查。
3. 修改 `11_code/README.md` 和根 `README.md`。
4. 修改 `14_user_manual/`，补充首次配置模型供应商的用户流程。

## 5. 删除 `isDefault`

1. 修改 `WorkflowEdgeDefinition`：
   - 删除 `isDefault` 字段。
   - 删除 `getIsDefault`。
   - 删除 `setIsDefault`。
2. 修改 `DefaultWorkflowDraftValidationService`：
   - 默认边只判断 `edge.getType() == WorkflowEdgeType.DEFAULT`。
   - 错误 detail 不再指向 `$.edges[*].isDefault`。
3. 修改 `ConditionNodeExecutor`：
   - 默认边只判断 `edge.getType() == WorkflowEdgeType.DEFAULT`。
4. 修改前端 `WorkflowPage.tsx`：
   - 删除 `isDefaultEdge` 对 `definition.isDefault` 的读取。
   - 删除 `delete nextDefinition.isDefault`。
   - 保存和 UI 归一化只处理 `type=DEFAULT`。
5. 修改前端测试：
   - 不再构造 `isDefault: true` 历史边。
6. 重新生成 OpenAPI：

```powershell
cd D:\myproject\MyAgent\11_code\frontend
npm run openapi:refresh
npm run openapi:check
```

## 6. 新增 metadata 接口

1. 后端新增 platform metadata 包或模块。
2. 新增 DTO：
   - `PlatformMetadataResponse`
   - `MetadataOption`
3. 新增接口：
   - `GET /api/platform-metadata`
4. 响应至少包含：
   - `nodeTypes`
   - `modelNodeTypes`
   - `edgeTypes`
   - `conditionOperators`
   - `conditionValueTypes`
   - `hardCheckTypes`
5. 后端从枚举或统一常量构造，不在 Controller 散写字符串。
6. 更新 OpenAPI。
7. 前端 `domainApi.ts` 新增 `getPlatformMetadata`。
8. 工作流页面改用 metadata 的节点、边、条件枚举。
9. Eval 页面改用 metadata 的 hardCheck 类型。
10. metadata 加载失败时显示中文错误，不使用本地业务枚举兜底。

## 7. 补齐结构化 UI

1. 梳理当前 `WorkflowPage.tsx` 中仍必须依赖高级 JSON 的核心字段。
2. 按节点类型补齐结构化表单。
3. 保留高级 JSON 时，将其改为只读预览或专家模式。
4. 保存时以结构化表单为主来源。
5. 发布校验错误需要能定位到具体结构化字段。
6. 增加前端测试覆盖每类节点关键配置保存。

## 8. 删除 Trace 假配置

1. 全局检索：

```powershell
rg -n "persist-full-model-content" D:\myproject\MyAgent
```

2. 删除正式文档中的命中。
3. 不新增同名配置。
4. 如果需要描述 Trace 行为，只描述当前真实实现。

## 9. 调整注释规范

1. 修改 `0_specifications/develop_specification.md`。
2. 明确 getter/setter 不强制 Javadoc。
3. 明确构造函数和简单 DTO 结构方法不强制 Javadoc。
4. 保留业务方法和复杂私有方法需要中文说明的要求。

## 10. 禁止做法

- 禁止以任何形式保留 `AGENT_STUDIO_OPENAI_*` 作为正式配置。
- 禁止 fresh install 自动创建默认供应商或默认模型。
- 禁止在模型调用失败时回退默认 provider。
- 禁止保留 `isDefault` 读取兼容。
- 禁止 metadata 加载失败时使用本地业务枚举兜底。
- 禁止把高级 JSON 作为普通用户唯一正式配置入口。
- 禁止实现 `agent.studio.trace.persist-full-model-content`。
- 禁止只改文档不改代码，或只改代码不改文档。
