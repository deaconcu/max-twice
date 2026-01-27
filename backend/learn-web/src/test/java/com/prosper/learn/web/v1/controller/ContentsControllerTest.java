package com.prosper.learn.web.v1.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prosper.learn.content.course.CourseDO;
import com.prosper.learn.content.course.CourseDataService;
import com.prosper.learn.content.node.NodeDO;
import com.prosper.learn.content.node.NodeDataService;
import com.prosper.learn.content.post.PostDO;
import com.prosper.learn.content.post.PostDataService;
import com.prosper.learn.content.toc.NodeTocDO;
import com.prosper.learn.content.toc.NodeTocDataService;
import com.prosper.learn.content.toc.UserNodeTocDO;
import com.prosper.learn.content.toc.UserNodeTocDataService;
import com.prosper.learn.shared.common.utils.Utils;
import com.prosper.learn.shared.domain.Enums.ContentState;
import com.prosper.learn.shared.domain.Enums.PostType;
import com.prosper.learn.shared.domain.exception.StatusCode;
import com.prosper.learn.user.profile.UserDO;
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
 * 内容管理接口测试
 * 测试文档: docs/test/contents.md
 *
 * Command 测试 - 写操作
 */
@Transactional
public class ContentsControllerTest extends BaseControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserDomainService userDomainService;

    @Autowired
    private CourseDataService courseDataService;

    @Autowired
    private NodeDataService nodeDataService;

    @Autowired
    private PostDataService postDataService;

    @Autowired
    private UserNodeTocDataService userNodeTocDataService;

    @Autowired
    private NodeTocDataService nodeTocDataService;

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
     * 创建已发布课程
     */
    private CourseDO createPublishedCourse(String name, Long creatorId) {
        CourseDO course = new CourseDO();
        course.setName(name);
        course.setDescription("这是一个完整的课程描述，用于测试内容管理接口的功能。");
        course.setCreatorId(creatorId);
        course.setState(ContentState.PUBLISHED.value());
        course.setMainCategory(1);
        course.setSubCategory(1);
        course.setRootNodeId(0L);
        course.setParentCourseId(0L);
        courseDataService.insert(course);

        // 创建根节点
        NodeDO rootNode = new NodeDO(creatorId, course.getId(), course.getName(),
                course.getDescription(), ContentState.PUBLISHED.value(), (byte)1);
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
     * 创建内容型帖子（目录型）
     */
    private PostDO createIndexPost(String content, Long nodeId, Long creatorId) {
        PostDO post = new PostDO();
        post.setContent(content);
        post.setNodeId(nodeId);
        post.setCreatorId(creatorId);
        post.setType(PostType.index.value());
        post.setState(ContentState.PUBLISHED.value());
        post.setScore(0.0);
        postDataService.insert(post);
        return post;
    }

    /**
     * 创建已发布的普通帖子
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
     * 初始化用户课程目录
     * 创建一个空的目录结构
     */
    private UserNodeTocDO initUserNodeToc(Long userId, Long courseId) {
        // 创建一个空的目录 JSON
        String emptyTocJson = "{}";
        String tocHash = Utils.hashSHA(emptyTocJson);

        // 创建 CourseTocDO
        NodeTocDO courseToc = nodeTocDataService.get(tocHash);
        if (courseToc == null) {
            courseToc = new NodeTocDO();
            courseToc.setHash(tocHash);
            courseToc.setToc(emptyTocJson);
            courseToc.setRefCount(0);
            nodeTocDataService.insert(courseToc);
        }

        // 创建 UserNodeTocDO
        UserNodeTocDO userNodeToc = new UserNodeTocDO();
        userNodeToc.setUserId(userId);
        userNodeToc.setNodeId(courseId);
        userNodeToc.setToc(tocHash);
        userNodeTocDataService.insert(userNodeToc);

        return userNodeToc;
    }

    // ==================== Command 测试（写操作） ====================

    /**
     * 测试1: 选择目录 - 成功选择
     */
    @Test
    @DisplayName("选择目录 - 成功")
    void testChooseContents_Success() throws Exception {
        UserDO user = createUser("user@example.com");
        CourseDO course = createPublishedCourse("测试课程", user.getId());
        NodeDO node1 = createPublishedNode("节点1", course.getId(), user.getId());
        NodeDO node2 = createPublishedNode("节点2", course.getId(), user.getId());

        // 创建内容型帖子，内容是节点ID列表
        String tocContent = node1.getId() + "," + node2.getId();
        PostDO contentsPost = createIndexPost(tocContent, course.getRootNodeId(), user.getId());

        // 初始化用户目录
        initUserNodeToc(user.getId(), course.getId());

        StpUtil.login(user.getId());

        try {
            String requestBody = String.format("""
                {
                    "path": "1-%d",
                    "courseId": %d,
                    "postingId": %d,
                    "action": 1
                }
                """, course.getRootNodeId(), course.getId(), contentsPost.getId());

            mockMvc.perform(post("/api/v1/contents")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));

            // 验证用户目录已更新
            UserNodeTocDO updatedToc = userNodeTocDataService.getByUserAndNode(user.getId(), course.getId());
            assertThat(updatedToc).isNotNull();
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试2: 选择目录 - path 为空
     */
    @Test
    @DisplayName("选择目录 - path 为空")
    void testChooseContents_PathEmpty() throws Exception {
        UserDO user = createUser("user@example.com");
        StpUtil.login(user.getId());

        try {
            String requestBody = """
                {
                    "path": "",
                    "courseId": 123,
                    "postingId": 456,
                    "action": 1
                }
                """;

            mockMvc.perform(post("/api/v1/contents")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试3: 选择目录 - courseId 为 null
     */
    @Test
    @DisplayName("选择目录 - courseId 为 null")
    void testChooseContents_CourseIdNull() throws Exception {
        UserDO user = createUser("user@example.com");
        StpUtil.login(user.getId());

        try {
            String requestBody = """
                {
                    "path": "1-123",
                    "postingId": 456,
                    "action": 1
                }
                """;

            mockMvc.perform(post("/api/v1/contents")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试4: 选择目录 - courseId = 0
     */
    @Test
    @DisplayName("选择目录 - courseId = 0")
    void testChooseContents_CourseIdZero() throws Exception {
        UserDO user = createUser("user@example.com");
        StpUtil.login(user.getId());

        try {
            String requestBody = """
                {
                    "path": "1-123",
                    "courseId": 0,
                    "postingId": 456,
                    "action": 1
                }
                """;

            mockMvc.perform(post("/api/v1/contents")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试5: 选择目录 - courseId = -1
     */
    @Test
    @DisplayName("选择目录 - courseId = -1")
    void testChooseContents_CourseIdNegative() throws Exception {
        UserDO user = createUser("user@example.com");
        StpUtil.login(user.getId());

        try {
            String requestBody = """
                {
                    "path": "1-123",
                    "courseId": -1,
                    "postingId": 456,
                    "action": 1
                }
                """;

            mockMvc.perform(post("/api/v1/contents")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试6: 选择目录 - postingId 为 null
     */
    @Test
    @DisplayName("选择目录 - postingId 为 null")
    void testChooseContents_PostingIdNull() throws Exception {
        UserDO user = createUser("user@example.com");
        StpUtil.login(user.getId());

        try {
            String requestBody = """
                {
                    "path": "1-123",
                    "courseId": 456,
                    "action": 1
                }
                """;

            mockMvc.perform(post("/api/v1/contents")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试7: 选择目录 - postingId = 0
     */
    @Test
    @DisplayName("选择目录 - postingId = 0")
    void testChooseContents_PostingIdZero() throws Exception {
        UserDO user = createUser("user@example.com");
        StpUtil.login(user.getId());

        try {
            String requestBody = """
                {
                    "path": "1-123",
                    "courseId": 456,
                    "postingId": 0,
                    "action": 1
                }
                """;

            mockMvc.perform(post("/api/v1/contents")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试8: 选择目录 - action 为 null
     */
    @Test
    @DisplayName("选择目录 - action 为 null")
    void testChooseContents_ActionNull() throws Exception {
        UserDO user = createUser("user@example.com");
        StpUtil.login(user.getId());

        try {
            String requestBody = """
                {
                    "path": "1-123",
                    "courseId": 456,
                    "postingId": 789
                }
                """;

            mockMvc.perform(post("/api/v1/contents")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试9: 选择目录 - action = 0
     */
    @Test
    @DisplayName("选择目录 - action = 0")
    void testChooseContents_ActionZero() throws Exception {
        UserDO user = createUser("user@example.com");
        StpUtil.login(user.getId());

        try {
            String requestBody = """
                {
                    "path": "1-123",
                    "courseId": 456,
                    "postingId": 789,
                    "action": 0
                }
                """;

            mockMvc.perform(post("/api/v1/contents")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试10: 选择目录 - action = 5（超出范围）
     */
    @Test
    @DisplayName("选择目录 - action = 5")
    void testChooseContents_ActionOutOfRange() throws Exception {
        UserDO user = createUser("user@example.com");
        StpUtil.login(user.getId());

        try {
            String requestBody = """
                {
                    "path": "1-123",
                    "courseId": 456,
                    "postingId": 789,
                    "action": 5
                }
                """;

            mockMvc.perform(post("/api/v1/contents")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试11: 选择目录 - 课程不存在
     */
    @Test
    @DisplayName("选择目录 - 课程不存在")
    void testChooseContents_CourseNotFound() throws Exception {
        UserDO user = createUser("user@example.com");
        StpUtil.login(user.getId());

        try {
            String requestBody = """
                {
                    "path": "1-123",
                    "courseId": 99999,
                    "postingId": 789,
                    "action": 1
                }
                """;

            mockMvc.perform(post("/api/v1/contents")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.COURSE_NOT_FOUND.getCode()));
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试12: 选择目录 - 帖子不存在
     */
    @Test
    @DisplayName("选择目录 - 帖子不存在")
    void testChooseContents_PostNotFound() throws Exception {
        UserDO user = createUser("user@example.com");
        CourseDO course = createPublishedCourse("测试课程", user.getId());
        initUserNodeToc(user.getId(), course.getId());

        StpUtil.login(user.getId());

        try {
            String requestBody = String.format("""
                {
                    "path": "1-%d",
                    "courseId": %d,
                    "postingId": 99999,
                    "action": 1
                }
                """, course.getRootNodeId(), course.getId());

            mockMvc.perform(post("/api/v1/contents")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.POST_NOT_FOUND.getCode()));
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试13: 选择目录 - 用户目录不存在
     */
    @Test
    @DisplayName("选择目录 - 用户目录不存在")
    void testChooseContents_UserTocNotFound() throws Exception {
        UserDO user = createUser("user@example.com");
        CourseDO course = createPublishedCourse("测试课程", user.getId());
        NodeDO node = createPublishedNode("节点1", course.getId(), user.getId());
        PostDO contentsPost = createIndexPost(node.getId().toString(), course.getRootNodeId(), user.getId());

        // 不初始化用户目录

        StpUtil.login(user.getId());

        try {
            String requestBody = String.format("""
                {
                    "path": "1-%d",
                    "courseId": %d,
                    "postingId": %d,
                    "action": 1
                }
                """, course.getRootNodeId(), course.getId(), contentsPost.getId());

            mockMvc.perform(post("/api/v1/contents")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.TOC_USER_TOC_NOT_FOUND.getCode()));
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试14: 选择目录 - 未登录
     */
    @Test
    @DisplayName("选择目录 - 未登录")
    void testChooseContents_NotLoggedIn() throws Exception {
        String requestBody = """
            {
                "path": "1-123",
                "courseId": 456,
                "postingId": 789,
                "action": 1
            }
            """;

        mockMvc.perform(post("/api/v1/contents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));
    }

    /**
     * 测试15: 取消选择 - 成功取消
     */
    @Test
    @DisplayName("取消选择 - 成功")
    void testUnchooseContents_Success() throws Exception {
        UserDO user = createUser("user@example.com");
        CourseDO course = createPublishedCourse("测试课程", user.getId());
        NodeDO node = createPublishedNode("节点1", course.getId(), user.getId());
        PostDO contentsPost = createIndexPost(node.getId().toString(), course.getRootNodeId(), user.getId());
        initUserNodeToc(user.getId(), course.getId());

        StpUtil.login(user.getId());

        try {
            // 先选择目录
            String chooseRequest = String.format("""
                {
                    "path": "1-%d",
                    "courseId": %d,
                    "postingId": %d,
                    "action": 1
                }
                """, course.getRootNodeId(), course.getId(), contentsPost.getId());

            mockMvc.perform(post("/api/v1/contents")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(chooseRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));

            // 再取消选择
            String unchooseRequest = String.format("""
                {
                    "path": "1-%d",
                    "courseId": %d,
                    "postingId": %d,
                    "action": 2
                }
                """, course.getRootNodeId(), course.getId(), contentsPost.getId());

            mockMvc.perform(post("/api/v1/contents")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(unchooseRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));

            // 验证目录已更新
            UserNodeTocDO updatedToc = userNodeTocDataService.getByUserAndNode(user.getId(), course.getId());
            assertThat(updatedToc).isNotNull();
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试16: 取消选择 - 幂等性测试
     */
    @Test
    @DisplayName("取消选择 - 幂等性")
    void testUnchooseContents_Idempotent() throws Exception {
        UserDO user = createUser("user@example.com");
        CourseDO course = createPublishedCourse("测试课程", user.getId());
        initUserNodeToc(user.getId(), course.getId());

        StpUtil.login(user.getId());

        try {
            // 直接取消选择（没有先选择）
            String requestBody = String.format("""
                {
                    "path": "1-%d",
                    "courseId": %d,
                    "postingId": 999,
                    "action": 2
                }
                """, course.getRootNodeId(), course.getId());

            mockMvc.perform(post("/api/v1/contents")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试17: 固定帖子 - 成功固定
     */
    @Test
    @DisplayName("固定帖子 - 成功")
    void testPinPost_Success() throws Exception {
        UserDO user = createUser("user@example.com");
        CourseDO course = createPublishedCourse("测试课程", user.getId());
        NodeDO node = createPublishedNode("节点1", course.getId(), user.getId());
        PostDO post = createPublishedPost("测试帖子内容", node.getId(), user.getId());
        initUserNodeToc(user.getId(), course.getId());

        StpUtil.login(user.getId());

        try {
            String requestBody = String.format("""
                {
                    "path": "1-%d",
                    "courseId": %d,
                    "postingId": %d,
                    "action": 3
                }
                """, course.getRootNodeId(), course.getId(), post.getId());

            mockMvc.perform(post("/api/v1/contents")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));

            // 验证目录已更新
            UserNodeTocDO updatedToc = userNodeTocDataService.getByUserAndNode(user.getId(), course.getId());
            assertThat(updatedToc).isNotNull();
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试18: 固定帖子 - 固定多个帖子
     */
    @Test
    @DisplayName("固定帖子 - 固定多个")
    void testPinPost_Multiple() throws Exception {
        UserDO user = createUser("user@example.com");
        CourseDO course = createPublishedCourse("测试课程", user.getId());
        NodeDO node = createPublishedNode("节点1", course.getId(), user.getId());
        PostDO post1 = createPublishedPost("帖子1", node.getId(), user.getId());
        PostDO post2 = createPublishedPost("帖子2", node.getId(), user.getId());
        PostDO post3 = createPublishedPost("帖子3", node.getId(), user.getId());
        initUserNodeToc(user.getId(), course.getId());

        StpUtil.login(user.getId());

        try {
            // 固定帖子1
            String request1 = String.format("""
                {
                    "path": "1-%d",
                    "courseId": %d,
                    "postingId": %d,
                    "action": 3
                }
                """, course.getRootNodeId(), course.getId(), post1.getId());

            mockMvc.perform(post("/api/v1/contents")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request1))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));

            // 固定帖子2
            String request2 = String.format("""
                {
                    "path": "1-%d",
                    "courseId": %d,
                    "postingId": %d,
                    "action": 3
                }
                """, course.getRootNodeId(), course.getId(), post2.getId());

            mockMvc.perform(post("/api/v1/contents")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request2))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));

            // 固定帖子3
            String request3 = String.format("""
                {
                    "path": "1-%d",
                    "courseId": %d,
                    "postingId": %d,
                    "action": 3
                }
                """, course.getRootNodeId(), course.getId(), post3.getId());

            mockMvc.perform(post("/api/v1/contents")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request3))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));

            // 验证目录已更新
            UserNodeTocDO updatedToc = userNodeTocDataService.getByUserAndNode(user.getId(), course.getId());
            assertThat(updatedToc).isNotNull();
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试19: 固定帖子 - 幂等性测试
     */
    @Test
    @DisplayName("固定帖子 - 幂等性")
    void testPinPost_Idempotent() throws Exception {
        UserDO user = createUser("user@example.com");
        CourseDO course = createPublishedCourse("测试课程", user.getId());
        PostDO post = createPublishedPost("测试帖子", course.getRootNodeId(), user.getId());
        initUserNodeToc(user.getId(), course.getId());

        StpUtil.login(user.getId());

        try {
            String requestBody = String.format("""
                {
                    "path": "1-%d",
                    "courseId": %d,
                    "postingId": %d,
                    "action": 3
                }
                """, course.getRootNodeId(), course.getId(), post.getId());

            // 第一次固定
            mockMvc.perform(post("/api/v1/contents")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));

            // 第二次固定同一帖子（幂等性）
            mockMvc.perform(post("/api/v1/contents")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试20: 取消固定 - 成功取消
     */
    @Test
    @DisplayName("取消固定 - 成功")
    void testUnpinPost_Success() throws Exception {
        UserDO user = createUser("user@example.com");
        CourseDO course = createPublishedCourse("测试课程", user.getId());
        PostDO post = createPublishedPost("测试帖子", course.getRootNodeId(), user.getId());
        initUserNodeToc(user.getId(), course.getId());

        StpUtil.login(user.getId());

        try {
            // 先固定帖子
            String pinRequest = String.format("""
                {
                    "path": "1-%d",
                    "courseId": %d,
                    "postingId": %d,
                    "action": 3
                }
                """, course.getRootNodeId(), course.getId(), post.getId());

            mockMvc.perform(post("/api/v1/contents")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(pinRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));

            // 再取消固定
            String unpinRequest = String.format("""
                {
                    "path": "1-%d",
                    "courseId": %d,
                    "postingId": %d,
                    "action": 4
                }
                """, course.getRootNodeId(), course.getId(), post.getId());

            mockMvc.perform(post("/api/v1/contents")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(unpinRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));

            // 验证目录已更新
            UserNodeTocDO updatedToc = userNodeTocDataService.getByUserAndNode(user.getId(), course.getId());
            assertThat(updatedToc).isNotNull();
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试21: 取消固定 - 从多个固定中移除一个
     */
    @Test
    @DisplayName("取消固定 - 从多个固定中移除一个")
    void testUnpinPost_RemoveOneFromMultiple() throws Exception {
        UserDO user = createUser("user@example.com");
        CourseDO course = createPublishedCourse("测试课程", user.getId());
        PostDO post1 = createPublishedPost("帖子1", course.getRootNodeId(), user.getId());
        PostDO post2 = createPublishedPost("帖子2", course.getRootNodeId(), user.getId());
        PostDO post3 = createPublishedPost("帖子3", course.getRootNodeId(), user.getId());
        initUserNodeToc(user.getId(), course.getId());

        StpUtil.login(user.getId());

        try {
            // 固定3个帖子
            for (PostDO post : new PostDO[]{post1, post2, post3}) {
                String pinRequest = String.format("""
                    {
                        "path": "1-%d",
                        "courseId": %d,
                        "postingId": %d,
                        "action": 3
                    }
                    """, course.getRootNodeId(), course.getId(), post.getId());

                mockMvc.perform(post("/api/v1/contents")
                        .header("token", StpUtil.getTokenValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pinRequest))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));
            }

            // 取消固定帖子2
            String unpinRequest = String.format("""
                {
                    "path": "1-%d",
                    "courseId": %d,
                    "postingId": %d,
                    "action": 4
                }
                """, course.getRootNodeId(), course.getId(), post2.getId());

            mockMvc.perform(post("/api/v1/contents")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(unpinRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));

            // 验证目录已更新
            UserNodeTocDO updatedToc = userNodeTocDataService.getByUserAndNode(user.getId(), course.getId());
            assertThat(updatedToc).isNotNull();
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试22: 取消固定 - 幂等性测试
     */
    @Test
    @DisplayName("取消固定 - 幂等性")
    void testUnpinPost_Idempotent() throws Exception {
        UserDO user = createUser("user@example.com");
        CourseDO course = createPublishedCourse("测试课程", user.getId());
        initUserNodeToc(user.getId(), course.getId());

        StpUtil.login(user.getId());

        try {
            // 直接取消固定（没有先固定）
            String requestBody = String.format("""
                {
                    "path": "1-%d",
                    "courseId": %d,
                    "postingId": 999,
                    "action": 4
                }
                """, course.getRootNodeId(), course.getId());

            mockMvc.perform(post("/api/v1/contents")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试23: 参数验证 - 所有必填字段都缺失
     */
    @Test
    @DisplayName("参数验证 - 所有字段缺失")
    void testContents_AllFieldsMissing() throws Exception {
        UserDO user = createUser("user@example.com");
        StpUtil.login(user.getId());

        try {
            String requestBody = "{}";

            mockMvc.perform(post("/api/v1/contents")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
        } finally {
            StpUtil.logout();
        }
    }
}
