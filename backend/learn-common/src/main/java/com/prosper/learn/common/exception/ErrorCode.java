package com.prosper.learn.common.exception;

/**
 * 错误码枚举
 */
public enum ErrorCode {
    
    // 用户认证相关 A0xxx
    USER_NOT_LOGIN("A0001", "用户未登录"),
    PERMISSION_DENIED("A0002", "权限不足"),
    USER_NOT_FOUND("A0003", "user.not.found"),
    USER_ALREADY_EXISTS("A0004", "user.already.exists"),
    USER_PASSWORD_WRONG("A0005", "user.password.wrong"),
    USER_EMAIL_NOT_VALIDATED("A0006", "user.email.not.validated"),
    USER_INVALID_EMAIL_FORMAT("A0007", "邮箱格式不正确"),
    USER_INVALID_USERNAME_LENGTH("A0008", "用户名长度超出限制"),
    USER_INVALID_PASSWORD_LENGTH("A0009", "密码长度不符合要求"),
    USER_VERIFICATION_CODE_INVALID("A0010", "验证码无效或已过期"),
    USER_VERIFICATION_CODE_NOT_FOUND("A0011", "验证码不存在"),
    USER_ALREADY_FOLLOWED("A0012", "已关注该用户"),
    USER_SUBSCRIPTION_PARSE_ERROR("A0013", "订阅数据解析失败"),
    USER_COURSE_ALREADY_SUBSCRIBED("A0014", "课程已订阅"),
    USER_COURSE_NOT_SUBSCRIBED("A0015", "课程未订阅"),

    // 通用业务相关 B00xx
    NOT_SUPPORTED("B0001", "不支持的操作类型"),
    INVALID_PARAMETER("B0002", "参数异常"),
    JSON_PROCESSING_ERROR("B0003", "JSON处理异常"),
    INVALID_DATE("B0004", "无效的日期"),
    INVALID_DAYS_RANGE("B0005", "无效的天数范围"),

    // 课程相关 B01xx
    COURSE_NOT_FOUND("B0101", "课程不存在"),
    COURSE_ALREADY_APPROVED("B0102", "课程状态已是批准状态，无需重复操作"),
    COURSE_ALREADY_REJECTED("B0103", "课程状态已是被屏蔽状态，无需重复操作"),
    COURSE_STATE_CONFLICT("B0104", "课程状态已被其他操作修改，请刷新后重试"),
    COURSE_PARENT_NOT_FOUND("B0105", "父课程不存在"),
    COURSE_DELETE_FAILED("B0106", "课程删除失败"),
    COURSE_OPERATION_FAILED("B0107", "课程操作失败"),

    // 路线图相关 B02xx
    ROADMAP_NOT_FOUND("B0201", "路线图不存在"),
    ROADMAP_CONTENT_INVALID("B0202", "路线图内容格式不正确"),
    ROADMAP_PIN_LIMIT_EXCEEDED("B0203", "最多只能置顶19个路线图"),
    
    // 学习进度相关 B03xx
    USER_ROADMAP_NOT_FOUND("B0301", "学习记录不存在"),
    USER_COURSE_NOT_FOUND("B0302", "课程学习记录不存在"),
    USER_COURSE_PROGRESS_INVALID("B0303", "进度百分比必须在0-100之间"),
    LEARNING_PROGRESS_SYNC_FAILED("B0304", "学习进度同步失败"),
    LEARNING_PROGRESS_REDIS_FAILED("B0305", "学习进度Redis操作失败"),
    LEARNING_PROGRESS_DATABASE_FAILED("B0306", "学习进度数据库操作失败"),
    LEARNING_PROGRESS_INVALID_NODE_ID("B0307", "无效的节点ID"),
    LEARNING_PROGRESS_TOC_PARSE_FAILED("B0308", "目录结构解析失败"),
    
    // 数据解析相关 B04xx
    JSON_PARSE_ERROR("B0401", "数据格式解析失败"),
    CONTENT_HASH_ERROR("B0402", "内容哈希计算失败"),

    // 评论相关 B05xx
    COMMENT_NOT_FOUND("B0501", "评论不存在"),
    COMMENT_PARENT_NOT_FOUND("B0502", "父评论不存在"),
    COMMENT_INVALID_TYPE("B0503", "评论类型不正确"),
    COMMENT_OBJECT_NOT_FOUND("B0504", "评论对象不存在"),

    // 内容管理相关 B06xx
    CONTENTS_COURSE_NOT_FOUND("B0601", "课程不存在"),
    CONTENTS_POST_NOT_FOUND("B0602", "帖子不存在"), 
    CONTENTS_PINNED_ITEMS_LIMIT_EXCEEDED("B0603", "置顶帖子数量超限"),
    CONTENTS_INVALID_POST_TYPE("B0604", "无效的帖子类型"),
    POSTING_INVALID_PARAMETER("B0605", "帖子参数无效"),
    POSTING_CONTENT_PARSE_FAILED("B0606", "帖子内容解析失败"),
    POSTING_NODE_NOT_FOUND("B0607", "节点不存在"),
    POSTING_LIST_QUERY_FAILED("B0608", "帖子列表查询失败"),

    // 目录管理相关 B07xx
    TOC_USER_TOC_NOT_FOUND("B0701", "用户目录不存在"),
    TOC_INDEX_OUT_OF_BOUNDS("B0702", "目录索引超出范围"),

    // 课程排行相关 B08xx
    COURSE_RANKING_INVALID_LIMIT("B0801", "排行榜查询数量超出限制"),
    COURSE_RANKING_REDIS_OPERATION_FAILED("B0802", "排行榜数据操作失败"),

    // 消息排行相关 B09xx
    MESSAGE_NOT_FOUND("B0901", "消息不存在"),

    // 专业相关 B10xx
    PROFESSION_NOT_FOUND("B1001", "专业不存在"),
    PROFESSION_NAME_REQUIRED("B1002", "专业名称不能为空"),
    PROFESSION_ALREADY_APPROVED("B1003", "专业状态已是批准状态，无需重复操作"),
    PROFESSION_ALREADY_REJECTED("B1004", "专业状态已是拒绝状态，无需重复操作"),
    PROFESSION_STATE_CONFLICT("B1005", "专业状态已被其他操作修改，请刷新后重试"),
    PROFESSION_HOT_LIST_FAILED("B1006", "获取热门专业失败"),
    PROFESSION_INVALID_LIMIT("B1007", "专业查询数量超出限制"),

    // 系统错误 S0xxx
    SYSTEM_ERROR("S0001", "系统繁忙，请稍后重试"),
    DATABASE_ERROR("S0002", "数据访问异常"),
    EXTERNAL_SERVICE_ERROR("S0003", "外部服务调用失败"),
    REDIS_CONNECTION_ERROR("S0004", "Redis连接异常"),
    REDIS_OPERATION_ERROR("S0005", "Redis操作失败"),
    AI_SERVICE_REQUEST_FAILED("S0006", "AI服务请求失败"),
    AI_SERVICE_RESPONSE_PARSE_FAILED("S0007", "AI服务响应解析失败"),
    AI_SERVICE_INVALID_PARAMETER("S0008", "AI服务参数无效"),
    SCHEDULER_TASK_FAILED("S0009", "定时任务执行失败"),
    SCHEDULER_DATA_SYNC_FAILED("S0010", "数据同步任务失败");

    private final String code;
    private final String message;
    
    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
    
    public String getCode() {
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