package com.prosper.learn.application.assembler;

import com.prosper.learn.analytics.dto.ContentStatsDTO;
import com.prosper.learn.analytics.stats.service.ContentStatsDomainService;
import com.prosper.learn.application.converter.CourseConverter;
import com.prosper.learn.application.converter.NodeConverter;
import com.prosper.learn.application.converter.PostConverter;
import com.prosper.learn.application.converter.UserConverter;
import com.prosper.learn.application.dto.response.node.NodeWithCourseBriefDTO;
import com.prosper.learn.application.dto.response.post.PostDetailDTO;
import com.prosper.learn.application.dto.response.post.PostFullDTO;
import com.prosper.learn.application.dto.response.post.PostSummaryDTO;
import com.prosper.learn.application.dto.response.post.PostWithVoteDTO;
import com.prosper.learn.application.dto.response.user.UserBriefDTO;
import com.prosper.learn.content.course.CourseDO;
import com.prosper.learn.content.course.CourseDataService;
import com.prosper.learn.content.node.NodeDO;
import com.prosper.learn.content.node.NodeDataService;
import com.prosper.learn.content.post.PostDO;
import com.prosper.learn.interaction.bookmark.BookmarkDataService;
import com.prosper.learn.interaction.upvote.UpvoteDO;
import com.prosper.learn.interaction.upvote.UpvoteDataService;
import com.prosper.learn.shared.domain.Enums.ContentType;
import com.prosper.learn.user.profile.UserDO;
import com.prosper.learn.user.profile.UserDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Post DTO 组装器
 * 负责将 PostDO 转换为各种 DTO（需要联查数据库）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostAssembler {

    private final PostConverter postConverter;
    private final NodeConverter nodeConverter;
    private final CourseConverter courseConverter;
    private final UserConverter userConverter;

    private final NodeDataService nodeDataService;
    private final CourseDataService courseDataService;
    private final UserDataService userDataService;
    private final UpvoteDataService upvoteDataService;
    private final ContentStatsDomainService contentStatsDomainService;
    private final BookmarkDataService bookmarkDataService;

    // ========== toDetailDTO ==========

    /**
     * 转换为帖子详情 DTO（含节点和课程信息）
     * 用于：收藏列表等需要显示节点名称的场景
     */
    public List<PostDetailDTO> toDetailDTO(List<PostDO> postDOList) {
        if (postDOList == null || postDOList.isEmpty()) {
            return new ArrayList<>();
        }

        List<PostDetailDTO> postDTOList = postConverter.toDetailDTO(postDOList);

        // 批量获取节点信息
        Set<Long> nodeIds = postDOList.stream()
            .map(PostDO::getNodeId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Long, NodeDO> nodeMap = nodeIds.isEmpty() ? Map.of() : nodeDataService.getMapByIds(nodeIds);

        // 批量获取课程信息
        Set<Long> courseIds = nodeMap.values().stream()
            .map(NodeDO::getCourseId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Long, CourseDO> courseMap = courseIds.isEmpty() ? Map.of() : courseDataService.getMapByIds(courseIds);

        // 填充节点和课程信息
        for (PostDetailDTO dto : postDTOList) {
            if (dto.getNodeId() != null) {
                NodeDO nodeDO = nodeMap.get(dto.getNodeId());
                if (nodeDO != null) {
                    NodeWithCourseBriefDTO nodeDTO = nodeConverter.toWithCourseBriefDTO(nodeDO);
                    if (nodeDO.getCourseId() != null) {
                        CourseDO courseDO = courseMap.get(nodeDO.getCourseId());
                        if (courseDO != null) {
                            nodeDTO.setCourse(courseConverter.toBriefDTO(courseDO));
                        }
                    }
                    dto.setNode(nodeDTO);
                }
            }
        }

        return postDTOList;
    }

    // ========== toFullDTO ==========

    /**
     * 转换为完整帖子信息（含节点、创建者、统计数据、投票类型、收藏状态）
     * 用于：帖子详情页、帖子列表（完整信息）
     */
    public List<PostFullDTO> toFullDTO(List<PostDO> postDOList, long userId) {
        if (postDOList == null || postDOList.isEmpty()) {
            return new ArrayList<>();
        }

        List<PostFullDTO> postDTOList = postConverter.toFullDTO(postDOList);

        // 批量获取统计数据
        List<Long> postIds = postDTOList.stream().map(PostSummaryDTO::getId).collect(Collectors.toList());
        Map<Long, ContentStatsDTO> statsMap = contentStatsDomainService.batchGetContentStats(ContentType.post, postIds);

        // 填充统计字段
        postDTOList.forEach(post -> {
            ContentStatsDTO stats = statsMap.get(post.getId());
            if (stats != null) {
                post.setViewCount(stats.getViewCount());
                post.setTwiceCount(stats.getTwiceCount());
                post.setLikeCount(stats.getLikeCount());
                post.setCommentCount(stats.getCommentCount());
            }
        });

        // 批量获取节点信息
        Set<Long> nodeIds = postDOList.stream()
            .map(PostDO::getNodeId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Long, NodeDO> nodeMap = nodeIds.isEmpty() ? Map.of() :
            nodeDataService.getByIds(new ArrayList<>(nodeIds)).stream()
                .collect(Collectors.toMap(NodeDO::getId, node -> node));

        // 批量获取课程信息
        Set<Long> courseIds = nodeMap.values().stream()
            .map(NodeDO::getCourseId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Long, CourseDO> courseMap = courseIds.isEmpty() ? Map.of() : courseDataService.getMapByIds(courseIds);

        // 批量获取投票状态
        Map<Long, Integer> voteTypeMap = new HashMap<>();
        if (!postIds.isEmpty()) {
            List<UpvoteDO> upvotes = upvoteDataService.getList(userId, postIds, ContentType.post.value());
            for (UpvoteDO upvote : upvotes) {
                voteTypeMap.put(upvote.getObjectId(), upvote.getType());
            }
        }

        // 批量获取收藏状态
        List<Long> bookmarkedIds = bookmarkDataService.getBookmarkedIds(userId, postIds, ContentType.post.value());
        Set<Long> bookmarkedSet = new HashSet<>(bookmarkedIds);

        // 批量获取创建者信息
        Set<Long> userIds = postDOList.stream()
            .map(PostDO::getCreatorId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Long, UserDO> userMap = userIds.isEmpty() ? Map.of() : userDataService.getMapByIds(userIds);

        // 填充所有信息
        for (PostFullDTO dto : postDTOList) {
            // 填充节点信息
            if (dto.getNodeId() != null) {
                NodeDO nodeDO = nodeMap.get(dto.getNodeId());
                if (nodeDO != null) {
                    NodeWithCourseBriefDTO nodeDTO = nodeConverter.toWithCourseBriefDTO(nodeDO);
                    if (nodeDO.getCourseId() != null) {
                        CourseDO courseDO = courseMap.get(nodeDO.getCourseId());
                        if (courseDO != null) {
                            nodeDTO.setCourse(courseConverter.toBriefDTO(courseDO));
                        }
                    }
                    dto.setNode(nodeDTO);
                }
            }

            // 填充投票状态
            if (voteTypeMap.containsKey(dto.getId())) {
                dto.setVoteType(voteTypeMap.get(dto.getId()));
            }

            // 填充收藏状态
            dto.setBookmarked(bookmarkedSet.contains(dto.getId()));

            // 填充创建者信息
            if (dto.getCreatorId() != null) {
                UserDO userDO = userMap.get(dto.getCreatorId());
                if (userDO != null) {
                    dto.setCreator(userConverter.toBriefDTO(userDO));
                }
            }
        }

        return postDTOList;
    }

    // ========== toWithVoteDTO ==========

    /**
     * 转换为帖子（含创建者和投票类型）
     * 用于：帖子列表（轻量级，不含完整节点信息）
     */
    public List<PostWithVoteDTO> toWithVoteDTO(List<PostDO> postDOList, long userId) {
        if (postDOList == null || postDOList.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> postIds = postDOList.stream().map(PostDO::getId).collect(Collectors.toList());

        // 批量获取创建者信息
        Set<Long> userIds = postDOList.stream()
            .map(PostDO::getCreatorId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        List<UserBriefDTO> userList = userIds.isEmpty() ?
            new ArrayList<>() : userConverter.toBriefDTO(userDataService.getByIds(new ArrayList<>(userIds)));
        Map<Long, UserBriefDTO> userMap = new HashMap<>();
        for (UserBriefDTO user : userList) {
            userMap.put(user.getId(), user);
        }

        List<PostWithVoteDTO> postDTOList = postConverter.toWithVoteDTO(postDOList);

        // 填充创建者信息
        for (PostWithVoteDTO dto : postDTOList) {
            dto.setCreator(userMap.get(dto.getCreatorId()));
        }

        if (!postIds.isEmpty()) {
            // 批量获取投票状态
            List<UpvoteDO> upvotes = upvoteDataService.getList(userId, postIds, ContentType.post.value());
            Map<Long, Integer> voteTypeMap = new HashMap<>();
            for (UpvoteDO upvote : upvotes) {
                voteTypeMap.put(upvote.getObjectId(), upvote.getType());
            }

            // 批量获取收藏状态
            List<Long> bookmarkedIds = bookmarkDataService.getBookmarkedIds(userId, postIds, ContentType.post.value());
            Set<Long> bookmarkedSet = new HashSet<>(bookmarkedIds);

            // 填充投票和收藏状态
            for (PostWithVoteDTO dto : postDTOList) {
                if (voteTypeMap.containsKey(dto.getId())) {
                    dto.setVoteType(voteTypeMap.get(dto.getId()));
                }
                dto.setBookmarked(bookmarkedSet.contains(dto.getId()));
            }
        }

        return postDTOList;
    }
}
