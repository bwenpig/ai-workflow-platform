package com.ben.workflow.scheduler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SchedulerJob 实体测试
 */
class SchedulerJobTest {

    @Test
    @DisplayName("SchedulerJob 基本属性设置和获取")
    void testGettersSetters() {
        SchedulerJob job = new SchedulerJob();
        
        job.setId("job-001");
        job.setWorkflowId("wf-001");
        job.setWorkflowName("Test Workflow");
        job.setCronExpression("0 0 9 * * *");
        job.setStatus("RUNNING");
        job.setDescription("每天9点执行");
        job.setCreatedBy("ben");
        
        Instant now = Instant.now();
        job.setCreatedAt(now);
        job.setUpdatedAt(now);
        job.setNextFireTime(now);
        job.setLastFireTime(now);
        job.setLastExecutionId("exec-001");
        
        assertEquals("job-001", job.getId());
        assertEquals("wf-001", job.getWorkflowId());
        assertEquals("Test Workflow", job.getWorkflowName());
        assertEquals("0 0 9 * * *", job.getCronExpression());
        assertEquals("RUNNING", job.getStatus());
        assertEquals("每天9点执行", job.getDescription());
        assertEquals("ben", job.getCreatedBy());
        assertEquals(now, job.getCreatedAt());
        assertEquals(now, job.getUpdatedAt());
        assertEquals(now, job.getNextFireTime());
        assertEquals(now, job.getLastFireTime());
        assertEquals("exec-001", job.getLastExecutionId());
    }

    @Test
    @DisplayName("SchedulerJob 有 MongoDB @Document 注解")
    void testHasDocumentAnnotation() {
        assertTrue(SchedulerJob.class.isAnnotationPresent(
                org.springframework.data.mongodb.core.mapping.Document.class));
        
        var doc = SchedulerJob.class.getAnnotation(
                org.springframework.data.mongodb.core.mapping.Document.class);
        assertEquals("scheduler_jobs", doc.collection());
    }

    @Test
    @DisplayName("SchedulerJob id 字段有 @Id 注解")
    void testIdAnnotation() throws Exception {
        var idField = SchedulerJob.class.getDeclaredField("id");
        assertTrue(idField.isAnnotationPresent(org.springframework.data.annotation.Id.class));
    }

    @Test
    @DisplayName("SchedulerJob 默认值为 null")
    void testDefaultValues() {
        SchedulerJob job = new SchedulerJob();
        assertNull(job.getId());
        assertNull(job.getWorkflowId());
        assertNull(job.getStatus());
        assertNull(job.getCronExpression());
        assertNull(job.getNextFireTime());
        assertNull(job.getLastFireTime());
    }
}
