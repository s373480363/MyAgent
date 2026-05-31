import { fireEvent, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { WorkflowPage } from "./WorkflowPage";
import { renderWithProviders } from "../../../test/renderWithProviders";
import {
  getWorkflowDraft,
  getWorkflowVersion,
  listWorkflowVersions,
  saveWorkflowDraft,
  validateWorkflowDraft
} from "../../../api/domainApi";

vi.mock("reactflow", async () => {
  const React = await import("react");
  return {
    default: ({ nodes, children, onNodeClick }: { nodes: Array<{ id: string; data: { label: string } }>; children: React.ReactNode; onNodeClick?: (event: unknown, node: unknown) => void }) => (
      <div data-testid="react-flow">
        {nodes.map((node) => (
          <button key={node.id} type="button" onClick={(event) => onNodeClick?.(event, node)}>
            {node.data.label}
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
    getWorkflowDraft: vi.fn(),
    getWorkflowVersion: vi.fn(),
    listWorkflowVersions: vi.fn(),
    publishWorkflowDraft: vi.fn(),
    saveWorkflowDraft: vi.fn(),
    validateWorkflowDraft: vi.fn()
  };
});

describe("WorkflowPage", () => {
  beforeEach(() => {
    vi.mocked(getWorkflowDraft).mockResolvedValue(workflowDraft());
    vi.mocked(getWorkflowVersion).mockResolvedValue({ ...workflowDraft(), status: "HISTORY" });
    vi.mocked(listWorkflowVersions).mockResolvedValue({
      items: [{ workflowVersionId: 12, versionNo: 2, status: "PUBLISHED" }],
      page: 1,
      pageSize: 50,
      total: 1
    });
    vi.mocked(saveWorkflowDraft).mockResolvedValue(workflowDraft());
    vi.mocked(validateWorkflowDraft).mockResolvedValue({ valid: true, errors: [] });
  });

  it("renders canvas actions and saves draft with generated OpenAPI types", async () => {
    renderWithProviders(<WorkflowPage />, {
      route: "/agents/1/workflow",
      path: "/agents/:agentId/workflow"
    });

    expect(await screen.findByText("工作流设计器")).toBeInTheDocument();
    expect(screen.getByTestId("react-flow")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "保存草稿" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /校\s*验/ })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /发\s*布/ })).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "保存草稿" }));

    await waitFor(() => {
      expect(saveWorkflowDraft).toHaveBeenCalledWith(
        1,
        expect.objectContaining({
          nodes: expect.arrayContaining([expect.objectContaining({ nodeId: "node-start" })]),
          edges: expect.arrayContaining([expect.objectContaining({ edgeId: "edge-1" })]),
          runtimeOptions: expect.objectContaining({ timeoutSeconds: 600 })
        })
      );
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
 * 构造工作流草稿。
 *
 * @returns 工作流草稿
 */
function workflowDraft() {
  return {
    workflowVersionId: 11,
    agentId: 1,
    versionNo: 1,
    status: "DRAFT" as const,
    nodes: [
      {
        nodeId: "node-start",
        type: "START" as const,
        name: "开始",
        config: {},
        ui: { position: { x: 0, y: 0 } }
      },
      {
        nodeId: "node-end",
        type: "END" as const,
        name: "结束",
        config: {},
        ui: { position: { x: 320, y: 0 } }
      }
    ],
    edges: [
      {
        edgeId: "edge-1",
        sourceNodeId: "node-start",
        targetNodeId: "node-end",
        type: "NORMAL" as const
      }
    ],
    runtimeOptions: {
      timeoutSeconds: 600,
      maxSteps: 30,
      maxAgentCallDepth: 3
    },
    referencedSchemaVersions: [],
    createdAt: "2026-05-31T00:00:00Z",
    updatedAt: "2026-05-31T00:00:00Z"
  };
}
