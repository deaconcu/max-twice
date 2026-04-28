package com.twicemax.web.v2.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.twicemax.application.dto.request.CreatePostRequest;
import com.twicemax.application.dto.request.UpdatePostRequest;
import com.twicemax.application.dto.response.node.NodeSearchResultDTO;
import com.twicemax.application.dto.response.post.PostFullDTO;
import com.twicemax.application.dto.response.post.PostSummaryDTO;
import com.twicemax.application.dto.response.post.PostWithVoteDTO;
import com.twicemax.application.dto.v2.CursorPage;
import com.twicemax.application.service.NodeService;
import com.twicemax.application.service.PostService;
import com.twicemax.shared.domain.Enums;
import com.twicemax.shared.domain.exception.StatusCode;
import com.twicemax.user.profile.UserDO;
import com.twicemax.web.ratelimit.LimitType;
import com.twicemax.web.ratelimit.RateLimit;
import com.twicemax.web.v2.annotation.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 帖子管理接口
 */
@RestController
@RequestMapping("/")
@RequiredArgsConstructor
@Validated
public class PostsController {

    private final PostService postService;
    private final NodeService nodeService;

    /**
     * 按 IDs 批量获取帖子
     */
    @GetMapping("/posts/batch")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public List<PostWithVoteDTO> getPostsByIds(
            @RequestParam List<Long> ids,
            @CurrentUser UserDO currentUser) {
        return postService.getPostsByIds(ids, currentUser.getId());
    }

    /**
     * 按节点分页获取帖子
     */
    @GetMapping("/nodes/{nodeId}/posts")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public CursorPage<PostWithVoteDTO> getNodePosts(
            @PathVariable @NotNull(message = "节点ID必须大于0") @Positive Long nodeId,
            @RequestParam(required = false) String cursor,
            @CurrentUser UserDO currentUser) {
        return postService.getNodePostsPage(nodeId, cursor, currentUser.getId());
    }

    @PostMapping("/posts")
    @SaCheckLogin
    @RateLimit(capacity = 30, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ResponseEntity<PostSummaryDTO> createPost(
            @Valid @RequestBody CreatePostRequest request,
            @CurrentUser UserDO currentUser) {
        PostSummaryDTO postDTO = postService.createPostAndReturn(currentUser, request);
        return ResponseEntity.status(201).body(postDTO);
    }

    @PutMapping("/posts/{id}")
    @SaCheckLogin
    @RateLimit(capacity = 30, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public PostSummaryDTO updatePost(
            @PathVariable @NotNull(message = "帖子ID不能为空") @Positive(message = "帖子ID必须大于0") Long id,
            @Valid @RequestBody UpdatePostRequest request,
            @CurrentUser UserDO currentUser) {
        return postService.updatePostAndReturn(id, request, currentUser);
    }

    @DeleteMapping("/posts/{id}")
    @SaCheckLogin
    @RateLimit(capacity = 30, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ResponseEntity<Void> deletePost(
            @PathVariable @NotNull(message = "帖子ID不能为空") @Positive(message = "帖子ID必须大于0") Long id,
            @CurrentUser UserDO currentUser) {
        postService.deletePost(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/posts/{id}")
    @SaCheckLogin
    @RateLimit(capacity = 150, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public PostSummaryDTO getPost(
            @PathVariable @NotNull(message = "帖子ID不能为空") @Positive(message = "帖子ID必须大于0") Long id,
            @CurrentUser UserDO currentUser) {
        return postService.getDTO(id, currentUser.getId());
    }

    @GetMapping("/users/{userId}/posts")
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public CursorPage<PostFullDTO> getUserPosts(
            @PathVariable @NotNull(message = "用户ID不能为空") @Positive(message = "用户ID必须大于0") Long userId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false, defaultValue = "2") Integer type) {

        Enums.PostType postType = Enums.PostType.getByValue(type);
        if (postType == null) {
            throw StatusCode.INVALID_PARAMETER.exception("无效的帖子类型");
        }
        return postService.getUserPostsWithPagination(userId, cursor, postType, Enums.ContentState.PUBLISHED.value());
    }

    @GetMapping("/users/me/posts")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public CursorPage<PostFullDTO> getCurrentUserAllPosts(
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false, defaultValue = "2") Integer type,
            @RequestParam(required = false) Integer state,
            @CurrentUser UserDO currentUser) {

        Enums.PostType postType = Enums.PostType.getByValue(type);
        if (postType == null) {
            throw StatusCode.INVALID_PARAMETER.exception("无效的帖子类型");
        }

        Byte stateValue = null;
        if (state != null) {
            if (state.byteValue() == Enums.ContentState.BANNED.value()) {
                throw StatusCode.INVALID_PARAMETER.exception("无效的状态参数");
            }
            stateValue = state.byteValue();
        }

        return postService.getUserPostsWithPagination(currentUser.getId(), cursor, postType, stateValue);
    }

    @GetMapping("/nodes/search")
    @SaCheckLogin
    @RateLimit(capacity = 50, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public List<NodeSearchResultDTO> searchSimilarNodes(
            @RequestParam @NotBlank(message = "查询文本不能为空") String query,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int topK,
            @RequestParam(defaultValue = "0.2") @DecimalMin("0.0") @DecimalMax("1.0") double threshold) {
        return nodeService.searchSimilarNodes(query, topK, threshold);
    }

    @GetMapping("/nodes/check-duplicate")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public boolean checkDuplicateNode(
            @RequestParam @NotNull(message = "课程ID不能为空") @Positive(message = "课程ID必须大于0") Long courseId,
            @RequestParam @NotBlank(message = "节点名称不能为空") String name) {
        return nodeService.checkDuplicateNode(courseId, name);
    }
}
