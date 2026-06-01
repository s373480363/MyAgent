package com.myagent.method.runtime;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Java 方法调用边界。
 */
public interface JavaMethodInvoker {

    /**
     * 调用已注册 Java 方法。
     *
     * @param descriptor 方法描述
     * @param input 节点输入
     * @return 方法输出
     */
    JsonNode invoke(JavaMethodDescriptor descriptor, JsonNode input);
}
