package com.prosper.learn.web.v1.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prosper.learn.content.course.CourseDO;
import com.prosper.learn.content.course.CourseDataService;
import com.prosper.learn.content.node.NodeDO;
import com.prosper.learn.content.node.NodeDataService;
import com.prosper.learn.content.post.PostDO;
import com.prosper.learn.content.post.PostDataService;
import com.prosper.learn.interaction.comment.CommentDO;
import com.prosper.learn.interaction.comment.CommentDataService;
import com.prosper.learn.interaction.upvote.UpvoteDO;
import com.prosper.learn.interaction.upvote.UpvoteDataService;
import com.prosper.learn.shared.domain.Enums.*;
import com.prosper.learn.shared.domain.exception.StatusCode;
import com.prosper.learn.user.profile.UserDO;
import com.prosper.learn.user.profile.UserDataService;
import com.prosper.learn.user.profile.UserDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 评论管理接口测试
 * 测试文档: docs/test/comment.md
 *
 * Command 测试 - 写操作
 * Query 测试 - 读操作
 */
@Transactional
public class CommentsControllerTest extends BaseControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CommentDataService commentDataService;

    @Autowired
    private UserDomainService userDomainService;

    @Autowired
    private UserDataService userDataService;

    @Autowired
    private CourseDataService courseDataService;

    @Autowired
    private NodeDataService nodeDataService;

    @Autowired
    private PostDataService postDataService;

    @Autowired
    private UpvoteDataService upvoteDataService;

    @BeforeEach
    void setUp() {
        // 初始化 MockMvc
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    // ==================== 测试辅助方法 ====================

    /**
     * 创建测试用户
     */
    private UserDO createUser(String email) {
        return userDomainService.createUser(email, "password123");
    }

    /**
     * 创建已发布课程
     */
    private CourseDO createPublishedCourse(String name, Long creatorId) {
        CourseDO course = new CourseDO();
        course.setName(name);
        course.setDescription("课程描述");
        course.setCreatorId(creatorId);
        course.setState(ContentState.PUBLISHED.value());
        course.setMainCategory(1);
        course.setSubCategory(1);
        course.setRootNodeId(0L);
        course.setParentCourseId(0L);
        courseDataService.insert(course);

        // 创建根节点
        NodeDO rootNode = NodeDO.createRoot(creatorId, course.getId());
        nodeDataService.insert(rootNode);

        // 更新课程的 rootNodeId
        course.setRootNodeId(rootNode.getId());
        courseDataService.update(course);

        return course;
    }

    /**
     * 创建已发布节点
     */
    private NodeDO createPublishedNode(String name, Long courseId, Long creatorId) {
        NodeDO node = new NodeDO();
        node.setName(name);
        node.setDescription("节点描述");
        node.setCourseId(courseId);
        node.setCreatorId(creatorId);
        node.setState(ContentState.PUBLISHED.value());
        nodeDataService.insert(node);
        return node;
    }

    /**
     * 创建已发布帖子
     */
    private PostDO createPublishedPost(String content, Long nodeId, Long creatorId) {
        PostDO post = new PostDO();
        post.setContent(content);
        post.setNodeId(nodeId);
        post.setCreatorId(creatorId);
        post.setType(PostType.article.value());
        post.setState(ContentState.PUBLISHED.value());
        post.setScore(0.0);
        postDataService.insert(post);
        return post;
    }

    /**
     * 创建已发布的顶级评论
     */
    private CommentDO createApprovedComment(String content, Long objectId, Integer objectType, Long creatorId) {
        CommentDO comment = new CommentDO();
        comment.setContent(content);
        comment.setObjectId(objectId);
        comment.setObjectType(objectType);
        comment.setCreatorId(creatorId);
        comment.setReplyToCommentId(0L);  // 顶级评论使用0，不是null
        comment.setToUserId(0L);  // 顶级评论使用0，不是null
        comment.setState(ContentState.PUBLISHED.value());
        comment.setScore(0.0);
        commentDataService.insert(comment);
        return comment;
    }

    /**
     * 创建已发布的回复评论
     */
    private CommentDO createApprovedReply(String content, Long objectId, Integer objectType,
                                          Long creatorId, Long replyToCommentId, Long toUserId) {
        CommentDO comment = new CommentDO();
        comment.setContent(content);
        comment.setObjectId(objectId);
        comment.setObjectType(objectType);
        comment.setCreatorId(creatorId);
        comment.setReplyToCommentId(replyToCommentId);
        comment.setToUserId(toUserId);
        comment.setState(ContentState.PUBLISHED.value());
        comment.setScore(0.0);
        commentDataService.insert(comment);
        return comment;
    }

    /**
     * 创建评论点赞记录
     */
    private void createCommentUpvote(Long userId, Long commentId) {
        UpvoteDO upvote = new UpvoteDO();
        upvote.setObjectId(commentId);
        upvote.setObjectType(ContentType.comment.value());
        upvote.setUserId(userId);
        upvote.setType(VoteType.like.value());  // 点赞
        upvoteDataService.insert(upvote);
    }

    /**
     * JSON 工具方法：在数组中查找指定 ID 的节点
     */
    private JsonNode findById(JsonNode array, Long id) {
        for (JsonNode node : array) {
            if (node.get("id").asLong() == id) {
                return node;
            }
        }
        return null;
    }

    // ==================== Command 测试（写操作） ====================

    /**
     * 1.1 成功创建顶级评论 - 评论帖子
     */
    @Test
    @DisplayName("成功创建顶级评论 - 评论帖子")
    void testCreateComment_TopLevelOnPost_Success() throws Exception {
        // 准备数据
        UserDO user = createUser("user1@test.com");
        CourseDO course = createPublishedCourse("测试课程", user.getId());
        NodeDO node = createPublishedNode("测试节点", course.getId(), user.getId());
        PostDO post = createPublishedPost("测试帖子", node.getId(), user.getId());

        // 模拟登录
        StpUtil.login(user.getId());

        // 构造请求体
        String requestBody = String.format("""
            {
                "objectId": %d,
                "objectType": %d,
                "content": "这是一条顶级评论"
            }
            """, post.getId(), ContentType.post.value());

        // 执行请求
        String response = mockMvc.perform(post("/api/v1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("token", StpUtil.getTokenValue())
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.content").value("这是一条顶级评论"))
                .andExpect(jsonPath("$.data.objectId").value(post.getId()))
                .andExpect(jsonPath("$.data.objectType").value(ContentType.post.value()))
                .andExpect(jsonPath("$.data.creatorId").value(user.getId()))
                .andExpect(jsonPath("$.data.replyToCommentId").value(0))
                .andExpect(jsonPath("$.data.toUserId").value(0))
                .andExpect(jsonPath("$.data.state").value((int)ContentState.SUBMITTED.value()))
                .andExpect(jsonPath("$.data.score").value(0.0))
                .andExpect(jsonPath("$.data.upvoteCount").value(0))
                .andExpect(jsonPath("$.data.replyCount").value(0))
                .andExpect(jsonPath("$.data.creatorName").exists())
                .andExpect(jsonPath("$.data.upvoted").value(false))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 验证数据库
        JsonNode jsonNode = objectMapper.readTree(response);
        Long commentId = jsonNode.get("data").get("id").asLong();
        CommentDO comment = commentDataService.getById(commentId);
        assertThat(comment).isNotNull();
        assertThat(comment.getContent()).isEqualTo("这是一条顶级评论");
        assertThat(comment.getReplyToCommentId()).isEqualTo(0L);  // 顶级评论使用0，不是null

        // 清理登录状态
        StpUtil.logout();
    }

    /**
     * 1.2 成功创建顶级评论 - 评论节点
     */
    @Test
    @DisplayName("成功创建顶级评论 - 评论节点")
    void testCreateComment_TopLevelOnNode_Success() throws Exception {
        // 准备数据
        UserDO user = createUser("user2@test.com");
        CourseDO course = createPublishedCourse("测试课程", user.getId());
        NodeDO node = createPublishedNode("测试节点", course.getId(), user.getId());

        // 模拟登录
        StpUtil.login(user.getId());

        // 构造请求体
        String requestBody = String.format("""
            {
                "objectId": %d,
                "objectType": %d,
                "content": "这是节点评论"
            }
            """, node.getId(), ContentType.node.value());

        // 执行请求
        mockMvc.perform(post("/api/v1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("token", StpUtil.getTokenValue())
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.objectType").value(ContentType.node.value()))
                .andExpect(jsonPath("$.data.replyToCommentId").value(0));

        StpUtil.logout();
    }

    /**
     * 1.3 成功创建回复评论 - 回复顶级评论
     */
    @Test
    @DisplayName("成功创建回复评论 - 回复顶级评论")
    void testCreateComment_ReplyToTopLevel_Success() throws Exception {
        // 准备数据
        UserDO userA = createUser("userA@test.com");
        UserDO userB = createUser("userB@test.com");
        CourseDO course = createPublishedCourse("测试课程", userA.getId());
        NodeDO node = createPublishedNode("测试节点", course.getId(), userA.getId());
        PostDO post = createPublishedPost("测试帖子", node.getId(), userA.getId());
        CommentDO topComment = createApprovedComment("顶级评论", post.getId(), ContentType.post.value(), userA.getId());

        // 模拟用户B登录
        StpUtil.login(userB.getId());

        // 构造请求体
        String requestBody = String.format("""
            {
                "objectId": %d,
                "objectType": %d,
                "replyTo": %d,
                "toUser": %d,
                "content": "回复顶级评论"
            }
            """, post.getId(), ContentType.post.value(), topComment.getId(), userA.getId());

        // 执行请求
        mockMvc.perform(post("/api/v1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("token", StpUtil.getTokenValue())
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.replyToCommentId").value(topComment.getId()))
                .andExpect(jsonPath("$.data.toUserId").value(userA.getId()))
                .andExpect(jsonPath("$.data.toUserName").exists());

        // 验证父评论的回复已创建（通过查询回复数量）
        // 注意：CommentDO 本身没有 replyCount 字段，需要通过查询统计

        StpUtil.logout();
    }

    /**
     * 1.4 成功创建子回复 - 回复子评论
     */
    @Test
    @DisplayName("成功创建子回复 - 回复子评论")
    void testCreateComment_ReplyToReply_Success() throws Exception {
        // 准备数据
        UserDO userA = createUser("userA@test.com");
        UserDO userB = createUser("userB@test.com");
        UserDO userC = createUser("userC@test.com");
        CourseDO course = createPublishedCourse("测试课程", userA.getId());
        NodeDO node = createPublishedNode("测试节点", course.getId(), userA.getId());
        PostDO post = createPublishedPost("测试帖子", node.getId(), userA.getId());

        // 用户A创建顶级评论
        CommentDO topComment = createApprovedComment("顶级评论", post.getId(), ContentType.post.value(), userA.getId());
        // 用户B回复顶级评论
        CommentDO reply1 = createApprovedReply("回复A", post.getId(), ContentType.post.value(), userB.getId(), topComment.getId(), userA.getId());

        // 模拟用户C登录
        StpUtil.login(userC.getId());

        // 构造请求体 - 用户C回复用户B的评论
        String requestBody = String.format("""
            {
                "objectId": %d,
                "objectType": %d,
                "replyTo": %d,
                "toUser": %d,
                "content": "回复B的评论"
            }
            """, post.getId(), ContentType.post.value(), topComment.getId(), userB.getId());

        // 执行请求
        mockMvc.perform(post("/api/v1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("token", StpUtil.getTokenValue())
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.replyToCommentId").value(topComment.getId()))  // 仍指向顶级评论
                .andExpect(jsonPath("$.data.toUserId").value(userB.getId()));  // 指向用户B

        StpUtil.logout();
    }

    /**
     * 1.5 字段验证 - content 为空
     */
    @Test
    @DisplayName("字段验证 - content 为空")
    void testCreateComment_ContentEmpty_Fail() throws Exception {
        UserDO user = createUser("user@test.com");
        CourseDO course = createPublishedCourse("测试课程", user.getId());
        NodeDO node = createPublishedNode("测试节点", course.getId(), user.getId());
        PostDO post = createPublishedPost("测试帖子", node.getId(), user.getId());

        StpUtil.login(user.getId());

        String requestBody = String.format("""
            {
                "objectId": %d,
                "objectType": 1,
                "content": ""
            }
            """, post.getId());

        mockMvc.perform(post("/api/v1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("token", StpUtil.getTokenValue())
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1002));  // StatusCode.INVALID_PARAMETER

        StpUtil.logout();
    }

    /**
     * 1.7 字段验证 - objectId 缺失
     */
    @Test
    @DisplayName("字段验证 - objectId 缺失")
    void testCreateComment_ObjectIdMissing_Fail() throws Exception {
        UserDO user = createUser("user@test.com");
        StpUtil.login(user.getId());

        String requestBody = """
            {
                "objectType": 1,
                "content": "评论内容"
            }
            """;

        mockMvc.perform(post("/api/v1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("token", StpUtil.getTokenValue())
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1002));  // StatusCode.INVALID_PARAMETER

        StpUtil.logout();
    }

    /**
     * 1.8 字段验证 - objectId 无效（0）
     */
    @Test
    @DisplayName("字段验证 - objectId 无效为0")
    void testCreateComment_ObjectIdZero_Fail() throws Exception {
        UserDO user = createUser("user@test.com");
        StpUtil.login(user.getId());

        String requestBody = String.format("""
            {
                "objectId": 0,
                "objectType": %d,
                "content": "评论内容"
            }
            """, ContentType.post.value());

        mockMvc.perform(post("/api/v1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("token", StpUtil.getTokenValue())
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

        StpUtil.logout();
    }

    /**
     * 1.10 字段验证 - objectType 超出范围
     */
    @Test
    @DisplayName("字段验证 - objectType 超出范围")
    void testCreateComment_ObjectTypeOutOfRange_Fail() throws Exception {
        UserDO user = createUser("user@test.com");
        StpUtil.login(user.getId());

        String requestBody = """
            {
                "objectId": 123,
                "objectType": 99,
                "content": "评论内容"
            }
            """;

        mockMvc.perform(post("/api/v1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("token", StpUtil.getTokenValue())
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

        StpUtil.logout();
    }

    /**
     * 1.17 权限验证 - 未登录
     */
    @Test
    @DisplayName("权限验证 - 未登录创建评论")
    void testCreateComment_NotLoggedIn_Fail() throws Exception {
        String requestBody = String.format("""
            {
                "objectId": 123,
                "objectType": %d,
                "content": "评论内容"
            }
            """, ContentType.post.value());

        mockMvc.perform(post("/api/v1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));
    }

    // ==================== Query 测试（读操作） ====================

    /**
     * 2.1 获取帖子的评论列表（带子评论）
     */
    @Test
    @DisplayName("获取帖子的评论列表（带子评论）")
    void testGetComments_WithReplies_Success() throws Exception {
        // 准备数据
        UserDO user = createUser("user@test.com");
        CourseDO course = createPublishedCourse("测试课程", user.getId());
        NodeDO node = createPublishedNode("测试节点", course.getId(), user.getId());
        PostDO post = createPublishedPost("测试帖子", node.getId(), user.getId());

        // 创建3条顶级评论
        CommentDO comment1 = createApprovedComment("评论1", post.getId(), ContentType.post.value(), user.getId());
        CommentDO comment2 = createApprovedComment("评论2", post.getId(), ContentType.post.value(), user.getId());
        CommentDO comment3 = createApprovedComment("评论3", post.getId(), ContentType.post.value(), user.getId());

        // 评论1有2条回复
        createApprovedReply("回复1-1", post.getId(), ContentType.post.value(), user.getId(), comment1.getId(), user.getId());
        createApprovedReply("回复1-2", post.getId(), ContentType.post.value(), user.getId(), comment1.getId(), user.getId());

        // 评论2有1条回复
        createApprovedReply("回复2-1", post.getId(), ContentType.post.value(), user.getId(), comment2.getId(), user.getId());

        // 模拟登录
        StpUtil.login(user.getId());

        // 执行请求
        String response = mockMvc.perform(get("/api/v1/comments")
                        .param("objectId", post.getId().toString())
                        .param("objectType", String.valueOf(ContentType.post.value()))
                        .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items.length()").value(3))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 验证子评论
        JsonNode jsonNode = objectMapper.readTree(response);
        JsonNode items = jsonNode.get("data").get("items");

        // 查找comment1，验证它有子评论（注意：getChildren只返回每个父评论最热门的1条）
        JsonNode comment1Node = findById(items, comment1.getId());
        assertThat(comment1Node).isNotNull();
        assertThat(comment1Node.get("children").size()).isEqualTo(1);  // 只返回最热门的1条

        // 查找comment2，验证它有子评论
        JsonNode comment2Node = findById(items, comment2.getId());
        assertThat(comment2Node).isNotNull();
        assertThat(comment2Node.get("children").size()).isEqualTo(1);

        // 查找comment3，验证它没有子评论
        JsonNode comment3Node = findById(items, comment3.getId());
        assertThat(comment3Node).isNotNull();
        assertThat(comment3Node.get("children").size()).isEqualTo(0);

        StpUtil.logout();
    }

    /**
     * 2.2 获取节点的评论列表
     */
    @Test
    @DisplayName("获取节点的评论列表")
    void testGetComments_OnNode_Success() throws Exception {
        // 准备数据
        UserDO user = createUser("user@test.com");
        CourseDO course = createPublishedCourse("测试课程", user.getId());
        NodeDO node = createPublishedNode("测试节点", course.getId(), user.getId());

        // 在节点下创建5条评论
        for (int i = 1; i <= 5; i++) {
            createApprovedComment("节点评论" + i, node.getId(), ContentType.node.value(), user.getId());
        }

        // 模拟登录
        StpUtil.login(user.getId());

        // 执行请求
        mockMvc.perform(get("/api/v1/comments")
                        .param("objectId", node.getId().toString())
                        .param("objectType", String.valueOf(ContentType.node.value()))
                        .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items.length()").value(5))
                .andExpect(jsonPath("$.data.items[0].objectType").value(ContentType.node.value()));

        StpUtil.logout();
    }

    /**
     * 2.8 评论点赞状态 - 已点赞
     */
    @Test
    @DisplayName("评论点赞状态 - 已点赞")
    void testGetComments_WithUpvoted_Success() throws Exception {
        // 准备数据
        UserDO user = createUser("user@test.com");
        CourseDO course = createPublishedCourse("测试课程", user.getId());
        NodeDO node = createPublishedNode("测试节点", course.getId(), user.getId());
        PostDO post = createPublishedPost("测试帖子", node.getId(), user.getId());
        CommentDO comment = createApprovedComment("测试评论", post.getId(), ContentType.post.value(), user.getId());

        // 用户对评论点赞
        createCommentUpvote(user.getId(), comment.getId());

        // 模拟登录
        StpUtil.login(user.getId());

        // 执行请求
        mockMvc.perform(get("/api/v1/comments")
                        .param("objectId", post.getId().toString())
                        .param("objectType", String.valueOf(ContentType.post.value()))
                        .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.items[0].upvoted").value(true))
                .andExpect(jsonPath("$.data.items[0].upvoteCount").exists());

        StpUtil.logout();
    }

    /**
     * 2.10 空结果 - 对象无评论
     */
    @Test
    @DisplayName("空结果 - 对象无评论")
    void testGetComments_EmptyResult_Success() throws Exception {
        // 准备数据
        UserDO user = createUser("user@test.com");
        CourseDO course = createPublishedCourse("测试课程", user.getId());
        NodeDO node = createPublishedNode("测试节点", course.getId(), user.getId());
        PostDO post = createPublishedPost("测试帖子", node.getId(), user.getId());

        // 模拟登录
        StpUtil.login(user.getId());

        // 执行请求
        mockMvc.perform(get("/api/v1/comments")
                        .param("objectId", post.getId().toString())
                        .param("objectType", String.valueOf(ContentType.post.value()))
                        .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items.length()").value(0))
                .andExpect(jsonPath("$.data.hasMore").value(false));

        StpUtil.logout();
    }

    /**
     * 2.11 参数验证 - objectId 缺失
     */
    @Test
    @DisplayName("参数验证 - objectId 缺失")
    void testGetComments_ObjectIdMissing_Fail() throws Exception {
        UserDO user = createUser("user@test.com");
        StpUtil.login(user.getId());

        mockMvc.perform(get("/api/v1/comments")
                        .param("objectType", String.valueOf(ContentType.post.value()))
                        .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

        StpUtil.logout();
    }

    /**
     * 2.16 权限验证 - 需要登录
     */
    @Test
    @DisplayName("权限验证 - 获取评论列表需要登录")
    void testGetComments_NotLoggedIn_Fail() throws Exception {
        mockMvc.perform(get("/api/v1/comments")
                        .param("objectId", "123")
                        .param("objectType", String.valueOf(ContentType.post.value())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));
    }

    /**
     * 3.1 获取评论的所有回复
     */
    @Test
    @DisplayName("获取评论的所有回复")
    void testGetCommentReplies_Success() throws Exception {
        // 准备数据
        UserDO user = createUser("user@test.com");
        CourseDO course = createPublishedCourse("测试课程", user.getId());
        NodeDO node = createPublishedNode("测试节点", course.getId(), user.getId());
        PostDO post = createPublishedPost("测试帖子", node.getId(), user.getId());

        // 创建顶级评论
        CommentDO topComment = createApprovedComment("顶级评论", post.getId(), ContentType.post.value(), user.getId());

        // 创建5条回复
        for (int i = 1; i <= 5; i++) {
            createApprovedReply("回复" + i, post.getId(), ContentType.post.value(), user.getId(), topComment.getId(), user.getId());
        }

        // 模拟登录
        StpUtil.login(user.getId());

        // 执行请求
        mockMvc.perform(get("/api/v1/comments/" + topComment.getId() + "/replies")
                        .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items.length()").value(5))
                .andExpect(jsonPath("$.data.items[0].replyToCommentId").value(topComment.getId()));

        StpUtil.logout();
    }

    /**
     * 3.6 空结果 - 评论无回复
     */
    @Test
    @DisplayName("空结果 - 评论无回复")
    void testGetCommentReplies_EmptyResult_Success() throws Exception {
        // 准备数据
        UserDO user = createUser("user@test.com");
        CourseDO course = createPublishedCourse("测试课程", user.getId());
        NodeDO node = createPublishedNode("测试节点", course.getId(), user.getId());
        PostDO post = createPublishedPost("测试帖子", node.getId(), user.getId());
        CommentDO topComment = createApprovedComment("顶级评论", post.getId(), ContentType.post.value(), user.getId());

        // 模拟登录
        StpUtil.login(user.getId());

        // 执行请求
        mockMvc.perform(get("/api/v1/comments/" + topComment.getId() + "/replies")
                        .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items.length()").value(0))
                .andExpect(jsonPath("$.data.hasMore").value(false));

        StpUtil.logout();
    }

    /**
     * 3.10 权限验证 - 需要登录
     */
    @Test
    @DisplayName("权限验证 - 获取评论回复需要登录")
    void testGetCommentReplies_NotLoggedIn_Fail() throws Exception {
        mockMvc.perform(get("/api/v1/comments/123/replies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));
    }

    // ==================== 软删除功能测试 ====================

    /**
     * 4.1 软删除评论 - 成功
     */
    @Test
    @DisplayName("软删除评论 - 成功")
    void testSoftDeleteComment_Success() {
        // 准备数据
        UserDO user = createUser("user@test.com");
        CourseDO course = createPublishedCourse("测试课程", user.getId());
        NodeDO node = createPublishedNode("测试节点", course.getId(), user.getId());
        PostDO post = createPublishedPost("测试帖子", node.getId(), user.getId());
        CommentDO comment = createApprovedComment("测试评论", post.getId(), ContentType.post.value(), user.getId());

        Long commentId = comment.getId();
        assertThat(commentDataService.getById(commentId)).isNotNull();

        // 执行软删除
        int result = commentDataService.deleteById(commentId);

        // 验证软删除成功，查询不到该评论
        assertThat(result).isEqualTo(1);
        assertThat(commentDataService.getById(commentId)).isNull();
    }

    /**
     * 4.2 软删除后查询评论列表 - 不包含已删除评论
     */
    @Test
    @DisplayName("软删除后查询评论列表 - 不包含已删除评论")
    void testGetComments_ExcludesDeletedComments() throws Exception {
        // 准备数据
        UserDO user = createUser("user@test.com");
        CourseDO course = createPublishedCourse("测试课程", user.getId());
        NodeDO node = createPublishedNode("测试节点", course.getId(), user.getId());
        PostDO post = createPublishedPost("测试帖子", node.getId(), user.getId());

        // 创建3条评论
        CommentDO comment1 = createApprovedComment("评论1", post.getId(), ContentType.post.value(), user.getId());
        CommentDO comment2 = createApprovedComment("评论2", post.getId(), ContentType.post.value(), user.getId());
        CommentDO comment3 = createApprovedComment("评论3", post.getId(), ContentType.post.value(), user.getId());

        // 软删除第2条评论
        commentDataService.deleteById(comment2.getId());

        // 模拟登录
        StpUtil.login(user.getId());

        // 查询评论列表
        String response = mockMvc.perform(get("/api/v1/comments")
                        .param("objectId", post.getId().toString())
                        .param("objectType", String.valueOf(ContentType.post.value()))
                        .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 验证返回的评论不包含已删除的评论2
        JsonNode jsonNode = objectMapper.readTree(response);
        JsonNode items = jsonNode.get("data").get("items");

        assertThat(findById(items, comment1.getId())).isNotNull();
        assertThat(findById(items, comment2.getId())).isNull();
        assertThat(findById(items, comment3.getId())).isNotNull();

        StpUtil.logout();
    }

    /**
     * 4.3 软删除父评论后 - 子评论查询不包含已删除父评论的子评论
     */
    @Test
    @DisplayName("软删除父评论后 - 子评论查询不包含已删除父评论")
    void testGetChildren_ExcludesDeletedParentComments() {
        // 准备数据
        UserDO user = createUser("user@test.com");
        CourseDO course = createPublishedCourse("测试课程", user.getId());
        NodeDO node = createPublishedNode("测试节点", course.getId(), user.getId());
        PostDO post = createPublishedPost("测试帖子", node.getId(), user.getId());

        // 创建两个父评论
        CommentDO parent1 = createApprovedComment("父评论1", post.getId(), ContentType.post.value(), user.getId());
        CommentDO parent2 = createApprovedComment("父评论2", post.getId(), ContentType.post.value(), user.getId());

        // 每个父评论创建子评论
        CommentDO child1 = createApprovedReply("子评论1", post.getId(), ContentType.post.value(), user.getId(), parent1.getId(), user.getId());
        CommentDO child2 = createApprovedReply("子评论2", post.getId(), ContentType.post.value(), user.getId(), parent2.getId(), user.getId());

        // 软删除第一个子评论
        commentDataService.deleteById(child1.getId());

        // 查询子评论（getChildren 返回每个父评论最高分的一条）
        List<CommentDO> children = commentDataService.getChildren(java.util.List.of(parent1.getId(), parent2.getId()));

        // 验证只返回未删除的子评论
        assertThat(children).hasSize(1);
        assertThat(children.get(0).getId()).isEqualTo(child2.getId());
    }

    /**
     * 4.4 软删除评论后 - 按状态查询不包含已删除评论
     */
    @Test
    @DisplayName("软删除评论后 - 按状态查询不包含已删除评论")
    void testGetListByState_ExcludesDeletedComments() {
        // 准备数据
        UserDO user = createUser("user@test.com");
        CourseDO course = createPublishedCourse("测试课程", user.getId());
        NodeDO node = createPublishedNode("测试节点", course.getId(), user.getId());
        PostDO post = createPublishedPost("测试帖子", node.getId(), user.getId());

        // 创建3条已发布评论
        CommentDO comment1 = createApprovedComment("评论1", post.getId(), ContentType.post.value(), user.getId());
        CommentDO comment2 = createApprovedComment("评论2", post.getId(), ContentType.post.value(), user.getId());
        CommentDO comment3 = createApprovedComment("评论3", post.getId(), ContentType.post.value(), user.getId());

        // 软删除第2条评论
        commentDataService.deleteById(comment2.getId());

        // 按状态查询
        List<CommentDO> comments = commentDataService.getListByState(
            ContentState.PUBLISHED.value(),
            null,
            10
        );

        // 验证只返回未删除的评论
        assertThat(comments).extracting(CommentDO::getId)
            .contains(comment1.getId(), comment3.getId())
            .doesNotContain(comment2.getId());
    }

    /**
     * 4.5 软删除评论后 - 条件过滤查询不包含已删除评论
     */
    @Test
    @DisplayName("软删除评论后 - 条件过滤查询不包含已删除评论")
    void testGetListByFilter_ExcludesDeletedComments() {
        // 准备数据
        UserDO user = createUser("user@test.com");
        CourseDO course = createPublishedCourse("测试课程", user.getId());
        NodeDO node = createPublishedNode("测试节点", course.getId(), user.getId());
        PostDO post = createPublishedPost("测试帖子", node.getId(), user.getId());

        // 创建3条评论
        CommentDO comment1 = createApprovedComment("评论1", post.getId(), ContentType.post.value(), user.getId());
        CommentDO comment2 = createApprovedComment("评论2", post.getId(), ContentType.post.value(), user.getId());
        CommentDO comment3 = createApprovedComment("评论3", post.getId(), ContentType.post.value(), user.getId());

        // 软删除第1条评论
        commentDataService.deleteById(comment1.getId());

        // 使用过滤条件查询
        List<CommentDO> comments = commentDataService.getListByFilter(
            ContentType.post.value(),
            post.getId(),
            null,
            null,
            ContentState.PUBLISHED.value(),
            10
        );

        // 验证只返回未删除的评论
        assertThat(comments).hasSize(2);
        assertThat(comments).extracting(CommentDO::getId)
            .contains(comment2.getId(), comment3.getId())
            .doesNotContain(comment1.getId());
    }
}
