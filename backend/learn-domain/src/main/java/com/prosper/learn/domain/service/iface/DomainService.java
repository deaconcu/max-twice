package com.prosper.learn.domain.service.iface;

import com.prosper.learn.domain.entity.Node;
import com.prosper.learn.domain.entity.NodeTable;

public interface DomainService {

    Node getContents(int subcourseId);

    void createNodeList(NodeTable nodeTable);
}
