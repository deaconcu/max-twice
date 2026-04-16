package com.prosper.learn.application.runner;

import com.prosper.learn.content.course.CourseDO;
import com.prosper.learn.content.course.CourseDataService;
import com.prosper.learn.content.node.NodeDO;
import com.prosper.learn.content.node.NodeDataService;
import com.prosper.learn.shared.domain.Enums;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 课程根节点数据修正
 *
 * 问题：
 * 1. 多个 course 的 root_node_id 指向同一个 node（错误）
 * 2. 根节点的 name/description 是"课程根目录"，与 course 不一致
 *
 * 修正逻辑：
 * 1. 先同步现有根节点的 name/description 为对应 course 的数据
 * 2. 检查 root_node_id 重复使用的情况
 * 3. 为重复的 course 创建新的根节点
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CourseRootNodeFixMigration {

    private final CourseDataService courseDataService;
    private final NodeDataService nodeDataService;

    public void execute() {
        log.info("========== 开始修正课程根节点数据 ==========");

        // 获取所有课程（通过循环分页获取）
        List<CourseDO> allCourses = getAllCourses();
        log.info("共找到 {} 个课程", allCourses.size());

        // 步骤1: 同步根节点的 name/description
        //syncRootNodeInfo(allCourses);

        // 步骤2: 修复重复的 root_node_id
        fixDuplicateRootNodeIds(allCourses);

        log.info("========== 修正完成 ==========");
    }

    /**
     * 获取所有课程（通过分页循环）
     */
    private List<CourseDO> getAllCourses() {
        List<CourseDO> allCourses = new ArrayList<>();
        Long lastId = null;

        while (true) {
            List<CourseDO> page = courseDataService.listByLastId(lastId);
            if (page.isEmpty()) {
                break;
            }

            allCourses.addAll(page);

            // 如果返回的数据少于 100，说明已经到最后一页
            if (page.size() < 100) {
                break;
            }

            // 更新 lastId 为当前页最后一个的 id
            lastId = page.get(page.size() - 1).getId();
        }

        return allCourses;
    }

    /**
     * 步骤1: 同步根节点的 name/description 为对应 course 的数据
     */
    private void syncRootNodeInfo(List<CourseDO> courses) {
        log.info("\n步骤1: 查找所有根节点并同步 name/description");

        // 查询所有根节点（is_course_root = 1）
        List<NodeDO> allRootNodes = nodeDataService.listByState(null, null, 10000, true)
            .stream()
            .filter(node -> node.getIsCourseRoot() != null && node.getIsCourseRoot() == 1)
            .collect(Collectors.toList());

        log.info("找到 {} 个根节点", allRootNodes.size());

        int updatedCount = 0;
        int errorCount = 0;

        // 遍历所有根节点，查询对应课程并同步 name/description
        for (NodeDO rootNode : allRootNodes) {
            try {
                Long courseId = rootNode.getCourseId();

                // 直接从数据库查询 course
                CourseDO course = courseDataService.getById(courseId);

                if (course == null) {
                    log.warn("根节点 {} 的 course_id={} 找不到对应的课程，跳过",
                        rootNode.getId(), courseId);
                    continue;
                }

                // 检查是否需要更新
                boolean needUpdate = false;

                if (!course.getName().equals(rootNode.getName())) {
                    log.info("更新根节点 {} 的 name: '{}' -> '{}'",
                        rootNode.getId(), rootNode.getName(), course.getName());
                    rootNode.setName(course.getName());
                    needUpdate = true;
                }

                if (!equals(course.getDescription(), rootNode.getDescription())) {
                    log.info("更新根节点 {} 的 description (course_id={})",
                        rootNode.getId(), courseId);
                    rootNode.setDescription(course.getDescription());
                    needUpdate = true;
                }

                if (needUpdate) {
                    nodeDataService.update(rootNode);
                    updatedCount++;
                }

            } catch (Exception e) {
                errorCount++;
                log.error("同步根节点 {} 失败: {}",
                    rootNode.getId(), e.getMessage(), e);
            }
        }

        log.info("步骤1完成: 更新了 {} 个根节点，失败 {} 个\n", updatedCount, errorCount);
    }

    /**
     * 步骤2: 检查每个 course 的 root_node，如果名称不一致则创建新的根节点
     */
    private void fixDuplicateRootNodeIds(List<CourseDO> courses) {
        log.info("步骤2: 检查 course.root_node_id 并修复名称不一致的问题");

        int createdCount = 0;
        int errorCount = 0;

        for (CourseDO course : courses) {
            try {
                Long rootNodeId = course.getRootNodeId();
                if (rootNodeId == null || rootNodeId == 0) {
                    log.warn("课程 {} (id={}) 的 root_node_id 为空，创建新根节点",
                        course.getName(), course.getId());
                    createAndAssignNewRootNode(course);
                    createdCount++;
                    continue;
                }

                NodeDO rootNode = nodeDataService.getById(rootNodeId);
                if (rootNode == null) {
                    log.warn("课程 {} (id={}) 的根节点 {} 不存在，创建新根节点",
                        course.getName(), course.getId(), rootNodeId);
                    createAndAssignNewRootNode(course);
                    createdCount++;
                    continue;
                }

                // 检查名称是否一致
                if (!course.getName().equals(rootNode.getName())) {
                    log.warn("课程 {} (id={}) 的根节点 {} 名称不一致: course='{}', node='{}'，创建新根节点",
                        course.getName(), course.getId(), rootNodeId, course.getName(), rootNode.getName());
                    createAndAssignNewRootNode(course);
                    createdCount++;
                }

            } catch (Exception e) {
                errorCount++;
                log.error("处理课程 {} (id={}) 失败: {}",
                    course.getName(), course.getId(), e.getMessage(), e);
            }
        }

        log.info("步骤2完成: 创建了 {} 个新根节点，失败 {} 个", createdCount, errorCount);
    }

    /**
     * 创建新根节点并赋值给 course
     */
    private void createAndAssignNewRootNode(CourseDO course) {
        NodeDO newRootNode = createRootNode(course);
        log.info("为课程 {} (id={}) 创建新根节点 {}", course.getName(), course.getId(), newRootNode.getId());

        course.setRootNodeId(newRootNode.getId());
        courseDataService.update(course);
        log.info("已更新课程 {} 的 root_node_id 为 {}", course.getId(), newRootNode.getId());
    }

    /**
     * 创建根节点
     */
    private NodeDO createRootNode(CourseDO course) {
        NodeDO node = new NodeDO();
        node.setName(course.getName());
        node.setDescription(course.getDescription());
        node.setCourseId(course.getId());
        node.setCreatorId(course.getCreatorId());
        node.setState(Enums.ContentState.PUBLISHED.value());
        node.setIsCourseRoot((byte) 1);

        nodeDataService.insert(node);
        return node;
    }

    /**
     * 比较两个字符串是否相等（处理 null）
     */
    private boolean equals(String s1, String s2) {
        if (s1 == null && s2 == null) return true;
        if (s1 == null || s2 == null) return false;
        return s1.equals(s2);
    }
}
