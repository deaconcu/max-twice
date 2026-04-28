package com.twicemax.web.v2.controller.admin;

import com.twicemax.application.dto.request.CreateCourseRequest;
import com.twicemax.application.dto.request.CreateNodeRequest;
import com.twicemax.application.dto.request.OperateRequest;
import com.twicemax.application.dto.request.UpdateCourseRequest;
import com.twicemax.application.dto.request.UpdateRoleRequest;
import com.twicemax.application.service.CommentService;
import com.twicemax.application.service.CourseService;
import com.twicemax.application.service.MemoryCardDeckService;
import com.twicemax.application.service.NodeService;
import com.twicemax.application.service.PostService;
import com.twicemax.application.service.RoleService;
import com.twicemax.application.service.RoadmapService;
import com.twicemax.application.service.StatsService;
import com.twicemax.shared.domain.exception.StatusCode;
import com.twicemax.user.profile.UserDO;
import com.twicemax.web.v2.annotation.CurrentUser;
import com.twicemax.web.v2.annotation.JsonParam;
import com.twicemax.web.v2.annotation.OperationLog;
import com.twicemax.web.v2.annotation.RequireRole;
import com.twicemax.web.ratelimit.LimitType;
import com.twicemax.web.ratelimit.RateLimit;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

import static com.twicemax.shared.domain.Enums.*;

/**
 * 内容管理统一后台接口
 * 支持所有内容类型的统一管理：post, roadmap, memory_card_deck, comment, course, role, node
 */
@RestController
@RequestMapping("/admin/contents")
@RequiredArgsConstructor
@Slf4j
@RequireRole(UserRole.ADMIN)
@Validated
public class AdminContentsController {

    private final PostService postService;
    private final RoadmapService roadmapService;
    private final MemoryCardDeckService memoryCardDeckService;
    private final CommentService commentService;
    private final CourseService courseService;
    private final RoleService roleService;
    private final NodeService nodeService;
    private final StatsService statsService;

    private static final int DEFAULT_PAGE_SIZE = 20;

    // ==================== 统一查询接口 ====================

    /**
     * 按状态查询内容列表（分页）
     * GET /api/v2/admin/contents/{contentType}?state=xxx&lastId=xxx
     */
    @GetMapping("/{contentType}")
    @RequireRole(UserRole.MODERATOR)
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public Object getContentsByState(
            @PathVariable @NotBlank(message = "内容类型不能为空") String contentType,
            @RequestParam(required = false) Byte state,
            @RequestParam(required = false) @Min(value = 0, message = "lastId不能小于0") Long lastId) {

        ContentState stateValue = ContentState.getByValue(state);

        return switch (contentType.toLowerCase()) {
            case "post" -> postService.listByState(stateValue, lastId, DEFAULT_PAGE_SIZE);
            case "roadmap" -> roadmapService.listByState(stateValue, lastId);
            case "memory_card_deck" -> memoryCardDeckService.listByState(stateValue, lastId);
            case "comment" -> commentService.listByState(stateValue, lastId);
            case "course" -> courseService.listByState(stateValue, lastId);
            case "role" -> roleService.listByState(stateValue, lastId, DEFAULT_PAGE_SIZE);
            case "node" -> nodeService.listByState(stateValue, lastId);
            default -> throw StatusCode.INVALID_PARAMETER.exception("不支持的内容类型: " + contentType);
        };
    }

    /**
     * 帖子高级筛选
     * GET /api/v2/admin/contents/post/filter
     */
    @GetMapping("/post/filter")
    @RequireRole(UserRole.MODERATOR)
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public Object filterPosts(
            @RequestParam(value = "nodeId", required = false) @Positive(message = "节点ID必须大于0") Long nodeId,
            @RequestParam(value = "creatorId", required = false) @Positive(message = "用户ID必须大于0") Long creatorId,
            @RequestParam(value = "lastId", required = false) @Min(value = 0, message = "最后ID不能小于0") Long lastId) {
        return postService.listByFilter(nodeId, creatorId, lastId);
    }

    /**
     * 评论高级筛选
     * GET /api/v2/admin/contents/comment/filter
     */
    @GetMapping("/comment/filter")
    @RequireRole(UserRole.MODERATOR)
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public Object filterComments(
            @RequestParam(value = "objectType", required = false) @Positive(message = "对象类型必须大于0") Integer objectType,
            @RequestParam(value = "objectId", required = false) @Positive(message = "对象ID必须大于0") Long objectId,
            @RequestParam(value = "creatorId", required = false) @Positive(message = "用户ID必须大于0") Long creatorId,
            @RequestParam(value = "lastId", required = false) @Min(value = 0, message = "最后ID不能小于0") Long lastId) {
        return commentService.listByFilter(objectType, objectId, creatorId, lastId);
    }

    /**
     * 路线图高级筛选
     * GET /api/v2/admin/contents/roadmap/filter
     */
    @GetMapping("/roadmap/filter")
    @RequireRole(UserRole.MODERATOR)
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public Object filterRoadmaps(
            @RequestParam(required = false) @Positive(message = "路线图ID必须大于0") Long roadmapId,
            @RequestParam(required = false) @Positive(message = "角色ID必须大于0") Long roleId,
            @RequestParam(required = false) @Positive(message = "创建者ID必须大于0") Long creatorId,
            @RequestParam(required = false) Long lastId) {
        return roadmapService.listByFilter(roadmapId, roleId, creatorId, lastId);
    }

    /**
     * 卡片组高级筛选
     * GET /api/v2/admin/contents/memory_card_deck/filter
     */
    @GetMapping("/memory_card_deck/filter")
    @RequireRole(UserRole.MODERATOR)
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public Object filterMemoryCardDecks(
            @RequestParam(required = false) @Positive(message = "状态必须大于0") Byte state,
            @RequestParam(required = false) @Positive(message = "帖子ID必须大于0") Long postId,
            @RequestParam(required = false) @Positive(message = "创建者ID必须大于0") Long creatorId,
            @RequestParam(required = false) Long lastId,
            @CurrentUser UserDO currentUser) {
        return memoryCardDeckService.getDecksForReview(
                postId, creatorId, state != null ? state.intValue() : null, lastId, currentUser);
    }

    /**
     * 节点高级筛选
     * GET /api/v2/admin/contents/node/filter
     */
    @GetMapping("/node/filter")
    @RequireRole(UserRole.MODERATOR)
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public Object filterNodes(
            @RequestParam(value = "nodeId", required = false) @Positive(message = "节点ID必须大于0") Long nodeId,
            @RequestParam(value = "courseId", required = false) @Positive(message = "课程ID必须大于0") Long courseId,
            @RequestParam(value = "creatorId", required = false) @Positive(message = "创建者ID必须大于0") Long creatorId,
            @RequestParam(value = "lastId", required = false) Long lastId) {
        return nodeService.listByFilter(nodeId, courseId, creatorId, lastId);
    }

    /**
     * 获取课程详情
     * GET /api/v2/admin/contents/course/{id}
     */
    @GetMapping("/course/{id}")
    @RequireRole(UserRole.MODERATOR)
    @RateLimit(capacity = 150, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public Object getCourseDetail(@PathVariable @Positive(message = "课程ID必须大于0") Long id) {
        return courseService.getAdminCourseById(id);
    }

    /**
     * 获取节点详情
     * GET /api/v2/admin/contents/node/{id}
     */
    @GetMapping("/node/{id}")
    @RequireRole(UserRole.MODERATOR)
    @RateLimit(capacity = 150, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public Object getNodeDetail(@PathVariable @Positive(message = "节点ID必须大于0") Long id) {
        return nodeService.getById(id);
    }

    /**
     * 获取子课程列表
     * GET /api/v2/admin/contents/course/{parentId}/subcourses
     */
    @GetMapping("/course/{parentId}/subcourses")
    @RequireRole(UserRole.MODERATOR)
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public Object getSubcourses(
            @PathVariable @Positive(message = "父课程ID必须大于0") Long parentId,
            @RequestParam(required = false) @Positive(message = "状态必须大于0") Integer state) {
        ContentState courseState = state != null ? ContentState.getByValue(state.byteValue()) : null;
        return courseService.getListByParent(parentId, courseState);
    }

    // ==================== 更新接口 ====================

    /**
     * 更新路线图描述
     * PUT /api/v2/admin/contents/roadmap/{id}
     */
    @PutMapping("/roadmap/{id}")
    @RequireRole(UserRole.MODERATOR)
    @RateLimit(capacity = 30, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    @Transactional
    @OperationLog(
        module = "用户内容管理",
        type = "更新路线图",
        level = OperationLevel.MEDIUM,
        targetType = "Roadmap",
        targetId = "#id"
    )
    public Object updateRoadmap(
            @PathVariable @NotNull(message = "路线图ID不能为空")
            @Positive(message = "路线图ID必须大于0") Long id,
            @JsonParam("description") @Size(max = 500, message = "描述长度不能超过500个字符") String description,
            @CurrentUser UserDO currentUser) {
        return roadmapService.updateDescription(id, description, currentUser);
    }

    /**
     * 更新课程信息
     * PUT /api/v2/admin/contents/course/{id}
     */
    @PutMapping("/course/{id}")
    @RequireRole(UserRole.MODERATOR)
    @RateLimit(capacity = 30, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    @Transactional
    @OperationLog(
        module = "全局内容管理",
        type = "更新课程",
        level = OperationLevel.MEDIUM,
        targetType = "Course",
        targetId = "#id"
    )
    public ResponseEntity<Void> updateCourse(
            @PathVariable @NotNull(message = "课程ID不能为空")
            @Positive(message = "课程ID必须大于0") Long id,
            @Valid @RequestBody UpdateCourseRequest request,
            @CurrentUser UserDO currentUser) {
        courseService.updateCourse(id, request, currentUser);
        return ResponseEntity.noContent().build();
    }

    /**
     * 按名称搜索课程（管理后台）
     * GET /api/v2/admin/contents/course/search
     */
    @GetMapping("/course/search")
    @RequireRole(UserRole.MODERATOR)
    @RateLimit(capacity = 60, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public Object searchCoursesByName(
            @RequestParam @NotBlank(message = "搜索名称不能为空") String name,
            @RequestParam(required = false) Long lastId) {
        return courseService.searchCoursesByName(name, lastId);
    }

    /**
     * 按名称搜索角色（管理后台）
     * GET /api/v2/admin/contents/role/search
     */
    @GetMapping("/role/search")
    @RequireRole(UserRole.MODERATOR)
    @RateLimit(capacity = 60, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public Object searchRolesByName(
            @RequestParam @NotBlank(message = "搜索名称不能为空") String name,
            @RequestParam(required = false) Long lastId) {
        return roleService.searchByName(name, lastId);
    }

    /**
     * 更新角色信息
     * PUT /api/v2/admin/contents/role/{id}
     */
    @PutMapping("/role/{id}")
    @RequireRole(UserRole.ADMIN)
    @RateLimit(capacity = 30, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    @Transactional
    @OperationLog(
        module = "全局内容管理",
        type = "更新角色",
        level = OperationLevel.MEDIUM,
        targetType = "Role",
        targetId = "#id"
    )
    public ResponseEntity<Void> updateRole(
            @PathVariable @NotNull(message = "角色ID不能为空")
            @Positive(message = "角色ID必须大于0") Long id,
            @Valid @RequestBody UpdateRoleRequest request,
            @CurrentUser UserDO currentUser) {
        roleService.update(id, request, currentUser);
        return ResponseEntity.noContent().build();
    }

    /**
     * 获取角色详情（管理后台）
     * GET /api/v2/admin/contents/role/{id}
     */
    @GetMapping("/role/{id}")
    @RequireRole(UserRole.MODERATOR)
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public Object getRoleById(
            @PathVariable @NotNull(message = "角色ID不能为空")
            @Positive(message = "角色ID必须大于0") Long id) {
        return roleService.getAdminById(id);
    }

    // ==================== 审核操作接口 ====================

    /**
     * 内容审核操作（统一接口）
     * POST /api/v2/admin/contents/{contentType}/{id}/operate
     */
    @PostMapping("/{contentType}/{id}/operate")
    @RequireRole(UserRole.MODERATOR)
    @RateLimit(capacity = 50, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    @Transactional
    @OperationLog(
        module = "#contentType == 'course' || #contentType == 'role' || #contentType == 'node' ? '全局内容管理' : '用户内容管理'",
        type = "#request.action.toUpperCase() == 'APPROVE' ? '审核通过' : " +
               "#request.action.toUpperCase() == 'REJECT' ? '审核拒绝' : " +
               "#request.action.toUpperCase() == 'REMOVE' ? '下架' : " +
               "#request.action.toUpperCase() == 'BAN' ? '封禁' : " +
               "#request.action.toUpperCase() == 'RESTORE' ? '恢复' : " +
               "#request.action.toUpperCase() == 'DELETE' ? '删除' : #request.action",
        level = OperationLevel.MEDIUM,
        targetType = "#contentType",
        targetId = "#id",
        reason = "#request.reason"
    )
    public ResponseEntity<Void> operateContent(
            @PathVariable @NotBlank(message = "内容类型不能为空") String contentType,
            @PathVariable @NotNull(message = "内容ID不能为空")
            @Positive(message = "内容ID必须大于0") Long id,
            @RequestBody @Valid OperateRequest request,
            @CurrentUser UserDO currentUser) {

        String action = request.getAction().toLowerCase();
        String reason = request.getReason();

        validateOperation(contentType, action);

        switch (contentType.toLowerCase()) {
            case "post" -> operatePost(id, action, reason, currentUser);
            case "roadmap" -> operateRoadmap(id, action, reason, currentUser);
            case "memory_card_deck" -> operateMemoryCardDeck(id, action, reason, currentUser);
            case "comment" -> operateComment(id, action, reason, currentUser);
            case "course" -> operateCourse(id, action, reason, currentUser);
            case "role" -> operateRole(id, action, reason, currentUser);
            case "node" -> operateNode(id, action, reason);
            default -> throw StatusCode.INVALID_PARAMETER.exception("不支持的内容类型: " + contentType);
        }

        return ResponseEntity.noContent().build();
    }

    // ==================== 操作验证 ====================

    private void validateOperation(String contentType, String action) {
        if ("remove".equals(action)) {
            if (!"post".equals(contentType) && !"roadmap".equals(contentType) && !"memory_card_deck".equals(contentType)) {
                throw StatusCode.INVALID_PARAMETER.exception(contentType + " 不支持下架操作");
            }
        }

        if ("delete".equals(action)) {
            if (!"course".equals(contentType) && !"role".equals(contentType)) {
                throw StatusCode.INVALID_PARAMETER.exception(contentType + " 不支持删除操作");
            }
        }

        if ("restore".equals(action)) {
            if ("comment".equals(contentType)) {
                throw StatusCode.INVALID_PARAMETER.exception("评论不支持恢复操作");
            }
        }
    }

    // ==================== 内容类型具体操作 ====================

    private void operatePost(Long id, String action, String reason, UserDO currentUser) {
        switch (action) {
            case "approve" -> postService.approve(id, currentUser);
            case "reject" -> postService.reject(id, reason, currentUser);
            case "remove" -> postService.remove(id, reason, currentUser);
            case "ban" -> postService.ban(id, reason, currentUser);
            case "restore" -> postService.restore(id, reason, currentUser);
            default -> throw StatusCode.INVALID_PARAMETER.exception("不支持的操作类型: " + action);
        }
    }

    private void operateRoadmap(Long id, String action, String reason, UserDO currentUser) {
        switch (action) {
            case "approve" -> roadmapService.approve(id, currentUser);
            case "reject" -> roadmapService.reject(id, reason, currentUser);
            case "remove" -> roadmapService.remove(id, reason, currentUser);
            case "ban" -> roadmapService.ban(id, reason, currentUser);
            case "restore" -> roadmapService.restore(id, reason, currentUser);
            default -> throw StatusCode.INVALID_PARAMETER.exception("不支持的操作类型: " + action);
        }
    }

    private void operateMemoryCardDeck(Long id, String action, String reason, UserDO currentUser) {
        switch (action) {
            case "approve" -> memoryCardDeckService.approve(id, currentUser.getId());
            case "reject" -> memoryCardDeckService.reject(id, currentUser.getId(), reason);
            case "remove" -> memoryCardDeckService.remove(id, currentUser.getId(), reason);
            case "ban" -> memoryCardDeckService.ban(id, currentUser.getId(), reason);
            case "restore" -> memoryCardDeckService.restoreDeck(id, currentUser.getId(), reason);
            default -> throw StatusCode.INVALID_PARAMETER.exception("不支持的操作类型: " + action);
        }
    }

    private void operateComment(Long id, String action, String reason, UserDO currentUser) {
        switch (action) {
            case "approve" -> commentService.approve(id, currentUser);
            case "reject" -> commentService.reject(id, reason, currentUser);
            case "ban" -> commentService.ban(id, reason, currentUser);
            case "restore" -> throw StatusCode.INVALID_PARAMETER.exception("评论不支持恢复操作");
            default -> throw StatusCode.INVALID_PARAMETER.exception("不支持的操作类型: " + action);
        }
    }

    private void operateCourse(Long id, String action, String reason, UserDO currentUser) {
        switch (action) {
            case "approve" -> courseService.approve(id, currentUser);
            case "reject" -> courseService.reject(id, reason, currentUser);
            case "ban" -> courseService.ban(id, reason, currentUser);
            case "delete" -> courseService.delete(id, currentUser);
            case "restore" -> courseService.approve(id, currentUser);
            default -> throw StatusCode.INVALID_PARAMETER.exception("不支持的操作类型: " + action);
        }
    }

    private void operateRole(Long id, String action, String reason, UserDO currentUser) {
        switch (action) {
            case "approve" -> roleService.approve(id, currentUser);
            case "reject" -> roleService.reject(id, reason, currentUser);
            case "ban" -> roleService.ban(id, reason, currentUser);
            case "delete" -> roleService.delete(id, currentUser);
            default -> throw StatusCode.INVALID_PARAMETER.exception("不支持的操作类型: " + action);
        }
    }

    private void operateNode(Long id, String action, String reason) {
        switch (action) {
            case "approve" -> nodeService.approve(id);
            case "reject" -> nodeService.reject(id, reason);
            case "ban" -> nodeService.ban(id, reason);
            case "restore" -> nodeService.restore(id, reason);
            default -> throw StatusCode.INVALID_PARAMETER.exception("不支持的操作类型: " + action);
        }
    }

    // ==================== 节点Embedding初始化 ====================

    /**
     * 批量初始化现有节点的embedding
     * POST /api/v2/admin/contents/nodes/init-embeddings
     */
    @PostMapping("/nodes/init-embeddings")
    @RateLimit(capacity = 5, refillPeriod = 1, refillUnit = TimeUnit.HOURS, limitType = LimitType.GLOBAL)
    @Transactional
    @OperationLog(
        module = "系统维护",
        type = "初始化节点Embedding",
        level = OperationLevel.HIGH,
        targetType = "Node",
        targetId = "0"
    )
    public Object initializeNodeEmbeddings(
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int batchSize,
            @CurrentUser UserDO currentUser) {

        log.info("管理后台 开始批量初始化节点向量，batchSize: {}", batchSize);
        var result = nodeService.initializeNodeEmbeddings(batchSize);
        log.info("管理后台 节点向量初始化完成: {}", result);
        return result;
    }

    // ==================== 节点引用数重新计算 ====================

    /**
     * 重新计算所有节点的引用数统计
     * POST /api/v2/admin/contents/nodes/recalculate-references
     */
    @PostMapping("/nodes/recalculate-references")
    @RateLimit(capacity = 5, refillPeriod = 1, refillUnit = TimeUnit.HOURS, limitType = LimitType.GLOBAL)
    @Transactional
    @OperationLog(
        module = "系统维护",
        type = "重算节点引用数",
        level = OperationLevel.HIGH,
        targetType = "Node",
        targetId = "0"
    )
    public Object recalculateNodeReferences() {
        log.info("开始重新计算节点引用数统计");
        var result = statsService.recalculateNodeReferenceCount();
        log.info("节点引用数统计重新计算完成: {}", result);
        return result;
    }

    // ==================== 创建课程和节点（Admin专用） ====================

    /**
     * 创建课程并自动审核通过
     * POST /api/v2/admin/contents/course/create
     */
    @PostMapping("/course/create")
    @Transactional
    @OperationLog(
        module = "全局内容管理",
        type = "创建课程",
        level = OperationLevel.MEDIUM,
        targetType = "Course",
        targetId = "#result"
    )
    @RateLimit(capacity = 50, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public Long createCourse(
            @RequestBody @Valid CreateCourseRequest request,
            @CurrentUser UserDO currentUser) {
        return courseService.createAndApprove(request, currentUser);
    }

    /**
     * 创建节点并自动审核通过
     * POST /api/v2/admin/contents/node/create
     */
    @PostMapping("/node/create")
    @Transactional
    @OperationLog(
        module = "全局内容管理",
        type = "创建节点",
        level = OperationLevel.MEDIUM,
        targetType = "Node",
        targetId = "#result"
    )
    @RateLimit(capacity = 50, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public Long createNode(
            @RequestBody @Valid CreateNodeRequest request,
            @CurrentUser UserDO currentUser) {
        return nodeService.createAndApprove(request, currentUser);
    }
}
