package com.prosper.learn.web.v1.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prosper.learn.content.profession.ProfessionDO;
import com.prosper.learn.content.profession.ProfessionDataService;
import com.prosper.learn.shared.domain.Enums.ContentState;
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

import java.nio.charset.Charset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 职业管理接口测试
 * 测试文档: docs/api/profession.md
 *
 * Command 测试 - 写操作
 * Query 测试 - 读操作
 */
@Transactional
public class ProfessionsControllerTest extends BaseControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProfessionDataService professionDataService;

    @Autowired
    private UserDomainService userDomainService;

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

    // ==================== Command 测试（写操作） ====================

    /**
     * 测试1: 创建职业 - 成功创建和字段验证
     *
     * 字段要求:
     * - name: 必填(@NotBlank), 最大长度100字符(@Size)
     * - description: 必填(@NotBlank), 最大长度500字符(@Size)
     * - mainCategory: 必填(@NotNull), 必须 > 0(@Positive)
     * - subCategory: 必填(@NotNull), 必须 > 0(@Positive)
     * - skills: 可选, 最大长度1000字符(@Size)
     *
     * 业务规则:
     * - 创建后状态为 SUBMITTED (待审核)
     * - creatorId 自动填充为当前用户ID
     * - price, icon, reason 默认为空字符串
     */
    @Test
    void testCreateProfession() throws Exception {
        UserDO user = createUser("creator@example.com");
        StpUtil.login(user.getId());

        try {
            // 1. 成功创建职业
            String validRequest = """
                {
                    "name": "Java开发工程师",
                    "description": "负责Java后端开发工作，参与系统架构设计与优化",
                    "mainCategory": 1,
                    "subCategory": 2,
                    "skills": "Java, Spring Boot, MySQL, Redis, 微服务架构"
                }
                """;

            mockMvc.perform(post("/api/v1/professions")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.message").value("操作成功"));

            // 验证：职业已创建
            List<ProfessionDO> professions = professionDataService.listByStateAndLastId(
                ContentState.SUBMITTED.value(), null, 100);
            ProfessionDO createdProfession = professions.stream()
                    .filter(p -> "Java开发工程师".equals(p.getName()))
                    .findFirst()
                    .orElse(null);

            assertThat(createdProfession).isNotNull();
            assertThat(createdProfession.getState()).isEqualTo(ContentState.SUBMITTED.value());
            assertThat(createdProfession.getCreatorId()).isEqualTo(user.getId());
            assertThat(createdProfession.getMainCategory()).isEqualTo(1);
            assertThat(createdProfession.getSubCategory()).isEqualTo(2);
            assertThat(createdProfession.getSkills()).isEqualTo("Java, Spring Boot, MySQL, Redis, 微服务架构");
            assertThat(createdProfession.getIcon()).isEqualTo("");
            assertThat(createdProfession.getReason()).isEqualTo("");

            // 2. 字段验证 - name 为空
            String emptyNameRequest = """
                {
                    "name": "",
                    "description": "负责Java后端开发工作",
                    "mainCategory": 1,
                    "subCategory": 2,
                    "skills": "Java"
                }
                """;

            mockMvc.perform(post("/api/v1/professions")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(emptyNameRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

            // 3. 字段验证 - description 为空
            String emptyDescRequest = """
                {
                    "name": "Python开发工程师",
                    "description": "",
                    "mainCategory": 1,
                    "subCategory": 2
                }
                """;

            mockMvc.perform(post("/api/v1/professions")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(emptyDescRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

            // 4. 字段验证 - mainCategory 缺失
            String missingMainCategoryRequest = """
                {
                    "name": "前端工程师",
                    "description": "负责前端开发工作",
                    "subCategory": 2
                }
                """;

            mockMvc.perform(post("/api/v1/professions")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(missingMainCategoryRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

            // 5. 参数验证 - mainCategory = 0
            String invalidCategoryRequest = """
                {
                    "name": "测试工程师",
                    "description": "负责软件测试工作",
                    "mainCategory": 0,
                    "subCategory": 1,
                    "skills": "测试技能"
                }
                """;

            mockMvc.perform(post("/api/v1/professions")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidCategoryRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

            // 6. 业务验证 - 主分类不存在（mainCategory = 999999）
            String nonExistentMainCategoryRequest = """
                {
                    "name": "测试职业",
                    "description": "这是一个测试职业的详细描述信息",
                    "mainCategory": 999999,
                    "subCategory": 1,
                    "skills": "测试技能"
                }
                """;

            mockMvc.perform(post("/api/v1/professions")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(nonExistentMainCategoryRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.PROFESSION_CATEGORY_INVALID.getCode()));

            // 7. 业务验证 - 子分类不存在（subCategory = 999999）
            String nonExistentSubCategoryRequest = """
                {
                    "name": "测试职业",
                    "description": "这是一个测试职业的详细描述信息",
                    "mainCategory": 1,
                    "subCategory": 999999,
                    "skills": "测试技能"
                }
                """;

            mockMvc.perform(post("/api/v1/professions")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(nonExistentSubCategoryRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.PROFESSION_CATEGORY_INVALID.getCode()));

            // 8. 未登录创建职业
            StpUtil.logout();
            mockMvc.perform(post("/api/v1/professions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));

        } finally {
            if (StpUtil.isLogin()) {
                StpUtil.logout();
            }
        }
    }

    // ==================== Query 测试（读操作） ====================

    /**
     * 测试2: 获取职业详情 - 各种场景
     *
     * API: GET /api/v1/professions/{id}
     *
     * 测试场景:
     * 1. 获取已发布职业
     * 2. 职业不存在
     * 3. 职业ID无效（0、负数）
     * 4. 未登录访问
     */
    @Test
    void testGetProfessionDetail() throws Exception {
        // 准备：创建用户和职业
        UserDO user = createUser("user@example.com");

        String createRequest = """
            {
                "name": "产品经理",
                "description": "负责产品规划和需求分析工作，协调各部门推进产品开发",
                "mainCategory": 1,
                "subCategory": 2,
                "skills": "产品设计, 需求分析, 项目管理"
            }
            """;

        StpUtil.login(user.getId());
        mockMvc.perform(post("/api/v1/professions")
                .header("token", StpUtil.getTokenValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));
        StpUtil.logout();

        // 从数据库查询创建的职业
        List<ProfessionDO> professions = professionDataService.listByStateAndLastId(
            ContentState.SUBMITTED.value(), null, 100);
        ProfessionDO profession = professions.stream()
                .filter(p -> "产品经理".equals(p.getName()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("职业未创建"));
        Long professionId = profession.getId();

        // 准备：审核通过职业
        professionDataService.approve(professionId);

        // 1. 获取已发布职业（已登录）
        StpUtil.login(user.getId());
        mockMvc.perform(get("/api/v1/professions/{id}", professionId)
                .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                .andExpect(jsonPath("$.data.id").value(professionId));

        // 2. 职业不存在
        mockMvc.perform(get("/api/v1/professions/{id}", 99999L)
                .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.PROFESSION_NOT_FOUND.getCode()));

        // 3. 职业ID无效 - ID = 0
        mockMvc.perform(get("/api/v1/professions/{id}", 0L)
                .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

        // 4. 职业ID无效 - ID = -1
        mockMvc.perform(get("/api/v1/professions/{id}", -1L)
                .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

        StpUtil.logout();
    }

    /**
     * 测试3: 获取职业列表 - 各种筛选条件
     *
     * API: GET /api/v1/professions
     *
     * 查询参数:
     * - mainCategory: 可选(@Positive), 主分类ID
     * - subCategory: 可选(@Positive), 子分类ID
     * - lastId: 可选(@Positive), 分页最后ID
     *
     * 测试场景:
     * 1. 获取所有已发布职业（无筛选条件）
     * 2. 按主分类筛选
     * 3. 按主分类+子分类筛选
     * 4. 分页功能（lastId）
     * 5. 参数验证
     */
    @Test
    void testGetProfessionList() throws Exception {
        UserDO user = createUser("list@example.com");
        StpUtil.login(user.getId());

        try {
            // 准备：创建多个测试职业
            String[] professionData = {
                "{\"name\":\"Java开发\",\"description\":\"Java后端开发\",\"mainCategory\":1,\"subCategory\":1,\"skills\":\"Java\"}",
                "{\"name\":\"Python开发\",\"description\":\"Python开发\",\"mainCategory\":1,\"subCategory\":2,\"skills\":\"Python\"}",
                "{\"name\":\"UI设计师\",\"description\":\"用户界面设计\",\"mainCategory\":2,\"subCategory\":1,\"skills\":\"Photoshop\"}"
            };

            for (String data : professionData) {
                mockMvc.perform(post("/api/v1/professions")
                        .header("token", StpUtil.getTokenValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(data))
                        .andExpect(status().isOk());
            }

            // 审核通过所有职业
            List<ProfessionDO> professions = professionDataService.listByStateAndLastId(
                ContentState.SUBMITTED.value(), null, 100);
            ProfessionDO profession1 = professions.stream()
                .filter(p -> "Java开发".equals(p.getName())).findFirst().orElseThrow();
            ProfessionDO profession2 = professions.stream()
                .filter(p -> "Python开发".equals(p.getName())).findFirst().orElseThrow();
            ProfessionDO profession3 = professions.stream()
                .filter(p -> "UI设计师".equals(p.getName())).findFirst().orElseThrow();

            professionDataService.approve(profession1.getId());
            professionDataService.approve(profession2.getId());
            professionDataService.approve(profession3.getId());

            // 1. 获取所有已发布职业
            String allResponse = mockMvc.perform(get("/api/v1/professions")
                    .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data.items").isArray())
                    .andExpect(jsonPath("$.data.hasMore").isBoolean())
                    .andReturn().getResponse().getContentAsString();

            JsonNode allData = objectMapper.readTree(allResponse).get("data").get("items");
            assertThat(allData.size()).isGreaterThanOrEqualTo(3);

            // 2. 按主分类筛选（主分类1）
            String mainCat1Response = mockMvc.perform(get("/api/v1/professions")
                    .param("mainCategory", "1")
                    .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data.items").isArray())
                    .andReturn().getResponse().getContentAsString();

            JsonNode mainCat1Data = objectMapper.readTree(mainCat1Response).get("data").get("items");
            assertThat(mainCat1Data.size()).isGreaterThanOrEqualTo(2);

            // 验证：所有职业的主分类都是1
            for (JsonNode prof : mainCat1Data) {
                assertThat(prof.get("mainCategory").asInt()).isEqualTo(1);
            }

            // 3. 按主分类+子分类筛选
            String subCat1Response = mockMvc.perform(get("/api/v1/professions")
                    .param("mainCategory", "1")
                    .param("subCategory", "1")
                    .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data.items").isArray())
                    .andReturn().getResponse().getContentAsString();

            JsonNode subCat1Data = objectMapper.readTree(subCat1Response).get("data").get("items");
            assertThat(subCat1Data.size()).isGreaterThanOrEqualTo(1);

            // 验证：分类正确
            for (JsonNode prof : subCat1Data) {
                assertThat(prof.get("mainCategory").asInt()).isEqualTo(1);
                assertThat(prof.get("subCategory").asInt()).isEqualTo(1);
            }

            // 4. 分页功能（lastId）
            Long firstProfessionId = profession1.getId();
            String pagedResponse = mockMvc.perform(get("/api/v1/professions")
                    .param("lastId", firstProfessionId.toString())
                    .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data.items").isArray())
                    .andReturn().getResponse().getContentAsString();

            JsonNode pagedData = objectMapper.readTree(pagedResponse).get("data").get("items");
            // 验证：返回的职业ID都应该小于lastId
            for (JsonNode prof : pagedData) {
                assertThat(prof.get("id").asLong()).isLessThan(firstProfessionId);
            }

            // 5. 参数验证 - mainCategory = 0
            mockMvc.perform(get("/api/v1/professions")
                    .param("mainCategory", "0")
                    .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

            // 6. 参数验证 - subCategory = -1
            mockMvc.perform(get("/api/v1/professions")
                    .param("mainCategory", "1")
                    .param("subCategory", "-1")
                    .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试4: 搜索职业 - 关键词搜索
     *
     * API: GET /api/v1/professions/search
     *
     * 查询参数:
     * - keyword: 必填(@NotBlank), 搜索关键词
     *
     * 测试场景:
     * 1. 搜索成功 - 找到匹配的职业
     * 2. 搜索无结果
     * 3. 关键词验证 - 空字符串
     */
    @Test
    void testSearchProfessions() throws Exception {
        UserDO user = createUser("search@example.com");
        StpUtil.login(user.getId());

        try {
            // 准备：创建测试职业
            String[] professionNames = {
                "Java高级开发工程师",
                "Python数据分析师",
                "JavaScript前端工程师"
            };

            for (String name : professionNames) {
                String request = String.format("""
                    {
                        "name": "%s",
                        "description": "这是关于%s的详细职业介绍",
                        "mainCategory": 1,
                        "subCategory": 1,
                        "skills": "编程"
                    }
                    """, name, name);

                mockMvc.perform(post("/api/v1/professions")
                        .header("token", StpUtil.getTokenValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                        .andExpect(status().isOk());
            }

            // 审核通过所有职业
            List<ProfessionDO> professions = professionDataService.listByStateAndLastId(
                ContentState.SUBMITTED.value(), null, 100);
            for (ProfessionDO profession : professions) {
                if (profession.getName().contains("Java") || profession.getName().contains("Python")
                    || profession.getName().contains("JavaScript")) {
                    professionDataService.approve(profession.getId());
                }
            }

            // 1. 搜索成功 - 搜索"Java"
            String javaSearchResponse = mockMvc.perform(get("/api/v1/professions/search")
                    .param("keyword", "Java")
                    .header("token", StpUtil.getTokenValue())
                    .characterEncoding("UTF-8"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data").isArray())
                    .andReturn().getResponse().getContentAsString(Charset.forName("UTF-8"));

            JsonNode javaSearchData = objectMapper.readTree(javaSearchResponse).get("data");
            assertThat(javaSearchData.size()).isGreaterThanOrEqualTo(1);

            // 验证：所有结果都包含"Java"
            for (JsonNode prof : javaSearchData) {
                String name = prof.get("name").asText();
                assertThat(name).containsIgnoringCase("Java");
            }

            // 2. 搜索无结果
            mockMvc.perform(get("/api/v1/professions/search")
                    .param("keyword", "不存在的职业12345")
                    .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data").isEmpty());

            // 3. 关键词验证 - 空字符串
            mockMvc.perform(get("/api/v1/professions/search")
                    .param("keyword", "")
                    .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

            // 4. 关键词验证 - 只有空格
            mockMvc.perform(get("/api/v1/professions/search")
                    .param("keyword", "   ")
                    .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试5: 获取热门职业
     *
     * API: GET /api/v1/professions/hot
     *
     * 查询参数:
     * - limit: 可选(@Positive), 限制返回数量, 默认10
     *
     * 测试场景:
     * 1. 默认数量
     * 2. 自定义数量
     * 3. limit参数验证
     * 4. 无数据时返回空列表
     */
    @Test
    void testGetHotProfessions() throws Exception {
        UserDO user = createUser("hot@example.com");
        StpUtil.login(user.getId());

        try {
            // 准备：创建测试职业
            for (int i = 1; i <= 5; i++) {
                String request = String.format("""
                    {
                        "name": "热门职业%d",
                        "description": "这是热门职业%d的详细描述",
                        "mainCategory": 1,
                        "subCategory": 1,
                        "skills": "技能%d"
                    }
                    """, i, i, i);

                mockMvc.perform(post("/api/v1/professions")
                        .header("token", StpUtil.getTokenValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                        .andExpect(status().isOk());
            }

            // 审核通过所有职业
            List<ProfessionDO> professions = professionDataService.listByStateAndLastId(
                ContentState.SUBMITTED.value(), null, 100);
            for (ProfessionDO profession : professions) {
                if (profession.getName().startsWith("热门职业")) {
                    professionDataService.approve(profession.getId());
                }
            }

            // 1. 默认数量
            mockMvc.perform(get("/api/v1/professions/hot")
                    .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data").isArray());

            // 2. 自定义数量（limit=3）
            String limitedResponse = mockMvc.perform(get("/api/v1/professions/hot")
                    .param("limit", "3")
                    .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data").isArray())
                    .andReturn().getResponse().getContentAsString();

            JsonNode limitedData = objectMapper.readTree(limitedResponse).get("data");
            assertThat(limitedData.size()).isLessThanOrEqualTo(3);

            // 3. limit参数验证 - limit = 0
            mockMvc.perform(get("/api/v1/professions/hot")
                    .param("limit", "0")
                    .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

            // 4. limit参数验证 - limit = -1
            mockMvc.perform(get("/api/v1/professions/hot")
                    .param("limit", "-1")
                    .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

            // 5. 无热门数据时不会报错
            mockMvc.perform(get("/api/v1/professions/hot")
                    .param("limit", "10")
                    .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data").isArray());

        } finally {
            StpUtil.logout();
        }
    }
}
