package com.myagent.runtime.executor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myagent.common.error.BizException;
import com.myagent.common.error.ErrorCode;
import com.myagent.method.runtime.JavaMethodDescriptor;
import com.myagent.method.runtime.JavaMethodInvoker;
import com.myagent.method.runtime.JavaMethodRegistry;
import com.myagent.run.domain.TraceEventType;
import com.myagent.runtime.NodeExecutionContext;
import com.myagent.runtime.NodeExecutionResult;
import com.myagent.runtime.NodeExecutor;
import com.myagent.runtime.SupportsNodeType;
import com.myagent.runtime.TraceEventRecord;
import com.myagent.schema.validation.ValidationStage;
import com.myagent.workflow.domain.WorkflowNodeType;
import org.springframework.stereotype.Component;

/**
 * JAVA_METHOD 节点执行器。
 */
@Component
public class JavaMethodNodeExecutor extends AbstractNodeExecutorSupport implements NodeExecutor, SupportsNodeType {

    /**
     * Java 方法注册目录。
     */
    private final JavaMethodRegistry javaMethodRegistry;

    /**
     * Java 方法调用器。
     */
    private final JavaMethodInvoker javaMethodInvoker;

    /**
     * 构造 JAVA_METHOD 节点执行器。
     *
     * @param objectMapper JSON 对象映射器
     * @param javaMethodRegistry Java 方法注册目录
     * @param javaMethodInvoker Java 方法调用器
     */
    public JavaMethodNodeExecutor(
            ObjectMapper objectMapper,
            JavaMethodRegistry javaMethodRegistry,
            JavaMethodInvoker javaMethodInvoker
    ) {
        super(objectMapper);
        this.javaMethodRegistry = javaMethodRegistry;
        this.javaMethodInvoker = javaMethodInvoker;
    }

    /**
     * 返回支持的节点类型。
     *
     * @return 节点类型
     */
    @Override
    public WorkflowNodeType supportedNodeType() {
        return WorkflowNodeType.JAVA_METHOD;
    }

    /**
     * 执行 JAVA_METHOD 节点。
     *
     * @param context 节点执行上下文
     * @return 节点执行结果
     */
    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        long startedAt = System.nanoTime();
        JsonNode input = extractInput(context);
        validateSchema(context, input, context.nodeDefinition().getInputSchemaRef(), ValidationStage.NODE_INPUT);
        String methodKey = null;
        JsonNode output = null;
        try {
            methodKey = readRequiredConfigText(context.nodeDefinition().getConfig(), "methodKey", "Java 方法节点缺少 methodKey。");
            JavaMethodDescriptor descriptor = javaMethodRegistry.getEnabledMethod(methodKey);
            output = javaMethodInvoker.invoke(descriptor, input);
            validateOutputSchema(context, output, context.nodeDefinition().getOutputSchemaRef(), ValidationStage.NODE_OUTPUT);
            long durationMs = elapsedMillis(startedAt);
            writeJavaMethodTrace(context, methodKey, input, output, durationMs, null);
            return NodeExecutionResult.success(output, durationMs);
        } catch (BizException exception) {
            writeJavaMethodTrace(context, methodKey, input, output, elapsedMillis(startedAt), exception.getMessage());
            throw exception;
        }
    }

    /**
     * 写入 Java 方法调用 Trace。
     *
     * @param context 节点执行上下文
     * @param methodKey 方法标识
     * @param input 输入 JSON
     * @param output 输出 JSON
     * @param durationMs 耗时毫秒
     * @param error 错误消息
     */
    private void writeJavaMethodTrace(
            NodeExecutionContext context,
            String methodKey,
            JsonNode input,
            JsonNode output,
            long durationMs,
            String error
    ) {
        com.fasterxml.jackson.databind.node.ObjectNode detail = objectMapper.createObjectNode()
                .put("methodKey", methodKey)
                .put("durationMs", durationMs)
                .put("error", error);
        detail.set("input", input);
        detail.set("output", output);
        context.traceWriter().writeEvent(new TraceEventRecord(
                context.agentRunDbId(),
                context.nodeRunDbId(),
                null,
                TraceEventType.JAVA_METHOD_CALL,
                error == null ? "Java 方法节点执行完成：" + methodKey : "Java 方法节点执行失败：" + methodKey,
                detail
        ));
    }

    /**
     * 读取必填文本配置。
     *
     * @param config 节点配置
     * @param fieldName 字段名
     * @param message 错误消息
     * @return 文本值
     */
    private String readRequiredConfigText(JsonNode config, String fieldName, String message) {
        if (config != null && config.hasNonNull(fieldName) && config.get(fieldName).isTextual()
                && !config.get(fieldName).asText().isBlank()) {
            return config.get(fieldName).asText();
        }
        throw new BizException(ErrorCode.JAVA_METHOD_EXECUTION_FAILED, message);
    }

    /**
     * 计算耗时。
     *
     * @param startedAtNanos 开始纳秒
     * @return 耗时毫秒
     */
    private long elapsedMillis(long startedAtNanos) {
        return java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNanos);
    }
}
