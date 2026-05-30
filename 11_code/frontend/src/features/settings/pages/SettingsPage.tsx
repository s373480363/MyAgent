import { FeatureIntroCard } from "../../../shared/components/FeatureIntroCard";

/**
 * 系统设置页面占位。
 */
export function SettingsPage() {
  return (
    <FeatureIntroCard
      title="系统设置"
      description="后续步骤将接入默认模型、默认超时等系统设置查询与更新能力。"
      bullets={[
        "默认模型配置",
        "默认超时配置",
        "运行限制配置",
        "后端配置摘要"
      ]}
    />
  );
}
