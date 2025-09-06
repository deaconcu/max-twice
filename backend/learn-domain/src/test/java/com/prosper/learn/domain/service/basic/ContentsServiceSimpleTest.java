package com.prosper.learn.domain.service.basic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.prosper.learn.common.Enums;
import com.prosper.learn.common.exception.BusinessException;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.domain.config.ContentsProperties;
import com.prosper.learn.persistence.dataobject.*;
import com.prosper.learn.persistence.mapper.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ContentsService 简化测试类
 * 只测试基础功能，避免复杂的mock配置
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ContentsService 简化测试")
class ContentsServiceSimpleTest {

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

    @Test
    @DisplayName("课程不存在时抛出异常")
    void getToc_CourseNotFound() {
        // Given
        when(courseMapper.getById(TEST_COURSE_ID)).thenReturn(null);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            contentsService.getToc(TEST_USER_ID, TEST_COURSE_ID, false);
        });
        
        assertEquals(ErrorCode.CONTENTS_COURSE_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("用户目录不存在且不创建时返回null")
    void getToc_UserTocNotExistsAndNoCreate() {
        // Given
        CourseDO course = new CourseDO();
        course.setId(TEST_COURSE_ID);
        course.setRootNodeId(1L);
        when(courseMapper.getById(TEST_COURSE_ID)).thenReturn(course);
        when(userCourseTocMapper.getByUserAndCourse(TEST_USER_ID, TEST_COURSE_ID)).thenReturn(null);

        // When
        ArrayNode result = contentsService.getToc(TEST_USER_ID, TEST_COURSE_ID, false);

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("帖子不存在时抛出异常")
    void choose_PostNotFound() {
        // Given - 只需要配置postMapper返回null
        when(postMapper.get(TEST_POST_ID)).thenReturn(null);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            contentsService.choose(TEST_USER_ID, TEST_PATH, TEST_COURSE_ID, TEST_POST_ID);
        });
        
        assertEquals(ErrorCode.CONTENTS_POST_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("文章类型帖子不能选择")
    void choose_ArticleTypePost() {
        // Given
        PostDO post = new PostDO();
        post.setId(TEST_POST_ID);
        post.setType(Enums.PostType.article.value());
        when(postMapper.get(TEST_POST_ID)).thenReturn(post);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            contentsService.choose(TEST_USER_ID, TEST_PATH, TEST_COURSE_ID, TEST_POST_ID);
        });
        
        assertEquals(ErrorCode.CONTENTS_INVALID_POST_TYPE.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("用户目录不存在时抛出异常")
    void choose_UserTocNotFound() {
        // Given
        PostDO post = new PostDO();
        post.setId(TEST_POST_ID);
        post.setType(Enums.PostType.contents.value());
        post.setContent("1,2,3"); // 添加content字段
        when(postMapper.get(TEST_POST_ID)).thenReturn(post);
        
        CourseDO course = new CourseDO();
        course.setId(TEST_COURSE_ID);
        when(courseMapper.getById(TEST_COURSE_ID)).thenReturn(course);
        
        // Mock objectMapper and contentsProperties because they will be called
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
    @DisplayName("目录索引越界时抛出异常")
    void choose_IndexOutOfBounds() {
        // Given
        PostDO post = new PostDO();
        post.setId(TEST_POST_ID);
        post.setType(Enums.PostType.contents.value());
        post.setContent("1,2,3"); // 添加content字段
        when(postMapper.get(TEST_POST_ID)).thenReturn(post);
        
        CourseDO course = new CourseDO();
        course.setId(TEST_COURSE_ID);
        when(courseMapper.getById(TEST_COURSE_ID)).thenReturn(course);
        
        // Mock objectMapper and contentsProperties because they will be called
        ObjectNode mockChildNode = mock(ObjectNode.class);
        when(objectMapper.createObjectNode()).thenReturn(mockChildNode);
        when(mockChildNode.putObject(anyString())).thenReturn(mockChildNode);
        when(mockChildNode.put(anyString(), anyLong())).thenReturn(mockChildNode);
        when(contentsProperties.getChosenField()).thenReturn(CHOSEN_FIELD);
        
        UserCourseTocDO userCourseToc = new UserCourseTocDO();
        userCourseToc.setUserId(TEST_USER_ID);
        userCourseToc.setCourseId(TEST_COURSE_ID);
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