package com.myagent.method.runtime;

import com.myagent.common.domain.EnableStatus;
import com.myagent.common.error.BizException;
import com.myagent.common.page.PageResult;
import com.myagent.method.application.query.ListJavaMethodsQuery;
import com.myagent.method.repository.JavaMethodRecord;
import com.myagent.method.repository.JavaMethodRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 默认 Java 方法注册目录测试。
 */
class DefaultJavaMethodRegistryTests {

    /**
     * 已启用且显式注册的方法可以返回运行时描述。
     */
    @Test
    void getEnabledMethodReturnsRegisteredDescriptor() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.registerBean("sampleJavaBean", SampleJavaBean.class);
        context.refresh();
        DefaultJavaMethodRegistry registry = new DefaultJavaMethodRegistry(
                context,
                new InMemoryJavaMethodRepository(Map.of(
                        "sample.echo",
                        javaMethodRecord("sample.echo", EnableStatus.ENABLED, "sampleJavaBean", "echo")
                ))
        );

        JavaMethodDescriptor descriptor = registry.getEnabledMethod("sample.echo");

        assertThat(descriptor.method().getName()).isEqualTo("echo");
        assertThat(descriptor.bean()).isInstanceOf(SampleJavaBean.class);
        context.close();
    }

    /**
     * 停用方法不能被执行。
     */
    @Test
    void getEnabledMethodRejectsDisabledMethod() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.registerBean("sampleJavaBean", SampleJavaBean.class);
        context.refresh();
        DefaultJavaMethodRegistry registry = new DefaultJavaMethodRegistry(
                context,
                new InMemoryJavaMethodRepository(Map.of(
                        "sample.echo",
                        javaMethodRecord("sample.echo", EnableStatus.DISABLED, "sampleJavaBean", "echo")
                ))
        );

        assertThatThrownBy(() -> registry.getEnabledMethod("sample.echo"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("已停用");
        context.close();
    }

    /**
     * 主数据存在但没有显式注册的方法不能执行。
     */
    @Test
    void getEnabledMethodRejectsUnregisteredMethod() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.registerBean("sampleJavaBean", SampleJavaBean.class);
        context.refresh();
        DefaultJavaMethodRegistry registry = new DefaultJavaMethodRegistry(
                context,
                new InMemoryJavaMethodRepository(Map.of(
                        "sample.missing",
                        javaMethodRecord("sample.missing", EnableStatus.ENABLED, "sampleJavaBean", "missing")
                ))
        );

        assertThatThrownBy(() -> registry.getEnabledMethod("sample.missing"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("未显式注册");
        context.close();
    }

    /**
     * 构造 Java 方法记录。
     *
     * @param methodKey 方法标识
     * @param status 启停状态
     * @param beanName Bean 名称
     * @param methodName 方法名
     * @return Java 方法记录
     */
    private static JavaMethodRecord javaMethodRecord(
            String methodKey,
            EnableStatus status,
            String beanName,
            String methodName
    ) {
        return new JavaMethodRecord(
                1L,
                methodKey,
                "测试方法",
                "",
                beanName,
                methodName,
                1L,
                2L,
                status,
                Instant.now(),
                Instant.now()
        );
    }

    /**
     * 测试 Java Bean。
     */
    public static final class SampleJavaBean {

        /**
         * 显式注册的测试方法。
         *
         * @return 测试输出
         */
        @RegisteredJavaMethod(methodKey = "sample.echo")
        public String echo() {
            return "ok";
        }
    }

    /**
     * 内存 Java 方法仓储。
     */
    private record InMemoryJavaMethodRepository(
            Map<String, JavaMethodRecord> records
    ) implements JavaMethodRepository {

        /**
         * 分页查询 Java 方法。
         *
         * @param query 查询条件
         * @return 分页结果
         */
        @Override
        public PageResult<JavaMethodRecord> listJavaMethods(ListJavaMethodsQuery query) {
            return PageResult.of(records.values().stream().toList(), query.page(), query.pageSize(), records.size());
        }

        /**
         * 按主键查询 Java 方法。
         *
         * @param methodId 方法主键
         * @return 方法记录
         */
        @Override
        public Optional<JavaMethodRecord> findById(long methodId) {
            return records.values().stream().filter(record -> record.id() == methodId).findFirst();
        }

        /**
         * 按方法标识查询 Java 方法。
         *
         * @param methodKey 方法标识
         * @return 方法记录
         */
        @Override
        public Optional<JavaMethodRecord> findByMethodKey(String methodKey) {
            return Optional.ofNullable(records.get(methodKey));
        }
    }
}
