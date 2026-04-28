package com.twicemax.content.roadmap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.twicemax.shared.common.utils.Utils;
import com.twicemax.shared.domain.exception.StatusCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

import static com.twicemax.shared.domain.Enums.ContentState;

/**
 * 路线图领域服务
 *
 * 只依赖 content 域，处理路线图的核心业务逻辑
 *
 * 内容格式 v=2：
 * <pre>
 * {
 *   "v": 2,
 *   "trunk": [
 *     { "t": "c"|"n"|"g"|"o", "id": 123, "label": "...", "children": [...] }
 *   ]
 * }
 * </pre>
 * 类型：c=course, n=node, g=group, o=note
 *
 * 落库规范：
 *  - c/n：必须有 id，不允许 children；label 入库前剥离（读取时根据 id 从课程/节点表查询填充）
 *  - g：label 为用户输入，最大 {@value #MAX_GROUP_LABEL_LEN} 字符；可有 children
 *  - o：label 为用户输入，最大 {@value #MAX_NOTE_LABEL_LEN} 字符；不允许 children
 *  - 最大深度 {@value #MAX_DEPTH}（与前端一致）
 *  - 单层子节点数量上限 {@value #MAX_CHILDREN_PER_NODE}
 *  - 主干节点数量上限 {@value #MAX_TRUNK_SIZE}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoadmapDomainService {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final int CURRENT_VERSION = RoadmapContentDTO.CURRENT_VERSION;
    private static final int MAX_DEPTH = 4;
    private static final int MAX_TRUNK_SIZE = 50;
    private static final int MAX_CHILDREN_PER_NODE = 20;
    private static final int MAX_GROUP_LABEL_LEN = 20;
    private static final int MAX_NOTE_LABEL_LEN = 50;

    private static final String TYPE_COURSE = "c";
    private static final String TYPE_NODE = "n";
    private static final String TYPE_GROUP = "g";
    private static final String TYPE_NOTE = "o";

    private final RoadmapDataService roadmapDataService;

    // ========== Query 方法 ==========

    public List<RoadmapDO> listByState(ContentState state, Long lastId, int limit) {
        return roadmapDataService.listByState(state != null ? state.value() : null, lastId, limit);
    }

    public List<RoadmapDO> listByFilter(Long roadmapId, Long roleId, Long creatorId, Long lastId, int limit) {
        return roadmapDataService.listByFilter(roadmapId, roleId, creatorId, lastId, limit);
    }

    public List<RoadmapDO> getRoadmapsByRolePublic(Long roleId, Long lastId, int limit) {
        if (lastId == null || lastId == 0) {
            return roadmapDataService.getListByRoleOrderBy(roleId, limit, "score");
        } else {
            RoadmapDO lastRoadmap = roadmapDataService.getById(lastId);
            if (lastRoadmap != null) {
                return roadmapDataService.getListByRoleAfterCursorOrderBy(
                    roleId, lastRoadmap.getScore(), lastRoadmap.getCreatedAt(), lastId, limit, "score");
            } else {
                return new ArrayList<>();
            }
        }
    }

    public List<RoadmapDO> getRoadmapsByRole(Long roleId, Long lastId, int limit, String sortBy) {
        if (lastId != null) {
            RoadmapDO lastRoadmap = roadmapDataService.getById(lastId);
            if (lastRoadmap != null) {
                return roadmapDataService.getListByRoleAfterCursorOrderBy(
                    roleId, lastRoadmap.getScore(), lastRoadmap.getCreatedAt(), lastId, limit, sortBy);
            }
        }
        return roadmapDataService.getListByRoleOrderBy(roleId, limit, sortBy);
    }

    public List<RoadmapDO> getUserRoadmaps(Long userId, Long lastId, int limit, Byte state) {
        return roadmapDataService.getListByCreatorWithPaging(userId, lastId, limit, state);
    }

    public List<RoadmapDO> getRoadmapsByIds(List<Long> roadmapIds) {
        return roadmapDataService.getByIds(roadmapIds);
    }

    // ========== 内容校验与处理 ==========

    /**
     * 校验路线图内容格式合法性，并去掉 c/n 节点的 label（由数据库回填）。
     *
     * @param content 前端提交的 v=2 JSON
     * @return 清洗后的标准化 JSON 字符串（用于落库与计算 hash）
     * @throws com.twicemax.shared.domain.exception.BusinessException 内容不合法时抛出
     */
    public String validateAndStrip(String content) {
        RoadmapContentDTO dto = parseContent(content);

        if (dto.getV() != CURRENT_VERSION) {
            throw StatusCode.ROADMAP_CONTENT_INVALID.exception("协议版本不支持: v=" + dto.getV());
        }
        List<RoadmapNodeDTO> trunk = dto.getTrunk();
        if (trunk == null || trunk.isEmpty()) {
            throw StatusCode.ROADMAP_CONTENT_INVALID.exception("主干为空");
        }
        if (trunk.size() > MAX_TRUNK_SIZE) {
            throw StatusCode.ROADMAP_CONTENT_INVALID.exception("主干节点过多");
        }

        for (RoadmapNodeDTO node : trunk) {
            validateNode(node, 1);
        }

        try {
            return objectMapper.writeValueAsString(dto);
        } catch (Exception e) {
            throw StatusCode.JSON_PROCESSING_ERROR.exception(e);
        }
    }

    private void validateNode(RoadmapNodeDTO node, int depth) {
        if (node == null) {
            throw StatusCode.ROADMAP_CONTENT_INVALID.exception("节点为空");
        }
        if (depth > MAX_DEPTH) {
            throw StatusCode.ROADMAP_CONTENT_INVALID.exception("节点深度超过上限");
        }
        String t = node.getT();
        if (t == null) {
            throw StatusCode.ROADMAP_CONTENT_INVALID.exception("节点缺少类型");
        }

        List<RoadmapNodeDTO> children = node.getChildren();
        boolean hasChildren = children != null && !children.isEmpty();

        switch (t) {
            case TYPE_COURSE:
            case TYPE_NODE: {
                if (node.getId() == null || node.getId() <= 0) {
                    throw StatusCode.ROADMAP_CONTENT_INVALID.exception(t + " 节点缺少 id");
                }
                if (hasChildren) {
                    throw StatusCode.ROADMAP_CONTENT_INVALID.exception(t + " 节点不允许有子节点");
                }
                node.setLabel(null); // c/n 的 label 不入库
                break;
            }
            case TYPE_GROUP: {
                String label = trim(node.getLabel());
                if (label.isEmpty()) {
                    throw StatusCode.ROADMAP_CONTENT_INVALID.exception("分组节点缺少标签");
                }
                if (label.length() > MAX_GROUP_LABEL_LEN) {
                    throw StatusCode.ROADMAP_CONTENT_INVALID.exception(
                        "分组节点标签过长（最多 " + MAX_GROUP_LABEL_LEN + " 字）");
                }
                node.setLabel(label);
                node.setId(null); // g 不应有 id
                break;
            }
            case TYPE_NOTE: {
                String label = trim(node.getLabel());
                if (label.isEmpty()) {
                    throw StatusCode.ROADMAP_CONTENT_INVALID.exception("说明节点缺少文本");
                }
                if (label.length() > MAX_NOTE_LABEL_LEN) {
                    throw StatusCode.ROADMAP_CONTENT_INVALID.exception(
                        "说明节点文本过长（最多 " + MAX_NOTE_LABEL_LEN + " 字）");
                }
                if (hasChildren) {
                    throw StatusCode.ROADMAP_CONTENT_INVALID.exception("说明节点不允许有子节点");
                }
                node.setLabel(label);
                node.setId(null);
                break;
            }
            default:
                throw StatusCode.ROADMAP_CONTENT_INVALID.exception("非法节点类型: " + t);
        }

        if (hasChildren) {
            if (children.size() > MAX_CHILDREN_PER_NODE) {
                throw StatusCode.ROADMAP_CONTENT_INVALID.exception("子节点数量超过上限");
            }
            for (RoadmapNodeDTO child : children) {
                validateNode(child, depth + 1);
            }
        }
    }

    /**
     * 从内容中收集所有 c 节点的 courseId 与 n 节点的 nodeId（用于跨域批量校验存在性）。
     */
    public BoundIds collectBoundIds(String content) {
        RoadmapContentDTO dto = parseContent(content);
        Set<Long> courseIds = new HashSet<>();
        Set<Long> nodeIds = new HashSet<>();
        if (dto.getTrunk() != null) {
            for (RoadmapNodeDTO node : dto.getTrunk()) {
                walkCollect(node, courseIds, nodeIds);
            }
        }
        return new BoundIds(courseIds, nodeIds);
    }

    private void walkCollect(RoadmapNodeDTO node, Set<Long> courseIds, Set<Long> nodeIds) {
        if (node == null) return;
        if (TYPE_COURSE.equals(node.getT()) && node.getId() != null) {
            courseIds.add(node.getId());
        } else if (TYPE_NODE.equals(node.getT()) && node.getId() != null) {
            nodeIds.add(node.getId());
        }
        if (node.getChildren() != null) {
            for (RoadmapNodeDTO child : node.getChildren()) {
                walkCollect(child, courseIds, nodeIds);
            }
        }
    }

    /**
     * 统计 c+n 叶子节点数量（用于 nodeCount 字段）。
     */
    public int countLeafBindings(String content) {
        RoadmapContentDTO dto = parseContent(content);
        int[] counter = {0};
        if (dto.getTrunk() != null) {
            for (RoadmapNodeDTO node : dto.getTrunk()) {
                walkCount(node, counter);
            }
        }
        return counter[0];
    }

    private void walkCount(RoadmapNodeDTO node, int[] counter) {
        if (node == null) return;
        if (TYPE_COURSE.equals(node.getT()) || TYPE_NODE.equals(node.getT())) {
            counter[0]++;
        }
        if (node.getChildren() != null) {
            for (RoadmapNodeDTO child : node.getChildren()) {
                walkCount(child, counter);
            }
        }
    }

    /**
     * 用查询到的课程/节点名称回填 c/n 的 label，输出可供前端渲染的完整 JSON。
     *
     * @param content 数据库中的 v=2 JSON
     * @param courseNames courseId → 课程名称（缺失项 label 留空）
     * @param nodeNames nodeId → 节点名称
     */
    public String enrichLabels(String content,
                               Map<Long, String> courseNames,
                               Map<Long, String> nodeNames) {
        RoadmapContentDTO dto = parseContent(content);
        if (dto.getTrunk() != null) {
            for (RoadmapNodeDTO node : dto.getTrunk()) {
                walkEnrich(node, courseNames != null ? courseNames : Collections.emptyMap(),
                    nodeNames != null ? nodeNames : Collections.emptyMap());
            }
        }
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (Exception e) {
            throw StatusCode.JSON_PROCESSING_ERROR.exception(e);
        }
    }

    private void walkEnrich(RoadmapNodeDTO node,
                            Map<Long, String> courseNames,
                            Map<Long, String> nodeNames) {
        if (node == null) return;
        if (TYPE_COURSE.equals(node.getT()) && node.getId() != null) {
            String name = courseNames.get(node.getId());
            node.setLabel(name != null ? name : "");
        } else if (TYPE_NODE.equals(node.getT()) && node.getId() != null) {
            String name = nodeNames.get(node.getId());
            node.setLabel(name != null ? name : "");
        }
        if (node.getChildren() != null) {
            for (RoadmapNodeDTO child : node.getChildren()) {
                walkEnrich(child, courseNames, nodeNames);
            }
        }
    }

    /**
     * 计算内容 hash。基于已剥离 label 的标准化 JSON 计算 md5。
     * 调用方应当先经过 {@link #validateAndStrip(String)}，传入清洗后的 JSON。
     */
    public String calculateContentHash(String cleanedContent) {
        try {
            return Utils.md5(cleanedContent);
        } catch (Exception e) {
            log.error("内容哈希计算失败: {}", cleanedContent, e);
            throw StatusCode.CONTENT_HASH_ERROR.exception(e);
        }
    }

    private RoadmapContentDTO parseContent(String content) {
        try {
            RoadmapContentDTO dto = objectMapper.readValue(content, RoadmapContentDTO.class);
            if (dto == null) {
                throw StatusCode.ROADMAP_CONTENT_INVALID.exception("内容为空");
            }
            return dto;
        } catch (com.twicemax.shared.domain.exception.BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("路线图内容解析失败", e);
            throw StatusCode.ROADMAP_CONTENT_INVALID.exception("内容格式错误");
        }
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    /**
     * c/n 节点引用的 courseId 与 nodeId 集合（用于批量校验存在性）。
     */
    public static final class BoundIds {
        private final Set<Long> courseIds;
        private final Set<Long> nodeIds;

        public BoundIds(Set<Long> courseIds, Set<Long> nodeIds) {
            this.courseIds = courseIds;
            this.nodeIds = nodeIds;
        }

        public Set<Long> getCourseIds() {
            return courseIds;
        }

        public Set<Long> getNodeIds() {
            return nodeIds;
        }
    }

    // ========== Command 方法 ==========

    /**
     * 创建路线图。content 应是经过 {@link #validateAndStrip(String)} 处理后的标准化 JSON。
     */
    @Transactional
    public Long createRoadmap(long roleId, String cleanedContent, String description,
                              long userId, int nodeCount, Byte state) {
        if (!ContentState.DRAFT.value().equals(state) && !ContentState.SUBMITTED.value().equals(state)) {
            throw new IllegalArgumentException("状态非法");
        }

        RoadmapDO roadmapDO = new RoadmapDO();
        roadmapDO.setContent(cleanedContent);
        roadmapDO.setContentHash(calculateContentHash(cleanedContent));
        roadmapDO.setDescription(description);
        roadmapDO.setRoleId(roleId);
        roadmapDO.setCreatorId(userId);
        roadmapDO.setNodeCount(nodeCount);
        roadmapDO.setState(state);
        roadmapDO.setScore(0.0);

        roadmapDataService.insert(roadmapDO);
        log.info("路线图 创建成功: roadmapId={}, roleId={}, userId={}, state={}",
            roadmapDO.getId(), roleId, userId, state);

        return roadmapDO.getId();
    }

    /**
     * 更新路线图。content 应是经过 {@link #validateAndStrip(String)} 处理后的标准化 JSON。
     */
    @Transactional
    public void updateRoadmap(long id, String cleanedContent, int nodeCount) {
        roadmapDataService.validateExists(id);

        RoadmapDO roadmapDO = roadmapDataService.getById(id);
        roadmapDO.setContent(cleanedContent);
        roadmapDO.setContentHash(calculateContentHash(cleanedContent));
        roadmapDO.setNodeCount(nodeCount);
        roadmapDO.setUpdatedAt(LocalDateTime.now());

        roadmapDataService.update(roadmapDO);
        log.info("路线图 更新成功: roadmapId={}", id);
    }

    @Transactional
    public void deleteRoadmap(long id) {
        roadmapDataService.validateExists(id);
        int result = roadmapDataService.softDelete(id);
        if (result == 0) {
            throw StatusCode.ROADMAP_NOT_FOUND.exception();
        }
        log.info("路线图 删除成功: roadmapId={}", id);
    }

    @Transactional
    public void approve(long id) {
        roadmapDataService.validateExists(id);
        RoadmapDO roadmap = roadmapDataService.getById(id);
        Utils.validateStateTransition(roadmap.getState(), ContentState.PUBLISHED);
        roadmapDataService.approve(id);
        log.info("路线图 审核通过: roadmapId={}", id);
    }

    @Transactional
    public void reject(long id, String reason) {
        roadmapDataService.validateExists(id);
        RoadmapDO roadmap = roadmapDataService.getById(id);
        Utils.validateStateTransition(roadmap.getState(), ContentState.REJECTED);
        roadmapDataService.reject(id, reason);
        log.info("路线图 审核拒绝: roadmapId={}, reason={}", id, reason);
    }

    @Transactional
    public void ban(long id, String reason) {
        roadmapDataService.validateExists(id);
        RoadmapDO roadmap = roadmapDataService.getById(id);
        Utils.validateStateTransition(roadmap.getState(), ContentState.BANNED);
        roadmapDataService.ban(id, reason);
        log.info("路线图 封禁: roadmapId={}, reason={}", id, reason);
    }

    @Transactional
    public void updateDescription(long id, String description) {
        roadmapDataService.validateExists(id);
        RoadmapDO roadmap = roadmapDataService.getById(id);
        roadmap.setDescription(description != null ? description : "");
        roadmapDataService.update(roadmap);
        log.info("路线图 描述更新成功: roadmapId={}", id);
    }
}
