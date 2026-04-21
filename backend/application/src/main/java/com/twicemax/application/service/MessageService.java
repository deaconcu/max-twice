package com.twicemax.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.twicemax.analytics.stats.dataservice.UserStatsDataService;
import com.twicemax.application.converter.MessageConverter;
import com.twicemax.application.converter.NodeConverter;
import com.twicemax.application.converter.UserConverter;
import com.twicemax.application.dto.response.message.*;
import com.twicemax.content.node.NodeDO;
import com.twicemax.content.node.NodeDataService;
import com.twicemax.content.post.PostDO;
import com.twicemax.content.post.PostDataService;
import com.twicemax.content.role.RoleDO;
import com.twicemax.content.role.RoleDataService;
import com.twicemax.content.roadmap.RoadmapDO;
import com.twicemax.content.roadmap.RoadmapDataService;
import com.twicemax.interaction.message.MessageDO;
import com.twicemax.interaction.message.MessageDataService;
import com.twicemax.interaction.message.MessageDomainService;
import com.twicemax.shared.common.utils.Utils;
import com.twicemax.shared.domain.Enums;
import com.twicemax.shared.domain.exception.StatusCode;
import com.twicemax.shared.infrastructure.config.SystemProperties;
import com.twicemax.user.profile.UserDO;
import com.twicemax.user.profile.UserDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.twicemax.shared.domain.Enums.*;
import static com.twicemax.shared.domain.Enums.MessageType.*;

/**
 * 消息管理应用服务
 *
 * 负责管理系统中的各种消息类型，包括：
 * - 用户之间的私信
 * - 系统通知消息
 * - 课程申请消息
 * - 评论、点赞、关注等系统事件消息
 *
 * 核心功能：
 * - 跨域验证和协调
 * - DTO转换和关联数据填充
 * - 业务流程处理
 *
 * @author Claude
 * @since 2024-01-20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    /** 系统消息内容常量 - 固定内容，定义为静态常量 */
    private static final Map<Integer, String> SYSTEM_MESSAGES = new HashMap<>();

    /** 默认分页大小常量 */
    private static final int DEFAULT_PAGE_SIZE = 20;

    /** 最大分页大小常量 */
    private static final int MAX_PAGE_SIZE = 100;

    static {
        SYSTEM_MESSAGES.put(1, "你的课程申请请求被拒绝了");
    }

    // 领域服务依赖
    private final MessageDomainService messageDomainService;

    // 跨域依赖
    private final PostDataService postDataService;
    private final NodeDataService nodeDataService;
    private final RoleDataService roleDataService;
    private final MessageDataService messageDataService;
    private final UserDataService userDataService;
    private final UserStatsDataService userStatsDataService;
    private final RoadmapDataService roadmapDataService;

    // 转换器依赖
    private final MessageConverter messageConverter;
    private final UserConverter userConverter;
    private final NodeConverter nodeConverter;

    /** 系统配置属性 */
    private final SystemProperties systemProperties;

    /** JSON 处理器 */
    private final ObjectMapper objectMapper;

    // ========== Command 方法（写操作）==========

    /**
     * 创建消息
     *
     * @param content 消息内容
     * @param senderId 发送者ID（系统消息时为0）
     * @param receiverId 接收者ID
     * @param messageType 消息类型
     */
    public void create(String content, long senderId, long receiverId, MessageType messageType) {
        // 跨域验证：验证发送者（系统消息发送者ID为0时跳过验证）
        if (senderId != 0) {
            userDataService.validateAndGet(senderId);
        }

        // 跨域验证：验证接收者
        if (receiverId != 0) {
            userDataService.validateAndGet(receiverId);
        }

        // 委托给领域服务处理核心逻辑
        messageDomainService.createMessage(content, senderId, receiverId,
                messageType.value(), messageType.getCategory());
    }

    /**
     * 创建评论消息
     */
    public void createCommentMessage(long receiverId, long commenterId, long nodeId, long commentId, int type) {
        messageDomainService.createCommentMessage(receiverId, commenterId, nodeId, commentId, type);
    }

    /**
     * 创建关注消息
     */
    public void createFollowMessage(long receiverId, long followerId) {
        messageDomainService.createFollowMessage(receiverId, followerId);
    }

    /**
     * 创建帖子点赞消息
     */
    public void createPostUpvoteMessage(long receiverId, long voterId, long nodeId, long postId, VoteType voteType) {
        messageDomainService.createPostUpvoteMessage(receiverId, voterId, nodeId, postId, voteType);
    }

    /**
     * 创建评论点赞消息
     */
    public void createCommentUpvoteMessage(long receiverId, long voterId, long nodeId, long commentId) {
        messageDomainService.createCommentUpvoteMessage(receiverId, voterId, nodeId, commentId);
    }

    /**
     * 创建路线图点赞消息
     */
    public void createRoadmapUpvoteMessage(long receiverId, long voterId, long roleId, long roadmapId) {
        messageDomainService.createRoadmapUpvoteMessage(receiverId, voterId, roleId, roadmapId);
    }

    /**
     * 创建记忆卡片组点赞消息
     */
    public void createMemoryDeckUpvoteMessage(long receiverId, long voterId, long nodeId, long deckId) {
        messageDomainService.createMemoryDeckUpvoteMessage(receiverId, voterId, nodeId, deckId);
    }

    /**
     * 创建邀请消息
     */
    public void createInviteMessage(long receiverId, long inviterId, long nodeId) {
        messageDomainService.createInviteMessage(receiverId, inviterId, nodeId);
    }

    // ========== Query 方法（读操作）==========

    /**
     * 获取指定ID的消息详情
     *
     * @param id 消息ID
     * @return 消息DTO对象
     */
    public MessageDTO get(long id) {
        MessageDO messageDO = messageDomainService.getById(id);
        return toDTOV1(messageDO);
    }

    /**
     * 按分类获取消息列表（返回包含 lastViewedMessageId 的响应）
     *
     * @param category 消息分类（0=全部, 1=互动, 2=系统, 3=私信）
     * @param receiverId 接收者ID
     * @param lastId 最后一条消息ID，用于分页
     * @param type 可选的消息类型过滤
     * @param currentUser 当前用户
     * @return 包含消息列表和 lastViewedMessageId 的响应
     */
    public MessageListResponse getListByCategoryWithLastViewed(int category, long receiverId, Long lastId,
                                                               Integer type, UserDO currentUser) {
        // 获取消息列表
        List<MessageDTO> messageDTOs = getListByCategory(category, receiverId, lastId, type);

        // 只在第一页（lastId == null）时处理 lastViewedMessageId
        Long lastViewedMessageId = null;
        if (lastId == null) {
            // 读取当前值
            lastViewedMessageId = userStatsDataService.getLastViewedMessageId(currentUser.getId());

            // 更新为最新消息ID（第一条消息是最新的，因为降序排列）
            if (!messageDTOs.isEmpty()) {
                long maxId = messageDTOs.get(0).getId();
                userStatsDataService.updateLastViewedMessageId(currentUser.getId(), maxId);
            }
        }

        return MessageListResponse.of(messageDTOs, lastViewedMessageId);
    }

    /**
     * 按分类获取消息列表
     */
    public List<MessageDTO> getListByCategory(int category, long receiverId, Long lastId, Integer type) {
        // 参数校验
        if (category < 1 || category > 4) {
            throw StatusCode.INVALID_PARAMETER.exception("消息分类必须为1-4");
        }

        // 委托给领域服务获取数据
        List<MessageDO> messageDOList = messageDomainService.getByCategory(receiverId, category, lastId, 20, type);

        // 跨域数据聚合和DTO转换
        return convertSystemMessages(messageDOList);
    }

    /**
     * 获取未读消息数量
     *
     * @param receiverId 接收者ID
     * @param lastViewedMessageId 最后查看的消息ID
     * @return 未读消息数量
     */
    public int getUnreadCount(long receiverId, long lastViewedMessageId) {
        return messageDomainService.countUnreadMessages(receiverId, lastViewedMessageId);
    }

    // ========== DTO转换方法 ==========

    /**
     * 将消息DO转换为DTO（包含发送者和接收者信息）
     * v1 = v0 + sender + receiver
     */
    public MessageDTO toDTOV1(MessageDO messageDO) {
        // 获取发送者和接收者信息（系统消息的发送者可能为null）
        UserDO sender = messageDO.getSenderId() != 0 ? userDataService.getById(messageDO.getSenderId()) : null;
        UserDO receiver = messageDO.getReceiverId() != 0 ? userDataService.getById(messageDO.getReceiverId()) : null;

        MessageDTO messageDTO = messageConverter.toDTO(messageDO);
        messageDTO.setSender(sender != null ? userConverter.toBriefDTO(sender) : null);
        messageDTO.setReceiver(receiver != null ? userConverter.toBriefDTO(receiver) : null);

        return messageDTO;
    }

    private List<MessageDTO> toDTOV1(List<MessageDO> messageDOList) {
        if (messageDOList.isEmpty()) {
            return new ArrayList<>();
        }

        // 收集所有涉及的用户ID
        Set<Long> userIdSet = new HashSet<>();
        // 收集所有涉及的节点ID（从邀请消息中）
        Set<Long> nodeIdSet = new HashSet<>();

        for (MessageDO messageDO : messageDOList) {
            if (messageDO.getSenderId() != 0) {
                userIdSet.add(messageDO.getSenderId());
            }
            if (messageDO.getReceiverId() != 0) {
                userIdSet.add(messageDO.getReceiverId());
            }

            // 解析邀请消息的 nodeId
            if (messageDO.getType() == invite.value() && messageDO.getContent() != null) {
                try {
                    Map<String, Object> contentMap = objectMapper.readValue(
                            messageDO.getContent(),
                            new TypeReference<Map<String, Object>>() {}
                    );
                    Object nodeIdObj = contentMap.get("nodeId");
                    if (nodeIdObj != null) {
                        nodeIdSet.add(((Number) nodeIdObj).longValue());
                    }
                } catch (Exception e) {
                    log.warn("消息服务 邀请消息内容解析失败: {}", messageDO.getContent(), e);
                }
            }
        }

        // 批量获取用户信息
        Map<Long, UserDO> userMap = new HashMap<>();
        if (!userIdSet.isEmpty()) {
            List<UserDO> userDOList = userDataService.getByIds(userIdSet);
            for (UserDO userDO : userDOList) {
                userMap.put(userDO.getId(), userDO);
            }
        }

        // 批量获取节点信息
        Map<Long, NodeDO> nodeMap = new HashMap<>();
        if (!nodeIdSet.isEmpty()) {
            List<NodeDO> nodeDOList = nodeDataService.getByIds(nodeIdSet);
            for (NodeDO nodeDO : nodeDOList) {
                nodeMap.put(nodeDO.getId(), nodeDO);
            }
        }

        // 转换为DTO
        List<MessageDTO> messageDTOList = new ArrayList<>();
        for (MessageDO messageDO : messageDOList) {
            MessageDTO messageDTO = messageConverter.toDTO(messageDO);

            UserDO sender = userMap.get(messageDO.getSenderId());
            UserDO receiver = userMap.get(messageDO.getReceiverId());

            messageDTO.setSender(sender != null ? userConverter.toBriefDTO(sender) : null);
            messageDTO.setReceiver(receiver != null ? userConverter.toBriefDTO(receiver) : null);

            // 填充邀请消息的节点名称
            if (messageDO.getType() == invite.value() && messageDO.getContent() != null) {
                try {
                    Map<String, Object> contentMap = objectMapper.readValue(
                            messageDO.getContent(),
                            new TypeReference<Map<String, Object>>() {}
                    );
                    Object nodeIdObj = contentMap.get("nodeId");
                    if (nodeIdObj != null) {
                        Long nodeId = ((Number) nodeIdObj).longValue();
                        NodeDO node = nodeMap.get(nodeId);
                        if (node != null) {
                            contentMap.put("nodeName", node.getName());
                            messageDTO.setContent(objectMapper.writeValueAsString(contentMap));
                        }
                    }
                } catch (Exception e) {
                    log.warn("消息服务 邀请消息补充信息失败: {}", messageDO.getContent(), e);
                }
            }

            messageDTOList.add(messageDTO);
        }
        return messageDTOList;
    }

    // ========== 审核通知方法 ==========

    /**
     * 发送课程审核通知
     */
    public void sendCourseModeration(long userId, long courseId, String courseName,
                                     ModerationAction action, String reason) {
        messageDomainService.sendCourseModeration(userId, courseId, courseName, action, reason);
    }

    /**
     * 发送帖子审核通知
     */
    public void sendPostModeration(long userId, long postId, String postPreview,
                                   long nodeId, String nodeName, String courseName,
                                   ModerationAction action, String reason) {
        messageDomainService.sendPostModeration(userId, postId, postPreview, nodeId, nodeName, courseName, action, reason);
    }

    /**
     * 发送角色审核通知
     */
    public void sendRoleModeration(long userId, long roleId, String roleName,
                                   ModerationAction action, String reason) {
        messageDomainService.sendRoleModeration(userId, roleId, roleName, action, reason);
    }

    /**
     * 发送路线图审核通知
     */
    public void sendRoadmapModeration(long userId, long roadmapId, long roleId, String roleName,
                                      ModerationAction action, String reason) {
        messageDomainService.sendRoadmapModeration(userId, roadmapId, roleId, roleName, action, reason);
    }

    /**
     * 发送记忆卡片组审核通知
     */
    public void sendMemoryDeckModeration(long userId, long deckId,
                                         long postId, String postTitle,
                                         ModerationAction action, String reason) {
        messageDomainService.sendMemoryDeckModeration(userId, deckId, postId, postTitle, action, reason);
    }

    // ========== Private 辅助方法 ==========

    /**
     * 转换系统消息列表（处理跨域数据聚合）
     */
    private List<MessageDTO> convertSystemMessages(List<MessageDO> messageDOList) {
        Set<Long> userIdSet = new HashSet<>();
        Set<Long> nodeIdSet = new HashSet<>();
        Set<Long> postingIdSet = new HashSet<>();
        Set<Long> roleIdSet = new HashSet<>();
        Set<Long> roadmapIdSet = new HashSet<>();

        for (MessageDO messageDO : messageDOList) {
            userIdSet.add(messageDO.getReceiverId());
            Map<String, Object> map = Utils.readValueToMap(messageDO.getContent());

            if (messageDO.getType() == upvote.value()) {
                Long postingId = Utils.getLong(map, "postingId");
                if (postingId != null) {
                    postingIdSet.add(postingId);
                }
                Long nodeId = Utils.getLong(map, "nodeId");
                if (nodeId != null) {
                    nodeIdSet.add(nodeId);
                }
                Long upvoterId = Utils.getLong(map, "upvoterId");
                if (upvoterId != null) {
                    userIdSet.add(upvoterId);
                }
                // 路线图点赞：收集 voterId 和 roleId
                Long voterId = Utils.getLong(map, "voterId");
                if (voterId != null) {
                    userIdSet.add(voterId);
                }
                Long roleId = Utils.getLong(map, "roleId");
                if (roleId != null) {
                    roleIdSet.add(roleId);
                }
            } else if (messageDO.getType() == invite.value()) {
                nodeIdSet.add(Utils.getLong(map, "nodeId"));
                userIdSet.add(Utils.getLong(map, "InviterId"));
            } else if (messageDO.getType() == follow.value()) {
                userIdSet.add(Utils.getLong(map, "followerId"));
            } else if (messageDO.getType() == postComment.value() || messageDO.getType() == replyPostingComment.value()) {
                // 帖子评论：收集 postId 和 commenterId
                Long postId = Utils.getLong(map, "postId");
                if (postId != null) {
                    postingIdSet.add(postId);
                }
                userIdSet.add(Utils.getLong(map, "commenterId"));
            } else if (messageDO.getType() == nodeComment.value() || messageDO.getType() == replyNodeComment.value()) {
                // 节点评论：收集 nodeId 和 commenterId
                Long nodeId = Utils.getLong(map, "nodeId");
                if (nodeId != null) {
                    nodeIdSet.add(nodeId);
                }
                userIdSet.add(Utils.getLong(map, "commenterId"));
            } else if (messageDO.getType() == roadmapComment.value() || messageDO.getType() == replyRoadmapComment.value()) {
                // 路线图评论：收集 roadmapId
                Long roadmapId = Utils.getLong(map, "roadmapId");
                if (roadmapId != null) {
                    roadmapIdSet.add(roadmapId);
                }
                userIdSet.add(Utils.getLong(map, "commenterId"));
            }
        }

        Map<Long, UserDO> userDOMap = userIdSet.isEmpty() ? new HashMap<>() : userDataService.getMapByIds(userIdSet);
        Map<Long, PostDO> postingDOMap = postingIdSet.isEmpty() ? new HashMap<>() : postDataService.getMapByIds(postingIdSet);
        Map<Long, RoleDO> roleDOMap = roleIdSet.isEmpty() ? new HashMap<>() : roleDataService.getMapByIds(roleIdSet);
        Map<Long, RoadmapDO> roadmapDOMap = roadmapIdSet.isEmpty() ? new HashMap<>() : roadmapDataService.getMapByIds(roadmapIdSet);

        // 从 post 收集 nodeId
        for (PostDO postDO : postingDOMap.values()) {
            nodeIdSet.add(postDO.getNodeId());
        }

        // 从 roadmap 收集 roleId
        for (RoadmapDO roadmapDO : roadmapDOMap.values()) {
            roleIdSet.add(roadmapDO.getRoleId());
        }

        Map<Long, NodeDO> nodeDOMap = nodeIdSet.isEmpty() ? new HashMap<>() : nodeDataService.getMapByIds(nodeIdSet);

        List<MessageDTO> messageDTOList = new ArrayList<>();
        for (MessageDO messageDO : messageDOList) {
            messageDTOList.add(convertMessage(messageDO, userDOMap, postingDOMap, nodeDOMap, roleDOMap, roadmapDOMap));
        }
        return messageDTOList;
    }

    private MessageDTO convertMessage(MessageDO messageDO, Map<Long, UserDO> userDOMap,
                                      Map<Long, PostDO> postingDOMap, Map<Long, NodeDO> nodeDOMap,
                                      Map<Long, RoleDO> roleDOMap, Map<Long, RoadmapDO> roadmapDOMap) {
        MessageDTO messageDTO;
        Map<String, Object> content = Utils.readValueToMap(messageDO.getContent());

        // 处理评论消息
        if (messageDO.getType() == postComment.value() || messageDO.getType() == replyPostingComment.value() ||
            messageDO.getType() == nodeComment.value() || messageDO.getType() == replyNodeComment.value() ||
            messageDO.getType() == roadmapComment.value() || messageDO.getType() == replyRoadmapComment.value()) {

            CommentMessageDTO m = new CommentMessageDTO();
            long commenterId = Utils.getLong(content, "commenterId");
            UserDO commenter = userDOMap.get(commenterId);

            m.setCommentId(Utils.getLong(content, "commentId"));
            m.setCommenter(userConverter.toBriefDTO(commenter));

            // 填充 commenterName 到 content JSON
            Map<String, Object> enrichedContent = new HashMap<>(content);
            if (commenter != null) {
                enrichedContent.put("commenterName", commenter.getName());
            }

            // 根据消息类型，获取对应的上下文信息
            NodeDO nodeDO = null;
            if (messageDO.getType() == nodeComment.value() || messageDO.getType() == replyNodeComment.value()) {
                // 节点评论：直接从 nodeId 获取
                Long nodeId = Utils.getLong(content, "nodeId");
                if (nodeId != null) {
                    nodeDO = nodeDOMap.get(nodeId);
                    if (nodeDO != null) {
                        enrichedContent.put("nodeName", nodeDO.getName());
                    }
                }
            } else if (messageDO.getType() == postComment.value() || messageDO.getType() == replyPostingComment.value()) {
                // 帖子评论：通过 postId 找到 post，再获取 nodeId
                Long postId = Utils.getLong(content, "postId");
                if (postId != null) {
                    PostDO postDO = postingDOMap.get(postId);
                    if (postDO != null) {
                        nodeDO = nodeDOMap.get(postDO.getNodeId());
                        if (nodeDO != null) {
                            enrichedContent.put("nodeName", nodeDO.getName());
                        }
                    }
                }
            } else if (messageDO.getType() == roadmapComment.value() || messageDO.getType() == replyRoadmapComment.value()) {
                // 路线图评论：通过 roadmapId 找到 roadmap，再获取 roleId
                Long roadmapId = Utils.getLong(content, "roadmapId");
                if (roadmapId != null) {
                    RoadmapDO roadmapDO = roadmapDOMap.get(roadmapId);
                    if (roadmapDO != null) {
                        RoleDO roleDO = roleDOMap.get(roadmapDO.getRoleId());
                        if (roleDO != null) {
                            enrichedContent.put("roleName", roleDO.getName());
                        }
                    }
                }
            }

            m.setNode(nodeConverter.toSummaryDTO(nodeDO));
            messageDTO = m;
            messageDTO.setContent(Utils.toJson(enrichedContent));
        } else if (messageDO.getType() == upvote.value()) {
            // 动态填充用户名和上下文名称到 content
            Map<String, Object> enrichedContent = new HashMap<>(content);

            // 填充点赞者用户名
            Long voterId = Utils.getLong(content, "voterId");
            if (voterId != null) {
                UserDO voter = userDOMap.get(voterId);
                if (voter != null) {
                    enrichedContent.put("voterName", voter.getName());
                }
            }
            Long upvoterId = Utils.getLong(content, "upvoterId");
            if (upvoterId != null) {
                UserDO upvoter = userDOMap.get(upvoterId);
                if (upvoter != null) {
                    enrichedContent.put("upvoterName", upvoter.getName());
                }
            }

            // 填充上下文名称
            Long roleId = Utils.getLong(content, "roleId");
            if (roleId != null) {
                RoleDO role = roleDOMap.get(roleId);
                if (role != null) {
                    enrichedContent.put("roleName", role.getName());
                }
            }
            Long nodeId = Utils.getLong(content, "nodeId");
            if (nodeId != null) {
                NodeDO node = nodeDOMap.get(nodeId);
                if (node != null) {
                    enrichedContent.put("nodeName", node.getName());
                }
            }

            UpvoteMessageDTO m = new UpvoteMessageDTO();
            if (content.containsKey("postingId")) {
                long postId = Utils.getLong(content, "postingId");
                m.setObjectId(postId);
                m.setObjectType(ContentType.post.value());
            } else if (content.containsKey("commentId")) {
                long commentId = Utils.getLong(content, "commentId");
                m.setObjectId(commentId);
                m.setObjectType(ContentType.comment.value());
            }

            m.setNode(nodeConverter.toSummaryDTO(nodeDOMap.get(Utils.getLong(content, "nodeId"))));
            m.setVoteType(Utils.getInteger(content, "type"));
            m.setUpvoter(userConverter.toBriefDTO(userDOMap.get(Utils.getLong(content, "upvoterId"))));
            messageDTO = m;

            // 设置填充后的 content
            messageDTO.setContent(Utils.toJson(enrichedContent));
        } else if (messageDO.getType() == follow.value()) {
            FollowMessageDTO m = new FollowMessageDTO();
            m.setFollower(userConverter.toBriefDTO(userDOMap.get(Utils.getLong(content, "followerId"))));
            messageDTO = m;
        } else if (messageDO.getType() == invite.value()) {
            InviteMessageDTO m = new InviteMessageDTO();
            Long inviterId = Utils.getLong(content, "inviterId");
            UserDO inviter = userDOMap.get(inviterId);
            m.setInviter(userConverter.toBriefDTO(inviter));

            Long nodeId = Utils.getLong(content, "nodeId");
            NodeDO node = nodeDOMap.get(nodeId);
            m.setNode(nodeConverter.toSummaryDTO(node));

            // Enrich content with inviterName and nodeName
            Map<String, Object> enrichedContent = new HashMap<>(content);
            if (inviter != null) {
                enrichedContent.put("inviterName", inviter.getName());
            }
            if (node != null) {
                enrichedContent.put("nodeName", node.getName());
            }
            messageDTO = m;
            messageDTO.setContent(Utils.toJson(enrichedContent));
        } else {
            // 其他类型消息(如审核消息)，使用基础 MessageDTO，保留原始 content
            messageDTO = messageConverter.toDTO(messageDO);
        }

        messageDTO.setId(messageDO.getId());
        messageDTO.setSender(null);
        messageDTO.setReceiver(userConverter.toBriefDTO(userDOMap.get(messageDO.getReceiverId())));
        messageDTO.setType(messageDO.getType());
        messageDTO.setCreatedAt(Utils.getTimeString(messageDO.getCreatedAt()));

        return messageDTO;
    }
}