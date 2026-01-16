package com.prosper.learn.web.v1.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prosper.learn.content.course.CourseDO;
import com.prosper.learn.content.course.CourseDataService;
import com.prosper.learn.content.node.NodeDO;
import com.prosper.learn.content.node.NodeDataService;
import com.prosper.learn.content.post.PostDO;
import com.prosper.learn.content.post.PostDataService;
import com.prosper.learn.content.roadmap.RoadmapDO;
import com.prosper.learn.content.roadmap.RoadmapDataService;
import com.prosper.learn.interaction.comment.CommentDO;
import com.prosper.learn.interaction.comment.CommentDataService;
import com.prosper.learn.interaction.upvote.UpvoteDO;
import com.prosper.learn.interaction.upvote.UpvoteDataService;
import com.prosper.learn.memory.deck.MemoryCardDeckDO;
import com.prosper.learn.memory.deck.MemoryCardDeckDataService;
import com.prosper.learn.shared.domain.Enums.ContentState;
import com.prosper.learn.shared.domain.Enums.ContentType;
import com.prosper.learn.shared.domain.Enums.PostType;
import com.prosper.learn.shared.domain.Enums.VoteType;
import com.prosper.learn.shared.domain.exception.StatusCode;
import com.prosper.learn.user.profile.UserDO;
import com.prosper.learn.user.profile.UserDataService;
import com.prosper.learn.user.profile.UserDomainService;
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
 * 点赞接口测试
 * 测试文档: docs/test/upvote.md
 */
@Transactional
public class UpvotesControllerTest extends BaseControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PostDataService postDataService;

    @Autowired
    private CommentDataService commentDataService;

    @Autowired
    private RoadmapDataService roadmapDataService;

    @Autowired
    private MemoryCardDeckDataService memoryCardDeckDataService;

    @Autowired
    private UpvoteDataService upvoteDataService;

    @Autowired
    private UserDataService userDataService;

    @Autowired
    private UserDomainService userDomainService;

    @Autowired
    private CourseDataService courseDataService;

    @Autowired
    private NodeDataService nodeDataService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
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
     * 创建测试课程
     */
    private CourseDO createCourse(Long creatorId) {
        // 先创建临时课程用于获取ID
        CourseDO course = new CourseDO();
        course.setCreatorId(creatorId);
        course.setName("测试课程");
        course.setDescription("这是一个测试课程");
        course.setMainCategory(1);
        course.setSubCategory(1);
        course.setParentCourseId(0L);
        course.setRootNodeId(0L); // 临时设置为0
        course.setState(ContentState.PUBLISHED.value());
        courseDataService.insert(course);

        // 创建根节点
        NodeDO rootNode = new NodeDO();
        rootNode.setCourseId(course.getId());
        rootNode.setName("根节点");
        rootNode.setDescription("根节点描述");
        rootNode.setCreatorId(creatorId);
        rootNode.setState(ContentState.PUBLISHED.value());
        nodeDataService.insert(rootNode);

        // 更新课程的 rootNodeId
        course.setRootNodeId(rootNode.getId());
        courseDataService.update(course);

        return course;
    }

    /**
     * 创建测试节点
     */
    private NodeDO createNode(Long courseId, Long creatorId) {
        NodeDO node = new NodeDO();
        node.setCourseId(courseId);
        node.setCreatorId(creatorId);
        node.setName("测试节点");
        node.setDescription("测试节点描述");
        node.setState(ContentState.PUBLISHED.value());
        nodeDataService.insert(node);
        return node;
    }

    /**
     * 创建测试帖子
     */
    private PostDO createPost(Long creatorId, Long nodeId) {
        PostDO post = new PostDO();
        post.setCreatorId(creatorId);
        post.setNodeId(nodeId);
        post.setType(PostType.article.value());
        post.setContent("这是一篇测试帖子的内容");
        post.setState(ContentState.PUBLISHED.value());
        postDataService.insert(post);
        return post;
    }

    /**
     * 创建测试评论
     */
    private CommentDO createComment(Long creatorId, Long objectId, ContentType objectType) {
        CommentDO comment = new CommentDO();
        comment.setCreatorId(creatorId);
        comment.setObjectId(objectId);
        comment.setObjectType(objectType.value());
        comment.setContent("这是一条测试评论");
        comment.setReplyToCommentId(0L);
        comment.setToUserId(0L);
        comment.setState(ContentState.PUBLISHED.value());
        comment.setScore(0.0);
        commentDataService.insert(comment);
        return comment;
    }

    /**
     * 创建测试路线图
     */
    private RoadmapDO createRoadmap(Long creatorId) {
        RoadmapDO roadmap = new RoadmapDO();
        roadmap.setCreatorId(creatorId);
        roadmap.setContent("{}");
        roadmap.setContentHash("test-hash");
        roadmap.setDescription("这是一个测试路线图");
        roadmap.setProfessionId(0L);
        roadmap.setState(ContentState.PUBLISHED.value());
        roadmap.setScore(0.0);
        roadmapDataService.insert(roadmap);
        return roadmap;
    }

    /**
     * 创建测试记忆卡片组
     */
    private MemoryCardDeckDO createDeck(Long creatorId) {
        MemoryCardDeckDO deck = new MemoryCardDeckDO();
        deck.setCreatorId(creatorId);
        deck.setTitle("测试卡片组");
        deck.setDescription("这是一个测试卡片组");
        deck.setPostId(0L);
        deck.setNodeId(0L);
        deck.setVersion(1);
        deck.setState((byte) 1);
        deck.setCardCount(0);
        memoryCardDeckDataService.insert(deck);
        return deck;
    }

    // ==================== 测试用例 ====================

    /**
     * 测试1: 帖子点赞 - 完整流程
     */
    @Test
    @DisplayName("帖子点赞 - 完整流程测试")
    void testPostUpvote_FullWorkflow() throws Exception {
        // 准备：创建用户和帖子
        UserDO user = createUser("upvote@test.com");
        UserDO creator = createUser("creator@test.com");

        CourseDO course = createCourse(creator.getId());
        NodeDO node = createNode(course.getId(), creator.getId());
        PostDO post = createPost(creator.getId(), node.getId());

        StpUtil.login(user.getId());

        try {
            // 1. 首次点 twice - 创建记录
            String twiceRequest = String.format("""
                {
                    "objectId": %d,
                    "objectType": 1,
                    "type": 1
                }
                """, post.getId());

            mockMvc.perform(post("/api/v1/upvotes")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(twiceRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data.twiced").value(true))
                    .andExpect(jsonPath("$.data.liked").value(false));

            // 验证：数据库中存在点赞记录
            UpvoteDO upvote = upvoteDataService.getByUserAndObject(
                user.getId(), post.getId(), ContentType.post.value());
            assertThat(upvote).isNotNull();
            assertThat(upvote.getType()).isEqualTo(VoteType.twice.value());

            // 2. 重复点 twice - 取消点赞
            mockMvc.perform(post("/api/v1/upvotes")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(twiceRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data.twiced").value(false))
                    .andExpect(jsonPath("$.data.liked").value(false));

            // 验证：数据库中点赞记录已删除
            UpvoteDO cancelledUpvote = upvoteDataService.getByUserAndObject(
                user.getId(), post.getId(), ContentType.post.value());
            assertThat(cancelledUpvote).isNull();

            // 3. 首次点 like - 创建记录
            String likeRequest = String.format("""
                {
                    "objectId": %d,
                    "objectType": 1,
                    "type": 2
                }
                """, post.getId());

            mockMvc.perform(post("/api/v1/upvotes")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(likeRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data.twiced").value(false))
                    .andExpect(jsonPath("$.data.liked").value(true));

            // 验证：数据库中存在 like 记录
            UpvoteDO likeUpvote = upvoteDataService.getByUserAndObject(
                user.getId(), post.getId(), ContentType.post.value());
            assertThat(likeUpvote).isNotNull();
            assertThat(likeUpvote.getType()).isEqualTo(VoteType.like.value());

            // 4. 重复点 like - 取消点赞
            mockMvc.perform(post("/api/v1/upvotes")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(likeRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data.twiced").value(false))
                    .andExpect(jsonPath("$.data.liked").value(false));

            // 5. 先点 twice 再点 like - 切换类型
            mockMvc.perform(post("/api/v1/upvotes")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(twiceRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.twiced").value(true));

            mockMvc.perform(post("/api/v1/upvotes")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(likeRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data.twiced").value(false))
                    .andExpect(jsonPath("$.data.liked").value(true));

            // 验证：数据库中是 like 记录
            UpvoteDO switchedUpvote = upvoteDataService.getByUserAndObject(
                user.getId(), post.getId(), ContentType.post.value());
            assertThat(switchedUpvote).isNotNull();
            assertThat(switchedUpvote.getType()).isEqualTo(VoteType.like.value());

            // 6. 先点 like 再点 twice - 切换类型
            mockMvc.perform(post("/api/v1/upvotes")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(twiceRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data.twiced").value(true))
                    .andExpect(jsonPath("$.data.liked").value(false));

            // 验证：数据库中是 twice 记录
            UpvoteDO finalUpvote = upvoteDataService.getByUserAndObject(
                user.getId(), post.getId(), ContentType.post.value());
            assertThat(finalUpvote).isNotNull();
            assertThat(finalUpvote.getType()).isEqualTo(VoteType.twice.value());

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试2: 评论点赞 - 仅 like
     */
    @Test
    @DisplayName("评论点赞 - 仅支持 like 点赞")
    void testCommentUpvote() throws Exception {
        // 准备：创建用户、帖子和评论
        UserDO user = createUser("comment-upvote@test.com");
        UserDO creator = createUser("comment-creator@test.com");

        CourseDO course = createCourse(creator.getId());
        NodeDO node = createNode(course.getId(), creator.getId());
        PostDO post = createPost(creator.getId(), node.getId());
        CommentDO comment = createComment(creator.getId(), post.getId(), ContentType.post);

        StpUtil.login(user.getId());

        try {
            // 1. 首次点 like - 创建记录
            String likeRequest = String.format("""
                {
                    "objectId": %d,
                    "objectType": 3,
                    "type": 2
                }
                """, comment.getId());

            mockMvc.perform(post("/api/v1/upvotes")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(likeRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data.twiced").value(false))
                    .andExpect(jsonPath("$.data.liked").value(true));

            // 验证：数据库中存在点赞记录
            UpvoteDO upvote = upvoteDataService.getByUserAndObject(
                user.getId(), comment.getId(), ContentType.comment.value());
            assertThat(upvote).isNotNull();
            assertThat(upvote.getType()).isEqualTo(VoteType.like.value());

            // 2. 重复点 like - 取消点赞
            mockMvc.perform(post("/api/v1/upvotes")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(likeRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data.twiced").value(false))
                    .andExpect(jsonPath("$.data.liked").value(false));

            // 验证：数据库中点赞记录已删除
            UpvoteDO cancelledUpvote = upvoteDataService.getByUserAndObject(
                user.getId(), comment.getId(), ContentType.comment.value());
            assertThat(cancelledUpvote).isNull();

            // 3. 评论不存在 - 返回404
            String invalidCommentRequest = """
                {
                    "objectId": 99999,
                    "objectType": 3,
                    "type": 2
                }
                """;

            mockMvc.perform(post("/api/v1/upvotes")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidCommentRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.COMMENT_NOT_FOUND.getCode()));

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试3: 路线图点赞
     */
    @Test
    @DisplayName("路线图点赞 - 仅支持 like 点赞")
    void testRoadmapUpvote() throws Exception {
        UserDO user = createUser("roadmap-upvote@test.com");
        UserDO creator = createUser("roadmap-creator@test.com");
        RoadmapDO roadmap = createRoadmap(creator.getId());

        StpUtil.login(user.getId());

        try {
            // 1. 首次点 like
            String likeRequest = String.format("""
                {
                    "objectId": %d,
                    "objectType": 4,
                    "type": 2
                }
                """, roadmap.getId());

            mockMvc.perform(post("/api/v1/upvotes")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(likeRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data.liked").value(true));

            // 2. 重复点 like
            mockMvc.perform(post("/api/v1/upvotes")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(likeRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data.liked").value(false));

            // 3. 路线图不存在
            String invalidRequest = """
                {
                    "objectId": 99999,
                    "objectType": 4,
                    "type": 2
                }
                """;

            mockMvc.perform(post("/api/v1/upvotes")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.ROADMAP_NOT_FOUND.getCode()));

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试4: 记忆卡片组点赞
     */
    @Test
    @DisplayName("记忆卡片组点赞 - 仅支持 like 点赞")
    void testMemoryCardDeckUpvote() throws Exception {
        UserDO user = createUser("deck-upvote@test.com");
        UserDO creator = createUser("deck-creator@test.com");
        MemoryCardDeckDO deck = createDeck(creator.getId());

        StpUtil.login(user.getId());

        try {
            // 1. 首次点 like
            String likeRequest = String.format("""
                {
                    "objectId": %d,
                    "objectType": 5,
                    "type": 2
                }
                """, deck.getId());

            mockMvc.perform(post("/api/v1/upvotes")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(likeRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data.liked").value(true));

            // 2. 重复点 like
            mockMvc.perform(post("/api/v1/upvotes")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(likeRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data.liked").value(false));

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试5: 获取点赞状态
     */
    @Test
    @DisplayName("获取点赞状态")
    void testGetUpvoteStatus() throws Exception {
        UserDO user = createUser("status@test.com");
        UserDO creator = createUser("status-creator@test.com");

        CourseDO course = createCourse(creator.getId());
        NodeDO node = createNode(course.getId(), creator.getId());
        PostDO post = createPost(creator.getId(), node.getId());

        StpUtil.login(user.getId());

        try {
            // 1. 获取未点赞的状态
            mockMvc.perform(get("/api/v1/upvotes/status")
                    .param("objectId", post.getId().toString())
                    .param("objectType", "1")
                    .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data.twiced").value(false))
                    .andExpect(jsonPath("$.data.liked").value(false));

            // 2. 点赞后获取状态（twice）
            String twiceRequest = String.format("""
                {
                    "objectId": %d,
                    "objectType": 1,
                    "type": 1
                }
                """, post.getId());

            mockMvc.perform(post("/api/v1/upvotes")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(twiceRequest))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/v1/upvotes/status")
                    .param("objectId", post.getId().toString())
                    .param("objectType", "1")
                    .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data.twiced").value(true))
                    .andExpect(jsonPath("$.data.liked").value(false));

            // 3. 切换为 like 后获取状态
            String likeRequest = String.format("""
                {
                    "objectId": %d,
                    "objectType": 1,
                    "type": 2
                }
                """, post.getId());

            mockMvc.perform(post("/api/v1/upvotes")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(likeRequest))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/v1/upvotes/status")
                    .param("objectId", post.getId().toString())
                    .param("objectType", "1")
                    .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data.twiced").value(false))
                    .andExpect(jsonPath("$.data.liked").value(true));

            // 4. 获取不存在内容的点赞状态
            mockMvc.perform(get("/api/v1/upvotes/status")
                    .param("objectId", "99999")
                    .param("objectType", "1")
                    .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data.twiced").value(false))
                    .andExpect(jsonPath("$.data.liked").value(false));

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试6: 参数验证
     */
    @Test
    @DisplayName("参数验证 - 所有验证场景")
    void testParameterValidation() throws Exception {
        UserDO user = createUser("validation@test.com");
        StpUtil.login(user.getId());

        try {
            // 1. objectId = 0
            String zeroIdRequest = """
                {
                    "objectId": 0,
                    "objectType": 1,
                    "type": 1
                }
                """;

            mockMvc.perform(post("/api/v1/upvotes")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(zeroIdRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

            // 2. objectId = -1
            String negativeIdRequest = """
                {
                    "objectId": -1,
                    "objectType": 1,
                    "type": 1
                }
                """;

            mockMvc.perform(post("/api/v1/upvotes")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(negativeIdRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

            // 3. objectId = null
            String nullIdRequest = """
                {
                    "objectType": 1,
                    "type": 1
                }
                """;

            mockMvc.perform(post("/api/v1/upvotes")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(nullIdRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

            // 4. objectType = null
            String nullTypeRequest = """
                {
                    "objectId": 123,
                    "type": 1
                }
                """;

            mockMvc.perform(post("/api/v1/upvotes")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(nullTypeRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

            // 5. type = 0 (超出范围)
            String zeroVoteTypeRequest = """
                {
                    "objectId": 123,
                    "objectType": 1,
                    "type": 0
                }
                """;

            mockMvc.perform(post("/api/v1/upvotes")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(zeroVoteTypeRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

            // 6. type = 3 (超出范围)
            String invalidVoteTypeRequest = """
                {
                    "objectId": 123,
                    "objectType": 1,
                    "type": 3
                }
                """;

            mockMvc.perform(post("/api/v1/upvotes")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidVoteTypeRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

            // 7. type = null
            String nullVoteTypeRequest = """
                {
                    "objectId": 123,
                    "objectType": 1
                }
                """;

            mockMvc.perform(post("/api/v1/upvotes")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(nullVoteTypeRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

            // 8. objectType = 0 (无效值)
            String zeroObjectTypeRequest = """
                {
                    "objectId": 123,
                    "objectType": 0,
                    "type": 1
                }
                """;

            mockMvc.perform(post("/api/v1/upvotes")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(zeroObjectTypeRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

            // 9. objectType = 99 (不支持的类型)
            String unsupportedObjectTypeRequest = """
                {
                    "objectId": 123,
                    "objectType": 99,
                    "type": 1
                }
                """;

            mockMvc.perform(post("/api/v1/upvotes")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(unsupportedObjectTypeRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

            // 10. 获取点赞状态 - objectId = 0
            mockMvc.perform(get("/api/v1/upvotes/status")
                    .param("objectId", "0")
                    .param("objectType", "1")
                    .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

            // 11. 获取点赞状态 - objectType = 0
            mockMvc.perform(get("/api/v1/upvotes/status")
                    .param("objectId", "123")
                    .param("objectType", "0")
                    .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试7: 认证测试
     */
    @Test
    @DisplayName("未登录用户点赞 - 返回401")
    void testUpvoteWithoutLogin() throws Exception {
        // 1. 未登录点赞
        String upvoteRequest = """
            {
                "objectId": 123,
                "objectType": 1,
                "type": 1
            }
            """;

        mockMvc.perform(post("/api/v1/upvotes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(upvoteRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));

        // 2. 未登录获取点赞状态
        mockMvc.perform(get("/api/v1/upvotes/status")
                .param("objectId", "123")
                .param("objectType", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));
    }
}
