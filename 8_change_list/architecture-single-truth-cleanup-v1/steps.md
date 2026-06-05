# 架构单一真相清理 v1 实施步骤

## 1. 准备

1. 阅读本变更包的 `purpose.md`、`plan.md`、`design.md`。
2. 阅读 `0_specifications/develop_specification.md` 和 `0_specifications/code_review_specification.md`。
3. 执行静态检索，建立待清理清单：

```powershell
rg -n "AGENT_STUDIO_OPENAI|isDefault|persist-full-model-content|assertionRules|judgeRules|isCritical|confirmationStatus" D:\myproject\MyAgent
```

4. 不要从旧变更包复制和本方案冲突的实现要求。

## 2. 移除 `AGENT_STUDIO_OPENAI_*`

1. 修改 `compose.yaml`，删除 API 服务中的 `AGENT_STUDIO_OPENAI_API_KEY`、`AGENT_STUDIO_OPENAI_BASE_URL`、`AGENT_STUDIO_OPENAI_DEFAULT_MODEL`。
2. 修改后端 `application.yml`，删除 `spring.ai.openai.*` 对上述环境变量的映射。
3. 删除 `LegacyOpenAiEnvironmentGuard` 或把它改为不再识别 `AGENT_STUDIO_OPENAI_*`。
4. 删除或重写对应测试：
   - `LegacyOpenAiEnvironmentGuardTests`
   - `ApplicationConfigurationContractTests`
5. 修改 `V4__model_provider_catalog_routing_v1` 或后续初始化代码，不再读取 `System.getenv("AGENT_STUDIO_OPENAI_*")`。
6. 移除 `spring-ai-starter-model-openai` 的自动配置入口；如果仍需 Spring AI OpenAI 类库，改用非 starter 模块或等价客户端库。
7. 确保 `OpenAiCompatibleModelInvoker` 继续按数据库解析出的 provider route 显式构造客户端。
8. 确认模型目录为空时应用启动成功。
9. 确认模型调用时如果没有可用 `modelOfferingKey`，返回明确中文错误。

## 3. 统一 EvalCase 文档

1. 修改产品文档中的 EvalCase 字段列表，删除旧字段。
2. 修改架构文档，确保 `scoreRule` 是唯一 LLM 辅助评分规则。
3. 修改接口文档，确认 `scoreRule.modelOfferingKey` 是唯一可写模型引用。
4. 检索并清理旧词：

```powershell
rg -n "assertionRules|judgeRules|isCritical|confirmationStatus|scoreRule\.model" D:\myproject\MyAgent\3_product_design D:\myproject\MyAgent\4_arch_design D:\myproject\MyAgent\7_interface_design D:\myproject\MyAgent\14_user_manual
```

5. 如果代码中已经禁止 `scoreRule.model`，保持该校验，不要恢复旧字段。

## 4. 删除 `isDefault`

1. 修改 `WorkflowEdgeDefinition`，删除字段、getter 和 setter。
2. 修改工作流校验逻辑，默认边只判断 `edge.getType() == WorkflowEdgeType.DEFAULT`。
3. 修改运行时执行逻辑，默认边只判断 `type=DEFAULT`。
4. 修改前端 `WorkflowPage.tsx`：
   - 删除 `isDefaultEdge` 对 `definition.isDefault` 的读取。
   - 删除 `delete nextDefinition.isDefault` 等旧字段处理。
   - 保存和回填只使用 `type=DEFAULT`。
5. 修改前端测试，不再构造 `isDefault=true` 的历史数据。
6. 重新生成 OpenAPI 前端 schema，确认不再包含 `isDefault`。
7. 不新增旧数据迁移脚本；当前旧数据会被删除。
8. 清理文档中所有 `isDefault` 表述。

## 5. 新增 metadata 接口

1. 后端新增 platform metadata application/service/controller。
2. 从后端枚举或统一常量构造 metadata 响应，不在 Controller 中散写字符串。
3. 接口至少返回：
   - `nodeTypes`
   - `modelNodeTypes`
   - `edgeTypes`
   - `conditionOperators`
   - `conditionValueTypes`
   - `evalAssertionTypes`
4. 每个枚举项至少包含 `value` 和中文 `label`。
5. 接口路径固定为 `GET /api/platform-metadata`。
6. 更新 `4_arch_design/12-公共接口架构设计-v1.md` 和 `7_interface_design/02-对外REST接口-v1.md`。
7. 前端新增 platform metadata API 调用。
8. 前端工作流页面和验收页面改用 platform metadata 返回值。
9. metadata 加载失败时显示中文错误状态，不使用本地旧列表兜底。

## 6. 删除 trace 假配置

1. 删除文档中的 `agent.studio.trace.persist-full-model-content`。
2. 不新增同名配置。
3. 如果文档需要描述 Trace 保存行为，只描述当前实现真实行为。

## 7. 调整注释规范

1. 修改 `0_specifications/develop_specification.md`。
2. 明确 getter/setter 不需要 Javadoc。
3. 明确构造函数和简单 DTO 结构方法不强制 Javadoc。
4. 保留复杂业务方法需要中文注释的要求。

## 8. 文档同步

1. 更新根 README 和 `11_code/README.md`，删除所有 `AGENT_STUDIO_OPENAI_*` 启动说明。
2. 更新 `13_release/` 发布检查和部署说明。
3. 更新涉及模型配置和节点验收的用户手册。
4. 更新相关架构、schema、接口文档。
5. 对旧变更包中冲突内容不做逐个历史改写时，至少在本变更 `status.md` 中说明本方案取代旧结论。

## 9. 禁止做法

- 禁止把 `AGENT_STUDIO_OPENAI_*` 改名后继续作为默认模型配置。
- 禁止继续使用 Spring Boot OpenAI starter 自动配置静态默认模型客户端。
- 禁止新增默认供应商或默认模型。
- 禁止保留 `isDefault` 读取兼容。
- 禁止为 `isDefault` 编写旧数据迁移脚本；旧数据会被删除。
- 禁止在前端写死 metadata 兜底枚举。
- 禁止实现 `agent.studio.trace.persist-full-model-content`。
- 禁止只改文档不改代码，或只改代码不改文档。
