import React from "react";
import ReactDOM from "react-dom/client";
import { RouterProvider } from "react-router-dom";
import { ConfigProvider } from "antd";
import { QueryClientProvider } from "@tanstack/react-query";
import "antd/dist/reset.css";
import zhCN from "antd/locale/zh_CN";
import { queryClient } from "./app/queryClient";
import { router } from "./app/router";
import "./styles/global.css";

/**
 * 前端应用入口。
 */
const rootElement = document.getElementById("root");

if (!rootElement) {
  throw new Error("未找到前端挂载节点。");
}

ReactDOM.createRoot(rootElement).render(
  <React.StrictMode>
    <ConfigProvider
      locale={zhCN}
      theme={{
        token: {
          colorPrimary: "#0f766e",
          colorInfo: "#0f766e",
          borderRadius: 10,
          fontFamily: "\"Segoe UI\", \"PingFang SC\", \"Microsoft YaHei\", sans-serif"
        }
      }}
    >
      <QueryClientProvider client={queryClient}>
        <RouterProvider router={router} />
      </QueryClientProvider>
    </ConfigProvider>
  </React.StrictMode>
);
