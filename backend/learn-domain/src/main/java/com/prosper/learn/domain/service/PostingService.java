package com.prosper.learn.domain.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prosper.learn.common.Enums;
import com.prosper.learn.common.Utils;
import com.prosper.learn.domain.util.Converter;
import com.prosper.learn.dto.NodeDTO;
import com.prosper.learn.dto.PostDTO;
import com.prosper.learn.dto.PostDTOV2;
import com.prosper.learn.dto.UserDTOV1;
import com.prosper.learn.persistence.dataobject.*;
import com.prosper.learn.persistence.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PostingService {

    @Autowired
    private NodeMapper nodeMapper;
    @Autowired
    private PostMapper postMapper;
    @Autowired
    private CourseMapper courseMapper;
    @Autowired
    private UpvoteMapper upvoteMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private DailyStatsService dailyStatsService;

    public PostDO get(long id) {
        PostDO posting = postMapper.get(id);
        idToName(posting);
        return posting;
    }

    public List<PostDO> getList(List<Long> ids) {
        if (ids == null || ids.size() == 0) return new ArrayList<>();
        List<PostDO> postings = postMapper.getByIds(ids);
        postings.forEach(postingDO -> idToName(postingDO));
        return postings;
    }

    public List<PostDO> getList(long nodeId) {
        int count = 2;
        List<PostDO> postings = postMapper.getListByNodeAndScore(nodeId, count, Enums.PostState.approved.value());
        postings.forEach(postingDO -> idToName(postingDO));
        return postings;
    }

    public List<PostDO> getList(int nodeId, int lastPostingId) {
        int count = 2;
        List<PostDO> postings = postMapper.getListByLastId(nodeId, lastPostingId, count, Enums.PostState.approved.value());
        postings.forEach(postingDO -> idToName(postingDO));
        return postings;
    }

    public void idToName(PostDO posting) {
        if (posting.getType() == Enums.PostType.article.value()) return;
        if (posting.getContent().equals("")) return;
        List<Long> ids = Arrays.stream(posting.getContent().split(",")).map(Long::parseLong).toList();
        List<NodeDO> nodeList = nodeMapper.getByIds(ids);
        String names = nodeList.stream().map(NodeDO::getName).collect(Collectors.joining(","));
        posting.setContent(names);
    }

    public List<PostDTO> getUserArticle(int userId, int lastId) {
        int count = 10;
        List<PostDO> postings = postMapper.getArticleListByUser(userId, lastId, count);
        if (postings == null || postings.size() == 0) return new ArrayList<>();

        List<PostDTO> postDTOList = Converter.INSTANCE.toPostDTO(postings);

        // get all user
        List<Long> userIds = postDTOList.stream().map(PostDTO::getCreatorId).collect(Collectors.toList());
        List<UserDO> userList = userMapper.getByIds(userIds);
        Map<Long, UserDO> userMap = userList.stream().collect(Collectors.toMap(UserDO::getId, node -> node));

        // get all node
        List<Long> nodeIds = postDTOList.stream().map(PostDTO::getNodeId).collect(Collectors.toList());
        List<NodeDO> nodeList = nodeMapper.getByIds(nodeIds);
        Map<Long, NodeDO> nodeMap = nodeList.stream().collect(Collectors.toMap(NodeDO::getId, node -> node));

        for (PostDTO postDTO : postDTOList) {
            postDTO.setNode(Converter.INSTANCE.toNodeDTO(nodeMap.get(postDTO.getNodeId())));
            NodeDTO node = postDTO.getNode();
            node.setCourse(Converter.INSTANCE.toCourseDTOV4(courseMapper.getById(node.getCourseId())));

            postDTO.setCreator(Converter.INSTANCE.toUserDTOV1(userMap.get(postDTO.getCreatorId())));
        }

        return postDTOList;
    }

    public List<PostDTO> getUserContents(int userId, int lastId) {
        int count = 10;
        List<PostDO> postings = postMapper.getContentsListByUser(userId, lastId, count);
        if (postings == null || postings.size() == 0) return new ArrayList<>();

        List<Long> nodeIds = postings.stream()
                .map(PostDO::getContent)
                .flatMap(s -> Arrays.stream(s.split(",")).map(Long::parseLong)).toList();
        List<NodeDO> nodeList = nodeMapper.getByIds(nodeIds);

        Map<Long, NodeDO> nodeMap = nodeList.stream().collect(Collectors.toMap(NodeDO::getId, node -> node));

        for (PostDO postDO : postings) {
            String content = postDO.getContent();
            String[] contents = content.split(",");
            StringBuilder newContent = new StringBuilder();
            for (int i = 0; i < contents.length; i++) {
                int id = Integer.parseInt(contents[i]);
                NodeDO node = nodeMap.get(id);
                if (node == null) {
                    continue;
                }
                if (i < contents.length - 1) {
                    newContent.append(node.getName()).append(",");
                } else {
                    newContent.append(node.getName());
                }
            }
            postDO.setContent(newContent.toString());
        }

        List<PostDTO> postDTOList = Converter.INSTANCE.toPostDTO(postings);
        nodeIds = postDTOList.stream().map(PostDTO::getNodeId).collect(Collectors.toList());
        nodeList = nodeMapper.getByIds(nodeIds);
        nodeMap = nodeList.stream().collect(Collectors.toMap(NodeDO::getId, node -> node));

        List<Long> allPostingIds = new LinkedList<>();
        if (postings != null) postings.stream().forEach(item -> allPostingIds.add(item.getId()));

        Map<Long, Integer> types = new HashMap<>();
        if (allPostingIds.size() > 0) {
            List<UpvoteDO> upvotes = upvoteMapper.getList(userId, allPostingIds, Enums.ObjectType.post.value());
            for (UpvoteDO upvote : upvotes) {
                types.put(upvote.getObjectId(), upvote.getType());
            }
        }

        // get all user
        List<Long> userIds = postDTOList.stream().map(PostDTO::getCreatorId).collect(Collectors.toList());
        List<UserDO> userList = userMapper.getByIds(userIds);
        Map<Long, UserDO> userMap = userList.stream().collect(Collectors.toMap(UserDO::getId, node -> node));

        for (PostDTO postDTO : postDTOList) {
            postDTO.setNode(Converter.INSTANCE.toNodeDTO(nodeMap.get(postDTO.getNodeId())));
            NodeDTO node = postDTO.getNode();
            node.setCourse(Converter.INSTANCE.toCourseDTOV4(courseMapper.getById(node.getCourseId())));

            if (types.containsKey(postDTO.getId()))
                postDTO.setVoteType(types.get(postDTO.getId()));

            postDTO.setCreator(Converter.INSTANCE.toUserDTOV1(userMap.get(postDTO.getCreatorId())));
        }
        return postDTOList;
    }

    /**
     * 基于分数的排序获取文章列表
     * @param nodeId 节点ID
     * @param limit 返回数量限制
     * @return 按分数排序的文章列表
     */
    public List<PostDO> getListByScore(int nodeId, int limit) {
        List<PostDO> postings = postMapper.getListByNodeAndScore(nodeId, limit, Enums.PostState.approved.value());
        postings.forEach(this::idToName);
        return postings;
    }

    /**
     * 基于分数的分页查询
     * @param nodeId 节点ID
     * @param lastScore 上一页最后一个文章的分数
     * @param lastId 上一页最后一个文章的ID
     * @param limit 返回数量限制
     * @return 按分数排序的文章列表
     */
    public List<PostDO> getListByScoreWithPagination(int nodeId, Double lastScore, int lastId, int limit) {
        if (lastScore == null) {
            return getListByScore(nodeId, limit);
        }

        List<PostDO> postings = postMapper.getListByNodeAndScoreAndPaginated(
                nodeId, lastScore, lastId, limit, Enums.PostState.approved.value());
        postings.forEach(this::idToName);
        return postings;
    }

    /**
     * 获取用户文章列表（包含阅读量）
     */
    public List<PostDTOV2> getUserArticleWithViews(long userId, long lastId) {
        int count = 10;
        List<PostDO> postings = postMapper.getArticleListByUser(userId, lastId, count);
        if (postings == null || postings.size() == 0) return new ArrayList<>();

        List<PostDTOV2> postDTOList = Converter.INSTANCE.toPostDTOV2(postings);

        // 设置views字段
        setViewsForPosts(postDTOList);

        // get all user
        List<Long> userIds = postDTOList.stream().map(PostDTOV2::getCreatorId).collect(Collectors.toList());
        List<UserDO> userList = userMapper.getByIds(userIds);
        Map<Long, UserDO> userMap = userList.stream().collect(Collectors.toMap(UserDO::getId, node -> node));

        // get all node
        List<Long> nodeIds = postDTOList.stream().map(PostDTOV2::getNodeId).collect(Collectors.toList());
        List<NodeDO> nodeList = nodeMapper.getByIds(nodeIds);
        Map<Long, NodeDO> nodeMap = nodeList.stream().collect(Collectors.toMap(NodeDO::getId, node -> node));

        for (PostDTOV2 postDTO : postDTOList) {
            postDTO.setNode(Converter.INSTANCE.toNodeDTO(nodeMap.get(postDTO.getNodeId())));
            NodeDTO node = postDTO.getNode();
            node.setCourse(Converter.INSTANCE.toCourseDTOV4(courseMapper.getById(node.getCourseId())));

            postDTO.setCreator(Converter.INSTANCE.toUserDTOV1(userMap.get(postDTO.getCreatorId())));
        }

        return postDTOList;
    }

    /**
     * 获取用户目录列表（包含阅读量）
     */
    public List<PostDTOV2> getUserContentsWithViews(long userId, long lastId) {
        int count = 10;
        List<PostDO> postings = postMapper.getContentsListByUser(userId, lastId, count);
        if (postings == null || postings.size() == 0) return new ArrayList<>();

        List<Long> nodeIds = postings.stream()
                .map(PostDO::getContent)
                .flatMap(s -> Arrays.stream(s.split(",")).map(Long::parseLong)).toList();
        List<NodeDO> nodeList = nodeMapper.getByIds(nodeIds);

        Map<Long, NodeDO> nodeMap = nodeList.stream().collect(Collectors.toMap(NodeDO::getId, node -> node));

        for (PostDO postDO : postings) {
            String content = postDO.getContent();
            String[] contents = content.split(",");
            StringBuilder newContent = new StringBuilder();
            for (int i = 0; i < contents.length; i++) {
                int id = Integer.parseInt(contents[i]);
                NodeDO node = nodeMap.get(id);
                if (node == null) {
                    continue;
                }
                if (i < contents.length - 1) {
                    newContent.append(node.getName()).append(",");
                } else {
                    newContent.append(node.getName());
                }
            }
            postDO.setContent(newContent.toString());
        }

        List<PostDTOV2> postDTOList = Converter.INSTANCE.toPostDTOV2(postings);
        
        // 设置views字段
        setViewsForPosts(postDTOList);
        
        nodeIds = postDTOList.stream().map(PostDTOV2::getNodeId).collect(Collectors.toList());
        nodeList = nodeMapper.getByIds(nodeIds);
        nodeMap = nodeList.stream().collect(Collectors.toMap(NodeDO::getId, node -> node));

        List<Long> allPostingIds = new LinkedList<>();
        if (postings != null) postings.stream().forEach(item -> allPostingIds.add(item.getId()));

        Map<Long, Integer> types = new HashMap<>();
        if (allPostingIds.size() > 0) {
            List<UpvoteDO> upvotes = upvoteMapper.getList(userId, allPostingIds, Enums.ObjectType.post.value());
            for (UpvoteDO upvote : upvotes) {
                types.put(upvote.getObjectId(), upvote.getType());
            }
        }

        // get all user
        List<Long> userIds = postDTOList.stream().map(PostDTOV2::getCreatorId).collect(Collectors.toList());
        List<UserDO> userList = userMapper.getByIds(userIds);
        Map<Long, UserDO> userMap = userList.stream().collect(Collectors.toMap(UserDO::getId, node -> node));

        for (PostDTOV2 postDTO : postDTOList) {
            postDTO.setNode(Converter.INSTANCE.toNodeDTO(nodeMap.get(postDTO.getNodeId())));
            NodeDTO node = postDTO.getNode();
            node.setCourse(Converter.INSTANCE.toCourseDTOV4(courseMapper.getById(node.getCourseId())));

            if (types.containsKey(postDTO.getId()))
                postDTO.setVoteType(types.get(postDTO.getId()));

            postDTO.setCreator(Converter.INSTANCE.toUserDTOV1(userMap.get(postDTO.getCreatorId())));
        }
        return postDTOList;
    }

    /**
     * 为文章列表设置阅读量（历史数据 + 今日实时数据）
     */
    private void setViewsForPosts(List<PostDTOV2> postDTOList) {
        // 使用DailyStatsService来设置阅读量
        dailyStatsService.setViewsForPosts(postDTOList);
    }

    /**
     * 批量获取帖子（带用户信息和投票状态）
     */
    public List<PostDTO> getPostsWithUserAndVoteInfo(List<Long> ids, Long nodeId, double lastScore, Long lastPostingId, long currentUserId) {
        List<PostDO> postDOList = null;
        if (ids != null && ids.size() > 0) {
            postDOList = postMapper.getByIds(ids);
        } else if (nodeId != null && nodeId > 0) {
            int count = 2;
            postDOList = postMapper.getListByNodeAndScoreAndPaginated(nodeId, lastScore, lastPostingId, count, Enums.PostState.approved.value());
        }
        
        if (postDOList == null) {
            throw new IllegalArgumentException("不能获取帖子列表");
        }

        List<Long> allPostingIds = new ArrayList<>();
        List<Long> userIds = new LinkedList<>();
        postDOList.forEach(postingDO -> {
            idToName(postingDO);
            allPostingIds.add(postingDO.getId());
            userIds.add(postingDO.getCreator());
        });

        List<UserDTOV1> userList = userIds.size() == 0 ?
                new ArrayList<>() : Converter.INSTANCE.toUserDTOV1(userMapper.getByIds(userIds));
        Map<Long, UserDTOV1> userMap = new HashMap<>();
        for (UserDTOV1 user : userList) {
            userMap.put(user.getId(), user);
        }

        List<PostDTO> postDTOList = Converter.INSTANCE.toPostDTO(postDOList);
        postDTOList.stream().forEach(item -> {
            item.setCreator(userMap.get(item.getCreatorId()));
        });

        if (allPostingIds.size() > 0) {
            List<UpvoteDO> upvotes = upvoteMapper.getList((int)currentUserId, allPostingIds, Enums.ObjectType.post.value());
            Map<Long, Integer> types = new HashMap<>();
            for (UpvoteDO upvote : upvotes) {
                types.put(upvote.getObjectId(), upvote.getType());
            }

            for (PostDTO posting : postDTOList) {
                if (types.containsKey(posting.getId()))
                    posting.setVoteType(types.get(posting.getId()));
            }
        }

        return postDTOList;
    }

    /**
     * 创建帖子（处理contents类型的特殊逻辑）
     */
    @Transactional
    public void createPost(PostDTO posting) {
        if (posting.getType() == Enums.PostType.contents.value()) {
            NodeDO nodeDO = nodeMapper.getById(posting.getNodeId());
            List<String> nodeNames;
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                nodeNames = objectMapper.readValue(posting.getContent(), new TypeReference<>() {});
            } catch (JsonProcessingException e) {
                log.error("Failed to process JSON content", e);
                throw new RuntimeException("JSON解析失败", e);
            }
            String[] ids = new String[nodeNames.size()];
            for (int i = 0; i < nodeNames.size(); i ++) {
                NodeDO node = new NodeDO();
                node.setName(nodeNames.get(i));
                node.setDescription("");
                node.setRoot(0l);
                node.setCourseId(nodeDO.getCourseId());
                node.setCreatedAt(Utils.getLocalDateTime());
                node.setUpdatedAt(Utils.getLocalDateTime());
                nodeMapper.insert(node);
                ids[i] = Long.toString(node.getId());
            }
            posting.setContent(String.join(",", ids));
        }

        posting.setCreatorId(0l);
        posting.setCreatedAt(Utils.getTimeString());
        postMapper.insert(Converter.INSTANCE.toPostDO(posting));
    }

    /**
     * 更新帖子内容
     */
    @Transactional
    public void updatePost(Long id, PostDTO posting) {
        PostDO postDO = postMapper.get(id);
        if (postDO == null) {
            throw new RuntimeException("帖子不存在");
        }

        postDO.setContent(posting.getContent());
        postDO.setUpdatedAt(Utils.getLocalDateTime());
        postMapper.update(postDO);
    }

    /**
     * 删除帖子（软删除）
     */
    @Transactional
    public void deletePost(Long id) {
        PostDO postDO = postMapper.get(id);
        if (postDO == null) {
            throw new RuntimeException("帖子不存在");
        }

        postDO.setState(Enums.PostState.deleted.value());
        postDO.setUpdatedAt(Utils.getLocalDateTime());
        postMapper.update(postDO);
    }

    /**
     * 获取帖子详情（转换为DTO）
     */
    public PostDTO getPostDetail(Long id) {
        return Converter.INSTANCE.toPostDTO(get(id));
    }

    /**
     * 获取节点帖子列表
     */
    public List<PostDTO> getNodePostsList(Long nodeId) {
        int count = 3;
        List<PostDO> postings = postMapper.getListByNode(nodeId, count, Enums.PostState.approved.value());
        postings.forEach(this::idToName);
        return Converter.INSTANCE.toPostDTO(postings);
    }

    /**
     * 获取待审核帖子列表
     */
    public List<PostDTO> getPendingPostsList() {
        List<PostDO> postDOList = postMapper.getListByState(Enums.PostState.approved.value(), 200);
        for (PostDO postDO : postDOList) {
            if (postDO.getType() == Enums.PostType.contents.value()) {
                idToName(postDO);
            }
        }
        return Converter.INSTANCE.toPostDTO(postDOList);
    }

    /**
     * 审核帖子
     */
    @Transactional
    public PostDTO approvePost(Long id, boolean approve) {
        PostDO postDO = postMapper.get(id);
        if (postDO == null) {
            throw new RuntimeException("帖子不存在");
        }

        if (approve && postDO.getState() != Enums.PostState.approved.value()) {
            postDO.setState(Enums.CommentState.approved.value());
            postMapper.update(postDO);
        }
        if (!approve && postDO.getState() != Enums.CommentState.deleted.value()) {
            postDO.setState(Enums.CommentState.deleted.value());
            postMapper.update(postDO);
        }
        return Converter.INSTANCE.toPostDTO(postDO);
    }
}
