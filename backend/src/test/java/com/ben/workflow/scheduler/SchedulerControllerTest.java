package com.ben.workflow.scheduler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SchedulerController 单元测试
 */
class SchedulerControllerTest {

    @Test
    @DisplayName("Controller 有 @RestController 注解")
    void testRestControllerAnnotation() {
        assertTrue(SchedulerController.class.isAnnotationPresent(RestController.class));
    }

    @Test
    @DisplayName("Controller 有正确的 @RequestMapping")
    void testRequestMapping() {
        RequestMapping mapping = SchedulerController.class.getAnnotation(RequestMapping.class);
        assertNotNull(mapping);
        assertArrayEquals(new String[]{"/api/v1/scheduler"}, mapping.value());
    }

    @Test
    @DisplayName("Controller 有 @CrossOrigin 注解")
    void testCrossOrigin() {
        assertTrue(SchedulerController.class.isAnnotationPresent(CrossOrigin.class));
    }

    @Test
    @DisplayName("createJob 端点: POST /jobs")
    void testCreateJobEndpoint() throws Exception {
        Method method = SchedulerController.class.getMethod("createJob",
                SchedulerController.CreateJobRequest.class, String.class);
        assertNotNull(method.getAnnotation(PostMapping.class));
        assertArrayEquals(new String[]{"/jobs"}, method.getAnnotation(PostMapping.class).value());
    }

    @Test
    @DisplayName("listJobs 端点: GET /jobs")
    void testListJobsEndpoint() throws Exception {
        Method method = SchedulerController.class.getMethod("listJobs");
        assertNotNull(method.getAnnotation(GetMapping.class));
        assertArrayEquals(new String[]{"/jobs"}, method.getAnnotation(GetMapping.class).value());
    }

    @Test
    @DisplayName("getJob 端点: GET /jobs/{id}")
    void testGetJobEndpoint() throws Exception {
        Method method = SchedulerController.class.getMethod("getJob", String.class);
        assertNotNull(method.getAnnotation(GetMapping.class));
        assertArrayEquals(new String[]{"/jobs/{id}"}, method.getAnnotation(GetMapping.class).value());
    }

    @Test
    @DisplayName("pauseJob 端点: POST /jobs/{id}/pause")
    void testPauseJobEndpoint() throws Exception {
        Method method = SchedulerController.class.getMethod("pauseJob", String.class);
        assertNotNull(method.getAnnotation(PostMapping.class));
        assertArrayEquals(new String[]{"/jobs/{id}/pause"}, method.getAnnotation(PostMapping.class).value());
    }

    @Test
    @DisplayName("resumeJob 端点: POST /jobs/{id}/resume")
    void testResumeJobEndpoint() throws Exception {
        Method method = SchedulerController.class.getMethod("resumeJob", String.class);
        assertNotNull(method.getAnnotation(PostMapping.class));
        assertArrayEquals(new String[]{"/jobs/{id}/resume"}, method.getAnnotation(PostMapping.class).value());
    }

    @Test
    @DisplayName("deleteJob 端点: DELETE /jobs/{id}")
    void testDeleteJobEndpoint() throws Exception {
        Method method = SchedulerController.class.getMethod("deleteJob", String.class);
        assertNotNull(method.getAnnotation(DeleteMapping.class));
        assertArrayEquals(new String[]{"/jobs/{id}"}, method.getAnnotation(DeleteMapping.class).value());
    }

    @Test
    @DisplayName("updateCron 端点: PUT /jobs/{id}/cron")
    void testUpdateCronEndpoint() throws Exception {
        Method method = SchedulerController.class.getMethod("updateCron", String.class, java.util.Map.class);
        assertNotNull(method.getAnnotation(PutMapping.class));
        assertArrayEquals(new String[]{"/jobs/{id}/cron"}, method.getAnnotation(PutMapping.class).value());
    }

    @Test
    @DisplayName("CreateJobRequest record 有正确的字段")
    void testCreateJobRequest() {
        var request = new SchedulerController.CreateJobRequest("wf-1", "0 0 9 * * *", "test desc");
        assertEquals("wf-1", request.workflowId());
        assertEquals("0 0 9 * * *", request.cronExpression());
        assertEquals("test desc", request.description());
    }

    @Test
    @DisplayName("CreateJobRequest description 可以为 null")
    void testCreateJobRequestNullDescription() {
        var request = new SchedulerController.CreateJobRequest("wf-1", "0 0 9 * * *", null);
        assertNull(request.description());
    }

    @Test
    @DisplayName("所有端点返回 Mono")
    void testAllEndpointsReturnMono() {
        for (Method method : SchedulerController.class.getDeclaredMethods()) {
            if (method.isAnnotationPresent(GetMapping.class)
                    || method.isAnnotationPresent(PostMapping.class)
                    || method.isAnnotationPresent(PutMapping.class)
                    || method.isAnnotationPresent(DeleteMapping.class)) {
                assertEquals(reactor.core.publisher.Mono.class, method.getReturnType(),
                        method.getName() + " 应返回 Mono");
            }
        }
    }
}
