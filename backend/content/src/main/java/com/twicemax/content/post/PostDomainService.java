package com.twicemax.content.post;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.twicemax.content.course.CourseDataService;
import com.twicemax.content.node.NodeDO;
import com.twicemax.content.node.NodeDataService;
import com.twicemax.shared.domain.Enums.ContentState;
import com.twicemax.shared.domain.Enums.PostType;
import com.twicemax.shared.domain.exception.StatusCode;
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
     * 获取用户的帖子列表
     */
    public List<PostDO> getUserPosts(Long userId, String type, Long lastId, Byte state, int count) {
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
     * 根据状态获取帖子列表
     */
    public List<PostDO> listByState(Byte state, Long lastId, int limit) {
        List<PostDO> posts = postDataService.listByState(state, lastId, limit);
        posts.forEach(this::processIdToName);
        return posts;
    }

    /**
     * Admin - 高级筛选帖子列表（不含 state）
     */
    public List<PostDO> listByFilter(Long nodeId, Long creatorId, Long lastId, int limit) {
        List<PostDO> posts = postDataService.listByFilter(nodeId, creatorId, lastId, limit);
        posts.forEach(this::processIdToName);
        return posts;
    }

    /**
     * 根据节点获取帖子列表（按分数排序，支持分页）
     *
     * @param nodeId 节点ID
     * @param lastScore 上一页最后一条的分数（首页传null）
     * @param lastPostingId 上一页最后一条的ID（首页传null）
     * @param count 查询数量
     * @param state 帖子状态
     * @return 帖子列表
     */
    public List<PostDO> getNodePostsByScore(Long nodeId, Double lastScore, Long lastPostingId, int count, Byte state) {
        if (nodeId == null || nodeId <= 0) {
            return List.of();
        }

        List<PostDO> posts;
        // 首次请求（无分页参数）：使用不带分页条件的查询
        if (lastScore == null || lastPostingId == null) {
            posts = postDataService.getListByNodeAndScore(nodeId, count, state);
        } else {
            // 后续请求（有分页参数）：使用带分页条件的查询
            posts = postDataService.getListByNodeAndScoreAndPaginated(nodeId, lastScore, lastPostingId, count, state);
        }

        posts.forEach(this::processIdToName);
        return posts;
    }

    // ========== Command 方法 ==========

    /**
     * 创建普通帖子
     */
    @Transactional
    public Long createArticlePost(long userId, long nodeId, String type, String content, ContentState state) {
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
        log.info("帖子创建成功: postId={}，nodeId={}，userId={}", postDO.getId(), nodeId, userId);

        return postDO.getId();
    }

    /**
     * 创建目录型帖子（index类型）
     * 创建时不创建节点，只保存JSON格式的节点信息
     * 节点将在审批通过时创建
     */
    @Transactional
    public Long createIndexPost(long userId, long nodeId, String jsonContent, ContentState state) {
        NodeDO nodeDO = nodeDataService.validateAndGet(nodeId);

        // 验证节点状态是否为已发布
        if (nodeDO.getState() != ContentState.PUBLISHED.value()) {
            throw StatusCode.NODE_STATE_INVALID.exception();
        }

        log.info("创建目录帖子，内容: {}", jsonContent);

        // 验证JSON格式并解析
        List<ChapterInfo> chapterInfos = parseJsonToChapterInfoList(jsonContent);

        if (chapterInfos.size() < 2) {
            throw StatusCode.INVALID_PARAMETER.exception("至少需要2个子目录");
        }

        Long courseId = nodeDO.getCourseId();

        // 验证引用的已有节点是否存在且已发布
        for (ChapterInfo chapterInfo : chapterInfos) {
            if (chapterInfo.id() != null) {
                NodeDO existingNode = nodeDataService.validateAndGet(chapterInfo.id());

                // 验证节点状态为已发布
                if (existingNode.getState() != ContentState.PUBLISHED.value()) {
                    throw StatusCode.NODE_STATE_INVALID.exception("引用的节点不是正常状态");
                }
            } else {
                // 验证新节点的名称和描述不为空
                if (chapterInfo.name() == null || chapterInfo.name().trim().isEmpty()) {
                    throw StatusCode.INVALID_PARAMETER.exception("节点名称不能为空");
                }
                if (chapterInfo.description() == null || chapterInfo.description().trim().isEmpty()) {
                    throw StatusCode.INVALID_PARAMETER.exception("节点描述不能为空");
                }
            }
        }

        // 创建 PostDO，直接保存JSON内容
        // 节点将在审批时创建
        PostDO postDO = new PostDO();
        postDO.setNodeId(nodeId);
        postDO.setCreatorId(userId);
        postDO.setType(PostType.INDEX.value());
        postDO.setContent(jsonContent);  // 保存原始JSON
        postDO.setState(state.value());
        postDataService.insert(postDO);

        log.info("目录帖子创建成功: postId={}，nodeId={}，userId={}", postDO.getId(), nodeId, userId);
        return postDO.getId();
    }

    /**
     * 更新帖子内容（根据类型自动分发）
     */
    @Transactional
    public void updatePost(long id, String content) {
        PostDO postDO = validateAndGet(id);

        if (PostType.INDEX.value().equals(postDO.getType())) {
            updateIndexPost(postDO, content);
        } else {
            updateArticlePost(postDO, content);
        }
    }

    /**
     * 更新文章类型帖子
     */
    private void updateArticlePost(PostDO postDO, String content) {
        postDO.setContent(content);
        postDataService.update(postDO);
        log.info("文章帖子更新成功: postId={}", postDO.getId());
    }

    /**
     * 更新index类型帖子
     * 只允许在DRAFT状态下修改，且需要检查节点重名
     */
    private void updateIndexPost(PostDO postDO, String jsonContent) {
        // 只允许草稿状态修改
        if (postDO.getState() != ContentState.DRAFT.value()) {
            throw StatusCode.INVALID_PARAMETER.exception("目录类型帖子只能在草稿状态下修改");
        }

        // 验证JSON格式
        List<ChapterInfo> chapterInfos = parseJsonToChapterInfoList(jsonContent);
        if (chapterInfos.size() < 2) {
            throw StatusCode.INVALID_PARAMETER.exception("至少需要2个子目录");
        }

        NodeDO parentNode = nodeDataService.validateAndGet(postDO.getNodeId());
        Long courseId = parentNode.getCourseId();

        // 检查新节点是否重名
        for (ChapterInfo chapterInfo : chapterInfos) {
            if (chapterInfo.id() == null) {
                // 新节点，检查课程内是否有同名的PUBLISHED节点
                String nodeName = chapterInfo.name();
                if (nodeName == null || nodeName.trim().isEmpty()) {
                    throw StatusCode.INVALID_PARAMETER.exception("节点名称不能为空");
                }
                if (chapterInfo.description() == null || chapterInfo.description().trim().isEmpty()) {
                    throw StatusCode.INVALID_PARAMETER.exception("节点描述不能为空");
                }

                NodeDO existingNode = nodeDataService.getByCourseAndName(courseId, nodeName);
                if (existingNode != null && existingNode.getState() == ContentState.PUBLISHED.value()) {
                    throw StatusCode.INVALID_PARAMETER.exception(
                        String.format("课程中已存在名为'%s'的节点(ID:%d)，请使用已有节点或修改节点名称",
                            nodeName, existingNode.getId())
                    );
                }
            } else {
                // 引用已有节点，验证存在且已发布
                NodeDO existingNode = nodeDataService.validateAndGet(chapterInfo.id());
                if (existingNode.getState() != ContentState.PUBLISHED.value()) {
                    throw StatusCode.NODE_STATE_INVALID.exception("引用的节点不是正常状态");
                }
            }
        }

        postDO.setContent(jsonContent);
        postDataService.update(postDO);
        log.info("目录帖子更新成功: postId={}", postDO.getId());
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

        log.info("帖子软删除成功: postId={}", id);
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

        log.info("帖子状态更新: postId={}，state={}", id, state);
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

        log.info("帖子审核通过: postId={}", id);
    }

    /**
     * 拒绝帖子
     */
    @Transactional
    public void reject(long id, String reason) {
        postDataService.validateAndGet(id);
        postDataService.reject(id, reason);

        log.info("帖子审核拒绝: postId={}，reason={}", id, reason);
    }

    /**
     * 封禁帖子
     */
    @Transactional
    public void ban(long id, String reason) {
        postDataService.validateAndGet(id);
        postDataService.ban(id, reason);

        log.info("帖子封禁: postId={}，reason={}", id, reason);
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
        if (post == null || PostType.ARTICLE.value().equals(post.getType()) ||
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
                            nodeInfo.put("state", node.getState());
                            // 如果节点被封禁，替换名称和描述
                            if (node.getState() != null && node.getState() == ContentState.BANNED.value()) {
                                nodeInfo.put("name", "目录节点已被屏蔽");
                                nodeInfo.put("description", "");
                            } else {
                                nodeInfo.put("name", node.getName());
                                nodeInfo.put("description", node.getDescription() != null ? node.getDescription() : "");
                            }
                            return nodeInfo;
                        })
                        .collect(Collectors.toList());

                String jsonContent = objectMapper.writeValueAsString(nodeInfoList);
                post.setContent(jsonContent);
            }
        } catch (NumberFormatException e) {
            log.warn("帖子内容ID解析失败: postId={}，content={}", post.getId(), post.getContent(), e);
        } catch (Exception e) {
            log.error("帖子节点信息转JSON失败: postId={}", post.getId(), e);
        }
    }
}
