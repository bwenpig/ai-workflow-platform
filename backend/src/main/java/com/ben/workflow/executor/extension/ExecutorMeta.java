package com.ben.workflow.executor.extension;

import java.lang.annotation.*;

/**
 * 执行器元数据注解
 * 声明式定义执行器的元数据信息
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExecutorMeta {

    /** 执行器类型标识 */
    String type();

    /** 显示名称 */
    String name() default "";

    /** 描述 */
    String description() default "";

    /** 分类 */
    String category() default "general";

    /** 图标 */
    String icon() default "";

    /** 版本 */
    String version() default "1.0.0";

    /** 是否实验性 */
    boolean experimental() default false;
}
