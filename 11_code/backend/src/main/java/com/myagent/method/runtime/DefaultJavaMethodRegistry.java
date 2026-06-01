package com.myagent.method.runtime;

import com.myagent.common.domain.EnableStatus;
import com.myagent.common.error.BizException;
import com.myagent.common.error.ErrorCode;
import com.myagent.method.repository.JavaMethodRecord;
import com.myagent.method.repository.JavaMethodRepository;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认 Java 方法注册目录。
 */
@Component
public class DefaultJavaMethodRegistry implements JavaMethodRegistry {

    /**
     * Spring 应用上下文。
     */
    private final ApplicationContext applicationContext;

    /**
     * Java 方法仓储。
     */
    private final JavaMethodRepository javaMethodRepository;

    /**
     * 已扫描到的显式注册方法。
     */
    private final Map<String, RegisteredMethodHandle> registeredMethods = new ConcurrentHashMap<>();

    /**
     * 构造 Java 方法注册目录。
     *
     * @param applicationContext Spring 应用上下文
     * @param javaMethodRepository Java 方法仓储
     */
    public DefaultJavaMethodRegistry(ApplicationContext applicationContext, JavaMethodRepository javaMethodRepository) {
        this.applicationContext = applicationContext;
        this.javaMethodRepository = javaMethodRepository;
    }

    /**
     * 刷新显式注册方法目录。
     */
    @Override
    public final void refresh() {
        Map<String, RegisteredMethodHandle> refreshed = new ConcurrentHashMap<>();
        for (String beanName : applicationContext.getBeanDefinitionNames()) {
            Object bean = applicationContext.getBean(beanName);
            Class<?> beanClass = AopUtils.getTargetClass(bean);
            for (Method method : beanClass.getMethods()) {
                RegisteredJavaMethod annotation = method.getAnnotation(RegisteredJavaMethod.class);
                if (annotation == null) {
                    continue;
                }
                validateMethod(annotation.methodKey(), method);
                RegisteredMethodHandle previous = refreshed.putIfAbsent(
                        annotation.methodKey(),
                        new RegisteredMethodHandle(beanName, bean, method)
                );
                if (previous != null) {
                    throw new BizException(ErrorCode.JAVA_METHOD_EXECUTION_FAILED, "Java 方法重复注册：" + annotation.methodKey());
                }
            }
        }
        registeredMethods.clear();
        registeredMethods.putAll(refreshed);
    }

    /**
     * 获取已启用且可执行的方法描述。
     *
     * @param methodKey 方法业务标识
     * @return 方法运行时描述
     */
    @Override
    public JavaMethodDescriptor getEnabledMethod(String methodKey) {
        JavaMethodRecord record = javaMethodRepository.findByMethodKey(methodKey)
                .orElseThrow(() -> new BizException(ErrorCode.JAVA_METHOD_EXECUTION_FAILED, "Java 方法不存在：" + methodKey));
        if (record.status() != EnableStatus.ENABLED) {
            throw new BizException(ErrorCode.JAVA_METHOD_EXECUTION_FAILED, "Java 方法已停用：" + methodKey);
        }
        if (registeredMethods.isEmpty()) {
            refresh();
        }
        RegisteredMethodHandle handle = registeredMethods.get(methodKey);
        if (handle == null) {
            throw new BizException(ErrorCode.JAVA_METHOD_EXECUTION_FAILED, "Java 方法未显式注册：" + methodKey);
        }
        if (!record.beanName().equals(handle.beanName()) || !record.methodName().equals(handle.method().getName())) {
            throw new BizException(ErrorCode.JAVA_METHOD_EXECUTION_FAILED, "Java 方法注册目录与主数据不一致：" + methodKey);
        }
        return new JavaMethodDescriptor(record, handle.bean(), handle.method());
    }

    /**
     * 校验注册方法形态。
     *
     * @param methodKey 方法业务标识
     * @param method Java 方法
     */
    private void validateMethod(String methodKey, Method method) {
        if (methodKey == null || methodKey.isBlank()) {
            throw new BizException(ErrorCode.JAVA_METHOD_EXECUTION_FAILED, "Java 方法注册标识不能为空。");
        }
        if (method.getParameterCount() > 1) {
            throw new BizException(ErrorCode.JAVA_METHOD_EXECUTION_FAILED, "Java 方法最多允许一个入参：" + methodKey);
        }
    }

    /**
     * 显式注册方法句柄。
     *
     * @param beanName Spring Bean 名称
     * @param bean Spring Bean 实例
     * @param method Java 方法
     */
    private record RegisteredMethodHandle(String beanName, Object bean, Method method) {
    }
}
