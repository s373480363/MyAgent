package com.myagent.common.repository;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

/**
 * Instant 与 PostgreSQL timestamptz 的 MyBatis 类型转换器。
 */
@MappedTypes(Instant.class)
@MappedJdbcTypes({JdbcType.TIMESTAMP, JdbcType.TIMESTAMP_WITH_TIMEZONE})
public class InstantTypeHandler extends BaseTypeHandler<Instant> {

    /**
     * 设置非空参数。
     *
     * @param ps 预编译语句
     * @param i 参数位置
     * @param parameter 时间值
     * @param jdbcType JDBC 类型
     * @throws SQLException 设置失败时抛出
     */
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Instant parameter, JdbcType jdbcType) throws SQLException {
        ps.setTimestamp(i, Timestamp.from(parameter));
    }

    /**
     * 按列名读取时间。
     *
     * @param rs 结果集
     * @param columnName 列名
     * @return 时间值
     * @throws SQLException 读取失败时抛出
     */
    @Override
    public Instant getNullableResult(ResultSet rs, String columnName) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(columnName);
        return timestamp == null ? null : timestamp.toInstant();
    }

    /**
     * 按列序号读取时间。
     *
     * @param rs 结果集
     * @param columnIndex 列序号
     * @return 时间值
     * @throws SQLException 读取失败时抛出
     */
    @Override
    public Instant getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(columnIndex);
        return timestamp == null ? null : timestamp.toInstant();
    }

    /**
     * 从存储过程读取时间。
     *
     * @param cs 存储过程语句
     * @param columnIndex 列序号
     * @return 时间值
     * @throws SQLException 读取失败时抛出
     */
    @Override
    public Instant getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        Timestamp timestamp = cs.getTimestamp(columnIndex);
        return timestamp == null ? null : timestamp.toInstant();
    }
}
