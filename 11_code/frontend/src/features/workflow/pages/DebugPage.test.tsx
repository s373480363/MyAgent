import { fireEvent, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { DebugPage } from "./DebugPage";
import { renderWithProviders } from "../../../test/renderWithProviders";
import { getRunDetail, listWorkflowVersions, runDebugAgent } from "../../../api/domainApi";

vi.mock("../../../api/domainApi", async (importOriginal) => {
  const actual = await importOriginal<typeof import("../../../api/domainApi")>();
  return {
    ...actual,
    getRunDetail: vi.fn(),
    listWorkflowVersions: vi.fn(),
    runDebugAgent: vi.fn()
  };
});

describe("DebugPage", () => {
  beforeEach(() => {
    vi.mocked(listWorkflowVersions).mockResolvedValue({
      items: [{ workflowVersionId: 12, versionNo: 2, status: "PUBLISHED" }],
      page: 1,
      pageSize: 50,
      total: 1
    });
    vi.mocked(runDebugAgent).mockResolvedValue({
      runId: "RUN-DEBUG-1",
      agentKey: "summary-agent",
      workflowVersionId: 12,
      status: "SUCCESS",
      output: { text: "ok" },
      durationMs: 24
    });
    vi.mocked(getRunDetail).mockResolvedValue({
      runId: "RUN-DEBUG-1",
      workflowVersion: { workflowVersionId: 12, versionNo: 2, status: "PUBLISHED" },
      status: "SUCCESS",
      nodeRuns: [],
      traceEvents: []
    });
  });

  it("starts debug run and shows bound workflowVersionId", async () => {
    renderWithProviders(<DebugPage />, {
      route: "/agents/1/debug",
      path: "/agents/:agentId/debug"
    });

    expect(await screen.findByText("调试运行")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "发起调试运行" }));

    await waitFor(() => {
      expect(runDebugAgent).toHaveBeenCalledWith(1, { workflowVersionId: undefined, input: undefined });
    });
    expect(await screen.findByText("RUN-DEBUG-1")).toBeInTheDocument();
    expect(screen.getByText("12")).toBeInTheDocument();
  });
});
