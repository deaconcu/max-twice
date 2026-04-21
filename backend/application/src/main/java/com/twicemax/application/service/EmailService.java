package com.twicemax.application.service;

import com.twicemax.analytics.monitoring.service.ErrorLogService;
import com.twicemax.shared.infrastructure.config.SystemProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Locale;

/**
 * 邮件服务
 *
 * 负责发送各类邮件通知，使用异步方式避免阻塞主业务流程
 * 支持根据用户语言站发送对应语言的邮件
 * 邮件采用 multipart/alternative，同时发送 HTML 和纯文本，兼容所有客户端
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final String BRAND_NAME = "TwiceMax";
    private static final String TEMPLATE_PATH_ZH = "templates/email/verification-zh.html";
    private static final String TEMPLATE_PATH_EN = "templates/email/verification-en.html";

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
            MimeMessage message = buildVerificationEmail(toEmail, code, language);
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
            MimeMessage message = buildVerificationEmail(toEmail, code, language);
            mailSender.send(message);
            log.info("验证邮件发送成功: {}, language={}", toEmail, language);
            return true;

        } catch (Exception e) {
            log.error("发送验证邮件失败: email={}", toEmail, e);
            return false;
        }
    }

    /**
     * 构建验证邮件（multipart/alternative，HTML + 纯文本）
     */
    private MimeMessage buildVerificationEmail(String toEmail, String code, String language)
            throws MessagingException, IOException {
        boolean isZh = "zh".equals(language);
        Locale locale = isZh ? Locale.CHINESE : Locale.ENGLISH;
        int expiryMinutes = systemProperties.getUser().getVerificationCodeExpiryMinutes();

        String senderName = messageSource.getMessage("email.sender.name", null, BRAND_NAME, locale);
        String subject = messageSource.getMessage("email.subject.verification", null, "Verification Code", locale);
        String plainText = messageSource.getMessage("email.verification.text",
                new Object[]{code, expiryMinutes},
                "Your verification code is: " + code, locale);

        String templatePath = isZh ? TEMPLATE_PATH_ZH : TEMPLATE_PATH_EN;
        String htmlText = renderTemplate(templatePath, code, String.valueOf(expiryMinutes));

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
        helper.setFrom(mailFrom, senderName);
        helper.setTo(toEmail);
        helper.setSubject(subject);
        // 第二个参数 true 表示第一个是 plainText，第二个是 HTML（multipart/alternative）
        helper.setText(plainText, htmlText);

        return message;
    }

    /**
     * 加载并渲染 HTML 模板
     *
     * 使用 MessageFormat 占位符 {0} {1}，与 messages.properties 保持一致
     */
    private String renderTemplate(String classpath, Object... args) throws IOException {
        ClassPathResource resource = new ClassPathResource(classpath);
        try (var is = resource.getInputStream()) {
            String template = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return MessageFormat.format(template, args);
        }
    }

    private String getStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
