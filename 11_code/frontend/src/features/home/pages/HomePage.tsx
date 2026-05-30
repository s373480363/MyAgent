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
          当前页面用于验证全局布局、基础路由、模块入口和公共组件骨架。后续步骤会在此基础上继续接入真实接口、表格、表单和工作流设计器。
        </Typography.Paragraph>
      </div>
      <Row gutter={[16, 16]}>
        <Col xs={24} md={8}>
          <Card className="metric-card">
            <Statistic title="当前阶段" value="步骤 01" />
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card className="metric-card">
            <Statistic title="后续页面入口" value={9} />
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card className="metric-card">
            <Statistic title="公共基线" value="已接入" />
          </Card>
        </Col>
      </Row>
      <FeatureIntroCard
        title="本步交付内容"
        description="前端工程骨架优先承接全局布局、模块入口和公共组件，不在第 01 步提前引入业务语义或复杂视觉包装。"
        bullets={[
          "React + TypeScript + Vite 工程可独立启动",
          "全局路由和侧边导航已接通",
          "TanStack Query 与 Ant Design 已接入",
          "后续各功能目录已预留",
          "统一 HTTP Client 已准备",
          "基础页面状态组件已准备"
        ]}
      />
    </Space>
  );
}
