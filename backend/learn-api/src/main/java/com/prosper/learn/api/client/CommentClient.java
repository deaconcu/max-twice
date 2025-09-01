package com.prosper.learn.api.client;

import com.prosper.learn.dto.response.CommentDTO;
import com.prosper.learn.dto.response.CommentDTOV1;
import com.prosper.learn.dto.response.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(value = "learn-service", contextId = "comment")
public interface CommentClient {

    //@PostMapping("/comment")
    Response<Object> create(@RequestBody CommentDTO commentDTO);

    //@PutMapping("/comment")
    Response<Object> approve(@RequestParam(value="id") Long id, @RequestParam(value="action") boolean approve);

    //@GetMapping("/comment")
    Response<List<CommentDTO>> getByObject(@RequestParam(value = "objectId") Long objectId,
                                           @RequestParam(value = "type") int type,
                                           @RequestParam(value="offsetId") Long offsetId);

    //@GetMapping("/comment/{id}/reply")
    Response<List<CommentDTO>> getByTopic(@PathVariable(value = "id") Long commentId,
                                          @RequestParam(value="offsetId") Long offsetId);

    //@GetMapping("/comment/censor")
    Response<List<CommentDTOV1>> getCensorList();
}