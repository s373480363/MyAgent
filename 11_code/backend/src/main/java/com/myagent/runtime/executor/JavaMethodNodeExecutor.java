package com.myagent.runtime.executor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myagent.common.domain.EnableStatus;
import com.myagent.common.error.BizException;
import com.myagent.common.error.ErrorCode;
import com.myagent.method.repository.JavaMethodRecord;
import com.myagent.method.repository.JavaMethodRepository;
import com.myagent.run.domain.TraceEventType;
import com.myagent.runtime.NodeExecutionContext;
import com.myagent.runtime.NodeExecutionResult;
import com.myagent.runtime.NodeExecutor;
import com.myagent.runtime.SupportsNodeType;
import com.myagent.runtime.TraceEventRecord;
import com.myagent.schema.validation.ValidationStage;
import com.myagent.workflow.domain.WorkflowNodeType;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * JAVA_METHOD 节点执行器。
 */
@Component
public class JavaMethodNodeExecutor extends AbstractNodeExecutorSupport implements NodeExecutor, SupportsNodeType {

    /**
     * Java 方法仓储。
     */
    private final JavaMethodRepository javaMethodRepository;

    /**
     * Spring 应用上下文。
     */
    private final ApplicationContext applicationContext;

    /**
     * 构造 JAVA_METHOD 节点执行器。
     *
     * @param objectMapper JSON 对象映射器
     * @param javaMethodRepository Java 方法仓储
     * @param applicationContext Spring 应用上下文
     */
    public JavaMethodNodeExecutor(
            ObjectMapper objectMapper,
            JavaMethodRepository javaMethodRepository,
            ApplicationContext applicationContext
    ) {
        super(objectMapper);
        this.javaMethodRepository = javaMethodRepository;
        this.applicationContext = applicationContext;
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
        String methodKey = readRequiredConfigText(context.nodeDefinition().getConfig(), "methodKey", "Java 方法节点缺少 methodKey。");
        JavaMethodRecord methodRecord = javaMethodRepository.findByMethodKey(methodKey)
                .orElseThrow(() -> new BizException(ErrorCode.JAVA_METHOD_EXECUTION_FAILED, "Java 方法不存在：" + methodKey));
        if (methodRecord.status() != EnableStatus.ENABLED) {
            throw new BizException(ErrorCode.JAVA_METHOD_EXECUTION_FAILED, "Java 方法已停用：" + methodKey);
        }
        JsonNode output = invokeMethod(methodRecord, input);
        validateSchema(context, output, context.nodeDefinition().getOutputSchemaRef(), ValidationStage.NODE_OUTPUT);
        context.traceWriter().writeEvent(new TraceEventRecord(
                context.agentRunDbId(),
                context.nodeRunDbId(),
                null,
                TraceEventType.JAVA_METHOD_CALL,
                "Java 方法节点执行完成：" + methodKey,
                objectMapper.createObjectNode()
                        .put("methodKey", methodKey)
                        .set("output", output)
        ));
        return NodeExecutionResult.success(output, elapsedMillis(startedAt));
    }

    /**
     * 调用显式注册的 Java 方法。
     *
     * @param methodRecord 方法记录
     * @param input 节点输入
     * @return 节点输出
     */
    private JsonNode invokeMethod(JavaMethodRecord methodRecord, JsonNode input) {
        try {
            Object bean = applicationContext.getBean(methodRecord.beanName());
            Method method = findInvokableMethod(bean.getClass(), methodRecord.methodName());
            Object result;
            if (method.getParameterCount() == 0) {
                result = method.invoke(bean);
            } else {
                Object argument = objectMapper.treeToValue(input, method.getParameterTypes()[0]);
                result = method.invoke(bean, argument);
            }
            return objectMapper.valueToTree(result);
        } catch (IllegalArgumentException exception) {
            throw new BizException(ErrorCode.JAVA_METHOD_EXECUTION_FAILED, "Java 方法参数转换失败：" + exception.getMessage());
        } catch (InvocationTargetException exception) {
            Throwable target = exception.getTargetException();
            throw new BizException(ErrorCode.JAVA_METHOD_EXECUTION_FAILED, "Java 方法执行失败：" + target.getMessage());
        } catch (Exception exception) {
            throw new BizException(ErrorCode.JAVA_METHOD_EXECUTION_FAILED, "Java 方法调用失败：" + exception.getMessage());
        }
    }

    /**
     * 查找可调用方法。
     *
     * @param beanClass Bean 类型
     * @param methodName 方法名
     * @return 方法
     */
    private Method findInvokableMethod(Class<?> beanClass, String methodName) {
        for (Method method : beanClass.getMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() <= 1) {
                return method;
            }
        }
        throw new BizException(ErrorCode.JAVA_METHOD_EXECUTION_FAILED, "未找到可调用 Java 方法：" + methodName);
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
