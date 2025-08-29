package com.prosper.learn.api.client;

import com.prosper.learn.dto.Response;
import com.prosper.learn.dto.UserDTO;
import com.prosper.learn.dto.UserDTOV2;
import com.prosper.learn.dto.UserDTOV4;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(value = "learn-service", contextId = "user")
public interface UserClient {

    //@GetMapping("/self")
    Response<UserDTO> getSelf();

    //@PostMapping("/self")
    Response<Object> modifySelf(@RequestParam String name, @RequestParam String biography);

    // @GetMapping("/user/{id}")
    Response<Object> getUser(@PathVariable Long id);

    // @GetMapping("/user")
    Response<List<UserDTOV4>> getUserByName(@RequestParam String name);

    // @PostMapping("/user")
    Response<Object> register(@RequestParam String userName, @RequestParam String email, @RequestParam String password);

    // @PostMapping("/login")
    Response<UserDTOV2> login(@RequestParam String email, @RequestParam String password);

    // @PostMapping("/user/validate")
    Response<UserDTO> validateMail(@RequestParam String email, @RequestParam String code);

    // @GetMapping("/user/article")
    Response<Object> getSelfArticle(@RequestParam Long userId, @RequestParam Long lastId);

    // @GetMapping("/user/contents")
    Response<Object> getSelfContents(@RequestParam Long userId, @RequestParam Long lastId);

    // @GetMapping("/user/subscription")
    Response<Object> getSubscription(@RequestParam Long userId);

    // @PostMapping("/user/subscription")
    Response<Object> subscript(@RequestParam Long courseId);

    //@PutMapping("/user/subscription")
    Response<Object> subscript(@RequestParam String subscription);

    // @DeleteMapping("/user/subscription")
    Response<Object> unsubscript(@RequestParam Long courseId);

    // @PostMapping("/user/follow")
    Response<Object> follow(@RequestParam Long followeeId);

    //@DeleteMapping("/user/follow")
    Response<Object> unfollow(@RequestParam Long followeeId);

    // @GetMapping("/user/followee")
    Response<Object> followee(@RequestParam Long followerId, @RequestParam String lastCreateTime);

    // @PostMapping("/user/complete/{nodeId}")
    Response<Object> markNodeCompleted(@PathVariable Long nodeId, @RequestParam Long courseId);

    // @DeleteMapping("/user/complete/{nodeId}")
    Response<Object> unmarkNodeCompleted(@PathVariable Long nodeId, @RequestParam Long courseId);

    //@GetMapping("/user/complete/{nodeId}")
    Response<Object> isNodeCompleted(@PathVariable Long nodeId);

    //@PostMapping("/user/complete/course/{courseId}")
    Response<Object> markCourseCompleted(@PathVariable Long courseId);
}


