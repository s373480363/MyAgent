package com.myagent.method.repository;

import com.myagent.common.page.PageResult;
import com.myagent.method.application.query.ListJavaMethodsQuery;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 基于 MyBatis 的 Java 方法仓储实现。
 */
@Repository
public class MyBatisJavaMethodRepository implements JavaMethodRepository {

    /**
     * MyBatis Mapper。
     */
    private final JavaMethodMapper javaMethodMapper;

    /**
     * 构造 Java 方法仓储。
     *
     * @param javaMethodMapper MyBatis Mapper
     */
    public MyBatisJavaMethodRepository(JavaMethodMapper javaMethodMapper) {
        this.javaMethodMapper = javaMethodMapper;
    }

    /**
     * 分页查询 Java 方法。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    @Override
    public PageResult<JavaMethodRecord> listJavaMethods(ListJavaMethodsQuery query) {
        long offset = (query.page() - 1) * query.pageSize();
        return PageResult.of(
                javaMethodMapper.listJavaMethods(query.keyword(), query.status(), query.pageSize(), offset),
                query.page(),
                query.pageSize(),
                javaMethodMapper.countJavaMethods(query.keyword(), query.status())
        );
    }

    /**
     * 按主键查询 Java 方法。
     *
     * @param methodId 方法主键
     * @return 方法记录
     */
    @Override
    public Optional<JavaMethodRecord> findById(long methodId) {
        return Optional.ofNullable(javaMethodMapper.findById(methodId));
    }

    /**
     * 按方法标识查询 Java 方法。
     *
     * @param methodKey 方法标识
     * @return 方法记录
     */
    @Override
    public Optional<JavaMethodRecord> findByMethodKey(String methodKey) {
        return Optional.ofNullable(javaMethodMapper.findByMethodKey(methodKey));
    }
}
