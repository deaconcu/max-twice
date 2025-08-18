package com.prosper.learn.api.client;

import com.prosper.learn.dto.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(value = "learn-service", contextId = "upvote")
public interface UpvoteClient {

    @GetMapping("/upvotes")
    List<Integer> getUpvotedList(
            @RequestParam("userId") int userId,
            @RequestParam("postingIds") List<Integer> postingIds);

    @GetMapping("/upvote")
    boolean isUpvoted(
            @RequestParam("userId") int userId,
            @RequestParam("postingId") int postingId);

    @PostMapping("/posting/{id}/vote")
    Response upvote(@PathVariable int id, @RequestParam("userId") int userId, @RequestParam("type") int type);

    @DeleteMapping("/posting/{id}/vote")
    Response cancelVote(@PathVariable int id);

}
