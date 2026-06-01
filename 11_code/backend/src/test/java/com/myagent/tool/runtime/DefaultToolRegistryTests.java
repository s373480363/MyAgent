package com.myagent.tool.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myagent.common.domain.EnableStatus;
import com.myagent.common.error.BizException;
import com.myagent.common.page.PageResult;
import com.myagent.tool.application.query.ListToolsQuery;
import com.myagent.tool.repository.ToolRecord;
import com.myagent.tool.repository.ToolRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 默认工具注册目录测试。
 */
class DefaultToolRegistryTests {

    /**
     * JSON 对象映射器。
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 已启用工具可以绑定到对应执行器。
     */
    @Test
    void getEnabledToolReturnsDescriptorWithExecutor() {
        DefaultToolRegistry registry = new DefaultToolRegistry(
                new InMemoryToolRepository(Map.of("tool.echo", toolRecord("tool.echo", "ECHO", EnableStatus.ENABLED))),
                List.of(new EchoToolExecutor())
        );

        ToolDescriptor descriptor = registry.getEnabledTool("tool.echo");

        assertThat(descriptor.executor().executorType()).isEqualTo("ECHO");
    }

    /**
     * 停用工具不能执行。
     */
    @Test
    void getEnabledToolRejectsDisabledTool() {
        DefaultToolRegistry registry = new DefaultToolRegistry(
                new InMemoryToolRepository(Map.of("tool.echo", toolRecord("tool.echo", "ECHO", EnableStatus.DISABLED))),
                List.of(new EchoToolExecutor())
        );

        assertThatThrownBy(() -> registry.getEnabledTool("tool.echo"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("已停用");
    }

    /**
     * 未注册执行器类型不能执行。
     */
    @Test
    void getEnabledToolRejectsUnknownExecutorType() {
        DefaultToolRegistry registry = new DefaultToolRegistry(
                new InMemoryToolRepository(Map.of("tool.missing", toolRecord("tool.missing", "MISSING", EnableStatus.ENABLED))),
                List.of(new EchoToolExecutor())
        );

        assertThatThrownBy(() -> registry.getEnabledTool("tool.missing"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("工具执行器未注册");
    }

    /**
     * 构造工具记录。
     *
     * @param toolKey 工具标识
     * @param executorType 执行器类型
     * @param status 启停状态
     * @return 工具记录
     */
    private ToolRecord toolRecord(String toolKey, String executorType, EnableStatus status) {
        return new ToolRecord(
                1L,
                toolKey,
                "测试工具",
                "",
                1L,
                2L,
                executorType,
                objectMapper.createObjectNode(),
                status,
                Instant.now(),
                Instant.now()
        );
    }

    /**
     * 内存工具仓储。
     */
    private record InMemoryToolRepository(Map<String, ToolRecord> records) implements ToolRepository {

        /**
         * 分页查询工具。
         *
         * @param query 查询条件
         * @return 分页结果
         */
        @Override
        public PageResult<ToolRecord> listTools(ListToolsQuery query) {
            return PageResult.of(records.values().stream().toList(), query.page(), query.pageSize(), records.size());
        }

        /**
         * 按主键查询工具。
         *
         * @param toolId 工具主键
         * @return 工具记录
         */
        @Override
        public Optional<ToolRecord> findById(long toolId) {
            return records.values().stream().filter(record -> record.id() == toolId).findFirst();
        }

        /**
         * 按工具标识查询工具。
         *
         * @param toolKey 工具标识
         * @return 工具记录
         */
        @Override
        public Optional<ToolRecord> findByToolKey(String toolKey) {
            return Optional.ofNullable(records.get(toolKey));
        }
    }
}
