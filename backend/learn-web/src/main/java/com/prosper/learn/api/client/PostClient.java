package com.prosper.learn.api.client;

import com.prosper.learn.dto.response.old.PostDTOV1;
import com.prosper.learn.dto.response.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(value = "learn-service", contextId = "nodeList")
public interface PostClient {

    // 在某一个node下创建内容
    //@PostMapping("/posting")
    Response create(@RequestBody PostDTOV1 posting);

    // 修改
    //@PutMapping("/posting")
    Response modify(@RequestBody PostDTOV1 posting);

    //@DeleteMapping("/posting")
    Response delete(@RequestParam Long id);

    //@GetMapping("/posting/{id}")
    PostDTOV1 get(@PathVariable Long id);

    // 获得某个node下的发帖
    //@GetMapping("/node/{nodeId}/posting")
    List<PostDTOV1> getPostings(@PathVariable Long nodeId);

    //@GetMapping("/node/{nodeId}/postings")
    List<PostDTOV1> getByLastId(@PathVariable Long nodeId, @RequestParam("lastId") Long lastPostingId);

    // todo not use
    //@GetMapping("/postings")
    Response<Object> get(@RequestParam(value = "ids", required = false) List<Long> ids,
                         @RequestParam(value = "nodeId", required = false) Long nodeId,
                         @RequestParam(value = "lastId", required = false) Long lastPostingId);

    // 获得审核列表
    //@GetMapping("/post/censor")
    Response<List<PostDTOV1>> getCensorList();

    //@PutMapping("/post")
    Response<Object> approve(@RequestParam(value="id") Long id, @RequestParam(value="action") boolean approve);
}