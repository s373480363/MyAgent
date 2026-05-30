/**
 * 前端共享类型入口。
 *
 * 步骤 03 起从 OpenAPI 生成类型入口统一导出，业务代码不得手工维护长期影子 DTO。
 */
import type { components } from "../../api/generated/schema";

export type ApiResponse = components["schemas"]["ApiResponse"];
export type ApiError = components["schemas"]["ApiError"];
export type ApiErrorDetail = components["schemas"]["ApiErrorDetail"];
export type PageResponse = components["schemas"]["PageResponse"];
