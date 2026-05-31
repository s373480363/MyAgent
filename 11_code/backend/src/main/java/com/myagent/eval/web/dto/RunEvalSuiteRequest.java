package com.myagent.eval.web.dto;

import java.util.List;

/**
 * 运行验收套件请求。
 *
 * @param caseIds 指定用例主键
 * @param includeUnconfirmed 是否包含未确认用例
 */
public record RunEvalSuiteRequest(List<Long> caseIds, boolean includeUnconfirmed) {
}
