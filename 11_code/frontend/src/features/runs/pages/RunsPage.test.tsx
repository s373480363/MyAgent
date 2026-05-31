import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { RunDetailView } from "./RunsPage";

describe("RunDetailView", () => {
  it("renders NodeRun database id and passes it to eval case entry", () => {
    const onCreateCase = vi.fn();

    render(
      <RunDetailView
        detail={{
          runId: "RUN-1",
          agent: { agentId: 1, agentKey: "summary-agent", agentName: "摘要 Agent" },
          workflowVersion: { workflowVersionId: 12, versionNo: 2, status: "PUBLISHED" },
          runType: "DEBUG",
          status: "SUCCESS",
          nodeRuns: [
            {
              nodeRunId: 42,
              nodeId: "node-llm",
              nodeName: "LLM 节点",
              nodeType: "LLM",
              status: "SUCCESS",
              input: { prompt: "hello" },
              output: { text: "world" },
              schemaValidationResult: { valid: true },
              durationMs: 11
            }
          ],
          traceEvents: [],
          childRuns: []
        }}
        onOpenRun={vi.fn()}
        onOpenVersion={vi.fn()}
        onCreateCase={onCreateCase}
      />
    );

    expect(screen.getByText("LLM 节点")).toBeInTheDocument();
    expect(screen.getByText("42")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "生成验收用例" }));

    expect(onCreateCase).toHaveBeenCalledWith(expect.objectContaining({ nodeRunId: 42 }));
  });
});
