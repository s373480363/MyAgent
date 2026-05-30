import { FeatureIntroCard } from "../../../shared/components/FeatureIntroCard";

/**
 * Java 方法页面占位。
 */
export function MethodsPage() {
  return (
    <FeatureIntroCard
      title="Java 方法目录"
      description="后续步骤将接入已注册方法目录、详情和可用性状态，不允许页面直接输入任意类名和方法名。"
      bullets={[
        "方法列表",
        "方法详情",
        "输入输出 Schema 摘要",
        "注册目录刷新"
      ]}
    />
  );
}
