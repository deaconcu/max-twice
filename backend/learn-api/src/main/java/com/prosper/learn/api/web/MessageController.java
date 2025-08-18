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
    public Response create(String content, int senderId, int receiverId) {
        messageService.create(content, senderId, receiverId, Enums.MessageType.other);
        return Response.success;
    }

    @Override
    public Response<MessageDTO> get(int id) {
        MessageDTO messageDTO = messageService.get(id);
        return new Response<>(messageDTO);
    }

    @Override
    public Response<Map<String, Object>> getSystemList(int type, int receiverId, int lastId) {
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
    public Response<Map<String, Object>> getCourseApplyList(int senderId, int lastId) {
        List<MessageDTO> messageDTOList = messageService.getCourseApplyList(senderId, lastId);
        Map<String, Object> result = new HashMap<>();
        result.put("messages", messageDTOList);

        return new Response<>(result);
    }

    @Override
    public Response Invite(int userId, int nodeId) {
        UserDO userDO = userMapper.getById(userId);
        if (userDO == null) return Response.badRequest;

        int inviterId = StpUtil.getLoginIdAsInt();

        messageService.createInviteMessage(userId, inviterId, nodeId);
        return Response.success;
    }

    public Response<List<MessageDTO>> getList(int type, int senderId, int receiverId, int lastId, int conversation) {
        List<MessageDTO> messageDTOList = messageService.getList(type, senderId, receiverId, lastId, conversation);
        return new Response<>(messageDTOList);
    }
}
