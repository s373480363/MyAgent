## 最高优先级
- 务必优先参考[项目结构说明]和[重要文档说明]中的目录和文件来获取和更新信息
- 优先级（冲突处理）：用户的明确指令 > 本 `AGENTS.md` > 其他仓库文档/现有代码习惯
- 优先使用尽可能简单的解决方案
- 尊重用户需求，严禁自作主张的设计；如果认为需求存在模糊点，必须主动向用户确认，而不是自行决定

## 项目结构说明
| 目录 | 职责 | 读写时机 |
|------|------|----------|
| `0_specifications\` | 规范与注意事项 | 执行任何任务前先检索需遵守的规则 |
| `1_context\` | 项目依赖的上下文 | 需要补充上下文时查找线索 |
| `2_tool_list\` | 可复用外部工具集 | 需要外部工具时优先查找；创建可复用工具时存放 |
| `3_product_design\` | 产品设计文档 | 查询/更新产品设计 |
| `4_arch_design\` | 架构设计文档 | 查询/更新架构设计 |
| `5_ui_design\` | 界面设计文档 | 查询/更新界面设计 |
| `6_schema_design\` | 数据结构设计文档 | 查询/更新数据结构 |
| `7_interface_design\` | 公共接口设计文档 | 查询/更新公共接口 |
| `8_change_list\` | 项目变更列表 | 查询/更新所有项目变动信息；每个变更目录内维护目的、计划、设计、步骤、验收步骤、验收结果和状态 |
| `9_dependency\` | 项目依赖 | 查询/更新项目依赖信息 |
| `10_error_list\` | 错误列表 | 查询历史错误/记录新错误 |
| `11_code\` | 源代码 | 所有项目代码及直接依赖 |
| `12_test_env\` | 测试环境信息 | 查询/更新验收测试所需要的环境信息 |
| `13_release\` | 发布产物 | 发布后内容 |
| `14_user_manual\` | 用户手册 | 提供给用户的教程说明 |

## 重要文档说明
- `0_specifications\develop_specification.md` — 开发规范
- `0_specifications\ui_deisgn_specification.md` — 前端设计规范
- `0_specifications\code_review_specification.md` — 代码审查规范
- `0_specifications\test_specification.md` — 测试验收规范
- `8_change_list\${change-name}\purpose.md` — 描述本次变更最终需要达成什么样的目的
- `8_change_list\${change-name}\plan.md` — 描述本次变更预期的修改计划
- `8_change_list\${change-name}\design.md` — 描述本次变更对于项目设计的调整，可能会包含 product、arch、ui、schema、interface 等多个维度的调整
- `8_change_list\${change-name}\steps.md` — 描述本次变更的具体执行步骤
- `8_change_list\${change-name}\test_steps.md` — 描述本次变更的具体验收步骤
- `8_change_list\${change-name}\test_result\` — 存放本次变更的实际测试报告、复验记录、终验报告等验收结果
- `8_change_list\${change-name}\status.md` — 描述本次变更的目前处于的状态，例如设计完成、架构审核完成、开发审核完成、开发完成、验收完成
