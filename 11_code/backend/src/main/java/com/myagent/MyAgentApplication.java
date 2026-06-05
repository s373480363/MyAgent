package com.myagent;

import com.myagent.config.LegacyOpenAiEnvironmentGuard;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * MyAgent 后端应用启动入口。
 */
@SpringBootApplication
public class MyAgentApplication {

    /**
     * 启动 Spring Boot 应用。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        // 启动前先拒绝旧 OpenAI 环境变量，避免部署入口出现第二套正式配置。
        LegacyOpenAiEnvironmentGuard.validate(System.getenv());
        // 统一从单体应用入口启动，后续所有模块都在同一 Spring 容器内装配。
        SpringApplication.run(MyAgentApplication.class, args);
    }
}
