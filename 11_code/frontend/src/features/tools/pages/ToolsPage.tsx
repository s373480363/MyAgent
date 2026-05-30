import { FeatureIntroCard } from "../../../shared/components/FeatureIntroCard";

/**
 * 工具目录页面占位。
 */
export function ToolsPage() {
  return (
    <FeatureIntroCard
      title="工具目录"
      description="后续步骤将接入工具列表、详情和注册目录能力，为 TOOL 节点提供可引用对象。"
      bullets={[
        "工具列表",
        "工具详情",
        "工具参数摘要",
        "目录刷新"
      ]}
    />
  );
}
