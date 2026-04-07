package com.ben.workflow.executor.extension;

import com.ben.dagscheduler.spi.NodeExecutionContext;
import com.ben.dagscheduler.spi.NodeExecutionResult;
import com.ben.dagscheduler.spi.NodeExecutor;
import com.ben.workflow.util.ConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Executor 基类
 * <p>
 * 提供通用的生命周期管理、配置处理、元数据构建等能力。
 * 所有扩展 Executor 应继承此类。
 */
public abstract class BaseExecutor implements NodeExecutor {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    /** 执行器配置 */
    protected ExecutorConfiguration configuration;

    /** 是否已初始化 */
    private volatile boolean initialized = false;

    // ========== NodeExecutor 接口实现 ==========

    @Override
    public void init() throws Exception {
        if (initialized) return;
        doInitialize();
        initialized = true;
        log.info("Executor [{}] initialized", getType());
    }

    @Override
    public void destroy() throws Exception {
        if (!initialized) return;
        doDestroy();
        initialized = false;
        log.info("Executor [{}] destroyed", getType());
    }

    @Override
    public boolean isAvailable() {
        return initialized;
    }

    @Override
    public boolean validateInputs(NodeExecutionContext context) {
        try {
            doValidate(context);
            return true;
        } catch (ValidationException e) {
            log.warn("Validation failed for executor [{}]: {}", getType(), e.getMessage());
            return false;
        }
    }

    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) throws Exception {
        LocalDateTime startTime = LocalDateTime.now();
        String nodeId = context != null ? context.getNodeId() : "unknown";

        try {
            // 校验
            doValidate(context);

            // 执行
            Map<String, Object> outputs = doExecute(context);

            return NodeExecutionResult.success(nodeId, outputs, startTime, LocalDateTime.now());
        } catch (Exception e) {
            log.error("Executor [{}] failed for node [{}]: {}", getType(), nodeId, e.getMessage());
            return NodeExecutionResult.failed(nodeId, e, startTime, LocalDateTime.now());
        }
    }

    // ========== 元数据 ==========

    /**
     * 获取元数据。优先从 @ExecutorMeta 注解读取，子类可覆盖。
     */
    public ExecutorMetadata getMetadata() {
        ExecutorMeta meta = getClass().getAnnotation(ExecutorMeta.class);
        if (meta != null) {
            return ExecutorMetadata.builder()
                    .type(meta.type())
                    .name(meta.name().isEmpty() ? meta.type() : meta.name())
                    .description(meta.description())
                    .category(meta.category())
                    .icon(meta.icon())
                    .version(meta.version())
                    .experimental(meta.experimental())
                    .inputParams(defineInputParams())
                    .outputParams(defineOutputParams())
                    .build();
        }

        // 默认元数据
        return ExecutorMetadata.builder()
                .type(getType())
                .name(getName())
                .description(getDescription())
                .category("general")
                .version("1.0.0")
                .inputParams(defineInputParams())
                .outputParams(defineOutputParams())
                .build();
    }

    // ========== 生命周期钩子（子类可覆盖） ==========

    /**
     * 初始化钩子
     */
    protected void doInitialize() throws Exception {
        // 子类可覆盖
    }

    /**
     * 销毁钩子
     */
    protected void doDestroy() throws Exception {
        // 子类可覆盖
    }

    /**
     * 校验钩子
     */
    protected void doValidate(NodeExecutionContext context) throws ValidationException {
        if (context == null) {
            throw new ValidationException("NodeExecutionContext cannot be null");
        }
    }

    // ========== 子类必须实现 ==========

    /**
     * 执行逻辑，子类必须实现
     *
     * @return 输出 Map
     */
    protected abstract Map<String, Object> doExecute(NodeExecutionContext context) throws Exception;

    // ========== 参数 Schema 定义（子类可覆盖） ==========

    /**
     * 定义输入参数 schema
     */
    protected List<ParameterSchema> defineInputParams() {
        return Collections.emptyList();
    }

    /**
     * 定义输出参数 schema
     */
    protected List<ParameterSchema> defineOutputParams() {
        return Collections.emptyList();
    }

    // ========== 工具方法 ==========

    /**
     * 设置配置
     */
    public void setConfiguration(ExecutorConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * 是否已初始化
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * 从 context 获取 String 参数
     */
    protected String getInputString(NodeExecutionContext context, String key, String defaultValue) {
        return ConfigUtils.getString(context.getInputs(), key, defaultValue);
    }

    /**
     * 从 context 获取 int 参数
     */
    protected int getInputInt(NodeExecutionContext context, String key, int defaultValue) {
        return ConfigUtils.getInt(context.getInputs(), key, defaultValue);
    }

    /**
     * 从 context 获取 double 参数
     */
    protected double getInputDouble(NodeExecutionContext context, String key, double defaultValue) {
        return ConfigUtils.getDouble(context.getInputs(), key, defaultValue);
    }

    /**
     * 从 context 获取 Object 参数
     */
    protected Object getInput(NodeExecutionContext context, String key) {
        return context.getInputs().get(key);
    }

    /**
     * 检查必填参数
     */
    protected void requireInput(NodeExecutionContext context, String... keys) throws ValidationException {
        Map<String, Object> inputs = context.getInputs();
        for (String key : keys) {
            Object v = inputs.get(key);
            if (v == null || (v instanceof String && ((String) v).isBlank())) {
                throw new ValidationException("Missing required parameter: " + key);
            }
        }
    }
}
