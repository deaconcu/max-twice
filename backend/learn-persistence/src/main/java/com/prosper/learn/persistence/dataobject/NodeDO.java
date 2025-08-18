package com.prosper.learn.persistence.dataobject;

import com.prosper.learn.common.Utils;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NodeDO {

    private int id;

    private String name;

    private String description;

    private int courseId;

    private int root;

    private int creator;

    private int commentCount;

    private int state;

    private LocalDateTime cTime;

    private LocalDateTime uTime;

    public static NodeDO createRoot(int creator, int courseId) {
        NodeDO node = new NodeDO();
        node.setName("课程根目录");
        node.setDescription("");
        node.setCourseId(courseId);
        node.setCreator(creator);
        node.setRoot(0);
        node.setCTime(LocalDateTime.now());
        node.setUTime(LocalDateTime.now());
        return node;
    }
}
