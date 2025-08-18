package com.prosper.learn.domain.entity;

import com.prosper.learn.common.Aggregate;
import lombok.Data;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Data
public class NodeTable implements Aggregate<Integer> {

    private Integer id;

    private String description;

    private List<Integer> nodeIds;

    private List<Node> nodes;

    private int nodeId;

    private int subcourseId;

    private int userId;

    private int vote;

    private Date createTime;

    public NodeTable() {
        nodes = new LinkedList<>();
    }

    public NodeTable(int nodeId, int subcourseId) {
        this();
        this.nodeId = nodeId;
        this.subcourseId = subcourseId;
        this.description = "DEFAULT";
        this.nodeIds = new LinkedList<>();
    }

    public void addNode(Map<Integer, Node> nodeMap) {
        for (int id: nodeIds) {
            if (!nodeMap.containsKey(id)) continue;
            nodes.add(nodeMap.get(id));
        }
    }
}