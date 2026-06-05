import { useEffect, useState } from "react";
import { App, Button, Card, Checkbox, Descriptions, Drawer, Form, Input, InputNumber, Modal, Space, Table, Tabs, Typography } from "antd";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useNavigate, useParams } from "react-router-dom";
import {
  archiveEvalCase,
  archiveEvalSuite,
  confirmEvalCase,
  confirmEvalSuite,
  createEvalCase,
  createEvalSuite,
  getEvalRun,
  listEvalCases,
  listEvalRunHistory,
  listEvalRunResults,
  listEvalRuns,
  listEvalSuites,
  runEvalSuite,
  updateEvalCase,
  updateEvalSuite,
  type Schema
} from "../../../api/domainApi";
import { queryClient } from "../../../app/queryClient";
import { JsonBlock, parseJsonText, stringifyJson } from "../../../shared/components/JsonBlock";
import { JsonTextArea } from "../../../shared/components/JsonTextArea";
import { ModelOfferingSelect } from "../../../shared/components/ModelOfferingSelect";
import { PageState } from "../../../shared/components/PageState";
import { StatusTag } from "../../../shared/components/StatusTag";

type SuiteFormValues = Schema["CreateEvalSuiteRequest"] & Schema["UpdateEvalSuiteRequest"];
type CaseFormMode = "create" | "edit";
type CaseFormValues = {
  caseNo?: string;
  title: string;
  inputText?: string;
  referenceAnswerText?: string;
  assertionsText?: string;
  scoreRuleEnabled?: boolean;
  scoreRuleModelOfferingKey?: string;
  scoreRuleTemperature?: number | null;
  scoreRulePromptTemplate?: string;
  critical?: boolean;
  description?: string;
};

/**
 * 判断套件是否已经进入归档只读状态。
 *
 * @param status 套件状态
 * @returns 归档时返回 true
 */
export function isArchivedEvalSuite(status?: string | null) {
  return status === "ARCHIVED";
}

/**
 * 判断用例是否应进入只读状态。
 *
 * @param suiteStatus 套件状态
 * @param confirmStatus 用例确认状态
 * @returns 归档套件或归档用例均返回 true
 */
export function isReadOnlyEvalCase(suiteStatus?: string | null, confirmStatus?: string | null) {
  return isArchivedEvalSuite(suiteStatus) || confirmStatus === "ARCHIVED";
}

/**
 * 节点验收页面。
 */
export function EvalsPage() {
  const { message } = App.useApp();
  const navigate = useNavigate();
  const { evalRunId: routeEvalRunId } = useParams();
  const [page, setPage] = useState(1);
  const [createOpen, setCreateOpen] = useState(false);
  const [editingSuite, setEditingSuite] = useState<Schema["EvalSuiteListItemResult"] | null>(null);
  const [selectedSuite, setSelectedSuite] = useState<Schema["EvalSuiteListItemResult"] | null>(null);
  const [selectedEvalRunId, setSelectedEvalRunId] = useState<string | null>(routeEvalRunId ?? null);
  const [form] = Form.useForm<SuiteFormValues>();
  const suitesQuery = useQuery({
    queryKey: ["eval-suites", page],
    queryFn: () => listEvalSuites({ page, pageSize: 20 })
  });
  const createMutation = useMutation({
    mutationFn: createEvalSuite,
    onSuccess: () => {
      message.success("验收套件已创建。");
      setCreateOpen(false);
      form.resetFields();
      void queryClient.invalidateQueries({ queryKey: ["eval-suites"] });
    }
  });
  const updateMutation = useMutation({
    mutationFn: ({ suiteId, body }: { suiteId: number; body: Schema["UpdateEvalSuiteRequest"] }) => updateEvalSuite(suiteId, body),
    onSuccess: () => {
      message.success("验收套件已更新。");
      setEditingSuite(null);
      void queryClient.invalidateQueries({ queryKey: ["eval-suites"] });
    }
  });
  const confirmMutation = useMutation({
    mutationFn: confirmEvalSuite,
    onSuccess: () => {
      message.success("验收套件已确认。");
      void queryClient.invalidateQueries({ queryKey: ["eval-suites"] });
    }
  });
  const archiveMutation = useMutation({
    mutationFn: archiveEvalSuite,
    onSuccess: () => {
      message.success("验收套件已归档。");
      void queryClient.invalidateQueries({ queryKey: ["eval-suites"] });
    }
  });
  const runMutation = useMutation({
    mutationFn: (suiteId: number) => runEvalSuite(suiteId, { includeUnconfirmed: false }),
    onSuccess: (result) => {
      message.success(`验收运行已完成：${result.evalRunId}`);
      void queryClient.invalidateQueries({ queryKey: ["eval-suites"] });
      void queryClient.invalidateQueries({ queryKey: ["eval-runs"] });
      if (result.evalRunId) {
        setSelectedEvalRunId(result.evalRunId);
        navigate(`/eval-runs/${result.evalRunId}`);
      }
    }
  });

  useEffect(() => {
    if (routeEvalRunId) {
      setSelectedEvalRunId(routeEvalRunId);
    }
  }, [routeEvalRunId]);

  useEffect(() => {
    if (editingSuite) {
      form.setFieldsValue({
        name: editingSuite.name ?? "",
        goal: editingSuite.goal,
        passThreshold: editingSuite.passThreshold
      });
    }
  }, [editingSuite, form]);

  if (suitesQuery.isLoading || suitesQuery.isError) {
    return (
      <PageState
        title="节点验收"
        description="暂无验收套件。"
        loading={suitesQuery.isLoading}
        error={suitesQuery.error?.message}
      />
    );
  }

  return (
    <Space direction="vertical" size={16} style={{ width: "100%" }}>
      <section className="page-section page-title-row">
        <div>
          <Typography.Title level={3}>节点验收</Typography.Title>
          <Typography.Paragraph className="muted-text">
            套件、用例、运行、结果明细和历史对比统一按正式 Eval DTO 展示；来源链路可回到运行详情。
          </Typography.Paragraph>
        </div>
        <Button type="primary" onClick={() => openCreateSuite()}>创建套件</Button>
      </section>
      <Card className="page-card">
        <Table<Schema["EvalSuiteListItemResult"]>
          rowKey={(record) => String(record.suiteId)}
          dataSource={suitesQuery.data?.items ?? []}
          pagination={{
            current: page,
            pageSize: suitesQuery.data?.pageSize ?? 20,
            total: suitesQuery.data?.total ?? 0,
            onChange: setPage
          }}
          columns={[
            { title: "套件", dataIndex: "name" },
            { title: "Agent", dataIndex: "agentId" },
            { title: "工作流版本", dataIndex: "workflowVersionId" },
            { title: "节点", dataIndex: "nodeId" },
            { title: "阈值", dataIndex: "passThreshold", render: (value) => `${value ?? 0}%` },
            { title: "状态", dataIndex: "status", render: (value) => <StatusTag status={String(value)} /> },
            {
              title: "操作",
              render: (_, record) => (
                <Space>
                  <Button type="link" onClick={() => setSelectedSuite(record)}>详情</Button>
                  <Button type="link" disabled={record.status !== "DRAFT"} onClick={() => setEditingSuite(record)}>编辑</Button>
                  <Button
                    type="link"
                    disabled={record.status !== "DRAFT"}
                    onClick={() => record.suiteId && confirmMutation.mutate(record.suiteId)}
                  >
                    确认
                  </Button>
                  <Button
                    type="link"
                    disabled={record.status !== "CONFIRMED"}
                    loading={runMutation.isPending}
                    onClick={() => record.suiteId && runMutation.mutate(record.suiteId)}
                  >
                    运行
                  </Button>
                  <Button
                    type="link"
                    danger
                    disabled={record.status === "ARCHIVED"}
                    onClick={() => record.suiteId && archiveMutation.mutate(record.suiteId)}
                  >
                    归档
                  </Button>
                </Space>
              )
            }
          ]}
        />
      </Card>
      <Modal
        title={editingSuite ? "更新验收套件" : "创建验收套件"}
        open={createOpen || Boolean(editingSuite)}
        onCancel={() => {
          setCreateOpen(false);
          setEditingSuite(null);
        }}
        confirmLoading={createMutation.isPending || updateMutation.isPending}
        onOk={() => {
          void form.validateFields().then((values) => submitSuite(values));
        }}
      >
        <Form form={form} layout="vertical" initialValues={{ passThreshold: 80 }}>
          {!editingSuite ? (
            <>
              <Form.Item name="agentId" label="Agent ID" rules={[{ required: true, message: "请输入 Agent ID。" }]}>
                <InputNumber min={1} style={{ width: "100%" }} />
              </Form.Item>
              <Form.Item name="workflowVersionId" label="WorkflowVersion ID" rules={[{ required: true, message: "请输入工作流版本 ID。" }]}>
                <InputNumber min={1} style={{ width: "100%" }} />
              </Form.Item>
              <Form.Item name="nodeId" label="节点 ID" rules={[{ required: true, message: "请输入节点 ID。" }]}>
                <Input />
              </Form.Item>
            </>
          ) : null}
          <Form.Item name="name" label="套件名称" rules={[{ required: true, message: "请输入套件名称。" }]}>
            <Input />
          </Form.Item>
          <Form.Item name="goal" label="验收目标">
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item name="passThreshold" label="通过率阈值">
            <InputNumber min={0} max={100} style={{ width: "100%" }} />
          </Form.Item>
        </Form>
      </Modal>
      <SuiteDrawer
        suite={selectedSuite}
        onClose={() => setSelectedSuite(null)}
        onSelectEvalRun={(evalRunId) => {
          setSelectedEvalRunId(evalRunId);
          navigate(`/eval-runs/${evalRunId}`);
        }}
        onOpenSourceRun={(runId) => navigate(`/runs/${runId}`)}
      />
      <EvalRunDrawer
        evalRunId={selectedEvalRunId}
        onClose={() => {
          setSelectedEvalRunId(null);
          navigate("/evals");
        }}
        onOpenRun={(runId) => navigate(`/runs/${runId}`)}
      />
    </Space>
  );

  /**
   * 打开创建套件弹窗。
   */
  function openCreateSuite() {
    form.resetFields();
    form.setFieldsValue({ passThreshold: 80 });
    setCreateOpen(true);
  }

  /**
   * 提交套件表单。
   *
   * @param values 表单值
   */
  function submitSuite(values: SuiteFormValues) {
    if (editingSuite?.suiteId) {
      updateMutation.mutate({
        suiteId: editingSuite.suiteId,
        body: {
          name: values.name,
          goal: values.goal,
          passThreshold: values.passThreshold
        }
      });
      return;
    }
    createMutation.mutate(values);
  }
}

/**
 * 验收套件抽屉。
 *
 * @param props 组件属性
 * @returns 套件详情
 */
function SuiteDrawer({
  suite,
  onClose,
  onSelectEvalRun,
  onOpenSourceRun
}: {
  suite: Schema["EvalSuiteListItemResult"] | null;
  onClose: () => void;
  onSelectEvalRun: (evalRunId: string) => void;
  onOpenSourceRun: (runId: string) => void;
}) {
  const { message } = App.useApp();
  const suiteId = suite?.suiteId;
  const suiteArchived = isArchivedEvalSuite(suite?.status);
  const [caseMode, setCaseMode] = useState<CaseFormMode | null>(null);
  const [editingCase, setEditingCase] = useState<Schema["EvalCaseResult"] | null>(null);
  const [caseForm] = Form.useForm<CaseFormValues>();
  const casesQuery = useQuery({
    queryKey: ["eval-cases", suiteId],
    queryFn: () => listEvalCases(suiteId ?? 0, { page: 1, pageSize: 50 }),
    enabled: Boolean(suiteId)
  });
  const runsQuery = useQuery({
    queryKey: ["eval-runs", suiteId],
    queryFn: () => listEvalRuns(suiteId ?? 0, { page: 1, pageSize: 20 }),
    enabled: Boolean(suiteId)
  });
  const historyQuery = useQuery({
    queryKey: ["eval-run-history", suiteId],
    queryFn: () => listEvalRunHistory(suiteId ?? 0, { page: 1, pageSize: 20 }),
    enabled: Boolean(suiteId)
  });
  const createCaseMutation = useMutation({
    mutationFn: ({ targetSuiteId, body }: { targetSuiteId: number; body: Schema["CreateEvalCaseRequest"] }) => createEvalCase(targetSuiteId, body),
    onSuccess: () => {
      message.success("验收用例已创建。");
      closeCaseForm();
      void queryClient.invalidateQueries({ queryKey: ["eval-cases", suiteId] });
    }
  });
  const updateCaseMutation = useMutation({
    mutationFn: ({ targetSuiteId, caseId, body }: { targetSuiteId: number; caseId: number; body: Schema["UpdateEvalCaseRequest"] }) =>
      updateEvalCase(targetSuiteId, caseId, body),
    onSuccess: () => {
      message.success("验收用例已更新。");
      closeCaseForm();
      void queryClient.invalidateQueries({ queryKey: ["eval-cases", suiteId] });
    }
  });
  const confirmCaseMutation = useMutation({
    mutationFn: ({ targetSuiteId, caseId }: { targetSuiteId: number; caseId: number }) => confirmEvalCase(targetSuiteId, caseId),
    onSuccess: () => {
      message.success("验收用例已确认。");
      void queryClient.invalidateQueries({ queryKey: ["eval-cases", suiteId] });
    }
  });
  const archiveCaseMutation = useMutation({
    mutationFn: ({ targetSuiteId, caseId }: { targetSuiteId: number; caseId: number }) => archiveEvalCase(targetSuiteId, caseId),
    onSuccess: () => {
      message.success("验收用例已归档。");
      void queryClient.invalidateQueries({ queryKey: ["eval-cases", suiteId] });
    }
  });

  return (
    <Drawer title={suite?.name ?? "验收套件"} open={Boolean(suite)} width={1080} onClose={onClose}>
      <Tabs
        items={[
          {
            key: "cases",
            label: "用例",
            children: (
              <Space direction="vertical" style={{ width: "100%" }}>
                <Button type="primary" disabled={!suiteId || suiteArchived} onClick={() => openCreateCase()}>创建用例</Button>
                <Table<Schema["EvalCaseResult"]>
                  rowKey={(record) => String(record.caseId)}
                  loading={casesQuery.isLoading}
                  dataSource={casesQuery.data?.items ?? []}
                  pagination={false}
                  expandable={{
                    expandedRowRender: (record) => (
                      <Space direction="vertical" style={{ width: "100%" }}>
                        <JsonBlock title="输入" value={record.input} />
                        <JsonBlock title="参考答案" value={record.referenceAnswer} />
                        <JsonBlock title="断言" value={record.assertions} />
                        <JsonBlock title="评分规则" value={record.scoreRule} />
                      </Space>
                    )
                  }}
                  columns={[
                    { title: "编号", dataIndex: "caseNo" },
                    { title: "标题", dataIndex: "title" },
                    { title: "确认状态", dataIndex: "confirmStatus", render: (value) => <StatusTag status={String(value)} /> },
                    { title: "关键", dataIndex: "critical", render: (value) => (value ? "是" : "否") },
                    {
                      title: "来源 Run",
                      dataIndex: "sourceRunId",
                      render: (value) => (value ? <Button type="link" onClick={() => onOpenSourceRun(String(value))}>{String(value)}</Button> : "-")
                    },
                    { title: "来源 NodeRun", dataIndex: "sourceNodeRunId" },
                    {
                      title: "操作",
                      render: (_, record) => {
                        const caseReadOnly = isReadOnlyEvalCase(suite?.status, record.confirmStatus);
                        return (
                          <Space>
                            <Button type="link" disabled={caseReadOnly} onClick={() => openEditCase(record)}>编辑</Button>
                            <Button
                              type="link"
                              disabled={!suiteId || !record.caseId || record.confirmStatus === "USER_CONFIRMED" || caseReadOnly}
                              onClick={() => suiteId && record.caseId && confirmCaseMutation.mutate({ targetSuiteId: suiteId, caseId: record.caseId })}
                            >
                              确认
                            </Button>
                            <Button
                              type="link"
                              danger
                              disabled={!suiteId || !record.caseId || caseReadOnly}
                              onClick={() => suiteId && record.caseId && archiveCaseMutation.mutate({ targetSuiteId: suiteId, caseId: record.caseId })}
                            >
                              归档
                            </Button>
                          </Space>
                        );
                      }
                    }
                  ]}
                />
              </Space>
            )
          },
          {
            key: "runs",
            label: "运行",
            children: (
              <Table<Schema["EvalRunListItemResult"]>
                rowKey={(record) => record.evalRunId ?? ""}
                loading={runsQuery.isLoading}
                dataSource={runsQuery.data?.items ?? []}
                pagination={false}
                columns={[
                  { title: "EvalRun", dataIndex: "evalRunId" },
                  { title: "Run", dataIndex: "runId", render: (value) => (value ? <Button type="link" onClick={() => onOpenSourceRun(String(value))}>{String(value)}</Button> : "-") },
                  { title: "状态", dataIndex: "status", render: (value) => <StatusTag status={String(value)} /> },
                  { title: "通过率", dataIndex: "passRate", render: (value) => `${value ?? 0}%` },
                  { title: "通过/失败", render: (_, record) => `${record.passedCaseCount ?? 0}/${record.failedCaseCount ?? 0}` },
                  {
                    title: "操作",
                    render: (_, record) => (
                      <Button type="link" onClick={() => record.evalRunId && onSelectEvalRun(record.evalRunId)}>
                        查看详情
                      </Button>
                    )
                  }
                ]}
              />
            )
          },
          {
            key: "history",
            label: "历史对比",
            children: (
              <Table<Schema["EvalRunHistoryItemResult"]>
                rowKey={(record) => record.evalRunId ?? ""}
                loading={historyQuery.isLoading}
                dataSource={historyQuery.data?.items ?? []}
                pagination={false}
                columns={[
                  { title: "EvalRun", dataIndex: "evalRunId" },
                  { title: "Run", dataIndex: "runId", render: (value) => (value ? <Button type="link" onClick={() => onOpenSourceRun(String(value))}>{String(value)}</Button> : "-") },
                  { title: "状态", dataIndex: "status", render: (value) => <StatusTag status={String(value)} /> },
                  { title: "通过率", dataIndex: "passRate", render: (value) => `${value ?? 0}%` },
                  { title: "通过率差值", dataIndex: "passRateDeltaFromPrevious" },
                  { title: "通过差值", dataIndex: "passedCaseCountDeltaFromPrevious" },
                  { title: "失败差值", dataIndex: "failedCaseCountDeltaFromPrevious" },
                  { title: "关键失败", dataIndex: "criticalFailedCaseCount" }
                ]}
              />
            )
          }
        ]}
      />
      <Modal
        title={caseMode === "create" ? "创建验收用例" : "编辑验收用例"}
        open={Boolean(caseMode)}
        width={760}
        onCancel={closeCaseForm}
        confirmLoading={createCaseMutation.isPending || updateCaseMutation.isPending}
        onOk={() => {
          void caseForm.validateFields().then((values) => submitCase(values));
        }}
      >
        <Form form={caseForm} layout="vertical">
          {caseMode === "create" ? (
            <Form.Item name="caseNo" label="用例编号" rules={[{ required: true, message: "请输入用例编号。" }]}>
              <Input />
            </Form.Item>
          ) : null}
          <Form.Item name="title" label="标题" rules={[{ required: true, message: "请输入标题。" }]}>
            <Input />
          </Form.Item>
          <Form.Item name="inputText" label="输入 JSON">
            <JsonTextArea rows={5} />
          </Form.Item>
          <Form.Item name="referenceAnswerText" label="参考答案 JSON">
            <JsonTextArea rows={5} />
          </Form.Item>
          <Form.Item name="assertionsText" label="确定性断言 JSON">
            <JsonTextArea rows={5} />
          </Form.Item>
          <Form.Item name="scoreRuleEnabled" valuePropName="checked">
            <Checkbox>启用 LLM 辅助评分</Checkbox>
          </Form.Item>
          <Form.Item name="scoreRuleModelOfferingKey" label="评分模型供应项">
            <ModelOfferingSelect dataTestId="eval-score-rule-model-offering-select" />
          </Form.Item>
          <Form.Item name="scoreRuleTemperature" label="评分温度">
            <InputNumber
              min={0}
              max={2}
              step={0.1}
              style={{ width: "100%" }}
            />
          </Form.Item>
          <Form.Item name="scoreRulePromptTemplate" label="评分提示词模板">
            <Input.TextArea
              rows={4}
              placeholder="留空时后端会使用默认评分提示词模板。"
            />
          </Form.Item>
          <Form.Item name="critical" valuePropName="checked">
            <Checkbox>关键用例</Checkbox>
          </Form.Item>
          <Form.Item name="description" label="说明">
            <Input.TextArea rows={3} />
          </Form.Item>
        </Form>
      </Modal>
    </Drawer>
  );

  /**
   * 打开创建用例表单。
   */
  function openCreateCase() {
    if (suiteArchived) {
      message.warning("归档套件不允许继续创建用例。");
      return;
    }
    setEditingCase(null);
    caseForm.resetFields();
    caseForm.setFieldsValue({
      critical: false,
      inputText: stringifyJson({}),
      referenceAnswerText: stringifyJson({}),
      assertionsText: stringifyJson([]),
      scoreRuleEnabled: false,
      scoreRuleModelOfferingKey: undefined,
      scoreRuleTemperature: undefined,
      scoreRulePromptTemplate: undefined
    });
    setCaseMode("create");
  }

  /**
   * 打开编辑用例表单。
   *
   * @param record 用例
   */
  function openEditCase(record: Schema["EvalCaseResult"]) {
    if (suiteArchived || record.confirmStatus === "ARCHIVED") {
      message.warning("归档用例不允许继续编辑。");
      return;
    }
    setEditingCase(record);
    caseForm.setFieldsValue({
      title: record.title ?? "",
      inputText: stringifyJson(record.input ?? {}),
      referenceAnswerText: stringifyJson(record.referenceAnswer ?? {}),
      assertionsText: stringifyJson(record.assertions ?? []),
      ...toScoreRuleFormValues(record.scoreRule),
      critical: record.critical,
      description: record.description
    });
    setCaseMode("edit");
  }

  /**
   * 关闭用例表单。
   */
  function closeCaseForm() {
    setCaseMode(null);
    setEditingCase(null);
    caseForm.resetFields();
  }

  /**
   * 提交用例表单。
   *
   * @param values 表单值
   */
  function submitCase(values: CaseFormValues) {
    if (!suiteId) {
      message.error("缺少套件主键，无法提交用例。");
      return;
    }
    if (suiteArchived) {
      message.error("归档套件不允许继续修改用例。");
      return;
    }
    try {
      const body = {
        title: values.title,
        input: parseOptionalJson(values.inputText),
        referenceAnswer: parseOptionalJson(values.referenceAnswerText),
        assertions: parseOptionalJson(values.assertionsText),
        scoreRule: buildScoreRule(values),
        critical: values.critical,
        description: values.description
      };
      if (caseMode === "create") {
        createCaseMutation.mutate({
          targetSuiteId: suiteId,
          body: {
            caseNo: values.caseNo ?? "",
            ...body
          }
        });
        return;
      }
      if (!editingCase?.caseId) {
        message.error("缺少用例主键，无法更新。");
        return;
      }
      updateCaseMutation.mutate({ targetSuiteId: suiteId, caseId: editingCase.caseId, body });
    } catch {
      message.error("用例 JSON 字段格式不正确。");
    }
  }
}

/**
 * 验收运行详情抽屉。
 *
 * @param props 组件属性
 * @returns 运行详情
 */
function EvalRunDrawer({
  evalRunId,
  onClose,
  onOpenRun
}: {
  evalRunId: string | null;
  onClose: () => void;
  onOpenRun: (runId: string) => void;
}) {
  const detailQuery = useQuery({
    queryKey: ["eval-run-detail", evalRunId],
    queryFn: () => getEvalRun(evalRunId ?? ""),
    enabled: Boolean(evalRunId)
  });
  const resultsQuery = useQuery({
    queryKey: ["eval-run-results", evalRunId],
    queryFn: () => listEvalRunResults(evalRunId ?? "", { page: 1, pageSize: 50 }),
    enabled: Boolean(evalRunId)
  });

  return (
    <Drawer title={evalRunId ? `验收运行：${evalRunId}` : "验收运行"} open={Boolean(evalRunId)} width={1040} onClose={onClose}>
      {detailQuery.isLoading || detailQuery.isError ? (
        <PageState
          title="验收运行"
          description="暂无验收运行详情。"
          loading={detailQuery.isLoading}
          error={detailQuery.error?.message}
        />
      ) : (
        <Space direction="vertical" size={16} style={{ width: "100%" }}>
          <Descriptions bordered size="small" column={2}>
            <Descriptions.Item label="状态"><StatusTag status={detailQuery.data?.status ?? "UNKNOWN"} /></Descriptions.Item>
            <Descriptions.Item label="Run ID">
              {detailQuery.data?.runId ? <Button type="link" onClick={() => onOpenRun(detailQuery.data?.runId ?? "")}>{detailQuery.data.runId}</Button> : "-"}
            </Descriptions.Item>
            <Descriptions.Item label="通过率">{detailQuery.data?.passRate}%</Descriptions.Item>
            <Descriptions.Item label="阈值">{detailQuery.data?.passThreshold}%</Descriptions.Item>
            <Descriptions.Item label="关键失败">{detailQuery.data?.criticalFailedCaseCount}</Descriptions.Item>
            <Descriptions.Item label="工作流版本">{detailQuery.data?.workflowVersion?.workflowVersionId}</Descriptions.Item>
            <Descriptions.Item label="摘要" span={2}>{detailQuery.data?.summary}</Descriptions.Item>
          </Descriptions>
          <Tabs
            items={[
              {
                key: "results",
                label: "结果明细",
                children: (
                  <Table<Schema["EvalRunResultItemResult"]>
                    rowKey={(record) => String(record.caseId)}
                    loading={resultsQuery.isLoading}
                    dataSource={resultsQuery.data?.items ?? []}
                    pagination={false}
                    expandable={{
                      expandedRowRender: (record) => (
                        <Space direction="vertical" style={{ width: "100%" }}>
                          <JsonBlock title="输入" value={record.input} />
                          <JsonBlock title="输出" value={record.output} />
                          <JsonBlock title="断言结果" value={record.assertionResults} />
                          <JsonBlock title="评分结果" value={record.scoreResult} />
                        </Space>
                      )
                    }}
                    columns={[
                      { title: "用例", dataIndex: "caseNo" },
                      { title: "标题", dataIndex: "title" },
                      { title: "确认状态", dataIndex: "confirmStatus", render: (value) => <StatusTag status={String(value)} /> },
                      { title: "关键", dataIndex: "critical", render: (value) => (value ? "是" : "否") },
                      { title: "结果", dataIndex: "passed", render: (value) => <StatusTag status={value ? "SUCCESS" : "FAILED"} /> },
                      { title: "错误", dataIndex: "errorMessage" },
                      { title: "耗时(ms)", dataIndex: "durationMs" }
                    ]}
                  />
                )
              },
              {
                key: "failure",
                label: "失败摘要",
                children: <JsonBlock title="失败摘要" value={detailQuery.data?.failureSummary} />
              },
              {
                key: "history",
                label: "历史对比摘要",
                children: <JsonBlock title="历史对比摘要" value={detailQuery.data?.historyComparison} />
              }
            ]}
          />
        </Space>
      )}
    </Drawer>
  );
}

/**
 * 解析可选 JSON。
 *
 * @param text JSON 文本
 * @returns JSON 值或 undefined
 */
function parseOptionalJson(text?: string) {
  if (!text?.trim()) {
    return undefined;
  }
  return parseJsonText(text);
}

/**
 * 将评分规则转换为表单字段。
 *
 * @param scoreRule 评分规则
 * @returns 表单字段
 */
function toScoreRuleFormValues(scoreRule: unknown): Pick<
  CaseFormValues,
  "scoreRuleEnabled" | "scoreRuleModelOfferingKey" | "scoreRuleTemperature" | "scoreRulePromptTemplate"
> {
  const rule = isJsonObject(scoreRule) ? scoreRule : undefined;
  return {
    scoreRuleEnabled: readBoolean(rule, "enabled"),
    scoreRuleModelOfferingKey: readText(rule, "modelOfferingKey"),
    scoreRuleTemperature: readNumber(rule, "temperature"),
    scoreRulePromptTemplate: readText(rule, "promptTemplate")
  };
}

/**
 * 根据表单字段构建正式评分规则。
 *
 * @param values 表单字段
 * @returns 评分规则；未配置时返回 undefined
 */
function buildScoreRule(values: CaseFormValues) {
  const modelOfferingKey = emptyToUndefined(values.scoreRuleModelOfferingKey);
  const promptTemplate = emptyToUndefined(values.scoreRulePromptTemplate);
  const hasTemperature = typeof values.scoreRuleTemperature === "number" && Number.isFinite(values.scoreRuleTemperature);
  if (!values.scoreRuleEnabled && !modelOfferingKey && !promptTemplate && !hasTemperature) {
    return undefined;
  }
  const scoreRule: Record<string, unknown> = {
    enabled: Boolean(values.scoreRuleEnabled)
  };
  if (modelOfferingKey) {
    scoreRule.modelOfferingKey = modelOfferingKey;
  }
  if (promptTemplate) {
    scoreRule.promptTemplate = promptTemplate;
  }
  if (hasTemperature) {
    scoreRule.temperature = values.scoreRuleTemperature;
  }
  return scoreRule;
}

/**
 * 读取文本字段。
 *
 * @param source 源对象
 * @param key 字段名
 * @returns 文本值
 */
function readText(source: Record<string, unknown> | undefined, key: string) {
  const value = source?.[key];
  return typeof value === "string" ? value : undefined;
}

/**
 * 读取数字字段。
 *
 * @param source 源对象
 * @param key 字段名
 * @returns 数字值
 */
function readNumber(source: Record<string, unknown> | undefined, key: string) {
  const value = source?.[key];
  return typeof value === "number" && Number.isFinite(value) ? value : undefined;
}

/**
 * 读取布尔字段。
 *
 * @param source 源对象
 * @param key 字段名
 * @returns 布尔值
 */
function readBoolean(source: Record<string, unknown> | undefined, key: string) {
  const value = source?.[key];
  return typeof value === "boolean" ? value : undefined;
}

/**
 * 空字符串转 undefined。
 *
 * @param value 输入值
 * @returns 转换结果
 */
function emptyToUndefined(value?: string) {
  const trimmed = value?.trim();
  return trimmed ? trimmed : undefined;
}

/**
 * 判断是否普通 JSON 对象。
 *
 * @param value 待判断值
 * @returns 普通对象返回 true
 */
function isJsonObject(value: unknown): value is Record<string, unknown> {
  return Boolean(value) && typeof value === "object" && !Array.isArray(value);
}
