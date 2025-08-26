package com.prosper.learn.domain.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.prosper.learn.common.Enums;
import com.prosper.learn.common.Utils;
import com.prosper.learn.dto.NodeDTOV2;
import com.prosper.learn.domain.util.Converter;
import com.prosper.learn.persistence.dataobject.*;
import com.prosper.learn.persistence.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContentsService {

    private final ContentsMapper contentsMapper;
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
    public ArrayNode getToc(int userId, int courseId, boolean create) {
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
            s.put(Integer.toString(courseDO.getRootNode()), objectMapper.createObjectNode());

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
    public String getToc(int userId, int courseId, int tocIndex) {
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


    public JsonNode getContents(int userId, int courseId, boolean create) {
        CourseDO courseDO = courseMapper.getById(courseId);
        if (courseDO == null) {
            throw new RuntimeException("course is not exist");
        }

        ContentsDO contentsDO = contentsMapper.getByUser(userId);
        if (contentsDO == null) {

            contentsDO = new ContentsDO();
            contentsDO.setUserId(userId);

            ObjectNode node = objectMapper.createObjectNode();

            node.set(Integer.toString(courseId), createDefaultContents(courseDO.getRootNode()));
            contentsDO.setContents(node.toString());

            contentsDO.setCTime(Utils.getLocalDateTime());
            contentsDO.setUTime(Utils.getLocalDateTime());

            contentsMapper.insert(contentsDO);
        }

        ObjectNode rootNode = null;
        try {
            rootNode = (ObjectNode) objectMapper.readTree(contentsDO.getContents());
            JsonNode jsonNode = rootNode.get(Integer.toString(courseId));

            if (jsonNode == null) {
                jsonNode = createDefaultContents(courseDO.getRootNode());
                rootNode.set(Integer.toString(courseId), jsonNode);
                contentsDO.setContents(rootNode.toString());
                contentsMapper.update(contentsDO);
            }

            return jsonNode;
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private ArrayNode createDefaultContents(int rootNodeId) {
        ArrayNode arrayNode = objectMapper.createArrayNode();

        ObjectNode s = objectMapper.createObjectNode();
        s.put(Integer.toString(rootNodeId), objectMapper.createObjectNode());

        arrayNode.add(s);
        return arrayNode;
    }

    @Transactional
    public void choose(int userId, String path, int courseId, int postId) {
        // validate
        PostDO postDO = postMapper.get(postId);
        if (postDO == null || postDO.getType() == Enums.PostType.article.value) return;

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

    public void unchoose(int userId, int courseId, String path) {
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

    public void pin(int userId, int courseId, String path, int postId, boolean add) {
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

    /*
    @Transactional
    public void choose(int userId, String path, int courseId, int postId) {
        PostDO postDO = postMapper.getById(postId);
        if (postDO == null || postDO.getType() == Enums.PostingType.article.value) return;

        // 创建childNode
        ObjectNode childNode = objectMapper.createObjectNode();
        Arrays.stream(postDO.getContent().split(",")).forEach(id->childNode.putObject((id)));
        childNode.put("+", postId);

        // 用childNode更新目录
        ContentsDO contentsDO = contentsMapper.getByUser(userId);
        String contents = updateContents(contentsDO.getContents(), courseId, path, childNode);
        contentsDO.setContents(contents);
        contentsMapper.update(contentsDO);
    }

    public void unchoose(int userId, int courseId, String path) {
        ContentsDO contentsDO = contentsMapper.getByUser(userId);
        String contents = updateContents(contentsDO.getContents(), courseId, path, objectMapper.createObjectNode());
        contentsDO.setContents(contents);
        contentsMapper.update(contentsDO);
    }

    public void pin(int userId, int courseId, String path, int postingId) {
        ContentsDO contentsDO = contentsMapper.getByUser(userId);
        String contents = insertContents(contentsDO.getContents(), courseId, path, postingId, true);
        contentsDO.setContents(contents);
        contentsMapper.update(contentsDO);
    }

    public void unpin(int userId, int courseId, String path, int postingId) {
        ContentsDO contentsDO = contentsMapper.getByUser(userId);
        String contents = insertContents(contentsDO.getContents(), courseId, path, postingId, false);
        contentsDO.setContents(contents);
        contentsMapper.update(contentsDO);
    }
     */

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
    private String insertContents(String contents, String path, int value, boolean add) {
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

    public void updateContentsList(int courseId, int userId, String list) {
        int[] arr = Arrays.stream(list.split(",")).mapToInt(Integer::parseInt).toArray();

        try {

            ContentsDO contentsDO = contentsMapper.getByUser(userId);
            String contents = contentsDO.getContents();
            ObjectNode rootNode = (ObjectNode)objectMapper.readTree(contents);

            CourseDO courseDO = courseMapper.getById(courseId);
            if (courseDO == null) throw new RuntimeException();

            String courseIdStr = Integer.toString(courseId);
            // 不存在课程对应的目录
            if (!rootNode.has(courseIdStr)) throw new RuntimeException();

            ObjectNode objectNode = objectMapper.createObjectNode();
            objectNode.put(Integer.toString(courseId), objectMapper.createObjectNode());

            ArrayNode arrayNode = (ArrayNode) rootNode.get(courseIdStr);

            ArrayNode newArrayNode = objectMapper.createArrayNode();
            for (int index: arr) {
                index = Math.abs(index);
                if (index == 0) {
                    newArrayNode.add(objectNode);
                } else if (arrayNode.has(index - 1)) {
                    newArrayNode.add(arrayNode.get(index - 1));
                }
            }

            rootNode.set(courseIdStr, newArrayNode);
            contents = objectMapper.writeValueAsString(rootNode);
            contentsDO.setContents(contents);
            contentsMapper.update(contentsDO);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
