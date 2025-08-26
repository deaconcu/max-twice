package com.prosper.learn.dto;

import lombok.Data;

import java.util.Map;

@Data
public class CourseTocDTO {

    private String contents;

    // 课程id和节点信息的map (包含完成状态)
    private Map<Integer, NodeDTOV2> nodeInfos;

    public CourseTocDTO(String contents, Map<Integer, NodeDTOV2> nodeInfos) {
        this.contents = contents;
        this.nodeInfos = nodeInfos;
    }

}
