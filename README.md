# Agent Studio

Agent Studio 是面向本机或内网单用户场景的智能体工作台。

唯一正式部署入口：

```powershell
cd D:\myproject\MyAgent\11_code
docker compose up -d --build
```

正式部署文件位于 `11_code/compose.yaml` 和 `11_code/.env`。

唯一正式访问入口：

- [http://127.0.0.1:18080](http://127.0.0.1:18080)

正式部署要求：

- `AGENT_STUDIO_POSTGRES_PASSWORD` 必填。
- `AGENT_STUDIO_SECRET_KEY` 必填，必须是 Base64 编码的 32 字节随机值，用于模型供应商 API Key 可逆加密。
- `AGENT_STUDIO_OPENAI_API_KEY` 必填。
- `AGENT_STUDIO_OPENAI_BASE_URL` 默认为 `https://api.openai.com`。
- `AGENT_STUDIO_OPENAI_DEFAULT_MODEL` 默认为 `gpt-4.1-mini`。
