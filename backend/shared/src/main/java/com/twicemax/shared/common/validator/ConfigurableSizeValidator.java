package com.twicemax.shared.common.validator;

import com.twicemax.shared.infrastructure.config.SystemProperties;
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
    private String customMessage;
    private int minLength;
    private int maxLength;
    private String fieldName;

    @Override
    public void initialize(ConfigurableSize annotation) {
        this.configKey = annotation.configKey();
        this.customMessage = annotation.message();
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
                this.fieldName = "评论内容";
                break;
            case "username":
                this.minLength = validation.getUsernameMinLength();
                this.maxLength = validation.getUsernameMaxLength();
                this.fieldName = "用户名";
                break;
            case "password":
                this.minLength = validation.getPasswordMinLength();
                this.maxLength = validation.getPasswordMaxLength();
                this.fieldName = "密码";
                break;
            case "biography":
                this.minLength = 0;
                this.maxLength = validation.getBiographyMaxLength();
                this.fieldName = "个人简介";
                break;
            case "email":
                this.minLength = 0;
                this.maxLength = validation.getEmailMaxLength();
                this.fieldName = "邮箱";
                break;
            case "course-name":
                this.minLength = validation.getCourseNameMinLength();
                this.maxLength = validation.getCourseNameMaxLength();
                this.fieldName = "课程名称";
                break;
            case "course-description":
                this.minLength = validation.getCourseDescriptionMinLength();
                this.maxLength = validation.getCourseDescriptionMaxLength();
                this.fieldName = "课程描述";
                break;
            case "post-content":
                this.minLength = validation.getPostContentMinLength();
                this.maxLength = validation.getPostContentMaxLength();
                this.fieldName = "帖子内容";
                break;
            case "role-name":
                this.minLength = validation.getRoleNameMinLength();
                this.maxLength = validation.getRoleNameMaxLength();
                this.fieldName = "角色名称";
                break;
            case "role-description":
                this.minLength = validation.getRoleDescriptionMinLength();
                this.maxLength = validation.getRoleDescriptionMaxLength();
                this.fieldName = "角色描述";
                break;
            case "card-front":
                this.minLength = validation.getCardFrontMinLength();
                this.maxLength = validation.getCardFrontMaxLength();
                this.fieldName = "问题";
                break;
            case "card-back":
                this.minLength = validation.getCardBackMinLength();
                this.maxLength = validation.getCardBackMaxLength();
                this.fieldName = "答案";
                break;
            case "deck-description":
                this.minLength = 0;
                this.maxLength = validation.getDeckDescriptionMaxLength();
                this.fieldName = "卡片组描述";
                break;
            case "message-content":
                this.minLength = validation.getMessageContentMinLength();
                this.maxLength = validation.getMessageContentMaxLength();
                this.fieldName = "消息内容";
                break;
            case "roadmap-content":
                this.minLength = validation.getRoadmapContentMinLength();
                this.maxLength = validation.getRoadmapContentMaxLength();
                this.fieldName = "路线图内容";
                break;
            case "roadmap-description":
                this.minLength = validation.getRoadmapDescriptionMinLength();
                this.maxLength = validation.getRoadmapDescriptionMaxLength();
                this.fieldName = "路线图描述";
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
            context.disableDefaultConstraintViolation();

            // 如果传了自定义消息，使用自定义消息；否则使用详细消息
            String message;
            if (customMessage != null && !customMessage.isEmpty()) {
                message = customMessage;
            } else {
                message = String.format("%s长度必须在%d-%d字符之间，当前长度：%d", fieldName, minLength, maxLength, length);
            }

            context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
            return false;
        }

        return true;
    }
}
