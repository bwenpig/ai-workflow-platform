package com.ben.workflow.executor.extension;

import com.ben.dagscheduler.registry.ExecutorRegistry;
import com.ben.dagscheduler.spi.NodeExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ServiceLoader;

/**
 * 配置驱动的 Executor 注册中心
 * <p>
 * 支持三种注册方式（优先级从高到低）：
 * 1. YAML 配置文件 (executors.yaml)
 * 2. Java SPI (META-INF/services)
 * 3. REST API 动态注册
 * <p>
 * 配置优先于 SPI，同类型后注册的覆盖先注册的。
 */
@Component
public class ConfigDrivenExecutorRegistry {

    private static final Logger log = LoggerFactory.getLogger(ConfigDrivenExecutorRegistry.class);

    @Autowired
    private ExecutorRegistry executorRegistry;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired(required = false)
    private ExecutorConfigLoader configLoader;

    /** 扩展元数据存储 */
    private final Map<String, ExecutorMetadata> metadataMap = new ConcurrentHashMap<>();

    /** 已注册的 executor 实例（用于生命周期管理） */
    private final Map<String, NodeExecutor> managedExecutors = new ConcurrentHashMap<>();

    /** 注册信息 */
    private final Map<String, ExecutorRegistration> registrations = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("Initializing ConfigDrivenExecutorRegistry...");

        // 1. 加载 SPI
        List<ExecutorRegistration> spiRegistrations = loadFromSPI();
        log.info("SPI discovered {} executors", spiRegistrations.size());

        // 2. 加载 YAML 配置（如果有）
        List<ExecutorRegistration> configRegistrations = loadFromConfig();
        log.info("Config loaded {} executors", configRegistrations.size());

        // 3. 合并（配置覆盖 SPI）
        Map<String, ExecutorRegistration> merged = new LinkedHashMap<>();
        spiRegistrations.forEach(r -> merged.put(r.getType(), r));
        configRegistrations.forEach(r -> merged.put(r.getType(), r));

        // 4. 注册
        for (ExecutorRegistration registration : merged.values()) {
            if (registration.isEnabled()) {
                registerExecutor(registration);
            } else {
                log.info("Executor [{}] is disabled, skipping", registration.getType());
            }
        }

        log.info("ConfigDrivenExecutorRegistry initialized. {} executors registered.", managedExecutors.size());
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down ConfigDrivenExecutorRegistry...");
        for (Map.Entry<String, NodeExecutor> entry : managedExecutors.entrySet()) {
            try {
                entry.getValue().destroy();
            } catch (Exception e) {
                log.error("Error destroying executor [{}]", entry.getKey(), e);
            }
        }
        managedExecutors.clear();
        metadataMap.clear();
        registrations.clear();
    }

    // ========== 注册方法 ==========

    /**
     * 注册一个 Executor
     */
    public void registerExecutor(ExecutorRegistration registration) {
        String type = registration.getType();
        try {
            NodeExecutor executor = createExecutor(registration);
            if (executor == null) {
                log.error("Failed to create executor for type: {}", type);
                return;
            }

            // 设置配置
            if (executor instanceof BaseExecutor && registration.getConfig() != null) {
                ((BaseExecutor) executor).setConfiguration(
                    ExecutorConfiguration.builder().params(registration.getConfig()).build()
                );
            }

            // 初始化
            executor.init();

            // 注册到底层 ExecutorRegistry
            executorRegistry.register(executor, type);

            // 存储元数据
            ExecutorMetadata metadata = buildMetadata(executor, registration);
            metadataMap.put(type, metadata);
            managedExecutors.put(type, executor);
            registrations.put(type, registration);

            log.info("Registered executor: {} -> {}", type, executor.getClass().getName());
        } catch (Exception e) {
            log.error("Failed to register executor: {}", type, e);
        }
    }

    /**
     * 注销一个 Executor
     */
    public void unregister(String type) {
        executorRegistry.unregister(type);
        metadataMap.remove(type);
        managedExecutors.remove(type);
        registrations.remove(type);
        log.info("Unregistered executor: {}", type);
    }

    // ========== 查询方法 ==========

    /**
     * 获取所有元数据
     */
    public List<ExecutorMetadata> getAllMetadata() {
        return new ArrayList<>(metadataMap.values());
    }

    /**
     * 获取指定类型的元数据
     */
    public ExecutorMetadata getMetadata(String type) {
        return metadataMap.get(type);
    }

    /**
     * 获取 Executor 实例
     */
    public NodeExecutor getExecutor(String type) {
        return executorRegistry.getExecutor(type);
    }

    /**
     * 是否已注册
     */
    public boolean hasExecutor(String type) {
        return executorRegistry.hasExecutor(type);
    }

    /**
     * 获取所有注册类型
     */
    public Set<String> getRegisteredTypes() {
        return executorRegistry.getRegisteredTypes();
    }

    /**
     * 获取所有注册信息
     */
    public Map<String, ExecutorRegistration> getRegistrations() {
        return Collections.unmodifiableMap(registrations);
    }

    // ========== 内部方法 ==========

    /**
     * 从 SPI 加载
     */
    List<ExecutorRegistration> loadFromSPI() {
        List<ExecutorRegistration> result = new ArrayList<>();
        try {
            ServiceLoader<NodeExecutor> loader = ServiceLoader.load(NodeExecutor.class);
            for (NodeExecutor executor : loader) {
                ExecutorRegistration registration = ExecutorRegistration.builder()
                        .type(executor.getType())
                        .className(executor.getClass().getName())
                        .enabled(true)
                        .build();
                result.add(registration);
            }
        } catch (Exception e) {
            log.error("Failed to load executors from SPI", e);
        }
        return result;
    }

    /**
     * 从配置文件加载
     */
    List<ExecutorRegistration> loadFromConfig() {
        if (configLoader != null) {
            return configLoader.load();
        }
        return Collections.emptyList();
    }

    /**
     * 创建 Executor 实例
     */
    private NodeExecutor createExecutor(ExecutorRegistration registration) {
        String className = registration.getClassName();
        if (className == null || className.isBlank()) {
            log.warn("No class specified for executor type: {}", registration.getType());
            return null;
        }

        try {
            // 先尝试从 Spring Context 获取
            Class<?> clazz = Class.forName(className);
            try {
                return (NodeExecutor) applicationContext.getBean(clazz);
            } catch (Exception e) {
                // 不在 Spring 容器中，手动实例化
                return (NodeExecutor) clazz.getDeclaredConstructor().newInstance();
            }
        } catch (Exception e) {
            log.error("Failed to create executor instance: {}", className, e);
            return null;
        }
    }

    /**
     * 构建元数据
     */
    private ExecutorMetadata buildMetadata(NodeExecutor executor, ExecutorRegistration registration) {
        // 优先使用 BaseExecutor 的元数据
        if (executor instanceof BaseExecutor) {
            return ((BaseExecutor) executor).getMetadata();
        }

        // 使用注册信息中的元数据
        if (registration.getMetadata() != null) {
            return registration.getMetadata();
        }

        // 构建默认元数据
        return ExecutorMetadata.builder()
                .type(executor.getType())
                .name(executor.getName())
                .description(executor.getDescription())
                .category("general")
                .version("1.0.0")
                .build();
    }
}
