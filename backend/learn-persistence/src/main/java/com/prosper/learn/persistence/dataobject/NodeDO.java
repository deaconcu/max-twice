package com.prosper.learn.persistence.dataobject;

import com.prosper.learn.common.Utils;
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

    private LocalDateTime cTime;

    private LocalDateTime uTime;

    public static NodeDO createRoot(Long creator, Long courseId) {
        NodeDO node = new NodeDO();
        node.setName("课程根目录");
        node.setDescription("");
        node.setCourseId(courseId);
        node.setCreator(creator);
        node.setRoot(0L);
        node.setCTime(LocalDateTime.now());
        node.setUTime(LocalDateTime.now());
        return node;
    }
}
