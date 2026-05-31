package com.myagent.method.repository;

import com.myagent.common.page.PageResult;
import com.myagent.method.application.query.ListJavaMethodsQuery;

import java.util.Optional;

/**
 * Java 方法仓储接口。
 */
public interface JavaMethodRepository {

    /**
     * 分页查询 Java 方法。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    PageResult<JavaMethodRecord> listJavaMethods(ListJavaMethodsQuery query);

    /**
     * 按主键查询 Java 方法。
     *
     * @param methodId 方法主键
     * @return 方法记录
     */
    Optional<JavaMethodRecord> findById(long methodId);

    /**
     * 按方法标识查询 Java 方法。
     *
     * @param methodKey 方法标识
     * @return 方法记录
     */
    Optional<JavaMethodRecord> findByMethodKey(String methodKey);
}
