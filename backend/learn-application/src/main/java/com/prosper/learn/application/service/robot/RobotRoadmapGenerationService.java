package com.prosper.learn.application.service.robot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prosper.learn.application.dto.response.RobotRoadmapDraftDTO;
import com.prosper.learn.application.dto.response.RobotRoadmapTaskDTO;
import com.prosper.learn.content.role.RoleDO;
import com.prosper.learn.content.role.RoleDataService;
import com.prosper.learn.infrastructure.ai.AIService;
import com.prosper.learn.shared.infrastructure.config.SystemProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Robot 路径生成服务
 * 负责异步生成学习路径
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RobotRoadmapGenerationService {

    private final RoleDataService roleDataService;
    private final AIService aiService;
    private final StringRedisTemplate redisTemplate;
    private final SystemProperties systemProperties;
    private final ApplicationContext applicationContext;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String TASK_KEY_PREFIX = "roadmap:task:";
    private static final String HISTORY_KEY_PREFIX = "roadmap:history:";
    private static final String DRAFT_KEY_PREFIX = "roadmap:draft:";
    private static final String DRAFT_LIST_KEY_PREFIX = "roadmap:drafts:";
    private static final int TASK_EXPIRE_DAYS = 30;  // 改为30天
    private static final int DRAFT_EXPIRE_DAYS = 7;  // 草稿保存7天
    private static final int MAX_HISTORY_SIZE = 50;
    private static final int MAX_DRAFT_SIZE = 100;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 提交生成任务
     */
    public String submitGenerateTask(Long roleId, Long userId) {
        // 验证职业存在
        roleDataService.validateAndGet(roleId);

        // 生成任务ID
        String taskId = "roadmap-" + UUID.randomUUID().toString();

        // 保存任务初始状态到 Redis
        RobotRoadmapTaskDTO task = RobotRoadmapTaskDTO.builder()
            .taskId(taskId)
            .roleId(roleId)
            .userId(userId)
            .status("PENDING")
            .createdAt(LocalDateTime.now().format(formatter))
            .build();

        try {
            String taskJson = objectMapper.writeValueAsString(task);
            redisTemplate.opsForValue().set(
                TASK_KEY_PREFIX + taskId,
                taskJson,
                TASK_EXPIRE_DAYS,
                TimeUnit.DAYS
            );
        } catch (Exception e) {
            log.error("Robot Redis 任务保存失败", e);
            throw new RuntimeException("创建任务失败", e);
        }

        // 异步执行生成（通过代理调用以确保 @Async 生效）
        RobotRoadmapGenerationService self = applicationContext.getBean(RobotRoadmapGenerationService.class);
        self.executeGenerateAsync(taskId, roleId, userId);

        return taskId;
    }

    /**
     * 异步执行路径生成
     */
    @Async
    public void executeGenerateAsync(String taskId, Long roleId, Long userId) {
        try {
            log.info("Robot 开始生成路径，taskId={}, roleId={}", taskId, roleId);

            // 查询职业信息
            RoleDO roleDO = roleDataService.validateAndGet(roleId);

            // 构建提示词
            String prompt = buildRoadmapPrompt(roleDO);
            String systemPrompt = buildRoadmapSystemPrompt();

            // 调用 AI 生成
            String result = aiService.generateContent(prompt, systemPrompt);

            log.info("Robot 路径生成完成，taskId={}，响应长度={}", taskId, result.length());

            // 更新任务状态为成功
            updateTaskStatus(taskId, "COMPLETED", result, null);

            // 保存到历史记录
            saveToHistory(userId, taskId);

        } catch (Exception e) {
            log.error("Robot 路径生成失败，taskId={}", taskId, e);
            updateTaskStatus(taskId, "FAILED", null, e.getMessage());
        }
    }

    /**
     * 获取任务状态
     */
    public RobotRoadmapTaskDTO getTaskStatus(String taskId) {
        String taskJson = redisTemplate.opsForValue().get(TASK_KEY_PREFIX + taskId);
        if (taskJson == null) {
            throw new RuntimeException("任务不存在或已过期");
        }

        try {
            return objectMapper.readValue(taskJson, RobotRoadmapTaskDTO.class);
        } catch (Exception e) {
            log.error("Robot Redis 任务解析失败", e);
            throw new RuntimeException("解析任务状态失败", e);
        }
    }

    /**
     * 更新任务状态
     */
    private void updateTaskStatus(String taskId, String status, String result, String error) {
        try {
            RobotRoadmapTaskDTO task = getTaskStatus(taskId);
            task.setStatus(status);
            task.setResult(result);
            task.setError(error);
            task.setCompletedAt(LocalDateTime.now().format(formatter));

            String taskJson = objectMapper.writeValueAsString(task);
            redisTemplate.opsForValue().set(
                TASK_KEY_PREFIX + taskId,
                taskJson,
                TASK_EXPIRE_DAYS,
                TimeUnit.DAYS
            );
        } catch (Exception e) {
            log.error("Robot 任务状态更新失败", e);
        }
    }

    /**
     * 保存任务到历史记录
     */
    private void saveToHistory(Long userId, String taskId) {
        try {
            String historyKey = HISTORY_KEY_PREFIX + userId;
            long timestamp = System.currentTimeMillis();
            redisTemplate.opsForZSet().add(historyKey, taskId, timestamp);

            // 保留最近50条
            Long count = redisTemplate.opsForZSet().zCard(historyKey);
            if (count != null && count > MAX_HISTORY_SIZE) {
                redisTemplate.opsForZSet().removeRange(historyKey, 0, count - MAX_HISTORY_SIZE - 1);
            }

            log.info("Robot 任务 {} 已保存到用户 {} 的历史记录", taskId, userId);
        } catch (Exception e) {
            log.error("Robot 历史记录保存失败", e);
        }
    }

    /**
     * 获取历史记录列表
     */
    public java.util.List<RobotRoadmapTaskDTO> getHistory(Long userId) {
        String historyKey = HISTORY_KEY_PREFIX + userId;
        java.util.Set<String> taskIds = redisTemplate.opsForZSet().reverseRange(historyKey, 0, -1);

        if (taskIds == null || taskIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        return taskIds.stream()
            .map(taskId -> {
                try {
                    return getTaskStatus(taskId);
                } catch (Exception e) {
                    log.warn("Robot 任务 {} 加载失败: {}", taskId, e.getMessage());
                    return null;
                }
            })
            .filter(java.util.Objects::nonNull)
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 保存草稿
     */
    public String saveDraft(Long roleId, Long userId, String draftContent) {
        try {
            // 生成唯一的草稿ID
            String draftId = "draft-" + UUID.randomUUID().toString();

            // 保存草稿内容
            String draftKey = DRAFT_KEY_PREFIX + draftId;
            redisTemplate.opsForValue().set(
                draftKey,
                draftContent,
                DRAFT_EXPIRE_DAYS,
                TimeUnit.DAYS
            );

            // 添加到用户的草稿列表
            String listKey = DRAFT_LIST_KEY_PREFIX + userId;
            long timestamp = System.currentTimeMillis();
            String listValue = roleId + ":" + draftId;
            redisTemplate.opsForZSet().add(listKey, listValue, timestamp);

            // 保留最近100条草稿
            Long count = redisTemplate.opsForZSet().zCard(listKey);
            if (count != null && count > MAX_DRAFT_SIZE) {
                // 删除最早的草稿
                Set<String> oldDrafts = redisTemplate.opsForZSet().range(listKey, 0, count - MAX_DRAFT_SIZE - 1);
                if (oldDrafts != null) {
                    for (String old : oldDrafts) {
                        String oldDraftId = old.split(":")[1];
                        redisTemplate.delete(DRAFT_KEY_PREFIX + oldDraftId);
                    }
                }
                redisTemplate.opsForZSet().removeRange(listKey, 0, count - MAX_DRAFT_SIZE - 1);
            }

            log.info("Robot 草稿 {} 已保存，职业: {}，用户: {}", draftId, roleId, userId);
            return draftId;
        } catch (Exception e) {
            log.error("Robot 草稿保存失败", e);
            throw new RuntimeException("保存草稿失败", e);
        }
    }

    /**
     * 获取草稿
     */
    public String getDraft(String draftId) {
        String draftKey = DRAFT_KEY_PREFIX + draftId;
        return redisTemplate.opsForValue().get(draftKey);
    }

    /**
     * 删除草稿
     */
    public void deleteDraft(Long userId, String draftId) {
        // 删除草稿内容
        String draftKey = DRAFT_KEY_PREFIX + draftId;
        redisTemplate.delete(draftKey);

        // 从用户的草稿列表中删除
        String listKey = DRAFT_LIST_KEY_PREFIX + userId;
        Set<String> allDrafts = redisTemplate.opsForZSet().range(listKey, 0, -1);
        if (allDrafts != null) {
            for (String value : allDrafts) {
                if (value.endsWith(":" + draftId)) {
                    redisTemplate.opsForZSet().remove(listKey, value);
                    break;
                }
            }
        }

        log.info("Robot 草稿 {} 已删除，用户: {}", draftId, userId);
    }

    /**
     * 获取用户的草稿列表
     */
    public List<RobotRoadmapDraftDTO> getDraftList(Long userId) {
        String listKey = DRAFT_LIST_KEY_PREFIX + userId;
        Set<String> draftValues = redisTemplate.opsForZSet().reverseRange(listKey, 0, -1);

        if (draftValues == null || draftValues.isEmpty()) {
            return Collections.emptyList();
        }

        return draftValues.stream()
            .map(value -> {
                try {
                    String[] parts = value.split(":");
                    Long roleId = Long.parseLong(parts[0]);
                    String draftId = parts[1];

                    // 获取保存时间
                    Double score = redisTemplate.opsForZSet().score(listKey, value);
                    String createdAt = score != null ?
                        LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(score.longValue()),
                            ZoneId.systemDefault()
                        ).format(formatter) : null;

                    return RobotRoadmapDraftDTO.builder()
                        .draftId(draftId)
                        .roleId(roleId)
                        .userId(userId)
                        .createdAt(createdAt)
                        .build();
                } catch (Exception e) {
                    log.warn("Robot 草稿值 {} 解析失败: {}", value, e.getMessage());
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    /**
     * 构建路径生成的系统提示词
     */
    private String buildRoadmapSystemPrompt() {
        return """
                你是学习路径规划专家。请严格遵守以下规范：

                ## 输出格式
                1. 必须输出合法的 JSON 格式
                2. 格式：{"nodes": [...], "edges": [...]}
                3. **禁止使用代码块标记**：不要使用 ```json 或 ``` 包裹输出
                4. 不要添加任何说明文字或其他内容
                5. 直接输出 JSON，以 '{' 开头，以 '}' 结尾

                ## 节点格式
                nodes 数组中的每个节点：
                {
                  "id": "temp-0",           // 根节点id为 0，其它节点为临时ID，从 temp-1 开始递增
                  "type": "course",         // 类型：course（完整课程）或 node（知识点片段）
                  "name": "课程名称",
                  "description": "课程描述"
                }

                ## 边格式（表示前置依赖关系）
                edges 数组中的每条边：
                {
                  "source": "temp-1",       // 前置节点ID（必须先学习）
                  "target": "temp-2"        // 目标节点ID（后学习）
                }

                **边的含义**：A -> B 表示"学完A才能学B"，即A是B的前置课程

                **重要**：判断是否应该添加边 A -> B，问自己：
                - "必须先学完A，才能学B吗？"
                - 如果答案是"是"，那么添加这条边
                - 如果答案是"不是"或"可以同时学"，那么不要添加这条边

                ## 路径设计原则

                ### 1. 根节点（职业目标）
                - 第一个节点(id:0)是根节点，代表职业本身（如"Vue.js开发工程师"）
                - 根节点是学习路径的**终点目标**
                - **根节点只有输入边，没有输出边**（有入无出）
                - 所有学习路径最终汇聚到根节点
                - **禁止使用**："XX职业导论"、"XX职业概览"、"XX入门指南"等虚构课程名

                ### 2. 依赖关系设计规则

                **基础课程的依赖关系**（特别注意！）：

                ✅ 正确示例：
                - "HTML与CSS" -> "JavaScript程序设计"（先学标记语言，再学编程）
                - "JavaScript程序设计" -> "TypeScript"（先学JS，再学TS）
                - "JavaScript程序设计" -> "数据结构与算法"（先学语言，再学算法）
                - "JavaScript程序设计" -> "Vue.js开发"（先学语言基础，再学框架）

                ❌ 错误示例：
                - "数据结构与算法" -> "JavaScript程序设计"（错！不需要先学算法才能学JS）
                - "HTTP协议" -> "JavaScript程序设计"（错！这两个是并行基础）
                - "计算机网络" -> "HTML与CSS"（错！不需要先学网络才能写网页）

                **并行学习**（不要添加依赖边）：
                - "HTML与CSS" 和 "Git版本控制" 可以并行学习
                - "JavaScript程序设计" 和 "Node.js基础" 可以并行学习
                - 基础课程之间如果没有明确的先后顺序，就不要添加边

                **框架与生态**：
                - "Vue.js开发" -> "Vue Router"（先学框架核心，再学路由）
                - "Vue.js开发" -> "Pinia状态管理"（先学框架核心，再学状态管理）
                - "Vue Router" 和 "Pinia" 之间不需要依赖（可以独立学习）

                ### 3. 课程命名规范（极其重要！）

                **course（完整课程）命名要求**：
                - 使用该领域**标准、通用、正式**的课程名称
                - 每个字都必须有明确含义，定义清晰的知识边界
                - 必须是一门**完整的、系统性的、可独立开设**的课程
                - 有明确的教学大纲和知识体系
                - 通常在大学、培训机构中作为独立课程存在

                - **禁止使用模糊修饰词**：
                  ❌ "JavaScript核心编程" → ✅ "JavaScript程序设计"
                  ❌ "Vue.js 3.x 核心语法" → ✅ "Vue.js 3基础"
                  ❌ "前端开发基础入门" → ✅ "Web前端开发基础"
                  ❌ "高级进阶实战" → 太模糊，应拆分为具体课程

                - **禁止使用吸引眼球的词汇**：核心、精通、实战、进阶、深入、全面、速成

                - **禁止用章节名称作为课程名**：
                  ❌ "Vue生态系统(Router & Pinia)" → 这是一个章节，不是课程
                  应拆分为：✅ "Vue Router路由管理" 和 ✅ "Pinia状态管理"

                - **课程名应该是独立的知识体系**：
                  ✅ "线性代数"、"数据结构与算法"、"计算机网络"、"操作系统"

                **正确示例**：
                - "Python程序设计" 而非 "Python核心编程"
                - "数据库系统原理" 而非 "数据库基础入门"
                - "机器学习" 而非 "机器学习实战进阶"
                - "Vue.js开发" 而非 "Vue.js 3.x核心语法"

                **node（知识点片段）命名要求**：
                - 可以包含具体技术点，明确来源
                - 表示某个课程中的**一部分内容**，不需要学完整门课
                - 例如："矩阵运算"（来自线性代数）、"HTTP协议"（来自计算机网络）
                - 例如："异步编程模式"（来自JavaScript程序设计）

                ### 4. 注意事项
                - 每个课程名必须定义**清晰的知识边界**
                - 不要创建"阶段"、"模块"、"体系"等抽象节点
                - 避免过度细分：不要把一门课的每个章节都拆成独立节点
                - 课程名要**朴实、标准、专业**，不要哗众取宠
                - 生成 20-30 个节点，确保路径完整
                - 设计**复杂的依赖关系网络**，不要只是简单的线性结构

                ### 5. 实际案例说明

                **场景：Vue.js开发工程师**

                ✅ 正确的课程设计：
                - "HTML与CSS"（标准名称）
                - "JavaScript程序设计"（不是"JavaScript核心"）
                - "TypeScript"（独立课程）
                - "Vue.js开发"（不是"Vue.js 3.x核心语法"）
                - "Vue Router路由管理"（独立课程）
                - "Pinia状态管理"（独立课程）

                ❌ 错误的课程设计：
                - "前端开发基础核心"（太模糊）
                - "JavaScript高级进阶"（不是标准课程名）
                - "Vue生态系统(Router & Pinia)"（这是章节组合，不是课程）
                - "Vue.js 3.x核心语法"（应该就叫"Vue.js开发"）

                请严格按照 JSON 格式输出，确保边的方向正确表示前置依赖关系。
                """;
    }

    /**
     * 构建路径生成的提示词
     */
    private String buildRoadmapPrompt(RoleDO roleDO) {
        return String.format("""
                职业名称：%s
                职业描述：%s

                任务：为该职业生成完整的学习路径。

                请设计一个从零基础到精通的学习路径，包含必要的课程和知识点。
                直接输出 JSON 格式的路径数据。
                """, roleDO.getName(), roleDO.getDescription());
    }
}
