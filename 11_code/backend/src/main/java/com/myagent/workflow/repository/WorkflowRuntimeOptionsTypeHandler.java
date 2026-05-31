package com.myagent.workflow.repository;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.myagent.common.repository.AbstractJsonValueTypeHandler;
import com.myagent.workflow.domain.WorkflowRuntimeOptions;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.JdbcType;

/**
 * 工作流运行约束 JSONB 类型处理器。
 */
@MappedJdbcTypes(JdbcType.OTHER)
public class WorkflowRuntimeOptionsTypeHandler extends AbstractJsonValueTypeHandler<WorkflowRuntimeOptions> {

    /**
     * 返回目标 JavaType。
     *
     * @return JavaType
     */
    @Override
    protected JavaType getJavaType() {
        return TypeFactory.defaultInstance().constructType(WorkflowRuntimeOptions.class);
    }
}
