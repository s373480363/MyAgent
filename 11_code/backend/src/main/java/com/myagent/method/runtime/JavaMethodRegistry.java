package com.myagent.method.runtime;

/**
 * Java 方法注册目录。
 */
public interface JavaMethodRegistry {

    /**
     * 刷新显式注册方法目录。
     */
    void refresh();

    /**
     * 获取已启用且可执行的方法描述。
     *
     * @param methodKey 方法业务标识
     * @return 方法运行时描述
     */
    JavaMethodDescriptor getEnabledMethod(String methodKey);
}
