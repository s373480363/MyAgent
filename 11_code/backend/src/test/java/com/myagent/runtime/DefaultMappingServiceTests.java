package com.myagent.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myagent.common.error.BizException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 默认映射服务测试。
 */
class DefaultMappingServiceTests {

    /**
     * JSON 对象映射器。
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 映射服务。
     */
    private final DefaultMappingService mappingService = new DefaultMappingService(objectMapper);

    /**
     * 输入映射支持点路径和数组下标，并按字段生成节点输入。
     *
     * @throws Exception JSON 解析失败时抛出
     */
    @Test
    void extractInputSupportsControlledJsonPathSubset() throws Exception {
        JsonNode context = objectMapper.readTree("""
                {
                  "input": {
                    "items": [
                      { "title": "第一项" }
                    ]
                  }
                }
                """);
        JsonNode mapping = objectMapper.readTree("""
                {
                  "title": "$.input.items[0].title"
                }
                """);

        JsonNode result = mappingService.extractInput(context, mapping);

        assertThat(result.get("title").asText()).isEqualTo("第一项");
    }

    /**
     * 读取失败必须直接抛错，不能静默返回 null。
     *
     * @throws Exception JSON 解析失败时抛出
     */
    @Test
    void extractInputFailsWhenPathMissing() throws Exception {
        JsonNode context = objectMapper.readTree("{\"input\":{}}");

        assertThatThrownBy(() -> mappingService.extractInput(context, objectMapper.readTree("\"$.input.missing\"")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("映射读取失败");
    }

    /**
     * 输出写回允许自动创建缺失中间对象。
     *
     * @throws Exception JSON 解析失败时抛出
     */
    @Test
    void applyOutputCreatesMissingObjects() throws Exception {
        JsonNode context = objectMapper.readTree("{\"input\":{}}");
        JsonNode output = objectMapper.readTree("{\"summary\":\"完成\"}");

        JsonNode result = mappingService.applyOutput(context, objectMapper.readTree("\"$.result.summary\""), output);

        assertThat(result.at("/result/summary/summary").asText()).isEqualTo("完成");
    }

    /**
     * 输出写回禁止覆盖运行输入。
     *
     * @throws Exception JSON 解析失败时抛出
     */
    @Test
    void applyOutputRejectsInputOverwrite() throws Exception {
        JsonNode context = objectMapper.readTree("{\"input\":{\"question\":\"原始问题\"}}");
        JsonNode output = objectMapper.readTree("{\"question\":\"覆盖\"}");

        assertThatThrownBy(() -> mappingService.applyOutput(context, objectMapper.readTree("\"$.input.question\""), output))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不允许覆盖 $.input");
    }
}
