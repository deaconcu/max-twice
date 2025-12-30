package com.prosper.learn.web.v1.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prosper.learn.application.dto.request.CreateCardRequest;
import com.prosper.learn.application.dto.request.CreateCourseRequest;
import com.prosper.learn.application.dto.request.UpdateCardRequest;
import com.prosper.learn.application.service.CourseService;
import com.prosper.learn.content.course.CourseDO;
import com.prosper.learn.content.course.CourseDataService;
import com.prosper.learn.content.node.NodeDO;
import com.prosper.learn.content.node.NodeDataService;
import com.prosper.learn.memory.card.*;
import com.prosper.learn.memory.deck.MemoryCardDeckDO;
import com.prosper.learn.memory.deck.MemoryCardDeckDataService;
import com.prosper.learn.memory.deck.MemoryCardDeckDomainService;
import com.prosper.learn.memory.review.UserCardSrsDO;
import com.prosper.learn.memory.review.UserCardSrsDataService;
import com.prosper.learn.shared.domain.Enums.ContentState;
import com.prosper.learn.shared.domain.exception.StatusCode;
import com.prosper.learn.user.profile.UserDO;
import com.prosper.learn.user.profile.UserDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 记忆卡片接口测试
 * 测试文档: docs/test/memory-card.md
 */
@Transactional
public class MemoryCardControllerTest extends BaseControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserDomainService userDomainService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private CourseDataService courseDataService;

    @Autowired
    private NodeDataService nodeDataService;

    @Autowired
    private MemoryCardDeckDomainService deckDomainService;

    @Autowired
    private MemoryCardDeckDataService deckDataService;

    @Autowired
    private MemoryCardDomainService cardDomainService;

    @Autowired
    private MemoryCardDataService cardDataService;

    @Autowired
    private MemoryCardVersionDataService cardVersionDataService;

    @Autowired
    private UserCardSrsDataService srsDataService;

    // 测试数据
    private UserDO testUser;
    private UserDO otherUser;
    private CourseDO testCourse;
    private NodeDO testNode;
    private MemoryCardDeckDO testDeck;
    private MemoryCardDO testCard1;
    private MemoryCardDO testCard2;
    private String testUserToken;
    private String otherUserToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // 创建测试用户
        testUser = userDomainService.createUser("test@example.com", "password123");
        otherUser = userDomainService.createUser("other@example.com", "password123");

        // 生成 Token
        testUserToken = generateToken(testUser.getId());
        otherUserToken = generateToken(otherUser.getId());

        // 创建测试课程
        CreateCourseRequest courseRequest = new CreateCourseRequest();
        courseRequest.setName("测试课程");
        courseRequest.setDescription("测试课程描述，至少需要20个字符才能满足验证要求");
        courseRequest.setMainCategory(1);
        courseRequest.setSubCategory(1);
        courseService.createCourse(courseRequest, testUser);

        // 获取刚创建的课程
        testCourse = courseDataService.listByStateAndLastId(ContentState.SUBMITTED, null).stream()
            .filter(c -> "测试课程".equals(c.getName()))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("未找到测试课程"));

        // 获取根节点
        testNode = nodeDataService.getById(testCourse.getRootNodeId());

        // 创建测试卡片组
        testDeck = deckDomainService.createDeck(
            testUser.getId(),
            0L,
            testNode.getId(),
            "测试卡片组描述",
            0
        );

        // 创建测试卡片
        testCard1 = cardDomainService.createCard(testUser.getId(), testDeck.getId(), "问题1", "答案1");
        testCard2 = cardDomainService.createCard(testUser.getId(), testDeck.getId(), "问题2", "答案2");

        // 创建 SRS 状态
        createSrsState(testUser.getId(), testCard1, testDeck);
        createSrsState(testUser.getId(), testCard2, testDeck);
    }

    // ========== 辅助方法 ==========

    /**
     * 创建 SRS 学习状态
     */
    private UserCardSrsDO createSrsState(Long userId, MemoryCardDO card, MemoryCardDeckDO deck) {
        UserCardSrsDO srs = new UserCardSrsDO();
        srs.setUserId(userId);
        srs.setCardId(card.getId());
        srs.setNodeId(testNode.getId());
        srs.setDeckId(deck.getId());
        srs.setType((byte)0);  // NEW 状态
        srs.setCurrentStep((byte)0);
        srs.setInterval(0);
        srs.setReviewDueAt(LocalDateTime.now());
        srs.setLastReviewedAt(null);
        srs.setRepetitions(0);
        srs.setLapseCount(0);
        srs.setEaseFactor(BigDecimal.valueOf(2.5));
        srs.setCardVersionId(card.getCurrentVersionId());  // 使用真实的版本ID
        srs.setDeckVersion(deck.getVersion());             // 使用真实的版本号
        srsDataService.insert(srs);
        return srs;
    }

    /**
     * 生成测试 Token
     */
    private String generateToken(Long userId) {
        StpUtil.login(userId);
        return StpUtil.getTokenValue();
    }

    // ========== 一、创建卡片接口测试 ==========

    @Test
    void testCreateCard_Success() throws Exception {
        CreateCardRequest request = new CreateCardRequest();
        request.setDeckId(testDeck.getId());
        request.setFront("什么是闭包？");
        request.setBack("闭包是函数和其周围词法环境的组合");

        mockMvc.perform(post("/api/v1/memory/cards")
                .header("token", testUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
            .andExpect(jsonPath("$.message").value("创建成功"))
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void testCreateCard_MissingDeckId() throws Exception {
        CreateCardRequest request = new CreateCardRequest();
        request.setFront("什么是闭包？");
        request.setBack("闭包是函数和其周围词法环境的组合");

        mockMvc.perform(post("/api/v1/memory/cards")
                .header("token", testUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
    }

    @Test
    void testCreateCard_MissingFront() throws Exception {
        CreateCardRequest request = new CreateCardRequest();
        request.setDeckId(testDeck.getId());
        request.setBack("闭包是函数和其周围词法环境的组合");

        mockMvc.perform(post("/api/v1/memory/cards")
                .header("token", testUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
    }

    @Test
    void testCreateCard_MissingBack() throws Exception {
        CreateCardRequest request = new CreateCardRequest();
        request.setDeckId(testDeck.getId());
        request.setFront("什么是闭包？");

        mockMvc.perform(post("/api/v1/memory/cards")
                .header("token", testUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
    }

    @Test
    void testCreateCard_FrontTooLong() throws Exception {
        CreateCardRequest request = new CreateCardRequest();
        request.setDeckId(testDeck.getId());
        request.setFront("A".repeat(3000));
        request.setBack("答案");

        mockMvc.perform(post("/api/v1/memory/cards")
                .header("token", testUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
    }

    @Test
    void testCreateCard_BackTooLong() throws Exception {
        CreateCardRequest request = new CreateCardRequest();
        request.setDeckId(testDeck.getId());
        request.setFront("问题");
        request.setBack("A".repeat(3000));

        mockMvc.perform(post("/api/v1/memory/cards")
                .header("token", testUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
    }

    @Test
    void testCreateCard_EmptyFront() throws Exception {
        CreateCardRequest request = new CreateCardRequest();
        request.setDeckId(testDeck.getId());
        request.setFront("   ");
        request.setBack("答案");

        mockMvc.perform(post("/api/v1/memory/cards")
                .header("token", testUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
    }

    @Test
    void testCreateCard_DeckNotFound() throws Exception {
        CreateCardRequest request = new CreateCardRequest();
        request.setDeckId(999999L);
        request.setFront("什么是闭包？");
        request.setBack("闭包是函数和其周围词法环境的组合");

        mockMvc.perform(post("/api/v1/memory/cards")
                .header("token", testUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.MEMORY_CARD_DECK_NOT_FOUND.getCode()));
    }

    @Test
    void testCreateCard_NotLogin() throws Exception {
        CreateCardRequest request = new CreateCardRequest();
        request.setDeckId(testDeck.getId());
        request.setFront("什么是闭包？");
        request.setBack("闭包是函数和其周围词法环境的组合");

        mockMvc.perform(post("/api/v1/memory/cards")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));
    }

    // ========== 二、更新卡片接口测试 ==========

    @Test
    void testUpdateCard_Success() throws Exception {
        UpdateCardRequest request = new UpdateCardRequest();
        request.setFront("什么是闭包？（更新后）");
        request.setBack("闭包是函数和其词法环境的组合（更新后）");

        mockMvc.perform(put("/api/v1/memory/cards/" + testCard1.getId())
                .header("token", testUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
            .andExpect(jsonPath("$.message").value("更新成功"))
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void testUpdateCard_CardNotFound() throws Exception {
        UpdateCardRequest request = new UpdateCardRequest();
        request.setFront("更新的问题");
        request.setBack("更新的答案");

        mockMvc.perform(put("/api/v1/memory/cards/999999")
                .header("token", testUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.MEMORY_CARD_NOT_FOUND.getCode()));
    }

    @Test
    void testUpdateCard_InvalidCardId() throws Exception {
        UpdateCardRequest request = new UpdateCardRequest();
        request.setFront("更新的问题");
        request.setBack("更新的答案");

        mockMvc.perform(put("/api/v1/memory/cards/0")
                .header("token", testUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
    }

    @Test
    void testUpdateCard_PermissionDenied() throws Exception {
        UpdateCardRequest request = new UpdateCardRequest();
        request.setFront("更新的问题");
        request.setBack("更新的答案");

        mockMvc.perform(put("/api/v1/memory/cards/" + testCard1.getId())
                .header("token", otherUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.PERMISSION_DENIED.getCode()));
    }

    @Test
    void testUpdateCard_FrontTooLong() throws Exception {
        UpdateCardRequest request = new UpdateCardRequest();
        request.setFront("A".repeat(2100));
        request.setBack("答案");

        mockMvc.perform(put("/api/v1/memory/cards/" + testCard1.getId())
                .header("token", testUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
    }

    @Test
    void testUpdateCard_BackTooLong() throws Exception {
        UpdateCardRequest request = new UpdateCardRequest();
        request.setFront("问题");
        request.setBack("A".repeat(2100));

        mockMvc.perform(put("/api/v1/memory/cards/" + testCard1.getId())
                .header("token", testUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
    }

    @Test
    void testUpdateCard_NotLogin() throws Exception {
        UpdateCardRequest request = new UpdateCardRequest();
        request.setFront("更新的问题");
        request.setBack("更新的答案");

        mockMvc.perform(put("/api/v1/memory/cards/" + testCard1.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));
    }

    // ========== 三、获取节点下卡片列表接口测试 ==========

    @Test
    void testGetCardsByNode_Success() throws Exception {
        mockMvc.perform(get("/api/v1/memory/cards/node/" + testNode.getId())
                .header("token", testUserToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void testGetCardsByNode_EmptyList() throws Exception {
        // 创建一个新课程和节点
        CreateCourseRequest courseRequest = new CreateCourseRequest();
        courseRequest.setName("空课程");
        courseRequest.setDescription("空课程描述，至少需要20个字符才能满足验证要求");
        courseRequest.setMainCategory(1);
        courseRequest.setSubCategory(1);
        courseService.createCourse(courseRequest, testUser);

        CourseDO emptyCourse = courseDataService.listByStateAndLastId(ContentState.SUBMITTED, null).stream()
            .filter(c -> "空课程".equals(c.getName()))
            .findFirst()
            .orElseThrow();
        NodeDO emptyNode = nodeDataService.getById(emptyCourse.getRootNodeId());

        mockMvc.perform(get("/api/v1/memory/cards/node/" + emptyNode.getId())
                .header("token", testUserToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void testGetCardsByNode_ContainsCompleteData() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/memory/cards/node/" + testNode.getId())
                .header("token", testUserToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
            .andReturn();

        String content = result.getResponse().getContentAsString();
        assertTrue(content.contains("\"id\""));
        assertTrue(content.contains("\"front\""));
        assertTrue(content.contains("\"back\""));
        assertTrue(content.contains("\"deck\""));
        assertTrue(content.contains("\"creator\""));
        assertTrue(content.contains("\"srsState\""));
    }

    @Test
    void testGetCardsByNode_InvalidNodeId() throws Exception {
        mockMvc.perform(get("/api/v1/memory/cards/node/0")
                .header("token", testUserToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
    }

    @Test
    void testGetCardsByNode_NotLogin() throws Exception {
        mockMvc.perform(get("/api/v1/memory/cards/node/" + testNode.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));
    }

    // ========== 四、获取卡片内容差异接口测试 ==========

    @Test
    void testGetCardDiff_NoDiff() throws Exception {
        mockMvc.perform(get("/api/v1/memory/cards/" + testCard1.getId() + "/diff")
                .header("token", testUserToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));
    }

    @Test
    void testGetCardDiff_CardNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/memory/cards/999999/diff")
                .header("token", testUserToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.MEMORY_CARD_NOT_FOUND.getCode()));
    }

    @Test
    void testGetCardDiff_PermissionDenied() throws Exception {
        mockMvc.perform(get("/api/v1/memory/cards/" + testCard1.getId() + "/diff")
                .header("token", otherUserToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
    }

    @Test
    void testGetCardDiff_InvalidCardId() throws Exception {
        mockMvc.perform(get("/api/v1/memory/cards/-1/diff")
                .header("token", testUserToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
    }

    @Test
    void testGetCardDiff_NotLogin() throws Exception {
        mockMvc.perform(get("/api/v1/memory/cards/" + testCard1.getId() + "/diff"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));
    }

    // ========== 五、删除卡片接口测试 ==========

    @Test
    void testDeleteCard_Success() throws Exception {
        mockMvc.perform(delete("/api/v1/memory/cards/" + testCard1.getId())
                .header("token", testUserToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
            .andExpect(jsonPath("$.message").value("删除成功"))
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void testDeleteCard_CardNotFound() throws Exception {
        mockMvc.perform(delete("/api/v1/memory/cards/999999")
                .header("token", testUserToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.MEMORY_CARD_NOT_FOUND.getCode()));
    }

    @Test
    void testDeleteCard_AlreadyDeleted() throws Exception {
        // 先删除一次
        mockMvc.perform(delete("/api/v1/memory/cards/" + testCard1.getId())
                .header("token", testUserToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));

        // 再删除一次
        mockMvc.perform(delete("/api/v1/memory/cards/" + testCard1.getId())
                .header("token", testUserToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.MEMORY_CARD_NOT_FOUND.getCode()));
    }

    @Test
    void testDeleteCard_PermissionDenied() throws Exception {
        mockMvc.perform(delete("/api/v1/memory/cards/" + testCard1.getId())
                .header("token", otherUserToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.PERMISSION_DENIED.getCode()));
    }

    @Test
    void testDeleteCard_InvalidCardId() throws Exception {
        mockMvc.perform(delete("/api/v1/memory/cards/0")
                .header("token", testUserToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
    }

    @Test
    void testDeleteCard_NotLogin() throws Exception {
        mockMvc.perform(delete("/api/v1/memory/cards/" + testCard1.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));
    }

    // ========== 六、响应格式测试 ==========

    @Test
    void testCreateCard_ResponseFormat() throws Exception {
        CreateCardRequest request = new CreateCardRequest();
        request.setDeckId(testDeck.getId());
        request.setFront("问题");
        request.setBack("答案");

        MvcResult result = mockMvc.perform(post("/api/v1/memory/cards")
                .header("token", testUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn();

        String content = result.getResponse().getContentAsString();
        assertTrue(content.contains("\"code\""));
        assertTrue(content.contains("\"message\""));
        assertTrue(content.contains("\"timestamp\""));
        assertFalse(content.contains("\"data\""));
    }

    @Test
    void testUpdateCard_ResponseFormat() throws Exception {
        UpdateCardRequest request = new UpdateCardRequest();
        request.setFront("更新的问题");
        request.setBack("更新的答案");

        MvcResult result = mockMvc.perform(put("/api/v1/memory/cards/" + testCard1.getId())
                .header("token", testUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn();

        String content = result.getResponse().getContentAsString();
        assertTrue(content.contains("\"code\""));
        assertTrue(content.contains("\"message\""));
        assertTrue(content.contains("\"timestamp\""));
        assertFalse(content.contains("\"data\""));
    }

    @Test
    void testDeleteCard_ResponseFormat() throws Exception {
        MvcResult result = mockMvc.perform(delete("/api/v1/memory/cards/" + testCard1.getId())
                .header("token", testUserToken))
            .andExpect(status().isOk())
            .andReturn();

        String content = result.getResponse().getContentAsString();
        assertTrue(content.contains("\"code\""));
        assertTrue(content.contains("\"message\""));
        assertTrue(content.contains("\"timestamp\""));
        assertFalse(content.contains("\"data\""));
    }

    // ========== 七、边界条件测试 ==========

    @Test
    void testCreateCard_SpecialCharacters() throws Exception {
        CreateCardRequest request = new CreateCardRequest();
        request.setDeckId(testDeck.getId());
        request.setFront("问题包含特殊字符：<>\"'&\n\t😀");
        request.setBack("答案包含特殊字符：<>\"'&\n\t😀");

        mockMvc.perform(post("/api/v1/memory/cards")
                .header("token", testUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));
    }

    @Test
    void testCreateCard_UnicodeCharacters() throws Exception {
        CreateCardRequest request = new CreateCardRequest();
        request.setDeckId(testDeck.getId());
        request.setFront("问题：日本語、中文、한국어");
        request.setBack("答案：🎉🎊🎈");

        mockMvc.perform(post("/api/v1/memory/cards")
                .header("token", testUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));
    }

    // ========== 八、集成测试 ==========

    @Test
    void testCardLifecycle_Complete() throws Exception {
        // 1. 查询卡片列表（验证 testCard1 存在）
        MvcResult listResult = mockMvc.perform(get("/api/v1/memory/cards/node/" + testNode.getId())
                .header("token", testUserToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(2))  // 验证返回2条记录
            .andReturn();

        // 2. 更新卡片
        UpdateCardRequest updateRequest = new UpdateCardRequest();
        updateRequest.setFront("问题1（已更新）");
        updateRequest.setBack("答案1（已更新）");

        mockMvc.perform(put("/api/v1/memory/cards/" + testCard1.getId())
                .header("token", testUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));

        // 3. 验证更新后可以查看差异
        mockMvc.perform(get("/api/v1/memory/cards/" + testCard1.getId() + "/diff")
                .header("token", testUserToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));

        // 4. 删除卡片
        mockMvc.perform(delete("/api/v1/memory/cards/" + testCard1.getId())
                .header("token", testUserToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));

        // 5. 验证删除后不能再次访问
        mockMvc.perform(get("/api/v1/memory/cards/" + testCard1.getId() + "/diff")
                .header("token", testUserToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.MEMORY_CARD_NOT_FOUND.getCode()));
    }
}
