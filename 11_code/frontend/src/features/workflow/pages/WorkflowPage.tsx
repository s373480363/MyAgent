import { useCallback, useEffect, useMemo, useRef, useState } from "react";
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
  getAgent,
  getWorkflowDraft,
  getWorkflowVersion,
  listAgents,
  listExternalAgents,
  listJavaMethods,
  listSchemas,
  listTools,
  listWorkflowVersions,
  publishWorkflowDraft,
  saveWorkflowDraft,
  validateWorkflowDraft,
  type Schema
} from "../../../api/domainApi";
import { queryClient } from "../../../app/queryClient";
import { JsonBlock, parseJsonText, stringifyJson } from "../../../shared/components/JsonBlock";
import { JsonTextArea } from "../../../shared/components/JsonTextArea";
import { ModelOfferingSelect } from "../../../shared/components/ModelOfferingSelect";
import { PageState } from "../../../shared/components/PageState";
import { StatusTag } from "../../../shared/components/StatusTag";

type WorkflowNodeType = Schema["WorkflowNodeDefinition"]["type"];
type WorkflowEdgeType = Schema["WorkflowEdgeDefinition"]["type"];
type JsonObject = Record<string, unknown>;
type ConditionValueType = "STRING" | "NUMBER" | "BOOLEAN" | "JSON";
type FlowNodeData = {
  label: string;
  nodeType: WorkflowNodeType;
  definition: Schema["WorkflowNodeDefinition"];
};
type FlowEdgeData = {
  definition: Schema["WorkflowEdgeDefinition"];
};
type RemoteSelectOption = {
  value: string;
  label: string;
  disabled?: boolean;
  isBoundFallback?: boolean;
};
type PagedListResult<T> = {
  items: T[];
  page: number;
  pageSize: number;
  total: number;
};
type RemoteQuery = {
  page: number;
  pageSize: number;
  keyword?: string;
};
type NodeFormValues = {
  type: WorkflowNodeType;
  name: string;
  description?: string;
  timeoutSeconds?: number;
  failurePolicy?: string;
  inputSchemaRefKey?: string;
  outputSchemaRefKey?: string;
  inputMappingText?: string;
  outputMappingText?: string;
  userPromptTemplate?: string;
  systemPromptTemplate?: string;
  modelOfferingKey?: string;
  temperature?: number | null;
  methodKey?: string;
  toolKey?: string;
  targetAgentKey?: string;
  adapterKey?: string;
  promptTemplate?: string;
  configText?: string;
};
type EdgeFormValues = {
  type: WorkflowEdgeType;
  description?: string;
  conditionLeft?: string;
  conditionOperator?: string;
  conditionValueType?: ConditionValueType;
  conditionRightText?: string;
  definitionText?: string;
};

const MODEL_NODE_TYPES = new Set<WorkflowNodeType>(["LLM", "REVIEW", "SUMMARY"]);
const DIRECTORY_PAGE_SIZE = 20;
const SCHEMA_REF_SEPARATOR = "@@";
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
const CONDITION_OPERATORS = [
  "EXISTS",
  "EQUALS",
  "NOT_EQUALS",
  "CONTAINS",
  "NOT_CONTAINS",
  "IN",
  "NOT_IN",
  "GREATER_THAN",
  "GREATER_THAN_OR_EQUALS",
  "LESS_THAN",
  "LESS_THAN_OR_EQUALS"
] as const;
const CONDITION_VALUE_TYPES: ConditionValueType[] = ["STRING", "NUMBER", "BOOLEAN", "JSON"];

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
  const [selectedEdgeId, setSelectedEdgeId] = useState<string | null>(null);
  const [historyOpen, setHistoryOpen] = useState(false);
  const [validationResult, setValidationResult] = useState<Schema["WorkflowValidationResult"] | null>(null);
  const [publishOpen, setPublishOpen] = useState(false);
  const [runtimeForm] = Form.useForm<Schema["WorkflowRuntimeOptions"]>();
  const [nodeForm] = Form.useForm<NodeFormValues>();
  const [edgeForm] = Form.useForm<EdgeFormValues>();
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
  const agentDetailQuery = useQuery({
    queryKey: ["agent-detail-for-workflow", numericAgentId],
    queryFn: () => getAgent(numericAgentId),
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
  const selectedEdge = useMemo(() => edges.find((edge) => edge.id === selectedEdgeId), [edges, selectedEdgeId]);
  const nodeTypeById = useMemo(() => {
    const mapping = new Map<string, WorkflowNodeType>();
    nodes.forEach((node) => {
      mapping.set(node.id, node.data.definition.type);
    });
    return mapping;
  }, [nodes]);

  useEffect(() => {
    if (source) {
      setNodes(source.nodes.map((definition) => toFlowNode(definition)));
      setEdges(source.edges.map(toFlowEdge));
      runtimeForm.setFieldsValue(source.runtimeOptions);
      setSelectedNodeId(null);
      setSelectedEdgeId(null);
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

  useEffect(() => {
    if (selectedEdge) {
      edgeForm.setFieldsValue(toEdgeFormValues(selectedEdge.data?.definition ?? toWorkflowEdge(selectedEdge)));
    } else {
      edgeForm.resetFields();
    }
  }, [edgeForm, selectedEdge]);

  useEffect(() => {
    if (selectedNodeId && !selectedNode) {
      setSelectedNodeId(null);
    }
  }, [selectedNode, selectedNodeId]);

  useEffect(() => {
    if (selectedEdgeId && !selectedEdge) {
      setSelectedEdgeId(null);
    }
  }, [selectedEdge, selectedEdgeId]);

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
                      onNodeClick={(_, node) => {
                        setSelectedNodeId(node.id);
                        setSelectedEdgeId(null);
                      }}
                      onEdgeClick={(_, edge) => {
                        setSelectedNodeId(null);
                        setSelectedEdgeId(edge.id);
                      }}
                      onPaneClick={() => {
                        setSelectedNodeId(null);
                        setSelectedEdgeId(null);
                      }}
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
                <Card
                  size="small"
                  title={selectedEdge ? "边属性" : "节点属性"}
                  className="workflow-side-card"
                >
                  {selectedEdge ? (
                    <EdgePropertyPanel
                      readonly={readonly}
                      selectedEdge={selectedEdge}
                      sourceNodeType={nodeTypeById.get(selectedEdge.source)}
                      form={edgeForm}
                      onApply={() => applyEdgeForm()}
                      onRemove={() => removeSelectedEdge()}
                    />
                  ) : (
                    <NodePropertyPanel
                      readonly={readonly}
                      selectedNode={selectedNode}
                      currentAgentKey={agentDetailQuery.data?.agentKey}
                      form={nodeForm}
                      onApply={() => applyNodeForm()}
                      onRemove={() => removeSelectedNode()}
                    />
                  )}
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
    setSelectedNodeId(nodeId);
    setSelectedEdgeId(null);
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
    setSelectedNodeId(null);
    setSelectedEdgeId(edgeDefinition.edgeId);
  }

  /**
   * 应用节点属性表单。
   */
  function applyNodeForm() {
    if (!selectedNodeId) {
      message.warning("请先选择节点。");
      return;
    }
    void nodeForm.validateFields().then((values) => {
      try {
        setNodes((current) =>
          current.map((node) => {
            if (node.id !== selectedNodeId) {
              return node;
            }
            const definition = toWorkflowNode(node);
            const baseConfig = parseJsonObjectText(values.configText, "高级配置 JSON 必须是 JSON 对象。");
            const nextDefinition: Schema["WorkflowNodeDefinition"] = {
              ...definition,
              type: values.type,
              name: values.name,
              description: emptyToUndefined(values.description),
              timeoutSeconds: values.timeoutSeconds,
              failurePolicy: emptyToUndefined(values.failurePolicy),
              inputSchemaRef: parseSchemaRefKey(values.inputSchemaRefKey),
              outputSchemaRef: parseSchemaRefKey(values.outputSchemaRefKey),
              inputMapping: parseOptionalJson(values.inputMappingText),
              outputMapping: parseOptionalJson(values.outputMappingText),
              config: buildNodeConfig(values.type, baseConfig, values)
            };
            return toFlowNode(nextDefinition, node.position);
          })
        );
        message.success("节点属性已更新。");
      } catch (error) {
        message.error(error instanceof Error ? error.message : "节点属性更新失败。");
      }
    });
  }

  /**
   * 应用边属性表单。
   */
  function applyEdgeForm() {
    if (!selectedEdgeId) {
      message.warning("请先选择边。");
      return;
    }
    void edgeForm.validateFields().then((values) => {
      try {
        const nextEdges = edges.map((edge) => {
          if (edge.id !== selectedEdgeId) {
            return edge;
          }
          const definition = toWorkflowEdge(edge);
          const baseDefinition = parseJsonObjectText(values.definitionText, "高级边定义 JSON 必须是 JSON 对象。");
          const nextDefinition = buildEdgeDefinition(definition, baseDefinition, values);
          return toFlowEdge(nextDefinition);
        });
        assertNoDuplicateConditionDefaultEdges(nodes.map(toWorkflowNode), nextEdges.map(toWorkflowEdge));
        setEdges(nextEdges);
        message.success("边属性已更新。");
      } catch (error) {
        message.error(error instanceof Error ? error.message : "边属性更新失败。");
      }
    });
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
   * 删除当前边。
   */
  function removeSelectedEdge() {
    if (!selectedEdgeId) {
      return;
    }
    setEdges((current) => current.filter((edge) => edge.id !== selectedEdgeId));
    setSelectedEdgeId(null);
  }

  /**
   * 保存工作流草稿。
   */
  function saveDraft() {
    void runtimeForm.validateFields().then((runtimeOptions) => {
      try {
        const workflowNodes = nodes.map(toWorkflowNode);
        const workflowEdges = edges.map(toWorkflowEdge);
        assertNoDuplicateConditionDefaultEdges(workflowNodes, workflowEdges);
        saveMutation.mutate({
          nodes: workflowNodes,
          edges: workflowEdges,
          runtimeOptions: {
            ...(source?.runtimeOptions ?? {}),
            ...runtimeOptions
          }
        });
      } catch (error) {
        message.error(error instanceof Error ? error.message : "工作流草稿保存失败。");
      }
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
  currentAgentKey,
  form,
  onApply,
  onRemove
}: {
  readonly: boolean;
  selectedNode?: Node<FlowNodeData>;
  currentAgentKey?: string;
  form: FormInstance<NodeFormValues>;
  onApply: () => void;
  onRemove: () => void;
}) {
  const syncGuardRef = useRef(false);
  const currentNodeType = Form.useWatch("type", form) ?? selectedNode?.data.definition.type;
  const inputSchemaValue = Form.useWatch("inputSchemaRefKey", form);
  const outputSchemaValue = Form.useWatch("outputSchemaRefKey", form);
  const fetchJavaMethods = useCallback(
    (query: RemoteQuery) => listJavaMethods({ ...query, status: "ENABLED" }),
    []
  );
  const fetchTools = useCallback(
    (query: RemoteQuery) => listTools({ ...query, status: "ENABLED" }),
    []
  );
  const fetchExternalAgents = useCallback(
    (query: RemoteQuery) => listExternalAgents({ ...query, status: "ENABLED" }),
    []
  );
  const fetchAgents = useCallback(
    (query: RemoteQuery) => listAgents({ ...query, status: "ENABLED" }),
    []
  );
  const fetchSchemas = useCallback(
    (query: RemoteQuery) => listSchemas(query),
    []
  );
  const mapJavaMethod = useCallback((item: Schema["JavaMethodListItemResult"]) => {
    if (!item.methodKey) {
      return undefined;
    }
    return {
      value: item.methodKey,
      label: `${item.name ?? item.methodKey} (${item.methodKey})`
    };
  }, []);
  const mapTool = useCallback((item: Schema["ToolListItemResult"]) => {
    if (!item.toolKey) {
      return undefined;
    }
    return {
      value: item.toolKey,
      label: `${item.name ?? item.toolKey} (${item.toolKey})`
    };
  }, []);
  const mapExternalAgent = useCallback((item: Schema["ExternalAgentListItemResult"]) => {
    if (!item.adapterKey) {
      return undefined;
    }
    return {
      value: item.adapterKey,
      label: `${item.name ?? item.adapterKey} (${item.adapterKey})`
    };
  }, []);
  const mapAgent = useCallback((item: Schema["AgentListItemResult"]) => {
    if (!item.agentKey || !item.currentPublishedWorkflowVersionId || item.agentKey === currentAgentKey) {
      return undefined;
    }
    return {
      value: item.agentKey,
      label: `${item.name ?? item.agentKey} (${item.agentKey})`
    };
  }, [currentAgentKey]);
  const mapSchema = useCallback((item: Schema["SchemaListItem"]) => {
    const label = `${item.name} (${item.schemaKey} / v${item.version} / ${item.status}${item.locked ? " / LOCKED" : ""})`;
    return {
      value: toSchemaRefKey({ schemaKey: item.schemaKey, version: item.version }),
      label
    };
  }, []);

  useEffect(() => {
    if (!selectedNode) {
      return;
    }
    const values = form.getFieldsValue();
    if (!values.configText) {
      form.setFieldValue("configText", stringifyJson(selectedNode.data.definition.config ?? {}));
    }
  }, [form, selectedNode]);

  if (!selectedNode) {
    return <Typography.Paragraph className="muted-text">选择一个节点或边后编辑属性。</Typography.Paragraph>;
  }

  return (
    <Space direction="vertical" style={{ width: "100%" }}>
      <Form
        form={form}
        layout="vertical"
        disabled={readonly}
        onValuesChange={(changedValues, allValues) => {
          if (syncGuardRef.current || !currentNodeType) {
            return;
          }
          if (Object.prototype.hasOwnProperty.call(changedValues, "configText")) {
            const parsedConfig = tryParseJsonObject(allValues.configText);
            if (!parsedConfig) {
              return;
            }
            syncGuardRef.current = true;
            form.setFieldsValue(extractNodeConfigFields(currentNodeType, parsedConfig));
            syncGuardRef.current = false;
            return;
          }
          if (Object.prototype.hasOwnProperty.call(changedValues, "type")) {
            const parsedConfig = tryParseJsonObject(allValues.configText);
            if (parsedConfig && allValues.type) {
              syncGuardRef.current = true;
              form.setFieldsValue(extractNodeConfigFields(allValues.type, parsedConfig));
              syncGuardRef.current = false;
            }
          }
          const parsedConfig = tryParseJsonObject(allValues.configText);
          if (!parsedConfig) {
            return;
          }
          syncGuardRef.current = true;
          form.setFieldValue("configText", stringifyJson(buildNodeConfig(allValues.type, parsedConfig, allValues)));
          syncGuardRef.current = false;
        }}
      >
        <Typography.Text strong>基础配置</Typography.Text>
        <Form.Item name="type" label="节点类型" rules={[{ required: true, message: "请选择节点类型。" }]}>
          <Select data-testid="node-type-select" options={NODE_TYPES.map((nodeType) => ({ value: nodeType, label: nodeType }))} />
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

        <Typography.Text strong>Schema 与映射</Typography.Text>
        <Form.Item name="inputSchemaRefKey" label="输入 Schema">
          <RemotePagedSelect
            placeholder="搜索并选择输入 Schema"
            disabled={readonly}
            fetchPage={fetchSchemas}
            mapItem={mapSchema}
            boundValueFormatter={formatSchemaFallbackLabel}
            dataTestId="node-input-schema-select"
          />
        </Form.Item>
        {inputSchemaValue ? (
          <Typography.Paragraph className="muted-text" style={{ marginTop: -12 }}>
            当前引用：{formatSchemaRefLabel(parseSchemaRefKey(inputSchemaValue))}
          </Typography.Paragraph>
        ) : null}
        <Form.Item name="outputSchemaRefKey" label="输出 Schema">
          <RemotePagedSelect
            placeholder="搜索并选择输出 Schema"
            disabled={readonly}
            fetchPage={fetchSchemas}
            mapItem={mapSchema}
            boundValueFormatter={formatSchemaFallbackLabel}
            dataTestId="node-output-schema-select"
          />
        </Form.Item>
        {outputSchemaValue ? (
          <Typography.Paragraph className="muted-text" style={{ marginTop: -12 }}>
            当前引用：{formatSchemaRefLabel(parseSchemaRefKey(outputSchemaValue))}
          </Typography.Paragraph>
        ) : null}
        <Form.Item name="inputMappingText" label="输入映射 JSON">
          <JsonTextArea rows={4} />
        </Form.Item>
        <Form.Item name="outputMappingText" label="输出映射 JSON">
          <JsonTextArea rows={4} />
        </Form.Item>

        <Typography.Text strong>节点类型配置</Typography.Text>
        {MODEL_NODE_TYPES.has(currentNodeType ?? "START") ? (
          <>
            <Form.Item
              name="userPromptTemplate"
              label="用户提示词模板"
              rules={[{ required: true, message: "请输入用户提示词模板。" }]}
            >
              <Input.TextArea rows={4} />
            </Form.Item>
            <Form.Item name="systemPromptTemplate" label="系统提示词模板">
              <Input.TextArea rows={3} />
            </Form.Item>
            <Form.Item name="modelOfferingKey" label="模型供应项">
              <ModelOfferingSelect
                placeholder="搜索并选择模型供应项"
                disabled={readonly}
                dataTestId="node-model-offering-select"
              />
            </Form.Item>
            <Form.Item name="temperature" label="温度" rules={[{ type: "number", min: 0, max: 2, message: "温度必须在 0 到 2 之间。" }]}>
              <InputNumber min={0} max={2} step={0.1} style={{ width: "100%" }} />
            </Form.Item>
          </>
        ) : null}
        {currentNodeType === "JAVA_METHOD" ? (
          <Form.Item name="methodKey" label="Java 方法" rules={[{ required: true, message: "请选择 Java 方法。" }]}>
            <RemotePagedSelect
              placeholder="搜索并选择 Java 方法"
              disabled={readonly}
              fetchPage={fetchJavaMethods}
              mapItem={mapJavaMethod}
              boundValueFormatter={formatBoundFallbackLabel}
              dataTestId="java-method-select"
            />
          </Form.Item>
        ) : null}
        {currentNodeType === "TOOL" ? (
          <Form.Item name="toolKey" label="工具" rules={[{ required: true, message: "请选择工具。" }]}>
            <RemotePagedSelect
              placeholder="搜索并选择工具"
              disabled={readonly}
              fetchPage={fetchTools}
              mapItem={mapTool}
              boundValueFormatter={formatBoundFallbackLabel}
              dataTestId="tool-select"
            />
          </Form.Item>
        ) : null}
        {currentNodeType === "AGENT_CALL" ? (
          <Form.Item name="targetAgentKey" label="目标 Agent" rules={[{ required: true, message: "请选择目标 Agent。" }]}>
            <RemotePagedSelect
              placeholder="搜索并选择目标 Agent"
              disabled={readonly}
              fetchPage={fetchAgents}
              mapItem={mapAgent}
              boundValueFormatter={formatBoundFallbackLabel}
              dataTestId="agent-call-select"
            />
          </Form.Item>
        ) : null}
        {currentNodeType === "EXTERNAL_AGENT" ? (
          <>
            <Form.Item name="adapterKey" label="外部 Agent 适配器" rules={[{ required: true, message: "请选择外部 Agent 适配器。" }]}>
              <RemotePagedSelect
                placeholder="搜索并选择外部 Agent 适配器"
                disabled={readonly}
                fetchPage={fetchExternalAgents}
                mapItem={mapExternalAgent}
                boundValueFormatter={formatBoundFallbackLabel}
                dataTestId="external-agent-select"
              />
            </Form.Item>
            <Form.Item name="promptTemplate" label="可选提示词模板">
              <Input.TextArea rows={3} />
            </Form.Item>
          </>
        ) : null}
        {currentNodeType === "CONDITION" ? (
          <Typography.Paragraph className="muted-text">
            CONDITION 节点的默认分支和显式条件分支在边属性面板中配置。
          </Typography.Paragraph>
        ) : null}

        <Typography.Text strong>高级 JSON</Typography.Text>
        <Form.Item name="configText" label="高级配置 JSON">
          <JsonTextArea rows={8} placeholder={nodeConfigPlaceholder(currentNodeType ?? selectedNode.data.nodeType)} dataTestId="node-config-json" />
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
 * 边属性面板。
 *
 * @param props 组件属性
 * @returns 属性表单
 */
function EdgePropertyPanel({
  readonly,
  selectedEdge,
  sourceNodeType,
  form,
  onApply,
  onRemove
}: {
  readonly: boolean;
  selectedEdge?: Edge<FlowEdgeData>;
  sourceNodeType?: WorkflowNodeType;
  form: FormInstance<EdgeFormValues>;
  onApply: () => void;
  onRemove: () => void;
}) {
  const syncGuardRef = useRef(false);
  const currentEdgeType = Form.useWatch("type", form) ?? "NORMAL";
  const edgeTypeOptions = sourceNodeType === "CONDITION"
    ? [
      { value: "DEFAULT", label: "默认分支" },
      { value: "CONDITION", label: "显式条件分支" }
    ]
    : [
      { value: "NORMAL", label: "普通连线" },
      { value: "END", label: "显式收口" }
    ];

  if (!selectedEdge) {
    return <Typography.Paragraph className="muted-text">选择一条边后编辑属性。</Typography.Paragraph>;
  }

  return (
    <Space direction="vertical" style={{ width: "100%" }}>
      <Form
        form={form}
        layout="vertical"
        disabled={readonly}
        onValuesChange={(changedValues, allValues) => {
          if (syncGuardRef.current) {
            return;
          }
          if (Object.prototype.hasOwnProperty.call(changedValues, "definitionText")) {
            const parsedDefinition = tryParseJsonObject(allValues.definitionText);
            if (!parsedDefinition) {
              return;
            }
            syncGuardRef.current = true;
            form.setFieldsValue(extractEdgeFormValues(parsedDefinition as Schema["WorkflowEdgeDefinition"]));
            syncGuardRef.current = false;
            return;
          }
          const parsedDefinition = tryParseJsonObject(allValues.definitionText);
          if (!parsedDefinition) {
            return;
          }
          try {
            const nextDefinition = buildEdgeDefinition(
              toWorkflowEdge(selectedEdge),
              parsedDefinition,
              allValues
            );
            syncGuardRef.current = true;
            form.setFieldValue("definitionText", stringifyJson(nextDefinition));
            syncGuardRef.current = false;
          } catch {
            // 条件右值未完成编辑时允许暂时不同步，最终由提交校验收口。
          }
        }}
      >
        <Typography.Text strong>边配置</Typography.Text>
        <Form.Item name="type" label="边类型" rules={[{ required: true, message: "请选择边类型。" }]}>
          <Select data-testid="edge-type-select" options={edgeTypeOptions} />
        </Form.Item>
        <Form.Item name="description" label="边说明">
          <Input.TextArea rows={2} />
        </Form.Item>

        {currentEdgeType === "CONDITION" ? (
          <>
            <Typography.Text strong>条件对象</Typography.Text>
            <Form.Item name="conditionLeft" label="左值 JSONPath" rules={[{ required: true, message: "请输入 left。" }]}>
              <Input placeholder="例如：$.input.score" />
            </Form.Item>
            <Form.Item name="conditionOperator" label="操作符" rules={[{ required: true, message: "请选择 operator。" }]}>
              <Select data-testid="condition-operator-select" options={CONDITION_OPERATORS.map((operator) => ({ value: operator, label: operator }))} />
            </Form.Item>
            <Form.Item name="conditionValueType" label="右值类型" rules={[{ required: true, message: "请选择 valueType。" }]}>
              <Select data-testid="condition-value-type-select" options={CONDITION_VALUE_TYPES.map((valueType) => ({ value: valueType, label: valueType }))} />
            </Form.Item>
            <Form.Item
              noStyle
              shouldUpdate={(previous, current) =>
                previous.conditionValueType !== current.conditionValueType ||
                previous.conditionOperator !== current.conditionOperator
              }
            >
              {() => {
                const watchedValueType = form.getFieldValue("conditionValueType") as ConditionValueType | undefined;
                const watchedOperator = form.getFieldValue("conditionOperator") as string | undefined;
                if (watchedOperator === "EXISTS") {
                  return (
                    <Typography.Paragraph className="muted-text">
                      EXISTS 不需要填写 right。
                    </Typography.Paragraph>
                  );
                }
                return (
                  <Form.Item
                    name="conditionRightText"
                    label="右值"
                    rules={[
                      {
                        validator: async (_, value) => {
                          if (!watchedOperator || !watchedValueType) {
                            return;
                          }
                          if (watchedOperator === "EXISTS") {
                            return;
                          }
                          if (!String(value ?? "").trim()) {
                            throw new Error("请输入 right。");
                          }
                          parseConditionRight(String(value), watchedValueType);
                        }
                      }
                    ]}
                  >
                    {watchedValueType === "JSON" ? (
                      <JsonTextArea rows={4} placeholder='例如：["A","B"]' />
                    ) : (
                      <Input placeholder={conditionRightPlaceholder(watchedValueType)} />
                    )}
                  </Form.Item>
                );
              }}
            </Form.Item>
          </>
        ) : null}

        <Typography.Text strong>高级 JSON</Typography.Text>
        <Form.Item name="definitionText" label="高级边定义 JSON">
          <JsonTextArea rows={8} dataTestId="edge-definition-json" />
        </Form.Item>
      </Form>
      {!readonly ? (
        <Space>
          <Button type="primary" onClick={onApply}>应用边属性</Button>
          <Button danger onClick={onRemove}>删除边</Button>
        </Space>
      ) : null}
    </Space>
  );
}

/**
 * 远程分页选择器。
 *
 * @param props 组件属性
 * @returns 下拉选择器
 */
function RemotePagedSelect<T>({
  value,
  onChange,
  placeholder,
  disabled,
  fetchPage,
  mapItem,
  boundValueFormatter,
  dataTestId
}: {
  value?: string;
  onChange?: (value?: string) => void;
  placeholder: string;
  disabled?: boolean;
  fetchPage: (query: RemoteQuery) => Promise<PagedListResult<T>>;
  mapItem: (item: T) => RemoteSelectOption | undefined;
  boundValueFormatter?: (value: string) => string;
  dataTestId?: string;
}) {
  const requestRef = useRef(0);
  const [open, setOpen] = useState(false);
  const [page, setPage] = useState(1);
  const [keyword, setKeyword] = useState("");
  const [options, setOptions] = useState<RemoteSelectOption[]>([]);
  const [hasMore, setHasMore] = useState(false);
  const [loading, setLoading] = useState(false);

  const boundFallbackOption = useMemo(() => {
    if (!value) {
      return undefined;
    }
    return {
      value,
      label: boundValueFormatter ? boundValueFormatter(value) : value,
      disabled: true,
      isBoundFallback: true
    };
  }, [boundValueFormatter, value]);

  useEffect(() => {
    if (!open) {
      return;
    }
    const requestId = ++requestRef.current;
    setLoading(true);
    void fetchPage({
      page,
      pageSize: DIRECTORY_PAGE_SIZE,
      keyword: keyword.trim() ? keyword.trim() : undefined
    }).then((result) => {
      if (requestRef.current !== requestId) {
        return;
      }
      const fetchedOptions = result.items
        .map(mapItem)
        .filter((item): item is RemoteSelectOption => Boolean(item));
      setOptions((current) => {
        const preserved = page === 1 ? [] : current.filter((option) => !option.isBoundFallback);
        return withBoundFallback(deduplicateOptions([...preserved, ...fetchedOptions]), boundFallbackOption);
      });
      setHasMore(result.page * result.pageSize < result.total);
    }).catch(() => {
      if (requestRef.current !== requestId) {
        return;
      }
      setHasMore(false);
    }).finally(() => {
      if (requestRef.current === requestId) {
        setLoading(false);
      }
    });
  }, [boundFallbackOption, fetchPage, keyword, mapItem, open, page]);

  useEffect(() => {
    setOptions((current) => withBoundFallback(current.filter((option) => !option.isBoundFallback), boundFallbackOption));
  }, [boundFallbackOption]);

  return (
    <Select
      showSearch
      allowClear
      filterOption={false}
      value={value}
      disabled={disabled}
      placeholder={placeholder}
      options={options}
      loading={loading}
      onChange={(nextValue) => onChange?.(nextValue)}
      onOpenChange={(nextOpen) => {
        setOpen(nextOpen);
        if (nextOpen) {
          setPage(1);
        }
      }}
      onSearch={(nextKeyword) => {
        setKeyword(nextKeyword);
        setPage(1);
      }}
      onPopupScroll={(event) => {
        const target = event.target as HTMLElement;
        const reachedBottom = target.scrollTop + target.clientHeight >= target.scrollHeight - 12;
        if (reachedBottom && hasMore && !loading) {
          setPage((current) => current + 1);
        }
      }}
      dropdownRender={(menu) => (
        <>
          {menu}
          <div style={{ padding: "4px 8px", borderTop: "1px solid #f0f0f0" }}>
            <Button
              type="link"
              block
              disabled={!hasMore || loading}
              onMouseDown={(event) => event.preventDefault()}
              onClick={() => setPage((current) => current + 1)}
            >
              {loading ? "加载中..." : hasMore ? "加载下一页" : "没有更多结果"}
            </Button>
          </div>
        </>
      )}
      notFoundContent={loading ? "加载中..." : "暂无可选项"}
      style={{ width: "100%" }}
      data-testid={dataTestId}
    />
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
  const normalized = normalizeEdgeDefinitionForUi(definition);
  return {
    id: normalized.edgeId,
    source: normalized.sourceNodeId,
    target: normalized.targetNodeId,
    label: normalized.type,
    data: { definition: normalized }
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
  const definition = edge.data?.definition ?? {
    edgeId: edge.id,
    sourceNodeId: edge.source,
    targetNodeId: edge.target,
    type: "NORMAL" as WorkflowEdgeType
  };
  return normalizeEdgeDefinitionForSave({
    ...definition,
    edgeId: edge.id,
    sourceNodeId: edge.source,
    targetNodeId: edge.target
  });
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
    inputSchemaRefKey: definition.inputSchemaRef ? toSchemaRefKey(definition.inputSchemaRef) : undefined,
    outputSchemaRefKey: definition.outputSchemaRef ? toSchemaRefKey(definition.outputSchemaRef) : undefined,
    inputMappingText: definition.inputMapping ? stringifyJson(definition.inputMapping) : "",
    outputMappingText: definition.outputMapping ? stringifyJson(definition.outputMapping) : "",
    ...extractNodeConfigFields(definition.type, definition.config),
    configText: stringifyJson(definition.config ?? {})
  };
}

/**
 * 转换边属性表单值。
 *
 * @param definition 边定义
 * @returns 表单值
 */
function toEdgeFormValues(definition: Schema["WorkflowEdgeDefinition"]): EdgeFormValues {
  const edgeFormValues = extractEdgeFormValues(definition);
  return {
    ...edgeFormValues,
    type: edgeFormValues.type,
    definitionText: stringifyJson(normalizeEdgeDefinitionForSave(definition))
  };
}

/**
 * 提取节点显式字段。
 *
 * @param nodeType 节点类型
 * @param config 节点配置
 * @returns 表单字段
 */
function extractNodeConfigFields(nodeType: WorkflowNodeType, config: unknown): Partial<NodeFormValues> {
  const values: Partial<NodeFormValues> = {
    userPromptTemplate: undefined,
    systemPromptTemplate: undefined,
    modelOfferingKey: undefined,
    temperature: undefined,
    methodKey: undefined,
    toolKey: undefined,
    targetAgentKey: undefined,
    adapterKey: undefined,
    promptTemplate: undefined
  };
  const configObject = isJsonObject(config) ? config : {};
  if (MODEL_NODE_TYPES.has(nodeType)) {
    values.userPromptTemplate = readText(configObject, "userPromptTemplate");
    values.systemPromptTemplate = readText(configObject, "systemPromptTemplate");
    values.modelOfferingKey = readText(configObject, "modelOfferingKey");
    values.temperature = readNumber(configObject, "temperature");
  }
  if (nodeType === "JAVA_METHOD") {
    values.methodKey = readText(configObject, "methodKey");
  }
  if (nodeType === "TOOL") {
    values.toolKey = readText(configObject, "toolKey");
  }
  if (nodeType === "AGENT_CALL") {
    values.targetAgentKey = readText(configObject, "targetAgentKey");
  }
  if (nodeType === "EXTERNAL_AGENT") {
    values.adapterKey = readText(configObject, "adapterKey");
    values.promptTemplate = readText(configObject, "promptTemplate");
  }
  return values;
}

/**
 * 根据表单值构建节点配置。
 *
 * @param nodeType 节点类型
 * @param baseConfig 基础配置
 * @param values 表单值
 * @returns 合并后的配置
 */
function buildNodeConfig(
  nodeType: WorkflowNodeType,
  baseConfig: JsonObject,
  values: NodeFormValues
) {
  const nextConfig: JsonObject = { ...baseConfig };
  if (MODEL_NODE_TYPES.has(nodeType)) {
    writeText(nextConfig, "userPromptTemplate", values.userPromptTemplate);
    writeOptionalText(nextConfig, "systemPromptTemplate", values.systemPromptTemplate);
    writeOptionalText(nextConfig, "modelOfferingKey", values.modelOfferingKey);
    writeOptionalNumber(nextConfig, "temperature", values.temperature);
  }
  if (nodeType === "JAVA_METHOD") {
    writeText(nextConfig, "methodKey", values.methodKey);
  }
  if (nodeType === "TOOL") {
    writeText(nextConfig, "toolKey", values.toolKey);
  }
  if (nodeType === "AGENT_CALL") {
    writeText(nextConfig, "targetAgentKey", values.targetAgentKey);
  }
  if (nodeType === "EXTERNAL_AGENT") {
    writeText(nextConfig, "adapterKey", values.adapterKey);
    writeOptionalText(nextConfig, "promptTemplate", values.promptTemplate);
  }
  return nextConfig;
}

/**
 * 从边定义提取显式字段。
 *
 * @param definition 边定义
 * @returns 表单值
 */
function extractEdgeFormValues(definition: Schema["WorkflowEdgeDefinition"]): Omit<EdgeFormValues, "definitionText"> {
  const normalized = normalizeEdgeDefinitionForUi(definition);
  const condition = isJsonObject(normalized.condition) ? normalized.condition : undefined;
  return {
    type: normalized.type ?? "NORMAL",
    description: normalized.description,
    conditionLeft: readText(condition, "left"),
    conditionOperator: readText(condition, "operator"),
    conditionValueType: readText(condition, "valueType") as ConditionValueType | undefined,
    conditionRightText: stringifyConditionRight(condition?.right)
  };
}

/**
 * 构建边定义。
 *
 * @param currentDefinition 当前边定义
 * @param baseDefinition 高级 JSON 基础对象
 * @param values 表单值
 * @returns 新边定义
 */
function buildEdgeDefinition(
  currentDefinition: Schema["WorkflowEdgeDefinition"],
  baseDefinition: JsonObject,
  values: EdgeFormValues
) {
  const nextDefinition: Schema["WorkflowEdgeDefinition"] = {
    ...(baseDefinition as Schema["WorkflowEdgeDefinition"]),
    edgeId: currentDefinition.edgeId,
    sourceNodeId: currentDefinition.sourceNodeId,
    targetNodeId: currentDefinition.targetNodeId,
    description: emptyToUndefined(values.description),
    type: values.type
  };
  delete nextDefinition.isDefault;
  if (values.type === "DEFAULT") {
    delete nextDefinition.condition;
    return normalizeEdgeDefinitionForSave(nextDefinition);
  }
  if (values.type === "CONDITION") {
    nextDefinition.condition = buildConditionObject(values);
    return normalizeEdgeDefinitionForSave(nextDefinition);
  }
  delete nextDefinition.condition;
  return normalizeEdgeDefinitionForSave(nextDefinition);
}

/**
 * 构建条件对象。
 *
 * @param values 表单值
 * @returns 条件对象
 */
function buildConditionObject(values: EdgeFormValues): JsonObject {
  const operator = values.conditionOperator;
  const valueType = values.conditionValueType;
  const left = emptyToUndefined(values.conditionLeft);
  if (!left) {
    throw new Error("条件 left 不能为空。");
  }
  if (!operator) {
    throw new Error("条件 operator 不能为空。");
  }
  if (!valueType) {
    throw new Error("条件 valueType 不能为空。");
  }
  const condition: JsonObject = {
    left,
    operator,
    valueType
  };
  if (operator !== "EXISTS") {
    condition.right = parseConditionRight(String(values.conditionRightText ?? ""), valueType);
  }
  return condition;
}

/**
 * 判断是否默认边。
 *
 * @param definition 边定义
 * @returns 默认边返回 true
 */
function isDefaultEdge(definition: Pick<Schema["WorkflowEdgeDefinition"], "type" | "isDefault">) {
  return definition.type === "DEFAULT" || definition.isDefault === true;
}

/**
 * 归一化 UI 读取用边定义。
 *
 * @param definition 边定义
 * @returns 归一化结果
 */
function normalizeEdgeDefinitionForUi(definition: Schema["WorkflowEdgeDefinition"]): Schema["WorkflowEdgeDefinition"] {
  if (isDefaultEdge(definition)) {
    return {
      ...definition,
      type: "DEFAULT"
    };
  }
  return definition;
}

/**
 * 归一化保存用边定义。
 *
 * @param definition 边定义
 * @returns 保存结果
 */
function normalizeEdgeDefinitionForSave(definition: Schema["WorkflowEdgeDefinition"]): Schema["WorkflowEdgeDefinition"] {
  const nextDefinition: Schema["WorkflowEdgeDefinition"] = {
    ...definition
  };
  delete nextDefinition.isDefault;
  if (isDefaultEdge(definition)) {
    nextDefinition.type = "DEFAULT";
    delete nextDefinition.condition;
    return nextDefinition;
  }
  if (definition.type === "CONDITION") {
    nextDefinition.type = "CONDITION";
    return nextDefinition;
  }
  delete nextDefinition.condition;
  nextDefinition.type = definition.type === "END" ? "END" : "NORMAL";
  return nextDefinition;
}

/**
 * 阻止同一 CONDITION 节点出现多条默认边，避免归一化后产生双轨真相。
 *
 * @param workflowNodes 工作流节点
 * @param workflowEdges 工作流边
 */
function assertNoDuplicateConditionDefaultEdges(
  workflowNodes: Schema["WorkflowNodeDefinition"][],
  workflowEdges: Schema["WorkflowEdgeDefinition"][]
) {
  const conditionNodeIds = new Set(
    workflowNodes
      .filter((node) => node.type === "CONDITION")
      .map((node) => node.nodeId)
  );
  const mapping = new Map<string, Schema["WorkflowEdgeDefinition"][]>();
  workflowEdges.forEach((edge) => {
    const normalizedEdge = normalizeEdgeDefinitionForSave(edge);
    if (!conditionNodeIds.has(normalizedEdge.sourceNodeId) || normalizedEdge.type !== "DEFAULT") {
      return;
    }
    const current = mapping.get(normalizedEdge.sourceNodeId) ?? [];
    current.push(normalizedEdge);
    mapping.set(normalizedEdge.sourceNodeId, current);
  });
  mapping.forEach((defaultEdges, sourceNodeId) => {
    if (defaultEdges.length <= 1) {
      return;
    }
    throw new Error(`CONDITION 节点 ${sourceNodeId} 存在多条默认分支：${defaultEdges.map((edge) => edge.edgeId).join("、")}。`);
  });
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

/**
 * 解析为 JSON 对象。
 *
 * @param text JSON 文本
 * @param errorMessage 错误消息
 * @returns JSON 对象
 */
function parseJsonObjectText(text: string | undefined, errorMessage: string) {
  const parsed = parseOptionalJson(text);
  if (parsed === undefined || parsed === null) {
    return {};
  }
  if (!isJsonObject(parsed)) {
    throw new Error(errorMessage);
  }
  return { ...parsed };
}

/**
 * 尝试解析 JSON 对象。
 *
 * @param text JSON 文本
 * @returns 解析成功时返回对象
 */
function tryParseJsonObject(text?: string) {
  try {
    return parseJsonObjectText(text, "JSON 必须是对象。");
  } catch {
    return undefined;
  }
}

/**
 * 写入必填文本字段。
 *
 * @param target 目标对象
 * @param key 字段名
 * @param value 值
 */
function writeText(target: JsonObject, key: string, value: string | undefined) {
  const text = emptyToUndefined(value);
  if (text) {
    target[key] = text;
  } else {
    delete target[key];
  }
}

/**
 * 写入可选文本字段。
 *
 * @param target 目标对象
 * @param key 字段名
 * @param value 值
 */
function writeOptionalText(target: JsonObject, key: string, value: string | undefined) {
  const text = emptyToUndefined(value);
  if (text) {
    target[key] = text;
  } else {
    delete target[key];
  }
}

/**
 * 写入可选数字字段。
 *
 * @param target 目标对象
 * @param key 字段名
 * @param value 值
 */
function writeOptionalNumber(target: JsonObject, key: string, value: number | null | undefined) {
  if (typeof value === "number" && Number.isFinite(value)) {
    target[key] = value;
  } else {
    delete target[key];
  }
}

/**
 * 读取文本字段。
 *
 * @param source 源对象
 * @param key 字段名
 * @returns 文本值
 */
function readText(source: Record<string, unknown> | undefined, key: string) {
  const value = source?.[key];
  return typeof value === "string" ? value : undefined;
}

/**
 * 读取数字字段。
 *
 * @param source 源对象
 * @param key 字段名
 * @returns 数字值
 */
function readNumber(source: Record<string, unknown> | undefined, key: string) {
  const value = source?.[key];
  return typeof value === "number" && Number.isFinite(value) ? value : undefined;
}

/**
 * 空字符串转 undefined。
 *
 * @param value 输入值
 * @returns 转换结果
 */
function emptyToUndefined(value?: string) {
  const trimmed = value?.trim();
  return trimmed ? trimmed : undefined;
}

/**
 * 判断是否普通 JSON 对象。
 *
 * @param value 待判断值
 * @returns 普通对象返回 true
 */
function isJsonObject(value: unknown): value is JsonObject {
  return Boolean(value) && typeof value === "object" && !Array.isArray(value);
}

/**
 * 去重选择器选项。
 *
 * @param options 选项列表
 * @returns 去重结果
 */
function deduplicateOptions(options: RemoteSelectOption[]) {
  const mapping = new Map<string, RemoteSelectOption>();
  options.forEach((option) => {
    mapping.set(option.value, option);
  });
  return Array.from(mapping.values());
}

/**
 * 补齐已绑定回填项。
 *
 * @param options 已加载选项
 * @param boundOption 当前绑定项
 * @returns 结果选项
 */
function withBoundFallback(options: RemoteSelectOption[], boundOption?: RemoteSelectOption) {
  if (!boundOption) {
    return options;
  }
  if (options.some((option) => option.value === boundOption.value)) {
    return options;
  }
  return [boundOption, ...options];
}

/**
 * WorkflowSchemaRef 转换为表单值。
 *
 * @param schemaRef Schema 引用
 * @returns 选择器值
 */
function toSchemaRefKey(schemaRef: Schema["WorkflowSchemaRef"]) {
  return `${schemaRef.schemaKey}${SCHEMA_REF_SEPARATOR}${schemaRef.version}`;
}

/**
 * 解析 Schema 选择器值。
 *
 * @param value 选择器值
 * @returns Schema 引用
 */
function parseSchemaRefKey(value?: string) {
  if (!value) {
    return undefined;
  }
  const [schemaKey, versionText] = value.split(SCHEMA_REF_SEPARATOR);
  const version = Number(versionText);
  if (!schemaKey || !Number.isInteger(version)) {
    throw new Error("Schema 引用格式不正确。");
  }
  return {
    schemaKey,
    version
  };
}

/**
 * 格式化当前绑定回填文案。
 *
 * @param value 当前绑定值
 * @returns 文案
 */
function formatBoundFallbackLabel(value: string) {
  return `当前绑定：${value}（未加载详情或当前结果未命中）`;
}

/**
 * 格式化 Schema 回填文案。
 *
 * @param value Schema 选择器值
 * @returns 文案
 */
function formatSchemaFallbackLabel(value: string) {
  return formatBoundFallbackLabel(formatSchemaRefLabel(parseSchemaRefKey(value)));
}

/**
 * 格式化 Schema 引用标签。
 *
 * @param schemaRef Schema 引用
 * @returns 文案
 */
function formatSchemaRefLabel(schemaRef?: Schema["WorkflowSchemaRef"]) {
  if (!schemaRef) {
    return "-";
  }
  return `${schemaRef.schemaKey} / v${schemaRef.version}`;
}

/**
 * 序列化条件右值。
 *
 * @param value 右值
 * @returns 文本
 */
function stringifyConditionRight(value: unknown) {
  if (typeof value === "string") {
    return value;
  }
  if (value === undefined) {
    return "";
  }
  return stringifyJson(value);
}

/**
 * 解析条件右值。
 *
 * @param text 文本
 * @param valueType 右值类型
 * @returns 解析结果
 */
function parseConditionRight(text: string, valueType: ConditionValueType) {
  const trimmed = text.trim();
  if (!trimmed) {
    throw new Error("条件 right 不能为空。");
  }
  if (valueType === "STRING") {
    return text;
  }
  if (valueType === "NUMBER") {
    const parsed = Number(trimmed);
    if (!Number.isFinite(parsed)) {
      throw new Error("NUMBER 类型的 right 必须是有效数字。");
    }
    return parsed;
  }
  if (valueType === "BOOLEAN") {
    if (trimmed === "true") {
      return true;
    }
    if (trimmed === "false") {
      return false;
    }
    throw new Error("BOOLEAN 类型的 right 只能是 true 或 false。");
  }
  return parseJsonText(text);
}

/**
 * 条件右值占位提示。
 *
 * @param valueType 右值类型
 * @returns 占位文案
 */
function conditionRightPlaceholder(valueType?: ConditionValueType) {
  if (valueType === "NUMBER") {
    return "例如：80";
  }
  if (valueType === "BOOLEAN") {
    return "true 或 false";
  }
  if (valueType === "JSON") {
    return '例如：["A","B"]';
  }
  return "例如：通过";
}

/**
 * 返回节点配置占位示例。
 *
 * @param nodeType 节点类型
 * @returns JSON 占位文本
 */
function nodeConfigPlaceholder(nodeType: WorkflowNodeType) {
  if (MODEL_NODE_TYPES.has(nodeType)) {
    return '{"userPromptTemplate":"请基于 {inputJson} 生成结果","systemPromptTemplate":"可选系统提示词","modelOfferingKey":"openai.gpt_4_1_mini","temperature":0.2}';
  }
  if (nodeType === "JAVA_METHOD") {
    return '{"methodKey":"demo.echo"}';
  }
  if (nodeType === "TOOL") {
    return '{"toolKey":"echo"}';
  }
  if (nodeType === "AGENT_CALL") {
    return '{"targetAgentKey":"child-agent"}';
  }
  if (nodeType === "EXTERNAL_AGENT") {
    return '{"adapterKey":"demo-http","promptTemplate":"请处理 {inputJson}"}';
  }
  return "{}";
}
