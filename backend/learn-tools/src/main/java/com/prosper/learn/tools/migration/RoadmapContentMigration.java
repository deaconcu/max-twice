package com.prosper.learn.application.runner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.prosper.learn.content.course.CourseDataService;
import com.prosper.learn.content.course.CourseDO;
import com.prosper.learn.content.roadmap.RoadmapDataService;
import com.prosper.learn.content.roadmap.RoadmapDO;
import com.prosper.learn.content.roadmap.RoadmapDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Roadmap Content 数据迁移服务
 *
 * 将 roadmap.content 中的 course_id 转换为对应的 root_node_id
 *
 * 执行方式：
 * 1. 通过管理后台手动触发
 * 2. 或通过命令行参数启动应用时执行
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoadmapContentMigration {

    private final RoadmapDataService roadmapDataService;
    private final CourseDataService courseDataService;
    private final RoadmapDomainService roadmapDomainService;
    private final ObjectMapper objectMapper;

    /**
     * 迁移所有 Roadmap 的 content 字段
     *
     * @return 迁移成功的数量
     */
    @Transactional
    public int migrateAllRoadmaps() {
        log.info("开始迁移 Roadmap content 数据...");

        // 分批查询所有未删除的 Roadmap
        List<RoadmapDO> allRoadmaps = new ArrayList<>();
        Long lastId = null;
        int batchSize = 20;

        while (true) {
            // 使用 listByFilter 分批获取（state=null 表示所有状态）
            List<RoadmapDO> batch = roadmapDataService.listByFilter(null, null, null, lastId);
            if (batch.isEmpty()) {
                break;
            }
            allRoadmaps.addAll(batch);
            lastId = batch.get(batch.size() - 1).getId();

            // 如果返回的数量少于 20（默认限制），说明已经到最后了
            if (batch.size() < batchSize) {
                break;
            }
        }

        log.info("查询到 {} 个 Roadmap，开始迁移...", allRoadmaps.size());

        int successCount = 0;
        int failCount = 0;

        for (RoadmapDO roadmap : allRoadmaps) {
            try {
                boolean migrated = migrateRoadmapContent(roadmap);
                if (migrated) {
                    successCount++;
                    log.info("成功迁移 Roadmap ID: {}", roadmap.getId());
                } else {
                    log.info("跳过已迁移的 Roadmap ID: {}", roadmap.getId());
                }
            } catch (Exception e) {
                failCount++;
                log.error("迁移 Roadmap ID: {} 失败", roadmap.getId(), e);
            }
        }

        log.info("Roadmap content 迁移完成。成功: {}, 失败: {}, 跳过: {}",
                successCount, failCount, allRoadmaps.size() - successCount - failCount);

        return successCount;
    }

    /**
     * 迁移单个 Roadmap 的 content
     *
     * @param roadmap Roadmap 对象
     * @return true=已迁移，false=已经是新格式或无需迁移
     */
    private boolean migrateRoadmapContent(RoadmapDO roadmap) throws Exception {
        String oldContent = roadmap.getContent();

        // 解析 JSON: [[edges], [nodes]]
        JsonNode root = objectMapper.readTree(oldContent);
        if (!root.isArray() || root.size() < 2) {
            log.warn("Roadmap ID: {} 的 content 格式不正确", roadmap.getId());
            return false;
        }

        ArrayNode edges = (ArrayNode) root.get(0);
        ArrayNode nodes = (ArrayNode) root.get(1);

        // 检查是否需要迁移（如果第一个节点是对象，说明已经是新格式）
        if (nodes.size() > 0 && nodes.get(0).isNumber()) {
            // 旧格式：[courseId1, courseId2, ...]
            ArrayNode newNodes = objectMapper.createArrayNode();

            for (JsonNode node : nodes) {
                long courseId = node.asLong();

                // 查询 Course 的 root_node_id
                CourseDO course = courseDataService.getById(courseId);
                if (course == null) {
                    log.warn("Roadmap ID: {} 引用了不存在的 Course ID: {}", roadmap.getId(), courseId);
                    newNodes.add(courseId); // 保持原值，避免数据丢失
                    continue;
                }

                // 使用 root_node_id 替换 course_id
                newNodes.add(course.getRootNodeId());
            }

            // 构建新的 content
            ArrayNode newContent = objectMapper.createArrayNode();
            newContent.add(edges);
            newContent.add(newNodes);

            String newContentStr = objectMapper.writeValueAsString(newContent);
            // 使用现有的 calculateContentHash 方法（会标准化并使用 MD5）
            String newHash = roadmapDomainService.calculateContentHash(newContentStr);

            // 更新 Roadmap
            roadmap.setContent(newContentStr);
            roadmap.setContentHash(newHash);
            roadmapDataService.update(roadmap);

            return true;
        } else {
            // 已经是新格式或无法识别，跳过
            return false;
        }
    }

    /**
     * 验证迁移结果
     *
     * @return 验证通过的数量
     */
    public int validateMigration() {
        log.info("开始验证 Roadmap content 迁移结果...");

        // 分批查询所有未删除的 Roadmap
        List<RoadmapDO> allRoadmaps = new ArrayList<>();
        Long lastId = null;
        int batchSize = 20;

        while (true) {
            List<RoadmapDO> batch = roadmapDataService.listByFilter(null, null, null, lastId);
            if (batch.isEmpty()) {
                break;
            }
            allRoadmaps.addAll(batch);
            lastId = batch.get(batch.size() - 1).getId();
            if (batch.size() < batchSize) {
                break;
            }
        }

        int validCount = 0;
        int invalidCount = 0;

        for (RoadmapDO roadmap : allRoadmaps) {
            try {
                JsonNode root = objectMapper.readTree(roadmap.getContent());
                if (!root.isArray() || root.size() < 2) {
                    invalidCount++;
                    log.warn("Roadmap ID: {} 的 content 格式不正确", roadmap.getId());
                    continue;
                }

                JsonNode nodes = root.get(1);

                // 检查所有节点都是数字（node_id）
                boolean allNumbers = true;
                for (JsonNode node : nodes) {
                    if (!node.isNumber()) {
                        allNumbers = false;
                        break;
                    }
                }

                if (allNumbers) {
                    validCount++;
                } else {
                    invalidCount++;
                    log.warn("Roadmap ID: {} 的节点格式不正确", roadmap.getId());
                }

            } catch (Exception e) {
                invalidCount++;
                log.error("验证 Roadmap ID: {} 失败", roadmap.getId(), e);
            }
        }

        log.info("验证完成。有效: {}, 无效: {}", validCount, invalidCount);
        return validCount;
    }
}
