package com.prosper.learn.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prosper.learn.application.converter.MessageConverter;
import com.prosper.learn.application.converter.NodeConverter;
import com.prosper.learn.application.converter.UserConverter;
import com.prosper.learn.application.dto.response.message.*;
import com.prosper.learn.content.course.CourseDO;
import com.prosper.learn.content.course.CourseDataService;
import com.prosper.learn.content.node.NodeDO;
import com.prosper.learn.content.node.NodeDataService;
import com.prosper.learn.content.post.PostDO;
import com.prosper.learn.content.post.PostDataService;
import com.prosper.learn.interaction.message.MessageDO;
import com.prosper.learn.interaction.message.MessageDataService;
import com.prosper.learn.interaction.message.MessageDomainService;
import com.prosper.learn.shared.common.utils.Utils;
import com.prosper.learn.shared.domain.exception.StatusCode;
import com.prosper.learn.shared.infrastructure.config.SystemProperties;
import com.prosper.learn.user.profile.UserDO;
import com.prosper.learn.user.profile.UserDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.prosper.learn.shared.domain.Enums.*;
import static com.prosper.learn.shared.domain.Enums.MessageType.*;

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
    private final MessageDataService messageDataService;
    private final UserDataService userDataService;
    private final CourseDataService courseDataService;

    // 转换器依赖
    private final MessageConverter messageConverter;
    private final UserConverter userConverter;
    private final NodeConverter nodeConverter;
    private final ObjectMapper objectMapper;

    /** 系统配置属性 */
    private final SystemProperties systemProperties;

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
    public void createRoadmapUpvoteMessage(long receiverId, long voterId, long professionId, long roadmapId) {
        messageDomainService.createRoadmapUpvoteMessage(receiverId, voterId, professionId, roadmapId);
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

    /**
     * 修改课程申请回复
     */
    public void modifyCourseApply(long messageId, String reply) {
        messageDomainService.modifyCourseApply(messageId, reply);
    }

    /**
     * 申请课程（带完整业务逻辑）
     */
    public void applyCourse(String title, String summary, String explanation, Long parentId, long userId) {
        CourseDO course = null;
        if (parentId != 0) {
            course = courseDataService.validateAndGet(parentId);
        }

        Map<String, String> data = new HashMap<>();
        data.put("title", title);
        data.put("summary", summary);
        data.put("explanation", explanation);
        data.put("parentId", Long.toString(parentId));
        if (course != null) {
            data.put("parentName", course.getName());
        }

        String jsonString;
        try {
            jsonString = objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw StatusCode.SYSTEM_ERROR.exception(e);
        }

        create(jsonString, userId, 0, applyCourse);
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
     * 获取消息列表
     *
     * @param type 消息类型
     * @param senderId 发送者ID
     * @param receiverId 接收者ID
     * @param lastId 最后一条消息ID
     * @param conversation 是否为对话模式
     * @return 消息DTO列表
     */
    public List<MessageDTO> getList(int type, long senderId, long receiverId, long lastId, int conversation) {
        // 委托给领域服务获取数据
        List<MessageDO> messageDOList = messageDomainService.getMessagesList(
                type, senderId, receiverId, lastId, DEFAULT_PAGE_SIZE, conversation);

        // DTO转换
        return toDTOV1(messageDOList);
    }

    /**
     * 获取系统消息列表
     *
     * @param type 消息类型
     * @param receiverId 接收者ID
     * @param lastId 最后一条消息ID
     * @return 消息DTO列表
     */
    public List<MessageDTO> getSystemList(int type, long receiverId, long lastId) {
        // 委托给领域服务获取数据
        List<MessageDO> messageDOList = messageDomainService.getSystemMessagesList(type, receiverId, lastId, 20);

        // 跨域数据聚合和DTO转换
        return convertSystemMessages(messageDOList);
    }

// --注释掉检查 START (2025/12/10 11:16):
//    /**
//     * 获取系统消息
//     */
//    public List<MessageDTO> getSystemMessage(int page, int length) {
//        if (page < 1) return new ArrayList<>();
//
//        List<MessageDO> messageDOList = messageDomainService.getApplyCourseMessages((page - 1) * length, length);
//        return messageConverter.toDTO(messageDOList);
//    }
// --注释掉检查 STOP (2025/12/10 11:16)

// --注释掉检查 START (2025/12/10 11:16):
//    /**
//     * 获取课程申请列表（带分页信息）
//     */
//    public Map<String, Object> getApplyCourseListWithPagination(int page, int length) {
//        if (page < 1) page = 1;
//        if (length < 1) length = 1;
//        if (length > 100) length = 100;
//        int count = (int) getApplyCourseCount();
//        int totalPage = count / length + 1;
//        if (page > totalPage) page = totalPage;
//
//        Map<String, Object> resultMap = new HashMap<>();
//        List<MessageDTO> messageDTOList = getApplyCourseMessage(page, length);
//        resultMap.put("messages", messageDTOList);
//
//        Map<String, Integer> pagination = new HashMap<>();
//        pagination.put("total", count);
//        pagination.put("pageSize", length);
//        pagination.put("currentPage", page);
//        pagination.put("totalPages", totalPage);
//        resultMap.put("pagination", pagination);
//        return resultMap;
//    }
// --注释掉检查 STOP (2025/12/10 11:16)

    /**
     * 按分类获取消息列表
     */
    public List<MessageDTO> getListByCategory(int category, long receiverId, Long lastId, Integer type) {
        // 参数校验
        if (category < 1 || category > 3) {
            throw StatusCode.INVALID_PARAMETER.exception("消息分类必须为1-3");
        }

        // 委托给领域服务获取数据
        List<MessageDO> messageDOList = messageDomainService.getByCategory(receiverId, category, lastId, 20, type);

        // 跨域数据聚合和DTO转换
        return convertSystemMessages(messageDOList);
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
        messageDTO.setSender(sender != null ? userConverter.toDTOV2(sender) : null);
        messageDTO.setReceiver(receiver != null ? userConverter.toDTOV2(receiver) : null);

        return messageDTO;
    }

    private List<MessageDTO> toDTOV1(List<MessageDO> messageDOList) {
        if (messageDOList.isEmpty()) {
            return new ArrayList<>();
        }

        // 收集所有涉及的用户ID
        Set<Long> userIdSet = new HashSet<>();
        for (MessageDO messageDO : messageDOList) {
            if (messageDO.getSenderId() != 0) {
                userIdSet.add(messageDO.getSenderId());
            }
            if (messageDO.getReceiverId() != 0) {
                userIdSet.add(messageDO.getReceiverId());
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

        // 转换为DTO
        List<MessageDTO> messageDTOList = new ArrayList<>();
        for (MessageDO messageDO : messageDOList) {
            MessageDTO messageDTO = messageConverter.toDTO(messageDO);

            UserDO sender = userMap.get(messageDO.getSenderId());
            UserDO receiver = userMap.get(messageDO.getReceiverId());

            messageDTO.setSender(sender != null ? userConverter.toDTOV2(sender) : null);
            messageDTO.setReceiver(receiver != null ? userConverter.toDTOV2(receiver) : null);
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

// --注释掉检查 START (2025/12/10 11:16):
//    /**
//     * 发送评论审核通知
//     */
//    public void sendCommentModeration(long userId, long commentId, String commentPreview,
//                                      String objectType, long objectId, String objectTitle,
//                                      ModerationAction action, String reason) {
//        messageDomainService.sendCommentModeration(userId, commentId, commentPreview, objectType, objectId, objectTitle, action, reason);
//    }
// --注释掉检查 STOP (2025/12/10 11:16)

    /**
     * 发送职业审核通知
     */
    public void sendProfessionModeration(long userId, long professionId, String professionName,
                                         ModerationAction action, String reason) {
        messageDomainService.sendProfessionModeration(userId, professionId, professionName, action, reason);
    }

    /**
     * 发送路线图审核通知
     */
    public void sendRoadmapModeration(long userId, long roadmapId, long professionId,
                                      String professionName, ModerationAction action, String reason) {
        messageDomainService.sendRoadmapModeration(userId, roadmapId, professionId, professionName, action, reason);
    }

    /**
     * 发送记忆卡片组审核通知
     */
    public void sendMemoryDeckModeration(long userId, long deckId, String deckTitle,
                                         long postId, String postTitle,
                                         ModerationAction action, String reason) {
        messageDomainService.sendMemoryDeckModeration(userId, deckId, deckTitle, postId, postTitle, action, reason);
    }

// --注释掉检查 START (2025/12/10 11:16):
//    /**
//     * 发送节点审核通知
//     */
//    public void sendNodeModeration(long userId, long nodeId, String nodeName,
//                                   long courseId, String courseName,
//                                   ModerationAction action, String reason) {
//        messageDomainService.sendNodeModeration(userId, nodeId, nodeName, courseId, courseName, action, reason);
//    }
// --注释掉检查 STOP (2025/12/10 11:16)

    // ========== Private 辅助方法 ==========

    /**
     * 转换系统消息列表（处理跨域数据聚合）
     */
    private List<MessageDTO> convertSystemMessages(List<MessageDO> messageDOList) {
        Set<Long> userIdSet = new HashSet<>();
        Set<Long> nodeIdSet = new HashSet<>();
        Set<Long> postingIdSet = new HashSet<>();

        for (MessageDO messageDO : messageDOList) {
            userIdSet.add(messageDO.getReceiverId());
            Map<String, Object> map = Utils.readValueToMap(messageDO.getContent());

            if (messageDO.getType() == upvote.value()) {
                Long postingId = Utils.getLong(map, "postingId");
                if (postingId != null) {
                    postingIdSet.add(postingId);
                }
                nodeIdSet.add(Utils.getLong(map, "nodeId"));
                userIdSet.add(Utils.getLong(map, "upvoterId"));
            } else if (messageDO.getType() == invite.value()) {
                nodeIdSet.add(Utils.getLong(map, "nodeId"));
                userIdSet.add(Utils.getLong(map, "InviterId"));
            } else if (messageDO.getType() == follow.value()) {
                userIdSet.add(Utils.getLong(map, "followerId"));
            } else if (messageDO.getType() == postComment.value() || messageDO.getType() == replyPostingComment.value() ||
                       messageDO.getType() == nodeComment.value() || messageDO.getType() == replyNodeComment.value()) {
                nodeIdSet.add(Utils.getLong(map, "nodeId"));
                userIdSet.add(Utils.getLong(map, "commenterId"));
            }
        }

        Map<Long, UserDO> userDOMap = userIdSet.isEmpty() ? new HashMap<>() : userDataService.getMapByIds(userIdSet);
        Map<Long, PostDO> postingDOMap = postingIdSet.isEmpty() ? new HashMap<>() : postDataService.getMapByIds(postingIdSet);

        for (PostDO postDO : postingDOMap.values()) {
            nodeIdSet.add(postDO.getNodeId());
        }

        Map<Long, NodeDO> nodeDOMap = nodeIdSet.isEmpty() ? new HashMap<>() : nodeDataService.getMapByIds(nodeIdSet);

        List<MessageDTO> messageDTOList = new ArrayList<>();
        for (MessageDO messageDO : messageDOList) {
            messageDTOList.add(convertMessage(messageDO, userDOMap, postingDOMap, nodeDOMap));
        }
        return messageDTOList;
    }

    private MessageDTO convertMessage(MessageDO messageDO, Map<Long, UserDO> userDOMap,
                                      Map<Long, PostDO> postingDOMap, Map<Long, NodeDO> nodeDOMap) {
        MessageDTO messageDTO;
        Map<String, Object> content = Utils.readValueToMap(messageDO.getContent());
        if (messageDO.getType() == postComment.value() || messageDO.getType() == replyPostingComment.value() ||
            messageDO.getType() == nodeComment.value() || messageDO.getType() == replyNodeComment.value()) {

            CommentMessageDTO m = new CommentMessageDTO();
            long nodeId = Utils.getLong(content, "nodeId");
            NodeDO nodeDO = nodeDOMap.get(nodeId);

            m.setCommentId(Utils.getLong(content, "commentId"));
            m.setNode(nodeConverter.toSummaryDTO(nodeDO));
            m.setCommenter(userConverter.toDTOV2(userDOMap.get(Utils.getLong(content, "commenterId"))));
            messageDTO = m;
        } else if (messageDO.getType() == upvote.value()) {
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
            m.setUpvoter(userConverter.toDTOV2(userDOMap.get(Utils.getLong(content, "upvoterId"))));
            messageDTO = m;
        } else if (messageDO.getType() == follow.value()) {
            FollowMessageDTO m = new FollowMessageDTO();
            m.setFollower(userConverter.toDTOV2(userDOMap.get(Utils.getLong(content, "followerId"))));
            messageDTO = m;
        } else if (messageDO.getType() == invite.value()) {
            InviteMessageDTO m = new InviteMessageDTO();
            m.setInviter(userConverter.toDTOV2(userDOMap.get(Utils.getLong(content, "inviterId"))));
            long nodeId = Utils.getLong(content, "nodeId");
            m.setNode(nodeConverter.toSummaryDTO(nodeDOMap.get(nodeId)));
            messageDTO = m;
        } else {
            // 其他类型消息(如审核消息)，使用基础 MessageDTO，保留原始 content
            messageDTO = messageConverter.toDTO(messageDO);
        }

        messageDTO.setId(messageDO.getId());
        messageDTO.setSender(null);
        messageDTO.setReceiver(userConverter.toDTOV2(userDOMap.get(messageDO.getReceiverId())));
        messageDTO.setType(messageDO.getType());
        messageDTO.setCreatedAt(Utils.getTimeString(messageDO.getCreatedAt()));

        return messageDTO;
    }
}