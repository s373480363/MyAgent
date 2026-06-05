import { request } from "./httpClient";
import type { components } from "./generated/schema";

export type Schema = components["schemas"];

/**
 * 前端分页查询参数。
 */
export type PageQuery = Record<string, string | number | boolean | undefined>;

/**
 * 查询系统设置。
 */
export function listSettings() {
  return request<Schema["SettingItemResult"][]>("/api/settings");
}

/**
 * 更新系统设置。
 *
 * @param items 设置项
 */
export function updateSettings(items: Schema["UpdateSettingsItemRequest"][]) {
  return request<Schema["SettingItemResult"][]>("/api/settings", {
    method: "PUT",
    body: JSON.stringify({ items })
  });
}

/**
 * 查询运行列表。
 *
 * @param query 查询条件
 */
export function listRuns(query: PageQuery) {
  return request<Schema["PageResponseRunListItemResult"]>("/api/runs", { query });
}

/**
 * 发起正式运行。
 *
 * @param agentKey Agent 业务标识
 * @param body 请求体
 */
export function runAgent(agentKey: string, body: Schema["RunRequest"]) {
  return request<Schema["RunResult"]>(`/api/agents/${encodeURIComponent(agentKey)}/runs`, {
    method: "POST",
    body: JSON.stringify(body)
  });
}

/**
 * 查询运行详情。
 *
 * @param runId 运行编号
 */
export function getRunDetail(runId: string) {
  return request<Schema["RunDetailResult"]>(`/api/runs/${encodeURIComponent(runId)}`);
}

/**
 * 查询模型供应商列表。
 *
 * @param query 查询条件
 */
export function listModelProviders(query: PageQuery) {
  return request<Schema["PageResponseModelProviderResult"]>("/api/model-providers", { query });
}

/**
 * 创建模型供应商。
 *
 * @param body 请求体
 */
export function createModelProvider(body: Schema["CreateModelProviderRequest"]) {
  return request<Schema["ModelProviderResult"]>("/api/model-providers", {
    method: "POST",
    body: JSON.stringify(body)
  });
}

/**
 * 查询模型供应商详情。
 *
 * @param providerId 供应商主键
 */
export function getModelProvider(providerId: number) {
  return request<Schema["ModelProviderResult"]>(`/api/model-providers/${providerId}`);
}

/**
 * 更新模型供应商。
 *
 * @param providerId 供应商主键
 * @param body 请求体
 */
export function updateModelProvider(providerId: number, body: Schema["UpdateModelProviderRequest"]) {
  return request<Schema["ModelProviderResult"]>(`/api/model-providers/${providerId}`, {
    method: "PUT",
    body: JSON.stringify(body)
  });
}

/**
 * 更新模型供应商状态。
 *
 * @param providerId 供应商主键
 * @param status 目标状态
 */
export function changeModelProviderStatus(providerId: number, status: Schema["ChangeModelProviderStatusRequest"]["status"]) {
  return request<Schema["ModelProviderResult"]>(`/api/model-providers/${providerId}/status`, {
    method: "PUT",
    body: JSON.stringify({ status })
  });
}

/**
 * 更新模型供应商密钥。
 *
 * @param providerId 供应商主键
 * @param body 请求体
 */
export function updateModelProviderSecrets(providerId: number, body: Schema["UpdateModelProviderSecretsRequest"]) {
  return request<Schema["ModelProviderResult"]>(`/api/model-providers/${providerId}/secrets`, {
    method: "PUT",
    body: JSON.stringify(body)
  });
}

/**
 * 测试模型供应商连接。
 *
 * @param providerId 供应商主键
 * @param body 请求体
 */
export function testModelProvider(providerId: number, body: Schema["TestModelProviderRequest"]) {
  return request<Schema["ModelProviderTestResult"]>(`/api/model-providers/${providerId}/test`, {
    method: "POST",
    body: JSON.stringify(body)
  });
}

/**
 * 查询模型供应项列表。
 *
 * @param query 查询条件
 */
export function listModelOfferings(query: PageQuery) {
  return request<Schema["PageResponseModelOfferingDescriptor"]>("/api/model-offerings", { query });
}

/**
 * 查询单个模型供应项详情。
 *
 * @param offeringKey 供应项标识
 */
export function getModelOffering(offeringKey: string) {
  return request<Schema["ModelOfferingDescriptor"]>(`/api/model-offerings/${encodeURIComponent(offeringKey)}`);
}

/**
 * 按键批量查询模型供应项。
 *
 * @param offeringKeys 供应项标识列表
 */
export function getModelOfferingsByKeys(offeringKeys: string[]) {
  const queryString = offeringKeys
    .map((offeringKey) => `offeringKeys=${encodeURIComponent(offeringKey)}`)
    .join("&");
  return request<Schema["ModelOfferingBatchResult"]>(`/api/model-offerings/by-keys${queryString ? `?${queryString}` : ""}`);
}

/**
 * 创建模型供应项。
 *
 * @param body 请求体
 */
export function createModelOffering(body: Schema["CreateModelOfferingRequest"]) {
  return request<Schema["ModelOfferingDescriptor"]>("/api/model-offerings", {
    method: "POST",
    body: JSON.stringify(body)
  });
}

/**
 * 更新模型供应项。
 *
 * @param offeringId 供应项主键
 * @param body 请求体
 */
export function updateModelOffering(offeringId: number, body: Schema["UpdateModelOfferingRequest"]) {
  return request<Schema["ModelOfferingDescriptor"]>(`/api/model-offerings/${offeringId}`, {
    method: "PUT",
    body: JSON.stringify(body)
  });
}

/**
 * 更新模型供应项状态。
 *
 * @param offeringId 供应项主键
 * @param status 目标状态
 */
export function changeModelOfferingStatus(offeringId: number, status: Schema["ChangeModelOfferingStatusRequest"]["status"]) {
  return request<Schema["ModelOfferingDescriptor"]>(`/api/model-offerings/${offeringId}/status`, {
    method: "PUT",
    body: JSON.stringify({ status })
  });
}

/**
 * 查询 Agent 列表。
 *
 * @param query 查询条件
 */
export function listAgents(query: PageQuery) {
  return request<Schema["PageResponseAgentListItemResult"]>("/api/agents", { query });
}

/**
 * 创建 Agent。
 *
 * @param body 请求体
 */
export function createAgent(body: Schema["CreateAgentRequest"]) {
  return request<Schema["AgentDetailResult"]>("/api/agents", {
    method: "POST",
    body: JSON.stringify(body)
  });
}

/**
 * 查询 Agent 详情。
 *
 * @param agentId Agent 主键
 */
export function getAgent(agentId: number) {
  return request<Schema["AgentDetailResult"]>(`/api/agents/${agentId}`);
}

/**
 * 更新 Agent。
 *
 * @param agentId Agent 主键
 * @param body 请求体
 */
export function updateAgent(agentId: number, body: Schema["UpdateAgentRequest"]) {
  return request<Schema["AgentDetailResult"]>(`/api/agents/${agentId}`, {
    method: "PUT",
    body: JSON.stringify(body)
  });
}

/**
 * 切换 Agent 状态。
 *
 * @param agentId Agent 主键
 * @param status 目标状态
 */
export function changeAgentStatus(agentId: number, status: Schema["ChangeAgentStatusRequest"]["status"]) {
  return request<Schema["AgentDetailResult"]>(`/api/agents/${agentId}/status`, {
    method: "PUT",
    body: JSON.stringify({ status })
  });
}

/**
 * 获取工作流草稿。
 *
 * @param agentId Agent 主键
 */
export function getWorkflowDraft(agentId: number) {
  return request<Schema["WorkflowDraftResult"]>(`/api/agents/${agentId}/workflow-draft`);
}

/**
 * 保存工作流草稿。
 *
 * @param agentId Agent 主键
 * @param body 请求体
 */
export function saveWorkflowDraft(agentId: number, body: Schema["SaveWorkflowDraftRequest"]) {
  return request<Schema["WorkflowDraftResult"]>(`/api/agents/${agentId}/workflow-draft`, {
    method: "PUT",
    body: JSON.stringify(body)
  });
}

/**
 * 发布工作流草稿。
 *
 * @param agentId Agent 主键
 */
export function publishWorkflowDraft(agentId: number, body: Schema["PublishWorkflowDraftRequest"] = {}) {
  return request<Schema["WorkflowPublishResult"]>(`/api/agents/${agentId}/workflow-draft/publish`, {
    method: "POST",
    body: JSON.stringify(body)
  });
}

/**
 * 校验工作流草稿。
 *
 * @param agentId Agent 主键
 */
export function validateWorkflowDraft(agentId: number) {
  return request<Schema["WorkflowValidationResult"]>(`/api/agents/${agentId}/workflow-draft/validate`, {
    method: "POST",
    body: JSON.stringify({})
  });
}

/**
 * 从历史版本复制生成新草稿。
 *
 * @param agentId Agent 主键
 * @param sourceWorkflowVersionId 来源版本主键
 */
export function copyWorkflowDraftFromVersion(agentId: number, sourceWorkflowVersionId: number) {
  return request<Schema["WorkflowDraftResult"]>(`/api/agents/${agentId}/workflow-draft/copy-from-version`, {
    method: "POST",
    body: JSON.stringify({ sourceWorkflowVersionId })
  });
}

/**
 * 查询工作流版本列表。
 *
 * @param agentId Agent 主键
 * @param query 查询条件
 */
export function listWorkflowVersions(agentId: number, query: PageQuery) {
  return request<Schema["PageResponseWorkflowVersionListItemResult"]>(`/api/agents/${agentId}/workflow-versions`, {
    query
  });
}

/**
 * 查询工作流版本详情。
 *
 * @param agentId Agent 主键
 * @param workflowVersionId 工作流版本主键
 */
export function getWorkflowVersion(agentId: number, workflowVersionId: number) {
  return request<Schema["WorkflowVersionResult"]>(`/api/agents/${agentId}/workflow-versions/${workflowVersionId}`);
}

/**
 * 发起调试运行。
 *
 * @param agentId Agent 主键
 * @param body 请求体
 */
export function runDebugAgent(agentId: number, body: Schema["DebugRunRequest"]) {
  return request<Schema["RunResult"]>(`/api/agents/${agentId}/debug-runs`, {
    method: "POST",
    body: JSON.stringify(body)
  });
}

/**
 * 查询 Schema 列表。
 *
 * @param query 查询条件
 */
export function listSchemas(query: PageQuery) {
  return request<Schema["SchemaPageResponse"]>("/api/schemas", { query });
}

/**
 * 创建 Schema。
 *
 * @param body 请求体
 */
export function createSchema(body: Schema["CreateSchemaRequest"]) {
  return request<Schema["SchemaDetail"]>("/api/schemas", {
    method: "POST",
    body: JSON.stringify(body)
  });
}

/**
 * 更新 Schema 草稿。
 *
 * @param schemaId Schema 主键
 * @param body 请求体
 */
export function updateSchema(schemaId: number, body: Schema["UpdateSchemaDraftRequest"]) {
  return request<Schema["SchemaDetail"]>(`/api/schemas/${schemaId}`, {
    method: "PUT",
    body: JSON.stringify(body)
  });
}

/**
 * 查询 Schema 详情。
 *
 * @param schemaId Schema 主键
 */
export function getSchema(schemaId: number) {
  return request<Schema["SchemaDetail"]>(`/api/schemas/${schemaId}`);
}

/**
 * 基于旧 Schema 创建新版本。
 *
 * @param schemaId 来源 Schema 主键
 * @param body 请求体
 */
export function createSchemaVersion(schemaId: number, body: Schema["CreateSchemaVersionRequest"]) {
  return request<Schema["SchemaDetail"]>(`/api/schemas/${schemaId}/versions`, {
    method: "POST",
    body: JSON.stringify(body)
  });
}

/**
 * 查询 Java 方法列表。
 *
 * @param query 查询条件
 */
export function listJavaMethods(query: PageQuery) {
  return request<Schema["PageResponseJavaMethodListItemResult"]>("/api/java-methods", { query });
}

/**
 * 查询 Java 方法详情。
 *
 * @param methodId 方法主键
 */
export function getJavaMethod(methodId: number) {
  return request<Schema["JavaMethodDetailResult"]>(`/api/java-methods/${methodId}`);
}

/**
 * 查询工具列表。
 *
 * @param query 查询条件
 */
export function listTools(query: PageQuery) {
  return request<Schema["PageResponseToolListItemResult"]>("/api/tools", { query });
}

/**
 * 查询工具详情。
 *
 * @param toolId 工具主键
 */
export function getTool(toolId: number) {
  return request<Schema["ToolDetailResult"]>(`/api/tools/${toolId}`);
}

/**
 * 查询外部 Agent 列表。
 *
 * @param query 查询条件
 */
export function listExternalAgents(query: PageQuery) {
  return request<Schema["PageResponseExternalAgentListItemResult"]>("/api/external-agents", { query });
}

/**
 * 查询外部 Agent 详情。
 *
 * @param adapterId 外部 Agent 主键
 */
export function getExternalAgent(adapterId: number) {
  return request<Schema["ExternalAgentDetailResult"]>(`/api/external-agents/${adapterId}`);
}

/**
 * 创建外部 Agent。
 *
 * @param body 请求体
 */
export function createExternalAgent(body: Schema["CreateExternalAgentRequest"]) {
  return request<Schema["ExternalAgentDetailResult"]>("/api/external-agents", {
    method: "POST",
    body: JSON.stringify(body)
  });
}

/**
 * 更新外部 Agent 普通配置。
 *
 * @param adapterId 外部 Agent 主键
 * @param body 请求体
 */
export function updateExternalAgent(adapterId: number, body: Schema["UpdateExternalAgentRequest"]) {
  return request<Schema["ExternalAgentDetailResult"]>(`/api/external-agents/${adapterId}`, {
    method: "PUT",
    body: JSON.stringify(body)
  });
}

/**
 * 更新外部 Agent 敏感 Header。
 *
 * @param adapterId 外部 Agent 主键
 * @param body 请求体
 */
export function updateExternalAgentSecrets(adapterId: number, body: Schema["UpdateExternalAgentSecretsRequest"]) {
  return request<Schema["ExternalAgentDetailResult"]>(`/api/external-agents/${adapterId}/secrets`, {
    method: "PUT",
    body: JSON.stringify(body)
  });
}

/**
 * 切换外部 Agent 状态。
 *
 * @param adapterId 外部 Agent 主键
 * @param status 目标状态
 */
export function changeExternalAgentStatus(adapterId: number, status: Schema["ChangeExternalAgentStatusRequest"]["status"]) {
  return request<Schema["ExternalAgentDetailResult"]>(`/api/external-agents/${adapterId}/status`, {
    method: "PUT",
    body: JSON.stringify({ status })
  });
}

/**
 * 测试外部 Agent。
 *
 * @param adapterId 外部 Agent 主键
 * @param body 请求体
 */
export function testExternalAgent(adapterId: number, body: Schema["TestExternalAgentRequest"]) {
  return request<Schema["ExternalAgentTestResult"]>(`/api/external-agents/${adapterId}/test`, {
    method: "POST",
    body: JSON.stringify(body)
  });
}

/**
 * 查询验收套件列表。
 *
 * @param query 查询条件
 */
export function listEvalSuites(query: PageQuery) {
  return request<Schema["PageResponseEvalSuiteListItemResult"]>("/api/eval-suites", { query });
}

/**
 * 创建验收套件。
 *
 * @param body 请求体
 */
export function createEvalSuite(body: Schema["CreateEvalSuiteRequest"]) {
  return request<Schema["EvalSuiteResult"]>("/api/eval-suites", {
    method: "POST",
    body: JSON.stringify(body)
  });
}

/**
 * 更新验收套件。
 *
 * @param suiteId 套件主键
 * @param body 请求体
 */
export function updateEvalSuite(suiteId: number, body: Schema["UpdateEvalSuiteRequest"]) {
  return request<Schema["EvalSuiteResult"]>(`/api/eval-suites/${suiteId}`, {
    method: "PUT",
    body: JSON.stringify(body)
  });
}

/**
 * 确认验收套件。
 *
 * @param suiteId 套件主键
 */
export function confirmEvalSuite(suiteId: number) {
  return request<Schema["EvalSuiteResult"]>(`/api/eval-suites/${suiteId}/confirm`, { method: "PUT" });
}

/**
 * 归档验收套件。
 *
 * @param suiteId 套件主键
 */
export function archiveEvalSuite(suiteId: number) {
  return request<Schema["EvalSuiteResult"]>(`/api/eval-suites/${suiteId}/archive`, { method: "PUT" });
}

/**
 * 运行验收套件。
 *
 * @param suiteId 套件主键
 * @param body 请求体
 */
export function runEvalSuite(suiteId: number, body: Schema["RunEvalSuiteRequest"]) {
  return request<Schema["EvalRunResult"]>(`/api/eval-suites/${suiteId}/runs`, {
    method: "POST",
    body: JSON.stringify(body)
  });
}

/**
 * 查询验收用例。
 *
 * @param suiteId 套件主键
 * @param query 查询条件
 */
export function listEvalCases(suiteId: number, query: PageQuery) {
  return request<Schema["PageResponseEvalCaseResult"]>(`/api/eval-suites/${suiteId}/cases`, { query });
}

/**
 * 创建验收用例。
 *
 * @param suiteId 套件主键
 * @param body 请求体
 */
export function createEvalCase(suiteId: number, body: Schema["CreateEvalCaseRequest"]) {
  return request<Schema["EvalCaseResult"]>(`/api/eval-suites/${suiteId}/cases`, {
    method: "POST",
    body: JSON.stringify(body)
  });
}

/**
 * 查询验收用例详情。
 *
 * @param suiteId 套件主键
 * @param caseId 用例主键
 */
export function getEvalCase(suiteId: number, caseId: number) {
  return request<Schema["EvalCaseResult"]>(`/api/eval-suites/${suiteId}/cases/${caseId}`);
}

/**
 * 更新验收用例。
 *
 * @param suiteId 套件主键
 * @param caseId 用例主键
 * @param body 请求体
 */
export function updateEvalCase(suiteId: number, caseId: number, body: Schema["UpdateEvalCaseRequest"]) {
  return request<Schema["EvalCaseResult"]>(`/api/eval-suites/${suiteId}/cases/${caseId}`, {
    method: "PUT",
    body: JSON.stringify(body)
  });
}

/**
 * 确认验收用例。
 *
 * @param suiteId 套件主键
 * @param caseId 用例主键
 */
export function confirmEvalCase(suiteId: number, caseId: number) {
  return request<Schema["EvalCaseResult"]>(`/api/eval-suites/${suiteId}/cases/${caseId}/confirm`, { method: "PUT" });
}

/**
 * 归档验收用例。
 *
 * @param suiteId 套件主键
 * @param caseId 用例主键
 */
export function archiveEvalCase(suiteId: number, caseId: number) {
  return request<Schema["EvalCaseResult"]>(`/api/eval-suites/${suiteId}/cases/${caseId}/archive`, { method: "PUT" });
}

/**
 * 从 NodeRun 创建验收用例。
 *
 * @param nodeRunId NodeRun 数据库主键
 * @param body 请求体
 */
export function createEvalCaseFromNodeRun(nodeRunId: number, body: Schema["CreateEvalCaseFromNodeRunRequest"]) {
  return request<Schema["EvalCaseResult"]>(`/api/node-runs/${nodeRunId}/eval-cases`, {
    method: "POST",
    body: JSON.stringify(body)
  });
}

/**
 * 查询验收运行列表。
 *
 * @param suiteId 套件主键
 * @param query 查询条件
 */
export function listEvalRuns(suiteId: number, query: PageQuery) {
  return request<Schema["PageResponseEvalRunListItemResult"]>(`/api/eval-suites/${suiteId}/runs`, { query });
}

/**
 * 查询验收历史对比。
 *
 * @param suiteId 套件主键
 * @param query 查询条件
 */
export function listEvalRunHistory(suiteId: number, query: PageQuery) {
  return request<Schema["PageResponseEvalRunHistoryItemResult"]>(`/api/eval-suites/${suiteId}/run-history`, { query });
}

/**
 * 查询验收运行详情。
 *
 * @param evalRunId 验收运行编号
 */
export function getEvalRun(evalRunId: string) {
  return request<Schema["EvalRunDetailResult"]>(`/api/eval-runs/${encodeURIComponent(evalRunId)}`);
}

/**
 * 查询验收运行结果明细。
 *
 * @param evalRunId 验收运行编号
 * @param query 查询条件
 */
export function listEvalRunResults(evalRunId: string, query: PageQuery) {
  return request<Schema["PageResponseEvalRunResultItemResult"]>(`/api/eval-runs/${encodeURIComponent(evalRunId)}/results`, {
    query
  });
}
