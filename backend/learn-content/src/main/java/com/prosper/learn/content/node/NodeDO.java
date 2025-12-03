package com.prosper.learn.content.node;

import com.prosper.learn.common.Enums;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NodeDO {

    private Long id;

    private String name;

    private String description;

    private Long courseId;

    private Long creatorId;

    private Byte state;

    private String reason;  // 拒绝/封禁原因

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static NodeDO createRoot(Long creator, Long courseId) {
        NodeDO node = new NodeDO();
        node.setName("课程根目录");
        node.setDescription("");
        node.setCourseId(courseId);
        node.setCreatorId(creator);
        node.setState(Enums.ContentState.PUBLISHED.value());
        return node;
    }
}
