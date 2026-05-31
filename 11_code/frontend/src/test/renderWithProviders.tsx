import { App as AntdApp } from "antd";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, type RenderResult } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import type { ReactElement } from "react";

/**
 * 使用业务页面所需 Provider 渲染组件。
 *
 * @param ui 被测组件
 * @param options 路由配置
 * @returns Testing Library 渲染结果和 QueryClient
 */
export function renderWithProviders(
  ui: ReactElement,
  options: { route?: string; path?: string } = {}
): RenderResult & { queryClient: QueryClient } {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false }
    }
  });
  const route = options.route ?? "/";
  const path = options.path ?? "/";
  const result = render(
    <QueryClientProvider client={queryClient}>
      <AntdApp>
        <MemoryRouter initialEntries={[route]}>
          <Routes>
            <Route path={path} element={ui} />
          </Routes>
        </MemoryRouter>
      </AntdApp>
    </QueryClientProvider>
  );

  return { ...result, queryClient };
}
