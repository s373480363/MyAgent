import { useEffect, useState } from "react";
import { App, Button, Card, Descriptions, Drawer, Form, Input, Modal, Select, Space, Table, Typography } from "antd";
import { useMutation, useQuery } from "@tanstack/react-query";
import {
  createSchema,
  createSchemaVersion,
  getSchema,
  listSchemas,
  updateSchema,
  type Schema
} from "../../../api/domainApi";
import { queryClient } from "../../../app/queryClient";
import { JsonBlock, parseJsonText, stringifyJson } from "../../../shared/components/JsonBlock";
import { JsonTextArea } from "../../../shared/components/JsonTextArea";
import { PageState } from "../../../shared/components/PageState";
import { StatusTag } from "../../../shared/components/StatusTag";

type SchemaFormMode = "create" | "edit" | "version";

type SchemaFormValues = {
  schemaKey?: string;
  name: string;
  description?: string;
  javaType?: string;
  createdFrom?: Schema["CreateSchemaRequest"]["createdFrom"];
  jsonSchemaText: string;
};

/**
 * Schema 管理页面。
 */
export function SchemasPage() {
  const { message } = App.useApp();
  const [page, setPage] = useState(1);
  const [selectedSchemaId, setSelectedSchemaId] = useState<number | null>(null);
  const [formMode, setFormMode] = useState<SchemaFormMode | null>(null);
  const [form] = Form.useForm<SchemaFormValues>();
  const schemasQuery = useQuery({
    queryKey: ["schemas", page],
    queryFn: () => listSchemas({ page, pageSize: 20 })
  });
  const detailQuery = useQuery({
    queryKey: ["schema-detail", selectedSchemaId],
    queryFn: () => getSchema(selectedSchemaId ?? 0),
    enabled: Boolean(selectedSchemaId)
  });
  const createMutation = useMutation({
    mutationFn: createSchema,
    onSuccess: (result) => {
      message.success("Schema 已创建。");
      setFormMode(null);
      setSelectedSchemaId(result.id);
      void queryClient.invalidateQueries({ queryKey: ["schemas"] });
    }
  });
  const updateMutation = useMutation({
    mutationFn: ({ schemaId, body }: { schemaId: number; body: Schema["UpdateSchemaDraftRequest"] }) => updateSchema(schemaId, body),
    onSuccess: () => {
      message.success("Schema 草稿已更新。");
      setFormMode(null);
      void queryClient.invalidateQueries({ queryKey: ["schemas"] });
      void queryClient.invalidateQueries({ queryKey: ["schema-detail", selectedSchemaId] });
    }
  });
  const versionMutation = useMutation({
    mutationFn: ({ schemaId, body }: { schemaId: number; body: Schema["CreateSchemaVersionRequest"] }) => createSchemaVersion(schemaId, body),
    onSuccess: (result) => {
      message.success("Schema 新版本已创建。");
      setFormMode(null);
      setSelectedSchemaId(result.id);
      void queryClient.invalidateQueries({ queryKey: ["schemas"] });
    }
  });

  if (schemasQuery.isLoading || schemasQuery.isError) {
    return (
      <PageState
        title="Schema 管理"
        description="暂无 Schema。"
        loading={schemasQuery.isLoading}
        error={schemasQuery.error?.message}
      />
    );
  }

  return (
    <Space direction="vertical" size={16} style={{ width: "100%" }}>
      <section className="page-section page-title-row">
        <div>
          <Typography.Title level={3}>Schema 管理</Typography.Title>
          <Typography.Paragraph className="muted-text">
            DRAFT 且未锁定的 Schema 可原地编辑；正式演进通过“创建新版本”保留版本链路。
          </Typography.Paragraph>
        </div>
        <Button type="primary" onClick={() => openCreateForm()}>创建 Schema</Button>
      </section>
      <Card className="page-card">
        <Table<Schema["SchemaListItem"]>
          rowKey={(record) => String(record.id)}
          dataSource={schemasQuery.data?.items ?? []}
          pagination={{
            current: page,
            pageSize: schemasQuery.data?.pageSize ?? 20,
            total: schemasQuery.data?.total ?? 0,
            onChange: setPage
          }}
          columns={[
            { title: "Schema Key", dataIndex: "schemaKey" },
            { title: "版本", dataIndex: "version" },
            { title: "名称", dataIndex: "name" },
            { title: "来源", dataIndex: "createdFrom", render: (value) => <StatusTag status={String(value)} /> },
            { title: "状态", dataIndex: "status", render: (value) => <StatusTag status={String(value)} /> },
            { title: "锁定", dataIndex: "locked", render: (value) => (value ? "是" : "否") },
            { title: "更新时间", dataIndex: "updatedAt" },
            {
              title: "操作",
              render: (_, record) => (
                <Space>
                  <Button type="link" onClick={() => setSelectedSchemaId(record.id)}>详情</Button>
                  <Button
                    type="link"
                    disabled={record.status !== "DRAFT" || record.locked}
                    onClick={() => openEditForm(record.id)}
                  >
                    编辑
                  </Button>
                  <Button type="link" onClick={() => openVersionForm(record.id)}>创建新版本</Button>
                </Space>
              )
            }
          ]}
        />
      </Card>
      <Drawer
        title={detailQuery.data ? `${detailQuery.data.schemaKey} v${detailQuery.data.version}` : "Schema 详情"}
        open={Boolean(selectedSchemaId)}
        width={860}
        onClose={() => setSelectedSchemaId(null)}
      >
        {detailQuery.isLoading || detailQuery.isError ? (
          <PageState
            title="Schema 详情"
            description="暂无 Schema 详情。"
            loading={detailQuery.isLoading}
            error={detailQuery.error?.message}
          />
        ) : (
          <SchemaDetail detail={detailQuery.data} onEdit={() => openEditForm(detailQuery.data?.id)} onCreateVersion={() => openVersionForm(detailQuery.data?.id)} />
        )}
      </Drawer>
      <Modal
        title={resolveFormTitle(formMode)}
        open={Boolean(formMode)}
        width={760}
        onCancel={() => setFormMode(null)}
        confirmLoading={createMutation.isPending || updateMutation.isPending || versionMutation.isPending}
        onOk={() => {
          void form.validateFields().then((values) => submitForm(values));
        }}
      >
        <Form form={form} layout="vertical">
          {formMode === "create" ? (
            <Form.Item name="schemaKey" label="Schema Key" rules={[{ required: true, message: "请输入 Schema Key。" }]}>
              <Input />
            </Form.Item>
          ) : null}
          <Form.Item name="name" label="名称" rules={[{ required: true, message: "请输入名称。" }]}>
            <Input />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item name="javaType" label="Java 类型">
            <Input />
          </Form.Item>
          {formMode === "create" ? (
            <Form.Item name="createdFrom" label="来源" rules={[{ required: true, message: "请选择来源。" }]}>
              <Select
                options={[
                  { value: "USER_CREATED", label: "用户创建" },
                  { value: "JAVA_METHOD_SCAN", label: "Java 方法扫描" },
                  { value: "TOOL_DEFINITION", label: "工具定义" },
                  { value: "SYSTEM_BUILTIN", label: "系统内置" },
                  { value: "AGENT_INPUT", label: "Agent 输入" },
                  { value: "AGENT_OUTPUT", label: "Agent 输出" }
                ]}
              />
            </Form.Item>
          ) : null}
          <Form.Item name="jsonSchemaText" label="JSON Schema" rules={[{ required: true, message: "请输入 JSON Schema。" }]}>
            <JsonTextArea rows={12} />
          </Form.Item>
        </Form>
      </Modal>
    </Space>
  );

  /**
   * 打开创建表单。
   */
  function openCreateForm() {
    form.setFieldsValue({
      createdFrom: "USER_CREATED",
      jsonSchemaText: stringifyJson({ type: "object", properties: {}, additionalProperties: true })
    });
    setFormMode("create");
  }

  /**
   * 打开编辑表单。
   *
   * @param schemaId Schema 主键
   */
  function openEditForm(schemaId?: number) {
    if (!schemaId) {
      return;
    }
    setSelectedSchemaId(schemaId);
    const source = detailQuery.data?.id === schemaId ? detailQuery.data : schemasQuery.data?.items.find((item) => item.id === schemaId);
    form.setFieldsValue({
      name: source?.name ?? "",
      description: source?.description,
      javaType: source?.javaType,
      jsonSchemaText: stringifyJson(source?.jsonSchema ?? { type: "object", properties: {} })
    });
    setFormMode("edit");
  }

  /**
   * 打开新版本表单。
   *
   * @param schemaId Schema 主键
   */
  function openVersionForm(schemaId?: number) {
    if (!schemaId) {
      return;
    }
    setSelectedSchemaId(schemaId);
    const source = detailQuery.data?.id === schemaId ? detailQuery.data : schemasQuery.data?.items.find((item) => item.id === schemaId);
    form.setFieldsValue({
      name: source?.name ?? "",
      description: source?.description,
      javaType: source?.javaType,
      jsonSchemaText: stringifyJson(source?.jsonSchema ?? { type: "object", properties: {} })
    });
    setFormMode("version");
  }

  /**
   * 提交 Schema 表单。
   *
   * @param values 表单值
   */
  function submitForm(values: SchemaFormValues) {
    try {
      const jsonSchema = parseJsonText(values.jsonSchemaText);
      if (formMode === "create") {
        createMutation.mutate({
          schemaKey: values.schemaKey ?? "",
          name: values.name,
          description: values.description,
          javaType: values.javaType,
          createdFrom: values.createdFrom ?? "USER_CREATED",
          jsonSchema
        });
        return;
      }

      if (!selectedSchemaId) {
        message.error("缺少 Schema 主键，无法提交。");
        return;
      }

      const body = {
        name: values.name,
        description: values.description,
        javaType: values.javaType,
        jsonSchema
      };
      if (formMode === "edit") {
        updateMutation.mutate({ schemaId: selectedSchemaId, body });
      } else if (formMode === "version") {
        versionMutation.mutate({ schemaId: selectedSchemaId, body });
      }
    } catch {
      message.error("JSON Schema 格式不正确。");
    }
  }
}

/**
 * Schema 详情。
 *
 * @param props 组件属性
 * @returns 详情区域
 */
function SchemaDetail({
  detail,
  onEdit,
  onCreateVersion
}: {
  detail?: Schema["SchemaDetail"];
  onEdit: () => void;
  onCreateVersion: () => void;
}) {
  return (
    <Space direction="vertical" size={16} style={{ width: "100%" }}>
      <Space>
        <Button disabled={detail?.status !== "DRAFT" || detail?.locked} onClick={onEdit}>编辑 DRAFT</Button>
        <Button onClick={onCreateVersion}>基于该版本创建新版本</Button>
      </Space>
      <Descriptions bordered column={2} size="small">
        <Descriptions.Item label="Schema Key">{detail?.schemaKey}</Descriptions.Item>
        <Descriptions.Item label="版本">{detail?.version}</Descriptions.Item>
        <Descriptions.Item label="名称">{detail?.name}</Descriptions.Item>
        <Descriptions.Item label="状态"><StatusTag status={detail?.status ?? "UNKNOWN"} /></Descriptions.Item>
        <Descriptions.Item label="来源"><StatusTag status={detail?.createdFrom ?? "UNKNOWN"} /></Descriptions.Item>
        <Descriptions.Item label="锁定">{detail?.locked ? "是" : "否"}</Descriptions.Item>
        <Descriptions.Item label="Java 类型" span={2}>{detail?.javaType ?? "-"}</Descriptions.Item>
        <Descriptions.Item label="描述" span={2}>{detail?.description ?? "-"}</Descriptions.Item>
      </Descriptions>
      <JsonBlock title="JSON Schema" value={detail?.jsonSchema} />
    </Space>
  );
}

/**
 * 解析表单标题。
 *
 * @param mode 表单模式
 * @returns 标题
 */
function resolveFormTitle(mode: SchemaFormMode | null) {
  if (mode === "create") {
    return "创建 Schema";
  }
  if (mode === "version") {
    return "创建 Schema 新版本";
  }
  return "编辑 Schema 草稿";
}
