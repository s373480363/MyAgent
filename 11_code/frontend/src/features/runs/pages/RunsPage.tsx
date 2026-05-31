import { useEffect, useState } from "react";
import { App, Button, Card, Descriptions, Drawer, Form, Input, InputNumber, Modal, Space, Table, Tabs, Typography } from "antd";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useNavigate, useParams } from "react-router-dom";
import { createEvalCaseFromNodeRun, getRunDetail, listRuns, type Schema } from "../../../api/domainApi";
import { queryClient } from "../../../app/queryClient";
import { JsonBlock } from "../../../shared/components/JsonBlock";
import { PageState } from "../../../shared/components/PageState";
import { StatusTag } from "../../../shared/components/StatusTag";

type CreateCaseFromNodeRunValues = {
  suiteId?: number;
  title: string;
  description?: string;
};

/**
 * 运行记录页面。
 */
export function RunsPage() {
  const { message } = App.useApp();
  const navigate = useNavigate();
  const { runId: routeRunId } = useParams();
  const [page, setPage] = useState(1);
  const [selectedRunId, setSelectedRunId] = useState<string | null>(routeRunId ?? null);
  const [caseNodeRun, setCaseNodeRun] = useState<Schema["NodeRunResult"] | null>(null);
  const [caseForm] = Form.useForm<CreateCaseFromNodeRunValues>();
  const runsQuery = useQuery({
    queryKey: ["runs", page],
    queryFn: () => listRuns({ page, pageSize: 20 })
  });
  const detailQuery = useQuery({
    queryKey: ["run-detail", selectedRunId],
    queryFn: () => getRunDetail(selectedRunId ?? ""),
    enabled: Boolean(selectedRunId)
  });
  const createCaseMutation = useMutation({
    mutationFn: ({ nodeRunId, body }: { nodeRunId: number; body: Schema["CreateEvalCaseFromNodeRunRequest"] }) => createEvalCaseFromNodeRun(nodeRunId, body),
    onSuccess: () => {
      message.success("已从 NodeRun 创建验收用例。");
      setCaseNodeRun(null);
      caseForm.resetFields();
      void queryClient.invalidateQueries({ queryKey: ["eval-suites"] });
    }
  });

  useEffect(() => {
    if (routeRunId) {
      setSelectedRunId(routeRunId);
    }
  }, [routeRunId]);

  if (runsQuery.isLoading || runsQuery.isError) {
    return (
      <PageState
        title="运行记录"
        description="暂无运行记录。"
        loading={runsQuery.isLoading}
        error={runsQuery.error?.message}
      />
    );
  }

  return (
    <Space direction="vertical" size={16} style={{ width: "100%" }}>
      <section className="page-section">
        <Typography.Title level={3}>运行记录</Typography.Title>
        <Typography.Paragraph className="muted-text">
          默认展示普通运行；验收运行需显式按 runType=EVAL 查询，运行详情统一承接 Trace 排障和版本回看。
        </Typography.Paragraph>
      </section>
      <Card className="page-card">
        <Table<Schema["RunListItemResult"]>
          rowKey={(record) => record.runId ?? ""}
          dataSource={runsQuery.data?.items ?? []}
          pagination={{
            current: page,
            pageSize: runsQuery.data?.pageSize ?? 20,
            total: runsQuery.data?.total ?? 0,
            onChange: setPage
          }}
          onRow={(record) => ({
            onClick: () => {
              if (record.runId) {
                setSelectedRunId(record.runId);
                navigate(`/runs/${record.runId}`);
              }
            }
          })}
          columns={[
            { title: "Run ID", dataIndex: "runId" },
            { title: "Agent", dataIndex: "agentName" },
            { title: "类型", dataIndex: "runType", render: (value) => <StatusTag status={String(value)} /> },
            { title: "状态", dataIndex: "status", render: (value) => <StatusTag status={String(value)} /> },
            { title: "版本", dataIndex: "workflowVersionId" },
            { title: "开始时间", dataIndex: "startedAt" },
            { title: "耗时(ms)", dataIndex: "durationMs" }
          ]}
        />
      </Card>
      <Drawer
        title={selectedRunId ? `运行详情：${selectedRunId}` : "运行详情"}
        width={1040}
        open={Boolean(selectedRunId)}
        onClose={() => {
          setSelectedRunId(null);
          navigate("/runs");
        }}
      >
        {detailQuery.isLoading || detailQuery.isError ? (
          <PageState
            title="运行详情"
            description="暂无运行详情。"
            loading={detailQuery.isLoading}
            error={detailQuery.error?.message}
          />
        ) : (
          <RunDetailView
            detail={detailQuery.data}
            onOpenRun={(runId) => navigate(`/runs/${runId}`)}
            onOpenVersion={(agentId, workflowVersionId) => navigate(`/agents/${agentId}/workflow/versions/${workflowVersionId}`)}
            onCreateCase={(nodeRun) => {
              setCaseNodeRun(nodeRun);
              caseForm.setFieldsValue({ title: `${nodeRun.nodeName ?? nodeRun.nodeId} 验收用例` });
            }}
          />
        )}
      </Drawer>
      <Modal
        title="从 NodeRun 创建验收用例"
        open={Boolean(caseNodeRun)}
        confirmLoading={createCaseMutation.isPending}
        onCancel={() => setCaseNodeRun(null)}
        onOk={() => {
          void caseForm.validateFields().then((values) => {
            if (!caseNodeRun?.nodeRunId) {
              message.error("缺少 node_run.id，无法创建验收用例。");
              return;
            }
            createCaseMutation.mutate({
              nodeRunId: caseNodeRun.nodeRunId,
              body: {
                suiteId: values.suiteId,
                title: values.title,
                description: values.description
              }
            });
          });
        }}
      >
        <Form form={caseForm} layout="vertical">
          <Form.Item name="suiteId" label="目标套件 ID">
            <InputNumber min={1} style={{ width: "100%" }} />
          </Form.Item>
          <Form.Item name="title" label="用例标题" rules={[{ required: true, message: "请输入用例标题。" }]}>
            <Input />
          </Form.Item>
          <Form.Item name="description" label="说明">
            <Input.TextArea rows={3} />
          </Form.Item>
        </Form>
      </Modal>
    </Space>
  );
}

/**
 * 运行详情展示。
 *
 * @param props 组件属性
 * @returns 运行详情
 */
export function RunDetailView({
  detail,
  onOpenRun,
  onOpenVersion,
  onCreateCase
}: {
  detail?: Schema["RunDetailResult"];
  onOpenRun: (runId: string) => void;
  onOpenVersion: (agentId: number, workflowVersionId: number) => void;
  onCreateCase: (nodeRun: Schema["NodeRunResult"]) => void;
}) {
  const workflowVersionId = detail?.workflowVersion?.workflowVersionId;
  const agentId = detail?.agent?.agentId;

  return (
    <Space direction="vertical" size={16} style={{ width: "100%" }}>
      <Descriptions bordered size="small" column={2}>
        <Descriptions.Item label="状态">
          <StatusTag status={detail?.status ?? "UNKNOWN"} />
        </Descriptions.Item>
        <Descriptions.Item label="类型">{detail?.runType}</Descriptions.Item>
        <Descriptions.Item label="Agent">{detail?.agent?.agentName}</Descriptions.Item>
        <Descriptions.Item label="工作流版本">
          <Space>
            <span>{workflowVersionId ?? "-"}</span>
            <Button
              size="small"
              disabled={!agentId || !workflowVersionId}
              onClick={() => agentId && workflowVersionId && onOpenVersion(agentId, workflowVersionId)}
            >
              回看版本
            </Button>
          </Space>
        </Descriptions.Item>
        <Descriptions.Item label="父运行">
          {detail?.parentRunId ? <Button type="link" onClick={() => onOpenRun(detail.parentRunId ?? "")}>{detail.parentRunId}</Button> : "-"}
        </Descriptions.Item>
        <Descriptions.Item label="EvalRun">{detail?.evalRunId ?? "-"}</Descriptions.Item>
        <Descriptions.Item label="错误" span={2}>{detail?.error?.message ?? "-"}</Descriptions.Item>
      </Descriptions>
      <Tabs
        items={[
          {
            key: "nodes",
            label: "NodeRuns",
            children: (
              <Table<Schema["NodeRunResult"]>
                rowKey={(record) => String(record.nodeRunId)}
                dataSource={detail?.nodeRuns ?? []}
                pagination={false}
                expandable={{
                  expandedRowRender: (record) => (
                    <Space direction="vertical" style={{ width: "100%" }}>
                      <JsonBlock title="输入" value={record.input} />
                      <JsonBlock title="输出" value={record.output} />
                      <JsonBlock title="Schema 校验结果" value={record.schemaValidationResult} />
                    </Space>
                  )
                }}
                columns={[
                  { title: "NodeRun ID", dataIndex: "nodeRunId" },
                  { title: "节点", dataIndex: "nodeName" },
                  { title: "类型", dataIndex: "nodeType" },
                  { title: "状态", dataIndex: "status", render: (value) => <StatusTag status={String(value)} /> },
                  { title: "错误", dataIndex: "errorMessage" },
                  { title: "耗时(ms)", dataIndex: "durationMs" },
                  {
                    title: "操作",
                    render: (_, record) => (
                      <Button type="link" disabled={!record.nodeRunId} onClick={() => onCreateCase(record)}>
                        生成验收用例
                      </Button>
                    )
                  }
                ]}
              />
            )
          },
          {
            key: "trace",
            label: "Trace",
            children: (
              <Table<Schema["TraceEventResult"]>
                rowKey={(record) => String(record.traceEventId)}
                dataSource={detail?.traceEvents ?? []}
                pagination={false}
                expandable={{ expandedRowRender: (record) => <JsonBlock title="详情" value={record.detailJson} /> }}
                columns={[
                  { title: "事件", dataIndex: "eventType", render: (value) => <StatusTag status={String(value)} /> },
                  { title: "摘要", dataIndex: "summary" },
                  { title: "NodeRun", dataIndex: "nodeRunId" },
                  { title: "EvalRun", dataIndex: "evalRunId" },
                  { title: "时间", dataIndex: "eventTime" }
                ]}
              />
            )
          },
          {
            key: "children",
            label: "子运行",
            children: (
              <Table<Schema["ChildRunResult"]>
                rowKey={(record) => record.runId ?? ""}
                dataSource={detail?.childRuns ?? []}
                pagination={false}
                columns={[
                  {
                    title: "Run ID",
                    dataIndex: "runId",
                    render: (value) => (value ? <Button type="link" onClick={() => onOpenRun(String(value))}>{String(value)}</Button> : "-")
                  },
                  { title: "摘要", dataIndex: "summary" }
                ]}
              />
            )
          },
          {
            key: "payload",
            label: "输入输出",
            children: (
              <Space direction="vertical" style={{ width: "100%" }}>
                <JsonBlock title="运行输入" value={detail?.input} />
                <JsonBlock title="运行输出" value={detail?.output} />
              </Space>
            )
          }
        ]}
      />
    </Space>
  );
}
