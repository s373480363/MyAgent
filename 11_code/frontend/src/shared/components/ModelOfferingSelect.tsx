import { Button, Select, Space, Typography } from "antd";
import { useEffect, useMemo, useRef, useState } from "react";
import { getModelOfferingsByKeys, listModelOfferings, type Schema } from "../../api/domainApi";

type ModelOfferingStatus = Schema["ModelOfferingDescriptor"]["status"];

type ModelOfferingSelectOption = {
  value: string;
  label: string;
  disabled?: boolean;
};

export type ModelOfferingSelectProps = {
  value?: string;
  onChange?: (value?: string) => void;
  placeholder?: string;
  disabled?: boolean;
  providerKey?: string;
  status?: ModelOfferingStatus;
  allowClear?: boolean;
  dataTestId?: string;
};

const PAGE_SIZE = 20;

/**
 * 模型供应项远程选择器。
 */
export function ModelOfferingSelect({
  value,
  onChange,
  placeholder = "搜索并选择模型供应项",
  disabled,
  providerKey,
  status,
  allowClear = true,
  dataTestId
}: ModelOfferingSelectProps) {
  const requestRef = useRef(0);
  const [open, setOpen] = useState(false);
  const [keyword, setKeyword] = useState("");
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(false);
  const [hasMore, setHasMore] = useState(false);
  const [options, setOptions] = useState<ModelOfferingSelectOption[]>([]);
  const [boundOptions, setBoundOptions] = useState<ModelOfferingSelectOption[]>([]);

  useEffect(() => {
    if (!value) {
      setBoundOptions([]);
      return;
    }
    const requestId = ++requestRef.current;
    void getModelOfferingsByKeys([value]).then((result) => {
      if (requestId !== requestRef.current) {
        return;
      }
      const descriptor = result.items?.find((item) => item.offeringKey === value);
      setBoundOptions([
        descriptor ? toModelOfferingOption(descriptor, true) : createMissingBoundOption(value)
      ]);
    });
  }, [value]);

  useEffect(() => {
    if (!open) {
      return;
    }
    setPage(1);
    void loadPage(1, keyword, false);
  }, [keyword, open, providerKey, status]);

  const mergedOptions = useMemo(
    () => deduplicateOptions([...boundOptions, ...options]),
    [boundOptions, options]
  );

  return (
    <Select
      showSearch
      allowClear={allowClear}
      filterOption={false}
      value={value}
      disabled={disabled}
      placeholder={placeholder}
      options={mergedOptions}
      loading={loading}
      onChange={(nextValue) => onChange?.(nextValue)}
      onSearch={(nextKeyword) => setKeyword(nextKeyword)}
      onOpenChange={(nextOpen) => {
        setOpen(nextOpen);
        if (!nextOpen) {
          return;
        }
        if (options.length === 0) {
          setPage(1);
          void loadPage(1, keyword, false);
        }
      }}
      notFoundContent={loading ? "加载中..." : "暂无可选模型供应项"}
      dropdownRender={(menu) => (
        <Space direction="vertical" size={8} style={{ width: "100%", paddingBottom: 4 }}>
          {menu}
          {hasMore ? (
            <Button
              type="link"
              loading={loading}
              onMouseDown={(event) => {
                event.preventDefault();
              }}
              onClick={() => {
                const nextPage = page + 1;
                setPage(nextPage);
                void loadPage(nextPage, keyword, true);
              }}
            >
              加载下一页
            </Button>
          ) : null}
          {value && boundOptions.some((option) => option.value === value && option.disabled) ? (
            <Typography.Text type="secondary" style={{ paddingInline: 12, paddingBottom: 4 }}>
              当前绑定项不可新选，仅用于回填展示。
            </Typography.Text>
          ) : null}
        </Space>
      )}
      data-testid={dataTestId}
    />
  );

  async function loadPage(nextPage: number, nextKeyword: string, append: boolean) {
    const requestId = ++requestRef.current;
    setLoading(true);
    try {
      const result = await listModelOfferings({
        page: nextPage,
        pageSize: PAGE_SIZE,
        keyword: nextKeyword || undefined,
        providerKey,
        status
      });
      if (requestId !== requestRef.current) {
        return;
      }
      const nextOptions = result.items.map((item) => toModelOfferingOption(item));
      setOptions((current) => append ? deduplicateOptions([...current, ...nextOptions]) : nextOptions);
      setHasMore(nextPage * PAGE_SIZE < (result.total ?? 0));
    } finally {
      if (requestId === requestRef.current) {
        setLoading(false);
      }
    }
  }
}

/**
 * 格式化模型供应项主文案。
 *
 * @param descriptor 供应项描述
 * @returns 展示文本
 */
export function formatModelOfferingLabel(descriptor: Partial<Schema["ModelOfferingDescriptor"]> | undefined) {
  if (!descriptor?.offeringKey) {
    return "-";
  }
  const provider = descriptor.providerName?.trim() || descriptor.providerKey?.trim() || "未命名供应商";
  const displayName = descriptor.displayName?.trim() || descriptor.offeringKey;
  const upstream = descriptor.upstreamModelName?.trim() || descriptor.modelKey?.trim();
  return upstream ? `${provider} / ${displayName} (${upstream})` : `${provider} / ${displayName}`;
}

function toModelOfferingOption(
  descriptor: Schema["ModelOfferingDescriptor"],
  isBoundOption = false
): ModelOfferingSelectOption {
  const label = isBoundOption ? formatBoundModelOfferingLabel(descriptor) : formatModelOfferingLabel(descriptor);
  return {
    value: descriptor.offeringKey ?? "",
    label,
    disabled: descriptor.selectable === false
  };
}

function formatBoundModelOfferingLabel(descriptor: Schema["ModelOfferingDescriptor"]) {
  const base = formatModelOfferingLabel(descriptor);
  if (descriptor.selectable === false) {
    return `当前绑定：${base}${descriptor.unavailableReason ? `（${descriptor.unavailableReason}）` : "（当前不可新选）"}`;
  }
  return base;
}

function createMissingBoundOption(offeringKey: string): ModelOfferingSelectOption {
  return {
    value: offeringKey,
    label: `当前绑定：${offeringKey}（记录不存在或当前结果未命中）`,
    disabled: true
  };
}

function deduplicateOptions(options: ModelOfferingSelectOption[]) {
  const mapping = new Map<string, ModelOfferingSelectOption>();
  options.forEach((option) => {
    if (!option.value) {
      return;
    }
    mapping.set(option.value, option);
  });
  return Array.from(mapping.values());
}
