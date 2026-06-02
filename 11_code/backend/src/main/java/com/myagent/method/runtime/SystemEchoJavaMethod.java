package com.myagent.method.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import org.springframework.stereotype.Component;

/**
 * V1 内置示例 Java 方法。
 *
 * <p>该方法作为正式交付的系统级主数据实现，用于验证 JAVA_METHOD 节点的
 * 目录发现、方法调用、NodeRun 和 Trace 全链路。
 */
@Component
public class SystemEchoJavaMethod {

    /**
     * 按原样回显节点输入。
     *
     * @param input 节点输入 JSON
     * @return 原样返回的节点输出 JSON
     */
    @RegisteredJavaMethod(methodKey = "java.sample.echo")
    public JsonNode execute(JsonNode input) {
        if (input == null) {
            return NullNode.getInstance();
        }
        return input.deepCopy();
    }
}
