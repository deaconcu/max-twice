package com.prosper.learn.api.client;

import com.prosper.learn.dto.PostDTO;
import com.prosper.learn.dto.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(value = "learn-service", contextId = "nodeList")
public interface PostClient {

    // 在某一个node下创建内容
    @PostMapping("/posting")
    Response create(@RequestBody PostDTO posting);

    // 修改
    @PutMapping("/posting")
    Response modify(@RequestBody PostDTO posting);

    @DeleteMapping("/posting")
    Response delete(@RequestParam int id);

    @GetMapping("/posting/{id}")
    PostDTO get(@PathVariable int id);

    // 获得某个node下的发帖
    @GetMapping("/node/{nodeId}/posting")
    List<PostDTO> getPostings(@PathVariable int nodeId);

    @GetMapping("/node/{nodeId}/postings")
    List<PostDTO> getByLastId(@PathVariable int nodeId, @RequestParam("lastId") int lastPostingId);

    // todo not use
    //@GetMapping("/postings")
    Response<Object> get(@RequestParam(value = "id", required = false) List<Integer> ids,
                         @RequestParam(value = "nodeId", required = false) int nodeId,
                         @RequestParam(value = "lastId", required = false) int lastPostingId);

    // 获得审核列表
    @GetMapping("/post/censor")
    Response<List<PostDTO>> getCensorList();

    @PutMapping("/post")
    Response<Object> approve(@RequestParam(value="id") int id, @RequestParam(value="action") boolean approve);

    /*
    // 选择某一个帖子作为目录
    @PostMapping("/posting/{postingId}/choose")
    Response choose(@PathVariable int postingId,
                    @RequestParam int courseId,
                    @RequestParam String currentPath,
                    @RequestParam int userId);
     */



}