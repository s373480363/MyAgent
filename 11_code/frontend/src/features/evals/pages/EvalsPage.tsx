import { FeatureIntroCard } from "../../../shared/components/FeatureIntroCard";

/**
 * 节点验收页面占位。
 */
export function EvalsPage() {
  return (
    <FeatureIntroCard
      title="节点验收"
      description="后续步骤将接入套件列表、创建、更新、确认、归档，用例管理，以及验收运行详情与历史对比能力。"
      bullets={[
        "验收套件列表与状态流转",
        "验收用例管理",
        "从 NodeRun 生成用例",
        "验收运行历史对比"
      ]}
    />
  );
}
