package com.prosper.learn.persistence.dataobject;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NodeDO {

    private Long id;

    private String name;

    private String description;

    private Long courseId;

    private Long root;

    private Long creator;

    private Integer commentCount;

    private Integer state;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static NodeDO createRoot(Long creator, Long courseId) {
        NodeDO node = new NodeDO();
        node.setName("课程根目录");
        node.setDescription("");
        node.setCourseId(courseId);
        node.setCreator(creator);
        node.setRoot(0L);
        node.setCreatedAt(LocalDateTime.now());
        node.setUpdatedAt(LocalDateTime.now());
        return node;
    }
}
