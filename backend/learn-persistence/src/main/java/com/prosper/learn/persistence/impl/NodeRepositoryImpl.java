package com.prosper.learn.persistence.impl;

//@Repository
public class NodeRepositoryImpl { //implements NodeRepository {

    /*
    private final NodeMapper nodeMapper;

    private final CourseMapper courseMapper;

    private final SubcourseMapper subCourseMapper;

    public NodeRepositoryImpl(NodeMapper nodeMapper, CourseMapper courseMapper, SubcourseMapper subCourseMapper) {
        this.nodeMapper = nodeMapper;
        this.courseMapper = courseMapper;
        this.subCourseMapper = subCourseMapper;
    }

    @Override
    public Node find(Integer id) {
        return Converter.INSTANCE.nodeToEntity(nodeMapper.getById(id));
    }

    @Override
    public List<Node> listByIds(List<Integer> ids) {
        if (ids == null || ids.size() == 0) return new LinkedList<>();
        return Converter.INSTANCE.nodeToEntity(nodeMapper.getByIds(ids));
    }

    @Override
    public List<Node> listByPage(int count, int offset) {
        return Converter.INSTANCE.nodeToEntity(nodeMapper.list(count, offset));
    }

    @Override
    public List<Node> listBySubcourse(int subcourseId) {
        return Converter.INSTANCE.nodeToEntity(nodeMapper.listBySubcourse(subcourseId));
    }

    @Override
    public List<Node> listByUser(int userId, int count, int offset) {
        return Converter.INSTANCE.nodeToEntity(nodeMapper.listByUser(userId, count, offset));
    }

    @Override
    public void remove(Node node) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void save(Node node) {
        if (node.getId() == null) {
            SubcourseDO subcourseInDb = subCourseMapper.getById(node.getSubcourseId());
            if (subcourseInDb == null) throw new IllegalArgumentException("课程不存在");
            node.setUserId(1);
            NodeDO nodeDO = Converter.INSTANCE.nodeToDo(node);
            nodeMapper.insert(nodeDO);
            node.setId(nodeDO.getId());
        } else {
            nodeMapper.update(Converter.INSTANCE.nodeToDo(node));
        }
    }
*/
}
