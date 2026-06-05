import { Card, Col, Row, Space, Statistic, Typography } from "antd";
import { FeatureIntroCard } from "../../../shared/components/FeatureIntroCard";

/**
 * 首页概览页。
 */
export function HomePage() {
  return (
    <Space direction="vertical" size={24} style={{ width: "100%" }}>
      <div className="page-section">
        <Typography.Title level={2}>Agent Studio 管理台</Typography.Title>
        <Typography.Paragraph className="muted-text">
          从这里进入 Agent 设计、运行回溯、模型供应商目录、节点验收和平台主数据维护。页面统一基于当前后端 OpenAPI 契约构建。
        </Typography.Paragraph>
      </div>
      <Row gutter={[16, 16]}>
        <Col xs={24} md={8}>
          <Card className="metric-card">
            <Statistic title="V1 能力入口" value={10} />
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card className="metric-card">
            <Statistic title="模型路由事实源" value="已统一" />
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card className="metric-card">
            <Statistic title="OpenAPI 类型" value="已同步" />
          </Card>
        </Col>
      </Row>
      <FeatureIntroCard
        title="控制台入口"
        description="侧边导航覆盖完整 V1 的配置、设计、运行、追踪和验收入口，便于按业务链路排查和回看。"
        bullets={[
          "Agents：维护 Agent 基础信息、默认模型供应项、草稿和历史版本入口",
          "Workflow：保存草稿、校验、发布和调试运行",
          "Runs：查看运行摘要、NodeRun、Trace 和绑定版本",
          "模型供应商：维护供应商、供应项、密钥和连接测试",
          "Evals：管理套件、用例、运行结果和历史对比",
          "Settings：仅显示和更新 V1 白名单配置"
        ]}
      />
    </Space>
  );
}
