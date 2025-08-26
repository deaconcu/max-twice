package com.prosper.learn.domain.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.prosper.learn.common.Utils;
import com.prosper.learn.domain.util.Converter;
import com.prosper.learn.dto.NodeDTOV2;
import com.prosper.learn.persistence.dataobject.NodeDO;
import com.prosper.learn.persistence.mapper.NodeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AggregateService {

    private final ContentsService contentsService;
    private final LearningProgressService learningProgressService;
    private final NodeMapper nodeMapper;

    public Utils.Pair<String, Map<Integer, NodeDTOV2>> getToc(int userId, int courseId, boolean create) {

        ArrayNode arrayNode = contentsService.getToc(userId, courseId, create);

        Set<Integer> keys = new HashSet<>();
        Utils.collectKeys(arrayNode, keys);

        List<NodeDO> nodeList = keys.isEmpty() ? new ArrayList<>() : nodeMapper.getByIds(keys.stream().toList());

        // 获取用户完成的节点集合
        Set<Integer> completedNodes = learningProgressService.getUserCompletedNodes(userId);

        // 构建包含完成状态的节点信息
        Map<Integer, NodeDTOV2> nodeInfos = nodeList.stream()
                .collect(Collectors.toMap(
                        NodeDO::getId,
                        node -> Converter.INSTANCE.toNodeDTOV2(node, completedNodes.contains(node.getId()))
                ));


        return new Utils.Pair<>(arrayNode.toString(), nodeInfos);
    }

    public Utils.Pair<String, Map<Integer, NodeDTOV2>> getContents(int userId, int courseId, boolean create) {
        JsonNode jsonNode = contentsService.getContents(userId, courseId, create);

        Set<Integer> keys = new HashSet<>();
        Utils.collectKeys(jsonNode, keys);

        List<NodeDO> nodeList = keys.isEmpty() ? new ArrayList<>() : nodeMapper.getByIds(keys.stream().toList());
        // 获取用户完成的节点集合
        Set<Integer> completedNodes = learningProgressService.getUserCompletedNodes(userId);

        // 构建包含完成状态的节点信息
        Map<Integer, NodeDTOV2> nodeInfos = nodeList.stream()
                .collect(Collectors.toMap(
                        NodeDO::getId,
                        node -> Converter.INSTANCE.toNodeDTOV2(node, completedNodes.contains(node.getId()))
                ));
        return new Utils.Pair<>(jsonNode.toString(), nodeInfos);
    }
}
