package com.twicemax.content.roadmap;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

/**
 * 路线图 content 字段的最外层结构。
 *
 *  - v: 协议版本，当前为 2
 *  - trunk: 主干节点列表
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoadmapContentDTO {

    public static final int CURRENT_VERSION = 2;

    private int v;

    private List<RoadmapNodeDTO> trunk;
}
