package com.twicemax.application.dto.v2;

/**
 * v2 通用「已受理」响应体。
 *
 * <p>用于 HTTP 202 Accepted 场景：资源已提交但未立即生效（待审核 / 异步任务等）。
 *
 * <p>响应示例：
 * <pre>
 * HTTP/1.1 202 Accepted
 * Content-Type: application/json
 *
 * { "id": 123 }
 * </pre>
 *
 * @param id 已受理资源的 ID
 */
public record CreateAcceptedResponse(Long id) {
}
