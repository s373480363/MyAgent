import { fireEvent, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { AgentsPage } from "./AgentsPage";
import { renderWithProviders } from "../../../test/renderWithProviders";
import {
  createAgent,
  getAgent,
  getModelOfferingsByKeys,
  listAgents,
  listModelOfferings,
  listWorkflowVersions,
  updateAgent
} from "../../../api/domainApi";

vi.mock("../../../api/domainApi", async (importOriginal) => {
  const actual = await importOriginal<typeof import("../../../api/domainApi")>();
  return {
    ...actual,
    changeAgentStatus: vi.fn(),
    createAgent: vi.fn(),
    getAgent: vi.fn(),
    getModelOfferingsByKeys: vi.fn(),
    listAgents: vi.fn(),
    listModelOfferings: vi.fn(),
    listWorkflowVersions: vi.fn(),
    updateAgent: vi.fn()
  };
});

describe("AgentsPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(listAgents).mockResolvedValue({
      items: [
        {
          agentId: 1,
          agentKey: "root-agent",
          name: "根 Agent",
          description: "用于文案测试",
          status: "ENABLED",
          currentDraftWorkflowVersionId: 11,
          currentPublishedWorkflowVersionId: 10
        }
      ],
      page: 1,
      pageSize: 20,
      total: 1
    });
    vi.mocked(getAgent).mockResolvedValue({
      agentId: 1,
      agentKey: "root-agent",
      status: "ENABLED",
      name: "根 Agent",
      description: "用于文案测试",
      systemPrompt: "你是默认提示词。",
      defaultModelOfferingKey: "openai.legacy",
      temperature: 0.2,
      timeoutSeconds: 600,
      maxSteps: 30,
      currentDraftWorkflow: {
        workflowVersionId: 11,
        versionNo: 3,
        status: "DRAFT"
      },
      currentPublishedWorkflow: {
        workflowVersionId: 10,
        versionNo: 2,
        status: "PUBLISHED"
      },
      historyVersionSummary: {
        total: 2,
        latestWorkflowVersionId: 11
      },
      updatedAt: "2026-06-04T00:00:00Z"
    });
    vi.mocked(listWorkflowVersions).mockResolvedValue({
      items: [],
      page: 1,
      pageSize: 20,
      total: 0
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
    vi.mocked(createAgent).mockResolvedValue({
      agentId: 2,
      agentKey: "new-agent",
      status: "ENABLED",
      name: "新 Agent",
      temperature: 0.2,
      timeoutSeconds: 600,
      maxSteps: 30,
      historyVersionSummary: {
        total: 0
      },
      updatedAt: "2026-06-04T00:00:00Z"
    });
    vi.mocked(updateAgent).mockResolvedValue({
      agentId: 1,
      agentKey: "root-agent",
      status: "ENABLED",
      name: "根 Agent",
      temperature: 0.2,
      timeoutSeconds: 600,
      maxSteps: 30,
      historyVersionSummary: {
        total: 2,
        latestWorkflowVersionId: 11
      },
      updatedAt: "2026-06-04T00:00:00Z"
    });
  });

  it("uses paged model offering selector in the create form and saves defaultModelOfferingKey", async () => {
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

    renderAgentsPage();

    fireEvent.click(await screen.findByRole("button", { name: "创建 Agent" }));

    expect(screen.getByText("LLM 节点默认模型供应项")).toBeInTheDocument();
    openSelect("agent-default-model-offering-select");
    await waitFor(() => {
      expect(hasPagedQueryCall(vi.mocked(listModelOfferings), 1)).toBe(true);
    });
    fireEvent.click(await screen.findByRole("button", { name: "加载下一页" }));
    await waitFor(() => {
      expect(hasPagedQueryCall(vi.mocked(listModelOfferings), 2)).toBe(true);
    });
    selectDropdownOption("OpenAI / GPT-4.1 Mini (gpt-4.1-mini)");

    fireEvent.change(screen.getByLabelText("Agent Key"), { target: { value: "new-agent" } });
    fireEvent.change(screen.getByLabelText("名称"), { target: { value: "新 Agent" } });
    clickModalOkButton();

    await waitFor(() => {
      expect(lastMockCall(vi.mocked(createAgent))?.[0]).toEqual(expect.objectContaining({
        agentKey: "new-agent",
        name: "新 Agent",
        defaultModelOfferingKey: "openai.gpt_4_1_mini"
      }));
    });
  });

  it("keeps the model offering field in the detail drawer and allows clearing it on edit", async () => {
    vi.mocked(getModelOfferingsByKeys).mockResolvedValue({
      items: [
        createOfferingDescriptor({
          offeringId: 201,
          offeringKey: "openai.legacy",
          providerKey: "openai",
          providerName: "OpenAI",
          modelKey: "gpt_4_1",
          displayName: "旧默认模型",
          upstreamModelName: "gpt-4.1"
        })
      ],
      missingKeys: []
    });

    renderAgentsPage();

    fireEvent.click(await screen.findByRole("button", { name: "详情" }));
    expect(await screen.findByText("LLM 节点默认模型供应项")).toBeInTheDocument();
    expect(screen.getByText("openai.legacy")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "编辑基础信息" }));
    await waitFor(() => {
      expect(getModelOfferingsByKeys).toHaveBeenCalledWith(["openai.legacy"]);
    });
    await waitFor(() => {
      expect(screen.getByTestId("agent-default-model-offering-select")).toHaveTextContent("OpenAI / 旧默认模型 (gpt-4.1)");
    });

    clearSelect("agent-default-model-offering-select");
    clickModalOkButton();

    await waitFor(() => {
      expect(lastMockCall(vi.mocked(updateAgent))?.[0]).toBe(1);
      expect(lastMockCall(vi.mocked(updateAgent))?.[1]).toEqual(expect.objectContaining({
        defaultModelOfferingKey: undefined
      }));
    });
  });
});

/**
 * 渲染 Agent 页面。
 */
function renderAgentsPage() {
  return renderWithProviders(<AgentsPage />, {
    route: "/agents",
    path: "/agents"
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
 * 清空选择器当前值。
 *
 * @param testId 选择器测试标识
 */
function clearSelect(testId: string) {
  const selectRoot = screen.getByTestId(testId);
  const clearButton = selectRoot.querySelector(".ant-select-clear");
  if (!clearButton) {
    throw new Error(`未找到清空按钮：${testId}`);
  }
  fireEvent.mouseDown(clearButton);
  fireEvent.click(clearButton);
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
  overrides: Partial<import("../../../api/domainApi").Schema["ModelOfferingDescriptor"]>
): import("../../../api/domainApi").Schema["ModelOfferingDescriptor"] {
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
