package com.prosper.learn.persistence.impl;

//@Repository
public class NodeTableRepositoryImpl { //implements NodeTableRepository {

    /*
    private final NodeTableMapper nodeTableMapper;
    private final NodeMapper nodeMapper;

    public NodeTableRepositoryImpl(NodeTableMapper nodeTableMapper, NodeMapper nodeMapper) {
        this.nodeTableMapper = nodeTableMapper;
        this.nodeMapper = nodeMapper;
    }

    @Override
    public NodeTable find(Integer id) {
        return Converter.INSTANCE.nodeListToEntity(nodeTableMapper.getById(id));
    }

    @Override
    public List<NodeTable> listByPage(int count, int offset) {
        return Converter.INSTANCE.nodeListToEntity(nodeTableMapper.list(count, offset));
    }

    @Override
    public List<NodeTable> listByNode(int nodeId) {
        return Converter.INSTANCE.nodeListToEntity(nodeTableMapper.listByNode(nodeId));
    }

    @Override
    public List<NodeTable> listWithNodeByNode(int nodeId) {
        List<NodeTable> nodeTables = Converter.INSTANCE.nodeListToEntity(nodeTableMapper.listByNode(nodeId));
        List<Integer> nodeIds = new LinkedList<>();
        for (NodeTable nodeTable : nodeTables) {
            if (nodeTable.getNodeIds() == null) continue;
            nodeIds.addAll(nodeTable.getNodeIds());
        }
        if (nodeIds.size() > 0) {
            List<Node> nodes = Converter.INSTANCE.nodeToEntity(nodeMapper.getByIds(nodeIds));
            Map<Integer, Node> nodeMap = new HashMap<>();
            for (Node node: nodes) {
                nodeMap.put(node.getId(), node);
            }
            for (NodeTable nodeTable : nodeTables) {
                nodeTable.addNode(nodeMap);
            }
        }
        return nodeTables;
    }

    @Override
    public List<NodeTable> listBySubcourse(int subcourseId) {
        return Converter.INSTANCE.nodeListToEntity(nodeTableMapper.listBySubcourse(subcourseId));
    }

    @Override
    public void remove(NodeTable nodeTable) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void save(NodeTable nodeTable) {
        if (nodeTable.getId() != null) return;
        // todo nodelist不能重复
        if (nodeTable.getNodeIds() != null && nodeTable.getNodeIds().size() > 0) {
            List<NodeDO> nodeDOList = nodeMapper.getByIds(nodeTable.getNodeIds());
            if (nodeDOList.size() != nodeTable.getNodeIds().size()) {
                throw new IllegalArgumentException("有部分节点不存在");
            }
        }

        NodeTableDO nodeTableDO = Converter.INSTANCE.nodeListToDo(nodeTable);
        nodeTableMapper.insert(nodeTableDO);
        nodeTable.setId(nodeTableDO.getId());
    }
     */
}
