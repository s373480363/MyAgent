package com.myagent.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.FileSystemResource;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 主配置文件契约测试。
 */
class ApplicationConfigurationContractTests {

    /**
     * 正式部署入口必须把 AGENT_STUDIO_OPENAI_API_KEY 单向映射给 Spring AI 自动配置。
     */
    @Test
    void applicationYmlMapsFormalOpenAiKeyToSpringAiProperty() {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new FileSystemResource("src/main/resources/application.yml"));

        Properties properties = factory.getObject();

        assertThat(properties).isNotNull();
        assertThat(properties.getProperty("spring.ai.openai.api-key"))
                .isEqualTo("${AGENT_STUDIO_OPENAI_API_KEY:}");
    }
}
