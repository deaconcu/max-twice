package com.prosper.learn.api.web;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.prosper.learn.api.client.AggregateClient;
import com.prosper.learn.common.Enums;
import com.prosper.learn.domain.service.basic.ContentsService;
import com.prosper.learn.domain.service.basic.MessageService;
import com.prosper.learn.domain.service.business.*;
import com.prosper.learn.domain.util.Converter;
import com.prosper.learn.common.Utils;
import com.prosper.learn.dto.*;
import com.prosper.learn.dto.message.MessageDTO;
import com.prosper.learn.persistence.dataobject.*;
import com.prosper.learn.persistence.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.stream.Collectors;

import static com.prosper.learn.common.Enums.ObjectType.comment;
import static com.prosper.learn.common.Enums.ObjectType.post;

//@RestController
//@SaCheckLogin
@RequiredArgsConstructor
public class AggregateController implements AggregateClient {

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
    private final MessageService messageService;
    private final UserMapper userMapper;
    private final CommentMapper commentMapper;
    private final UserCourseMapper userCourseMapper;
    private final UserProfileMapper userProfileMapper;
    private final LearningProgressService learningProgressService;
    private final PageService pageService;

    @Override
    public Response<Object> readByComment(Long commentId) {
        Map<String, Object> data = new HashMap<>();
        if (commentId <= 0) throw new IllegalArgumentException("评论ID必须大于0");

        CommentDO commentDO = commentMapper.get(commentId);
        if (commentDO == null) throw new IllegalArgumentException("评论不存在");

        if (commentDO.getReplyTo() != 0) {
            data.put("commentId", commentDO.getReplyTo());
            data.put("subCommentId", commentDO.getId());
            return new Response<>(data);
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

        return read(courseDO, path, nodeDO, postDO);
    }

    @Override
    public Response<Object> readByPost(Long postId) {
        if (postId <= 0) throw new IllegalArgumentException("帖子ID必须大于0");

        PostDO postDO = postingService.get(postId);
        if (postDO == null) throw new IllegalArgumentException("帖子不存在");

        NodeDO nodeDO = nodeMapper.getById(postDO.getNodeId());
        CourseDO courseDO = courseMapper.getById(nodeDO.getCourseId());
        String path = "1-" + courseDO.getRootNode();

        return read(courseDO, path, nodeDO, postDO);
    }

    @Override
    public Response<Object> readByNode(Long nodeId) {
        NodeDO nodeDO = nodeMapper.getById(nodeId);
        CourseDO courseDO = courseMapper.getById(nodeDO.getCourseId());
        String path = "1-" + courseDO.getRootNode();

        return read(courseDO, path, nodeDO, null);
    }

    @Override
    public Response<Object> readByPath(Long courseId, String path) {
        CourseDO courseDO = courseMapper.getById(courseId);
        return read(courseDO, path, null, null);
    }

    /**
     * 参数优先级是 postId > nodeId > courseId + path > courseId
     * 1. 给了 postId，自动计算 nodeId，courseId，path 为根节点
     * 2. 给了 nodeId, 自动计算 courseId，path 为根节点
     * 3. 给了 courseId 和 path，正常返回
     * 4. 只给了 courseId，path 为根节点
     */
    private Response<Object> read(CourseDO courseDO, String path, NodeDO nodeDO, PostDO postDO) {

        long userId = StpUtil.getLoginIdAsLong();
        Utils.Pair<String, Map<Long, NodeDTOV2>> response = pageService.getToc(userId, courseDO.getId(), true);
        CourseTocDTO courseTocDTO = new CourseTocDTO(response.left(), response.right());

        //Map<String, Object> contents;
        List<Object> contents;
        List<PostDTO> fixedPostings = null;
        PostDTO chosenPosting = null;
        List<Long> fixedIds = null;
        List<Long> userIds = new LinkedList<>();

        try {
            contents = objectMapper.readValue(courseTocDTO.getContents(), List.class);
            JsonNode rootNode = objectMapper.readTree(courseTocDTO.getContents());

            validatePath(path);
            // path 不合法时设置默认路径
            if (path == null || path.equals("") || !path.contains("-")) {
                path = "1-" + courseDO.getRootNode();
            }

            Utils.Pair<Integer, JsonNode> pair = Utils.getNodeByPath(rootNode, path);

            // 目录不存在
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
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        List<PostDO> postDOList = postingService.getList(nodeDO.getId());
        for (PostDO item: postDOList) {
            userIds.add(item.getCreator());
        }
        List<PostDTO> otherPostings = Converter.INSTANCE.toPostDTO(postDOList);

        // get all users
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
            parentCourse = Converter.INSTANCE.toCourseDTOV4(parentCourseDO, false); // 先设置为false，后面会更新
        } else {
            parentCourse = Converter.INSTANCE.toCourseDTOV4(courseDO, false); // 先设置为false，后面会更新
        }

        // 检查父课程收藏状态
        boolean subscribed = false;
        try {
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
        } catch (Exception e) {
            subscribed = false;
        }

        // 更新父课程的订阅状态
        parentCourse.setSubscribed(subscribed);

        List<CourseDTOV2> subCourseList = Converter.INSTANCE.toCourseDTOV2(
                courseMapper.listByParentAndState("APPROVED", parentCourse.getId()));

        // 检查节点完成状态并创建DTO
        boolean nodeCompleted = learningProgressService.isNodeCompleted(userId, nodeDO.getId());

        // 获取课程进度
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
        return new Response<>(data);
    }

    private void validatePath(String path) {
        // todo path 必须是 xx-yy-zz 的格式，中间必须是数字

    }

    @Override
    public Response<Object> getPostings(@RequestParam(value = "ids", required = false) List<Long> ids,
                                        @RequestParam(value = "nodeId", required = false) Long nodeId,
                                        @RequestParam(value = "lastScore", required = false) double lastScore,
                                        @RequestParam(value = "lastId", required = false) Long lastPostingId) {
        List<PostDO> postDOList = null;
        if (ids != null && ids.size() > 0) {
            postDOList = postMapper.getByIds(ids);
        } else if (nodeId > 0) {
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

        // get all users
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

            if (postDOList != null) {
                for (PostDTO posting : postDTOList) {
                    if (types.containsKey(posting.getId()))
                        posting.setVoteType(types.get(posting.getId()));
                }
            }
        }

        return new Response<>(postDTOList);
    }

    @Override
    public Response<Object> upvote(Long objectId, int objectType, int type) {
        if (objectType == post.value()) {
            long postId = objectId;
            long userId = StpUtil.getLoginIdAsLong();
            upvoteService.upvotePost(postId, userId, type);

            PostDTO postDTO = Converter.INSTANCE.toPostDTO(postMapper.get(postId));
            UpvoteDO upvoteDO = upvoteMapper.get(userId, postId, post.value());
            if (upvoteDO != null) {
                postDTO.setVoteType(upvoteDO.getType());
            }
            return new Response<>(postDTO);
        } else if (objectType == comment.value()) {
            long commentId = objectId;
            long userId = StpUtil.getLoginIdAsLong();
            upvoteService.upvoteComment(commentId, userId);

            CommentDO commentDO = commentMapper.get(commentId);
            CommentDTO commentDTO = Converter.INSTANCE.toCommentDTO(commentDO);
            UpvoteDO upvoteDO = upvoteMapper.get(userId, commentId, comment.value());
            if (upvoteDO != null) {
                commentDTO.setUpvoted(1);
            }
            return new Response<>(commentDTO);
        } else {
            throw new IllegalArgumentException("不支持的对象类型");
        }
    }

    @Override
    public Response<Object> postContents(@RequestParam("path") String path,
                                         @RequestParam("courseId") Long courseId,
                                         @RequestParam("postingId") Long postingId,
                                         @RequestParam("action") int action,
                                         Model model) {
        int userId = StpUtil.getLoginIdAsInt();
        switch (action) {
            case 1:
                contentsService.choose(userId, path, courseId, postingId);
                break;
            case 2:
                contentsService.unchoose(userId, courseId, path);
                break;
            case 3:
                contentsService.pin(userId, courseId, path, postingId, true);
                break;
            case 4:
                contentsService.pin(userId, courseId, path, postingId, false);
                break;
        }
        return Response.success;
    }

    @Override
    public Response<Object> chatWithGPT(String prompt, String model) throws Exception {
        // 构建 JSON 请求体
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", model);

        // 构造 messages 数组
        ArrayNode messages = objectMapper.createArrayNode();

        ObjectNode systemMessage = objectMapper.createObjectNode();
        systemMessage.put("role", "system");
        systemMessage.put("content", "你是一个老师，能把复杂的问题用生动的方式讲的很容易让人理解");
        messages.add(systemMessage);

        ObjectNode userMessage = objectMapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messages.add(userMessage);

        requestBody.set("messages", messages);
        requestBody.put("temperature", 0.7);

        String jsonRequest = objectMapper.writeValueAsString(requestBody);

        // 创建 HTTP 请求
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Authorization", "Bearer " + API_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                .build();

        // 发送请求
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // 使用 Jackson 解析响应
        JsonNode root = objectMapper.readTree(response.body());
        // 从返回结果中取第一个 choice 的 message.content
        String answer = root.path("choices").get(0).path("message").path("content").asText();

        return new Response<>(answer);
    }


    @Override
    public Response applyCourse(String title, String summary, String explanation, Long parentId) {
        int userId = StpUtil.getLoginIdAsInt();
        CourseDO course = null;
        if (parentId != 0) {
            course = courseMapper.getById(parentId);
            if (course == null) {
                throw new RuntimeException("course not found");
            }
        }

        try {
            Map<String, String> data = new HashMap<>();
            data.put("title", title);
            data.put("summary", summary);
            data.put("explanation", explanation);
            data.put("parentId", Long.toString(parentId));
            if (course != null) {
                data.put("parentName", course.getName());
            }

            ObjectMapper objectMapper = new ObjectMapper();
            String jsonString = objectMapper.writeValueAsString(data);

            messageService.create(jsonString, userId, 0, Enums.MessageType.applyCourse);
            return Response.success;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public Response<Map<String, Object>> getApplCourseList(int page, int length) {
        if (page < 1) page = 1;
        if (length < 1) length = 1;
        if (length > 100) length = 100;
        int count = messageService.getApplyCourseCount();
        int totalPage = count / length + 1;
        if (page > totalPage) page = totalPage;

        Map<String, Object> resultMap = new HashMap<>();
        List<MessageDTO> messageDTOList = messageService.getApplyCourseMessage(page, length);
        resultMap.put("messages", messageDTOList);


        Map<String, Integer> pagination = new HashMap<>();
        pagination.put("total", count);
        pagination.put("pageSize", length);
        pagination.put("currentPage", page);
        pagination.put("totalPages", totalPage);
        resultMap.put("pagination", pagination);
        return new Response<>(resultMap);
    }

    @Override
    public Response<List<MessageDTO>> getMessageList(Long userId, int type, Long lastId, int conversation) {
        int self = StpUtil.getLoginIdAsInt();
        List<MessageDTO> messageDTOList = messageService.getList(type, self, userId, lastId, conversation);
        return new Response<>(messageDTOList);
    }

    @Override
    public Response postSystemMessage(int type, Long userId, String content) {
        //messageService.createSystemMessage(type, userId, content);
        return Response.success;
    }

    @Override
    public Response modifyCourseApply(Long id, String reply) {
        messageService.modifyCourseApply(id, reply);
        return Response.success;
    }
}
