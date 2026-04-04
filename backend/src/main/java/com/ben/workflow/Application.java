package com.ben.workflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import com.ben.dagscheduler.registry.ExecutorRegistry;

/**
 * AI Workflow Platform - 多模型工作流画布平台
 * 
 * @author Ben
 * @since 2026-03-29
 */
@SpringBootApplication(scanBasePackages = {"com.ben.workflow"})
@Import(ExecutorRegistry.class)
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
