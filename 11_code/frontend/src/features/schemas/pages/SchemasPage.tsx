import { FeatureIntroCard } from "../../../shared/components/FeatureIntroCard";

/**
 * Schema 页面占位。
 */
export function SchemasPage() {
  return (
    <FeatureIntroCard
      title="Schema 管理"
      description="后续步骤将接入 Schema 列表、详情、DRAFT 编辑、基于旧版本创建新版本和引用锁定查看能力。"
      bullets={[
        "列表与详情",
        "DRAFT 原地编辑",
        "从旧版本创建新版本",
        "锁定状态与引用关系"
      ]}
    />
  );
}
