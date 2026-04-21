package com.twicemax.application.service;

import com.twicemax.analytics.monitoring.service.ErrorLogService;
import com.twicemax.shared.infrastructure.config.SystemProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;

/**
 * 邮件服务
 *
 * 负责发送各类邮件通知，使用异步方式避免阻塞主业务流程
 * 支持根据用户语言站发送对应语言的邮件
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final ErrorLogService errorLogService;
    private final MessageSource messageSource;
    private final SystemProperties systemProperties;

    @Value("${spring.mail.username}")
    private String mailUsername;

    @Value("${spring.mail.from}")
    private String mailFrom;

    /**
     * 异步发送验证邮件
     *
     * 使用 @Async 注解确保邮件发送不阻塞主流程
     * 即使发送失败也不会影响用户注册等核心业务
     *
     * @param toEmail 收件人邮箱
     * @param code 验证码
     * @param language 语言代码（zh/en）
     */
    @Async
    public void sendVerificationEmailAsync(String toEmail, String code, String language) {
        try {
            SimpleMailMessage message = buildVerificationEmail(toEmail, code, language);
            mailSender.send(message);
            log.info("验证邮件发送成功: {}, language={}", toEmail, language);

        } catch (org.springframework.mail.MailAuthenticationException e) {
            log.error("邮件服务认证失败，请检查 SMTP 配置: email={}, error={}", toEmail, e.getMessage());
            errorLogService.recordBackendError(
                    e.getClass().getName(),
                    "SMTP认证失败: " + e.getMessage(),
                    getStackTrace(e),
                    "EmailService.sendVerificationEmailAsync", null, null);

        } catch (org.springframework.mail.MailSendException e) {
            log.error("邮件发送失败: email={}, error={}", toEmail, e.getMessage());
            errorLogService.recordBackendError(
                    e.getClass().getName(),
                    "邮件发送失败: email=" + toEmail + ", " + e.getMessage(),
                    getStackTrace(e),
                    "EmailService.sendVerificationEmailAsync", null, null);

        } catch (Exception e) {
            log.error("发送验证邮件时发生未知错误: email={}", toEmail, e);
            errorLogService.recordBackendError(
                    e.getClass().getName(),
                    "发送验证邮件未知错误: email=" + toEmail + ", " + e.getMessage(),
                    getStackTrace(e),
                    "EmailService.sendVerificationEmailAsync", null, null);
        }
    }

    /**
     * 同步发送验证邮件（用于需要立即知道结果的场景）
     *
     * @param toEmail 收件人邮箱
     * @param code 验证码
     * @param language 语言代码（zh/en）
     * @return 是否发送成功
     */
    public boolean sendVerificationEmailSync(String toEmail, String code, String language) {
        try {
            SimpleMailMessage message = buildVerificationEmail(toEmail, code, language);
            mailSender.send(message);
            log.info("验证邮件发送成功: {}, language={}", toEmail, language);
            return true;

        } catch (Exception e) {
            log.error("发送验证邮件失败: email={}", toEmail, e);
            return false;
        }
    }

    /**
     * 构建验证邮件
     */
    private SimpleMailMessage buildVerificationEmail(String toEmail, String code, String language) {
        Locale locale = "zh".equals(language) ? Locale.CHINESE : Locale.ENGLISH;
        int expiryMinutes = systemProperties.getUser().getVerificationCodeExpiryMinutes();

        String senderName = messageSource.getMessage("email.sender.name", null, "Max Twice", locale);
        String subject = messageSource.getMessage("email.subject.verification", null, "Verification Code", locale);
        String text = messageSource.getMessage("email.verification.text",
                new Object[]{code, expiryMinutes},
                "Your verification code is: " + code, locale);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderName + " <" + mailFrom + ">");
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(text);

        return message;
    }

    private String getStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
