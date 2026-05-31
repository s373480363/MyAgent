import { useState } from "react";
import { Card, Descriptions, Drawer, Space, Table, Typography } from "antd";
import { useQuery } from "@tanstack/react-query";
import { getJavaMethod, listJavaMethods, type Schema } from "../../../api/domainApi";
import { JsonBlock } from "../../../shared/components/JsonBlock";
import { PageState } from "../../../shared/components/PageState";
import { StatusTag } from "../../../shared/components/StatusTag";

/**
 * Java 方法页面。
 */
export function MethodsPage() {
  const [page, setPage] = useState(1);
  const [selectedMethodId, setSelectedMethodId] = useState<number | null>(null);
  const methodsQuery = useQuery({
    queryKey: ["java-methods", page],
    queryFn: () => listJavaMethods({ page, pageSize: 20 })
  });
  const detailQuery = useQuery({
    queryKey: ["java-method-detail", selectedMethodId],
    queryFn: () => getJavaMethod(selectedMethodId ?? 0),
    enabled: Boolean(selectedMethodId)
  });

  if (methodsQuery.isLoading || methodsQuery.isError) {
    return (
      <PageState
        title="Java 方法"
        description="暂无 Java 方法。"
        loading={methodsQuery.isLoading}
        error={methodsQuery.error?.message}
      />
    );
  }

  return (
    <Space direction="vertical" size={16} style={{ width: "100%" }}>
      <section className="page-section">
        <Typography.Title level={3}>Java 方法</Typography.Title>
        <Typography.Paragraph className="muted-text">
          Java 方法目录为 JAVA_METHOD 节点提供可调用目标和输入输出 Schema。
        </Typography.Paragraph>
      </section>
      <Card className="page-card">
        <Table<Schema["JavaMethodListItemResult"]>
          rowKey={(record) => String(record.id)}
          dataSource={methodsQuery.data?.items ?? []}
          pagination={{
            current: page,
            pageSize: methodsQuery.data?.pageSize ?? 20,
            total: methodsQuery.data?.total ?? 0,
            onChange: setPage
          }}
          onRow={(record) => ({
            onClick: () => record.id && setSelectedMethodId(record.id)
          })}
          columns={[
            { title: "Method Key", dataIndex: "methodKey" },
            { title: "名称", dataIndex: "name" },
            { title: "Bean", dataIndex: "beanName" },
            { title: "方法", dataIndex: "methodName" },
            { title: "输入 Schema", dataIndex: "inputSchemaId" },
            { title: "输出 Schema", dataIndex: "outputSchemaId" },
            { title: "状态", dataIndex: "status", render: (value) => <StatusTag status={String(value)} /> }
          ]}
        />
      </Card>
      <Drawer title="Java 方法详情" open={Boolean(selectedMethodId)} width={760} onClose={() => setSelectedMethodId(null)}>
        {detailQuery.isLoading || detailQuery.isError ? (
          <PageState
            title="Java 方法详情"
            description="暂无 Java 方法详情。"
            loading={detailQuery.isLoading}
            error={detailQuery.error?.message}
          />
        ) : (
          <Space direction="vertical" size={16} style={{ width: "100%" }}>
            <Descriptions bordered column={2} size="small">
              <Descriptions.Item label="Method Key">{detailQuery.data?.methodKey}</Descriptions.Item>
              <Descriptions.Item label="状态"><StatusTag status={detailQuery.data?.status ?? "UNKNOWN"} /></Descriptions.Item>
              <Descriptions.Item label="Bean">{detailQuery.data?.beanName}</Descriptions.Item>
              <Descriptions.Item label="方法">{detailQuery.data?.methodName}</Descriptions.Item>
              <Descriptions.Item label="输入 Schema">{detailQuery.data?.inputSchemaId ?? "-"}</Descriptions.Item>
              <Descriptions.Item label="输出 Schema">{detailQuery.data?.outputSchemaId ?? "-"}</Descriptions.Item>
              <Descriptions.Item label="描述" span={2}>{detailQuery.data?.description ?? "-"}</Descriptions.Item>
            </Descriptions>
            <JsonBlock title="方法目录对象" value={detailQuery.data} />
          </Space>
        )}
      </Drawer>
    </Space>
  );
}
