package com.prosper.learn.shared.dto.response;

import com.prosper.learn.shared.dto.response.course.CourseBriefDTO;
import lombok.Data;

import java.util.List;

@Data
public class NodeDTO {

    private Long id;

    private String name;

    private String description;

    private Long courseId;

    /**
     * 课程简要信息
     * 说明：包含课程的 id 和 name，用于显示节点所属课程
     */
    private CourseBriefDTO course;

    private List<NodeDTO> children;

    private Long creatorId;

    private Integer commentCount;

    private Byte state;

    private String createdAt;

    private String updatedAt;

    private Boolean isCompleted;
}
