package com.prosper.learn.common.validation;

import com.prosper.learn.common.config.SystemProperties;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 可配置长度验证器实现
 * 从 SystemProperties 读取验证规则
 */
@Component
public class ConfigurableSizeValidator implements ConstraintValidator<ConfigurableSize, String> {

    @Autowired
    private SystemProperties systemProperties;

    private String configKey;
    private int minLength;
    private int maxLength;

    @Override
    public void initialize(ConfigurableSize annotation) {
        this.configKey = annotation.configKey();
        loadConfigValues();
    }

    /**
     * 根据 configKey 加载配置值
     */
    private void loadConfigValues() {
        SystemProperties.Validation validation = systemProperties.getValidation();

        switch (configKey) {
            case "comment-content":
                this.minLength = validation.getCommentContentMinLength();
                this.maxLength = validation.getCommentContentMaxLength();
                break;
            case "username":
                this.minLength = validation.getUsernameMinLength();
                this.maxLength = validation.getUsernameMaxLength();
                break;
            case "password":
                this.minLength = validation.getPasswordMinLength();
                this.maxLength = validation.getPasswordMaxLength();
                break;
            case "biography":
                this.minLength = 0;
                this.maxLength = validation.getBiographyMaxLength();
                break;
            case "email":
                this.minLength = 0;
                this.maxLength = validation.getEmailMaxLength();
                break;
            case "course-name":
                this.minLength = validation.getCourseNameMinLength();
                this.maxLength = validation.getCourseNameMaxLength();
                break;
            case "course-description":
                this.minLength = validation.getCourseDescriptionMinLength();
                this.maxLength = validation.getCourseDescriptionMaxLength();
                break;
            case "post-content":
                this.minLength = validation.getPostContentMinLength();
                this.maxLength = validation.getPostContentMaxLength();
                break;
            case "profession-name":
                this.minLength = validation.getProfessionNameMinLength();
                this.maxLength = validation.getProfessionNameMaxLength();
                break;
            case "profession-description":
                this.minLength = validation.getProfessionDescriptionMinLength();
                this.maxLength = validation.getProfessionDescriptionMaxLength();
                break;
            case "card-front":
                this.minLength = validation.getCardFrontMinLength();
                this.maxLength = validation.getCardFrontMaxLength();
                break;
            case "card-back":
                this.minLength = validation.getCardBackMinLength();
                this.maxLength = validation.getCardBackMaxLength();
                break;
            case "deck-title":
                this.minLength = validation.getDeckTitleMinLength();
                this.maxLength = validation.getDeckTitleMaxLength();
                break;
            case "deck-description":
                this.minLength = 0;
                this.maxLength = validation.getDeckDescriptionMaxLength();
                break;
            case "message-content":
                this.minLength = validation.getMessageContentMinLength();
                this.maxLength = validation.getMessageContentMaxLength();
                break;
            case "roadmap-content":
                this.minLength = validation.getRoadmapContentMinLength();
                this.maxLength = validation.getRoadmapContentMaxLength();
                break;
            case "roadmap-description":
                this.minLength = validation.getRoadmapDescriptionMinLength();
                this.maxLength = validation.getRoadmapDescriptionMaxLength();
                break;
            default:
                throw new IllegalArgumentException("未知的配置键: " + configKey + ", 请在 SystemProperties.Validation 中添加对应配置");
        }
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // null 由 @NotNull/@NotBlank 处理
        }

        int length = value.length();

        if (length < minLength || length > maxLength) {
            // 自定义错误消息
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    String.format("长度必须在%d-%d字符之间,当前长度:%d", minLength, maxLength, length)
            ).addConstraintViolation();
            return false;
        }

        return true;
    }
}
