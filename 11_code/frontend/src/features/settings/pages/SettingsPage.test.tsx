import { App as AntdApp } from "antd";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { SettingsPage } from "./SettingsPage";

describe("SettingsPage", () => {
  beforeEach(() => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      headers: new Headers({ "content-type": "application/json" }),
      json: async () => ({
        success: true,
        data: [
          {
            settingKey: "myagent.openai.default-model",
            settingValue: "gpt-4.1-mini",
            valueType: "STRING",
            editable: true,
            description: "默认模型",
            source: "SYSTEM_SETTING"
          }
        ]
      })
    }));
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("renders settings returned by backend whitelist API", async () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } }
    });

    render(
      <QueryClientProvider client={queryClient}>
        <AntdApp>
          <SettingsPage />
        </AntdApp>
      </QueryClientProvider>
    );

    expect(await screen.findByText("myagent.openai.default-model")).toBeInTheDocument();
    expect(screen.getByText("gpt-4.1-mini")).toBeInTheDocument();
  });
});
