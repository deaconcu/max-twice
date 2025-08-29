package com.prosper.learn.domain.service.basic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prosper.learn.common.Enums;
import com.prosper.learn.common.Enums.MessageType;
import com.prosper.learn.common.Utils;
import com.prosper.learn.common.exception.BusinessException;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.domain.config.SystemProperties;
import com.prosper.learn.domain.util.Converter;
import com.prosper.learn.domain.util.Util;
import com.prosper.learn.dto.message.*;
import com.prosper.learn.persistence.dataobject.CourseDO;
import com.prosper.learn.persistence.dataobject.MessageDO;
import com.prosper.learn.persistence.dataobject.NodeDO;
import com.prosper.learn.persistence.dataobject.PostDO;
import com.prosper.learn.persistence.dataobject.UserDO;
import com.prosper.learn.persistence.mapper.CourseMapper;
import com.prosper.learn.persistence.mapper.MessageMapper;
import com.prosper.learn.persistence.mapper.NodeMapper;
import com.prosper.learn.persistence.mapper.PostMapper;
import com.prosper.learn.persistence.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.prosper.learn.common.Enums.MessageType.*;

/**
 * 消息管理服务
 * 
 * 负责管理系统中的各种消息类型，包括：
 * - 用户之间的私信
 * - 系统通知消息
 * - 课程申请消息
 * - 评论、点赞、关注等系统事件消息
 * 
 * 核心功能：
 * - 消息的创建和查询
 * - 不同类型消息的格式化和转换
 * - 批量消息处理和分页查询
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

    /** 帖子数据访问接口 */
    private final PostMapper postMapper;
    
    /** 节点数据访问接口 */
    private final NodeMapper nodeMapper;
    
    /** 消息数据访问接口 */
    private final MessageMapper messageMapper;
    
    /** 用户数据访问接口 */
    private final UserMapper userMapper;
    
    /** 课程数据访问接口 */
    private final CourseMapper courseMapper;
    
    /** JSON对象映射器，用于消息内容的序列化和反序列化 */
    private final ObjectMapper objectMapper;
    
    /** 系统配置属性 */
    private final SystemProperties systemProperties;

    /**
     * 验证用户存在性
     * 
     * @param userId 用户ID
     * @return 用户实体对象
     * @throws BusinessException 当用户不存在时抛出异常
     */
    private UserDO validateUserExists(long userId) {
        if (userId <= 0) {
            throw ErrorCode.INVALID_PARAMETER.exception("用户ID无效: " + userId);
        }
        UserDO userDO = userMapper.getById(userId);
        if (userDO == null) {
            throw ErrorCode.USER_NOT_FOUND.exception();
        }
        return userDO;
    }

    /**
     * 验证消息存在性
     * 
     * @param messageId 消息ID
     * @return 消息实体对象
     * @throws BusinessException 当消息不存在时抛出异常
     */
    private MessageDO validateMessageExists(long messageId) {
        if (messageId <= 0) {
            throw ErrorCode.INVALID_PARAMETER.exception("消息ID无效: " + messageId);
        }
        MessageDO messageDO = messageMapper.getById(messageId);
        if (messageDO == null) {
            throw ErrorCode.MESSAGE_NOT_FOUND.exception();
        }
        return messageDO;
    }

    /**
     * 验证课程存在性
     * 
     * @param courseId 课程ID
     * @return 课程实体对象
     * @throws BusinessException 当课程不存在时抛出异常
     */
    private CourseDO validateCourseExists(long courseId) {
        if (courseId <= 0) {
            throw ErrorCode.INVALID_PARAMETER.exception("课程ID无效: " + courseId);
        }
        CourseDO courseDO = courseMapper.getById(courseId);
        if (courseDO == null) {
            throw ErrorCode.COURSE_NOT_FOUND.exception();
        }
        return courseDO;
    }

    /**
     * 创建消息
     * 
     * @param content 消息内容
     * @param senderId 发送者ID（系统消息时为0）
     * @param receiverId 接收者ID
     * @param messageType 消息类型
     */
    public void create(String content, long senderId, long receiverId, MessageType messageType) {
        // 验证发送者（系统消息发送者ID为0时跳过验证）
        if (senderId != 0) {
            validateUserExists(senderId);
        }
        
        // 验证接收者
        if (receiverId != 0) {
            validateUserExists(receiverId);
        }

        MessageDO messageDO = new MessageDO();
        messageDO.setContent(content);
        messageDO.setSenderId(senderId);
        messageDO.setType(messageType.value());
        messageDO.setReceiverId(receiverId);
        messageMapper.insert(messageDO);
    }

    /**
     * 获取指定ID的消息详情
     * 
     * @param id 消息ID
     * @return 消息DTO对象
     */
    public MessageDTO get(long id) {
        MessageDO messageDO = validateMessageExists(id);
        
        // 获取发送者和接收者信息（系统消息的发送者可能为null）
        UserDO sender = messageDO.getSenderId() != 0 ? userMapper.getById(messageDO.getSenderId()) : null;
        UserDO receiver = messageDO.getReceiverId() != 0 ? userMapper.getById(messageDO.getReceiverId()) : null;

        MessageDTO messageDTO = Converter.INSTANCE.toMessageDTO(messageDO);
        messageDTO.setSender(sender != null ? Converter.INSTANCE.toUserDTOV4(sender) : null);
        messageDTO.setReceiver(receiver != null ? Converter.INSTANCE.toUserDTOV4(receiver) : null);

        return messageDTO;
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
        List<MessageDO> messageDOList;
        
        if (type == applyCourse.value()) {
            messageDOList = messageMapper.listByPull(type, lastId, DEFAULT_PAGE_SIZE);
        } else if (senderId == 0) {
            messageDOList = messageMapper.listByPull(type, lastId, DEFAULT_PAGE_SIZE);
        } else if (conversation == 0) {
            messageDOList = messageMapper.getListByUser(type, senderId, receiverId, lastId, DEFAULT_PAGE_SIZE);
        } else {
            messageDOList = messageMapper.getConversationByUser(senderId, receiverId, lastId, DEFAULT_PAGE_SIZE);
        }
        
        return convertMessagesToDTO(messageDOList);
    }
    
    /**
     * 将消息DO列表转换为DTO列表的通用方法
     * 
     * @param messageDOList 消息DO列表
     * @return 消息DTO列表
     */
    private List<MessageDTO> convertMessagesToDTO(List<MessageDO> messageDOList) {
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
            List<UserDO> userDOList = userMapper.getByIds(userIdSet);
            for (UserDO userDO : userDOList) {
                userMap.put(userDO.getId(), userDO);
            }
        }

        // 转换为DTO
        List<MessageDTO> messageDTOList = new ArrayList<>();
        for (MessageDO messageDO : messageDOList) {
            MessageDTO messageDTO = Converter.INSTANCE.toMessageDTO(messageDO);
            
            UserDO sender = userMap.get(messageDO.getSenderId());
            UserDO receiver = userMap.get(messageDO.getReceiverId());
            
            messageDTO.setSender(sender != null ? Converter.INSTANCE.toUserDTOV4(sender) : null);
            messageDTO.setReceiver(receiver != null ? Converter.INSTANCE.toUserDTOV4(receiver) : null);
            messageDTOList.add(messageDTO);
        }
        return messageDTOList;
    }

    public List<MessageDTO> getSystemList(int type, long receiverId, long lastId) {
        List<MessageDO> messageDOList;
        if (type == system.value()) {
            messageDOList = messageMapper.getSystemListByUser(receiverId, lastId, 20);
        } else {
            messageDOList = messageMapper.getSystemItemListByUser(type, receiverId, lastId, 20);
        }

        Set<Long> userIdSet = new HashSet<>();
        Set<Long> nodeIdSet = new HashSet<>();
        Set<Long> postingIdSet = new HashSet<>();

        for (MessageDO messageDO : messageDOList) {
            userIdSet.add(messageDO.getReceiverId());
            Map<String, Object> map = Util.readValueToMap(messageDO.getContent());

            if (messageDO.getType() == upvote.value()) {
                if (map.containsKey("postingId")) {
                    postingIdSet.add((Long) map.get("postingId"));
                }
                nodeIdSet.add((Long) map.get("nodeId"));
                userIdSet.add((Long) map.get("upvoterId"));
            } else if (messageDO.getType() == invite.value()) {
                nodeIdSet.add((Long) map.get("nodeId"));
                userIdSet.add((Long) map.get("InviterId"));
            } else if (messageDO.getType() == follow.value()) {
                userIdSet.add((Long) map.get("followerId"));
            } else if (messageDO.getType() == postComment.value() || messageDO.getType() == replyPostingComment.value() ||
                       messageDO.getType() == nodeComment.value() || messageDO.getType() == replyNodeComment.value()) {
                nodeIdSet.add((Long) map.get("nodeId"));
                userIdSet.add((Long) map.get("commenterId"));
            }
        }

        Map<Long, UserDO> userDOMap = userIdSet.size() == 0 ? new HashMap<>() : userMapper.getMapByIds(userIdSet);
        Map<Long, PostDO> postingDOMap = postingIdSet.size() == 0 ? new HashMap<>() : postMapper.getMapByIds(postingIdSet);

        for (PostDO postDO : postingDOMap.values()) {
            nodeIdSet.add(postDO.getNodeId());
        }

        Map<Long, NodeDO> nodeDOMap = nodeIdSet.size() == 0 ? new HashMap<>() : nodeMapper.getMapByIds(nodeIdSet);

        List<MessageDTO> messageDTOList = new ArrayList<>();
        for (MessageDO messageDO : messageDOList) {
            messageDTOList.add(convertMessage(messageDO, userDOMap, postingDOMap, nodeDOMap));
        }
        return messageDTOList;
    }

    private MessageDTO convertMessage(MessageDO messageDO, Map<Long, UserDO> userDOMap,
                                      Map<Long, PostDO> postingDOMap, Map<Long, NodeDO> nodeDOMap) {
        MessageDTO messageDTO = null;
        Map<String, Object> content = Util.readValueToMap(messageDO.getContent());
        if (messageDO.getType() == postComment.value() || messageDO.getType() == replyPostingComment.value() ||
            messageDO.getType() == nodeComment.value() || messageDO.getType() == replyNodeComment.value()) {

            CommentMessageDTO m = new CommentMessageDTO();
            long nodeId = (Long) content.get("nodeId");
            NodeDO nodeDO = nodeDOMap.get(nodeId);

            m.setCommentId((Long)content.get("commentId"));
            m.setNode(Converter.INSTANCE.toNodeDTOV1(nodeDO));
            m.setCommenter(Converter.INSTANCE.toUserDTOV4(userDOMap.get(content.get("commenterId"))));
            messageDTO = m;
        } else if (messageDO.getType() == upvote.value()) {
            UpvoteMessageDTO m = new UpvoteMessageDTO();
            if (content.containsKey("postingId")) {
                long postId = (Long) content.get("postingId");
                m.setObjectId(postId);
                m.setObjectType(Enums.ObjectType.post.value());
            } else if (content.containsKey("commentId")) {
                long commentId = (Long) content.get("commentId");
                m.setObjectId(commentId);
                m.setObjectType(Enums.ObjectType.comment.value());
            }

            m.setNode(Converter.INSTANCE.toNodeDTOV1(nodeDOMap.get((Integer)content.get("nodeId"))));
            m.setVoteType((Integer) content.get("type"));
            m.setUpvoter(Converter.INSTANCE.toUserDTOV4(userDOMap.get(content.get("upvoterId"))));
            messageDTO = m;
        } else if (messageDO.getType() == follow.value()) {
            FollowMessageDTO m = new FollowMessageDTO();
            m.setFollower(Converter.INSTANCE.toUserDTOV4(userDOMap.get(content.get("followerId"))));
            messageDTO = m;
        } else if (messageDO.getType() == invite.value()) {
            InviteMessageDTO m = new InviteMessageDTO();
            m.setInviter(Converter.INSTANCE.toUserDTOV4(userDOMap.get(content.get("inviterId"))));
            int nodeId = (Integer) content.get("nodeId");
            m.setNode(Converter.INSTANCE.toNodeDTOV1(nodeDOMap.get(nodeId)));
            messageDTO = m;
        }

        messageDTO.setId(messageDO.getId());
        messageDTO.setSender(null);
        messageDTO.setReceiver(Converter.INSTANCE.toUserDTOV4(userDOMap.get(messageDO.getReceiverId())));
        messageDTO.setType(messageDO.getType());
        messageDTO.setCreatedAt(Utils.getTimeString(messageDO.getCreatedAt()));
        messageDTO.setIsRead(messageDO.getIsRead());

        return messageDTO;
    }

    public List<MessageDTO> getCourseApplyList(long senderId, long lastId) {
        List<MessageDO> messageDOList = messageMapper.getApplyCourseListByUser(senderId, lastId, 20);

        Set<Long> userIdSet = new HashSet<>();
        for (MessageDO messageDO : messageDOList) {
            userIdSet.add(messageDO.getSenderId());
        }

        Map<Long, UserDO> userMap = new HashMap<>();
        if (userIdSet.size() != 0) {
            List<UserDO> userDOList = userMapper.getByIds(userIdSet);
            for (UserDO userDO : userDOList) {
                userMap.put(userDO.getId(), userDO);
            }
        }

        List<MessageDTO> messageDTOList = new ArrayList<>();
        for (MessageDO messageDO : messageDOList) {
            MessageDTO messageDTO = Converter.INSTANCE.toMessageDTO(messageDO);
            messageDTO.setSender(Converter.INSTANCE.toUserDTOV4(userMap.get(messageDO.getSenderId())));
            messageDTOList.add(messageDTO);
        }
        return messageDTOList;
    }

    public List<MessageDTO> getApplyCourseMessage(int page, int length) {
        if (page < 1) return new ArrayList<>();
        return Converter.INSTANCE.toMessageDTO(messageMapper.getApplyCourseList((page - 1) * length, length));
    }

    public List<MessageDTO> getSystemMessage(int page, int length) {
        if (page < 1) return new ArrayList<>();
        return Converter.INSTANCE.toMessageDTO(messageMapper.getApplyCourseList((page - 1) * length, length));
    }

    public int getApplyCourseCount() {
        return messageMapper.getApplyCourseCount();
    }

    public void createCommentMessage(long recieverId, long commenterId, long nodeId, long commentId, int type) {
        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("commenterId", commenterId);
        messageMap.put("nodeId", nodeId);
        messageMap.put("commentId", commentId);

        createSystemMessage(type, recieverId, Util.toJson(messageMap));
    }

    public void createFollowMessage(long recieverId, long followerId) {
        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("followerId", followerId);

        createSystemMessage(follow.value(), recieverId, Util.toJson(messageMap));
    }

    public void createUpvoteMessage(long recieverId, long voterId, long nodeId, long objectId, int objectType, int type) {
        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("voterId", voterId);
        messageMap.put("type", type);
        messageMap.put("nodeId", nodeId);
        if (objectType == Enums.ObjectType.post.value()) {
            messageMap.put("postingId", objectId);
        } else if (objectType == Enums.ObjectType.comment.value()) {
            messageMap.put("commentId", objectId);
        }

        createSystemMessage(upvote.value(), recieverId, Util.toJson(messageMap));
    }

    public void createInviteMessage(long recieverId, long InviterId, long nodeId) {
        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("inviterId", InviterId);
        messageMap.put("nodeId", nodeId);

        createSystemMessage(invite.value(), recieverId, Util.toJson(messageMap));
    }

    public void createSystemMessage(int type, long userId, String content) {
        MessageDO messageDO = new MessageDO();
        messageDO.setType(type);
        messageDO.setSenderId(0L);
        messageDO.setReceiverId(userId);
        messageDO.setContent(content);
        messageMapper.insert(messageDO);
    }

    public void modifyCourseApply(long messageId, String reply) {
        MessageDO messageDO = messageMapper.getById(messageId);
        if (messageDO == null) throw new RuntimeException("Message not found");

        String content = messageDO.getContent();
        try {
            Map<String, String> map = objectMapper.readValue(content, Map.class);
            map.put("reply", reply);

            content = objectMapper.writeValueAsString(map);
            messageDO.setContent(content);
            messageMapper.update(messageDO);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 申请课程（带完整业务逻辑）
     */
    public void applyCourse(String title, String summary, String explanation, Long parentId, long userId) {
        CourseDO course = null;
        if (parentId != 0) {
            course = courseMapper.getById(parentId);
            if (course == null) {
                throw new RuntimeException("course not found");
            }
        }

        Map<String, String> data = new HashMap<>();
        data.put("title", title);
        data.put("summary", summary);
        data.put("explanation", explanation);
        data.put("parentId", Long.toString(parentId));
        if (course != null) {
            data.put("parentName", course.getName());
        }

        String jsonString = null;
        try {
            jsonString = objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw ErrorCode.SYSTEM_ERROR.exception(e);
        }

        create(jsonString, userId, 0, Enums.MessageType.applyCourse);
    }

    /**
     * 获取课程申请列表（带分页信息）
     */
    public Map<String, Object> getApplyCourseListWithPagination(int page, int length) {
        if (page < 1) page = 1;
        if (length < 1) length = 1;
        if (length > 100) length = 100;
        int count = getApplyCourseCount();
        int totalPage = count / length + 1;
        if (page > totalPage) page = totalPage;

        Map<String, Object> resultMap = new HashMap<>();
        List<MessageDTO> messageDTOList = getApplyCourseMessage(page, length);
        resultMap.put("messages", messageDTOList);

        Map<String, Integer> pagination = new HashMap<>();
        pagination.put("total", count);
        pagination.put("pageSize", length);
        pagination.put("currentPage", page);
        pagination.put("totalPages", totalPage);
        resultMap.put("pagination", pagination);
        return resultMap;
    }
}
