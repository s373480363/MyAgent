import type { components } from "./generated/schema";

/**
 * 传输层原始错误解析对象。
 *
 * 步骤 03 起使用 OpenAPI 生成类型作为契约来源；该对象仍仅限传输层内部使用，不允许被业务页面引用。
 */
type RawApiError = components["schemas"]["ApiError"];

/**
 * 传输层统一响应解析对象。
 *
 * 步骤 03 起使用 OpenAPI 生成类型作为契约来源；该对象仍仅限传输层内部使用，不允许被业务页面引用。
 */
type RawApiResponse<T> = {
  success?: components["schemas"]["ApiResponse"]["success"];
  data?: T | null;
  error?: RawApiError | null;
};

/**
 * 传输层请求错误对象。
 *
 * 仅用于传输层保留后端结构化错误语义；业务 DTO 仍以 OpenAPI 生成类型为唯一来源。
 */
export class HttpRequestError extends Error {
  /**
   * HTTP 状态码。
   */
  readonly httpStatus: number;

  /**
   * 后端稳定错误码。
   */
  readonly code?: string;

  /**
   * 后端字段级错误明细。
   */
  readonly details?: RawApiError["details"];

  /**
   * 构造请求错误对象。
   *
   * @param httpStatus HTTP 状态码
   * @param code 后端稳定错误码
   * @param message 错误消息
   * @param details 字段级错误明细
   */
  constructor(httpStatus: number, code: string | undefined, message: string, details?: RawApiError["details"]) {
    super(message);
    this.name = "HttpRequestError";
    this.httpStatus = httpStatus;
    this.code = code;
    this.details = details;
  }
}

/**
 * HTTP 请求配置。
 */
export interface RequestOptions extends RequestInit {
  query?: Record<string, string | number | boolean | undefined>;
}

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

/**
 * 发送统一 HTTP 请求。
 *
 * @param path 接口路径
 * @param options 请求配置
 * @returns 业务数据
 */
export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const url = buildUrl(path, options.query);
  const response = await fetch(url, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(options.headers ?? {})
    }
  });

  const payload = await parseResponse<T>(response);
  if (!response.ok) {
    throw createRequestError(response.status, payload.error, "接口请求失败。");
  }

  if (!payload.success) {
    throw createRequestError(response.status, payload.error, "业务执行失败。");
  }

  return payload.data as T;
}

/**
 * 构造请求 URL。
 *
 * @param path 接口路径
 * @param query 查询参数
 * @returns 完整 URL
 */
function buildUrl(path: string, query?: RequestOptions["query"]) {
  const url = new URL(path, API_BASE_URL);
  if (query) {
    Object.entries(query).forEach(([key, value]) => {
      if (value !== undefined) {
        url.searchParams.set(key, String(value));
      }
    });
  }
  return url.toString();
}

/**
 * 解析统一响应体。
 *
 * @param response Fetch 响应对象
 * @returns 传输层响应解析结果
 */
async function parseResponse<T>(response: Response): Promise<RawApiResponse<T>> {
  const contentType = response.headers.get("content-type") ?? "";
  if (!contentType.includes("application/json")) {
    return {};
  }

  return (await response.json()) as RawApiResponse<T>;
}

/**
 * 构造结构化请求错误。
 *
 * @param httpStatus HTTP 状态码
 * @param error 后端错误对象
 * @param fallbackMessage 默认错误消息
 * @returns 结构化请求错误
 */
function createRequestError(httpStatus: number, error: RawApiError | null | undefined, fallbackMessage: string) {
  return new HttpRequestError(
    httpStatus,
    error?.code,
    error?.message ?? fallbackMessage,
    error?.details
  );
}
