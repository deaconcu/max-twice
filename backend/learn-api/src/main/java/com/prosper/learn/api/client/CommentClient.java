package com.prosper.learn.api.client;

import com.prosper.learn.dto.CommentDTO;
import com.prosper.learn.dto.CommentDTOV1;
import com.prosper.learn.dto.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(value = "learn-service", contextId = "comment")
public interface CommentClient {

    @PostMapping("/comment")
    Response<Object> create(@RequestBody CommentDTO commentDTO);

    @PutMapping("/comment")
    Response<Object> approve(@RequestParam(value="id") int id, @RequestParam(value="action") boolean approve);

    @GetMapping("/comment")
    Response<List<CommentDTO>> getByObject(@RequestParam(value = "objectId") int objectId,
                                           @RequestParam(value = "type") int type,
                                           @RequestParam(value="offsetId") int offsetId);

    @GetMapping("/comment/{id}/reply")
    Response<List<CommentDTO>> getByTopic(@PathVariable(value = "id") int commentId,
                                          @RequestParam(value="offsetId") int offsetId);

    @GetMapping("/comment/censor")
    Response<List<CommentDTOV1>> getCensorList();
}