package com.ben.workflow.repository;

import com.ben.workflow.model.WorkflowExecution;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * 工作流执行实例数据访问层
 */
@Repository
public interface ExecutionRepository extends MongoRepository<WorkflowExecution, String> {

    /**
     * 按工作流 ID 查找执行记录
     */
    List<WorkflowExecution> findByWorkflowId(String workflowId);

    /**
     * 按状态查找执行记录
     */
    List<WorkflowExecution> findByStatus(String status);

    /**
     * 查找用户最近的执行记录
     */
    List<WorkflowExecution> findByCreatedByOrderByCreatedAtDesc(String createdBy, int limit);

    /**
     * 查找运行中的执行
     */
    List<WorkflowExecution> findByStatusAndStartedAtBefore(String status, Instant startedAt);
}
