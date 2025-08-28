package com.prosper.learn.domain.service;

import com.prosper.learn.common.Enums;
import com.prosper.learn.domain.util.Converter;
import com.prosper.learn.dto.NodeDTO;
import com.prosper.learn.dto.PostDTO;
import com.prosper.learn.dto.PostDTOV2;
import com.prosper.learn.persistence.dataobject.*;
import com.prosper.learn.persistence.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
        List<PostDO> postings = postMapper.getListByNodeAndScore(nodeId, count, Enums.PostState.approved.value);
        postings.forEach(postingDO -> idToName(postingDO));
        return postings;
    }

    public List<PostDO> getList(int nodeId, int lastPostingId) {
        int count = 2;
        List<PostDO> postings = postMapper.getListByLastId(nodeId, lastPostingId, count, Enums.PostState.approved.value);
        postings.forEach(postingDO -> idToName(postingDO));
        return postings;
    }

    public void idToName(PostDO posting) {
        if (posting.getType() == Enums.PostType.article.value) return;
        if (posting.getContent().equals("")) return;
        List<Integer> ids = Arrays.stream(posting.getContent().split(",")).map(Integer::parseInt).toList();
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
        List<Integer> userIds = postDTOList.stream().map(PostDTO::getCreatorId).collect(Collectors.toList());
        List<UserDO> userList = userMapper.getByIds(userIds);
        Map<Integer, UserDO> userMap = userList.stream().collect(Collectors.toMap(UserDO::getId, node -> node));

        // get all node
        List<Integer> nodeIds = postDTOList.stream().map(PostDTO::getNodeId).collect(Collectors.toList());
        List<NodeDO> nodeList = nodeMapper.getByIds(nodeIds);
        Map<Integer, NodeDO> nodeMap = nodeList.stream().collect(Collectors.toMap(NodeDO::getId, node -> node));

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

        List<Integer> nodeIds = postings.stream()
                .map(PostDO::getContent)
                .flatMap(s -> Arrays.stream(s.split(",")).map(Integer::parseInt)).toList();
        List<NodeDO> nodeList = nodeMapper.getByIds(nodeIds);

        Map<Integer, NodeDO> nodeMap = nodeList.stream().collect(Collectors.toMap(NodeDO::getId, node -> node));

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

        List<Integer> allPostingIds = new LinkedList<>();
        if (postings != null) postings.stream().forEach(item -> allPostingIds.add(item.getId()));

        Map<Integer, Integer> types = new HashMap<>();
        if (allPostingIds.size() > 0) {
            List<UpvoteDO> upvotes = upvoteMapper.getList(userId, allPostingIds, Enums.ObjectType.post.value);
            for (UpvoteDO upvote : upvotes) {
                types.put(upvote.getObjectId(), upvote.getType());
            }
        }

        // get all user
        List<Integer> userIds = postDTOList.stream().map(PostDTO::getCreatorId).collect(Collectors.toList());
        List<UserDO> userList = userMapper.getByIds(userIds);
        Map<Integer, UserDO> userMap = userList.stream().collect(Collectors.toMap(UserDO::getId, node -> node));

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
        List<PostDO> postings = postMapper.getListByNodeAndScore(nodeId, limit, Enums.PostState.approved.value);
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
                nodeId, lastScore, lastId, limit, Enums.PostState.approved.value);
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
        Map<Integer, UserDO> userMap = userList.stream().collect(Collectors.toMap(UserDO::getId, node -> node));

        // get all node
        List<Integer> nodeIds = postDTOList.stream().map(PostDTOV2::getNodeId).collect(Collectors.toList());
        List<NodeDO> nodeList = nodeMapper.getByIds(nodeIds);
        Map<Integer, NodeDO> nodeMap = nodeList.stream().collect(Collectors.toMap(NodeDO::getId, node -> node));

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

        List<Integer> nodeIds = postings.stream()
                .map(PostDO::getContent)
                .flatMap(s -> Arrays.stream(s.split(",")).map(Integer::parseInt)).toList();
        List<NodeDO> nodeList = nodeMapper.getByIds(nodeIds);

        Map<Integer, NodeDO> nodeMap = nodeList.stream().collect(Collectors.toMap(NodeDO::getId, node -> node));

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

        List<Integer> allPostingIds = new LinkedList<>();
        if (postings != null) postings.stream().forEach(item -> allPostingIds.add(item.getId()));

        Map<Integer, Integer> types = new HashMap<>();
        if (allPostingIds.size() > 0) {
            List<UpvoteDO> upvotes = upvoteMapper.getList(userId, allPostingIds, Enums.ObjectType.post.value);
            for (UpvoteDO upvote : upvotes) {
                types.put(upvote.getObjectId(), upvote.getType());
            }
        }

        // get all user
        List<Integer> userIds = postDTOList.stream().map(PostDTOV2::getCreatorId).collect(Collectors.toList());
        List<UserDO> userList = userMapper.getByIds(userIds);
        Map<Integer, UserDO> userMap = userList.stream().collect(Collectors.toMap(UserDO::getId, node -> node));

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

}
