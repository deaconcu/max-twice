package com.prosper.learn.api.v1.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于将 JSON 请求体中的字段直接映射到方法参数
 * 
 * @author Claude
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonParam {
    /**
     * JSON 字段名
     */
    String value();
    
    /**
     * 是否必需，默认为 true
     */
    boolean required() default true;
}