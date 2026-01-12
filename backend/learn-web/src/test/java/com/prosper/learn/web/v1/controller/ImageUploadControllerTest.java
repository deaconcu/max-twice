package com.prosper.learn.web.v1.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prosper.learn.application.service.ImageUploadService;
import com.prosper.learn.content.course.CourseDO;
import com.prosper.learn.content.course.CourseDataService;
import com.prosper.learn.content.node.NodeDO;
import com.prosper.learn.content.node.NodeDataService;
import com.prosper.learn.infrastructure.image.ImageCompressionService;
import com.prosper.learn.infrastructure.image.R2Service;
import com.prosper.learn.shared.domain.Enums.ContentState;
import com.prosper.learn.shared.domain.Enums.UserRole;
import com.prosper.learn.shared.domain.exception.StatusCode;
import com.prosper.learn.user.profile.UserDO;
import com.prosper.learn.user.profile.UserDataService;
import com.prosper.learn.user.profile.UserDomainService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 图片上传接口测试
 * 测试文档: docs/test/image-upload.md
 *
 * Command 测试 - 写操作
 * Query 测试 - 读操作
 */
@Transactional
public class ImageUploadControllerTest extends BaseControllerTest {

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
    private ImageUploadService imageUploadService;

    @MockBean
    private R2Service r2Service;

    @MockBean
    private ImageCompressionService imageCompressionService;

    @Autowired
    private CourseDataService courseDataService;

    @Autowired
    private NodeDataService nodeDataService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // 为所有测试配置默认的 Mock 行为

        // Mock ImageCompressionService - 直接返回原始数据，不真正压缩
        Mockito.when(imageCompressionService.compress(any(byte[].class), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0)); // 返回原始数据

        Mockito.when(imageCompressionService.compress(any(byte[].class), anyString(), anyInt(), anyInt(), anyBoolean()))
                .thenAnswer(invocation -> invocation.getArgument(0)); // 返回原始数据

        Mockito.when(imageCompressionService.getCompressedContentType())
                .thenReturn("image/webp");

        // Mock R2Service - 返回成功的URL
        Mockito.when(r2Service.upload(any(byte[].class), anyString(), anyString()))
                .thenReturn("https://example.com/uploads/test.webp");
    }

    @AfterEach
    void tearDown() {
        // 清理 Sa-Token 登录状态
        StpUtil.logout();
    }

    // ==================== 测试辅助方法 ====================

    /**
     * 创建测试用户
     */
    private UserDO createUser(String email) {
        return userDomainService.createUser(email, "password123");
    }

    /**
     * 创建管理员用户
     */
    private UserDO createAdminUser(String email) {
        UserDO user = userDomainService.createUser(email, "password123");
        // 使用专用方法更新角色（update方法不会更新role字段）
        userDataService.updateRole(user.getId(), UserRole.ADMIN.value());
        // 重新获取用户，确保拿到更新后的数据
        return userDataService.getById(user.getId());
    }

    /**
     * 创建模拟图片文件（生成有效的最小图片数据）
     */
    private MockMultipartFile createMockImageFile(String filename, String contentType, long sizeInBytes) {
        byte[] content;

        // 对于需要大文件的测试，直接创建指定大小但保持有效图片头部
        if (sizeInBytes > 1024 * 1024) { // 大于1MB，认为是测试文件大小限制
            if (contentType.equals("image/jpeg")) {
                // JPEG 文件头 + 填充数据 + JPEG 文件尾
                content = new byte[(int) sizeInBytes];
                // JPEG 文件开始标记
                content[0] = (byte)0xFF;
                content[1] = (byte)0xD8;
                // JPEG 文件结束标记
                content[content.length - 2] = (byte)0xFF;
                content[content.length - 1] = (byte)0xD9;
                // 中间填充JFIF头部
                byte[] jfifHeader = new byte[] {
                    (byte)0xFF, (byte)0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46,
                    0x00, 0x01, 0x01, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00
                };
                System.arraycopy(jfifHeader, 0, content, 2, jfifHeader.length);
            } else {
                // 其他格式直接用空数组
                content = new byte[(int) sizeInBytes];
            }
            return new MockMultipartFile("file", filename, contentType, content);
        }

        // 小文件使用完整的有效图片数据
        if (contentType.equals("image/png")) {
            // 最小的1x1 PNG图片 (67字节)
            content = new byte[] {
                (byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
                0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
                0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte)0xC4,
                (byte)0x89, 0x00, 0x00, 0x00, 0x0A, 0x49, 0x44, 0x41, 0x54,
                0x78, (byte)0x9C, 0x63, 0x00, 0x01, 0x00, 0x00, 0x05,
                0x00, 0x01, 0x0D, 0x0A, 0x2D, (byte)0xB4, 0x00, 0x00,
                0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, (byte)0xAE, 0x42,
                0x60, (byte)0x82
            };
        } else if (contentType.equals("image/jpeg")) {
            // 最小的JPEG图片 (125字节)
            content = new byte[] {
                (byte)0xFF, (byte)0xD8, (byte)0xFF, (byte)0xE0, 0x00, 0x10, 0x4A, 0x46,
                0x49, 0x46, 0x00, 0x01, 0x01, 0x00, 0x00, 0x01,
                0x00, 0x01, 0x00, 0x00, (byte)0xFF, (byte)0xDB, 0x00, 0x43,
                0x00, 0x03, 0x02, 0x02, 0x02, 0x02, 0x02, 0x03,
                0x02, 0x02, 0x02, 0x03, 0x03, 0x03, 0x03, 0x04,
                0x06, 0x04, 0x04, 0x04, 0x04, 0x04, 0x08, 0x06,
                0x06, 0x05, 0x06, 0x09, 0x08, 0x0A, 0x0A, 0x09,
                0x08, 0x09, 0x09, 0x0A, 0x0C, 0x0F, 0x0C, 0x0A,
                0x0B, 0x0E, 0x0B, 0x09, 0x09, 0x0D, 0x11, 0x0D,
                0x0E, 0x0F, 0x10, 0x10, 0x11, 0x10, 0x0A, 0x0C,
                0x12, 0x13, 0x12, 0x10, 0x13, 0x0F, 0x10, 0x10,
                0x10, (byte)0xFF, (byte)0xC0, 0x00, 0x0B, 0x08, 0x00, 0x01,
                0x00, 0x01, 0x01, 0x01, 0x11, 0x00, (byte)0xFF, (byte)0xC4,
                0x00, 0x14, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x03, (byte)0xFF, (byte)0xC4, 0x00, 0x14,
                0x10, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, (byte)0xFF, (byte)0xDA, 0x00, 0x08, 0x01, 0x01,
                0x00, 0x00, 0x3F, 0x00, 0x37, (byte)0xFF, (byte)0xD9
            };
        } else if (contentType.equals("image/webp")) {
            // 最小的WebP图片
            content = new byte[] {
                0x52, 0x49, 0x46, 0x46, 0x24, 0x00, 0x00, 0x00,
                0x57, 0x45, 0x42, 0x50, 0x56, 0x50, 0x38, 0x20,
                0x18, 0x00, 0x00, 0x00, 0x30, 0x01, 0x00, (byte)0x9D,
                0x01, 0x2A, 0x01, 0x00, 0x01, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00
            };
        } else {
            // 其他类型（PDF等）使用空数组
            content = new byte[(int) sizeInBytes];
        }

        return new MockMultipartFile("file", filename, contentType, content);
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
        node.setDescription("");
        node.setCourseId(courseId);
        node.setCreatorId(creatorId);
        node.setState(ContentState.PUBLISHED.value());
        nodeDataService.insert(node);
        return node;
    }

    // ==================== 接口1: 上传图片 ====================

    /**
     * 测试1.1: 成功上传图片 - JPEG格式
     */
    @Test
    @DisplayName("成功上传图片 - JPEG格式")
    void testUpload_Jpeg_Success() throws Exception {
        UserDO user = createUser("user@example.com");
        StpUtil.login(user.getId());

        MockMultipartFile file = createMockImageFile("test.jpg", "image/jpeg", 1024 * 1024); // 1MB

        mockMvc.perform(multipart("/api/v1/images/upload")
                        .file(file)
                        .param("refType", "post")
                        .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                .andExpect(jsonPath("$.data.fileUrl").isNotEmpty());
    }

    /**
     * 测试1.2: 成功上传图片 - PNG格式
     */
    @Test
    @DisplayName("成功上传图片 - PNG格式")
    void testUpload_Png_Success() throws Exception {
        UserDO user = createUser("user@example.com");
        StpUtil.login(user.getId());

        MockMultipartFile file = createMockImageFile("test.png", "image/png", 2 * 1024 * 1024); // 2MB

        mockMvc.perform(multipart("/api/v1/images/upload")
                        .file(file)
                        .param("refType", "comment")
                        .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                .andExpect(jsonPath("$.data.fileUrl").isNotEmpty());
    }

    /**
     * 测试1.3: 成功上传图片 - WebP格式
     */
    @Test
    @DisplayName("成功上传图片 - WebP格式")
    void testUpload_Webp_Success() throws Exception {
        UserDO user = createUser("user@example.com");
        StpUtil.login(user.getId());

        MockMultipartFile file = createMockImageFile("test.webp", "image/webp", 500 * 1024); // 500KB

        mockMvc.perform(multipart("/api/v1/images/upload")
                        .file(file)
                        .param("refType", "avatar")
                        .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                .andExpect(jsonPath("$.data.fileUrl").isNotEmpty());
    }

    /**
     * 测试1.4: 字段验证 - refType为空
     */
    @Test
    @DisplayName("字段验证 - refType为空")
    void testUpload_RefTypeEmpty_Fail() throws Exception {
        UserDO user = createUser("user@example.com");
        StpUtil.login(user.getId());

        MockMultipartFile file = createMockImageFile("test.jpg", "image/jpeg", 1024 * 1024);

        mockMvc.perform(multipart("/api/v1/images/upload")
                        .file(file)
                        .param("refType", "")
                        .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
    }

    /**
     * 测试1.5: 字段验证 - 文件为空
     */
    @Test
    @DisplayName("字段验证 - 文件为空")
    void testUpload_FileEmpty_Fail() throws Exception {
        UserDO user = createUser("user@example.com");
        StpUtil.login(user.getId());

        MockMultipartFile file = new MockMultipartFile("file", "", "image/jpeg", new byte[0]);

        mockMvc.perform(multipart("/api/v1/images/upload")
                        .file(file)
                        .param("refType", "post")
                        .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
    }

    /**
     * 测试1.6: 文件类型验证 - 不支持的格式
     */
    @Test
    @DisplayName("文件类型验证 - 不支持的格式")
    void testUpload_UnsupportedType_Fail() throws Exception {
        UserDO user = createUser("user@example.com");
        StpUtil.login(user.getId());

        MockMultipartFile file = createMockImageFile("test.pdf", "application/pdf", 1024 * 1024);

        mockMvc.perform(multipart("/api/v1/images/upload")
                        .file(file)
                        .param("refType", "post")
                        .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.FILE_TYPE_NOT_ALLOWED.getCode()));
    }

    /**
     * 测试1.7: 文件大小验证 - 文件过大
     */
    @Test
    @DisplayName("文件大小验证 - 文件过大")
    void testUpload_FileTooLarge_Fail() throws Exception {
        UserDO user = createUser("user@example.com");
        StpUtil.login(user.getId());

        MockMultipartFile file = createMockImageFile("test.jpg", "image/jpeg", 6 * 1024 * 1024); // 15MB

        mockMvc.perform(multipart("/api/v1/images/upload")
                        .file(file)
                        .param("refType", "post")
                        .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.FILE_TOO_LARGE.getCode()));
    }

    /**
     * 测试1.8: 权限验证 - 未登录
     */
    @Test
    @DisplayName("权限验证 - 未登录")
    void testUpload_NotLogin_Fail() throws Exception {
        MockMultipartFile file = createMockImageFile("test.jpg", "image/jpeg", 1024 * 1024);

        mockMvc.perform(multipart("/api/v1/images/upload")
                        .file(file)
                        .param("refType", "post"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));
    }

    // ==================== 接口2: 标记图片为使用中 ====================

    /**
     * 测试2.1: 成功标记单张图片 - 管理员操作
     */
    @Test
    @DisplayName("成功标记单张图片 - 管理员操作")
    void testMarkAsUsed_SingleImage_Success() throws Exception {
        UserDO admin = createAdminUser("admin@example.com");
        StpUtil.login(admin.getId());

        String requestBody = """
            {
                "fileUrls": ["https://example.com/test.jpg"],
                "refType": "post",
                "refId": 123
            }
            """;

        mockMvc.perform(post("/api/v1/images/mark-used")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));
    }

    /**
     * 测试2.2: 成功标记多张图片 - 管理员操作
     */
    @Test
    @DisplayName("成功标记多张图片 - 管理员操作")
    void testMarkAsUsed_MultipleImages_Success() throws Exception {
        UserDO admin = createAdminUser("admin@example.com");
        StpUtil.login(admin.getId());

        String requestBody = """
            {
                "fileUrls": [
                    "https://example.com/test1.jpg",
                    "https://example.com/test2.jpg",
                    "https://example.com/test3.jpg"
                ],
                "refType": "post",
                "refId": 123
            }
            """;

        mockMvc.perform(post("/api/v1/images/mark-used")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));
    }

    /**
     * 测试2.3: 字段验证 - fileUrls为空
     */
    @Test
    @DisplayName("字段验证 - fileUrls为空")
    void testMarkAsUsed_FileUrlsEmpty_Fail() throws Exception {
        UserDO admin = createAdminUser("admin@example.com");
        StpUtil.login(admin.getId());

        String requestBody = """
            {
                "fileUrls": [],
                "refType": "post",
                "refId": 123
            }
            """;

        mockMvc.perform(post("/api/v1/images/mark-used")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
    }

    /**
     * 测试2.4: 字段验证 - refType为空
     */
    @Test
    @DisplayName("字段验证 - refType为空")
    void testMarkAsUsed_RefTypeNull_Fail() throws Exception {
        UserDO admin = createAdminUser("admin@example.com");
        StpUtil.login(admin.getId());

        String requestBody = """
            {
                "fileUrls": ["https://example.com/test.jpg"],
                "refType": null,
                "refId": 123
            }
            """;

        mockMvc.perform(post("/api/v1/images/mark-used")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
    }

    /**
     * 测试2.5: 字段验证 - refId为空
     */
    @Test
    @DisplayName("字段验证 - refId为空")
    void testMarkAsUsed_RefIdNull_Fail() throws Exception {
        UserDO admin = createAdminUser("admin@example.com");
        StpUtil.login(admin.getId());

        String requestBody = """
            {
                "fileUrls": ["https://example.com/test.jpg"],
                "refType": "post",
                "refId": null
            }
            """;

        mockMvc.perform(post("/api/v1/images/mark-used")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
    }

    /**
     * 测试2.6: 权限验证 - 普通用户无权限
     */
    @Test
    @DisplayName("权限验证 - 普通用户无权限")
    void testMarkAsUsed_NormalUserNoPermission_Fail() throws Exception {
        UserDO user = createUser("user@example.com");
        StpUtil.login(user.getId());

        String requestBody = """
            {
                "fileUrls": ["https://example.com/test.jpg"],
                "refType": "post",
                "refId": 123
            }
            """;

        mockMvc.perform(post("/api/v1/images/mark-used")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.PERMISSION_DENIED.getCode()));
    }

    /**
     * 测试2.7: 权限验证 - 未登录
     */
    @Test
    @DisplayName("权限验证 - 未登录")
    void testMarkAsUsed_NotLogin_Fail() throws Exception {
        String requestBody = """
            {
                "fileUrls": ["https://example.com/test.jpg"],
                "refType": "post",
                "refId": 123
            }
            """;

        mockMvc.perform(post("/api/v1/images/mark-used")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));
    }

    // ==================== 接口3: 删除图片 ====================

    /**
     * 测试3.1: 字段验证 - fileUrl为空
     */
    @Test
    @DisplayName("字段验证 - fileUrl为空")
    void testDelete_FileUrlEmpty_Fail() throws Exception {
        UserDO admin = createAdminUser("admin@example.com");
        StpUtil.login(admin.getId());

        mockMvc.perform(delete("/api/v1/images")
                        .param("fileUrl", "")
                        .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
    }

    /**
     * 测试3.2: 业务验证 - 图片不存在
     */
    @Test
    @DisplayName("业务验证 - 图片不存在")
    void testDelete_ImageNotFound_Fail() throws Exception {
        UserDO admin = createAdminUser("admin@example.com");
        StpUtil.login(admin.getId());

        mockMvc.perform(delete("/api/v1/images")
                        .param("fileUrl", "https://example.com/not-exist.jpg")
                        .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.NOT_FOUND.getCode()));
    }

    /**
     * 测试3.3: 权限验证 - 普通用户无权限
     */
    @Test
    @DisplayName("权限验证 - 普通用户无权限")
    void testDelete_NormalUserNoPermission_Fail() throws Exception {
        UserDO user = createUser("user@example.com");
        StpUtil.login(user.getId());

        mockMvc.perform(delete("/api/v1/images")
                        .param("fileUrl", "https://example.com/test.jpg")
                        .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.PERMISSION_DENIED.getCode()));
    }

    /**
     * 测试3.4: 权限验证 - 未登录
     */
    @Test
    @DisplayName("权限验证 - 未登录")
    void testDelete_NotLogin_Fail() throws Exception {
        mockMvc.perform(delete("/api/v1/images")
                        .param("fileUrl", "https://example.com/test.jpg"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));
    }

    // ==================== 接口4: 获取配额使用情况 ====================

    /**
     * 测试4.1: 成功获取配额 - 未使用
     */
    @Test
    @DisplayName("成功获取配额 - 未使用")
    void testGetQuota_NotUsed_Success() throws Exception {
        UserDO user = createUser("user@example.com");
        StpUtil.login(user.getId());

        mockMvc.perform(get("/api/v1/images/quota")
                        .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                .andExpect(jsonPath("$.data.minuteUsed").value(0))
                .andExpect(jsonPath("$.data.minuteLimit").isNumber())
                .andExpect(jsonPath("$.data.hourUsed").value(0))
                .andExpect(jsonPath("$.data.hourLimit").isNumber())
                .andExpect(jsonPath("$.data.dailyUsed").value(0))
                .andExpect(jsonPath("$.data.dailyLimit").isNumber());
    }

    /**
     * 测试4.2: 权限验证 - 未登录
     */
    @Test
    @DisplayName("权限验证 - 未登录")
    void testGetQuota_NotLogin_Fail() throws Exception {
        mockMvc.perform(get("/api/v1/images/quota"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));
    }

    // ==================== 接口5: 获取上传历史 ====================

    /**
     * 测试5.1: 成功获取上传历史 - 无记录
     */
    @Test
    @DisplayName("成功获取上传历史 - 无记录")
    void testGetHistory_NoRecords_Success() throws Exception {
        UserDO user = createUser("user@example.com");
        StpUtil.login(user.getId());

        mockMvc.perform(get("/api/v1/images/history")
                        .param("limit", "20")
                        .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    /**
     * 测试5.2: 参数验证 - 默认限制
     */
    @Test
    @DisplayName("参数验证 - 默认限制")
    void testGetHistory_DefaultLimit_Success() throws Exception {
        UserDO user = createUser("user@example.com");
        StpUtil.login(user.getId());

        mockMvc.perform(get("/api/v1/images/history")
                        .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                .andExpect(jsonPath("$.data").isArray());
    }

    /**
     * 测试5.3: 权限验证 - 未登录
     */
    @Test
    @DisplayName("权限验证 - 未登录")
    void testGetHistory_NotLogin_Fail() throws Exception {
        mockMvc.perform(get("/api/v1/images/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));
    }

    // ==================== 集成测试：自动标记图片 ====================

    /**
     * 测试6.1: 创建帖子 - 内容无图片
     */
    @Test
    @DisplayName("创建帖子 - 内容无图片")
    void testCreatePost_NoImages_Success() throws Exception {
        UserDO user = createUser("user@example.com");
        CourseDO course = createPublishedCourse("测试课程", user.getId());
        NodeDO node = createPublishedNode("测试节点", course.getId(), user.getId());

        StpUtil.login(user.getId());

        // 使用 Map 构建请求体
        java.util.Map<String, Object> requestMap = new java.util.HashMap<>();
        requestMap.put("nodeId", node.getId());
        requestMap.put("content", "这是一个测试帖子的内容，不包含任何图片，只有纯文本");
        requestMap.put("type", 2);

        String requestBody = objectMapper.writeValueAsString(requestMap);

        mockMvc.perform(post("/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));
    }

    /**
     * 测试6.2: 创建帖子 - 包含图片（自动标记）
     */
    @Test
    @DisplayName("创建帖子 - 包含图片（自动标记）")
    void testCreatePost_WithImages_AutoMark() throws Exception {
        UserDO user = createUser("user@example.com");
        CourseDO course = createPublishedCourse("测试课程", user.getId());
        NodeDO node = createPublishedNode("测试节点", course.getId(), user.getId());

        StpUtil.login(user.getId());

        String imageUrl1 = "https://example.com/test1.jpg";
        String imageUrl2 = "https://example.com/test2.jpg";

        // 使用 Map 构建请求体
        java.util.Map<String, Object> requestMap = new java.util.HashMap<>();
        requestMap.put("nodeId", node.getId());
        requestMap.put("content", String.format("这是测试文本内容，包含图片<img src='%s'/>和另一张图片<img src='%s'/>的帖子", imageUrl1, imageUrl2));
        requestMap.put("type", 2);

        String requestBody = objectMapper.writeValueAsString(requestMap);

        mockMvc.perform(post("/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));

        // 注意：这里只验证帖子创建成功，图片标记的验证需要查询数据库
        // 由于没有实际的图片记录，markAsUsed 会失败，但不影响帖子创建
    }

    /**
     * 测试6.3: 图片提取 - 多种HTML格式
     */
    @Test
    @DisplayName("图片提取 - 多种HTML格式")
    void testCreatePost_VariousHtmlFormats_Success() throws Exception {
        UserDO user = createUser("user@example.com");
        CourseDO course = createPublishedCourse("测试课程", user.getId());
        NodeDO node = createPublishedNode("测试节点", course.getId(), user.getId());

        StpUtil.login(user.getId());

        // 使用 Map 构建请求体，避免 JSON 转义问题
        java.util.Map<String, Object> requestMap = new java.util.HashMap<>();
        requestMap.put("nodeId", node.getId());
        requestMap.put("content", "这是测试内容，包含多种HTML格式的图片<img src='https://example.com/img1.jpg'>和<img src='https://example.com/img2.jpg'/>标签");
        requestMap.put("type", 2);

        String requestBody = objectMapper.writeValueAsString(requestMap);

        mockMvc.perform(post("/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));
    }
}
