package com.ben.workflow.scheduler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SchedulerConfig 测试
 */
class SchedulerConfigTest {

    @Test
    @DisplayName("SchedulerConfig 有 @Configuration 注解")
    void testConfigurationAnnotation() {
        assertTrue(SchedulerConfig.class.isAnnotationPresent(
                org.springframework.context.annotation.Configuration.class));
    }

    @Test
    @DisplayName("SchedulerConfig 有 @EnableScheduling 注解")
    void testEnableSchedulingAnnotation() {
        assertTrue(SchedulerConfig.class.isAnnotationPresent(EnableScheduling.class));
    }

    @Test
    @DisplayName("taskScheduler Bean 方法存在且返回 TaskScheduler")
    void testTaskSchedulerBeanMethod() throws Exception {
        Method method = SchedulerConfig.class.getMethod("taskScheduler");
        assertNotNull(method);
        assertTrue(TaskScheduler.class.isAssignableFrom(method.getReturnType()));
        assertNotNull(method.getAnnotation(org.springframework.context.annotation.Bean.class));
    }

    @Test
    @DisplayName("taskScheduler 返回 ThreadPoolTaskScheduler 实例")
    void testTaskSchedulerInstance() {
        SchedulerConfig config = new SchedulerConfig();
        TaskScheduler scheduler = config.taskScheduler();
        assertNotNull(scheduler);
        assertInstanceOf(ThreadPoolTaskScheduler.class, scheduler);
    }
}
