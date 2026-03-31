package com.ben.workflow.spi;

import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 执行器注册中心
 */
@Service
public class ExecutorRegistry {
    private final Map<String, ModelExecutor> executors = new ConcurrentHashMap<>();

    @Autowired
    private ApplicationContext applicationContext;

    @PostConstruct
    public void init() {
        // 自动注册所有 ModelExecutor 实现
        Map<String, ModelExecutor> executorBeans = applicationContext.getBeansOfType(ModelExecutor.class);
        for (ModelExecutor executor : executorBeans.values()) {
            register(executor);
        }
    }

    /**
     * 注册执行器
     */
    public void register(ModelExecutor executor) {
        if (executor != null && executor.getType() != null) {
            executors.put(executor.getType(), executor);
            System.out.println("注册执行器：type=" + executor.getType() + ", class=" + executor.getClass().getSimpleName());
        }
    }

    /**
     * 获取执行器
     */
    public ModelExecutor getExecutor(String type) {
        return executors.get(type);
    }

    /**
     * 获取已注册的所有执行器类型
     */
    public Set<String> getRegisteredTypes() {
        return executors.keySet();
    }

    /**
     * 检查是否已注册指定类型的执行器
     */
    public boolean hasExecutor(String type) {
        return executors.containsKey(type);
    }
}
