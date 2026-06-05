import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { ModelOfferingSelect } from "./ModelOfferingSelect";
import { getModelOfferingsByKeys, listModelOfferings, type Schema } from "../../api/domainApi";

vi.mock("../../api/domainApi", async (importOriginal) => {
  const actual = await importOriginal<typeof import("../../api/domainApi")>();
  return {
    ...actual,
    getModelOfferingsByKeys: vi.fn(),
    listModelOfferings: vi.fn()
  };
});

describe("ModelOfferingSelect", () => {
  it("keeps current bound unavailable value and supports paged selection", async () => {
    const handleChange = vi.fn();
    vi.mocked(getModelOfferingsByKeys).mockResolvedValue({
      items: [
        createOfferingDescriptor({
          offeringId: 1,
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
    vi.mocked(listModelOfferings)
      .mockResolvedValue({
        items: [],
        page: 1,
        pageSize: 20,
        total: 0
      })
      .mockResolvedValueOnce({
        items: [
          createOfferingDescriptor({
            offeringId: 2,
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
            offeringId: 3,
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

    render(
      <ModelOfferingSelect
        value="openai.legacy"
        onChange={handleChange}
        dataTestId="model-offering-select"
      />
    );

    await waitFor(() => {
      expect(getModelOfferingsByKeys).toHaveBeenCalledWith(["openai.legacy"]);
    });
    await waitFor(() => {
      expect(screen.getByTestId("model-offering-select")).toHaveTextContent("当前绑定：OpenAI / 旧模型 (gpt-4.1)（供应项已停用）");
    });

    openSelect("model-offering-select");
    await waitFor(() => {
      expect(hasPagedQueryCall(vi.mocked(listModelOfferings), 1)).toBe(true);
    });
    fireEvent.click(await screen.findByRole("button", { name: "加载下一页" }));
    await waitFor(() => {
      expect(hasPagedQueryCall(vi.mocked(listModelOfferings), 2)).toBe(true);
    });
    selectDropdownOption("OpenAI / GPT-4.1 Mini (gpt-4.1-mini)");

    expect(handleChange).toHaveBeenCalledWith("openai.gpt_4_1_mini");
  }, 10000);
});

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
