package com.prosper.learn.web.v1.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prosper.learn.content.course.CourseDO;
import com.prosper.learn.content.course.CourseDataService;
import com.prosper.learn.shared.domain.Enums.ContentState;
import com.prosper.learn.shared.domain.exception.StatusCode;
import com.prosper.learn.user.profile.UserDO;
import com.prosper.learn.user.profile.UserDomainService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.Charset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 公开接口测试（不需要登录）
 */
@Transactional
public class PublicControllerTest extends BaseControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserDomainService userDomainService;

    @Autowired
    private CourseDataService courseDataService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    /**
     * 测试：未登录用户查询课程列表
     */
    @Test
    @DisplayName("公开接口 - 未登录用户查询课程列表")
    void testGetPublicCourses() throws Exception {
        // 准备测试数据
        UserDO user = userDomainService.createUser("public-test@test.com", "password123");

        // 创建已发布课程
        for (int i = 1; i <= 5; i++) {
            CourseDO course = new CourseDO();
            course.setName("公开课程" + i);
            course.setDescription("这是公开课程" + i + "的描述");
            course.setCreatorId(user.getId());
            course.setState(ContentState.PUBLISHED.value());
            course.setMainCategory(1);
            course.setSubCategory(1);
            course.setRootNodeId(0L);
            course.setParentCourseId(0L);
            courseDataService.insert(course);
        }

        // 未登录用户查询列表（不传token）
        String response = mockMvc.perform(get("/api/v1/public/courses")
                .param("mainCategory", "1")
                .characterEncoding("UTF-8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.hasMore").exists())
                .andReturn().getResponse().getContentAsString(Charset.forName("UTF-8"));

        JsonNode data = objectMapper.readTree(response).get("data").get("items");

        // 验证：未登录用户的subscribed和progress都是默认值
        for (JsonNode course : data) {
            assertThat(course.get("subscribed").asBoolean()).isFalse();
            assertThat(course.get("progress").asInt()).isEqualTo(0);
        }
    }
}
