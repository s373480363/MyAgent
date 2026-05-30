import { FeatureIntroCard } from "../../../shared/components/FeatureIntroCard";

/**
 * 外部 Agent 页面占位。
 */
export function ExternalAgentsPage() {
  return (
    <FeatureIntroCard
      title="外部 Agent"
      description="后续步骤将接入外部 Agent 列表、创建、编辑、启停、测试和结果摘要能力。"
      bullets={[
        "适配器列表",
        "创建与编辑配置",
        "启停状态管理",
        "连接测试结果"
      ]}
    />
  );
}
