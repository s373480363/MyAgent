import { Tag } from "antd";

const statusColorMap: Record<string, string> = {
  ACTIVE: "success",
  DISABLED: "default",
  DRAFT: "processing",
  PUBLISHED: "success",
  HISTORY: "default",
  FAILED: "error",
  TIMEOUT: "warning",
  CANCELED: "default",
  SUCCESS: "success"
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
