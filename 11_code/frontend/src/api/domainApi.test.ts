import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  createEvalCaseFromNodeRun,
  getWorkflowVersion,
  listRuns,
  runDebugAgent,
  validateWorkflowDraft
} from "./domainApi";

describe("domainApi", () => {
  beforeEach(() => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      headers: new Headers({ "content-type": "application/json" }),
      json: async () => ({ success: true, data: {} })
    }));
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("uses frozen workflow and debug endpoint paths", async () => {
    await validateWorkflowDraft(1);
    await getWorkflowVersion(1, 12);
    await runDebugAgent(1, { workflowVersionId: 12, input: { topic: "测试" } });

    const calls = fetchCalls();
    expect(calls[0][0]).toBe("/api/agents/1/workflow-draft/validate");
    expect(calls[0][1]).toMatchObject({ method: "POST" });
    expect(calls[1][0]).toBe("/api/agents/1/workflow-versions/12");
    expect(calls[2][0]).toBe("/api/agents/1/debug-runs");
    expect(calls[2][1]).toMatchObject({ method: "POST" });
  });

  it("uses node_run.id path when creating eval case from NodeRun", async () => {
    await createEvalCaseFromNodeRun(42, { suiteId: 1, title: "节点用例" });

    const [[url, options]] = fetchCalls();
    expect(url).toBe("/api/node-runs/42/eval-cases");
    expect(options).toMatchObject({ method: "POST" });
    expect(JSON.parse(String(options?.body))).toEqual({ suiteId: 1, title: "节点用例" });
  });

  it("keeps run list query parameters explicit", async () => {
    await listRuns({ page: 2, pageSize: 20, runType: "EVAL" });

    const [[url]] = fetchCalls();
    expect(url).toBe("/api/runs?page=2&pageSize=20&runType=EVAL");
  });
});

describe("domainApi explicit base url override", () => {
  afterEach(() => {
    vi.resetModules();
    vi.unstubAllEnvs();
    vi.unstubAllGlobals();
  });

  it("uses VITE_API_BASE_URL when explicitly configured", async () => {
    vi.stubEnv("VITE_API_BASE_URL", "http://127.0.0.1:8080");
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      headers: new Headers({ "content-type": "application/json" }),
      json: async () => ({ success: true, data: {} })
    }));

    vi.resetModules();
    const { listRuns: listRunsWithOverride } = await import("./domainApi");
    await listRunsWithOverride({ page: 1, pageSize: 10 });

    const calls = vi.mocked(fetch).mock.calls as Array<[string, RequestInit | undefined]>;
    expect(calls[0][0]).toBe("http://127.0.0.1:8080/api/runs?page=1&pageSize=10");
  });
});

/**
 * 获取 fetch 调用列表。
 *
 * @returns fetch 调用
 */
function fetchCalls() {
  return vi.mocked(fetch).mock.calls as Array<[string, RequestInit | undefined]>;
}
