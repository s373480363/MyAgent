package com.myagent.method.application;

import com.myagent.common.page.PageResult;
import com.myagent.method.application.query.GetJavaMethodQuery;
import com.myagent.method.application.query.ListJavaMethodsQuery;
import com.myagent.method.application.result.JavaMethodDetailResult;
import com.myagent.method.application.result.JavaMethodListItemResult;

/**
 * Java 方法应用服务。
 */
public interface JavaMethodApplicationService {

    /**
     * 查询 Java 方法分页列表。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    PageResult<JavaMethodListItemResult> listJavaMethods(ListJavaMethodsQuery query);

    /**
     * 查询 Java 方法详情。
     *
     * @param query 查询条件
     * @return 详情结果
     */
    JavaMethodDetailResult getJavaMethod(GetJavaMethodQuery query);

    /**
     * 刷新 Java 方法目录。
     */
    void refreshJavaMethodCatalog();
}
