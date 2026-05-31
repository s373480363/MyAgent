import { useState } from "react";
import { Card, Descriptions, Drawer, Space, Table, Typography } from "antd";
import { useQuery } from "@tanstack/react-query";
import { getTool, listTools, type Schema } from "../../../api/domainApi";
import { JsonBlock } from "../../../shared/components/JsonBlock";
import { PageState } from "../../../shared/components/PageState";
import { StatusTag } from "../../../shared/components/StatusTag";

/**
 * 工具目录页面。
 */
export function ToolsPage() {
  const [page, setPage] = useState(1);
  const [selectedToolId, setSelectedToolId] = useState<number | null>(null);
  const toolsQuery = useQuery({
    queryKey: ["tools", page],
    queryFn: () => listTools({ page, pageSize: 20 })
  });
  const detailQuery = useQuery({
    queryKey: ["tool-detail", selectedToolId],
    queryFn: () => getTool(selectedToolId ?? 0),
    enabled: Boolean(selectedToolId)
  });

  if (toolsQuery.isLoading || toolsQuery.isError) {
    return (
      <PageState
        title="工具目录"
        description="暂无工具。"
        loading={toolsQuery.isLoading}
        error={toolsQuery.error?.message}
      />
    );
  }

  return (
    <Space direction="vertical" size={16} style={{ width: "100%" }}>
      <section className="page-section">
        <Typography.Title level={3}>工具目录</Typography.Title>
        <Typography.Paragraph className="muted-text">
          TOOL 节点只能引用已注册工具，输入输出均由 Schema 约束。
        </Typography.Paragraph>
      </section>
      <Card className="page-card">
        <Table<Schema["ToolListItemResult"]>
          rowKey={(record) => String(record.id)}
          dataSource={toolsQuery.data?.items ?? []}
          pagination={{
            current: page,
            pageSize: toolsQuery.data?.pageSize ?? 20,
            total: toolsQuery.data?.total ?? 0,
            onChange: setPage
          }}
          onRow={(record) => ({
            onClick: () => record.id && setSelectedToolId(record.id)
          })}
          columns={[
            { title: "Tool Key", dataIndex: "toolKey" },
            { title: "名称", dataIndex: "name" },
            { title: "执行器", dataIndex: "executorType" },
            { title: "输入 Schema", dataIndex: "inputSchemaId" },
            { title: "输出 Schema", dataIndex: "outputSchemaId" },
            { title: "状态", dataIndex: "status", render: (value) => <StatusTag status={String(value)} /> }
          ]}
        />
      </Card>
      <Drawer title="工具详情" open={Boolean(selectedToolId)} width={760} onClose={() => setSelectedToolId(null)}>
        {detailQuery.isLoading || detailQuery.isError ? (
          <PageState
            title="工具详情"
            description="暂无工具详情。"
            loading={detailQuery.isLoading}
            error={detailQuery.error?.message}
          />
        ) : (
          <Space direction="vertical" size={16} style={{ width: "100%" }}>
            <Descriptions bordered column={2} size="small">
              <Descriptions.Item label="Tool Key">{detailQuery.data?.toolKey}</Descriptions.Item>
              <Descriptions.Item label="状态"><StatusTag status={detailQuery.data?.status ?? "UNKNOWN"} /></Descriptions.Item>
              <Descriptions.Item label="名称">{detailQuery.data?.name}</Descriptions.Item>
              <Descriptions.Item label="执行器">{detailQuery.data?.executorType}</Descriptions.Item>
              <Descriptions.Item label="输入 Schema">{detailQuery.data?.inputSchemaId ?? "-"}</Descriptions.Item>
              <Descriptions.Item label="输出 Schema">{detailQuery.data?.outputSchemaId ?? "-"}</Descriptions.Item>
              <Descriptions.Item label="描述" span={2}>{detailQuery.data?.description ?? "-"}</Descriptions.Item>
            </Descriptions>
            <JsonBlock title="执行器配置" value={detailQuery.data?.executorConfigJson} />
          </Space>
        )}
      </Drawer>
    </Space>
  );
}
