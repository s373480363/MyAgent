import { useEffect, useState } from "react";
import { App, Button, Card, Checkbox, Descriptions, Drawer, Form, Input, InputNumber, Modal, Select, Space, Table, Typography } from "antd";
import { useMutation, useQuery } from "@tanstack/react-query";
import {
  changeExternalAgentStatus,
  createExternalAgent,
  getExternalAgent,
  listExternalAgents,
  testExternalAgent,
  updateExternalAgent,
  updateExternalAgentSecrets,
  type Schema
} from "../../../api/domainApi";
import { queryClient } from "../../../app/queryClient";
import { JsonBlock, parseJsonText, stringifyJson } from "../../../shared/components/JsonBlock";
import { JsonTextArea } from "../../../shared/components/JsonTextArea";
import { PageState } from "../../../shared/components/PageState";
import { StatusTag } from "../../../shared/components/StatusTag";

type ExternalAgentFormMode = "create" | "edit";

type ExternalAgentFormValues = {
  adapterKey?: string;
  adapterType?: Schema["CreateExternalAgentRequest"]["adapterType"];
  name: string;
  description?: string;
  commandJsonText: string;
  secretHeadersText?: string;
  workingDirectory?: string;
  timeoutSeconds?: number;
  captureStdout?: boolean;
  captureStderr?: boolean;
  captureGitDiff?: boolean;
  outputSchemaId?: number;
};

type SecretFormValues = {
  itemsText?: string;
  clearHeaderNames?: string;
};

type TestFormValues = {
  prompt?: string;
  inputText?: string;
};

/**
 * 外部 Agent 页面。
 */
export function ExternalAgentsPage() {
  const { message } = App.useApp();
  const [page, setPage] = useState(1);
  const [selectedAdapterId, setSelectedAdapterId] = useState<number | null>(null);
  const [formMode, setFormMode] = useState<ExternalAgentFormMode | null>(null);
  const [secretsOpen, setSecretsOpen] = useState(false);
  const [testOpen, setTestOpen] = useState(false);
  const [testResult, setTestResult] = useState<Schema["ExternalAgentTestResult"] | null>(null);
  const [form] = Form.useForm<ExternalAgentFormValues>();
  const [secretForm] = Form.useForm<SecretFormValues>();
  const [testForm] = Form.useForm<TestFormValues>();
  const externalAgentsQuery = useQuery({
    queryKey: ["external-agents", page],
    queryFn: () => listExternalAgents({ page, pageSize: 20 })
  });
  const detailQuery = useQuery({
    queryKey: ["external-agent-detail", selectedAdapterId],
    queryFn: () => getExternalAgent(selectedAdapterId ?? 0),
    enabled: Boolean(selectedAdapterId)
  });
  const createMutation = useMutation({
    mutationFn: createExternalAgent,
    onSuccess: (result) => {
      message.success("外部 Agent 已创建。");
      setFormMode(null);
      setSelectedAdapterId(result.id ?? null);
      void queryClient.invalidateQueries({ queryKey: ["external-agents"] });
    }
  });
  const updateMutation = useMutation({
    mutationFn: ({ adapterId, body }: { adapterId: number; body: Schema["UpdateExternalAgentRequest"] }) => updateExternalAgent(adapterId, body),
    onSuccess: () => {
      message.success("外部 Agent 已更新。");
      setFormMode(null);
      void queryClient.invalidateQueries({ queryKey: ["external-agents"] });
      void queryClient.invalidateQueries({ queryKey: ["external-agent-detail", selectedAdapterId] });
    }
  });
  const statusMutation = useMutation({
    mutationFn: ({ adapterId, status }: { adapterId: number; status: Schema["ChangeExternalAgentStatusRequest"]["status"] }) => changeExternalAgentStatus(adapterId, status),
    onSuccess: () => {
      message.success("外部 Agent 状态已更新。");
      void queryClient.invalidateQueries({ queryKey: ["external-agents"] });
      void queryClient.invalidateQueries({ queryKey: ["external-agent-detail", selectedAdapterId] });
    }
  });
  const secretsMutation = useMutation({
    mutationFn: ({ adapterId, body }: { adapterId: number; body: Schema["UpdateExternalAgentSecretsRequest"] }) => updateExternalAgentSecrets(adapterId, body),
    onSuccess: () => {
      message.success("敏感 Header 已更新。");
      setSecretsOpen(false);
      secretForm.resetFields();
      void queryClient.invalidateQueries({ queryKey: ["external-agent-detail", selectedAdapterId] });
    }
  });
  const testMutation = useMutation({
    mutationFn: ({ adapterId, body }: { adapterId: number; body: Schema["TestExternalAgentRequest"] }) => testExternalAgent(adapterId, body),
    onSuccess: (result) => {
      setTestResult(result);
      message.success("外部 Agent 测试已完成。");
    }
  });

  useEffect(() => {
    if (formMode === "edit" && detailQuery.data) {
      form.setFieldsValue({
        adapterType: detailQuery.data.adapterType,
        name: detailQuery.data.name ?? "",
        description: detailQuery.data.description,
        commandJsonText: stringifyJson(detailQuery.data.commandJson ?? {}),
        workingDirectory: detailQuery.data.workingDirectory,
        timeoutSeconds: detailQuery.data.timeoutSeconds,
        captureStdout: detailQuery.data.captureStdout,
        captureStderr: detailQuery.data.captureStderr,
        captureGitDiff: detailQuery.data.captureGitDiff,
        outputSchemaId: detailQuery.data.outputSchemaId
      });
    }
  }, [detailQuery.data, form, formMode]);

  if (externalAgentsQuery.isLoading || externalAgentsQuery.isError) {
    return (
      <PageState
        title="外部 Agent"
        description="暂无外部 Agent。"
        loading={externalAgentsQuery.isLoading}
        error={externalAgentsQuery.error?.message}
      />
    );
  }

  return (
    <Space direction="vertical" size={16} style={{ width: "100%" }}>
      <section className="page-section page-title-row">
        <div>
          <Typography.Title level={3}>外部 Agent</Typography.Title>
          <Typography.Paragraph className="muted-text">
            普通配置与敏感 Header 分开维护；详情只展示 secretConfigured，不回显密钥或掩码。
          </Typography.Paragraph>
        </div>
        <Button type="primary" onClick={() => openCreateForm()}>创建外部 Agent</Button>
      </section>
      <Card className="page-card">
        <Table<Schema["ExternalAgentListItemResult"]>
          rowKey={(record) => String(record.id)}
          dataSource={externalAgentsQuery.data?.items ?? []}
          pagination={{
            current: page,
            pageSize: externalAgentsQuery.data?.pageSize ?? 20,
            total: externalAgentsQuery.data?.total ?? 0,
            onChange: setPage
          }}
          columns={[
            { title: "Adapter Key", dataIndex: "adapterKey" },
            { title: "名称", dataIndex: "name" },
            { title: "类型", dataIndex: "adapterType", render: (value) => <StatusTag status={String(value)} /> },
            { title: "超时(s)", dataIndex: "timeoutSeconds" },
            { title: "输出 Schema", dataIndex: "outputSchemaId" },
            { title: "状态", dataIndex: "status", render: (value) => <StatusTag status={String(value)} /> },
            {
              title: "操作",
              render: (_, record) => (
                <Space>
                  <Button type="link" onClick={() => record.id && setSelectedAdapterId(record.id)}>详情</Button>
                  <Button
                    type="link"
                    onClick={() => record.id && statusMutation.mutate({
                      adapterId: record.id,
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
        title={detailQuery.data ? `外部 Agent：${detailQuery.data.name}` : "外部 Agent 详情"}
        open={Boolean(selectedAdapterId)}
        width={920}
        onClose={() => setSelectedAdapterId(null)}
      >
        {detailQuery.isLoading || detailQuery.isError ? (
          <PageState
            title="外部 Agent 详情"
            description="暂无外部 Agent 详情。"
            loading={detailQuery.isLoading}
            error={detailQuery.error?.message}
          />
        ) : (
          <ExternalAgentDetail
            detail={detailQuery.data}
            onEdit={() => setFormMode("edit")}
            onSecrets={() => setSecretsOpen(true)}
            onTest={() => {
              testForm.resetFields();
              setTestResult(null);
              setTestOpen(true);
            }}
          />
        )}
      </Drawer>
      <Modal
        title={formMode === "create" ? "创建外部 Agent" : "编辑外部 Agent"}
        open={Boolean(formMode)}
        width={760}
        onCancel={() => setFormMode(null)}
        confirmLoading={createMutation.isPending || updateMutation.isPending}
        onOk={() => {
          void form.validateFields().then((values) => submitForm(values));
        }}
      >
        <Form form={form} layout="vertical">
          {formMode === "create" ? (
            <Form.Item name="adapterKey" label="Adapter Key" rules={[{ required: true, message: "请输入 Adapter Key。" }]}>
              <Input />
            </Form.Item>
          ) : null}
          <Form.Item name="adapterType" label="类型" rules={[{ required: true, message: "请选择类型。" }]}>
            <Select
              options={[
                { value: "CODEX_CLI", label: "Codex CLI" },
                { value: "OPENCODE_CLI", label: "OpenCode CLI" },
                { value: "CUSTOM_CLI", label: "自定义 CLI" },
                { value: "CUSTOM_HTTP", label: "自定义 HTTP" }
              ]}
              disabled={formMode === "edit"}
            />
          </Form.Item>
          <Form.Item name="name" label="名称" rules={[{ required: true, message: "请输入名称。" }]}>
            <Input />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item name="commandJsonText" label="命令或 HTTP 配置 JSON" rules={[{ required: true, message: "请输入配置 JSON。" }]}>
            <JsonTextArea rows={10} />
          </Form.Item>
          {formMode === "create" ? (
            <Form.Item name="secretHeadersText" label="创建时写入的敏感 Header JSON 数组">
              <JsonTextArea rows={5} placeholder={'[{"headerName":"Authorization","secretValue":"Bearer ..."}]'} />
            </Form.Item>
          ) : null}
          <Form.Item name="workingDirectory" label="工作目录">
            <Input />
          </Form.Item>
          <Form.Item name="timeoutSeconds" label="超时（秒）">
            <InputNumber min={1} style={{ width: "100%" }} />
          </Form.Item>
          <Form.Item name="outputSchemaId" label="输出 Schema ID">
            <InputNumber min={1} style={{ width: "100%" }} />
          </Form.Item>
          <Space>
            <Form.Item name="captureStdout" valuePropName="checked">
              <Checkbox>采集 stdout</Checkbox>
            </Form.Item>
            <Form.Item name="captureStderr" valuePropName="checked">
              <Checkbox>采集 stderr</Checkbox>
            </Form.Item>
            <Form.Item name="captureGitDiff" valuePropName="checked">
              <Checkbox>采集 Git diff</Checkbox>
            </Form.Item>
          </Space>
        </Form>
      </Modal>
      <Modal
        title="更新敏感 Header"
        open={secretsOpen}
        width={720}
        onCancel={() => setSecretsOpen(false)}
        confirmLoading={secretsMutation.isPending}
        onOk={() => {
          void secretForm.validateFields().then((values) => submitSecrets(values));
        }}
      >
        <Form form={secretForm} layout="vertical">
          <Form.Item name="itemsText" label="覆盖写入项 JSON 数组">
            <JsonTextArea rows={6} placeholder={'[{"headerName":"Authorization","secretValue":"Bearer ..."}]'} />
          </Form.Item>
          <Form.Item name="clearHeaderNames" label="显式清空 Header 名称（逗号分隔）">
            <Input placeholder="Authorization,X-Token" />
          </Form.Item>
        </Form>
      </Modal>
      <Modal
        title="连接测试"
        open={testOpen}
        width={820}
        onCancel={() => setTestOpen(false)}
        confirmLoading={testMutation.isPending}
        onOk={() => {
          void testForm.validateFields().then((values) => submitTest(values));
        }}
      >
        <Form form={testForm} layout="vertical">
          <Form.Item name="prompt" label="测试 Prompt">
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item name="inputText" label="测试输入 JSON">
            <JsonTextArea rows={6} />
          </Form.Item>
        </Form>
        {testResult ? <JsonBlock title="测试结果" value={testResult} /> : null}
      </Modal>
    </Space>
  );

  /**
   * 打开创建表单。
   */
  function openCreateForm() {
    form.resetFields();
    form.setFieldsValue({
      adapterType: "CUSTOM_HTTP",
      commandJsonText: stringifyJson({ method: "POST", url: "https://example.com", headers: [], bodyTemplate: {}, resultSource: "$.outputJson" }),
      timeoutSeconds: 120,
      captureStdout: false,
      captureStderr: false,
      captureGitDiff: false
    });
    setFormMode("create");
  }

  /**
   * 提交普通配置表单。
   *
   * @param values 表单值
   */
  function submitForm(values: ExternalAgentFormValues) {
    try {
      const commandJson = parseJsonText(values.commandJsonText);
      if (formMode === "create") {
        createMutation.mutate({
          adapterKey: values.adapterKey ?? "",
          adapterType: values.adapterType ?? "CUSTOM_HTTP",
          name: values.name,
          description: values.description,
          commandJson,
          secretHeaders: parseOptionalArray<Schema["ExternalAgentSecretHeaderRequest"]>(values.secretHeadersText),
          workingDirectory: values.workingDirectory,
          timeoutSeconds: values.timeoutSeconds,
          captureStdout: values.captureStdout,
          captureStderr: values.captureStderr,
          captureGitDiff: values.captureGitDiff,
          outputSchemaId: values.outputSchemaId
        });
        return;
      }

      if (!selectedAdapterId) {
        message.error("缺少外部 Agent 主键，无法更新。");
        return;
      }
      updateMutation.mutate({
        adapterId: selectedAdapterId,
        body: {
          name: values.name,
          description: values.description,
          commandJson,
          workingDirectory: values.workingDirectory,
          timeoutSeconds: values.timeoutSeconds,
          captureStdout: values.captureStdout,
          captureStderr: values.captureStderr,
          captureGitDiff: values.captureGitDiff,
          outputSchemaId: values.outputSchemaId
        }
      });
    } catch {
      message.error("配置 JSON 格式不正确。");
    }
  }

  /**
   * 提交敏感 Header 更新。
   *
   * @param values 表单值
   */
  function submitSecrets(values: SecretFormValues) {
    if (!selectedAdapterId) {
      message.error("缺少外部 Agent 主键，无法更新密钥。");
      return;
    }
    try {
      secretsMutation.mutate({
        adapterId: selectedAdapterId,
        body: {
          items: parseOptionalArray<Schema["ExternalAgentSecretHeaderRequest"]>(values.itemsText),
          clearHeaderNames: values.clearHeaderNames?.split(",").map((item) => item.trim()).filter(Boolean)
        }
      });
    } catch {
      message.error("敏感 Header JSON 格式不正确。");
    }
  }

  /**
   * 提交测试请求。
   *
   * @param values 表单值
   */
  function submitTest(values: TestFormValues) {
    if (!selectedAdapterId) {
      message.error("缺少外部 Agent 主键，无法测试。");
      return;
    }
    try {
      testMutation.mutate({
        adapterId: selectedAdapterId,
        body: {
          prompt: values.prompt,
          input: values.inputText ? parseJsonText(values.inputText) : undefined
        }
      });
    } catch {
      message.error("测试输入 JSON 格式不正确。");
    }
  }
}

/**
 * 外部 Agent 详情。
 *
 * @param props 组件属性
 * @returns 详情区域
 */
function ExternalAgentDetail({
  detail,
  onEdit,
  onSecrets,
  onTest
}: {
  detail?: Schema["ExternalAgentDetailResult"];
  onEdit: () => void;
  onSecrets: () => void;
  onTest: () => void;
}) {
  return (
    <Space direction="vertical" size={16} style={{ width: "100%" }}>
      <Space>
        <Button onClick={onEdit}>编辑普通配置</Button>
        <Button onClick={onSecrets}>更新密钥</Button>
        <Button type="primary" onClick={onTest}>连接测试</Button>
      </Space>
      <Descriptions bordered column={2} size="small">
        <Descriptions.Item label="Adapter Key">{detail?.adapterKey}</Descriptions.Item>
        <Descriptions.Item label="状态"><StatusTag status={detail?.status ?? "UNKNOWN"} /></Descriptions.Item>
        <Descriptions.Item label="类型"><StatusTag status={detail?.adapterType ?? "UNKNOWN"} /></Descriptions.Item>
        <Descriptions.Item label="超时">{detail?.timeoutSeconds ?? "-"} 秒</Descriptions.Item>
        <Descriptions.Item label="工作目录" span={2}>{detail?.workingDirectory ?? "-"}</Descriptions.Item>
        <Descriptions.Item label="输出 Schema" span={2}>{detail?.outputSchemaId ?? "-"}</Descriptions.Item>
        <Descriptions.Item label="描述" span={2}>{detail?.description ?? "-"}</Descriptions.Item>
      </Descriptions>
      <Card size="small" title="敏感 Header 配置状态">
        <Table<Schema["ExternalAgentSecretHeaderResult"]>
          rowKey={(record) => record.headerName ?? ""}
          dataSource={detail?.secretHeaders ?? []}
          pagination={false}
          columns={[
            { title: "Header", dataIndex: "headerName" },
            { title: "是否已配置密钥", dataIndex: "secretConfigured", render: (value) => (value ? "已配置" : "未配置") }
          ]}
        />
      </Card>
      <JsonBlock title="命令或 HTTP 配置" value={detail?.commandJson} />
    </Space>
  );
}

/**
 * 解析可选数组 JSON。
 *
 * @param text JSON 文本
 * @returns 数组或 undefined
 */
function parseOptionalArray<T>(text?: string) {
  if (!text?.trim()) {
    return undefined;
  }
  const value = parseJsonText(text);
  if (!Array.isArray(value)) {
    throw new Error("必须是数组。");
  }
  return value as T[];
}
