package com.myagent.workflow.repository;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.myagent.common.repository.AbstractJsonValueTypeHandler;
import com.myagent.workflow.domain.WorkflowEdgeDefinition;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;

import java.util.List;

/**
 * 工作流边列表 JSONB 类型处理器。
 */
@MappedJdbcTypes(JdbcType.OTHER)
public class WorkflowEdgeDefinitionListTypeHandler extends AbstractJsonValueTypeHandler<List<WorkflowEdgeDefinition>> {

    /**
     * 返回目标 JavaType。
     *
     * @return JavaType
     */
    @Override
    protected JavaType getJavaType() {
        return TypeFactory.defaultInstance().constructCollectionType(List.class, WorkflowEdgeDefinition.class);
    }
}
