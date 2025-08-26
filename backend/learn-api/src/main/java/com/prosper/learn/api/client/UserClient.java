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

    @GetMapping("/self")
    Response getSelf();

    @PostMapping("/self")
    Response modifySelf(@RequestParam String name, @RequestParam String biography);

    @GetMapping("/user/{id}")
    Response getUser(@PathVariable int id);

    @GetMapping("/user")
    Response<List<UserDTOV4>> getUserByName(@RequestParam String name);

    @PostMapping("/user")
    Response register(@RequestParam String userName, @RequestParam String email, @RequestParam String password);

    @PostMapping("/login")
    Response<UserDTOV2> login(@RequestParam String email, @RequestParam String password);

    @PostMapping("/user/validate")
    Response<UserDTO> validateMail(@RequestParam String email, @RequestParam String code);

    @GetMapping("/user/article")
    Response getSelfArticle(@RequestParam int userId, @RequestParam int lastId);

    @GetMapping("/user/contents")
    Response getSelfContents(@RequestParam int userId, @RequestParam int lastId);

    @GetMapping("/user/subscription")
    Response getSubscription(@RequestParam int userId);

    @PostMapping("/user/subscription")
    Response subscript(@RequestParam int courseId);

    @PutMapping("/user/subscription")
    Response subscript(@RequestParam String subscription);

    @DeleteMapping("/user/subscription")
    Response unsubscript(@RequestParam int courseId);

    @PostMapping("/user/follow")
    Response follow(@RequestParam int followeeId);

    @DeleteMapping("/user/follow")
    Response unfollow(@RequestParam int followeeId);

    @GetMapping("/user/followee")
    Response followee(@RequestParam int followerId, @RequestParam String lastCreateTime);

    @PostMapping("/user/complete/{nodeId}")
    Response markNodeCompleted(@PathVariable Integer nodeId);

    @DeleteMapping("/user/complete/{nodeId}")
    Response unmarkNodeCompleted(@PathVariable Integer nodeId);

    @GetMapping("/user/complete/{nodeId}")
    Response isNodeCompleted(@PathVariable Integer nodeId);
}


