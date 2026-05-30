import { FeatureIntroCard } from "../../../shared/components/FeatureIntroCard";

/**
 * 运行记录页面占位。
 */
export function RunsPage() {
  return (
    <FeatureIntroCard
      title="运行记录"
      description="后续步骤将接入运行列表、运行详情、Trace 时间线、节点输入输出和父子运行关联能力。"
      bullets={[
        "运行列表筛选",
        "运行详情聚合展示",
        "NodeRun 输入输出查看",
        "Trace 时间线"
      ]}
    />
  );
}
