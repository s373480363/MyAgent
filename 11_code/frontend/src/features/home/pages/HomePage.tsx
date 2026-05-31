import { Card, Col, Row, Space, Statistic, Typography } from "antd";
import { FeatureIntroCard } from "../../../shared/components/FeatureIntroCard";

/**
 * 首页概览页面。
 */
export function HomePage() {
  return (
    <Space direction="vertical" size={24} style={{ width: "100%" }}>
      <div className="page-section">
        <Typography.Title level={2}>MyAgent 管理台</Typography.Title>
        <Typography.Paragraph className="muted-text">
          从这里进入 Agent 设计、运行回溯、节点验收和平台主数据维护。页面均基于后端 OpenAPI 契约和统一运行语义构建。
        </Typography.Paragraph>
      </div>
      <Row gutter={[16, 16]}>
        <Col xs={24} md={8}>
          <Card className="metric-card">
            <Statistic title="V1 能力域" value={9} />
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card className="metric-card">
            <Statistic title="运行 ID 口径" value="已统一" />
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card className="metric-card">
            <Statistic title="OpenAPI 类型" value="已接入" />
          </Card>
        </Col>
      </Row>
      <FeatureIntroCard
        title="控制台入口"
        description="侧边导航覆盖完整 V1 的配置、设计、运行、追踪和验收入口，便于按业务链路排查和回看。"
        bullets={[
          "Agents：维护 Agent 基础信息、启停、草稿和历史版本入口",
          "Workflow：保存草稿、校验、发布和调试运行",
          "Runs：查看运行摘要、NodeRun、Trace 和绑定版本",
          "Evals：管理套件、用例、运行结果和历史对比",
          "Settings：仅展示和更新 V1 白名单配置"
        ]}
      />
    </Space>
  );
}
