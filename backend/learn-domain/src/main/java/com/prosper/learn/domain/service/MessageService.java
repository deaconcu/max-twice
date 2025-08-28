package com.prosper.learn.domain.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prosper.learn.common.Enums;
import com.prosper.learn.common.Enums.MessageType;
import com.prosper.learn.common.Utils;
import com.prosper.learn.domain.util.Converter;
import com.prosper.learn.domain.util.Util;
import com.prosper.learn.dto.message.*;
import com.prosper.learn.persistence.dataobject.MessageDO;
import com.prosper.learn.persistence.dataobject.NodeDO;
import com.prosper.learn.persistence.dataobject.PostDO;
import com.prosper.learn.persistence.dataobject.UserDO;
import com.prosper.learn.persistence.mapper.MessageMapper;
import com.prosper.learn.persistence.mapper.NodeMapper;
import com.prosper.learn.persistence.mapper.PostMapper;
import com.prosper.learn.persistence.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.prosper.learn.common.Enums.MessageType.*;

@Service
@RequiredArgsConstructor
public class MessageService {

    private static Map<Integer, String> systemMessages = new HashMap<>();

    static {
        systemMessages.put(1, "你的课程申请请求被拒绝了");
    }

    private final PostMapper postMapper;
    private final NodeMapper nodeMapper;
    private final MessageMapper messageMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    public void create(String content, long senderId, long receiverId, MessageType messageType) {
        UserDO sender = userMapper.getById(senderId);
        if (sender == null) {
            throw new RuntimeException("Sender not found");
        }

        if (receiverId != 0) {
            UserDO receiver = userMapper.getById(receiverId);
            if (receiver == null) {
                throw new RuntimeException("Receiver not found");
            }
        }

        MessageDO messageDO = new MessageDO();
        messageDO.setContent(content);
        messageDO.setSenderId(senderId);
        messageDO.setType(messageType.value());
        messageDO.setReceiverId(receiverId);
        messageMapper.insert(messageDO);
    }

    public MessageDTO get(long id) {
        MessageDO messageDO = messageMapper.getById(id);
        UserDO sender = userMapper.getById(messageDO.getSenderId());
        UserDO receiver = userMapper.getById(messageDO.getReceiverId());

        MessageDTO messageDTO = Converter.INSTANCE.toMessageDTO(messageDO);
        messageDTO.setSender(Converter.INSTANCE.toUserDTOV4(sender));
        messageDTO.setReceiver(Converter.INSTANCE.toUserDTOV4(receiver));

        return messageDTO;
    }

    public List<MessageDTO> getList(int type, long senderId, long receiverId, long lastId, int conversation) {
        List<MessageDO> messageDOList;
        if (type == applyCourse.value()) {
            messageDOList = messageMapper.listByPull(type, lastId, 20);
        }
        if (senderId == 0) {
            messageDOList = messageMapper.listByPull(type, lastId, 20);
        } else if (conversation == 0) {
            messageDOList = messageMapper.getListByUser(type, senderId, receiverId, lastId, 20);
        } else {
            messageDOList = messageMapper.getConversationByUser(senderId, receiverId, lastId, 20);
        }

        Set<Long> userIdSet = new HashSet<>();
        for (MessageDO messageDO : messageDOList) {
            userIdSet.add(messageDO.getSenderId());
            userIdSet.add(messageDO.getReceiverId());
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
            messageDTO.setReceiver(Converter.INSTANCE.toUserDTOV4(userMap.get(messageDO.getReceiverId())));
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

}
