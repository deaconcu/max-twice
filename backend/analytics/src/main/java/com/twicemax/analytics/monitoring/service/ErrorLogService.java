package com.twicemax.analytics.monitoring.service;

import com.twicemax.analytics.monitoring.ErrorLogDO;
import com.twicemax.analytics.monitoring.ErrorLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 错误日志服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ErrorLogService {

    private final ErrorLogMapper errorLogMapper;

    /** 复发告警间隔（超过这个时间再次发生视为复发） */
    private static final Duration ALERT_INTERVAL = Duration.ofHours(24);

    /**
     * 记录后端错误
     *
     * @param errorType  异常类名
     * @param message    错误消息
     * @param stackTrace 堆栈信息
     * @param url        请求URL
     * @param userId     用户ID（可空）
     * @param ip         IP地址
     */
    @Async
    public void recordBackendError(String errorType, String message, String stackTrace,
                                    String url, Long userId, String ip) {
        record("backend", errorType, message, stackTrace, url, userId, ip, null, null);
    }

    /**
     * 记录前端错误
     *
     * @param errorType  错误类型
     * @param message    错误消息
     * @param stackTrace 堆栈信息
     * @param url        页面URL
     * @param userId     用户ID（可空）
     * @param ip         IP地址
     * @param userAgent  浏览器UA
     * @param extraData  额外数据（JSON）
     */
    @Async
    public void recordFrontendError(String errorType, String message, String stackTrace,
                                     String url, Long userId, String ip, String userAgent, String extraData) {
        record("frontend", errorType, message, stackTrace, url, userId, ip, userAgent, extraData);
    }

    /**
     * 记录错误
     */
    private void record(String source, String errorType, String message, String stackTrace,
                        String url, Long userId, String ip, String userAgent, String extraData) {
        try {
            // 计算指纹
            String fingerprint = calculateFingerprint(source, errorType, message, stackTrace);

            // 查询是否已存在
            ErrorLogDO existing = errorLogMapper.getByFingerprint(fingerprint);
            LocalDateTime now = LocalDateTime.now();

            if (existing == null) {
                // 新错误，插入
                ErrorLogDO errorLog = new ErrorLogDO();
                errorLog.setFingerprint(fingerprint);
                errorLog.setSource(source);
                errorLog.setErrorType(truncate(errorType, 200));
                errorLog.setMessage(truncate(message, 1000));
                errorLog.setStackTrace(stackTrace);
                errorLog.setUrl(truncate(url, 500));
                errorLog.setUserId(userId);
                errorLog.setIp(truncate(ip, 50));
                errorLog.setUserAgent(truncate(userAgent, 500));
                errorLog.setExtraData(extraData);
                errorLog.setCount(1);
                errorLog.setFirstSeenAt(now);
                errorLog.setLastSeenAt(now);
                errorLog.setStatus("new");

                errorLogMapper.insert(errorLog);

                // 新错误，需要告警
                sendAlert(errorLog, true);

            } else {
                // 已存在，更新计数
                errorLogMapper.incrementCount(existing.getId(), now);

                // 判断是否需要复发告警
                if (shouldAlertForRecurrence(existing, now)) {
                    existing.setLastSeenAt(now);
                    sendAlert(existing, false);
                }
            }
        } catch (Exception e) {
            // 错误记录本身出错，只打日志，不抛异常
            log.error("记录错误日志失败", e);
        }
    }

    /**
     * 计算错误指纹
     */
    private String calculateFingerprint(String source, String errorType, String message, String stackTrace) {
        // 提取堆栈第一行（错误发生位置）
        String firstStackLine = "";
        if (stackTrace != null && !stackTrace.isEmpty()) {
            String[] lines = stackTrace.split("\n");
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.startsWith("at ")) {
                    firstStackLine = trimmed;
                    break;
                }
            }
        }

        String content = source + "|" + errorType + "|" + message + "|" + firstStackLine;
        return sha256(content);
    }

    /**
     * SHA256 哈希
     */
    private String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString().substring(0, 64);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * 判断是否需要复发告警
     */
    private boolean shouldAlertForRecurrence(ErrorLogDO existing, LocalDateTime now) {
        // ignored 状态不告警
        if ("ignored".equals(existing.getStatus())) {
            return false;
        }

        // 距离上次发生超过 24 小时，视为复发
        Duration sinceLastSeen = Duration.between(existing.getLastSeenAt(), now);
        return sinceLastSeen.compareTo(ALERT_INTERVAL) > 0;
    }

    /**
     * 发送告警
     */
    private void sendAlert(ErrorLogDO errorLog, boolean isNew) {
        // TODO: 实现告警逻辑（邮件/钉钉/微信）
        String alertType = isNew ? "新错误" : "错误复发";
        log.warn("[{}] {} - {}: {}", alertType, errorLog.getSource(), errorLog.getErrorType(), errorLog.getMessage());
    }

    /**
     * 截断字符串
     */
    private String truncate(String str, int maxLength) {
        if (str == null) {
            return null;
        }
        return str.length() > maxLength ? str.substring(0, maxLength) : str;
    }

    // ========== 管理接口 ==========

    /**
     * 查询错误日志列表
     */
    public List<ErrorLogDO> queryLogs(String source, String status, Long lastId, int limit) {
        return errorLogMapper.queryLogs(source, status, lastId, limit);
    }

    /**
     * 根据ID查询
     */
    public ErrorLogDO getById(Long id) {
        return errorLogMapper.getById(id);
    }

    /**
     * 更新状态
     */
    public void updateStatus(Long id, String status) {
        errorLogMapper.updateStatus(id, status);
    }

    /**
     * 统计未处理的错误数量
     */
    public int countNew() {
        return errorLogMapper.countNew();
    }

    /**
     * 删除过期的错误日志
     */
    public int deleteExpired(int days) {
        LocalDateTime expireTime = LocalDateTime.now().minusDays(days);
        return errorLogMapper.deleteExpired(expireTime);
    }
}
