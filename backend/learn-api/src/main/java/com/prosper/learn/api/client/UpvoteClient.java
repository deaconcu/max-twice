package com.prosper.learn.api.client;

import com.prosper.learn.dto.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(value = "learn-service", contextId = "upvote")
public interface UpvoteClient {

    @GetMapping("/upvotes")
    List<Long> getUpvotedList(
            @RequestParam("userId") Long userId,
            @RequestParam("postingIds") List<Long> postingIds);

    @GetMapping("/upvote")
    boolean isUpvoted(
            @RequestParam("userId") Long userId,
            @RequestParam("postingId") Long postingId);

    @PostMapping("/posting/{id}/vote")
    Response upvote(@PathVariable Long id, @RequestParam("userId") Long userId, @RequestParam("type") int type);

    @DeleteMapping("/posting/{id}/vote")
    Response cancelVote(@PathVariable Long id);

}
