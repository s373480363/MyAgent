import { Input } from "antd";

/**
 * JSON 文本编辑器。
 *
 * @param props 组件属性
 * @returns JSON 文本框
 */
export function JsonTextArea({
  value = "",
  onChange,
  rows = 8,
  placeholder
}: {
  value?: string;
  onChange?: (value: string) => void;
  rows?: number;
  placeholder?: string;
}) {
  return (
    <Input.TextArea
      rows={rows}
      value={value}
      onChange={(event) => onChange?.(event.target.value)}
      spellCheck={false}
      placeholder={placeholder ?? "{\n  \"type\": \"object\"\n}"}
      className="json-editor-textarea"
    />
  );
}
