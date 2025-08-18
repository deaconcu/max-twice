package com.prosper.learn.api.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(value = "learn-service", contextId = "courseRequest")
public interface HistoryClient {

    // 获取某一篇文章的历史列表
    @GetMapping("/posting/{id}/history")
    List<com.prosper.learn.dto.HistoryDTO> listByPostingId(@PathVariable int id,
                                     @RequestParam("page") int page,
                                     @RequestParam("pageSize") int pageSize);

    @GetMapping("/history/{id}")
    void prove(@PathVariable int id);

}
