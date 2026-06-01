package com.myagent.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WorkflowContext 回归测试。
 */
class WorkflowContextTests {

    /**
     * JSON 对象映射器。
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 替换根对象后仍然能够写入节点输出。
     *
     * @throws Exception JSON 解析失败时抛出
     */
    @Test
    void putNodeOutputRecreatesNodesAfterReplaceRoot() throws Exception {
        WorkflowContext context = new WorkflowContext(objectMapper, objectMapper.readTree("""
                {
                  "question": "hello"
                }
                """));

        context.replaceRoot(objectMapper.readTree("""
                {
                  "input": {
                    "question": "updated"
                  }
                }
                """));
        context.putNodeOutput("llm", objectMapper.readTree("""
                {
                  "summary": "ok"
                }
                """));

        assertThat(context.input().path("question").asText()).isEqualTo("updated");
        assertThat(context.root().path("nodes").path("llm").path("summary").asText()).isEqualTo("ok");
    }
}
