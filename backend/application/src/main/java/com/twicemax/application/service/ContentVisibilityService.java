package com.twicemax.application.service;

import com.twicemax.content.course.CourseDO;
import com.twicemax.content.course.CourseDataService;
import com.twicemax.content.node.NodeDO;
import com.twicemax.content.node.NodeDataService;
import com.twicemax.content.post.PostDO;
import com.twicemax.content.post.PostDataService;
import com.twicemax.content.roadmap.RoadmapDO;
import com.twicemax.content.roadmap.RoadmapDataService;
import com.twicemax.content.role.RoleDO;
import com.twicemax.content.role.RoleDataService;
import com.twicemax.interaction.comment.CommentDO;
import com.twicemax.interaction.comment.CommentDataService;
import com.twicemax.memory.deck.MemoryCardDeckDO;
import com.twicemax.memory.deck.MemoryCardDeckDataService;
import com.twicemax.shared.domain.Enums.ContentState;
import com.twicemax.shared.domain.Enums.ContentType;
import com.twicemax.shared.domain.Enums.NewContentState;
import com.twicemax.shared.domain.exception.StatusCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 内容可见性服务
 *
 * 负责检查内容及其祖先链的可见性，以及创建子内容的权限。
 *
 * <h2>内容关系链</h2>
 * <pre>
 * 课程 (Course) —— 最多两级
 * ├── 子课程 (Course)
 * └── 节点 (Node)
 *     └── 帖子 (Post)
 *
 * 角色 (Role)
 * └── 路线图 (Roadmap)
 *
 * 评论 (Comment) —— 可附加到：
 * ├── 节点 (Node)
 * ├── 帖子 (Post)
 * ├── 路线图 (Roadmap)
 * └── 评论 (Comment)
 *
 * 记忆卡片组 (MemoryCardDeck)
 * ├── 必须关联：节点 (Node)
 * └── 可选关联：帖子 (Post)
 * </pre>
 *
 * <h2>可见性规则</h2>
 * <ul>
 *   <li>自己的内容：state != BANNED 可见</li>
 *   <li>别人的内容：state == PUBLISHED 可见</li>
 *   <li>链上任意一个不可见 → 整体不可见</li>
 * </ul>
 *
 * <h2>创建规则</h2>
 * <ul>
 *   <li>祖先链必须全部为 PUBLISHED 状态（不区分自己/别人）</li>
 * </ul>
 *
 * <h2>入口检查（需要检查完整祖先链）</h2>
 * <ul>
 *   <li>课程详情：课程 → (父课程)</li>
 *   <li>节点详情：节点 → 课程 → (父课程)</li>
 *   <li>帖子详情：帖子 → 节点 → 课程 → (父课程)</li>
 *   <li>路线图详情：路线图 → 角色</li>
 *   <li>评论上下文：评论 → 所属对象的祖先链</li>
 *   <li>记忆卡片组详情：卡片组 → 节点 → 课程 → (父课程)，(帖子)</li>
 * </ul>
 *
 * <h2>列表查询（不检查祖先链）</h2>
 * 所有列表只过滤内容自身状态，不检查祖先链。
 * 极少数情况下，用户可能在列表中看到内容，点进去后因祖先不可见返回 403，这是可接受的。
 *
 * @see docs/内容状态与权限控制.md
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentVisibilityService {

    private final CourseDataService courseDataService;
    private final NodeDataService nodeDataService;
    private final PostDataService postDataService;
    private final RoleDataService roleDataService;
    private final RoadmapDataService roadmapDataService;
    private final CommentDataService commentDataService;
    private final MemoryCardDeckDataService memoryCardDeckDataService;

    // ==================== 可见性检查（详情页入口） ====================

    /**
     * 检查内容及其祖先链是否可见（不抛异常）
     *
     * @param type 内容类型
     * @param id 内容ID
     * @param currentUserId 当前用户ID
     * @return true 可见，false 不可见
     */
    public boolean isVisible(ContentType type, Long id, Long currentUserId) {
        try {
            validateVisibility(type, id, currentUserId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查内容及其祖先链是否可见
     * 不可见则抛出 CONTENT_NOT_VISIBLE 异常
     *
     * @param type 内容类型
     * @param id 内容ID
     * @param currentUserId 当前用户ID
     */
    public void validateVisibility(ContentType type, Long id, Long currentUserId) {
        switch (type) {
            case course -> validateCourseVisibility(id, currentUserId);
            case node -> validateNodeVisibility(id, currentUserId);
            case post -> validatePostVisibility(id, currentUserId);
            case role -> validateRoleVisibility(id, currentUserId);
            case roadmap -> validateRoadmapVisibility(id, currentUserId);
            case comment -> validateCommentVisibility(id, currentUserId);
            case memory_card_deck -> validateMemoryCardDeckVisibility(id, currentUserId);
            default -> throw StatusCode.INVALID_PARAMETER.exception("不支持的内容类型: " + type);
        }
    }

    // ==================== 创建权限检查 ====================

    /**
     * 检查是否可以在父内容上创建子内容
     * 祖先链必须全部为 PUBLISHED 状态
     * 不满足则抛出 INVALID_PARAMETER 异常
     *
     * @param parentType 父内容类型
     * @param parentId 父内容ID
     */
    public void validateCanCreateOn(ContentType parentType, Long parentId) {
        switch (parentType) {
            case course -> validateCourseChainPublished(parentId);
            case node -> validateNodeChainPublished(parentId);
            case post -> validatePostChainPublished(parentId);
            case role -> validateRolePublished(parentId);
            case roadmap -> validateRoadmapChainPublished(parentId);
            case comment -> validateCommentChainPublished(parentId);
            default -> throw StatusCode.INVALID_PARAMETER.exception("不支持在此类型上创建内容: " + parentType);
        }
    }

    // ==================== 私有方法：可见性检查 ====================

    /**
     * 检查课程可见性（课程 → 父课程）
     */
    private void validateCourseVisibility(Long courseId, Long currentUserId) {
        CourseDO course = courseDataService.getById(courseId);
        if (course == null) {
            throw StatusCode.CONTENT_NOT_VISIBLE.exception();
        }

        // 检查课程本身（course 已迁到 NewContentState 字符串枚举）
        checkVisibleByNewState(course.getState(), course.getCreatorId(), currentUserId);

        // 检查父课程
        if (course.getParentCourseId() != null && course.getParentCourseId() > 0) {
            CourseDO parentCourse = courseDataService.getById(course.getParentCourseId());
            if (parentCourse == null) {
                throw StatusCode.CONTENT_NOT_VISIBLE.exception();
            }
            checkVisibleByNewState(parentCourse.getState(), parentCourse.getCreatorId(), currentUserId);
        }
    }

    /**
     * 检查节点可见性（节点 → 课程 → 父课程）
     */
    private void validateNodeVisibility(Long nodeId, Long currentUserId) {
        NodeDO node = nodeDataService.getById(nodeId);
        if (node == null) {
            throw StatusCode.CONTENT_NOT_VISIBLE.exception();
        }

        // 检查节点本身
        checkContentVisible(node.getState(), node.getCreatorId(), currentUserId);

        // 检查课程链
        validateCourseVisibility(node.getCourseId(), currentUserId);
    }

    /**
     * 检查帖子可见性（帖子 → 节点 → 课程 → 父课程）
     */
    private void validatePostVisibility(Long postId, Long currentUserId) {
        PostDO post = postDataService.getById(postId);
        if (post == null) {
            throw StatusCode.CONTENT_NOT_VISIBLE.exception();
        }

        // 检查帖子本身
        checkContentVisible(post.getState(), post.getCreatorId(), currentUserId);

        // 检查节点链
        validateNodeVisibility(post.getNodeId(), currentUserId);
    }

    /**
     * 检查角色可见性
     */
    private void validateRoleVisibility(Long roleId, Long currentUserId) {
        RoleDO role = roleDataService.getById(roleId);
        if (role == null) {
            throw StatusCode.CONTENT_NOT_VISIBLE.exception();
        }

        // 检查角色本身（role 已迁到 NewContentState 字符串枚举）
        checkVisibleByNewState(role.getState(), role.getCreatorId(), currentUserId);
    }

    /**
     * 检查路线图可见性（路线图 → 角色）
     */
    private void validateRoadmapVisibility(Long roadmapId, Long currentUserId) {
        RoadmapDO roadmap = roadmapDataService.getById(roadmapId);
        if (roadmap == null) {
            throw StatusCode.CONTENT_NOT_VISIBLE.exception();
        }

        // 检查路线图本身（roadmap 已迁到 NewContentState 字符串枚举）
        checkVisibleByNewState(roadmap.getState(), roadmap.getCreatorId(), currentUserId);

        // 检查角色
        validateRoleVisibility(roadmap.getRoleId(), currentUserId);
    }

    /**
     * 基于 NewContentState 的通用可见性判定（roadmap / role 等已迁到字符串枚举的内容）。
     * - 自己的内容：BANNED 不可见
     * - 别人的内容：仅 PUBLISHED 可见
     */
    private void checkVisibleByNewState(String state, Long creatorId, Long currentUserId) {
        boolean isOwner = creatorId != null && creatorId.equals(currentUserId);
        if (isOwner) {
            if (NewContentState.BANNED_VALUE.equals(state)) {
                throw StatusCode.CONTENT_NOT_VISIBLE.exception();
            }
        } else {
            if (!NewContentState.PUBLISHED_VALUE.equals(state)) {
                throw StatusCode.CONTENT_NOT_VISIBLE.exception();
            }
        }
    }

    /**
     * 检查评论可见性（评论 → 所属对象的祖先链）
     */
    private void validateCommentVisibility(Long commentId, Long currentUserId) {
        CommentDO comment = commentDataService.getById(commentId);
        if (comment == null) {
            throw StatusCode.CONTENT_NOT_VISIBLE.exception();
        }

        // 检查评论本身
        checkContentVisible(comment.getState(), comment.getCreatorId(), currentUserId);

        // 检查父内容
        validateParentContentVisibility(comment.getObjectType(), comment.getObjectId(), currentUserId);
    }

    /**
     * 检查记忆卡片组可见性（卡片组 → 节点 → 课程 → 父课程，可选：帖子）
     */
    private void validateMemoryCardDeckVisibility(Long deckId, Long currentUserId) {
        MemoryCardDeckDO deck = memoryCardDeckDataService.getById(deckId);
        if (deck == null) {
            throw StatusCode.CONTENT_NOT_VISIBLE.exception();
        }

        // 检查卡片组本身
        checkContentVisible(deck.getState(), deck.getCreatorId(), currentUserId);

        // 检查节点链
        validateNodeVisibility(deck.getNodeId(), currentUserId);

        // 如果有来源帖子，检查帖子可见性
        if (deck.getPostId() != null && deck.getPostId() > 0) {
            PostDO post = postDataService.getById(deck.getPostId());
            if (post != null) {
                checkContentVisible(post.getState(), post.getCreatorId(), currentUserId);
            }
        }
    }

    /**
     * 检查父内容可见性（递归）
     */
    private void validateParentContentVisibility(Integer objectType, Long objectId, Long currentUserId) {
        ContentType type = ContentType.getByValue(objectType);
        switch (type) {
            case post -> validatePostVisibility(objectId, currentUserId);
            case node -> validateNodeVisibility(objectId, currentUserId);
            case roadmap -> validateRoadmapVisibility(objectId, currentUserId);
            case comment -> validateCommentVisibility(objectId, currentUserId);
            default -> throw StatusCode.INVALID_PARAMETER.exception("不支持的父内容类型: " + type);
        }
    }

    /**
     * 检查单个内容是否可见
     * - 自己的内容：state != BANNED 可见
     * - 别人的内容：state == PUBLISHED 可见
     */
    private void checkContentVisible(Byte state, Long creatorId, Long currentUserId) {
        boolean isOwner = creatorId != null && creatorId.equals(currentUserId);

        if (isOwner) {
            // 自己的内容：BANNED 不可见
            if (state == ContentState.BANNED.value()) {
                throw StatusCode.CONTENT_NOT_VISIBLE.exception();
            }
        } else {
            // 别人的内容：只有 PUBLISHED 可见
            if (state != ContentState.PUBLISHED.value()) {
                throw StatusCode.CONTENT_NOT_VISIBLE.exception();
            }
        }
    }

    // ==================== 私有方法：创建权限检查 ====================

    /**
     * 检查课程链是否全部 PUBLISHED（用于创建子内容）
     */
    private void validateCourseChainPublished(Long courseId) {
        CourseDO course = courseDataService.getById(courseId);
        if (course == null) {
            throw StatusCode.INVALID_PARAMETER.exception("课程不存在");
        }

        if (!NewContentState.PUBLISHED_VALUE.equals(course.getState())) {
            throw StatusCode.INVALID_PARAMETER.exception("无法对未发布的内容进行操作");
        }

        // 检查父课程
        if (course.getParentCourseId() != null && course.getParentCourseId() > 0) {
            CourseDO parentCourse = courseDataService.getById(course.getParentCourseId());
            if (parentCourse == null || !NewContentState.PUBLISHED_VALUE.equals(parentCourse.getState())) {
                throw StatusCode.INVALID_PARAMETER.exception("无法对未发布的内容进行操作");
            }
        }
    }

    /**
     * 检查节点链是否全部 PUBLISHED
     */
    private void validateNodeChainPublished(Long nodeId) {
        NodeDO node = nodeDataService.getById(nodeId);
        if (node == null) {
            throw StatusCode.INVALID_PARAMETER.exception("节点不存在");
        }

        if (node.getState() != ContentState.PUBLISHED.value()) {
            throw StatusCode.INVALID_PARAMETER.exception("无法对未发布的内容进行操作");
        }

        // 检查课程链
        validateCourseChainPublished(node.getCourseId());
    }

    /**
     * 检查帖子链是否全部 PUBLISHED
     */
    private void validatePostChainPublished(Long postId) {
        PostDO post = postDataService.getById(postId);
        if (post == null) {
            throw StatusCode.INVALID_PARAMETER.exception("帖子不存在");
        }

        if (post.getState() != ContentState.PUBLISHED.value()) {
            throw StatusCode.INVALID_PARAMETER.exception("无法对未发布的内容进行操作");
        }

        // 检查节点链
        validateNodeChainPublished(post.getNodeId());
    }

    /**
     * 检查角色是否 PUBLISHED
     */
    private void validateRolePublished(Long roleId) {
        RoleDO role = roleDataService.getById(roleId);
        if (role == null) {
            throw StatusCode.INVALID_PARAMETER.exception("角色不存在");
        }

        if (!NewContentState.PUBLISHED_VALUE.equals(role.getState())) {
            throw StatusCode.INVALID_PARAMETER.exception("无法对未发布的内容进行操作");
        }
    }

    /**
     * 检查路线图链是否全部 PUBLISHED
     */
    private void validateRoadmapChainPublished(Long roadmapId) {
        RoadmapDO roadmap = roadmapDataService.getById(roadmapId);
        if (roadmap == null) {
            throw StatusCode.INVALID_PARAMETER.exception("路线图不存在");
        }

        if (!NewContentState.PUBLISHED_VALUE.equals(roadmap.getState())) {
            throw StatusCode.INVALID_PARAMETER.exception("无法对未发布的内容进行操作");
        }

        // 检查角色
        validateRolePublished(roadmap.getRoleId());
    }

    /**
     * 检查评论链是否全部 PUBLISHED
     */
    private void validateCommentChainPublished(Long commentId) {
        CommentDO comment = commentDataService.getById(commentId);
        if (comment == null) {
            throw StatusCode.INVALID_PARAMETER.exception("评论不存在");
        }

        if (comment.getState() != ContentState.PUBLISHED.value()) {
            throw StatusCode.INVALID_PARAMETER.exception("无法对未发布的内容进行操作");
        }

        // 检查父内容链
        validateParentChainPublished(comment.getObjectType(), comment.getObjectId());
    }

    /**
     * 检查父内容链是否全部 PUBLISHED（递归）
     */
    private void validateParentChainPublished(Integer objectType, Long objectId) {
        ContentType type = ContentType.getByValue(objectType);
        switch (type) {
            case post -> validatePostChainPublished(objectId);
            case node -> validateNodeChainPublished(objectId);
            case roadmap -> validateRoadmapChainPublished(objectId);
            case comment -> validateCommentChainPublished(objectId);
            default -> throw StatusCode.INVALID_PARAMETER.exception("不支持的父内容类型: " + type);
        }
    }
}
