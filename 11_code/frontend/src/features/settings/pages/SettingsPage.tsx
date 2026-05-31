import { useState } from "react";
import { App, Button, Card, Form, Input, Modal, Space, Table, Typography } from "antd";
import { useMutation, useQuery } from "@tanstack/react-query";
import { listSettings, updateSettings, type Schema } from "../../../api/domainApi";
import { queryClient } from "../../../app/queryClient";
import { PageState } from "../../../shared/components/PageState";
import { StatusTag } from "../../../shared/components/StatusTag";

/**
 * 系统设置页面。
 */
export function SettingsPage() {
  const { message } = App.useApp();
  const [editing, setEditing] = useState<Schema["SettingItemResult"] | null>(null);
  const [form] = Form.useForm<{ settingValue: string }>();
  const settingsQuery = useQuery({
    queryKey: ["settings"],
    queryFn: listSettings
  });
  const updateMutation = useMutation({
    mutationFn: updateSettings,
    onSuccess: () => {
      message.success("系统设置已更新。");
      setEditing(null);
      void queryClient.invalidateQueries({ queryKey: ["settings"] });
    }
  });

  if (settingsQuery.isLoading || settingsQuery.isError) {
    return (
      <PageState
        title="系统设置"
        description="暂无系统设置。"
        loading={settingsQuery.isLoading}
        error={settingsQuery.error?.message}
      />
    );
  }

  return (
    <Space direction="vertical" size={16} style={{ width: "100%" }}>
      <section className="page-section">
        <Typography.Title level={3}>系统设置</Typography.Title>
        <Typography.Paragraph className="muted-text">
          页面只展示后端白名单返回的配置项；密钥和部署级配置不在此处维护。
        </Typography.Paragraph>
      </section>
      <Card className="page-card">
        <Table<Schema["SettingItemResult"]>
          rowKey={(record) => record.settingKey ?? ""}
          dataSource={settingsQuery.data ?? []}
          pagination={false}
          columns={[
            { title: "配置键", dataIndex: "settingKey" },
            { title: "当前值", dataIndex: "settingValue" },
            { title: "类型", dataIndex: "valueType", render: (value) => <StatusTag status={String(value)} /> },
            { title: "来源", dataIndex: "source", render: (value) => <StatusTag status={String(value)} /> },
            { title: "说明", dataIndex: "description" },
            {
              title: "操作",
              render: (_, record) => (
                <Button
                  type="link"
                  disabled={!record.editable}
                  onClick={() => {
                    setEditing(record);
                    form.setFieldsValue({ settingValue: record.settingValue ?? "" });
                  }}
                >
                  编辑
                </Button>
              )
            }
          ]}
        />
      </Card>
      <Modal
        title="编辑系统设置"
        open={Boolean(editing)}
        onCancel={() => setEditing(null)}
        confirmLoading={updateMutation.isPending}
        onOk={() => {
          void form.validateFields().then((values) => {
            if (!editing?.settingKey || !editing.valueType) {
              return;
            }
            updateMutation.mutate([
              {
                settingKey: editing.settingKey,
                settingValue: values.settingValue,
                valueType: editing.valueType
              }
            ]);
          });
        }}
      >
        <Form form={form} layout="vertical">
          <Form.Item label="配置值" name="settingValue" rules={[{ required: true, message: "请输入配置值。" }]}>
            <Input />
          </Form.Item>
        </Form>
      </Modal>
    </Space>
  );
}
