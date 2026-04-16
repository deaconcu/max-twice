package com.prosper.learn.shared.common.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 可配置的字符串长度验证注解
 * 长度限制强制从配置文件读取
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ConfigurableSizeValidator.class)
@Documented
public @interface ConfigurableSize {

    /**
     * 配置键标识
     * 例如: "comment-content", "username", "course-name"
     */
    String configKey();

    /**
     * 错误消息模板
     */
    String message() default "字符串长度不符合要求";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
