package com.ben.workflow.engine;

import com.ben.workflow.model.Workflow;
import com.ben.workflow.model.WorkflowNode;
import reactor.core.publisher.Mono;
import java.util.Map;

/**
 * 工作流执行引擎接口
 */
public interface WorkflowEngine {

    /**
     * 执行工作流
     * 
     * @param workflow 工作流定义
     * @param inputs 输入参数
     * @return 执行实例 ID
     */
    Mono<String> execute(Workflow workflow, Map<String, Object> inputs);

    /**
     * 查询执行状态
     */
    Mono<ExecutionState> getState(String instanceId);

    /**
     * 取消执行
     */
    Mono<Void> cancel(String instanceId);

    /**
     * 重试失败节点
     */
    Mono<Void> retry(String instanceId, String nodeId);
}
