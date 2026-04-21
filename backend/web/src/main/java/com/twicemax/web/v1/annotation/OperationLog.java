package com.twicemax.web.v1.annotation;

import com.twicemax.shared.domain.Enums;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static com.twicemax.shared.domain.Enums.*;

/**
 * 操作日志注解
 * 用于自动记录管理员的关键操作
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {

    /** 模块名称 */
    String module();

    /** 操作类型 */
    String type();

    /** 操作级别 */
    OperationLevel level() default OperationLevel.MEDIUM;

    /** 目标类型（如 "User", "Post", "Course"） */
    String targetType();

    /** 目标ID参数名（SpEL表达式，如 "#userId", "#id"） */
    String targetId();

    /** 目标名称参数名（可选，SpEL表达式） */
    String targetName() default "";

    /** 原因参数名（可选，SpEL表达式，如 "#reason"） */
    String reason() default "";
}
