import { Card, Empty, Result, Skeleton } from "antd";
import { ReactNode } from "react";

/**
 * 页面状态组件属性。
 */
interface PageStateProps {
  title: string;
  description: string;
  loading?: boolean;
  error?: string | null;
  extra?: ReactNode;
}

/**
 * 通用页面状态组件。
 *
 * @param props 页面状态属性
 * @returns 页面状态展示
 */
export function PageState({ title, description, loading = false, error, extra }: PageStateProps) {
  if (loading) {
    return (
      <Card className="page-card">
        <Skeleton active paragraph={{ rows: 6 }} />
      </Card>
    );
  }

  if (error) {
    return (
      <Card className="page-card">
        <Result status="error" title={title} subTitle={error} extra={extra} />
      </Card>
    );
  }

  return (
    <Card className="page-card">
      <Empty description={description} image={Empty.PRESENTED_IMAGE_SIMPLE} />
      {extra}
    </Card>
  );
}
