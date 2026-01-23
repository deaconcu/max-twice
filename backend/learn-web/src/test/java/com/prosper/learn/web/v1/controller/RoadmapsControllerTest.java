package com.prosper.learn.web.v1.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prosper.learn.content.course.CourseDataService;
import com.prosper.learn.content.profession.ProfessionDO;
import com.prosper.learn.content.profession.ProfessionDataService;
import com.prosper.learn.content.roadmap.RoadmapDO;
import com.prosper.learn.content.roadmap.RoadmapDataService;
import com.prosper.learn.shared.domain.Enums.*;
import com.prosper.learn.shared.domain.exception.StatusCode;
import com.prosper.learn.user.profile.*;
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
 * 路线图管理接口测试
 * 测试文档: docs/test/roadmap.md
 */
@Transactional
public class RoadmapsControllerTest extends BaseControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserDomainService userDomainService;

    @Autowired
    private UserDataService userDataService;

    @Autowired
    private UserProfileDataService userProfileDataService;

    @Autowired
    private ProfessionDataService professionDataService;

    @Autowired
    private RoadmapDataService roadmapDataService;

    @Autowired
    private CourseDataService courseDataService;

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
     * 创建测试专业
     */
    private ProfessionDO createProfession(String name, Long creatorId) {
        ProfessionDO profession = new ProfessionDO();
        profession.setName(name);
        profession.setDescription("专业描述");
        profession.setIcon("mdi-icon");
        profession.setSkills("技能列表");
        profession.setMainCategory(1);
        profession.setSubCategory(1);
        profession.setState(ContentState.PUBLISHED.value());
        profession.setCreatorId(creatorId);
        professionDataService.insert(profession);
        return profession;
    }

    /**
     * 创建已发布路线图
     */
    private RoadmapDO createPublishedRoadmap(Long professionId, Long creatorId, String description) {
        RoadmapDO roadmap = new RoadmapDO();
        roadmap.setProfessionId(professionId);
        roadmap.setCreatorId(creatorId);
        roadmap.setContent(getRoadmapContent());
        roadmap.setContentHash("test-hash-" + System.currentTimeMillis());
        roadmap.setDescription(description);
        roadmap.setState(ContentState.PUBLISHED.value());
        roadmap.setNodeCount(3);
        roadmap.setScore(0.0);
        roadmapDataService.insert(roadmap);
        return roadmap;
    }

    /**
     * 创建草稿路线图
     */
    private RoadmapDO createDraftRoadmap(Long professionId, Long creatorId, String description) {
        RoadmapDO roadmap = new RoadmapDO();
        roadmap.setProfessionId(professionId);
        roadmap.setCreatorId(creatorId);
        roadmap.setContent(getRoadmapContent());
        roadmap.setContentHash("test-hash-" + System.currentTimeMillis());
        roadmap.setDescription(description);
        roadmap.setState(ContentState.DRAFT.value());
        roadmap.setNodeCount(3);
        roadmap.setScore(0.0);
        roadmapDataService.insert(roadmap);
        return roadmap;
    }

    /**
     * 获取有效的路线图内容
     */
    private String getRoadmapContent() {
        return "[[[1,2],[2,3]],[1,2,3]]";
    }

    // ==================== 接口1: 获取专业下的路线图列表 ====================

    /**
     * 1.1 成功获取路线图列表 - 有路线图
     */
    @Test
    @DisplayName("成功获取专业路线图列表 - 有路线图")
    void testGetRoadmapsByProfession_WithRoadmaps_Success() throws Exception {
        // 准备数据
        UserDO user = createUser("user1@test.com");
        ProfessionDO profession = createProfession("后端开发", user.getId());
        RoadmapDO roadmap1 = createPublishedRoadmap(profession.getId(), user.getId(), "路线图1");
        RoadmapDO roadmap2 = createPublishedRoadmap(profession.getId(), user.getId(), "路线图2");
        RoadmapDO roadmap3 = createPublishedRoadmap(profession.getId(), user.getId(), "路线图3");

        // 用户登录
        StpUtil.login(user.getId());

        // 获取路线图列表
        mockMvc.perform(get("/api/v1/professions/" + profession.getId() + "/roadmaps")
                        .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].id").exists())
                .andExpect(jsonPath("$.data[0].content").exists())
                .andExpect(jsonPath("$.data[0].professionId").value(profession.getId()))
                .andExpect(jsonPath("$.data[0].creator").exists())
                .andExpect(jsonPath("$.data[0].creator.id").value(user.getId()))
                .andExpect(jsonPath("$.data[0].profession").exists())
                .andExpect(jsonPath("$.data[0].profession.name").value(profession.getName()))
                .andExpect(jsonPath("$.data[0].upvoted").exists())
                .andExpect(jsonPath("$.data[0].learning").exists());

        StpUtil.logout();
    }

    /**
     * 1.2 成功获取路线图列表 - 无路线图
     */
    @Test
    @DisplayName("成功获取专业路线图列表 - 无路线图")
    void testGetRoadmapsByProfession_NoRoadmaps_Success() throws Exception {
        UserDO user = createUser("user2@test.com");
        ProfessionDO profession = createProfession("前端开发", user.getId());

        StpUtil.login(user.getId());

        mockMvc.perform(get("/api/v1/professions/" + profession.getId() + "/roadmaps")
                        .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));

        StpUtil.logout();
    }

    /**
     * 1.3 字段验证 - professionId 无效（0）
     */
    @Test
    @DisplayName("字段验证 - professionId 无效为0")
    void testGetRoadmapsByProfession_ProfessionIdZero_Fail() throws Exception {
        UserDO user = createUser("user3@test.com");
        StpUtil.login(user.getId());

        mockMvc.perform(get("/api/v1/professions/0/roadmaps")
                        .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

        StpUtil.logout();
    }

    /**
     * 1.4 字段验证 - professionId 无效（负数）
     */
    @Test
    @DisplayName("字段验证 - professionId 无效为负数")
    void testGetRoadmapsByProfession_ProfessionIdNegative_Fail() throws Exception {
        UserDO user = createUser("user4@test.com");
        StpUtil.login(user.getId());

        mockMvc.perform(get("/api/v1/professions/-1/roadmaps")
                        .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

        StpUtil.logout();
    }

    /**
     * 1.5 权限验证 - 未登录
     */
    @Test
    @DisplayName("权限验证 - 未登录获取专业路线图")
    void testGetRoadmapsByProfession_NotLoggedIn_Fail() throws Exception {
        UserDO user = createUser("admin@test.com");
        ProfessionDO profession = createProfession("测试专业", user.getId());

        mockMvc.perform(get("/api/v1/professions/" + profession.getId() + "/roadmaps"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));
    }

    // ==================== 接口2: 更新路线图 ====================

    /**
     * 2.1 成功更新路线图 - 只更新内容
     */
    @Test
    @DisplayName("成功更新路线图 - 只更新内容")
    void testUpdateRoadmap_ContentOnly_Success() throws Exception {
        UserDO user = createUser("user5@test.com");
        ProfessionDO profession = createProfession("专业1", user.getId());
        RoadmapDO roadmap = createPublishedRoadmap(profession.getId(), user.getId(), "原描述");

        String newContent = "[[[1,2],[2,3],[3,4]],[1,2,3,4]]";

        StpUtil.login(user.getId());

        mockMvc.perform(put("/api/v1/roadmaps/" + roadmap.getId())
                        .header("token", StpUtil.getTokenValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"" + newContent + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").doesNotExist());

        // 验证数据库
        RoadmapDO updated = roadmapDataService.getById(roadmap.getId());
        // JSON比较，忽略空格差异
        assertThat(objectMapper.readTree(updated.getContent()))
            .isEqualTo(objectMapper.readTree(newContent));
        assertThat(updated.getDescription()).isEqualTo("原描述");

        StpUtil.logout();
    }

    /**
     * 2.2 成功更新路线图 - 同时更新内容和描述
     */
    @Test
    @DisplayName("成功更新路线图 - 同时更新内容和描述")
    void testUpdateRoadmap_ContentAndDescription_Success() throws Exception {
        UserDO user = createUser("user6@test.com");
        ProfessionDO profession = createProfession("专业2", user.getId());
        RoadmapDO roadmap = createPublishedRoadmap(profession.getId(), user.getId(), "原描述");

        String newContent = "[[[1,2]],[1,2]]";
        String newDescription = "新描述";

        StpUtil.login(user.getId());

        mockMvc.perform(put("/api/v1/roadmaps/" + roadmap.getId())
                        .header("token", StpUtil.getTokenValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"" + newContent + "\", \"description\": \"" + newDescription + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 验证数据库
        RoadmapDO updated = roadmapDataService.getById(roadmap.getId());
        // JSON比较，忽略空格差异
        assertThat(objectMapper.readTree(updated.getContent()))
            .isEqualTo(objectMapper.readTree(newContent));
        assertThat(updated.getDescription()).isEqualTo(newDescription);

        StpUtil.logout();
    }

    /**
     * 2.3 字段验证 - content 缺失
     */
    @Test
    @DisplayName("字段验证 - content 缺失")
    void testUpdateRoadmap_ContentMissing_Fail() throws Exception {
        UserDO user = createUser("user7@test.com");
        ProfessionDO profession = createProfession("专业3", user.getId());
        RoadmapDO roadmap = createPublishedRoadmap(profession.getId(), user.getId(), "描述");

        StpUtil.login(user.getId());

        mockMvc.perform(put("/api/v1/roadmaps/" + roadmap.getId())
                        .header("token", StpUtil.getTokenValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

        StpUtil.logout();
    }

    /**
     * 2.5 字段验证 - id 无效（0）
     */
    @Test
    @DisplayName("字段验证 - roadmapId 无效为0")
    void testUpdateRoadmap_IdZero_Fail() throws Exception {
        UserDO user = createUser("user8@test.com");
        StpUtil.login(user.getId());

        mockMvc.perform(put("/api/v1/roadmaps/0")
                        .header("token", StpUtil.getTokenValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"内容\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

        StpUtil.logout();
    }

    /**
     * 2.6 业务验证 - 路线图不存在
     */
    @Test
    @DisplayName("业务验证 - 路线图不存在")
    void testUpdateRoadmap_RoadmapNotFound_Fail() throws Exception {
        UserDO user = createUser("user9@test.com");
        StpUtil.login(user.getId());

        mockMvc.perform(put("/api/v1/roadmaps/99999")
                        .header("token", StpUtil.getTokenValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"内容\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.ROADMAP_NOT_FOUND.getCode()));

        StpUtil.logout();
    }

    /**
     * 2.7 权限验证 - 修改他人的路线图
     */
    @Test
    @DisplayName("权限验证 - 修改他人的路线图")
    void testUpdateRoadmap_NotOwner_Fail() throws Exception {
        UserDO userA = createUser("userA@test.com");
        ProfessionDO profession = createProfession("专业4", userA.getId());
        UserDO userB = createUser("userB@test.com");
        RoadmapDO roadmap = createPublishedRoadmap(profession.getId(), userA.getId(), "描述");

        StpUtil.login(userB.getId());

        mockMvc.perform(put("/api/v1/roadmaps/" + roadmap.getId())
                        .header("token", StpUtil.getTokenValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"新内容\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.PERMISSION_DENIED.getCode()));

        StpUtil.logout();
    }

    /**
     * 2.9 权限验证 - 未登录
     */
    @Test
    @DisplayName("权限验证 - 未登录更新路线图")
    void testUpdateRoadmap_NotLoggedIn_Fail() throws Exception {
        mockMvc.perform(put("/api/v1/roadmaps/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"内容\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));
    }

    // ==================== 接口3: 创建路线图 ====================

    /**
     * 3.1 成功创建路线图
     */
    @Test
    @DisplayName("成功创建路线图")
    void testCreateRoadmap_Success() throws Exception {
        UserDO user = createUser("user10@test.com");
        ProfessionDO profession = createProfession("专业5", user.getId());

        StpUtil.login(user.getId());

        String content = getRoadmapContent();
        String requestBody = String.format(
                "{\"professionId\": %d, \"content\": \"%s\", \"description\": \"测试路线图\"}",
                profession.getId(), content);

        mockMvc.perform(post("/api/v1/roadmaps")
                        .header("token", StpUtil.getTokenValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isNumber());

        StpUtil.logout();
    }

    /**
     * 3.2 字段验证 - professionId 缺失
     */
    @Test
    @DisplayName("字段验证 - professionId 缺失")
    void testCreateRoadmap_ProfessionIdMissing_Fail() throws Exception {
        UserDO user = createUser("user11@test.com");
        StpUtil.login(user.getId());

        mockMvc.perform(post("/api/v1/roadmaps")
                        .header("token", StpUtil.getTokenValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"内容\", \"description\": \"描述\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

        StpUtil.logout();
    }

    /**
     * 3.3 字段验证 - content 缺失
     */
    @Test
    @DisplayName("字段验证 - content 缺失")
    void testCreateRoadmap_ContentMissing_Fail() throws Exception {
        UserDO user = createUser("user12@test.com");
        ProfessionDO profession = createProfession("专业6", user.getId());
        StpUtil.login(user.getId());

        mockMvc.perform(post("/api/v1/roadmaps")
                        .header("token", StpUtil.getTokenValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"professionId\": " + profession.getId() + ", \"description\": \"描述\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

        StpUtil.logout();
    }

    /**
     * 3.4 字段验证 - description 缺失
     */
    @Test
    @DisplayName("字段验证 - description 缺失")
    void testCreateRoadmap_DescriptionMissing_Fail() throws Exception {
        UserDO user = createUser("user13@test.com");
        ProfessionDO profession = createProfession("专业7", user.getId());
        StpUtil.login(user.getId());

        mockMvc.perform(post("/api/v1/roadmaps")
                        .header("token", StpUtil.getTokenValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"professionId\": " + profession.getId() + ", \"content\": \"内容\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

        StpUtil.logout();
    }

    /**
     * 3.5 业务验证 - 专业不存在
     */
    @Test
    @DisplayName("业务验证 - 专业不存在")
    void testCreateRoadmap_ProfessionNotFound_Fail() throws Exception {
        UserDO user = createUser("user14@test.com");
        StpUtil.login(user.getId());

        mockMvc.perform(post("/api/v1/roadmaps")
                        .header("token", StpUtil.getTokenValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"professionId\": 99999, \"content\": \"内容\", \"description\": \"描述\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.PROFESSION_NOT_FOUND.getCode()));

        StpUtil.logout();
    }

    /**
     * 3.6 权限验证 - 未登录
     */
    @Test
    @DisplayName("权限验证 - 未登录创建路线图")
    void testCreateRoadmap_NotLoggedIn_Fail() throws Exception {
        mockMvc.perform(post("/api/v1/roadmaps")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"professionId\": 1, \"content\": \"内容\", \"description\": \"描述\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));
    }

    // ==================== 接口4: 获取路线图详情 ====================

    /**
     * 4.1 成功获取路线图详情
     */
    @Test
    @DisplayName("成功获取路线图详情")
    void testGetRoadmap_Success() throws Exception {
        UserDO user = createUser("user15@test.com");
        ProfessionDO profession = createProfession("专业8", user.getId());
        RoadmapDO roadmap = createPublishedRoadmap(profession.getId(), user.getId(), "详情测试");

        StpUtil.login(user.getId());

        mockMvc.perform(get("/api/v1/roadmaps/" + roadmap.getId())
                        .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.id").value(roadmap.getId()))
                .andExpect(jsonPath("$.data.creator").exists())
                .andExpect(jsonPath("$.data.profession").exists())
                .andExpect(jsonPath("$.data.upvoted").exists());

        StpUtil.logout();
    }

    /**
     * 4.2 字段验证 - id 无效（0）
     */
    @Test
    @DisplayName("字段验证 - roadmapId 无效为0")
    void testGetRoadmap_IdZero_Fail() throws Exception {
        UserDO user = createUser("user16@test.com");
        StpUtil.login(user.getId());

        mockMvc.perform(get("/api/v1/roadmaps/0")
                        .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

        StpUtil.logout();
    }

    /**
     * 4.3 业务验证 - 路线图不存在
     */
    @Test
    @DisplayName("业务验证 - 路线图不存在")
    void testGetRoadmap_NotFound_Fail() throws Exception {
        UserDO user = createUser("user17@test.com");
        StpUtil.login(user.getId());

        mockMvc.perform(get("/api/v1/roadmaps/99999")
                        .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.ROADMAP_NOT_FOUND.getCode()));

        StpUtil.logout();
    }

    /**
     * 4.4 权限验证 - 未登录
     */
    @Test
    @DisplayName("权限验证 - 未登录获取路线图详情")
    void testGetRoadmap_NotLoggedIn_Fail() throws Exception {
        mockMvc.perform(get("/api/v1/roadmaps/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));
    }

    // ==================== 接口6: 获取当前用户创建的路线图列表 ====================

    /**
     * 6.1 成功获取路线图列表 - 包含所有状态
     */
    @Test
    @DisplayName("成功获取当前用户路线图列表 - 包含所有状态")
    void testGetCurrentUserRoadmaps_AllStates_Success() throws Exception {
        UserDO user = createUser("user22@test.com");
        ProfessionDO profession = createProfession("专业11", user.getId());

        // 创建不同状态的路线图
        RoadmapDO published = createPublishedRoadmap(profession.getId(), user.getId(), "已发布");
        RoadmapDO draft = createDraftRoadmap(profession.getId(), user.getId(), "草稿");

        StpUtil.login(user.getId());

        mockMvc.perform(get("/api/v1/users/me/roadmaps")
                        .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].profession").exists())
                .andExpect(jsonPath("$.data[0].profession.name").value(profession.getName()));

        StpUtil.logout();
    }

    /**
     * 6.2 成功获取路线图列表 - 无路线图
     */
    @Test
    @DisplayName("成功获取当前用户路线图列表 - 无路线图")
    void testGetCurrentUserRoadmaps_NoRoadmaps_Success() throws Exception {
        UserDO user = createUser("user23@test.com");

        StpUtil.login(user.getId());

        mockMvc.perform(get("/api/v1/users/me/roadmaps")
                        .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));

        StpUtil.logout();
    }

    /**
     * 6.3 权限验证 - 未登录
     */
    @Test
    @DisplayName("权限验证 - 未登录获取当前用户路线图")
    void testGetCurrentUserRoadmaps_NotLoggedIn_Fail() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/roadmaps"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));
    }

    // ==================== 接口7: 获取指定用户创建的路线图列表 ====================

    /**
     * 7.1 成功获取路线图列表 - 只返回已发布
     */
    @Test
    @DisplayName("成功获取指定用户路线图列表 - 只返回已发布")
    void testGetUserRoadmaps_OnlyPublished_Success() throws Exception {
        UserDO user = createUser("user24@test.com");
        ProfessionDO profession = createProfession("专业12", user.getId());

        // 创建不同状态的路线图
        RoadmapDO published1 = createPublishedRoadmap(profession.getId(), user.getId(), "已发布1");
        RoadmapDO published2 = createPublishedRoadmap(profession.getId(), user.getId(), "已发布2");
        RoadmapDO draft = createDraftRoadmap(profession.getId(), user.getId(), "草稿");

        mockMvc.perform(get("/api/v1/users/" + user.getId() + "/roadmaps?lastId=0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].profession").exists());
    }

    /**
     * 7.2 成功获取路线图列表 - 无已发布路线图
     */
    @Test
    @DisplayName("成功获取指定用户路线图列表 - 无已发布路线图")
    void testGetUserRoadmaps_NoPublished_Success() throws Exception {
        UserDO user = createUser("user25@test.com");
        ProfessionDO profession = createProfession("专业13", user.getId());

        // 只创建草稿
        RoadmapDO draft = createDraftRoadmap(profession.getId(), user.getId(), "草稿");

        mockMvc.perform(get("/api/v1/users/" + user.getId() + "/roadmaps?lastId=0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    /**
     * 7.3 字段验证 - userId 无效（0）
     */
    @Test
    @DisplayName("字段验证 - userId 无效为0")
    void testGetUserRoadmaps_UserIdZero_Fail() throws Exception {
        mockMvc.perform(get("/api/v1/users/0/roadmaps?lastId=0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
    }

    /**
     * 7.5 字段验证 - lastId 缺失
     */
    @Test
    @DisplayName("字段验证 - lastId 缺失")
    void testGetUserRoadmaps_LastIdMissing_Fail() throws Exception {
        UserDO user = createUser("user26@test.com");

        mockMvc.perform(get("/api/v1/users/" + user.getId() + "/roadmaps"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
    }

    /**
     * 7.7 权限验证 - 不需要登录
     */
    @Test
    @DisplayName("权限验证 - 不需要登录获取指定用户路线图")
    void testGetUserRoadmaps_NoLoginRequired_Success() throws Exception {
        UserDO user = createUser("user27@test.com");
        ProfessionDO profession = createProfession("专业14", user.getId());
        RoadmapDO roadmap = createPublishedRoadmap(profession.getId(), user.getId(), "公开");

        // 不传token也能获取
        mockMvc.perform(get("/api/v1/users/" + user.getId() + "/roadmaps?lastId=0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    // ==================== 接口8: 删除路线图 ====================

    /**
     * 8.1 成功删除路线图
     */
    @Test
    @DisplayName("成功删除路线图")
    void testDeleteRoadmap_Success() throws Exception {
        UserDO user = createUser("user28@test.com");
        ProfessionDO profession = createProfession("专业15", user.getId());
        RoadmapDO roadmap = createPublishedRoadmap(profession.getId(), user.getId(), "待删除");

        StpUtil.login(user.getId());

        mockMvc.perform(delete("/api/v1/roadmaps/" + roadmap.getId())
                        .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").doesNotExist());

        // 验证数据库：软删除后查询返回 null（被过滤）
        RoadmapDO deleted = roadmapDataService.getById(roadmap.getId());
        assertThat(deleted).isNull();

        StpUtil.logout();
    }

    /**
     * 8.2 字段验证 - id 无效（0）
     */
    @Test
    @DisplayName("字段验证 - roadmapId 无效为0")
    void testDeleteRoadmap_IdZero_Fail() throws Exception {
        UserDO user = createUser("user29@test.com");
        StpUtil.login(user.getId());

        mockMvc.perform(delete("/api/v1/roadmaps/0")
                        .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

        StpUtil.logout();
    }

    /**
     * 8.3 业务验证 - 路线图不存在
     */
    @Test
    @DisplayName("业务验证 - 路线图不存在")
    void testDeleteRoadmap_NotFound_Fail() throws Exception {
        UserDO user = createUser("user30@test.com");
        StpUtil.login(user.getId());

        mockMvc.perform(delete("/api/v1/roadmaps/99999")
                        .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.ROADMAP_NOT_FOUND.getCode()));

        StpUtil.logout();
    }

    /**
     * 8.4 权限验证 - 删除他人的路线图
     */
    @Test
    @DisplayName("权限验证 - 删除他人的路线图")
    void testDeleteRoadmap_NotOwner_Fail() throws Exception {
        UserDO userA = createUser("userA2@test.com");
        ProfessionDO profession = createProfession("专业16", userA.getId());
        UserDO userB = createUser("userB2@test.com");
        RoadmapDO roadmap = createPublishedRoadmap(profession.getId(), userA.getId(), "他人路线图");

        StpUtil.login(userB.getId());

        mockMvc.perform(delete("/api/v1/roadmaps/" + roadmap.getId())
                        .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.PERMISSION_DENIED.getCode()));

        StpUtil.logout();
    }

    /**
     * 8.6 权限验证 - 未登录
     */
    @Test
    @DisplayName("权限验证 - 未登录删除路线图")
    void testDeleteRoadmap_NotLoggedIn_Fail() throws Exception {
        mockMvc.perform(delete("/api/v1/roadmaps/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));
    }

    // ==================== 业务场景测试 ====================

    /**
     * 11.1 完整创建-修改-删除流程
     */
    @Test
    @DisplayName("完整创建-修改-删除流程")
    void testRoadmapLifecycle_Success() throws Exception {
        UserDO user = createUser("user31@test.com");
        ProfessionDO profession = createProfession("专业17", user.getId());

        StpUtil.login(user.getId());

        // 1. 创建路线图
        String createBody = String.format(
                "{\"professionId\": %d, \"content\": \"%s\", \"description\": \"初始描述\"}",
                profession.getId(), getRoadmapContent());

        String createResponse = mockMvc.perform(post("/api/v1/roadmaps")
                        .header("token", StpUtil.getTokenValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();

        Long roadmapId = objectMapper.readTree(createResponse).get("data").asLong();

        // 2. 修改路线图
        mockMvc.perform(put("/api/v1/roadmaps/" + roadmapId)
                        .header("token", StpUtil.getTokenValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"[[[1,2]],[1,2]]\", \"description\": \"修改后描述\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 3. 删除路线图
        mockMvc.perform(delete("/api/v1/roadmaps/" + roadmapId)
                        .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 4. 验证软删除：查询返回 null（被过滤）
        RoadmapDO deleted = roadmapDataService.getById(roadmapId);
        assertThat(deleted).isNull();

        StpUtil.logout();
    }

    /**
     * 11.3 跨用户隔离
     */
    @Test
    @DisplayName("跨用户隔离")
    void testCrossUserIsolation_Success() throws Exception {
        UserDO userA = createUser("userA3@test.com");
        ProfessionDO profession = createProfession("专业18", userA.getId());
        UserDO userB = createUser("userB3@test.com");

        // 用户A创建路线图
        RoadmapDO roadmapA = createPublishedRoadmap(profession.getId(), userA.getId(), "用户A的路线图");

        // 用户B创建路线图
        RoadmapDO roadmapB = createPublishedRoadmap(profession.getId(), userB.getId(), "用户B的路线图");

        // 验证用户A只能看到自己的
        StpUtil.login(userA.getId());
        mockMvc.perform(get("/api/v1/users/me/roadmaps")
                        .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(roadmapA.getId()));
        StpUtil.logout();

        // 验证用户B只能看到自己的
        StpUtil.login(userB.getId());
        mockMvc.perform(get("/api/v1/users/me/roadmaps")
                        .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(roadmapB.getId()));
        StpUtil.logout();
    }
}
