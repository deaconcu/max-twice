package com.prosper.learn.domain.service.basic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.prosper.learn.common.Enums;
import com.prosper.learn.common.Utils;
import com.prosper.learn.persistence.dataobject.*;
import com.prosper.learn.persistence.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ContentsService {

    private final CourseMapper courseMapper;
    private final ObjectMapper objectMapper;
    private final NodeMapper nodeMapper;
    private final PostMapper postMapper;
    private final UserCourseTocMapper userCourseTocMapper;
    private final CourseTocMapper courseTocMapper;

    /**
     * 返回用户在某一个课程下的目录
     * @return (目录JSON, NodeMap<id, NodeDTOV2>)
     */
    public ArrayNode getToc(long userId, long courseId, boolean create) {
        CourseDO courseDO = courseMapper.getById(courseId);
        if (courseDO == null) {
            throw new RuntimeException("course is not exist");
        }

        UserCourseTocDO userCourseTocDO = userCourseTocMapper.getByUserAndCourse(userId, courseId);
        String tocStr = "";

        if (userCourseTocDO == null && !create) return null;

        if (userCourseTocDO == null) {
            // create root toc
            ObjectNode s = objectMapper.createObjectNode();
            s.put(Long.toString(courseDO.getRootNode()), objectMapper.createObjectNode());

            tocStr = s.toString();
            String tosHash = Utils.hashSHA(tocStr);

            CourseTocDO courseTocDO = courseTocMapper.get(tosHash);
            if (courseTocDO == null) {
                CourseTocDO newToc = new CourseTocDO();
                newToc.setHash(tosHash);
                newToc.setToc(tocStr);
                courseTocMapper.insert(newToc);
            }

            userCourseTocDO = new UserCourseTocDO();
            userCourseTocDO.setCourseId(courseId);
            userCourseTocDO.setUserId(userId);
            userCourseTocDO.setToc(tosHash);

            userCourseTocMapper.insert(userCourseTocDO);
        } else {
            tocStr = userCourseTocDO.getToc();
        }

        ArrayNode arrayNode = objectMapper.createArrayNode();
        String[] tocHashArr = tocStr.split(",");

        Map<String, CourseTocDO> map = courseTocMapper.getByHashes(tocHashArr);
        for(String tocHash: tocHashArr) {
            try {
                arrayNode.add(objectMapper.readTree(map.get(tocHash).getToc()));
            } catch (IOException e) {
                throw new RuntimeException("Error while reading toc hash");
            }
        }

        return arrayNode;
    }

    /**
     * 返回用户在某个课程下的目录JSON
     */
    public String getToc(long userId, long courseId, int tocIndex) {
        CourseDO courseDO = courseMapper.getById(courseId);
        if (courseDO == null) {
            throw new RuntimeException("course is not exist");
        }

        UserCourseTocDO userCourseTocDO = userCourseTocMapper.getByUserAndCourse(userId, courseId);
        String tocStr = "";
        if (userCourseTocDO == null) return null;

        tocStr = userCourseTocDO.getToc();

        ArrayNode arrayNode = objectMapper.createArrayNode();
        String[] tocHashArr = tocStr.split(",");

        if (tocIndex > tocHashArr.length) throw new RuntimeException("toc index out of index");

        CourseTocDO courseTocDO = courseTocMapper.get(tocHashArr[tocIndex - 1]);
        return courseTocDO.getToc();
    }


    private ArrayNode createDefaultContents(int rootNodeId) {
        ArrayNode arrayNode = objectMapper.createArrayNode();

        ObjectNode s = objectMapper.createObjectNode();
        s.put(Integer.toString(rootNodeId), objectMapper.createObjectNode());

        arrayNode.add(s);
        return arrayNode;
    }

    @Transactional
    public void choose(long userId, String path, long courseId, long postId) {
        // validate
        PostDO postDO = postMapper.get(postId);
        if (postDO == null || postDO.getType() == Enums.PostType.article.value()) return;

        CourseDO courseDO = courseMapper.getById(courseId);
        if (courseDO == null) {
            throw new RuntimeException("course is not exist");
        }

        // 创建childNode
        ObjectNode childNode = objectMapper.createObjectNode();
        Arrays.stream(postDO.getContent().split(",")).forEach(id->childNode.putObject((id)));
        childNode.put("+", postId);

        String[] pathParts = path.split("-", 2);
        int tocIndex = Integer.parseInt(pathParts[0]);

        // get user toc hash
        UserCourseTocDO userCourseTocDO = userCourseTocMapper.getByUserAndCourse(userId, courseId);
        if (userCourseTocDO == null) throw new RuntimeException("user toc is not exist");

        String[] tocHashArr = userCourseTocDO.getToc().split(",");
        if (tocIndex > tocHashArr.length) throw new RuntimeException("toc index out of index");

        CourseTocDO courseTocDO = courseTocMapper.get(tocHashArr[tocIndex - 1]);
        courseTocMapper.incrRef(courseTocDO.getHash(), -1);
        String tocStr = courseTocDO.getToc();

        // 用childNode更新目录
        String toc = updateContents(tocStr, pathParts[1], childNode);

        // 用新的toc更新courseToc表
        String hash = Utils.hashSHA(toc);
        if (courseTocMapper.get(hash) == null) courseTocMapper.insert(new CourseTocDO(hash, toc));
        courseTocMapper.incrRef(hash, 1);

        // 用新的md5更新userCourseToc表
        tocHashArr[tocIndex - 1] = hash;
        userCourseTocDO.setToc(String.join(",", tocHashArr));
        userCourseTocMapper.update(userCourseTocDO);
    }

    public void unchoose(long userId, long courseId, String path) {
        CourseDO courseDO = courseMapper.getById(courseId);
        if (courseDO == null) {
            throw new RuntimeException("course is not exist");
        }

        String[] pathParts = path.split("-", 2);
        int tocIndex = Integer.parseInt(pathParts[0]);

        // get user toc hash
        UserCourseTocDO userCourseTocDO = userCourseTocMapper.getByUserAndCourse(userId, courseId);
        if (userCourseTocDO == null) throw new RuntimeException("user toc is not exist");

        String[] tocHashArr = userCourseTocDO.getToc().split(",");
        if (tocIndex > tocHashArr.length) throw new RuntimeException("toc index out of index");

        CourseTocDO courseTocDO = courseTocMapper.get(tocHashArr[tocIndex - 1]);
        courseTocMapper.incrRef(courseTocDO.getHash(), -1);
        String tocStr = courseTocDO.getToc();

        // 用空节点更新目录
        String toc = updateContents(tocStr, pathParts[1], objectMapper.createObjectNode());

        // 用新的toc更新courseToc表
        String hash = Utils.hashSHA(toc);
        if (courseTocMapper.get(hash) == null) courseTocMapper.insert(new CourseTocDO(hash, toc));
        courseTocMapper.incrRef(hash, 1);

        // 用新的md5更新userCourseToc表
        tocHashArr[tocIndex - 1] = hash;
        userCourseTocDO.setToc(String.join(",", tocHashArr));
        userCourseTocMapper.update(userCourseTocDO);
    }

    public void pin(long userId, long courseId, String path, long postId, boolean add) {
        CourseDO courseDO = courseMapper.getById(courseId);
        if (courseDO == null) {
            throw new RuntimeException("course is not exist");
        }

        String[] pathParts = path.split("-", 2);
        int tocIndex = Integer.parseInt(pathParts[0]);

        // get user toc hash
        UserCourseTocDO userCourseTocDO = userCourseTocMapper.getByUserAndCourse(userId, courseId);
        if (userCourseTocDO == null) throw new RuntimeException("user toc is not exist");

        String[] tocHashArr = userCourseTocDO.getToc().split(",");
        if (tocIndex > tocHashArr.length) throw new RuntimeException("toc index out of index");

        CourseTocDO courseTocDO = courseTocMapper.get(tocHashArr[tocIndex - 1]);
        courseTocMapper.incrRef(courseTocDO.getHash(), -1);
        String tocStr = courseTocDO.getToc();

        // 用空节点更新目录
        String toc = insertContents(tocStr, pathParts[1], postId, add);

        // 用新的toc更新courseToc表
        String hash = Utils.hashSHA(toc);
        if (courseTocMapper.get(hash) == null) courseTocMapper.insert(new CourseTocDO(hash, toc));
        courseTocMapper.incrRef(hash, 1);

        // 用新的md5更新userCourseToc表
        tocHashArr[tocIndex - 1] = hash;
        userCourseTocDO.setToc(String.join(",", tocHashArr));
        userCourseTocMapper.update(userCourseTocDO);
    }

    /**
     * 更新某个课程的某个路径下的目录节点，并返回字符串
     * @param contents 用户目录
     */
    private String updateContents(String contents, String path, ObjectNode newNode) {
        try {
            ObjectNode rootNode = (ObjectNode)objectMapper.readTree(contents);
            String[] pathParts = path.split("-");

            ObjectNode node = rootNode;
            for (int i = 0; i < pathParts.length - 1; i++) {
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
    private String insertContents(String contents, String path, long value, boolean add) {
        try {
            ObjectNode rootNode = (ObjectNode)objectMapper.readTree(contents);
            ObjectNode node = rootNode;

            String[] pathParts = path.split("-");

            for (int i = 0; i <= pathParts.length - 1; i++) {
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
