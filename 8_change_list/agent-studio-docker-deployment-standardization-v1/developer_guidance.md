# 面向开发人员的架构说明

## 1. 总体回应

开发评审提出的四个核心问题成立：OpenAI 环境变量、PostgreSQL 账号、宿主机访问端口和验收覆盖范围在原方案中没有冻结到足够清楚，确实会让开发人员自行判断并产生第二套真相。

仓库级目录重命名和 Java 根包迁移也确实扩大了本次变更影响面。它们不是不能做，但不应该和 Docker 正式部署规范化绑在同一个 change 里。

因此本次修订后的架构预期是：先把正式交付命名、配置契约和 Docker Compose 部署入口收口；目录和 Java 包名重构后续独立处理。

## 2. OpenAI 环境变量如何处理

预期方案：

- 正式部署只接受 `AGENT_STUDIO_OPENAI_API_KEY`。
- 正式部署只接受 `AGENT_STUDIO_OPENAI_BASE_URL`。
- 正式部署只接受 `AGENT_STUDIO_OPENAI_DEFAULT_MODEL`。
- `OPENAI_API_KEY`、`SPRING_AI_OPENAI_BASE_URL`、`MYAGENT_OPENAI_DEFAULT_MODEL` 不再是正式入口。

为什么这样设计：

当前 Spring AI 需要读取 `spring.ai.openai.*` 配置，但这只是框架内部属性，不应该成为项目对外交付环境变量。否则用户会同时看到 `AGENT_STUDIO_OPENAI_*` 和 Spring 原生变量，两边都能配，最终不知道哪边生效。

开发应如何解决：

- 在 `application.yml` 中把 `spring.ai.openai.*` 映射到 `AGENT_STUDIO_OPENAI_*`。
- 在启动阶段检测旧变量，发现旧变量直接启动失败。
- Compose、README、部署说明和发布检查清单中只写 `AGENT_STUDIO_OPENAI_*`。
- fail-fast 校验类和测试中可以出现旧变量字符串，但这些字符串只能用于拒绝旧变量，不能用于读取配置、设置默认值或形成成功路径。
- `AGENT_STUDIO_OPENAI_API_KEY` 在正式部署中必须是必填项，不能再通过 `dummy-key` 或其他伪值兜底。
- 可以保留 `AGENT_STUDIO_OPENAI_BASE_URL` 和 `AGENT_STUDIO_OPENAI_DEFAULT_MODEL` 的真实默认值，但不要对 Key 格式、模型名格式或 Base URL 可达性做启动期严格校验。

## 3. PostgreSQL 账号如何处理

预期方案：

- Compose 数据库名固定为 `agent_studio`。
- Compose 数据库用户名固定为 `agent_studio`。
- Compose 数据库密码只由 `AGENT_STUDIO_POSTGRES_PASSWORD` 提供。
- API 容器内 JDBC URL 固定为 `jdbc:postgresql://postgres:5432/agent_studio`。

为什么这样设计：

本次部署默认包含 PostgreSQL，因此没有必要把数据库名、用户名、JDBC URL 都做成可变输入。默认 Compose 只需要一个密码变量，部署者需要理解的东西最少，也能保证 psql 验收命令、API 连接配置和 Compose 配置完全一致。

开发应如何解决：

- `11_code/compose.yaml` 中固定 `POSTGRES_DB=agent_studio` 和 `POSTGRES_USER=agent_studio`。
- `11_code/compose.yaml` 中通过 `AGENT_STUDIO_POSTGRES_PASSWORD` 同时设置 PostgreSQL 密码和 API 数据源密码。
- 文档和验收命令全部使用 `psql -U agent_studio -d agent_studio`。
- 不要在本次默认 Compose 中开放 `POSTGRES_USER`、`POSTGRES_DB`、`AGENT_STUDIO_DATASOURCE_URL` 作为正式覆盖项。

## 4. 正式访问端口如何处理

预期方案：

- 正式 Web 宿主机端口固定为 `18080`。
- 浏览器入口固定为 `http://127.0.0.1:18080`。
- API 容器只在 Compose 网络内暴露 `8080`。
- PostgreSQL 容器只在 Compose 网络内暴露 `5432`。

为什么这样设计：

端口不冻结会导致 Compose、部署说明和验收命令各自假定不同入口。选择 `18080` 是为了避免默认 `80` 端口需要管理员权限或和本机服务冲突，同时仍保持单一正式入口。

开发应如何解决：

- Web 服务映射 `18080:80`。
- 不要把 API 的 `8080` 映射到宿主机。
- 不要把 PostgreSQL 的 `5432` 映射到宿主机。
- 所有正式访问和验收命令都走 `http://127.0.0.1:18080`。

## 5. 验收范围如何处理

预期方案：

静态验收必须覆盖当前正式源码和当前正式文档，包括：

- `README.md`
- `3_product_design`
- `4_arch_design`
- `6_schema_design`
- `7_interface_design`
- `11_code`
- `13_release`
- `14_user_manual`
- `11_code/compose.yaml`

为什么这样设计：

如果只扫源码和部分发布说明，接口设计或数据结构设计仍可能保留旧命名，后续开发会继续引用旧口径。这就是双轨真相。

开发应如何解决：

- 按 `test_steps.md` 的静态检索范围执行。
- 历史验收记录可以保留旧名，不作为失败。
- 当前正式文档中不得再把旧名作为产品、部署、配置、数据库或环境变量口径。

## 6. 仓库级重命名如何处理

预期方案：

本次不改：

- `11_code/backend`
- `11_code/frontend`
- `com.myagent`

后续如需要，可以单独发起 `agent-studio-source-package-rename` 之类的重构变更。

为什么这样设计：

目录和 Java 根包迁移会牵动 MyBatis XML、包扫描、OpenAPI 示例、测试包路径、日志配置和大量 import。它和 Docker 正式部署不是同一个风险面。把两者放在一起会让问题定位困难，也会拖慢最核心的部署收口。

开发应如何解决：

- 本次只改对外交付命名和部署契约。
- 代码内部 `com.myagent` 可以短期保留，但不得作为当前对外交付品牌、镜像名、配置前缀、环境变量或数据库名。
- 对外 OpenAPI 示例不属于内部代码结构，必须清理 `com.myagent` 和 `myagent-backend`。
- Java 类型示例使用 `syc.agentstudio.example.*`，服务名示例使用 `agent-studio-api`。
- 后续重构时再一次性迁移 Java 包、目录和 XML namespace。

## 7. Compose project 如何处理

预期方案：

- `11_code/compose.yaml` 顶层必须写 `name: agent-studio`。
- 不能只依赖当前目录名生成 Compose project。

为什么这样设计：

Compose project 名会影响 Docker 网络、volume 和容器前缀。只把 `agent-studio` 写在命名表里，而不写进 `11_code/compose.yaml`，运行态仍可能按仓库目录生成另一套名字。

开发应如何解决：

- 在 `11_code/compose.yaml` 顶层增加 `name: agent-studio`。
- 验收执行 `docker compose config | Select-String -Pattern '^name: agent-studio$'`。
- 网络和 volume 命名应以 `agent-studio` 为前缀。

## 8. 正式 OpenAI Key 为什么不能再用伪默认值

预期方案：

- `AGENT_STUDIO_OPENAI_API_KEY` 是正式部署必填项。
- 正式部署缺失该变量时，Compose 或应用启动必须失败。
- 测试环境可以在测试配置中使用测试值，但测试值不能出现在主配置或正式 Compose 中。

为什么这样设计：

这次变更的目标不是“页面能打开”，而是“正式部署入口唯一且含义明确”。如果 Compose 默认塞一个 `dummy-key`，系统会表现为：

- 在 `D:\myproject\MyAgent\11_code` 下执行 `docker compose up -d --build` 成功；
- `/actuator/health` 返回 `UP`；
- `/api/settings` 返回正常；
- 但任何真实 LLM 调用在运行时才失败。

这会把“部署成功”和“业务可用”拆成两套真相，是典型隐藏技术债务。

开发应如何解决：

- `11_code/compose.yaml` 中把 `AGENT_STUDIO_OPENAI_API_KEY` 改为必填插值，而不是 `dummy-key` 默认值。
- `application.yml` 中移除主配置级的 `dummy-key` 默认值。
- 如测试需要占位值，把占位值放到测试专用配置中，不要放到正式启动路径。
- 缺失 Key 时只校验“缺失/空白”，不要校验格式，不要在启动期调用外部模型接口探活。

## 9. OpenAPI 正式入口地址为什么必须收口

预期方案：

- 通过正式入口 `http://127.0.0.1:18080/v3/api-docs` 获取的 OpenAPI 文档，`servers.url` 必须仍然是 `http://127.0.0.1:18080`。
- 不能出现不带端口的 `http://127.0.0.1`。
- 不能出现内部地址 `http://api:8080`。

为什么这样设计：

OpenAPI 不是内部调试输出，而是对外契约。只要 `servers.url` 丢了端口，任何基于该契约生成客户端、调试脚本或文档的人，拿到的都是另一套入口真相。

当前正式入口已经冻结为 `18080`，那 OpenAPI 也必须表达同一个入口，不能让部署文档一套、OpenAPI 一套。

开发应如何解决：

- 在反向代理层补齐 Host/Port 转发头，不要只传 `$host`。
- 在后端启用 forward headers 处理，让 SpringDoc 基于外部请求头生成正确的 `servers.url`。
- 修完运行态生成链路后，重新刷新 `agent-studio-openapi.json` 和 `schema.ts`。
- 不要手工编辑 OpenAPI 快照或前端生成文件来掩盖运行态问题。

## 10. 数据库现场验收为什么要改口径

预期方案：

- fresh install 只要求 Flyway 迁移完成。
- fresh install 场景下，`system_setting` 表可以为空。
- 旧键重命名路径由自动化迁移测试证明；当表内已有记录时，再要求键名必须都是 `agent.studio.*`。

为什么这样设计：

当前默认值来自应用配置，不来自 `system_setting` 种子数据。因此 fresh install 看到空表不是实现错误。如果继续把“查到若干 setting_key”写成现场必过条件，验收动作本身就不准确。

开发应如何解决：

- 保留并强化自动化迁移测试，证明 `myagent.*` 能升级为 `agent.studio.*`。
- 发布文档和验收步骤把 SQL 验收改成“空表允许；有记录时必须全为新键”。
- 运行态功能验证以 `/api/settings` 返回 7 个白名单项为主，因为那才是当前产品真实对外行为。

## 8. 最终交付判断

开发完成后，如果还存在以下情况，应判定为不符合架构预期：

- `OPENAI_API_KEY` 或 `SPRING_AI_OPENAI_BASE_URL` 仍是成功路径。
- 正式部署缺失 `AGENT_STUDIO_OPENAI_API_KEY` 仍能借助伪默认值成功启动。
- Compose 允许 API 或 PostgreSQL 作为正式宿主机入口。
- Web 正式入口不是 `http://127.0.0.1:18080`。
- 数据库用户名和验收命令不一致。
- `6_schema_design` 或 `7_interface_design` 仍把旧名作为当前契约。
- OpenAPI JSON、`schema.ts` 或 Swagger 示例仍出现 `com.myagent` / `myagent-backend`。
- 通过正式入口输出的 OpenAPI `servers.url` 不是 `http://127.0.0.1:18080`。
- `11_code/compose.yaml` 未设置顶层 `name: agent-studio`。
- 开发顺手做了 Java 根包迁移并导致本次 Docker 交付被扩大。
