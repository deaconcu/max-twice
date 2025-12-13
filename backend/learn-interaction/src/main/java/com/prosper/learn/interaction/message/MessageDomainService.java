package com.prosper.learn.interaction.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prosper.learn.shared.common.utils.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.prosper.learn.shared.domain.Enums.*;

/**
 * 消息领域服务
 *
 * 负责消息领域内的核心业务逻辑，只依赖interaction领域模块
 * 处理消息的创建、查询等核心功能
 *
 * @author Claude
 * @since 2024-12-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageDomainService {

    private final MessageDataService messageDataService;
    private final ObjectMapper objectMapper;

    // ========== Command 方法（写操作）==========

    /**
     * 创建消息（核心领域逻辑）
     *
     * @param content 消息内容
     * @param senderId 发送者ID（系统消息时为0）
     * @param receiverId 接收者ID
     * @param type 消息类型值
     * @param category 消息分类
     * @return 创建的消息对象
     */
    public MessageDO createMessage(String content, long senderId, long receiverId, int type, int category) {
        MessageDO messageDO = new MessageDO();
        messageDO.setSenderId(senderId);
        messageDO.setReceiverId(receiverId);
        messageDO.setContent(content);
        messageDO.setType(type);
        messageDO.setCategory(category);

        messageDataService.insert(messageDO);
        log.debug("Created message: senderId={}, receiverId={}, type={}", senderId, receiverId, type);

        return messageDO;
    }

    /**
     * 创建系统消息（核心领域逻辑）
     *
     * @param type 消息类型值
     * @param userId 用户ID
     * @param content 消息内容
     * @return 创建的消息对象
     */
    public MessageDO createSystemMessage(int type, long userId, String content) {
        // 自动设置 category
        MessageType messageType = MessageType.getByValue(type);
        int category = messageType != null ? messageType.getCategory() : 2; // 默认系统消息

        return createMessage(content, 0L, userId, type, category);
    }

    /**
     * 创建评论消息
     */
    public void createCommentMessage(long receiverId, long commenterId, long nodeId, long commentId, int type) {
        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("commenterId", commenterId);
        messageMap.put("nodeId", nodeId);
        messageMap.put("commentId", commentId);

        createSystemMessage(type, receiverId, Utils.toJson(messageMap));
    }

    /**
     * 创建关注消息
     */
    public void createFollowMessage(long receiverId, long followerId) {
        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("followerId", followerId);

        createSystemMessage(MessageType.follow.value(), receiverId, Utils.toJson(messageMap));
    }

    /**
     * 创建帖子点赞消息
     */
    public void createPostUpvoteMessage(long receiverId, long voterId, long nodeId, long postId, VoteType voteType) {
        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("voterId", voterId);
        messageMap.put("voteType", voteType.toString());
        messageMap.put("nodeId", nodeId);
        messageMap.put("postId", postId);
        messageMap.put("contentType", "post");

        createSystemMessage(MessageType.upvote.value(), receiverId, Utils.toJson(messageMap));
    }

    /**
     * 创建评论点赞消息
     */
    public void createCommentUpvoteMessage(long receiverId, long voterId, long nodeId, long commentId) {
        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("voterId", voterId);
        messageMap.put("nodeId", nodeId);
        messageMap.put("commentId", commentId);
        messageMap.put("contentType", "comment");

        createSystemMessage(MessageType.upvote.value(), receiverId, Utils.toJson(messageMap));
    }

    /**
     * 创建路线图点赞消息
     */
    public void createRoadmapUpvoteMessage(long receiverId, long voterId, long professionId, long roadmapId) {
        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("voterId", voterId);
        messageMap.put("professionId", professionId);
        messageMap.put("roadmapId", roadmapId);
        messageMap.put("contentType", "roadmap");

        createSystemMessage(MessageType.upvote.value(), receiverId, Utils.toJson(messageMap));
    }

    /**
     * 创建记忆卡片组点赞消息
     */
    public void createMemoryDeckUpvoteMessage(long receiverId, long voterId, long nodeId, long deckId) {
        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("voterId", voterId);
        messageMap.put("nodeId", nodeId);
        messageMap.put("deckId", deckId);
        messageMap.put("contentType", "memory_deck");

        createSystemMessage(MessageType.upvote.value(), receiverId, Utils.toJson(messageMap));
    }

    /**
     * 创建邀请消息
     */
    public void createInviteMessage(long receiverId, long inviterId, long nodeId) {
        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("inviterId", inviterId);
        messageMap.put("nodeId", nodeId);

        createSystemMessage(MessageType.invite.value(), receiverId, Utils.toJson(messageMap));
    }

    /**
     * 更新消息
     *
     * @param message 要更新的消息
     */
    public void updateMessage(MessageDO message) {
        messageDataService.update(message);
        log.debug("Updated message: {}", message.getId());
    }

    /**
     * 修改课程申请回复
     */
    public void modifyCourseApply(long messageId, String reply) {
        MessageDO messageDO = getById(messageId);

        String content = messageDO.getContent();
        try {
            Map<String, String> map = objectMapper.readValue(content, Map.class);
            map.put("reply", reply);

            content = objectMapper.writeValueAsString(map);
            messageDO.setContent(content);
            updateMessage(messageDO);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    // ========== 审核通知方法 ==========

    /**
     * 发送课程审核通知
     */
    public void sendCourseModeration(long userId, long courseId, String courseName,
                                     ModerationAction action, String reason) {
        Map<String, Object> data = new HashMap<>();
        data.put("courseId", courseId);
        data.put("courseName", courseName);

        int type;
        switch (action) {
            case APPROVED -> {
                data.put("linkUrl", "/read?courseId=" + courseId);
                type = MessageType.courseApproved.value();
            }
            case REJECTED -> {
                data.put("reason", reason != null ? reason : "");
                type = MessageType.courseRejected.value();
            }
            case BANNED -> {
                data.put("reason", reason != null ? reason : "");
                type = MessageType.courseBanned.value();
            }
            default -> throw new RuntimeException("无效的审核操作: " + action);
        }

        createSystemMessage(type, userId, Utils.toJson(data));
    }

    /**
     * 发送帖子审核通知
     */
    public void sendPostModeration(long userId, long postId, String postPreview,
                                   long nodeId, String nodeName, String courseName,
                                   ModerationAction action, String reason) {
        Map<String, Object> data = new HashMap<>();
        data.put("postId", postId);
        data.put("postPreview", Utils.stripFormatting(postPreview));
        data.put("nodeId", nodeId);
        data.put("nodeName", nodeName);
        data.put("courseName", courseName);
        data.put("reason", reason != null ? reason : "");
        data.put("linkUrl", "/self?tab=posts");

        int type = switch (action) {
            case REJECTED -> MessageType.postRejected.value();
            case BANNED -> MessageType.postBanned.value();
            default -> throw new RuntimeException("帖子审核操作只支持 REJECTED 和 BANNED");
        };

        createSystemMessage(type, userId, Utils.toJson(data));
    }

    /**
     * 发送评论审核通知
     */
    public void sendCommentModeration(long userId, long commentId, String commentPreview,
                                      String objectType, long objectId, String objectTitle,
                                      ModerationAction action, String reason) {
        Map<String, Object> data = new HashMap<>();
        data.put("commentId", commentId);
        data.put("commentPreview", Utils.stripFormatting(commentPreview));
        data.put("objectType", objectType);
        data.put("objectId", objectId);
        data.put("objectTitle", objectTitle);
        data.put("reason", reason != null ? reason : "");

        int type = switch (action) {
            case REJECTED -> MessageType.commentRejected.value();
            case BANNED -> MessageType.commentBanned.value();
            default -> throw new RuntimeException("评论审核操作只支持 REJECTED 和 BANNED");
        };

        createSystemMessage(type, userId, Utils.toJson(data));
    }

    /**
     * 发送职业审核通知
     */
    public void sendProfessionModeration(long userId, long professionId, String professionName,
                                         ModerationAction action, String reason) {
        Map<String, Object> data = new HashMap<>();
        data.put("professionId", professionId);
        data.put("professionName", professionName);

        int type;
        switch (action) {
            case APPROVED -> {
                data.put("linkUrl", "/roadmap/" + professionId);
                type = MessageType.professionApproved.value();
            }
            case REJECTED -> {
                data.put("reason", reason != null ? reason : "");
                type = MessageType.professionRejected.value();
            }
            case BANNED -> {
                data.put("reason", reason != null ? reason : "");
                type = MessageType.professionBanned.value();
            }
            default -> throw new RuntimeException("无效的审核操作: " + action);
        }

        createSystemMessage(type, userId, Utils.toJson(data));
    }

    /**
     * 发送路线图审核通知
     */
    public void sendRoadmapModeration(long userId, long roadmapId, long professionId,
                                      String professionName, ModerationAction action, String reason) {
        Map<String, Object> data = new HashMap<>();
        data.put("roadmapId", roadmapId);
        data.put("professionId", professionId);
        data.put("professionName", professionName);
        data.put("reason", reason != null ? reason : "");
        data.put("linkUrl", "/self?tab=roadmaps");

        int type = switch (action) {
            case REJECTED -> MessageType.roadmapRejected.value();
            case BANNED -> MessageType.roadmapBanned.value();
            default -> throw new RuntimeException("路线图审核操作只支持 REJECTED 和 BANNED");
        };

        createSystemMessage(type, userId, Utils.toJson(data));
    }

    /**
     * 发送记忆卡片组审核通知
     */
    public void sendMemoryDeckModeration(long userId, long deckId, String deckTitle,
                                         long postId, String postTitle,
                                         ModerationAction action, String reason) {
        Map<String, Object> data = new HashMap<>();
        data.put("deckId", deckId);
        data.put("deckTitle", deckTitle);
        data.put("postId", postId);
        data.put("postTitle", postTitle);
        data.put("reason", reason != null ? reason : "");
        data.put("linkUrl", "/self?tab=memory-decks");

        int type = switch (action) {
            case REJECTED -> MessageType.memoryDeckRejected.value();
            case BANNED -> MessageType.memoryDeckBanned.value();
            default -> throw new RuntimeException("记忆卡片组审核操作只支持 REJECTED 和 BANNED");
        };

        createSystemMessage(type, userId, Utils.toJson(data));
    }

    /**
     * 发送节点审核通知
     */
    public void sendNodeModeration(long userId, long nodeId, String nodeName,
                                   long courseId, String courseName,
                                   ModerationAction action, String reason) {
        Map<String, Object> data = new HashMap<>();
        data.put("nodeId", nodeId);
        data.put("nodeName", nodeName);
        data.put("courseId", courseId);
        data.put("courseName", courseName);
        data.put("reason", reason != null ? reason : "");

        int type = switch (action) {
            case REJECTED -> MessageType.nodeRejected.value();
            case BANNED -> MessageType.nodeBanned.value();
            default -> throw new RuntimeException("节点审核操作只支持 REJECTED 和 BANNED");
        };

        createSystemMessage(type, userId, Utils.toJson(data));
    }

    // ========== Query 方法（读操作）==========

    /**
     * 获取消息列表（核心查询逻辑）
     *
     * @param type 消息类型
     * @param senderId 发送者ID
     * @param receiverId 接收者ID
     * @param lastId 最后一条消息ID
     * @param pageSize 分页大小
     * @param conversation 是否为对话模式
     * @return 消息DO列表
     */
    public List<MessageDO> getMessagesList(int type, long senderId, long receiverId,
                                         long lastId, int pageSize, int conversation) {
        if (type == MessageType.applyCourse.value()) {
            return messageDataService.listByPull(type, lastId, pageSize);
        } else if (senderId == 0) {
            return messageDataService.listByPull(type, lastId, pageSize);
        } else if (conversation == 0) {
            return messageDataService.getListByUser(type, senderId, receiverId, lastId, pageSize);
        } else {
            return messageDataService.getConversationByUser(senderId, receiverId, lastId, pageSize);
        }
    }

    /**
     * 获取系统消息列表（核心查询逻辑）
     *
     * @param type 消息类型
     * @param receiverId 接收者ID
     * @param lastId 最后一条消息ID
     * @param pageSize 分页大小
     * @return 消息DO列表
     */
    public List<MessageDO> getSystemMessagesList(int type, long receiverId, long lastId, int pageSize) {
        if (type == MessageType.system.value()) {
            return messageDataService.getSystemListByUser(receiverId, lastId, pageSize);
        } else {
            return messageDataService.getSystemItemListByUser(type, receiverId, lastId, pageSize);
        }
    }

    /**
     * 按分类获取消息列表（核心查询逻辑）
     *
     * @param receiverId 接收者ID
     * @param category 消息分类
     * @param lastId 最后一条消息ID
     * @param pageSize 分页大小
     * @param type 可选的消息类型过滤
     * @return 消息DO列表
     */
    public List<MessageDO> getByCategory(long receiverId, int category, Long lastId, int pageSize, Integer type) {
        if (type != null && type > 0) {
            // 按类型查询
            return messageDataService.listByType(type, receiverId, lastId, pageSize);
        } else {
            // 按分类查询
            return messageDataService.listByCategory(receiverId, category, lastId, pageSize);
        }
    }

    /**
     * 根据ID获取消息
     *
     * @param id 消息ID
     * @return 消息DO对象
     */
    public MessageDO getById(long id) {
        return messageDataService.validateAndGet(id);
    }
}