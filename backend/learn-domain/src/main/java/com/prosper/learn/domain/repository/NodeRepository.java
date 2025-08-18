package com.prosper.learn.domain.repository;

import com.prosper.learn.common.Repository;
import com.prosper.learn.domain.entity.Node;

import java.util.List;

public interface NodeRepository extends Repository<Node, Integer> {

    List<Node> listBySubcourse(int subcourseId);

    List<Node> listByUser(int userId, int count, int offset);

    List<Node> listByIds(List<Integer> ids);
}