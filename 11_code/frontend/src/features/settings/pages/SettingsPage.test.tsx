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
            settingKey: "agent.studio.runtime.default-llm-timeout-seconds",
            settingValue: "120",
            valueType: "INTEGER",
            editable: true,
            description: "默认 LLM 超时时间",
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

    expect(await screen.findByText("agent.studio.runtime.default-llm-timeout-seconds")).toBeInTheDocument();
    expect(screen.getByText("120")).toBeInTheDocument();
  });
});
