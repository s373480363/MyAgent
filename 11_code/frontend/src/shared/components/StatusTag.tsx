import { Tag } from "antd";

const statusColorMap: Record<string, string> = {
  ACTIVE: "success",
  ENABLED: "success",
  DISABLED: "default",
  DRAFT: "processing",
  CONFIRMED: "success",
  ARCHIVED: "default",
  PUBLISHED: "success",
  HISTORY: "default",
  RUNNING: "processing",
  PENDING: "default",
  FAILED: "error",
  TIMEOUT: "warning",
  CANCELED: "default",
  SUCCESS: "success",
  STRING: "blue",
  NUMBER: "geekblue",
  BOOLEAN: "cyan",
  JSON: "purple",
  SYSTEM_SETTING: "success",
  APPLICATION_CONFIG: "default",
  USER_CREATED: "success",
  USER_CONFIRMED: "success",
  AI_DRAFT_PENDING: "warning"
};

/**
 * 状态标签属性。
 */
interface StatusTagProps {
  status: string;
}

/**
 * 统一状态标签。
 *
 * @param props 状态标签属性
 * @returns 状态标签
 */
export function StatusTag({ status }: StatusTagProps) {
  return <Tag color={statusColorMap[status] ?? "default"}>{status}</Tag>;
}
