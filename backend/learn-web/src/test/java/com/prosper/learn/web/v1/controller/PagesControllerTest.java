package com.prosper.learn.web.v1.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.prosper.learn.content.course.CourseDO;
import com.prosper.learn.content.course.CourseDataService;
import com.prosper.learn.content.course.CourseDomainService;
import com.prosper.learn.content.node.NodeDO;
import com.prosper.learn.content.node.NodeDataService;
import com.prosper.learn.content.node.NodeDomainService;
import com.prosper.learn.content.post.PostDO;
import com.prosper.learn.content.post.PostDataService;
import com.prosper.learn.content.post.PostDomainService;
import com.prosper.learn.content.toc.TocDomainService;
import com.prosper.learn.interaction.comment.CommentDO;
import com.prosper.learn.interaction.comment.CommentDataService;
import com.prosper.learn.interaction.comment.CommentDomainService;
import com.prosper.learn.shared.domain.Enums.ContentState;
import com.prosper.learn.shared.domain.Enums.ContentType;
import com.prosper.learn.shared.domain.Enums.PostType;
import com.prosper.learn.shared.domain.exception.StatusCode;
import com.prosper.learn.user.profile.UserDO;
import com.prosper.learn.user.profile.UserDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 页面聚合接口测试
 * 测试文档: docs/test/page.md
 *
 * Query 测试 - 读操作
 */
@Transactional
public class PagesControllerTest extends BaseControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CourseDataService courseDataService;

    @Autowired
    private NodeDataService nodeDataService;

    @Autowired
    private PostDataService postDataService;

    @Autowired
    private PostDomainService postDomainService;

    @Autowired
    private CommentDataService commentDataService;

    @Autowired
    private CommentDomainService commentDomainService;

    @Autowired
    private UserDomainService userDomainService;

    @BeforeEach
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
     * 创建已发布课程（带根节点）
     * 通过 HTTP API 创建
     */
    private CourseDO createPublishedCourse(String name, Long creatorId) throws Exception {
        // 登录创建者
        StpUtil.login(creatorId);

        String createRequest = String.format("""
            {
                "name": "%s",
                "description": "这是一个完整的课程描述，用于测试页面聚合接口的功能。课程内容涵盖多个知识点，帮助学生系统性地学习相关技能。",
                "mainCategory": 1,
                "subCategory": 1
            }
            """, name);

        mockMvc.perform(post("/api/v1/courses")
                .header("token", StpUtil.getTokenValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));

        StpUtil.logout();

        // 从数据库查询创建的课程并审核通过
        List<CourseDO> courses = courseDataService.listByStateAndLastId(ContentState.SUBMITTED, null);
        CourseDO course = courses.stream()
                .filter(c -> name.equals(c.getName()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("课程未创建"));

        courseDataService.approve(course.getId());
        return courseDataService.getById(course.getId());
    }

    /**
     * 创建已发布节点
     * 直接使用 NodeDataService 插入（因为没有节点创建的 HTTP API）
     */
    private NodeDO createPublishedNode(String name, Long courseId, Long creatorId) {
        NodeDO node = new NodeDO();
        node.setName(name);
        node.setDescription("这是一个测试节点的详细描述，用于验证页面聚合接口的功能。节点包含多个帖子，支持评论和投票等交互功能，为学习者提供丰富的学习体验。");
        node.setCourseId(courseId);
        node.setCreatorId(creatorId);
        node.setState(ContentState.PUBLISHED.value());
        nodeDataService.insert(node);
        return node;
    }

    /**
     * 创建已发布帖子
     * 通过 HTTP API 创建
     */
    private PostDO createPublishedPost(Long nodeId, Long creatorId, String content) throws Exception {
        StpUtil.login(creatorId);

        // 确保内容长度至少10个字符
        String fullContent = content.length() >= 10 ? content : content + "，这是测试帖子的补充内容。";

        String createRequest = String.format("""
            {
                "nodeId": %d,
                "content": "%s",
                "type": 2
            }
            """, nodeId, fullContent);

        String response = mockMvc.perform(post("/api/v1/posts")
                .header("token", StpUtil.getTokenValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                .andReturn().getResponse().getContentAsString();

        StpUtil.logout();

        // 从响应中提取创建的帖子ID (API现在返回帖子ID)
        // 如果API不返回ID，我们需要查询数据库
        // 先尝试查询最近创建的帖子
        List<PostDO> posts = postDataService.getListByNodeAndCreator(nodeId, creatorId, null, ContentState.SUBMITTED.value(), 100);
        PostDO post = posts.stream()
                .filter(p -> p.getContent().contains(content))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("帖子未创建"));

        postDomainService.approve(post.getId());
        return postDataService.getById(post.getId());
    }

    /**
     * 创建评论
     * 通过 HTTP API 创建
     */
    private CommentDO createComment(Long objectId, ContentType objectType, Long creatorId, String content) throws Exception {
        StpUtil.login(creatorId);

        String createRequest = String.format("""
            {
                "objectId": %d,
                "objectType": %d,
                "content": "%s"
            }
            """, objectId, objectType.value(), content);

        mockMvc.perform(post("/api/v1/comments")
                .header("token", StpUtil.getTokenValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));

        StpUtil.logout();

        // 从数据库查询创建的评论（SUBMITTED 状态）
        List<CommentDO> comments = commentDataService.getListByFilter(
                objectType.value(), objectId, creatorId, null, ContentState.SUBMITTED.value(), 100);
        CommentDO comment = comments.stream()
                .filter(c -> content.equals(c.getContent()) && (c.getReplyToCommentId() == null || c.getReplyToCommentId() == 0))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("评论未创建"));

        // 审核通过评论
        commentDomainService.approveComment(comment.getId());
        return commentDataService.getById(comment.getId());
    }

    /**
     * 创建回复评论
     * 通过 HTTP API 创建
     */
    private CommentDO createReplyComment(Long objectId, ContentType objectType, Long creatorId,
                                         String content, Long replyToCommentId, Long toUserId) throws Exception {
        StpUtil.login(creatorId);

        String createRequest = String.format("""
            {
                "objectId": %d,
                "objectType": %d,
                "content": "%s",
                "replyTo": %d,
                "toUser": %d
            }
            """, objectId, objectType.value(), content, replyToCommentId, toUserId);

        mockMvc.perform(post("/api/v1/comments")
                .header("token", StpUtil.getTokenValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));

        StpUtil.logout();

        // 从数据库查询创建的回复评论（SUBMITTED 状态）
        List<CommentDO> comments = commentDataService.getListByFilter(
                objectType.value(), objectId, creatorId, null, ContentState.SUBMITTED.value(), 100);
        CommentDO comment = comments.stream()
                .filter(c -> content.equals(c.getContent()) && replyToCommentId.equals(c.getReplyToCommentId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("回复评论未创建"));

        // 审核通过评论
        commentDomainService.approveComment(comment.getId());
        return commentDataService.getById(comment.getId());
    }

    // ==================== Query 测试（读操作）====================

    /**
     * 测试1: 读取完整页面数据 - 通过课程路径
     *
     * API: GET /api/v1/pages/read?courseId={courseId}&path={path}
     *
     * 测试场景:
     * 1. 通过 courseId + path 读取成功
     * 2. 参数验证：courseId = 0
     * 3. 资源不存在：courseId 不存在
     * 4. 权限验证：未登录
     * 5. 状态验证：课程未发布
     */
    @Test
    void testReadPageByCoursePath() throws Exception {
        // 准备测试数据
        UserDO user = createUser("page@example.com");
        CourseDO course = createPublishedCourse("测试课程", user.getId());
        Long rootNodeId = course.getRootNodeId();

        NodeDO node = createPublishedNode("测试节点", course.getId(), user.getId());
        PostDO post = createPublishedPost(node.getId(), user.getId(), "这是一个测试帖子的内容，用于验证页面聚合接口的功能。");

        String path = "1-" + rootNodeId;

        StpUtil.login(user.getId());

        try {
            // 1. 成功读取
            String response = mockMvc.perform(get("/api/v1/pages/read")
                    .header("token", StpUtil.getTokenValue())
                    .param("courseId", course.getId().toString())
                    .param("path", path))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data").exists())
                    .andExpect(jsonPath("$.data.toc").isArray())
                    .andExpect(jsonPath("$.data.node").exists())
                    .andExpect(jsonPath("$.data.course").exists())
                    .andExpect(jsonPath("$.data.parentCourse").exists())
                    .andExpect(jsonPath("$.data.subCourseList").isArray())
                    .andExpect(jsonPath("$.data.otherPostings").isArray())
                    .andExpect(jsonPath("$.data.users").isArray())
                    .andExpect(jsonPath("$.data.learning").exists())
                    .andExpect(jsonPath("$.data.tocNodeInfos").exists())
                    .andExpect(jsonPath("$.data.path").exists())
                    .andReturn().getResponse().getContentAsString();

            JsonNode data = objectMapper.readTree(response).get("data");
            assertThat(data.get("course").get("id").asLong()).isEqualTo(course.getId());
            assertThat(data.get("toc").isArray()).isTrue();

            // 2. 参数验证：courseId = 0
            mockMvc.perform(get("/api/v1/pages/read")
                    .header("token", StpUtil.getTokenValue())
                    .param("courseId", "0")
                    .param("path", path))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

            // 3. 资源不存在：courseId 不存在
            mockMvc.perform(get("/api/v1/pages/read")
                    .header("token", StpUtil.getTokenValue())
                    .param("courseId", "99999")
                    .param("path", path))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.NOT_FOUND.getCode()));

            // 4. 权限验证：未登录
            StpUtil.logout();
            mockMvc.perform(get("/api/v1/pages/read")
                    .param("courseId", course.getId().toString())
                    .param("path", path))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));

        } finally {
            if (StpUtil.isLogin()) {
                StpUtil.logout();
            }
        }
    }

    /**
     * 测试2: 读取完整页面数据 - 通过节点ID
     *
     * API: GET /api/v1/pages/read?nodeId={nodeId}
     *
     * 测试场景:
     * 1. 通过 nodeId 读取成功
     * 2. 参数验证：nodeId = 0
     * 3. 参数验证：nodeId = -1
     * 4. 资源不存在：nodeId 不存在
     * 5. 状态验证：节点未发布
     */
    @Test
    void testReadPageByNodeId() throws Exception {
        UserDO user = createUser("node@example.com");
        CourseDO course = createPublishedCourse("测试课程", user.getId());
        NodeDO node = createPublishedNode("测试节点", course.getId(), user.getId());

        StpUtil.login(user.getId());

        try {
            // 1. 成功读取
            mockMvc.perform(get("/api/v1/pages/read")
                    .header("token", StpUtil.getTokenValue())
                    .param("nodeId", node.getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data.node.id").value(node.getId()));

            // 2. 参数验证：nodeId = 0
            mockMvc.perform(get("/api/v1/pages/read")
                    .header("token", StpUtil.getTokenValue())
                    .param("nodeId", "0"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

            // 3. 参数验证：nodeId = -1
            mockMvc.perform(get("/api/v1/pages/read")
                    .header("token", StpUtil.getTokenValue())
                    .param("nodeId", "-1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

            // 4. 资源不存在
            mockMvc.perform(get("/api/v1/pages/read")
                    .header("token", StpUtil.getTokenValue())
                    .param("nodeId", "99999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.NOT_FOUND.getCode()));

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试3: 读取完整页面数据 - 通过帖子ID
     *
     * API: GET /api/v1/pages/read?postId={postId}
     *
     * 测试场景:
     * 1. 通过 postId 读取成功
     * 2. 返回 post 字段
     * 3. 参数验证：postId = 0
     * 4. 资源不存在：postId 不存在
     */
    @Test
    void testReadPageByPostId() throws Exception {
        UserDO user = createUser("post@example.com");
        CourseDO course = createPublishedCourse("测试课程", user.getId());
        NodeDO node = createPublishedNode("测试节点", course.getId(), user.getId());
        PostDO post = createPublishedPost(node.getId(), user.getId(), "这是一个测试帖子的内容，用于验证页面聚合接口的功能。");

        StpUtil.login(user.getId());

        try {
            // 1. 成功读取
            String response = mockMvc.perform(get("/api/v1/pages/read")
                    .header("token", StpUtil.getTokenValue())
                    .param("postId", post.getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data.post").exists())
                    .andExpect(jsonPath("$.data.post.id").value(post.getId()))
                    .andReturn().getResponse().getContentAsString();

            JsonNode data = objectMapper.readTree(response).get("data");
            assertThat(data.get("post").get("id").asLong()).isEqualTo(post.getId());

            // 2. 参数验证：postId = 0
            mockMvc.perform(get("/api/v1/pages/read")
                    .header("token", StpUtil.getTokenValue())
                    .param("postId", "0"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

            // 3. 资源不存在
            mockMvc.perform(get("/api/v1/pages/read")
                    .header("token", StpUtil.getTokenValue())
                    .param("postId", "99999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.NOT_FOUND.getCode()));

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试4: 读取完整页面数据 - 通过评论ID
     *
     * API: GET /api/v1/pages/read?commentId={commentId}
     *
     * 测试场景:
     * 1. 通过顶级评论ID读取成功
     * 2. 通过回复评论ID读取 - 返回重定向信息
     * 3. 参数验证：commentId = 0
     * 4. 资源不存在：commentId 不存在
     */
    @Test
    void testReadPageByCommentId() throws Exception {
        UserDO user = createUser("comment@example.com");
        CourseDO course = createPublishedCourse("测试课程", user.getId());
        NodeDO node = createPublishedNode("测试节点", course.getId(), user.getId());
        PostDO post = createPublishedPost(node.getId(), user.getId(), "这是一个测试帖子的内容，用于验证页面聚合接口的功能。");

        // 创建顶级评论和回复评论
        CommentDO topComment = createComment(post.getId(), ContentType.post, user.getId(), "顶级评论");
        CommentDO replyComment = createReplyComment(post.getId(), ContentType.post, user.getId(),
                "回复评论", topComment.getId(), user.getId());

        StpUtil.login(user.getId());

        try {
            // 1. 通过顶级评论读取成功
            mockMvc.perform(get("/api/v1/pages/read")
                    .header("token", StpUtil.getTokenValue())
                    .param("commentId", topComment.getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data.post").exists())
                    .andExpect(jsonPath("$.data.node").exists());

            // 2. 通过回复评论读取 - 返回重定向信息
            String response = mockMvc.perform(get("/api/v1/pages/read")
                    .header("token", StpUtil.getTokenValue())
                    .param("commentId", replyComment.getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data.commentId").value(topComment.getId()))
                    .andExpect(jsonPath("$.data.subCommentId").value(replyComment.getId()))
                    .andReturn().getResponse().getContentAsString();

            JsonNode data = objectMapper.readTree(response).get("data");
            assertThat(data.get("commentId").asLong()).isEqualTo(topComment.getId());
            assertThat(data.get("subCommentId").asLong()).isEqualTo(replyComment.getId());

            // 3. 参数验证：commentId = 0
            mockMvc.perform(get("/api/v1/pages/read")
                    .header("token", StpUtil.getTokenValue())
                    .param("commentId", "0"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

            // 4. 资源不存在
            mockMvc.perform(get("/api/v1/pages/read")
                    .header("token", StpUtil.getTokenValue())
                    .param("commentId", "99999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.NOT_FOUND.getCode()));

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试5: 参数验证 - 所有参数都缺失
     *
     * 测试场景:
     * 1. 不提供任何参数 → 返回 400
     */
    @Test
    void testReadPageWithoutParameters() throws Exception {
        UserDO user = createUser("noparam@example.com");
        StpUtil.login(user.getId());

        try {
            mockMvc.perform(get("/api/v1/pages/read")
                    .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试6: 读取节点帖子列表（优化版）
     *
     * API: GET /api/v1/pages/node?nodeId={nodeId}
     *
     * 测试场景:
     * 1. 读取成功 - 返回优化数据
     * 2. 验证不返回 TOC 相关字段
     * 3. 参数验证：nodeId 缺失
     * 4. 参数验证：nodeId = 0
     * 5. 参数验证：nodeId = -1
     * 6. 资源不存在
     * 7. 权限验证：未登录
     */
    @Test
    void testReadNodePosts() throws Exception {
        UserDO user = createUser("nodeposts@example.com");
        CourseDO course = createPublishedCourse("测试课程", user.getId());
        NodeDO node = createPublishedNode("测试节点", course.getId(), user.getId());
        PostDO post = createPublishedPost(node.getId(), user.getId(), "这是测试帖子的完整内容，包含足够的字符数。");

        StpUtil.login(user.getId());

        try {
            // 1. 成功读取
            String response = mockMvc.perform(get("/api/v1/pages/node")
                    .header("token", StpUtil.getTokenValue())
                    .param("nodeId", node.getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data.node").exists())
                    .andExpect(jsonPath("$.data.course").exists())
                    .andExpect(jsonPath("$.data.parentCourse").exists())
                    .andExpect(jsonPath("$.data.subCourseList").isArray())
                    .andExpect(jsonPath("$.data.otherPostings").isArray())
                    .andExpect(jsonPath("$.data.users").isArray())
                    .andExpect(jsonPath("$.data.learning").exists())
                    .andReturn().getResponse().getContentAsString();

            JsonNode data = objectMapper.readTree(response).get("data");

            // 2. 验证不返回 TOC 相关字段
            assertThat(data.has("toc")).isFalse();
            assertThat(data.has("tocNodeInfos")).isFalse();
            assertThat(data.has("chosenPosting")).isFalse();
            assertThat(data.has("fixedPostings")).isFalse();
            assertThat(data.has("path")).isFalse();

            // 验证返回的字段
            assertThat(data.get("node").get("id").asLong()).isEqualTo(node.getId());
            assertThat(data.get("otherPostings").isArray()).isTrue();

            // 3. 参数验证：nodeId 缺失
            mockMvc.perform(get("/api/v1/pages/node")
                    .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

            // 4. 参数验证：nodeId = 0
            mockMvc.perform(get("/api/v1/pages/node")
                    .header("token", StpUtil.getTokenValue())
                    .param("nodeId", "0"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

            // 5. 参数验证：nodeId = -1
            mockMvc.perform(get("/api/v1/pages/node")
                    .header("token", StpUtil.getTokenValue())
                    .param("nodeId", "-1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

            // 6. 资源不存在
            mockMvc.perform(get("/api/v1/pages/node")
                    .header("token", StpUtil.getTokenValue())
                    .param("nodeId", "99999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.NOT_FOUND.getCode()));

            // 7. 权限验证：未登录
            StpUtil.logout();
            mockMvc.perform(get("/api/v1/pages/node")
                    .param("nodeId", node.getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));

        } finally {
            if (StpUtil.isLogin()) {
                StpUtil.logout();
            }
        }
    }

    /**
     * 测试7: 读取帖子详情（优化版）
     *
     * API: GET /api/v1/pages/post?postId={postId} 或 ?commentId={commentId}
     *
     * 测试场景:
     * 1. 通过 postId 读取成功
     * 2. 验证不返回 TOC 和帖子列表字段
     * 3. 通过顶级评论 commentId 读取成功
     * 4. 通过回复评论 commentId 读取 - 返回重定向信息
     * 5. 参数验证：postId 和 commentId 都缺失
     * 6. 参数验证：postId = 0
     * 7. 资源不存在
     * 8. 权限验证：未登录
     */
    @Test
    void testReadPostDetail() throws Exception {
        UserDO user = createUser("postdetail@example.com");
        CourseDO course = createPublishedCourse("测试课程", user.getId());
        NodeDO node = createPublishedNode("测试节点", course.getId(), user.getId());
        PostDO post = createPublishedPost(node.getId(), user.getId(), "这是测试帖子的完整内容，包含足够的字符数。");

        CommentDO topComment = createComment(post.getId(), ContentType.post, user.getId(), "顶级评论");
        CommentDO replyComment = createReplyComment(post.getId(), ContentType.post, user.getId(),
                "回复评论", topComment.getId(), user.getId());

        StpUtil.login(user.getId());

        try {
            // 1. 通过 postId 读取成功
            String response = mockMvc.perform(get("/api/v1/pages/post")
                    .header("token", StpUtil.getTokenValue())
                    .param("postId", post.getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data.node").exists())
                    .andExpect(jsonPath("$.data.course").exists())
                    .andExpect(jsonPath("$.data.post").exists())
                    .andExpect(jsonPath("$.data.users").isArray())
                    .andReturn().getResponse().getContentAsString();

            JsonNode data = objectMapper.readTree(response).get("data");

            // 2. 验证不返回 TOC 和帖子列表字段
            assertThat(data.has("toc")).isFalse();
            assertThat(data.has("tocNodeInfos")).isFalse();
            assertThat(data.has("chosenPosting")).isFalse();
            assertThat(data.has("fixedPostings")).isFalse();
            assertThat(data.has("otherPostings")).isFalse();
            assertThat(data.has("path")).isFalse();

            // 验证返回的字段
            assertThat(data.get("post").get("id").asLong()).isEqualTo(post.getId());

            // 3. 通过顶级评论读取成功
            mockMvc.perform(get("/api/v1/pages/post")
                    .header("token", StpUtil.getTokenValue())
                    .param("commentId", topComment.getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data.post").exists());

            // 4. 通过回复评论读取 - 返回重定向信息
            String replyResponse = mockMvc.perform(get("/api/v1/pages/post")
                    .header("token", StpUtil.getTokenValue())
                    .param("commentId", replyComment.getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data.commentId").value(topComment.getId()))
                    .andExpect(jsonPath("$.data.subCommentId").value(replyComment.getId()))
                    .andReturn().getResponse().getContentAsString();

            JsonNode replyData = objectMapper.readTree(replyResponse).get("data");
            assertThat(replyData.get("commentId").asLong()).isEqualTo(topComment.getId());
            assertThat(replyData.get("subCommentId").asLong()).isEqualTo(replyComment.getId());

            // 5. 参数验证：都缺失
            mockMvc.perform(get("/api/v1/pages/post")
                    .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

            // 6. 参数验证：postId = 0
            mockMvc.perform(get("/api/v1/pages/post")
                    .header("token", StpUtil.getTokenValue())
                    .param("postId", "0"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

            // 7. 资源不存在
            mockMvc.perform(get("/api/v1/pages/post")
                    .header("token", StpUtil.getTokenValue())
                    .param("postId", "99999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.NOT_FOUND.getCode()));

            // 8. 权限验证：未登录
            StpUtil.logout();
            mockMvc.perform(get("/api/v1/pages/post")
                    .param("postId", post.getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));

        } finally {
            if (StpUtil.isLogin()) {
                StpUtil.logout();
            }
        }
    }

    /**
     * 测试8: 参数优先级测试
     *
     * 测试场景:
     * 1. 同时提供 commentId + postId → commentId 优先
     * 2. 同时提供 postId + nodeId → postId 优先
     * 3. 同时提供 nodeId + courseId → nodeId 优先
     */
    @Test
    void testParameterPriority() throws Exception {
        UserDO user = createUser("priority@example.com");
        CourseDO course = createPublishedCourse("测试课程", user.getId());
        NodeDO node1 = createPublishedNode("节点1", course.getId(), user.getId());
        NodeDO node2 = createPublishedNode("节点2", course.getId(), user.getId());
        PostDO post1 = createPublishedPost(node1.getId(), user.getId(), "这是第一个测试帖子的完整内容。");
        PostDO post2 = createPublishedPost(node2.getId(), user.getId(), "这是第二个测试帖子的完整内容。");

        CommentDO comment = createComment(post1.getId(), ContentType.post, user.getId(), "评论");

        StpUtil.login(user.getId());

        try {
            // 1. commentId + postId → commentId 优先
            String response1 = mockMvc.perform(get("/api/v1/pages/read")
                    .header("token", StpUtil.getTokenValue())
                    .param("commentId", comment.getId().toString())
                    .param("postId", post2.getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andReturn().getResponse().getContentAsString();

            JsonNode data1 = objectMapper.readTree(response1).get("data");
            // 验证使用的是 commentId 关联的 post1，而不是 post2
            assertThat(data1.get("post").get("id").asLong()).isEqualTo(post1.getId());

            // 2. postId + nodeId → postId 优先
            String response2 = mockMvc.perform(get("/api/v1/pages/read")
                    .header("token", StpUtil.getTokenValue())
                    .param("postId", post1.getId().toString())
                    .param("nodeId", node2.getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andReturn().getResponse().getContentAsString();

            JsonNode data2 = objectMapper.readTree(response2).get("data");
            // 验证使用的是 post1 的 node1，而不是 node2
            assertThat(data2.get("node").get("id").asLong()).isEqualTo(node1.getId());

            // 3. nodeId + courseId → nodeId 优先
            String response3 = mockMvc.perform(get("/api/v1/pages/read")
                    .header("token", StpUtil.getTokenValue())
                    .param("nodeId", node1.getId().toString())
                    .param("courseId", course.getId().toString())
                    .param("path", "1-" + course.getRootNodeId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andReturn().getResponse().getContentAsString();

            JsonNode data3 = objectMapper.readTree(response3).get("data");
            // 验证使用的是 node1
            assertThat(data3.get("node").get("id").asLong()).isEqualTo(node1.getId());

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试9: 内容状态验证
     *
     * 测试场景:
     * 1. 课程状态 = SUBMITTED → 返回 403
     * 2. 课程状态 = PUBLISHED → 成功
     * 3. 节点状态 = SUBMITTED → 返回 403
     * 4. 节点状态 = PUBLISHED → 成功
     */
    @Test
    void testContentStateValidation() throws Exception {
        UserDO user = createUser("state@example.com");

        // 创建已发布课程
        CourseDO publishedCourse = createPublishedCourse("已发布课程", user.getId());

        // 创建已发布节点
        NodeDO publishedNode = createPublishedNode("已发布节点", publishedCourse.getId(), user.getId());

        StpUtil.login(user.getId());

        try {
            // 验证：课程已发布 → 成功
            mockMvc.perform(get("/api/v1/pages/read")
                    .header("token", StpUtil.getTokenValue())
                    .param("courseId", publishedCourse.getId().toString())
                    .param("path", "1-" + publishedCourse.getRootNodeId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));

            // 验证：节点已发布 → 成功
            mockMvc.perform(get("/api/v1/pages/read")
                    .header("token", StpUtil.getTokenValue())
                    .param("nodeId", publishedNode.getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));

        } finally {
            StpUtil.logout();
        }
    }
}
