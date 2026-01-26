package com.prosper.learn.content.node;

import com.prosper.learn.content.course.CourseDO;
import com.prosper.learn.shared.domain.Enums;
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

    private Byte isCourseRoot;  // 是否为课程根节点：0=普通节点, 1=课程根节点

    private String reason;  // 拒绝/封禁原因

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public NodeDO() {}

    public NodeDO(Long creator, Long courseId, String name, String description, Byte state, Byte isCourseRoot) {
        setName(name);
        setDescription(description);
        setCourseId(courseId);
        setCreatorId(creator);
        setState(state);
        setIsCourseRoot(isCourseRoot);  // 标记为课程根节点
    }
}
