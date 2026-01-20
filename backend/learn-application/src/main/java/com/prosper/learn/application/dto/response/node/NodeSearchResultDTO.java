package com.prosper.learn.application.dto.response.node;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 节点搜索结果 DTO
 *
 * 用途：向量搜索返回的节点结果，包含相似度分数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class NodeSearchResultDTO extends NodeWithCourseDTO {

    /**
     * 相似度分数
     * 范围：0.0-1.0，1.0表示完全匹配
     */
    private Float similarityScore;
}
