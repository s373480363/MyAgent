# 项目依赖

本目录用于存放项目依赖信息和本地工具链依赖。

## 目录约定

- `tools\`：本地工具链缓存目录，用于存放本项目固定使用的 JDK、Maven 等工具。该目录由 `.gitignore` 忽略，不提交到 Git。
- 依赖说明应记录版本、用途和获取方式。
- 不允许在本目录保存密钥、Token、账号密码或其他敏感信息。

## 当前工具链

- JDK：`21.0.11`
- Maven：`3.9.11`
- Node.js：`24.x` 或兼容版本
- npm：`11.x` 或兼容版本
- PostgreSQL：`15+`
- Docker Desktop：用于 Testcontainers 和本地 PostgreSQL 验收辅助
