import { useEffect, useMemo, useState } from "react";
import { App, Button, Card, Descriptions, Drawer, Form, Input, InputNumber, Modal, Select, Space, Table, Typography } from "antd";
import { useMutation, useQuery } from "@tanstack/react-query";
import {
  changeModelOfferingStatus,
  changeModelProviderStatus,
  createModelOffering,
  createModelProvider,
  getModelProvider,
  listModelOfferings,
  listModelProviders,
  testModelProvider,
  updateModelOffering,
  updateModelProvider,
  updateModelProviderSecrets,
  type Schema
} from "../../../api/domainApi";
import { queryClient } from "../../../app/queryClient";
import { JsonBlock } from "../../../shared/components/JsonBlock";
import { formatModelOfferingLabel } from "../../../shared/components/ModelOfferingSelect";
import { PageState } from "../../../shared/components/PageState";
import { StatusTag } from "../../../shared/components/StatusTag";

type ProviderFormMode = "create" | "edit";
type OfferingFormMode = "create" | "edit";
type OfferingStatusFilter = "ENABLED" | "DISABLED";

type ProviderFormValues = {
  providerKey?: string;
  name: string;
  baseUrl: string;
  description?: string;
  apiKey?: string;
};

type ProviderSecretFormValues = {
  apiKey?: string;
  clearApiKey?: "true";
};

type OfferingFormValues = {
  offeringKey?: string;
  modelKey: string;
  displayName: string;
  upstreamModelName: string;
  defaultTemperature?: number | null;
  description?: string;
};

type TestFormValues = {
  prompt?: string;
};

/**
 * 模型供应商与供应项管理页。
 */
export function ModelProvidersPage() {
  const { message } = App.useApp();
  const [page, setPage] = useState(1);
  const [selectedProviderId, setSelectedProviderId] = useState<number | null>(null);
  const [providerFormMode, setProviderFormMode] = useState<ProviderFormMode | null>(null);
  const [secretOpen, setSecretOpen] = useState(false);
  const [offeringFormMode, setOfferingFormMode] = useState<OfferingFormMode | null>(null);
  const [editingOffering, setEditingOffering] = useState<Schema["ModelOfferingDescriptor"] | null>(null);
  const [testTargetOffering, setTestTargetOffering] = useState<Schema["ModelOfferingDescriptor"] | null>(null);
  const [testResult, setTestResult] = useState<Schema["ModelProviderTestResult"] | null>(null);
  const [offeringStatusFilter, setOfferingStatusFilter] = useState<OfferingStatusFilter>("ENABLED");
  const [providerForm] = Form.useForm<ProviderFormValues>();
  const [secretForm] = Form.useForm<ProviderSecretFormValues>();
  const [offeringForm] = Form.useForm<OfferingFormValues>();
  const [testForm] = Form.useForm<TestFormValues>();

  const providersQuery = useQuery({
    queryKey: ["model-providers", page],
    queryFn: () => listModelProviders({ page, pageSize: 20 })
  });
  const providerDetailQuery = useQuery({
    queryKey: ["model-provider-detail", selectedProviderId],
    queryFn: () => getModelProvider(selectedProviderId ?? 0),
    enabled: Boolean(selectedProviderId)
  });
  const offeringsQuery = useQuery({
    queryKey: ["model-offerings", providerDetailQuery.data?.providerKey, offeringStatusFilter],
    queryFn: () => listModelOfferings({
      page: 1,
      pageSize: 50,
      providerKey: providerDetailQuery.data?.providerKey,
      status: offeringStatusFilter
    }),
    enabled: Boolean(providerDetailQuery.data?.providerKey)
  });

  const createProviderMutation = useMutation({
    mutationFn: createModelProvider,
    onSuccess: (result) => {
      message.success("模型供应商已创建。");
      setProviderFormMode(null);
      setSelectedProviderId(result.providerId ?? null);
      void queryClient.invalidateQueries({ queryKey: ["model-providers"] });
    }
  });
  const updateProviderMutation = useMutation({
    mutationFn: ({ providerId, body }: { providerId: number; body: Schema["UpdateModelProviderRequest"] }) => updateModelProvider(providerId, body),
    onSuccess: () => {
      message.success("模型供应商已更新。");
      setProviderFormMode(null);
      void queryClient.invalidateQueries({ queryKey: ["model-providers"] });
      void queryClient.invalidateQueries({ queryKey: ["model-provider-detail", selectedProviderId] });
    }
  });
  const changeProviderStatusMutation = useMutation({
    mutationFn: ({ providerId, status }: { providerId: number; status: Schema["ChangeModelProviderStatusRequest"]["status"] }) =>
      changeModelProviderStatus(providerId, status),
    onSuccess: () => {
      message.success("模型供应商状态已更新。");
      void queryClient.invalidateQueries({ queryKey: ["model-providers"] });
      void queryClient.invalidateQueries({ queryKey: ["model-provider-detail", selectedProviderId] });
      void queryClient.invalidateQueries({ queryKey: ["model-offerings"] });
    }
  });
  const updateSecretMutation = useMutation({
    mutationFn: ({ providerId, body }: { providerId: number; body: Schema["UpdateModelProviderSecretsRequest"] }) =>
      updateModelProviderSecrets(providerId, body),
    onSuccess: () => {
      message.success("模型供应商密钥已更新。");
      setSecretOpen(false);
      secretForm.resetFields();
      void queryClient.invalidateQueries({ queryKey: ["model-provider-detail", selectedProviderId] });
    }
  });
  const createOfferingMutation = useMutation({
    mutationFn: createModelOffering,
    onSuccess: () => {
      message.success("模型供应项已创建。");
      closeOfferingForm();
      void queryClient.invalidateQueries({ queryKey: ["model-offerings"] });
    }
  });
  const updateOfferingMutation = useMutation({
    mutationFn: ({ offeringId, body }: { offeringId: number; body: Schema["UpdateModelOfferingRequest"] }) =>
      updateModelOffering(offeringId, body),
    onSuccess: () => {
      message.success("模型供应项已更新。");
      closeOfferingForm();
      void queryClient.invalidateQueries({ queryKey: ["model-offerings"] });
    }
  });
  const changeOfferingStatusMutation = useMutation({
    mutationFn: ({ offeringId, status }: { offeringId: number; status: Schema["ChangeModelOfferingStatusRequest"]["status"] }) =>
      changeModelOfferingStatus(offeringId, status),
    onSuccess: () => {
      message.success("模型供应项状态已更新。");
      void queryClient.invalidateQueries({ queryKey: ["model-offerings"] });
    }
  });
  const testMutation = useMutation({
    mutationFn: ({ providerId, body }: { providerId: number; body: Schema["TestModelProviderRequest"] }) => testModelProvider(providerId, body),
    onSuccess: (result) => {
      setTestResult(result);
      message.success("连接测试已完成。");
    },
    onError: (error) => {
      setTestResult(null);
      message.error(error instanceof Error ? error.message : "连接测试失败。");
    }
  });

  useEffect(() => {
    if (providerFormMode !== "edit" || !providerDetailQuery.data) {
      return;
    }
    providerForm.setFieldsValue({
      name: providerDetailQuery.data.name ?? "",
      baseUrl: providerDetailQuery.data.baseUrl ?? "",
      description: providerDetailQuery.data.description
    });
  }, [providerDetailQuery.data, providerForm, providerFormMode]);

  useEffect(() => {
    if (offeringFormMode !== "edit" || !editingOffering) {
      return;
    }
    offeringForm.setFieldsValue({
      modelKey: editingOffering.modelKey ?? "",
      displayName: editingOffering.displayName ?? "",
      upstreamModelName: editingOffering.upstreamModelName ?? "",
      defaultTemperature: editingOffering.defaultTemperature,
      description: editingOffering.description
    });
  }, [editingOffering, offeringForm, offeringFormMode]);

  const currentProviderKey = providerDetailQuery.data?.providerKey;
  const offeringRecords = useMemo(
    () => offeringsQuery.data?.items ?? [],
    [offeringsQuery.data?.items]
  );

  if (providersQuery.isLoading || providersQuery.isError) {
    return (
      <PageState
        title="模型供应商"
        description="暂无模型供应商目录。"
        loading={providersQuery.isLoading}
        error={providersQuery.error?.message}
      />
    );
  }

  return (
    <Space direction="vertical" size={16} style={{ width: "100%" }}>
      <section className="page-section page-title-row">
        <div>
          <Typography.Title level={3}>模型供应商</Typography.Title>
          <Typography.Paragraph className="muted-text">
            供应商、供应项、密钥与连接测试统一走正式模型目录契约；页面不再接受自由模型名或旧 OpenAI 默认模型字段。
          </Typography.Paragraph>
        </div>
        <Button type="primary" onClick={openCreateProviderForm}>创建模型供应商</Button>
      </section>
      <Card className="page-card">
        <Table<Schema["ModelProviderResult"]>
          rowKey={(record) => String(record.providerId)}
          dataSource={providersQuery.data?.items ?? []}
          pagination={{
            current: page,
            pageSize: providersQuery.data?.pageSize ?? 20,
            total: providersQuery.data?.total ?? 0,
            onChange: setPage
          }}
          columns={[
            { title: "Provider Key", dataIndex: "providerKey" },
            { title: "名称", dataIndex: "name" },
            { title: "Base URL", dataIndex: "baseUrl" },
            { title: "密钥", render: (_, record) => record.apiKeyConfigured ? record.apiKeyMask ?? "已配置" : "未配置" },
            { title: "状态", dataIndex: "status", render: (value) => <StatusTag status={String(value)} /> },
            {
              title: "操作",
              render: (_, record) => (
                <Space>
                  <Button type="link" onClick={() => record.providerId && setSelectedProviderId(record.providerId)}>详情</Button>
                  <Button
                    type="link"
                    onClick={() => record.providerId && changeProviderStatusMutation.mutate({
                      providerId: record.providerId,
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
        title={providerDetailQuery.data ? `模型供应商：${providerDetailQuery.data.name}` : "模型供应商详情"}
        open={Boolean(selectedProviderId)}
        width={1080}
        onClose={() => setSelectedProviderId(null)}
      >
        {providerDetailQuery.isLoading || providerDetailQuery.isError ? (
          <PageState
            title="模型供应商详情"
            description="暂无模型供应商详情。"
            loading={providerDetailQuery.isLoading}
            error={providerDetailQuery.error?.message}
          />
        ) : (
          <Space direction="vertical" size={16} style={{ width: "100%" }}>
            <Space wrap>
              <Button onClick={() => setProviderFormMode("edit")}>编辑供应商</Button>
              <Button onClick={() => setSecretOpen(true)}>更新密钥</Button>
              <Button type="primary" onClick={openCreateOfferingForm}>创建模型供应项</Button>
            </Space>
            <Descriptions bordered column={2} size="small">
              <Descriptions.Item label="Provider Key">{providerDetailQuery.data?.providerKey}</Descriptions.Item>
              <Descriptions.Item label="状态"><StatusTag status={providerDetailQuery.data?.status ?? "UNKNOWN"} /></Descriptions.Item>
              <Descriptions.Item label="类型">{providerDetailQuery.data?.providerType ?? "-"}</Descriptions.Item>
              <Descriptions.Item label="密钥状态">
                {providerDetailQuery.data?.apiKeyConfigured ? providerDetailQuery.data.apiKeyMask ?? "已配置" : "未配置"}
              </Descriptions.Item>
              <Descriptions.Item label="Base URL" span={2}>{providerDetailQuery.data?.baseUrl ?? "-"}</Descriptions.Item>
              <Descriptions.Item label="描述" span={2}>{providerDetailQuery.data?.description ?? "-"}</Descriptions.Item>
            </Descriptions>
            <Card
              size="small"
              title="模型供应项"
              extra={(
                <Select<OfferingStatusFilter>
                  value={offeringStatusFilter}
                  onChange={setOfferingStatusFilter}
                  style={{ width: 140 }}
                  options={[
                    { value: "ENABLED", label: "启用中" },
                    { value: "DISABLED", label: "已停用" }
                  ]}
                />
              )}
            >
              <Table<Schema["ModelOfferingDescriptor"]>
                rowKey={(record) => String(record.offeringId)}
                loading={offeringsQuery.isLoading}
                dataSource={offeringRecords}
                pagination={false}
                columns={[
                  { title: "Offering Key", dataIndex: "offeringKey" },
                  { title: "显示名称", dataIndex: "displayName" },
                  { title: "模型标识", dataIndex: "modelKey" },
                  { title: "上游模型名", dataIndex: "upstreamModelName" },
                  { title: "默认温度", dataIndex: "defaultTemperature", render: (value) => value ?? "-" },
                  { title: "状态", dataIndex: "status", render: (value) => <StatusTag status={String(value)} /> },
                  {
                    title: "操作",
                    render: (_, record) => (
                      <Space>
                        <Button type="link" onClick={() => openEditOfferingForm(record)}>编辑</Button>
                        <Button type="link" onClick={() => openTestDialog(record)}>连接测试</Button>
                        <Button
                          type="link"
                          onClick={() => record.offeringId && changeOfferingStatusMutation.mutate({
                            offeringId: record.offeringId,
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
          </Space>
        )}
      </Drawer>
      <Modal
        title={providerFormMode === "create" ? "创建模型供应商" : "编辑模型供应商"}
        open={Boolean(providerFormMode)}
        width={720}
        onCancel={() => setProviderFormMode(null)}
        confirmLoading={createProviderMutation.isPending || updateProviderMutation.isPending}
        onOk={() => {
          void providerForm.validateFields().then((values) => submitProviderForm(values));
        }}
      >
        <Form form={providerForm} layout="vertical">
          {providerFormMode === "create" ? (
            <Form.Item name="providerKey" label="Provider Key" rules={[{ required: true, message: "请输入 Provider Key。" }]}>
              <Input />
            </Form.Item>
          ) : null}
          <Form.Item label="供应商类型">
            <Input value="OPENAI_COMPATIBLE" disabled />
          </Form.Item>
          <Form.Item name="name" label="名称" rules={[{ required: true, message: "请输入名称。" }]}>
            <Input />
          </Form.Item>
          <Form.Item name="baseUrl" label="Base URL" rules={[{ required: true, message: "请输入 Base URL。" }]}>
            <Input />
          </Form.Item>
          {providerFormMode === "create" ? (
            <Form.Item name="apiKey" label="初始 API Key">
              <Input.Password />
            </Form.Item>
          ) : null}
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={3} />
          </Form.Item>
        </Form>
      </Modal>
      <Modal
        title="更新模型供应商密钥"
        open={secretOpen}
        onCancel={() => setSecretOpen(false)}
        confirmLoading={updateSecretMutation.isPending}
        onOk={() => {
          void secretForm.validateFields().then((values) => submitSecretForm(values));
        }}
      >
        <Form form={secretForm} layout="vertical">
          <Form.Item name="apiKey" label="新 API Key">
            <Input.Password />
          </Form.Item>
          <Form.Item
            name="clearApiKey"
            label="显式清空"
            tooltip="选择后会清空当前密钥；不选且不输入新值时，不会覆盖已有密钥。"
          >
            <Select
              allowClear
              options={[
                { value: "true", label: "清空当前密钥" }
              ]}
            />
          </Form.Item>
        </Form>
      </Modal>
      <Modal
        title={offeringFormMode === "create" ? "创建模型供应项" : "编辑模型供应项"}
        open={Boolean(offeringFormMode)}
        width={720}
        onCancel={closeOfferingForm}
        confirmLoading={createOfferingMutation.isPending || updateOfferingMutation.isPending}
        onOk={() => {
          void offeringForm.validateFields().then((values) => submitOfferingForm(values));
        }}
      >
        <Form form={offeringForm} layout="vertical">
          {offeringFormMode === "create" ? (
            <Form.Item name="offeringKey" label="Offering Key" rules={[{ required: true, message: "请输入 Offering Key。" }]}>
              <Input />
            </Form.Item>
          ) : null}
          <Form.Item label="所属 Provider Key">
            <Input value={currentProviderKey ?? ""} disabled />
          </Form.Item>
          <Form.Item name="modelKey" label="模型标识" rules={[{ required: true, message: "请输入模型标识。" }]}>
            <Input />
          </Form.Item>
          <Form.Item name="displayName" label="显示名称" rules={[{ required: true, message: "请输入显示名称。" }]}>
            <Input />
          </Form.Item>
          <Form.Item name="upstreamModelName" label="上游模型名" rules={[{ required: true, message: "请输入上游模型名。" }]}>
            <Input />
          </Form.Item>
          <Form.Item name="defaultTemperature" label="默认温度">
            <InputNumber min={0} max={2} step={0.1} style={{ width: "100%" }} />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={3} />
          </Form.Item>
        </Form>
      </Modal>
      <Modal
        title="模型供应商连接测试"
        open={Boolean(testTargetOffering)}
        width={760}
        onCancel={closeTestDialog}
        confirmLoading={testMutation.isPending}
        onOk={() => {
          void testForm.validateFields().then((values) => submitTestForm(values));
        }}
      >
        <Descriptions bordered column={1} size="small" style={{ marginBottom: 16 }}>
          <Descriptions.Item label="目标供应项">
            {formatModelOfferingLabel(testTargetOffering ?? undefined)}
          </Descriptions.Item>
          <Descriptions.Item label="Offering Key">
            {testTargetOffering?.offeringKey ?? "-"}
          </Descriptions.Item>
        </Descriptions>
        <Form form={testForm} layout="vertical">
          <Form.Item name="prompt" label="测试 Prompt">
            <Input.TextArea rows={3} placeholder="留空时后端会使用 ping。" />
          </Form.Item>
        </Form>
        {testResult ? <JsonBlock title="测试结果" value={testResult} /> : null}
      </Modal>
    </Space>
  );

  function openCreateProviderForm() {
    providerForm.resetFields();
    providerForm.setFieldsValue({
      baseUrl: "https://api.openai.com",
      name: "OpenAI Compatible"
    });
    setProviderFormMode("create");
  }

  function submitProviderForm(values: ProviderFormValues) {
    if (providerFormMode === "create") {
      createProviderMutation.mutate({
        providerKey: values.providerKey ?? "",
        providerType: "OPENAI_COMPATIBLE",
        name: values.name,
        baseUrl: values.baseUrl,
        apiKey: values.apiKey,
        description: values.description
      });
      return;
    }
    if (!selectedProviderId) {
      message.error("缺少模型供应商主键，无法更新。");
      return;
    }
    updateProviderMutation.mutate({
      providerId: selectedProviderId,
      body: {
        name: values.name,
        baseUrl: values.baseUrl,
        description: values.description
      }
    });
  }

  function submitSecretForm(values: ProviderSecretFormValues) {
    if (!selectedProviderId) {
      message.error("缺少模型供应商主键，无法更新密钥。");
      return;
    }
    if (!values.apiKey?.trim() && values.clearApiKey !== "true") {
      message.error("请输入新密钥，或显式选择清空当前密钥。");
      return;
    }
    updateSecretMutation.mutate({
      providerId: selectedProviderId,
      body: {
        apiKey: values.apiKey?.trim() || undefined,
        clearApiKey: values.clearApiKey === "true"
      }
    });
  }

  function openCreateOfferingForm() {
    if (!currentProviderKey) {
      message.error("缺少 Provider Key，无法创建模型供应项。");
      return;
    }
    setEditingOffering(null);
    offeringForm.resetFields();
    setOfferingFormMode("create");
  }

  function openEditOfferingForm(record: Schema["ModelOfferingDescriptor"]) {
    setEditingOffering(record);
    setOfferingFormMode("edit");
  }

  function closeOfferingForm() {
    setOfferingFormMode(null);
    setEditingOffering(null);
    offeringForm.resetFields();
  }

  function submitOfferingForm(values: OfferingFormValues) {
    if (!currentProviderKey) {
      message.error("缺少 Provider Key，无法提交模型供应项。");
      return;
    }
    const body = {
      providerKey: currentProviderKey,
      modelKey: values.modelKey,
      displayName: values.displayName,
      upstreamModelName: values.upstreamModelName,
      defaultTemperature: values.defaultTemperature ?? undefined,
      description: values.description
    };
    if (offeringFormMode === "create") {
      createOfferingMutation.mutate({
        offeringKey: values.offeringKey ?? "",
        ...body
      });
      return;
    }
    if (!editingOffering?.offeringId) {
      message.error("缺少模型供应项主键，无法更新。");
      return;
    }
    updateOfferingMutation.mutate({
      offeringId: editingOffering.offeringId,
      body
    });
  }

  function openTestDialog(record: Schema["ModelOfferingDescriptor"]) {
    setTestTargetOffering(record);
    setTestResult(null);
    testForm.resetFields();
  }

  function closeTestDialog() {
    setTestTargetOffering(null);
    setTestResult(null);
    testForm.resetFields();
  }

  function submitTestForm(values: TestFormValues) {
    if (!selectedProviderId || !testTargetOffering?.offeringKey) {
      message.error("缺少测试目标，无法发起连接测试。");
      return;
    }
    testMutation.mutate({
      providerId: selectedProviderId,
      body: {
        offeringKey: testTargetOffering.offeringKey,
        prompt: values.prompt?.trim() || undefined
      }
    });
  }
}
