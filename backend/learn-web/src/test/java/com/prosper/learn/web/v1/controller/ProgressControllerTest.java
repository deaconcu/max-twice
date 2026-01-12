package com.prosper.learn.web.v1.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prosper.learn.content.course.CourseDO;
import com.prosper.learn.content.course.CourseDataService;
import com.prosper.learn.content.node.NodeDO;
import com.prosper.learn.content.node.NodeDataService;
import com.prosper.learn.content.profession.ProfessionDO;
import com.prosper.learn.content.profession.ProfessionDataService;
import com.prosper.learn.content.roadmap.RoadmapDO;
import com.prosper.learn.content.roadmap.RoadmapDataService;
import com.prosper.learn.learning.enrollment.UserCourseDO;
import com.prosper.learn.learning.enrollment.UserCourseDataService;
import com.prosper.learn.learning.enrollment.UserRoadmapDO;
import com.prosper.learn.learning.enrollment.UserRoadmapDataService;
import com.prosper.learn.shared.domain.Enums.UserProgressState;
import com.prosper.learn.shared.domain.Enums.UserState;
import com.prosper.learn.shared.domain.Enums.UserRole;
import com.prosper.learn.shared.domain.exception.StatusCode;
import com.prosper.learn.user.profile.UserDO;
import com.prosper.learn.user.profile.UserDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Transactional
public class ProgressControllerTest extends BaseControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserDataService userDataService;

    @Autowired
    private CourseDataService courseDataService;

    @Autowired
    private NodeDataService nodeDataService;

    @Autowired
    private UserCourseDataService userCourseDataService;

    @Autowired
    private ProfessionDataService professionDataService;

    @Autowired
    private RoadmapDataService roadmapDataService;

    @Autowired
    private UserRoadmapDataService userRoadmapDataService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void setUp() {
        // 初始化 MockMvc
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // 清理 Redis 测试数据
        Set<String> keys = redisTemplate.keys("user:*:course:*:completed");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 创建测试用户
     */
    private UserDO createUser(String email) {
        UserDO user = new UserDO();
        user.setEmail(email);
        user.setName(email.split("@")[0]);
        user.setPassword("hashed_password");
        user.setState(UserState.ACTIVE.value());
        user.setEmailValidated(true);
        user.setBiography("");
        user.setRole(UserRole.USER.value());
        user.setMsgReadTime(java.time.LocalDateTime.now());
        userDataService.insert(user);
        return user;
    }

    /**
     * 创建测试课程（简化版-直接插入数据库）
     */
    private CourseDO createCourse(String name) {
        CourseDO course = new CourseDO();
        course.setName(name);
        course.setDescription("测试课程");
        course.setCreatorId(1L);
        course.setParentCourseId(0L);
        course.setRootNodeId(0L);
        course.setMainCategory(1);
        course.setSubCategory(1);
        course.setState((byte) 2); // PUBLISHED
        courseDataService.insert(course);
        return course;
    }

    /**
     * 创建测试节点
     */
    private NodeDO createNode(Long courseId, String name) {
        NodeDO node = new NodeDO();
        node.setCourseId(courseId);
        node.setName(name);
        node.setDescription("测试节点");
        node.setCreatorId(1L);
        node.setState((byte) 2); // PUBLISHED
        nodeDataService.insert(node);
        return node;
    }

    /**
     * 创建测试专业（简化版-直接插入数据库）
     */
    private ProfessionDO createProfession(String name) {
        ProfessionDO profession = new ProfessionDO();
        profession.setName(name);
        profession.setDescription("测试专业");
        profession.setCreatorId(1L);
        profession.setState((byte) 2); // PUBLISHED
        profession.setIcon("");
        profession.setSkills("");
        profession.setMainCategory(1);
        profession.setSubCategory(1);
        professionDataService.insert(profession);
        return profession;
    }

    /**
     * 创建测试路线图（简化版-直接插入数据库）
     */
    private RoadmapDO createRoadmap(Long professionId, int nodeCount) {
        // content 格式: [[[edges]], [nodeIds]]
        String content = String.format("[[[1,2],[2,3]],%s]",
            IntStream.rangeClosed(1, nodeCount)
                .mapToObj(String::valueOf)
                .collect(Collectors.joining(",", "[", "]")));

        RoadmapDO roadmap = new RoadmapDO();
        roadmap.setProfessionId(professionId);
        roadmap.setContent(content);
        roadmap.setContentHash(String.valueOf(content.hashCode()));
        roadmap.setDescription("测试路线图");
        roadmap.setCreatorId(1L);
        roadmap.setNodeCount(nodeCount);
        roadmap.setState((byte) 2); // PUBLISHED
        roadmap.setScore(0.0);
        roadmapDataService.insert(roadmap);
        return roadmap;
    }

    /**
     * 生成测试 Token
     */
    private String generateToken(Long userId) {
        StpUtil.login(userId);
        return StpUtil.getTokenValue();
    }

    // ========== 节点进度测试 ==========

    @Test
    void testMarkNodeComplete_Success() throws Exception {
        // 准备数据
        UserDO user = createUser("user1@test.com");
        CourseDO course = createCourse("Java基础");
        NodeDO node = createNode(course.getId(), "变量与数据类型");
        String token = generateToken(user.getId());

        // 构建请求体
        Map<String, Object> requestBody = Map.of("courseId", course.getId());

        // 执行请求
        mockMvc.perform(post("/api/v1/progress/nodes/{nodeId}/complete", node.getId())
                .header("token", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.nodeId").value(node.getId()))
            .andExpect(jsonPath("$.data.completed").value(true));
    }

    @Test
    void testMarkNodeComplete_AlreadyCompleted() throws Exception {
        // 准备数据
        UserDO user = createUser("user2@test.com");
        CourseDO course = createCourse("Python入门");
        NodeDO node = createNode(course.getId(), "环境搭建");
        String token = generateToken(user.getId());

        Map<String, Object> requestBody = Map.of("courseId", course.getId());

        // 第一次标记完成
        mockMvc.perform(post("/api/v1/progress/nodes/{nodeId}/complete", node.getId())
                .header("token", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
            .andExpect(status().isOk());

        // 第二次标记完成 - 期望失败
        mockMvc.perform(post("/api/v1/progress/nodes/{nodeId}/complete", node.getId())
                .header("token", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.NODE_ALREADY_COMPLETED.getCode()))
            .andExpect(jsonPath("$.message").value("节点已是完成状态"));
    }

    @Test
    void testMarkNodeComplete_InvalidId() throws Exception {
        UserDO user = createUser("user3@test.com");
        CourseDO course = createCourse("测试课程");
        String token = generateToken(user.getId());

        Map<String, Object> requestBody = Map.of("courseId", course.getId());

        // 测试负数ID - 应该返回参数异常
        mockMvc.perform(post("/api/v1/progress/nodes/{nodeId}/complete", -1)
                .header("token", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

        // 测试0 - 应该返回参数异常
        mockMvc.perform(post("/api/v1/progress/nodes/{nodeId}/complete", 0)
                .header("token", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
    }

    @Test
    void testUnmarkNodeComplete_Success() throws Exception {
        // 准备数据
        UserDO user = createUser("user4@test.com");
        CourseDO course = createCourse("Go语言");
        NodeDO node = createNode(course.getId(), "基础语法");
        String token = generateToken(user.getId());

        Map<String, Object> requestBody = Map.of("courseId", course.getId());

        // 先标记完成
        mockMvc.perform(post("/api/v1/progress/nodes/{nodeId}/complete", node.getId())
                .header("token", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
            .andExpect(status().isOk());

        // 取消完成
        mockMvc.perform(delete("/api/v1/progress/nodes/{nodeId}/complete", node.getId())
                .header("token", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.nodeId").value(node.getId()))
            .andExpect(jsonPath("$.data.completed").value(false));
    }

    @Test
    void testUnmarkNodeComplete_AlreadyNotCompleted() throws Exception {
        // 准备数据
        UserDO user = createUser("user5@test.com");
        CourseDO course = createCourse("Rust编程");
        NodeDO node = createNode(course.getId(), "所有权系统");
        String token = generateToken(user.getId());

        Map<String, Object> requestBody = Map.of("courseId", course.getId());

        // 取消未完成的节点 - 期望失败
        mockMvc.perform(delete("/api/v1/progress/nodes/{nodeId}/complete", node.getId())
                .header("token", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.NODE_ALREADY_NOT_COMPLETED.getCode()))
            .andExpect(jsonPath("$.message").value("节点已是未完成状态"));
    }

    @Test
    void testGetNodeStatus_Completed() throws Exception {
        // 准备数据
        UserDO user = createUser("user6@test.com");
        CourseDO course = createCourse("C++高级");
        NodeDO node = createNode(course.getId(), "模板编程");
        String token = generateToken(user.getId());

        Map<String, Object> requestBody = Map.of("courseId", course.getId());

        // 标记完成
        mockMvc.perform(post("/api/v1/progress/nodes/{nodeId}/complete", node.getId())
                .header("token", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
            .andExpect(status().isOk());

        // 查询状态
        mockMvc.perform(get("/api/v1/progress/nodes/{nodeId}/status", node.getId())
                .header("token", token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.nodeId").value(node.getId()))
            .andExpect(jsonPath("$.data.completed").value(true));
    }

    @Test
    void testGetNodeStatus_NotCompleted() throws Exception {
        // 准备数据
        UserDO user = createUser("user7@test.com");
        CourseDO course = createCourse("测试课程");
        NodeDO node = createNode(course.getId(), "测试节点");
        String token = generateToken(user.getId());

        // 查询状态 - 未完成
        mockMvc.perform(get("/api/v1/progress/nodes/{nodeId}/status", node.getId())
                .header("token", token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.nodeId").value(node.getId()))
            .andExpect(jsonPath("$.data.completed").value(false));
    }

    // ========== 课程进度测试 ==========

    @Test
    void testStartCourse_Success() throws Exception {
        // 准备数据
        UserDO user = createUser("user8@test.com");
        CourseDO course = createCourse("JavaScript基础");
        String token = generateToken(user.getId());

        // 执行请求
        mockMvc.perform(post("/api/v1/progress/courses/{courseId}/enrollment", course.getId())
                .header("token", token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.courseId").value(course.getId()))
            .andExpect(jsonPath("$.data.learning").value(true));

        // 验证数据库
        UserCourseDO userCourse = userCourseDataService.getByUserIdAndCourseId(user.getId(), course.getId());
        assertNotNull(userCourse);
        assertEquals(0, userCourse.getProgressPercent());
    }

    @Test
    void testStartCourse_AlreadyStarted() throws Exception {
        // 准备数据
        UserDO user = createUser("user9@test.com");
        CourseDO course = createCourse("Vue框架");
        String token = generateToken(user.getId());

        // 第一次开始学习
        mockMvc.perform(post("/api/v1/progress/courses/{courseId}/enrollment", course.getId())
                .header("token", token))
            .andExpect(status().isOk());

        // 第二次开始学习 - 期望失败
        mockMvc.perform(post("/api/v1/progress/courses/{courseId}/enrollment", course.getId())
                .header("token", token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.USER_COURSE_ALREADY_STARTED.getCode()))
            .andExpect(jsonPath("$.message").value("课程已开始学习"));
    }

    @Test
    void testCancelCourse_Success() throws Exception {
        // 准备数据
        UserDO user = createUser("user10@test.com");
        CourseDO course = createCourse("React框架");
        String token = generateToken(user.getId());

        // 开始学习
        mockMvc.perform(post("/api/v1/progress/courses/{courseId}/enrollment", course.getId())
                .header("token", token))
            .andExpect(status().isOk());

        // 取消学习
        mockMvc.perform(delete("/api/v1/progress/courses/{courseId}/enrollment", course.getId())
                .header("token", token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.courseId").value(course.getId()))
            .andExpect(jsonPath("$.data.learning").value(false));

        // 验证数据库 - 记录应该被删除
        UserCourseDO userCourse = userCourseDataService.getByUserIdAndCourseId(user.getId(), course.getId());
        assertNull(userCourse);
    }

    @Test
    void testCancelCourse_NotStarted() throws Exception {
        // 准备数据
        UserDO user = createUser("user11@test.com");
        CourseDO course = createCourse("Angular框架");
        String token = generateToken(user.getId());

        // 取消未开始的学习 - 期望失败
        mockMvc.perform(delete("/api/v1/progress/courses/{courseId}/enrollment", course.getId())
                .header("token", token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.USER_COURSE_NOT_STARTED.getCode()))
            .andExpect(jsonPath("$.message").value("课程尚未开始学习"));
    }

    @Test
    void testGetCourseProgress_Success() throws Exception {
        // 准备数据
        UserDO user = createUser("user12@test.com");
        CourseDO course = createCourse("TypeScript");
        String token = generateToken(user.getId());

        // 开始学习
        mockMvc.perform(post("/api/v1/progress/courses/{courseId}/enrollment", course.getId())
                .header("token", token))
            .andExpect(status().isOk());

        // 查询进度
        mockMvc.perform(get("/api/v1/progress/courses/{courseId}", course.getId())
                .header("token", token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.courseId").value(course.getId()))
            .andExpect(jsonPath("$.data.progressPercent").value(0))
            .andExpect(jsonPath("$.data.course").isNotEmpty())
            .andExpect(jsonPath("$.data.course.id").value(course.getId()))
            .andExpect(jsonPath("$.data.course.name").value(course.getName()));
    }

    @Test
    void testGetCourseProgress_NotFound() throws Exception {
        // 准备数据
        UserDO user = createUser("user13@test.com");
        CourseDO course = createCourse("测试课程");
        String token = generateToken(user.getId());

        // 查询不存在的进度 - 期望失败
        mockMvc.perform(get("/api/v1/progress/courses/{courseId}", course.getId())
                .header("token", token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.USER_COURSE_NOT_FOUND.getCode()))
            .andExpect(jsonPath("$.message").value("课程学习记录不存在"));
    }

    @Test
    void testGetAllCoursesProgress_Multiple() throws Exception {
        // 准备数据
        UserDO user = createUser("user14@test.com");
        CourseDO course1 = createCourse("课程1");
        CourseDO course2 = createCourse("课程2");
        String token = generateToken(user.getId());

        // 开始学习两门课程
        mockMvc.perform(post("/api/v1/progress/courses/{courseId}/enrollment", course1.getId())
                .header("token", token))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/progress/courses/{courseId}/enrollment", course2.getId())
                .header("token", token))
            .andExpect(status().isOk());

        // 查询所有进度
        mockMvc.perform(get("/api/v1/progress/courses")
                .header("token", token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void testGetAllCoursesProgress_Empty() throws Exception {
        // 准备数据
        UserDO user = createUser("user15@test.com");
        String token = generateToken(user.getId());

        // 查询所有进度 - 没有学习任何课程
        mockMvc.perform(get("/api/v1/progress/courses")
                .header("token", token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void testUpdateCourseProgress_Success() throws Exception {
        // 准备数据
        UserDO user = createUser("user16@test.com");
        CourseDO course = createCourse("Spring Boot");
        String token = generateToken(user.getId());

        // 开始学习
        mockMvc.perform(post("/api/v1/progress/courses/{courseId}/enrollment", course.getId())
                .header("token", token))
            .andExpect(status().isOk());

        // 更新进度
        Map<String, Object> requestBody = Map.of("progressPercent", 60);
        mockMvc.perform(put("/api/v1/progress/courses/{courseId}", course.getId())
                .header("token", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.progressPercent").value(60));

        // 验证数据库
        UserCourseDO userCourse = userCourseDataService.getByUserIdAndCourseId(user.getId(), course.getId());
        assertEquals(60, userCourse.getProgressPercent());
    }

    @Test
    void testUpdateCourseProgress_Complete() throws Exception {
        // 准备数据
        UserDO user = createUser("user17@test.com");
        CourseDO course = createCourse("MyBatis");
        String token = generateToken(user.getId());

        // 开始学习
        mockMvc.perform(post("/api/v1/progress/courses/{courseId}/enrollment", course.getId())
                .header("token", token))
            .andExpect(status().isOk());

        // 更新进度到100%
        Map<String, Object> requestBody = Map.of("progressPercent", 100);
        mockMvc.perform(put("/api/v1/progress/courses/{courseId}", course.getId())
                .header("token", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.progressPercent").value(100))
            .andExpect(jsonPath("$.data.state").value((int) UserProgressState.COMPLETED.value()))
            .andExpect(jsonPath("$.data.completedAt").isNotEmpty());

        // 验证数据库
        UserCourseDO userCourse = userCourseDataService.getByUserIdAndCourseId(user.getId(), course.getId());
        assertEquals(100, userCourse.getProgressPercent());
        assertEquals(UserProgressState.COMPLETED.value(), userCourse.getState());
        assertNotNull(userCourse.getCompletedAt());
    }

    @Test
    void testUpdateCourseProgress_NotFound() throws Exception {
        // 准备数据
        UserDO user = createUser("user18@test.com");
        CourseDO course = createCourse("测试课程");
        String token = generateToken(user.getId());

        // 更新不存在的进度 - 期望失败
        Map<String, Object> requestBody = Map.of("progressPercent", 50);
        mockMvc.perform(put("/api/v1/progress/courses/{courseId}", course.getId())
                .header("token", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.USER_COURSE_NOT_FOUND.getCode()))
            .andExpect(jsonPath("$.message").value("课程学习记录不存在"));
    }

    @Test
    void testUpdateCourseProgress_InvalidProgressPercent() throws Exception {
        // 准备数据
        UserDO user = createUser("user19@test.com");
        CourseDO course = createCourse("测试课程");
        String token = generateToken(user.getId());

        // 开始学习
        mockMvc.perform(post("/api/v1/progress/courses/{courseId}/enrollment", course.getId())
                .header("token", token))
            .andExpect(status().isOk());

        // 测试负数 - 验证注解失败，返回 INVALID_PARAMETER
        Map<String, Object> requestBody1 = Map.of("progressPercent", -10);
        mockMvc.perform(put("/api/v1/progress/courses/{courseId}", course.getId())
                .header("token", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody1)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

        // 测试超过100 - 验证注解失败，返回 INVALID_PARAMETER
        Map<String, Object> requestBody2 = Map.of("progressPercent", 150);
        mockMvc.perform(put("/api/v1/progress/courses/{courseId}", course.getId())
                .header("token", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody2)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
    }

    // ========== 路线图进度测试 ==========

    @Test
    void testStartRoadmap_Success() throws Exception {
        // 准备数据
        UserDO user = createUser("user20@test.com");
        ProfessionDO profession = createProfession("后端开发");
        RoadmapDO roadmap = createRoadmap(profession.getId(), 10);
        String token = generateToken(user.getId());

        // 执行请求
        mockMvc.perform(post("/api/v1/progress/roadmaps/{roadmapId}/enrollment", roadmap.getId())
                .header("token", token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.roadmapId").value(roadmap.getId()))
            .andExpect(jsonPath("$.data.learning").value(true));

        // 验证数据库
        UserRoadmapDO userRoadmap = userRoadmapDataService.getByUserAndRoadmap(user.getId(), roadmap.getId());
        assertNotNull(userRoadmap);
        assertEquals(0, userRoadmap.getProgressPercent());
        assertEquals(UserProgressState.IN_PROGRESS.value(), userRoadmap.getState());
    }

    @Test
    void testStartRoadmap_AlreadyStarted() throws Exception {
        // 准备数据
        UserDO user = createUser("user21@test.com");
        ProfessionDO profession = createProfession("前端开发");
        RoadmapDO roadmap = createRoadmap(profession.getId(), 8);
        String token = generateToken(user.getId());

        // 第一次开始学习
        mockMvc.perform(post("/api/v1/progress/roadmaps/{roadmapId}/enrollment", roadmap.getId())
                .header("token", token))
            .andExpect(status().isOk());

        // 第二次开始学习 - 期望失败
        mockMvc.perform(post("/api/v1/progress/roadmaps/{roadmapId}/enrollment", roadmap.getId())
                .header("token", token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.USER_ROADMAP_ALREADY_STARTED.getCode()))
            .andExpect(jsonPath("$.message").value("路线图已开始学习"));
    }

    @Test
    void testCancelRoadmap_Success() throws Exception {
        // 准备数据
        UserDO user = createUser("user22@test.com");
        ProfessionDO profession = createProfession("数据科学");
        RoadmapDO roadmap = createRoadmap(profession.getId(), 12);
        String token = generateToken(user.getId());

        // 开始学习
        mockMvc.perform(post("/api/v1/progress/roadmaps/{roadmapId}/enrollment", roadmap.getId())
                .header("token", token))
            .andExpect(status().isOk());

        // 取消学习
        mockMvc.perform(delete("/api/v1/progress/roadmaps/{roadmapId}/enrollment", roadmap.getId())
                .header("token", token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.roadmapId").value(roadmap.getId()))
            .andExpect(jsonPath("$.data.learning").value(false));

        // 验证数据库 - 记录应该被删除
        UserRoadmapDO userRoadmap = userRoadmapDataService.getByUserAndRoadmap(user.getId(), roadmap.getId());
        assertNull(userRoadmap);
    }

    @Test
    void testCancelRoadmap_NotStarted() throws Exception {
        // 准备数据
        UserDO user = createUser("user23@test.com");
        ProfessionDO profession = createProfession("移动开发");
        RoadmapDO roadmap = createRoadmap(profession.getId(), 9);
        String token = generateToken(user.getId());

        // 取消未开始的学习 - 期望失败
        mockMvc.perform(delete("/api/v1/progress/roadmaps/{roadmapId}/enrollment", roadmap.getId())
                .header("token", token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.USER_ROADMAP_NOT_STARTED.getCode()))
            .andExpect(jsonPath("$.message").value("路线图尚未开始学习"));
    }

    @Test
    void testGetRoadmapProgress_Success() throws Exception {
        // 准备数据
        UserDO user = createUser("user24@test.com");
        ProfessionDO profession = createProfession("网络安全");
        RoadmapDO roadmap = createRoadmap(profession.getId(), 16);
        String token = generateToken(user.getId());

        // 开始学习
        mockMvc.perform(post("/api/v1/progress/roadmaps/{roadmapId}/enrollment", roadmap.getId())
                .header("token", token))
            .andExpect(status().isOk());

        // 查询进度
        mockMvc.perform(get("/api/v1/progress/roadmaps/{roadmapId}", roadmap.getId())
                .header("token", token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.roadmapId").value(roadmap.getId()))
            .andExpect(jsonPath("$.data.progressPercent").value(0))
            .andExpect(jsonPath("$.data.state").value((int) UserProgressState.IN_PROGRESS.value()))
            .andExpect(jsonPath("$.data.roadmap").isNotEmpty());
    }

    @Test
    void testGetRoadmapProgress_NotFound() throws Exception {
        // 准备数据
        UserDO user = createUser("user25@test.com");
        ProfessionDO profession = createProfession("DevOps");
        RoadmapDO roadmap = createRoadmap(profession.getId(), 14);
        String token = generateToken(user.getId());

        // 查询不存在的进度 - 期望失败
        mockMvc.perform(get("/api/v1/progress/roadmaps/{roadmapId}", roadmap.getId())
                .header("token", token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.USER_ROADMAP_NOT_FOUND.getCode()))
            .andExpect(jsonPath("$.message").value("学习记录不存在"));
    }

    @Test
    void testGetAllRoadmapsProgress_Multiple() throws Exception {
        // 准备数据
        UserDO user = createUser("user26@test.com");
        ProfessionDO profession1 = createProfession("后端");
        ProfessionDO profession2 = createProfession("前端");
        RoadmapDO roadmap1 = createRoadmap(profession1.getId(), 10);
        RoadmapDO roadmap2 = createRoadmap(profession2.getId(), 8);
        String token = generateToken(user.getId());

        // 开始学习两个路线图
        mockMvc.perform(post("/api/v1/progress/roadmaps/{roadmapId}/enrollment", roadmap1.getId())
                .header("token", token))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/progress/roadmaps/{roadmapId}/enrollment", roadmap2.getId())
                .header("token", token))
            .andExpect(status().isOk());

        // 查询所有进度
        MvcResult result = mockMvc.perform(get("/api/v1/progress/roadmaps")
                .header("token", token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(2))
            .andReturn();

        // 验证返回的简要信息格式
        String responseBody = result.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode dataArray = root.get("data");

        for (JsonNode item : dataArray) {
            JsonNode roadmapData = item.get("roadmap");
            assertTrue(roadmapData.has("id"));
            assertTrue(roadmapData.has("professionName"));
            assertTrue(roadmapData.has("nodeCount"));
            // 验证不包含详细信息
            assertFalse(roadmapData.has("content"));
            assertFalse(roadmapData.has("description"));
        }
    }

    @Test
    void testGetAllRoadmapsProgress_Empty() throws Exception {
        // 准备数据
        UserDO user = createUser("user27@test.com");
        String token = generateToken(user.getId());

        // 查询所有进度 - 没有学习任何路线图
        mockMvc.perform(get("/api/v1/progress/roadmaps")
                .header("token", token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void testUpdateRoadmapProgress_Success() throws Exception {
        // 准备数据
        UserDO user = createUser("user28@test.com");
        ProfessionDO profession = createProfession("云计算");
        RoadmapDO roadmap = createRoadmap(profession.getId(), 15);
        String token = generateToken(user.getId());

        // 开始学习
        mockMvc.perform(post("/api/v1/progress/roadmaps/{roadmapId}/enrollment", roadmap.getId())
                .header("token", token))
            .andExpect(status().isOk());

        // 更新进度
        Map<String, Object> requestBody = Map.of("progressPercent", 60);
        mockMvc.perform(put("/api/v1/progress/roadmaps/{roadmapId}", roadmap.getId())
                .header("token", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.progressPercent").value(60));

        // 验证数据库
        UserRoadmapDO userRoadmap = userRoadmapDataService.getByUserAndRoadmap(user.getId(), roadmap.getId());
        assertEquals(60, userRoadmap.getProgressPercent());
    }

    @Test
    void testUpdateRoadmapProgress_Complete() throws Exception {
        // 准备数据
        UserDO user = createUser("user29@test.com");
        ProfessionDO profession = createProfession("人工智能");
        RoadmapDO roadmap = createRoadmap(profession.getId(), 20);
        String token = generateToken(user.getId());

        // 开始学习（注册路线图）
        mockMvc.perform(post("/api/v1/progress/roadmaps/{roadmapId}/enrollment", roadmap.getId())
                .header("token", token))
            .andExpect(status().isOk());

        // 更新进度到100%
        Map<String, Object> requestBody = Map.of("progressPercent", 100);
        mockMvc.perform(put("/api/v1/progress/roadmaps/{roadmapId}", roadmap.getId())
                .header("token", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.progressPercent").value(100))
            .andExpect(jsonPath("$.data.state").value((int) UserProgressState.COMPLETED.value()))
            .andExpect(jsonPath("$.data.completedAt").isNotEmpty());

        // 验证数据库
        UserRoadmapDO userRoadmap = userRoadmapDataService.getByUserAndRoadmap(user.getId(), roadmap.getId());
        assertEquals(100, userRoadmap.getProgressPercent());
        assertEquals(UserProgressState.COMPLETED.value(), userRoadmap.getState());
        assertNotNull(userRoadmap.getCompletedAt());
    }

    @Test
    void testUpdateRoadmapProgress_NotFound() throws Exception {
        // 准备数据
        UserDO user = createUser("user30@test.com");
        ProfessionDO profession = createProfession("区块链");
        RoadmapDO roadmap = createRoadmap(profession.getId(), 10);
        String token = generateToken(user.getId());

        // 更新不存在的进度 - 期望失败
        Map<String, Object> requestBody = Map.of("progressPercent", 50);
        mockMvc.perform(put("/api/v1/progress/roadmaps/{roadmapId}", roadmap.getId())
                .header("token", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.USER_ROADMAP_NOT_FOUND.getCode()))
            .andExpect(jsonPath("$.message").value("学习记录不存在"));
    }

    @Test
    void testUpdateRoadmapProgress_InvalidProgressPercent() throws Exception {
        // 准备数据
        UserDO user = createUser("user31@test.com");
        ProfessionDO profession = createProfession("游戏开发");
        RoadmapDO roadmap = createRoadmap(profession.getId(), 18);
        String token = generateToken(user.getId());

        // 开始学习
        mockMvc.perform(post("/api/v1/progress/roadmaps/{roadmapId}/enrollment", roadmap.getId())
                .header("token", token))
            .andExpect(status().isOk());

        // 测试负数 - 验证注解失败，返回 INVALID_PARAMETER
        Map<String, Object> requestBody1 = Map.of("progressPercent", -10);
        mockMvc.perform(put("/api/v1/progress/roadmaps/{roadmapId}", roadmap.getId())
                .header("token", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody1)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

        // 测试超过100 - 验证注解失败，返回 INVALID_PARAMETER
        Map<String, Object> requestBody2 = Map.of("progressPercent", 150);
        mockMvc.perform(put("/api/v1/progress/roadmaps/{roadmapId}", roadmap.getId())
                .header("token", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody2)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
    }

    // ========== 参数验证测试 ==========

    @Test
    void testUnauthorized() throws Exception {
        // 测试未登录访问 - 返回 200 + USER_NOT_LOGIN 错误码
        mockMvc.perform(post("/api/v1/progress/courses/1/enrollment"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));

        mockMvc.perform(get("/api/v1/progress/courses"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));

        mockMvc.perform(post("/api/v1/progress/roadmaps/1/enrollment"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));
    }
}
