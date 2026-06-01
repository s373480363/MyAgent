package com.myagent.method.runtime;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 显式注册为可被工作流调用的 Java 方法。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RegisteredJavaMethod {

    /**
     * 方法业务标识。
     *
     * @return 方法业务标识
     */
    String methodKey();
}
