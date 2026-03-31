package com.ben.workflow.spi;

/**
 * 模型执行器 SPI 接口
 * 基于 dag-scheduler NodeExecutor 适配
 */
public interface ModelExecutor {
    /**
     * 获取执行器类型
     * @return 执行器类型标识
     */
    String getType();

    /**
     * 执行模型
     * @param context 执行上下文
     * @return 执行结果
     */
    ModelExecutionResult execute(ModelExecutionContext context);
}
