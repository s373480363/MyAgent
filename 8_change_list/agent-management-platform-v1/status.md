# Agent 管理平台 V1 状态

当前状态：验收完成。

本文件记录本次变更的状态变化、审核整改和验收收口情况。原 `8_change_list\agent-management-platform-v1\status.md` 中的整改说明已合并到本文件，旧目录不再作为查询入口。

## 11-V1正式验收问题整改说明-v1

# V1 正式验收问题整改说明 v1

## 1. 文档目的

本文档面向开发人员，说明本轮正式验收中发现的关键问题、架构判断、推荐整改方案、设计原因和最终处理要求。

本文档不是缺陷列表的简单复述，而是把“为什么当前实现不满足正式交付要求”与“应该如何按统一架构口径修正”说清楚，避免开发各自补丁式修复，继续引入第二套真相。

## 2. 输入依据

- `8_change_list/agent-management-platform-v1/test_steps.md`
- `13_release/01-V1发布前检查清单-v1.md`
- `13_release/02-V1部署说明-v1.md`
- `11_code/README.md`
- `11_code/frontend/src/api/httpClient.ts`
- `11_code/frontend/vite.config.ts`
- `11_code/frontend/package.json`
- `11_code/frontend/openapi/myagent-openapi.json`
- `11_code/frontend/src/api/generated/schema.ts`

## 3. 总体判断

本轮发现的问题里，真正影响正式交付的是两类：

- 前后端联调架构没有闭合，导致前端主链路在真实浏览器中不可用。
- OpenAPI 产物管理和仓库说明文档已经与当前代码状态脱节，继续放任会让后续联调、回归、发布全部建立在错误前提上。

这三项问题的共同根因不是“某个页面没写完”，而是交付真相没有被收敛到单一来源：

- 前端访问真相没有收敛。
- OpenAPI 契约真相没有收敛。
- 启动与发布说明真相没有收敛。

架构整改目标不是临时把页面跑起来，而是把上述三类真相统一下来，保证后续开发、联调、测试和发布都围绕同一套规则执行。

## 4. 问题一：前后端联调架构未闭合

### 4.1 现象

当前实现中：

- 前端默认把 API 基地址写死为 `http://localhost:8080`。
- Vite dev server 没有配置 `/api` 代理。
- 后端没有看到任何明确的跨域配置。
- 文档给出的标准启动方式是“前端 `5173` + 后端 `8080`”分端口运行。

这意味着只要用户按当前文档执行本地联调，就会天然进入浏览器跨域场景。由于没有代理，也没有 CORS，页面在真实浏览器中会直接表现为 `Failed to fetch`。

这不是“开发机配置问题”，而是当前交付架构本身没有定义清楚“前端如何合法访问后端”。

### 4.2 架构判断

V1 的部署边界已经明确为“本机或内网单用户可信环境”，不是面向公网的多端开放 API 平台。因此这里的首选方案不应是放开通配跨域，而应是：

- 对外口径坚持同源访问。
- 开发态通过代理模拟同源。
- 只有在确有需要的场景下，才允许通过显式配置覆盖到其他 API 基地址。

也就是说，**默认开发体验必须是前端直接可跑、页面直接可用、无需开发者自行推断端口/CORS 细节**。

### 4.3 推荐设计

推荐把正式口径统一为以下规则：

1. 浏览器侧默认只访问相对路径 `/api`、`/v3/api-docs`、`/swagger-ui.html`。
2. 前端默认不内置 `http://localhost:8080` 这样的绝对地址。
3. 本地开发使用 Vite `server.proxy` 把 `/api` 等路径转发到后端。
4. `VITE_API_BASE_URL` 只作为显式覆盖项存在，用于特殊联调或反向代理尚未就绪的环境，不作为默认真相。
5. 后端不以“放开全局 CORS”作为正式解法；如果确有需要，也只能在开发 profile 下受控开启，不能把宽松跨域配置带入正式部署口径。

### 4.4 为什么这样设计

- 同源是最简单、最稳定的浏览器访问模型，能减少跨域、Cookie、Header、代理、网关等附带复杂度。
- 开发态代理和生产态同源可以保持一致的 URL 语义，减少“开发能跑、发布不通”的分叉。
- 当前项目不是开放平台，不需要为“任意来源浏览器调用”设计默认能力。
- 如果继续把绝对地址写死在前端，会导致测试、联调、预发、演示环境都需要额外解释端口和跨域规则，属于不必要的运维成本。

### 4.5 开发应如何处理

开发按以下方式整改：

1. 修改 `11_code/frontend/src/api/httpClient.ts`
   - 去掉默认 `http://localhost:8080`。
   - 默认走相对路径。
   - 保留 `VITE_API_BASE_URL` 作为可选覆盖项，但覆盖逻辑必须是“显式配置时才启用”。

2. 修改 `11_code/frontend/vite.config.ts`
   - 为 `/api` 配置代理到后端。
   - 同时代理 `/v3/api-docs`、`/swagger-ui.html`，必要时补 `/actuator`。
   - 开发人员本地只需要启动前后端两个进程，不需要额外理解浏览器跨域问题。

3. 检查前端 API 相关测试
   - `domainApi.test.ts` 不应再把 `http://localhost:8080` 当作默认真相。
   - 测试应围绕“默认相对路径 + 可选覆盖地址”设计。

4. 如团队仍保留跨端口直连能力
   - 只能作为显式调试手段。
   - 必须在文档中标注这是“覆盖模式”，不是标准启动路径。

### 4.6 最终验收标准

整改完成后，以下条件必须同时成立：

- 按标准文档启动前端与后端后，浏览器打开 `/agents`、`/schemas`、`/runs`、`/evals` 等页面不再出现 `Failed to fetch`。
- 前端默认启动不依赖手工设置 `VITE_API_BASE_URL`。
- 开发态与正式部署态都遵循同一套 URL 语义，不存在第二套访问真相。

## 5. 问题二：OpenAPI 契约产物已经失真

### 5.1 现象

当前仓库中：

- `frontend/openapi/myagent-openapi.json` 仍然只包含 `/api/ping`。
- `frontend/package.json` 的 `openapi:generate` 又明确依赖这个文件生成前端类型。
- 但 `frontend/src/api/generated/schema.ts` 已经包含大量真实业务接口类型。

这说明仓库内已经出现了至少两套 OpenAPI 相关真相：

- 一套是旧快照。
- 一套是较新的生成结果。

只要这种状态存在，后续任何开发人员运行一次 `openapi:generate`，都可能把前端类型错误回滚到旧契约。

### 5.2 架构判断

V1 的正式契约唯一来源必须是后端实时产出的 `/v3/api-docs`。

如果仓库里保留 OpenAPI 快照文件，它必须是该来源的派生产物，而不是另一份人工维护的“准契约”。前端生成类型、测试和代码评审，都必须建立在这个唯一来源之上。

### 5.3 推荐设计

推荐采用“一个来源、两个派生产物、一个校验闸门”的方式：

1. **唯一来源**
   - 后端 `/v3/api-docs`

2. **派生产物**
   - `frontend/openapi/myagent-openapi.json`
   - `frontend/src/api/generated/schema.ts`

3. **统一生成链路**
   - 先从后端下载最新 `/v3/api-docs`
   - 再基于下载结果生成 TypeScript 类型

4. **一致性校验闸门**
   - 只要后端 Controller、DTO、OpenAPI 注解变更，就必须同步刷新这两个产物。
   - CI 或本地检查必须能发现“后端变了但快照/生成类型没刷新”的情况。

### 5.4 为什么这样设计

- OpenAPI 本质上是后端对外契约，不应由前端或快照文件反向定义。
- 仓库中保留快照有价值，因为它便于代码评审和离线比对契约变化。
- 但只保留快照、不做刷新闸门，会立刻退化成双轨真相。
- 因此前端生成类型和 OpenAPI 快照可以提交到仓库，但必须被明确定义为“生成产物”，不能被当成手工维护文件。

### 5.5 开发应如何处理

开发按以下方式整改：

1. 保留 `frontend/package.json` 中的 `openapi:download` / `openapi:generate` / `openapi:refresh` 机制，但必须刷新为当前真实契约。
2. 在本地启动后端后执行一次正式刷新：
   - 下载当前 `/v3/api-docs`
   - 重新生成 `src/api/generated/schema.ts`
3. 确保 `myagent-openapi.json` 与 `schema.ts` 来自同一次刷新，不允许单独手改其中任意一个。
4. 增加校验规则，至少满足以下之一：
   - CI 中执行 `openapi:refresh` 后若产物有 diff 则失败。
   - 或在发布前检查脚本中执行相同校验。
5. 所有前端 API 类型继续只从 `src/api/generated/schema.ts` 消费，不引入手写长期业务 DTO 真相。

### 5.6 最终验收标准

整改完成后，以下条件必须同时成立：

- `frontend/openapi/myagent-openapi.json` 能反映当前真实业务接口，而不是只剩 `/api/ping`。
- `frontend/src/api/generated/schema.ts` 与该快照同源生成。
- 后端接口变更后，若未刷新 OpenAPI 产物，检查流程会明确失败。

## 6. 问题三：代码目录说明与启动说明已经过时

### 6.1 现象

当前文档存在两类明显漂移：

1. `11_code/README.md` 仍写“当前已完成开发执行步骤 01”，与当前已实现的完整 V1 范围明显不符。
2. 文档给出的后端启动命令仍是通用 `mvn spring-boot:run -Dspring-boot.run.profiles=local`，但当前仓库环境实际依赖 `9_dependency\tools` 下的 JDK 21 与 Maven；如果开发人员直接按文档执行，容易落到系统默认 JDK 17，导致启动失败。

这类问题的本质不是“文档没更新漂亮”，而是**交付说明已经不能可靠指导他人复现系统**。

### 6.2 架构判断

在 V1 正式验收口径下，README、部署说明、发布前检查清单都是交付物的一部分，不是可有可无的注释。

如果代码已经是完整 V1，但文档还停留在步骤 01，会直接产生以下风险：

- 新开发或测试人员对系统范围判断错误。
- 联调人员按错误命令启动，误以为项目不可用。
- 后续整改无法围绕统一命令和统一环境执行。

### 6.3 推荐设计

推荐把文档真相统一为以下层次：

1. `11_code/README.md`
   - 说明当前代码范围、模块构成、最低运行前提、快速启动方式。

2. `13_release/02-V1部署说明-v1.md`
   - 说明正式部署与联调步骤、环境变量、数据库前提、真实模型配置方式。

3. 如有必要，新增启动脚本
   - 例如 `11_code/scripts/start-backend-local.ps1`
   - 例如 `11_code/scripts/start-frontend-dev.ps1`
   - 通过脚本固化 `9_dependency\tools` JDK/Maven、数据库地址、标准端口、可选环境变量

推荐优先使用“文档 + 启动脚本”双保险，而不是仅靠 README 文字说明。

### 6.4 为什么这样设计

- 文字说明容易被复制后遗忘，脚本更能固化真实启动方式。
- README 面向开发者快速进入，部署说明面向交付和联调，两者职责不同，不能混写。
- 当前项目已经不是骨架工程，文档仍按骨架工程描述，会持续误导后续工作。

### 6.5 开发应如何处理

开发按以下方式整改：

1. 更新 `11_code/README.md`
   - 把“只完成步骤 01”的描述改为当前真实交付范围。
   - 明确后端、前端、数据库、OpenAPI、真实模型、外部 Agent 的当前状态。

2. 修正启动说明
   - 如果仓库标准做法依赖 `9_dependency\tools`，文档必须直接给出 `9_dependency\tools` 版本命令。
   - 如果仓库要求预装 JDK 21 / Maven 3.9，则必须写成强前提，而不是默认 `mvn` 可用。

3. 与问题一保持一致
   - 文档里前端启动方式必须与最终前后端访问方案一致。
   - 如果前端默认走代理，就不要再让文档把跨域环境当作标准路径。

4. 更新发布前检查清单
   - 把“真实前后端联调数据回溯检查”从口头项落实到可执行步骤。

5. 视团队习惯决定是否补启动脚本
   - 若补脚本，应以脚本作为标准入口，文档只说明如何调用脚本。

### 6.6 最终验收标准

整改完成后，以下条件必须同时成立：

- 新同事只看仓库文档即可判断当前系统已是完整 V1，而不是步骤 01 骨架。
- 按文档执行能在当前标准环境中拉起系统，不依赖额外口头说明。
- 文档中的端口、命令、环境变量与真实联调方式一致。

## 7. 建议整改顺序

建议开发严格按以下顺序处理，不要并行乱改：

1. 先收口前后端访问方案
   - 先确定“默认相对路径 + Vite 代理”的统一口径。

2. 再刷新 OpenAPI 产物
   - 因为前端 API 访问层和契约生成链路要一起收口。

3. 再修正文档与脚本
   - 用最终实际可跑的方式反写 README、部署说明和检查清单。

4. 最后做完整回归
   - 前端单测
   - 前端 build
   - 后端测试
   - 一轮真实前后端联调

## 8. 开发完成后的最低回归要求

整改完成后，至少需要回归以下项目：

1. 后端：
   - `mvn test`

2. 前端：
   - `npm test`
   - `npm run build`

3. OpenAPI：
   - 执行 `openapi:refresh`
   - 确认 `myagent-openapi.json` 与 `schema.ts` 已同步刷新

4. 真实联调：
   - 首页可打开
   - `Settings`
   - `Schemas`
   - `Agents`
   - `Workflow`
   - `Debug`
   - `Runs`
   - `Evals`

5. 联调重点观察项：
   - 页面不再出现 `Failed to fetch`
   - 浏览器网络请求全部命中预期后端
   - Runs 详情、NodeRun、Trace、EvalCase 创建链路可正常走通

## 9. 最终要求

本轮整改不接受以下处理方式：

- 仅在测试代码里改常量，正式前端访问方案不改。
- 只把 `myagent-openapi.json` 手工替换掉，不建立刷新闸门。
- 只改 README 一句话，不修正实际启动命令和联调方式。
- 通过放开全局任意来源 CORS 来掩盖架构未闭合问题。
- 新增第二套手写 API DTO 或第二套接口清单来绕过 OpenAPI 失真问题。

本轮整改的目标是把“访问真相、契约真相、启动真相”全部收口到唯一来源。只有这样，后续的测试结论、联调结论和发布结论才可信。


## 12-V1二轮正式验收遗留问题整改说明-v1

# V1 二轮正式验收遗留问题整改说明 v1

## 1. 文档目的

本文档面向开发人员，说明 2026-06-02 二轮正式验收中确认的遗留问题、架构判断、预期整改方案、设计原因和最终处理要求。

本文档只基于当前仓库代码、既有设计文档和已实际复验的结果编写，不基于猜测给出“可能可行”的补丁式建议。目标是把开发整改再次收口到单一架构口径，避免出现“局部现象修好了，但正式交付真相仍然不闭合”的情况。

## 2. 输入依据

- `8_change_list/agent-management-platform-v1/test_steps.md`
- `8_change_list/agent-management-platform-v1/plan.md`
- `4_arch_design/03-前端架构设计-v1.md`
- `4_arch_design/12-公共接口架构设计-v1.md`
- `4_arch_design/13-部署安全与质量架构设计-v1.md`
- `7_interface_design/05-响应错误分页与OpenAPI约定-v1.md`
- `11_code/README.md`
- `13_release/02-V1部署说明-v1.md`
- `11_code/frontend/src/api/httpClient.ts`
- `11_code/frontend/vite.config.ts`
- `11_code/frontend/openapi/myagent-openapi.json`
- `11_code/frontend/src/api/generated/schema.ts`
- `11_code/frontend/src/features/runs/pages/RunsPage.tsx`
- `11_code/frontend/scripts/download-openapi.ps1`
- `11_code/frontend/scripts/check-openapi-sync.ps1`
- `11_code/scripts/start-backend-local.ps1`

## 3. 二轮复验总体结论

### 3.1 已符合预期的整改项

以下整改方向已经基本符合架构预期，应保持，不要回退：

- 前端默认访问语义已从“硬编码绝对地址”收口为“浏览器默认走相对路径”。
- Vite dev server 已为 `/api`、`/v3/api-docs`、`/swagger-ui.html`、`/swagger-ui`、`/actuator` 建立代理，方向正确。
- OpenAPI 快照已刷新为真实接口集，不再只剩 `/api/ping`。
- README 和部署说明已经开始围绕统一启动入口和统一联调口径编写，方向正确。

这说明首轮发现的“前端主链路 Failed to fetch 架构未闭合”问题，原则上已经修对了。

### 3.2 当前仍然不能判定通过的原因

二轮复验后，仍存在 3 个 P1 级阻塞问题，因此当前正式验收结论仍应为“不通过”：

1. 前端生产构建仍失败。
2. 标准后端启动脚本在 PowerShell 下仍不可用。
3. `openapi:check` 质量闸门仍可能误报通过。

这三个问题的共同性质不是“边角料缺陷”，而是仍然直接影响正式交付可信度：

- 构建失败意味着前端交付物不可发布。
- 标准启动脚本失败意味着文档定义的标准启动真相不成立。
- 质量闸门误通过意味着团队无法可信地判断 OpenAPI 产物是否同步。

## 4. 问题一：Runs 详情页仍存在前后端契约漂移

### 4.1 现象与证据

当前 `11_code/frontend/src/features/runs/pages/RunsPage.tsx:255` 仍在使用 `record.detailJson`：

```tsx
expandable={{ expandedRowRender: (record) => <JsonBlock title="详情" value={record.detailJson} /> }}
```

但当前生成类型 `11_code/frontend/src/api/generated/schema.ts:2544-2553` 中，`TraceEventResult` 的字段已经是 `detail`，不是 `detailJson`。

因此 `npm run build` 会因为类型不匹配而失败。这不是测试误报，而是正式构建阻塞。

### 4.2 架构判断

根据 `7_interface_design/05-响应错误分页与OpenAPI约定-v1.md`、`4_arch_design/03-前端架构设计-v1.md` 和测试计划，前端接口类型必须以 OpenAPI 生成结果为唯一来源，禁止长期保留影子 DTO 字段。

所以这里不能采用“局部 any 化”“额外兼容 detailJson”“前端手写一份临时类型”的方式绕过。正确方向只有一个：

- 页面消费当前 OpenAPI 生成类型；
- 旧字段引用全部清理；
- 以构建通过作为最低交付门槛。

### 4.3 预期解决方案

开发应按以下方式处理：

1. 把 `RunsPage.tsx` 中 `record.detailJson` 改为 `record.detail`。
2. 全仓搜索并清理所有残留的 `detailJson` 旧字段引用，避免只修一个页面。
3. 复核运行详情页中 Trace 展开、NodeRun 展开、详情 JSON 展示逻辑，确认与当前契约一致。
4. 重新执行前端构建，确保类型系统已完全收口。

### 4.4 为什么要这样设计

- OpenAPI 是后端对外契约真相，前端只能消费派生产物，不能与旧字段并存。
- 保留旧字段兼容会制造第二套真相，后续任何接口改动都会继续累积类似问题。
- 当前项目是框架产品，重点是流程正确与契约一致，不是页面临时显示出内容即可。

### 4.5 开发最终应如何处理

必须把本问题按“契约收口问题”处理，而不是按“页面显示问题”处理：

- 禁止新增手写影子类型或 `as any`。
- 禁止通过双字段兼容掩盖契约漂移。
- 必须以 OpenAPI 生成类型为准统一修改消费端。

### 4.6 最终验收标准

整改完成后，以下条件必须同时成立：

- `npm run build` 成功。
- 运行详情页 Trace 展开区可正常展示 `detail` 内容。
- 前端源码中不再存在残留 `detailJson` 字段引用。
- 不引入新的手写长期 DTO 真相。

## 5. 问题二：标准后端启动脚本仍未成为可信真相

### 5.1 现象与证据

当前 `11_code/scripts/start-backend-local.ps1:27` 为：

```powershell
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

在 PowerShell 5.1 下，该写法会触发 Maven 参数解析异常，实际复验报错为：

```text
Unknown lifecycle phase ".run.profiles=local"
```

而 README 与部署文档已经把这个脚本当作标准启动入口。如果脚本本身不可用，则文档中的“标准启动方式”就是失真的。

### 5.2 架构判断

根据当前 V1 交付边界，仓库已经明确采用：

- 仓库内 `9_dependency\tools` 提供固定 JDK 21 与 Maven 3.9.11；
- `11_code/scripts/start-backend-local.ps1` 作为标准本地启动入口；
- README 与部署说明围绕该入口编排联调流程。

既然已经收口到“脚本即标准入口”，那就必须保证这个脚本在目标 shell 下直接可用。不能一边写“请执行该脚本”，一边默认开发人员自己知道如何绕开 PowerShell 参数解析问题。

### 5.3 预期解决方案

推荐把 Maven 调用改成 PowerShell 安全调用，不再依赖裸命令解析。推荐优先级如下：

1. 首选：显式调用 `mvn.cmd`，并以参数数组传入。
2. 次选：使用 `Start-Process` 调起 `mvn.cmd` 并等待退出码。
3. 备选：使用 PowerShell 停止解析或 `cmd /c` 包装。

推荐落地方式示例：

```powershell
$mvnCmd = Join-Path $mavenBin "mvn.cmd"
& $mvnCmd "spring-boot:run" "-Dspring-boot.run.profiles=local"
exit $LASTEXITCODE
```

如果后续还要承载更多启动参数，推荐继续保持“显式命令路径 + 显式参数数组”这一模式，不要再退回裸字符串命令。

### 5.4 为什么要这样设计

- 当前问题不是 Maven 不可用，而是 PowerShell 参数边界没有被正确处理。
- 显式调用 `mvn.cmd` 并传参数数组，语义最直接，兼容性最好，也最容易复现。
- 既然 `9_dependency\tools` 已经被选为统一工具链来源，就应该让脚本完整封装 JDK/Maven 真相，而不是把最后一步执行细节留给调用者猜。

### 5.5 开发最终应如何处理

- 保持 `9_dependency\tools` 作为标准 JDK/Maven 来源，不要把系统环境变量重新变成隐式前提。
- 修复脚本后，README 与部署文档只保留真实可执行的标准命令。
- 如补充 `start-frontend-dev.ps1`、其他联调脚本，也要遵守相同原则：脚本必须在目标 shell 下直接可跑。

### 5.6 最终验收标准

整改完成后，以下条件必须同时成立：

- 在 PowerShell 中执行 `powershell -NoProfile -ExecutionPolicy Bypass -File .\11_code\scripts\start-backend-local.ps1` 可以正常启动后端。
- 后端进程实际使用仓库内 `9_dependency\tools` 的 JDK 21 与 Maven 3.9.11。
- README 与部署说明中的标准启动命令无需额外口头解释即可复现。

## 6. 问题三：OpenAPI 一致性检查仍然会误通过

### 6.1 现象与证据

当前 `11_code/frontend/scripts/download-openapi.ps1:15` 直接执行：

```powershell
Invoke-WebRequest -Uri "$BaseUrl/v3/api-docs" -OutFile $OutputPath
```

当前 `11_code/frontend/scripts/check-openapi-sync.ps1:12-15` 只检查：

```powershell
npm run openapi:refresh
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}
```

问题在于 PowerShell 中 `Invoke-WebRequest` 的连接失败默认可能是非终止错误。这样一来：

- `download-openapi.ps1` 控制台会打印连接失败；
- 但整个脚本不一定以非零退出；
- `check-openapi-sync.ps1` 只看 `$LASTEXITCODE`，就可能继续往下走；
- 最终 `npm run openapi:check` 可能错误返回成功。

这意味着当前质量闸门不是 fail-fast，而是 fail-open。对于发布和验收来说，这个行为不可接受。

### 6.2 架构判断

根据 `4_arch_design/13-部署安全与质量架构设计-v1.md`、`13_release/01-V1发布前检查清单-v1.md` 和测试计划，OpenAPI 同步检查属于正式质量闸门，不是辅助脚本。

既然它被定义为质量闸门，就必须满足一个基本原则：

- 后端不可达、下载失败、产物非法、生成失败、对比不一致，任何一步都必须明确失败。

不能接受“控制台看起来有报错，但退出码还是 0”的状态，因为 CI、发布前检查和人工验收都会被误导。

### 6.3 预期解决方案

开发应按以下方式处理：

1. 在 `download-openapi.ps1` 中把下载失败改为终止错误。
2. 下载先落临时文件，再校验 JSON 合法性，最后再覆盖正式快照。
3. `check-openapi-sync.ps1` 必须在刷新链路任一步失败时直接非零退出。
4. 可选但推荐：对下载结果做最小内容校验，确认不是空文件，也不是只含错误页。

推荐最小实现要点：

```powershell
$ErrorActionPreference = 'Stop'
Invoke-WebRequest -Uri "$BaseUrl/v3/api-docs" -OutFile $tempFile -ErrorAction Stop
$json = Get-Content -Path $tempFile -Raw | ConvertFrom-Json
if (-not $json.paths) {
    throw "OpenAPI 文档缺少 paths 节点。"
}
Move-Item -Path $tempFile -Destination $OutputPath -Force
```

对于 `check-openapi-sync.ps1`：

- 不要只依赖 `$LASTEXITCODE`；
- 要确保被调用脚本抛出的终止错误会使当前脚本失败；
- 必要时显式设置 `$ErrorActionPreference = 'Stop'`。

### 6.4 为什么要这样设计

- OpenAPI 快照与生成类型是正式派生产物，检查脚本必须“失败即停止”。
- 先写临时文件再替换，可避免下载失败时污染仓库中的现有快照。
- JSON 合法性和最小结构校验可以防止把 HTML 错误页、空响应或截断文件当成有效 OpenAPI。

### 6.5 开发最终应如何处理

- 把 `openapi:check` 当成正式质量闸门实现，不是“给人参考一下有没有 diff”的辅助命令。
- 下载失败、生成失败、内容非法、产物不同步，任何一种都必须让命令退出非零。
- 如后续还有其他发布前校验脚本，也应统一采用 fail-fast、fail-closed 设计，不允许误通过。

### 6.6 最终验收标准

整改完成后，以下条件必须同时成立：

- 后端可达时，`npm run openapi:check` 成功。
- 故意把 `MYAGENT_OPENAPI_BASE_URL` 指向不可达地址时，`npm run openapi:check` 明确失败并返回非零退出码。
- 下载失败不会污染现有 `openapi/myagent-openapi.json`。
- OpenAPI 快照与生成类型能被可信地判定为同源且幂等。

## 7. 建议整改顺序

建议开发按以下顺序处理，不要并行乱改：

1. 先修复前端契约漂移并恢复 `npm run build`。
2. 再修复后端标准启动脚本，使文档入口真实可跑。
3. 再修复 OpenAPI 检查脚本，使质量闸门可信。
4. 最后做一轮完整回归，确认代码、脚本、文档三者口径一致。

这个顺序的原因是：

- 构建通过是前端可交付的最低前提。
- 启动脚本可用是联调与复验的基础。
- 质量闸门可信后，后续每次契约刷新结果才有判断价值。

## 8. 最低回归要求

开发整改完成后，至少需要回归以下内容：

### 8.1 前端

- `cd D:\myproject\MyAgent\11_code\frontend`
- `npm test -- --run`
- `npm run build`
- `npm run openapi:check`

### 8.2 后端

- `cd D:\myproject\MyAgent`
- `powershell -NoProfile -ExecutionPolicy Bypass -File .\11_code\scripts\start-backend-local.ps1`
- `cd D:\myproject\MyAgent\11_code\backend`
- `mvn -q test`

### 8.3 反向失败验证

必须补做一条“故意失败”的验证：

- 设置不可达 `MYAGENT_OPENAPI_BASE_URL`
- 执行 `npm run openapi:check`
- 确认命令非零退出

如果这条失败验证不能成立，则说明质量闸门仍然不可信，验收不能通过。

## 9. 最终要求

本轮整改不接受以下处理方式：

- 用 `any`、类型断言或手写影子 DTO 绕过 `detailJson` 与 `detail` 的契约差异。
- 继续保留一个文档中“标准可用”、实际执行时却需要人工改命令的启动脚本。
- 让 `openapi:check` 继续保持“控制台报错但退出码为 0”的行为。
- 只更新文档，不修脚本和质量闸门本体。

本轮整改的目标不是“把页面再点通一次”，而是把以下三类真相全部收口：

- 前端消费真相：以 OpenAPI 生成类型为唯一契约来源。
- 启动真相：文档给出的标准脚本在目标 shell 下直接可跑。
- 质量闸门真相：OpenAPI 同步失败时必须明确失败。

只有这三项全部成立，二轮正式验收遗留问题才算真正关闭。

## 10. 项目结构重构完成记录

### 10.1 结构收口结果

- 已按最新 `AGENTS.md` 将项目根目录收口为 `0_specifications` 至 `14_user_manual` 的编号目录结构。
- 已新增最高优先级规则：尊重用户需求，严禁自作主张的设计；需求存在模糊点时必须主动向用户确认。
- 已将旧开发计划、测试计划、计划执行进程目录中的 V1 文档合并迁移到 `8_change_list\agent-management-platform-v1`。
- `8_change_list\agent-management-platform-v1` 仅保留 `purpose.md`、`plan.md`、`design.md`、`steps.md`、`test_steps.md`、`status.md` 六个约定文件。
- 已删除被替换的旧计划、测试、执行进程、错误集目录。
- 已将根目录旧工具缓存目录移入 `9_dependency\tools`，并在 `9_dependency\README.md` 中说明工具链依赖规则。
- 已创建 `10_error_list\README.md`，作为错误列表目录的空目录占位说明。
- 已同步更新历史测试报告、部署说明、代码说明和脚本中的旧路径引用。

### 10.2 验证结果

- 旧路径引用检索：无命中，排除了工具缓存、前端依赖、前端构建产物、OpenAPI 生成产物和后端构建产物。
- `rg --files D:\myproject\MyAgent\8_change_list\agent-management-platform-v1`：仅返回六个约定文件。
- 根目录旧工具缓存目录存在性检查：不存在。
- `Test-Path 9_dependency\tools`：`True`。
- 使用 `9_dependency\tools` 中的工具链执行 `java -version`、`mvn -version`：JDK `21.0.11`、Maven `3.9.11` 可用。
- `git diff --check`：通过；仅有 Git LF/CRLF 换行提示。
- `cd D:\myproject\MyAgent\11_code\backend; mvn -q test`：通过。
- `cd D:\myproject\MyAgent\11_code\frontend; npm test -- --run`：通过，7 个测试文件、13 个测试用例通过。
- `cd D:\myproject\MyAgent\11_code\frontend; npm run build`：通过；仍有 Vite 大 chunk 警告，不影响本次目录结构重构验收。



