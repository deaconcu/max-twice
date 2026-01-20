package com.prosper.learn.content.post;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prosper.learn.content.course.CourseDataService;
import com.prosper.learn.content.node.NodeDO;
import com.prosper.learn.content.node.NodeDataService;
import com.prosper.learn.shared.domain.Enums.ContentState;
import com.prosper.learn.shared.domain.Enums.PostType;
import com.prosper.learn.shared.domain.event.content.lifecycle.NodeCreatedEvent;
import com.prosper.learn.shared.domain.exception.StatusCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 帖子领域服务
 * 只依赖本领域（content）模块，处理核心业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostDomainService {

    private final PostDataService postDataService;
    private final NodeDataService nodeDataService;
    private final CourseDataService courseDataService;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    // ========== Query 方法 ==========

    /**
     * 根据ID获取帖子（包含 idToName 处理）
     */
    public PostDO getWithIdToName(long id) {
        PostDO post = validateAndGet(id);
        processIdToName(post);
        return post;
    }

    /**
     * 批量获取帖子（包含 idToName 处理）
     */
    public List<PostDO> getByIdsWithIdToName(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<PostDO> posts = postDataService.getByIds(ids);
        posts.forEach(this::processIdToName);
        return posts;
    }

    /**
     * 根据节点和创建者获取帖子列表
     */
    public List<PostDO> getListByNodeAndCreator(Long nodeId, Long creatorId, Long lastId, Byte state, int limit) {
        List<PostDO> posts = postDataService.getListByNodeAndCreator(nodeId, creatorId, lastId, state, limit);
        posts.forEach(this::processIdToName);
        return posts;
    }

    /**
     * 获取用户的帖子列表
     */
    public List<PostDO> getUserPosts(Long userId, Integer type, Long lastId, Byte state, int count) {
        List<PostDO> posts = postDataService.getPostsByUser(userId, type, lastId, state, count);
        posts.forEach(this::processIdToName);
        return posts;
    }

    /**
     * 获取节点下的帖子列表
     */
    public List<PostDO> getNodePostsList(Long nodeId, int count, Byte state) {
        List<PostDO> posts = postDataService.getListByNode(nodeId, count, state);
        posts.forEach(this::processIdToName);
        return posts;
    }

    /**
     * 根据节点和分数获取帖子列表
     */
    public List<PostDO> getListByNodeAndScore(long nodeId, int limit, Byte state) {
        List<PostDO> posts = postDataService.getListByNodeAndScore(nodeId, limit, state);
        posts.forEach(this::processIdToName);
        return posts;
    }

    /**
     * 根据节点和分数获取帖子列表（分页版本）
     */
    public List<PostDO> getListByNodeAndScorePaginated(long nodeId, double lastScore, long lastId, int limit, Byte state) {
        List<PostDO> posts = postDataService.getListByNodeAndScoreAndPaginated(nodeId, lastScore, lastId, limit, state);
        posts.forEach(this::processIdToName);
        return posts;
    }

    /**
     * 根据状态获取帖子列表
     */
    public List<PostDO> getListByState(Byte state, int limit) {
        List<PostDO> posts = postDataService.getListByState(state, limit);
        posts.forEach(this::processIdToName);
        return posts;
    }

    /**
     * 根据状态获取帖子列表（分页）
     */
    public List<PostDO> getListByState(Byte state, Long lastId, Integer limit) {
        List<PostDO> posts = postDataService.getListByState(state, lastId, limit);
        posts.forEach(this::processIdToName);
        return posts;
    }

    /**
     * 根据ID列表或节点查询帖子
     */
    public List<PostDO> getPostsByIdsOrNode(List<Long> ids, Long nodeId, Double lastScore, Long lastPostingId, int count, Byte state) {
        List<PostDO> posts = null;
        if (ids != null && !ids.isEmpty()) {
            posts = postDataService.getByIds(ids);
        } else if (nodeId != null && nodeId > 0) {
            // 首次请求（无分页参数）：使用不带分页条件的查询
            if (lastScore == null || lastPostingId == null) {
                posts = postDataService.getListByNodeAndScore(nodeId, count, state);
            } else {
                // 后续请求（有分页参数）：使用带分页条件的查询
                posts = postDataService.getListByNodeAndScoreAndPaginated(nodeId, lastScore, lastPostingId, count, state);
            }
        }

        if (posts == null) return List.of();
        posts.forEach(this::processIdToName);
        return posts;
    }

    // ========== Command 方法 ==========

    /**
     * 创建普通帖子
     */
    @Transactional
    public Long createArticlePost(long userId, long nodeId, int type, String content, ContentState state) {
        // 验证节点是否存在
        NodeDO nodeDO = nodeDataService.validateAndGet(nodeId);

        // 验证节点状态是否为已发布
        if (nodeDO.getState() != ContentState.PUBLISHED.value()) {
            throw StatusCode.NODE_STATE_INVALID.exception();
        }

        PostDO postDO = new PostDO();
        postDO.setNodeId(nodeId);
        postDO.setCreatorId(userId);
        postDO.setType(type);
        postDO.setContent(content);
        postDO.setState(state.value());

        postDataService.insert(postDO);
        log.info("Created article post: {} in node: {} by user: {}", postDO.getId(), nodeId, userId);

        return postDO.getId();
    }

    /**
     * 创建目录型帖子（contents类型）
     */
    @Transactional
    public Long createContentsPost(long userId, long nodeId, String jsonContent, ContentState state) {
        NodeDO nodeDO = nodeDataService.validateAndGet(nodeId);

        // 验证节点状态是否为已发布
        if (nodeDO.getState() != ContentState.PUBLISHED.value()) {
            throw StatusCode.NODE_STATE_INVALID.exception();
        }

        log.info("Creating contents post with content: {}", jsonContent);
        List<ChapterInfo> chapterInfos = parseJsonToChapterInfoList(jsonContent);

        if (chapterInfos.size() < 2) {
            throw StatusCode.INVALID_PARAMETER.exception("至少需要2个子目录");
        }

        String[] ids = new String[chapterInfos.size()];
        Long courseId = nodeDO.getCourseId();

        for (int i = 0; i < chapterInfos.size(); i++) {
            ChapterInfo chapterInfo = chapterInfos.get(i);

            // 如果提供了节点ID，直接使用（用于选择已有节点）
            if (chapterInfo.id() != null) {
                NodeDO existingNode = nodeDataService.validateAndGet(chapterInfo.id());

                // 验证节点状态为已发布
                if (existingNode.getState() != ContentState.PUBLISHED.value()) {
                    throw StatusCode.NODE_STATE_INVALID.exception();
                }

                ids[i] = Long.toString(existingNode.getId());
                log.info("Using existing node: {} (id: {}) from course: {}", existingNode.getName(), existingNode.getId(), existingNode.getCourseId());
            } else {
                // 没有提供ID，按节点名称创建
                String nodeName = chapterInfo.name();

                // 检查本课程是否已存在同名节点
                NodeDO existingNode = nodeDataService.getByCourseAndName(courseId, nodeName);

                if (existingNode != null) {
                    // 存在同名节点，抛错让用户手动选择
                    throw StatusCode.INVALID_PARAMETER.exception(
                            String.format("课程中已存在名为'%s'的节点(ID:%d)，请使用「选择现有」功能手动选择该节点",
                                    nodeName, existingNode.getId()));
                }

                // 创建新节点
                NodeDO newNode = new NodeDO();
                newNode.setName(nodeName);
                newNode.setDescription(chapterInfo.description());
                newNode.setCourseId(courseId);
                newNode.setCreatorId(userId);
                newNode.setState(ContentState.PUBLISHED.value());
                nodeDataService.insert(newNode);
                ids[i] = Long.toString(newNode.getId());
                log.info("Created new node: {} (id: {}) in course: {}", nodeName, newNode.getId(), courseId);

                // 发布节点创建事件
                eventPublisher.publishEvent(new NodeCreatedEvent(
                        newNode.getId(),
                        newNode.getName(),
                        newNode.getDescription()
                ));
            }
        }

        // 创建 PostDO
        PostDO postDO = new PostDO();
        postDO.setNodeId(nodeId);
        postDO.setCreatorId(userId);
        postDO.setType(PostType.index.value());
        postDO.setContent(String.join(",", ids));
        postDO.setState(state.value());
        postDataService.insert(postDO);

        log.info("Created contents post: {} in node: {} by user: {}", postDO.getId(), nodeId, userId);
        return postDO.getId();
    }

    /**
     * 更新帖子内容
     */
    @Transactional
    public void updatePost(long id, String content) {
        PostDO postDO = validateAndGet(id);

        if (postDO.getType() == PostType.index.value()) {
            throw StatusCode.INVALID_PARAMETER.exception("目录类型帖子不支持修改");
        }

        postDO.setContent(content);
        postDataService.update(postDO);

        log.info("Updated post: {}", id);
    }

    /**
     * 软删除帖子
     */
    @Transactional
    public void softDelete(long id) {
        postDataService.validateAndGet(id);

        int result = postDataService.softDelete(id);
        if (result == 0) {
            throw StatusCode.POST_NOT_FOUND.exception();
        }

        log.info("Soft deleted post: {}", id);
    }

    /**
     * 更新帖子状态
     */
    @Transactional
    public void updateState(long id, ContentState state, String reason) {
        PostDO postDO = validateAndGet(id);
        postDO.setState(state.value());
        postDO.setReason(reason);
        postDataService.update(postDO);

        log.info("Updated post {} state to: {}", id, state);
    }

    /**
     * 批准帖子
     */
    @Transactional
    public void approve(long id) {
        PostDO postDO = validateAndGet(id);
        postDO.setState(ContentState.PUBLISHED.value());
        postDO.setReason(null);
        postDataService.update(postDO);

        log.info("Approved post: {}", id);
    }

    /**
     * 拒绝帖子
     */
    @Transactional
    public void reject(long id, String reason) {
        postDataService.validateAndGet(id);
        postDataService.reject(id, reason);

        log.info("Rejected post: {} with reason: {}", id, reason);
    }

    /**
     * 封禁帖子
     */
    @Transactional
    public void ban(long id, String reason) {
        postDataService.validateAndGet(id);
        postDataService.ban(id, reason);

        log.info("Banned post: {} with reason: {}", id, reason);
    }

    // ========== 验证方法 ==========

    /**
     * 验证并获取帖子
     */
    public PostDO validateAndGet(long postId) {
        return postDataService.validateAndGet(postId);
    }

    // ========== Private 辅助方法 ==========

    /**
     * 章节信息记录类
     */
    private record ChapterInfo(Long id, String name, String description) {}

    /**
     * 解析JSON到章节信息列表
     * 支持两种格式：
     * 1. {"name": "节点名", "description": "描述"} - 创建新节点
     * 2. {"id": 123, "name": "节点名", "description": "描述"} - 使用已有节点
     */
    private List<ChapterInfo> parseJsonToChapterInfoList(String jsonContent) {
        try {
            List<Map<String, Object>> chapterMaps = objectMapper.readValue(jsonContent, new TypeReference<>() {});
            return chapterMaps.stream().map(chapterMap -> {
                Long id = null;
                String name = null;
                String description = null;

                // 解析 id（可选）
                if (chapterMap.containsKey("id")) {
                    Object idObj = chapterMap.get("id");
                    if (idObj instanceof Number) {
                        id = ((Number) idObj).longValue();
                    }
                }

                // 解析 name（必需）
                if (chapterMap.containsKey("name")) {
                    name = String.valueOf(chapterMap.get("name"));
                } else if (id == null && chapterMap.size() == 1) {
                    // 兼容旧格式：{"节点名": "描述"}
                    Map.Entry<String, Object> entry = chapterMap.entrySet().iterator().next();
                    name = entry.getKey();
                    description = entry.getValue() != null ? String.valueOf(entry.getValue()) : "";
                    if (description.trim().isEmpty()) {
                        throw StatusCode.INVALID_PARAMETER.exception(String.format("节点'%s'的描述不能为空", name));
                    }
                    return new ChapterInfo(null, name, description);
                } else {
                    throw StatusCode.INVALID_PARAMETER.exception("章节必须包含 name 字段");
                }

                // 解析 description（必需）
                if (chapterMap.containsKey("description")) {
                    description = String.valueOf(chapterMap.get("description"));
                    if (description == null || description.trim().isEmpty()) {
                        throw StatusCode.INVALID_PARAMETER.exception(String.format("节点'%s'的描述不能为空", name));
                    }
                } else {
                    throw StatusCode.INVALID_PARAMETER.exception(String.format("节点'%s'缺少 description 字段", name));
                }

                return new ChapterInfo(id, name, description);
            }).collect(Collectors.toList());
        } catch (JsonProcessingException e) {
            throw StatusCode.INVALID_PARAMETER.exception("目录内容格式错误，请使用正确的JSON格式");
        }
    }

    /**
     * 将目录型帖子的内容ID转换为节点信息（JSON格式）
     * 公开方法，允许外部调用
     */
    public void processIdToName(PostDO post) {
        if (post == null || post.getType() == PostType.article.value() ||
                post.getContent() == null || post.getContent().isEmpty()) {
            return;
        }

        try {
            List<Long> ids = Arrays.stream(post.getContent().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .toList();

            if (!ids.isEmpty()) {
                List<NodeDO> nodeList = nodeDataService.getByIds(ids);

                List<Map<String, Object>> nodeInfoList = nodeList.stream()
                        .map(node -> {
                            Map<String, Object> nodeInfo = new HashMap<>();
                            nodeInfo.put("id", node.getId());
                            nodeInfo.put("name", node.getName());
                            nodeInfo.put("description", node.getDescription() != null ? node.getDescription() : "");
                            return nodeInfo;
                        })
                        .collect(Collectors.toList());

                String jsonContent = objectMapper.writeValueAsString(nodeInfoList);
                post.setContent(jsonContent);
            }
        } catch (NumberFormatException e) {
            log.warn("Failed to parse content IDs for post {}: {}", post.getId(), post.getContent(), e);
        } catch (Exception e) {
            log.error("Failed to convert node info to JSON for post {}", post.getId(), e);
        }
    }
}
