package com.prosper.learn.api.client;

import com.prosper.learn.dto.response.message.MessageDTO;
import com.prosper.learn.dto.response.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(value = "learn-service", contextId = "courseRequest")
public interface MessageClient {

    //@PostMapping("/message")
    Response create(@RequestParam("content") String content,
                    @RequestParam("senderId") Long senderId,
                    @RequestParam("receiverId") Long receiverId);

    //@GetMapping("/message/{id}")
    Response<MessageDTO> get(@PathVariable Long id);

    //@GetMapping("/message/system")
    Response<Map<String, Object>> getSystemList(
            @RequestParam int type, @RequestParam("userId") Long receiverId, @RequestParam Long lastId);

    //@GetMapping("/message/course-apply")
    Response<Map<String, Object>> getCourseApplyList(@RequestParam("userId") Long senderId, @RequestParam Long lastId);

    //@PostMapping("/message/invite")
    Response Invite(@RequestParam Long userId, @RequestParam Long nodeId);

}
