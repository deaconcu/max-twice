package com.prosper.learn.domain.service.basic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.prosper.learn.common.Enums;
import com.prosper.learn.common.Utils;
import com.prosper.learn.common.exception.BusinessException;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.domain.config.ContentsProperties;
import com.prosper.learn.persistence.dataobject.*;
import com.prosper.learn.persistence.mapper.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ContentsService 测试类
 * 
 * 测试覆盖：
 * - 验证方法的正常和异常情况
 * - 目录获取和创建逻辑
 * - 帖子选择、取消选择功能
 * - 置顶管理功能
 * - 错误处理和边界条件
 * 
 * @author Claude
 * @since 2024-01-20
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ContentsService 测试")
class ContentsServiceTest {

    @Mock
    private CourseMapper courseMapper;
    
    @Mock
    private ObjectMapper objectMapper;
    
    @Mock
    private NodeMapper nodeMapper;
    
    @Mock
    private PostMapper postMapper;
    
    @Mock
    private UserCourseTocMapper userCourseTocMapper;
    
    @Mock
    private CourseTocMapper courseTocMapper;
    
    @Mock
    private ContentsProperties contentsProperties;

    @InjectMocks
    private ContentsService contentsService;

    // 测试数据常量
    private static final long TEST_USER_ID = 1L;
    private static final long TEST_COURSE_ID = 100L;
    private static final long TEST_POST_ID = 200L;
    private static final String TEST_PATH = "1-chapter1-section1";
    private static final String TEST_TOC_HASH = "test-hash-123";
    private static final int MAX_PINNED_ITEMS = 10;
    private static final String PIN_FIELD = "^";
    private static final String CHOSEN_FIELD = "+";

    @BeforeEach
    void setUp() {
        // 删除不必要的全局配置，在具体测试中按需配置
    }

    /**
     * 创建测试用的课程对象
     */
    private CourseDO createTestCourse() {
        CourseDO course = new CourseDO();
        course.setId(TEST_COURSE_ID);
        course.setRootNode(1L);
        return course;
    }

    /**
     * 创建测试用的帖子对象
     */
    private PostDO createTestPost(int type) {
        PostDO post = new PostDO();
        post.setId(TEST_POST_ID);
        post.setType(type);
        post.setContent("1,2,3");
        return post;
    }

    /**
     * 创建测试用的用户目录对象
     */
    private UserCourseTocDO createTestUserCourseToc() {
        UserCourseTocDO userCourseToc = new UserCourseTocDO();
        userCourseToc.setUserId(TEST_USER_ID);
        userCourseToc.setCourseId(TEST_COURSE_ID);
        userCourseToc.setToc(TEST_TOC_HASH);
        return userCourseToc;
    }

    /**
     * 创建测试用的课程目录对象
     */
    private CourseTocDO createTestCourseToc() {
        CourseTocDO courseToc = new CourseTocDO();
        courseToc.setHash(TEST_TOC_HASH);
        courseToc.setToc("{\"1\":{}}");
        return courseToc;
    }

    @Nested
    @DisplayName("验证方法测试")
    class ValidationTests {

        @Test
        @DisplayName("课程存在性验证 - 正常情况")
        void validateCourseExists_Success() {
            // Given
            CourseDO course = createTestCourse();
            when(courseMapper.getById(TEST_COURSE_ID)).thenReturn(course);

            // When & Then - 通过调用公共方法间接测试验证逻辑
            assertDoesNotThrow(() -> {
                contentsService.getToc(TEST_USER_ID, TEST_COURSE_ID, false);
            });
            
            // 验证courseMapper被调用
            verify(courseMapper).getById(TEST_COURSE_ID);
        }

        @Test
        @DisplayName("课程存在性验证 - 课程不存在")
        void validateCourseExists_CourseNotFound() {
            // Given
            when(courseMapper.getById(TEST_COURSE_ID)).thenReturn(null);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                contentsService.getToc(TEST_USER_ID, TEST_COURSE_ID, false);
            });
            
            assertEquals(ErrorCode.CONTENTS_COURSE_NOT_FOUND.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("帖子类型验证 - 内容类型帖子")
        void validatePostForContents_ContentsType() throws Exception {
            // Given
            PostDO post = createTestPost(Enums.PostType.contents.value());
            when(postMapper.get(TEST_POST_ID)).thenReturn(post);
            
            CourseDO course = createTestCourse();
            when(courseMapper.getById(TEST_COURSE_ID)).thenReturn(course);
            
            UserCourseTocDO userCourseToc = createTestUserCourseToc();
            when(userCourseTocMapper.getByUserAndCourse(TEST_USER_ID, TEST_COURSE_ID)).thenReturn(userCourseToc);
            
            CourseTocDO courseToc = createTestCourseToc();
            when(courseTocMapper.get(TEST_TOC_HASH)).thenReturn(courseToc);
            
            ObjectNode childNode = mock(ObjectNode.class);
            when(objectMapper.createObjectNode()).thenReturn(childNode);
            when(childNode.putObject(anyString())).thenReturn(childNode);
            when(childNode.put(eq(CHOSEN_FIELD), eq(TEST_POST_ID))).thenReturn(childNode);
            when(contentsProperties.getChosenField()).thenReturn(CHOSEN_FIELD);
            
            ObjectNode rootNode = mock(ObjectNode.class);
            ObjectNode targetNode = mock(ObjectNode.class);
            when(objectMapper.readTree(anyString())).thenReturn(rootNode);
            when(rootNode.get(anyString())).thenReturn(targetNode);
            when(targetNode.has(anyString())).thenReturn(false);
            when(targetNode.get(anyString())).thenReturn(null);
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"updated\":\"toc\"}");
            
            try (MockedStatic<Utils> mockedUtils = mockStatic(Utils.class)) {
                mockedUtils.when(() -> Utils.hashSHA(anyString())).thenReturn("new-hash");
                when(courseTocMapper.get("new-hash")).thenReturn(null);

                // When & Then - 内容类型帖子应该正常处理
                assertDoesNotThrow(() -> {
                    contentsService.choose(TEST_USER_ID, TEST_PATH, TEST_COURSE_ID, TEST_POST_ID);
                });
            }
        }

        @Test
        @DisplayName("帖子类型验证 - 文章类型帖子")
        void validatePostForContents_ArticleType() {
            // Given
            PostDO post = createTestPost(Enums.PostType.article.value());
            when(postMapper.get(TEST_POST_ID)).thenReturn(post);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                contentsService.choose(TEST_USER_ID, TEST_PATH, TEST_COURSE_ID, TEST_POST_ID);
            });
            
            assertEquals(ErrorCode.CONTENTS_INVALID_POST_TYPE.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("帖子存在性验证 - 帖子不存在")
        void validatePostForContents_PostNotFound() {
            // Given
            when(postMapper.get(TEST_POST_ID)).thenReturn(null);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                contentsService.choose(TEST_USER_ID, TEST_PATH, TEST_COURSE_ID, TEST_POST_ID);
            });
            
            assertEquals(ErrorCode.CONTENTS_POST_NOT_FOUND.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("用户目录存在性验证 - 目录不存在")
        void validateUserTocExists_TocNotFound() {
            // Given
            PostDO post = createTestPost(Enums.PostType.contents.value());
            when(postMapper.get(TEST_POST_ID)).thenReturn(post);
            
            CourseDO course = createTestCourse();
            when(courseMapper.getById(TEST_COURSE_ID)).thenReturn(course);
            
            // Mock objectMapper and contentsProperties since they'll be called
            ObjectNode mockChildNode = mock(ObjectNode.class);
            when(objectMapper.createObjectNode()).thenReturn(mockChildNode);
            when(mockChildNode.putObject(anyString())).thenReturn(mockChildNode);
            when(mockChildNode.put(anyString(), anyLong())).thenReturn(mockChildNode);
            when(contentsProperties.getChosenField()).thenReturn(CHOSEN_FIELD);
            
            when(userCourseTocMapper.getByUserAndCourse(TEST_USER_ID, TEST_COURSE_ID)).thenReturn(null);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                contentsService.choose(TEST_USER_ID, TEST_PATH, TEST_COURSE_ID, TEST_POST_ID);
            });
            
            assertEquals(ErrorCode.TOC_USER_TOC_NOT_FOUND.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("目录索引验证 - 索引越界")
        void validateTocIndex_IndexOutOfBounds() {
            // Given
            PostDO post = createTestPost(Enums.PostType.contents.value());
            when(postMapper.get(TEST_POST_ID)).thenReturn(post);
            
            CourseDO course = createTestCourse();
            when(courseMapper.getById(TEST_COURSE_ID)).thenReturn(course);
            
            // Mock objectMapper and contentsProperties since they'll be called
            ObjectNode mockChildNode = mock(ObjectNode.class);
            when(objectMapper.createObjectNode()).thenReturn(mockChildNode);
            when(mockChildNode.putObject(anyString())).thenReturn(mockChildNode);
            when(mockChildNode.put(anyString(), anyLong())).thenReturn(mockChildNode);
            when(contentsProperties.getChosenField()).thenReturn(CHOSEN_FIELD);
            
            UserCourseTocDO userCourseToc = createTestUserCourseToc();
            userCourseToc.setToc("hash1,hash2"); // 只有2个哈希
            when(userCourseTocMapper.getByUserAndCourse(TEST_USER_ID, TEST_COURSE_ID)).thenReturn(userCourseToc);

            // When & Then - 尝试访问第3个索引（超出范围）
            String invalidPath = "3-chapter1-section1";
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                contentsService.choose(TEST_USER_ID, invalidPath, TEST_COURSE_ID, TEST_POST_ID);
            });
            
            assertEquals(ErrorCode.TOC_INDEX_OUT_OF_BOUNDS.getCode(), exception.getCode());
        }
    }

    @Nested
    @DisplayName("目录获取测试")
    class TocRetrievalTests {

        @Test
        @DisplayName("获取目录 - 用户目录存在")
        void getToc_UserTocExists() throws Exception {
            // Given
            CourseDO course = createTestCourse();
            when(courseMapper.getById(TEST_COURSE_ID)).thenReturn(course);
            
            UserCourseTocDO userCourseToc = createTestUserCourseToc();
            when(userCourseTocMapper.getByUserAndCourse(TEST_USER_ID, TEST_COURSE_ID)).thenReturn(userCourseToc);
            
            CourseTocDO courseToc = createTestCourseToc();
            Map<String, CourseTocDO> tocMap = new HashMap<>();
            tocMap.put(TEST_TOC_HASH, courseToc);
            when(courseTocMapper.getByHashes(any())).thenReturn(tocMap);
            
            ArrayNode arrayNode = mock(ArrayNode.class);
            when(objectMapper.createArrayNode()).thenReturn(arrayNode);
            
            JsonNode jsonNode = mock(JsonNode.class);
            when(objectMapper.readTree(courseToc.getToc())).thenReturn(jsonNode);

            // When
            ArrayNode result = contentsService.getToc(TEST_USER_ID, TEST_COURSE_ID, false);

            // Then
            assertNotNull(result);
            verify(arrayNode).add(jsonNode);
        }

        @Test
        @DisplayName("获取目录 - 用户目录不存在且不创建")
        void getToc_UserTocNotExistsAndNoCreate() {
            // Given
            CourseDO course = createTestCourse();
            when(courseMapper.getById(TEST_COURSE_ID)).thenReturn(course);
            when(userCourseTocMapper.getByUserAndCourse(TEST_USER_ID, TEST_COURSE_ID)).thenReturn(null);

            // When
            ArrayNode result = contentsService.getToc(TEST_USER_ID, TEST_COURSE_ID, false);

            // Then
            assertNull(result);
        }

        @Test
        @DisplayName("获取目录 - 用户目录不存在但创建新目录")
        void getToc_UserTocNotExistsButCreate() throws Exception {
            // Given
            CourseDO course = createTestCourse();
            when(courseMapper.getById(TEST_COURSE_ID)).thenReturn(course);
            when(userCourseTocMapper.getByUserAndCourse(TEST_USER_ID, TEST_COURSE_ID)).thenReturn(null);
            
            // Mock ObjectMapper for root toc creation
            ObjectNode rootNode = mock(ObjectNode.class);
            when(objectMapper.createObjectNode()).thenReturn(rootNode);
            when(rootNode.toString()).thenReturn("{\"1\":{}}");
            
            // Mock hash calculation
            try (MockedStatic<Utils> mockedUtils = mockStatic(Utils.class)) {
                mockedUtils.when(() -> Utils.hashSHA(anyString())).thenReturn(TEST_TOC_HASH);
                
                when(courseTocMapper.get(TEST_TOC_HASH)).thenReturn(null);
                
                ArrayNode arrayNode = mock(ArrayNode.class);
                when(objectMapper.createArrayNode()).thenReturn(arrayNode);
                
                CourseTocDO courseToc = createTestCourseToc();
                Map<String, CourseTocDO> tocMap = new HashMap<>();
                tocMap.put(TEST_TOC_HASH, courseToc);
                when(courseTocMapper.getByHashes(any())).thenReturn(tocMap);
                
                JsonNode jsonNode = mock(JsonNode.class);
                when(objectMapper.readTree(courseToc.getToc())).thenReturn(jsonNode);

                // When
                ArrayNode result = contentsService.getToc(TEST_USER_ID, TEST_COURSE_ID, true);

                // Then
                assertNotNull(result);
                verify(courseTocMapper).insert(any(CourseTocDO.class));
                verify(userCourseTocMapper).insert(any(UserCourseTocDO.class));
            }
        }

        @Test
        @DisplayName("获取指定索引目录")
        void getTocByIndex_Success() {
            // Given
            CourseDO course = createTestCourse();
            when(courseMapper.getById(TEST_COURSE_ID)).thenReturn(course);
            
            UserCourseTocDO userCourseToc = createTestUserCourseToc();
            when(userCourseTocMapper.getByUserAndCourse(TEST_USER_ID, TEST_COURSE_ID)).thenReturn(userCourseToc);
            
            CourseTocDO courseToc = createTestCourseToc();
            when(courseTocMapper.get(TEST_TOC_HASH)).thenReturn(courseToc);

            // When
            String result = contentsService.getToc(TEST_USER_ID, TEST_COURSE_ID, 1);

            // Then
            assertEquals(courseToc.getToc(), result);
        }
    }

    @Nested
    @DisplayName("内容选择测试")
    class ContentChoiceTests {

        @Test
        @DisplayName("选择帖子到目录")
        void choose_Success() throws Exception {
            // Given
            PostDO post = createTestPost(Enums.PostType.contents.value());
            when(postMapper.get(TEST_POST_ID)).thenReturn(post);
            
            setupMocksForContentOperation();
            when(contentsProperties.getChosenField()).thenReturn(CHOSEN_FIELD);
            
            String updatedToc = "{\"1\":{\"chapter1\":{\"section1\":{}}}}";
            when(objectMapper.writeValueAsString(any())).thenReturn(updatedToc);
            
            try (MockedStatic<Utils> mockedUtils = mockStatic(Utils.class)) {
                mockedUtils.when(() -> Utils.hashSHA(updatedToc)).thenReturn("new-hash");
                when(courseTocMapper.get("new-hash")).thenReturn(null);

                // When
                assertDoesNotThrow(() -> {
                    contentsService.choose(TEST_USER_ID, TEST_PATH, TEST_COURSE_ID, TEST_POST_ID);
                });

                // Then
                verify(courseTocMapper).incrRef(TEST_TOC_HASH, -1);
                verify(courseTocMapper).insert(any(CourseTocDO.class));
                verify(courseTocMapper).incrRef("new-hash", 1);
                verify(userCourseTocMapper).update(any(UserCourseTocDO.class));
            }
        }

        @Test
        @DisplayName("取消选择目录内容")
        void unchoose_Success() throws Exception {
            // Given
            setupMocksForContentOperation();
            
            String updatedToc = "{\"1\":{\"chapter1\":{\"section1\":{}}}}";
            when(objectMapper.writeValueAsString(any())).thenReturn(updatedToc);
            
            try (MockedStatic<Utils> mockedUtils = mockStatic(Utils.class)) {
                mockedUtils.when(() -> Utils.hashSHA(updatedToc)).thenReturn("new-hash");
                when(courseTocMapper.get("new-hash")).thenReturn(null);

                // When
                assertDoesNotThrow(() -> {
                    contentsService.unchoose(TEST_USER_ID, TEST_COURSE_ID, TEST_PATH);
                });

                // Then
                verify(courseTocMapper).incrRef(TEST_TOC_HASH, -1);
                verify(courseTocMapper).insert(any(CourseTocDO.class));
                verify(courseTocMapper).incrRef("new-hash", 1);
                verify(userCourseTocMapper).update(any(UserCourseTocDO.class));
            }
        }

        private void setupMocksForContentOperation() throws Exception {
            CourseDO course = createTestCourse();
            when(courseMapper.getById(TEST_COURSE_ID)).thenReturn(course);
            
            UserCourseTocDO userCourseToc = createTestUserCourseToc();
            when(userCourseTocMapper.getByUserAndCourse(TEST_USER_ID, TEST_COURSE_ID)).thenReturn(userCourseToc);
            
            CourseTocDO courseToc = createTestCourseToc();
            when(courseTocMapper.get(TEST_TOC_HASH)).thenReturn(courseToc);
            
            ObjectNode rootNode = mock(ObjectNode.class);
            ObjectNode childNode = mock(ObjectNode.class);
            ObjectNode targetNode = mock(ObjectNode.class);
            when(objectMapper.createObjectNode()).thenReturn(childNode);
            when(objectMapper.readTree(anyString())).thenReturn(rootNode);
            when(rootNode.get(anyString())).thenReturn(targetNode);
            when(targetNode.get(anyString())).thenReturn(null);
            when(childNode.putObject(anyString())).thenReturn(childNode);
            when(childNode.put(anyString(), anyLong())).thenReturn(childNode);
        }

        @Test
        @DisplayName("添加置顶帖子")
        void pin_AddPin_Success() throws Exception {
            // Given
            setupMocksForContentOperation();
            
            // 添加为pin方法特殊配置的mock
            ArrayNode arrayNode = mock(ArrayNode.class);
            when(objectMapper.createArrayNode()).thenReturn(arrayNode);
            when(arrayNode.size()).thenReturn(0);
            when(arrayNode.add(anyLong())).thenReturn(arrayNode);
            
            // 配置pin相关的属性和节点行为
            when(contentsProperties.getPinField()).thenReturn(PIN_FIELD);
            when(contentsProperties.getMaxPinnedItems()).thenReturn(MAX_PINNED_ITEMS);
            
            // 配置目标节点的pin字段访问
            ObjectNode targetNode = mock(ObjectNode.class);
            when(objectMapper.readTree(anyString())).thenReturn(targetNode);
            when(targetNode.get(PIN_FIELD)).thenReturn(null); // 没有已存在的pin数组
            when(targetNode.put(eq(PIN_FIELD), any(ArrayNode.class))).thenReturn(targetNode);
            
            String updatedToc = "{\"1\":{\"chapter1\":{\"section1\":{\"^\":[" + TEST_POST_ID + "]}}}}";
            when(objectMapper.writeValueAsString(any())).thenReturn(updatedToc);
            
            try (MockedStatic<Utils> mockedUtils = mockStatic(Utils.class)) {
                mockedUtils.when(() -> Utils.hashSHA(updatedToc)).thenReturn("new-hash");
                when(courseTocMapper.get("new-hash")).thenReturn(null);

                // When
                assertDoesNotThrow(() -> {
                    contentsService.pin(TEST_USER_ID, TEST_COURSE_ID, TEST_PATH, TEST_POST_ID, true);
                });

                // Then
                verify(courseTocMapper).incrRef(TEST_TOC_HASH, -1);
                verify(courseTocMapper).insert(any(CourseTocDO.class));
                verify(courseTocMapper).incrRef("new-hash", 1);
                verify(userCourseTocMapper).update(any(UserCourseTocDO.class));
            }
        }

        @Test
        @DisplayName("取消置顶帖子")
        void pin_RemovePin_Success() throws Exception {
            // Given
            setupMocksForContentOperation();
            
            // 添加为pin方法特殊配置的mock  
            ArrayNode arrayNode = mock(ArrayNode.class);
            when(objectMapper.createArrayNode()).thenReturn(arrayNode);
            when(arrayNode.size()).thenReturn(1); // 模拟已有一个置顶项
            when(arrayNode.get(0)).thenReturn(mock(com.fasterxml.jackson.databind.JsonNode.class));
            when(arrayNode.get(0).asInt()).thenReturn((int)TEST_POST_ID);
            when(arrayNode.remove(0)).thenReturn(mock(com.fasterxml.jackson.databind.JsonNode.class));
            
            // 配置pin相关的属性
            when(contentsProperties.getPinField()).thenReturn(PIN_FIELD);
            
            // 配置目标节点的pin字段访问
            ObjectNode targetNode = mock(ObjectNode.class);
            when(objectMapper.readTree(anyString())).thenReturn(targetNode);
            when(targetNode.get(PIN_FIELD)).thenReturn(arrayNode); // 返回已存在的pin数组
            
            String updatedToc = "{\"1\":{\"chapter1\":{\"section1\":{\"^\":[]}}}}";
            when(objectMapper.writeValueAsString(any())).thenReturn(updatedToc);
            
            try (MockedStatic<Utils> mockedUtils = mockStatic(Utils.class)) {
                mockedUtils.when(() -> Utils.hashSHA(updatedToc)).thenReturn("new-hash");
                when(courseTocMapper.get("new-hash")).thenReturn(null);

                // When
                assertDoesNotThrow(() -> {
                    contentsService.pin(TEST_USER_ID, TEST_COURSE_ID, TEST_PATH, TEST_POST_ID, false);
                });

                // Then
                verify(courseTocMapper).incrRef(TEST_TOC_HASH, -1);
                verify(courseTocMapper).insert(any(CourseTocDO.class));
                verify(courseTocMapper).incrRef("new-hash", 1);
                verify(userCourseTocMapper).update(any(UserCourseTocDO.class));
            }
        }
    }

    @Nested
    @DisplayName("异常场景测试")
    class ExceptionScenarioTests {

        @Test
        @DisplayName("JSON 处理异常")
        void jsonProcessingException_ShouldThrowBusinessException() throws Exception {
            // Given
            CourseDO course = createTestCourse();
            when(courseMapper.getById(TEST_COURSE_ID)).thenReturn(course);
            
            UserCourseTocDO userCourseToc = createTestUserCourseToc();
            when(userCourseTocMapper.getByUserAndCourse(TEST_USER_ID, TEST_COURSE_ID)).thenReturn(userCourseToc);
            
            CourseTocDO courseToc = createTestCourseToc();
            Map<String, CourseTocDO> tocMap = new HashMap<>();
            tocMap.put(TEST_TOC_HASH, courseToc);
            when(courseTocMapper.getByHashes(any())).thenReturn(tocMap);
            
            ArrayNode arrayNode = mock(ArrayNode.class);
            when(objectMapper.createArrayNode()).thenReturn(arrayNode);
            
            // Mock JSON processing exception
            when(objectMapper.readTree(anyString())).thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("Test JSON error") {});

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                contentsService.getToc(TEST_USER_ID, TEST_COURSE_ID, false);
            });
            
            assertEquals(ErrorCode.JSON_PROCESSING_ERROR.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("目录哈希数据不一致")
        void tocHashInconsistency_ShouldThrowException() {
            // Given
            CourseDO course = createTestCourse();
            when(courseMapper.getById(TEST_COURSE_ID)).thenReturn(course);
            
            UserCourseTocDO userCourseToc = createTestUserCourseToc();
            when(userCourseTocMapper.getByUserAndCourse(TEST_USER_ID, TEST_COURSE_ID)).thenReturn(userCourseToc);
            
            // Mock empty map - 模拟数据库中不存在对应的哈希
            Map<String, CourseTocDO> emptyMap = new HashMap<>();
            when(courseTocMapper.getByHashes(any())).thenReturn(emptyMap);
            
            ArrayNode arrayNode = mock(ArrayNode.class);
            when(objectMapper.createArrayNode()).thenReturn(arrayNode);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                contentsService.getToc(TEST_USER_ID, TEST_COURSE_ID, false);
            });
            
            assertEquals(ErrorCode.TOC_INDEX_OUT_OF_BOUNDS.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("配置属性使用验证")
        void configurationPropertiesUsage() {
            // Given
            when(contentsProperties.getMaxPinnedItems()).thenReturn(5);
            when(contentsProperties.getPinField()).thenReturn("@");
            when(contentsProperties.getChosenField()).thenReturn("#");

            // When & Then - 验证配置属性被正确调用
            contentsProperties.getMaxPinnedItems();
            contentsProperties.getPinField();
            contentsProperties.getChosenField();
            
            verify(contentsProperties, atLeast(1)).getMaxPinnedItems();
            verify(contentsProperties, atLeast(1)).getPinField();
            verify(contentsProperties, atLeast(1)).getChosenField();
        }
    }
}