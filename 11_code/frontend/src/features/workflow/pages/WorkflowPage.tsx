import { useEffect, useMemo, useState } from "react";
import { App, Button, Card, Descriptions, Drawer, Form, Input, InputNumber, Modal, Select, Space, Table, Tabs, Typography } from "antd";
import type { FormInstance } from "antd";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useNavigate, useParams } from "react-router-dom";
import ReactFlow, {
  Background,
  Controls,
  MiniMap,
  addEdge,
  useEdgesState,
  useNodesState,
  type Connection,
  type Edge,
  type Node
} from "reactflow";
import "reactflow/dist/style.css";
import {
  copyWorkflowDraftFromVersion,
  getWorkflowDraft,
  getWorkflowVersion,
  listWorkflowVersions,
  publishWorkflowDraft,
  saveWorkflowDraft,
  validateWorkflowDraft,
  type Schema
} from "../../../api/domainApi";
import { queryClient } from "../../../app/queryClient";
import { JsonBlock, parseJsonText, stringifyJson } from "../../../shared/components/JsonBlock";
import { JsonTextArea } from "../../../shared/components/JsonTextArea";
import { PageState } from "../../../shared/components/PageState";
import { StatusTag } from "../../../shared/components/StatusTag";

type WorkflowNodeType = Schema["WorkflowNodeDefinition"]["type"];
type WorkflowEdgeType = Schema["WorkflowEdgeDefinition"]["type"];
type FlowNodeData = {
  label: string;
  nodeType: WorkflowNodeType;
  definition: Schema["WorkflowNodeDefinition"];
};
type FlowEdgeData = {
  definition: Schema["WorkflowEdgeDefinition"];
};
type NodeFormValues = {
  type: WorkflowNodeType;
  name: string;
  description?: string;
  timeoutSeconds?: number;
  failurePolicy?: string;
  inputSchemaRefText?: string;
  outputSchemaRefText?: string;
  inputMappingText?: string;
  outputMappingText?: string;
  configText?: string;
};

const NODE_TYPES: WorkflowNodeType[] = [
  "START",
  "LLM",
  "CONDITION",
  "JAVA_METHOD",
  "TOOL",
  "AGENT_CALL",
  "EXTERNAL_AGENT",
  "REVIEW",
  "SUMMARY",
  "END"
];

const EDGE_TYPES: WorkflowEdgeType[] = ["NORMAL", "CONDITION", "DEFAULT", "END"];

/**
 * 工作流页面。
 *
 * @param props 组件属性
 * @returns 工作流设计器或只读版本页
 */
export function WorkflowPage({ readonly = false }: { readonly?: boolean }) {
  const { message } = App.useApp();
  const navigate = useNavigate();
  const { agentId, workflowVersionId } = useParams();
  const numericAgentId = Number(agentId);
  const numericWorkflowVersionId = Number(workflowVersionId);
  const [nodes, setNodes, onNodesChange] = useNodesState<FlowNodeData>([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState<FlowEdgeData>([]);
  const [selectedNodeId, setSelectedNodeId] = useState<string | null>(null);
  const [historyOpen, setHistoryOpen] = useState(false);
  const [validationResult, setValidationResult] = useState<Schema["WorkflowValidationResult"] | null>(null);
  const [publishOpen, setPublishOpen] = useState(false);
  const [runtimeForm] = Form.useForm<Schema["WorkflowRuntimeOptions"]>();
  const [nodeForm] = Form.useForm<NodeFormValues>();
  const draftQuery = useQuery({
    queryKey: ["workflow-draft", numericAgentId],
    queryFn: () => getWorkflowDraft(numericAgentId),
    enabled: !readonly && Number.isFinite(numericAgentId) && numericAgentId > 0
  });
  const versionQuery = useQuery({
    queryKey: ["workflow-version", numericAgentId, numericWorkflowVersionId],
    queryFn: () => getWorkflowVersion(numericAgentId, numericWorkflowVersionId),
    enabled: readonly && Number.isFinite(numericAgentId) && Number.isFinite(numericWorkflowVersionId)
  });
  const versionsQuery = useQuery({
    queryKey: ["workflow-versions", numericAgentId],
    queryFn: () => listWorkflowVersions(numericAgentId, { page: 1, pageSize: 50 }),
    enabled: Number.isFinite(numericAgentId) && numericAgentId > 0
  });
  const saveMutation = useMutation({
    mutationFn: (body: Schema["SaveWorkflowDraftRequest"]) => saveWorkflowDraft(numericAgentId, body),
    onSuccess: () => {
      message.success("工作流草稿已保存。");
      void queryClient.invalidateQueries({ queryKey: ["workflow-draft", numericAgentId] });
      void queryClient.invalidateQueries({ queryKey: ["workflow-versions", numericAgentId] });
    }
  });
  const validateMutation = useMutation({
    mutationFn: () => validateWorkflowDraft(numericAgentId),
    onSuccess: (result) => {
      setValidationResult(result);
      if (result.valid) {
        message.success("工作流校验通过。");
      } else {
        message.warning(`工作流校验未通过：${result.errors.length} 个问题。`);
      }
    }
  });
  const publishMutation = useMutation({
    mutationFn: (publishMessage?: string) => publishWorkflowDraft(numericAgentId, publishMessage ? { publishMessage } : undefined),
    onSuccess: () => {
      message.success("工作流已发布。");
      setPublishOpen(false);
      void queryClient.invalidateQueries({ queryKey: ["workflow-draft", numericAgentId] });
      void queryClient.invalidateQueries({ queryKey: ["workflow-versions", numericAgentId] });
    }
  });
  const copyMutation = useMutation({
    mutationFn: (sourceWorkflowVersionId: number) => copyWorkflowDraftFromVersion(numericAgentId, sourceWorkflowVersionId),
    onSuccess: () => {
      message.success("已从历史版本复制生成新草稿。");
      setHistoryOpen(false);
      void queryClient.invalidateQueries({ queryKey: ["workflow-draft", numericAgentId] });
      void queryClient.invalidateQueries({ queryKey: ["workflow-versions", numericAgentId] });
    }
  });
  const source = readonly ? versionQuery.data : draftQuery.data;
  const selectedNode = useMemo(() => nodes.find((node) => node.id === selectedNodeId), [nodes, selectedNodeId]);

  useEffect(() => {
    if (source) {
      setNodes(source.nodes.map((definition) => toFlowNode(definition)));
      setEdges(source.edges.map(toFlowEdge));
      runtimeForm.setFieldsValue(source.runtimeOptions);
      setSelectedNodeId(null);
      setValidationResult(null);
    }
  }, [runtimeForm, setEdges, setNodes, source]);

  useEffect(() => {
    if (selectedNode) {
      nodeForm.setFieldsValue(toNodeFormValues(selectedNode.data.definition));
    } else {
      nodeForm.resetFields();
    }
  }, [nodeForm, selectedNode]);

  if (draftQuery.isLoading || draftQuery.isError || versionQuery.isLoading || versionQuery.isError) {
    return (
      <PageState
        title={readonly ? "工作流历史版本" : "工作流设计器"}
        description="暂无工作流数据。"
        loading={draftQuery.isLoading || versionQuery.isLoading}
        error={draftQuery.error?.message ?? versionQuery.error?.message}
      />
    );
  }

  return (
    <Space direction="vertical" size={16} style={{ width: "100%" }}>
      <section className="page-section page-title-row">
        <div>
          <Typography.Title level={3}>{readonly ? "工作流历史版本" : "工作流设计器"}</Typography.Title>
          <Typography.Paragraph className="muted-text">
            {readonly ? "历史版本只读打开，可复制生成新草稿；不能直接修改不可变版本快照。" : "画布编辑、节点配置、保存草稿、校验、发布和调试入口在同一页面闭环。"}
          </Typography.Paragraph>
        </div>
        <Space wrap>
          <Button onClick={() => setHistoryOpen(true)}>历史版本</Button>
          {!readonly ? (
            <>
              <Button loading={saveMutation.isPending} onClick={() => saveDraft()}>保存草稿</Button>
              <Button loading={validateMutation.isPending} onClick={() => validateMutation.mutate()}>校验</Button>
              <Button type="primary" loading={publishMutation.isPending} onClick={() => setPublishOpen(true)}>发布</Button>
              <Button onClick={() => navigate(`/agents/${numericAgentId}/debug`)}>调试</Button>
            </>
          ) : (
            <Button
              type="primary"
              disabled={!source?.workflowVersionId}
              loading={copyMutation.isPending}
              onClick={() => source?.workflowVersionId && copyMutation.mutate(source.workflowVersionId)}
            >
              复制为新草稿
            </Button>
          )}
        </Space>
      </section>
      <Card className="page-card">
        <Descriptions bordered size="small" column={4}>
          <Descriptions.Item label="版本 ID">{source?.workflowVersionId}</Descriptions.Item>
          <Descriptions.Item label="版本号">{source?.versionNo}</Descriptions.Item>
          <Descriptions.Item label="状态"><StatusTag status={source?.status ?? "UNKNOWN"} /></Descriptions.Item>
          <Descriptions.Item label="来源版本">{source?.sourceWorkflowVersionId ?? "-"}</Descriptions.Item>
        </Descriptions>
      </Card>
      <Tabs
        items={[
          {
            key: "canvas",
            label: "画布",
            children: (
              <div className="workflow-layout">
                {!readonly ? (
                  <Card size="small" title="节点库" className="workflow-side-card">
                    <Space direction="vertical" style={{ width: "100%" }}>
                      {NODE_TYPES.map((nodeType) => (
                        <Button key={nodeType} block onClick={() => addWorkflowNode(nodeType)}>
                          {nodeType}
                        </Button>
                      ))}
                    </Space>
                  </Card>
                ) : null}
                <Card className="workflow-canvas-card" bodyStyle={{ padding: 0 }}>
                  <div className="workflow-canvas">
                    <ReactFlow
                      nodes={nodes}
                      edges={edges}
                      onNodesChange={readonly ? undefined : onNodesChange}
                      onEdgesChange={readonly ? undefined : onEdgesChange}
                      onConnect={readonly ? undefined : onConnect}
                      onNodeClick={(_, node) => setSelectedNodeId(node.id)}
                      onPaneClick={() => setSelectedNodeId(null)}
                      nodesDraggable={!readonly}
                      nodesConnectable={!readonly}
                      elementsSelectable
                      fitView
                    >
                      <MiniMap />
                      <Controls />
                      <Background />
                    </ReactFlow>
                  </div>
                </Card>
                <Card size="small" title="节点属性" className="workflow-side-card">
                  <NodePropertyPanel
                    readonly={readonly}
                    selectedNode={selectedNode}
                    form={nodeForm}
                    onApply={() => applyNodeForm()}
                    onRemove={() => removeSelectedNode()}
                  />
                </Card>
              </div>
            )
          },
          {
            key: "runtime",
            label: "运行约束",
            children: (
              <Card className="page-card">
                <Form form={runtimeForm} layout="vertical" disabled={readonly}>
                  <Form.Item name="timeoutSeconds" label="工作流总超时（秒）" rules={[{ required: true, message: "请输入总超时。" }]}>
                    <InputNumber min={1} style={{ width: "100%" }} />
                  </Form.Item>
                  <Form.Item name="maxSteps" label="最大执行步数" rules={[{ required: true, message: "请输入最大执行步数。" }]}>
                    <InputNumber min={1} style={{ width: "100%" }} />
                  </Form.Item>
                  <Form.Item name="maxAgentCallDepth" label="最大 Agent 调用深度" rules={[{ required: true, message: "请输入最大调用深度。" }]}>
                    <InputNumber min={1} style={{ width: "100%" }} />
                  </Form.Item>
                </Form>
              </Card>
            )
          },
          {
            key: "json",
            label: "JSON 视图",
            children: (
              <Space direction="vertical" size={16} style={{ width: "100%" }}>
                <JsonBlock title="Nodes" value={nodes.map(toWorkflowNode)} />
                <JsonBlock title="Edges" value={edges.map(toWorkflowEdge)} />
                <JsonBlock title="Runtime Options" value={runtimeForm.getFieldsValue()} />
                <JsonBlock title="Referenced Schema Versions" value={source?.referencedSchemaVersions} />
              </Space>
            )
          },
          {
            key: "validation",
            label: "校验结果",
            children: validationResult ? (
              <JsonBlock title="工作流校验结果" value={validationResult} />
            ) : (
              <PageState title="校验结果" description="点击“校验”后展示后端工作流校验结果。" />
            )
          }
        ]}
      />
      <Drawer title="工作流历史版本" open={historyOpen} width={760} onClose={() => setHistoryOpen(false)}>
        <Table<Schema["WorkflowVersionListItemResult"]>
          rowKey={(record) => String(record.workflowVersionId)}
          loading={versionsQuery.isLoading}
          dataSource={versionsQuery.data?.items ?? []}
          pagination={false}
          columns={[
            { title: "版本 ID", dataIndex: "workflowVersionId" },
            { title: "版本号", dataIndex: "versionNo" },
            { title: "状态", dataIndex: "status", render: (value) => <StatusTag status={String(value)} /> },
            { title: "来源版本", dataIndex: "sourceWorkflowVersionId" },
            {
              title: "操作",
              render: (_, record) => (
                <Space>
                  <Button
                    type="link"
                    disabled={!record.workflowVersionId}
                    onClick={() => record.workflowVersionId && navigate(`/agents/${numericAgentId}/workflow/versions/${record.workflowVersionId}`)}
                  >
                    只读打开
                  </Button>
                  <Button
                    type="link"
                    disabled={!record.workflowVersionId}
                    onClick={() => record.workflowVersionId && copyMutation.mutate(record.workflowVersionId)}
                  >
                    复制为草稿
                  </Button>
                </Space>
              )
            }
          ]}
        />
      </Drawer>
      <Modal
        title="发布工作流"
        open={publishOpen}
        confirmLoading={publishMutation.isPending}
        onCancel={() => setPublishOpen(false)}
        onOk={() => publishMutation.mutate(undefined)}
      >
        <Typography.Paragraph>
          发布会把当前草稿固化为不可变 WorkflowVersion，并保留 runtimeOptions 与 referencedSchemaVersions 快照。
        </Typography.Paragraph>
      </Modal>
    </Space>
  );

  /**
   * 添加工作流节点。
   *
   * @param nodeType 节点类型
   */
  function addWorkflowNode(nodeType: WorkflowNodeType) {
    const nodeId = `${nodeType.toLowerCase()}-${Date.now()}`;
    const definition: Schema["WorkflowNodeDefinition"] = {
      nodeId,
      type: nodeType,
      name: `${nodeType} 节点`,
      config: {},
      ui: { position: { x: 120 + nodes.length * 30, y: 80 + nodes.length * 30 } }
    };
    setNodes((current) => [...current, toFlowNode(definition)]);
  }

  /**
   * 连接两个节点。
   *
   * @param connection React Flow 连线
   */
  function onConnect(connection: Connection) {
    if (!connection.source || !connection.target) {
      return;
    }
    const edgeDefinition: Schema["WorkflowEdgeDefinition"] = {
      edgeId: `edge-${Date.now()}`,
      sourceNodeId: connection.source,
      targetNodeId: connection.target,
      type: "NORMAL"
    };
    setEdges((current) => addEdge(toFlowEdge(edgeDefinition), current));
  }

  /**
   * 应用节点属性表单。
   */
  function applyNodeForm() {
    if (!selectedNodeId) {
      message.warning("请先选择节点。");
      return;
    }
    try {
      const values = nodeForm.getFieldsValue();
      setNodes((current) =>
        current.map((node) => {
          if (node.id !== selectedNodeId) {
            return node;
          }
          const definition = toWorkflowNode(node);
          const nextDefinition: Schema["WorkflowNodeDefinition"] = {
            ...definition,
            type: values.type,
            name: values.name,
            description: values.description,
            timeoutSeconds: values.timeoutSeconds,
            failurePolicy: values.failurePolicy,
            inputSchemaRef: parseOptionalJson<Schema["WorkflowSchemaRef"]>(values.inputSchemaRefText),
            outputSchemaRef: parseOptionalJson<Schema["WorkflowSchemaRef"]>(values.outputSchemaRefText),
            inputMapping: parseOptionalJson(values.inputMappingText),
            outputMapping: parseOptionalJson(values.outputMappingText),
            config: parseOptionalJson(values.configText) ?? {}
          };
          return toFlowNode(nextDefinition, node.position);
        })
      );
      message.success("节点属性已更新。");
    } catch {
      message.error("节点属性中的 JSON 格式不正确。");
    }
  }

  /**
   * 删除当前节点。
   */
  function removeSelectedNode() {
    if (!selectedNodeId) {
      return;
    }
    setNodes((current) => current.filter((node) => node.id !== selectedNodeId));
    setEdges((current) => current.filter((edge) => edge.source !== selectedNodeId && edge.target !== selectedNodeId));
    setSelectedNodeId(null);
  }

  /**
   * 保存工作流草稿。
   */
  function saveDraft() {
    void runtimeForm.validateFields().then((runtimeOptions) => {
      saveMutation.mutate({
        nodes: nodes.map(toWorkflowNode),
        edges: edges.map(toWorkflowEdge),
        runtimeOptions: {
          ...(source?.runtimeOptions ?? {}),
          ...runtimeOptions
        }
      });
    });
  }
}

/**
 * 节点属性面板。
 *
 * @param props 组件属性
 * @returns 属性表单
 */
function NodePropertyPanel({
  readonly,
  selectedNode,
  form,
  onApply,
  onRemove
}: {
  readonly: boolean;
  selectedNode?: Node<FlowNodeData>;
  form: FormInstance<NodeFormValues>;
  onApply: () => void;
  onRemove: () => void;
}) {
  if (!selectedNode) {
    return <Typography.Paragraph className="muted-text">选择一个节点后编辑属性。</Typography.Paragraph>;
  }

  return (
    <Space direction="vertical" style={{ width: "100%" }}>
      <Form form={form} layout="vertical" disabled={readonly}>
        <Form.Item name="type" label="节点类型" rules={[{ required: true, message: "请选择节点类型。" }]}>
          <Select options={NODE_TYPES.map((nodeType) => ({ value: nodeType, label: nodeType }))} />
        </Form.Item>
        <Form.Item name="name" label="节点名称" rules={[{ required: true, message: "请输入节点名称。" }]}>
          <Input />
        </Form.Item>
        <Form.Item name="description" label="说明">
          <Input.TextArea rows={2} />
        </Form.Item>
        <Form.Item name="timeoutSeconds" label="节点超时（秒）">
          <InputNumber min={1} style={{ width: "100%" }} />
        </Form.Item>
        <Form.Item name="failurePolicy" label="失败策略">
          <Input placeholder="FAIL_FAST" />
        </Form.Item>
        <Form.Item name="inputSchemaRefText" label="输入 Schema 引用 JSON">
          <JsonTextArea rows={3} placeholder={'{"schemaKey":"input","version":1}'} />
        </Form.Item>
        <Form.Item name="outputSchemaRefText" label="输出 Schema 引用 JSON">
          <JsonTextArea rows={3} placeholder={'{"schemaKey":"output","version":1}'} />
        </Form.Item>
        <Form.Item name="inputMappingText" label="输入映射 JSON">
          <JsonTextArea rows={4} />
        </Form.Item>
        <Form.Item name="outputMappingText" label="输出映射 JSON">
          <JsonTextArea rows={4} />
        </Form.Item>
        <Form.Item name="configText" label="节点配置 JSON">
          <JsonTextArea rows={6} />
        </Form.Item>
      </Form>
      {!readonly ? (
        <Space>
          <Button type="primary" onClick={onApply}>应用节点属性</Button>
          <Button danger onClick={onRemove}>删除节点</Button>
        </Space>
      ) : null}
    </Space>
  );
}

/**
 * 转换为 React Flow 节点。
 *
 * @param definition 工作流节点定义
 * @param position 指定位置
 * @returns React Flow 节点
 */
function toFlowNode(definition: Schema["WorkflowNodeDefinition"], position?: Node["position"]): Node<FlowNodeData> {
  const nodePosition = position ?? {
    x: definition.ui?.position?.x ?? 0,
    y: definition.ui?.position?.y ?? 0
  };
  return {
    id: definition.nodeId,
    position: nodePosition,
    data: {
      label: `${definition.name}\n${definition.type}`,
      nodeType: definition.type,
      definition
    }
  };
}

/**
 * 转换为 React Flow 边。
 *
 * @param definition 工作流边定义
 * @returns React Flow 边
 */
function toFlowEdge(definition: Schema["WorkflowEdgeDefinition"]): Edge<FlowEdgeData> {
  return {
    id: definition.edgeId,
    source: definition.sourceNodeId,
    target: definition.targetNodeId,
    label: definition.type,
    data: { definition }
  };
}

/**
 * 转换为工作流节点定义。
 *
 * @param node React Flow 节点
 * @returns 工作流节点定义
 */
function toWorkflowNode(node: Node<FlowNodeData>): Schema["WorkflowNodeDefinition"] {
  return {
    ...node.data.definition,
    nodeId: node.id,
    ui: {
      ...(node.data.definition.ui ?? {}),
      position: {
        x: Math.round(node.position.x),
        y: Math.round(node.position.y)
      }
    }
  };
}

/**
 * 转换为工作流边定义。
 *
 * @param edge React Flow 边
 * @returns 工作流边定义
 */
function toWorkflowEdge(edge: Edge<FlowEdgeData>): Schema["WorkflowEdgeDefinition"] {
  return {
    ...(edge.data?.definition ?? {
      edgeId: edge.id,
      sourceNodeId: edge.source,
      targetNodeId: edge.target,
      type: "NORMAL" as WorkflowEdgeType
    }),
    edgeId: edge.id,
    sourceNodeId: edge.source,
    targetNodeId: edge.target
  };
}

/**
 * 转换节点属性表单值。
 *
 * @param definition 节点定义
 * @returns 表单值
 */
function toNodeFormValues(definition: Schema["WorkflowNodeDefinition"]): NodeFormValues {
  return {
    type: definition.type,
    name: definition.name,
    description: definition.description,
    timeoutSeconds: definition.timeoutSeconds,
    failurePolicy: definition.failurePolicy,
    inputSchemaRefText: definition.inputSchemaRef ? stringifyJson(definition.inputSchemaRef) : "",
    outputSchemaRefText: definition.outputSchemaRef ? stringifyJson(definition.outputSchemaRef) : "",
    inputMappingText: definition.inputMapping ? stringifyJson(definition.inputMapping) : "",
    outputMappingText: definition.outputMapping ? stringifyJson(definition.outputMapping) : "",
    configText: stringifyJson(definition.config ?? {})
  };
}

/**
 * 解析可选 JSON 文本。
 *
 * @param text JSON 文本
 * @returns JSON 值或 undefined
 */
function parseOptionalJson<T = unknown>(text?: string) {
  if (!text?.trim()) {
    return undefined;
  }
  return parseJsonText(text) as T;
}
