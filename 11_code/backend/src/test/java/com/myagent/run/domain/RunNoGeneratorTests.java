package com.myagent.run.domain;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 运行编号生成器测试。
 */
class RunNoGeneratorTests {

    /**
     * AgentRun 编号必须使用统一 run 前缀和冻结格式。
     */
    @Test
    void nextRunNoUsesRunPrefixAndFrozenFormat() {
        RunNoGenerator generator = new RunNoGenerator();

        String runNo = generator.nextRunNo();

        assertThat(runNo).matches("run_\\d{14}_[0-9a-f]{8}");
    }

    /**
     * EvalRun 编号必须使用统一 eval 前缀和冻结格式。
     */
    @Test
    void nextEvalRunNoUsesEvalPrefixAndFrozenFormat() {
        RunNoGenerator generator = new RunNoGenerator();

        String runNo = generator.nextEvalRunNo();

        assertThat(runNo).matches("eval_\\d{14}_[0-9a-f]{8}");
    }

    /**
     * RunNoGenerator 只能暴露两个 public 入口，不能再开放自由 prefix API。
     */
    @Test
    void exposesOnlyTwoPublicGenerators() {
        Method[] publicMethods = RunNoGenerator.class.getDeclaredMethods();

        long publicCount = Arrays.stream(publicMethods)
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .count();

        assertThat(publicCount).isEqualTo(2);
        assertThat(Arrays.stream(publicMethods)
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .map(Method::getName)
                .toList())
                .containsExactlyInAnyOrder("nextRunNo", "nextEvalRunNo");
    }
}
