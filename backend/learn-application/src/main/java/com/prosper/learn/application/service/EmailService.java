package com.prosper.learn.application.service;

import com.prosper.learn.shared.infrastructure.config.SystemProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 邮件服务
 *
 * 负责发送各类邮件通知，使用异步方式避免阻塞主业务流程
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final SystemProperties systemProperties;

    /**
     * 异步发送验证邮件
     *
     * 使用 @Async 注解确保邮件发送不阻塞主流程
     * 即使发送失败也不会影响用户注册等核心业务
     *
     * @param toEmail 收件人邮箱
     * @param code 验证码
     */
    @Async
    public void sendVerificationEmailAsync(String toEmail, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(systemProperties.getUser().getEmailSender());
            message.setTo(toEmail);
            message.setSubject(systemProperties.getUser().getEmailSubject());
            message.setText("Your verification code is: " + code);

            mailSender.send(message);

            log.info("验证邮件发送成功: {}", toEmail);

        } catch (org.springframework.mail.MailAuthenticationException e) {
            // SMTP 认证失败（配置问题）
            log.error("邮件服务认证失败，请检查 SMTP 配置: email={}, error={}",
                     toEmail, e.getMessage());
            // TODO: 发送告警给管理员

        } catch (org.springframework.mail.MailSendException e) {
            // 邮件发送失败（收件地址无效、被拒绝等）
            log.error("邮件发送失败: email={}, error={}", toEmail, e.getMessage());
            // TODO: 可以记录到失败队列，稍后重试

        } catch (Exception e) {
            // 其他未知错误
            log.error("发送验证邮件时发生未知错误: email={}", toEmail, e);
        }
    }

    /**
     * 同步发送验证邮件（用于需要立即知道结果的场景）
     *
     * @param toEmail 收件人邮箱
     * @param code 验证码
     * @return 是否发送成功
     */
    public boolean sendVerificationEmailSync(String toEmail, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(systemProperties.getUser().getEmailSender());
            message.setTo(toEmail);
            message.setSubject(systemProperties.getUser().getEmailSubject());
            message.setText("Your verification code is: " + code);

            mailSender.send(message);

            log.info("验证邮件发送成功: {}", toEmail);
            return true;

        } catch (Exception e) {
            log.error("发送验证邮件失败: email={}", toEmail, e);
            return false;
        }
    }
}
