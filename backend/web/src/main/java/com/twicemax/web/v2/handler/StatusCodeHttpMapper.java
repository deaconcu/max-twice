package com.twicemax.web.v2.handler;

import com.twicemax.shared.domain.exception.StatusCode;
import org.springframework.http.HttpStatus;

import java.util.EnumMap;
import java.util.Map;

/**
 * 业务错误码 → HTTP 状态码映射（v2 规范 3.3）。
 *
 * <p>独立于 {@link StatusCode} 维护，让领域层不依赖 Web 协议细节。
 * 新增枚举如忘记加映射，{@link com.twicemax.web.v2.handler.StatusCodeHttpMapperTest}
 * 会在 CI 阶段失败。
 *
 * <p>{@link StatusCode#OK} 不在 mapper 内（成功路径不走异常处理器）。
 * 未配置或 null 时回落 {@link HttpStatus#INTERNAL_SERVER_ERROR}。
 */
public final class StatusCodeHttpMapper {

    private static final Map<StatusCode, HttpStatus> MAP = new EnumMap<>(StatusCode.class);

    static {
        // 400 Bad Request：参数/格式错误
        MAP.put(StatusCode.INVALID_PARAMETER, HttpStatus.BAD_REQUEST);
        MAP.put(StatusCode.INVALID_DATE, HttpStatus.BAD_REQUEST);
        MAP.put(StatusCode.INVALID_DAYS_RANGE, HttpStatus.BAD_REQUEST);
        MAP.put(StatusCode.INVALID_OPERATION, HttpStatus.BAD_REQUEST);
        MAP.put(StatusCode.NOT_SUPPORTED, HttpStatus.BAD_REQUEST);
        MAP.put(StatusCode.BATCH_SIZE_EXCEEDED, HttpStatus.BAD_REQUEST);
        MAP.put(StatusCode.USER_INVALID_EMAIL_FORMAT, HttpStatus.BAD_REQUEST);
        MAP.put(StatusCode.USER_INVALID_USERNAME_LENGTH, HttpStatus.BAD_REQUEST);
        MAP.put(StatusCode.USER_INVALID_PASSWORD_LENGTH, HttpStatus.BAD_REQUEST);
        MAP.put(StatusCode.ROLE_NAME_REQUIRED, HttpStatus.BAD_REQUEST);
        MAP.put(StatusCode.ROLE_INVALID_LIMIT, HttpStatus.BAD_REQUEST);
        MAP.put(StatusCode.ROLE_CATEGORY_INVALID, HttpStatus.BAD_REQUEST);
        MAP.put(StatusCode.COURSE_CATEGORY_INVALID, HttpStatus.BAD_REQUEST);
        MAP.put(StatusCode.COURSE_RANKING_INVALID_LIMIT, HttpStatus.BAD_REQUEST);
        MAP.put(StatusCode.INVALID_POST_TYPE, HttpStatus.BAD_REQUEST);
        MAP.put(StatusCode.POST_INVALID_PARAMETER, HttpStatus.BAD_REQUEST);
        MAP.put(StatusCode.USER_COURSE_PROGRESS_INVALID, HttpStatus.BAD_REQUEST);
        MAP.put(StatusCode.LEARNING_PROGRESS_INVALID_NODE_ID, HttpStatus.BAD_REQUEST);
        MAP.put(StatusCode.COMMENT_INVALID_TYPE, HttpStatus.BAD_REQUEST);
        MAP.put(StatusCode.TOC_INDEX_OUT_OF_BOUNDS, HttpStatus.BAD_REQUEST);
        MAP.put(StatusCode.INVALID_REVIEW_RESULT, HttpStatus.BAD_REQUEST);
        MAP.put(StatusCode.INVALID_FREQUENCY_SETTING, HttpStatus.BAD_REQUEST);
        MAP.put(StatusCode.INVALID_COURSE_STUDY_STATUS, HttpStatus.BAD_REQUEST);
        MAP.put(StatusCode.AI_SERVICE_INVALID_PARAMETER, HttpStatus.BAD_REQUEST);

        // 401 Unauthorized：未认证
        MAP.put(StatusCode.USER_NOT_LOGIN, HttpStatus.UNAUTHORIZED);
        MAP.put(StatusCode.USER_PASSWORD_WRONG, HttpStatus.UNAUTHORIZED);
        MAP.put(StatusCode.USER_LOGIN_FAILED, HttpStatus.UNAUTHORIZED);

        // 403 Forbidden：已认证但禁止
        MAP.put(StatusCode.PERMISSION_DENIED, HttpStatus.FORBIDDEN);
        MAP.put(StatusCode.USER_EMAIL_NOT_VALIDATED, HttpStatus.FORBIDDEN);
        MAP.put(StatusCode.USER_BANNED, HttpStatus.FORBIDDEN);
        MAP.put(StatusCode.INVITE_ONLY, HttpStatus.FORBIDDEN);
        MAP.put(StatusCode.CONTENT_NOT_VISIBLE, HttpStatus.FORBIDDEN);
        MAP.put(StatusCode.COURSE_IS_NOT_PUBLISHED, HttpStatus.FORBIDDEN);
        MAP.put(StatusCode.NODE_STATE_INVALID, HttpStatus.FORBIDDEN);
        MAP.put(StatusCode.ROLE_BLOCKED, HttpStatus.FORBIDDEN);
        MAP.put(StatusCode.MEMORY_CARD_NOT_AVAILABLE, HttpStatus.FORBIDDEN);
        MAP.put(StatusCode.INTERACTION_CANNOT_UPVOTE_OWN_CONTENT, HttpStatus.FORBIDDEN);

        // 404 Not Found
        MAP.put(StatusCode.NOT_FOUND, HttpStatus.NOT_FOUND);
        MAP.put(StatusCode.USER_NOT_FOUND, HttpStatus.NOT_FOUND);
        MAP.put(StatusCode.USER_PROFILE_NOT_FOUND, HttpStatus.NOT_FOUND);
        MAP.put(StatusCode.ROLE_NOT_FOUND, HttpStatus.NOT_FOUND);
        MAP.put(StatusCode.COURSE_NOT_FOUND, HttpStatus.NOT_FOUND);
        MAP.put(StatusCode.COURSE_PARENT_NOT_FOUND, HttpStatus.NOT_FOUND);
        MAP.put(StatusCode.POST_NOT_FOUND, HttpStatus.NOT_FOUND);
        MAP.put(StatusCode.NODE_NOT_FOUND, HttpStatus.NOT_FOUND);
        MAP.put(StatusCode.COMMENT_NOT_FOUND, HttpStatus.NOT_FOUND);
        MAP.put(StatusCode.COMMENT_PARENT_NOT_FOUND, HttpStatus.NOT_FOUND);
        MAP.put(StatusCode.COMMENT_OBJECT_NOT_FOUND, HttpStatus.NOT_FOUND);
        MAP.put(StatusCode.ROADMAP_NOT_FOUND, HttpStatus.NOT_FOUND);
        MAP.put(StatusCode.USER_ROADMAP_NOT_FOUND, HttpStatus.NOT_FOUND);
        MAP.put(StatusCode.USER_COURSE_NOT_FOUND, HttpStatus.NOT_FOUND);
        MAP.put(StatusCode.TOC_USER_TOC_NOT_FOUND, HttpStatus.NOT_FOUND);
        MAP.put(StatusCode.COURSE_TOC_NOT_FOUND, HttpStatus.NOT_FOUND);
        MAP.put(StatusCode.MESSAGE_NOT_FOUND, HttpStatus.NOT_FOUND);
        MAP.put(StatusCode.MEMORY_CARD_DECK_NOT_FOUND, HttpStatus.NOT_FOUND);
        MAP.put(StatusCode.MEMORY_CARD_NOT_FOUND, HttpStatus.NOT_FOUND);
        MAP.put(StatusCode.MEMORY_CARD_VERSION_NOT_FOUND, HttpStatus.NOT_FOUND);
        MAP.put(StatusCode.MEMORY_BANK_COURSE_NOT_FOUND, HttpStatus.NOT_FOUND);
        MAP.put(StatusCode.SRS_STATE_NOT_FOUND, HttpStatus.NOT_FOUND);
        MAP.put(StatusCode.USER_CARD_IN_COURSE_NOT_FOUND, HttpStatus.NOT_FOUND);
        MAP.put(StatusCode.USER_VERIFICATION_CODE_NOT_FOUND, HttpStatus.NOT_FOUND);

        // 409 Conflict：资源状态冲突
        MAP.put(StatusCode.USER_ALREADY_EXISTS, HttpStatus.CONFLICT);
        MAP.put(StatusCode.USER_ALREADY_FOLLOWED, HttpStatus.CONFLICT);
        MAP.put(StatusCode.USER_COURSE_ALREADY_SUBSCRIBED, HttpStatus.CONFLICT);
        MAP.put(StatusCode.USER_COURSE_NOT_SUBSCRIBED, HttpStatus.CONFLICT);
        MAP.put(StatusCode.USER_COURSE_ALREADY_STARTED, HttpStatus.CONFLICT);
        MAP.put(StatusCode.USER_COURSE_NOT_STARTED, HttpStatus.CONFLICT);
        MAP.put(StatusCode.USER_ROADMAP_ALREADY_STARTED, HttpStatus.CONFLICT);
        MAP.put(StatusCode.USER_ROADMAP_NOT_STARTED, HttpStatus.CONFLICT);
        MAP.put(StatusCode.USER_PASSWORD_ALREADY_SET, HttpStatus.CONFLICT);
        MAP.put(StatusCode.COURSE_ALREADY_APPROVED, HttpStatus.CONFLICT);
        MAP.put(StatusCode.COURSE_ALREADY_REJECTED, HttpStatus.CONFLICT);
        MAP.put(StatusCode.COURSE_ALREADY_BANNED, HttpStatus.CONFLICT);
        MAP.put(StatusCode.COURSE_STATE_CONFLICT, HttpStatus.CONFLICT);
        MAP.put(StatusCode.ROLE_ALREADY_APPROVED, HttpStatus.CONFLICT);
        MAP.put(StatusCode.ROLE_ALREADY_REJECTED, HttpStatus.CONFLICT);
        MAP.put(StatusCode.ROLE_STATE_CONFLICT, HttpStatus.CONFLICT);
        MAP.put(StatusCode.NODE_ALREADY_COMPLETED, HttpStatus.CONFLICT);
        MAP.put(StatusCode.NODE_ALREADY_NOT_COMPLETED, HttpStatus.CONFLICT);
        MAP.put(StatusCode.MEMORY_CARD_DECK_ALREADY_EXISTS, HttpStatus.CONFLICT);

        // 410 Gone：会话/验证码失效
        MAP.put(StatusCode.PENDING_SESSION_INVALID, HttpStatus.GONE);
        MAP.put(StatusCode.PASSWORD_RESET_SESSION_INVALID, HttpStatus.GONE);
        MAP.put(StatusCode.USER_VERIFICATION_CODE_EXPIRED, HttpStatus.GONE);
        MAP.put(StatusCode.CAPTCHA_EXPIRED, HttpStatus.GONE);

        // 412 Precondition Failed
        MAP.put(StatusCode.PASSWORD_RESET_NOT_VERIFIED, HttpStatus.PRECONDITION_FAILED);

        // 413 Payload Too Large
        MAP.put(StatusCode.FILE_TOO_LARGE, HttpStatus.PAYLOAD_TOO_LARGE);
        MAP.put(StatusCode.IMAGE_DIMENSION_TOO_LARGE, HttpStatus.PAYLOAD_TOO_LARGE);

        // 415 Unsupported Media Type
        MAP.put(StatusCode.FILE_TYPE_NOT_ALLOWED, HttpStatus.UNSUPPORTED_MEDIA_TYPE);

        // 422 Unprocessable Entity：业务校验失败
        MAP.put(StatusCode.USER_VERIFICATION_CODE_INVALID, HttpStatus.UNPROCESSABLE_ENTITY);
        MAP.put(StatusCode.USER_PASSWORD_TOO_WEAK, HttpStatus.UNPROCESSABLE_ENTITY);
        MAP.put(StatusCode.CAPTCHA_INVALID, HttpStatus.UNPROCESSABLE_ENTITY);
        MAP.put(StatusCode.INVALID_IMAGE, HttpStatus.UNPROCESSABLE_ENTITY);
        MAP.put(StatusCode.ROADMAP_CONTENT_INVALID, HttpStatus.UNPROCESSABLE_ENTITY);

        // 428 Precondition Required
        MAP.put(StatusCode.CAPTCHA_REQUIRED, HttpStatus.PRECONDITION_REQUIRED);

        // 429 Too Many Requests：限流/超限
        MAP.put(StatusCode.RATE_LIMIT_EXCEEDED, HttpStatus.TOO_MANY_REQUESTS);
        MAP.put(StatusCode.USER_VERIFICATION_CODE_SEND_TOO_FREQUENT, HttpStatus.TOO_MANY_REQUESTS);
        MAP.put(StatusCode.USER_VERIFICATION_CODE_ATTEMPTS_EXCEEDED, HttpStatus.TOO_MANY_REQUESTS);
        MAP.put(StatusCode.UPLOAD_TOO_FREQUENT, HttpStatus.TOO_MANY_REQUESTS);
        MAP.put(StatusCode.UPLOAD_QUOTA_EXCEEDED, HttpStatus.TOO_MANY_REQUESTS);
        MAP.put(StatusCode.USER_SUBSCRIPTION_LIMIT_EXCEEDED, HttpStatus.TOO_MANY_REQUESTS);
        MAP.put(StatusCode.LEARNING_ROADMAP_LIMIT_EXCEEDED, HttpStatus.TOO_MANY_REQUESTS);
        MAP.put(StatusCode.NODE_CARD_LIMIT_EXCEEDED, HttpStatus.TOO_MANY_REQUESTS);
        MAP.put(StatusCode.USER_CARD_LIMIT_EXCEEDED, HttpStatus.TOO_MANY_REQUESTS);

        // 502 Bad Gateway：外部服务
        MAP.put(StatusCode.EXTERNAL_SERVICE_ERROR, HttpStatus.BAD_GATEWAY);
        MAP.put(StatusCode.AI_SERVICE_REQUEST_FAILED, HttpStatus.BAD_GATEWAY);
        MAP.put(StatusCode.CAPTCHA_SERVICE_ERROR, HttpStatus.BAD_GATEWAY);

        // 503 Service Unavailable
        MAP.put(StatusCode.SYSTEM_READONLY_MODE, HttpStatus.SERVICE_UNAVAILABLE);

        // 500 Internal Server Error
        MAP.put(StatusCode.UNKNOWN_EXCEPTION, HttpStatus.INTERNAL_SERVER_ERROR);
        MAP.put(StatusCode.SYSTEM_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        MAP.put(StatusCode.OPERATION_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
        MAP.put(StatusCode.JSON_PROCESSING_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        MAP.put(StatusCode.JSON_PARSE_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        MAP.put(StatusCode.CONTENT_HASH_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        MAP.put(StatusCode.DATABASE_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        MAP.put(StatusCode.REDIS_CONNECTION_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        MAP.put(StatusCode.REDIS_OPERATION_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        MAP.put(StatusCode.AI_SERVICE_RESPONSE_PARSE_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
        MAP.put(StatusCode.SCHEDULER_TASK_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
        MAP.put(StatusCode.SCHEDULER_DATA_SYNC_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
        MAP.put(StatusCode.RATE_LIMIT_CONFIG_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        MAP.put(StatusCode.COURSE_DELETE_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
        MAP.put(StatusCode.COURSE_OPERATION_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
        MAP.put(StatusCode.ROLE_HOT_LIST_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
        MAP.put(StatusCode.POST_CONTENT_PARSE_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
        MAP.put(StatusCode.POST_LIST_QUERY_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
        MAP.put(StatusCode.LEARNING_PROGRESS_SYNC_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
        MAP.put(StatusCode.LEARNING_PROGRESS_REDIS_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
        MAP.put(StatusCode.LEARNING_PROGRESS_DATABASE_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
        MAP.put(StatusCode.LEARNING_PROGRESS_TOC_PARSE_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
        MAP.put(StatusCode.COURSE_RANKING_REDIS_OPERATION_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
        MAP.put(StatusCode.USER_SUBSCRIPTION_PARSE_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        MAP.put(StatusCode.FILE_UPLOAD_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
        MAP.put(StatusCode.FILE_DELETE_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
        MAP.put(StatusCode.IMAGE_COMPRESSION_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private StatusCodeHttpMapper() {}

    /**
     * 把业务 StatusCode 映射到 HTTP 状态码。
     * 未列入或 null 时返回 500（保守的回落策略；测试会确保所有枚举都已配置）。
     */
    public static HttpStatus toHttp(StatusCode code) {
        if (code == null) return HttpStatus.INTERNAL_SERVER_ERROR;
        return MAP.getOrDefault(code, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * 给单测使用：判断某 code 是否已显式配置（区分"配了 500"和"没配回落 500"）。
     */
    static boolean hasMapping(StatusCode code) {
        return MAP.containsKey(code);
    }
}
