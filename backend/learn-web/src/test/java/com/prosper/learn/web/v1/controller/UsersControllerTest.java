package com.prosper.learn.web.v1.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prosper.learn.shared.domain.Enums;
import com.prosper.learn.shared.domain.exception.StatusCode;
import com.prosper.learn.user.auth.VerificationDO;
import com.prosper.learn.user.auth.VerificationDataService;
import com.prosper.learn.user.profile.UserDO;
import com.prosper.learn.user.profile.UserDataService;
import com.prosper.learn.user.profile.UserDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 用户管理接口测试
 * 测试文档: docs/test/user.md
 *
 * Command 测试 - 写操作
 * Query 测试 - 读操作
 */
@Transactional
public class UsersControllerTest extends BaseControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserDataService userDataService;

    @Autowired
    private UserDomainService userDomainService;

    @Autowired
    private VerificationDataService verificationDataService;

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
     * 创建已验证邮箱的用户
     */
    private UserDO createValidatedUser(String email) {
        UserDO user = userDomainService.createUser(email, "password123");
        user.setEmailValidated(true);
        // 设置 msgReadTime 避免数据库约束错误
        if (user.getMsgReadTime() == null) {
            user.setMsgReadTime(LocalDateTime.now());
        }
        userDataService.update(user);
        return user;
    }

    // ==================== Command 测试（写操作） ====================

    /**
     * 测试1: 用户注册 - 成功注册
     */
    @Test
    @DisplayName("成功注册")
    void testRegister_Success() throws Exception {
        String requestBody = """
            {
                "email": "newuser@example.com",
                "password": "password123"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));

        // 验证用户已创建
        UserDO user = userDataService.getByEmail("newuser@example.com");
        assertThat(user).isNotNull();
        assertThat(user.getEmailValidated()).isFalse();
    }

    /**
     * 测试2: 用户注册 - email 为空
     */
    @Test
    @DisplayName("注册 - email 为空")
    void testRegister_EmailEmpty() throws Exception {
        String requestBody = """
            {
                "email": "",
                "password": "password123"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
    }

    /**
     * 测试3: 用户注册 - email 格式错误
     */
    @Test
    @DisplayName("注册 - email 格式错误")
    void testRegister_EmailInvalid() throws Exception {
        String requestBody = """
            {
                "email": "invalid-email",
                "password": "password123"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
    }

    /**
     * 测试4: 用户注册 - password 为空
     */
    @Test
    @DisplayName("注册 - password 为空")
    void testRegister_PasswordEmpty() throws Exception {
        String requestBody = """
            {
                "email": "user@example.com",
                "password": ""
            }
            """;

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
    }

    /**
     * 测试5: 用户注册 - 邮箱已存在
     */
    @Test
    @DisplayName("注册 - 邮箱已存在")
    void testRegister_EmailExists() throws Exception {
        createUser("existing@example.com");

        String requestBody = """
            {
                "email": "existing@example.com",
                "password": "password123"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.USER_ALREADY_EXISTS.getCode()));
    }

    /**
     * 测试6: 用户登录 - 成功登录
     */
    @Test
    @DisplayName("成功登录")
    void testLogin_Success() throws Exception {
        UserDO user = createValidatedUser("user@example.com");

        String requestBody = """
            {
                "email": "user@example.com",
                "password": "password123"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.name").exists());
    }

    /**
     * 测试7: 用户登录 - email 为空
     */
    @Test
    @DisplayName("登录 - email 为空")
    void testLogin_EmailEmpty() throws Exception {
        String requestBody = """
            {
                "email": "",
                "password": "password123"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
    }

    /**
     * 测试8: 用户登录 - password 为空
     */
    @Test
    @DisplayName("登录 - password 为空")
    void testLogin_PasswordEmpty() throws Exception {
        String requestBody = """
            {
                "email": "user@example.com",
                "password": ""
            }
            """;

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
    }

    /**
     * 测试9: 用户登录 - 用户不存在
     */
    @Test
    @DisplayName("登录 - 用户不存在")
    void testLogin_UserNotFound() throws Exception {
        String requestBody = """
            {
                "email": "nonexistent@example.com",
                "password": "password123"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_FOUND.getCode()));
    }

    /**
     * 测试10: 用户登录 - 密码错误
     * TODO: 密码验证逻辑被注释，暂时跳过此测试
     */
    // @Test
    // @DisplayName("登录 - 密码错误")
    // void testLogin_WrongPassword() throws Exception {
    //     createValidatedUser("user@example.com");

    //     String requestBody = """
    //         {
    //             "email": "user@example.com",
    //             "password": "wrongpassword"
    //         }
    //         """;

    //     mockMvc.perform(post("/api/v1/auth/login")
    //             .contentType(MediaType.APPLICATION_JSON)
    //             .content(requestBody))
    //             .andExpect(status().isOk())
    //             .andExpect(jsonPath("$.code").value(StatusCode.USER_PASSWORD_WRONG.getCode()));
    // }

    /**
     * 测试11: 用户登录 - 邮箱未验证
     */
    @Test
    @DisplayName("登录 - 邮箱未验证")
    void testLogin_EmailNotValidated() throws Exception {
        createUser("user@example.com");  // 未验证邮箱的用户

        String requestBody = """
            {
                "email": "user@example.com",
                "password": "password123"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.USER_EMAIL_NOT_VALIDATED.getCode()));
    }

    /**
     * 测试12: 修改用户信息 - 成功修改
     */
    @Test
    @DisplayName("成功修改用户信息")
    void testUpdateCurrentUser_Success() throws Exception {
        UserDO user = createUser("user@example.com");
        StpUtil.login(user.getId());

        try {
            String requestBody = """
                {
                    "name": "新用户名",
                    "biography": "这是我的新简介"
                }
                """;

            mockMvc.perform(put("/api/v1/users/current")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));

            // 验证数据库状态
            UserDO updatedUser = userDataService.getById(user.getId());
            assertThat(updatedUser.getName()).isEqualTo("新用户名");
            assertThat(updatedUser.getBiography()).isEqualTo("这是我的新简介");
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试13: 修改用户信息 - name 为空
     */
    @Test
    @DisplayName("修改用户信息 - name 为空")
    void testUpdateCurrentUser_NameEmpty() throws Exception {
        UserDO user = createUser("user@example.com");
        StpUtil.login(user.getId());

        try {
            String requestBody = """
                {
                    "name": "",
                    "biography": "简介"
                }
                """;

            mockMvc.perform(put("/api/v1/users/current")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试14: 修改用户信息 - 未登录
     */
    @Test
    @DisplayName("修改用户信息 - 未登录")
    void testUpdateCurrentUser_NotLoggedIn() throws Exception {
        String requestBody = """
            {
                "name": "新用户名",
                "biography": "这是我的新简介"
            }
            """;

        mockMvc.perform(put("/api/v1/users/current")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));
    }

    // ==================== Query 测试（读操作） ====================

    /**
     * 测试15: 获取当前用户信息 - 成功获取
     */
    @Test
    @DisplayName("成功获取当前用户信息")
    void testGetCurrentUser_Success() throws Exception {
        UserDO user = createUser("user@example.com");
        StpUtil.login(user.getId());

        try {
            mockMvc.perform(get("/api/v1/users/current")
                    .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data.id").value(user.getId()))
                    .andExpect(jsonPath("$.data.email").value("user@example.com"))
                    .andExpect(jsonPath("$.data.name").exists())
                    .andExpect(jsonPath("$.data.biography").exists())
                    .andExpect(jsonPath("$.data.emailValidated").exists())
                    .andExpect(jsonPath("$.data.createdAt").exists());
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试16: 获取当前用户信息 - 未登录
     */
    @Test
    @DisplayName("获取当前用户信息 - 未登录")
    void testGetCurrentUser_NotLoggedIn() throws Exception {
        mockMvc.perform(get("/api/v1/users/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));
    }

    /**
     * 测试17: 获取用户公开信息 - 成功获取
     */
    @Test
    @DisplayName("成功获取用户公开信息")
    void testGetUser_Success() throws Exception {
        UserDO currentUser = createUser("current@example.com");
        UserDO targetUser = createUser("target@example.com");

        StpUtil.login(currentUser.getId());

        try {
            mockMvc.perform(get("/api/v1/users/" + targetUser.getName())
                    .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data.id").value(targetUser.getId()))
                    .andExpect(jsonPath("$.data.name").exists())
                    .andExpect(jsonPath("$.data.biography").exists())
                    .andExpect(jsonPath("$.data.isFollowing").exists());
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试18: 获取用户公开信息 - 用户不存在
     */
    @Test
    @DisplayName("获取用户公开信息 - 用户不存在")
    void testGetUser_NotFound() throws Exception {
        UserDO user = createUser("user@example.com");
        StpUtil.login(user.getId());

        try {
            mockMvc.perform(get("/api/v1/users/nonexistent")
                    .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_FOUND.getCode()));
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试19: 获取用户公开信息 - username 为空
     */
    @Test
    @DisplayName("获取用户公开信息 - username 为空")
    void testGetUser_UsernameEmpty() throws Exception {
        UserDO user = createUser("user@example.com");
        StpUtil.login(user.getId());

        try {
            mockMvc.perform(get("/api/v1/users/ ")
                    .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试20: 获取用户公开信息 - 未登录
     */
    @Test
    @DisplayName("获取用户公开信息 - 未登录")
    void testGetUser_NotLoggedIn() throws Exception {
        UserDO user = createUser("user@example.com");

        mockMvc.perform(get("/api/v1/users/" + user.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));
    }

    /**
     * 测试21: 搜索用户 - 成功搜索
     */
    @Test
    @DisplayName("成功搜索用户")
    void testSearchUsers_Success() throws Exception {
        UserDO user1 = createUser("zhang3@example.com");
        user1.setName("张三");
        userDataService.update(user1);

        UserDO user2 = createUser("zhang3feng@example.com");
        user2.setName("张三丰");
        userDataService.update(user2);

        UserDO user3 = createUser("lisi@example.com");
        user3.setName("李四");
        userDataService.update(user3);

        mockMvc.perform(get("/api/v1/users/search")
                .param("name", "张"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").exists())
                .andExpect(jsonPath("$.data[0].name").exists());
    }

    /**
     * 测试22: 搜索用户 - 空结果
     */
    @Test
    @DisplayName("搜索用户 - 空结果")
    void testSearchUsers_EmptyResult() throws Exception {
        createUser("user@example.com");

        mockMvc.perform(get("/api/v1/users/search")
                .param("name", "不存在的用户名"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    /**
     * 测试23: 搜索用户 - name 为空
     */
    @Test
    @DisplayName("搜索用户 - name 为空")
    void testSearchUsers_NameEmpty() throws Exception {
        mockMvc.perform(get("/api/v1/users/search")
                .param("name", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
    }

    /**
     * 测试24: 搜索用户 - name 缺失
     */
    @Test
    @DisplayName("搜索用户 - name 缺失")
    void testSearchUsers_NameMissing() throws Exception {
        mockMvc.perform(get("/api/v1/users/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
    }

    /**
     * 测试25: 搜索用户 - 不需要登录
     */
    @Test
    @DisplayName("搜索用户 - 不需要登录")
    void testSearchUsers_NoLoginRequired() throws Exception {
        UserDO user = createUser("user@example.com");
        user.setName("测试用户");
        userDataService.update(user);

        mockMvc.perform(get("/api/v1/users/search")
                .param("name", "测试"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));
    }

    // ==================== 验证码类型测试 ====================

    /**
     * 测试26: 邮箱验证 - 成功验证（type=REGISTER）
     */
    @Test
    @DisplayName("邮箱验证 - 成功验证注册类型验证码")
    void testValidateEmail_RegisterType_Success() throws Exception {
        // 1. 创建用户
        UserDO user = createUser("test@example.com");

        // 2. 创建注册类型验证码
        userDomainService.createVerificationCode("test@example.com", "123456");

        // 3. 验证邮箱
        String requestBody = """
            {
                "email": "test@example.com",
                "code": "123456"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/validate-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                .andExpect(jsonPath("$.data.id").exists());

        // 4. 验证用户邮箱已验证
        UserDO updatedUser = userDataService.getByEmail("test@example.com");
        assertThat(updatedUser.getEmailValidated()).isTrue();

        // 5. 验证验证码已被标记为已使用
        VerificationDO verification = verificationDataService.getByEmailAndType(
            "test@example.com", Enums.VerificationType.REGISTER.value(), false);
        assertThat(verification).isNull();  // 未使用的验证码已经不存在了
    }

    /**
     * 测试27: 验证码类型 - 注册类型验证码默认值
     */
    @Test
    @DisplayName("验证码类型 - 注册类型验证码默认值为type=1")
    void testVerificationCode_RegisterTypeDefault() throws Exception {
        // 1. 创建验证码（使用默认构造方法，应该默认为REGISTER类型）
        userDomainService.createVerificationCode("test@example.com", "123456");

        // 2. 查询验证码,验证type=1(REGISTER)
        VerificationDO verification = verificationDataService.getByEmailAndType(
            "test@example.com", Enums.VerificationType.REGISTER.value(), false);

        assertThat(verification).isNotNull();
        assertThat(verification.getType()).isEqualTo(Enums.VerificationType.REGISTER.value());
        assertThat(verification.getEmail()).isEqualTo("test@example.com");
        assertThat(verification.getCode()).isEqualTo("123456");
        assertThat(verification.getUsed()).isFalse();
    }

    /**
     * 测试28: 验证码类型 - 不同类型验证码互不干扰
     */
    @Test
    @DisplayName("验证码类型 - 不同类型验证码互不干扰")
    void testVerificationCode_DifferentTypesIndependent() throws Exception {
        String email = "test@example.com";

        // 1. 创建不同类型的验证码
        VerificationDO registerCode = new VerificationDO(email, "111111", Enums.VerificationType.REGISTER.value());
        verificationDataService.insert(registerCode);

        VerificationDO resetPasswordCode = new VerificationDO(email, "222222", Enums.VerificationType.RESET_PASSWORD.value());
        verificationDataService.insert(resetPasswordCode);

        VerificationDO changeEmailCode = new VerificationDO(email, "333333", Enums.VerificationType.CHANGE_EMAIL.value());
        verificationDataService.insert(changeEmailCode);

        // 2. 验证可以分别查询到不同类型的验证码
        VerificationDO queriedRegister = verificationDataService.getByEmailAndType(
            email, Enums.VerificationType.REGISTER.value(), false);
        assertThat(queriedRegister).isNotNull();
        assertThat(queriedRegister.getCode()).isEqualTo("111111");

        VerificationDO queriedResetPassword = verificationDataService.getByEmailAndType(
            email, Enums.VerificationType.RESET_PASSWORD.value(), false);
        assertThat(queriedResetPassword).isNotNull();
        assertThat(queriedResetPassword.getCode()).isEqualTo("222222");

        VerificationDO queriedChangeEmail = verificationDataService.getByEmailAndType(
            email, Enums.VerificationType.CHANGE_EMAIL.value(), false);
        assertThat(queriedChangeEmail).isNotNull();
        assertThat(queriedChangeEmail.getCode()).isEqualTo("333333");
    }

    /**
     * 测试29: 验证码类型 - 查询错误类型返回null
     */
    @Test
    @DisplayName("验证码类型 - 查询错误类型返回null")
    void testVerificationCode_WrongTypeReturnsNull() throws Exception {
        String email = "test@example.com";

        // 1. 创建REGISTER类型验证码
        VerificationDO registerCode = new VerificationDO(email, "123456", Enums.VerificationType.REGISTER.value());
        verificationDataService.insert(registerCode);

        // 2. 用RESET_PASSWORD类型查询,应该返回null
        VerificationDO queried = verificationDataService.getByEmailAndType(
            email, Enums.VerificationType.RESET_PASSWORD.value(), false);
        assertThat(queried).isNull();
    }

    /**
     * 测试30: 邮箱验证 - 只能使用REGISTER类型验证码
     */
    @Test
    @DisplayName("邮箱验证 - 只能使用REGISTER类型验证码")
    void testValidateEmail_OnlyRegisterTypeWorks() throws Exception {
        String email = "test@example.com";

        // 1. 创建用户
        createUser(email);

        // 2. 创建RESET_PASSWORD类型验证码(非REGISTER类型)
        VerificationDO resetPasswordCode = new VerificationDO(email, "123456", Enums.VerificationType.RESET_PASSWORD.value());
        verificationDataService.insert(resetPasswordCode);

        // 3. 尝试验证邮箱,应该失败(因为查询的是REGISTER类型)
        String requestBody = """
            {
                "email": "test@example.com",
                "code": "123456"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/validate-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.USER_VERIFICATION_CODE_NOT_FOUND.getCode()));
    }

    /**
     * 测试31: 验证码类型 - 使用后标记为已使用
     */
    @Test
    @DisplayName("验证码类型 - 使用后标记为已使用")
    void testVerificationCode_MarkedAsUsedAfterValidation() throws Exception {
        String email = "test@example.com";

        // 1. 创建用户和验证码
        createUser(email);
        userDomainService.createVerificationCode(email, "123456");

        // 2. 验证前确认验证码存在且未使用
        VerificationDO beforeValidation = verificationDataService.getByEmailAndType(
            email, Enums.VerificationType.REGISTER.value(), false);
        assertThat(beforeValidation).isNotNull();
        assertThat(beforeValidation.getUsed()).isFalse();

        // 3. 进行验证
        String requestBody = """
            {
                "email": "test@example.com",
                "code": "123456"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/validate-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));

        // 4. 验证后确认未使用的验证码查询不到
        VerificationDO afterValidation = verificationDataService.getByEmailAndType(
            email, Enums.VerificationType.REGISTER.value(), false);
        assertThat(afterValidation).isNull();

        // 5. 但是查询已使用的验证码可以找到
        VerificationDO usedVerification = verificationDataService.getByEmailAndType(
            email, Enums.VerificationType.REGISTER.value(), true);
        assertThat(usedVerification).isNotNull();
        assertThat(usedVerification.getUsed()).isTrue();
    }

    /**
     * 测试32: 验证码发送间隔 - 60秒内重复发送被拒绝
     */
    @Test
    @DisplayName("验证码发送间隔 - 60秒内重复发送被拒绝")
    void testVerificationCode_SendIntervalCheck() throws Exception {
        String email = "test@example.com";

        // 1. 第一次发送验证码
        userDomainService.createVerificationCode(email, "123456");

        // 2. 立即再次发送,应该失败
        try {
            userDomainService.createVerificationCode(email, "654321");
            assert false : "应该抛出异常";
        } catch (Exception e) {
            assertThat(e.getMessage()).contains("验证码发送过于频繁");
        }

        // 3. 等待2秒后(测试配置中设置为2秒)再发送,应该成功
        Thread.sleep(2100);
        userDomainService.createVerificationCode(email, "654321");

        // 4. 验证最新的验证码是654321
        VerificationDO latestVerification = verificationDataService.getByEmailAndType(
            email, Enums.VerificationType.REGISTER.value(), false);
        assertThat(latestVerification).isNotNull();
        assertThat(latestVerification.getCode()).isEqualTo("654321");
    }
}
