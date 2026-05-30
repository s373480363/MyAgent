import { Card, List, Space, Typography } from "antd";
import { ReactNode } from "react";

/**
 * 功能说明卡片属性。
 */
interface FeatureIntroCardProps {
  title: string;
  description: string;
  bullets: string[];
  extra?: ReactNode;
}

/**
 * 功能说明卡片。
 *
 * @param props 卡片属性
 * @returns 说明卡片
 */
export function FeatureIntroCard({ title, description, bullets, extra }: FeatureIntroCardProps) {
  return (
    <Card className="page-card">
      <Space direction="vertical" size={12} style={{ width: "100%" }}>
        <div>
          <Typography.Title level={4}>{title}</Typography.Title>
          <Typography.Paragraph className="muted-text">{description}</Typography.Paragraph>
        </div>
        <List
          className="feature-list"
          dataSource={bullets}
          renderItem={(bullet) => (
            <List.Item>
              <Typography.Text>{bullet}</Typography.Text>
            </List.Item>
          )}
        />
        {extra}
      </Space>
    </Card>
  );
}
