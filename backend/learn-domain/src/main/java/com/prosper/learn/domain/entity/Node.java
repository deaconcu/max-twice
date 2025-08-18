package com.prosper.learn.domain.entity;

import com.prosper.learn.common.Aggregate;
import lombok.Data;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

@Data
public class Node implements Aggregate<Integer> {

    private Integer id;

    private String name;

    private String description;

    private int subcourseId;

    private int userId;

    private List<NodeTable> nodeTables;

    private Date createTime;

    private Date updateTime;

    public Node() {
        nodeTables = new LinkedList<>();
    }

    public Node(int subcourseId) {
        this();
        this.name = "ROOT";
        this.description = "ROOT";
        this.subcourseId = subcourseId;
    }

    public void addChild(NodeTable nodeTable) {
        nodeTables.add(nodeTable);
    }

    public void sortChild() {
        
    }
}
