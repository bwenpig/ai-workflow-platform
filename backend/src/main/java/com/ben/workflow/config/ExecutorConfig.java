package com.ben.workflow.config;

import com.ben.dagscheduler.registry.ExecutorRegistry;
import com.ben.workflow.executor.ConditionalExecutor;
import com.ben.workflow.executor.LLMNodeExecutor;
import com.ben.workflow.executor.LoopExecutor;
import com.ben.workflow.executor.HttpRequestExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * 执行器配置类
 * 手动注册自定义执行器到 ExecutorRegistry
 */
@Configuration
public class ExecutorConfig {

    private static final Logger logger = LoggerFactory.getLogger(ExecutorConfig.class);

    @Autowired
    private ExecutorRegistry registry;

    @PostConstruct
    public void registerExecutors() {
        try {
            // 注册 conditional 执行器
            ConditionalExecutor conditionalExecutor = new ConditionalExecutor();
            conditionalExecutor.init();
            registry.register(conditionalExecutor, "conditional");
            logger.info("Registered executor: conditional");
            
            // 注册 loop 执行器
            LoopExecutor loopExecutor = new LoopExecutor();
            loopExecutor.init();
            registry.register(loopExecutor, "loop");
            logger.info("Registered executor: loop");
            
            // 注册 http_request 执行器
            HttpRequestExecutor httpRequestExecutor = new HttpRequestExecutor();
            httpRequestExecutor.init();
            registry.register(httpRequestExecutor, "http_request");
            logger.info("Registered executor: http_request");
            
            // 注册 llm 执行器
            LLMNodeExecutor llmExecutor = new LLMNodeExecutor();
            llmExecutor.init();
            registry.register(llmExecutor, "llm");
            logger.info("Registered executor: llm");
            
            logger.info("✓ Registered custom executors: conditional, loop, http_request, llm");
        } catch (Exception e) {
            logger.error("Failed to register executors", e);
            throw new RuntimeException("Failed to register executors", e);
        }
    }
}
