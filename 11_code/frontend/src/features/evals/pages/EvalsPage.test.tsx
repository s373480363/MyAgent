import { screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { EvalsPage, isArchivedEvalSuite, isReadOnlyEvalCase } from "./EvalsPage";
import { renderWithProviders } from "../../../test/renderWithProviders";
import {
  archiveEvalCase,
  archiveEvalSuite,
  confirmEvalCase,
  confirmEvalSuite,
  createEvalCase,
  createEvalSuite,
  getEvalRun,
  listEvalCases,
  listEvalRunHistory,
  listEvalRunResults,
  listEvalRuns,
  listEvalSuites,
  runEvalSuite,
  updateEvalCase,
  updateEvalSuite
} from "../../../api/domainApi";

vi.mock("../../../api/domainApi", async (importOriginal) => {
  const actual = await importOriginal<typeof import("../../../api/domainApi")>();
  return {
    ...actual,
    archiveEvalCase: vi.fn(),
    archiveEvalSuite: vi.fn(),
    confirmEvalCase: vi.fn(),
    confirmEvalSuite: vi.fn(),
    createEvalCase: vi.fn(),
    createEvalSuite: vi.fn(),
    getEvalRun: vi.fn(),
    listEvalCases: vi.fn(),
    listEvalRunHistory: vi.fn(),
    listEvalRunResults: vi.fn(),
    listEvalRuns: vi.fn(),
    listEvalSuites: vi.fn(),
    runEvalSuite: vi.fn(),
    updateEvalCase: vi.fn(),
    updateEvalSuite: vi.fn()
  };
});

describe("EvalsPage", () => {
  beforeEach(() => {
    vi.mocked(listEvalSuites).mockResolvedValue({
      items: [
        {
          suiteId: 1,
          agentId: 1,
          workflowVersionId: 12,
          nodeId: "node-llm",
          name: "LLM 节点验收",
          goal: "验证摘要质量",
          passThreshold: 80,
          status: "CONFIRMED"
        }
      ],
      page: 1,
      pageSize: 20,
      total: 1
    });
    vi.mocked(listEvalCases).mockResolvedValue({
      items: [],
      page: 1,
      pageSize: 50,
      total: 0
    });
    vi.mocked(listEvalRuns).mockResolvedValue({
      items: [],
      page: 1,
      pageSize: 20,
      total: 0
    });
    vi.mocked(listEvalRunHistory).mockResolvedValue({
      items: [],
      page: 1,
      pageSize: 20,
      total: 0
    });
    vi.mocked(getEvalRun).mockResolvedValue({
      evalRunId: "EV-1",
      runId: "RUN-EVAL-1",
      suite: { suiteId: 1, name: "LLM 节点验收" },
      workflowVersion: { workflowVersionId: 12, versionNo: 2 },
      status: "FAILED",
      passThreshold: 80,
      passRate: 50,
      totalCaseCount: 2,
      passedCaseCount: 1,
      failedCaseCount: 1,
      criticalFailedCaseCount: 1,
      summary: "关键断言失败",
      historyComparison: {
        previousEvalRunId: "EV-0",
        previousRunId: "RUN-EVAL-0",
        previousPassRate: 100,
        passRateDelta: -50
      },
      failureSummary: [
        {
          caseId: 7,
          caseNo: "CASE-7",
          title: "关键摘要",
          critical: true,
          reason: "摘要缺少关键字段"
        }
      ]
    });
    vi.mocked(listEvalRunResults).mockResolvedValue({
      items: [
        {
          caseId: 7,
          caseNo: "CASE-7",
          title: "关键摘要",
          confirmStatus: "USER_CONFIRMED",
          critical: true,
          passed: false,
          input: { topic: "测试" },
          output: { text: "不完整" },
          assertionResults: [{ type: "JSON_PATH", passed: false, message: "缺少字段" }],
          scoreResult: { score: 0.3 },
          errorMessage: "摘要缺少关键字段",
          durationMs: 12
        }
      ],
      page: 1,
      pageSize: 50,
      total: 1
    });
    vi.mocked(archiveEvalCase).mockResolvedValue({});
    vi.mocked(archiveEvalSuite).mockResolvedValue({});
    vi.mocked(confirmEvalCase).mockResolvedValue({});
    vi.mocked(confirmEvalSuite).mockResolvedValue({});
    vi.mocked(createEvalCase).mockResolvedValue({});
    vi.mocked(createEvalSuite).mockResolvedValue({});
    vi.mocked(runEvalSuite).mockResolvedValue({});
    vi.mocked(updateEvalCase).mockResolvedValue({});
    vi.mocked(updateEvalSuite).mockResolvedValue({});
  });

  it("renders eval run detail, result item and history summary fields", async () => {
    renderWithProviders(<EvalsPage />, {
      route: "/eval-runs/EV-1",
      path: "/eval-runs/:evalRunId"
    });

    expect(await screen.findByText("节点验收")).toBeInTheDocument();
    expect(await screen.findByText("验收运行：EV-1")).toBeInTheDocument();
    expect(screen.getByText("RUN-EVAL-1")).toBeInTheDocument();
    expect(screen.getByText("CASE-7")).toBeInTheDocument();
    expect(screen.getByText("关键摘要")).toBeInTheDocument();
    expect(screen.getByText("摘要缺少关键字段")).toBeInTheDocument();
  });

  it("closes archived suite and archived case mutation actions", async () => {
    expect(isArchivedEvalSuite("ARCHIVED")).toBe(true);
    expect(isArchivedEvalSuite("CONFIRMED")).toBe(false);
    expect(isReadOnlyEvalCase("ARCHIVED", "USER_CREATED")).toBe(true);
    expect(isReadOnlyEvalCase("CONFIRMED", "ARCHIVED")).toBe(true);
    expect(isReadOnlyEvalCase("CONFIRMED", "USER_CONFIRMED")).toBe(false);
  });
});
