# 测试环境目录

本目录记录验收测试需要使用的环境信息。

- `test_env.md`：正式业务验收入口、后端本机 JDK/Maven 工具链、Docker Desktop/Testcontainers 前置条件和 LLM 测试配置。

测试人员执行真实验收前必须先阅读 `test_env.md`，不得在未确认 Testcontainers 可访问 Docker 的环境中形成完整后端自动化测试结论。
