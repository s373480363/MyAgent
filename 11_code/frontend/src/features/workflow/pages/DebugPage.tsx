import { useState } from "react";
import { App, Button, Card, Descriptions, Form, InputNumber, Select, Space, Typography } from "antd";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useNavigate, useParams } from "react-router-dom";
import { getRunDetail, listWorkflowVersions, runDebugAgent, type Schema } from "../../../api/domainApi";
import { JsonBlock, parseJsonText } from "../../../shared/components/JsonBlock";
import { JsonTextArea } from "../../../shared/components/JsonTextArea";
import { PageState } from "../../../shared/components/PageState";
import { StatusTag } from "../../../shared/components/StatusTag";

type DebugFormValues = {
  workflowVersionId?: number;
  inputText?: string;
};

/**
 * 调试运行页面。
 */
export function DebugPage() {
  const { message } = App.useApp();
  const navigate = useNavigate();
  const { agentId } = useParams();
  const numericAgentId = Number(agentId);
  const [form] = Form.useForm<DebugFormValues>();
  const [runId, setRunId] = useState<string | null>(null);
  const versionsQuery = useQuery({
    queryKey: ["workflow-versions", numericAgentId],
    queryFn: () => listWorkflowVersions(numericAgentId, { page: 1, pageSize: 50 }),
    enabled: Number.isFinite(numericAgentId) && numericAgentId > 0
  });
  const runDetailQuery = useQuery({
    queryKey: ["run-detail", runId],
    queryFn: () => getRunDetail(runId ?? ""),
    enabled: Boolean(runId)
  });
  const debugMutation = useMutation({
    mutationFn: (body: Schema["DebugRunRequest"]) => runDebugAgent(numericAgentId, body),
    onSuccess: (result) => {
      message.success(`调试运行已完成：${result.runId}`);
      setRunId(result.runId ?? null);
    }
  });

  if (versionsQuery.isLoading || versionsQuery.isError) {
    return (
      <PageState
        title="调试运行"
        description="暂无可调试工作流版本。"
        loading={versionsQuery.isLoading}
        error={versionsQuery.error?.message}
      />
    );
  }

  return (
    <Space direction="vertical" size={16} style={{ width: "100%" }}>
      <section className="page-section page-title-row">
        <div>
          <Typography.Title level={3}>调试运行</Typography.Title>
          <Typography.Paragraph className="muted-text">
            调试运行必须绑定已持久化的 WorkflowVersion，运行详情中继续展示实际 workflowVersionId。
          </Typography.Paragraph>
        </div>
        <Button onClick={() => navigate(`/agents/${numericAgentId}/workflow`)}>返回工作流</Button>
      </section>
      <Card className="page-card">
        <Form form={form} layout="vertical">
          <Form.Item name="workflowVersionId" label="工作流版本">
            <Select
              allowClear
              placeholder="默认由后端选择当前可调试版本"
              options={(versionsQuery.data?.items ?? []).map((item) => ({
                value: item.workflowVersionId,
                label: `#${item.workflowVersionId} / v${item.versionNo} / ${item.status}`
              }))}
            />
          </Form.Item>
          <Form.Item name="inputText" label="运行输入 JSON">
            <JsonTextArea rows={10} />
          </Form.Item>
          <Button type="primary" loading={debugMutation.isPending} onClick={() => submitDebug()}>
            发起调试运行
          </Button>
        </Form>
      </Card>
      {debugMutation.data ? (
        <Card className="page-card" title="调试运行摘要">
          <Descriptions bordered size="small" column={3}>
            <Descriptions.Item label="Run ID">{debugMutation.data.runId}</Descriptions.Item>
            <Descriptions.Item label="状态"><StatusTag status={debugMutation.data.status ?? "UNKNOWN"} /></Descriptions.Item>
            <Descriptions.Item label="绑定版本">{debugMutation.data.workflowVersionId}</Descriptions.Item>
            <Descriptions.Item label="耗时">{debugMutation.data.durationMs ?? "-"} ms</Descriptions.Item>
            <Descriptions.Item label="错误" span={2}>{debugMutation.data.error?.message ?? "-"}</Descriptions.Item>
          </Descriptions>
          <Space style={{ marginTop: 12 }}>
            <Button disabled={!debugMutation.data.runId} onClick={() => debugMutation.data?.runId && navigate(`/runs/${debugMutation.data.runId}`)}>
              打开运行详情
            </Button>
          </Space>
        </Card>
      ) : null}
      {runDetailQuery.data ? (
        <JsonBlock
          title={`运行详情快照：${runDetailQuery.data.runId}`}
          value={{
            workflowVersion: runDetailQuery.data.workflowVersion,
            status: runDetailQuery.data.status,
            nodeRuns: runDetailQuery.data.nodeRuns,
            traceEvents: runDetailQuery.data.traceEvents
          }}
        />
      ) : null}
    </Space>
  );

  /**
   * 提交调试运行。
   */
  function submitDebug() {
    try {
      const values = form.getFieldsValue();
      debugMutation.mutate({
        workflowVersionId: values.workflowVersionId,
        input: values.inputText ? parseJsonText(values.inputText) : undefined
      });
    } catch {
      message.error("运行输入 JSON 格式不正确。");
    }
  }
}
