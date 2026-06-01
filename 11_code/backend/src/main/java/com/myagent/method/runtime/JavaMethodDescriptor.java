package com.myagent.method.runtime;

import com.myagent.method.repository.JavaMethodRecord;

import java.lang.reflect.Method;

/**
 * Java 方法运行时描述。
 *
 * @param record Java 方法主数据
 * @param bean Spring Bean 实例
 * @param method 已显式注册的方法
 */
public record JavaMethodDescriptor(
        JavaMethodRecord record,
        Object bean,
        Method method
) {
}
