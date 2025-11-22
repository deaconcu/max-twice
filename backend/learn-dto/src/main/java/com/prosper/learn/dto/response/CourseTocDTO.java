package com.prosper.learn.dto.response;

import com.prosper.learn.dto.response.node.NodeWithProgressDTO;
import lombok.Data;

import java.util.Map;

@Data
public class CourseTocDTO {

    private String contents;

    // 课程id和节点信息的map (包含完成状态)
    private Map<Long, NodeWithProgressDTO> nodeInfos;

    public CourseTocDTO(String contents, Map<Long, NodeWithProgressDTO> nodeInfos) {
        this.contents = contents;
        this.nodeInfos = nodeInfos;
    }

}
