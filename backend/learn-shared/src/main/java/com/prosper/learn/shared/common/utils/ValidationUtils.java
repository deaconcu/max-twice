package com.prosper.learn.shared.common.utils;

import com.prosper.learn.shared.domain.exception.ErrorCode;

/**
 * 通用参数验证工具类
 *
 * 提供Service层常用的简单验证方法
 * Controller层的基础验证(@Valid、@NotNull等)已经处理了空值检查
 */
public class ValidationUtils {

    /**
     * 验证ID是否大于0
     */
    public static void requirePositiveId(Long id) {
        if (id == null || id <= 0) {
            throw ErrorCode.INVALID_PARAMETER.exception("ID必须大于0");
        }
    }

    /**
     * 验证进度百分比是否在0-100之间
     */
    public static void requireValidPercent(Integer percent) {
        if (percent == null || percent < 0 || percent > 100) {
            throw ErrorCode.INVALID_PARAMETER.exception("百分比必须在0-100之间");
        }
    }

    /**
     * 验证数值是否在指定范围内
     */
    public static void requireInRange(Integer value, int min, int max, String fieldName) {
        if (value == null || value < min || value > max) {
            throw ErrorCode.INVALID_PARAMETER.exception(
                String.format("%s必须在%d-%d之间", fieldName, min, max)
            );
        }
    }

    /**
     * 验证字符串非空(去除空白后)
     */
    public static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw ErrorCode.INVALID_PARAMETER.exception(fieldName + "不能为空");
        }
    }

    /**
     * 验证业务条件
     */
    public static void require(boolean condition, String message) {
        if (!condition) {
            throw ErrorCode.INVALID_PARAMETER.exception(message);
        }
    }

    /**
     * 验证权限
     */
    public static void requirePermission(boolean hasPermission, String message) {
        if (!hasPermission) {
            throw ErrorCode.PERMISSION_DENIED.exception(message);
        }
    }

    private ValidationUtils() {
        throw new UnsupportedOperationException("Utility class");
    }
}