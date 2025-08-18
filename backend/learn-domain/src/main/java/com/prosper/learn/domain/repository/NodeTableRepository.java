package com.prosper.learn.domain.repository;

import com.prosper.learn.common.Repository;
import com.prosper.learn.domain.entity.NodeTable;

import java.util.List;

public interface NodeTableRepository extends Repository<NodeTable, Integer> {

    List<NodeTable> listBySubcourse(int subcourseId);

    List<NodeTable> listByNode(int nodeId);

    List<NodeTable> listWithNodeByNode(int nodeId);
}
