import { fireEvent, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { WorkflowPage } from "./WorkflowPage";
import { renderWithProviders } from "../../../test/renderWithProviders";
import {
  getAgent,
  getModelOfferingsByKeys,
  getWorkflowDraft,
  getWorkflowVersion,
  listAgents,
  listExternalAgents,
  listJavaMethods,
  listModelOfferings,
  listSchemas,
  listTools,
  listWorkflowVersions,
  saveWorkflowDraft,
  validateWorkflowDraft,
  type Schema
} from "../../../api/domainApi";

vi.mock("reactflow", async () => {
  const React = await import("react");
  return {
    default: ({
      nodes,
      edges,
      children,
      onNodeClick,
      onEdgeClick,
      onPaneClick
    }: {
      nodes: Array<{ id: string; data: { label: string } }>;
      edges: Array<{ id: string; label?: string }>;
      children: React.ReactNode;
      onNodeClick?: (event: unknown, node: unknown) => void;
      onEdgeClick?: (event: unknown, edge: unknown) => void;
      onPaneClick?: () => void;
    }) => (
      <div data-testid="react-flow">
        <button type="button" data-testid="flow-pane" onClick={() => onPaneClick?.()}>
          pane
        </button>
        {nodes.map((node) => (
          <button key={node.id} type="button" data-testid={`node-${node.id}`} onClick={(event) => onNodeClick?.(event, node)}>
            {node.data.label}
          </button>
        ))}
        {edges.map((edge) => (
          <button key={edge.id} type="button" data-testid={`edge-${edge.id}`} onClick={(event) => onEdgeClick?.(event, edge)}>
            {edge.label ?? edge.id}
          </button>
        ))}
        {children}
      </div>
    ),
    Background: () => <div data-testid="flow-background" />,
    Controls: () => <div data-testid="flow-controls" />,
    MiniMap: () => <div data-testid="flow-minimap" />,
    addEdge: (edge: unknown, edges: unknown[]) => [...edges, edge],
    useEdgesState: (initial: unknown[]) => {
      const [edges, setEdges] = React.useState(initial);
      return [edges, setEdges, () => undefined];
    },
    useNodesState: (initial: unknown[]) => {
      const [nodes, setNodes] = React.useState(initial);
      return [nodes, setNodes, () => undefined];
    }
  };
});

vi.mock("../../../api/domainApi", async (importOriginal) => {
  const actual = await importOriginal<typeof import("../../../api/domainApi")>();
  return {
    ...actual,
    copyWorkflowDraftFromVersion: vi.fn(),
    getAgent: vi.fn(),
    getModelOfferingsByKeys: vi.fn(),
    getWorkflowDraft: vi.fn(),
    getWorkflowVersion: vi.fn(),
    listAgents: vi.fn(),
    listExternalAgents: vi.fn(),
    listJavaMethods: vi.fn(),
    listModelOfferings: vi.fn(),
    listSchemas: vi.fn(),
    listTools: vi.fn(),
    listWorkflowVersions: vi.fn(),
    publishWorkflowDraft: vi.fn(),
    saveWorkflowDraft: vi.fn(),
    validateWorkflowDraft: vi.fn()
  };
});

describe("WorkflowPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(getAgent).mockResolvedValue({
      agentId: 1,
      agentKey: "root-agent",
      status: "ENABLED",
      name: "根 Agent",
      description: "用于工作流页面测试",
      defaultModelOfferingKey: "openai.gpt_4_1_mini",
      temperature: 0.2,
      timeoutSeconds: 600,
      maxSteps: 30,
      historyVersionSummary: {
        total: 0
      },
      updatedAt: "2026-06-04T00:00:00Z"
    });
    vi.mocked(getWorkflowDraft).mockResolvedValue(createWorkflowDraft());
    vi.mocked(getWorkflowVersion).mockResolvedValue({ ...createWorkflowDraft(), status: "HISTORY" });
    vi.mocked(listWorkflowVersions).mockResolvedValue({
      items: [{ workflowVersionId: 12, versionNo: 2, status: "PUBLISHED" }],
      page: 1,
      pageSize: 50,
      total: 1
    });
    vi.mocked(listAgents).mockResolvedValue(emptyPage());
    vi.mocked(listExternalAgents).mockResolvedValue(emptyPage());
    vi.mocked(listJavaMethods).mockResolvedValue(emptyPage());
    vi.mocked(listModelOfferings).mockResolvedValue(emptyPage());
    vi.mocked(listSchemas).mockResolvedValue(emptyPage());
    vi.mocked(listTools).mockResolvedValue(emptyPage());
    vi.mocked(getModelOfferingsByKeys).mockResolvedValue({ items: [], missingKeys: [] });
    vi.mocked(saveWorkflowDraft).mockResolvedValue(createWorkflowDraft());
    vi.mocked(validateWorkflowDraft).mockResolvedValue({ valid: true, errors: [] });
  });

  it("uses paged model offering selector and saves canonical LLM config fields", async () => {
    vi.mocked(getWorkflowDraft).mockResolvedValue(
      createWorkflowDraft({
        nodes: [
          createNode({ nodeId: "node-start", type: "START", name: "开始" }),
          createNode({ nodeId: "node-llm", type: "LLM", name: "总结节点", config: {} }),
          createNode({ nodeId: "node-end", type: "END", name: "结束" })
        ],
        edges: [
          createEdge({ edgeId: "edge-start-llm", sourceNodeId: "node-start", targetNodeId: "node-llm" }),
          createEdge({ edgeId: "edge-llm-end", sourceNodeId: "node-llm", targetNodeId: "node-end" })
        ]
      })
    );
    vi.mocked(listModelOfferings)
      .mockResolvedValueOnce({
        items: [
          createOfferingDescriptor({
            offeringId: 101,
            offeringKey: "page-one.offering",
            providerKey: "page-one",
            providerName: "第一页供应商",
            modelKey: "page-one-model",
            displayName: "第一页模型",
            upstreamModelName: "page-one-upstream"
          })
        ],
        page: 1,
        pageSize: 20,
        total: 21
      })
      .mockResolvedValueOnce({
        items: [
          createOfferingDescriptor({
            offeringId: 102,
            offeringKey: "openai.gpt_4_1_mini",
            providerKey: "openai",
            providerName: "OpenAI",
            modelKey: "gpt_4_1_mini",
            displayName: "GPT-4.1 Mini",
            upstreamModelName: "gpt-4.1-mini"
          })
        ],
        page: 2,
        pageSize: 20,
        total: 21
      });

    renderWorkflowPage();

    fireEvent.click(await screen.findByTestId("node-node-llm"));
    fireEvent.change(screen.getByLabelText("节点名称"), { target: { value: "总结节点已配置" } });
    fireEvent.change(screen.getByLabelText("用户提示词模板"), { target: { value: "请总结 {inputJson}" } });
    fireEvent.change(screen.getByLabelText("系统提示词模板"), { target: { value: "你是一个严谨的总结助手。" } });
    openSelect("node-model-offering-select");
    await waitFor(() => {
      expect(hasPagedQueryCall(vi.mocked(listModelOfferings), 1)).toBe(true);
    });
    fireEvent.click(await screen.findByRole("button", { name: "加载下一页" }));
    await waitFor(() => {
      expect(hasPagedQueryCall(vi.mocked(listModelOfferings), 2)).toBe(true);
    });
    selectDropdownOption("OpenAI / GPT-4.1 Mini (gpt-4.1-mini)");
    fireEvent.change(screen.getByLabelText("温度"), { target: { value: "0.6" } });

    fireEvent.click(screen.getByRole("button", { name: "应用节点属性" }));
    await waitFor(() => expect(screen.getByTestId("node-node-llm")).toHaveTextContent("总结节点已配置"));
    fireEvent.click(screen.getByRole("button", { name: "保存草稿" }));

    await waitFor(() => expect(saveWorkflowDraft).toHaveBeenCalledTimes(1));
    const payload = lastSavedPayload();
    const llmNode = payload.nodes.find((node) => node.nodeId === "node-llm");
    expect(llmNode?.config).toMatchObject({
      userPromptTemplate: "请总结 {inputJson}",
      systemPromptTemplate: "你是一个严谨的总结助手。",
      modelOfferingKey: "openai.gpt_4_1_mini",
      temperature: 0.6
    });
  });

  it("keeps current bound model offering when editing other fields and preserves unknown config", async () => {
    vi.mocked(getWorkflowDraft).mockResolvedValue(
      createWorkflowDraft({
        nodes: [
          createNode({ nodeId: "node-start", type: "START", name: "开始" }),
          createNode({
            nodeId: "node-llm",
            type: "LLM",
            name: "分析节点",
            config: {
              userPromptTemplate: "旧提示词",
              modelOfferingKey: "openai.legacy",
              xExperimentalField: "must_keep"
            }
          }),
          createNode({ nodeId: "node-end", type: "END", name: "结束" })
        ],
        edges: [
          createEdge({ edgeId: "edge-start-llm", sourceNodeId: "node-start", targetNodeId: "node-llm" }),
          createEdge({ edgeId: "edge-llm-end", sourceNodeId: "node-llm", targetNodeId: "node-end" })
        ]
      })
    );
    vi.mocked(getModelOfferingsByKeys).mockResolvedValue({
      items: [
        createOfferingDescriptor({
          offeringId: 201,
          offeringKey: "openai.legacy",
          providerKey: "openai",
          providerName: "OpenAI",
          modelKey: "gpt_4_1",
          displayName: "旧模型",
          upstreamModelName: "gpt-4.1",
          selectable: false,
          unavailableReason: "供应项已停用"
        })
      ],
      missingKeys: []
    });

    renderWorkflowPage();

    fireEvent.click(await screen.findByTestId("node-node-llm"));
    await waitFor(() => {
      expect(screen.getByTestId("node-model-offering-select")).toHaveTextContent("当前绑定：OpenAI / 旧模型 (gpt-4.1)（供应项已停用）");
    });
    fireEvent.change(screen.getByLabelText("节点名称"), { target: { value: "分析节点已配置" } });
    const configEditor = getNodeConfigEditor();
    fireEvent.change(configEditor, {
      target: {
        value: JSON.stringify(
          {
            userPromptTemplate: "通过 JSON 更新",
            modelOfferingKey: "openai.legacy",
            xExperimentalField: "must_keep"
          },
          null,
          2
        )
      }
    });

    await waitFor(() => expect(screen.getByLabelText("用户提示词模板")).toHaveValue("通过 JSON 更新"));
    expect(screen.getByTestId("node-model-offering-select")).toHaveTextContent("当前绑定：OpenAI / 旧模型 (gpt-4.1)（供应项已停用）");

    fireEvent.click(screen.getByRole("button", { name: "应用节点属性" }));
    await waitFor(() => expect(screen.getByTestId("node-node-llm")).toHaveTextContent("分析节点已配置"));
    fireEvent.click(screen.getByRole("button", { name: "保存草稿" }));

    await waitFor(() => expect(saveWorkflowDraft).toHaveBeenCalledTimes(1));
    const payload = lastSavedPayload();
    const llmNode = payload.nodes.find((node) => node.nodeId === "node-llm");
    expect(llmNode?.config).toMatchObject({
      userPromptTemplate: "通过 JSON 更新",
      modelOfferingKey: "openai.legacy",
      xExperimentalField: "must_keep"
    });
  });

  it("normalizes historical isDefault edges to type DEFAULT and removes legacy isDefault on save", async () => {
    vi.mocked(getWorkflowDraft).mockResolvedValue(
      createWorkflowDraft({
        nodes: [
          createNode({ nodeId: "node-start", type: "START", name: "开始" }),
          createNode({ nodeId: "node-condition", type: "CONDITION", name: "条件判断" }),
          createNode({ nodeId: "node-end-default", type: "END", name: "默认结束" }),
          createNode({ nodeId: "node-end-match", type: "END", name: "命中结束" })
        ],
        edges: [
          createEdge({ edgeId: "edge-start-condition", sourceNodeId: "node-start", targetNodeId: "node-condition" }),
          createEdge({
            edgeId: "edge-default",
            sourceNodeId: "node-condition",
            targetNodeId: "node-end-default",
            type: "NORMAL",
            isDefault: true
          }),
          createEdge({
            edgeId: "edge-condition",
            sourceNodeId: "node-condition",
            targetNodeId: "node-end-match",
            type: "CONDITION",
            condition: {
              left: "$.order.amount",
              operator: "GREATER_THAN",
              right: 100,
              valueType: "NUMBER"
            }
          })
        ]
      })
    );

    renderWorkflowPage();
    fireEvent.click(await screen.findByRole("button", { name: "保存草稿" }));

    await waitFor(() => expect(saveWorkflowDraft).toHaveBeenCalledTimes(1));
    const defaultEdge = lastSavedPayload().edges.find((edge) => edge.edgeId === "edge-default");
    expect(defaultEdge).toMatchObject({
      edgeId: "edge-default",
      type: "DEFAULT"
    });
    expect(defaultEdge).not.toHaveProperty("isDefault");
  });

  it("blocks saving when one CONDITION node would keep multiple default edges after normalization", async () => {
    vi.mocked(getWorkflowDraft).mockResolvedValue(
      createWorkflowDraft({
        nodes: [
          createNode({ nodeId: "node-start", type: "START", name: "开始" }),
          createNode({ nodeId: "node-condition", type: "CONDITION", name: "条件判断" }),
          createNode({ nodeId: "node-end-a", type: "END", name: "结束 A" }),
          createNode({ nodeId: "node-end-b", type: "END", name: "结束 B" })
        ],
        edges: [
          createEdge({ edgeId: "edge-start-condition", sourceNodeId: "node-start", targetNodeId: "node-condition" }),
          createEdge({ edgeId: "edge-default-a", sourceNodeId: "node-condition", targetNodeId: "node-end-a", type: "DEFAULT" }),
          createEdge({ edgeId: "edge-default-b", sourceNodeId: "node-condition", targetNodeId: "node-end-b", type: "NORMAL", isDefault: true })
        ]
      })
    );

    renderWorkflowPage();
    fireEvent.click(await screen.findByRole("button", { name: "保存草稿" }));

    await waitFor(() => expect(saveWorkflowDraft).not.toHaveBeenCalled());
  });

  it("keeps current bound AgentCall value when user edits other fields, and filters selector by self plus published status", async () => {
    vi.mocked(getWorkflowDraft).mockResolvedValue(
      createWorkflowDraft({
        nodes: [
          createNode({ nodeId: "node-start", type: "START", name: "开始" }),
          createNode({
            nodeId: "node-agent-call",
            type: "AGENT_CALL",
            name: "调用子 Agent",
            config: {
              targetAgentKey: "legacy-agent"
            }
          }),
          createNode({ nodeId: "node-end", type: "END", name: "结束" })
        ],
        edges: [
          createEdge({ edgeId: "edge-start-agent", sourceNodeId: "node-start", targetNodeId: "node-agent-call" }),
          createEdge({ edgeId: "edge-agent-end", sourceNodeId: "node-agent-call", targetNodeId: "node-end" })
        ]
      })
    );
    vi.mocked(listAgents)
      .mockResolvedValueOnce({
        items: [
          { agentId: 1, agentKey: "root-agent", name: "当前 Agent", status: "ENABLED", currentPublishedWorkflowVersionId: 11 },
          { agentId: 2, agentKey: "draft-only-agent", name: "未发布 Agent", status: "ENABLED" }
        ],
        page: 1,
        pageSize: 20,
        total: 21
      })
      .mockResolvedValueOnce({
        items: [
          { agentId: 3, agentKey: "published-child", name: "已发布子 Agent", status: "ENABLED", currentPublishedWorkflowVersionId: 22 }
        ],
        page: 2,
        pageSize: 20,
        total: 21
    });

    renderWorkflowPage();
    fireEvent.click(await screen.findByTestId("node-node-agent-call"));
    expect(screen.getByText("当前绑定：legacy-agent（未加载详情或当前结果未命中）")).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText("节点名称"), { target: { value: "调用子 Agent（保留旧值）" } });
    fireEvent.click(screen.getByRole("button", { name: "应用节点属性" }));
    fireEvent.click(screen.getByRole("button", { name: "保存草稿" }));

    await waitFor(() => expect(saveWorkflowDraft).toHaveBeenCalledTimes(1));
    const firstPayload = lastSavedPayload();
    const agentCallNode = firstPayload.nodes.find((node) => node.nodeId === "node-agent-call");
    expect(agentCallNode?.config).toMatchObject({
      targetAgentKey: "legacy-agent"
    });

    openSelect("agent-call-select");
    await waitFor(() => {
      expect(listAgents).toHaveBeenNthCalledWith(1, { page: 1, pageSize: 20, status: "ENABLED" });
    });
    expect(screen.queryByText("当前 Agent (root-agent)")).not.toBeInTheDocument();
    expect(screen.queryByText("未发布 Agent (draft-only-agent)")).not.toBeInTheDocument();

    fireEvent.click(await screen.findByRole("button", { name: "加载下一页" }));
    await waitFor(() => {
      expect(listAgents).toHaveBeenNthCalledWith(2, { page: 2, pageSize: 20, status: "ENABLED" });
    });
    expect(await screen.findByText("已发布子 Agent (published-child)")).toBeInTheDocument();
  });

  it("keeps current bound Java method value and lets user pick a later-page option", async () => {
    vi.mocked(getWorkflowDraft).mockResolvedValue(
      createWorkflowDraft({
        nodes: [
          createNode({ nodeId: "node-start", type: "START", name: "开始" }),
          createNode({
            nodeId: "node-java",
            type: "JAVA_METHOD",
            name: "调用 Java 方法",
            config: {
              methodKey: "legacy-method"
            }
          }),
          createNode({ nodeId: "node-end", type: "END", name: "结束" })
        ],
        edges: [
          createEdge({ edgeId: "edge-start-java", sourceNodeId: "node-start", targetNodeId: "node-java" }),
          createEdge({ edgeId: "edge-java-end", sourceNodeId: "node-java", targetNodeId: "node-end" })
        ]
      })
    );
    vi.mocked(listJavaMethods)
      .mockResolvedValueOnce({
        items: [
          { methodKey: "page-one-method", name: "第一页方法", status: "ENABLED" }
        ],
        page: 1,
        pageSize: 20,
        total: 21
      })
      .mockResolvedValueOnce({
        items: [
          { methodKey: "method-new", name: "已注册方法", status: "ENABLED" }
        ],
        page: 2,
        pageSize: 20,
        total: 21
      });

    renderWorkflowPage();
    fireEvent.click(await screen.findByTestId("node-node-java"));
    expect(screen.getByText("当前绑定：legacy-method（未加载详情或当前结果未命中）")).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText("节点名称"), { target: { value: "调用 Java 方法（保留旧值）" } });
    fireEvent.click(screen.getByRole("button", { name: "应用节点属性" }));
    fireEvent.click(screen.getByRole("button", { name: "保存草稿" }));

    await waitFor(() => expect(saveWorkflowDraft).toHaveBeenCalledTimes(1));
    expect(lastSavedPayload().nodes.find((node) => node.nodeId === "node-java")?.config).toMatchObject({
      methodKey: "legacy-method"
    });

    vi.mocked(saveWorkflowDraft).mockClear();

    openSelect("java-method-select");
    await waitFor(() => {
      expect(listJavaMethods).toHaveBeenNthCalledWith(1, expect.objectContaining({ page: 1, pageSize: 20, status: "ENABLED" }));
    });
    fireEvent.click(await screen.findByRole("button", { name: "加载下一页" }));
    await waitFor(() => {
      expect(listJavaMethods).toHaveBeenNthCalledWith(2, expect.objectContaining({ page: 2, pageSize: 20, status: "ENABLED" }));
    });
    selectDropdownOption("已注册方法 (method-new)");
    await waitFor(() => expect(screen.getByTestId("java-method-select")).toHaveTextContent("已注册方法 (method-new)"));
    fireEvent.change(screen.getByLabelText("节点名称"), { target: { value: "调用 Java 方法（已重新选择）" } });

    fireEvent.click(screen.getByRole("button", { name: "应用节点属性" }));
    await waitFor(() => expect(screen.getByTestId("node-node-java")).toHaveTextContent("调用 Java 方法（已重新选择）"));
    fireEvent.click(screen.getByRole("button", { name: "保存草稿" }));

    await waitFor(() => expect(saveWorkflowDraft).toHaveBeenCalledTimes(1));
    expect(lastSavedPayload().nodes.find((node) => node.nodeId === "node-java")?.config).toMatchObject({
      methodKey: "method-new"
    });
  });

  it("keeps current bound tool value and lets user pick a later-page option", async () => {
    vi.mocked(getWorkflowDraft).mockResolvedValue(
      createWorkflowDraft({
        nodes: [
          createNode({ nodeId: "node-start", type: "START", name: "开始" }),
          createNode({
            nodeId: "node-tool",
            type: "TOOL",
            name: "调用工具",
            config: {
              toolKey: "legacy-tool"
            }
          }),
          createNode({ nodeId: "node-end", type: "END", name: "结束" })
        ],
        edges: [
          createEdge({ edgeId: "edge-start-tool", sourceNodeId: "node-start", targetNodeId: "node-tool" }),
          createEdge({ edgeId: "edge-tool-end", sourceNodeId: "node-tool", targetNodeId: "node-end" })
        ]
      })
    );
    vi.mocked(listTools)
      .mockResolvedValueOnce({
        items: [
          { toolKey: "page-one-tool", name: "第一页工具", status: "ENABLED" }
        ],
        page: 1,
        pageSize: 20,
        total: 21
      })
      .mockResolvedValueOnce({
        items: [
          { toolKey: "tool-new", name: "已注册工具", status: "ENABLED" }
        ],
        page: 2,
        pageSize: 20,
        total: 21
      });

    renderWorkflowPage();
    fireEvent.click(await screen.findByTestId("node-node-tool"));
    expect(screen.getByText("当前绑定：legacy-tool（未加载详情或当前结果未命中）")).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText("节点名称"), { target: { value: "调用工具（保留旧值）" } });
    fireEvent.click(screen.getByRole("button", { name: "应用节点属性" }));
    fireEvent.click(screen.getByRole("button", { name: "保存草稿" }));

    await waitFor(() => expect(saveWorkflowDraft).toHaveBeenCalledTimes(1));
    expect(lastSavedPayload().nodes.find((node) => node.nodeId === "node-tool")?.config).toMatchObject({
      toolKey: "legacy-tool"
    });

    vi.mocked(saveWorkflowDraft).mockClear();

    openSelect("tool-select");
    await waitFor(() => {
      expect(listTools).toHaveBeenNthCalledWith(1, expect.objectContaining({ page: 1, pageSize: 20, status: "ENABLED" }));
    });
    fireEvent.click(await screen.findByRole("button", { name: "加载下一页" }));
    await waitFor(() => {
      expect(listTools).toHaveBeenNthCalledWith(2, expect.objectContaining({ page: 2, pageSize: 20, status: "ENABLED" }));
    });
    selectDropdownOption("已注册工具 (tool-new)");
    await waitFor(() => expect(screen.getByTestId("tool-select")).toHaveTextContent("已注册工具 (tool-new)"));
    fireEvent.change(screen.getByLabelText("节点名称"), { target: { value: "调用工具（已重新选择）" } });

    fireEvent.click(screen.getByRole("button", { name: "应用节点属性" }));
    await waitFor(() => expect(screen.getByTestId("node-node-tool")).toHaveTextContent("调用工具（已重新选择）"));
    fireEvent.click(screen.getByRole("button", { name: "保存草稿" }));

    await waitFor(() => expect(saveWorkflowDraft).toHaveBeenCalledTimes(1));
    expect(lastSavedPayload().nodes.find((node) => node.nodeId === "node-tool")?.config).toMatchObject({
      toolKey: "tool-new"
    });
  });

  it("keeps current bound external agent value and lets user pick a later-page option", async () => {
    vi.mocked(getWorkflowDraft).mockResolvedValue(
      createWorkflowDraft({
        nodes: [
          createNode({ nodeId: "node-start", type: "START", name: "开始" }),
          createNode({
            nodeId: "node-external",
            type: "EXTERNAL_AGENT",
            name: "调用外部 Agent",
            config: {
              adapterKey: "legacy-adapter"
            }
          }),
          createNode({ nodeId: "node-end", type: "END", name: "结束" })
        ],
        edges: [
          createEdge({ edgeId: "edge-start-external", sourceNodeId: "node-start", targetNodeId: "node-external" }),
          createEdge({ edgeId: "edge-external-end", sourceNodeId: "node-external", targetNodeId: "node-end" })
        ]
      })
    );
    vi.mocked(listExternalAgents)
      .mockResolvedValueOnce({
        items: [
          { adapterKey: "page-one-adapter", name: "第一页适配器", status: "ENABLED" }
        ],
        page: 1,
        pageSize: 20,
        total: 21
      })
      .mockResolvedValueOnce({
        items: [
          { adapterKey: "adapter-new", name: "已启用适配器", status: "ENABLED" }
        ],
        page: 2,
        pageSize: 20,
        total: 21
      });

    renderWorkflowPage();
    fireEvent.click(await screen.findByTestId("node-node-external"));
    expect(screen.getByText("当前绑定：legacy-adapter（未加载详情或当前结果未命中）")).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText("节点名称"), { target: { value: "调用外部 Agent（保留旧值）" } });
    fireEvent.click(screen.getByRole("button", { name: "应用节点属性" }));
    fireEvent.click(screen.getByRole("button", { name: "保存草稿" }));

    await waitFor(() => expect(saveWorkflowDraft).toHaveBeenCalledTimes(1));
    expect(lastSavedPayload().nodes.find((node) => node.nodeId === "node-external")?.config).toMatchObject({
      adapterKey: "legacy-adapter"
    });

    vi.mocked(saveWorkflowDraft).mockClear();

    openSelect("external-agent-select");
    await waitFor(() => {
      expect(listExternalAgents).toHaveBeenNthCalledWith(1, expect.objectContaining({ page: 1, pageSize: 20, status: "ENABLED" }));
    });
    fireEvent.click(await screen.findByRole("button", { name: "加载下一页" }));
    await waitFor(() => {
      expect(listExternalAgents).toHaveBeenNthCalledWith(2, expect.objectContaining({ page: 2, pageSize: 20, status: "ENABLED" }));
    });
    selectDropdownOption("已启用适配器 (adapter-new)");
    await waitFor(() => expect(screen.getByTestId("external-agent-select")).toHaveTextContent("已启用适配器 (adapter-new)"));
    fireEvent.change(screen.getByLabelText("节点名称"), { target: { value: "调用外部 Agent（已重新选择）" } });

    fireEvent.click(screen.getByRole("button", { name: "应用节点属性" }));
    await waitFor(() => expect(screen.getByTestId("node-node-external")).toHaveTextContent("调用外部 Agent（已重新选择）"));
    fireEvent.click(screen.getByRole("button", { name: "保存草稿" }));

    await waitFor(() => expect(saveWorkflowDraft).toHaveBeenCalledTimes(1));
    expect(lastSavedPayload().nodes.find((node) => node.nodeId === "node-external")?.config).toMatchObject({
      adapterKey: "adapter-new"
    });
  });

  it("keeps current bound schema refs when user edits other fields", async () => {
    vi.mocked(getWorkflowDraft).mockResolvedValue(
      createWorkflowDraft({
        nodes: [
          createNode({
            nodeId: "node-start",
            type: "START",
            name: "开始",
            inputSchemaRef: {
              schemaKey: "legacy-input",
              version: 1
            },
            outputSchemaRef: {
              schemaKey: "legacy-output",
              version: 2
            }
          }),
          createNode({ nodeId: "node-end", type: "END", name: "结束" })
        ]
      })
    );

    renderWorkflowPage();
    fireEvent.click(await screen.findByTestId("node-node-start"));

    expect(screen.getByText("当前绑定：legacy-input / v1（未加载详情或当前结果未命中）")).toBeInTheDocument();
    expect(screen.getByText("当前绑定：legacy-output / v2（未加载详情或当前结果未命中）")).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText("节点名称"), { target: { value: "开始节点（保留 Schema 引用）" } });
    fireEvent.click(screen.getByRole("button", { name: "应用节点属性" }));
    fireEvent.click(screen.getByRole("button", { name: "保存草稿" }));

    await waitFor(() => expect(saveWorkflowDraft).toHaveBeenCalledTimes(1));
    const startNode = lastSavedPayload().nodes.find((node) => node.nodeId === "node-start");
    expect(startNode?.inputSchemaRef).toEqual({
      schemaKey: "legacy-input",
      version: 1
    });
    expect(startNode?.outputSchemaRef).toEqual({
      schemaKey: "legacy-output",
      version: 2
    });
  });

  it("loads a later-page input schema option and saves input schema ref", async () => {
    vi.mocked(getWorkflowDraft).mockResolvedValue(
      createWorkflowDraft({
        nodes: [
          createNode({ nodeId: "node-start", type: "START", name: "开始" }),
          createNode({ nodeId: "node-end", type: "END", name: "结束" })
        ]
      })
    );
    vi.mocked(listSchemas)
      .mockResolvedValueOnce({
        items: [
          createSchemaListItem({ id: 101, schemaKey: "input-page-one", version: 1, name: "第一页输入" })
        ],
        page: 1,
        pageSize: 20,
        total: 21
      })
      .mockResolvedValueOnce({
        items: [
          createSchemaListItem({ id: 102, schemaKey: "input-target", version: 3, name: "订单输入" })
        ],
        page: 2,
        pageSize: 20,
        total: 21
      });

    renderWorkflowPage();
    fireEvent.click(await screen.findByTestId("node-node-start"));

    openSelect("node-input-schema-select");
    await waitFor(() => {
      expect(listSchemas).toHaveBeenNthCalledWith(1, expect.objectContaining({ page: 1, pageSize: 20 }));
    });
    fireEvent.click(await screen.findByRole("button", { name: "加载下一页" }));
    await waitFor(() => {
      expect(listSchemas).toHaveBeenNthCalledWith(2, expect.objectContaining({ page: 2, pageSize: 20 }));
    });
    selectDropdownOption("订单输入 (input-target / v3 / ACTIVE)");
    await waitFor(() => expect(screen.getByTestId("node-input-schema-select")).toHaveTextContent("订单输入 (input-target / v3 / ACTIVE)"));
    fireEvent.change(screen.getByLabelText("节点名称"), { target: { value: "开始节点（已选择输入 Schema）" } });

    fireEvent.click(screen.getByRole("button", { name: "应用节点属性" }));
    await waitFor(() => expect(screen.getByTestId("node-node-start")).toHaveTextContent("开始节点（已选择输入 Schema）"));
    fireEvent.click(screen.getByRole("button", { name: "保存草稿" }));

    await waitFor(() => expect(saveWorkflowDraft).toHaveBeenCalledTimes(1));
    const startNode = lastSavedPayload().nodes.find((node) => node.nodeId === "node-start");
    expect(startNode?.inputSchemaRef).toEqual({
      schemaKey: "input-target",
      version: 3
    });
  });

  it("loads a later-page output schema option and saves output schema ref", async () => {
    vi.mocked(getWorkflowDraft).mockResolvedValue(
      createWorkflowDraft({
        nodes: [
          createNode({ nodeId: "node-start", type: "START", name: "开始" }),
          createNode({ nodeId: "node-end", type: "END", name: "结束" })
        ]
      })
    );
    vi.mocked(listSchemas)
      .mockResolvedValueOnce({
        items: [
          createSchemaListItem({ id: 201, schemaKey: "output-page-one", version: 1, name: "第一页输出" })
        ],
        page: 1,
        pageSize: 20,
        total: 21
      })
      .mockResolvedValueOnce({
        items: [
          createSchemaListItem({ id: 202, schemaKey: "output-target", version: 4, name: "订单输出" })
        ],
        page: 2,
        pageSize: 20,
        total: 21
      });

    renderWorkflowPage();
    fireEvent.click(await screen.findByTestId("node-node-start"));

    openSelect("node-output-schema-select");
    await waitFor(() => {
      expect(listSchemas).toHaveBeenNthCalledWith(1, expect.objectContaining({ page: 1, pageSize: 20 }));
    });
    fireEvent.click(await screen.findByRole("button", { name: "加载下一页" }));
    await waitFor(() => {
      expect(listSchemas).toHaveBeenNthCalledWith(2, expect.objectContaining({ page: 2, pageSize: 20 }));
    });
    selectDropdownOption("订单输出 (output-target / v4 / ACTIVE)");
    await waitFor(() => expect(screen.getByTestId("node-output-schema-select")).toHaveTextContent("订单输出 (output-target / v4 / ACTIVE)"));
    fireEvent.change(screen.getByLabelText("节点名称"), { target: { value: "开始节点（已选择输出 Schema）" } });

    fireEvent.click(screen.getByRole("button", { name: "应用节点属性" }));
    await waitFor(() => expect(screen.getByTestId("node-node-start")).toHaveTextContent("开始节点（已选择输出 Schema）"));
    fireEvent.click(screen.getByRole("button", { name: "保存草稿" }));

    await waitFor(() => expect(saveWorkflowDraft).toHaveBeenCalledTimes(1));
    const startNode = lastSavedPayload().nodes.find((node) => node.nodeId === "node-start");
    expect(startNode?.outputSchemaRef).toEqual({
      schemaKey: "output-target",
      version: 4
    });
  });

  it("opens historical version as readonly page", async () => {
    renderWithProviders(<WorkflowPage readonly />, {
      route: "/agents/1/workflow/versions/12",
      path: "/agents/:agentId/workflow/versions/:workflowVersionId"
    });

    expect(await screen.findByText("工作流历史版本")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "复制为新草稿" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "保存草稿" })).not.toBeInTheDocument();
  });
});

/**
 * 渲染工作流页面。
 */
function renderWorkflowPage() {
  return renderWithProviders(<WorkflowPage />, {
    route: "/agents/1/workflow",
    path: "/agents/:agentId/workflow"
  });
}

/**
 * 打开基于 Ant Design Select 的下拉框。
 *
 * @param testId 选择器测试标识
 */
function openSelect(testId: string) {
  const selectRoot = screen.getByTestId(testId);
  const selector = selectRoot.querySelector(".ant-select-selector");
  fireEvent.mouseDown(selector ?? selectRoot);
  fireEvent.click(selector ?? selectRoot);
}

/**
 * 点击下拉选项。
 *
 * @param label 选项文案
 */
function selectDropdownOption(label: string) {
  const option = Array.from(document.querySelectorAll<HTMLElement>(".ant-select-item-option"))
    .find((item) => item.textContent?.includes(label));
  if (!option) {
    throw new Error(`未找到下拉选项：${label}`);
  }
  fireEvent.mouseDown(option);
  fireEvent.click(option);
}

/**
 * 返回最近一次保存请求。
 *
 * @returns 最近一次保存请求体
 */
function lastSavedPayload() {
  const payload = vi.mocked(saveWorkflowDraft).mock.calls.at(-1)?.[1];
  if (!payload) {
    throw new Error("尚未捕获保存草稿请求。");
  }
  return payload;
}

/**
 * 判断远程分页查询是否已请求指定页码。
 *
 * @param queryMock 查询 mock
 * @param page 页码
 * @returns 命中时返回 true
 */
function hasPagedQueryCall(queryMock: ReturnType<typeof vi.fn>, page: number) {
  return queryMock.mock.calls.some(([query]) => query?.page === page);
}

/**
 * 返回节点属性面板中的高级配置 JSON 文本框。
 *
 * @returns 高级配置文本框
 */
function getNodeConfigEditor() {
  const editors = document.querySelectorAll<HTMLTextAreaElement>("textarea.json-editor-textarea");
  const configEditor = editors.item(2);
  if (!configEditor) {
    throw new Error("未找到节点高级配置 JSON 文本框。");
  }
  return configEditor;
}

/**
 * 创建空分页响应。
 *
 * @returns 空列表
 */
function emptyPage<T extends object>(): { items: T[]; page: number; pageSize: number; total: number } {
  return {
    items: [],
    page: 1,
    pageSize: 20,
    total: 0
  };
}

/**
 * 构造工作流草稿。
 *
 * @param overrides 覆盖值
 * @returns 工作流草稿
 */
function createWorkflowDraft(overrides: Partial<Schema["WorkflowDraftResult"]> = {}): Schema["WorkflowDraftResult"] {
  return {
    workflowVersionId: 11,
    agentId: 1,
    versionNo: 1,
    status: "DRAFT",
    nodes: [
      createNode({ nodeId: "node-start", type: "START", name: "开始" }),
      createNode({ nodeId: "node-end", type: "END", name: "结束", ui: { position: { x: 320, y: 0 } } })
    ],
    edges: [
      createEdge({ edgeId: "edge-1", sourceNodeId: "node-start", targetNodeId: "node-end" })
    ],
    runtimeOptions: {
      timeoutSeconds: 600,
      maxSteps: 30,
      maxAgentCallDepth: 3
    },
    referencedSchemaVersions: [],
    createdAt: "2026-06-04T00:00:00Z",
    updatedAt: "2026-06-04T00:00:00Z",
    ...overrides
  };
}

/**
 * 构造节点定义。
 *
 * @param overrides 覆盖值
 * @returns 节点定义
 */
function createNode(overrides: Partial<Schema["WorkflowNodeDefinition"]> & Pick<Schema["WorkflowNodeDefinition"], "nodeId" | "type" | "name">): Schema["WorkflowNodeDefinition"] {
  return {
    config: {},
    ui: { position: { x: 0, y: 0 } },
    ...overrides
  };
}

/**
 * 构造边定义。
 *
 * @param overrides 覆盖值
 * @returns 边定义
 */
function createEdge(
  overrides: Partial<Schema["WorkflowEdgeDefinition"]> & Pick<Schema["WorkflowEdgeDefinition"], "edgeId" | "sourceNodeId" | "targetNodeId">
): Schema["WorkflowEdgeDefinition"] {
  return {
    type: "NORMAL",
    ...overrides
  };
}

/**
 * 构造 Schema 列表项。
 *
 * @param overrides 覆盖值
 * @returns Schema 列表项
 */
function createSchemaListItem(
  overrides: Partial<Schema["SchemaListItem"]> & Pick<Schema["SchemaListItem"], "id" | "schemaKey" | "version" | "name">
): Schema["SchemaListItem"] {
  const { id, schemaKey, version, name, ...rest } = overrides;
  return {
    id,
    schemaKey,
    version,
    name,
    createdFrom: "USER_CREATED",
    status: "ACTIVE",
    locked: false,
    createdAt: "2026-06-04T00:00:00Z",
    updatedAt: "2026-06-04T00:00:00Z",
    ...rest
  };
}

/**
 * 构造模型供应项描述。
 *
 * @param overrides 覆盖值
 * @returns 模型供应项
 */
function createOfferingDescriptor(
  overrides: Partial<Schema["ModelOfferingDescriptor"]>
): Schema["ModelOfferingDescriptor"] {
  return {
    offeringId: 1,
    offeringKey: "openai.gpt_4_1_mini",
    providerKey: "openai",
    providerName: "OpenAI",
    modelKey: "gpt_4_1_mini",
    displayName: "GPT-4.1 Mini",
    upstreamModelName: "gpt-4.1-mini",
    status: "ENABLED",
    providerStatus: "ENABLED",
    selectable: true,
    description: "",
    ...overrides
  };
}
