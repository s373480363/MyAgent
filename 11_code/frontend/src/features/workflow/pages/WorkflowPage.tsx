import { FeatureIntroCard } from "../../../shared/components/FeatureIntroCard";

/**
 * 工作流页面占位。
 */
export function WorkflowPage() {
  return (
    <FeatureIntroCard
      title="工作流设计器"
      description="后续步骤将接入 React Flow 画布、节点配置面板、草稿保存、发布校验、发布和历史版本只读回看能力。"
      bullets={[
        "画布与节点拖拽",
        "草稿保存与发布",
        "历史版本只读查看",
        "调试入口与版本绑定"
      ]}
    />
  );
}
