package com.myagent.schema.application;

import com.myagent.common.page.PageResult;
import com.myagent.schema.application.command.CreateSchemaCommand;
import com.myagent.schema.application.command.CreateSchemaVersionCommand;
import com.myagent.schema.application.command.UpdateSchemaDraftCommand;
import com.myagent.schema.application.query.GetSchemaQuery;
import com.myagent.schema.application.query.ListSchemasQuery;
import com.myagent.schema.application.result.SchemaDetailResult;
import com.myagent.schema.application.result.SchemaListItemResult;

/**
 * Schema 应用服务接口。
 */
public interface SchemaApplicationService {

    /**
     * 分页查询 Schema。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    PageResult<SchemaListItemResult> listSchemas(ListSchemasQuery query);

    /**
     * 创建 Schema。
     *
     * @param command 创建命令
     * @return Schema 详情
     */
    SchemaDetailResult createSchema(CreateSchemaCommand command);

    /**
     * 更新 Schema 草稿。
     *
     * @param command 更新命令
     * @return Schema 详情
     */
    SchemaDetailResult updateSchemaDraft(UpdateSchemaDraftCommand command);

    /**
     * 查询 Schema 详情。
     *
     * @param query 查询条件
     * @return Schema 详情
     */
    SchemaDetailResult getSchema(GetSchemaQuery query);

    /**
     * 基于旧版本创建新版本。
     *
     * @param command 创建版本命令
     * @return Schema 详情
     */
    SchemaDetailResult createSchemaVersion(CreateSchemaVersionCommand command);

    /**
     * 锁定 Schema 版本。
     *
     * @param schemaId Schema 主键
     */
    void lockSchemaVersion(long schemaId);
}
