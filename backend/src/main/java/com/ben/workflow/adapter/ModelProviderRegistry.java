package com.ben.workflow.adapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ModelProvider 注册表
 * 管理所有模型提供者实例
 */
@Component
public class ModelProviderRegistry {

    private static final Logger log = LoggerFactory.getLogger(ModelProviderRegistry.class);

    private final Map<String, ModelProvider> providers = new HashMap<>();

    public ModelProviderRegistry(List<ModelProvider> providerList) {
        for (ModelProvider provider : providerList) {
            String name = getProviderName(provider);
            providers.put(name, provider);
            log.info("注册 ModelProvider: {} -> {}", name, provider.getClass().getSimpleName());
        }
    }

    private String getProviderName(ModelProvider provider) {
        String className = provider.getClass().getSimpleName();
        // KlingExecutor -> kling
        // WanExecutor -> wan
        // SeedanceExecutor -> seedance
        // NanoBananaExecutor -> nanobanana
        return className.replace("Executor", "").toLowerCase();
    }

    /**
     * 获取指定名称的 ModelProvider
     */
    public Optional<ModelProvider> getProvider(String name) {
        return Optional.ofNullable(providers.get(name.toLowerCase()));
    }

    /**
     * 获取所有已注册的 Provider
     */
    public Map<String, ModelProvider> getAllProviders() {
        return new HashMap<>(providers);
    }

    /**
     * 检查是否已注册指定 Provider
     */
    public boolean hasProvider(String name) {
        return providers.containsKey(name.toLowerCase());
    }
}
