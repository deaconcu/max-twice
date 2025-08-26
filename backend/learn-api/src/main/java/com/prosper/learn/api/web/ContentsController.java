package com.prosper.learn.api.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.prosper.learn.api.client.ContentsClient;
import com.prosper.learn.domain.service.AggregateService;
import com.prosper.learn.dto.CourseTocDTO;
import com.prosper.learn.dto.NodeDTOV2;
import com.prosper.learn.dto.Response;
import com.prosper.learn.common.Enums;
import com.prosper.learn.common.Utils;
import com.prosper.learn.domain.service.ContentsService;
import com.prosper.learn.persistence.dataobject.*;
import com.prosper.learn.persistence.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
//@SaCheckLogin
@Slf4j
@RequiredArgsConstructor
public class ContentsController implements ContentsClient {

    private final ContentsMapper contentsMapper;
    private final ObjectMapper objectMapper;
    private final NodeMapper nodeMapper;
    private final PostMapper postMapper;
    private final CourseMapper courseMapper;
    private final ContentsService contentsService;
    private final AggregateService aggregateService;

    @Override
    public Response<CourseTocDTO> get(int userId, int courseId, boolean create) {
        Utils.Pair<String, Map<Integer, NodeDTOV2>> pair = aggregateService.getContents(userId, courseId, create);
        return new Response<>(new CourseTocDTO(pair.left(), pair.right()));
    }

    @Transactional
    public Response<Object> choose(int userId, String path, int courseId, int postingId) {
        PostDO postDO = postMapper.get(postingId);
        if (postDO == null || postDO.getType() == Enums.PostType.article.value) return Response.badRequest;

        // 创建childNode
        ObjectNode childNode = objectMapper.createObjectNode();
        Arrays.stream(postDO.getContent().split(",")).forEach(id->childNode.putObject((id)));
        childNode.put("+", postingId);

        // 用childNode更新目录
        ContentsDO contentsDO = contentsMapper.getByUser(userId);
        String contents = updateContents(contentsDO.getContents(), courseId, path, childNode);
        contentsDO.setContents(contents);
        contentsMapper.update(contentsDO);

        return Response.success;
    }

    @Override
    public Response<Object> unchoose(int userId, int courseId, String path) {
        ContentsDO contentsDO = contentsMapper.getByUser(userId);
        String contents = updateContents(contentsDO.getContents(), courseId, path, objectMapper.createObjectNode());
        contentsDO.setContents(contents);
        contentsMapper.update(contentsDO);
        return Response.success;
    }

    @Override
    public Response<Object> pin(int userId, int courseId, String path, int postingId) {
        ContentsDO contentsDO = contentsMapper.getByUser(userId);
        String contents = insertContents(contentsDO.getContents(), courseId, path, postingId, true);
        contentsDO.setContents(contents);
        contentsMapper.update(contentsDO);
        return Response.success;
    }

    @Override
    public Response<Object> unpin(int userId, int courseId, String path, int postingId) {
        ContentsDO contentsDO = contentsMapper.getByUser(userId);
        String contents = insertContents(contentsDO.getContents(), courseId, path, postingId, false);
        contentsDO.setContents(contents);
        contentsMapper.update(contentsDO);
        return Response.success;
    }

    /**
     * 更新某个课程的某个路径下的目录节点，并返回字符串
     * @param contents 用户目录
     */
    private String updateContents(String contents, int courseId, String path, ObjectNode newNode) {
        try {
            ObjectNode rootNode = (ObjectNode)objectMapper.readTree(contents);
            ObjectNode node = rootNode;

            String[] pathParts = path.split("-");
            String courseIdStr = Integer.toString(courseId);
            // 不存在课程对应的目录
            if (!node.has(courseIdStr)) throw new RuntimeException();

            ArrayNode arrayNode = (ArrayNode) node.get(courseIdStr);
            int index = Integer.parseInt(pathParts[0]);
            // 不存在给定index的目录
            if (index > arrayNode.size()) throw new RuntimeException();

            node = (ObjectNode) arrayNode.get(index - 1);
            for (int i = 1; i < pathParts.length - 1; i++) {
                String part = pathParts[i];
                if (!node.has(part)) {
                    node.set(part, objectMapper.createObjectNode()); // 如果路径不存在，创建
                }
                node = (ObjectNode) node.get(part);
            }

            // 设置或替换目标节点
            String finalPart = pathParts[pathParts.length - 1];
            // 设置之前置顶的帖子
            JsonNode finalNode = node.get(finalPart);
            if (finalNode.has("^")) {
                newNode.put("^", node.get(finalPart).get("^"));
            }
            node.set(finalPart, newNode);
            return objectMapper.writeValueAsString(rootNode);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 设置置顶
     */
    private String insertContents(String contents, int courseId, String path, int value, boolean add) {
        try {
            ObjectNode rootNode = (ObjectNode)objectMapper.readTree(contents);
            ObjectNode node = rootNode;

            String[] pathParts = path.split("-");
            String courseIdStr = Integer.toString(courseId);
            // 不存在课程对应的目录
            if (!node.has(courseIdStr)) throw new RuntimeException();

            ArrayNode arrayNode = (ArrayNode) node.get(courseIdStr);
            int index = Integer.parseInt(pathParts[0]);
            // 不存在给定index的目录
            if (index > arrayNode.size()) throw new RuntimeException();

            node = (ObjectNode) arrayNode.get(index - 1);
            for (int i = 1; i <= pathParts.length - 1; i++) {
                String part = pathParts[i];
                if (!node.has(part)) {
                    node.set(part, objectMapper.createObjectNode()); // 如果路径不存在，创建
                }
                node = (ObjectNode) node.get(part);
            }

            // 设置或替换目标节点
            ArrayNode pinedArray = ((ArrayNode)node.get("^"));
            if (pinedArray == null) {
                pinedArray = objectMapper.createArrayNode();
                node.put("^", pinedArray);
            }
            if (add) {
                boolean exist = false;
                for (int i = 0; i < pinedArray.size(); i++) {
                    if (pinedArray.get(i).asInt() == value) {
                        exist = true;
                    }
                }
                if (pinedArray.size() >= 10) throw new RuntimeException();
                if (!exist) pinedArray.add(value);
            } else {
                for (int i = 0; i < pinedArray.size(); i++) {
                    if (pinedArray.get(i).asInt() == value) {
                        pinedArray.remove(i);
                        break;
                    }
                }
            }
            return objectMapper.writeValueAsString(rootNode);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }


}
