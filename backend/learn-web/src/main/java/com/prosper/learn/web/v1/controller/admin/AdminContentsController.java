package com.prosper.learn.web.v1.controller.admin;

import com.prosper.learn.application.dto.request.CreateCourseRequest;
import com.prosper.learn.application.dto.request.CreateNodeRequest;
import com.prosper.learn.application.dto.request.OperateRequest;
import com.prosper.learn.application.dto.request.UpdateCourseRequest;
import com.prosper.learn.application.dto.request.UpdateProfessionRequest;
import com.prosper.learn.application.service.CommentService;
import com.prosper.learn.application.service.CourseService;
import com.prosper.learn.application.service.MemoryCardDeckService;
import com.prosper.learn.application.service.NodeService;
import com.prosper.learn.application.service.PostService;
import com.prosper.learn.application.service.ProfessionService;
import com.prosper.learn.application.service.RoadmapService;
import com.prosper.learn.application.service.StatsService;
import com.prosper.learn.shared.domain.exception.StatusCode;
import com.prosper.learn.user.profile.UserDO;
import com.prosper.learn.web.v1.annotation.CurrentUser;
import com.prosper.learn.web.v1.annotation.JsonParam;
import com.prosper.learn.web.v1.annotation.OperationLog;
import com.prosper.learn.web.v1.annotation.RequireRole;
import com.prosper.learn.application.dto.ApiResponse;
import com.prosper.learn.web.ratelimit.LimitType;
import com.prosper.learn.web.ratelimit.RateLimit;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

import static com.prosper.learn.shared.domain.Enums.*;

/**
 * 内容管理统一后台接口
 * 支持所有内容类型的统一管理：post, roadmap, memory_card_deck, comment, course, profession, node
 *
 * 统一接口：
 * - GET  /contents/{contentType}?state=xxx&lastId=xxx - 按状态分页查询（每页20条）
 * - POST /contents/{contentType}/{id}/operate - 审核操作
 */
@RestController
@RequestMapping("/api/v1/admin/contents")
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
    private final ProfessionService professionService;
    private final NodeService nodeService;
    private final StatsService statsService;

    private static final int DEFAULT_PAGE_SIZE = 20;

    // ==================== 统一查询接口 ====================

    /**
     * 按状态查询内容列表（分页）
     * GET /api/v1/admin/contents/{contentType}?state=xxx&lastId=xxx
     *
     * @param contentType 内容类型: post, roadmap, memory_card_deck, comment, course, profession
     * @param state 状态: pending/submitted, approved/published, rejected, banned
     * @param lastId 分页游标（默认0）
     * @return 内容列表（每页20条）
     */
    @GetMapping("/{contentType}")
    @RequireRole(UserRole.MODERATOR)
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<?> getContentsByState(
            @PathVariable @NotBlank(message = "内容类型不能为空") String contentType,
            @RequestParam(required = false) Byte state,
            @RequestParam(required = false) @Min(value = 0, message = "lastId不能小于0") Long lastId) {

        // 解析状态
        ContentState stateValue = ContentState.getByValue(state);

        return switch (contentType.toLowerCase()) {
            case "post" -> ApiResponse.success(
                postService.listByState(stateValue, lastId, DEFAULT_PAGE_SIZE)
            );
            case "roadmap" -> ApiResponse.success(
                roadmapService.listByState(stateValue, lastId)
            );
            case "memory_card_deck" -> ApiResponse.success(
                memoryCardDeckService.listByState(stateValue, lastId)
            );
            case "comment" -> ApiResponse.success(
                commentService.listByState(stateValue, lastId)
            );
            case "course" -> ApiResponse.success(
                courseService.listByState(stateValue, lastId)
            );
            case "profession" -> ApiResponse.success(
                professionService.listByState(stateValue, lastId, DEFAULT_PAGE_SIZE)
            );
            case "node" -> ApiResponse.success(
                nodeService.listByState(stateValue, lastId)
            );
            default -> throw StatusCode.INVALID_PARAMETER.exception("不支持的内容类型: " + contentType);
        };
    }

    /**
     * 帖子高级筛选
     * GET /api/v1/admin/contents/post/filter?nodeId=xxx&creatorId=xxx&lastId=xxx
     */
    @GetMapping("/post/filter")
    @RequireRole(UserRole.MODERATOR)
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<?> filterPosts(
            @RequestParam(value = "nodeId", required = false) @Positive(message = "节点ID必须大于0") Long nodeId,
            @RequestParam(value = "creatorId", required = false) @Positive(message = "用户ID必须大于0") Long creatorId,
            @RequestParam(value = "lastId", required = false) @Min(value = 0, message = "最后ID不能小于0") Long lastId) {
        return ApiResponse.success(postService.listByFilter(nodeId, creatorId, lastId));
    }

    /**
     * 评论高级筛选
     * GET /api/v1/admin/contents/comment/filter?objectType=xxx&objectId=xxx&creatorId=xxx&lastId=xxx
     */
    @GetMapping("/comment/filter")
    @RequireRole(UserRole.MODERATOR)
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<?> filterComments(
            @RequestParam(value = "objectType", required = false) @Positive(message = "对象类型必须大于0") Integer objectType,
            @RequestParam(value = "objectId", required = false) @Positive(message = "对象ID必须大于0") Long objectId,
            @RequestParam(value = "creatorId", required = false) @Positive(message = "用户ID必须大于0") Long creatorId,
            @RequestParam(value = "lastId", required = false) @Min(value = 0, message = "最后ID不能小于0") Long lastId) {
        return ApiResponse.success(commentService.listByFilter(objectType, objectId, creatorId, lastId));
    }

    /**
     * 路线图高级筛选
     * GET /api/v1/admin/contents/roadmap/filter?roadmapId=xxx&professionId=xxx&creatorId=xxx&lastId=xxx
     */
    @GetMapping("/roadmap/filter")
    @RequireRole(UserRole.MODERATOR)
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<?> filterRoadmaps(
            @RequestParam(required = false) @Positive(message = "路线图ID必须大于0") Long roadmapId,
            @RequestParam(required = false) @Positive(message = "职业ID必须大于0") Long professionId,
            @RequestParam(required = false) @Positive(message = "创建者ID必须大于0") Long creatorId,
            @RequestParam(required = false) Long lastId) {
        return ApiResponse.success(roadmapService.listByFilter(roadmapId, professionId, creatorId, lastId));
    }

    /**
     * 卡片组高级筛选
     * GET /api/v1/admin/contents/memory_card_deck/filter?state=xxx&postId=xxx&creatorId=xxx&lastId=xxx
     */
    @GetMapping("/memory_card_deck/filter")
    @RequireRole(UserRole.MODERATOR)
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<?> filterMemoryCardDecks(
            @RequestParam(required = false) @Positive(message = "状态必须大于0") Byte state,
            @RequestParam(required = false) @Positive(message = "帖子ID必须大于0") Long postId,
            @RequestParam(required = false) @Positive(message = "创建者ID必须大于0") Long creatorId,
            @RequestParam(required = false) Long lastId) {
        return ApiResponse.success(memoryCardDeckService.getDecksForReview(
                postId, creatorId, state != null ? state.intValue() : null, lastId, null));
    }

    /**
     * 节点高级筛选
     * GET /api/v1/admin/contents/node/filter?nodeId=xxx&courseId=xxx&creatorId=xxx&lastId=xxx
     */
    @GetMapping("/node/filter")
    @RequireRole(UserRole.MODERATOR)
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<?> filterNodes(
            @RequestParam(value = "nodeId", required = false) @Positive(message = "节点ID必须大于0") Long nodeId,
            @RequestParam(value = "courseId", required = false) @Positive(message = "课程ID必须大于0") Long courseId,
            @RequestParam(value = "creatorId", required = false) @Positive(message = "创建者ID必须大于0") Long creatorId,
            @RequestParam(value = "lastId", required = false) Long lastId) {
        return ApiResponse.success(nodeService.listByFilter(nodeId, courseId, creatorId, lastId));
    }

    /**
     * 获取课程详情
     * GET /api/v1/admin/contents/course/{id}
     */
    @GetMapping("/course/{id}")
    @RequireRole(UserRole.MODERATOR)
    @RateLimit(capacity = 150, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<?> getCourseDetail(@PathVariable @Positive(message = "课程ID必须大于0") Long id) {
        return ApiResponse.success(courseService.getAdminCourseById(id));
    }

    /**
     * 获取节点详情
     * GET /api/v1/admin/contents/node/{id}
     */
    @GetMapping("/node/{id}")
    @RequireRole(UserRole.MODERATOR)
    @RateLimit(capacity = 150, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<?> getNodeDetail(@PathVariable @Positive(message = "节点ID必须大于0") Long id) {
        return ApiResponse.success(nodeService.getById(id));
    }

    /**
     * 获取子课程列表
     * GET /api/v1/admin/contents/course/{parentId}/subcourses?state=xxx
     */
    @GetMapping("/course/{parentId}/subcourses")
    @RequireRole(UserRole.MODERATOR)
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<?> getSubcourses(
            @PathVariable @Positive(message = "父课程ID必须大于0") Long parentId,
            @RequestParam(required = false) @Positive(message = "状态必须大于0") Integer state) {
        ContentState courseState = state != null ? ContentState.getByValue(state.byteValue()) : null;
        return ApiResponse.success(courseService.getListByParent(parentId, courseState));
    }

    // ==================== 更新接口 ====================

    /**
     * 更新路线图描述
     * PUT /api/v1/admin/contents/roadmap/{id}
     */
    @PutMapping("/roadmap/{id}")
    @RequireRole(UserRole.MODERATOR)
    @RateLimit(capacity = 30, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    @OperationLog(
        module = "用户内容管理",
        type = "更新路线图",
        level = OperationLevel.MEDIUM,
        targetType = "Roadmap",
        targetId = "#id"
    )
    public ApiResponse<?> updateRoadmap(
            @PathVariable @NotNull(message = "路线图ID不能为空")
            @Positive(message = "路线图ID必须大于0") Long id,
            @JsonParam("description") @Size(max = 500, message = "描述长度不能超过500个字符") String description,
            @CurrentUser UserDO currentUser) {
        return ApiResponse.success(roadmapService.updateDescription(id, description, currentUser));
    }

    /**
     * 更新课程信息
     * PUT /api/v1/admin/contents/course/{id}
     */
    @PutMapping("/course/{id}")
    @RequireRole(UserRole.MODERATOR)
    @RateLimit(capacity = 30, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    @OperationLog(
        module = "全局内容管理",
        type = "更新课程",
        level = OperationLevel.MEDIUM,
        targetType = "Course",
        targetId = "#id"
    )
    public ApiResponse<?> updateCourse(
            @PathVariable @NotNull(message = "课程ID不能为空")
            @Positive(message = "课程ID必须大于0") Long id,
            @Valid @RequestBody UpdateCourseRequest request,
            @CurrentUser UserDO currentUser) {
        courseService.updateCourse(id, request, currentUser);
        return ApiResponse.success("更新成功");
    }

    /**
     * 更新职业信息
     * PUT /api/v1/admin/contents/profession/{id}
     */
    @PutMapping("/profession/{id}")
    @RequireRole(UserRole.ADMIN)
    @RateLimit(capacity = 30, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    @OperationLog(
        module = "全局内容管理",
        type = "更新职业",
        level = OperationLevel.MEDIUM,
        targetType = "Profession",
        targetId = "#id"
    )
    public ApiResponse<?> updateProfession(
            @PathVariable @NotNull(message = "职业ID不能为空")
            @Positive(message = "职业ID必须大于0") Long id,
            @Valid @RequestBody UpdateProfessionRequest request,
            @CurrentUser UserDO currentUser) {
        professionService.update(id, request, currentUser);
        return ApiResponse.success();
    }

    /**
     * 获取职业详情（管理后台）
     * GET /api/v1/admin/contents/profession/{id}
     */
    @GetMapping("/profession/{id}")
    @RequireRole(UserRole.MODERATOR)
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<?> getProfessionById(
            @PathVariable @NotNull(message = "职业ID不能为空")
            @Positive(message = "职业ID必须大于0") Long id) {
        return ApiResponse.query(professionService.getAdminById(id));
    }

    /**
     * 解析状态字符串为数值
     */
    private Byte parseState(String state) {
        if (state == null || state.isBlank()) {
            return null;
        }

        return switch (state.toLowerCase()) {
            case "draft" -> ContentState.DRAFT.value();
            case "pending", "submitted" -> ContentState.SUBMITTED.value();
            case "approved", "published" -> ContentState.PUBLISHED.value();
            case "rejected" -> ContentState.REJECTED.value();
            case "banned" -> ContentState.BANNED.value();
            default -> {
                try {
                    yield Byte.parseByte(state);
                } catch (NumberFormatException e) {
                    throw StatusCode.INVALID_PARAMETER.exception("无效的状态值: " + state);
                }
            }
        };
    }

    // ==================== 审核操作接口 ====================

    /**
     * 内容审核操作（统一接口）
     * 支持操作: approve(审核通过), reject(审核拒绝), remove(下架), ban(封禁), restore(恢复), delete(删除)
     *
     * @param contentType 内容类型: post, roadmap, memory_card_deck, comment, course, profession, node
     * @param id 内容ID
     * @param request 操作请求 { action, reason }
     * @param currentUser 当前操作员
     *
     * 注意:
     * - remove 操作仅支持 post, roadmap, memory_card_deck
     * - restore 操作仅支持 post, roadmap, memory_card_deck, course, profession
     * - delete 操作仅支持 course, profession
     * - comment 不发送审核通知
     */
    @PostMapping("/{contentType}/{id}/operate")
    @RequireRole(UserRole.MODERATOR)
    @RateLimit(capacity = 50, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    @OperationLog(
        module = "#contentType == 'course' || #contentType == 'profession' || #contentType == 'node' ? '全局内容管理' : '用户内容管理'",
        type = "#{" +
               "'APPROVE': '审核通过', " +
               "'REJECT': '审核拒绝', " +
               "'REMOVE': '下架', " +
               "'BAN': '封禁', " +
               "'RESTORE': '恢复', " +
               "'DELETE': '删除'" +
               "}[#request.action] ?: '未知操作'",
        level = OperationLevel.MEDIUM,
        targetType = "#contentType",
        targetId = "#id",
        reason = "#request.reason"
    )
    public ApiResponse<Void> operateContent(
            @PathVariable @NotBlank(message = "内容类型不能为空") String contentType,
            @PathVariable @NotNull(message = "内容ID不能为空")
            @Positive(message = "内容ID必须大于0") Long id,
            @RequestBody @Valid OperateRequest request,
            @CurrentUser UserDO currentUser) {

        String action = request.getAction().toLowerCase();
        String reason = request.getReason();

        // 验证操作的合法性
        validateOperation(contentType, action);

        switch (contentType.toLowerCase()) {
            case "post" -> operatePost(id, action, reason, currentUser);
            case "roadmap" -> operateRoadmap(id, action, reason, currentUser);
            case "memory_card_deck" -> operateMemoryCardDeck(id, action, reason, currentUser);
            case "comment" -> operateComment(id, action, reason, currentUser);
            case "course" -> operateCourse(id, action, reason, currentUser);
            case "profession" -> operateProfession(id, action, reason, currentUser);
            case "node" -> operateNode(id, action, reason);
            default -> throw StatusCode.INVALID_PARAMETER.exception("不支持的内容类型: " + contentType);
        }

        return ApiResponse.success();
    }

    // ==================== 操作验证 ====================

    private void validateOperation(String contentType, String action) {
        // remove 操作只支持特定内容类型
        if ("remove".equals(action)) {
            if (!"post".equals(contentType) && !"roadmap".equals(contentType) && !"memory_card_deck".equals(contentType)) {
                throw StatusCode.INVALID_PARAMETER.exception(contentType + " 不支持下架操作");
            }
        }

        // delete 操作只支持 course 和 profession
        if ("delete".equals(action)) {
            if (!"course".equals(contentType) && !"profession".equals(contentType)) {
                throw StatusCode.INVALID_PARAMETER.exception(contentType + " 不支持删除操作");
            }
        }

        // restore 验证
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
        // 评论不支持 remove 和 restore 操作
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
            case "restore" -> courseService.approve(id, currentUser);  // restore 等同于 approve
            default -> throw StatusCode.INVALID_PARAMETER.exception("不支持的操作类型: " + action);
        }
    }

    private void operateProfession(Long id, String action, String reason, UserDO currentUser) {
        switch (action) {
            case "approve" -> professionService.approve(id, currentUser);
            case "reject" -> professionService.reject(id, reason, currentUser);
            case "ban" -> professionService.ban(id, reason, currentUser);
            case "delete" -> professionService.delete(id, currentUser);
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
     * POST /api/v1/admin/contents/nodes/init-embeddings?batchSize=20
     *
     * @param batchSize 每批处理数量（默认20）
     * @return 处理结果统计
     */
    @PostMapping("/nodes/init-embeddings")
    @RateLimit(capacity = 5, refillPeriod = 1, refillUnit = TimeUnit.HOURS, limitType = LimitType.GLOBAL)
    @OperationLog(
        module = "系统维护",
        type = "初始化节点Embedding",
        level = OperationLevel.HIGH,
        targetType = "Node",
        targetId = "0"
    )
    public ApiResponse<?> initializeNodeEmbeddings(
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int batchSize,
            @CurrentUser UserDO currentUser) {

        log.info("Starting batch initialization of node embeddings, batchSize: {}", batchSize);

        var result = nodeService.initializeNodeEmbeddings(batchSize);

        log.info("Node embeddings initialization completed: {}", result);

        return ApiResponse.success(result);
    }

    // ==================== 节点引用数重新计算 ====================

    /**
     * 重新计算所有节点的引用数统计
     * POST /api/v1/admin/contents/nodes/recalculate-references
     *
     * @return 处理结果统计
     */
    @PostMapping("/nodes/recalculate-references")
    @RateLimit(capacity = 5, refillPeriod = 1, refillUnit = TimeUnit.HOURS, limitType = LimitType.GLOBAL)
    @OperationLog(
        module = "系统维护",
        type = "重算节点引用数",
        level = OperationLevel.HIGH,
        targetType = "Node",
        targetId = "0"
    )
    public ApiResponse<?> recalculateNodeReferences() {
        log.info("开始重新计算节点引用数统计");

        var result = statsService.recalculateNodeReferenceCount();

        log.info("节点引用数统计重新计算完成: {}", result);

        return ApiResponse.success(result);
    }

    // ==================== 创建课程和节点（Admin专用） ====================

    /**
     * 创建课程并自动审核通过
     * POST /api/v1/admin/contents/course/create
     */
    @PostMapping("/course/create")
    @OperationLog(
        module = "全局内容管理",
        type = "创建课程",
        level = OperationLevel.MEDIUM,
        targetType = "Course",
        targetId = "#result"
    )
    @RateLimit(capacity = 50, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<Long> createCourse(
            @RequestBody @Valid CreateCourseRequest request,
            @CurrentUser UserDO currentUser) {

        Long courseId = courseService.createAndApprove(request, currentUser);
        return ApiResponse.success(courseId);
    }

    /**
     * 创建节点并自动审核通过
     * POST /api/v1/admin/contents/node/create
     */
    @PostMapping("/node/create")
    @OperationLog(
        module = "全局内容管理",
        type = "创建节点",
        level = OperationLevel.MEDIUM,
        targetType = "Node",
        targetId = "#result"
    )
    @RateLimit(capacity = 50, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<Long> createNode(
            @RequestBody @Valid CreateNodeRequest request,
            @CurrentUser UserDO currentUser) {

        Long nodeId = nodeService.createAndApprove(request, currentUser);
        return ApiResponse.success(nodeId);
    }
}
