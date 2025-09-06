package com.prosper.learn.persistence.dataobject;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NodeDO {

    private Long id;

    private String name;

    private String description;

    private Long courseId;

    private Long creatorId;

    private Integer commentCount;

    private Integer state;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static NodeDO createRoot(Long creator, Long courseId) {
        NodeDO node = new NodeDO();
        node.setName("课程根目录");
        node.setDescription("");
        node.setCourseId(courseId);
        node.setCreatorId(creator);
        node.setCreatedAt(LocalDateTime.now());
        node.setUpdatedAt(LocalDateTime.now());
        return node;
    }
}
