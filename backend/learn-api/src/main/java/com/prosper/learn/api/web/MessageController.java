package com.prosper.learn.api.web;

import cn.dev33.satoken.stp.StpUtil;
import com.prosper.learn.api.client.MessageClient;
import com.prosper.learn.common.Enums;
import com.prosper.learn.common.Utils;
import com.prosper.learn.domain.service.MessageService;
import com.prosper.learn.dto.message.MessageDTO;
import com.prosper.learn.dto.Response;
import com.prosper.learn.persistence.dataobject.UserDO;
import com.prosper.learn.persistence.mapper.MessageMapper;
import com.prosper.learn.persistence.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
//@SaCheckLogin
@Slf4j
@RequiredArgsConstructor
public class MessageController implements MessageClient {

    private final MessageMapper messageMapper;
    private final UserMapper userMapper;
    private final MessageService messageService;

    @Override
    public Response create(String content, Long senderId, Long receiverId) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("消息内容不能为空");
        }
        if (senderId <= 0) {
            throw new IllegalArgumentException("发送者ID无效");
        }
        if (receiverId <= 0) {
            throw new IllegalArgumentException("接收者ID无效");
        }
        
        messageService.create(content, senderId, receiverId, Enums.MessageType.other);
        return Response.success;
    }

    @Override
    public Response<MessageDTO> get(Long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("消息ID无效");
        }
        
        MessageDTO messageDTO = messageService.get(id);
        return new Response<>(messageDTO);
    }

    @Override
    public Response<Map<String, Object>> getSystemList(int type, Long receiverId, Long lastId) {
        if (receiverId <= 0) {
            throw new IllegalArgumentException("接收者ID无效");
        }
        if (lastId < 0) {
            throw new IllegalArgumentException("lastId不能为负数");
        }
        
        List<MessageDTO> messageDTOList = messageService.getSystemList(type, receiverId, lastId);

        int userId = StpUtil.getLoginIdAsInt();
        UserDO userDO = userMapper.getById(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("messages", messageDTOList);
        result.put("lastReadTime", Utils.getTimeString(userDO.getMsgReadTime()));

        userDO.setMsgReadTime(Utils.getLocalDateTime());
        userMapper.update(userDO);
        return new Response<>(result);
    }

    @Override
    public Response<Map<String, Object>> getCourseApplyList(Long senderId, Long lastId) {
        if (senderId <= 0) {
            throw new IllegalArgumentException("发送者ID无效");
        }
        if (lastId < 0) {
            throw new IllegalArgumentException("lastId不能为负数");
        }
        
        List<MessageDTO> messageDTOList = messageService.getCourseApplyList(senderId, lastId);
        Map<String, Object> result = new HashMap<>();
        result.put("messages", messageDTOList);

        return new Response<>(result);
    }

    @Override
    public Response Invite(Long userId, Long nodeId) {
        if (userId <= 0) {
            throw new IllegalArgumentException("用户ID无效");
        }
        if (nodeId <= 0) {
            throw new IllegalArgumentException("节点ID无效");
        }
        
        UserDO userDO = userMapper.getById(userId);
        if (userDO == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        int inviterId = StpUtil.getLoginIdAsInt();

        messageService.createInviteMessage(userId, inviterId, nodeId);
        return Response.success;
    }

    public Response<List<MessageDTO>> getList(int type, int senderId, int receiverId, int lastId, int conversation) {
        if (senderId <= 0) {
            throw new IllegalArgumentException("发送者ID无效");
        }
        if (receiverId <= 0) {
            throw new IllegalArgumentException("接收者ID无效");
        }
        if (lastId < 0) {
            throw new IllegalArgumentException("lastId不能为负数");
        }
        
        List<MessageDTO> messageDTOList = messageService.getList(type, senderId, receiverId, lastId, conversation);
        return new Response<>(messageDTOList);
    }
}
