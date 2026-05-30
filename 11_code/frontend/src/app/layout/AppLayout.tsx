import { PropsWithChildren } from "react";
import { Layout, Menu, Typography } from "antd";
import {
  ApartmentOutlined,
  ApiOutlined,
  AppstoreOutlined,
  BranchesOutlined,
  DatabaseOutlined,
  ExperimentOutlined,
  PlayCircleOutlined,
  RobotOutlined,
  SettingOutlined
} from "@ant-design/icons";
import { useLocation, useNavigate } from "react-router-dom";

const { Header, Sider, Content } = Layout;

const menuItems = [
  { key: "/", icon: <AppstoreOutlined />, label: "概览" },
  { key: "/agents", icon: <RobotOutlined />, label: "Agents" },
  { key: "/runs", icon: <PlayCircleOutlined />, label: "运行记录" },
  { key: "/schemas", icon: <DatabaseOutlined />, label: "Schemas" },
  { key: "/methods", icon: <ApiOutlined />, label: "Java 方法" },
  { key: "/tools", icon: <ApartmentOutlined />, label: "工具目录" },
  { key: "/external-agents", icon: <BranchesOutlined />, label: "外部 Agent" },
  { key: "/evals", icon: <ExperimentOutlined />, label: "节点验收" },
  { key: "/settings", icon: <SettingOutlined />, label: "系统设置" }
];

/**
 * 应用全局布局。
 *
 * @param props 子组件
 * @returns 页面布局
 */
export function AppLayout({ children }: PropsWithChildren) {
  const location = useLocation();
  const navigate = useNavigate();

  return (
    <Layout className="app-shell">
      <Sider width={240} theme="light" className="app-sider">
        <div className="brand-panel">
          <Typography.Text className="brand-kicker">V1 开发骨架</Typography.Text>
          <Typography.Title level={4} className="brand-title">
            MyAgent 管理台
          </Typography.Title>
          <Typography.Paragraph className="brand-desc">
            当前阶段先完成工程底座、公共基线和模块入口，后续业务能力将按实施方案逐步接入。
          </Typography.Paragraph>
        </div>
        <Menu
          mode="inline"
          selectedKeys={[resolveMenuKey(location.pathname)]}
          items={menuItems}
          onClick={(event) => navigate(event.key)}
        />
      </Sider>
      <Layout>
        <Header className="app-header">
          <div>
            <Typography.Text className="header-label">当前路径</Typography.Text>
            <Typography.Title level={5} className="header-title">
              {location.pathname}
            </Typography.Title>
          </div>
        </Header>
        <Content className="app-content">{children}</Content>
      </Layout>
    </Layout>
  );
}

/**
 * 根据当前路径计算菜单高亮键。
 *
 * @param pathname 当前路径
 * @returns 菜单键
 */
function resolveMenuKey(pathname: string) {
  if (pathname.startsWith("/agents/") && pathname.includes("/workflow")) {
    return "/agents";
  }
  const item = menuItems.find((menuItem) => menuItem.key !== "/" && pathname.startsWith(menuItem.key));
  return item?.key ?? "/";
}
