package com.prosper.learn.domain.service.impl;

import com.prosper.learn.domain.entity.Node;
import com.prosper.learn.domain.entity.NodeTable;
import com.prosper.learn.domain.repository.NodeTableRepository;
import com.prosper.learn.domain.repository.NodeRepository;
import com.prosper.learn.domain.service.iface.DomainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

//@Service
public class DomainServiceImpl implements DomainService {

    @Autowired
    private NodeTableRepository nodeTableRepository;
    @Autowired
    private NodeRepository nodeRepository;

    @Override
    public Node getContents(int subcourseId) {
        List<NodeTable> nodeTables = nodeTableRepository.listBySubcourse(subcourseId);
        List<Node> nodes = nodeRepository.listBySubcourse(subcourseId);

        Node root = null;
        Map<Integer, Node> nodeMap = new HashMap<>();
        for (Node node: nodes) {
            nodeMap.put(node.getId(), node);
            if (node.getName().equals("ROOT")) {
                root = node;
            }
        }

        for (NodeTable nodeTable : nodeTables) {
            Node node = nodeMap.get(nodeTable.getNodeId());
            node.addChild(nodeTable);

            List<Integer> nodeIds = nodeTable.getNodeIds();
            List<Node> ns = new LinkedList<>();
            for (int nodeId: nodeIds) {
                if (!nodeMap.containsKey(nodeId)) continue;
                ns.add(nodeMap.get(nodeId));
            }
            nodeTable.setNodes(ns);
        }

        for (Node node: nodes) {
            node.sortChild();
        }
        return root;
    }

    @Override
    public void createNodeList(NodeTable nodeTable) {
        Node node = nodeRepository.find(nodeTable.getNodeId());
        nodeTable.setSubcourseId(node.getSubcourseId());

        nodeTableRepository.save(nodeTable);
    }
}
