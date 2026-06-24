package com.twicemax.content.roadmap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.twicemax.content.shared.revision.ContentRevisionDO;
import com.twicemax.content.shared.revision.ContentRevisionDataService;
import com.twicemax.shared.common.utils.CanonicalJson;
import com.twicemax.shared.domain.Enums.NewContentType;
import com.twicemax.shared.domain.Enums.RevisionStatus;
import com.twicemax.shared.domain.Enums.NewContentState;
import com.twicemax.shared.domain.exception.StatusCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 路线图领域服务（revision 模型）。
 * <p>
 * 主体状态（roadmap.state）只有 NEVER_PUBLISHED / PUBLISHED / BANNED 三种，描述对外可见性；
 * 一次"提交→审核"的生命周期落在 {@link ContentRevisionDO}（status: SUBMITTED / PUBLISHED /
 * REJECTED / WITHDRAWN）。两者通过 current_revision_id（已发布版本）和 pending_revision_id
 * （审核中版本）连接。
 * <p>
 * 内容格式 v=2：
 * <pre>
 * { "v": 2, "trunk": [ { "t": "c"|"n"|"g"|"o", "id": 123, "label": "...", "children": [...] } ] }
 * </pre>
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

    private static final String CONTENT_TYPE = NewContentType.ROADMAP_VALUE;

    private final RoadmapDataService roadmapDataService;
    private final ContentRevisionDataService revisionDataService;

    // ========== Query 方法 ==========

    public List<RoadmapDO> listByState(NewContentState state, Long lastId, int limit) {
        return roadmapDataService.listByState(state != null ? state.value() : null, lastId, limit);
    }

    public List<RoadmapDO> listByFilter(Long roadmapId, Long roleId, Long creatorId, Long lastId, int limit) {
        return roadmapDataService.listByFilter(roadmapId, roleId, creatorId, lastId, limit);
    }

    public List<RoadmapDO> getRoadmapsByRolePublic(Long roleId, Long lastId, int limit) {
        if (lastId == null || lastId == 0) {
            return roadmapDataService.getListByRoleOrderBy(roleId, limit, "score");
        }
        RoadmapDO lastRoadmap = roadmapDataService.getById(lastId);
        if (lastRoadmap == null) {
            return new ArrayList<>();
        }
        return roadmapDataService.getListByRoleAfterCursorOrderBy(
            roleId, lastRoadmap.getScore(), lastRoadmap.getCreatedAt(), lastId, limit, "score");
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

    /**
     * @param state 仅传入 PUBLISHED / NEVER_PUBLISHED / BANNED；null 时 mapper 默认排除 BANNED。
     */
    public List<RoadmapDO> getUserRoadmaps(Long userId, Long lastId, int limit, NewContentState state) {
        return roadmapDataService.getListByCreatorWithPaging(
            userId, lastId, limit, state != null ? state.value() : null);
    }

    public List<RoadmapDO> getRoadmapsByIds(List<Long> roadmapIds) {
        return roadmapDataService.getByIds(roadmapIds);
    }

    // ========== 内容校验与处理 ==========

    /**
     * 校验路线图内容格式合法性，并去掉 c/n 节点的 label（由数据库回填）。
     * 返回的 JSON 已做 Jackson 序列化，但未做 canonical 化；调用方需要 hash/落库时请额外走 {@link CanonicalJson}.
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
                node.setLabel(null);
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
                node.setId(null);
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

    // ========== Command 方法（revision 模型）==========

    /**
     * 创建一个空的 roadmap 主体（NEVER_PUBLISHED，仅承载 draft）。
     * 不写 revision，不写 content/content_hash；node_count = 0。
     */
    @Transactional
    public Long createDraft(long roleId, String draftContent, String description, long userId) {
        RoadmapDO roadmapDO = new RoadmapDO();
        roadmapDO.setRoleId(roleId);
        roadmapDO.setCreatorId(userId);
        roadmapDO.setDescription(description);
        roadmapDO.setState(NewContentState.NEVER_PUBLISHED_VALUE);
        roadmapDO.setDraftContent(draftContent);
        roadmapDO.setDraftUpdatedAt(LocalDateTime.now());
        roadmapDO.setNodeCount(0);
        roadmapDO.setScore(0.0);
        roadmapDataService.insert(roadmapDO);
        log.info("Roadmap 创建草稿: roadmapId={}, roleId={}, userId={}", roadmapDO.getId(), roleId, userId);
        return roadmapDO.getId();
    }

    /**
     * 仅更新草稿（save-draft）。不动 state / current / pending。
     */
    @Transactional
    public void saveDraft(long roadmapId, String draftContent, String description) {
        roadmapDataService.validateExists(roadmapId);
        roadmapDataService.updateDraft(roadmapId, draftContent, LocalDateTime.now(), description);
        log.info("Roadmap 保存草稿: roadmapId={}", roadmapId);
    }

    /**
     * 提交审核：基于 cleanedContent 生成 canonical payload + SHA-256 hash，
     * 与最近一次 revision 比对去重，写入新的 SUBMITTED revision，并把
     * roadmap.pending_revision_id 切到新 revision、清空 draft。
     *
     * @param cleanedContent 已经过 {@link #validateAndStrip} 的内容
     * @return 新 revision 的 id
     * @throws com.twicemax.shared.domain.exception.BusinessException 已存在 pending 或 hash 与最近一次相同
     */
    @Transactional
    public Long submit(long roadmapId, String cleanedContent, long authorId) {
        RoadmapDO roadmap = roadmapDataService.validateAndGet(roadmapId);

        if (NewContentState.BANNED_VALUE.equals(roadmap.getState())) {
            throw StatusCode.INVALID_PARAMETER.exception("已封禁的路线图不能提交");
        }
        if (roadmap.getPendingRevisionId() != null) {
            throw StatusCode.INVALID_PARAMETER.exception("已有审核中的版本，请先撤回再提交");
        }

        String payload = CanonicalJson.canonicalize(cleanedContent);
        String hash = CanonicalJson.hash(cleanedContent);

        // 与最近一次 revision（任意状态）比对，禁止重复提交相同内容
        ContentRevisionDO latest = revisionDataService.getLatest(CONTENT_TYPE, roadmapId);
        if (latest != null && Objects.equals(hash, latest.getHash())) {
            throw StatusCode.INVALID_PARAMETER.exception("内容与最近一次版本相同，无需重复提交");
        }

        ContentRevisionDO revision = new ContentRevisionDO();
        revision.setContentType(CONTENT_TYPE);
        revision.setContentId(roadmapId);
        revision.setRevisionNo(revisionDataService.nextRevisionNo(CONTENT_TYPE, roadmapId));
        revision.setStatus(RevisionStatus.SUBMITTED_VALUE);
        revision.setPayload(payload);
        revision.setHash(hash);
        revision.setAuthorId(authorId);
        revisionDataService.insert(revision);

        // 切 pending、清 draft
        roadmapDataService.updatePending(roadmapId, revision.getId(), null, null);
        log.info("Roadmap 提交审核: roadmapId={}, revisionId={}, revisionNo={}",
            roadmapId, revision.getId(), revision.getRevisionNo());
        return revision.getId();
    }

    /**
     * 作者撤回审核中的版本。revision → WITHDRAWN，pending 清空，回填 draft（payload）。
     */
    @Transactional
    public void withdraw(long roadmapId, long authorId) {
        RoadmapDO roadmap = roadmapDataService.validateAndGet(roadmapId);
        Long pendingId = roadmap.getPendingRevisionId();
        if (pendingId == null) {
            throw StatusCode.INVALID_PARAMETER.exception("当前没有审核中的版本");
        }
        ContentRevisionDO revision = revisionDataService.validateAndGet(pendingId);
        if (!RevisionStatus.SUBMITTED_VALUE.equals(revision.getStatus())) {
            throw StatusCode.INVALID_PARAMETER.exception("版本状态非 SUBMITTED，无法撤回");
        }

        revision.setStatus(RevisionStatus.WITHDRAWN_VALUE);
        revision.setReviewedAt(LocalDateTime.now());
        revisionDataService.updateStatus(revision);

        roadmapDataService.updatePending(roadmapId, null, revision.getPayload(), LocalDateTime.now());
        log.info("Roadmap 撤回审核: roadmapId={}, revisionId={}, authorId={}", roadmapId, pendingId, authorId);
    }

    /**
     * 审核通过：revision → PUBLISHED，roadmap.state = PUBLISHED，
     * content/contentHash/nodeCount/current_revision_id 一并更新，pending 清空。
     */
    @Transactional
    public void approve(long roadmapId, long reviewerId) {
        RoadmapDO roadmap = roadmapDataService.validateAndGet(roadmapId);
        Long pendingId = roadmap.getPendingRevisionId();
        if (pendingId == null) {
            throw StatusCode.INVALID_PARAMETER.exception("当前没有审核中的版本");
        }
        ContentRevisionDO revision = revisionDataService.validateAndGet(pendingId);
        if (!RevisionStatus.SUBMITTED_VALUE.equals(revision.getStatus())) {
            throw StatusCode.INVALID_PARAMETER.exception("版本状态非 SUBMITTED，无法通过");
        }

        revision.setStatus(RevisionStatus.PUBLISHED_VALUE);
        revision.setReviewerId(reviewerId);
        revision.setReviewedAt(LocalDateTime.now());
        revisionDataService.updateStatus(revision);

        Integer nodeCount = countLeafBindings(revision.getPayload());
        roadmapDataService.approve(roadmapId, revision.getPayload(), revision.getHash(), nodeCount, revision.getId());
        log.info("Roadmap 审核通过: roadmapId={}, revisionId={}, reviewerId={}", roadmapId, pendingId, reviewerId);
    }

    /**
     * 审核驳回：revision → REJECTED（带 reason），pending 清空，回填 draft（payload 供作者继续编辑）。
     */
    @Transactional
    public void reject(long roadmapId, String reason, long reviewerId) {
        RoadmapDO roadmap = roadmapDataService.validateAndGet(roadmapId);
        Long pendingId = roadmap.getPendingRevisionId();
        if (pendingId == null) {
            throw StatusCode.INVALID_PARAMETER.exception("当前没有审核中的版本");
        }
        ContentRevisionDO revision = revisionDataService.validateAndGet(pendingId);
        if (!RevisionStatus.SUBMITTED_VALUE.equals(revision.getStatus())) {
            throw StatusCode.INVALID_PARAMETER.exception("版本状态非 SUBMITTED，无法驳回");
        }

        revision.setStatus(RevisionStatus.REJECTED_VALUE);
        revision.setRejectReason(reason);
        revision.setReviewerId(reviewerId);
        revision.setReviewedAt(LocalDateTime.now());
        revisionDataService.updateStatus(revision);

        roadmapDataService.updatePending(roadmapId, null, revision.getPayload(), LocalDateTime.now());
        log.info("Roadmap 审核驳回: roadmapId={}, revisionId={}, reason={}", roadmapId, pendingId, reason);
    }

    /**
     * 封禁：roadmap.state = BANNED；若存在 pending revision，连带标 REJECTED。draft 回填 pending payload。
     */
    @Transactional
    public void ban(long roadmapId, String reason, long operatorId) {
        RoadmapDO roadmap = roadmapDataService.validateAndGet(roadmapId);
        if (NewContentState.BANNED_VALUE.equals(roadmap.getState())) {
            throw StatusCode.INVALID_PARAMETER.exception("已是封禁状态");
        }

        String draftContent = null;
        LocalDateTime draftUpdatedAt = null;

        Long pendingId = roadmap.getPendingRevisionId();
        if (pendingId != null) {
            ContentRevisionDO revision = revisionDataService.validateAndGet(pendingId);
            if (RevisionStatus.SUBMITTED_VALUE.equals(revision.getStatus())) {
                revision.setStatus(RevisionStatus.REJECTED_VALUE);
                revision.setRejectReason(reason);
                revision.setReviewerId(operatorId);
                revision.setReviewedAt(LocalDateTime.now());
                revisionDataService.updateStatus(revision);
            }
            draftContent = revision.getPayload();
            draftUpdatedAt = LocalDateTime.now();
        }

        roadmapDataService.ban(roadmapId, draftContent, draftUpdatedAt);
        log.info("Roadmap 封禁: roadmapId={}, operatorId={}, reason={}", roadmapId, operatorId, reason);
    }

    /**
     * 解封：根据是否存在已发布版本恢复到 PUBLISHED 或 NEVER_PUBLISHED。
     */
    @Transactional
    public void restore(long roadmapId) {
        RoadmapDO roadmap = roadmapDataService.validateAndGet(roadmapId);
        if (!NewContentState.BANNED_VALUE.equals(roadmap.getState())) {
            throw StatusCode.INVALID_PARAMETER.exception("仅 BANNED 状态可解封");
        }
        String newState = roadmap.getCurrentRevisionId() != null
            ? NewContentState.PUBLISHED_VALUE
            : NewContentState.NEVER_PUBLISHED_VALUE;
        roadmapDataService.updateState(roadmapId, newState);
        log.info("Roadmap 解封: roadmapId={}, newState={}", roadmapId, newState);
    }

    @Transactional
    public void deleteRoadmap(long id) {
        roadmapDataService.validateExists(id);
        int result = roadmapDataService.softDelete(id);
        if (result == 0) {
            throw StatusCode.ROADMAP_NOT_FOUND.exception();
        }
        log.info("Roadmap 删除: roadmapId={}", id);
    }

    /**
     * 仅更新主表 description（管理员/作者用）。
     */
    @Transactional
    public void updateDescription(long id, String description) {
        RoadmapDO roadmap = roadmapDataService.validateAndGet(id);
        roadmap.setDescription(description != null ? description : "");
        roadmapDataService.update(roadmap);
        log.info("Roadmap 描述更新: roadmapId={}", id);
    }
}
