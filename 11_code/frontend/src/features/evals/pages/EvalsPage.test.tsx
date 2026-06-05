import { fireEvent, screen, waitFor } from "@testing-library/react";
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
  getModelOfferingsByKeys,
  listEvalCases,
  listModelOfferings,
  listEvalRunHistory,
  listEvalRunResults,
  listEvalRuns,
  listEvalSuites,
  runEvalSuite,
  type Schema,
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
    getModelOfferingsByKeys: vi.fn(),
    listEvalCases: vi.fn(),
    listModelOfferings: vi.fn(),
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
    vi.mocked(listModelOfferings).mockResolvedValue({
      items: [],
      page: 1,
      pageSize: 20,
      total: 0
    });
    vi.mocked(getModelOfferingsByKeys).mockResolvedValue({
      items: [],
      missingKeys: []
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

  it("uses the paged model offering selector when creating scoreRule", async () => {
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

    renderEvalsPage();

    fireEvent.click(await screen.findByRole("button", { name: "详情" }));
    fireEvent.click(await screen.findByRole("button", { name: "创建用例" }));
    const scoreRuleCheckbox = screen.getByRole<HTMLInputElement>("checkbox", { name: "启用 LLM 辅助评分" });
    fireEvent.click(scoreRuleCheckbox);
    expect(scoreRuleCheckbox.checked).toBe(true);

    await screen.findByTestId("eval-score-rule-model-offering-select");
    openSelect("eval-score-rule-model-offering-select");
    await waitFor(() => {
      expect(hasPagedQueryCall(vi.mocked(listModelOfferings), 1)).toBe(true);
    });
    fireEvent.click(await screen.findByRole("button", { name: "加载下一页" }));
    await waitFor(() => {
      expect(hasPagedQueryCall(vi.mocked(listModelOfferings), 2)).toBe(true);
    });
    selectDropdownOption("OpenAI / GPT-4.1 Mini (gpt-4.1-mini)");

    fireEvent.change(screen.getByLabelText("用例编号"), { target: { value: "CASE-NEW" } });
    fireEvent.change(screen.getByLabelText("标题"), { target: { value: "新的评分用例" } });
    clickModalOkButton();

    await waitFor(() => {
      expect(lastMockCall(vi.mocked(createEvalCase))?.[0]).toBe(1);
      expect(lastMockCall(vi.mocked(createEvalCase))?.[1]).toEqual(expect.objectContaining({
        caseNo: "CASE-NEW",
        title: "新的评分用例",
        scoreRule: expect.objectContaining({
          enabled: true,
          modelOfferingKey: "openai.gpt_4_1_mini"
        })
      }));
    });
  }, 10000);
});

/**
 * 渲染 Eval 页面。
 */
function renderEvalsPage() {
  return renderWithProviders(<EvalsPage />, {
    route: "/evals",
    path: "/evals"
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
 * 点击当前可见弹窗的主确认按钮。
 */
function clickModalOkButton() {
  const buttons = Array.from(document.querySelectorAll<HTMLButtonElement>(".ant-modal .ant-btn-primary"));
  const button = buttons.at(-1);
  if (!button) {
    throw new Error("未找到弹窗确认按钮。");
  }
  fireEvent.click(button);
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
 * 读取 mock 最近一次调用参数。
 *
 * @param mockFn mock 函数
 * @returns 调用参数
 */
function lastMockCall(mockFn: ReturnType<typeof vi.fn>) {
  return mockFn.mock.calls.at(-1);
}

/**
 * 构造模型供应项描述。
 *
 * @param overrides 覆盖字段
 * @returns 描述对象
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
