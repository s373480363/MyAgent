package com.myagent.method.application;

import com.myagent.common.error.BizException;
import com.myagent.common.error.ErrorCode;
import com.myagent.common.page.PageResult;
import com.myagent.method.application.query.GetJavaMethodQuery;
import com.myagent.method.application.query.ListJavaMethodsQuery;
import com.myagent.method.application.result.JavaMethodDetailResult;
import com.myagent.method.application.result.JavaMethodListItemResult;
import com.myagent.method.repository.JavaMethodRecord;
import com.myagent.method.repository.JavaMethodRepository;
import org.springframework.stereotype.Service;

/**
 * Java 方法应用服务默认实现。
 */
@Service
public class DefaultJavaMethodApplicationService implements JavaMethodApplicationService {

    /**
     * Java 方法仓储。
     */
    private final JavaMethodRepository javaMethodRepository;

    /**
     * 构造 Java 方法应用服务。
     *
     * @param javaMethodRepository Java 方法仓储
     */
    public DefaultJavaMethodApplicationService(JavaMethodRepository javaMethodRepository) {
        this.javaMethodRepository = javaMethodRepository;
    }

    /**
     * 查询 Java 方法分页列表。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    @Override
    public PageResult<JavaMethodListItemResult> listJavaMethods(ListJavaMethodsQuery query) {
        return javaMethodRepository.listJavaMethods(query).map(this::toListItem);
    }

    /**
     * 查询 Java 方法详情。
     *
     * @param query 查询条件
     * @return 详情结果
     */
    @Override
    public JavaMethodDetailResult getJavaMethod(GetJavaMethodQuery query) {
        JavaMethodRecord record = javaMethodRepository.findById(query.methodId())
                .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND, "指定 Java 方法不存在。"));
        return toDetail(record);
    }

    /**
     * 刷新 Java 方法目录。
     */
    @Override
    public void refreshJavaMethodCatalog() {
        // 步骤 05 仅落地主数据查询链路；实际执行器和注解扫描在后续运行时步骤实现。
    }

    /**
     * 转换列表项结果。
     *
     * @param record 持久化记录
     * @return 列表项结果
     */
    private JavaMethodListItemResult toListItem(JavaMethodRecord record) {
        return new JavaMethodListItemResult(
                record.id(),
                record.methodKey(),
                record.name(),
                record.description(),
                record.beanName(),
                record.methodName(),
                record.inputSchemaId(),
                record.outputSchemaId(),
                record.status(),
                record.createdAt(),
                record.updatedAt()
        );
    }

    /**
     * 转换详情结果。
     *
     * @param record 持久化记录
     * @return 详情结果
     */
    private JavaMethodDetailResult toDetail(JavaMethodRecord record) {
        return new JavaMethodDetailResult(
                record.id(),
                record.methodKey(),
                record.name(),
                record.description(),
                record.beanName(),
                record.methodName(),
                record.inputSchemaId(),
                record.outputSchemaId(),
                record.status(),
                record.createdAt(),
                record.updatedAt()
        );
    }
}
