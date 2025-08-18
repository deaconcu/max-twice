package com.prosper.learn.api.client;

import com.prosper.learn.dto.message.MessageDTO;
import com.prosper.learn.dto.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(value = "learn-service", contextId = "courseRequest")
public interface MessageClient {

    //@PostMapping("/message")
    Response create(@RequestParam("content") String content,
                    @RequestParam("senderId") int senderId,
                    @RequestParam("receiverId") int receiverId);

    //@GetMapping("/message/{id}")
    Response<MessageDTO> get(@PathVariable int id);

    @GetMapping("/message/system")
    Response<Map<String, Object>> getSystemList(
            @RequestParam int type, @RequestParam("userId") int receiverId, @RequestParam int lastId);

    @GetMapping("/message/course-apply")
    Response<Map<String, Object>> getCourseApplyList(@RequestParam("userId") int senderId, @RequestParam int lastId);

    @PostMapping("/message/invite")
    Response Invite(@RequestParam int userId, @RequestParam int nodeId);

}
