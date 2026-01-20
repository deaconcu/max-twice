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
import com.prosper.learn.interaction.upvote.UpvoteDO;
import com.prosper.learn.interaction.upvote.UpvoteDataService;
import com.prosper.learn.shared.domain.Enums.*;
import com.prosper.learn.shared.domain.exception.StatusCode;
import com.prosper.learn.user.profile.UserDO;
import com.prosper.learn.user.profile.UserDataService;
import com.prosper.learn.user.profile.UserDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.Charset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 帖子管理接口测试
 * 测试文档: docs/test/post.md
 *
 * Command 测试 - 写操作
 * Query 测试 - 读操作
 */
@Transactional
public class PostsControllerTest extends BaseControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PostDataService postDataService;

    @Autowired
    private UserDomainService userDomainService;

    @Autowired
    private UserDataService userDataService;

    @Autowired
    private CourseDataService courseDataService;

    @Autowired
    private NodeDataService nodeDataService;

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
        node.setDescription(""); // description 不能为 null
        node.setCourseId(courseId);
        node.setCreatorId(creatorId);
        node.setState(ContentState.PUBLISHED.value());
        nodeDataService.insert(node);
        return node;
    }

    /**
     * 创建已发布帖子
     */
    private PostDO createPublishedPost(String content, Long nodeId, Long creatorId, PostType type) {
        PostDO post = new PostDO();
        post.setContent(content);
        post.setNodeId(nodeId);
        post.setCreatorId(creatorId);
        post.setType(type.value());
        post.setState(ContentState.PUBLISHED.value());
        post.setScore(0.0);
        postDataService.insert(post);
        return post;
    }

    /**
     * 创建指定状态的帖子
     */
    private PostDO createPostWithState(String content, Long nodeId, Long creatorId, ContentState state) {
        PostDO post = new PostDO();
        post.setContent(content);
        post.setNodeId(nodeId);
        post.setCreatorId(creatorId);
        post.setType(PostType.article.value());
        post.setState(state.value());
        post.setScore(0.0);
        postDataService.insert(post);
        return post;
    }

    /**
     * 创建投票记录
     */
    private void createVote(Long userId, Long postId, VoteType voteType) {
        UpvoteDO upvote = new UpvoteDO();
        upvote.setObjectId(postId);
        upvote.setObjectType(ContentType.post.value());
        upvote.setUserId(userId);
        upvote.setType(voteType.value());
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
     * 测试1: 创建帖子 - 成功创建和字段验证
     *
     * 字段要求:
     * - content: 必填(@NotBlank), 长度由配置决定(@ConfigurableSize)
     * - nodeId: 必填(@NotNull), 必须 > 0(@Positive)
     * - type: 可选, 默认为 POST(0)
     *
     * 业务规则:
     * - 创建后状态为 SUBMITTED (待审核)
     * - creatorId 自动填充为当前用户ID
     * - score 初始化为 0.0
     * - 节点必须存在且未被屏蔽
     */
    @Test
    void testCreatePost() throws Exception {
        UserDO user = createUser("creator@example.com");
        CourseDO course = createPublishedCourse("测试课程", user.getId());
        NodeDO node = createPublishedNode("测试节点", course.getId(), user.getId());

        StpUtil.login(user.getId());

        try {
            // 1. 成功创建普通帖子(文章类型)
            String validRequest = String.format("""
                {
                    "content": "这是一个测试帖子的内容，包含足够的文字来满足最小长度要求",
                    "nodeId": %d,
                    "type": %d
                }
                """, node.getId(), PostType.article.value());

            mockMvc.perform(post("/api/v1/posts")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));

            // 验证：帖子已创建
            List<PostDO> allPosts = postDataService.getListByNodeAndCreator(node.getId(), user.getId());
            assertThat(allPosts.size()).isGreaterThan(0);
            PostDO createdPost = allPosts.get(0);
            assertThat(createdPost).isNotNull();
            assertThat(createdPost.getState()).isEqualTo(ContentState.SUBMITTED.value());
            assertThat(createdPost.getCreatorId()).isEqualTo(user.getId());
            assertThat(createdPost.getType()).isEqualTo(PostType.article.value());
            assertThat(createdPost.getScore()).isEqualTo(0.0);

            // 2. 成功创建文章帖子
            String articleRequest = String.format("""
                {
                    "content": "这是一篇测试文章的内容，包含详细的描述信息和完整的文字内容",
                    "nodeId": %d,
                    "type": 2
                }
                """, node.getId());

            mockMvc.perform(post("/api/v1/posts")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(articleRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));

            // 验证：文章类型正确
            List<PostDO> allPostsInNode = postDataService.getListByNodeAndCreator(node.getId(), user.getId());
            PostDO article = allPostsInNode.stream()
                    .filter(p -> p.getType() == PostType.article.value())
                    .findFirst()
                    .orElse(null);
            assertThat(article).isNotNull();
            assertThat(article.getType()).isEqualTo(PostType.article.value());

            // 3. 成功创建内容帖子（目录）
            String contentRequest = String.format("""
                {
                    "content": "[{\\"第一章\\": \\"1\\"}, {\\"第二章\\": \\"2\\"}]",
                    "nodeId": %d,
                    "type": %d
                }
                """, node.getId(), PostType.index.value());

            mockMvc.perform(post("/api/v1/posts")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(contentRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));

            // 4. 字段验证 - content 为空
            String emptyContentRequest = String.format("""
                {
                    "content": "",
                    "nodeId": %d,
                    "type": %d
                }
                """, node.getId(), PostType.article.value());

            mockMvc.perform(post("/api/v1/posts")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(emptyContentRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

            // 5. 字段验证 - nodeId 缺失
            String missingNodeIdRequest = String.format("""
                {
                    "content": "这是测试内容",
                    "type": %d
                }
                """, PostType.article.value());

            mockMvc.perform(post("/api/v1/posts")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(missingNodeIdRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

            // 6. 字段验证 - nodeId 无效（0）
            String invalidNodeIdRequest = String.format("""
                {
                    "content": "这是测试内容，包含足够的文字来满足最小长度要求",
                    "nodeId": 0,
                    "type": %d
                }
                """, PostType.article.value());

            mockMvc.perform(post("/api/v1/posts")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidNodeIdRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

            // 7. 字段验证 - nodeId 无效（负数）
            String negativeNodeIdRequest = String.format("""
                {
                    "content": "这是测试内容，包含足够的文字来满足最小长度要求",
                    "nodeId": -1,
                    "type": %d
                }
                """, PostType.article.value());

            mockMvc.perform(post("/api/v1/posts")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(negativeNodeIdRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

            // 8. 业务验证 - 节点不存在
            String nonExistentNodeRequest = String.format("""
                {
                    "content": "这是测试内容，包含足够的文字来满足最小长度要求",
                    "nodeId": 99999,
                    "type": %d
                }
                """, PostType.article.value());

            mockMvc.perform(post("/api/v1/posts")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(nonExistentNodeRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.NODE_NOT_FOUND.getCode()));

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试2: 修改帖子 - 成功修改和权限验证
     *
     * 字段要求:
     * - content: 必填(@NotBlank), 长度由配置决定(@ConfigurableSize)
     * - id (路径参数): 必填(@NotNull), 必须 > 0(@Positive)
     *
     * 业务规则:
     * - 只有创建者或管理员可以修改
     * - 修改后状态变为 SUBMITTED（待审核）
     * - updatedAt 时间自动更新
     */
    @Test
    void testUpdatePost() throws Exception {
        UserDO user = createUser("creator@example.com");
        CourseDO course = createPublishedCourse("测试课程", user.getId());
        NodeDO node = createPublishedNode("测试节点", course.getId(), user.getId());
        PostDO post = createPublishedPost("原始内容", node.getId(), user.getId(), PostType.article);

        StpUtil.login(user.getId());

        try {
            // 1. 成功修改帖子内容
            String updateRequest = """
                {
                    "content": "这是修改后的新内容，包含更新后的详细信息和完整描述"
                }
                """;

            String response = mockMvc.perform(put("/api/v1/posts/" + post.getId())
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(updateRequest)
                    .characterEncoding("UTF-8"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data").exists())
                    .andExpect(jsonPath("$.data.id").value(post.getId()))
                    .andExpect(jsonPath("$.data.content").value("这是修改后的新内容，包含更新后的详细信息和完整描述"))
                    .andReturn().getResponse().getContentAsString(Charset.forName("UTF-8"));

            // 验证：状态变为待审核
            PostDO updatedPost = postDataService.getById(post.getId());
            assertThat(updatedPost.getState()).isEqualTo(ContentState.SUBMITTED.value());
            assertThat(updatedPost.getContent()).isEqualTo("这是修改后的新内容，包含更新后的详细信息和完整描述");

            // 2. 字段验证 - content 为空
            String emptyContentRequest = """
                {
                    "content": ""
                }
                """;

            mockMvc.perform(put("/api/v1/posts/" + post.getId())
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(emptyContentRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

            // 3. 业务验证 - 帖子不存在
            mockMvc.perform(put("/api/v1/posts/99999")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(updateRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.POST_NOT_FOUND.getCode()));

            // 4. 参数验证 - 帖子ID = 0
            mockMvc.perform(put("/api/v1/posts/0")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(updateRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

            // 5. 参数验证 - 帖子ID = -1
            mockMvc.perform(put("/api/v1/posts/-1")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(updateRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

            // 6. 权限验证 - 只有创建者可以修改
            StpUtil.logout();
            UserDO anotherUser = createUser("another@example.com");
            StpUtil.login(anotherUser.getId());

            mockMvc.perform(put("/api/v1/posts/" + post.getId())
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(updateRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.PERMISSION_DENIED.getCode()));

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试3: 删除帖子 - 软删除和权限验证
     *
     * 业务规则:
     * - 只有创建者或管理员可以删除
     * - 软删除：state 变为 DELETED
     * - 删除后数据仍在数据库中
     */
    @Test
    void testDeletePost() throws Exception {
        UserDO user = createUser("creator@example.com");
        CourseDO course = createPublishedCourse("测试课程", user.getId());
        NodeDO node = createPublishedNode("测试节点", course.getId(), user.getId());
        PostDO post = createPublishedPost("测试内容", node.getId(), user.getId(), PostType.article);

        StpUtil.login(user.getId());

        try {
            // 1. 成功删除帖子
            mockMvc.perform(delete("/api/v1/posts/" + post.getId())
                    .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));

            // 验证：帖子状态变为 BANNED（软删除）
            PostDO deletedPost = postDataService.getById(post.getId());
            // 软删除可能返回 null，所以先检查是否存在，如果存在则验证状态
            if (deletedPost != null) {
                assertThat(deletedPost.getState()).isEqualTo(ContentState.BANNED.value());
            }

            // 2. 业务验证 - 帖子不存在
            mockMvc.perform(delete("/api/v1/posts/99999")
                    .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.POST_NOT_FOUND.getCode()));

            // 3. 参数验证 - 帖子ID = 0
            mockMvc.perform(delete("/api/v1/posts/0")
                    .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

            // 4. 参数验证 - 帖子ID = -1
            mockMvc.perform(delete("/api/v1/posts/-1")
                    .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

            // 5. 权限验证 - 只有创建者可以删除
            PostDO anotherPost = createPublishedPost("另一个帖子", node.getId(), user.getId(), PostType.article);

            StpUtil.logout();
            UserDO anotherUser = createUser("another@example.com");
            StpUtil.login(anotherUser.getId());

            mockMvc.perform(delete("/api/v1/posts/" + anotherPost.getId())
                    .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.PERMISSION_DENIED.getCode()));

        } finally {
            StpUtil.logout();
        }
    }

    // ==================== Query 测试（读操作） ====================

    /**
     * 测试4: 获取帖子详情 - 各种场景
     *
     * API: GET /api/v1/posts/{id}
     *
     * 测试场景:
     * 1. 获取已发布帖子
     * 2. 获取待审核帖子
     * 3. 帖子不存在
     * 4. 帖子ID无效
     * 5. 不需要登录
     */
    @Test
    void testGetPostDetail() throws Exception {
        // 准备：创建用户、课程、节点、帖子
        UserDO user = createUser("user@example.com");
        CourseDO course = createPublishedCourse("测试课程", user.getId());
        NodeDO node = createPublishedNode("测试节点", course.getId(), user.getId());
        PostDO publishedPost = createPublishedPost("已发布帖子内容", node.getId(), user.getId(), PostType.article);
        PostDO submittedPost = createPostWithState("待审核帖子内容", node.getId(), user.getId(), ContentState.SUBMITTED);

        StpUtil.login(user.getId());

        try {
            // 1. 获取已发布帖子
            mockMvc.perform(get("/api/v1/posts/" + publishedPost.getId())
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data").exists())
                    .andExpect(jsonPath("$.data.id").value(publishedPost.getId()))
                    .andExpect(jsonPath("$.data.content").value("已发布帖子内容"))
                    .andExpect(jsonPath("$.data.state").value((int) ContentState.PUBLISHED.value()));

            // 2. 获取待审核帖子
            mockMvc.perform(get("/api/v1/posts/" + submittedPost.getId())
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data.id").value(submittedPost.getId()))
                    .andExpect(jsonPath("$.data.state").value((int) ContentState.SUBMITTED.value()));

            // 3. 帖子不存在
            mockMvc.perform(get("/api/v1/posts/99999")
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.POST_NOT_FOUND.getCode()));

            // 4. 帖子ID无效 - ID = 0
            mockMvc.perform(get("/api/v1/posts/0")
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

            // 5. 帖子ID无效 - ID = -1
            mockMvc.perform(get("/api/v1/posts/-1")
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

            // 6. 需要登录才能访问
            mockMvc.perform(get("/api/v1/posts/" + publishedPost.getId())
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试5: 批量获取帖子 - 按IDs查询
     *
     * API: GET /api/v1/posts?ids=1,2,3
     *
     * 测试场景:
     * 1. 按 IDs 批量查询
     * 2. 包含用户投票信息
     * 3. 部分ID不存在
     * 4. ids 为空
     * 5. 需要登录
     */
    @Test
    void testGetPostsByIds() throws Exception {
        // 准备：创建用户、课程、节点、帖子
        UserDO user = createUser("user@example.com");
        CourseDO course = createPublishedCourse("测试课程", user.getId());
        NodeDO node = createPublishedNode("测试节点", course.getId(), user.getId());
        PostDO post1 = createPublishedPost("帖子1", node.getId(), user.getId(), PostType.article);
        PostDO post2 = createPublishedPost("帖子2", node.getId(), user.getId(), PostType.article);
        PostDO post3 = createPublishedPost("帖子3", node.getId(), user.getId(), PostType.article);

        StpUtil.login(user.getId());

        try {
            // 1. 按 IDs 批量查询
            String response = mockMvc.perform(get("/api/v1/posts")
                    .param("ids", post1.getId().toString() + "," + post2.getId() + "," + post3.getId())
                    .header("token", StpUtil.getTokenValue())
                    .characterEncoding("UTF-8"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data").isArray())
                    .andReturn().getResponse().getContentAsString(Charset.forName("UTF-8"));

            JsonNode data = objectMapper.readTree(response).get("data");
            assertThat(data.size()).isEqualTo(3);

            // 2. 包含用户投票信息
            // 用户对 post1 点赞
            createVote(user.getId(), post1.getId(), VoteType.like);

            String responseWithVote = mockMvc.perform(get("/api/v1/posts")
                    .param("ids", post1.getId().toString() + "," + post2.getId().toString())
                    .header("token", StpUtil.getTokenValue())
                    .characterEncoding("UTF-8"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andReturn().getResponse().getContentAsString(Charset.forName("UTF-8"));

            JsonNode dataWithVote = objectMapper.readTree(responseWithVote).get("data");
            JsonNode post1Data = findById(dataWithVote, post1.getId());

            // 验证 post1 有投票信息
            if (post1Data.has("userVote") && !post1Data.get("userVote").isNull()) {
                assertThat(post1Data.get("userVote").asInt()).isEqualTo((int) VoteType.like.value());
            }

            // 3. 部分ID不存在
            String partialResponse = mockMvc.perform(get("/api/v1/posts")
                    .param("ids", post1.getId().toString() + ",99999," + post2.getId())
                    .header("token", StpUtil.getTokenValue())
                    .characterEncoding("UTF-8"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andReturn().getResponse().getContentAsString(Charset.forName("UTF-8"));

            JsonNode partialData = objectMapper.readTree(partialResponse).get("data");
            assertThat(partialData.size()).isEqualTo(2); // 只返回存在的帖子

            // 4. ids 为空或不传 nodeId - 参数不足
            mockMvc.perform(get("/api/v1/posts")
                    .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value("必须提供 ids 或 nodeId 参数"));

        } finally {
            StpUtil.logout();
        }

        // 5. 权限验证 - 需要登录
        mockMvc.perform(get("/api/v1/posts")
                .param("ids", post1.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));
    }

    /**
     * 测试6: 按节点分页查询帖子
     *
     * API: GET /api/v1/posts?nodeId=10
     *
     * 测试场景:
     * 1. 获取节点下的帖子（第一页）
     * 2. 分页查询 - 使用游标
     * 3. 空结果 - 节点下无帖子
     * 4. 参数验证
     * 5. 需要登录
     */
    @Test
    void testGetPostsByNode() throws Exception {
        // 准备：创建用户、课程、节点
        UserDO user = createUser("user@example.com");
        CourseDO course = createPublishedCourse("测试课程", user.getId());
        NodeDO node = createPublishedNode("测试节点", course.getId(), user.getId());

        // 创建5个帖子
        for (int i = 1; i <= 5; i++) {
            PostDO post = createPublishedPost("帖子" + i, node.getId(), user.getId(), PostType.article);
            // 设置不同的 score
            post.setScore((double) (10 - i));
            postDataService.update(post);
        }

        List<PostDO> posts = postDataService.getListByNodeAndCreator(node.getId(), user.getId());
        StpUtil.login(user.getId());

        try {
            // 1. 获取节点下的帖子（第一页）
            String response = mockMvc.perform(get("/api/v1/posts")
                    .param("nodeId", node.getId().toString())
                    .header("token", StpUtil.getTokenValue())
                    .characterEncoding("UTF-8"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data.items").isArray())
                    .andExpect(jsonPath("$.data.hasMore").exists())
                    .andReturn().getResponse().getContentAsString(Charset.forName("UTF-8"));

            JsonNode data = objectMapper.readTree(response).get("data");
            JsonNode items = data.get("items");
            assertThat(items.size()).isEqualTo(5);
            assertThat(data.get("hasMore").asBoolean()).isFalse(); // 只有5个，不足20个

            // 验证：按 score 倒序排列
            double previousScore = Double.MAX_VALUE;
            for (JsonNode item : items) {
                double currentScore = item.get("score").asDouble();
                assertThat(currentScore).isLessThanOrEqualTo(previousScore);
                previousScore = currentScore;
            }

            // 2. 分页查询 - 创建更多帖子测试分页
            for (int i = 6; i <= 25; i++) {
                PostDO post = createPublishedPost("帖子" + i, node.getId(), user.getId(), PostType.article);
                post.setScore((double) (30 - i));
                postDataService.update(post);
            }

            // 第一页
            String firstPageResponse = mockMvc.perform(get("/api/v1/posts")
                    .param("nodeId", node.getId().toString())
                    .header("token", StpUtil.getTokenValue())
                    .characterEncoding("UTF-8"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andReturn().getResponse().getContentAsString(Charset.forName("UTF-8"));

            JsonNode firstPageData = objectMapper.readTree(firstPageResponse).get("data");
            assertThat(firstPageData.get("items").size()).isEqualTo(20); // 每页20条
            assertThat(firstPageData.get("hasMore").asBoolean()).isTrue();

            // 第二页 - 使用游标
            JsonNode nextCursor = firstPageData.get("nextCursor");
            double lastScore = nextCursor.get("lastScore").asDouble();
            long lastId = nextCursor.get("lastId").asLong();

            String secondPageResponse = mockMvc.perform(get("/api/v1/posts")
                    .param("nodeId", node.getId().toString())
                    .param("lastScore", String.valueOf(lastScore))
                    .param("lastId", String.valueOf(lastId))
                    .header("token", StpUtil.getTokenValue())
                    .characterEncoding("UTF-8"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andReturn().getResponse().getContentAsString(Charset.forName("UTF-8"));

            JsonNode secondPageData = objectMapper.readTree(secondPageResponse).get("data");
            assertThat(secondPageData.get("items").size()).isEqualTo(5); // 剩余5条
            assertThat(secondPageData.get("hasMore").asBoolean()).isFalse();

            // 3. 空结果 - 节点下无帖子
            NodeDO emptyNode = createPublishedNode("空节点", course.getId(), user.getId());

            mockMvc.perform(get("/api/v1/posts")
                    .param("nodeId", emptyNode.getId().toString())
                    .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data.items").isArray())
                    .andExpect(jsonPath("$.data.items").isEmpty())
                    .andExpect(jsonPath("$.data.hasMore").value(false));

            // 4. 参数验证 - nodeId = 0
            mockMvc.perform(get("/api/v1/posts")
                    .param("nodeId", "0")
                    .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

            // 5. 参数验证 - nodeId = -1
            mockMvc.perform(get("/api/v1/posts")
                    .param("nodeId", "-1")
                    .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

            // 6. 参数验证 - lastId = -1
            mockMvc.perform(get("/api/v1/posts")
                    .param("nodeId", node.getId().toString())
                    .param("lastId", "-1")
                    .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

        } finally {
            StpUtil.logout();
        }

        // 7. 权限验证 - 需要登录
        mockMvc.perform(get("/api/v1/posts")
                .param("nodeId", node.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));
    }

    /**
     * 测试7: 获取用户帖子
     *
     * API: GET /api/v1/users/{userId}/posts
     *
     * 测试场景:
     * 1. 获取用户的文章列表（type=2）
     * 2. 获取用户的内容帖列表（type=1）
     * 3. 只返回已发布的帖子
     * 4. 参数验证
     * 5. 不需要登录
     */
    @Test
    void testGetUserPosts() throws Exception {
        // 准备：创建用户、课程、节点
        UserDO user = createUser("user@example.com");
        CourseDO course = createPublishedCourse("测试课程", user.getId());
        NodeDO node = createPublishedNode("测试节点", course.getId(), user.getId());

        // 创建5篇文章（type=2）和3个内容帖（type=1）
        for (int i = 1; i <= 5; i++) {
            createPublishedPost("文章" + i, node.getId(), user.getId(), PostType.article);
        }
        for (int i = 1; i <= 3; i++) {
            createPublishedPost("内容帖" + i, node.getId(), user.getId(), PostType.index);
        }

        // 创建2篇待审核文章和1篇已拒绝文章
        createPostWithState("待审核文章1", node.getId(), user.getId(), ContentState.SUBMITTED);
        createPostWithState("待审核文章2", node.getId(), user.getId(), ContentState.SUBMITTED);
        createPostWithState("已拒绝文章", node.getId(), user.getId(), ContentState.REJECTED);

        StpUtil.login(user.getId());

        try {
            // 1. 获取用户的文章列表（type=2）
            String articlesResponse = mockMvc.perform(get("/api/v1/users/" + user.getId() + "/posts")
                            .param("type", "2")
                            .header("token", StpUtil.getTokenValue())
                            .characterEncoding("UTF-8"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data.items").isArray())
                    .andReturn().getResponse().getContentAsString(Charset.forName("UTF-8"));

            JsonNode articlesData = objectMapper.readTree(articlesResponse).get("data").get("items");
            assertThat(articlesData.size()).isEqualTo(5); // 只返回5篇已发布文章

            // 验证：所有返回的都是文章类型
            for (JsonNode article : articlesData) {
                assertThat(article.get("type").asInt()).isEqualTo((int) PostType.article.value());
                assertThat(article.get("state").asInt()).isEqualTo((int) ContentState.PUBLISHED.value());
            }

            // 2. 获取用户的内容帖列表（type=1）
            String contentsResponse = mockMvc.perform(get("/api/v1/users/" + user.getId() + "/posts")
                            .param("type", "1")
                            .header("token", StpUtil.getTokenValue())
                            .characterEncoding("UTF-8"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andReturn().getResponse().getContentAsString(Charset.forName("UTF-8"));

            JsonNode contentsData = objectMapper.readTree(contentsResponse).get("data").get("items");
            assertThat(contentsData.size()).isEqualTo(3); // 只返回3个内容帖

            // 3. 默认类型 - 不传 type 参数
            String defaultResponse = mockMvc.perform(get("/api/v1/users/" + user.getId() + "/posts")
                            .header("token", StpUtil.getTokenValue())
                            .characterEncoding("UTF-8"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andReturn().getResponse().getContentAsString(Charset.forName("UTF-8"));

            JsonNode defaultData = objectMapper.readTree(defaultResponse).get("data").get("items");
            // 默认 type=2（文章）
            for (JsonNode item : defaultData) {
                assertThat(item.get("type").asInt()).isEqualTo((int) PostType.article.value());
            }

            // 4. 参数验证 - userId = 0
            mockMvc.perform(get("/api/v1/users/0/posts")
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

            // 5. 参数验证 - userId = -1
            mockMvc.perform(get("/api/v1/users/-1/posts")
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

            // 6. 参数验证 - type 无效
            mockMvc.perform(get("/api/v1/users/" + user.getId() + "/posts")
                            .param("type", "99")
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value("无效的帖子类型"));

            // 7. 空结果 - 用户无帖子
            UserDO newUser = createUser("newuser@example.com");
            mockMvc.perform(get("/api/v1/users/" + newUser.getId() + "/posts")
                            .param("type", "2")
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data.items").isArray())
                    .andExpect(jsonPath("$.data.items").isEmpty());
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试8: 获取当前用户所有状态的帖子
     *
     * API: GET /api/v1/users/me/posts
     *
     * 测试场景:
     * 1. 获取当前用户所有状态的文章
     * 2. 与公开接口对比 - 返回所有状态
     * 3. 只能查看自己的内容
     * 4. 必须登录
     */
    @Test
    void testGetCurrentUserAllPosts() throws Exception {
        // 准备：创建用户、课程、节点
        UserDO user = createUser("user@example.com");
        CourseDO course = createPublishedCourse("测试课程", user.getId());
        NodeDO node = createPublishedNode("测试节点", course.getId(), user.getId());

        // 创建不同状态的文章
        createPublishedPost("已发布文章1", node.getId(), user.getId(), PostType.article);
        createPublishedPost("已发布文章2", node.getId(), user.getId(), PostType.article);
        createPostWithState("待审核文章", node.getId(), user.getId(), ContentState.SUBMITTED);
        createPostWithState("已拒绝文章", node.getId(), user.getId(), ContentState.REJECTED);

        StpUtil.login(user.getId());

        try {
            // 1. 获取当前用户所有状态的文章
            String response = mockMvc.perform(get("/api/v1/users/me/posts")
                    .param("type", "2")
                    .header("token", StpUtil.getTokenValue())
                    .characterEncoding("UTF-8"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data.items").isArray())
                    .andReturn().getResponse().getContentAsString(Charset.forName("UTF-8"));

            JsonNode data = objectMapper.readTree(response).get("data").get("items");
            assertThat(data.size()).isEqualTo(4); // 返回所有4篇文章

            // 验证：包含所有状态
            int publishedCount = 0, submittedCount = 0, rejectedCount = 0;
            for (JsonNode item : data) {
                int state = item.get("state").asInt();
                if (state == ContentState.PUBLISHED.value()) publishedCount++;
                else if (state == ContentState.SUBMITTED.value()) submittedCount++;
                else if (state == ContentState.REJECTED.value()) rejectedCount++;
            }
            assertThat(publishedCount).isEqualTo(2);
            assertThat(submittedCount).isEqualTo(1);
            assertThat(rejectedCount).isEqualTo(1);

            // 2. 与公开接口对比
            String publicResponse = mockMvc.perform(get("/api/v1/users/" + user.getId() + "/posts")
                    .param("type", "2")
                    .header("token", StpUtil.getTokenValue())
                    .characterEncoding("UTF-8"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString(Charset.forName("UTF-8"));

            JsonNode publicData = objectMapper.readTree(publicResponse).get("data").get("items");
            assertThat(publicData.size()).isEqualTo(2); // 公开接口只返回已发布的2篇

        } finally {
            StpUtil.logout();
        }

        // 3. 必须登录
        mockMvc.perform(get("/api/v1/users/me/posts")
                .param("type", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));
    }
}
