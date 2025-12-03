package com.prosper.learn.business.service.application;

import com.prosper.learn.common.config.SystemProperties;
import com.prosper.learn.dto.response.ValidationRuleDTO;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 验证规则配置服务
 * 从 application.yml 读取验证规则，提供给前端使用
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ValidationConfigService {

    private final SystemProperties systemProperties;

    private Map<String, ValidationRuleDTO> cachedRules;

    /**
     * 应用启动时初始化配置
     */
    @PostConstruct
    public void init() {
        this.cachedRules = loadFromYaml();
        log.info("验证规则配置已加载，共 {} 个字段", cachedRules.size());
    }

    /**
     * 从 application.yml 加载所有验证规则
     */
    private Map<String, ValidationRuleDTO> loadFromYaml() {
        Map<String, ValidationRuleDTO> rules = new HashMap<>();
        SystemProperties.Validation validation = systemProperties.getValidation();

        // 卡片相关
        rules.put("card-front", ValidationRuleDTO.builder()
                .minLength(validation.getCardFrontMinLength())
                .maxLength(validation.getCardFrontMaxLength())
                .label("问题")
                .build());

        rules.put("card-back", ValidationRuleDTO.builder()
                .minLength(validation.getCardBackMinLength())
                .maxLength(validation.getCardBackMaxLength())
                .label("答案")
                .build());

        rules.put("deck-title", ValidationRuleDTO.builder()
                .minLength(validation.getDeckTitleMinLength())
                .maxLength(validation.getDeckTitleMaxLength())
                .label("卡片组标题")
                .build());

        rules.put("deck-description", ValidationRuleDTO.builder()
                .minLength(0)
                .maxLength(validation.getDeckDescriptionMaxLength())
                .label("卡片组描述")
                .build());

        // 评论相关
        rules.put("comment-content", ValidationRuleDTO.builder()
                .minLength(validation.getCommentContentMinLength())
                .maxLength(validation.getCommentContentMaxLength())
                .label("评论内容")
                .build());

        // 用户相关
        rules.put("username", ValidationRuleDTO.builder()
                .minLength(validation.getUsernameMinLength())
                .maxLength(validation.getUsernameMaxLength())
                .label("用户名")
                .build());

        rules.put("password", ValidationRuleDTO.builder()
                .minLength(validation.getPasswordMinLength())
                .maxLength(validation.getPasswordMaxLength())
                .label("密码")
                .build());

        rules.put("biography", ValidationRuleDTO.builder()
                .minLength(0)
                .maxLength(validation.getBiographyMaxLength())
                .label("个人简介")
                .build());

        rules.put("email", ValidationRuleDTO.builder()
                .minLength(0)
                .maxLength(validation.getEmailMaxLength())
                .label("邮箱")
                .build());

        // 课程相关
        rules.put("course-name", ValidationRuleDTO.builder()
                .minLength(validation.getCourseNameMinLength())
                .maxLength(validation.getCourseNameMaxLength())
                .label("课程名称")
                .build());

        rules.put("course-description", ValidationRuleDTO.builder()
                .minLength(validation.getCourseDescriptionMinLength())
                .maxLength(validation.getCourseDescriptionMaxLength())
                .label("课程描述")
                .build());

        // 帖子相关
        rules.put("post-content", ValidationRuleDTO.builder()
                .minLength(validation.getPostContentMinLength())
                .maxLength(validation.getPostContentMaxLength())
                .label("帖子内容")
                .build());

        // 职业相关
        rules.put("profession-name", ValidationRuleDTO.builder()
                .minLength(validation.getProfessionNameMinLength())
                .maxLength(validation.getProfessionNameMaxLength())
                .label("职业名称")
                .build());

        rules.put("profession-description", ValidationRuleDTO.builder()
                .minLength(validation.getProfessionDescriptionMinLength())
                .maxLength(validation.getProfessionDescriptionMaxLength())
                .label("职业描述")
                .build());

        // 消息相关
        rules.put("message-content", ValidationRuleDTO.builder()
                .minLength(validation.getMessageContentMinLength())
                .maxLength(validation.getMessageContentMaxLength())
                .label("消息内容")
                .build());

        // 路线图相关
        rules.put("roadmap-content", ValidationRuleDTO.builder()
                .minLength(validation.getRoadmapContentMinLength())
                .maxLength(validation.getRoadmapContentMaxLength())
                .label("路线图内容")
                .build());

        rules.put("roadmap-description", ValidationRuleDTO.builder()
                .minLength(validation.getRoadmapDescriptionMinLength())
                .maxLength(validation.getRoadmapDescriptionMaxLength())
                .label("路线图描述")
                .build());

        return rules;
    }

    /**
     * 获取所有验证规则
     */
    public Map<String, ValidationRuleDTO> getAllRules() {
        return new HashMap<>(cachedRules);
    }

    /**
     * 获取指定字段的验证规则
     */
    public ValidationRuleDTO getRule(String fieldKey) {
        return cachedRules.get(fieldKey);
    }
}
