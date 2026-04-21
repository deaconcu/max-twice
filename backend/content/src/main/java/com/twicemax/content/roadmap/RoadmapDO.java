package com.twicemax.content.roadmap;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RoadmapDO {

    private Long id;

    private String content;

    private String contentHash;

    private Long roleId;

    private Long creatorId;

    private String description;

    private Byte state;  // 状态：0-待审核，1-已批准，2-已拒绝

    private String reason;  // 拒绝/封禁原因

    private Double score;  // 计算出的排序分数

    private LocalDateTime scoreCalculatedAt;  // 分数计算时间

    private Integer nodeCount;  // 路线图包含的节点数量

    private LocalDateTime deletedAt;  // 软删除时间

    private LocalDateTime updatedAt;

    private LocalDateTime createdAt;
}
