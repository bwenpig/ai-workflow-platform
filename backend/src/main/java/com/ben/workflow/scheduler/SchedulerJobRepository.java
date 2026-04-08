package com.ben.workflow.scheduler;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 定时任务数据访问层
 */
@Repository
public interface SchedulerJobRepository extends MongoRepository<SchedulerJob, String> {

    /** 按状态查询 */
    List<SchedulerJob> findByStatus(String status);

    /** 按工作流ID查询 */
    List<SchedulerJob> findByWorkflowId(String workflowId);

    /** 按创建人查询 */
    List<SchedulerJob> findByCreatedBy(String createdBy);

    /** 查找运行中的任务 */
    List<SchedulerJob> findByStatusIn(List<String> statuses);
}
