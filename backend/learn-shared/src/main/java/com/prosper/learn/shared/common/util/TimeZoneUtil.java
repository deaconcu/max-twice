package com.prosper.learn.shared.common.util;

import java.time.*;
import java.time.format.DateTimeFormatter;

/**
 * 时区工具类
 *
 * 统一管理系统时区，确保所有时间相关操作使用一致的时区。
 * 系统时区：美国旧金山（America/Los_Angeles，PST/PDT）
 *
 * 设计原则：
 * - 所有业务代码使用此工具类获取当前时间
 * - 避免直接使用 LocalDate.now() 或 LocalDateTime.now()
 * - 确保跨环境（开发/测试/生产）时间一致性
 *
 * @author Claude Code
 * @since 2026-01-10
 */
public final class TimeZoneUtil {

    /**
     * 系统统一时区：旧金山时区
     */
    public static final ZoneId SYSTEM_ZONE_ID = ZoneId.of("America/Los_Angeles");

    /**
     * UTC时区（用于数据存储和跨时区通信）
     */
    public static final ZoneId UTC_ZONE_ID = ZoneId.of("UTC");

    /**
     * 常用日期格式
     */
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    // 防止实例化
    private TimeZoneUtil() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * 获取当前日期（系统时区）
     *
     * 替代: LocalDate.now()
     *
     * @return 当前日期（旧金山时区）
     */
    public static LocalDate now() {
        return LocalDate.now(SYSTEM_ZONE_ID);
    }

    /**
     * 获取当前日期时间（系统时区）
     *
     * 替代: LocalDateTime.now()
     *
     * @return 当前日期时间（旧金山时区）
     */
    public static LocalDateTime nowDateTime() {
        return LocalDateTime.now(SYSTEM_ZONE_ID);
    }

    /**
     * 获取当前时间戳（系统时区）
     *
     * @return 当前时间戳
     */
    public static Instant nowInstant() {
        return Instant.now();
    }

    /**
     * 获取当前带时区的日期时间
     *
     * @return 当前日期时间（包含时区信息）
     */
    public static ZonedDateTime nowZoned() {
        return ZonedDateTime.now(SYSTEM_ZONE_ID);
    }

    /**
     * 获取UTC时区的当前日期
     *
     * @return UTC时区的当前日期
     */
    public static LocalDate nowUTC() {
        return LocalDate.now(UTC_ZONE_ID);
    }

    /**
     * 获取UTC时区的当前日期时间
     *
     * @return UTC时区的当前日期时间
     */
    public static LocalDateTime nowDateTimeUTC() {
        return LocalDateTime.now(UTC_ZONE_ID);
    }

    /**
     * 将UTC时间转换为系统时区
     *
     * @param utcDateTime UTC时间
     * @return 系统时区的时间
     */
    public static LocalDateTime fromUTC(LocalDateTime utcDateTime) {
        return utcDateTime.atZone(UTC_ZONE_ID)
            .withZoneSameInstant(SYSTEM_ZONE_ID)
            .toLocalDateTime();
    }

    /**
     * 将系统时区时间转换为UTC
     *
     * @param localDateTime 系统时区时间
     * @return UTC时间
     */
    public static LocalDateTime toUTC(LocalDateTime localDateTime) {
        return localDateTime.atZone(SYSTEM_ZONE_ID)
            .withZoneSameInstant(UTC_ZONE_ID)
            .toLocalDateTime();
    }

    /**
     * 格式化日期为字符串
     *
     * @param date 日期
     * @return 格式化后的字符串 (yyyy-MM-dd)
     */
    public static String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FORMATTER) : null;
    }

    /**
     * 格式化日期时间为字符串
     *
     * @param dateTime 日期时间
     * @return 格式化后的字符串 (yyyy-MM-dd HH:mm:ss)
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATETIME_FORMATTER) : null;
    }

    /**
     * 解析日期字符串
     *
     * @param dateStr 日期字符串 (yyyy-MM-dd)
     * @return LocalDate对象
     */
    public static LocalDate parseDate(String dateStr) {
        return dateStr != null ? LocalDate.parse(dateStr, DATE_FORMATTER) : null;
    }

    /**
     * 解析日期时间字符串
     *
     * @param dateTimeStr 日期时间字符串 (yyyy-MM-dd HH:mm:ss)
     * @return LocalDateTime对象
     */
    public static LocalDateTime parseDateTime(String dateTimeStr) {
        return dateTimeStr != null ? LocalDateTime.parse(dateTimeStr, DATETIME_FORMATTER) : null;
    }

    /**
     * 获取当前日期字符串
     *
     * @return 当前日期字符串 (yyyy-MM-dd)
     */
    public static String todayString() {
        return formatDate(now());
    }

    /**
     * 获取昨天的日期
     *
     * @return 昨天的日期
     */
    public static LocalDate yesterday() {
        return now().minusDays(1);
    }

    /**
     * 获取明天的日期
     *
     * @return 明天的日期
     */
    public static LocalDate tomorrow() {
        return now().plusDays(1);
    }

    /**
     * 判断是否是今天
     *
     * @param date 日期
     * @return 是否是今天
     */
    public static boolean isToday(LocalDate date) {
        return date != null && date.equals(now());
    }

    /**
     * 获取系统时区名称
     *
     * @return 时区名称
     */
    public static String getSystemTimeZoneName() {
        return SYSTEM_ZONE_ID.getId();
    }

    /**
     * 获取当前时区的偏移量
     *
     * @return 时区偏移量（如 -08:00）
     */
    public static String getSystemTimeZoneOffset() {
        ZonedDateTime zdt = ZonedDateTime.now(SYSTEM_ZONE_ID);
        return zdt.getOffset().toString();
    }

    /**
     * 获取用户时区的当前日期
     *
     * @param userTimezone 用户时区（IANA格式，如 "America/Los_Angeles"）
     * @return 用户时区的当前日期，如果时区无效则返回系统时区日期
     */
    public static LocalDate getUserToday(String userTimezone) {
        if (userTimezone != null && !userTimezone.isEmpty()) {
            try {
                ZoneId userZone = ZoneId.of(userTimezone);
                return LocalDate.now(userZone);
            } catch (Exception e) {
                // 时区无效，使用系统时区
            }
        }
        return now();
    }
}
