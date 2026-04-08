package com.ben.workflow.scheduler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SchedulerService 单元测试
 * 注：Java 25 + Mockito 兼容性限制，使用反射验证结构
 */
class SchedulerServiceTest {

    @Test
    @DisplayName("SchedulerService 类存在且有 @Service 注解")
    void testClassExists() {
        assertTrue(SchedulerService.class.isAnnotationPresent(
                org.springframework.stereotype.Service.class));
    }

    @Test
    @DisplayName("SchedulerService 有 createJob 方法")
    void testCreateJobMethod() throws Exception {
        Method method = SchedulerService.class.getMethod("createJob", String.class, String.class, String.class, String.class);
        assertNotNull(method);
        assertEquals(reactor.core.publisher.Mono.class, method.getReturnType());
    }

    @Test
    @DisplayName("SchedulerService 有 listJobs 方法")
    void testListJobsMethod() throws Exception {
        Method method = SchedulerService.class.getMethod("listJobs");
        assertNotNull(method);
        assertEquals(reactor.core.publisher.Mono.class, method.getReturnType());
    }

    @Test
    @DisplayName("SchedulerService 有 getJob 方法")
    void testGetJobMethod() throws Exception {
        Method method = SchedulerService.class.getMethod("getJob", String.class);
        assertNotNull(method);
        assertEquals(reactor.core.publisher.Mono.class, method.getReturnType());
    }

    @Test
    @DisplayName("SchedulerService 有 pauseJob 方法")
    void testPauseJobMethod() throws Exception {
        Method method = SchedulerService.class.getMethod("pauseJob", String.class);
        assertNotNull(method);
        assertEquals(reactor.core.publisher.Mono.class, method.getReturnType());
    }

    @Test
    @DisplayName("SchedulerService 有 resumeJob 方法")
    void testResumeJobMethod() throws Exception {
        Method method = SchedulerService.class.getMethod("resumeJob", String.class);
        assertNotNull(method);
        assertEquals(reactor.core.publisher.Mono.class, method.getReturnType());
    }

    @Test
    @DisplayName("SchedulerService 有 deleteJob 方法")
    void testDeleteJobMethod() throws Exception {
        Method method = SchedulerService.class.getMethod("deleteJob", String.class);
        assertNotNull(method);
        assertEquals(reactor.core.publisher.Mono.class, method.getReturnType());
    }

    @Test
    @DisplayName("SchedulerService 有 updateCron 方法")
    void testUpdateCronMethod() throws Exception {
        Method method = SchedulerService.class.getMethod("updateCron", String.class, String.class);
        assertNotNull(method);
        assertEquals(reactor.core.publisher.Mono.class, method.getReturnType());
    }

    @Test
    @DisplayName("SchedulerService 有 getActiveTaskCount 方法")
    void testGetActiveTaskCountMethod() throws Exception {
        Method method = SchedulerService.class.getMethod("getActiveTaskCount");
        assertNotNull(method);
        assertEquals(int.class, method.getReturnType());
    }

    @Test
    @DisplayName("SchedulerService 有 @PostConstruct recoverRunningJobs")
    void testRecoverRunningJobsMethod() throws Exception {
        Method method = SchedulerService.class.getMethod("recoverRunningJobs");
        assertNotNull(method);
        assertTrue(method.isAnnotationPresent(jakarta.annotation.PostConstruct.class));
    }

    @Test
    @DisplayName("SchedulerService 构造函数注入4个依赖")
    void testConstructorDependencies() {
        var constructors = SchedulerService.class.getConstructors();
        assertEquals(1, constructors.length);
        assertEquals(4, constructors[0].getParameterCount());
    }
}
