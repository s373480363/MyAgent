package com.myagent.method.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myagent.common.error.BizException;
import com.myagent.common.error.ErrorCode;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 基于显式注册句柄的 Java 方法调用器。
 */
@Component
public class ReflectiveJavaMethodInvoker implements JavaMethodInvoker {

    /**
     * JSON 对象映射器。
     */
    private final ObjectMapper objectMapper;

    /**
     * 构造 Java 方法调用器。
     *
     * @param objectMapper JSON 对象映射器
     */
    public ReflectiveJavaMethodInvoker(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 调用已注册 Java 方法。
     *
     * @param descriptor 方法描述
     * @param input 节点输入
     * @return 方法输出
     */
    @Override
    public JsonNode invoke(JavaMethodDescriptor descriptor, JsonNode input) {
        try {
            Method method = descriptor.method();
            Object result;
            if (method.getParameterCount() == 0) {
                result = method.invoke(descriptor.bean());
            } else {
                Object argument = objectMapper.treeToValue(input, method.getParameterTypes()[0]);
                result = method.invoke(descriptor.bean(), argument);
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
}
