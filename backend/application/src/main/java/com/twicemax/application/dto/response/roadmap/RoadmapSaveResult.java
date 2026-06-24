package com.twicemax.application.dto.response.roadmap;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 路线图保存（创建/更新）的返回结果。
 *
 * <p>草稿场景：即使引用失效也允许落库，把失效引用通过 {@code invalidReferences} 反馈给前端。
 * <p>发布场景：引用失效会直接抛 {@code ROADMAP_CONTENT_INVALID}，
 * 失效列表通过异常的 {@code details} 字段传递，本 DTO 的 {@code invalidReferences} 始终为空。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoadmapSaveResult {

    /** 路线图 ID（创建时为新生成的 ID，更新时为入参 ID） */
    private Long id;

    /** 失效引用列表，仅草稿场景可能非空；为空时省略输出 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private InvalidReferences invalidReferences;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvalidReferences {
        private List<Long> missingCourseIds;
        private List<Long> missingNodeIds;

        public boolean isEmpty() {
            return (missingCourseIds == null || missingCourseIds.isEmpty())
                && (missingNodeIds == null || missingNodeIds.isEmpty());
        }
    }
}
