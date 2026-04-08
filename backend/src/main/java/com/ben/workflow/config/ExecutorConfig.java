package com.ben.workflow.config;

import com.ben.dagscheduler.registry.ExecutorRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 执行器配置类
 * <p>
 * 原有的硬编码注册已迁移到 ConfigDrivenExecutorRegistry，
 * 通过 executors.yaml + SPI 自动加载。
 * <p>
 * 此配置类仅确保 ExecutorRegistry Bean 存在。
 */
@Configuration
public class ExecutorConfig {

    private static final Logger logger = LoggerFactory.getLogger(ExecutorConfig.class);

    /**
     * 确保 ExecutorRegistry Bean 存在
     * （dag-scheduler 可能已自动注册，这里做兜底）
     */
    @Bean
    public ExecutorRegistry executorRegistry() {
        logger.info("Creating ExecutorRegistry bean (managed by ConfigDrivenExecutorRegistry)");
        return new ExecutorRegistry();
    }
}
