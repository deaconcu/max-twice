package com.twicemax.web.v1.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 从 JSON 请求体中提取单个参数，适用于 1-2 个简单参数的场景
 *
 * <p><b>使用场景：</b>
 * <ul>
 *   <li>只需要 JSON 中的 1-2 个字段，避免创建只有少量字段的 DTO 类</li>
 *   <li>配合 @PathVariable 使用，主要参数在 URL，辅助参数在 body</li>
 * </ul>
 *
 * <p><b>限制：</b>
 * <ul>
 *   <li>❌ 不能和 @RequestBody 混用（InputStream 只能读取一次）</li>
 *   <li>✅ 同一请求可使用多个 @JsonParam（共享缓存）</li>
 * </ul>
 *
 * <p><b>示例：</b>
 * <pre>
 * // 正确：单个或多个 @JsonParam
 * &#64;PostMapping("/follows")
 * public ApiResponse&lt;Void&gt; follow(
 *     &#64;JsonParam("followeeId") Long followeeId) {
 *     // body: {"followeeId": 123}
 * }
 *
 * // 错误：不能混用 @RequestBody
 * &#64;PostMapping("/bad")
 * public ApiResponse&lt;?&gt; bad(
 *     &#64;JsonParam("id") Long id,
 *     &#64;RequestBody SomeDTO dto) { // ❌ 会报错
 * }
 * </pre>
 *
 * <p><b>何时使用 @RequestBody：</b>参数 ≥3 个、有嵌套对象、有复杂验证逻辑
 *
 * @see JsonParamArgumentResolver
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