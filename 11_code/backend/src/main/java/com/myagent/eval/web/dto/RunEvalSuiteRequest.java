package com.myagent.eval.web.dto;

import java.util.List;

/**
 * 运行验收套件请求。
 *
 * @param caseIds 指定运行的用例主键列表；为空时运行全部正式用例
 */
public record RunEvalSuiteRequest(List<Long> caseIds) {
}
