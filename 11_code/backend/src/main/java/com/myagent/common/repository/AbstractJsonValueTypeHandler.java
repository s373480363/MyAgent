package com.myagent.common.repository;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * 基于 Jackson 的通用 JSON 值类型处理器。
 *
 * @param <T> Java 类型
 */
public abstract class AbstractJsonValueTypeHandler<T> extends BaseTypeHandler<T> {

    /**
     * JSON 对象映射器。
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 返回当前处理器负责的 JavaType。
     *
     * @return JavaType
     */
    protected abstract JavaType getJavaType();

    /**
     * 设置非空参数。
     *
     * @param ps 预编译语句
     * @param i 参数位置
     * @param parameter 参数值
     * @param jdbcType JDBC 类型
     * @throws SQLException 序列化失败时抛出
     */
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType) throws SQLException {
        try {
            ps.setObject(i, OBJECT_MAPPER.writeValueAsString(parameter), Types.OTHER);
        } catch (Exception exception) {
            throw new SQLException("JSON 字段序列化失败。", exception);
        }
    }

    /**
     * 按列名读取结果。
     *
     * @param rs 结果集
     * @param columnName 列名
     * @return 解析后的对象
     * @throws SQLException 解析失败时抛出
     */
    @Override
    public T getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parseJson(rs.getString(columnName));
    }

    /**
     * 按列序号读取结果。
     *
     * @param rs 结果集
     * @param columnIndex 列序号
     * @return 解析后的对象
     * @throws SQLException 解析失败时抛出
     */
    @Override
    public T getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parseJson(rs.getString(columnIndex));
    }

    /**
     * 读取存储过程结果。
     *
     * @param cs 调用语句
     * @param columnIndex 列序号
     * @return 解析后的对象
     * @throws SQLException 解析失败时抛出
     */
    @Override
    public T getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parseJson(cs.getString(columnIndex));
    }

    /**
     * 解析 JSON 字符串。
     *
     * @param value JSON 字符串
     * @return Java 对象
     * @throws SQLException 解析失败时抛出
     */
    private T parseJson(String value) throws SQLException {
        if (value == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(value, getJavaType());
        } catch (Exception exception) {
            throw new SQLException("JSON 字段解析失败。", exception);
        }
    }
}
