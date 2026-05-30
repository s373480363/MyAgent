import { App as AntdApp } from "antd";
import { Outlet } from "react-router-dom";
import { AppLayout } from "./layout/AppLayout";

/**
 * 应用根组件。
 */
export function App() {
  return (
    <AntdApp>
      <AppLayout>
        <Outlet />
      </AppLayout>
    </AntdApp>
  );
}
