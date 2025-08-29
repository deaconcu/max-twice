package com.prosper.learn.api.v1.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.common.Enums;
import com.prosper.learn.common.Utils;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.domain.service.PostingService;
import com.prosper.learn.domain.util.Converter;
import com.prosper.learn.dto.*;
import com.prosper.learn.persistence.dataobject.*;
import com.prosper.learn.persistence.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static com.prosper.learn.common.Enums.ObjectType.post;

/**
 * 帖子管理接口
 * 从PostClient和AggregateClient迁移而来
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class PostsController {

    private final PostingService postingService;
    private final PostMapper postMapper;
    private final UpvoteMapper upvoteMapper;
    private final UserMapper userMapper;
    private final NodeMapper nodeMapper;
    private final ObjectMapper objectMapper;

    /**
     * 批量获取帖子
     * 映射: GET /postings?ids=1,2,3 → GET /api/v1/posts?ids=1,2,3
     */
    @GetMapping("/posts")
    public ApiResponse<Object> getPosts(
            @RequestParam(value = "ids", required = false) List<Long> ids,
            @RequestParam(value = "nodeId", required = false) Long nodeId,
            @RequestParam(value = "lastScore", required = false, defaultValue = "0") double lastScore,
            @RequestParam(value = "lastId", required = false, defaultValue = "0") Long lastPostingId) {
        
        List<PostDO> postDOList = null;
        if (ids != null && ids.size() > 0) {
            postDOList = postMapper.getByIds(ids);
        } else if (nodeId != null && nodeId > 0) {
            int count = 2;
            postDOList = postMapper.getListByNodeAndScoreAndPaginated(nodeId, lastScore, lastPostingId, count, Enums.PostState.approved.value());
        }
        
        if (postDOList == null) {
            throw new IllegalArgumentException("不能获取帖子列表");
        }

        List<Long> allPostingIds = new ArrayList<>();
        List<Long> userIds = new LinkedList<>();
        postDOList.forEach(postingDO -> {
            postingService.idToName(postingDO);
            allPostingIds.add(postingDO.getId());
            userIds.add(postingDO.getCreator());
        });

        List<UserDTOV1> userList = userIds.size() == 0 ?
                new ArrayList<>() : Converter.INSTANCE.toUserDTOV1(userMapper.getByIds(userIds));
        Map<Long, UserDTOV1> userMap = new HashMap<>();
        for (UserDTOV1 user : userList) {
            userMap.put(user.getId(), user);
        }

        List<PostDTO> postDTOList = Converter.INSTANCE.toPostDTO(postDOList);
        postDTOList.stream().forEach(item -> {
            item.setCreator(userMap.get(item.getCreatorId()));
        });

        if (allPostingIds.size() > 0) {
            List<UpvoteDO> upvotes = upvoteMapper.getList(StpUtil.getLoginIdAsInt(), allPostingIds, post.value());
            Map<Long, Integer> types = new HashMap<>();
            for (UpvoteDO upvote : upvotes) {
                types.put(upvote.getObjectId(), upvote.getType());
            }

            for (PostDTO posting : postDTOList) {
                if (types.containsKey(posting.getId()))
                    posting.setVoteType(types.get(posting.getId()));
            }
        }

        return ApiResponse.success(postDTOList);
    }

    /**
     * 创建帖子
     * 映射: POST /posting → POST /api/v1/posts
     */
    @PostMapping("/posts")
    public ApiResponse<Object> createPost(@RequestBody PostDTO posting) {
        if (posting.getType() == Enums.PostType.contents.value()) {
            NodeDO nodeDO = nodeMapper.getById(posting.getNodeId());
            List<String> nodeNames;
            try {
                nodeNames = objectMapper.readValue(posting.getContent(), new TypeReference<>() {});
            } catch (JsonProcessingException e) {
                log.error("Failed to process JSON content", e);
                throw ErrorCode.JSON_PARSE_ERROR.exception(e);
            }
            String[] ids = new String[nodeNames.size()];
            for (int i = 0; i < nodeNames.size(); i ++) {
                NodeDO node = new NodeDO();
                node.setName(nodeNames.get(i));
                node.setDescription("");
                node.setRoot(0l);
                node.setCourseId(nodeDO.getCourseId());
                node.setCreatedAt(Utils.getLocalDateTime());
                node.setUpdatedAt(Utils.getLocalDateTime());
                nodeMapper.insert(node);
                ids[i] = Long.toString(node.getId());
            }
            posting.setContent(String.join(",", ids));
        }

        posting.setCreatorId(0l);
        posting.setCreatedAt(Utils.getTimeString());
        postMapper.insert(Converter.INSTANCE.toPostDO(posting));
        return ApiResponse.success();
    }

    /**
     * 修改帖子
     * 映射: PUT /posting → PUT /api/v1/posts/{id}
     */
    @PutMapping("/posts/{id}")
    public ApiResponse<Object> updatePost(@PathVariable Long id, @RequestBody PostDTO posting) {
        PostDO postDO = postMapper.get(id);
        if (postDO == null) {
            throw ErrorCode.SYSTEM_ERROR.exception();
        }

        postDO.setContent(posting.getContent());
        postDO.setUpdatedAt(Utils.getLocalDateTime());
        postMapper.update(postDO);
        return ApiResponse.success();
    }

    /**
     * 删除帖子
     * 映射: DELETE /posting → DELETE /api/v1/posts/{id}
     */
    @DeleteMapping("/posts/{id}")
    public ApiResponse<Object> deletePost(@PathVariable Long id) {
        PostDO postDO = postMapper.get(id);
        if (postDO == null) {
            throw ErrorCode.SYSTEM_ERROR.exception();
        }

        postDO.setState(Enums.PostState.deleted.value());
        postDO.setUpdatedAt(Utils.getLocalDateTime());
        postMapper.update(postDO);
        return ApiResponse.success();
    }

    /**
     * 获取帖子详情
     * 映射: GET /posting/{id} → GET /api/v1/posts/{id}
     */
    @GetMapping("/posts/{id}")
    public ApiResponse<PostDTO> getPost(@PathVariable Long id) {
        return ApiResponse.success(Converter.INSTANCE.toPostDTO(postingService.get(id)));
    }

    /**
     * 获取节点帖子
     * 映射: GET /node/{nodeId}/posting → GET /api/v1/nodes/{nodeId}/posts
     */
    @GetMapping("/nodes/{nodeId}/posts")
    public ApiResponse<List<PostDTO>> getNodePosts(@PathVariable Long nodeId) {
        int count = 3;
        List<PostDO> postings = postMapper.getListByNode(nodeId, count, Enums.PostState.approved.value());
        postings.forEach(postingDO -> postingService.idToName(postingDO));
        return ApiResponse.success(Converter.INSTANCE.toPostDTO(postings));
    }

    /**
     * 获取待审核帖子
     * 映射: GET /post/censor → GET /api/v1/admin/posts/pending
     */
    @GetMapping("/admin/posts/pending")
    public ApiResponse<List<PostDTO>> getPendingPosts() {
        List<PostDO> postDOList = postMapper.getListByState(Enums.PostState.approved.value(), 200);
        for (PostDO postDO : postDOList) {
            if (postDO.getType() == Enums.PostType.contents.value()) {
                postingService.idToName(postDO);
            }
        }
        return ApiResponse.success(Converter.INSTANCE.toPostDTO(postDOList));
    }

    /**
     * 审核帖子
     * 映射: PUT /post → PUT /api/v1/admin/posts/{id}/approve
     */
    @PutMapping("/admin/posts/{id}/approve")
    public ApiResponse<Object> approvePost(@PathVariable Long id, @RequestParam boolean approve) {
        PostDO postDO = postMapper.get(id);
        if (postDO == null) {
            throw ErrorCode.SYSTEM_ERROR.exception();
        }

        if (approve && postDO.getState() != Enums.PostState.approved.value()) {
            postDO.setState(Enums.CommentState.approved.value());
            postMapper.update(postDO);
        }
        if (!approve && postDO.getState() != Enums.CommentState.deleted.value()) {
            postDO.setState(Enums.CommentState.deleted.value());
            postMapper.update(postDO);
        }
        return ApiResponse.success(Converter.INSTANCE.toPostDTO(postDO));
    }
}