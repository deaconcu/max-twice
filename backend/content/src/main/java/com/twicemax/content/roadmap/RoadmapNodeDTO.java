package com.twicemax.content.roadmap;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

/**
 * 路线图节点 DTO，对应前端发送的 trunk 中的每个节点
 *
 * 字段说明：
 *  - t: 类型，c=course, n=node, g=group, o=note
 *  - id: c/n 时是数据库 id（c 为 courseId，n 为 nodeId）
 *  - label: g/o 时为用户输入文本；c/n 入库时丢弃，查询时由后端填充
 *  - children: 子节点列表
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoadmapNodeDTO {

    private String t;

    private Long id;

    private String label;

    private List<RoadmapNodeDTO> children;
}
