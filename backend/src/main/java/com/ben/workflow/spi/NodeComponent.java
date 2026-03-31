package com.ben.workflow.spi;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * 节点组件注解
 * 标识模型执行器组件
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface NodeComponent {
    /**
     * 节点类型标识
     */
    String value();

    /**
     * 节点名称
     */
    String name() default "";

    /**
     * 节点描述
     */
    String description() default "";
}
