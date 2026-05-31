import { createBrowserRouter, Navigate } from "react-router-dom";
import { App } from "./App";
import { HomePage } from "../features/home/pages/HomePage";
import { AgentsPage } from "../features/agents/pages/AgentsPage";
import { WorkflowPage } from "../features/workflow/pages/WorkflowPage";
import { DebugPage } from "../features/workflow/pages/DebugPage";
import { RunsPage } from "../features/runs/pages/RunsPage";
import { SchemasPage } from "../features/schemas/pages/SchemasPage";
import { MethodsPage } from "../features/methods/pages/MethodsPage";
import { ToolsPage } from "../features/tools/pages/ToolsPage";
import { ExternalAgentsPage } from "../features/externalAgents/pages/ExternalAgentsPage";
import { EvalsPage } from "../features/evals/pages/EvalsPage";
import { SettingsPage } from "../features/settings/pages/SettingsPage";

/**
 * 应用路由配置。
 */
export const router = createBrowserRouter([
  {
    path: "/",
    element: <App />,
    children: [
      {
        index: true,
        element: <HomePage />
      },
      {
        path: "agents",
        element: <AgentsPage />
      },
      {
        path: "agents/:agentId/workflow",
        element: <WorkflowPage />
      },
      {
        path: "agents/:agentId/workflow/versions/:workflowVersionId",
        element: <WorkflowPage readonly />
      },
      {
        path: "agents/:agentId/debug",
        element: <DebugPage />
      },
      {
        path: "runs",
        element: <RunsPage />
      },
      {
        path: "runs/:runId",
        element: <RunsPage />
      },
      {
        path: "schemas",
        element: <SchemasPage />
      },
      {
        path: "methods",
        element: <MethodsPage />
      },
      {
        path: "tools",
        element: <ToolsPage />
      },
      {
        path: "external-agents",
        element: <ExternalAgentsPage />
      },
      {
        path: "evals",
        element: <EvalsPage />
      },
      {
        path: "eval-runs/:evalRunId",
        element: <EvalsPage />
      },
      {
        path: "settings",
        element: <SettingsPage />
      },
      {
        path: "*",
        element: <Navigate to="/" replace />
      }
    ]
  }
]);
