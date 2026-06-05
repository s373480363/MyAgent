# 架构单一真相清理 v1 开发说明

## 1. 为什么要这样改

这次清理不是为了改名，也不是为了做一次普通重构。当前系统里有几类旧设计残留已经变成了事实分叉：

- 部署文档要求 `AGENT_STUDIO_OPENAI_*`，但运行时模型路由已经来自数据库模型供应商目录。
- 文档说 `isDefault` 只是历史读取字段，但代码和生成接口仍然暴露它。
- 产品文档里的 EvalCase 字段停留在旧设计，而接口和代码已经使用新合同。
- 文档写了一个不存在的 Trace 配置项。
- 前端页面里手写枚举列表，后端校验又维护另一套列表。

用户已经明确要求不要保留兼容、止血、默认供应商、默认模型这类隐藏技术债务。因此开发时不能再用“最小改动保留旧路径”的方式处理。

## 2. 对开发人员的核心要求

### 2.1 删除旧路径，不做新包装

不要把 `AGENT_STUDIO_OPENAI_*` 改成另一个名字继续使用，也不要把它们包装成“初始化来源”“迁移来源”或“默认供应商来源”。

新的模型配置路径只有一个：用户在页面维护模型供应商和模型供应项，运行时从数据库解析。

### 2.2 不使用 starter 自动配置静态 provider

用户明确质疑 Spring AI OpenAI starter 的合理性，这个质疑是成立的。

starter 的典型作用是根据 `spring.ai.openai.*` 在应用启动时自动装配一个默认 OpenAI 客户端或模型 bean。这个模式适合单 provider 应用，不适合本项目的多 provider 动态路由。因为本项目每次模型调用都可能选择不同供应商、不同 Base URL、不同 API Key 和不同上游模型名。

开发人员可以继续使用 Spring AI 的 OpenAI-compatible 客户端能力，也可以换成等价客户端库，但调用方式必须是动态显式构造：

- 从数据库按 `modelOfferingKey` 解析 provider 和 offering。
- 用本次 route 的 `baseUrl` 和解密后的 API Key 构造客户端。
- 用本次 route 的 `upstreamModelName` 构造请求选项。
- 不依赖 `spring.ai.openai.*`、环境变量或 starter 自动装配出来的默认 bean。

如果保留 starter 依赖会触发自动配置或让开发人员误以为系统仍有默认 OpenAI provider，就应移除 starter，改用非 starter 模块或等价库。

### 2.3 不保留 `isDefault`

不要写下面这种逻辑：

```java
edge.getType() == WorkflowEdgeType.DEFAULT || Boolean.TRUE.equals(edge.getIsDefault())
```

正确逻辑只有：

```java
edge.getType() == WorkflowEdgeType.DEFAULT
```

当前系统旧数据会被删除，不编写旧数据迁移分支，也不在运行时留下分支。

### 2.4 metadata 接口不是兜底配置

`GET /api/platform-metadata` 的意义是让后端成为枚举单一真相。前端不应该在 metadata 加载失败时偷偷使用本地旧数组，否则双轨真相会以另一种形式回来。

metadata 加载失败时，页面应该显示错误状态，让问题暴露出来。

### 2.5 删除错误文档项，不补不存在的需求

`agent.studio.trace.persist-full-model-content` 不是需求。不要为了让文档“看起来正确”而补一个配置字段。正确做法是删除它。

### 2.6 注释规范要真实可执行

getter/setter 不需要 Javadoc。业务方法、复杂私有方法、状态转换和有副作用的流程需要中文说明。

注释的目标是解释业务意图和边界，不是复述代码。

## 3. 需要特别小心的地方

### 3.1 Fresh install

删除 `AGENT_STUDIO_OPENAI_*` 后，全新安装时模型目录为空是正常状态。开发人员不要再引入启动失败校验。

模型调用失败应该发生在用户实际发布或运行需要模型的工作流时，错误消息应告诉用户先配置模型供应商和模型供应项。

### 3.2 旧数据库

如果旧数据库里已经有模型供应商和模型供应项，这些数据仍然是合法业务数据。不要删除它们。

删除的是“从环境变量自动创建默认供应商”的代码路径。

### 3.3 旧工作流数据

旧工作流数据会被删除，不需要迁移为 `type=DEFAULT`。代码不再理解 `isDefault`。

如果因为担心旧数据而在运行时代码里保留 `isDefault` 分支，本次变更就是失败的。

### 3.4 旧变更包

仓库中已有变更包记录了过去的设计结论，其中部分内容已经被本次用户决策推翻。开发人员不要以旧变更包为准继续实现。

本变更包是当前正式方案。

## 4. 推荐实施顺序

1. 先移除 `AGENT_STUDIO_OPENAI_*`，让启动和模型配置边界清晰。
2. 再移除 Spring Boot OpenAI starter 自动配置入口，确保模型调用按 provider route 动态构造。
3. 再删除 `isDefault`，配套完成 OpenAPI 更新。
4. 接着新增 `GET /api/platform-metadata` 并改造前端枚举来源。
5. 同步 EvalCase 产品和接口文档。
6. 删除 trace 假配置文档。
7. 最后调整注释规范并跑全量静态检索。

这个顺序的原因是：先处理会影响运行和数据合同的旧路径，再处理文档和规范，能避免开发过程中继续以旧事实为依据。
