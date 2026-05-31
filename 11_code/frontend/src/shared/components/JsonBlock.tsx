import { Card } from "antd";

/**
 * JSON 只读展示块。
 *
 * @param props 组件属性
 * @returns JSON 展示组件
 */
export function JsonBlock({ title, value }: { title: string; value: unknown }) {
  return (
    <Card size="small" title={title}>
      <pre className="json-block">{stringifyJson(value)}</pre>
    </Card>
  );
}

/**
 * 格式化 JSON 值。
 *
 * @param value JSON 值
 * @returns 格式化文本
 */
export function stringifyJson(value: unknown) {
  return JSON.stringify(value ?? null, null, 2);
}

/**
 * 解析 JSON 文本。
 *
 * @param text JSON 文本
 * @returns JSON 值
 */
export function parseJsonText(text: string) {
  const trimmed = text.trim();
  if (!trimmed) {
    return null;
  }
  return JSON.parse(trimmed) as unknown;
}
