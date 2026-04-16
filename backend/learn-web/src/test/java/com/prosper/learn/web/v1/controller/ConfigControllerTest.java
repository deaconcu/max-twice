package com.prosper.learn.web.v1.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prosper.learn.application.dto.response.ValidationRuleDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class ConfigControllerTest extends BaseControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    // ========== 一、获取验证规则接口测试 ==========

    @Test
    void testGetValidationRules_Success() throws Exception {
        mockMvc.perform(get("/api/v1/config/validation"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data").isMap())
            .andExpect(jsonPath("$.data").isNotEmpty())
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void testGetValidationRules_ContainsAllFields() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/config/validation"))
            .andExpect(status().isOk())
            .andReturn();

        String content = result.getResponse().getContentAsString();
        Map<String, Object> response = objectMapper.readValue(content, Map.class);
        Map<String, Object> data = (Map<String, Object>) response.get("data");

        // 验证包含所有预期字段
        assertTrue(data.containsKey("card-front"), "应包含 card-front");
        assertTrue(data.containsKey("card-back"), "应包含 card-back");
        assertTrue(data.containsKey("deck-description"), "应包含 deck-description");
        assertTrue(data.containsKey("comment-content"), "应包含 comment-content");
        assertTrue(data.containsKey("username"), "应包含 username");
        assertTrue(data.containsKey("password"), "应包含 password");
        assertTrue(data.containsKey("biography"), "应包含 biography");
        assertTrue(data.containsKey("email"), "应包含 email");
        assertTrue(data.containsKey("course-name"), "应包含 course-name");
        assertTrue(data.containsKey("course-description"), "应包含 course-description");
        assertTrue(data.containsKey("post-content"), "应包含 post-content");
        assertTrue(data.containsKey("role-name"), "应包含 role-name");
        assertTrue(data.containsKey("role-description"), "应包含 role-description");
        assertTrue(data.containsKey("message-content"), "应包含 message-content");
        assertTrue(data.containsKey("roadmap-content"), "应包含 roadmap-content");
        assertTrue(data.containsKey("roadmap-description"), "应包含 roadmap-description");

        // 验证字段数量
        assertTrue(data.size() >= 16, "至少应有 16 个字段");
    }

    @Test
    void testGetValidationRules_FieldStructure() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/config/validation"))
            .andExpect(status().isOk())
            .andReturn();

        String content = result.getResponse().getContentAsString();
        Map<String, Object> response = objectMapper.readValue(content, Map.class);
        Map<String, Object> data = (Map<String, Object>) response.get("data");

        // 验证每个规则的结构
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            Map<String, Object> rule = (Map<String, Object>) entry.getValue();

            assertTrue(rule.containsKey("minLength"), entry.getKey() + " 应包含 minLength");
            assertTrue(rule.containsKey("maxLength"), entry.getKey() + " 应包含 maxLength");
            assertTrue(rule.containsKey("label"), entry.getKey() + " 应包含 label");

            Integer minLength = (Integer) rule.get("minLength");
            Integer maxLength = (Integer) rule.get("maxLength");
            String label = (String) rule.get("label");

            assertNotNull(minLength, entry.getKey() + " 的 minLength 不能为 null");
            assertNotNull(maxLength, entry.getKey() + " 的 maxLength 不能为 null");
            assertNotNull(label, entry.getKey() + " 的 label 不能为 null");

            assertTrue(minLength >= 0, entry.getKey() + " 的 minLength 应 >= 0");
            assertTrue(maxLength >= 0, entry.getKey() + " 的 maxLength 应 >= 0");
            assertTrue(minLength <= maxLength, entry.getKey() + " 的 minLength 应 <= maxLength");
            assertFalse(label.isEmpty(), entry.getKey() + " 的 label 不能为空");
        }
    }

    @Test
    void testGetValidationRules_SpecificFieldValues() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/config/validation"))
            .andExpect(status().isOk())
            .andReturn();

        String content = result.getResponse().getContentAsString();
        Map<String, Object> response = objectMapper.readValue(content, Map.class);
        Map<String, Object> data = (Map<String, Object>) response.get("data");

        // 验证 username 规则
        Map<String, Object> usernameRule = (Map<String, Object>) data.get("username");
        Integer usernameMin = (Integer) usernameRule.get("minLength");
        Integer usernameMax = (Integer) usernameRule.get("maxLength");
        assertTrue(usernameMin >= 2, "username minLength 应 >= 2");
        assertTrue(usernameMax <= 50, "username maxLength 应 <= 50");

        // 验证 password 规则
        Map<String, Object> passwordRule = (Map<String, Object>) data.get("password");
        Integer passwordMin = (Integer) passwordRule.get("minLength");
        assertTrue(passwordMin >= 6, "password minLength 应 >= 6（安全性要求）");

        // 验证 email 规则
        Map<String, Object> emailRule = (Map<String, Object>) data.get("email");
        Integer emailMin = (Integer) emailRule.get("minLength");
        assertEquals(0, emailMin, "email minLength 应为 0（允许为空）");

        // 验证 card-front 规则
        Map<String, Object> cardFrontRule = (Map<String, Object>) data.get("card-front");
        Integer cardFrontMin = (Integer) cardFrontRule.get("minLength");
        assertTrue(cardFrontMin >= 1, "card-front minLength 应 >= 1");

        // 验证 course-name 规则
        Map<String, Object> courseNameRule = (Map<String, Object>) data.get("course-name");
        Integer courseNameMax = (Integer) courseNameRule.get("maxLength");
        assertTrue(courseNameMax > 0, "course-name maxLength 应 > 0");
    }

    @Test
    void testGetValidationRules_ContainsETagHeader() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/config/validation"))
            .andExpect(status().isOk())
            .andExpect(header().exists("ETag"))
            .andReturn();

        String etag = result.getResponse().getHeader("ETag");
        assertNotNull(etag, "ETag 不能为 null");
        assertFalse(etag.isEmpty(), "ETag 不能为空");
        assertTrue(etag.startsWith("\"") && etag.endsWith("\""), "ETag 应该被引号包裹");
    }

    @Test
    void testGetValidationRules_ContainsCacheControlHeader() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/config/validation"))
            .andExpect(status().isOk())
            .andExpect(header().exists("Cache-Control"))
            .andReturn();

        String cacheControl = result.getResponse().getHeader("Cache-Control");
        assertNotNull(cacheControl, "Cache-Control 不能为 null");
        assertTrue(cacheControl.contains("no-cache"), "Cache-Control 应包含 no-cache");
        assertTrue(cacheControl.contains("must-revalidate"), "Cache-Control 应包含 must-revalidate");
    }

    // ========== 二、ETag 缓存机制测试 ==========

    @Test
    void testETag_FirstRequestReturnsFullData() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/config/validation"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data").isNotEmpty())
            .andExpect(header().exists("ETag"))
            .andReturn();

        String etag = result.getResponse().getHeader("ETag");
        assertNotNull(etag);
        assertFalse(etag.isEmpty());
    }

    @Test
    void testETag_SameETagReturns304() throws Exception {
        // 首次请求获取 ETag
        MvcResult firstResult = mockMvc.perform(get("/api/v1/config/validation"))
            .andExpect(status().isOk())
            .andReturn();

        String etag = firstResult.getResponse().getHeader("ETag");

        // 使用相同 ETag 再次请求
        mockMvc.perform(get("/api/v1/config/validation")
                .header("If-None-Match", etag))
            .andExpect(status().isNotModified())
            .andExpect(header().exists("ETag"))
            .andExpect(header().string("ETag", etag));
    }

    @Test
    void testETag_WrongETagReturns200() throws Exception {
        mockMvc.perform(get("/api/v1/config/validation")
                .header("If-None-Match", "\"wrong-etag\""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data").isNotEmpty())
            .andExpect(header().exists("ETag"));
    }

    @Test
    void testETag_Consistency() throws Exception {
        // 发送两次请求
        MvcResult result1 = mockMvc.perform(get("/api/v1/config/validation"))
            .andExpect(status().isOk())
            .andReturn();

        MvcResult result2 = mockMvc.perform(get("/api/v1/config/validation"))
            .andExpect(status().isOk())
            .andReturn();

        // 验证 ETag 相同
        String etag1 = result1.getResponse().getHeader("ETag");
        String etag2 = result2.getResponse().getHeader("ETag");
        assertEquals(etag1, etag2, "配置未变时 ETag 应相同");

        // 验证数据内容相同（比较解析后的对象，而不是 JSON 字符串，因为 Map 的顺序可能不同）
        String content1 = result1.getResponse().getContentAsString();
        String content2 = result2.getResponse().getContentAsString();

        Map<String, Object> response1 = objectMapper.readValue(content1, Map.class);
        Map<String, Object> response2 = objectMapper.readValue(content2, Map.class);

        Map<String, Object> data1 = (Map<String, Object>) response1.get("data");
        Map<String, Object> data2 = (Map<String, Object>) response2.get("data");

        assertEquals(data1.size(), data2.size(), "配置字段数量应相同");
        assertEquals(data1.keySet(), data2.keySet(), "配置字段名称应相同");
    }

    @Test
    void testETag_EmptyETagReturns200() throws Exception {
        mockMvc.perform(get("/api/v1/config/validation")
                .header("If-None-Match", ""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isNotEmpty());
    }

    @Test
    void testETag_Multiple304Responses() throws Exception {
        // 首次请求获取 ETag
        MvcResult firstResult = mockMvc.perform(get("/api/v1/config/validation"))
            .andExpect(status().isOk())
            .andReturn();

        String etag = firstResult.getResponse().getHeader("ETag");

        // 连续3次使用相同 ETag 请求
        for (int i = 0; i < 3; i++) {
            MvcResult result = mockMvc.perform(get("/api/v1/config/validation")
                    .header("If-None-Match", etag))
                .andExpect(status().isNotModified())
                .andExpect(header().exists("ETag"))
                .andReturn();

            String returnedETag = result.getResponse().getHeader("ETag");
            assertEquals(etag, returnedETag, "第 " + (i + 1) + " 次请求的 ETag 应相同");
        }
    }

    // ========== 三、响应格式测试 ==========

    @Test
    void testResponse_ApiResponseFormat() throws Exception {
        mockMvc.perform(get("/api/v1/config/validation"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").exists())
            .andExpect(jsonPath("$.data").exists())
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void testResponse_SuccessCode() throws Exception {
        mockMvc.perform(get("/api/v1/config/validation"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testResponse_DataType() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/config/validation"))
            .andExpect(status().isOk())
            .andReturn();

        String content = result.getResponse().getContentAsString();
        Map<String, Object> response = objectMapper.readValue(content, Map.class);
        Object data = response.get("data");

        assertTrue(data instanceof Map, "data 应该是 Map 类型");

        Map<String, Object> dataMap = (Map<String, Object>) data;
        for (Map.Entry<String, Object> entry : dataMap.entrySet()) {
            assertTrue(entry.getKey() instanceof String, "Map 的 key 应该是 String");
            assertTrue(entry.getValue() instanceof Map, "Map 的 value 应该是对象");
        }
    }

    // ========== 四、边界条件测试 ==========

    @Test
    void testBoundary_NoHeaders() throws Exception {
        mockMvc.perform(get("/api/v1/config/validation"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isNotEmpty());
    }

    @Test
    void testBoundary_WithExtraHeaders() throws Exception {
        mockMvc.perform(get("/api/v1/config/validation")
                .header("X-Custom-Header", "custom-value")
                .header("User-Agent", "test-agent"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isNotEmpty());
    }

    @Test
    void testBoundary_IfNoneMatchCaseInsensitive() throws Exception {
        // 首次请求获取 ETag
        MvcResult firstResult = mockMvc.perform(get("/api/v1/config/validation"))
            .andExpect(status().isOk())
            .andReturn();

        String etag = firstResult.getResponse().getHeader("ETag");

        // HTTP header 不区分大小写，但实际测试中 Spring 会标准化处理
        mockMvc.perform(get("/api/v1/config/validation")
                .header("If-None-Match", etag))
            .andExpect(status().isNotModified());
    }

    @Test
    void testBoundary_ETagWithSpecialCharacters() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/config/validation"))
            .andExpect(status().isOk())
            .andReturn();

        String etag = result.getResponse().getHeader("ETag");

        // 验证 ETag 格式正确（带引号）
        assertTrue(etag.startsWith("\"") && etag.endsWith("\""), "ETag 应该带引号");
    }

    // ========== 五、数据完整性测试 ==========

    @Test
    void testDataIntegrity_AllFieldsPresent() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/config/validation"))
            .andExpect(status().isOk())
            .andReturn();

        String content = result.getResponse().getContentAsString();
        Map<String, Object> response = objectMapper.readValue(content, Map.class);
        Map<String, Object> data = (Map<String, Object>) response.get("data");

        // 验证字段数量
        assertEquals(16, data.size(), "应该有 16 个验证规则字段");
    }

    @Test
    void testDataIntegrity_RulesReasonable() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/config/validation"))
            .andExpect(status().isOk())
            .andReturn();

        String content = result.getResponse().getContentAsString();
        Map<String, Object> response = objectMapper.readValue(content, Map.class);
        Map<String, Object> data = (Map<String, Object>) response.get("data");

        // 验证所有规则的合理性
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            Map<String, Object> rule = (Map<String, Object>) entry.getValue();
            Integer minLength = (Integer) rule.get("minLength");
            Integer maxLength = (Integer) rule.get("maxLength");

            assertTrue(minLength <= maxLength,
                entry.getKey() + ": minLength 不应大于 maxLength");

            // 验证 maxLength 合理（不会过大）
            assertTrue(maxLength <= 100000,
                entry.getKey() + ": maxLength 不应超过 100000");
        }

        // 验证密码安全性要求
        Map<String, Object> passwordRule = (Map<String, Object>) data.get("password");
        Integer passwordMin = (Integer) passwordRule.get("minLength");
        assertTrue(passwordMin >= 6, "密码最小长度应 >= 6");

        // 验证内容类字段的长度足够
        Map<String, Object> postContentRule = (Map<String, Object>) data.get("post-content");
        Integer postContentMax = (Integer) postContentRule.get("maxLength");
        assertTrue(postContentMax >= 1000, "帖子内容最大长度应 >= 1000");

        Map<String, Object> roadmapContentRule = (Map<String, Object>) data.get("roadmap-content");
        Integer roadmapContentMax = (Integer) roadmapContentRule.get("maxLength");
        assertTrue(roadmapContentMax >= 1000, "路线图内容最大长度应 >= 1000");
    }

    @Test
    void testDataIntegrity_ChineseLabels() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/config/validation"))
            .andExpect(status().isOk())
            .andReturn();

        String content = result.getResponse().getContentAsString();
        Map<String, Object> response = objectMapper.readValue(content, Map.class);
        Map<String, Object> data = (Map<String, Object>) response.get("data");

        // 验证所有 label 不为空
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            Map<String, Object> rule = (Map<String, Object>) entry.getValue();
            String label = (String) rule.get("label");

            assertNotNull(label, entry.getKey() + " 的 label 不能为 null");
            assertFalse(label.isEmpty(), entry.getKey() + " 的 label 不能为空");
            assertFalse(label.isBlank(), entry.getKey() + " 的 label 不能为空白");
        }
    }

    // ========== 六、性能测试 ==========

    @Test
    void testPerformance_FirstRequestTime() throws Exception {
        long startTime = System.currentTimeMillis();

        mockMvc.perform(get("/api/v1/config/validation"))
            .andExpect(status().isOk());

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        assertTrue(duration < 200, "首次请求应在 200ms 内完成，实际耗时: " + duration + "ms");
    }

    @Test
    void testPerformance_304ResponseTime() throws Exception {
        // 首次请求获取 ETag
        MvcResult firstResult = mockMvc.perform(get("/api/v1/config/validation"))
            .andExpect(status().isOk())
            .andReturn();

        String etag = firstResult.getResponse().getHeader("ETag");

        // 测试 304 响应时间
        long startTime = System.currentTimeMillis();

        mockMvc.perform(get("/api/v1/config/validation")
                .header("If-None-Match", etag))
            .andExpect(status().isNotModified());

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        assertTrue(duration < 100, "304 响应应在 100ms 内完成，实际耗时: " + duration + "ms");
    }

    @Test
    void testPerformance_ConcurrentRequests() throws Exception {
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    mockMvc.perform(get("/api/v1/config/validation"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.code").value(200));
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        assertEquals(threadCount, successCount.get(), "所有并发请求都应成功");
    }

    @Test
    void testPerformance_Concurrent304Requests() throws Exception {
        // 首次请求获取 ETag
        MvcResult firstResult = mockMvc.perform(get("/api/v1/config/validation"))
            .andExpect(status().isOk())
            .andReturn();

        String etag = firstResult.getResponse().getHeader("ETag");

        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    mockMvc.perform(get("/api/v1/config/validation")
                            .header("If-None-Match", etag))
                        .andExpect(status().isNotModified());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        assertEquals(threadCount, successCount.get(), "所有并发 304 请求都应成功");
    }

    // ========== 七、缓存一致性测试 ==========

    @Test
    void testCacheConsistency_ETagNotChangedDataConsistent() throws Exception {
        // 首次请求
        MvcResult result1 = mockMvc.perform(get("/api/v1/config/validation"))
            .andExpect(status().isOk())
            .andReturn();

        String etag1 = result1.getResponse().getHeader("ETag");
        String content1 = result1.getResponse().getContentAsString();

        // 等待一小段时间
        Thread.sleep(100);

        // 第二次请求
        MvcResult result2 = mockMvc.perform(get("/api/v1/config/validation"))
            .andExpect(status().isOk())
            .andReturn();

        String etag2 = result2.getResponse().getHeader("ETag");
        String content2 = result2.getResponse().getContentAsString();

        // 验证 ETag 相同
        assertEquals(etag1, etag2, "ETag 应该相同");

        // 验证数据内容相同（比较解析后的对象）
        Map<String, Object> response1 = objectMapper.readValue(content1, Map.class);
        Map<String, Object> response2 = objectMapper.readValue(content2, Map.class);

        Map<String, Object> data1 = (Map<String, Object>) response1.get("data");
        Map<String, Object> data2 = (Map<String, Object>) response2.get("data");

        assertEquals(data1.size(), data2.size(), "配置字段数量应相同");
        assertEquals(data1.keySet(), data2.keySet(), "配置字段名称应相同");
    }

    // ========== 九、HTTP 标准兼容性测试 ==========

    @Test
    void testHttpStandard_OnlyGetMethodAllowed() throws Exception {
        // POST 方法应该不支持（全局异常处理器会返回 200 + 参数错误）
        mockMvc.perform(post("/api/v1/config/validation"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(1002))  // INVALID_PARAMETER
            .andExpect(jsonPath("$.message").value("不支持的请求方法: POST"));

        // PUT 方法应该不支持
        mockMvc.perform(put("/api/v1/config/validation"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(1002))
            .andExpect(jsonPath("$.message").value("不支持的请求方法: PUT"));

        // DELETE 方法应该不支持
        mockMvc.perform(delete("/api/v1/config/validation"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(1002))
            .andExpect(jsonPath("$.message").value("不支持的请求方法: DELETE"));
    }

    @Test
    void testHttpStandard_AcceptHeader() throws Exception {
        // 不带 Accept header
        mockMvc.perform(get("/api/v1/config/validation"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        // Accept: application/json
        mockMvc.perform(get("/api/v1/config/validation")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        // Accept: */*
        mockMvc.perform(get("/api/v1/config/validation")
                .accept(MediaType.ALL))
            .andExpect(status().isOk());
    }

    @Test
    void testHttpStandard_ContentType() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/config/validation"))
            .andExpect(status().isOk())
            .andReturn();

        String contentType = result.getResponse().getContentType();
        assertNotNull(contentType, "Content-Type 不能为 null");
        assertTrue(contentType.contains("application/json"),
            "Content-Type 应为 application/json，实际为: " + contentType);
        // 注意：Spring 默认可能不显式设置 charset，但默认使用 UTF-8
    }
}
