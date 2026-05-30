import { FeatureIntroCard } from "../../../shared/components/FeatureIntroCard";

/**
 * Agent 列表页面占位。
 */
export function AgentsPage() {
  return (
    <FeatureIntroCard
      title="Agent 管理"
      description="后续步骤将接入 Agent 列表、创建、编辑、启停和详情能力，并承接工作流草稿、当前发布版本和历史版本入口。"
      bullets={[
        "列表筛选与分页",
        "创建与编辑表单",
        "启停状态流转",
        "详情页版本摘要入口"
      ]}
    />
  );
}
