import { fireEvent, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ModelProvidersPage } from "./ModelProvidersPage";
import { renderWithProviders } from "../../../test/renderWithProviders";
import {
  changeModelProviderStatus,
  createModelProvider,
  getModelProvider,
  listModelOfferings,
  listModelProviders
} from "../../../api/domainApi";

vi.mock("../../../api/domainApi", async (importOriginal) => {
  const actual = await importOriginal<typeof import("../../../api/domainApi")>();
  return {
    ...actual,
    changeModelOfferingStatus: vi.fn(),
    changeModelProviderStatus: vi.fn(),
    createModelOffering: vi.fn(),
    createModelProvider: vi.fn(),
    getModelProvider: vi.fn(),
    listModelOfferings: vi.fn(),
    listModelProviders: vi.fn(),
    testModelProvider: vi.fn(),
    updateModelOffering: vi.fn(),
    updateModelProvider: vi.fn(),
    updateModelProviderSecrets: vi.fn()
  };
});

describe("ModelProvidersPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(listModelProviders).mockResolvedValue({
      items: [
        {
          providerId: 1,
          providerKey: "openai",
          name: "OpenAI",
          providerType: "OPENAI_COMPATIBLE",
          baseUrl: "https://api.openai.com",
          apiKeyConfigured: true,
          apiKeyMask: "sk-...abcd",
          status: "ENABLED",
          description: "正式供应商"
        }
      ],
      page: 1,
      pageSize: 20,
      total: 1
    });
    vi.mocked(getModelProvider).mockResolvedValue({
      providerId: 1,
      providerKey: "openai",
      name: "OpenAI",
      providerType: "OPENAI_COMPATIBLE",
      baseUrl: "https://api.openai.com",
      apiKeyConfigured: true,
      apiKeyMask: "sk-...abcd",
      status: "ENABLED",
      description: "正式供应商"
    });
    vi.mocked(listModelOfferings).mockResolvedValue({
      items: [
        {
          offeringId: 11,
          offeringKey: "openai.gpt_4_1_mini",
          providerKey: "openai",
          providerName: "OpenAI",
          modelKey: "gpt_4_1_mini",
          displayName: "GPT-4.1 Mini",
          upstreamModelName: "gpt-4.1-mini",
          defaultTemperature: 0.2,
          status: "ENABLED",
          providerStatus: "ENABLED",
          selectable: true,
          description: "默认供应项"
        }
      ],
      page: 1,
      pageSize: 50,
      total: 1
    });
    vi.mocked(createModelProvider).mockResolvedValue({
      providerId: 2,
      providerKey: "siliconflow",
      name: "硅基流动",
      providerType: "OPENAI_COMPATIBLE",
      baseUrl: "https://api.siliconflow.cn",
      apiKeyConfigured: true,
      apiKeyMask: "sk-...1234",
      status: "ENABLED",
      description: "新供应商"
    });
    vi.mocked(changeModelProviderStatus).mockResolvedValue({
      providerId: 1,
      providerKey: "openai",
      name: "OpenAI",
      providerType: "OPENAI_COMPATIBLE",
      baseUrl: "https://api.openai.com",
      apiKeyConfigured: true,
      apiKeyMask: "sk-...abcd",
      status: "DISABLED",
      description: "正式供应商"
    });
  });

  it("submits provider creation through the formal API and keeps apiKey write-only", async () => {
    renderProvidersPage();

    fireEvent.click(await screen.findByRole("button", { name: "创建模型供应商" }));
    expect(screen.getByText("初始 API Key")).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText("Provider Key"), { target: { value: "siliconflow" } });
    fireEvent.change(screen.getByLabelText("名称"), { target: { value: "硅基流动" } });
    fireEvent.change(screen.getByLabelText("Base URL"), { target: { value: "https://api.siliconflow.cn" } });
    fireEvent.change(screen.getByLabelText("初始 API Key"), { target: { value: "sk-live-secret" } });
    clickModalOkButton();

    await waitFor(() => {
      expect(lastMockCall(vi.mocked(createModelProvider))?.[0]).toEqual({
        providerKey: "siliconflow",
        providerType: "OPENAI_COMPATIBLE",
        name: "硅基流动",
        baseUrl: "https://api.siliconflow.cn",
        apiKey: "sk-live-secret",
        description: undefined
      });
    });
  });

  it("loads provider detail with masked key and offering list", async () => {
    renderProvidersPage();

    fireEvent.click(await screen.findByRole("button", { name: "详情" }));
    expect(await screen.findByText("sk-...abcd")).toBeInTheDocument();
    expect(screen.queryByText("sk-live-secret")).not.toBeInTheDocument();
    expect(await screen.findByText("GPT-4.1 Mini")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "创建模型供应项" })).toBeInTheDocument();
  }, 10000);
});

/**
 * 渲染模型供应商页面。
 */
function renderProvidersPage() {
  return renderWithProviders(<ModelProvidersPage />);
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
 * 读取 mock 最近一次调用参数。
 *
 * @param mockFn mock 函数
 * @returns 调用参数
 */
function lastMockCall(mockFn: ReturnType<typeof vi.fn>) {
  return mockFn.mock.calls.at(-1);
}
