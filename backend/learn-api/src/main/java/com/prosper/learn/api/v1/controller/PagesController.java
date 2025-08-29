package com.prosper.learn.api.v1.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.common.Enums;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.domain.service.*;
import com.prosper.learn.domain.util.Converter;
import com.prosper.learn.common.Utils;
import com.prosper.learn.dto.*;
import com.prosper.learn.persistence.dataobject.*;
import com.prosper.learn.persistence.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.stream.Collectors;

import static com.prosper.learn.common.Enums.ObjectType.comment;
import static com.prosper.learn.common.Enums.ObjectType.post;

/**
 * 页面聚合接口
 * 从AggregateClient拆分出的页面聚合功能
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PagesController {

    private static final String API_KEY = "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";
    private static final String API_URL = "https://openrouter.ai/api/v1/chat/completions";

    private final ContentsService contentsService;
    private final CourseMapper courseMapper;
    private final ObjectMapper objectMapper;
    private final PostingService postingService;
    private final PostMapper postMapper;
    private final UpvoteMapper upvoteMapper;
    private final NodeMapper nodeMapper;
    private final UpvoteService upvoteService;
    private final CourseService courseService;
    private final UserMapper userMapper;
    private final CommentMapper commentMapper;
    private final UserCourseMapper userCourseMapper;
    private final UserProfileMapper userProfileMapper;
    private final LearningProgressService learningProgressService;
    private final AggregateService aggregateService;

    /**
     * 根据不同参数读取页面数据
     * 映射: GET /read?courseId=123&path=xxx → GET /api/v1/pages/read?courseId=123&path=xxx
     * 映射: GET /read?nodeId=123 → GET /api/v1/pages/read?nodeId=123
     * 映射: GET /read?postId=123 → GET /api/v1/pages/read?postId=123
     * 映射: GET /read?commentId=123 → GET /api/v1/pages/read?commentId=123
     */
    @GetMapping("/pages/read")
    public ApiResponse<Object> read(
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) String path,
            @RequestParam(required = false) Long nodeId,
            @RequestParam(required = false) Long postId,
            @RequestParam(required = false) Long commentId) {

        // 参数优先级是 commentId > postId > nodeId > courseId + path
        if (commentId != null) {
            return readByComment(commentId);
        } else if (postId != null) {
            return readByPost(postId);
        } else if (nodeId != null) {
            return readByNode(nodeId);
        } else if (courseId != null) {
            return readByPath(courseId, path);
        } else {
            throw new IllegalArgumentException("至少需要提供一个参数");
        }
    }

    private ApiResponse<Object> readByComment(Long commentId) {
        Map<String, Object> data = new HashMap<>();
        if (commentId <= 0) throw new IllegalArgumentException("评论ID必须大于0");

        CommentDO commentDO = commentMapper.get(commentId);
        if (commentDO == null) throw new IllegalArgumentException("评论不存在");

        if (commentDO.getReplyTo() != 0) {
            data.put("commentId", commentDO.getReplyTo());
            data.put("subCommentId", commentDO.getId());
            return ApiResponse.success(data);
        }

        PostDO postDO = null;
        NodeDO nodeDO = null;
        CourseDO courseDO = null;
        String path = "";

        if (commentDO.getType() == post.value()) {
            long postId = commentDO.getObjectId();
            postDO = postingService.get(postId);
            nodeDO = nodeMapper.getById(postDO.getNodeId());
            courseDO = courseMapper.getById(nodeDO.getCourseId());
            path = "1-" + courseDO.getRootNode();
        } else {
            nodeDO = nodeMapper.getById(commentDO.getObjectId());
            courseDO = courseMapper.getById(nodeDO.getCourseId());
            path = "1-" + courseDO.getRootNode();
        }

        return ApiResponse.success(read(courseDO, path, nodeDO, postDO));
    }

    private ApiResponse<Object> readByPost(Long postId) {
        if (postId <= 0) throw new IllegalArgumentException("帖子ID必须大于0");

        PostDO postDO = postingService.get(postId);
        if (postDO == null) throw new IllegalArgumentException("帖子不存在");

        NodeDO nodeDO = nodeMapper.getById(postDO.getNodeId());
        CourseDO courseDO = courseMapper.getById(nodeDO.getCourseId());
        String path = "1-" + courseDO.getRootNode();

        return ApiResponse.success(read(courseDO, path, nodeDO, postDO));
    }

    private ApiResponse<Object> readByNode(Long nodeId) {
        NodeDO nodeDO = nodeMapper.getById(nodeId);
        CourseDO courseDO = courseMapper.getById(nodeDO.getCourseId());
        String path = "1-" + courseDO.getRootNode();

        return ResponseEntity.ok(ApiResponse.success(read(courseDO, path, nodeDO, null)));
    }

    private ApiResponse<Object> readByPath(Long courseId, String path) {
        CourseDO courseDO = courseMapper.getById(courseId);
        return ResponseEntity.ok(ApiResponse.success(read(courseDO, path, null, null)));
    }

    private Object read(CourseDO courseDO, String path, NodeDO nodeDO, PostDO postDO) {
        long userId = StpUtil.getLoginIdAsLong();
        Utils.Pair<String, Map<Long, NodeDTOV2>> response = aggregateService.getToc(userId, courseDO.getId(), true);
        CourseTocDTO courseTocDTO = new CourseTocDTO(response.left(), response.right());

        List<Object> contents;
        List<PostDTO> fixedPostings = null;
        PostDTO chosenPosting = null;
        List<Long> fixedIds = null;
        List<Long> userIds = new LinkedList<>();

        JsonNode rootNode;

        try {
            contents = objectMapper.readValue(courseTocDTO.getContents(), List.class);
            rootNode = objectMapper.readTree(courseTocDTO.getContents());
        } catch (JsonProcessingException e) {
            throw ErrorCode.SYSTEM_ERROR.exception(e);
        }

        validatePath(path);
        if (path == null || path.equals("") || !path.contains("-")) {
            path = "1-" + courseDO.getRootNode();
        }

        Utils.Pair<Integer, JsonNode> pair = Utils.getNodeByPath(rootNode, path);

        if (pair == null) {
            path = "1-" + courseDO.getRootNode();
            pair = Utils.getNodeByPath(rootNode, path);
        }

        if (nodeDO == null) {
            int nodeId = pair.left();
            nodeDO = nodeMapper.getById(nodeId);

            JsonNode currentNode = pair.right();
            if (currentNode != null && currentNode.has("+")) {
                PostDO chosenPostDO = postingService.get(currentNode.get("+").asInt());
                userIds.add(chosenPostDO.getCreator());
                chosenPosting = Converter.INSTANCE.toPostDTO(chosenPostDO);
            }

            if (currentNode != null && currentNode.has("^")) {
                ArrayNode idsNode = (ArrayNode) currentNode.get("^");
                fixedIds = objectMapper.convertValue(idsNode, List.class);

                List<PostDO> postDOList = postingService.getList(fixedIds);
                for (PostDO postingDTO : postDOList) {
                    userIds.add(postingDTO.getCreator());
                }
                fixedPostings = Converter.INSTANCE.toPostDTO(postDOList);
            }
        }

        List<PostDO> postDOList = postingService.getList(nodeDO.getId());
        for (PostDO item: postDOList) {
            userIds.add(item.getCreator());
        }
        List<PostDTO> otherPostings = Converter.INSTANCE.toPostDTO(postDOList);

        List<UserDTOV1> userList = userIds.size() == 0 ? new ArrayList<>() : Converter.INSTANCE.toUserDTOV1(userMapper.getByIds(userIds));
        Map<Long, UserDTOV1> userMap = new HashMap<>();
        for (UserDTOV1 user : userList) {
            userMap.put(user.getId(), user);
        }

        PostDTO postDTO = null;
        if (postDO != null) {
            postDTO = Converter.INSTANCE.toPostDTO(postDO);
            postDTO.setCreator(userMap.get(postDTO.getCreatorId()));
        }

        List<Long> allPostingIds = new ArrayList<>();
        if (chosenPosting != null) {
            allPostingIds.add(chosenPosting.getId());
            chosenPosting.setCreator(userMap.get(chosenPosting.getCreatorId()));
        }
        if (fixedPostings != null) fixedPostings.stream().forEach(item -> {
            allPostingIds.add(item.getId());
            item.setCreator(userMap.get(item.getCreatorId()));
        });
        if (otherPostings != null) otherPostings.stream().forEach(item -> {
            allPostingIds.add(item.getId());
            item.setCreator(userMap.get(item.getCreatorId()));
        });

        if (allPostingIds.size() > 0) {
            List<UpvoteDO> upvotes = upvoteMapper.getList(userId, allPostingIds, post.value());
            Map<Long, Integer> types = new HashMap<>();
            for (UpvoteDO upvote : upvotes) {
                types.put(upvote.getObjectId(), upvote.getType());
            }

            if (chosenPosting != null && types.containsKey(chosenPosting.getId()))
                chosenPosting.setVoteType(types.get(chosenPosting.getId()));

            if (fixedPostings != null) {
                for (PostDTO posting : fixedPostings) {
                    if (types.containsKey(posting.getId()))
                        posting.setVoteType(types.get(posting.getId()));
                }
            }

            if (otherPostings != null) {
                for (PostDTO posting : otherPostings) {
                    if (types.containsKey(posting.getId()))
                        posting.setVoteType(types.get(posting.getId()));
                }
            }
        }

        long lastId = -1;
        if (allPostingIds.size() > 0) {
            lastId = allPostingIds.get(allPostingIds.size() - 1);
        }

        UserCourseDO userCourseDo = userCourseMapper.getByUserIdAndCourseId((long)userId, (long)courseDO.getId());
        boolean learning = userCourseDo != null ?  true : false;

        CourseDTOV4 parentCourse = null;
        if (courseDO.getParent() != 0) {
            CourseDO parentCourseDO = courseMapper.getById(courseDO.getParent());
            parentCourse = Converter.INSTANCE.toCourseDTOV4(parentCourseDO, false);
        } else {
            parentCourse = Converter.INSTANCE.toCourseDTOV4(courseDO, false);
        }

        boolean subscribed = false;
        UserProfileDO userProfileDO = userProfileMapper.getById(userId);
        if (userProfileDO != null && userProfileDO.getSubscription() != null && !userProfileDO.getSubscription().trim().isEmpty()) {
            List<Integer> subscriptionIds = Arrays.stream(userProfileDO.getSubscription().split(","))
                    .filter(s -> !s.trim().isEmpty())
                    .map(String::trim)
                    .mapToInt(Integer::parseInt)
                    .boxed()
                    .collect(Collectors.toList());
            subscribed = subscriptionIds.contains(parentCourse.getId());
        }

        parentCourse.setSubscribed(subscribed);

        List<CourseDTOV2> subCourseList = Converter.INSTANCE.toCourseDTOV2(
                courseMapper.listByParentAndState("APPROVED", parentCourse.getId()));

        boolean nodeCompleted = learningProgressService.isNodeCompleted(userId, nodeDO.getId());

        UserCourseDO userCourse = userCourseMapper.getByUserIdAndCourseId((long)userId, (long)courseDO.getId());
        Integer courseProgress = userCourse != null ? userCourse.getProgressPercent() : 0;

        Map<String, Object> data = new HashMap<>();
        data.put("node", Converter.INSTANCE.toNodeDTOV2(nodeDO, nodeCompleted));
        data.put("parentCourse", parentCourse);
        data.put("course", Converter.INSTANCE.toCourseDTOV4(courseDO, subscribed, courseProgress));
        data.put("subCourseList", subCourseList);
        data.put("chosenPosting", chosenPosting);
        data.put("fixedPostings", fixedPostings);
        data.put("otherPostings", otherPostings);
        data.put("lastId", lastId);
        data.put("toc", contents);
        data.put("tocNodeInfos", courseTocDTO.getNodeInfos());
        data.put("path", path);
        data.put("users", userList);
        data.put("learning", learning);
        if (postDTO != null) data.put("post", postDTO);
        return data;
    }

    private void validatePath(String path) {
        // todo path 必须是 xx-yy-zz 的格式，中间必须是数字
    }
}