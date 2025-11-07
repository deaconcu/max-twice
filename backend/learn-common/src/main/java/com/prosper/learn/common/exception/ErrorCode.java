package com.prosper.learn.common.exception;

/**
 * 错误码枚举
 * 
 * 错误码分段规则：
 * - 0: 成功
 * - 1xxx: 业务异常
 *   - 10xx: 通用错误（参数、权限等）
 *   - 11xx: 用户模块错误
 *   - 12xx: 课程模块错误
 *   - 13xx: 内容管理模块错误
 *   - 14xx: 评论模块错误
 *   - 15xx: 路线图模块错误
 * - 9xxx: 系统错误
 */
public enum ErrorCode {

    // 通用业务相关 10xx
    NOT_SUPPORTED(1001, "不支持的操作类型"),
    INVALID_PARAMETER(1002, "参数异常"),
    JSON_PROCESSING_ERROR(1003, "JSON处理异常"),
    INVALID_DATE(1004, "无效的日期"),
    INVALID_DAYS_RANGE(1005, "无效的天数范围"),
    PERMISSION_DENIED(1006, "权限不足"),
    NOT_FOUND(1007, "没有找到对象"),
    ALREADY_APPROVED(1008, "专业状态已是批准状态，无需重复操作"),
    ALREADY_REJECTED(1009, "专业状态已是拒绝状态，无需重复操作"),
    ALREADY_BANNED(1010, "专业状态已是屏蔽状态，无需重复操作"),
    INVALID_OPERATION(1011, "不合法的操作类型"),

    // 用户认证相关 11xx
    USER_NOT_LOGIN(1001, "用户未登录"),
    USER_NOT_FOUND(1101, "用户不存在"),
    USER_ALREADY_EXISTS(1102, "用户已存在"),
    USER_PASSWORD_WRONG(1103, "密码错误"),
    USER_EMAIL_NOT_VALIDATED(1104, "邮箱未验证"),
    USER_INVALID_EMAIL_FORMAT(1105, "邮箱格式不正确"),
    USER_INVALID_USERNAME_LENGTH(1106, "用户名长度超出限制"),
    USER_INVALID_PASSWORD_LENGTH(1107, "密码长度不符合要求"),
    USER_VERIFICATION_CODE_INVALID(1108, "验证码无效或已过期"),
    USER_VERIFICATION_CODE_NOT_FOUND(1109, "验证码不存在"),
    USER_ALREADY_FOLLOWED(1110, "已关注该用户"),
    USER_SUBSCRIPTION_PARSE_ERROR(1111, "订阅数据解析失败"),
    USER_COURSE_ALREADY_SUBSCRIBED(1112, "课程已订阅"),
    USER_COURSE_NOT_SUBSCRIBED(1113, "课程未订阅"),
    USER_BANNED(1114, "用户已被屏蔽"),

    // 课程相关 12xx
    COURSE_NOT_FOUND(1201, "课程不存在"),
    COURSE_ALREADY_APPROVED(1202, "课程状态已是批准状态，无需重复操作"),
    COURSE_ALREADY_REJECTED(1203, "课程状态已是被拒绝状态，无需重复操作"),
    COURSE_STATE_CONFLICT(1204, "课程状态已被其他操作修改，请刷新后重试"),
    COURSE_PARENT_NOT_FOUND(1205, "父课程不存在"),
    COURSE_DELETE_FAILED(1206, "课程删除失败"),
    COURSE_OPERATION_FAILED(1207, "课程操作失败"),
    COURSE_IS_NOT_PUBLISHED(1208, "该课程不是公开发布状态，暂时无法访问"),
    COURSE_ALREADY_BANNED(1209, "课程状态已是被屏蔽状态，无需重复操作"),

    // 路线图相关 15xx
    ROADMAP_NOT_FOUND(1501, "路线图不存在"),
    ROADMAP_CONTENT_INVALID(1502, "路线图内容格式不正确"),
    ROADMAP_PIN_LIMIT_EXCEEDED(1503, "最多只能置顶19个路线图"),
    
    // 学习进度相关 16xx
    USER_ROADMAP_NOT_FOUND(1601, "学习记录不存在"),
    USER_COURSE_NOT_FOUND(1602, "课程学习记录不存在"),
    USER_COURSE_PROGRESS_INVALID(1603, "进度百分比必须在0-100之间"),
    LEARNING_PROGRESS_SYNC_FAILED(1604, "学习进度同步失败"),
    LEARNING_PROGRESS_REDIS_FAILED(1605, "学习进度Redis操作失败"),
    LEARNING_PROGRESS_DATABASE_FAILED(1606, "学习进度数据库操作失败"),
    LEARNING_PROGRESS_INVALID_NODE_ID(1607, "无效的节点ID"),
    LEARNING_PROGRESS_TOC_PARSE_FAILED(1608, "目录结构解析失败"),
    
    // 数据解析相关 17xx
    JSON_PARSE_ERROR(1701, "数据格式解析失败"),
    CONTENT_HASH_ERROR(1702, "内容哈希计算失败"),

    // 评论相关 14xx
    COMMENT_NOT_FOUND(1401, "评论不存在"),
    COMMENT_PARENT_NOT_FOUND(1402, "父评论不存在"),
    COMMENT_INVALID_TYPE(1403, "评论类型不正确"),
    COMMENT_OBJECT_NOT_FOUND(1404, "评论对象不存在"),

    // 内容管理相关 13xx
    CONTENTS_COURSE_NOT_FOUND(1301, "课程不存在"),
    CONTENTS_POST_NOT_FOUND(1302, "帖子不存在"),
    CONTENTS_PINNED_ITEMS_LIMIT_EXCEEDED(1303, "置顶帖子数量超限"),
    CONTENTS_INVALID_POST_TYPE(1304, "无效的帖子类型"),
    POSTING_INVALID_PARAMETER(1305, "帖子参数无效"),
    POSTING_CONTENT_PARSE_FAILED(1306, "帖子内容解析失败"),
    POSTING_NODE_NOT_FOUND(1307, "节点不存在"),
    POSTING_LIST_QUERY_FAILED(1308, "帖子列表查询失败"),
    NODE_STATE_INVALID(1309, "节点不是发布状态，暂时无法访问"),

    // 目录管理相关 18xx
    TOC_USER_TOC_NOT_FOUND(1801, "用户目录不存在"),
    TOC_INDEX_OUT_OF_BOUNDS(1802, "目录索引超出范围"),

    // 课程排行相关 19xx
    COURSE_RANKING_INVALID_LIMIT(1901, "排行榜查询数量超出限制"),
    COURSE_RANKING_REDIS_OPERATION_FAILED(1902, "排行榜数据操作失败"),

    // 消息排行相关 20xx
    MESSAGE_NOT_FOUND(2001, "消息不存在"),

    // 专业相关 21xx
    PROFESSION_NOT_FOUND(2101, "专业不存在"),
    PROFESSION_NAME_REQUIRED(2102, "专业名称不能为空"),
    PROFESSION_ALREADY_APPROVED(2103, "专业状态已是批准状态，无需重复操作"),
    PROFESSION_ALREADY_REJECTED(2104, "专业状态已是拒绝状态，无需重复操作"),
    PROFESSION_STATE_CONFLICT(2105, "专业状态已被其他操作修改，请刷新后重试"),
    PROFESSION_HOT_LIST_FAILED(2106, "获取热门专业失败"),
    PROFESSION_INVALID_LIMIT(2107, "专业查询数量超出限制"),
    PROFESSION_BLOCKED(2108, "该职业已被屏蔽，暂时无法访问"),

    // 记忆卡片相关 22xx
    MEMORY_CARD_DECK_NOT_FOUND(2201, "卡片组不存在"),
    MEMORY_CARD_NOT_FOUND(2202, "记忆卡片不存在"),
    MEMORY_CARD_VERSION_NOT_FOUND(2203, "卡片版本不存在"),
    MEMORY_BANK_COURSE_NOT_FOUND(2204, "记忆库课程不存在"),
    SRS_STATE_NOT_FOUND(2205, "SRS复习状态不存在"),
    MEMORY_CARD_DECK_ALREADY_EXISTS(2206, "卡片组已存在于记忆库中"),
    INVALID_REVIEW_RESULT(2207, "无效的复习结果"),
    INVALID_FREQUENCY_SETTING(2208, "无效的复习频率设置"),
    INVALID_COURSE_STUDY_STATUS(2209, "无效的课程学习状态"),

    // 限流相关 23xx
    RATE_LIMIT_EXCEEDED(2301, "访问过于频繁，请稍后再试"),
    RATE_LIMIT_CONFIG_ERROR(2302, "限流配置错误"),

    // 系统错误 9xxx
    SYSTEM_ERROR(9999, "系统繁忙，请稍后重试"),
    SYSTEM_READONLY_MODE(9010, "系统维护中，暂时无法进行此操作"),
    DATABASE_ERROR(9001, "数据访问异常"),
    EXTERNAL_SERVICE_ERROR(9002, "外部服务调用失败"),
    REDIS_CONNECTION_ERROR(9003, "Redis连接异常"),
    REDIS_OPERATION_ERROR(9004, "Redis操作失败"),
    AI_SERVICE_REQUEST_FAILED(9005, "AI服务请求失败"),
    AI_SERVICE_RESPONSE_PARSE_FAILED(9006, "AI服务响应解析失败"),
    AI_SERVICE_INVALID_PARAMETER(9007, "AI服务参数无效"),
    SCHEDULER_TASK_FAILED(9008, "定时任务执行失败"),
    SCHEDULER_DATA_SYNC_FAILED(9009, "数据同步任务失败");

    private final int code;
    private final String message;
    
    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
    
    public int getCode() {
        return code;
    }
    
    public String getMessage() {
        return message;
    }
    
    /**
     * 创建业务异常
     */
    public BusinessException exception() {
        return new BusinessException(this.code, this.message);
    }
    
    /**
     * 创建支持国际化的业务异常
     */
    public BusinessException exception(String localizedMessage) {
        return new BusinessException(this.code, localizedMessage);
    }
    
    /**
     * 创建带原因的业务异常
     */
    public BusinessException exception(Throwable cause) {
        return new BusinessException(this.code, this.message, cause);
    }
}