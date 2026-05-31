import { useEffect, useState } from "react";
import { App, Button, Card, Descriptions, Drawer, Form, Input, InputNumber, Modal, Space, Table, Typography } from "antd";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import {
  changeAgentStatus,
  createAgent,
  getAgent,
  listAgents,
  listWorkflowVersions,
  updateAgent,
  type Schema
} from "../../../api/domainApi";
import { queryClient } from "../../../app/queryClient";
import { PageState } from "../../../shared/components/PageState";
import { StatusTag } from "../../../shared/components/StatusTag";

type AgentFormMode = "create" | "edit";

/**
 * Agent 管理页面。
 */
export function AgentsPage() {
  const { message } = App.useApp();
  const navigate = useNavigate();
  const [page, setPage] = useState(1);
  const [selectedAgentId, setSelectedAgentId] = useState<number | null>(null);
  const [formMode, setFormMode] = useState<AgentFormMode | null>(null);
  const [form] = Form.useForm<Schema["CreateAgentRequest"] & Schema["UpdateAgentRequest"]>();
  const agentsQuery = useQuery({
    queryKey: ["agents", page],
    queryFn: () => listAgents({ page, pageSize: 20 })
  });
  const detailQuery = useQuery({
    queryKey: ["agent-detail", selectedAgentId],
    queryFn: () => getAgent(selectedAgentId ?? 0),
    enabled: Boolean(selectedAgentId)
  });
  const versionsQuery = useQuery({
    queryKey: ["workflow-versions", selectedAgentId],
    queryFn: () => listWorkflowVersions(selectedAgentId ?? 0, { page: 1, pageSize: 20 }),
    enabled: Boolean(selectedAgentId)
  });
  const createMutation = useMutation({
    mutationFn: createAgent,
    onSuccess: (result) => {
      message.success("Agent 已创建。");
      setFormMode(null);
      setSelectedAgentId(result.agentId);
      void queryClient.invalidateQueries({ queryKey: ["agents"] });
    }
  });
  const updateMutation = useMutation({
    mutationFn: ({ agentId, body }: { agentId: number; body: Schema["UpdateAgentRequest"] }) => updateAgent(agentId, body),
    onSuccess: () => {
      message.success("Agent 已更新。");
      setFormMode(null);
      void queryClient.invalidateQueries({ queryKey: ["agents"] });
      void queryClient.invalidateQueries({ queryKey: ["agent-detail", selectedAgentId] });
    }
  });
  const statusMutation = useMutation({
    mutationFn: ({ agentId, status }: { agentId: number; status: Schema["ChangeAgentStatusRequest"]["status"] }) => changeAgentStatus(agentId, status),
    onSuccess: () => {
      message.success("Agent 状态已更新。");
      void queryClient.invalidateQueries({ queryKey: ["agents"] });
      void queryClient.invalidateQueries({ queryKey: ["agent-detail", selectedAgentId] });
    }
  });

  useEffect(() => {
    if (formMode === "edit" && detailQuery.data) {
      form.setFieldsValue({
        name: detailQuery.data.name,
        description: detailQuery.data.description,
        systemPrompt: detailQuery.data.systemPrompt,
        defaultModel: detailQuery.data.defaultModel,
        temperature: detailQuery.data.temperature,
        timeoutSeconds: detailQuery.data.timeoutSeconds,
        maxSteps: detailQuery.data.maxSteps
      });
    }
  }, [detailQuery.data, form, formMode]);

  if (agentsQuery.isLoading || agentsQuery.isError) {
    return (
      <PageState
        title="Agent 管理"
        description="暂无 Agent。"
        loading={agentsQuery.isLoading}
        error={agentsQuery.error?.message}
      />
    );
  }

  return (
    <Space direction="vertical" size={16} style={{ width: "100%" }}>
      <section className="page-section page-title-row">
        <div>
          <Typography.Title level={3}>Agent 管理</Typography.Title>
          <Typography.Paragraph className="muted-text">
            Agent 详情展示当前草稿、当前发布版本和历史版本入口；运行语义以 WorkflowVersion 快照为准。
          </Typography.Paragraph>
        </div>
        <Button type="primary" onClick={() => openCreateForm()}>创建 Agent</Button>
      </section>
      <Card className="page-card">
        <Table<Schema["AgentListItemResult"]>
          rowKey={(record) => String(record.agentId)}
          dataSource={agentsQuery.data?.items ?? []}
          pagination={{
            current: page,
            pageSize: agentsQuery.data?.pageSize ?? 20,
            total: agentsQuery.data?.total ?? 0,
            onChange: setPage
          }}
          columns={[
            { title: "Agent Key", dataIndex: "agentKey" },
            { title: "名称", dataIndex: "name" },
            { title: "说明", dataIndex: "description" },
            { title: "状态", dataIndex: "status", render: (value) => <StatusTag status={String(value)} /> },
            { title: "草稿版本", dataIndex: "currentDraftWorkflowVersionId" },
            { title: "发布版本", dataIndex: "currentPublishedWorkflowVersionId" },
            {
              title: "操作",
              render: (_, record) => (
                <Space>
                  <Button type="link" onClick={() => record.agentId && setSelectedAgentId(record.agentId)}>详情</Button>
                  <Button type="link" onClick={() => record.agentId && navigate(`/agents/${record.agentId}/workflow`)}>
                    工作流
                  </Button>
                  <Button type="link" onClick={() => record.agentId && navigate(`/agents/${record.agentId}/debug`)}>
                    调试
                  </Button>
                  <Button
                    type="link"
                    onClick={() => record.agentId && statusMutation.mutate({
                      agentId: record.agentId,
                      status: record.status === "ENABLED" ? "DISABLED" : "ENABLED"
                    })}
                  >
                    {record.status === "ENABLED" ? "停用" : "启用"}
                  </Button>
                </Space>
              )
            }
          ]}
        />
      </Card>
      <Drawer
        title="Agent 详情"
        open={Boolean(selectedAgentId)}
        width={920}
        onClose={() => setSelectedAgentId(null)}
      >
        {detailQuery.isLoading || detailQuery.isError ? (
          <PageState
            title="Agent 详情"
            description="暂无 Agent 详情。"
            loading={detailQuery.isLoading}
            error={detailQuery.error?.message}
          />
        ) : (
          <AgentDetail
            detail={detailQuery.data}
            versions={versionsQuery.data?.items ?? []}
            versionsLoading={versionsQuery.isLoading}
            onEdit={() => setFormMode("edit")}
            onWorkflow={(agentId) => navigate(`/agents/${agentId}/workflow`)}
            onDebug={(agentId) => navigate(`/agents/${agentId}/debug`)}
            onOpenVersion={(agentId, workflowVersionId) => navigate(`/agents/${agentId}/workflow/versions/${workflowVersionId}`)}
          />
        )}
      </Drawer>
      <Modal
        title={formMode === "create" ? "创建 Agent" : "编辑 Agent"}
        open={Boolean(formMode)}
        width={720}
        onCancel={() => setFormMode(null)}
        confirmLoading={createMutation.isPending || updateMutation.isPending}
        onOk={() => {
          void form.validateFields().then((values) => submitForm(values));
        }}
      >
        <Form form={form} layout="vertical">
          {formMode === "create" ? (
            <Form.Item name="agentKey" label="Agent Key" rules={[{ required: true, message: "请输入 Agent Key。" }]}>
              <Input />
            </Form.Item>
          ) : null}
          <Form.Item name="name" label="名称" rules={[{ required: true, message: "请输入名称。" }]}>
            <Input />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item name="systemPrompt" label="系统提示词">
            <Input.TextArea rows={4} />
          </Form.Item>
          <Form.Item name="defaultModel" label="默认模型">
            <Input />
          </Form.Item>
          <Form.Item name="temperature" label="温度">
            <InputNumber min={0} max={2} step={0.1} style={{ width: "100%" }} />
          </Form.Item>
          <Form.Item name="timeoutSeconds" label="默认总超时（秒）">
            <InputNumber min={1} style={{ width: "100%" }} />
          </Form.Item>
          <Form.Item name="maxSteps" label="默认最大步数">
            <InputNumber min={1} style={{ width: "100%" }} />
          </Form.Item>
        </Form>
      </Modal>
    </Space>
  );

  /**
   * 打开创建表单。
   */
  function openCreateForm() {
    form.resetFields();
    form.setFieldsValue({
      defaultModel: "gpt-4.1-mini",
      temperature: 0.2,
      timeoutSeconds: 600,
      maxSteps: 30
    });
    setFormMode("create");
  }

  /**
   * 提交 Agent 表单。
   *
   * @param values 表单值
   */
  function submitForm(values: Schema["CreateAgentRequest"] & Schema["UpdateAgentRequest"]) {
    if (formMode === "create") {
      createMutation.mutate(values);
      return;
    }

    if (!selectedAgentId) {
      message.error("缺少 Agent 主键，无法更新。");
      return;
    }
    updateMutation.mutate({
      agentId: selectedAgentId,
      body: {
        name: values.name,
        description: values.description,
        systemPrompt: values.systemPrompt,
        defaultModel: values.defaultModel,
        temperature: values.temperature,
        timeoutSeconds: values.timeoutSeconds,
        maxSteps: values.maxSteps
      }
    });
  }
}

/**
 * Agent 详情组件。
 *
 * @param props 组件属性
 * @returns 详情展示
 */
function AgentDetail({
  detail,
  versions,
  versionsLoading,
  onEdit,
  onWorkflow,
  onDebug,
  onOpenVersion
}: {
  detail?: Schema["AgentDetailResult"];
  versions: Schema["WorkflowVersionListItemResult"][];
  versionsLoading: boolean;
  onEdit: () => void;
  onWorkflow: (agentId: number) => void;
  onDebug: (agentId: number) => void;
  onOpenVersion: (agentId: number, workflowVersionId: number) => void;
}) {
  const agentId = detail?.agentId;
  return (
    <Space direction="vertical" size={16} style={{ width: "100%" }}>
      <Space>
        <Button onClick={onEdit}>编辑基础信息</Button>
        <Button disabled={!agentId} onClick={() => agentId && onWorkflow(agentId)}>编辑工作流</Button>
        <Button disabled={!agentId} onClick={() => agentId && onDebug(agentId)}>调试</Button>
      </Space>
      <Descriptions bordered column={2} size="small">
        <Descriptions.Item label="Agent Key">{detail?.agentKey}</Descriptions.Item>
        <Descriptions.Item label="状态"><StatusTag status={detail?.status ?? "UNKNOWN"} /></Descriptions.Item>
        <Descriptions.Item label="名称">{detail?.name}</Descriptions.Item>
        <Descriptions.Item label="默认模型">{detail?.defaultModel}</Descriptions.Item>
        <Descriptions.Item label="温度">{detail?.temperature ?? "-"}</Descriptions.Item>
        <Descriptions.Item label="超时/步数">{detail?.timeoutSeconds} 秒 / {detail?.maxSteps} 步</Descriptions.Item>
        <Descriptions.Item label="当前草稿">{formatWorkflowSummary(detail?.currentDraftWorkflow)}</Descriptions.Item>
        <Descriptions.Item label="当前发布">{formatWorkflowSummary(detail?.currentPublishedWorkflow)}</Descriptions.Item>
        <Descriptions.Item label="历史摘要" span={2}>
          {detail?.historyVersionSummary?.total ?? 0} 个历史版本，最近版本：
          {detail?.historyVersionSummary?.latestWorkflowVersionId ?? "-"}
        </Descriptions.Item>
        <Descriptions.Item label="描述" span={2}>{detail?.description ?? "-"}</Descriptions.Item>
        <Descriptions.Item label="系统提示词" span={2}>{detail?.systemPrompt ?? "-"}</Descriptions.Item>
      </Descriptions>
      <Card size="small" title="工作流版本">
        <Table<Schema["WorkflowVersionListItemResult"]>
          rowKey={(record) => String(record.workflowVersionId)}
          loading={versionsLoading}
          dataSource={versions}
          pagination={false}
          columns={[
            { title: "版本 ID", dataIndex: "workflowVersionId" },
            { title: "版本号", dataIndex: "versionNo" },
            { title: "状态", dataIndex: "status", render: (value) => <StatusTag status={String(value)} /> },
            { title: "来源版本", dataIndex: "sourceWorkflowVersionId" },
            { title: "发布时间", dataIndex: "publishedAt" },
            {
              title: "操作",
              render: (_, record) => (
                <Button
                  type="link"
                  disabled={!agentId || !record.workflowVersionId}
                  onClick={() => agentId && record.workflowVersionId && onOpenVersion(agentId, record.workflowVersionId)}
                >
                  只读打开
                </Button>
              )
            }
          ]}
        />
      </Card>
    </Space>
  );
}

/**
 * 格式化工作流摘要。
 *
 * @param value 工作流摘要
 * @returns 展示文本
 */
function formatWorkflowSummary(value?: Schema["WorkflowVersionSummaryResult"]) {
  if (!value?.workflowVersionId) {
    return "-";
  }
  return `#${value.workflowVersionId} / v${value.versionNo ?? "-"} / ${value.status ?? "-"}`;
}
