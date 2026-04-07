package com.ben.workflow.executor.extension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.*;

/**
 * Executor 配置加载器
 * 从 classpath:executors.yaml 加载配置
 */
@Component
public class ExecutorConfigLoader {

    private static final Logger log = LoggerFactory.getLogger(ExecutorConfigLoader.class);
    private static final String CONFIG_FILE = "executors.yaml";

    /**
     * 加载配置文件
     */
    @SuppressWarnings("unchecked")
    public List<ExecutorRegistration> load() {
        List<ExecutorRegistration> registrations = new ArrayList<>();

        Resource resource = new ClassPathResource(CONFIG_FILE);
        if (!resource.exists()) {
            log.info("No {} found on classpath, skipping config-based loading", CONFIG_FILE);
            return registrations;
        }

        try (InputStream is = resource.getInputStream()) {
            ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
            Map<String, Object> root = yamlMapper.readValue(is, Map.class);

            List<Map<String, Object>> executorConfigs = (List<Map<String, Object>>) root.get("executors");
            if (executorConfigs == null) {
                return registrations;
            }

            for (Map<String, Object> item : executorConfigs) {
                ExecutorRegistration registration = parseRegistration(item);
                if (registration != null) {
                    registrations.add(registration);
                }
            }

            log.info("Loaded {} executor registrations from {}", registrations.size(), CONFIG_FILE);
        } catch (Exception e) {
            log.error("Failed to load {}", CONFIG_FILE, e);
        }

        return registrations;
    }

    @SuppressWarnings("unchecked")
    private ExecutorRegistration parseRegistration(Map<String, Object> item) {
        String type = (String) item.get("type");
        String className = (String) item.get("class");

        if (type == null || type.isBlank()) {
            log.warn("Skipping executor config with missing type");
            return null;
        }

        Boolean enabled = (Boolean) item.getOrDefault("enabled", true);
        Map<String, Object> config = (Map<String, Object>) item.get("config");

        return ExecutorRegistration.builder()
                .type(type)
                .className(className)
                .enabled(enabled != null ? enabled : true)
                .config(config)
                .build();
    }
}
