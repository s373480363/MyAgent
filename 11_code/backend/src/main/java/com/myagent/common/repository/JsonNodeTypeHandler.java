package com.myagent.common.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * JsonNode 与 PostgreSQL jsonb 字段之间的 MyBatis 类型转换器。
 */
@MappedTypes(JsonNode.class)
@MappedJdbcTypes(JdbcType.OTHER)
public class JsonNodeTypeHandler extends BaseTypeHandler<JsonNode> {

    /**
     * JSON 对象映射器。
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 设置非空 JSON 参数。
     *
     * @param ps 预编译语句
     * @param i 参数位置
     * @param parameter JSON 参数
     * @param jdbcType JDBC 类型
     * @throws SQLException 设置参数失败时抛出
     */
    @Override
    public void setNonNullParameter(
            PreparedStatement ps,
            int i,
            JsonNode parameter,
            JdbcType jdbcType
    ) throws SQLException {
        ps.setObject(i, parameter.toString(), Types.OTHER);
    }

    /**
     * 按列名读取 JSON。
     *
     * @param rs 结果集
     * @param columnName 列名
     * @return JSON 节点
     * @throws SQLException 读取失败时抛出
     */
    @Override
    public JsonNode getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parseJson(rs.getString(columnName));
    }

    /**
     * 按列序号读取 JSON。
     *
     * @param rs 结果集
     * @param columnIndex 列序号
     * @return JSON 节点
     * @throws SQLException 读取失败时抛出
     */
    @Override
    public JsonNode getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parseJson(rs.getString(columnIndex));
    }

    /**
     * 从存储过程读取 JSON。
     *
     * @param cs 存储过程语句
     * @param columnIndex 列序号
     * @return JSON 节点
     * @throws SQLException 读取失败时抛出
     */
    @Override
    public JsonNode getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parseJson(cs.getString(columnIndex));
    }

    /**
     * 解析 JSON 字符串。
     *
     * @param value JSON 字符串
     * @return JSON 节点
     * @throws SQLException 解析失败时抛出
     */
    private JsonNode parseJson(String value) throws SQLException {
        if (value == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readTree(value);
        } catch (Exception exception) {
            throw new SQLException("JSON 字段解析失败。", exception);
        }
    }
}
