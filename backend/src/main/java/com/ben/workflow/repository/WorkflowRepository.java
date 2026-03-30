package com.ben.workflow.repository;

import com.ben.workflow.model.Workflow;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 工作流数据访问层
 */
@Repository
public interface WorkflowRepository extends MongoRepository<Workflow, String> {

    /**
     * 按名称查找工作流
     */
    List<Workflow> findByName(String name);

    /**
     * 查找用户的工作流
     */
    List<Workflow> findByCreatedBy(String createdBy);

    /**
     * 查找已发布的工作流
     */
    List<Workflow> findByPublishedTrue();

    /**
     * 按名称和版本查找
     */
    Optional<Workflow> findByNameAndVersion(String name, Integer version);
}
